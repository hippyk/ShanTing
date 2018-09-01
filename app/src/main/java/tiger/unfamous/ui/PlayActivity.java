package tiger.unfamous.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.common.ApplicationGlobalVariable;
import tiger.unfamous.common.InternetStateMgr;
import tiger.unfamous.data.Book;
import tiger.unfamous.data.Play_State;
import tiger.unfamous.data.Song;
import tiger.unfamous.download.DownloadItem;
import tiger.unfamous.download.DownloadList;
import tiger.unfamous.download.DownloadListListener;
import tiger.unfamous.download.DownloadService;
import tiger.unfamous.services.MainService;
import tiger.unfamous.utils.AccountConnect;
import tiger.unfamous.utils.CommonDlg;
import tiger.unfamous.utils.DataBaseHelper;
import tiger.unfamous.utils.MyLog;
import tiger.unfamous.utils.Utils;

public class PlayActivity extends CommonPlayUI implements
		DownloadListListener/* , UpdatePointsNotifier  AccountListener, */{
	/** Called when the activity is first created. */

	public static final int SHARE_WORDS_COUNT = 107;
	public static final int SCORE_PER_DOWNLOAD = 30;
	public static final String CUR_PAGE = "curpage";

	private static MyLog log = new MyLog();

	private DownloadList m_list = null;
	private InternetStateMgr mInternetStateMgr;

	Book mCurPlayBook;
	Book mCurBrowseBook;
	String mCurSongName;
	Song song = null;
	// int tempInt;
	int mCurBrowsePage;
	int mUpdateBrowsePage;
	boolean isPrePage = false;
	boolean isNextPage = false;
	boolean isSkipPage = false;
	// 播放列表footerView
	LinearLayout mPlayListFooterView;
	ArrayList<Song> mCurBrowseList;
	// private boolean isPlayNextSongByManual = false;
	public static boolean mIsStartingDownAllSongs = false;
	// String tempStr;
	// Resources resources;
	AlertDialog.Builder bulider;
	Object mLock = new Object();
//	private boolean mIsShortCut;
	
	// 快捷方式接入的时候需要更新书架
	public ArrayList<Book> mBookList = new ArrayList<Book>();
	
	//当前选中的下载项的位置
	int mCurSelectedPos = 0;
	
	//是否整页购买都需要支付费用
//	boolean mIsPayForPageDown;
	
	boolean mNextSongByUserAction = false;
	
	long mLastDuration;
//	boolean needPayForPlaying = false;

	public void updateListView() {
		// if (mCurBrowseBook.mPageCount > Cfg.FIRST_PAGE) {
		if (mListView != null) {
			HeaderViewListAdapter headerViewListAdapter = (HeaderViewListAdapter) mListView
					.getAdapter();
			if (headerViewListAdapter != null) {
				BaseAdapter ba = (BaseAdapter) headerViewListAdapter
						.getWrappedAdapter();
				if (ba != null) {
					ba.notifyDataSetChanged();
				}
			}

		}

	}

	private static final int REFRESH = 1;
	private static final int DATACHANGE = 2;
	private static final int EVENT_DOWNLOAD_FAILED = 3;
	private static final int EVENT_NOINTERNET = 4;
	private static final int EVENT_SDCARD_NOT_MOUNTED = 5;
	private static final int EVENT_SDCARD_NO_SPACE = 6;
	private static final int EVENT_START_DOWN_ALL_SONGS_END = 7;
	private static final int EVENT_WAITING_FOR_CHARGE = 8;
	
	private static final String TAG = "PlayActivity";
	ProgressDialog dialog;



	// private ArrayList<Song> curBrowseList;

	DataBaseHelper database;

	// SongsListAdapter adapter;
	// public SeekBar mSeekBar;

	@Override
	public void onStateChanged(Play_State curState) {

//		super.onStateChanged(curState);

		switch (curState) {
			case PREPARING:
				mCurrentTime.setVisibility(View.VISIBLE);
				mCurrentTime.setText(R.string.wait);
				mTotalTime.setVisibility(View.GONE);
				mProgressBar.setVisibility(View.VISIBLE);
				break;
			case STARTED:
				mCurrentTime.setVisibility(View.VISIBLE);
				setPauseButtonImage();
				updateListView();
				enablePlayBtn(true);

				long next = refreshNow();
				queueNextRefresh(next);
				if (!mService.hasNextSong()) {
					mNextSongBtn.setEnabled(false);
				} else {
					mNextSongBtn.setEnabled(true);
				}
				mBtnErrorReport.setEnabled(true);
				ApplicationGlobalVariable.setBoolean(false);
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
				// mCurrentTime.setVisibility(View.INVISIBLE); //设成gone的话右边控件会左移
				mCurrentTime.setText(Utils.makeTimeString(this, 0));
				mTotalTime.setText(Utils.makeTimeString(this, 0));
				mProgressBar.setVisibility(View.GONE);
				// mTotalTime.setVisibility(View.GONE);
				// mCustomTitle.setText("");
				if (mCurBrowseBook != null) {
					setTitle(mCurBrowseBook.mTitle);
				}
				// mBtnErrorReport.setEnabled(false);
				break;
//
//		case MainService.PLAY_STATE_FINISHED:
//			mService.setMusicLocal(false);
		}
	}

	public boolean isPlaying() {
		// TODO Auto-generated method stub
		return mService.isPlaying();
	}




	private void queueNextRefresh(long delay) {
		mHandler.removeMessages(REFRESH);
		if (!mService.isPlayStopped()) {
			// Log.d(TAG,"next update time:" + delay);
			Message msg = mHandler.obtainMessage(REFRESH);
			mHandler.sendMessageDelayed(msg, delay);
		}
	}

	private long refreshNow() {

			if (mService == null) {
				return TIME_1000MS;// 500;
			}
			long pos = mService.position(); // mPosOverride < 0 ?
			// 每隔五分钟进行一次gc
			if (pos > 1000 * 60) {
				int minute = (int) (pos % (1000 * 60 * 5));
				if (minute >= 0 && minute <= 1000 * 3) {
					System.gc();
				}
			}

			setmDuration(mService.duration());
			// mTotalTime.setText(Utils.makeTimeString(this, mDuration
			// / TIME_1000MS));
			// mService.position() : mPosOverride;
			long remaining = TIME_1000MS;// - (pos % TIME_1000MS);
//			if ((pos > 0) && (mDuration > 0) && !mService.isPlayStopped()) {
			// enablePlayBtn(true);
			mCurrentTime.setText(Utils.makeTimeString(this, pos
					/ TIME_1000MS));
			if (isPlaying()) {
				// 设置进度为1，表明播放已经开始，滑动进度条不再弹回到起点
				if (this.mProgressValue == 0 && mDuration > 0
						&& mOldPos == 0) {
					int beginProgress = (int) (MAX_SEEK_VALUE * pos / mDuration);
					log.e("beginProgress = " + beginProgress);
					setFirstProgress(beginProgress);
				}

//					mCurrentTime.setVisibility(View.VISIBLE);
//					if (mOldPos == pos && !currentSongSkipPlay()) {
//						mCurrentTime.setText(R.string.wait);
//						mTotalTime.setVisibility(View.GONE);
//						mProgressBar.setVisibility(View.VISIBLE);
//					} else if (mProgressBar.getVisibility() == View.VISIBLE) {
					updateTrackInfo();
//					}
				mOldPos = pos;
			} else {
				// blink the counter
				int vis = mCurrentTime.getVisibility();
				mCurrentTime
						.setVisibility(vis == View.INVISIBLE ? View.VISIBLE
								: View.INVISIBLE);
				// remaining = 500;
			}

			// 重新获取AnYueService的position，跟mOldPos比较，解决拖动进度条带来的闪烁问题
//			try {
//				Thread.sleep(100);
//				remaining = TIME_1000MS - 100;
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			long position = mService.position();
//			int progress = 0;
//			if (mDuration > 0) {
//				progress = (int) (MAX_SEEK_VALUE * position / mDuration);
//			}
//			if (position > mOldPos) {
//				setFirstProgress(progress);
//			} else if (mService.position() == mDuration) {
//				setFirstProgress(MAX_SEEK_VALUE);
//			}
//			}

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

	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case REFRESH:
				long next = refreshNow();
				queueNextRefresh(next);
				break;

			case DATACHANGE:
				updateListView();
				break;

			case EVENT_DOWNLOAD_FAILED:
				Toast.makeText(PlayActivity.this,
						getString(R.string.download_fail_in_list),
						Toast.LENGTH_SHORT).show();
				break;


			case EVENT_NOINTERNET:
				Toast.makeText(PlayActivity.this, R.string.no_internet,
						Toast.LENGTH_SHORT).show();
				break;

			case EVENT_SDCARD_NOT_MOUNTED:
				Toast.makeText(PlayActivity.this,
						R.string.download_sdcard_not_mounted,
						Toast.LENGTH_SHORT).show();
				break;
			case EVENT_SDCARD_NO_SPACE:
				Toast.makeText(PlayActivity.this,
						R.string.download_sdcard_no_space, Toast.LENGTH_SHORT)
						.show();
				break;
			case EVENT_START_DOWN_ALL_SONGS_END:
				try {
					if (PlayActivity.this.mProgressDialog != null
							&& !PlayActivity.this.isFinishing()) {
						PlayActivity.this.mProgressDialog.cancel();
						PlayActivity.this.mProgressDialog = null;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case EVENT_WAITING_FOR_CHARGE:
				showContentProgress(true, getResources().getString(R.string.paying_for_download));
				break;
			default:
				break;
			}
		}
	};
	private boolean mLoadFirstTime = true;
	public static DownloadService.LocalBinder binder = null;
	private boolean m_isBound = false;
	private final ServiceConnection mDownloadConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			log.verbose("i am here: onServiceConnected enter");
			binder = (DownloadService.LocalBinder) service;
			m_isBound = true;

		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			m_isBound = false;
			binder = null;
			log.verbose("i am here: onServiceDisconnected");
		}

	};

	@Override
	protected void onServiceConnected() {
		Log.e(TAG, "ServiceConnected");
		initViews();
		mService.setPlayActivity(this);
		int curBrowsePage = 1;

		if (mRestored) {

			mService.mCurPlayBook = mCurPlayBook;
			mService.mCurBrowseBook = mCurBrowseBook;
			// mService.mCurPlayPage = mCurBrowsePage;
			// mService.mCurSongName = mCurSongName;
			Intent i = getIntent();
			if (i.getBooleanExtra("shortcut", false)) {
				if (mCurBrowseBook == null) {
					if (mFromNotify) {
						Log.e(TAG, "onServiceConnected1");
						MobclickAgent.onEvent(this, Cfg.UM_EXPTION,
								"PlayActivity:onServiceConnected1");
						CommonDlg.showErrorDlg(this,
								"进程意外中止，如果您使用了进程杀手工具，建议将善听加入到排除列表！",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface arg0,
											int arg1) {
										finish();
									}
								});
						return;
					} else {
						Log.e(TAG, "onServiceConnected2");
						MobclickAgent.onEvent(this, Cfg.UM_EXPTION,
								"PlayActivity:onServiceConnected2");
						CommonDlg.showErrorDlg(this, "内部错误!", null);
						return;
					}
				}

				if (mCurBrowseBook.equals(mCurPlayBook)) {
					mShowPlaying = true;
				}

				if ((!mService.isPlayStopped() && mShowPlaying)
						|| mLocateHistory) {
					// mService.mBrowseDir = mService.mPlayDir;
					mCurBrowseBook = mCurPlayBook;
					curBrowsePage = mCurPlayBook.mCurPlayPage;
				} else {
					curBrowsePage = Cfg.loadBrowsePage(getApplicationContext(),
							mCurBrowseBook.mTitle, Cfg.FIRST_PAGE);
				}
			}
		} else {
			mCurBrowseBook = mService.mCurBrowseBook;
			mCurPlayBook = mService.mCurPlayBook;
			if (mCurBrowseBook == null) {
				if (mFromNotify) {
					Log.e(TAG, "onServiceConnected1");
					MobclickAgent.onEvent(this, Cfg.UM_EXPTION,
							"PlayActivity:onServiceConnected1");
					CommonDlg.showErrorDlg(this,
							"进程意外中止，如果您使用了进程杀手工具，建议将善听加入到排除列表！",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									finish();
								}
							});
					return;
				} else {
					Log.e(TAG, "onServiceConnected2");
					MobclickAgent.onEvent(this, Cfg.UM_EXPTION,
							"PlayActivity:onServiceConnected2");
					CommonDlg.showErrorDlg(this, "内部错误!", null);
					return;
				}
			}

			if (mCurBrowseBook.equals(mCurPlayBook)) {
				mShowPlaying = true;
			}

			if ((!mService.isPlayStopped() && mShowPlaying) || mLocateHistory) {
				// mService.mBrowseDir = mService.mPlayDir;
				mCurBrowseBook = mCurPlayBook;
				curBrowsePage = mCurPlayBook.mCurPlayPage;
			} /*
			 * else if (mLocateHistory) { // mHistory = mService.mHistory; if
			 * (mCurPlayBook == null) { MobclickAgent .onEvent(this,
			 * Cfg.UM_EXPTION, "mHistory null"); CommonDlg.showErrorDlg(this,
			 * "历史记录出错！", null); return; } mCurBrowsePage =
			 * mCurPlayBook.mCurPlayPage; }
			 */else {
				curBrowsePage = Cfg.loadBrowsePage(getApplicationContext(),
						mCurBrowseBook.mTitle, Cfg.FIRST_PAGE);
			}
		}

		mFromNotify = false;

		if (curBrowsePage > mCurBrowseBook.mPageCount
				|| curBrowsePage < Cfg.FIRST_PAGE) {
			curBrowsePage = Cfg.FIRST_PAGE;
		}

		setTitle(mCurBrowseBook.mTitle);
		if (mCurPlayBook != null && mSongName != null && mService.isPlaying()) {
			mSongName.setText(mCurPlayBook.mSongName);
		}
		mListView = (ListView) findViewById(R.id.songslist);
		mListView.setOnItemClickListener(this);
		setJumpPage();
		// mCurBrowseList = mCurBrowseBook.getCurPageSongs();
		// loadPage();
		
//		if (mIsShortCut) {
//			checkSeries();
//		}
		
		syncLoadPage(mCurBrowseBook, curBrowsePage);
	}

	public void setJumpPage() {
		LayoutInflater inflater = (LayoutInflater) PlayActivity.this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mPlayListFooterView = (LinearLayout) inflater.inflate(
				R.layout.jump_layout, null);
		mSkip = (Button) mPlayListFooterView.findViewById(R.id.skip_page);
		mSkip.setOnClickListener(this);
		mPrePageBtn = (Button) mPlayListFooterView.findViewById(R.id.prev_page);
		mPrePageBtn.setOnClickListener(this);
		mNextPageBtn = (Button) mPlayListFooterView
				.findViewById(R.id.next_page);
		mNextPageBtn.setOnClickListener(this);
		// if (mCurBrowseBook.mPageCount > Cfg.FIRST_PAGE ) {
		mListView.addFooterView(mPlayListFooterView, null, false);
		// }
	}

	DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			finish();
		}
	};


	/**
	 * 封装以前的loadPage方法，变为异步加载，因为某一页的列表需要向服务器获取
	 */
	public void syncLoadPage(Book book, int page) {

		if (book != null) {

			this.showContentProgress(true, "耐心是一切聪明才智的基础");
			book.getSongs(page,
					mCurGetMusicPathListener = book.new GetMusicPathListener() {
						@Override
						public void onGetMusicPath(final int resultCode,
								final int curPage, final ArrayList<Song> songs) {
							// TODO Auto-generated method stub
							PlayActivity.this.showContentProgress(false);
							if (resultCode == 0) {

								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										mCurBrowseList = songs;
										mCurBrowsePage = curPage;
										loadPage();
									}
								});

							} else {
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										CommonDlg
												.showConfirmDlg(
														PlayActivity.this,
														-1,
														"加载失败，再试一次？",
														new DialogInterface.OnClickListener() {

															@Override
															public void onClick(
																	DialogInterface dialog,
																	int which) {
																// TODO
																// Auto-generated
																// method stub
																syncLoadPage(
																		mCurBrowseBook,
																		curPage);
															}
														});
									}
								});

							}
						}
					});
		}
	}

	void loadPage() {

		HeaderViewListAdapter adapter = ((HeaderViewListAdapter) mListView
				.getAdapter());
		SongsListAdapter songsListAdapter = new SongsListAdapter(this);

		if (adapter == null) {
			adapter = new HeaderViewListAdapter(null, null, songsListAdapter);
			mListView.setAdapter(songsListAdapter);
		} else {
			BaseAdapter sAdapter = (BaseAdapter) adapter.getWrappedAdapter();
			sAdapter.notifyDataSetChanged();
		}

		if (mCurBrowseBook.mPageCount > Cfg.FIRST_PAGE) {
			mPlayListFooterView.setVisibility(View.VISIBLE);
			if (mListView.getFooterViewsCount() == 0) {
				mListView.addFooterView(mPlayListFooterView);
			}
		} else {
			mPlayListFooterView.setVisibility(View.GONE);
			if (mListView.getFooterViewsCount() > 0) {
				mListView.removeFooterView(mPlayListFooterView);
			}
		}


		// 更新页码
		if (!TextUtils.isEmpty(mCurBrowseBook.mTitle)) {
			setTitle(mCurBrowseBook.mTitle);
		}

		if (mCurBrowsePage == Cfg.FIRST_PAGE) {
			mPrePageBtn.setEnabled(false);
			mPrePageBtn.setTextColor(0xffa6a6a0);

		} else if (mCurBrowsePage > Cfg.FIRST_PAGE) {
			mPrePageBtn.setTextColor(0XFFFFFFFF);
			mPrePageBtn.setEnabled(true);
		}
		if (mCurBrowseBook.mPageCount == Cfg.FIRST_PAGE) {
			mSkip.setEnabled(false);
			mSkip.setTextColor(0xffa6a6a0);
		} else if (mCurBrowseBook.mPageCount > Cfg.FIRST_PAGE) {
			mSkip.setTextColor(0XFFFFFFFF);
			mSkip.setEnabled(true);
		}
		if (mCurBrowsePage == mCurBrowseBook.mPageCount) {
			mNextPageBtn.setEnabled(false);
			mNextPageBtn.setTextColor(0xffa6a6a0);
		} else if (mCurBrowsePage < mCurBrowseBook.mPageCount) {
			mNextPageBtn.setTextColor(0XFFFFFFFF);
			mNextPageBtn.setEnabled(true);
		}

		if (mLoadFirstTime) {
			mLoadFirstTime = false;

			if (mService.isPlaying()) {
				setPauseButtonImage();
			} else if (mService.isPlayStopped()) {
				enablePlayBtn(false);
			}

			updateTrackInfo();
			long next = refreshNow();
			queueNextRefresh(next);

			if (mLocateHistory) {
				// mLocateLast = false;
				int lastSongIdx = -1;
				Song lastSong = null;
				// mHistory = mService.mHistory;
				if (mCurPlayBook != null && mCurPlayBook.mSongName != null) {
					for (Song s : mCurBrowseList) {
						if (mCurPlayBook.mSongName.equals(s.getName())) {
							lastSongIdx = mCurBrowseList.indexOf(s);
							// mCurPlayBook.mCurPlayPage = mCurBrowsePage;
							// mCurBook.mSongName = mHistory.mSongName;
							lastSong = s;
						}
					}

					if (lastSong != null && lastSongIdx != -1) {
						long pos = mCurPlayBook.mPosition;
						if (pos == MainService.POS_COMPLETION) {
							if (lastSongIdx + 1 < mCurBrowseList.size()) {
								lastSongIdx++;
								mListView.performItemClick(new View(this),
										lastSongIdx, lastSongIdx);
								// 1.播放完成
								// 2.列表内只有一首歌曲，则从头播放
							} /*
							 * else if (mCurBrowseList.size() == 1){
							 * mListView.performItemClick(new View(this),
							 * lastSongIdx, lastSongIdx); }
							 */
						} else if (Utils.canSeekPos(lastSong,
								mCurPlayBook.mTitle)) {
							mListView.performItemClick(new View(this),
									lastSongIdx, lastSongIdx);
						}
						mListView.setSelection(lastSongIdx);
					}
				}

			} else if (mShowPlaying || mRestored) {
				mListView.setSelection(mService.getCurSongIdx());
			}
			mRestored = false;
		} else {
			// log.e("setSelection(0)");
			mListView.setSelection(0);
		}

	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Log.e(TAG, "onCreate");
		// onPreCreate();
		if (savedInstanceState != null) {
			// Cfg.init(this);
			mCurBrowsePage = savedInstanceState
					.getInt(CUR_PAGE, Cfg.FIRST_PAGE);
			mCurBrowseBook = (Book) savedInstanceState
					.getSerializable(DN.CUR_BROWSE_BOOK);
			mCurPlayBook = (Book) savedInstanceState
					.getSerializable(DN.CUR_PLAY_BOOK);
			mCurSongName = savedInstanceState.getString(DN.CUR_SONG_NAME);
			// if (mLocateLast) {
			// mHistory = (History1)
			// savedInstanceState.getSerializable(HISTORY);
			// }
			if (mCurBrowseBook != null) {
				mRestored = true;
			}
		} else {
			Intent i = getIntent();

			mShowPlaying = i.getBooleanExtra(DN.SHOW_PLAYING_SONG, false);
			mFromNotify = i.getBooleanExtra(DN.FROM_NOTIFY, false);
			mLocateHistory = i.getBooleanExtra(DN.LOCATE_HISTORY, false);
		}
		Intent i = new Intent(this, DownloadService.class);
		startService(i);
		bindService(i, mDownloadConnection, BIND_AUTO_CREATE);
		database = DataBaseHelper.getInstance();
		m_list = DownloadList.getInstance();
		m_list.setListListener(PlayActivity.this);
		mInternetStateMgr = new InternetStateMgr(this);
		super.onCreate(savedInstanceState); // 先restore数据，再连接service，所以放到最后
	}

	@Override
	public void onNewIntent(Intent intent) {
		mShowPlaying = intent.getBooleanExtra(DN.SHOW_PLAYING_SONG, false);
		if (mService == null) {
			MobclickAgent.onEvent(this, Cfg.UM_EXPTION, "mShowPlaying:"
					+ mShowPlaying);
			return;
		}
		Book curBrowse = mService.mCurBrowseBook;
		if (curBrowse != null && curBrowse.equals(mService.mCurPlayBook)) {
			mShowPlaying = true;
		}
		if (mShowPlaying && mService != null/* && mService.mPlayDir != null */) {
			// mService.mBrowseDir = mService.mPlayDir;
			mCurPlayBook = mService.mCurPlayBook;
			mCurBrowseBook = mCurPlayBook;
			// mCurBrowsePage = mCurPlayBook.mCurPlayPage;
			syncLoadPage(mCurBrowseBook, mCurBrowseBook.mCurPlayPage);
			mListView.setSelection(mService.getCurSongIdx());
		}
	}

	public void initViews() {

		setContentView(R.layout.player);
		Utils.setColorTheme(findViewById(R.id.night_mask));
		mTopTitle = (TextView) findViewById(R.id.playingtitle);
		mBtnSetting = (Button) findViewById(R.id.btn_setting);
		mBtnExit = (Button) findViewById(R.id.btn_exit);

		// BtnListener mBtnListener = new BtnListener();
		mBtnExit.setOnClickListener(this);
		mBtnSetting.setOnClickListener(this);
		// mTitle = (TextView)findViewById(R.id.wy_title);

		Utils.addAdView(this);


		mSeekBar = (SeekBar) findViewById(android.R.id.progress);

		mSeekBar.setOnSeekBarChangeListener(this);

		mSeekBar.setMax(MAX_SEEK_VALUE);

		mStopButton = (ImageButton) findViewById(R.id.stop);

		mStopButton.setOnClickListener(mExitListener);

		mPauseButton = (ImageButton) findViewById(R.id.pause);
		mPauseButton.requestFocus();
		mPauseButton.setOnClickListener(mPauseListener);
		mSongShareBtn = (Button) findViewById(R.id.share);
		mNextSongBtn = (ImageButton) findViewById(R.id.next_Song);
		mSongShareBtn.setOnClickListener(this);
		mNextSongBtn.setOnClickListener(this);

		mCurrentTime = (TextView) findViewById(R.id.currenttime);
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
		mTotalTime = (TextView) findViewById(R.id.totaltime);
		mBtnErrorReport = (Button) findViewById(R.id.button_report);
		mBtnErrorReport.setOnClickListener(this);

		mSongName = (TextView) findViewById(R.id.song_name);

		if (!mService.isPlaying()) {
			mBtnErrorReport.setEnabled(false);
		}
		if (!mService.isPlaying() || !mService.hasNextSong()) {
			mNextSongBtn.setEnabled(false);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_DOWNLOAD_ALL, MENU_DOWNLOAD_ALL, R.string.all_download)
				.setIcon(R.drawable.menu_download_all);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {


			case MENU_DOWNLOAD_ALL:

				if (Utils.isToDownloadDayLimit(this)) {
					DownloadManager.showDownloadLimitDialog(PlayActivity.this, DownloadManager.DOWNLOAD_CONFIRM_DAY_TIME);

				} /*else if (Utils.isToDownloadLimitInPeakTimes(this)) {
				DownloadManager.showDownloadLimitDialog(PlayActivity.this, 
						DownloadManager.DOWNLOAD_CONFIRM_PEAK_TIME);
			} */ else if (mInternetStateMgr.hasWifi()) {
					new Thread(new startDownAllSongsThread(
							mInternetStateMgr.hasWifi()), "startDwonAll").start();
//				MobclickAgent.onEvent(PlayActivity.this, Cfg.UM_DOWNALL);
				} else {
					CommonDlg.showConfirmDlg(PlayActivity.this,
							R.string.dialog_title_download_confirm,
							getString(R.string.not_wifi_page_download_protect) + ",确定下载？",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
													int which) {

									new Thread(new startDownAllSongsThread(false), "startDownloadAll")
											.start();
//								MobclickAgent.onEvent(PlayActivity.this,
//										Cfg.UM_DOWNALL);
								}

							}, null, null);
				}

				// }
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		m_list.clearListListener(this);
		if (database != null) {
			database.close();
		}
		if (mCurBrowseBook != null) {
			Cfg.saveBrowsePage(getApplicationContext(), mCurBrowseBook.mTitle,
					mCurBrowsePage);
		}


		if (m_isBound) {
			unbindService(mDownloadConnection);
		}

		if (mService != null && mService.isPlayStopped() && mService.mMainActivity == null) {
			Intent intent = new Intent(this, MainService.class);
			stopService(intent);
		}

		// 如果非下载中，则停止服务
		Intent i = new Intent(this, DownloadService.class);
		i.setAction(Cfg.ACTION_CHECK_STOP_SERVICE);
		startService(i);

		// Log.d(TAG, "onDestroy");

		// unbindService(mConnection);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.e(TAG, "onSaveInstanceState!");
		// if (mService != null) {
		outState.putSerializable(DN.CUR_BROWSE_BOOK, mCurBrowseBook);
		outState.putSerializable(DN.CUR_PLAY_BOOK, mCurPlayBook);
		outState.putSerializable(CUR_PAGE, mCurBrowsePage);

		super.onSaveInstanceState(outState);
	}

	private void startPlayback(final boolean isLocal, final int idx) {
		log.d("startPlayback: isLocal = " + isLocal);

		
		startPlayback(isLocal, idx, 0L);
		
	}

	private void startPlayback(final boolean isLocal, final int idx, long pos) {

		String songName = mService.getCurSongName(idx);

		mCurPlayBook.mSongName = songName;
		mCurPlayBook.mPosition = pos; // 覆盖POS_COMPLETION

//		mCurrentTime.setVisibility(View.VISIBLE);
//		mCurrentTime.setText(R.string.wait);
//		mTotalTime.setVisibility(View.GONE);
//		mProgressBar.setVisibility(View.VISIBLE);

		mService.playMusic(isLocal, idx, pos, this);
		if (mCurPlayBook.mPageCount == mCurPlayBook.mCurPlayPage
				&& idx == mService.getCurListSize() - 1) {
			mNextSongBtn.setEnabled(false);
		} else {
			mNextSongBtn.setEnabled(true);
		}
		// if (songName != null ) {
		// setTitle(songName);
		// }


//		setPauseButtonImage();

//		int price = Utils.getDownloadPriceByBook(mCurPlayBook);
//		if(price > 0){
//			if(price == Cfg.DOWNLOAD_COMMON_PRICE){
////				MobclickAgent.onEvent(PlayActivity.this, Cfg.UM_PLAY_COMMON_CHARGED, songName);
//			} else {
//				MobclickAgent.onEvent(PlayActivity.this, Cfg.UM_PLAY_CHARGED, songName);
//			}
//
//		}

		// setCustomTitle(" - " + mService.getCurSong().getName());

		// }
	}

	@Override
	public synchronized boolean playNextSong() {
		if (mService != null && mService.mCurPlayBook != null && !mService.mCurPlayBook.mOnline) {
			return playLocationNextSong();
		} else {
			// isPlayNextSongByManual = true;
			boolean result = playOnlineNextSong();
			// isPlayNextSongByManual = false;
			return result;
		}

	}

	private void playNextSongByIndex(final int index) {
		Song song = mService.mCurPlayList.get(index);
		String str = Cfg.DOWNLOAD_DIR + "/" + mCurPlayBook.mTitle + "/"
				+ song.getName();

		if (audioExists(str)) {
			startPlayback(true, index);
			// } else if (!isPlayNextSongByManual) {
			// if (!mInternetStateMgr.hasInternet()) {
			// Toast.makeText(PlayActivity.this,
			// R.string.online_play_no_internet, Toast.LENGTH_SHORT).show();
			// } else if (Cfg.mNotWifiProtect && mInternetStateMgr.hasMobile())
			// {
			// Toast.makeText(PlayActivity.this,
			// R.string.online_play_wifi_disconnected,
			// Toast.LENGTH_SHORT).show();
			// } else {
			// startPlayback(false, index);
			// }
			// } else if (isPlayNextSongByManual) {
		} else {
			if (!mInternetStateMgr.hasInternet()) {
				Toast.makeText(PlayActivity.this, R.string.no_internet,
						Toast.LENGTH_SHORT).show();
			} else if (mInternetStateMgr.hasMobile() && Cfg.mNotWifiProtect) {
				str = "\n\n（流量保护已开启）网络连接非Wi-Fi，会消耗手机卡流量"
						+ Song.getSizeInMB(song.mSize)
						+ "，确定播放？                                                             ";
				CommonDlg.showConfirmDlg(PlayActivity.this,
						R.string.dialog_title_play_confirm, song.getName()
								+ str, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								startPlayback(false, index);
							}
						});
			} else {
				startPlayback(false, index);

			}
		}
	}

	/**
	 * 在线播放下一曲
	 * 
	 * @return
	 */
	public synchronized boolean playOnlineNextSong() {

		int index = mService.getCurSongIdx();
		if (index <= -1) {
			return false;
		}

		if (mCurPlayBook.mPageCount > mCurPlayBook.mCurPlayPage
				&& index == mService.getCurListSize() - 1) {
			// 翻页
			index = 0;
			this.showContentProgress(true, "耐心是一切聪明才智的基础");
			mCurPlayBook
					.getSongs(
							mCurPlayBook.mCurPlayPage + 1,
							mCurGetMusicPathListener = mCurPlayBook.new GetMusicPathListener() {

								@Override
								public void onGetMusicPath(int resultCode,
										final int curPage,
										final ArrayList<Song> songs) {

									PlayActivity.this
											.showContentProgress(false);
									if (resultCode == 0) {
										runOnUiThread(new Runnable() {

											@Override
											public void run() {
												// TODO Auto-generated method
												// stub
												mService.mCurPlayList = songs;
												mCurPlayBook.mCurPlayPage = curPage;

												if (mCurPlayBook
														.equals(mCurBrowseBook)) {
													mCurBrowsePage = mCurPlayBook.mCurPlayPage;
													mCurBrowseList = mService.mCurPlayList;
													loadPage();
													mListView.setSelection(0);
												}
												playNextSongByIndex(0);
											}
										});

									} else {
										mService.onFinished();
										CommonDlg.showErrorDlg(
												PlayActivity.this, "播放列表加载错误",
												null);
									}

								}
							});

		} else if (index >= mService.getCurListSize() - 1) {
			return false;
		} else {
			index++;
			playNextSongByIndex(index);
		}

		return true;
	}

	/**
	 * 上一曲
	 * 
	 * @return
	 */
	@Override
	public boolean playPreviousSong() {
		int index = mService.getCurSongIdx();
		// getCurSongIdx出错返回值为-1
		if (index <= 0) {
			return false;
		}
		index--;

		// ArrayList<Song> mCurBrowseList = getCurBrowseList();
		if (mCurBrowseList == null) {
			return false;
		}
		Song song = mCurBrowseList.get(index);
		String str = Cfg.DOWNLOAD_DIR + "/" + mCurBrowseBook.mTitle + "/"
				+ song.getName();
		if (audioExists(str)) {
			startPlayback(true, index);
		} else {
			if (!mInternetStateMgr.hasInternet()) {
				Toast.makeText(PlayActivity.this,
						R.string.online_play_no_internet, Toast.LENGTH_SHORT)
						.show();
			} else if (Cfg.mNotWifiProtect && mInternetStateMgr.hasMobile()) {
				Toast.makeText(PlayActivity.this,
						R.string.online_play_wifi_disconnected,
						Toast.LENGTH_SHORT).show();
			} else {
				startPlayback(false, index);
			}
		}

		return true;
	}

	protected class SongsListAdapter extends BaseAdapter {
		protected Context context;
		private Song anYueSong;
		LayoutInflater mInflate;

		public SongsListAdapter(Context context) {
			this.context = context;
			mInflate = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mCurBrowseList.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mCurBrowseList.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		public void setColor(TextView tv, boolean playing) {
			if (playing) {
				tv.setTextColor(Color.RED);
//				tv.getPaint().setFakeBoldText(true);
			}  else {
				tv.setTextColor(Color.BLACK);
//				tv.getPaint().setFakeBoldText(false);
			}
		}
		


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// ArrayList<Song> mCurBrowseList = getCurBrowseList();
			anYueSong = mCurBrowseList.get(position);
			LinearLayout ll = (LinearLayout) convertView;
			if (ll == null) {
				ll = (LinearLayout) mInflate.inflate(R.layout.song_item, null,
						false);
			}
			ImageView image = (ImageView) ll.findViewById(R.id.file_del);
			TextView songName = (TextView) ll.findViewById(R.id.file_name);
//			TextView songPrice = (TextView) ll.findViewById(R.id.price);
			View downTouchAreaView = ll.findViewById(R.id.download_touch_area);
			TextView playPriceView = (TextView)ll.findViewById(R.id.play_price);
//			View playIconArea = ll.findViewById(R.id.play_icon_area);
			
//			int price = Utils.getDownloadPriceByBook(mCurBrowseBook);

			ProgressBar pb = (ProgressBar) ll
					.findViewById(R.id.file_downloading);
			songName.setText(anYueSong.getSinger() /* + " - " */
					+ anYueSong.getName()
					+ (anYueSong.mDuration == null ? "" : anYueSong.mDuration));
			// setColor(filename,anYueSong.isColorPlay());
			
			String str = Cfg.DOWNLOAD_DIR + "/" + mCurBrowseBook.mTitle + "/"
					+ anYueSong.getName();
			
			
			boolean playing = false;
			boolean audioExist = audioExists(str);
			if (mCurPlayBook != null
					&& mCurPlayBook.mCurPlayPage == mCurBrowsePage
					&& anYueSong.getName().equals(mCurPlayBook.mSongName)) {
				playing = true;
			}
//			setPlayPrice(position, null, playPriceView, audioExist);
			setColor(songName, playing);
			//设置点击区域，
			downTouchAreaView.setOnClickListener(new onSongClickDown(position));

			
			if (audioExist) {
				pb.setVisibility(View.INVISIBLE);
				image.setVisibility(View.VISIBLE);
				image.setImageResource(R.drawable.list_item_del);
				image.setOnClickListener(new onSongClickDel(position));
//				if(songPrice.getVisibility() != View.GONE){
//					songPrice.setVisibility(View.GONE);
//				}
			} else {
				DownloadItem ditm = database
						.getDownloadItemByUnitUrl(mCurBrowseList.get(position)
								.getnetAddress());
				if (ditm != null && ditm.getStatus() == DownloadItem.RUNNING) {
					pb.setVisibility(View.VISIBLE);
					image.setVisibility(View.INVISIBLE);
//					if(songPrice.getVisibility() != View.GONE){
//						songPrice.setVisibility(View.GONE);
//					}
				} else {
					pb.setVisibility(View.INVISIBLE);
					image.setVisibility(View.VISIBLE);
//					if(price > 0){
//						if(songPrice.getVisibility() != View.VISIBLE){
//							songPrice.setVisibility(View.VISIBLE);
//						}
//						songPrice.setText(price + getString(R.string.shanbei));
//					} else if(price == Cfg.DOWNLOAD_VIP_PRICE){
//						if(songPrice.getVisibility() != View.VISIBLE){
//							songPrice.setVisibility(View.VISIBLE);
//						}
//						songPrice.setText("VIP");
//					} else {
//						if(songPrice.getVisibility() != View.GONE){
//							songPrice.setVisibility(View.GONE);
//						}
//					}
					image.setImageResource(R.drawable.list_item_download);

					image.setOnClickListener(new onSongClickDown(position));

				}
			}
			return ll;
		}
	}


	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, final int idx,
			long arg3) {
		final long posToSeek;
		// // @author mtfabc@hotmail.com
		// mTotalTime.setVisibility(TextView.GONE);
		// loadBar = (ProgressBar) findViewById(R.id.loading_bar);
		// loadBar.setVisibility(ProgressBar.VISIBLE);
		if (mLocateHistory) {
			long hisPos = mCurPlayBook.mPosition;
			posToSeek = hisPos < MainService.POS_COMPLETION ? hisPos : 0L;
		} else {
			posToSeek = 0L;
		}

		// 不能太晚调用
		if (!mService.isPlayStopped()
				&& !mCurBrowseBook.equals(mCurPlayBook)) {
			// mCurPlayBook.mPosition = mService.position();
			mService.saveHistory();
		}

		// ArrayList<Song> mCurBrowseList = getCurBrowseList();
		mService.mCurPlayList = mCurBrowseList;

		// mDuration = 0;
		// System.out.println("OnSongClick"+idx);
		song = mCurBrowseList.get(idx);
		String str = Cfg.DOWNLOAD_DIR + "/" + mCurBrowseBook.mTitle + "/"
				+ song.getName();
		
//			final int price = Utils.getPlayPriceByBook(mCurBrowseBook, idx + Cfg.SONGS_PER_PAGE * mCurBrowsePage - 30);

		if (audioExists(str)) {
			if (!mService.isPlayStopped()) {
				CommonDlg.showConfirmDlg(PlayActivity.this, -1,
						mCurBrowseList.get(idx).getName() + "\n\n"
								+ getString(R.string.play_confirm),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0,
									int arg1) {
								doPlayClickItem(true, idx, posToSeek);
							}
						});
			} else {
				doPlayClickItem(true, idx, posToSeek);
			}
		} else {
			if (!mInternetStateMgr.hasInternet()) {
				Toast.makeText(PlayActivity.this, R.string.no_internet,
						Toast.LENGTH_SHORT).show();
			} /*else if(price > 0){
				showContentProgress(true, getResources().getString(R.string.waiting_for_query));
				final int songIndex = Cfg.SONGS_PER_PAGE * mCurBrowseBook.mCurPlayPage - 30 + idx;
				final String bookId = mCurBrowseBook.mID;
				onPlayChargedChaps(price, bookId, songIndex, posToSeek);
			
			} */else if (mInternetStateMgr.hasMobile()
					&& (mLocateHistory || Cfg.mNotWifiProtect)) {
				Song curSong = mCurBrowseList.get(idx);
				str = Cfg.mNotWifiProtect ? ("\n\n（流量保护已开启）网络连接非Wi-Fi，会消耗手机卡流量"
						+ Song.getSizeInMB(curSong.mSize) + "，确定播放？                                                             ")
						: ("\n\n网络连接非Wi-Fi，会消耗手机卡流量"
								+ Song.getSizeInMB(curSong.mSize) + "，确定播放？                                                                      ");
				CommonDlg.showConfirmDlg(PlayActivity.this,
						R.string.dialog_title_play_confirm,
						curSong.getName() + str,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0,
									int arg1) {
								doPlayClickItem(false, idx, posToSeek);
							}
						});
			} else {
				if (!mService.isPlayStopped()) {
					CommonDlg.showConfirmDlg(PlayActivity.this, -1,
							mCurBrowseList.get(idx).getName() + "\n\n"
									+ getString(R.string.play_confirm),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									doPlayClickItem(false, idx, posToSeek);
								}
							});
				} else {
					doPlayClickItem(false, idx, posToSeek);
				}
			}
		}

		updateListView();
		mLocateHistory = false;
	}
	

	private void doPlayClickItem(final boolean isLocal, final int idx, long pos) {
		mCurPlayBook = mCurBrowseBook;
		mCurPlayBook.mCurPlayPage = mCurBrowsePage;
		mService.mCurPlayBook = mCurPlayBook;

		startPlayback(isLocal, idx, pos);
//		MobclickAgent.onEvent(PlayActivity.this, Cfg.UM_SONG_CLICK);
	}

	private class onSongClickDown implements OnClickListener {
		// AdapterView<?> viewgroup;
		private int position = 0;

		// ArrayList<Song> mCurBrowseList = getCurBrowseList();

		public onSongClickDown(int pos) {
			position = pos;
		}

		/**
		 * 确认下载曲目
		 * 
		 * @param view
		 **/
		private void doDownload(View view) {
			// int first = mListView.getFirstVisiblePosition();
			// if(((position - first) < 0)||(position-first >=
			// mListView.getAdapter().getCount())){
			// MyToast.showShort(PlayActivity.this,
			// R.string.download_fail_in_list);
			// MobclickAgent.onEvent(PlayActivity.this, Cfg.UM_EXPTION ,
			// "positon:" + position + ",first:" + first);
			// return;
			// }
			//
			// ViewGroup pview = (ViewGroup) mListView
			// .getChildAt(position - first);
			ViewGroup pview;
			try {
				pview = (ViewGroup) view.getParent();
			} catch (Exception e) {
				pview = null;
			}

			if (pview == null) {
				return;
			}

			if (mCurBrowseList == null || position >= mCurBrowseList.size()
					|| mCurBrowseList.get(position) == null) {
				return;
			}

			if(downloadItemByPosition(position)){
				ProgressBar pb = (ProgressBar) pview
						.findViewById(R.id.file_downloading);
				ImageView image = (ImageView) pview.findViewById(R.id.file_del);
				pb.setVisibility(View.VISIBLE);
				image.setVisibility(View.INVISIBLE);
			}

		};

		@Override
		public void onClick(final View v) {
			if (PlayActivity.binder == null)
				return;

//			int price = Utils.getDownloadPriceByBook(mCurBrowseBook);
			mCurSelectedPos = position;
//			mIsPayForPageDown = false;
			
			if (!mInternetStateMgr.hasInternet()) {
				Toast.makeText(PlayActivity.this, R.string.no_internet,
						Toast.LENGTH_SHORT).show();
			}

			else if(Utils.isToDownloadDayLimit(PlayActivity.this)){
				DownloadManager.showDownloadLimitDialog(PlayActivity.this,
						DownloadManager.DOWNLOAD_CONFIRM_DAY_TIME);
			} /*else if (Utils.isToDownloadLimitInPeakTimes(PlayActivity.this)) {
				DownloadManager.showDownloadLimitDialog(PlayActivity.this, 
						DownloadManager.DOWNLOAD_CONFIRM_PEAK_TIME);
			} */else if (Cfg.mNotWifiProtect && mInternetStateMgr.hasMobile()) {
				Song curSong = mCurBrowseList.get(position);
				CommonDlg
						.showConfirmDlg(
								PlayActivity.this,
								R.string.dialog_title_download_confirm,
								curSong.getName()
										+ "\n\n（流量保护已开启）网络连接非Wi-Fi，会消耗手机卡流量"
										+ Song.getSizeInMB(curSong.mSize)
										+ "，确定下载？                                                                      ",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface arg0,
											int arg1) {
										doDownload(v);
									}
								});
			} else {
				doDownload(v);
			}
		}

	}
	
	private boolean downloadItemByPosition(int pos){
		boolean beginDownload = false;
		
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {
			Message message = mHandler.obtainMessage();
			message.what = EVENT_SDCARD_NOT_MOUNTED;
			mHandler.sendMessage(message);
		} else if (status.equals(Environment.MEDIA_MOUNTED)
				&& !(Utils.getFreeSpace(Cfg.SDCARD_PATH) > Cfg.sdSpaceLimitation * 1024)) {
			Message message = mHandler.obtainMessage();
			message.what = EVENT_SDCARD_NO_SPACE;
			mHandler.sendMessage(message);
		} else {
			
			String encodeUrl = mCurBrowseList.get(pos).getnetAddress();
			Intent it = new Intent(PlayActivity.this, DownloadService.class);

			it.putExtra(DownloadManager.LAUNCH_UDTYPE,
					DownloadManager.LAUNCH_DOWNLOADING);
			it.putExtra(DownloadService.DOWNLOAD_DIR_NAME,
					mCurBrowseBook.mTitle);
			it.putExtra(DownloadService.DOWNLOAD_NAME,
					mCurBrowseList.get(pos).getName());
			it.putExtra(DownloadService.DOWNLOAD_FILE_TYPE,
					DownloadItem.AUDIO_FILE);
			it.putExtra(DownloadService.DOWNLOAD_URL, encodeUrl);
			it.putExtra(DownloadService.DOWNLOAD_FILESIZE,
					mCurBrowseList.get(pos).mSize);

			startService(it);
			
			beginDownload = true;
		}
		
		return beginDownload;
	}

	private class onSongClickDel implements OnClickListener {
		// AdapterView<?> viewgroup;
		private int position = 0;

		// ArrayList<Song> mCurBrowseList = getCurBrowseList();

		public onSongClickDel(int pos) {
			position = pos;
		}

		@Override
		public void onClick(View v) {

			new AlertDialog.Builder(PlayActivity.this)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(R.string.dialog_title_confirm)
					.setMessage(
							mCurBrowseList.get(position).getName()
									+ "\n\n确定删除？                                                                                          ")
					.setPositiveButton(R.string.confirm,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {

									Song song = mCurBrowseList.get(position);
									String str = Cfg.DOWNLOAD_DIR + "/"
											+ mCurBrowseBook.mTitle + "/"
											+ song.getName();

									File file1 = new File(str + ".mp3");
									File file2 = new File(str + ".wma");
									// Log.e(TAG, file1.getAbsolutePath());
									if (file1.exists()) {
										file1.delete();
									} else if (file2.exists()) {
										file2.delete();
									}

									DownloadItem ditm = database
											.getDownloadItemByUnitUrl(mCurBrowseList
													.get(position)
													.getnetAddress());
									if (ditm != null) {
										m_list.removeItem(ditm.getItemId());
									}

									int first = mListView
											.getFirstVisiblePosition();
									int last = mListView
											.getLastVisiblePosition();

									if (position >= first && position <= last) {
										ViewGroup pview = (ViewGroup) mListView
												.getChildAt(position - first);
										if (pview != null) {
											ProgressBar pb = (ProgressBar) pview
													.findViewById(R.id.file_downloading);
											ImageView image = (ImageView) pview
													.findViewById(R.id.file_del);
											pb.setVisibility(View.INVISIBLE);
											image.setVisibility(View.VISIBLE);
											image.setImageResource(R.drawable.list_item_download);
											image.setOnClickListener(new onSongClickDown(
													position));
										}
									}
									
									updateListView();
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).create().show();
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		// mCurTitle = title;
		mTopTitle.setText(title + " [页码:" + mCurBrowsePage + "/"
				+ mCurBrowseBook.mPageCount + "]");
	}



	public void enablePlayBtn(boolean enabled) {
		mPauseButton.setEnabled(enabled);
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
			// System.exit(0);
		}
	};



	@Override
	public void setSecondProgress(int secondProgress) {
		mSeekBar.setSecondaryProgress(secondProgress);
	}

	@Override
	public void DownloadListChanged(int eventid, DownloadItem item) {
		Message msg = mHandler.obtainMessage(DATACHANGE);
		switch (eventid) {

		case DownloadList.DOWNLOAD_LIST_EVNET_UPDATE_ITEM:


		case DownloadList.DOWNLOAD_LIST_EVNET_ADD_ITEM:
		case DownloadList.DOWNLOAD_LIST_EVNET_REMOVE_ITEM:
			// log.d("DownloadListChanged PAUSED or FINISHED");
			mHandler.removeMessages(DATACHANGE);
			mHandler.sendMessage(msg);
			break;
		}
	}


//	public void updateView() {
//		//
//		updateListView();
//	}


	private void startDownAllSongs(boolean isDownloadWithWifi) {

		// ArrayList<Song> mCurBrowseList = getCurBrowseList();

		if (null == mCurBrowseList) {
			return;
		}

		mIsStartingDownAllSongs = true;

		for (int i = 0; i < mCurBrowseList.size(); i++) {
			Song song = mCurBrowseList.get(i);
			String str = Cfg.DOWNLOAD_DIR + "/" + mCurBrowseBook.mTitle + "/"
					+ song.getName();
			if (audioExists(str)) {
				continue;
			}
			String encodeUrl = song.getnetAddress();

			// String encodeUrl =
			// mCurBrowseList.get(i).getnetAddress();
			if (encodeUrl != null) {

				// check sd card status and internet status
				String status = Environment.getExternalStorageState();
				if (!status.equals(Environment.MEDIA_MOUNTED)) {
					Message message = mHandler.obtainMessage();
					message.what = EVENT_SDCARD_NOT_MOUNTED;
					mHandler.sendMessage(message);
					break;
				} else if (status.equals(Environment.MEDIA_MOUNTED)
						&& !(Utils.getFreeSpace(Cfg.SDCARD_PATH) > Cfg.sdSpaceLimitation * 1024)) {
					Message message = mHandler.obtainMessage();
					message.what = EVENT_SDCARD_NO_SPACE;
					mHandler.sendMessage(message);
					break;
				} else if (!mInternetStateMgr.hasInternet()) {
					Message message = mHandler.obtainMessage();
					message.what = EVENT_NOINTERNET;
					mHandler.sendMessage(message);
					break;
				}

				DownloadItem item = null;
				item = database.getDownloadItemByUnitUrl(encodeUrl);
				long itemId;
				if (item == null) {
					item = new DownloadItem(-1, encodeUrl,
							DownloadItem.RUNNING, mCurBrowseBook.mTitle,
							mCurBrowseList.get(i).getName(),
							DownloadItem.AUDIO_FILE, 0, new Date().getTime(),
							song.mSize, 0);
					itemId = m_list.addItem(item);
				} else {
					itemId = item.getItemId();
					if (item.getStatus() == DownloadItem.FINISHED) {
						item.setDownload_pos(0);
						item.setFileSize(0);
						item.setDownloadSize(0);
					}
				}

				int downloadResult = binder.getService().startDownloadTask(
						itemId, isDownloadWithWifi);
				if (downloadResult == DownloadService.ERR_CODE_SDCARD_NO_SPACE
						|| downloadResult == DownloadService.ERR_CODE_SDCARD_NOT_MOUNTED) {
					break;
				}
				// }
			}
		}

		mIsStartingDownAllSongs = false;
	}

	private class startDownAllSongsThread implements Runnable {
		private final boolean isDownloadWithWifi;

		public startDownAllSongsThread(boolean isDownloadWithWifi) {
			this.isDownloadWithWifi = isDownloadWithWifi;
			PlayActivity.this.mProgressDialog = ProgressDialog.show(
					PlayActivity.this, "",
					getString(R.string.adding_per_page_songs), true);
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
//			Looper.prepare();
			startDownAllSongs(isDownloadWithWifi);

			Message message = mHandler.obtainMessage();
			message.what = EVENT_START_DOWN_ALL_SONGS_END;
			mHandler.sendMessage(message);
//			Looper.loop();
		}
	}



	private Book.GetMusicPathListener mCurGetMusicPathListener;

	@Override
	protected void onContentProgressCanceled() {
		if (mCurGetMusicPathListener != null) {
			mCurGetMusicPathListener.cancel();
			mCurGetMusicPathListener = null;
		}
	}

	private boolean audioExists(String absPathWithoutExt) {
		File file1 = new File(absPathWithoutExt + ".mp3");
		File file2 = new File(absPathWithoutExt + ".wma");
		// Log.e(TAG, file1.getAbsolutePath());
		return file1.exists() || file2.exists();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btn_exit:
			Intent i = new Intent(PlayActivity.this, Main.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			finish();
			break;
		case R.id.btn_setting:
			Intent intent = new Intent(PlayActivity.this, Preferences.class);
			startActivity(intent);
			break;
		case R.id.prev_page:
			// mCurBrowsePage--;
			isPrePage = true;
			syncLoadPage(mCurBrowseBook, mCurBrowsePage - 1);
			break;
		case R.id.next_page:
			// mCurBrowsePage++;
			isNextPage = true;
			syncLoadPage(mCurBrowseBook, mCurBrowsePage + 1);
			break;
		case R.id.skip_page:
			isSkipPage = true;
			showJumpPageDialog();
			break;
		case R.id.share:
			if (!mService.isPlayStopped() && mService.mCurPlayBook != null && !mService.mCurPlayBook.mOnline 
					&& mService.mCurPlayBook.mLcDirPath != null ) {
				shareLocationSong();
			} else {
				shareSong();
			}
			break;
		case R.id.next_Song:
			mNextSongByUserAction = true;
			playNextSong();
			mNextSongByUserAction = false;
//			MobclickAgent.onEvent(PlayActivity.this, Cfg.UM_SONG_CLICK);
			break;
		case R.id.button_report:
			reportResourceProblem();
			break;
		}
	}

	public void shareSong() {
		MobclickAgent.onEvent(PlayActivity.this, Cfg.UM_SHARE, Cfg.UM_SHARE2);

		String url = null, title = null;

		Book shareBook = mCurBrowseBook;
		if (mService != null && !mService.isPlayStopped()
				&& mCurPlayBook != null) {
			shareBook = mCurPlayBook;
		}

		if (shareBook != null) {
			ArrayList<Song> songs = shareBook.getCurPageSongs();
			if (songs != null) {
				Song song = songs.get(0);
				url = song.getnetAddress();
				title = song.getName();
			}
		}

		url = Cfg.WEB_HOME + "/player/xml.php?url=" + url + "&title=" + title;

//		String content = "我正在用 @善听听书( http://shantingshu.com )收听《"
//				+ shareBook.mTitle + "》---" + shareBook.mIntro;

		Intent i = new Intent();
		String description = "";
		if(shareBook.mNarrator != null && shareBook.mNarrator.length() > 0){
			description = "演播：" + shareBook.mNarrator;
		}
		
		if(shareBook.mWriter != null && shareBook.mWriter.length() > 0){
			description += "\n作者：" + shareBook.mWriter;
		}
		
		if(shareBook.mIntro != null && shareBook.mIntro.length() > 0){
			if(description.length() > 0){
				description += "\n";
			}
			description += shareBook.mIntro;
		}

		i.putExtra(AccountConnect.INTENT_SHARE_DESCRIPTION, description);
		
		i.putExtra(AccountConnect.INTENT_SHARE_SONG_NAME, shareBook.mTitle);

		i.putExtra(AccountConnect.INTENT_SHARE_TITLE, title);
		i.putExtra(AccountConnect.INTENT_SHARE_URL, url);
		if (shareBook.mPicAddress != null && shareBook.mPicAddress.length() > 0) {
			i.putExtra(AccountConnect.INTENT_SHARE_PICURL,
					shareBook.mPicAddress);
		}

		Utils.share(this, i);
	}

	/**
	 * 本地资源共享
	 */
	public void shareLocationSong() {
		Book shareBook = mCurPlayBook;
		if (shareBook == null || shareBook.mTitle == null || shareBook.mSongName == null) {
			return;
		}

		MobclickAgent.onEvent(PlayActivity.this, Cfg.UM_SHARE, Cfg.UM_SHARE2);
		Intent i = new Intent();
//		i.putExtra(AccountConnect.INTENT_SHARE_CONTENT, "我正在用善听收听\""
//				+ shareBook.mTitle + "\", 新潮的”阅读“方式，快来体验一下吧！@善听听书  ");
		
		i.putExtra(AccountConnect.INTENT_SHARE_SONG_NAME, shareBook.mTitle);
		
		i.putExtra(AccountConnect.INTENT_SHARE_TITLE, shareBook.mSongName);
		Utils.share(this, i);
	}

	public void showJumpPageDialog() {
		int end = 0;
		int countOfLastPage = mCurBrowseBook.mSongCount % Cfg.SONGS_PER_PAGE;
		final String[] items = new String[mCurBrowseBook.mPageCount];
		final int[] nums = new int[mCurBrowseBook.mPageCount];
		for (int i = 0; i < mCurBrowseBook.mPageCount; i++) {
			int num = i + 1;
			end = (num) * Cfg.SONGS_PER_PAGE;
			if (num == mCurBrowseBook.mPageCount && countOfLastPage > 0) {
				end = (num - 1) * Cfg.SONGS_PER_PAGE + countOfLastPage;
			}
			int start = 1 + (num - 1) * Cfg.SONGS_PER_PAGE;
			nums[i] = i + 1;
			items[i] = "第" + (i + 1) + "页 (" + start + "～" + end + "集)";
		}

		new AlertDialog.Builder(PlayActivity.this).setTitle("请选择跳转页面")
				.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int item) {
						// mCurBrowsePage=nums[item];
						syncLoadPage(mCurBrowseBook, nums[item]);
					}
				}).show();

	}


	public boolean playLocationNextSong() {
		ArrayList<File> mFiles = ApplicationGlobalVariable.getmFiles();
		Book mCurBook = mService.mCurPlayBook;
		// TODO Auto-generated method stub
		int index = -1;
		File file = null;
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

		file = mFiles.get(index);
		if (file != null) {
			if (mCurBook != null) {
				mCurBook.mPosition = 0;
				mCurBook.mSongName = file.getName();
			}

			mService.playMusic(file, 0, this);
		}

		return true;
	}


	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		Utils.setColorTheme(findViewById(R.id.night_mask));
		if(mService != null && !mService.isPlayStopped()){
			mHandler.sendEmptyMessage(REFRESH);
		}
		super.onResume();
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
	
	private void loadList() {
		mBookList.clear();
		
		String commonPath = Cfg.mInnerRootPath + Cfg.BOOKSHELF_FILE_DIR
		+ Cfg.BOOKSHELF_FILE_FOREBODY;
		for (int i = 0; i < Cfg.COUNT_BOOK_ON_SHELF; i++) {
			String path = commonPath + i;
			File f = new File(path);
			try {
				if (!f.exists()) {
					continue;
				}

				FileInputStream fis = new FileInputStream(f);
				ObjectInputStream in = new ObjectInputStream(fis);
				Book book = (Book) in.readObject();
				// Log.e(TAG, his.mLcDirName + his.mLcDirPath);
				// File dir = new File(his.mLcDirPath);
				if (!book.mOnline
						&& (TextUtils
								.isEmpty(book.mLcDirPath))) {
					BookShelf.removeBook(i);
					i--;
					continue;
				}
				
				mService.setBookHistoryCache(book.mOnline ? book.mID : book.mLcDirPath, i);
				mBookList.add(book);
				in.close();
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void showUpInfoOnUiThread(final String msg) {
		runOnUiThread(new Runnable() {			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (msg.length() > 0) {
					CommonDlg.showInfoDlg(PlayActivity.this, "连载更新", msg);
				}
			}
		});
	}


	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		mHandler.removeMessages(REFRESH);
		super.onPause();
	}
	
	@Override
	public void setmDuration(long duration) {
		// TODO Auto-generated method stub
		super.setmDuration(duration);
		mLastDuration = duration;
	}
	
}