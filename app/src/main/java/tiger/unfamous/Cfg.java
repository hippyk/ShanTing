package tiger.unfamous;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;

import tiger.unfamous.common.InternetStateMgr;
import tiger.unfamous.utils.NotificationMgr;
import tiger.unfamous.utils.Utils;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
@SuppressLint("NewApi")
public class Cfg {
	private static final String TAG = "Cfg";

	public static final boolean DEBUG = true;
	public static final int SDK_VERSION =Integer.parseInt(android.os.Build.VERSION.SDK);
	public static final int BOUND_VERSION = /*Cfg.DEBUG ? 12 : */8;
	// public static boolean IS_MM;
	// public static boolean IS_MOTO;
	// public static boolean IS_IANDROID;
	public static final int SIZE_1K = 1024;
	public static final String DB_NAME = "download.db";
	public static final int DB_VERSION = 11;

	public static final int screenWidth = 320;
	public static final int screenHeight = 480;
	public static final long sdSpaceLimitation = 1024 * 2;
	public static final long MS_PER_DAY = 24 * 3600 * 1000L; 
	public static final long MS_PER_HOUR = 3600 * 1000L; 

	public static final String PREFERENCE_NAME_BROWSE = "browsePages";
	
	public static String mInnerRootPath;
	public static final String SDCARD_PATH = Environment
			.getExternalStorageDirectory().getAbsolutePath();
	public static String DOWNLOAD_DIR;// = SDCARD_PATH + "/feizhuming/";
	public static String OLD_FILE_PATH;

	public static final int SONGS_PER_PAGE = 30;
	public static final int LIST_LINE_SEG_COUNT = 5;
	public static final int FIRST_PAGE = 1;
//	public static final int TOP_CHILD_COUNT = 8;
	public static final int DEF_PAGE_COUNT = 1;
	public static final int COUNT_BOOK_ON_SHELF = 15;
	
	public static final int WELCOME_SCREEN_LAST_TIME = 500;
	
	public static final int HISTORY_VERSION_1 = 99;

	public static final int HISTORY_VERSION_2 = 115;
	
	public static final int HISTORY_VERSION_MODIFY_DEFAULT_DOWNLOAD_DIRECTROY = 118;
	
	public static final int HISTORY_VERSION_DELETE_FINISHED_DOWNLOAD_ITEMS = 118;
	// public static long CHECK_UPDATE_INTERVAL = 24 * 60 * 60 * 1000L; // 24
	// hour
	
	public static final int HOST_TYPE_3G = 0;
	public static final int HOST_TYPE_TEST = 2;
	public static final int HOST_TYPE_TEMP = 1;
	public static final int HOST_TYPE_COM = 3;
	
	public static int   mHostType = HOST_TYPE_3G;

	public static final String WEB_HOME_TEST = "file:///android_asset/html/book.html";
	public static final String WEB_HOME_TEMP = "http://temp.shanting.mobi";
	public static final String WEB_HOME_3G = "http://3g.shanting.mobi";
	public static final String WEB_HOME_COM = "http://3g.shantingshu.com";   //mobi出错，则会尝试com
	
	public static String WEB_HOME = /* Cfg.DEBUG ? WEB_HOME_TEST	: */WEB_HOME_3G;
	public static String WEB_HOME_NOPIC = WEB_HOME + "/index.htm";
	public static String WEB_HOME_HASPIC = WEB_HOME + "/index.html";
//			: "http://3g.shanting.mobi/index.htm";
	
//	public static final String WEB_HOME = /*Cfg.DEBUG ? "http://test.shanting.mobi" : */"http://3g.shanting.mobi";
	public static final String BBS_URL = "http://bbs.shanting.mobi";
//	public static final String WEB_HOME = "http://vm-192-168-14-152.shengyun.grandcloud.cn/";
	public static String FAVORITE_URL = WEB_HOME + "/e/extend/shoucang/shoucangjia.html";
	public static String BOOK_QUERY_URL = WEB_HOME + "/e/extend/interface/bookquery.php";
	//public static final String WEB_HOME = "local.shanting.mobi";
	public static String FAVORITE_URL_NOPIC = WEB_HOME
	+ "/e/extend/shoucang/shoucangjiawt.html";
	public static String GET_MUSIC_PATHS_URL = WEB_HOME + "/e/extend/interface/musicpath.php";
	public static String FAQ_URL = WEB_HOME + "/about/question.html";
	
	public static final String LISTS_ROOT = "";
	// public static final String DIR_LIST_URL = LIST_URL + "/resource3";
	// public static final String DIR_LIST_URL = LIST_URL + "/resource_ceshi";
//	public static final String GONG_GAO_ADDRESS = "gonggao";
//	public static final String GONG_GAO_URL = "http://" + HOST + "/others/"
//			+ GONG_GAO_ADDRESS;
	public static final long PUSH_INTERVAL = 7 * 24 * 60 * 60 * 1000L; // 7 days;
//	public static final String PREFS_LAST_PUSH_TIME = "pref_last_push_time";
//	public static final String PREFS_LAST_PLAY_SONG = "pref_last_play_song";
	public static final String RET_SYMBOL_DOS = "\r\n";
	public static final String RET_SYMBOL_UNIX = "\n";
	public static final String DIVIDER = "/";
	public static final String LIST_FILE_NAME = "list";
//	public static final String DEBUG_LIST_FILE_NAME = "list_debug";

	public static final String HISTORY_FILE_DIR = "/history";
	public static final String HISTORY_FILE_FOREBODY = "/obj_history_";
	
	public static final String BOOKSHELF_FILE_DIR = "/book";
	public static final String BOOKSHELF_FILE_FOREBODY = "/obj_book_";

	public static String UM_EXPTION = "exception";
	public static String UM_DERR = "downloaderr";
	public static String UM_DLT_ERR = "dlterr";
	public static String UM_DOK = "dok";
//	public static String UM_PONLINEERR = "ponlineerr";
	public static String UM_PONLINEOK = "ponlineok";
	public static String UM_PLOCALOK = "plocalok";
//	public static String UM_PERR = "playerr";
	public static String UM_SHARE = "fenxiang";
	public static String UM_SHARE1 = "share1";
	public static String UM_SHARE2 = "share2";
	public static String UM_BC_GUANGGAO = "BC_YouGuangGao";
	public static String UM_BC_QITA = "BC_QiTa";
	public static String UM_BC_NEIRONG = "BC_NeiRongBuFu";
	public static String UM_BC_YINLIANG = "BC_YinLiang";
	public static String UM_BC_YINZHI= "BC_YinZhi";
	public static String UM_NOTIFICATION_COUNT = "ntfCount";
	public static String UM_WX_SHARE_CLICK = "WX_share_click";
	public static String UM_WX_SHARE_DONE = "WX_share_done";
	public static String UM_VIP_PLAY_ALL = "vip_p_all";
	public static String UM_VIP_PLAY_VIP = "vip_p_vip";

	
	
	public static long mLastPayScoreTime;
//	public static boolean mFirstStartup;
	public static boolean mInitialized;
	public static boolean IS_LEPHONE;
//	public static boolean IS_SAMSUNG;
	public static boolean IS_WITHAD;	//部分渠道禁止加广告
	public static boolean IS_HIAPK;
//	public static boolean mNextPageTipShown = false;
//	public static long mLastPushTime;
	public static int mTipShownVer;
	public static boolean mAutoPlayNext;
	public static boolean mShowListAfterFinish;
	public static boolean mNotWifiProtect;
	public static boolean mWifiAutoDownload;
	// 是否使用无图模式
	public static boolean mWebUsingNoPicMode;

	// 是否向用户提示过无图模式
	public static boolean mHasNoticedNoPicMode;
	
	// 网络状�?
	public static int mInternetState;
	
	// 通知ID
	public static int mNotificationId;

	public static final String PREF_TIP_SHOWN_VERSION = "preftipshownversion";
	//书架历史记录版本
	public static final String PREF_HISTORY_VERSION = "prefHistoryVersion";

	public static final String PREF_AUTO_PLAY_NEXT = "pref_auto_play_next";
	public static final String PREF_SHOW_LIST_AFTER_FINISH = "pref_show_list_after_finish";
	public static final String PREF_LAST_HOUR = "prefdefhour";
	public static final String PREF_LAST_MINUTE = "prefdefminute";
	public static final String PREF_NOT_WIFI_PROTECT = "pref_not_wifi_protect";
	public static final String PREF_WIFI_AUTO_DOWNLOAD = "pref_wifi_auto_download";
	// 书城无图模式
	public static final String PREF_WEB_NOPIC_MODE = "pref_web_nopic_mode";
	
	// 网络状�?
	public static final String PREF_INTERNET_STATE = "pref_internet_state";
	public static final String PREF_HOST_TYPE = "pref_host_type";
	public static final String PREF_MAX_DOWNLOAD_TASKS = "max_download_task";
	public static final String PREF_CURRENT_DOWNLOADED_TASKS = "cur_downloaded_task_count";
	public static final String PREF_DOWNLOADED_TASKS_BY_DAY = "downloaded_tasks_by_day";
	public static final String PREF_LAST_DOWNLOAD_LIMIT_DATE = "last_limit_date";
	public static final String PREF_LAST_DOWLOAD_DAY_LIMIT_DATE = "last_day_limit_date";
	
	public static final String PREF_ACCOUNTS_BIND_AWARD_FLAGS = "bind_award_flags";
	
	// 通知ID
	public static final String PREF_NOTIFICATION_ID = "pref_notification_id";
	
	public static final String PREF_NIGHT_MODE = "night_mode_flag";
	
	public static final String PREF_SIMPLE_BOOKS_DATA = "simple_books_data";
	
//	public static final String PREF_DOWNLOAD_CHARGE_LIST = "download_charge_list";
	public static final String PREF_AWARD_FOLLOW_QQ = "awardFollowQQ";
	public static final String PREF_AWARD_FOLLOW_SINA = "awardFollowSina";
//	public static final String PREF_PLAY_CHARGE_LIST = "play_charge_list";
//	public static final String PREF_PLAYED_CHAPTERS_LIST = "played_chapters_list";
	public static final String PREF_FLIGHT_MODE_AFTER_FINISH = "pref_flight_mode_after_finish";
	
	// wiyun pang hang bang
	// public static final String PREF_WY_TIMELONG_1DAY = "prefwytimelong1day";
	// public static final String PREF_WY_LAST_SUBMIT_DAY =
	// "prefwylastsubmitday";
	// public static final String PREF_WY_SUB_SCORE = "pref_wy_sub_score";

	public static final String ACTION_UPDATE_HISTORY = "tiger.unfamous.action.updatehistory";
	public static final String ACTION_SCHEDULE_STOP = "tiger.unfamous.action.schedulestop";
	public static final String ACTION_CANCEL_TIMER = "tiger.unfamous.action.canceltimer";
	public static final String ACTION_CANCEL_PLAY_NEXT = "tiger.unfamous.action.cancelplaynext";
	public static final String ACTION_FEED_BACK = "tiger.unfamous.action.feedback";
	public static final String ACTION_SHOW_PLAYING = "tiger.unfamous.action.showplaying";
	public static final String ACTION_EXIT = "tiger.unfamous.action.EXIT";
	public static final String ACTION_BACK_HOME = "tiger.unfamous.action.BACKHOME";
	public static final String ACTION_HEADSET_KEYEVENT = "tiger.unfamous.action.MEDIA.BUTTON";
	public static final String ACTION_SHOW_FAVORITE = "tiger.unfamous.action.SHOW.FAVORITE";
	public static final String ACTION_CHECK_STOP_SERVICE = "tiger.unfamous.action.checkstopservice";
	public static final String ACTION_CONNECTIVITY_CHANGE = "tiger.unfamous.action.CONNECTIVITY.CHANGE";
	public static final String ACTION_CONNECTIVITY_WIFICONNECTED = "tiger.unfamous.action.WIFICONNECTED";
	public static final String ACTION_CONNECTIVITY_WIFIDISCONNECTED = "tiger.unfamous.action.WIFIDISCONNECTED";
	public static final String EXTRA_KILL_PROCESS = "tiger.unfamous.extra.kill.process";
	public static final String ACTION_SHOW_NOTIFICATION = "tiger.unfamous.action.SHOWNOTIFICATION";
	
	
//	public static final int HIDDEN_GUANGGAO_NEED_SCORE = 1500;

//	public static final String NEXT_PAGE_TIP_SHOWN = "nextpagetipshown";
//	public static final String FIRST_STARTUP_STR = "firststarup";
//	public static final String LAST_PUSH_TIME = "lastpushtime";
	
	// QQ登陆相关
	public static final String PREF_QQ_ACCESS_TOKEN = "qToken";
	public static final String PREF_QQ_OPEN_ID = "qOpenID";
	public static final String PREF_QQ_USING_MAINSITE_URL = "usingMainSiteUrl";
	public static final String PREF_QQ_NICK_NAME = "qNickName";
	
	// 新浪登录相关
	public static final String PREF_WEIBO_ACCESS_TOKEN = "sinaToken";
	public static final String PREF_WEIBO_UID = "sinaUID";
	public static final String PREF_WEIBO_NICK_NAME = "sinaNickName";
//	public static final String PREF_WEIBO_EXPIRES_IN = "sinaExpiresIn"; // 单位是秒
//	public static final String PREF_WEIBO_AUTHORIZATION_TIME="sinaAuthorizationTime";
	public static final String PREF_WEIBO_EXPIRES_TIME = "sinaExpiresTime"; // 过期时间

	public static final String PREF_HAS_NOTICED_NOPIC_MODE = "notice_nopic_mode";

	//记录收藏夹数据的版本
	public static final String PREF_FAVORITE_DATABASE_VERSION = "favorite_database_version";
	

	public static final String ACCOUNT_INIT = "accountinit";
	public static final String PREF_USER_ID	= "userID";
//	public static final String PREF_REGISTERED_IMEI	= "registeredImei";
	public static final String PREF_CHECK_CDMA_IMEI	= "checkedCDMAImei";
	public static final String IS_VIP = "isvip";
	
	
	public static final long EXIT_SPACE_TIME = 2000;
	
	public static final String LAST_PAY_SCORE_TIME = "lastPayScoreTime";
	public static final int HIDDEN_MONTH_GUANGGAO_NEED_SCORE = 150;
	public static final int ADS_HIDDEN_DAYS = 30;
	public static boolean focusLosed=false; 
	
	
	/**
	 * 下载收费的书籍的ID列表以及它所对应的价
	 */
//	public static HashMap<String, Integer> mSoldBookPrice = new HashMap<String, Integer>(2);
	
	/**
	 * key: bookId
     * value : int[2] playing price and other free chapters count
     *
	 */
//	public static HashMap<String, int[]> mPlayChargedBookPrice = new HashMap<String, int[]>(2);
	
	/**
	 * key: bookId
     * value: chapters which has been chargred
	 */
//	public static HashMap<String, String> mPlayedChapters = new HashMap<String, String>(5);
	
	public static final String SERVICE_EXPIRE_TIME = "serviceExpireTime";

	
	public static final int FAVORITE_DATABASE_VERSION = 113;
	
	//记录收藏夹数据库的版本，以后如果还有更新，根据版本号进行更新
	public static int mFavoriteDatabaseVersion;
	public static int mHistoryVersion = 0;
	
	/**
	 * 标志用户的信
	 * 1.imei
	 * 2.wifi的mac地址mac+ 
	 */
	public static String mUserID;
	
	/**
	 * CDMA用户由于之前获取的MEID不正确，注册时都使用了mac地址
	 * 这部分用户使用积分墙的时候，不一定能获取到积分
	 */
	public static String mOldUserID;
	
	/**
	 * 是否已经联网初始化帐户信息
	 */
	public static boolean mAccountInited;
	
	public static boolean mIsVIP;
//	public static boolean mRegisteredImei;
	
	/**
	 * 服务的过期时
	 */
	public static long mServiceExpireTime;
	
	/**
	 * 是否已经提醒过用户评星
	 */
	public static boolean mRateTipsShown;
	public static final String  PREF_RATE_TIPS_SHOWN = "ratetipsshown";
	
	/**
	 * 用户是否已经评过星级
	 */
	public static boolean mHasUserRated;
	public static final String  PREF_USER_HAS_RATED = "hasRated";
	/**
	 * 启动次数
	 */
	public static int mAppStartTimes;
	public static final String  PREF_APP_START_TIMESA = "startTimes";
	
//	public static int mPlayedMinutes;
//	public static final String PREF_PLAY_MINUTES = "playedMinutes";
	
	public static int mContinuouslyPlayChaps = Integer.MAX_VALUE;
	public static final String PREF_CONTINOUSLY_CHAPS = "continouslyChaps";
	
//	public static final int MAX_DONWLOAD_TASKS_IN_PEAR_TIME = 20;
	
	public static final int MAX_DONWLOAD_TASKS_IN_DAY = 10;
	
	
//	public static int mCompletedTaskInPeakTimes = 0;
	
	public static int mCompletedTaskInOneDay = 0;
	
	public static long mLastPeakLimitMills = 0;
	
	public static long mLastDayLimitMills = 0;
	
	
	/**
	 * 绑定账号奖励标志
	 */
	public static int mAwardFalgs;

	// 通知栏相关的ID
	public static final int NOTIFICATION_ID_BEGIN = Integer.MAX_VALUE;
	public static final int DOWNLOAD_COMPLETE_NOTIFICATION_ID = NOTIFICATION_ID_BEGIN;
	public static final int NOTIFICATION_ID = DOWNLOAD_COMPLETE_NOTIFICATION_ID -1;
	public static final int PLAYBACKSERVICE_STATUS_ID = NOTIFICATION_ID - 1;
	public static final int TIMER_STATUS_ID = PLAYBACKSERVICE_STATUS_ID - 1;
	public static final int AUTO_PLAY_NEXT_STATUS_ID = TIMER_STATUS_ID - 1;
	public static final int NOTIFICATION_ID_END = AUTO_PLAY_NEXT_STATUS_ID;
	
	
	// 通知机制
	public static boolean mIsClickNotification = false;
	public static String mNotificationAction = null;
	public static String mNotificationActionText = null;
	
	public static final int RATE_STAR_CONDITION_START_TIMES = DEBUG ? 2 : 200;
	public static final int AWARD_RATE_STAR_POINTS = 60;
	
	public static boolean mIsNightMode = false;
	
	public static final int DOWNLOAD_COMMON_PRICE = 0;
	
	public static final int DOWNLOAD_VIP_PRICE = Integer.MIN_VALUE;
	
	public static boolean mHasFollowedQQ = false;
	public static boolean mHasFollowedSina = false;
	
	/**
	 * enter filght mode after playing finish
	 */
	public static boolean mToFlightModeAfterFinish = false;
	
	public enum UpdateState {
		IN_SERIES , DISCONTINUE, END
	};
	
	public static void init(Context ctx) {
		if (mInitialized) {
			return;
		}
		
//		ShengMengSdk.smInit(ctx, "126ba6600eb772f558bbc7c449883b48"); 
		if (Utils.shouldShowPopAd()) {
			AppConnect.getInstance("8a8f5d16dbc458d39617aceecba245d4", "", ctx).initPopAd(ctx);
		}
		
		// 判断版本号  如果2.3以下不执行以下方法 避免3.0以上版本出现android.os.NetworkOnMainThreadException错误
		if (Cfg.SDK_VERSION >= Build.VERSION_CODES.GINGERBREAD) {
			android.os.StrictMode.setThreadPolicy(new android.os.StrictMode.ThreadPolicy.Builder()
							.detectDiskReads().detectDiskWrites()
							.detectNetwork() // 这里可以替换为detectAll()
							.build());
			android.os.StrictMode.setVmPolicy(new android.os.StrictMode.VmPolicy.Builder()
							.detectLeakedSqlLiteObjects() // 探测SQLite数据库操作
							.penaltyDeath()
							.build());
		}
		
		
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		mServiceExpireTime = loadLong(ctx, Cfg.SERVICE_EXPIRE_TIME, 0L);
//		mRegisteredImei = loadBool(ctx, PREF_REGISTERED_IMEI);
		mRateTipsShown = loadBool(ctx, PREF_RATE_TIPS_SHOWN);
		mAccountInited = loadBool(ctx, ACCOUNT_INIT);
		mIsVIP = loadBool(ctx,IS_VIP);
		
		if(mServiceExpireTime <= 0 && !mAccountInited){
			
			mLastPayScoreTime = loadLong(ctx, Cfg.LAST_PAY_SCORE_TIME, 0L);
			
			if(mLastPayScoreTime > 0){
				mServiceExpireTime = mLastPayScoreTime + ADS_HIDDEN_DAYS * MS_PER_DAY;
				saveLong(ctx, Cfg.SERVICE_EXPIRE_TIME, mServiceExpireTime);
				saveLong(ctx, Cfg.LAST_PAY_SCORE_TIME, 0L);
				
				mLastPayScoreTime = 0L;
				if(mSharedPreferences != null){
					mSharedPreferences.edit().remove(Cfg.LAST_PAY_SCORE_TIME);
				}
			}
		}
		
//		mFirstStartup = loadBool(FIRST_STARTUP_STR, true);

		Resources res = ctx.getResources();
		IS_LEPHONE = res.getBoolean(R.bool.is_lephone);
//		IS_SAMSUNG = res.getBoolean(R.bool.is_samsung);
		IS_WITHAD = res.getBoolean(R.bool.is_withad);
		IS_HIAPK = res.getBoolean(R.bool.is_hiapk);


		// Time t = new Time();
		// t.setToNow();
		// TODAY = t.monthDay;
		// IS_MM = res.getBoolean(R.bool.is_mm);
		// IS_MOTO = res.getBoolean(R.bool.is_moto);
		// IS_IANDROID = res.getBoolean(R.bool.is_iandroid);
//		UM_EXPTION += Utils.getVersionCode(ctx);


//		mLastPushTime = loadLong(ctx, PREFS_LAST_PUSH_TIME, 0L);
//		mNextPageTipShown = loadBool(ctx,NEXT_PAGE_TIP_SHOWN);

		mTipShownVer = loadInt(ctx,PREF_TIP_SHOWN_VERSION, 0);
		mHistoryVersion = loadInt(ctx, PREF_HISTORY_VERSION, -1);
		if(mHistoryVersion <= 0){
			mHistoryVersion = mTipShownVer;
		}
		loadSetting(ctx);
		mInnerRootPath = ctx.getFilesDir().getAbsolutePath();
		if (mInnerRootPath.endsWith("/")) {
			mInnerRootPath = mInnerRootPath.substring(0, mInnerRootPath
					.length() - 1);
		}

		Log.v(TAG, mInnerRootPath);

		File folder = new File(mInnerRootPath + BOOKSHELF_FILE_DIR);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		// modify "ShanTing" to "善听"
		String newDownloadDir = null;
		if (IS_LEPHONE) {
			OLD_FILE_PATH = SDCARD_PATH + "/download/ShanTing";
			newDownloadDir = SDCARD_PATH + "/download/善听";
		} else {
			OLD_FILE_PATH = SDCARD_PATH + "/ShanTing";
			newDownloadDir = SDCARD_PATH + "/善听";
		}

		File oldFolder = new File(OLD_FILE_PATH);
		folder = new File(newDownloadDir);
		if (oldFolder.exists()) {
			oldFolder.renameTo(folder);
		} else if (!folder.exists()) {
			folder.mkdirs();
		}
		// Init download directory
		DOWNLOAD_DIR = loadStr(ctx, "mCurrentDir", SDCARD_PATH + "/善听");
		
		
		
		mHasNoticedNoPicMode = loadBool(ctx, PREF_HAS_NOTICED_NOPIC_MODE, false);
		// mWebUsingNoPicMode默认为false，如果用户手动开启过无图模式，则网�?慢也不再提示
		if (!mHasNoticedNoPicMode && mWebUsingNoPicMode) {
			mHasNoticedNoPicMode = true;
			saveBool(ctx, PREF_HAS_NOTICED_NOPIC_MODE, true);
		}
		mFavoriteDatabaseVersion = loadInt(ctx, PREF_FAVORITE_DATABASE_VERSION, 0);
		
		mInternetState = loadInt(ctx, PREF_INTERNET_STATE, InternetStateMgr.INVALID_STATE);
		mNotificationId = loadInt(ctx, PREF_NOTIFICATION_ID, NotificationMgr.INVALID_NOTIFICATION_ID);
		
		mUserID = loadStr(ctx, PREF_USER_ID, "");
		boolean checkedCDMA = loadBool(ctx, PREF_CHECK_CDMA_IMEI);
		
//		String androidID = Secure.getString(ctx.getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
		
		if(mUserID == null || mUserID.length() <= 0){
			TelephonyManager telephonyManager=(TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
			String deviceID = telephonyManager.getDeviceId();
			
			WifiManager wifi = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);  
		    WifiInfo info = wifi.getConnectionInfo();
		    String macInfo = null;
		    if(info != null && info.getMacAddress() != null){
		    	macInfo = "mac" + info.getMacAddress().replace(":", "");
		    }
			//优先级：1.IMEI\MEID\ESN  2.mac地址  3.androidID
			if(deviceID == null || deviceID.length() <= 0){
				 if(macInfo != null && macInfo.length() > 0){
					 deviceID = macInfo;
				 } else {
					 /**
					  * ANDROID_ID: android2.2 will alreays return 9774d56d682e549c
					  * http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id
					  */
			    	 deviceID = Secure.getString(ctx.getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
			     }
			     
			     if(deviceID != null){
			    	 deviceID = deviceID.trim();
			     }
			     
			} 
			
			mUserID = deviceID;
			saveStr(ctx, PREF_USER_ID, mUserID);
			
			//
			checkedCDMA = true;
			Cfg.saveBool(ctx, Cfg.PREF_CHECK_CDMA_IMEI, true);
			
		} else if(!checkedCDMA){
			TelephonyManager telephonyManager=(TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
			String imeiString = telephonyManager.getDeviceId();
//			imeiString = "A00120012";
			
			//imei，但是跟UserId不一样，说明以前注册的是mac或androidID
			if(!mUserID.equalsIgnoreCase(imeiString)){
				//等待重新注册成功后，才会修改PREF_CHECK_CDMA_IMEI 为true
				mOldUserID = mUserID;
				mUserID = imeiString;
				mAccountInited = false;
				Cfg.saveBool(ctx, ACCOUNT_INIT, mAccountInited);
			} else {
				//若一样，则认为检查过
				Cfg.saveBool(ctx, PREF_CHECK_CDMA_IMEI, true);
			}
		}
		
		mHasUserRated = loadBool(ctx, PREF_USER_HAS_RATED);
		mAppStartTimes = loadInt(ctx, PREF_APP_START_TIMESA, 0);
//		mPlayedMinutes = loadInt(ctx, PREF_PLAY_MINUTES, 0);
		
		//the default domain is 'shantingshu.com'
		mHostType = loadInt(ctx, PREF_HOST_TYPE, HOST_TYPE_3G);
		if(mHostType != HOST_TYPE_3G && Cfg.DEBUG){
			Utils.switchHost(ctx, false);
		}
//		mCompletedTaskInPeakTimes = loadInt(ctx , PREF_CURRENT_DOWNLOADED_TASKS, 0);
		mLastPeakLimitMills = loadLong(ctx, PREF_LAST_DOWNLOAD_LIMIT_DATE, 0L);
		mCompletedTaskInOneDay = loadInt(ctx, PREF_DOWNLOADED_TASKS_BY_DAY, 0);
		mLastDayLimitMills = loadLong(ctx, PREF_LAST_DOWLOAD_DAY_LIMIT_DATE, 0L);
		
		mAwardFalgs = loadInt(ctx, Cfg.PREF_ACCOUNTS_BIND_AWARD_FLAGS, 0);
		mIsNightMode = loadBool(ctx, PREF_NIGHT_MODE);
//		loadDownloadChargeList(ctx);
//		loadPlayChargeList(ctx);
//		loadPlayedChapters(ctx);
		
		mHasFollowedQQ = loadBool(ctx, Cfg.PREF_AWARD_FOLLOW_QQ);
		mHasFollowedSina = loadBool(ctx, Cfg.PREF_AWARD_FOLLOW_SINA);
		
		mInitialized = true;
	}

	public static void loadSetting(Context ctx) {
		mAutoPlayNext = loadBool(ctx, PREF_AUTO_PLAY_NEXT, true);
		mShowListAfterFinish = loadBool(ctx, PREF_SHOW_LIST_AFTER_FINISH, false);
		mNotWifiProtect = loadBool(ctx, PREF_NOT_WIFI_PROTECT, true);
		mWifiAutoDownload = loadBool(ctx, PREF_WIFI_AUTO_DOWNLOAD, false);
		mWebUsingNoPicMode = loadBool(ctx, PREF_WEB_NOPIC_MODE, false);
		mToFlightModeAfterFinish = loadBool(ctx, PREF_FLIGHT_MODE_AFTER_FINISH, false);
		// mSubScore = loadBool(PREF_WY_SUB_SCORE, true);
		// Log.e(TAG, "mSubScore:" + mSubScore);
	}

	private static SharedPreferences mSharedPreferences;
	private static SharedPreferences mBrowsePagesPreference;

	public static String loadStr(Context ctx, String key, String defStr) {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		String value = mSharedPreferences.getString(key, defStr);
		Log.v(TAG, "load str:" + value);
		return value;
	}

	public static void saveStr(Context ctx, String key, String value) {
		Log.v(TAG, "save str:" + value);
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		mSharedPreferences.edit().putString(key, value).commit();
	}

	public static boolean loadBool(Context ctx, String key) {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		return mSharedPreferences.getBoolean(key, false);
	}

	public static boolean loadBool(Context ctx, String key, boolean val) {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		return mSharedPreferences.getBoolean(key, val);
	}

	public static void saveBool(Context ctx, String key, boolean value) {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		mSharedPreferences.edit().putBoolean(key, value).commit();
	}

	public static int loadInt(Context ctx, String key, int defVal) {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		return mSharedPreferences.getInt(key, defVal);
	}

	public static void saveInt(Context ctx, String key, int value) {
		// Log.e(TAG, "int val:" + value);
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		mSharedPreferences.edit().putInt(key, value).commit();
	}

	public static long loadLong(Context ctx, String key, Long defVal) {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		return mSharedPreferences.getLong(key, defVal);
	}

	public static void saveLong(Context ctx, String key, Long value) {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		mSharedPreferences.edit().putLong(key, value).commit();
	}

	public static Float loadFloat(Context ctx, String key, Float defVal) {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		return mSharedPreferences.getFloat(key, defVal);
	}

	public static void saveFloat(Context ctx, String key, Float value) {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		mSharedPreferences.edit().putFloat(key, value).commit();
	}
	
	public static void saveBrowsePage(Context ctx, String key, int page){
		if(mBrowsePagesPreference == null){
			mBrowsePagesPreference = ctx.getSharedPreferences(PREFERENCE_NAME_BROWSE, 0);
		}
		
		mBrowsePagesPreference.edit().putInt(key, page).commit();
	}
	
	public static int loadBrowsePage(Context ctx, String key, int defValue){
		if (mBrowsePagesPreference == null) {
			mBrowsePagesPreference = PreferenceManager.getDefaultSharedPreferences(ctx);
		}
		return mBrowsePagesPreference.getInt(key, defValue);
	}

	/**
	 * 判断是否正在是否失去耳机或扬声器的方法
	 * 
	 * @return
	 */
	
	public static boolean isFocusLosed() {
		return focusLosed;
	}

	public static void setFocusLosed(boolean focusLosed) {
		Cfg.focusLosed = focusLosed;
	}
	
	/**
	 * 加载下载收费列表
	 * 
	 * @param ctx
	 */
//	private static void loadDownloadChargeList(Context ctx){
//		String chargeString = Cfg.loadStr(ctx, PREF_DOWNLOAD_CHARGE_LIST, null);
//		if(chargeString == null || chargeString.length() <= 0){
//			return ;
//		}
//		String[] pairs = chargeString.split(";");
//		if(pairs != null){
//			String[] pair;
//			int price;
//			for(int i = 0; i < pairs.length; i++){
//				pair = pairs[i].split("=");
//				if(pair.length >= 2){
//					price = Utils.parseInt(pair[1], 0);
//					mSoldBookPrice.put(pair[0], price);
//				}
//			}
//		}
//		
//	}
	
	/**
	 * 保存下载收费列表
	 * 
	 * @param ctx
	 */
//	public  static void saveDownloadChargeList(Context ctx){
//		StringBuilder sBuilder = new StringBuilder();
//		
//		boolean needSemicolon = false;
//		if(mSoldBookPrice != null){
//			Iterator<Entry<String , Integer>> iterator = mSoldBookPrice.entrySet().iterator();
//			Entry<String, Integer> entry;
//			while(iterator.hasNext()){
//				entry = iterator.next();
//				if(needSemicolon){
//					sBuilder.append(";");
//				}
//				sBuilder.append(entry.getKey()).append("=").append(entry.getValue());
//				needSemicolon = true;
//			}
//		}
//		if(needSemicolon){
//			Cfg.saveStr(ctx, PREF_DOWNLOAD_CHARGE_LIST, sBuilder.toString());
//		}
//		
//	}
	/**
	 * 保存播放收费列表
	 * 
	 * @param ctx
	 */
//	public static void savePlayChargeList(Context ctx){
//		StringBuilder sBuilder = new StringBuilder();
//		
//		boolean needSemicolon = false;
//		if(mPlayChargedBookPrice != null){
//			Iterator<Entry<String , int[]>> iterator = mPlayChargedBookPrice.entrySet().iterator();
//			Entry<String, int[]> entry;
//			while(iterator.hasNext()){
//				entry = iterator.next();
//				if(needSemicolon){
//					sBuilder.append(";");
//				}
//				int[]value = entry.getValue();
//				sBuilder.append(entry.getKey()).append("=").append(value[0]).append(",").append(value[1]);
//				needSemicolon = true;
//			}
//		}
//		if(needSemicolon){
//			Cfg.saveStr(ctx, PREF_PLAY_CHARGE_LIST, sBuilder.toString());
//		}
//	}

	/**
	 * 加载播放收费列表
	 * 
	 * @param ctx
	 */
//	public static void loadPlayChargeList(Context ctx){
//		String chargeString = Cfg.loadStr(ctx, PREF_PLAY_CHARGE_LIST, null);
//		if(chargeString == null || chargeString.length() <= 0){
//			return ;
//		}
//		String[] pairs = chargeString.split(";");
//		if(pairs != null){
//			String[] pair;
//			for(int i = 0; i < pairs.length; i++){
//				pair = pairs[i].split("=");
//				int price[] = new int[2];
//				if(pair.length >= 2){
//					String priceString[] = pair[1].split(",");
//					for(int j = 0; j < priceString.length && j < 2; j++){
//						price[j] = Utils.parseInt(priceString[j], 0);
//					}
//					mPlayChargedBookPrice.put(pair[0], price);
//				}
//			}
//		}
//	}
	
	/**
	 * 保存播放过的曲目列表
	 * @param ctx
	 */
//	public static void savePlayedChapters(Context ctx){
//	StringBuilder sBuilder = new StringBuilder();
//		
//		boolean needSemicolon = false;
//		if(mPlayedChapters != null){
//			Iterator<Entry<String , String>> iterator = mPlayedChapters.entrySet().iterator();
//			Entry<String, String> entry;
//			while(iterator.hasNext()){
//				entry = iterator.next();
//				if(needSemicolon){
//					sBuilder.append(";");
//				}
//				sBuilder.append(entry.getKey()).append("=").append(entry.getValue());
//				needSemicolon = true;
//			}
//		}
//		if(needSemicolon){
//			Cfg.saveStr(ctx, PREF_PLAYED_CHAPTERS_LIST, sBuilder.toString());
//		}
//	}
	
	/**
	 * 加载播放过的章节列表
	 * @param ctx
	 */
//	public static void loadPlayedChapters(Context ctx){
//		String chargeString = Cfg.loadStr(ctx, PREF_PLAYED_CHAPTERS_LIST, null);
//		if(chargeString == null || chargeString.length() <= 0){
//			return ;
//		}
//		String[] pairs = chargeString.split(";");
//		if(pairs != null){
//			String[] pair;
//			for(int i = 0; i < pairs.length; i++){
//				pair = pairs[i].split("=");
//				if(pair.length >= 2){
//					mPlayedChapters.put(pair[0], pair[1]);
//				}
//			}
//		}
//	}

}
