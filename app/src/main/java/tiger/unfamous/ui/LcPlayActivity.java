package tiger.unfamous.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.common.ApplicationGlobalVariable;
import tiger.unfamous.data.Book;
import tiger.unfamous.data.Play_State;
import tiger.unfamous.services.MainService;
//import tiger.unfamous.services.DurationToker;
import tiger.unfamous.utils.AccountConnect;
import tiger.unfamous.utils.CommonDlg;
import tiger.unfamous.utils.MyLog;
import tiger.unfamous.utils.Utils;

public class LcPlayActivity extends CommonPlayUI {
	public static final String TAG = "LcPlayActivity";
	private static final int REFRESH = 1;
	// private static final int MENU_DOWNLOAD_QUEUE = Menu.FIRST;
	// private static final int MENU_SHARE = Menu.FIRST + 1;

	private static MyLog log = new MyLog();

	private ListView mListView;

//	private DurationToker mDurationToker = new DurationToker();

	public void updateListView() {
		if (mListView != null && (mListView.getAdapter() != null)) {
			((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
		}
	}

	private Book mCurBook;
	private File mCurBrowseDir;
	// private String mCurSongName;
	private final ArrayList<File> mFiles = new ArrayList<File>();
	private boolean mDoPlay;
//	private boolean mIsShortCut;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// onPreCreate();
		
//		Log.e("MyLog", "LcPlayActivity onCreate");

		Intent i = getIntent();

		if (savedInstanceState != null) {
			mRestored = true;
			mCurBrowseDir = (File) savedInstanceState
					.getSerializable(DN.CUR_PLAY_DIR);
			mCurBook = (Book) savedInstanceState
					.getSerializable(DN.CUR_PLAY_BOOK);

		}

		super.onCreate(savedInstanceState);
	}

	// @Override
	// public void onDestroy() {
	// super.onDestroy();
	// }

	@Override
	protected void onServiceConnected() {
		// if (isPlaying() && !mService.mPlayingOnline) {
		Intent i = getIntent();

		if (!i.getBooleanExtra("shortcut", false)) {

			if (i != null && i.getBooleanExtra(DN.DO_PLAY, false)) {
				mCurBook = mService.mCurBrowseBook;
			} else {
				mCurBook = mService.mCurPlayBook;
			}
		}
		mService.setPlayActivity(this);
		mService.mCurPlayBook = mCurBook;
		mService.mCurBrowseBook = mCurBook;

		// }
		setContentView(R.layout.lcplayer);
		Utils.setColorTheme(findViewById(R.id.night_mask));
		Utils.addAdView(this);

		mListView = (ListView) findViewById(R.id.songslist);
		mListView.setOnItemClickListener(this);
		// mTopTitle = (TextView)findViewById(R.id.wy_title);
		mSeekBar = (SeekBar) findViewById(android.R.id.progress);
		mSeekBar.setOnSeekBarChangeListener(this);
		mSeekBar.setMax(MAX_SEEK_VALUE);

		mStopButton = (ImageButton) findViewById(R.id.stop);
		mStopButton.setOnClickListener(mExitListener);
		// setStopBtnToExit(mStopButton, mService.isPlayStopped() ? 2 : 1);
		mPauseButton = (ImageButton) findViewById(R.id.pause);
		mPauseButton.requestFocus();
		mPauseButton.setOnClickListener(mPauseListener);

		mCurrentTime = (TextView) findViewById(R.id.currenttime);
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
		mTotalTime = (TextView) findViewById(R.id.totaltime);
		mTopTitle = (TextView) findViewById(R.id.playingtitle);
		mBtnSetting = (Button) findViewById(R.id.btn_setting);
		mBtnExit = (Button) findViewById(R.id.btn_exit);
		mBtnErrorReport = (Button) findViewById(R.id.button_report);

		mBtnErrorReport.setOnClickListener(this);

		mBtnExit.setOnClickListener(this);
		mBtnSetting.setOnClickListener(this);
		mSongShareBtn = (Button) findViewById(R.id.share);
		mSongShareBtn.setOnClickListener(shareClick);
		mNextSongBtn = (ImageButton) findViewById(R.id.next_Song);
		mNextSongBtn.setOnClickListener(this);

		mSongName = (TextView) findViewById(R.id.song_name);

		/*
		 * findViewById(R.id.prev_page).setEnabled(false);
		 * findViewById(R.id.next_page).setEnabled(false);
		 */

		if (mService.isPlaying()) {
			setPauseButtonImage();
		} else if (mService.isPlayStopped()) {
			enablePlayBtn(false);
		}
		updateTrackInfo();
		long next = refreshNow();
		queueNextRefresh(next);

		if (mRestored && mCurBook != null) {
			// mCurBook.mChapName = mCurSongName;
			browseTo(mCurBrowseDir);
		} else {
			String dirPath = Cfg.DOWNLOAD_DIR;
			if (!i.getBooleanExtra("shortcut", false))
				mDoPlay = i.getBooleanExtra(DN.DO_PLAY, false);
			mShowPlaying = i.getBooleanExtra(DN.SHOW_PLAYING_SONG, false);
			mFromNotify = i.getBooleanExtra(DN.FROM_NOTIFY, false);
			mLocateHistory = i.getBooleanExtra(DN.LOCATE_HISTORY, false);
			Uri uri = i.getData();
			if (uri != null) {
				String str = uri.toString();
				// if (str.contains(".mp3") || str.contains(".wma")) {
				mDoPlay = true;
				int lastSlashIdx = str.lastIndexOf('/');
				int firsSlashIdx = str.indexOf('/');
				// mCurBook.mChapName = URLDecoder.decode(str
				// .substring(lastSlashIdx + 1));
				// mService.mLcPlayDirPath = URLDecoder.decode(str.substring(
				// firsSlashIdx, lastSlashIdx));
				String songName = URLDecoder.decode(str
						.substring(lastSlashIdx + 1));
				dirPath = URLDecoder.decode(str.substring(firsSlashIdx,
						lastSlashIdx));
				mCurBook = new Book(dirPath, songName);
				mService.mCurBrowseBook = mCurBook;
				// }
			} else {
				if (mCurBook == null) {
					log.e("mCurBook == null, may be killed before!");
					MobclickAgent.onEvent(this, Cfg.UM_EXPTION,
							"LcPlayActivity:onServiceConnected0");
					// finish(); //可能导致一直无法启动
					return;
				}
				dirPath = mCurBook.mLcDirPath;
			}

			if (mDoPlay || mShowPlaying) {
				dirPath = mCurBook.mLcDirPath;
			} else if (mLocateHistory) {
				// mHistory = mService.mHistory;
				// dirPath = mHistory.mLcDirPath;
			}

			if (dirPath != null) {
				browseTo(new File(dirPath));
			} else if (mFromNotify) {
				Log.e(TAG, "onServiceConnected1");
				MobclickAgent.onEvent(this, Cfg.UM_EXPTION,
						"LcPlayActivity:onServiceConnected1");
				CommonDlg.showErrorDlg(this,
						"进程意外中止，如果您使用了进程杀手工具，建议将善听加入到排除列表！",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								finish();
							}
						});
			} else {
				MobclickAgent.onEvent(this, Cfg.UM_EXPTION,
						"LcPlayActivity:onServiceConnected2");
				Log.e(TAG, "onServiceConnected2");
			}
		}

		if (!mService.isPlaying() || !mService.hasNextSong()) {
			mNextSongBtn.setEnabled(false);
		}
		if (mService.mCurPlayBook != null) {
			mSongName.setText(mService.mCurPlayBook.mSongName);
		}
		mFromNotify = false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// log.d("onSaveInstanceState");
		if (mService != null) {
			outState.putSerializable(DN.CUR_PLAY_DIR, mCurBrowseDir);
			outState.putSerializable(DN.CUR_PLAY_BOOK, mCurBook);
		} else {
			MobclickAgent.onEvent(this, Cfg.UM_EXPTION,
					"LcPlayActivity:onSaveInstanceState");
			log.e("Service is null!!");
		}
		super.onSaveInstanceState(outState);
	}


	private final View.OnClickListener mExitListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			mHandler.removeMessages(REFRESH);
			if (!mService.isPlayStopped()) {
				mService.saveHistory();
				mService.stopMusic();

				if (mService.isTimerRunning()) {
					mService.cancelTimer();
				}
			}
			finish();
		}
	};

	public void refresh() {
		if (mCurBrowseDir != null) {
			browseTo(mCurBrowseDir);
		}
	}

	private void sortFiles(ArrayList<File> files) {
		try {
			Collections.sort(files, new Comparator<File>() {
				@Override
				public int compare(File object1, File object2) {
					String s1 = object1.getName();
					String s2 = object2.getName();
					Pattern p = Pattern.compile("\\d+");
					Matcher m1 = p.matcher(s1);
					Matcher m2 = p.matcher(s2);
					int ret = 0;
					boolean completed = false;
					while (!completed) {
						int intVal = 0;
						if (m1.find()) {
							s1 = m1.group();
							intVal++;
						}
						if (m2.find()) {
							s2 = m2.group();
							intVal++;
						}
						if (intVal == 2) {
							try {
								ret = Integer.parseInt(s1)
										- Integer.parseInt(s2);
								if (ret != 0) {
									completed = true;
								} else {
									completed = false;
								}
							} catch (Exception e) {
								// TODO: handle exception
								completed = false;
							}
						} else {
							completed = true;
						}
					}
					return ret;
				}
			});
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	private synchronized void browseTo(final File location) {
		if (location == null) {
			// finish();
			return;
		}

		mCurBrowseDir = location;

		// mDirFiles.clear();
		mFiles.clear();


		File[] files = mCurBrowseDir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (!file.isDirectory() && Utils.isMp3orWma(file.getName())) {
					mFiles.add(file);
				}
			}
		}

		sortFiles(mFiles);

		SongsListAdapter adapter = ((SongsListAdapter) mListView.getAdapter());
		if (adapter == null) {
			mListView.setAdapter(new SongsListAdapter(this));
		} else {
			adapter.notifyDataSetChanged();
		}

		String title = null;
		if (mDoPlay || mShowPlaying || mLocateHistory || mRestored) {
			int idx = getCurSongIdx();
			if (idx != -1) {
				// mListView.setSelection(idx);
				if (mDoPlay) {
					mListView.performItemClick(new View(this), idx, idx);
				} else if (mLocateHistory) {
					// mCurBook = mService.mHistory;
					long pos = mCurBook.mPosition;
					if (pos == MainService.POS_COMPLETION) {
						if (idx + 1 < mFiles.size()) {
							idx++;
							mListView
									.performItemClick(new View(this), idx, idx);
						} else if (mFiles.size() == 1) {
							mListView
									.performItemClick(new View(this), idx, idx);
						}
					} else {
						mListView.performItemClick(new View(this), idx, idx);
					}
				}
				mListView.setSelection(idx);
			}
			title = mCurBook.mTitle;
			mDoPlay = mShowPlaying = mRestored = false;
		} else {
			title = mCurBrowseDir.getName().compareTo("") == 0 ? mCurBrowseDir
					.getPath() : mCurBrowseDir.getName();
		}
		mTopTitle.setText(title);
	}

	private int getCurSongIdx() {
		if (mCurBook == null) {
			return -1;
		}
		for (File f : mFiles) {
			if (f.getName().equals(mCurBook.mSongName)) {
				return mFiles.indexOf(f);
			}
		}
		return -1;
	}

	protected class SongsListAdapter extends BaseAdapter {
		protected Context context;
		// private File anYueSong;
		LayoutInflater mInflate;

		public SongsListAdapter(Context context) {
			this.context = context;
			mInflate = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mFiles.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mFiles.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		public void setColor(TextView tv, boolean playing) {
			if (playing) {
				tv.setTextColor(Color.RED);
			} else {
				tv.setTextColor(Color.BLACK);
			}
		}

		@Override
		public View getView(final int index, View convertView, ViewGroup parent) {
			File currentFile = mFiles.get(index);
			LinearLayout ll = (LinearLayout) convertView;
			if (ll == null) {
				ll = (LinearLayout) mInflate.inflate(R.layout.local_file_item,
						null, false);
			}
			ImageView iconImg = (ImageView) ll.findViewById(R.id.icon);
			ImageView delImage = (ImageView) ll.findViewById(R.id.file_del);
			TextView songName = (TextView) ll.findViewById(R.id.file_name);
			// TextView songSize = (TextView) ll.findViewById(R.id.file_size);

			String filename;
			if (index == 0
					&& (currentFile.getParentFile() == null || currentFile
							.getParentFile().getAbsolutePath()
							.compareTo(mCurBrowseDir.getAbsolutePath()) != 0)) {
				iconImg.setImageResource(R.drawable.updirectory);
				delImage.setVisibility(View.GONE);
				filename = new String("..");
			} else {
				if (currentFile.isDirectory()) {
					iconImg.setImageResource(R.drawable.directory);
				} else {
					iconImg.setImageResource(R.drawable.audio);
				}
				delImage.setVisibility(View.VISIBLE);
				filename = currentFile.getName();
			}

//			if (currentFile.isFile()) {
//				// songSize.setVisibility(View.VISIBLE);
//				mDurationToker.setFileLength(currentFile.length());
//				long seconds = mDurationToker.getTime(currentFile);
//				filename += " [" + seconds / 60 + "." + (seconds % 60) / 6
//						+ "\']";
//
//			}
			// setColor(filename,anYueSong.isColorPlay());
			songName.setText(filename);
			boolean playing = false;
			if (currentFile.getName().equals(mCurBook.mSongName)) {
				playing = true;
			}
			setColor(songName, playing);

			delImage.setImageResource(R.drawable.list_item_del);
			delImage.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					final File file = mFiles.get(index);
					String tip = file.isDirectory() ? "\n\n确定删除整个文件夹？                                                                                     "
							: "\n\n确定删除？                                                                                          ";
					CommonDlg.showConfirmDlg(LcPlayActivity.this, -1,
							file.getName() + tip,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									final File parent = file.getParentFile();
									// file.delete();
									removeFile(file);
									browseTo(parent);
								}
							});

				}
			});

			return ll;
		}

		public void removeFile(File file) {
			Log.e(TAG, "removing file " + file.getPath());
			if (file.isDirectory()) {
				File[] child = file.listFiles();
				if (child != null && child.length != 0) {
					for (File f : child) {
						removeFile(f);
						f.delete();
					}
				}
			}
			file.delete();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, final int arg2,
			long id) {
		final File file = mFiles.get((int) id);

		// if (file.isDirectory()) {
		// browseTo(file);
		// } else {

		if (!mService.isPlayStopped()) {
			CommonDlg.showConfirmDlg(LcPlayActivity.this, -1, file.getName()
					+ "\n\n" + getString(R.string.play_confirm),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							mService.mCurPlayBook = mService.mCurBrowseBook;
							startPlayback(file);
							setNextSongBtn(arg2);
						}
					});
		} else {
			mService.mCurPlayBook = mService.mCurBrowseBook;
			startPlayback(file);
			setNextSongBtn(arg2);
		}

//		MobclickAgent.onEvent(LcPlayActivity.this, Cfg.UM_SONG_CLICK);
		// }
	}

	private void setNextSongBtn(int selected) {
		if (mFiles.size() == 1 || selected == mFiles.size() - 1) {
			mNextSongBtn.setEnabled(false);
		} else {
			mNextSongBtn.setEnabled(true);
		}
	}

	private void startPlayback(final File f) {
		// mService.setPlayActivity(this);

		mCurBook.mSongName = f.getName();

		final long posToSeek;
		if (mLocateHistory) {
			mLocateHistory = false;
			long hisPos = mCurBook.mPosition;
			posToSeek = hisPos < MainService.POS_COMPLETION ? hisPos : 0L;
			System.out.println("posToSeek------->" + posToSeek);
		} else {
			posToSeek = 0L;
		}
		mCurBook.mPosition = posToSeek; // 覆盖POS_COMPLETION

		{
			mCurrentTime.setVisibility(View.VISIBLE);
			mCurrentTime.setText(R.string.wait);
			mTotalTime.setVisibility(View.GONE);
			mProgressBar.setVisibility(View.VISIBLE);
			mTopTitle.setText(mCurBook.mTitle);
			mService.playMusic(f, posToSeek, this);
			ApplicationGlobalVariable.setBoolean(true);
			ApplicationGlobalVariable.setmCurBook(mCurBook);
//			ApplicationGlobalVariable.setmCurBrowseDir(mCurBrowseDir);
//			ApplicationGlobalVariable.setmListView(mListView);
			ApplicationGlobalVariable.setmFiles(mFiles);
		}
		// setCustomTopTitle(" - " + mService.getCurSong().getName());

		// }
	}

	// public void setTitle(CharSequence title) {
	// mTopTitle.setText(title);
	// }

	public void enablePlayBtn(boolean enabled) {
		mPauseButton.setEnabled(enabled);
	}

	public boolean isPlaying() {
		// TODO Auto-generated method stub
		return mService.isPlaying();
	}

	boolean isPaused() {
		return mService.isPlayPaused();
	}

//	public void setPauseButtonImage() {
//		if (mService != null && isPlaying()) {
//			mPauseButton.setImageResource(R.drawable.button_pause);
//		} else {
//			mPauseButton.setImageResource(R.drawable.player_play_btn);
//		}
//	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REFRESH:
				long next = refreshNow();
				queueNextRefresh(next);
				break;

			default:
				break;
			}
		}
	};

	private void queueNextRefresh(long delay) {
		if (!mService.isPlayStopped()) {
			// Log.d(TAG,"next update time:" + delay);
			Message msg = mHandler.obtainMessage(REFRESH);
			mHandler.removeMessages(REFRESH);
			mHandler.sendMessageDelayed(msg, delay);
		}
	}

	private long refreshNow() {
		if (mService == null) {
			return TIME_1000MS;// 500;
		}
		long pos = mService.position(); // mPosOverride < 0 ?
										// mService.position() : mPosOverride;
		setmDuration(mService.duration());
		long remaining = TIME_1000MS;// - (pos % TIME_1000MS);
		if ((pos > 0) && (mDuration > 0)) {
			// Log.i("pos", "refresh :"+pos+"duration:"+mDuration);
			mCurrentTime.setText(Utils.makeTimeString(this, pos / TIME_1000MS));
			// if (mProgressBar.getVisibility() == View.VISIBLE) {
			// mProgressBar.setVisibility(View.GONE);
			// mTotalTime.setVisibility(View.VISIBLE);
			// }
			if (isPlaying()) {
				// Log.i("refresh", "isPlaying");
				mCurrentTime.setVisibility(View.VISIBLE);
				if (mOldPos == pos) {
					mCurrentTime.setText(R.string.wait);
					mTotalTime.setVisibility(View.GONE);
					mProgressBar.setVisibility(View.VISIBLE);
				} else if (mProgressBar.getVisibility() == View.VISIBLE) {
					updateTrackInfo();
				}
				mOldPos = pos;
			} else if(isPaused()){
				// Log.i("refresh", "cuttentTime.visible");
				// blink the counter
				int vis = mCurrentTime.getVisibility();
				mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE
						: View.INVISIBLE);
				// remaining = 500;
			}
			setFirstProgress((int) (MAX_SEEK_VALUE * pos / mDuration));
			// Log.e(TAG, "refreshNow,pos:" + pos);
		}

		// return the number of milliseconds until the next full second, so
		// the counter can be updated at just the right time
		return remaining;
	}

	private void updateTrackInfo() {
		if (mService == null) {
			return;
		}

		if (mService.duration() > 0) {
			mProgressBar.setVisibility(View.GONE);
			mTotalTime.setVisibility(View.VISIBLE);
			mTotalTime.setText(Utils.makeTimeString(this, mDuration
					/ TIME_1000MS));
		}

		// Song song = mService.getCurSong();
		// if (song != null) {
		// mCustomTopTitle.setText(" - " + song.getName());
		if (mCurBook != null) {
			String songName = mCurBook.mSongName;
			if (!TextUtils.isEmpty(songName)) {
				setTitle(songName);
			}
		}
		// }
	}

	@Override
	public void onStateChanged(Play_State curState) {
		// TODO Auto-generated method stub
//		mService.mPlayState = curState;
//		super.onStateChanged(curState);

		switch (curState) {
			case PREPARING:
				mCurrentTime.setVisibility(View.VISIBLE);
				mCurrentTime.setText(R.string.wait);
				mTotalTime.setVisibility(View.GONE);
				mProgressBar.setVisibility(View.VISIBLE);
				break;
			case STARTED:
				// setStopBtnToExit(mStopButton, 1);
				updateListView();
				enablePlayBtn(true);
				setPauseButtonImage();
				long next = refreshNow();
				queueNextRefresh(next);
				if (!mService.hasNextSong()) {
					mNextSongBtn.setEnabled(false);
				} else {
					mNextSongBtn.setEnabled(true);
				}
				// mService.lockWifi();
				break;
			case PAUSED:
				setPauseButtonImage();
				break;
			case STOPPED:
				// setStopBtnToExit(mStopButton, 2);
				setPauseButtonImage();
				enablePlayBtn(false);
				setFirstProgress(0);
				setSecondProgress(0);
				mCurrentTime.setVisibility(View.INVISIBLE); // 设成gone的话右边控件会左移
				mProgressBar.setVisibility(View.GONE);
				// mTotalTime.setVisibility(View.GONE);
				if (mCurBrowseDir != null) {
					String dirName = mCurBrowseDir.getName();
					// if (!TextUtils.isEmpty(dirName)) {
					setTitle(TextUtils.isEmpty(dirName) ? mCurBrowseDir.getPath()
							: dirName);
					// }
				}
				break;
		}
	}

	@Override
	public boolean playNextSong() {
		// TODO Auto-generated method stub
		int index = -1;
		for (File f : mFiles) {
			if (f.getName().equals(mCurBook.mSongName)) {
				index = mFiles.indexOf(f);
			}
		}
		if (index == -1) {
			return false;
		}
		index++;
		if (index >= mFiles.size() - 1) {
			mNextSongBtn.setEnabled(false);
		} else {
			mNextSongBtn.setEnabled(true);
		}
		if (index == mFiles.size()) {
			return false;
		}
		File file = mFiles.get(index);
		if (file != null) {
			startPlayback(file);
		}
		return true;
	}

	@Override
	public void setSecondProgress(int secondProgress) {
		// TODO Auto-generated method stub
		mSeekBar.setSecondaryProgress(secondProgress);
	}



	@Override
	public boolean playPreviousSong() {
		int index = 0;
		for (File f : mFiles) {
			if (f.getName().equals(mCurBook.mSongName)) {
				index = mFiles.indexOf(f);
			}
		}
		if (index == 0) {
			return false;
		}
		index--;
		if (index == mFiles.size()) {
			return false;
		}
		mListView.performItemClick(new View(this), index, index);
		return true;
	}


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btn_exit:
			Intent i = new Intent(LcPlayActivity.this, Main.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			finish();
			break;
		case R.id.btn_setting:
			Intent intent = new Intent(LcPlayActivity.this,
					Preferences.class);
			startActivity(intent);
			break;
		case R.id.next_Song:
			playNextSong();
//				MobclickAgent.onEvent(LcPlayActivity.this, Cfg.UM_SONG_CLICK);
			break;

		case R.id.button_report:
			reportResourceProblem();
			break;
		}
	}
	

	View.OnClickListener shareClick = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			shareSong();
		}

	};

	public void shareSong() {
		MobclickAgent.onEvent(LcPlayActivity.this, Cfg.UM_SHARE, Cfg.UM_SHARE2);
		Intent i = new Intent();
		
		i.putExtra(AccountConnect.INTENT_SHARE_SONG_NAME, mCurBrowseDir.getName());

		Utils.share(this, i);
	}


	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		Utils.setColorTheme(findViewById(R.id.night_mask));
		if(mService != null && !mService.isPlayStopped()){
			mHandler.sendEmptyMessage(REFRESH);
		}
//		Log.e("MyLog", "LcPlayActivity onResume");
		super.onResume();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
//		Log.e("MyLog", "LcPlayActivity onDestroy");
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_VOLUME_UP 
				|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			return super.onKeyDown(keyCode, event);
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			return super.onKeyDown(keyCode, event);
		} else {
			return false;
		}
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		mHandler.removeMessages(REFRESH);
		super.onPause();
	}
	
}