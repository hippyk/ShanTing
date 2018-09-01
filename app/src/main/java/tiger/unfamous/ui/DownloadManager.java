package tiger.unfamous.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.umeng.analytics.MobclickAgent;
import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.common.InternetStateMgr;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.data.Song;
import tiger.unfamous.download.DownloadItem;
import tiger.unfamous.download.DownloadList;
import tiger.unfamous.download.DownloadListListener;
import tiger.unfamous.download.DownloadService;
import tiger.unfamous.utils.CommonDlg;
import tiger.unfamous.utils.MyLog;
import tiger.unfamous.utils.Utils;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class DownloadManager extends ListActivity implements
		DownloadListListener {

	private static final int MSG_TYPE_REMOVETASK = 100;
	public static String ACTION_REMOVE_TASK = "action.remove_task";
	private final List<Map<String, Object>> mListData = new ArrayList<Map<String, Object>>();
	private MySimpleAdapter mAdapter;
	private ListView listView;

	public final static String LAUNCH_TYPE = "launchType";
	public final static String LAUNCH_DOWNLOADING = "downloading";
	public final static String LAUNCH_COMPLETED = "completed";
	public static String LAUNCH_UDTYPE = LAUNCH_COMPLETED;
	
//	public static final int DOWNLOAD_CONFIRM_PEAK_TIME = 1;
	public static final int DOWNLOAD_CONFIRM_DAY_TIME = 2;

	TextView empty;

	public static DownloadService.LocalBinder binder = null;
	private boolean m_isBound = false;
	private static final MyLog log = new MyLog();
	private DownloadList m_list = null;

	private String name;
	private String url;
	private String type;
	private long statusCode;
	private InternetStateMgr mInternetStateMgr;
	public static boolean mIsStartingDownAllSongs = false;
	public static boolean mIsStoppingDownAllSongs = false;
	public static boolean mIsDeletingUncompleteSongs = false;

	private static final int OPT_SUSPEND_ALL_MENUID = 0;
	private static final int OPT_START_ALL_MENUID = 1;
	private static final int OPT_DELETE_ALL_UNCOMPLETED_MENUID = 2;
	private static final int OPT_VIEW_COMPLETED_MENUID = 3;
	private static final String TAG = "DownloadManager";
	
	private static final int EVENT_STOPPING_DOWNLOAD_ALL_SONGS_CANNOT_START = 1;
	private static final int EVENT_DELETING_UNCOMPLETE_SONGS_CANNOT_START = 2;
	private static final int EVENT_STARTING_DOWNLOAD_ALL_SONGS_CANNOT_STOP = 3;
	private static final int EVENT_DELETING_UNCOMPLETE_SONGS_CANNOT_STOP = 4;
	private static final int EVENT_STARTING_DOWNLOAD_ALL_SONGS_CANNOT_DELETE = 5;
	private static final int EVENT_STOPPING_DOWNLOAD_ALL_SONGS_CANNOT_DELETE =6;
//	private static final int EVENT_NO_SDCARD = 4;
	private static final int EVENT_NO_INTERNET = 7;
	private static final int EVENT_SDCARD_NOT_MOUNTED = 8;
	private static final int EVENT_SDCARD_NO_SPACE = 9;
	private static final int EVENT_START_DOWN_ALL_SONGS_END = 10;
	private static final int EVENT_STOP_DOWN_ALL_SONGS_END = 11;
	private static final int EVENT_DELETE_ALL_UNCOMPLETED_SONGS_END = 12;
	
	private ProgressDialog mProgressDialog = null;
	

	// Handler myhandler = new Handler(){
	// @Override
	// public void handleMessage(Message msg) {
	//			
	// mAdapter.notifyDataSetChanged();
	// empty.setText(R.string.no_downloading);
	// log.v("mAdapter size is:============"+mAdapter.getCount());
	// super.handleMessage(msg);
	// }
	// };

	Handler myViewUpdateHandler = new Handler() {
		// @Override
		@Override
		@SuppressWarnings("unchecked")
		public void handleMessage(Message msg) {

			if (msg.what == MSG_TYPE_REMOVETASK) {
				final Long itemid = msg.getData().getLong(
						DownloadService.DOWNLOAD_ID);
				statusCode = msg.getData().getLong(
						DownloadService.DOWNLOAD_DOWNLOAD_STATUS);
				if (itemid >= 0 && statusCode == 403 || statusCode == 404) {
					try {
						DownloadItem item = m_list.getItemById(itemid);
						if (item == null)
							return;
						String showName = item.getUnit_name();

						showName = showName.substring(0, showName.indexOf("."));
						String dialogMessage = String.format(
								getString(R.string.download_urlnoeffect),
								new Object[] { showName });
						new AlertDialog.Builder(DownloadManager.this).setIcon(
								android.R.drawable.ic_lock_idle_alarm)
								.setMessage(dialogMessage).setPositiveButton(
										getText(R.string.confirm),
										new DialogInterface.OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												if (DownloadManager.binder != null)
													DownloadManager.binder
															.getService()
															.removeDownloadTask(
																	itemid,
																	true);
											}

										}).setNegativeButton(
										getText(R.string.cancel),
										new DialogInterface.OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {

											}

										}).setCancelable(false).create().show();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				return;
			} else {
				int i = 0;
				ListView listView = getListView();
				int first = listView.getFirstVisiblePosition();
				int last = listView.getLastVisiblePosition();

				long id = msg.getData().getLong(DownloadService.DOWNLOAD_ID);
				boolean updateFlag = msg.getData().getBoolean("listupdate");

				// log.d("getListView().getAdapter().getCount()==========="+getListView().getAdapter().getCount());
				if (getListView().getAdapter().getCount() == 0 || updateFlag) {
					log.verbose("mAdapter count == 0: " + first + ":" + last
							+ " || updateFlag=true");
					DownloadManager.this.mAdapter.notifyDataSetChanged();
					return;
				}

				ListAdapter adapter2 = listView.getAdapter();
				for (i = first; i <= last; i++) {
					// log.d(first + "-" + i + "-" + last);
					if (i >= adapter2.getCount()) {
						break;
					}

					if (adapter2.getItem(i) == null) {
						// log.d("item is null!The number is " + i);
						continue;
					}
					HashMap<String, Object> map = (HashMap<String, Object>) (adapter2
							.getItem(i));
					int status = (Integer) map
							.get(DownloadService.DOWNLOAD_DOWNLOAD_STATUS);
					if (status == DownloadItem.FINISHED)
						return;
					long lid = (Long) map.get(DownloadService.DOWNLOAD_ID);
					if (lid == id) {
						// log.d("child at:" + (i - first));
						View line = listView.getChildAt(i - first);
						if (line != null) {
							long downloadpos = (Long) map
									.get(DownloadService.DOWNLOAD_POS);
							long fileSize = (Long) map
									.get(DownloadService.DOWNLOAD_FILESIZE);
							// String name = (String)
							// map.get(DownloadService.DOWNLOAD_NAME);
							// log.i("downloadpos:" + downloadpos);
							// log.i("fileSize:" + fileSize);
							float per = (float) downloadpos / fileSize;
							ProgressBar progressbar = (ProgressBar) line
									.findViewById(R.id.progress_horizontal);
							if (progressbar != null)
								progressbar.setProgress(Math.round(per * 100));

							TextView v_progress = (TextView) line
									.findViewById(R.id.file_progress);
							if (v_progress != null)
								v_progress.setText(Math.round(per * 100) + "%");

							// TextView t_name =
							// (TextView)findViewById(R.id.book_name);
							// t_name.setText(name);

							TextView file_size = (TextView) line
									.findViewById(R.id.file_size);
							long k_size = fileSize / 1024
									+ (fileSize % 1024 == 0 ? 0 : 1);
							file_size.setText(k_size + "k");
						}
						break;
					}

				}
			}

		}
	};

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case EVENT_STOPPING_DOWNLOAD_ALL_SONGS_CANNOT_START:
				Toast.makeText(DownloadManager.this,
						R.string.stopping_download_all_songs_cannot_start,
						Toast.LENGTH_SHORT).show();
				break;
			case EVENT_DELETING_UNCOMPLETE_SONGS_CANNOT_START:
				Toast.makeText(DownloadManager.this,
						R.string.deleting_uncomplete_songs_cannot_start,
						Toast.LENGTH_SHORT).show();
				break;
			case EVENT_STARTING_DOWNLOAD_ALL_SONGS_CANNOT_STOP:
				Toast.makeText(DownloadManager.this,
						R.string.starting_download_all_songs_cannot_stop,
						Toast.LENGTH_SHORT).show();
				break;
			case EVENT_DELETING_UNCOMPLETE_SONGS_CANNOT_STOP:
				Toast.makeText(DownloadManager.this,
						R.string.deleting_uncomplete_songs_cannot_stop,
						Toast.LENGTH_SHORT).show();
				break;
			case EVENT_STARTING_DOWNLOAD_ALL_SONGS_CANNOT_DELETE:
				Toast.makeText(DownloadManager.this,
						R.string.starting_download_all_songs_cannot_delete,
						Toast.LENGTH_SHORT).show();
				break;
			case EVENT_STOPPING_DOWNLOAD_ALL_SONGS_CANNOT_DELETE:
				Toast.makeText(DownloadManager.this,
						R.string.stopping_download_all_songs_cannot_delete,
						Toast.LENGTH_SHORT).show();
				break;
//			case EVENT_NO_SDCARD:
//				Toast.makeText(DownloadManager.this,
//						R.string.download_no_sdcard, Toast.LENGTH_SHORT).show();
//				break;
			case EVENT_NO_INTERNET:
				Toast.makeText(DownloadManager.this, R.string.no_internet,
						Toast.LENGTH_SHORT).show();
				break;
			case EVENT_SDCARD_NOT_MOUNTED:
				Toast.makeText(DownloadManager.this,
						R.string.download_sdcard_not_mounted, Toast.LENGTH_SHORT).show();
				break;
			case EVENT_SDCARD_NO_SPACE:
				Toast.makeText(DownloadManager.this,
						R.string.download_sdcard_no_space, Toast.LENGTH_SHORT).show();
				break;
			case EVENT_START_DOWN_ALL_SONGS_END:
			case EVENT_STOP_DOWN_ALL_SONGS_END:
			case EVENT_DELETE_ALL_UNCOMPLETED_SONGS_END:
				try {
					if (mProgressDialog != null && !DownloadManager.this.isFinishing()) {
						mProgressDialog.cancel();
						mProgressDialog = null;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.download_list);
		Utils.addAdView(this);

		((Button) findViewById(R.id.rightbtn)).setText("返回");
		((TextView) findViewById(R.id.title)).setText("下载管理");
		((Button) findViewById(R.id.rightbtn)).setOnClickListener(new View.OnClickListener() {		
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
		
		empty = (TextView) findViewById(android.R.id.empty);
		empty.setText(R.string.please_wait);
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			name = bundle.getString(DownloadService.DOWNLOAD_NAME);
			type = bundle.getString(DownloadService.DOWNLOAD_FILE_TYPE);
			url = bundle.getString(DownloadService.DOWNLOAD_URL);
			
			/*if(bundle.getBoolean(DN.EXTRA_SHOW_PEAK_LIMIT, false)){
				showDownloadLimitDialog(DownloadManager.this, DOWNLOAD_CONFIRM_PEAK_TIME);
			} else */
			if(bundle.getBoolean(DN.EXTRA_SHOW_DAY_LIMIT, false)){
				showDownloadLimitDialog(DownloadManager.this, DOWNLOAD_CONFIRM_DAY_TIME);
			}
			
		}
		// name = "bixuejian";
		// type = "t";
		// url = "http://fengxing.umlife.com/wuxiaxianxia/jingyong/bixuejian";
		Intent it = new Intent(this, DownloadService.class);
		it.putExtra(LAUNCH_UDTYPE, LAUNCH_DOWNLOADING);
		it.putExtra(DownloadService.DOWNLOAD_NAME, name);
		it.putExtra(DownloadService.DOWNLOAD_FILE_TYPE, type);
		it.putExtra(DownloadService.DOWNLOAD_URL, url);
		bindService(new Intent(this, DownloadService.class), m_connection,
				BIND_AUTO_CREATE);
		
		startService(it);

		m_list = DownloadList.getInstance();
		m_list.setListListener(DownloadManager.this);

		registerRemoveTask(this);

		listView = getListView();
		mAdapter = new MySimpleAdapter(this, mListData, R.layout.download_item,
				new String[] {}, new int[] {});
		listView.setCacheColorHint(Color.argb(0, 0, 0, 0));
		getListView().setDivider(
				getResources().getDrawable(R.drawable.listview_divider));
		this.setListAdapter(mAdapter);
		listView.setOnItemClickListener(clickListener);
		
		mInternetStateMgr = new InternetStateMgr(this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		if(intent != null){
			Bundle bundle = intent.getExtras();
			if(bundle != null){
				/*if(bundle.getBoolean(DN.EXTRA_SHOW_PEAK_LIMIT, false)){
					showDownloadLimitDialog(DownloadManager.this, DOWNLOAD_CONFIRM_PEAK_TIME);
				} else */
				if(bundle.getBoolean(DN.EXTRA_SHOW_DAY_LIMIT, false)){
					showDownloadLimitDialog(DownloadManager.this, DOWNLOAD_CONFIRM_DAY_TIME);
				}
			}
		}
	}
	@Override
	protected void onResume() {
		super.onResume();
		// new Thread(){
		// public void run(){
		loadData();
		// myhandler.sendEmptyMessage(0);
		// }
		// }.start();

		log.v("onResume");
		Utils.setColorTheme(findViewById(R.id.night_mask));
		MobclickAgent.onResume(this); // umeng
	}

	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);
		menu.add(0, OPT_SUSPEND_ALL_MENUID, OPT_SUSPEND_ALL_MENUID,
				R.string.all_pause).setIcon(R.drawable.menu_pause_all);
		menu.add(0, OPT_START_ALL_MENUID, OPT_START_ALL_MENUID,
				R.string.all_start).setIcon(R.drawable.menu_start_or_playing);
		menu.add(0, OPT_DELETE_ALL_UNCOMPLETED_MENUID,
				OPT_DELETE_ALL_UNCOMPLETED_MENUID, R.string.strDeleteAll)
				.setIcon(R.drawable.menu_del_all);
		menu.add(0, OPT_VIEW_COMPLETED_MENUID, OPT_VIEW_COMPLETED_MENUID,
				R.string.label_downloaded).setIcon(
				R.drawable.menu_local);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final ArrayList<DownloadItem> items;
		switch (item.getItemId()) {
		case OPT_SUSPEND_ALL_MENUID:
//			DownloadManager.binder.getService().shutdownExecutor();
//			ArrayList<DownloadItem> sitems = m_list
//					.getAllNonCompleteItems(DownloadItem.UDTYPE_DOWNLOADING);
			{
				items = m_list.getAllNonCompleteItems(DownloadItem.UDTYPE_DOWNLOADING);
				new Thread(new stopDownAllSongsThread(items),"stopAll").start();	
			}
			
			break;

		case OPT_START_ALL_MENUID:
			items = m_list.getAllNonCompleteItems(DownloadItem.UDTYPE_DOWNLOADING);
			if (items.size() == 0) {
				break;
			}
		
			if(!mInternetStateMgr.hasInternet()) {
				Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
			} else if(Utils.isToDownloadDayLimit(DownloadManager.this)){
				showDownloadLimitDialog(DownloadManager.this, DOWNLOAD_CONFIRM_DAY_TIME);
			} /*else if(Utils.isToDownloadLimitInPeakTimes(this)){
				showDownloadLimitDialog(DownloadManager.this, DOWNLOAD_CONFIRM_PEAK_TIME);
			} */else if (mInternetStateMgr.hasMobile()) {
				CommonDlg.showConfirmDlg(this,
						-1,
						getResources().getString(R.string.not_wifi_all_download_protect),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								//ArrayList<DownloadItem> items = m_list.getAllNonCompleteItems(DownloadItem.UDTYPE_DOWNLOADING);
								new Thread(new startDownAllSongsThread(items, false), "startDownAll").start();
							}
						});	
			} else {
				new Thread(new startDownAllSongsThread(items, true), "startDownAllSongs").start();
			}
			break;

		case OPT_VIEW_COMPLETED_MENUID:
			Intent sdintent = new Intent();
			sdintent.setClass(getApplicationContext(), LocalBrowser.class);
			sdintent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			sdintent.putExtra(DN.IN_MAIN_TAB, false);
			startActivity(sdintent);
			break;
		case OPT_DELETE_ALL_UNCOMPLETED_MENUID:
			items = m_list.getAllNonCompleteItems(DownloadItem.UDTYPE_DOWNLOADING);
			if (items.size() == 0) {
				break;
			}

			CommonDlg.showConfirmDlg(this, -1, "确定全部删除？",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							new Thread(new deleteAllUncompletedSongsThread(items), "deleteAllUncompletedSongs").start();
						}
					});

			break;

		}
		return super.onOptionsItemSelected(item);
	}

	private void stopDownAllSongs(ArrayList<DownloadItem> items) {		
		if (mIsStartingDownAllSongs
				|| PlayActivity.mIsStartingDownAllSongs
				|| DownloadService.mIsStartingDownAllSongs) {
			Message message = mHandler.obtainMessage();
			message.what = EVENT_STARTING_DOWNLOAD_ALL_SONGS_CANNOT_STOP;
			mHandler.sendMessage(message);
			return;
		}
		
		if (mIsDeletingUncompleteSongs) {
			Message message = mHandler.obtainMessage();
			message.what = EVENT_DELETING_UNCOMPLETE_SONGS_CANNOT_STOP;
			mHandler.sendMessage(message);
			return;
		}

		mIsStoppingDownAllSongs = true;
		
		DownloadManager.binder.getService().shutdownExecutor();
		for (int i = 0; i < items.size(); i++) {
			DownloadItem ditem = items.get(i);
			if (ditem.getStatus() == DownloadItem.RUNNING) {
				DownloadManager.binder.getService().stopDownloadTask(
						ditem.getItemId(), true);
			}
		}
		
		mIsStoppingDownAllSongs = false;
	}
	
	private class stopDownAllSongsThread implements Runnable {
		private ArrayList<DownloadItem> items = null;
		
		public stopDownAllSongsThread (ArrayList<DownloadItem> items) {
			this.items = items;
			mProgressDialog = ProgressDialog.show(DownloadManager.this, "",
					getString(R.string.stopping_all_songs), true);
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
//			Looper.prepare();
			stopDownAllSongs(items);

			Message message = mHandler.obtainMessage();
			message.what = EVENT_STOP_DOWN_ALL_SONGS_END;
			mHandler.sendMessage(message);
//			Looper.loop();
		}
	}
	
	private void deleteAllUncompletedSongs(ArrayList<DownloadItem> items) {
		if (mIsStartingDownAllSongs
				|| PlayActivity.mIsStartingDownAllSongs
				|| DownloadService.mIsStartingDownAllSongs) {
			Message message = mHandler.obtainMessage();
			message.what = EVENT_STARTING_DOWNLOAD_ALL_SONGS_CANNOT_DELETE;
			mHandler.sendMessage(message);
			return;
		}
		
		if (mIsStoppingDownAllSongs
				|| DownloadService.mIsStoppingDownAllSongs) {
			Message message = mHandler.obtainMessage();
			message.what = EVENT_STOPPING_DOWNLOAD_ALL_SONGS_CANNOT_DELETE;
			mHandler.sendMessage(message);
			return;			
		}
		
		mIsDeletingUncompleteSongs = true;
		
		DownloadManager.binder.getService().shutdownExecutor();
		for (DownloadItem ditem : items) {
			DownloadManager.binder.getService().removeDownloadTask(
					ditem.getItemId(), true);
		}
		
		mIsDeletingUncompleteSongs = false;
	}
	
	private class deleteAllUncompletedSongsThread implements Runnable {
		private ArrayList<DownloadItem> items = null;
		
		public deleteAllUncompletedSongsThread (ArrayList<DownloadItem> items) {
			this.items = items;
			mProgressDialog = ProgressDialog.show(DownloadManager.this, "",
					getString(R.string.deleting_all_songs), true);
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
//			Looper.prepare();
			deleteAllUncompletedSongs(items);
			
			Message message = mHandler.obtainMessage();
			message.what = EVENT_DELETE_ALL_UNCOMPLETED_SONGS_END;
			mHandler.sendMessage(message);
//			Looper.loop();
		}
	}
	
	private void startDownAllSongs(ArrayList<DownloadItem> items, boolean isDownloadWithWifi) {
		if (mIsStoppingDownAllSongs
				|| DownloadService.mIsStoppingDownAllSongs) {
			Message message = mHandler.obtainMessage();
			message.what = EVENT_STOPPING_DOWNLOAD_ALL_SONGS_CANNOT_START;
			mHandler.sendMessage(message);
			return;
		}
		
		if (mIsDeletingUncompleteSongs) {
			Message message = mHandler.obtainMessage();
			message.what = EVENT_DELETING_UNCOMPLETE_SONGS_CANNOT_START;
			mHandler.sendMessage(message);
			return;			
		}
		
		mIsStartingDownAllSongs = true;
		for (int i = 0; i < items.size(); i++) {
			DownloadItem ditem = items.get(i);
			if (ditem.getStatus() == DownloadItem.PAUSED){
				String status = Environment.getExternalStorageState();
				if (ditem.getFileType().equals(DownloadItem.AUDIO_FILE)
						&& !status.equals(Environment.MEDIA_MOUNTED)) {
					Message message = mHandler.obtainMessage();
					message.what = EVENT_SDCARD_NOT_MOUNTED;
					mHandler.sendMessage(message);
					break;
				} else if (ditem.getFileType().equals(DownloadItem.AUDIO_FILE)
						&& status.equals(Environment.MEDIA_MOUNTED)
						&& !(Utils.getFreeSpace(Cfg.SDCARD_PATH) > Cfg.sdSpaceLimitation * 1024)) {
					Message message = mHandler.obtainMessage();
					message.what = EVENT_SDCARD_NO_SPACE;
					mHandler.sendMessage(message);
					break;
				} else if (!mInternetStateMgr.hasInternet()) {
					Message message = mHandler.obtainMessage();
					message.what = EVENT_NO_INTERNET;
					mHandler.sendMessage(message);
					break;
				} else {
					int downloadResult = binder.getService().startDownloadTask(ditem.getItemId(), isDownloadWithWifi);
					if (downloadResult == DownloadService.ERR_CODE_SDCARD_NO_SPACE
							|| downloadResult == DownloadService.ERR_CODE_SDCARD_NOT_MOUNTED) {
						break;
					}
				}
			}
		}
		
		for (int i = 0; i < items.size(); i++) {
			DownloadItem ditem = items.get(i);
			if (ditem.getFileType().equals(DownloadItem.INSTALL_PACKAGE_FILE)
					&& ditem.getStatus() == DownloadItem.PAUSED) {
				if (!mInternetStateMgr.hasInternet()) {
					Message message = mHandler.obtainMessage();
					message.what = EVENT_NO_INTERNET;
					mHandler.sendMessage(message);
					break;
				} else {
					binder.getService().startDownloadTask(ditem.getItemId(), isDownloadWithWifi);
				}
			}
		}
		
		mIsStartingDownAllSongs = false;
	}
	
	private class startDownAllSongsThread implements Runnable  {
		private ArrayList<DownloadItem> items = null;
		private final boolean isDownloadWithWifi;
		
		public startDownAllSongsThread (ArrayList<DownloadItem> items, boolean isDownloadWithWifi) {
			this.items = items;
			this.isDownloadWithWifi = isDownloadWithWifi;
			mProgressDialog = ProgressDialog.show(DownloadManager.this, "",
					getString(R.string.starting_all_songs), true);
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
//			Looper.prepare();
			startDownAllSongs(items, isDownloadWithWifi);
			
			Message message = mHandler.obtainMessage();
			message.what = EVENT_START_DOWN_ALL_SONGS_END;
			mHandler.sendMessage(message);
//			Looper.loop();
		}
	}
	
	private void loadData() {
		listView.setVisibility(View.INVISIBLE);
		mListData.clear();
		ArrayList<DownloadItem> items = m_list
				.getAllNonCompleteItems(DownloadItem.UDTYPE_DOWNLOADING);
		int item_size = items.size();
		for (int i = 0; i < item_size; i++) {
			HashMap<String, Object> ii = new HashMap<String, Object>();
			DownloadItem downloadItem = items.get(i);
			ii.put(DownloadService.DOWNLOAD_ID, downloadItem.getItemId());
			ii.put(DownloadService.DOWNLOAD_NAME, downloadItem.getUnit_name());
			ii.put(DownloadService.DOWNLOAD_DOWNLOAD_STATUS, downloadItem
					.getStatus());
			ii.put(DownloadService.DOWNLOAD_FILESIZE, downloadItem
					.getFileSize());
			ii.put(DownloadService.DOWNLOAD_POS, downloadItem
							.getDownload_pos());
			mListData.add(ii);
		}
		listView.setVisibility(View.VISIBLE);
		mAdapter.notifyDataSetChanged();
		empty.setText(R.string.no_downloading);
		log.v("mAdapter size is:============" + mAdapter.getCount());
	};

	private class MySimpleAdapter extends SimpleAdapter {
		public MySimpleAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
		}

		@Override
		public int getCount() {
			return mListData.size();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.download_item, null);
			}
			if (mListData == null || mListData.size() == 0
					|| position >= mListData.size()) {
				return convertView;
			}

			TextView v_name = (TextView) convertView
					.findViewById(R.id.file_name);
			// TextView v_authur =
			// (TextView)convertView.findViewById(R.id.book_authur);
			TextView v_size = (TextView) convertView
					.findViewById(R.id.file_size);
			TextView v_progress = (TextView) convertView
					.findViewById(R.id.file_progress);
			ProgressBar v_pb = (ProgressBar) convertView
					.findViewById(R.id.progress_horizontal);

			String file_name = (String) mListData.get(position).get(
					DownloadService.DOWNLOAD_NAME);
			long file_size = (Long) mListData.get(position).get(
					DownloadService.DOWNLOAD_FILESIZE);
			long download_pos = (Long) mListData.get(position).get(
					DownloadService.DOWNLOAD_POS);
			// String filetype = (String)
			// mListData.get(position).get(DownloadService.DOWNLOAD_FILE_TYPE);

			// log.d(position + " file_name:" + file_name);
			float per = (float) download_pos / file_size;
			v_name.setText(file_name);
			v_size.setText(v_size + "");
			long lk_size = file_size / 1024 + (file_size % 1024 == 0 ? 0 : 1);
			if (v_size != null) {
				v_size.setText(lk_size + "k");
			}
			if (file_size == 0) {
				v_progress.setText("0" + "%");
				v_pb.setProgress(0);
			} else {
				v_progress.setText(Math.round(per * 100) + "%");
				v_pb.setProgress(Math.round(per * 100));
			}
			return convertView;
		}
	}

	OnItemClickListener clickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {

			commandDialog(position);
		}

	};

	@SuppressWarnings("unchecked")
	private void commandDialog(final int position) {
		Log.e(TAG, "commandDialog:enter");
		final Dialog d = new Dialog(DownloadManager.this, R.style.PupDialog);
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.setContentView(R.layout.list_dialog);
		final ListView list = (ListView) d.findViewById(android.R.id.list);
		list.setDivider(getResources().getDrawable(
						R.drawable.listview_divider));
		HashMap<String, Object> map = (HashMap<String, Object>) (getListView()
				.getAdapter().getItem(position));
		final long item_id = (Long) map.get(DownloadService.DOWNLOAD_ID);
		final DownloadItem di = m_list.getItemById(item_id);
		if (di == null) {
			DownloadManager.this.mAdapter.notifyDataSetChanged();
			Log.e(TAG, "commandDialog:DownloadItem null");
			return;
		}
		final int status = di.getStatus();

		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				if (DownloadManager.binder == null)
					return;
				switch (status) {
				case DownloadItem.RUNNING:
					switch (arg2) {
					case 0:
						DownloadManager.binder.getService().stopDownloadTask(
								item_id, true);
						break;
					case 1:
						DownloadManager.binder.getService().removeDownloadTask(
								item_id, true);
						break;
					case 2:
						d.dismiss();
						break;
					}
					break;
				case DownloadItem.PAUSED:
					switch (arg2) {
					case 0:
						//
						if(!mInternetStateMgr.hasInternet()) {
							Toast.makeText(DownloadManager.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
						} else if(Utils.isToDownloadDayLimit(DownloadManager.this)){
							showDownloadLimitDialog(DownloadManager.this, DOWNLOAD_CONFIRM_DAY_TIME);
							
						} /*else if(Utils.isToDownloadLimitInPeakTimes(DownloadManager.this)){
							showDownloadLimitDialog(DownloadManager.this, DOWNLOAD_CONFIRM_PEAK_TIME);
						} */else if (Cfg.mNotWifiProtect && mInternetStateMgr.hasMobile()) {
							CommonDlg.showConfirmDlg(DownloadManager.this,
									-1,
									di.getUnit_name() + "\n\n网络连接非Wi-Fi，会消耗手机卡流量" + Song.getSizeInMB(di.getFileSize()) + "，确定下载？" ,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface arg0, int arg1) {
											DownloadManager.binder.getService().startDownloadTask(item_id, false);
										}
									});	
						} else {
							DownloadManager.binder.getService().startDownloadTask(item_id, true);
						}
						break;
					case 1:
						DownloadManager.binder.getService().removeDownloadTask(
								item_id, true);
						break;
					case 2:
						d.dismiss();
						break;
					}
					break;
				default:
					switch (arg2) {
					case 0:
						DownloadManager.binder.getService().stopDownloadTask(
								item_id, true);
						break;
					case 1:
						DownloadManager.binder.getService().startDownloadTask(
								item_id, mInternetStateMgr.hasWifi());
						break;
					case 2:
						DownloadManager.binder.getService().removeDownloadTask(
								item_id, true);
						break;
					case 3:
						d.dismiss();
						break;
					}
				}

				d.dismiss();
			}

		});
		String[] commands = null;
		switch (status) {
		case DownloadItem.RUNNING:
			commands = new String[] { getString(R.string.download_pause),
					getString(R.string.download_cancel),
					getString(R.string.download_back) };
			break;
		case DownloadItem.PAUSED:
			commands = new String[] { getString(R.string.download_go),
					getString(R.string.download_cancel),
					getString(R.string.download_back) };
			break;
		default:
			commands = new String[] { getString(R.string.download_pause),
					getString(R.string.download_go),
					getString(R.string.download_cancel),
					getString(R.string.download_back) };
		}

		list.setAdapter(new ArrayAdapter<String>(DownloadManager.this,
				R.layout.list_dialog_item, commands));

		d.show();
	}

	private final ServiceConnection m_connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			log.verbose("i am here: onServiceConnected enter");
			binder = (DownloadService.LocalBinder) service;
			m_isBound = true;
			log.verbose("i am here: onServiceConnected exit");
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			m_isBound = false;
			binder = null;
			log.verbose("i am here: onServiceDisconnected");
			DownloadManager.this.finish();
		}
	};

	@Override
	public void DownloadListChanged(final int eventid, final DownloadItem item) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				switch (eventid) {
				case DownloadList.DOWNLOAD_LIST_EVNET_ADD_ITEM: {
					boolean find = false;
					for (int i = 0; i < mListData.size(); i++) {
						HashMap<String, Object> ii = (HashMap<String, Object>) mListData
								.get(i);
						long id = (Long) ii.get(DownloadService.DOWNLOAD_ID);
						if (id == item.getItemId()) {
							find = true;
							break;
						}
					}

					if (!find) {
						HashMap<String, Object> ii = new HashMap<String, Object>();
						ii.put(DownloadService.DOWNLOAD_ID, item.getItemId());
						ii.put(DownloadService.DOWNLOAD_NAME,
								item.getUnit_name());
						ii.put(DownloadService.DOWNLOAD_POS,
								item.getDownload_pos());
						ii.put(DownloadService.DOWNLOAD_DOWNLOAD_STATUS,
								item.getStatus());
						ii.put(DownloadService.DOWNLOAD_FILESIZE,
								item.getFileSize());
						ii.put(DownloadService.DOWNLOAD_FILE_TYPE,
								item.getFileType());

						mListData.add(ii);
						Message msg = myViewUpdateHandler.obtainMessage();
						msg.getData().putBoolean("listupdate", true);
						msg.getData().putLong(DownloadService.DOWNLOAD_ID,
								item.getItemId());
						myViewUpdateHandler.sendMessage(msg);
					}
					break;
				}
				case DownloadList.DOWNLOAD_LIST_EVNET_REMOVE_ITEM: {
					log.d("EVNET_REMOVE_ITEM:" + item.getUnit_name() + "--"
							+ item.getDownloadSize());
					for (int i = 0; i < mListData.size(); i++) {
						HashMap<String, Object> ii = (HashMap<String, Object>) mListData
								.get(i);
						long id = (Long) ii.get(DownloadService.DOWNLOAD_ID);
						if (id == item.getItemId()) {
							log.verbose("EVNET_REMOVE_ITEM ,remove it");
							mListData.remove(i);
							break;
						}
					}

					Message msg = myViewUpdateHandler.obtainMessage();
					msg.getData().putBoolean("listupdate", true);
					msg.getData().putLong("id", item.getItemId());
					myViewUpdateHandler.sendMessage(msg);
					mAdapter.notifyDataSetChanged();
					break;
				}
				case DownloadList.DOWNLOAD_LIST_EVNET_UPDATE_ITEM: {
					// log.d("UDTYPE_DOWNLOADING");
					Message msg = myViewUpdateHandler.obtainMessage();
					boolean find = false;
					for (int i = 0; i < mListData.size(); i++) {
						HashMap<String, Object> ii = (HashMap<String, Object>) mListData
								.get(i);
						long id = (Long) ii.get(DownloadService.DOWNLOAD_ID);
						if (id == item.getItemId()) {
							find = true;
							if (item.getDownload_pos() != 0
									&& item.getStatus() == DownloadItem.FINISHED) {
								log.verbose("download item " + id
										+ " status is " + item.getStatus()
										+ " , finished ,remove it");
								mListData.remove(i);
								msg.getData().putBoolean("listupdate", true);

							} else {
								ii.put(DownloadService.DOWNLOAD_ID,
										item.getItemId());
								ii.put(DownloadService.DOWNLOAD_NAME,
										item.getUnit_name());
								ii.put(DownloadService.DOWNLOAD_POS,
										item.getDownload_pos());
								ii.put(DownloadService.DOWNLOAD_DOWNLOAD_STATUS,
										item.getStatus());
								ii.put(DownloadService.DOWNLOAD_FILESIZE,
										item.getFileSize());
								ii.put(DownloadService.DOWNLOAD_FILE_TYPE,
										item.getFileType());
								msg.getData().putBoolean("listupdate", false);
							}
							break;
						}
					}

					if (false == find) {
						if (item.getDownload_pos() != 0
								&& item.getDownload_pos() == item.getFileSize()) {
							msg.getData().putBoolean("listupdate", false);
						} else {
							HashMap<String, Object> ii = new HashMap<String, Object>();
							ii.put(DownloadService.DOWNLOAD_ID,
									item.getItemId());
							ii.put(DownloadService.DOWNLOAD_NAME,
									item.getUnit_name());
							ii.put(DownloadService.DOWNLOAD_POS,
									item.getDownload_pos());
							ii.put(DownloadService.DOWNLOAD_DOWNLOAD_STATUS,
									item.getStatus());
							ii.put(DownloadService.DOWNLOAD_FILESIZE,
									item.getFileSize());
							ii.put(DownloadService.DOWNLOAD_FILE_TYPE,
									item.getFileType());
							mListData.add(ii);
							msg.getData().putBoolean("listupdate", true);
						}
					}

					msg.getData().putLong(DownloadService.DOWNLOAD_ID,
							item.getItemId());
					myViewUpdateHandler.sendMessage(msg);
					break;
				}
				case DownloadList.DOWNLOAD_LIST_EVNET_REMOVE_ITEMS:
					break;
				}
			}
		});
	}

	@Override
	public void onDestroy() {
		m_list.clearListListener(this);
		if (m_isBound) {
			unbindService(m_connection);
		}
		if (removeTaskReceiver != null) {
			unregisterReceiver(removeTaskReceiver);
			removeTaskReceiver = null;
		}
		super.onDestroy();
	}

//	private boolean hasWindowFoucs;
//
//	@Override
//	public void onWindowFocusChanged(boolean hasFocus) {
//		hasWindowFoucs = hasFocus;
//		super.onWindowFocusChanged(hasFocus);
//	}

	private BroadcastReceiver removeTaskReceiver = null;

	public void registerRemoveTask(Context context) {
		if (removeTaskReceiver == null) {
			removeTaskReceiver = new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {
					Message message = DownloadManager.this.myViewUpdateHandler
							.obtainMessage(MSG_TYPE_REMOVETASK);
					Bundle bundle = new Bundle();
					bundle.putLong("itemid", intent.getLongExtra("itemid", -1));
					bundle.putLong("statuscode", Long.parseLong(Integer
							.toString(intent.getIntExtra("statuscode", -1))));
					message.setData(bundle);
					myViewUpdateHandler.sendMessage(message);
				}
			};
		}

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_REMOVE_TASK);
		context.registerReceiver(removeTaskReceiver, intentFilter);
	}
	
	public static void showDownloadLimitDialog(final Context ctx, int type){
		String message;
		/*if(type == DOWNLOAD_CONFIRM_PEAK_TIME){
			message = Utils.getPeakTimeLimitMsgString() + "开通VIP会员，即可享受无限制高速下载。";
		} else {*/
		message = Utils.getDayLimitMsgString() + "开通VIP会员，即可享受无限制高速下载。";
//		}
		CommonDlg.showConfirmDlg(ctx,
				-1, message , "开通VIP", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Utils.openBuyVipPage(ctx);
					}
				});
	}
	
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		// TODO Auto-generated method stub
		return CustomOptionMenu.showCustomMenu(featureId, menu, this, 
				((ViewGroup)findViewById(android.R.id.content)).getChildAt(0));
	}

}