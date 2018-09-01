package tiger.unfamous.ui;

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.umeng.analytics.MobclickAgent;
import com.umeng.analytics.ReportPolicy;

import java.io.File;
import java.util.ArrayList;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.download.DownloadItem;
import tiger.unfamous.download.DownloadList;
import tiger.unfamous.download.DownloadService;
import tiger.unfamous.services.MainService;
import tiger.unfamous.utils.AppUpdater;
import tiger.unfamous.utils.CommonDlg;
import tiger.unfamous.utils.ExitDlg;
import tiger.unfamous.utils.MyLog;
import tiger.unfamous.utils.ShanTingAccount;
import tiger.unfamous.utils.ShanTingAccount.AccountListener;
import tiger.unfamous.utils.Utils;


public class Main extends TabActivity /*implements UpdatePointsNotifier*/ {
//	private static final String TAG = "TabActivity";
//	private static final int[] tab_icon_1 = { R.drawable.home_icon,
//			R.drawable.history_icon, R.drawable.my_resource_icon,
//			R.drawable.more_icon };
	private static final MyLog log = new MyLog();


	private static final String TAG_HOME = "home";
	private static final String TAG_HISTORY = "history";
	private static final String TAG_LOCAL = "local";
	private static final String TAG_MORE = "more";

	private static final int IDX_HISTORY = 0;
	private static final int IDX_HOME = 1;
	private static final int IDX_LOCAL = 2;
	private static final int IDX_MORE = 3;

	private static final int TAB_COUNT = 4;
	private static final int MENU_PLAYING = Menu.FIRST;
	private static final int MENU_DOWNLOAD_MANAGER = Menu.FIRST + 1;
//	private static final int MENU_ABOUT = Menu.FIRST + 2;
	private static final int MENU_HIDDEN_AD = Menu.FIRST + 3;
	private static final int MENU_EXIT = Menu.FIRST + 5;
	private static final int MENU_SWITCH_MODE = Menu.FIRST + 4;
	
	private final RadioButton[] mRadioButtons = new RadioButton[TAB_COUNT];
//	private int mLastTabIdx;
	protected MainService mService;
	private ArrayList<DownloadItem> m_list = null;
//	private View 	mWelcomeBg;
//	private View 	mWelcomeLogo;
//	private View 	mWelcomeAnzhi;
	
	boolean 		mExitFlag;
	TabHost 		mTabHost;
	
	private static final int MSG_INIT_ACTIVITY = 1;
	private static final int MSG_START_SERVICE = 2;
	private static final int MSG_INIT_VIEW = 3;
	private static final int MSG_INFO = 4;
	
	ProgressDialog mProgressDialog = null;
	public void showContentProgress(boolean bShow, String msg) {
		try {
			if (bShow && mProgressDialog == null) {
				// Log.e(TAG, "progress show!");
				mProgressDialog = ProgressDialog.show(this, "", msg, true, true, new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						try {
							arg0.dismiss();
							mProgressDialog = null;
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
				});
			} else {
				if (null != mProgressDialog) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}
	
	CustomOptionMenu mCustomOptionMenu;
	private AccountListener mAwardRateStarListener = new AccountListener() {

		@Override
		public boolean onPointsOperationResult(int resultCode, int operType,
				int wastePoints, int totalPoints) {
			final StringBuilder sBuilder = new StringBuilder();
			
			showContentProgress(false, "");
			
			if(resultCode == AccountListener.RESULT_OK){
				sBuilder.append("领取奖励成功（市场评分），您当前的扇贝余量为：")
						.append(totalPoints).append(". 再次感谢您的支持！");
				
				Utils.onAwardDone(Main.this, Utils.AWARD_TYPE_START_200);
				
			} else if (resultCode == AccountListener.RESULT_FALSE){
				Utils.onAwardDone(Main.this, Utils.AWARD_TYPE_START_200);
				return true;
			} else {
				sBuilder.append("很抱歉，领取奖励失败。别担心，下次启动会自动重试。");
			}
			
//			mHandler.obtainMessage(MSG_INFO, sBuilder.toString()).sendToTarget();
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					CommonDlg.showInfoDlg(Main.this, "奖励结果", sBuilder.toString());
				}
			});
			return true;
		}
		
	};
	
	private final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(android.os.Message msg) {
			
//			Intent shortCutIntent = getIntent();
			
			switch(msg.what){
//			case MSG_INIT_ACTIVITY:
//
//				break;
//			case MSG_START_SERVICE:

				
//				mInWelcomeView = false;
				
					
				//确实是从shortcut来的
//				if(shortCutIntent != null && shortCutIntent.getBooleanExtra(DN.EXTRA_SHORTCUT, false)
//						){
//					if(mService == null || mService.isPlayStopped()){
//						startPlayByIntent(shortCutIntent);
//					} else {
//						mService.showPlaying(Main.this);
//					}
//					
//				}

//				break;
				
			case MSG_INIT_VIEW:

				onAccountInited();
//				mHandler.sendEmptyMessage(MSG_START_SERVICE);
				break;
				
//			case MSG_INFO:
//				CommonDlg.showInfoDlg(Main.this, "奖励结果", msg.obj.toString());
//				break;
			}
		};
	};
	
	
//	private long	mWelcomeStartTime;
//	private boolean	mInWelcomeView;
	private android.widget.CompoundButton.OnCheckedChangeListener mChangeListener = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			// TODO Auto-generated method stub
			if (!isChecked) {
				return;
			}
			int curIdx;
			if (buttonView == mRadioButtons[IDX_HISTORY]) {
				curIdx = IDX_HISTORY;
			} else if (buttonView == mRadioButtons[IDX_HOME]) {
				curIdx = IDX_HOME;
			} else if (buttonView == mRadioButtons[IDX_LOCAL]) {
				curIdx = IDX_LOCAL;
			} else {
				curIdx = IDX_MORE;
			}

			mTabHost.setCurrentTab(curIdx);
		}
	};
	

	protected ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			
			mService = ((MainService.LocalBinder) arg1).getService();
			mService.mMainActivity = Main.this;
			
//			clearWelcomeView();
			
			//加载前先将历史记录进行升级
			int verCode = Utils.getVersionCode(Main.this);
			if (Cfg.mTipShownVer < verCode) {
				CommonDlg.showInfoDlg(Main.this, "本版更新(" + Utils.getAppVersionName(Main.this)
						+ ")", getString(R.string.upgrade_log),"知道了",null);
				Cfg.mTipShownVer = verCode;
				Cfg.saveInt(getApplicationContext(), Cfg.PREF_TIP_SHOWN_VERSION, verCode);
				
			}
			// umeng
			try{
				com.umeng.fb.UMFeedbackService.enableNewReplyNotification(Main.this,
						com.umeng.fb.NotificationType.AlertDialog);	
			}catch (Exception e) {
			}
			
			long currentTime = System.currentTimeMillis();
			if (Cfg.mIsVIP) {
				Cfg.mIsVIP = (currentTime <= Cfg.mServiceExpireTime);
				Cfg.saveBool(getApplicationContext(), Cfg.IS_VIP, Cfg.mIsVIP);
				//test
//				Cfg.mAdsHidden = false;
				//test
				if (Cfg.mServiceExpireTime > 0L && !Cfg.mIsVIP) {
					Cfg.mServiceExpireTime = 0L;
					Cfg.saveLong(getApplicationContext(), Cfg.SERVICE_EXPIRE_TIME, 0L);
					CommonDlg.showConfirmDlg(Main.this,
							-1,
							"您的VIP服务已到期，现在就去续期？",
							new OnClickListener() {			
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Intent intent = new Intent();
									intent.setClass(Main.this, WebBrowser.class);
									String url = ShanTingAccount.instance().getBuyServiceUrl();
									intent.putExtra(DN.URL, url);
									intent.putExtra(DN.TITLE, getResources().getString(R.string.userCenter));
									startActivity(intent);
								}
							});
				}
			}
			
			// if (!Cfg.mFirstStartup) {
			// if (Cfg.IS_WITHAD && !Cfg.mAdsHidden) {
			// long lastTime = Cfg.loadLong(Cfg.LAST_PUSH_TIME, 0L);
			// long cur = System.currentTimeMillis();
			// if (Math.abs(cur - lastTime) > 3 * 24 * 60 * 60 * 1000L) {
			// AppConnect.getInstance(this).setPushAudio(false);
			// AppConnect.getInstance(this).getPushAd();
			// Cfg.saveLong(Cfg.LAST_PUSH_TIME, cur);
			// }
			// }
			// } else {
			// Cfg.mFirstStartup = false;
			// Cfg.saveBool(Cfg.FIRST_STARTUP_STR, false);
			// }

			// waps 连接服务器. 应用启动时调用.
//			AppConnect.getInstance("8a8f5d16dbc458d39617aceecba245d4", Main.this);
			MobclickAgent.onError(Main.this);

		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mService = null;
		}

	};

	private void setTabChecked(){
		String bookShelfFilePath = Cfg.mInnerRootPath
			+ Cfg.BOOKSHELF_FILE_DIR + Cfg.BOOKSHELF_FILE_FOREBODY + "0";
		if (new File(bookShelfFilePath).exists()) {
			mRadioButtons[IDX_HISTORY].setChecked(true);
		} else {
			mRadioButtons[IDX_HOME].setChecked(true);
		}

	}
	// private BroadcastReceiver mExitReceiver;

//	public void loadScore() {
//
//		AppConnect.getInstance("8a8f5d16dbc458d39617aceecba245d4", Main.this).getPoints(this);
//	}
	
	public void startAnYueService(){
		
		new Thread(new AppUpdater(this), "AppUpdater").start();
	
		if(mService == null){
			Intent intent = new Intent(Main.this, MainService.class);
			startService(intent);
			getApplicationContext().bindService(intent, mConnection,
					Context.BIND_AUTO_CREATE);
		}
		
	}
	
	public void onAccountInited() {
		
		setContentView(R.layout.main);
//		
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
		mTabHost = getTabHost();
		mTabHost.setup();
		
		TabSpec tab = mTabHost
				.newTabSpec(TAG_HISTORY)
				.setIndicator(TAG_HISTORY)
				.setContent(new Intent(Main.this, BookShelf.class));
		
		mTabHost.addTab(tab);

		tab = mTabHost
			.newTabSpec(TAG_HOME)
			.setIndicator(TAG_HOME)
			.setContent(new Intent(Main.this, BookStore.class));
		
		mTabHost.addTab(tab);

		tab = mTabHost
			.newTabSpec(TAG_LOCAL)
			.setIndicator(TAG_LOCAL)
			.setContent(new Intent(Main.this, LocalBrowser.class).putExtra(DN.IN_MAIN_TAB, true));
		
		mTabHost.addTab(tab);

		tab = mTabHost
			.newTabSpec(TAG_MORE)
			.setIndicator(TAG_MORE)
			.setContent(new Intent(Main.this, MoreActivity.class));
		
		mTabHost.addTab(tab);
		
		for (int i = 0; i < TAB_COUNT; i++) {
			
			mRadioButtons[i] = (RadioButton) findViewById(R.id.radio_button0
					+ i);
			
			mRadioButtons[i]
					.setOnCheckedChangeListener(mChangeListener);
		}
		Utils.setColorTheme(findViewById(R.id.night_mask));

		setTabChecked();

		//执行启动200次的200扇贝奖励
		if(!Utils.hasAwardBefore(Utils.AWARD_TYPE_START_200) && Cfg.mHasUserRated 
				&& Cfg.mAppStartTimes > Cfg.RATE_STAR_CONDITION_START_TIMES){
			ShanTingAccount.instance().pointsOperation(ShanTingAccount.OPER_AWARD_RATE_STAR, mAwardRateStarListener);
			showContentProgress(true, "感谢您的评分支持，正在领取" + Cfg.AWARD_RATE_STAR_POINTS +  "扇贝奖励，请稍候...");			
		}
		
		startAnYueService();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Cfg.init(this);
//		mHandler.sendEmptyMessage(MSG_INIT_ACTIVITY);
//		mWelcomeStartTime = System.currentTimeMillis();
		String str = getResources().getString(R.string.app_name);
		if (!str.equals("善听")) {	//防止别人改名
			Main.this.finish();
			return;
		}

//		if(Cfg.SDK_VERSION <= 7){
//			setContentView(R.layout.main);
//		}
		if (Cfg.DEBUG) {
			MobclickAgent.setDefaultReportPolicy(Main.this, ReportPolicy.REALTIME);
		}
		
		Cfg.mAppStartTimes ++;
		Cfg.saveInt(Main.this, Cfg.PREF_APP_START_TIMESA, Cfg.mAppStartTimes);
		
		updateDownloadItem();
		
//		mWelcomeStartTime = System.currentTimeMillis() - mWelcomeStartTime;
		
		
		// MSG_INIT_VIEW的初始化时间约为 MSG_INIT_VARAIABLE 的3-5倍，
		// 预估初始化所需要的时间
//		log.d("start time : " + mWelcomeStartTime);
		long waitTime;// = Cfg.WELCOME_SCREEN_LAST_TIME - mWelcomeStartTime * 5;
//		if(waitTime > Cfg.WELCOME_SCREEN_LAST_TIME){
		waitTime = Cfg.WELCOME_SCREEN_LAST_TIME;
//		}
		//若为快捷方式启动，则等待时间减少
//		if((shortCutIntent != null && shortCutIntent.hasExtra(DN.EXTRA_SHORTCUT))){
//			waitTime /= 4;
//		}
		//首次启动，快速进入页面
		if(waitTime < 0 || !Cfg.mAccountInited || Cfg.mAppStartTimes < 2){
			waitTime = 0;
		}
		//2.1及以下的SDK很容易出现空指针：
		//      widget.TabHost.dispatchWindowFocusChanged(TabHost.java:317)
		//参考解释：http://stackoverflow.com/questions/10328546/application-crashes-on-android-2-1-and-belownullpointer-exception
//		if(Cfg.SDK_VERSION <= 7){
//			mTabHost = getTabHost();
//			initMainView(mTabHost);
//			mHandler.sendEmptyMessageDelayed(MSG_START_SERVICE, waitTime);
//		}else{
//			mHandler.postEmptyMessageDelayed(MSG_INIT_VIEW, waitTime);
		
		if (Cfg.SDK_VERSION <= Build.VERSION_CODES.ECLAIR_MR1) {	//规避2.1空指针错误
			onAccountInited();
			if (!Cfg.mAccountInited) {
				new Thread(new Runnable() {						
					@Override
					public void run() {
						ShanTingAccount.instance().init(Main.this);
					}
				}).start();
			}
		} else {		
			mHandler.postDelayed(new Runnable() {				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					if(Cfg.DEBUG) {
						Cfg.mAccountInited = true;  //强行跳过初始化
					}

					if (!Cfg.mAccountInited) {		
						showContentProgress(true, "初始化帐户信息，请稍候...");
						new Thread(new Runnable() {						
							@Override
							public void run() {
								ShanTingAccount.instance().init(Main.this);
							}
						}).start();
					} else {
						onAccountInited();
					}
				}
			}, waitTime);
		}

		Utils.cancelKillProcess();

		Utils.setColorTheme(findViewById(R.id.night_mask));
//		if (Cfg.mIsClickNotification) {
//			if (Cfg.mNotificationAction.equals("SHOW_DIALOG_ACTION")) {
//				CommonDlg.showInfoDlg(this, getString(R.string.notification_name), Cfg.mNotificationActionText);
//			}
//
//			Cfg.mIsClickNotification = false;
//		}
	}


	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mService != null) {
			mService.mMainActivity = null;
			getApplicationContext().unbindService(mConnection);
			if (mService.isPlayStopped()) {
				Intent intent = new Intent(Main.this, MainService.class);
				stopService(intent);
			}
			
			//如果非下载中，则停止服务
			Intent i = new Intent(this,DownloadService.class);
			i.setAction(Cfg.ACTION_CHECK_STOP_SERVICE);
			if(mExitFlag){
				i.putExtra(Cfg.EXTRA_KILL_PROCESS, true);
			}
			startService(i);
		}
		// unregisterReceiver(mExitReceiver);
		// GuoheAdManager.finish(this);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_DOWNLOAD_MANAGER, MENU_DOWNLOAD_MANAGER,
				R.string.downloading).setIcon(R.drawable.menu_download_manager);
		menu.add(0, MENU_EXIT, MENU_EXIT, R.string.menu_exit).setIcon(R.drawable.menu_exit);
//		menu.add(0, MENU_ABOUT, MENU_ABOUT, R.string.menu_about);
//		if (Cfg.IS_WITHAD) {
//			if (!Cfg.mAdsHidden) {
//				menu.add(0, MENU_HIDDEN_AD, MENU_HIDDEN_AD, R.string.menu_hidden).setIcon(R.drawable.menu_remove_ad);
//			}
//		}
		
		// menu.add(0,MENU_RECOMMEND,MENU_RECOMMEND,R.string.menu_recommend);
		// menu.add(0,MENU_WIYUN,MENU_WIYUN,"微云交友");
		
		return true;
	}

	//设置menu菜单的背景
//    	protected void setMenuBackground(){
//            
//            getLayoutInflater().setFactory( new Factory() {
//                
//                @Override
//                public View onCreateView(String name, Context context,
//						AttributeSet attrs) {
//                 
//                    if (name.equalsIgnoreCase("com.android.internal.view.menu.IconMenuItemView" ) ) {
//                        
//                        try { // Ask our inflater to create the view
//                            LayoutInflater f = getLayoutInflater();
//                            final View view = f.createView( name, null, attrs );
//                            new Handler().post( new Runnable() {
//                                @Override
//								public void run () {
////                                    view.setBackgroundResource( R.drawable.menu_backg);//设置背景图片
//                                    view.setBackgroundColor(0x88222222);//设置背景色
//                                }
//                            } );
//                            return view;
//                        }
//                        catch ( InflateException e ) {}
//                        catch ( ClassNotFoundException e ) {}
//                    }
//                    return null;
//                }
//
//            });
//    }
    
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		if (mService != null && !mService.isPlayStopped()) {
			if (menu.findItem(MENU_PLAYING) == null) {
				menu.add(0, MENU_PLAYING, MENU_PLAYING, "正在播放").setIcon(R.drawable.menu_start_or_playing);
			}
		} else {
			menu.removeItem(MENU_PLAYING);
		}
		
		if (menu.findItem(MENU_SWITCH_MODE) != null){
			menu.removeItem(MENU_SWITCH_MODE);
		}
		if(!Cfg.mIsNightMode){
			menu.add(0, MENU_SWITCH_MODE, MENU_SWITCH_MODE,"夜间模式").setIcon(R.drawable.icon_night);
		} else {
			menu.add(0, MENU_SWITCH_MODE, MENU_SWITCH_MODE,"日间模式").setIcon(R.drawable.icon_day);
			
		}
		return super.onPrepareOptionsMenu(menu);
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i = null;
		switch (item.getItemId()) {
		case MENU_PLAYING:
			if (mService != null) {
				mService.showPlaying(this);
			}
			break;
		case MENU_DOWNLOAD_MANAGER:
			i = new Intent();
			i.setClass(getApplicationContext(), DownloadManager.class);
			i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(i);
			break;
		case MENU_EXIT:
			if(!mService.isPlayStopped()){
				new ExitDlg(this).show3btn(mHideListener);
			} else {
//				AppConnect.getInstance(this).finalize();
				i = new Intent(this,MainService.class);
				stopService(i);
				mExitFlag = true;
				finish();
			}
			
			break;
		// return true;
//		case MENU_ABOUT:
//			Intent intentAbout = new Intent(Main.this, AboutActivity.class);
//			startActivity(intentAbout);
//			break;
		case MENU_HIDDEN_AD:
			Intent intent = new Intent();
			intent.setClass(this, WebBrowser.class);
			intent.putExtra(DN.URL, ShanTingAccount.instance().getBuyServiceUrl());
			intent.putExtra(DN.TITLE, "个人中心");
			startActivity(intent);
			break;
//		case MENU_ADDING_SCORE:
//			AppConnect.getInstance("8a8f5d16dbc458d39617aceecba245d4", this).awardPoints(200, this);
//			break;
//		case MENU_REDUCE_SCORE:
//			AppConnect.getInstance("8a8f5d16dbc458d39617aceecba245d4", this).spendPoints(200, this);
//			break;
		case MENU_SWITCH_MODE:
			Cfg.mIsNightMode = !Cfg.mIsNightMode;
			Cfg.saveBool(Main.this, Cfg.PREF_NIGHT_MODE, Cfg.mIsNightMode);
			Utils.setColorTheme(findViewById(R.id.night_mask));
			break;
			
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	// protected void showContentProgress(boolean bShow, String msg) {
	// if (bShow) {
	// mProgress = CommonDlg.showProgressDlg(this, "加载中", null);
	// } else {
	//
	// }
	// }

//	DialogInterface.OnClickListener mGetScoreListener = new DialogInterface.OnClickListener() {
//
//		@Override
//		public void onClick(DialogInterface dialog, int which) {
//			// TODO Auto-generated method stub
//			AppConnect.getInstance("8a8f5d16dbc458d39617aceecba245d4", Main.this).showOffers(Main.this);
//			finish();
//		}
//	};
	
	DialogInterface.OnClickListener mHideListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			if (!mService.isPlayStopped()) {
				mService.saveHistory();
				mService.stopMusic();
			}
//			AppConnect.getInstance(Main.this).finalize();
			finish();
			mExitFlag = true;
		}
	};
	
	DialogInterface.OnClickListener mCancelListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub

		}
	};

//	@Override
//	public void getUpdatePoints(String arg0, int arg1) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	public void getUpdatePointsFailed(String arg0) {
//		// TODO Auto-generated method stub
//		
//	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    log.i("main receive the onKeyDown Event");
	    
	    //闪屏阶段，屏蔽按键
//	    if(mInWelcomeView){
//	    	return true;
//	    }
//		if (keyCode == KeyEvent.KEYCODE_BACK && (mService == null || mService.isPlayStopped())) {
//			new ExitDlg(Main.this).show2btn(mHideListener);
//			return true;		
//		}
		if (keyCode == KeyEvent.KEYCODE_BACK ) {
	    	if(mService != null && !mService.isPlayStopped()) {
	    		new ExitDlg(Main.this).show3btn(mHideListener);
			} else {
				new ExitDlg(Main.this).show2btn(mHideListener);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
//	public void clearWelcomeView(){
//		if(mWelcomeBg != null){
//			mWelcomeBg.setOnTouchListener(null);
//			mWelcomeBg.setVisibility(View.GONE);
//		}
//		if(mWelcomeLogo != null){
//			mWelcomeLogo.setVisibility(View.GONE);
//		}
//		if(mWelcomeAnzhi != null){
//			mWelcomeAnzhi.setVisibility(View.GONE);
//		}
//		
//		mWelcomeBg = null;
//		mWelcomeLogo = null;
//		mWelcomeAnzhi = null;
//		mInWelcomeView = false;
//	}
//	private void wifiAutoDownload() {
//		m_list = DownloadList.getInstance();
//		ArrayList<DownloadItem> items = m_list.getAllNonCompleteItems(DownloadItem.UDTYPE_DOWNLOADING);
//		if (Cfg.mWifiAutoDownload) {
//			if (mInternetStateMgr == null) {
//				mInternetStateMgr = new InternetStateMgr(this);
//			}
//			
//			mInternetState = mInternetStateMgr.getState();
//			if (mInternetStateMgr.isWifiConnected(mInternetState)) {
//				for (int i = 0; i < items.size(); i++) {
//					DownloadItem item = items.get(i);
//					Intent it = new Intent(Main.this, DownloadService.class);
//					it.putExtra("itemid", item.getItemId());
//					startService(it);
//				}
//			}
//		}
//	}

	/**
	 * Update DownloadItem for version before 1.14(include 1.14), because in these version
	 * when download all songs by page, the filetype field will be "downloading".
	 * After update the filetype field, the field will be modified to DownloadItem.AUDIO_FILE
	 */
	private void updateDownloadItem() {
		if (Cfg.mTipShownVer > 114) {
			return;
		}

		m_list = DownloadList.getInstance().getList();
		for (int i = 0; i < m_list.size(); i++) {
			DownloadItem item = m_list.get(i);
			if (DownloadManager.LAUNCH_DOWNLOADING.equals(item.getFileType())) {
				item.setFileType(DownloadItem.AUDIO_FILE);
				DownloadList.getInstance().updateItem(item);
			}
		}
	}
	
//	@Override
//		protected void onNewIntent(Intent intent) {
//			// TODO Auto-generated method stub
//			super.onNewIntent(intent);
//
//			//打开Main的intent
//			if(intent != null/* && intent.getBooleanExtra(DN.EXTRA_SHORTCUT, false)*/) {
//				final Intent i = intent;
//				
//				if(mService != null && !mService.isPlayStopped()){
//					boolean isCurPlayingBook = false;
//					String title = getBookTitleByIntent(intent);
//					
//					if(mService.mCurPlayBook != null){
//						if(mService.mIsMusicLocal){
//							String path = intent.getStringExtra(DN.EXTRA_SHORTCUT_SONG_PATH);
//							if(path != null && path.equalsIgnoreCase(mService.mCurPlayBook.mLcDirPath)){
//								isCurPlayingBook = true;
//							}
//						} else {
//							String bookId = intent.getStringExtra(DN.EXTRA_SHORTCUT_BOOK_ID);
//							if(bookId != null && bookId.equalsIgnoreCase(mService.mCurPlayBook.mID)){
//								isCurPlayingBook = true;
//							}
//						}
//						
//					}
//					if(!isCurPlayingBook){
//						
//						CommonDlg.showConfirmDlg(this, -1, title
//								+ "\n\n" + getString(R.string.play_confirm), 
//								new DialogInterface.OnClickListener() {
//									
//									@Override
//									public void onClick(DialogInterface dialog, int which) {
//										// TODO Auto-generated method stub
//										mService.stopMusic();
//										startPlayByIntent(i);
//									}
//								});
//					} 
//				} else {
//					startPlayByIntent(i);
//				}	
//			}
//		}
	
//	private void startPlayByIntent(Intent intent){
//		
//		final String type = intent.getType();
//		if(type.startsWith(DN.INTENT_TYPE_SHORTCUT_LCPLAY)){
//			//本地播放
//			intent.setClass(Main.this, LcPlayActivity.class);
//			startActivity(intent);
//			
//		} else if(type.startsWith(DN.INTENT_TYPE_SHORTCUT_PLAY)){
//			//在线播放
//			intent.setClass(Main.this, PlayActivity.class);
//			startActivity(intent);
//		}
//	}
	
//	private String getBookTitleByIntent(Intent intent){
//		String title = "";
//		final String type = intent.getType();
//		
//		if(type.startsWith(DN.INTENT_TYPE_SHORTCUT_LCPLAY)){
//			//本地播放
//			title = type.substring(DN.INTENT_TYPE_SHORTCUT_LCPLAY.length());
//			
//		} else if(type.startsWith(DN.INTENT_TYPE_SHORTCUT_PLAY)){
//			//在线播放
//			title = intent.getStringExtra(DN.EXTRA_SHORTCUT_BOOK_TITLE);
//		}
//		return title;
//		
//	}
}

	
	