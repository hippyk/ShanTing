package tiger.unfamous.services;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.common.ApplicationGlobalVariable;
import tiger.unfamous.common.InternetStateMgr;
import tiger.unfamous.common.MyAudioManager;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.data.Book;
import tiger.unfamous.data.Play_State;
import tiger.unfamous.data.Song;
import tiger.unfamous.ui.CommonPlayUI;
import tiger.unfamous.ui.LcPlayActivity;
import tiger.unfamous.ui.Main;
import tiger.unfamous.ui.PlayActivity;
import tiger.unfamous.utils.MyLog;
import tiger.unfamous.utils.NotificationMgr;
import tiger.unfamous.utils.Utils;

public class MainService extends Service implements MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener,
		OnBufferingUpdateListener,
		OnCompletionListener {

	private static final MyLog log = new MyLog();
	public static final int TIMER_MESSAGE_CODE = 0;
	public static final int AUTO_PLAY_NEXT_MSG = 1;
//	public static final int PLAY_RETRY_TIMES = 2;
	public static final int MUSIC_VOLUME_DOWN = 3;
	public static final int CANCEL_MUSIC_VOLUME_DOWN = 4;
	public static final int CANCEL_TIMER = 5;
	public static final int UPDATE_PLAYING_STATE = 6;
	public static final int SAVE_HISTORY = 7;
	public static final int CHECK_PLAYER_WORKED = 8;
	
	
	public static final long POS_COMPLETION = Long.MAX_VALUE;
	
	public static final int PLAYER_MAX_BLOCK_TIMES = 5;
	public static String COMMAND = "command";
	public static String CMDSTOP = "CMDSTOP";
	public static String CMDTOGGLEPAUSE = "CMDTOGGLEPAUSE";
	public static String CMDNEXT = "CMDNEXT";
	public static String PREVIOUS = "PREVIOUS";

	private WifiManager.WifiLock mWifiLock;
	private PowerManager.WakeLock mWakeLock;
	
	private final MediaPlayer mPlayer = new MediaPlayer();;
	private String mPath;
//	private boolean mPlayerInitialized = false;
//	private boolean mPlayerPrepared;
	private long mPosition = -1;	
	private long mLastPosition = -1;
	
	public Activity mMainActivity;
	
	private CommonPlayUI mPlayUI;
	public boolean mUseOnlinePlayer;
	public boolean mIsMusicLocal;
	public Book mCurPlayBook;
	public Book mCurBrowseBook;
	int   mCurPlayIndex = -1;
	private MyAudioManager mMyAudioFocusManager;
	public static final int MUSIC_VOLUME_INVALID_VALUE = -1;
	private boolean mIsNeedReleaseWifi = true;
	private int mMusicVolumeBeforeVolumeDown = MUSIC_VOLUME_INVALID_VALUE;
	private int mMusicMaxVolume = MUSIC_VOLUME_INVALID_VALUE;
	
	/**
	 * 缓存在线书的历史记录的位置 key: mID, Value:历史记录的ID
	 */
	private HashMap<String, Integer > mBookHistoryCache = new HashMap<String, Integer>(10);
	
	/**
//	public void setHistory(Book his) {
//		mHistory = his;
//		mCurSongName = his.mSongName;
//		if (his.mOnline) {
//			mBrowseDir = his.mDir;
//		}
//	}

//	Toast toast;
	/**
	 * 记录当前播放的歌曲列表
	 */
	public ArrayList<Song> mCurPlayList;

	// public int mCurSongIndex = -1;
//	public String mCurSongName;
//	public int mCurPlayPage = -1;
//	public long mLastPosition;
	int mMinutesLeft = 0;

//	private Long mStartTime;

	public boolean playErr = false;
	
	private InternetStateMgr mInternetStateMgr;
	
	
	private int mPlayerBlockTimes = PLAYER_MAX_BLOCK_TIMES;
	
//	private MediaButtonIntentReceiver mReceiver;
	// private boolean mSubScore;
	// public void resetPlayInfo() {
	// mPlayDir = null;
	// mCurPlayPage = -1;
	// mCurSongName = null;
	// }

	long  	mPlayStartMillSeconds = -1;
	/**
	 * 检测耳机插入拔出
	 */
	HeadSetPlugReceiver mHeadSetReceiver = new HeadSetPlugReceiver();
	//仅适用在线播放
	public Song getCurSong() {		
		if (mCurPlayList != null && mCurPlayBook != null) {
			String songName = mCurPlayBook.mSongName;
			if (!TextUtils.isEmpty(songName)) {
				for (Song s : mCurPlayList) {
					if (songName.equals(s.getName())) {
						return s;
					}
				}
			}
			// return (Song) mCurPlayList.get(mCurSongIndex);
		}
		return null;
	}
	
	public String getCurSongName(int idx) {		
		if (mCurPlayList != null) {
			Song song = mCurPlayList.get(idx);
			if (song == null) {
				return null;
			}
			return song.getName();
		}
		return null;
	}

	public void saveHistory() {
		final Book book = (Book) mCurPlayBook.clone();
		
		
		if (book != null && book.mPosition != POS_COMPLETION) {
			book.mPosition = position();
		}
		mHandler.obtainMessage(SAVE_HISTORY, book).sendToTarget();
	}
	private void saveHistoryByBook(Book book){
		
//		Song s = getCurSong();
//		if (TextUtils.isEmpty(mCurBook.mSongName)) {
//			log.e("not playing, don't save history!");
//			return;
//		}
		if (book == null) {
			return;
		}

		String commonPath = Cfg.mInnerRootPath + Cfg.BOOKSHELF_FILE_DIR
				+ Cfg.BOOKSHELF_FILE_FOREBODY;
		
		int duplicateIdx = getSavedBookIndexByBook(book);
		

		int removeIdx = duplicateIdx;
		if (removeIdx == -1) {
			removeIdx = Cfg.COUNT_BOOK_ON_SHELF - 1;
		}
		for (int i = removeIdx - 1; i >= 0; i--) {
			String path = commonPath + i;
			File f = new File(path);
			if (f.exists()) {
				f.renameTo(new File(commonPath + (i + 1)));
			}
		}

		// Song s = getCurSong();

//		Log.v(TAG, "mLastPosition:" + mLastPosition);

//		Book history = null;
//		if (mUseOnlinePlayer) {
//			history = new Book(mPlayDir, mCurPlayPage, s.getName(),
//					mLastPosition);
//		} else {
//			history = new Book(mLcPlayDirPath, mLcPlayDirName,
//					mCurSongName, mLastPosition);
//		}

//		log.e(mCurPlayBook.mPosition);
		
		//zhaoyp: 保存历史记录改为发送message的形式执行，以加快界面响应的速度。因此保存参数book即可
		//另，在执行本函数时，有可能播放已经停止，不能使用position(),book在发送前已经获取到了position()的
		if (book != null/* && book.mPosition != POS_COMPLETION */) {
			//
//			book.mPosition = position();
			//
			SharedPreferences sp = getSharedPreferences(DN.SHORTCUT_PREF_NAME, 0);
			Editor editor = sp.edit();
			if (book.mOnline) {
				editor.putLong(DN.PREF_SHORTCUT_POSITION + book.mID,
						book.mPosition);
				editor.putInt(book.mTitle + "mCurPlayPage", book.mCurPlayPage);
				editor.putString(book.mTitle + "mSongName", book.mSongName);
			} else {
				editor.putLong(DN.PREF_SHORTCUT_POSITION + book.mLcDirPath, book.mPosition);
				editor.putString(DN.SHORTCUT_PREF_SONG_NAME + book.mLcDirPath, book.mSongName);
				
			}
			log.d("book position = " + book.mPosition + ", " + book.mSongName );
			editor.commit();
			//System.out.println("position---------->"+mCurPlayBook.mPosition);
//			log.e(mCurPlayBook.mPosition);
		}
	
		FileOutputStream fileStream = null;
		try {
			fileStream = new FileOutputStream(commonPath + 0);
			ObjectOutputStream out = new ObjectOutputStream(fileStream);
			onPrepareSaveBook(book);
			out.writeObject(book);
			out.close();
			fileStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		// resetPlayInfo();
		sendBroadcast(new Intent(Cfg.ACTION_UPDATE_HISTORY));

		
	}

	public int getSavedBookIndexByBook(Book book) {
		int duplicateIdx = -1;
		String commonPath = Cfg.mInnerRootPath + Cfg.BOOKSHELF_FILE_DIR
				+ Cfg.BOOKSHELF_FILE_FOREBODY;
		// 如果有缓存，从缓存中查找id，否则读取book文件
		String cacheKey;
		if (book.mOnline) {
			cacheKey = book.mID;
		} else {
			cacheKey = book.mLcDirPath;
		}

		if (mBookHistoryCache.containsKey(cacheKey)) {
			duplicateIdx = mBookHistoryCache.get(cacheKey);
		}

		// zhaoyp：readObject非常慢，循环读取会降低很多速度
		if (duplicateIdx == -1) {

			for (int i = 0; i < Cfg.COUNT_BOOK_ON_SHELF; i++) {
				String path = commonPath + i;
				File f = new File(path);
				try {
					if (!f.exists()) {
						continue;
					}
					// Log.v(TAG, path);
					FileInputStream fis = new FileInputStream(f);
					ObjectInputStream in = new ObjectInputStream(fis);
					Book bookToFind = (Book) in.readObject();
					if (book.equals(bookToFind)) {
						duplicateIdx = i;
						break;
					}

					in.close();
					fis.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
		
		return duplicateIdx;
	}
	
//	public static final int PLAY_STATE_PLAYING = 1, PAUSED = 2,
//			STOPPED = 3, PLAY_STATE_FINISHED = 4;

	public Play_State mPlayState;

	public boolean isPlaying() {
		return mPlayState == Play_State.STARTED;
	}

	public boolean isPlayPaused() {
		return mPlayState == Play_State.PAUSED;
	}

	public  boolean isPlayStopped() {
		return (mPlayState == Play_State.STOPPED);
	}

	private String tempStr;

	// int duration,curPosition,oldPosition;
	Resources resources;

	// private WakeLock mWakeLock;

	Object mLock = new Object();

	public void lockWifi() {
		if (mWakeLock != null && !mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}
		if (mWifiLock != null && !mWifiLock.isHeld() && mPlayUI != null && (mPlayUI instanceof PlayActivity)) {
			mWifiLock.acquire();
		}
	}

	public void releaseWifi() {
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		if (mWifiLock != null && mWifiLock.isHeld()) {
			mWifiLock.release();
			log.e("releaseWifi1");
		}
	}

	@Override
	public void onCreate() {
		log.e("onCreate!");
//		 mediaReceiver = new MediaButtonIntentReceiver(this);
		// mediaReceiver.registerAction();
		Cfg.init(this);




		// Keep wifi while idle
		if (mWifiLock == null) {
			mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
					.createWifiLock("PlayWifiLock");
		}
		 if (mWakeLock == null) {
			 mWakeLock =
			 ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
			 "PlayWakeLock");
		 }

		// Needs to be done in this thread, since otherwise
		// ApplicationContext.getPowerManager() crashes.
//		mPlayer = new MultiPlayer(this, mPlayUI);
//		mMediaPlayer=new StreamingMediaPlayer(this,mPlayUI);
//
//		mPlayer= new RealPlayer(this, mPlayUI);

		//init player
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setOnBufferingUpdateListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnErrorListener(this);
		mPlayer.setOnPreparedListener(this);
		mPlayState = Play_State.STOPPED;


		 if(mHeadSetReceiver != null){
				IntentFilter  filter = new IntentFilter();
			    filter.addAction("android.intent.action.HEADSET_PLUG");
				registerReceiver(mHeadSetReceiver, filter);
			}
		mNM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		try {
			mStartForeground = getClass().getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
			try {
				mSetForeground = getClass().getMethod("setForeground",
						mSetForegroundSignature);
			} catch (NoSuchMethodException ex) {
				throw new IllegalStateException(
						"OS doesn't have Service.startForeground OR Service.setForeground!");
			}
		}
		// Clear leftover notification in case this service previously got
		// killed while playing
		stopForegroundCompat(Cfg.PLAYBACKSERVICE_STATUS_ID);

		if (Cfg.SDK_VERSION >= 8) {	//2.2
			mMyAudioFocusManager = new MyAudioManager(this);
			mMyAudioFocusManager.requestAudioFocus();
//			mMyAudioFocusManager.registerRemoteControl();
		} else {
			TelephonyManager mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			mTelephonyMgr.listen(new TeleListener(),
					PhoneStateListener.LISTEN_CALL_STATE);
		}

		mInternetStateMgr = new InternetStateMgr(this);

		new Thread(new NotificationMgr(MainService.this), "NotificationMgr").start();
//		mPlayer= new RealPlayer(this, mPlayUI);


	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == TIMER_MESSAGE_CODE) {
				NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				if (mMinutesLeft > 1) {
//					RemoteViews views = new RemoteViews(getPackageName(),
//							R.layout.statusbar_small);
//					views.setImageViewResource(R.id.icon, R.drawable.timer);
//					views.setTextViewText(R.id.trackname, (mMinutesLeft - 1)
//							+ "分钟后停止播放");
					Notification status = new Notification();
//					status.contentView = views;
					Intent i = new Intent(getApplicationContext(),
							MainService.class);
					i.setAction(Cfg.ACTION_CANCEL_TIMER);

					PendingIntent intent = PendingIntent.getService(
							MainService.this, 0, i,
							PendingIntent.FLAG_UPDATE_CURRENT);

					status.flags |= Notification.FLAG_ONGOING_EVENT;
					status.icon = R.drawable.timer;

					status.contentIntent = intent;


					status.setLatestEventInfo(MainService.this, (mMinutesLeft - 1) + "分钟后停止播放",
							"点击取消", intent);
					nm.notify(Cfg.TIMER_STATUS_ID, status);

					mMinutesLeft--;

					if (mMinutesLeft == 1) {
						mMusicVolumeBeforeVolumeDown = Utils.getMusicVolume(MainService.this);
						mMusicMaxVolume = Utils.getMusicMaxVolume(MainService.this);
						sendEmptyMessageDelayed(MUSIC_VOLUME_DOWN, 60 * 1000);
					} else {
						sendEmptyMessageDelayed(TIMER_MESSAGE_CODE, 60 * 1000);
					}
				}
			} else if (msg.what == AUTO_PLAY_NEXT_MSG) {
				int count = Cfg.mContinuouslyPlayChaps;

				NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				if (count != Integer.MAX_VALUE && count >= 0) {

//					RemoteViews views = new RemoteViews(getPackageName(),
//							R.layout.statusbar_small);
//					views.setImageViewResource(R.id.icon, R.drawable.auto_play_next_status_bar);
//					views.setTextViewText(R.id.trackname, ("播放" + (count + 1) + " 集后停止"));
					Notification status = new Notification();
					Intent i = new Intent(getApplicationContext(),
							MainService.class);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_SINGLE_TOP);
					i.setAction(Cfg.ACTION_CANCEL_PLAY_NEXT);

					PendingIntent pendingIntent = PendingIntent.getService(
							MainService.this, 0, i,
							PendingIntent.FLAG_UPDATE_CURRENT);

//					status.contentView = views;
					status.flags |= Notification.FLAG_ONGOING_EVENT;
					status.icon = R.drawable.auto_play_next_status_bar;

					status.contentIntent = pendingIntent;
					status.setLatestEventInfo(MainService.this, count == 0 ? "本集播完后停止" :("再播 " + (count + 1) + " 集后停止"),
							"点击取消", pendingIntent);
					nm.notify(Cfg.AUTO_PLAY_NEXT_STATUS_ID, status);

				} else if (count < 0){
					Cfg.mContinuouslyPlayChaps = Integer.MAX_VALUE;

					nm.cancel(Cfg.AUTO_PLAY_NEXT_STATUS_ID);
					if (!isPlayStopped()) {
						saveHistory();
						stopMusic();
						Utils.gotoFlightMode(MainService.this);
					}
					// mPlayer.release();
					stopSelf(); // 只有没有被bind，才会真正stop
				}else {
					cancelPlayNext();
				}
			} else if (msg.what == MUSIC_VOLUME_DOWN) {
				if (mMinutesLeft == 0) { // cancel timer
					Utils.setMusicVolumeDirect(MainService.this, mMusicVolumeBeforeVolumeDown);
					if (!isPlayStopped()) {
						saveHistory();
					}
					return;
				}

				int musicVolume = Utils.getMusicVolume(MainService.this);

				if (musicVolume > (mMusicMaxVolume+1)/2) {
					Utils.setMusicVolumeByStep(MainService.this, AudioManager.ADJUST_LOWER);
					sendEmptyMessageDelayed(MUSIC_VOLUME_DOWN, 500);
				} else {
					if (!isPlayStopped() && (musicVolume == (mMusicMaxVolume+1)/2)) {
						saveHistory();
					}
					Utils.setMusicVolumeByStep(MainService.this, AudioManager.ADJUST_LOWER);

					if (musicVolume > 0) {
						sendEmptyMessageDelayed(MUSIC_VOLUME_DOWN, 500);
					}
				}

				if (musicVolume == 0) {
					if (!isPlayStopped()) {
						saveHistory();
						stopMusic();
						Utils.gotoFlightMode(MainService.this);
					}

					Utils.setMusicVolumeDirect(MainService.this, mMusicVolumeBeforeVolumeDown);
					mMinutesLeft = 0;
					NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					nm.cancel(Cfg.TIMER_STATUS_ID);
					Toast.makeText(MainService.this, "定时停止已取消！", Toast.LENGTH_SHORT).show();
					stopSelf(); // 只有没有被bind，才会真正stop
				}
			} else if (msg.what == CANCEL_TIMER) {
				mMinutesLeft = 0;
				NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				nm.cancel(Cfg.TIMER_STATUS_ID);
				Toast.makeText(MainService.this, "定时停止已取消！", Toast.LENGTH_SHORT).show();
			} else if (msg.what == CANCEL_MUSIC_VOLUME_DOWN) {
				log.e("msg.what = CANCEL_MUSIC_VOLUME_DOWN");
				mMinutesLeft = 0;
				Utils.setMusicVolumeDirect(MainService.this, mMusicVolumeBeforeVolumeDown);
				NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				nm.cancel(Cfg.TIMER_STATUS_ID);
				Toast.makeText(MainService.this, "定时停止已取消！", Toast.LENGTH_SHORT).show();
			} else if(msg.what == UPDATE_PLAYING_STATE) {
				Notification status = new Notification();
				status.flags |= Notification.FLAG_ONGOING_EVENT;
				status.icon = Cfg.IS_LEPHONE ? R.drawable.status_icon_lephone
						: R.drawable.status_play;
				Intent i = new Intent(MainService.this, mUseOnlinePlayer ? PlayActivity.class
						: LcPlayActivity.class);
				i.putExtra(DN.SHOW_PLAYING_SONG, true);
				i.putExtra(DN.FROM_NOTIFY, true);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

				PendingIntent pendingIntent = PendingIntent.getActivity(MainService.this, 0, i,
						PendingIntent.FLAG_UPDATE_CURRENT);

				status.contentIntent = pendingIntent;

				status.setLatestEventInfo(MainService.this, mCurPlayBook.mSongName,
						"点击回到播放列表", pendingIntent);

				startForegroundCompat(Cfg.PLAYBACKSERVICE_STATUS_ID, status);

			} else if(msg.what == SAVE_HISTORY){
				saveHistoryByBook((Book)msg.obj);
			} else if(msg.what == CHECK_PLAYER_WORKED){
				log.d("check Player position: " + mLastPosition + ", positon = " + position() + ",time = " + mPlayerBlockTimes);
				if(isPlaying()){
					if(mLastPosition == position()){
						mPlayerBlockTimes --;
						if(mPlayerBlockTimes < 0){
							saveHistory();
							stopMusic();
							mPlayerBlockTimes = PLAYER_MAX_BLOCK_TIMES;
							MyToast.showLong(MainService.this, "您的播放器长时间没有反应，已停止播放");
						} else {
							checkIsPlayerWorkedFine();
						}
					} else {
						mPlayerBlockTimes = PLAYER_MAX_BLOCK_TIMES;
						checkIsPlayerWorkedFine();
					}
				}

				mLastPosition = position();


			}
		}
	};

	@Override
	public void onStart(Intent intent, final int startId) {
		super.onStart(intent, startId);
		log.v("onStart!");
		if(intent == null){
			return ;
		}
		if (Cfg.ACTION_SCHEDULE_STOP.equals(intent.getAction())) {
			mMinutesLeft = intent.getIntExtra(DN.TIME_LEFT, 0);
			if (mMinutesLeft > 0) {
				mHandler.removeMessages(TIMER_MESSAGE_CODE);
				mHandler.sendEmptyMessage(TIMER_MESSAGE_CODE);
				if(Cfg.mContinuouslyPlayChaps != Integer.MAX_VALUE){
					cancelPlayNext();
				}
			}
		} else if (Cfg.ACTION_CANCEL_TIMER.equals(intent.getAction())) {
			cancelTimer();
		} else if (Cfg.ACTION_CANCEL_PLAY_NEXT.equals(intent.getAction())){
			cancelPlayNext();
		} else if (Cfg.ACTION_HEADSET_KEYEVENT.equals(intent.getAction()) && mPlayUI != null) {// 处理耳机监听
			String cmdKey = intent.getStringExtra(COMMAND);
			if (cmdKey.equals(CMDTOGGLEPAUSE)) {
				// 暂停/播放
				if(position() > 0 && duration() > 0){	//非缓冲状态
					doPauseResume();
				}
			} else if (cmdKey.equals(CMDNEXT)) {
				// 下一曲
				mPlayUI.playNextSong();
			} else if (cmdKey.equals(PREVIOUS)) {
				// 上一曲
				mPlayUI.playPreviousSong();
			}
		} else if (Cfg.ACTION_SHOW_NOTIFICATION.equals(intent.getAction())) {
			String action = intent.getStringExtra("action");
			String actiontext = intent.getStringExtra("actiontext");

			if (action.equals("SHOW_WEBPAGE_ACTION")){
				if (!actiontext.startsWith("http://")) {
					actiontext = "http://" + actiontext;
//					Log.e("MyLog", "Cfg.mNotificationActionText = " + Cfg.mNotificationActionText);

				}
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(actiontext));
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				i.setClassName("com.android.browser","com.android.browser.BrowserActivity");
				startActivity(i);
			} else {
				Intent i = new Intent(this, Main.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				Cfg.mIsClickNotification = true;
				Cfg.mNotificationAction = action;
				Cfg.mNotificationActionText = actiontext;
				startActivity(i);
			}
		}
	}

	// public void onStop() {
	// stopMusic();
	// removeShakeListener();
	// }

	private void cancelPlayNext(){
		Cfg.mContinuouslyPlayChaps = Integer.MAX_VALUE;
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(Cfg.AUTO_PLAY_NEXT_STATUS_ID);
		MyToast.showShort(this, "定集停止已取消");
	}

	public void cancelTimer() {
		if (mMinutesLeft == 0) { // already cancel
			return;
		}

		if (mMinutesLeft == 1) { // the last 1 minute
			mHandler.removeMessages(MUSIC_VOLUME_DOWN);
			mHandler.sendEmptyMessage(CANCEL_MUSIC_VOLUME_DOWN);
		} else {
			mHandler.removeMessages(TIMER_MESSAGE_CODE);
			mHandler.sendEmptyMessage(CANCEL_TIMER);
		}
	}

	@Override
	public void onDestroy() {
		log.e("onDestroy!");
		if (Cfg.SDK_VERSION >= 8) {  //2.2
//			mAudioManager.abandonAudioFocus((OnAudioFocusChangeListener) mAudioFocusListener);
			mMyAudioFocusManager.abandonAudioFocus();
//			mMyAudioFocusManager.unregisterRemoteControl();
		}

		if (mMinutesLeft > 0) {
			cancelTimer();
		}
		if (!isPlayStopped()) {
			saveHistory();
			stopMusic();
		}
		if(Cfg.mContinuouslyPlayChaps > 0 && Cfg.mContinuouslyPlayChaps != Integer.MAX_VALUE){
			cancelPlayNext();
		}
//		mPlayer.release();

		if(mHeadSetReceiver != null){
			unregisterReceiver(mHeadSetReceiver);
		}

		// if (mSubScore) {
		// long time = System.currentTimeMillis() - mStartTime;
		// int minutes = (int) (time / 1000 / 60);//
		// int timelong = Cfg.loadInt(Cfg.PREF_WY_TIMELONG_1DAY, 0) + minutes;
		// Log.e(TAG, "timelong:" + timelong);
		// Cfg.SaveInt(Cfg.PREF_WY_TIMELONG_1DAY, timelong);
		// }
	}

//	 private Object mAudioFocusListener;

	class TeleListener extends PhoneStateListener {
		boolean PlayBeforeRing = false;

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				if (PlayBeforeRing) {
					PlayBeforeRing = false;
					reSartMusic();
				}
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
			case TelephonyManager.CALL_STATE_RINGING:
				if (isPlaying()) {
					PlayBeforeRing = true;
					pauseMusic();
				}
				break;
			default:
				break;
			}
		}

	}

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public MainService getService() {
			return MainService.this;
		}
	}

	public void setPlayActivity(CommonPlayUI a) {
		if (mPlayUI != null) {
			((Activity) mPlayUI).finish();
		}
//		if(mPlayer == null){
//			mPlayer = new RealPlayer(this, a);
//		}else{
//			mPlayer.setPlayInterface(a);
//		}

		mPlayUI = a;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	public void playMusic(boolean isLocal, int songIndex, long pos,
			CommonPlayUI playUI) {
		// setPlayActivity(playUI);
		mUseOnlinePlayer = true;
//		mPlayDir = mBrowseDir;
//		mCurPlayBook.mSongName = mCurPlayList.get(songIndex).getName();
		Song song = mCurPlayList.get(songIndex);
		if (song == null) {
			MyToast.showShort(this, "很抱歉，发生内部错误，无法播放！");
			MobclickAgent.onEvent(this, Cfg.UM_EXPTION,
			"AnYueService_song_is_null");
			return;
		}

//		为mediaPlayer播放设置必要的条件
//		String extension0 = Utils.getExtention(song.getnetAddress());
//		String downPath = Cfg.DOWNLOAD_DIR + "/" + mCurPlayBook.mTitle + "/";
//		String songName=song.getName() + extension0;
//		mPlayer.setDownpathAndSongName(downPath, songName);
		if (isLocal) {
				String extension = Utils.getExtention(song.getnetAddress());
				tempStr = Cfg.DOWNLOAD_DIR + "/" + mCurPlayBook.mTitle + "/"
						+ song.getName() + extension;
			} else {
				tempStr = song.getnetAddress();
			}
//		setError(false);
		playMusic(isLocal, tempStr, pos);
	}

	public void playMusic(File lcFile, long pos, CommonPlayUI playUI) {

		// setPlayActivity(playUI);
		mUseOnlinePlayer = false;
//		File parent = lcFile.getParentFile();
//		mLcPlayDirPath = parent.getPath();
//		mLcPlayDirName = parent.getName();
//		mCurSongName = lcFile.getName();
		playMusic(true, lcFile.getPath(), pos);
	}

	private void playMusic(boolean isLocal, String path, long pos) {
		log.d(path);

		mIsMusicLocal = isLocal;

		mPlayStartMillSeconds = System.currentTimeMillis();

//		mLastPosition = 0L;
//		mCurPlayBook.mPosition = 0L;
		if (mPlayState == Play_State.STARTED || mPlayState == Play_State.PAUSED) {
			mIsNeedReleaseWifi = false;
			stopMusic();
//			mPlayer.stop();

		}

		lockWifi();
		if(!mInternetStateMgr.hasInternet() && !mIsMusicLocal){
			mPlayState = Play_State.STOPPED;
			if(mPlayUI != null){
				mPlayUI.onStateChanged(Play_State.STOPPED);
			}
			MyToast.showShort(this , R.string.noNetWork);
			gotoIdleState();
			return;
		}


		mPosition = pos;
		mPath = path;
//		mPlayerInitialized = false;

		mPlayer.reset();
		mPlayState = Play_State.STOPPED;
		if (isLocal) {
//				mPlayer.setDataSource(path, pos);
//			mPlayerPrepared = false;
			try{
				if (path.startsWith("content://")) {
					mPlayer.setDataSource(this, Uri.parse(path));
				} else {
					mPlayer.setDataSource(path);
				}
				mPlayer.prepare();
			} catch (IOException ex) {
				// TODO: notify the user why the file couldn't be opened
				ex.printStackTrace();
				return;
			} catch (IllegalArgumentException ex) {
				// TODO: notify the user why the file couldn't be opened
				ex.printStackTrace();
				return;
			}
//			mPlayerInitialized = true;
//			mPlayerPrepared = true;
			mPlayState = Play_State.PREPARING;
		} else {
			try {
//				initPlayer(path, pos, false);
				mPlayer.setDataSource(path);
				mPlayer.prepareAsync();
			} catch (IOException ex) {
				// TODO: notify the user why the file couldn't be opened
				ex.printStackTrace();
				return;
			} catch (IllegalArgumentException ex) {
				// TODO: notify the user why the file couldn't be opened
				ex.printStackTrace();
				return;
			}
//			mPlayerInitialized = true;
			mPlayState = Play_State.PREPARING;
		}



//		boolean initOK = isLocal ? mPlayerPrepared : mPlayerInitialized;
		if (mPlayState != Play_State.PREPARING) {
			Log.v("run", "playerr");
			MyToast.showLong(this, "抱歉，无法播放!");
			stopMusic();
			return;
		}

		mPlayUI.onStateChanged(Play_State.PREPARING);

		mPlayUI.updateCurPlaySongName(mCurPlayBook.mSongName);

		// setForeground(true);
		// NotificationManager nm =
		// (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

		updatePlayingNotifaction();

		checkIsPlayerWorkedFine();
	}

	public int getCurListSize() {
		return mCurPlayList.size();
	}

	private void gotoIdleState() {
		// NotificationManager nm =
		// (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// nm.cancel(PLAYBACKSERVICE_STATUS);
		stopForegroundCompat(Cfg.PLAYBACKSERVICE_STATUS_ID);
	}

	public void doPauseResume() {
		if (isPlayPaused()) {
			reSartMusic();
		} else if (isPlaying()) {
			saveHistory();
			pauseMusic();
		}
	}

	public void pauseMusic() {
		if(mPlayer !=null)
		{
			releaseWifi();

			mPlayer.pause();
			mPlayState = Play_State.PAUSED;

			if(mPlayUI!=null)
			{
				mPlayUI.onStateChanged(Play_State.PAUSED);
			}

		}
		
		
	}

	public void reSartMusic() {
		if(mPlayer !=null)
		{
			lockWifi();

			mPlayer.start();

			mPlayState = Play_State.STARTED;
			if (mPlayUI != null) {
				mPlayUI.onStateChanged(Play_State.STARTED);
			}

			if(Cfg.mContinuouslyPlayChaps != Integer.MAX_VALUE){
				updateAutoNextNotifaction();	
			}
			
			checkIsPlayerWorkedFine();
			
		}
	}

	public void stopMusic() {
		if(Cfg.mContinuouslyPlayChaps != Integer.MAX_VALUE){
			Cfg.mContinuouslyPlayChaps = Integer.MAX_VALUE;
			updateAutoNextNotifaction();
		}
		if (isPlayStopped()) {
			return;
		}
		// if (mPlayingOnline && isPlaying()) {
		// String dirName = getPlayDirName();
		// if (dirName.contains("郭德纲")) {
		// MobclickAgent.onEvent(this, "stop", mCurSongName);
		// }
		// }

//		mLastPosition = position();
//		if (mCurPlayBook != null) {
//			mCurPlayBook.mPosition = position();
//		}
		// if(!isStopped()) {

		mPlayer.stop();
		mPlayState = Play_State.STOPPED;

		if (mPlayUI != null) {
			mPlayUI.onStateChanged(Play_State.STOPPED);
		}
		// }
		if (mIsNeedReleaseWifi) {
			releaseWifi();
		}
		
		mIsNeedReleaseWifi = true;
		
		gotoIdleState();
//		calculatePlayMinutes();
	}
	
	void stopAndReleasePlayer() {
//		mRetryCount = 0;
//		mPlayer.reset();
//		mPlayerInitialized = false;
//		mPlayerPrepared = false;
		stopMusic();
		mPlayer.release();
	}

//	void calculatePlayMinutes(){
//		if(mPlayStartMillSeconds != -1L){
//			Cfg.mPlayedMinutes += (System.currentTimeMillis() - mPlayStartMillSeconds)/(1000 * 60);
//			Cfg.saveInt(this, Cfg.PREF_PLAY_MINUTES, Cfg.mPlayedMinutes);
//		}
//		mPlayStartMillSeconds = -1L;
//	}
	
	
	@Override
	public void onPrepared(MediaPlayer mp) {
//		mPlayState = Play_State.PREPARED;

		mPlayUI.setmDuration(duration());
		if (mPosition > 0L) {
			seek(mPosition);
		}else{
			seek(1);
		}

		mPlayer.start();
		mPlayState = Play_State.STARTED;
		if(mPlayUI != null) {
			mPlayUI.onStateChanged(Play_State.STARTED);
		}
	}
	
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		log.e("what:" + what + "__extra:" + extra);
		
//		if(!mPlayerInitialized && extra == 0x80000000){
//			//停止播放后，出现错误
//		//在root/frameworks/base/include/utils/Errors.h中查到unknow_error
//	    //http://android.joao.jp/2011/07/mediaplayer-errors.html 一部分错误码
//
//			return true;
//		}

		if(!isPlayStopped()){
//			if (mRetryCount < PLAY_RETRY_TIMES) {
//				log.e("retry : " + mRetryCount);
//				if (mRetryCount == 0) {
//					MyToast.showShort(this, "播放出错，正在重试...");
//				}
//				mPlayer.reset();
//				try {
//					Thread.sleep(20);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//
//				//重试
//				playMusic(mIsMusicLocal, mPath, mPosition);
//
////				//重置数据源进行播放
////				if(mIsMusicLocal){
////					//位置向后推移1s
////					mPlayer.setDataSource(mPath, mPosition + 1000);
////				}else{
////					mPlayer.setDataSourceAsync(mPath, mCachedPosition + 1000);
////				}
//
//				mRetryCount++;
//			} else {
			//TODO: 最好能有声音提示出错了
			MyToast.showShort(this, "播放失败，建议您尝试下载！");

			saveHistory();
			stopMusic();
//			}
		}
		
		return true;
	}
	
		
	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
//		Log.d("MyLog", "buffer(%):" + String.valueOf(arg1));
		if (!isPlayStopped()) {
			mPlayUI.setSecondProgress(arg1 * (CommonPlayUI.MAX_SEEK_VALUE / 100));
		}
	}

	/**
	 * 列表播放结束
	 */
	public void onFinished(){

		mCurPlayBook.mPosition = POS_COMPLETION;
		saveHistory();
		releaseWifi();
		if (Cfg.mShowListAfterFinish) {
			Intent i = new Intent(this, mUseOnlinePlayer ? PlayActivity.class
					: LcPlayActivity.class);
			i.putExtra(DN.SHOW_PLAYING_SONG, true);
			i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
					| Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}
		
//		if (mPlayUI != null) {
//			mPlayUI.onStateChanged(PLAY_STATE_FINISHED);
//		}
		
//		//kuguo ad
//		long cur = System.currentTimeMillis();
//		if(!Cfg.mAdsHidden && Math.abs(cur - Cfg.mLastPushTime) > Cfg.PUSH_INTERVAL) {
//			PushAdsManager paManager = PushAdsManager.getInstance();
//	    	paManager.setCooId(this, Cfg.DEBUG ? "f946b3d4086249a6968aabec7c752027" : "4f95b2d92a034a3dae117661dea3e54b");
//	    	paManager.receivePushMessage(this, false);
//	    	Cfg.mLastPushTime = cur;
//	    	Cfg.saveLong(this, Cfg.PREFS_LAST_PUSH_TIME, Cfg.mLastPushTime);
//		}
		
//		//计算播放时长，
//		calculatePlayMinutes();
		
		//结束播放，只消除notification，不进行提示
		if(Cfg.mContinuouslyPlayChaps != Integer.MAX_VALUE){
			Cfg.mContinuouslyPlayChaps = Integer.MAX_VALUE;
			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			nm.cancel(Cfg.AUTO_PLAY_NEXT_STATUS_ID);
		}
		if (mMinutesLeft > 0) {
			cancelTimer();
		}
	
	}
	
	@Override
	public void onCompletion(MediaPlayer arg0) {
		log.d("播放完毕");

		gotoIdleState();
		// need to do before stopMusic
		
//		boolean completeOK = true;
		//对出错做了处理，出错后也不会调用到onCompletion，正常播完后才会调用到此处
//		if(Cfg.SDK_VERSION <=7 || getError()){
//			Log.i("AnyueService", "sdk<7");
//			completeOK=true;
//		}	
//		else
//			completeOK=mPlayer.isPrepared();
		
		// String dirName = mPlayingOnline ? getPlayDirName() : mLcPlayDirName;
//		if (completeOK) {
//			if (mIsMusicLocal) {
//				MobclickAgent.onEvent(this, Cfg.UM_PLOCALOK);
//			} else {
//				MobclickAgent.onEvent(this, Cfg.UM_PONLINEOK);
//			}
//			mLastPosition = POS_COMPLETION;
//		} 
//		else {
//			mCurPlayBook.mPosition = position();
//			Song s = getCurSong();
//			if (s != null) {
//				MobclickAgent.onEvent(this, Cfg.UM_PERR, getCurSong().getnetAddress());
//			}
//		}
		boolean finish = true;
//		Log.i("AnyueSerivice","是否自动播放"+Cfg.mAutoPlayNext);
			if ((Cfg.mContinuouslyPlayChaps > 0 && Cfg.mContinuouslyPlayChaps < Integer.MAX_VALUE) || 
					(Cfg.mContinuouslyPlayChaps == Integer.MAX_VALUE 
							&& (Cfg.mAutoPlayNext || mMinutesLeft > 0 ))) {
				
				if(Cfg.mContinuouslyPlayChaps != Integer.MAX_VALUE){
					Cfg.mContinuouslyPlayChaps--;
					updateAutoNextNotifaction();
				}
				
				finish = !mPlayUI.playNextSong();
			} else if(Cfg.mContinuouslyPlayChaps <= 0){
				Utils.gotoFlightMode(MainService.this);
			}

		if (finish) {
			onFinished();
			stopAndReleasePlayer();
			
		}else {
			if(mCurPlayBook != null){
				mPlayUI.updateCurPlaySongName(mCurPlayBook.mSongName);	
			}
		}
	}

	/**
	 * Returns the duration of the file in milliseconds. Currently this method
	 * returns -1 for the duration of MIDI files.
	 */
	public long duration() {
        if(mPlayState != Play_State.PREPARING
				&& mPlayState != Play_State.STOPPED) {
        	return mPlayer.getDuration();
        }
        return -1;
	}

	/**
	 * Returns the current playback position in milliseconds
	 */
	public long position() {
//		if (!mPlayUI.currentSongSkipPlay()) {
			if(mPlayState != Play_State.PREPARING
					&& mPlayState != Play_State.STOPPED) {
				return mPlayer.getCurrentPosition();
			}     
			return -1;
//		} else {
//			return mPosition;
//		}
		
	}
	
	public void setPositon(long position) {
		mPosition = position;
	}
	
	/**
	 * Seeks to the position specified.
	 * 
	 * @param pos
	 *            The position to seek to, in milliseconds
	 */
	public long seek(long pos) {
		if(mPlayState != Play_State.PREPARING
				&& mPlayState != Play_State.STOPPED) {
			if (pos < 0) {
				pos = 0;
			}
			long dur = duration();
			if (pos > dur) {
				pos = dur;
			}
			mPlayer.seekTo((int) pos);
			return pos;
		}
		return -1;
		
	}

	

	public int getCurSongIdx() {
		
		if (mCurPlayList != null) {
			for (Song s : mCurPlayList) {
				String curSong = mCurPlayBook.mSongName;
				if (TextUtils.isEmpty(curSong) ) {
					break;
				}
				if (curSong.equals(s.getName())) {
					return mCurPlayList.indexOf(s);
				}
			}
		}
		return -1;
	}
	
	public void showPlaying(Activity act) {
		if (isPlayStopped()) {
			MyToast.showShort(act, "当前没有播放内容！");
		} else {
			Intent i = new Intent(act,
					mUseOnlinePlayer ? PlayActivity.class
							: LcPlayActivity.class);
			i.putExtra(DN.SHOW_PLAYING_SONG, true);
			act.startActivity(i);
		}
	}

	// 兼容老版SetForeground方法
	private static final Class<?>[] mSetForegroundSignature = new Class[] { boolean.class };
	private static final Class<?>[] mStartForegroundSignature = new Class[] {
			int.class, Notification.class };
	private static final Class<?>[] mStopForegroundSignature = new Class[] { boolean.class };

	private NotificationManager mNM;
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private final Object[] mSetForegroundArgs = new Object[1];
	private final Object[] mStartForegroundArgs = new Object[2];
	private final Object[] mStopForegroundArgs = new Object[1];

	void invokeMethod(Method method, Object[] args) {
		try {
			method.invoke(this, args);
		} catch (InvocationTargetException e) {
			// Should not happen.
			Log.w("ApiDemos", "Unable to invoke method", e);
		} catch (IllegalAccessException e) {
			// Should not happen.
			Log.w("ApiDemos", "Unable to invoke method", e);
		}
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			invokeMethod(mStartForeground, mStartForegroundArgs);
			return;
		}

		// Fall back on the old API.
		mSetForegroundArgs[0] = Boolean.TRUE;
		invokeMethod(mSetForeground, mSetForegroundArgs);
		mNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	 void stopForegroundCompat(int id) {
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
//				Log.w(TAG, "Unable to invoke stopForeground", e);
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// Should not happen.
//				Log.w(TAG, "Unable to invoke stopForeground", e);
				e.printStackTrace();
			}
			return;
		}
		// Fall back on the old API. Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		mNM.cancel(id);
		mSetForegroundArgs[0] = Boolean.FALSE;
		invokeMethod(mSetForeground, mSetForegroundArgs);
	}
	
	/**
	 * the kb has been read
	 */
//	public long getTotalKbRead()
//	{
//		return mPlayer.getPlayedStreamingSizeInKB();
//	}
	/**
	 * the song length
	 */
//	public long getTotalKb()
//	{
//		return mPlayer.getTotalStreamingSizeInKB();
//	}
	
	/**
	 * 更新数据源的方法,2.1以下为自己实现的边下边播，隔一段时间需要更新数据源
	 */
//	public void updateDataSource()
//	{
//		mPlayer.updateStreamingDataSource();
//	}
	

//	
//	public void finish()
//	{
//		mCurPlayBook.mPosition = POS_COMPLETION;
//		saveHistory();
//		releaseWifi();
//		if (Cfg.mShowListAfterFinish) {
//			Intent i = new Intent(this, mUseOnlinePlayer ? PlayActivity.class
//					: LcPlayActivity.class);
//			i.putExtra(DN.SHOW_PLAYING_SONG, true);
//			i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
//					| Intent.FLAG_ACTIVITY_NEW_TASK);
//			startActivity(i);
//		}
//	}
	
//	public boolean getError()
//	{
//		if(mPlayer !=null)
//			return mPlayer.isError();
//		return false;
//	}
//	public void setError(boolean error)
//	{
//		if(mPlayer !=null)
//			mPlayer.setError(error);
//	}

	public boolean isMusicLocal() {
		return mIsMusicLocal;
	}

//	public void setMusicLocal(boolean mIsMusicLocal) {
//		this.mIsMusicLocal = mIsMusicLocal;
//	}

//	public boolean downingPlayerIsPlay()
//	{
//		if(mPlayer !=null)
//			return mPlayer.isDowningPlaying();
//		return false;
//	}
	/**
	 * 判断是否有网络
	 * @return
	 */
//	public  boolean hasInternet() {
//		ConnectivityManager con = (ConnectivityManager) this.getSystemService(Activity.CONNECTIVITY_SERVICE);
//		if (con == null) {
//			return true;
//		}
//		boolean wifi = con.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
//				.isConnectedOrConnecting();
//		boolean internet = con.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
//				.isConnectedOrConnecting();
////		System.out.println("有无wifi：" + wifi + ",,有无gprs：" + internet);
//		return (wifi || internet);
//	}
	
	/**
	 * 获取缓存文件
	 */
//	public File getTempFile(){
//		if(mPlayer !=null){
//			return mPlayer.getTmepFile();
//		}
//		return null;
//	}
	
	/**
	 * 判断downingPlayer是否在加载中
	 */
//	public boolean isloading()
//	{
//		if(mPlayer !=null){
//			return mPlayer.isLoading();
//		}
//		return false;
//	}
//	public void setLoading(boolean load)
//	{
//		if(mPlayer !=null)
//			mPlayer.setLoading(load);
//	}
//	/**
//	 * 重新加载的方法
//	 */
//	public void refresh(boolean tryAgain,int pos)
//	{
//		if(mPlayer !=null){
//			mPlayer.refresh(tryAgain,pos);
//		}
//	}
	
	public void stopPlay(){
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
//				Log.w(TAG, "Unable to invoke stopForeground", e);
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// Should not happen.
//				Log.w(TAG, "Unable to invoke stopForeground", e);
				e.printStackTrace();
			}
			return;
		}
	}
	
	/**
	 * 获取尝试的次数
	 */
//	public int getTryTime(){
//		if(mPlayer !=null){
//			return mPlayer.getTryTime();
//		}
//		return 0;
//	}
	
	public boolean hasNextSong(){
		if(mCurPlayBook.mCurPlayPage < mCurPlayBook.mPageCount){
			return true;
		}
		if(mCurPlayList != null ){
			return getCurSongIdx() < getCurListSize() - 1;
		}
		if(mIsMusicLocal && mCurPlayBook != null 
				&& mCurPlayBook.mLcDirPath != null && mCurPlayBook.mSongName != null){
			
			ArrayList<File> mFiles = ApplicationGlobalVariable.getmFiles();
			Book mCurBook = ApplicationGlobalVariable.getmCurBook();
			if(mFiles != null && mCurBook != null){
				int index = -1;
				for (File f : mFiles) {
					if (f.getName().equals(mCurBook.mSongName)) {
						index = mFiles.indexOf(f);
					}
				}
				if (index  >= 0 && index < mFiles.size() - 1) {
					return true;
				}
			}
		}
		return false;
	}
	public void updateAutoNextNotifaction(){
		mHandler.sendEmptyMessage(AUTO_PLAY_NEXT_MSG);
		if(mMinutesLeft > 0){
			cancelTimer();	
		}
		
	}
	
	public void checkIsPlayerWorkedFine(){
		mHandler.removeMessages(CHECK_PLAYER_WORKED);
		mHandler.sendEmptyMessageDelayed(CHECK_PLAYER_WORKED, 60 * 1000);
	}
	
	public void updatePlayingNotifaction(){
//		RemoteViews views = new RemoteViews(getPackageName(),
//				R.layout.statusbar);
//		views.setImageViewResource(R.id.icon,
//				Cfg.IS_LEPHONE ? R.drawable.icon_lephone : R.drawable.icon);
//		views.setTextViewText(R.id.trackname, mCurPlayBook.mSongName);
		mHandler.sendEmptyMessage(UPDATE_PLAYING_STATE);
		
	}
	
	private void onPrepareSaveBook(Book book){
		if(book.mSongSizeMap != null ){
			Iterator<Integer> keys = book.mSongSizeMap.keySet().iterator();
			if(keys != null){
				while(keys.hasNext()){
					int i = keys.next();
					if(i < book.mCurPlayPage){
						keys.remove();
					}
				}
				
			}
			
		}
		if(book.mSongStringsMap != null){
			Iterator<Integer> keys = book.mSongStringsMap.keySet().iterator();
			if(keys != null){
				while(keys.hasNext()){
					int i = keys.next();
					if(i < book.mCurPlayPage){
//						book.mSongStringsMap.remove(i);
						keys.remove();
					}
				}
				
			}
		}
	}
	
	public void setNeedReleaseWifi(boolean isNeedReleaseWifi) {
		mIsNeedReleaseWifi = isNeedReleaseWifi;
	}
	
	public boolean isTimerRunning() {
		if (mMinutesLeft > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public void setBookHistoryCache(String key, int historyID){
		if(key == null){
			return ;
		}
		mBookHistoryCache.put(key, historyID);
	
	}
	
	class HeadSetPlugReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent != null){
				String action = intent.getAction();
				if(action != null && action.equalsIgnoreCase("android.intent.action.HEADSET_PLUG")
						&& intent.hasExtra("state")){
					if(intent.getIntExtra("state", 0)== 0 && isPlaying()){ //拔出耳机时，暂停播放
						doPauseResume();
	                }  
				}
			}
		}
		
	}
	
}
