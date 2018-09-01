package tiger.unfamous.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.data.Book;
import tiger.unfamous.utils.CommonDlg;
import tiger.unfamous.utils.Utils;

public class LocalBrowser extends BasicActivity implements AdapterView.OnItemClickListener {
	public static final String TAG = "LcPlayActivity";

	public static final int MAX_SEEK_VALUE = 1000;
	public static final int TIME_1000MS = 1000;

	// private static final int REFRESH = 1;
	// private static boolean isTwo = false;
	// private long mTime = 0;
	protected TextView mCurPath;
	private ListView mListView;
	private boolean mIsInMainTab;
	private Button mBtnDownloadManager;

	private Button mUpDir;
//	private DurationToker mDurationToker = new DurationToker();

	public void updateListView() {
		((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
	}

	// private Book mCurBook;
	private File mCurrentDir;
	private final ArrayList<File> mFiles = new ArrayList<File>();
	private final ArrayList<File> mDirFiles = new ArrayList<File>();

	// TextView mTitle;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// onPreCreate();
		if (savedInstanceState != null) {
			mRestored = true;
			mCurrentDir = (File) savedInstanceState.getSerializable(DN.CUR_PLAY_BOOK);
		}

		mIsInMainTab = getIntent().getBooleanExtra(DN.IN_MAIN_TAB, true);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.local_browser);
		Utils.addAdView(this);

		mTopTitle = (TextView) findViewById(R.id.title);
		mTopTitle.setText("本地资源");

		mCurPath = (TextView) findViewById(R.id.currentdir);

		mUpDir = (Button) findViewById(R.id.updir);
		mUpDir.setOnClickListener(new BtnOnClickListener());

		ImageGetter imgGetter = new Html.ImageGetter() {
			@Override
			public Drawable getDrawable(String source) {
				Drawable drawable = null;
				drawable = LocalBrowser.this.getResources().getDrawable(Integer.parseInt(source));
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
				return drawable;
			}
		};

		StringBuffer strBuf = new StringBuffer();
		strBuf.append("<img src=\"").append(R.drawable.updirectory).append("\"/>");
		Spanned span = Html.fromHtml(strBuf.toString(), imgGetter, null);
		mUpDir.setText(span);

		mListView = (ListView) findViewById(R.id.songslist);
		mListView.setOnItemClickListener(this);
		// mTitle = (TextView) findViewById(R.id.title);
		mBtnDownloadManager = (Button) findViewById(R.id.rightbtn);
		mShowPlaying = (Button) findViewById(R.id.show_playing);
		mBtnDownloadManager.setText("下载管理");
		mBtnDownloadManager.setOnClickListener(new BtnOnClickListener());
		mShowPlaying.setOnClickListener(new BtnOnClickListener());

		if (mRestored && mCurrentDir != null) {
			browseTo(mCurrentDir);
			mRestored = false;
		} else {
			Cfg.init(this);
			browseTo(new File(Cfg.DOWNLOAD_DIR));
		}
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	@Override
	protected void onServiceConnected() {
		// mCurBook = mService.mCurBook;

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mService != null) {
			outState.putSerializable(DN.CUR_PLAY_BOOK, mCurrentDir);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "刷新目录").setIcon(R.drawable.menu_refresh);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			refresh();
			MyToast.showShort(this, "当前目录已刷新！");
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void refresh() {
		if (mCurrentDir != null) {
			browseTo(mCurrentDir);
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
								ret = Integer.parseInt(s1) - Integer.parseInt(s2);
								if (ret != 0) {
									completed = true;
								} else {
									completed = false;
								}
							} catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
								completed = false;
								// MobclickAgent.onEvent(LocalBrowser.this,
								// Cfg.UM_EXPTION_LABEL, "sortFiles: " +
								// object1.getName() + "_" + object2.getName());
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
			return;
		}

		mCurrentDir = location;

		mCurPath.setText(mCurrentDir.getAbsolutePath());

		mDirFiles.clear();
		mFiles.clear();

		// if (location.getParentFile() != null) {
		// mDirFiles.add(mCurrentDir.getParentFile());
		// }

		File[] files = mCurrentDir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory() && !file.getName().startsWith(".")) {
					mDirFiles.add(file);
				} else if (Utils.isMp3orWma(file.getName())) {
					mFiles.add(file);
				}
			}
		}

		sortFiles(mDirFiles);
		sortFiles(mFiles);

		mFiles.addAll(0, mDirFiles);

		// if(mListView != null) mListView.setAdapter(new
		// SongsListAdapter(this));
		SongsListAdapter adapter = ((SongsListAdapter) mListView.getAdapter());
		if (adapter == null) {
			mListView.setAdapter(new SongsListAdapter(this));
		} else {
			adapter.notifyDataSetChanged();
		}

		String title = mCurrentDir.getName().compareTo("") == 0 ? mCurrentDir.getPath() : mCurrentDir.getName();
		setTitle(title);
	}

	// private int getCurSongIdx() {
	// for (File f : mFiles) {
	// if (f.getName().equals(mCurBook.mChapName)) {
	// return mFiles.indexOf(f);
	// }
	// }
	// return -1;
	// }

	protected class SongsListAdapter extends BaseAdapter {
		protected Context context;
		// private File anYueSong;
		LayoutInflater mInflate;

		public SongsListAdapter(Context context) {
			this.context = context;
			mInflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
				ll = (LinearLayout) mInflate.inflate(R.layout.local_file_item, null, false);
			}
			ImageView iconImg = (ImageView) ll.findViewById(R.id.icon);
			ImageView delImage = (ImageView) ll.findViewById(R.id.file_del);
			TextView songName = (TextView) ll.findViewById(R.id.file_name);
			// TextView songSize = (TextView) ll.findViewById(R.id.file_size);

			String filename;
			if (currentFile.isDirectory()) {
				iconImg.setImageResource(R.drawable.directory);
			} else {
				iconImg.setImageResource(R.drawable.audio);
			}
			delImage.setVisibility(View.VISIBLE);
			filename = currentFile.getName();

//			if (currentFile.isFile()) {
//				// songSize.setVisibility(View.VISIBLE);
//				mDurationToker.setFileLength(currentFile.length());
//				long seconds = mDurationToker.getTime(currentFile);
//				filename += " [" + seconds / 60 + "." + (seconds % 60) / 6 + "\']";
//			}
			songName.setText(filename);

			// setColor(filename,anYueSong.isColorPlay());
			// boolean playing = false;
			// if (currentFile.getName().equals(mCurBook.mChapName)) {
			// playing = true;
			// }
			// setColor(songName, playing);

			delImage.setImageResource(R.drawable.list_item_del);
			delImage.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					final File file = mFiles.get(index);
					String tip = file.isDirectory() ? "\n\n确定删除整个文件夹？                                                                                     " : "\n\n确定删除？                                                                                          ";
					CommonDlg.showConfirmDlg(LocalBrowser.this, -1, file.getName() + tip, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
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
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long id) {

		if (id > mFiles.size()) {
			return;
		}

		final File file = mFiles.get((int) id);

		if (file.isDirectory()) {
			browseTo(file);
		} else {
			if (!mService.isPlayStopped()) {
				CommonDlg.showConfirmDlg(LocalBrowser.this, -1, file.getName() + "\n\n" + getString(R.string.play_confirm), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// mService.setNeedReleaseWifi(false);
						mService.stopMusic();
						File parent = file.getParentFile();
						Book book = new Book(parent.getPath(), file.getName());
						Intent intent = new Intent(LocalBrowser.this, LcPlayActivity.class);
						intent.putExtra(DN.DO_PLAY, true);
						mService.mCurBrowseBook = book;
						mService.mCurPlayBook = book;
						startActivity(intent);
					}
				});
			} else {
				File parent = file.getParentFile();
				Book book = new Book(parent.getPath(), file.getName());
				Intent intent = new Intent(LocalBrowser.this, LcPlayActivity.class);
				intent.putExtra(DN.DO_PLAY, true);
				mService.mCurBrowseBook = book;
				mService.mCurPlayBook = book;
				startActivity(intent);
			}
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		String t = "本地资源 [" + title + "]";
		// if (mIsInMainTab && mService.mMainActivity != null) {
		// mService.mMainActivity.setTitle(t);
		// } else {
		super.setTitle(t);
		// }
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			return super.onKeyDown(keyCode, event);
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			// if (mCurrentDir.getAbsolutePath().startsWith(Cfg.DOWNLOAD_DIR)
			// && !mCurrentDir.getAbsolutePath().equals(Cfg.DOWNLOAD_DIR)) {
			// File parentDir = mCurrentDir.getParentFile();
			// browseTo(parentDir);
			// return true;
			// } else {
			if (!mIsInMainTab) {
				return super.onKeyDown(keyCode, event);
			} else {
				return false;
			}
			// }

		} else {
			return false;
		}
	}

	class BtnOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.rightbtn:
				Intent i = new Intent(LocalBrowser.this, DownloadManager.class);
				startActivity(i);
				break;
			case R.id.show_playing:
				if (mService != null) {
					mService.showPlaying(LocalBrowser.this);
				}
				break;
			case R.id.updir:
				File parentDir = mCurrentDir.getParentFile();
				if (parentDir != null) {
					browseTo(parentDir);
				}
				break;
			}
		}

	}
}