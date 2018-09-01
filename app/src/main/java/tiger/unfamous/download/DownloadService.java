package tiger.unfamous.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.umeng.analytics.MobclickAgent;
import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.common.InternetStateMgr;
import tiger.unfamous.ui.DownloadManager;
import tiger.unfamous.ui.LocalBrowser;
import tiger.unfamous.ui.PlayActivity;
import tiger.unfamous.utils.DataBaseHelper;
import tiger.unfamous.utils.MyLog;
import tiger.unfamous.utils.Utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager.WakeLock;
import android.widget.RemoteViews;
import android.widget.Toast;

public class DownloadService extends Service implements
		MyTaskListener<DownloadTask> {
	private static final MyLog logger = new MyLog();

	private final static int MaxTask = 1;

	private DownloadList m_list = null;

	public static final int ERR_CODE_SUCCESS = 0;
	public static final int ERR_CODE_SDCARD_NO_SPACE = -1;
	public static final int ERR_CODE_NO_ITEM = -2;
	public static final int ERR_CODE_ERROR = -3;
	public static final int ERR_CODE_NIGHT_LIMIT = -4;
	public static final int ERR_CODE_SDCARD_NOT_MOUNTED = -5;

	private static final int KEEP_ALIVE = 100;
	
	private static final int  NIGHT_DOWNLOAD_LIMIT_TAG = 1100;
	
	
//	public static final int MOOD_VIEW_PEAK_LIMIT = 7;
	public static final int MOOD_VIEW_DAY_LIMIT = 10;

	private ThreadPoolExecutor m_executor;
	private Map<Long, DownloadTask> m_taskMap;

	private BroadcastReceiver mUnmountReceiver = null;
	private BroadcastReceiver mPowerOffReceiver = null;

	public static String DOWNLOAD_ID = "file.did";
	public static String DOWNLOAD_URL = "file.url";
	public static String DOWNLOAD_DIR_NAME = "file.dirname";
	public static String DOWNLOAD_NAME = "file.name";
	public static String DOWNLOAD_FILE_TYPE = "file.type";
	public static String DOWNLOAD_DOWNLOAD_STATUS = "file.status";
	public static String DOWNLOAD_FILESIZE = "file.file.size";
	public static String DOWNLOAD_POS = "file.pos";

	public static final int EVENT_DOWNLOAD_COMPLITED = 1;
	public static final int EVENT_UPLOAD_COMPLITED = 2;
	public static final int EVENT_RECEIVED_PUSH = 3;
	//public static final int EVENT_NOSDCARD = 4;
	private static final int EVENT_DATA_STATE_CHANGED = 6;
	private static final int EVENT_DATA_STATE_TIMEOUT = 7;
	private static final int EVENT_DOWNLOAD_FAILED = 8;
	private static final int EVENT_NETWORK_EXCEPTION = 9;
	private static final int EVENT_DATA_DUPLICATE = 10;
	private static final int EVENT_DATA_DUPLICATE_PART = 12;
	private static final int EVENT_IO_EXCEPTION = 11;
	private static final int EVENT_NOT_FOUND = 13;
	private static final int EVENT_DOWNLOAD_WIFI_DISCONNECTED = 14;
	private static final int EVENT_STOPPING_DOWNLOAD_TASK = 15;
	private static final int EVENT_SDCARD_NOT_MOUNTED = 16;
	private static final int EVENT_SDCARD_NO_SPACE = 17;
	private static final int EVENT_SDCARD_UNMOUNT = 18;
	private static final int EVENT_SDCARD_REMOVE = 19;
	private static final int EVENT_SDCARD_MOUNTED = 20;
//	private static final int EVENT_DOWNLOAD_SERVICE_RECREATE = 21;

	private final long[] itembuf = new long[10];

	private DataBaseHelper databaseHelper;
	private WifiManager.WifiLock mWifiLock;
	private WakeLock mWakeLock;
	
	private InternetStateMgr mInternetStateMgr;
	private int mInternetState;
	public static boolean mIsStartingDownAllSongs = false;
	public static boolean mIsStoppingDownAllSongs = false;
	
	private boolean mNeedKillProcessFlag;
	
	private boolean mDownloadingNoficationUpdate = false;

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case EVENT_DOWNLOAD_COMPLITED:
				// String dhint = msg.getData().getString(DOWNLOAD_NAME) +
				// getString(R.string.downloadcomplete);
				// Toast.makeText(DownloadService.this, dhint, 8000).show();
				break;
			case EVENT_DOWNLOAD_FAILED:
				// Toast.makeText(DownloadService.this,getString(R.string.download_fail_in_list),
				// Toast.LENGTH_SHORT).show();
				break;
//			case EVENT_NOSDCARD:
//				Toast.makeText(DownloadService.this,
//						R.string.download_no_sdcard, Toast.LENGTH_SHORT).show();
//				break;
			case EVENT_DATA_STATE_TIMEOUT:
				
				break;
			case EVENT_NETWORK_EXCEPTION:
				Toast
						.makeText(DownloadService.this,
								R.string.download_network_exception,
								Toast.LENGTH_SHORT).show();
				break;
			case EVENT_IO_EXCEPTION:
				Toast.makeText(DownloadService.this, R.string.filedeleted,
						Toast.LENGTH_SHORT).show();
				break;
			case EVENT_DATA_STATE_CHANGED:
				
				break;

			case EVENT_DATA_DUPLICATE:
				Toast.makeText(DownloadService.this,
						R.string.download_dupicate, Toast.LENGTH_SHORT).show();
				break;
			case EVENT_DATA_DUPLICATE_PART:
				Toast.makeText(DownloadService.this,
						R.string.download_dupicate_part, Toast.LENGTH_SHORT)
						.show();
				break;
			case EVENT_DOWNLOAD_WIFI_DISCONNECTED:
				break;				
			case EVENT_NOT_FOUND:
				removeDownloadTask(msg.arg1, true);
				break;
			case EVENT_STOPPING_DOWNLOAD_TASK:
				Toast.makeText(DownloadService.this, R.string.stopping, Toast.LENGTH_SHORT)
						.show();
				break;
			case EVENT_SDCARD_NOT_MOUNTED:
				Toast.makeText(DownloadService.this,
						R.string.download_sdcard_not_mounted, Toast.LENGTH_SHORT).show();
				break;
			case EVENT_SDCARD_NO_SPACE:
				Toast.makeText(DownloadService.this,
						R.string.download_sdcard_no_space, Toast.LENGTH_SHORT).show();
				break;
			case EVENT_SDCARD_UNMOUNT:
				Toast.makeText(DownloadService.this,
						R.string.download_sdcard_unmount, Toast.LENGTH_SHORT).show();				
				break;
			case EVENT_SDCARD_REMOVE:
				Toast.makeText(DownloadService.this,
						R.string.download_sdcard_remove, Toast.LENGTH_SHORT).show();
				break;
//			case EVENT_DOWNLOAD_SERVICE_RECREATE:
//				Toast.makeText(DownloadService.this,
//						R.string.download_service_recreate, Toast.LENGTH_SHORT).show();
//				break;
			case EVENT_SDCARD_MOUNTED:
				Toast.makeText(DownloadService.this, R.string.download_sdcard_mounted,
						Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}

		}
	};

	private void doDownload(long itemid) {
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {
			DownloadItem item = m_list.getItemById(itemid);
			item.setStatus(DownloadItem.PAUSED);
			m_list.updateItem(item);
			Message message = mHandler.obtainMessage();
			message.what = EVENT_SDCARD_NOT_MOUNTED;
			mHandler.sendMessage(message);
		} else if (status.equals(Environment.MEDIA_MOUNTED)
				&& !(Utils.getFreeSpace(Cfg.SDCARD_PATH) > Cfg.sdSpaceLimitation * 1024)) {
			Message message = mHandler.obtainMessage();
			message.what = EVENT_SDCARD_NO_SPACE;
			mHandler.sendMessage(message);			
		} else {
			DownloadManager.LAUNCH_UDTYPE = DownloadManager.LAUNCH_DOWNLOADING;
			startDownloadTask(itemid, this.isWifiConnected());
		}
	}

	public void lockWifi() {
		if (mWakeLock != null && !mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}
		if (mWifiLock != null && !mWifiLock.isHeld()) {
			mWifiLock.acquire();
		}
	}

	public void releaseWifi() {
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		if (mWifiLock != null && mWifiLock.isHeld()) {
			mWifiLock.release();
			logger.e("releaseWifi1");
		}
	}

	public DownloadService() {

	}

	@Override
	public boolean isWifiConnected() {
		return mInternetStateMgr.isWifiConnected(mInternetStateMgr.getState());
	}

	@Override
	public boolean hasInternet() {
		// TODO Auto-generated method stub
		return mInternetStateMgr.hasInternet();
	}	
	
	@Override
	public void onCreate() {
		super.onCreate();
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mDownloadNotification = new Notification();
		logger.v("DownloadService onCreate() ---> Enter");
		
		mInternetStateMgr = new InternetStateMgr(this);
		mInternetState = Cfg.loadInt(this, Cfg.PREF_INTERNET_STATE, InternetStateMgr.INVALID_STATE);
		logger.v("DownloadService onCreate() mInternetState is " + mInternetState);
		if (mInternetState == InternetStateMgr.INVALID_STATE) {
			mInternetState = mInternetStateMgr.getState();
			Cfg.saveInt(this, Cfg.PREF_INTERNET_STATE, mInternetState);
		}
		
		for (int i = 0; i < itembuf.length; i++) {
			itembuf[i] = -1;
		}

		m_list = DownloadList.getInstance();
		m_taskMap = new ConcurrentHashMap<Long, DownloadTask>();
		m_executor = new ThreadPoolExecutor(MaxTask, MaxTask, KEEP_ALIVE,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

		registerExternalStorageListener();
		registerPowerOffListener();

		if (m_list != null) {
			new Thread(new stopAllAbnormalExitDownloadTaskThread(), "stopAllAbnormalExitTask").start();
		}
		
		databaseHelper = DataBaseHelper.getInstance();
//		if (mWakeLock == null) {
//			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE); 
//			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ""); 
//		}
//		if (mWifiLock == null) {
//			mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
//					.createWifiLock("DownLoadWifiLock");
//		}
		logger.v("onCreate() ---> Exit");
	}

	private void stopAllAbnormalExitDownloadTask() {
		mIsStoppingDownAllSongs = true;
		ArrayList<DownloadItem> list = m_list.getList();
		
		boolean hasRunning = false;
		for (int i = 0; i < list.size(); i++) {
			DownloadItem nextItem = list.get(i);
			int status = nextItem.getStatus();
			if (status == DownloadItem.RUNNING) {
				this.setMoodViewUpdate(nextItem, 5);
				nextItem.setStatus(DownloadItem.PAUSED);
				m_list.updateItem(nextItem);
				
				if (!hasRunning) {
					hasRunning = true;
					logger.i("DownloadService re-create");
//					mHandler.sendMessage(mHandler.obtainMessage(EVENT_DOWNLOAD_SERVICE_RECREATE));
				}
			}
		}
		
		mIsStoppingDownAllSongs = false;
	}
	
	private class stopAllAbnormalExitDownloadTaskThread implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
//			Looper.prepare();
			stopAllAbnormalExitDownloadTask();
//			Looper.loop();
		}
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		logger.v("DownloadService onDestroy() ---> Enter");
		for (Long tid : m_taskMap.keySet()) {
			DownloadTask t = m_taskMap.get(tid);
			if (t != null) {
				t.cancelTask();
				m_executor.remove(t);
				m_taskMap.remove(tid);
			}
		}
		
		if (m_list != null) {
			Iterator<DownloadItem> it = m_list.getList().iterator();
			while (it.hasNext()) {
				DownloadItem nextItem = it.next();
				int status = nextItem.getStatus();		
				if (status == DownloadItem.RUNNING) {
					nextItem.setStatus(DownloadItem.PAUSED);
					m_list.updateItem(nextItem);
				}
			}
		}

		if (mUnmountReceiver != null) {
			unregisterReceiver(mUnmountReceiver);
			mUnmountReceiver = null;
		}

		if (mPowerOffReceiver != null) {
			unregisterReceiver(mPowerOffReceiver);
			mPowerOffReceiver = null;
		}


//		releaseWifi();  //可能导致 Under-Locked exception
		logger.v("onDestroy() ---> Exit");
		if(mNeedKillProcessFlag){
			cancelNotification();
			Utils.killProcessByTime(5000);
		}
		
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		if (intent == null) {
			return;
		}
		
		logger.v("DownloadService onStart() ---> Enter");
//		Cfg.init(this);

//		if (Cfg.ACTION_CONNECTIVITY_CHANGE.equals(intent.getAction())) {	
//			int state = intent.getIntExtra("connectState", InternetStateMgr.INVALID_STATE);			
//			if (state != mInternetState) {
//				if (Cfg.mWifiAutoDownload
//						&& m_list.getAllNonCompleteItems(DownloadItem.UDTYPE_DOWNLOADING).size() > 0
//						&& mInternetStateMgr.isToWifi(mInternetState, state)) {
//					onWifiConnected();			
//				} else if (Cfg.mNotWifiProtect
//						&& mInternetStateMgr.isFromWifi(mInternetState, state)) {
//					onWifiDisConnected();
//				}
//			}
//			
//			mInternetState = state;
//
//			if (m_taskMap.size() == 0) {
//				release();
//			}
//			return;
//		}
		
//		mInternetState = mInternetStateMgr.getState();

		if (Cfg.ACTION_CONNECTIVITY_WIFICONNECTED.equals(intent.getAction())) {
			while (mIsStoppingDownAllSongs
					|| DownloadManager.mIsStoppingDownAllSongs
					|| DownloadManager.mIsDeletingUncompleteSongs) {
				try {
					Thread.sleep(1000);
					logger.d("startDownAllSongs wait for stopDownAllSongs and deleteUncompleteSongs end...");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			new Thread(new onWifiConnectedThread(), "onWifiConnected").start();
			return;
		}

		if (Cfg.ACTION_CONNECTIVITY_WIFIDISCONNECTED.equals(intent.getAction())) {
			while (DownloadManager.mIsStartingDownAllSongs
					|| PlayActivity.mIsStartingDownAllSongs
					|| mIsStartingDownAllSongs
					|| DownloadManager.mIsDeletingUncompleteSongs) {
				try {
					Thread.sleep(1000);
					logger.d("stopDownAllSongs wait for startDownAllSongs and deleteUncompleteSongs end...");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			shutdownExecutor();
			new Thread(new onWifiDisConnectedThread(), "onWifiDisconnect").start();
			return;
		}

		if (Cfg.ACTION_CHECK_STOP_SERVICE.equals(intent.getAction())) {
			if (m_taskMap.size() == 0) {
				//user select exit,so we need to kill the process
				mNeedKillProcessFlag = intent.getBooleanExtra(Cfg.EXTRA_KILL_PROCESS, false);
				release();
				return;
			}
		}

		String udtype = intent.getStringExtra(DownloadManager.LAUNCH_UDTYPE);
		if (udtype != null
				&& udtype.equalsIgnoreCase(DownloadManager.LAUNCH_DOWNLOADING)) {
			String unit_url = intent.getStringExtra(DOWNLOAD_URL);
			String dir_name = intent.getStringExtra(DOWNLOAD_DIR_NAME);
			String unit_name = intent.getStringExtra(DOWNLOAD_NAME);
			String unit_type = intent.getStringExtra(DOWNLOAD_FILE_TYPE);
			int fileSize = intent.getIntExtra(DOWNLOAD_FILESIZE, 0);

			if (unit_url != null) {
				// check sd card status
				String status = Environment.getExternalStorageState();
				if (DownloadItem.AUDIO_FILE.equals(unit_type)
						&& !status.equals(Environment.MEDIA_MOUNTED)) {
					Message message = mHandler.obtainMessage();
					message.what = EVENT_SDCARD_NOT_MOUNTED;
					mHandler.sendMessage(message);
					return;
				} else if (DownloadItem.AUDIO_FILE.equals(unit_type)
						&& status.equals(Environment.MEDIA_MOUNTED)
						&& !(Utils.getFreeSpace(Cfg.SDCARD_PATH) > Cfg.sdSpaceLimitation * 1024)) {
					Message message = mHandler.obtainMessage();
					message.what = EVENT_SDCARD_NO_SPACE;
					mHandler.sendMessage(message);
					return;
				}
				
				DownloadItem item = null;
				item = databaseHelper.getDownloadItemByUnitUrl(unit_url);
//				if (oitem != null) {
//					File file = new File(Cfg.DOWNLOAD_DIR + "/" + dir_name
//							+ "/" + unit_name + oitem.getExtention());
//					File filepart = new File(Cfg.DOWNLOAD_DIR + unit_name
//							+ DownloadTask.TAG_SUFFIX);
//					if (file.exists() || filepart.exists()) {
//						Message message = mHandler.obtainMessage();
//						message.what = EVENT_DATA_DUPLICATE;
//						mHandler.sendMessage(message);
//					return;
//					} else {
//						m_list.removeItem(oitem.getItemId());
//					}
//				}
				long itemId;
				if (item == null) {
					item = new DownloadItem(-1, unit_url,
							DownloadItem.RUNNING, dir_name, unit_name, unit_type,
							0, new Date().getTime(), fileSize, 0);
					itemId = m_list.addItem(item);
				} else {
					itemId = item.getItemId();
					if (item.getStatus() == DownloadItem.FINISHED) {
						item.setDownload_pos(0);
						item.setFileSize(0);
						item.setDownloadSize(0);
					}
				}

//				String status = Environment.getExternalStorageState();
//				if (item.getFileType().equals(DownloadItem.AUDIO_FILE) 
//						&& !status.equals(Environment.MEDIA_MOUNTED)) {
//					Message message = mHandler.obtainMessage();
//					message.what = EVENT_NOSDCARD;
//					mHandler.sendMessage(message);
//				} else {
//					if (oitem == null) {
//						itemid = m_list.addItem(item);
//					} else {
//						itemid = oitem.getItemId();
//					}
					
					startDownloadTask(itemId, this.isWifiConnected());
//				}
				return;
			}
		} else {
			long itemid = intent.getLongExtra("itemid", -1);
			if (itemid != -1) {
				doDownload(itemid);
			}
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return m_binder;
	}

	// service binder
	private final IBinder m_binder = new LocalBinder();

	public class LocalBinder extends Binder {
		public DownloadService getService() {
			return DownloadService.this;
		}
	}

	public DownloadList getDownloadList() {
		return m_list;
	}

	public int startDownloadTask(long item_id, boolean isDownloadWithWifi) {
		logger.v("DownloadService startDownloadTask() ---> Enter");
		// releaseWifi();
		lockWifi();
		DownloadItem item = m_list.getItemById(item_id);
		if (item == null) {
			logger.d("DownloadItem is null!");
			return ERR_CODE_NO_ITEM;
		}

		DownloadTask t = null;
		item.setStatus(DownloadItem.RUNNING);
		t = new DownloadTask(item, this);
		String status = Environment.getExternalStorageState();
		logger.v("SD card status is " + status);

		if (item.getFileType().equals(DownloadItem.AUDIO_FILE)
				&& !status.equals(Environment.MEDIA_MOUNTED)) {
			item.setStatus(DownloadItem.PAUSED);
			m_list.updateItem(item);
			mHandler.sendMessage(mHandler.obtainMessage(EVENT_SDCARD_NOT_MOUNTED));
			return ERR_CODE_SDCARD_NOT_MOUNTED;
		} else if (item.getFileType().equals(DownloadItem.AUDIO_FILE)
						&& status.equals(Environment.MEDIA_MOUNTED) 
						&&!(Utils.getFreeSpace(Cfg.SDCARD_PATH) > Cfg.sdSpaceLimitation * 1024)) {
			item.setStatus(DownloadItem.PAUSED);
			m_list.updateItem(item);
			mHandler.sendMessage(mHandler.obtainMessage(EVENT_SDCARD_NO_SPACE));
			return ERR_CODE_SDCARD_NO_SPACE;
		}
		// else
		// if(Settings.System.getInt(DownloadService.this.getContentResolver(),
		// Settings.System.AIRPLANE_MODE_ON, 0) == 1){
		// item.setStatus(DownloadItem.PAUSED);
		// m_list.updateItem(item);
		// mHandler.sendMessage(mHandler.obtainMessage(EVENT_NETWORK_EXCEPTION));
		// return ERR_CODE_SUCCESS;

		// }
		else {
			// Toast.makeText(this, R.string.starting, Toast.LENGTH_SHORT)
			// .show();
			m_list.updateItem(item);
			m_taskMap.put(item_id, t);
			
			t.setDwonloadWithWifi(isDownloadWithWifi);
			
			if (m_executor.isShutdown()) {
				m_executor = null;
				m_executor = new ThreadPoolExecutor(MaxTask, MaxTask,
						KEEP_ALIVE, TimeUnit.MILLISECONDS,
						new LinkedBlockingQueue<Runnable>());
			}

			m_executor.execute(t);
			logger.v("startDownloadTask() ---> Exit");
			return ERR_CODE_SUCCESS;
		}
	}

	// public boolean stopDownloadTask(long item_id) {
	// logger.v("DownloadService stopDownloadTask() ---> Enter");
	// DownloadTask t = m_taskMap.get(item_id);
	// if (t != null) {
	//
	// if (t.isWaiting() && !t.isRunning()) {
	// DownloadItem item = t.getDownloadItem();
	// item.setStatus(DownloadItem.PAUSED);
	// m_list.updateItem(item);
	// t.cancelTask();
	// logger.d("DownloadTask.isCancel=" + t.isCancel());
	// logger.d("DownloadTask.isRuning=" + t.isRunning());
	// } else if (t.isCancel()) {
	// Toast.makeText(this, R.string.stopping, Toast.LENGTH_SHORT)
	// .show();
	// } else
	// t.cancelTask();
	// }
	// logger.v("stopDownloadTask() ---> Exit");
	// return true;
	// }

	public void shutdownExecutor() {
		//m_executor.shutdownNow();
		LinkedBlockingQueue<Runnable> linkedQueue = (LinkedBlockingQueue<Runnable>)m_executor.getQueue();
		int queueSize = linkedQueue.size();
		for (int i = 0; i < queueSize; i++) {
			DownloadTask t = (DownloadTask)linkedQueue.peek();
			m_executor.remove(t);
		}
		
//		for (Long tid : m_taskMap.keySet()) {
//			DownloadTask t = m_taskMap.get(tid);
//			if (t != null && !t.isRunning()) {
//				m_executor.remove(t);
//			}
//		}
	}

	/**
	 * 
	 * @param item_id
	 *            download task related item id
	 * @param notify
	 *            true:notify user(display a toast message.else false.You should
	 *            never set this flag to true when in a thread
	 * @return true:stop ok.
	 */
	public boolean stopDownloadTask(long item_id, boolean notify) {
		logger.v("stopDownloadTask() ---> Enter");
		DownloadTask t = m_taskMap.get(item_id);
		if (t != null) {
			if (t.isCancel()) {
				if (notify) {
					Message message = mHandler.obtainMessage();
					message.what = EVENT_STOPPING_DOWNLOAD_TASK;
					mHandler.sendMessage(message);
				}
			} else {
				t.cancelTask();
			}
			taskCancelled(t, null);
		}
		logger.v("stopDownloadTask() ---> Exit");
		return true;
	}
	
	public boolean stopDownloadTaskForWiFiDisconnected(long item_id) {
		logger.v("stopDownloadTaskForWiFiDisconnnected() ---> Enter");
		DownloadTask t = m_taskMap.get(item_id);
		if (t != null) {
//			DownloadItem item = t.getDownloadItem();
//			item.setStatus(DownloadItem.PAUSED);
//			item.setTimeStep(new Date().getTime());
//			m_list.updateItem(item);
			if (t.isDownloadWithWifi()) {
				t.cancelTask();
				taskWifiDisconnected(t);
			}
		}
		logger.v("stopDownloadTaskForWiFiDisconnnected() ---> Exit");
		return true;
	}

	public boolean stopDownloadTaskForSDEject(long item_id) {
		logger.v("stopDownloadTaskForSDEject() ---> Enter");
		DownloadTask t = m_taskMap.get(item_id);
		if (t != null) {
			t.cancelTask();
			taskSDEject(t);
		}
		logger.v("stopDownloadTaskForSDEject() ---> Exit");
		return true;
	}
	
	public boolean removeDownloadTask(long item_id, boolean removefile) {
		logger.v("DownloadService removeDownloadTask() ---> Enter");
		if(m_taskMap == null || m_list == null){
			return false;
		}
		DownloadTask t = m_taskMap.get(item_id);
		DownloadItem item = m_list.getItemById(item_id);

		if (t != null) {
			 t.removeTask();
			 taskRemoved(t, null);
			// m_executor.remove(t);
		} else if (item != null){
			// if (item.getCharpter_name() != null) {
			File file = new File(Cfg.DOWNLOAD_DIR + "/" + item.getDir_name()
					+ "/" + item.getUnit_name());
			if (file.exists() && file.isDirectory()) {
				String[] filelist = file.list();
				for (int k = 0; k < filelist.length; k++) {
					File delfile = new File(Cfg.DOWNLOAD_DIR + "/"
							+ item.getDir_name() + "/" + item.getUnit_name()
							+ "\\" + filelist[k]);
					if (!delfile.isDirectory())
						delfile.delete();
				}
				File pfile = file.getParentFile();
				int brothers = pfile.listFiles().length;
				file.delete();

				if (brothers == 1) {
					pfile.delete();
				}
			} else {

			}
			// }
			m_list.removeItem(item_id);
			m_list.sortList();
		}
		logger.v("removeDownloadTask() ---> Exit");
		return true;
	}

	// don't del local part file
	@Override
	public void taskCancelled(DownloadTask downTask, Object obj) {
		// update list.
		logger.v("DownloadService taskCancelled:--------------->enter!");
		DownloadItem item = downTask.getDownloadItem();
		// see comment 8 of issue 83
		DownloadTask task = m_taskMap.get(item.getItemId());
		if ((task != null) && !downTask.equals(task)) {
			logger.i("DownloadService taskCancelled: DownloadTasks are different, don't cancel the task!");
			return;
		}
		
		if (item.getStatus() == DownloadItem.PAUSED) {
			logger.v("DownloadService taskCancelled: downTask has stopped.");	
			setMoodViewUpdate(item, 5);
			return;
		}
		
		// DownloadTask downTask = (DownloadTask) task;
		item.setStatus(DownloadItem.PAUSED);
		item.setTimeStep(new Date().getTime());
		setMoodViewUpdate(item, 5);
		m_list.updateItem(item);
		m_executor.remove(downTask);
		m_taskMap.remove(item.getItemId());

		release();
		logger.v(String.format("item id=%d", item.getItemId()));
		logger.v("taskCancelled:--------------->exit!");
	}

	// del local part file
	@Override
	public void taskRemoved(DownloadTask downTask, Object obj) {
		logger.v("DownloadService taskRemoved:--------------->enter!");
		// DownloadTask downTask = (DownloadTask) task;
		DownloadItem item = downTask.getDownloadItem();
		item.setStatus(DownloadItem.PAUSED);
		setMoodViewUpdate(item, 5);
		File file = new File(Cfg.DOWNLOAD_DIR + "/" + item.getDir_name() + "/"
				+ item.getUnit_name());
		if (file.exists()) {
			file.delete();
		} else {
			File tempfile = new File(Cfg.DOWNLOAD_DIR + "/"
					+ item.getDir_name() + "/" + item.getPartName());
			if (tempfile.exists())
				tempfile.delete();
		}

		m_list.removeItem(item.getItemId());
		m_list.sortList();
		m_executor.remove(downTask);
		m_taskMap.remove(item.getItemId());

		release();
		logger.v("DownloadService taskRemoved:--------------->eixt!");
	}
	

	@Override
	public void taskCompleted(DownloadTask downTask, Object obj) {
		// update list.
		// DownloadTask downTask = (DownloadTask) task;
		Utils.onCompletedOneTask(this);
//			Cfg.mCompletedTaskInPeakTimes++ ;
//			Cfg.saveInt(this, Cfg.PREF_CURRENT_DOWNLOADED_TASKS, Cfg.mCompletedTaskInPeakTimes);
//			Log.d("DownloadService", "total = " + Cfg.mTotalDownloadTasks + ",cur = " + Cfg.mCurCompleteTasks);
//			if( Cfg.mTotalDownloadTasks <= Cfg.mCurCompleteTasks){
//				stopAllDownloadTask(downTask);
//			}
//			
//		}
		DownloadItem item = downTask.getDownloadItem();
		logger.v("DownloadService taskCompleted:--------------->id: "
				+ item.getItemId());
//		item.setStatus(DownloadItem.FINISHED);
//		item.setTimeStep(new Date().getTime());
		
		String downloadDir = null;
		if (item.getFileType().equals(DownloadItem.AUDIO_FILE)) {
			if (m_list.getList().size() == 1) {
				setMoodViewUpdate(item, 2);
			} else {
				setMoodViewUpdate(item, 5);
			}
			downloadDir = Cfg.DOWNLOAD_DIR;
		} else if (item.getFileType().equals(DownloadItem.INSTALL_PACKAGE_FILE)) {
			setMoodViewUpdate(item, 5);
			downloadDir = Cfg.mInnerRootPath;
		}

		String partName;
		String realName = item.getUnit_name();

		// rename the file
		partName = downloadDir + "/" + item.getDir_name() + "/"
				+ item.getPartName();
		File partFile = new File(partName);
		partFile.renameTo(new File(downloadDir + "/" + item.getDir_name()
				+ "/" + realName + item.getExtention()));

		m_taskMap.remove(item.getItemId());
		m_list.removeItem(item.getItemId());
		m_list.sortList();

		Message msg = mHandler.obtainMessage();
		Bundle data = new Bundle();
		data.putString(DOWNLOAD_NAME, item.getUnit_name());
		msg.what = EVENT_DOWNLOAD_COMPLITED;
		msg.setData(data);
		mHandler.sendMessage(msg);
		
		if (item.getFileType().equals(DownloadItem.INSTALL_PACKAGE_FILE)) {
			File installFile = new File(downloadDir + "/" + item.getDir_name()
					+ "/" + realName + item.getExtention());
			Uri uri = Uri.fromFile(installFile);
			try {
				FileOutputStream fos = openFileOutput(installFile.getName(), MODE_WORLD_READABLE | MODE_APPEND);
				fos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			Intent installIntent = new Intent(Intent.ACTION_VIEW);
			installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
			startActivity(installIntent);
		}
		
		release();
		MobclickAgent.onEvent(this, Cfg.UM_DOK);
		logger.v("DownloadService taskCompleted:--------------->exit!");
	}

	@Override
	public void taskFailed(DownloadTask downTask, Throwable ex) {
		// 地址末尾有多余空格的话，会导致 IllegalArgumentException
		logger.e(ex);
		// DownloadTask downTask = (DownloadTask) task;
		DownloadItem item = downTask.getDownloadItem();
		if (!downTask.isRunning()) {
			item.setStatus(DownloadItem.PAUSED);
			item.setTimeStep(new Date().getTime());

			m_list.updateItem(item);
			m_taskMap.remove(item.getItemId());
			return;
		}

		String sdcardStatus = Environment.getExternalStorageState();
		String downloadFileType = item.getFileType();
		if (downloadFileType.equals(DownloadItem.AUDIO_FILE)
				&& (sdcardStatus.equals(Environment.MEDIA_REMOVED)
						|| sdcardStatus.equals(Environment.MEDIA_BAD_REMOVAL)
						|| sdcardStatus.equals(Environment.MEDIA_UNMOUNTED))) {
			setMoodViewUpdate(item, 8);
		} else if(DownloadItem.AUDIO_FILE.equals(downloadFileType)
				&& sdcardStatus.equals(Environment.MEDIA_MOUNTED)
				&& !(Utils.getFreeSpace(Cfg.SDCARD_PATH) > Cfg.sdSpaceLimitation * 1024)) {
			setMoodViewUpdate(item, 3);
			mHandler.sendMessage(mHandler.obtainMessage(EVENT_SDCARD_NO_SPACE));			
		} else {
			if (ex instanceof SocketTimeoutException) {
				setMoodViewUpdate(item, 3);
				Message msg = new Message();
				msg.what = EVENT_DATA_STATE_TIMEOUT;
				msg.arg1 = Integer.parseInt(item.getItemId() + "");
				mHandler.sendMessage(msg);
			} else if (DN.HTTP_NOT_FOUND_EXCEPTION.equals(ex.getMessage())) {
				setMoodViewUpdate(item, 4);
				Message msg = new Message();
				msg.what = EVENT_NOT_FOUND;

				msg.arg1 = Integer.parseInt(item.getItemId() + "");
				mHandler.sendMessage(msg);
			} else if (DN.RESOURCE_CHANGED_EXCEPTION.equals(ex.getMessage())) {
				setMoodViewUpdate(item, 3);
//				mHandler.sendMessage(mHandler.obtainMessage(EVENT_DOWNLOAD_RESOURCE_CHANGED));
			} else {
				setMoodViewUpdate(item, 3);
				mHandler.sendMessage(mHandler
						.obtainMessage(EVENT_DOWNLOAD_FAILED));
			}
		}

		release();
	}
	
	@Override
	public void taskWifiDisconnected(DownloadTask downTask) {
		DownloadItem item = downTask.getDownloadItem();
		if (item.getStatus() == DownloadItem.PAUSED) {
			setMoodViewUpdate(item, 6);
			return;
		}
		
		item.setStatus(DownloadItem.PAUSED);
		item.setTimeStep(new Date().getTime());
		m_list.updateItem(item);
		m_executor.remove(downTask);
		m_taskMap.remove(item.getItemId());
		setMoodViewUpdate(item, 6);
		mHandler.sendMessage(mHandler
				.obtainMessage(EVENT_DOWNLOAD_WIFI_DISCONNECTED));
		release();
	}
	
	@Override
	public void taskSDEject(DownloadTask downTask) {
		DownloadItem item = downTask.getDownloadItem();
		if (item.getStatus() == DownloadItem.PAUSED) {
			setMoodViewUpdate(item, 8);
			return;
		}
		
		item.setStatus(DownloadItem.PAUSED);
		item.setTimeStep(new Date().getTime());
		m_list.updateItem(item);
		m_executor.remove(downTask);
		m_taskMap.remove(item.getItemId());
		setMoodViewUpdate(item, 8);	
		release();
	}
	
	@Override
	public void taskSDNoSpace(DownloadTask downTask) {
		shutdownExecutor();	//when sdcard has exception, shutdown all the waiting tasks
		DownloadItem item = downTask.getDownloadItem();
		
		item.setStatus(DownloadItem.PAUSED);
		item.setTimeStep(new Date().getTime());
		m_list.updateItem(item);
		m_executor.remove(downTask);
		m_taskMap.remove(item.getItemId());
		setMoodViewUpdate(item, 8);

		Iterator<DownloadItem> it = m_list.getList().iterator();
		while (it.hasNext()) {
			DownloadItem nextItem = it.next();
			if (nextItem.getStatus() == DownloadItem.RUNNING) {
				nextItem.setStatus(DownloadItem.PAUSED);
				nextItem.setTimeStep(new Date().getTime());
				m_list.updateItem(nextItem);
				m_executor.remove(m_taskMap.get(nextItem.getItemId()));
				m_taskMap.remove(nextItem.getItemId());
			}
		}
		
		release();
		
	}
	
	@Override
	public void taskNoInternet(DownloadTask downTask) {
		shutdownExecutor();	//when has no internet, shutdown all the waiting tasks
		DownloadItem item = downTask.getDownloadItem();
		
		item.setStatus(DownloadItem.PAUSED);
		item.setTimeStep(new Date().getTime());
		m_list.updateItem(item);
		m_executor.remove(downTask);
		m_taskMap.remove(item.getItemId());
		setMoodViewUpdate(item, 9);
		
		
		Iterator<DownloadItem> it = m_list.getList().iterator();
		while (it.hasNext()) {
			DownloadItem nextItem = it.next();
			if (nextItem.getStatus() == DownloadItem.RUNNING) {
				nextItem.setStatus(DownloadItem.PAUSED);
				nextItem.setTimeStep(new Date().getTime());
				m_list.updateItem(nextItem);
				m_executor.remove(m_taskMap.get(nextItem.getItemId()));
				m_taskMap.remove(nextItem.getItemId());
			}
		}
		
		release();
	}
	
	@Override
	public void taskProgress(DownloadTask downTask, long value, long max) {
		// logger.v("Download file taskProgress() ---> Enter");
		
		if (downTask.isCancel() || downTask.isRemove()) {
			return;
		}

		// DownloadTask downTask = (DownloadTask) task;
		DownloadItem item = downTask.getDownloadItem();
		item.setStatus(DownloadItem.RUNNING);
		item.setDownload_pos(value);
		// item.setTimeStep(new Date().getTime());

		if (System.currentTimeMillis() - preupdatatime > 2000) {
			setMoodViewUpdate(item, 1);
			m_list.updateItem(item);
			preupdatatime = System.currentTimeMillis();
		}
		// logger.v("Download file taskProgress() ---> Exit");
	}

	@Override
	public void taskStarted(DownloadTask downTask) {
		logger.d("taskStarted:--------------->enter!");
		// DownloadTask downTask = (DownloadTask) task;
		//如果没有初始化，则进行初始化
		Cfg.init(this);
		
		if(Utils.isToDownloadDayLimit(this)){
			stopAllDownloadTask(null, MOOD_VIEW_DAY_LIMIT);
			return ;
		} /*else if(Utils.isToDownloadLimitInPeakTimes(this)){
			stopAllDownloadTask(null, MOOD_VIEW_PEAK_LIMIT);
			return ;
		} */
		
		DownloadItem item = downTask.getDownloadItem();
		//item.setStatus(DownloadItem.RUNNING);
		item.setTimeStep(new Date().getTime());
		setMoodViewUpdate(item, 0);
		m_list.updateItem(item);
		logger.d("taskStarted:--------------->exit!");
	}

	/**
	 * Registers an intent to listen for ACTION_MEDIA_EJECT notifications.
	 */
	private void registerExternalStorageListener() {
		logger.v("registerExternalStorageListener() ---> Enter");

		if (mUnmountReceiver == null) {
			mUnmountReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction().equals(Intent.ACTION_MEDIA_EJECT)) {
						// SD card removed
						new Thread(new onSDEjectThread(), "onSDEject").start();
					} else if (intent.getAction().equals(
							Intent.ACTION_MEDIA_MOUNTED)) {
						// SD card mounted
						onSDMounted();
					}
				}
			};
			IntentFilter iFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
			// iFilter.addAction(Intent.ACTION_MEDIA_KILL_ALL);
			iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
			iFilter.addDataScheme("file");
			registerReceiver(mUnmountReceiver, iFilter);
		}

		logger.v("registerExternalStorageListener() ---> Exit");
	}

	private void onSDMounted() {
		mHandler.sendEmptyMessage(EVENT_SDCARD_MOUNTED);

		Iterator<DownloadItem> it = m_list.getList().iterator();
		while (it.hasNext()) {
			DownloadItem nextItem = it.next();
			if (nextItem.getStatus() == DownloadItem.RUNNING) {
				stopDownloadTask(nextItem.getItemId(), false);
			}
		}

		it = m_list.getList().iterator();
		while (it.hasNext()) {
			DownloadItem nextItem = it.next();
			if (nextItem.getStatus() == DownloadItem.FINISHED)
				continue;
			// File file=new
			// File(nextItem.getFileName()+DownloadTask.TAG_SUFFIX);
			// boolean exists=file.exists();
			// if(exists&&file.length()!=nextItem.getDownloadSize()){
			// nextItem.setDownloadSize(file.length());
			// m_list.updateItem(nextItem);
			// }else if(!exists){
			// nextItem.setDownloadSize(0);
			// m_list.updateItem(nextItem);
			// }

		}
	}

	private void onSDEject() {
		logger.v("onSDEject() ---> Enter");
		while (DownloadManager.mIsStartingDownAllSongs ||
				PlayActivity.mIsStartingDownAllSongs ||
				mIsStartingDownAllSongs) {
			try {
				Thread.sleep(1000);
				logger.d("stopDownAllSongs wait for startDownAllSongs end...");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		shutdownExecutor();
		mIsStoppingDownAllSongs = true;

		Iterator<DownloadItem> it = m_list.getList().iterator();
		while (it.hasNext()) {
			DownloadItem nextItem = it.next();
			if (nextItem.getStatus() == DownloadItem.RUNNING) {
				DownloadTask t = m_taskMap.get(nextItem.getItemId());
				if (t.getDownloadResult() == DownloadTask.DOWNLOAD_RESULT_SDCARD_NOT_MOUNTED
						|| t.isRunning()) {
					stopDownloadTaskForSDEject(nextItem.getItemId());
					t.setSDEjectDetect(true);
				} else if (t.getDownloadResult() == DownloadTask.DOWNLOAD_RESULT_INVALID_VALUE){
					stopDownloadTask(nextItem.getItemId(), false);
				}
			}
		}

		String status = null;
		while ((status = Environment.getExternalStorageState()).equals(Environment.MEDIA_MOUNTED)) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		if (status.equals(Environment.MEDIA_UNMOUNTED)) {
			mHandler.sendMessage(mHandler.obtainMessage(EVENT_SDCARD_UNMOUNT));
		} else {
			mHandler.sendMessage(mHandler.obtainMessage(EVENT_SDCARD_REMOVE));		
		}

		mIsStoppingDownAllSongs = false;
		logger.v("onSDEject() ---> Exit");
	}
	
	private class onSDEjectThread implements Runnable {

		@Override
		public void run() {
//			Looper.prepare();
			onSDEject();
//			Looper.loop();	
		}
	}

	private void onPowerDown() {
		logger.v("onPowerDown() ---> Enter");

		Iterator<DownloadItem> it = m_list.getList().iterator();
		while (it.hasNext()) {
			DownloadItem nextItem = it.next();
			if (nextItem.getStatus() != DownloadItem.FINISHED) {
				stopDownloadTask(nextItem.getItemId(), false);
			}
		}

		mHandler.sendMessage(mHandler.obtainMessage(EVENT_SDCARD_NOT_MOUNTED));
		logger.v("onPowerDown ---> Exit");
	}

	private void registerPowerOffListener() {
		logger.v("registerPowerOffListener() ---> Enter");

		if (mPowerOffReceiver == null) {
			mPowerOffReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					onPowerDown();
				}
			};
			IntentFilter iFilter = new IntentFilter("oms.action.POWERDOWN");
			registerReceiver(mPowerOffReceiver, iFilter);
		}

		logger.v("registerPowerOffListener() ---> Exit");
	}
	
	private void onWifiConnected() {
		logger.v("onWifiConnected() ---> Enter");
		mIsStartingDownAllSongs = true;
//		mInternetState = mInternetStateMgr.getState();
		Iterator<DownloadItem> it = m_list.getList().iterator();
		while (it.hasNext()) {
			DownloadItem nextItem = it.next();
			if (nextItem.getStatus() == DownloadItem.PAUSED) {
				String status = Environment.getExternalStorageState();
				if (nextItem.getFileType().equals(DownloadItem.AUDIO_FILE)
						&& !status.equals(Environment.MEDIA_MOUNTED)) {
					mHandler.sendMessage(mHandler.obtainMessage(EVENT_SDCARD_NOT_MOUNTED));
					break;
				} else if (nextItem.getFileType().equals(DownloadItem.AUDIO_FILE)
						&& Environment.MEDIA_MOUNTED.equals(status)
						&& !(Utils.getFreeSpace(Cfg.SDCARD_PATH) > Cfg.sdSpaceLimitation * 1024)) {
					mHandler.sendMessage(mHandler.obtainMessage(EVENT_SDCARD_NO_SPACE));
					break;
				} else if (!isWifiConnected()) {
					break;
				} else {
					startDownloadTask(nextItem.getItemId(), true);
				}
			}
		}
		
		it = m_list.getList().iterator();
		while (it.hasNext()) {
			DownloadItem nextItem = it.next();
				if (nextItem.getFileType().equals(DownloadItem.INSTALL_PACKAGE_FILE)
						&& nextItem.getStatus() == DownloadItem.PAUSED) {
						startDownloadTask(nextItem.getItemId(), true);
				}
		}

		mIsStartingDownAllSongs = false;
		logger.v("onWifiConnected() ---> Exit");	
	}
	
	private class onWifiConnectedThread implements Runnable {

		@Override
		public void run() {
//			Looper.prepare();
			onWifiConnected();
//			Looper.loop();
		}
	}

	private void onWifiDisConnected() {
		logger.v("onWifiDisConnected() ---> Enter");
		mIsStoppingDownAllSongs = true;
//		mInternetState = mInternetStateMgr.getState();
//		Toast.makeText(this, R.string.download_wifi_disconnected, Toast.LENGTH_SHORT).show();
		Iterator<DownloadItem> it = m_list.getList().iterator();
		while (it.hasNext()) {
			DownloadItem nextItem = it.next();
			if (nextItem.getStatus() == DownloadItem.RUNNING) {
				DownloadTask t = m_taskMap.get(nextItem.getItemId());
				if (t == null) {
					continue;
				}
				if (t.getDownloadResult() == DownloadTask.DOWNLOAD_RESULT_WIFI_DISCONNECTED
						|| t.isRunning()) {
					stopDownloadTaskForWiFiDisconnected(nextItem.getItemId());
					t.setNotWifiProtectDetect(true);			
				} else if (t.getDownloadResult() == DownloadTask.DOWNLOAD_RESULT_INVALID_VALUE) {
					stopDownloadTask(nextItem.getItemId(), false);
				}
			}
		}
		
		mIsStoppingDownAllSongs = false;
		logger.v("onWifiDisConnected() ---> Exit");
	}

	private class onWifiDisConnectedThread implements Runnable {

		@Override
		public void run() {
//			Looper.prepare();
			onWifiDisConnected();
//			Looper.loop();
		}
	}

//	private void registerWifiAutoDownloadListener() {
//		logger.v("registerWifiAutoDownloadListener() ---> Enter");
//		
//		if (mWifiAutoDownloadReceiver == null) {
//			mWifiAutoDownloadReceiver = new BroadcastReceiver() {
//				@Override
//				public void onReceive(Context context, Intent intent) {
//					if (Cfg.mWifiAutoDownload && isWifiConnected()) {
//						Iterator<DownloadItem> it = m_list.getList().iterator();
//						while (it.hasNext()) {
//							DownloadItem nextItem = it.next();
//							if (nextItem.getStatus() == DownloadItem.PAUSED) {
//								startDownloadTask(nextItem.getItemId());
//							}
//						}	
//					}
//				}
//			};
//		}
//
//		IntentFilter iFilter = new IntentFilter(Preferences.WIFIAUTODOWNLOAD_ACTION);
//		registerReceiver(mWifiAutoDownloadReceiver, iFilter);
//		logger.v("registerWifiAutoDownloadListener() ---> Exit");		
//	}	
	
	private NotificationManager mNotificationManager;
	
	private Notification mDownloadNotification;

	private void setMoodViewUpdate(DownloadItem item, int status) {

		switch (status) {
		// start
		case 0:
			// cancel complete notification
			mNotificationManager.cancel(Cfg.DOWNLOAD_COMPLETE_NOTIFICATION_ID);
//			
//			Notification notif_start = new Notification();
//			PendingIntent contentIntent_start = PendingIntent.getActivity(this,
//					0, new Intent(this, DownloadManager.class)
//							.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
//					PendingIntent.FLAG_UPDATE_CURRENT);
//			notif_start.contentIntent = contentIntent_start;
//			notif_start.icon = R.drawable.stat_downloading;
//			RemoteViews contentView_start = new RemoteViews(getPackageName(),
//					R.layout.notify_itme);
//			contentView_start.setTextViewText(R.id.file_name, item
//					.getUnit_name());
//			contentView_start.setTextViewText(R.id.file_progress, "0%");
//			contentView_start.setImageViewResource(R.id.file_image,
//					R.drawable.stat_downloading);
//			contentView_start.setProgressBar(R.id.progress_horizontal, 100, 0,
//					false);
//			contentView_start.setTextViewText(R.id.file_size, "k");
//			notif_start.contentView = contentView_start;
//			mNotificationManager.notify((int) item.getItemId(), notif_start);
			
//			Notification notif_start = new Notification();
			PendingIntent contentIntent_start = PendingIntent.getActivity(this, 0,
					new Intent(this, DownloadManager.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
					PendingIntent.FLAG_UPDATE_CURRENT);
			mDownloadNotification.contentIntent = contentIntent_start;
			if (!mDownloadingNoficationUpdate) {
				mDownloadNotification.icon = R.drawable.stat_downloading;
				mDownloadingNoficationUpdate = true;
			} else {
				mDownloadNotification.icon = R.drawable.stat_downloading_update;
				mDownloadingNoficationUpdate = false;
			}

			long fileSizeByKB_start = item.getFileSize() / 1024 + (item.getFileSize() % 1024 == 0 ? 0 : 1);		
			String notityDetails_start ="文件大小：" + fileSizeByKB_start + "K，"
					+ "当前进度：" + "0%";
			mDownloadNotification.setLatestEventInfo(this, item.getUnit_name(),
					notityDetails_start, contentIntent_start);
			mNotificationManager.notify((int)item.getItemId(), mDownloadNotification);	
			break;
		// progress
		case 1:
//			Notification notifling = new Notification();
//			PendingIntent contentIntenting = PendingIntent.getActivity(this, 0,
//					new Intent(this, DownloadManager.class)
//							.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
//					PendingIntent.FLAG_UPDATE_CURRENT);
//			notifling.contentIntent = contentIntenting;
//			notifling.icon = R.drawable.stat_downloading;
//			RemoteViews contentView = new RemoteViews(getPackageName(),
//					R.layout.notify_itme);
//			contentView.setTextViewText(R.id.file_name, item.getUnit_name());
//			float per = (float) item.getDownload_pos() / item.getFileSize();
//			contentView.setTextViewText(R.id.file_progress, Math
//					.round(per * 100)
//					+ "%");
//			contentView.setImageViewResource(R.id.file_image,
//					R.drawable.stat_downloading);
//			contentView.setProgressBar(R.id.progress_horizontal, 100, Math
//					.round(per * 100), false);
//			long k_size = item.getFileSize() / 1024
//					+ (item.getFileSize() % 1024 == 0 ? 0 : 1);
//			contentView.setTextViewText(R.id.file_size, k_size + "k");
//			notifling.contentView = contentView;
//			mNotificationManager.notify((int) item.getItemId(), notifling);
			PendingIntent contentIntenting = PendingIntent.getActivity(this, 0,
					new Intent(this, DownloadManager.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
					PendingIntent.FLAG_UPDATE_CURRENT);
			mDownloadNotification.contentIntent = contentIntenting;
			
			if (!mDownloadingNoficationUpdate) {
				mDownloadNotification.icon = R.drawable.stat_downloading;
				mDownloadingNoficationUpdate = true;
			} else {
				mDownloadNotification.icon = R.drawable.stat_downloading_update;
				mDownloadingNoficationUpdate = false;
			}

//			long downloadSizeByKB = item.getDownload_pos() / 1024 + (item.getDownload_pos() % 1024 == 0 ? 0 : 1);
			long fileSizeByKB = item.getFileSize() / 1024 + (item.getFileSize() % 1024 == 0 ? 0 : 1);		
			float downloadPercent = (float) item.getDownload_pos() / item.getFileSize();
			String notityDetails = "文件大小：" + fileSizeByKB + "K，"
					+ "当前进度：" + Math.round(downloadPercent * 100) + "%";
			mDownloadNotification.setLatestEventInfo(this, item.getUnit_name(),
					notityDetails, contentIntenting);
			mNotificationManager.notify((int)item.getItemId(), mDownloadNotification);
			break;
		// audio file complete
		case 2:
			mNotificationManager.cancel((int)item.getItemId());
			
//			Notification notify = new Notification();
			Intent i = new Intent(this, LocalBrowser.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			i.putExtra(DN.IN_MAIN_TAB, false);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i,
					PendingIntent.FLAG_UPDATE_CURRENT);
			mDownloadNotification.contentIntent = contentIntent;
			mDownloadNotification.icon = R.drawable.stat_download_complete;
			mDownloadNotification.flags = Notification.FLAG_AUTO_CANCEL;

			mDownloadNotification.setLatestEventInfo(this, getString(R.string.app_name),
					getString(R.string.downloadcomplete), contentIntent);
			mNotificationManager.notify(Cfg.DOWNLOAD_COMPLETE_NOTIFICATION_ID, mDownloadNotification);
			break;
		// failed
		case 3:
//			Notification notify_failed = new Notification();
			PendingIntent contentIntent_failed = PendingIntent.getActivity(
					this, 0, new Intent(this, DownloadManager.class)
							.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
					PendingIntent.FLAG_UPDATE_CURRENT);
			mDownloadNotification.contentIntent = contentIntent_failed;
			mDownloadNotification.icon = R.drawable.indicator_input_error;
			mDownloadNotification.flags = Notification.FLAG_AUTO_CANCEL;

			mDownloadNotification.setLatestEventInfo(this, item.getUnit_name(),
					getString(R.string.download_fail_in_list),
					contentIntent_failed);
			mNotificationManager.notify((int) item.getItemId(), mDownloadNotification);
			break;
		// not_found
		case 4:
//			Notification notify_failed_notfound = new Notification();
			PendingIntent contentIntent_failed_notfound = PendingIntent
					.getActivity(this, 0, new Intent(this,
							DownloadManager.class)
							.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
							PendingIntent.FLAG_UPDATE_CURRENT);
			mDownloadNotification.contentIntent = contentIntent_failed_notfound;
			mDownloadNotification.icon = R.drawable.indicator_input_error;
			mDownloadNotification.flags = Notification.FLAG_AUTO_CANCEL;

			mDownloadNotification.setLatestEventInfo(this,
					item.getUnit_name(),
					getString(R.string.download_not_found),
					contentIntent_failed_notfound);
			mNotificationManager.notify((int) item.getItemId(),
					mDownloadNotification);
			break;
		case 5:
			mNotificationManager.cancel((int)item.getItemId());
			break;
		//wifi disconnected
		case 6:
//			Notification notify_wifi_disconnected = new Notification();
			PendingIntent contentIntent_wifi_disconnected = PendingIntent.getActivity(
					this, 0, new Intent(this, DownloadManager.class)
							.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
					PendingIntent.FLAG_UPDATE_CURRENT);
			mDownloadNotification.contentIntent = contentIntent_wifi_disconnected;
			mDownloadNotification.icon = R.drawable.indicator_input_error;
			mDownloadNotification.flags = Notification.FLAG_AUTO_CANCEL;

			mDownloadNotification.setLatestEventInfo(this, item.getUnit_name(),
					getString(R.string.download_wifi_disconnected),
					contentIntent_wifi_disconnected);
			mNotificationManager.notify((int) item.getItemId(), mDownloadNotification);
			break;	
			
			//
		case MOOD_VIEW_DAY_LIMIT:
//		case MOOD_VIEW_PEAK_LIMIT:
//			Notification night_limit = new Notification();
//			PendingIntent intent = PendingIntent.getActivity(this, 0, intent, flags)
			Intent intent = new Intent(this, DownloadManager.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(/*status == MOOD_VIEW_PEAK_LIMIT ? 
					DN.EXTRA_SHOW_PEAK_LIMIT : */DN.EXTRA_SHOW_DAY_LIMIT, 
					true);
			
			PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			RemoteViews views = new RemoteViews(getPackageName(),
					R.layout.multi_lines_status_bar);
			views.setImageViewResource(R.id.icon, R.drawable.indicator_input_error);
			views.setTextViewText(R.id.trackname, 
					/*status == MOOD_VIEW_PEAK_LIMIT ? Utils.getPeakTimeLimitNtfMsg() : */Utils.getDayLimitNtfMsg());
			mDownloadNotification.contentIntent = pIntent;
			mDownloadNotification.flags |= Notification.FLAG_AUTO_CANCEL;
			mDownloadNotification.contentView = views;
			mDownloadNotification.icon = R.drawable.indicator_input_error;
			mNotificationManager.notify(NIGHT_DOWNLOAD_LIMIT_TAG, mDownloadNotification);
			break;
			
		//SD eject
		case 8:
//			Notification notify_sd_eject = new Notification();
			PendingIntent contentIntent_sd_eject = PendingIntent.getActivity(
					this, 0, new Intent(this, DownloadManager.class)
							.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
					PendingIntent.FLAG_UPDATE_CURRENT);
			mDownloadNotification.contentIntent = contentIntent_sd_eject;
			mDownloadNotification.icon = R.drawable.indicator_input_error;
			mDownloadNotification.flags = Notification.FLAG_AUTO_CANCEL;

			String sdcardStatus = Environment.getExternalStorageState();
			String notifyMessage = null;
			if (sdcardStatus.equals(Environment.MEDIA_MOUNTED)
					&& !(Utils.getFreeSpace(Cfg.SDCARD_PATH) > Cfg.sdSpaceLimitation * 1024)) {
				notifyMessage = getString(R.string.download_sdcard_no_space);
			} else if (sdcardStatus.equals(Environment.MEDIA_UNMOUNTED)) {
				notifyMessage = getString(R.string.download_sdcard_unmount);
			} else {
				notifyMessage = getString(R.string.download_sdcard_remove);
			}
			
			mDownloadNotification.setLatestEventInfo(this, item.getUnit_name(), notifyMessage, contentIntent_sd_eject);
			mNotificationManager.notify((int) item.getItemId(), mDownloadNotification);
			break;
		
		// lost internet
		case 9:
//			Notification notify_no_internet = new Notification();
			PendingIntent contentIntent_no_internet = PendingIntent.getActivity(
					this, 0, new Intent(this, DownloadManager.class)
							.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
					PendingIntent.FLAG_UPDATE_CURRENT);
			mDownloadNotification.contentIntent = contentIntent_no_internet;
			mDownloadNotification.icon = R.drawable.indicator_input_error;
			mDownloadNotification.flags = Notification.FLAG_AUTO_CANCEL;

			mDownloadNotification.setLatestEventInfo(this, item.getUnit_name(),
					getString(R.string.download_no_internet),
					contentIntent_no_internet);
			mNotificationManager.notify((int) item.getItemId(), mDownloadNotification);
			break;
			
//		// install package file complete
//		case 9:
//			Notification install_notify = new Notification();
//			String fileName = Cfg.DOWNLOAD_DIR + "/" + "善听.apk";   
//			Uri uri = Uri.fromFile(new File(fileName));   
//			Intent installIntent = new Intent(Intent.ACTION_VIEW);
//			installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
//
//			PendingIntent installPendingIntent = PendingIntent.getActivity(this, 0,
//					installIntent,
//					PendingIntent.FLAG_UPDATE_CURRENT);
//			install_notify.contentIntent = installPendingIntent;
//			install_notify.icon = R.drawable.download;
//			install_notify.flags = Notification.FLAG_AUTO_CANCEL;
//
//			install_notify.setLatestEventInfo(this, item.getUnit_name(),
//					getString(R.string.install_package_downloadcomplete), installPendingIntent);
//			mNotificationManager.notify((int) item.getItemId(), install_notify);
//			break;
		}

	}

	private long preupdatatime = System.currentTimeMillis();

//	private boolean allTaskStopped() {
//		Collection<DownloadTask> tasks = m_taskMap.values();
//		for (DownloadTask task : tasks) {
//			if (task.isRunning() || task.isWaiting()) {
//				return false;
//			}
//		}
//		return true;
//	}

	private void release() {
//		logger.e("size:" + m_taskMap.size());
		if ((m_taskMap.size() == 0)) {
			releaseWifi();
			stopSelf();
		}
	}

	@Override
	public Context getAppContext() {
		// TODO Auto-generated method stub
		return getApplicationContext();
	}
	
	private void stopAllDownloadTask(DownloadItem curItem, int type){
		
		 if(m_list != null){
			 ArrayList<DownloadItem> list = m_list.getList();
			 for(int i = 0; i < list.size(); i++){
				 DownloadItem item = list.get(i);
				 
				 if(curItem == null || item.getItemId() != curItem.getItemId()){
					 stopDownloadTask(item.getItemId(), false);
				 }
			 }
			 
			 setMoodViewUpdate(curItem, type);
			 	 
		 }
			 
	}
	
	private void cancelNotification() {
		for (int i = Cfg.NOTIFICATION_ID_BEGIN; i >= Cfg.NOTIFICATION_ID_END; i--) {
			if (i != Cfg.NOTIFICATION_ID) {
				mNotificationManager.cancel(i);
			}
		}
		
		ArrayList<DownloadItem> list = m_list.getList();
		for(int i = 0; i < list.size(); i++) {
			DownloadItem item = list.get(i);
			mNotificationManager.cancel((int) item.getItemId());
		}
	}
}
