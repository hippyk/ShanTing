package tiger.unfamous.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.data.Book;
import tiger.unfamous.data.Song;
import tiger.unfamous.utils.CommonDlg;
import tiger.unfamous.utils.MyHttpClient;
import tiger.unfamous.utils.Utils;

public class BookShelf extends BasicActivity {
	
//	private static final String FETCH_BOOK_INFO_URL = "http://www.shanting.mobi/log.txt";

	protected int responseCode = 0;
	private ListView mListView;
//	private RadioButton mRadioBtn;
	private Button mFavorite;
	private ProgressBar mProgress;
	private static final int EVENT_LIST_CHANGED = 0;
	private static final int EVENT_LIST_PROGRESS = 1;
	private static final int EVENT_LIST_SHOW_UP_INFO = 2;
	
	
	private Handler mListEventHandler = new Handler(){
		
		@Override
		public void handleMessage(android.os.Message msg) {
			switch(msg.what){
			case EVENT_LIST_CHANGED:
				if (mListView != null) {
					DirListAdapter adapter = ((DirListAdapter) mListView.getAdapter());
					if (adapter == null) {
						mListView.setAdapter(new DirListAdapter(BookShelf.this));
					} else {
						adapter.notifyDataSetChanged();
					}
				}
				break;
			case EVENT_LIST_PROGRESS:
				mProgress.setVisibility(View.VISIBLE);
				break;
				
			case EVENT_LIST_SHOW_UP_INFO:
				String upInfo = (String) msg.obj;
				
				mProgress.setVisibility(View.INVISIBLE);
				if (upInfo.length() > 0) {
					CommonDlg.showInfoDlg(BookShelf.this, "连载更新", upInfo);
				}
				break;
			}
		};
	};

	public ArrayList<Book> mBookList = new ArrayList<Book>();


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
					removeBook(i);
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
		mListEventHandler.sendEmptyMessage(EVENT_LIST_CHANGED);
		
	}

	@Override
	protected void onServiceConnected() {

//		setTitle(getString(R.string.label_history));
		mProgress = (ProgressBar) findViewById(R.id.progress);
		mTopTitle = (TextView) findViewById(R.id.title);
		mTopTitle.setText("书架(历史)");
		
		mFavorite = (Button)findViewById(R.id.rightbtn);
		mShowPlaying = (Button)findViewById(R.id.show_playing);

		mFavorite.setText("收藏夹");
		mFavorite.setOnClickListener(new BtnClickListener());
		mShowPlaying.setOnClickListener(new BtnClickListener());
		
		mListView = (ListView) findViewById(R.id.songslist);
		mListView.setOnItemClickListener(new OnDirItemClick());
		
		//以前升级历史记录放在Main里，有可能不同步，loadList比记录升级更早完成
		new Thread(new Runnable() {			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Utils.refactorHisFiles(BookShelf.this);
				mBookList.clear();
				if(Utils.getpreLoadBooksList(mBookList)){
					mListEventHandler.sendEmptyMessage(EVENT_LIST_CHANGED);
				} else {
					//若预加载列表为空，则重新加载，防止漏掉
					loadList();
				}
				
				checkSeries();
			}
		}).start();

		Utils.addAdView(this);    //不在onCreate调用，减少超时白屏概率
	}

	// public void setTitle(CharSequence title) {
	// ((TextView)findViewById(R.id.title)).setText(title);
	// }
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//onPreCreate();
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.book_shelf);

		// IntentFilter msgFilter = new IntentFilter();
		// msgFilter.addAction(Cfg.ACTION_UPDATE_HISTORY);
		registerReceiver(mBCReceiver, new IntentFilter(
				Cfg.ACTION_UPDATE_HISTORY));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mBCReceiver);
	}

	void showChildList(final int idx) {

		// mService.mParentDirIdx = dirIdx;
		final Book book = mBookList.get(idx);
		final Intent intent = new Intent(this, book.mOnline ? PlayActivity.class
				: LcPlayActivity.class);
		intent.putExtra(DN.LOCATE_HISTORY, true);
		
		if (!book.mOnline) {
			File f = new File(book.mLcDirPath);
			if (!f.exists()) {
				CommonDlg.showConfirmDlg(BookShelf.this, -1, "找不到本地目录，可能是由于sd卡拔出，或本地目录已被删除。\n\n是否删除此条记录？", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						removeBook(idx);
						loadList();
					}
				});
				return;
			}
		} 
		
		if (!mService.isPlayStopped()) {
			if (!book.equals(mService.mCurPlayBook)) {
				mService.saveHistory();
			}
			mService.setNeedReleaseWifi(false);
			mService.stopMusic();
		}
		mService.mCurBrowseBook = book;		
		mService.mCurPlayBook = book;
				
		startActivity(intent);
	}
	


	class ClearListener implements DialogInterface.OnClickListener {

		private final int idx;

		public ClearListener(int idx) {
			this.idx = idx;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {

			removeBook(idx);
			loadList();
		}
	}

	protected class DirListAdapter extends BaseAdapter {
		protected Context context;
		LayoutInflater mInflate;

		public DirListAdapter(Context context) {
			this.context = context;
			mInflate = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mBookList.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mBookList.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return createViewFromResource(position, convertView, parent,
					R.layout.song_item);
		}

		private View createViewFromResource(int position, View convertView,
				ViewGroup parent, int resource) {
			View v;
			if (convertView == null) {
				v = mInflate.inflate(resource, parent, false);
			} else {
				v = convertView;
			}
			// setupView(position, v);
			ImageView image = (ImageView) v.findViewById(R.id.file_del);
			image.setImageResource(R.drawable.list_item_del);
			image.setOnClickListener(new onDirClickDel(position));

			TextView dirName = (TextView) v.findViewById(R.id.file_name);
//			TextView duration = (TextView) v.findViewById(R.id.duration);
//			duration.setVisibility(View.GONE);

			Book book = mBookList.get(position);
			if (book.mOnline) {
//				Dir1 dir = book.mDir;
				(dirName).setText(" - " + book.mTitle);
			} else {
				(dirName).setText(" - " + book.mTitle + "[本地]");
			}
			return v;
		}
	}

	protected class OnDirItemClick implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, final int idx,
				long arg3) {
//			Log.v(TAG, mService.isPlaying() + "");
			if (!mService.isPlayStopped()) {
				CommonDlg.showConfirmDlg(BookShelf.this, -1, getString(R.string.play_confirm), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						// finish();
						showChildList(idx);
					}
				});
			} else {
				showChildList(idx);
			}
		}
	}

	private class onDirClickDel implements OnClickListener {
		// AdapterView<?> viewgroup;
		private int position = 0;

		public onDirClickDel(int pos) {
			position = pos;
		}

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Book book = mBookList.get(position);
			CommonDlg.showConfirmDlg(BookShelf.this,
					-1,
					book.mTitle
							+ "\n\n确定移出书架？                                                                                        ",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							removeBook(position);
							loadList();
						}
					});
		}
	}

	public static void removeBook(int removeIdx) {
		// int removeIdx = position;
		String commonPath = Cfg.mInnerRootPath + Cfg.BOOKSHELF_FILE_DIR
				+ Cfg.BOOKSHELF_FILE_FOREBODY;
		new File(commonPath + removeIdx).delete();
		for (int i = removeIdx + 1; i < Cfg.COUNT_BOOK_ON_SHELF; i++) {
			String path = commonPath + i;
			File f = new File(path);
			if (f.exists()) {
				f.renameTo(new File(commonPath + (i - 1)));
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "清空书架").setIcon(R.drawable.menu_clear_bookshelf);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			CommonDlg.showConfirmDlg(BookShelf.this, -1, "移除全部记录？",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							String commonPath = Cfg.mInnerRootPath
									+ Cfg.BOOKSHELF_FILE_DIR
									+ Cfg.BOOKSHELF_FILE_FOREBODY;
							for (int i = 0; i < Cfg.COUNT_BOOK_ON_SHELF; i++) {
								String path = commonPath + i;
								File f = new File(path);
								if (f.exists()) {
									f.delete();
								}
							}
							loadList();
						}
					});

			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public BroadcastReceiver mBCReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (Cfg.ACTION_UPDATE_HISTORY.equals(intent.getAction())) {
				loadList();
			}
		}
	};

//	@Override
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP){
			return super.onKeyDown(keyCode, event);
		}
		return false;
	}

	class BtnClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.show_playing:
				if (mService != null) {
					mService.showPlaying(BookShelf.this);
				}
				break;

			case R.id.rightbtn:
				try{
					RadioButton radioBtn1 = (RadioButton) mService.mMainActivity
					.findViewById(R.id.radio_button1);
					radioBtn1.setChecked(true);
					Intent intent = new Intent(Cfg.ACTION_SHOW_FAVORITE);
					sendBroadcast(intent);
					break;
				}catch (Exception e) {
					// TODO: handle exception
				}
				
			}

		}

	};
	
	
//	private void showUpInfoOnUiThread(final String msg) {
//		runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//
//			}
//		});
//	}
	
	//检查连载
	private void checkSeries() {
		if (mBookList.size() == 0) {
			return;
		}
		mListEventHandler.sendEmptyMessage(EVENT_LIST_PROGRESS);
		
		final StringBuffer sb = new StringBuffer();
		StringBuilder inSeriesBookIDs = new StringBuilder();
		boolean isFirst = true;
		
		for (int i = 0; i < mBookList.size(); i++) { 
			final Book book = mBookList.get(i);
			if (book.mUpdateState == Cfg.UpdateState.IN_SERIES) {
				if(!isFirst){
					inSeriesBookIDs.append(",");
				}
				isFirst = false;
				inSeriesBookIDs.append(book.mID);
			}
		}
		
		//有连载小说需要更新
		if (!isFirst) {
			String urlWithID, url;
			urlWithID = Utils.setUrlParam(Cfg.BOOK_QUERY_URL, DN.BOOK_ID, inSeriesBookIDs.toString());
			url= Utils.setUrlParam(urlWithID, DN.BOOK_FETCH_PARAM, DN.BOOK_SONG_COUNT);
			InputStream in = MyHttpClient.getInstance().sendHttpGet(url, null);
			
			int newCount[] = null;
			String countStrings[] = null;
			
            try {
				JsonReader reader = new JsonReader(new InputStreamReader(in, "GBK"));
				reader.beginObject();
			    while (reader.hasNext()) {
			    	String name = reader.nextName();
			    	if (name.equals(DN.BOOK_SONG_COUNT)) {
			    		String strCount= reader.nextString();
			    		countStrings = strCount.split(",");
			    	} else {
			            reader.skipValue();
			        }
			    }
			 reader.endObject();
			 
			 if(countStrings != null && countStrings.length > 0){
				 newCount = new int[countStrings.length];
				 for(int i = 0; i < countStrings.length; i++){
					 newCount[i] = Integer.parseInt(countStrings[i]);
				 }
			 }
			 
			 for(int i = 0, j = 0; i < mBookList.size() && j < newCount.length; i++){
				 final Book book = mBookList.get(i);
				 if (book.mUpdateState == Cfg.UpdateState.IN_SERIES){
					 if (newCount[j] > book.mSongCount) {
					    	int increment = newCount[j] - book.mSongCount;
					    	
					    	
					    	//用户读的是最后一页，而这一页刚好需要更新了
					    	if(book.mCurPlayPage * Cfg.SONGS_PER_PAGE > book.mSongCount 
					    			&& book.requestMusicPaths(book.mCurPlayPage) != 0){
					    		sb.append("");
					    	} else {
					    		book.mSongCount = newCount[j];
					    		book.mPageCount = (book.mSongCount - 1) / Cfg.SONGS_PER_PAGE + 1 ;
					    		saveBook(book);
					    		sb.append(book.mTitle + "（更新至" + book.mSongCount + " ,新增" + increment + "集）" + "\n\n");
						    	
					    	}
					    	//正在播放音乐，需要更新列表
					    	if(!mService.isPlayStopped() && mService.mCurPlayBook != null 
					    			&& book.mID.equalsIgnoreCase(mService.mCurPlayBook.mID)){
					    		mService.mCurPlayBook.mSongCount = book.mSongCount;
					    		mService.mCurPlayBook.mPageCount = book.mPageCount;
					    		mService.mCurPlayBook.mSongStringsMap = book.mSongStringsMap;
					    		mService.mCurPlayBook.mSongStrings = book.mSongStrings;
					    		mService.mCurPlayBook.getSongs(mService.mCurPlayBook.mCurPlayPage, mService.mCurPlayBook.new GetMusicPathListener() {
									
									@Override
									public void onGetMusicPath(int resultCode, int curPage,
											ArrayList<Song> songs) {
										if(resultCode == 0){
											mService.mCurPlayList = songs;	
										}
										
									}
								});
					    		
					    	}
					    	
					    }
					 
					 j++;
				 }
			 }		 
			 if(sb.length() > 0){
				 mListEventHandler.obtainMessage(EVENT_LIST_SHOW_UP_INFO, sb.toString()).sendToTarget();
			 }
			 
			 
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
		}
		
				
		//结束进度
		mListEventHandler.obtainMessage(EVENT_LIST_SHOW_UP_INFO, "").sendToTarget();
	}
	
	public static void saveBook(Book b) {
		String commonPath = Cfg.mInnerRootPath + Cfg.BOOKSHELF_FILE_DIR
				+ Cfg.BOOKSHELF_FILE_FOREBODY;
		int duplicateIdx = -1;
		for (int i = 0; i < Cfg.COUNT_BOOK_ON_SHELF; i++) {
			String path = commonPath + i;
			File f = new File(path);
			try {
				if (!f.exists()) {
					continue;
				}
//				Log.v(TAG, path);
				FileInputStream fis = new FileInputStream(f);
				ObjectInputStream in = new ObjectInputStream(fis);
				Book book = (Book) in.readObject();
				if (book.equals(b)) {
					duplicateIdx = i;
					break;
				}

				in.close();
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		int removeIdx = duplicateIdx;
		if (removeIdx == -1) {
			removeIdx = Cfg.COUNT_BOOK_ON_SHELF - 1;
		}
		for (int i = duplicateIdx - 1; i >= 0; i--) {
			String path = commonPath + i;
			File f = new File(path);
			if (f.exists()) {
				f.renameTo(new File(commonPath + (i + 1)));
			}
		}
		
//		log.e(b.mSongStrings.length);

		FileOutputStream fileStream = null;
		try {
			fileStream = new FileOutputStream(commonPath + 0);
			ObjectOutputStream out = new ObjectOutputStream(fileStream);
			out.writeObject(b);
			out.close();
			fileStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}