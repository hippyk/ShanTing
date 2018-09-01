package tiger.unfamous.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.stream.JsonReader;
import com.tencent.tauth.http.Callback;
import com.umeng.analytics.MobclickAgent;
import com.weibo.sdk.android.WeiboErrorCode;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.common.InternetStateMgr;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.data.Book;
import tiger.unfamous.utils.AccountConnect;
import tiger.unfamous.utils.CommonDlg;
import tiger.unfamous.utils.MyLog;
import tiger.unfamous.utils.Utils;


public class BookStore extends BasicActivity {

	private static final MyLog log = new MyLog();
	private static final long PROGRESSLONG = 100;
	private static final int MENU_RELOAD = 0;
	public static final int MENU_TO_3G_HOST = Menu.FIRST + 20;
	public static final String INTENT_SWITCH_HOST = "switchHost";
	public static final String STRING_TRUE = "true";
	public static final String STRING_false = "false";


	private SharedPreferences mFavPreference;
	JSONObject mJsonObject;
	private WebView mWebView;
	// 当前是否是无图模式
	private boolean mUsingNoPicMode;

	// BroadcastReceiver mBroadcastHistory;
	private Button mBtnAddFav;
	private ProgressBar mProgress, mReprogress;

	//
	// 记录打开书城首页时，耗费的时间
	private long mLoadingWebHomeMSeconds;

	// String mBookTitle;
	// int mCount;
	// String mCommonTitle;
	// String mCommonAddr;
	// String[] mAddrs;

	static boolean mHasSwitchedHost = false;

	private InternetStateMgr mInternetMgr;

	@Override
	public void onCreate(Bundle icicle) {
		// 获取Method ,兼容1.6老版本
		// try {
		// // Log.i("WebView", "Load Class and getMethod");
		// setting = (Class<WebSettings>)
		// Class.forName("android.webkit.WebSettings");
		// // Log.i("BookStore", "class name:" + setting.getName());
		// setMaxSize = setting.getMethod("setAppCacheMaxSize", args);
		// } catch (ClassNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (SecurityException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (NoSuchMethodException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		onPreCreate();
		super.onCreate(icicle);
		setContentView(R.layout.web_layout);
		// Utils.addWapsMiniAd(this);
		// findViewById(R.id.wy_title_bar).setVisibility(View.GONE);
		Utils.addAdView(this);
		mWebView = (WebView) findViewById(R.id.webview);
		mTopTitle = (TextView) findViewById(R.id.title);
		mTopTitle.setText("在线书城");

		mProgress = (ProgressBar) findViewById(R.id.progress);
		mShowPlaying = (Button) findViewById(R.id.show_playing);
		mReprogress = (ProgressBar) findViewById(R.id.reprogress);
		mBtnAddFav = (Button) findViewById(R.id.rightbtn);
		mInternetMgr = new InternetStateMgr(this);

		mBtnAddFav.setText("收藏本页");
		mBtnAddFav.setOnClickListener(new BtnClickListener());
		mShowPlaying.setOnClickListener(new BtnClickListener());

		WebSettings webSettings = mWebView.getSettings();
		// if(setMaxSize!=null){
		// // Log.i("WebView", "设置缓存值");
		// setCacheMaxSize(webSettings, setMaxSize, 2 * 1024 * 1024);
		// }

		// log.e(webSettings.getUserAgentString());
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(false);
		webSettings.setUserAgentString("ShanTing/Android");
		// default和normal的区别:1.default的reload会更新缓存，normal不会
		webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

		mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
		// mWebView.setScrollContainer(true);
		mWebView.setWebChromeClient(new MyWebChromeClient());
		mWebView.setWebViewClient(new MyWebViewClient());
		mWebView.addJavascriptInterface(new JavaScriptInterface(this), "player");

		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				if (mWebView == null) {
					mWebView = (WebView) findViewById(R.id.webview);
				}
				// 无图有图的收藏夹地址不一样
				if (mUsingNoPicMode) {
					mWebView.loadUrl(Cfg.FAVORITE_URL_NOPIC);
				} else {
					mWebView.loadUrl(Cfg.FAVORITE_URL);
				}

			}
		}, new IntentFilter(Cfg.ACTION_SHOW_FAVORITE));

		mUsingNoPicMode = Cfg.mWebUsingNoPicMode;

		// 有图无图的首页不一样
		String url;
		if (mUsingNoPicMode) {
			url = Cfg.WEB_HOME_NOPIC;
		} else {
			url = Cfg.DEBUG ? Cfg.WEB_HOME_TEST : Cfg.WEB_HOME;
		}
		// url = "http://test.shanting.mobi/example.html";
		mWebView.loadUrl(url);

		// 没有提示过，当前用的也不是无图模式，依照打开网页的速度提示用户
		if (!Cfg.mHasNoticedNoPicMode && !mUsingNoPicMode) {
			mLoadingWebHomeMSeconds = System.currentTimeMillis();
		}
	}

	public class JavaScriptInterface {
		Context mContext;

		// /** Instantiate the interface and set the context */
		JavaScriptInterface(Context c) {
			mContext = c;
		}

		public void play(String jsonStr) {
			log.d(jsonStr);
			Intent i = new Intent(mContext, PlayActivity.class);
			// i.putExtra(DN.SHOW_PLAYING_SONG, true);

			try {
				String action = "";
				String id = "";
				String state = "";
				String title = "";
				String count = "";
				String autoPath = "";
				String commonTitle = "";
				String commonAddr = "";
				String addrs = "";
				String intro = "";
				String picAddr = "";
				String bitRateString = "";
				String sizeString = "";
//				String priceString = null;
				String writer = "";
				String readerString = "";
//				String playPriceString = "";
//				String playFreeNumString = "";
//				String playedChapts = "";
				String forvip = "";
				
				InputStream in = new ByteArrayInputStream(jsonStr.getBytes("GBK"));
				JsonReader reader = new JsonReader(new InputStreamReader(in, "GBK"));
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					// log.e(name);
					if (name.equals("action")) {
						action = reader.nextString();
					} else if (name.equals(DN.BOOK_ID)) {
						id = reader.nextString();
					} else if (name.equals("Serial")) {
						state = reader.nextString();
					} else if (name.equals("title")) {
						title = reader.nextString();
					} else if (name.equals(DN.BOOK_SONG_COUNT)) {
						count = reader.nextString();
					} else if (name.equals("Autopath")) {
						autoPath = reader.nextString();
					} else if (name.equals("commonTitle")) {
						commonTitle = reader.nextString();
					} else if (name.equals("commonAddr")) {
						commonAddr = reader.nextString();
					} else if (name.equals(DN.BOOK_PATH)) {
						addrs = reader.nextString();
					} else if (name.equals("newstext")) {
						intro = reader.nextString();
					} else if (name.equals("titlepic")) {
						picAddr = reader.nextString();
					} else if (name.equals("bitrate")) {
						bitRateString = reader.nextString();
					} else if (name.equals("size")) {
						sizeString = reader.nextString();
//					} 
//					else if (name.equals("price")) {
//						priceString = reader.nextString();
					} else if (name.equals("writer")) {
						writer = reader.nextString();
					} else if (name.equals("declaimer")) {
						readerString = reader.nextString();
//					} else if (name.equals("pprice")) {
//						playPriceString = reader.nextString();
//					} else if (name.equals("freenum")) {
//						playFreeNumString = reader.nextString();
//					} else if (name.equals("precord")) {
//						playedChapts = reader.nextString();
					} else if (name.equals("vip")) {
						forvip = reader.nextString();
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();

				//vip内容？
				if (forvip.equals(STRING_TRUE)) {
					if (!Cfg.mIsVIP) {
						CommonDlg.showConfirmDlg(mContext,
								-1, "抱歉，本内容为VIP专享 ，马上升级VIP会员，享受尊贵特权！" , "开通VIP", new DialogInterface.OnClickListener() {								
									@Override
									public void onClick(DialogInterface dialog, int which) {
										Utils.openBuyVipPage(mContext);
									}
								});
						return;
					}
				}
				
				
							
				
				int bitRate = Utils.parseInt(bitRateString, 32);

				String sizeArray[] = null;
				int size[] = null;
				sizeString = sizeString.replace("\n|", "|");
				sizeArray = sizeString.split("\\|");

				if (sizeArray != null) {
					size = new int[sizeArray.length];
					for (int j = 0; j < sizeArray.length; j++) {
						size[j] = Utils.parseInt(sizeArray[j], 0);
					}
				}
				
				

				if (autoPath.equals(STRING_TRUE)) {
					mService.mCurBrowseBook = new Book(id, state, title, Integer.parseInt(count), commonTitle, commonAddr, bitRate, size);
				} else {
					String[] addrArray;
					if (addrs.contains(Cfg.RET_SYMBOL_DOS)) {
						addrArray = addrs.split(Cfg.RET_SYMBOL_DOS);
					} else {
						addrArray = addrs.split(Cfg.RET_SYMBOL_UNIX);
					}

					int countInt = Integer.parseInt(count);
					if (addrArray == null /* || addrArray.length != countInt */) {
						// log.e("addr[0]:  " + addrArray[0]);
						// log.e(addrArray.length + "_" + countInt);
						MyToast.showShort(mContext, "参数错误");
						// MobclickAgent.onEvent(mContext, Cfg.UM_PERR, title);
						return;
					}
					mService.mCurBrowseBook = new Book(id, state, title, countInt, addrArray, bitRate, size);
				}
				mService.mCurBrowseBook.mWriter = writer;
				mService.mCurBrowseBook.mNarrator = readerString;

				mService.mCurBrowseBook.mIntro = intro;
				mService.mCurBrowseBook.mPicAddress = picAddr;

				if (action.equals("view")) {
					startActivity(i);
				} else if (action.equals("play")) {

				} else if (action.equals("addshelf")) {

				}
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						mWebView.clearCache(false);
					}
				});				
				
				
				if (Cfg.mIsVIP) {
					if (forvip.equals(STRING_TRUE)) {
						MobclickAgent.onEvent(BookStore.this, Cfg.UM_VIP_PLAY_VIP,
								title);
					} else {
						MobclickAgent.onEvent(BookStore.this, Cfg.UM_VIP_PLAY_ALL,
								title);
					}
				}
				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				MyToast.showShort(mContext, "参数传递错误！");
			}

		}

//		public void clearCache() {
//			showContentProgress(true, "正在清除，请稍候...");
//			new Thread(new Runnable() {
//
//				@Override
//				public void run() {
//					// TODO Auto-generated method stub
//
//					Utils.clearWebViewCache(mContext);
//
//					if (mWebView != null) {
//						mWebView.reload();
//					}
//
//					BookStore.this.runOnUiThread(new Runnable() {
//
//						@Override
//						public void run() {
//							// TODO Auto-generated method stub
//							MyToast.showShort(mContext, "清除成功");
//							BookStore.this.showContentProgress(false);
//						}
//					});
//				}
//			}, "clearCache").start();
//
//		}

		public void showToast(final String toast) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					MyToast.showShort(mContext, toast);
				}
			});
		}

		public void saveFav(String name, String value) {
			// log.e("put:" + name + "=" + value);
			// mJsonObject.put(name, value);
			initFavPref();
			mFavPreference.edit().putString(name, value).commit();
		}

		public void delFav(String name) {
			// log.e("delFav:" + name);
			mFavPreference.edit().remove(name).commit();
		}

		@SuppressWarnings("unchecked")
		public String getFavs() {
			initFavPref();

			Map<String, String> map = (Map<String, String>) mFavPreference.getAll();

			// 如果是新版本，对收藏夹内容进行更新
			if (Cfg.mFavoriteDatabaseVersion < Cfg.FAVORITE_DATABASE_VERSION) {
				Cfg.mFavoriteDatabaseVersion = Cfg.FAVORITE_DATABASE_VERSION;
				Cfg.saveInt(mContext, Cfg.PREF_FAVORITE_DATABASE_VERSION, Cfg.FAVORITE_DATABASE_VERSION);
				Utils.updateFavoritePages(map, Cfg.mWebUsingNoPicMode);

				Set<Map.Entry<String, String>> entrys = map.entrySet();
				Iterator<Map.Entry<String, String>> iterator = entrys.iterator();
				Map.Entry<String, String> entry;
				while (iterator.hasNext()) {
					entry = iterator.next();
					saveFav(entry.getKey(), entry.getValue());
				}
			}

			mJsonObject = new JSONObject(map);
			// log.e("getFavs:" + mJsonObject.toString());
			return mJsonObject.toString();
		}

		private void initFavPref() {
			if (mFavPreference == null) {
				mFavPreference = getSharedPreferences("favorite", Context.MODE_PRIVATE);
			}
		}

		// public void showAd() {
		// log.d("showad");
		// runOnUiThread(new Runnable() {
		// @Override
		// public void run() {
		// // TODO Auto-generated method stub
		// Utils.addAdView(BookStore.this);
		// }
		// });
		// }

		public String getUserid() {
			return Cfg.mUserID;
		}

		public boolean checkSinaLogin() {
			AccountConnect instance = new AccountConnect(mContext);

			return instance.hasSinaWeiboLogin();
		}

		public boolean checkQQLogin() {
			AccountConnect instance = new AccountConnect(mContext);
			return instance.hasQQAlreadyLogin();
		}

		public void shareToSina(String jsonStr) {

			String content = "";
			String url = "";
			InputStream in;
			final AccountConnect instance = new AccountConnect(mContext);
			try {
				in = new ByteArrayInputStream(jsonStr.getBytes("GBK"));
				JsonReader reader = new JsonReader(new InputStreamReader(in, "GBK"));
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					// log.e(name);
					if (name.equals("content")) {
						content = reader.nextString();
					} else if (name.equals("url")) {
						url = reader.nextString();
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			instance.shareToWeibo(content, url, true, new Callback() {

				@Override
				public void onSuccess(Object arg0) {
					// TODO Auto-generated method stub
					showToast("新浪微博同步成功");
				}

				@Override
				public void onFail(int arg0, String arg1) {
					// TODO Auto-generated method stub
					if (arg0 == WeiboErrorCode.TOKEN_EXPIRED || arg0 == WeiboErrorCode.EXPIRED_TOKEN) {  //token过期
						instance.realse(AccountConnect.SNS_TYPE_SINA_WEIBO);
						mWebView.loadUrl("javascript:deselectWeiBo()");
						showToast("新浪微博同步失败，原因：授权过期，需要重新登陆！");
					} else {
						showToast("新浪微博同步失败，原因：" + arg1);
					}
				}
			});
		}

		public void shareToQQWeibo(String jsonStr) {
			String content = "";
			String url = "";
			InputStream in;
			AccountConnect instance = new AccountConnect(mContext);
			try {
				in = new ByteArrayInputStream(jsonStr.getBytes("GBK"));
				JsonReader reader = new JsonReader(new InputStreamReader(in, "GBK"));
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					// log.e(name);
					if (name.equals("content")) {
						content = reader.nextString();
					} else if (name.equals("url")) {
						url = reader.nextString();
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			instance.qShare(AccountConnect.SHARE_TO_Q_WEIBO, content, url, "善听", null, new Callback() {

				@Override
				public void onSuccess(Object arg0) {
					showToast("腾讯微博同步成功");
				}

				@Override
				public void onFail(int arg0, String arg1) {
					showToast("腾讯微博同步失败，原因：" + arg1);
				}
			}, true);
		}

		public void loginSina() {
			Intent i = new Intent(mContext, SNSAccountActivity.class);
			mContext.startActivity(i);
		}

		public void loginQQ() {
			loginSina();
		}

		public int getVersionCode() {
			return Utils.getVersionCode(mContext);
		}

	}

	/**
	 * Provides a hook for calling "alert" from javascript. Useful for debugging
	 * your javascript.
	 */
	final class MyWebChromeClient extends WebChromeClient {
		@Override
		public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {

			CommonDlg.showInfoDlg(BookStore.this, "信息", message, "确定", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					result.confirm();
				}
			});

			return true;
		}

		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			// TODO Auto-generated method stub
			if (newProgress == PROGRESSLONG) {
				mReprogress.setVisibility(View.GONE);
				mProgress.setVisibility(View.INVISIBLE);
			} else {
				mReprogress.setVisibility(View.VISIBLE);
				mProgress.setVisibility(View.VISIBLE);
			}
		}
	}

	final class MyWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// log.e("0___" + url);
			// if (url.contains("shanting.mobi")) {
			//
			// log.e("1___" + url);
			// view.clearCache(false);
			// view.loadUrl(url);
			// } else {
			//
			// log.e("2___" + url);
			// Intent intent =new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			// startActivity(intent);
			// }
			// ((View)mWebView).
			mHasSwitchedHost = false;
			url = url.toLowerCase();
			if (url.contains("shanting.mobi") || url.contains("shantingshu.com") || url.contains("service.sina.com") || url.contains("weibo.com") || url.contains("qq.com")) {
				view.clearCache(false);

				// 没有提示过，当前用的也不是无图模式，则计算打开页面所需要的时间
				if (!Cfg.mHasNoticedNoPicMode && !mUsingNoPicMode) {
					mLoadingWebHomeMSeconds = System.currentTimeMillis();
				}
				if (!Utils.canLoadUrl(url)) {
					Utils.loadErrorPage(view, url);
					return true;
				}
				return false;
			} else {
				try {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(intent);
				} catch (Exception e) {
					// TODO: handle exception
				}

				return true;
			}

		}

		@Override
		public void onPageFinished(WebView view, String url) {
			// view.requestFocus();
			// view.clearFocus();
			if (!Cfg.mHasNoticedNoPicMode && !mUsingNoPicMode && url != null) {

				// 没有提示过用户，当前也不是无图模式，则计算网速，速度慢的话提醒用户使用无图
				mLoadingWebHomeMSeconds = System.currentTimeMillis() - mLoadingWebHomeMSeconds;
				if (mLoadingWebHomeMSeconds >= 10000) {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {

							AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(BookStore.this);

							mDialogBuilder.setIcon(android.R.drawable.ic_dialog_info);
							mDialogBuilder.setTitle("温馨提示");
							mDialogBuilder.setMessage("厄，速度有点慢，建议开启无图模式，无图模式可到“设置”中修改");
							mDialogBuilder.setCancelable(true);

							mDialogBuilder.setPositiveButton("立即开启", new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									useNoPicMode();
									if (mWebView != null && mWebView.getUrl() != null) {
										String newUrlString = Utils.switchUrlByPicMode(mWebView.getUrl(), true);
										if (newUrlString != mWebView.getUrl()) {
											mWebView.loadUrl(newUrlString);
											MyToast.showLong(mWebView.getContext(), "无图模式，正在进入....");
										}
									}
								}
							});
							mDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							});

							try {
								if (!BookStore.this.isFinishing()) {
									mDialogBuilder.show();
									alreadyShowedNotice();
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
				mLoadingWebHomeMSeconds = Long.MAX_VALUE;
			}
			super.onPageFinished(view, url);

			// log.e("scale:" + mWebView.getScale ());
			// if (mWebView.getScale() < 1.5f) {
			// mWebView.zoomIn();
			// }
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

			if (Cfg.SDK_VERSION < 5) {
				Utils.loadErrorPage(view, failingUrl);
				return;
			}
			// WebViewClient.ERROR_HOST_LOOKUP = -2, API LEVEL = 5,
			if ((errorCode == -2) && !mHasSwitchedHost && (Cfg.mHostType == Cfg.HOST_TYPE_COM || Cfg.mHostType == Cfg.HOST_TYPE_3G) && mInternetMgr.hasInternet()) {

				String url = failingUrl;
				if (Cfg.mHostType == Cfg.HOST_TYPE_COM) {
					Cfg.mHostType = Cfg.HOST_TYPE_3G;
					url = failingUrl.replace(Cfg.WEB_HOME_COM, Cfg.WEB_HOME_3G);
				} else if (Cfg.mHostType == Cfg.HOST_TYPE_3G) {
					Cfg.mHostType = Cfg.HOST_TYPE_COM;
					url = failingUrl.replace(Cfg.WEB_HOME_3G, Cfg.WEB_HOME_COM);
				}
				Cfg.saveInt(BookStore.this, Cfg.PREF_HOST_TYPE, Cfg.mHostType);
				Utils.switchHost(BookStore.this, false);

				mHasSwitchedHost = true;
				view.loadUrl(url);
			} else {
				Utils.loadErrorPage(view, failingUrl);
			}

		}
	}

	/**
	 * 已经提醒过用户
	 */
	private void alreadyShowedNotice() {
		Cfg.mHasNoticedNoPicMode = true;
		Cfg.saveBool(this, Cfg.PREF_HAS_NOTICED_NOPIC_MODE, true);
	}

	private void useNoPicMode() {
		Cfg.mWebUsingNoPicMode = true;
		Cfg.saveBool(this, Cfg.PREF_WEB_NOPIC_MODE, true);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
		// mWebView.goBack();
		// return true;
		// }else if(keyCode == KeyEvent.KEYCODE_BACK) {
		// if(!mService.isPlayStopped()){
		// new ExitDlg(BookStore.this).show3btn(mHideListener);
		// } else {
		// new ExitDlg(BookStore.this).show2btn(mHideListener);
		// }
		// return true;
		// }
		// return super.onKeyDown(keyCode, event);
		if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
			if (Utils.goBackHistory(mWebView)) {   	// 可能导致正常后退出错
				return true;
			}
//			mWebView.goBack();
//			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			return super.onKeyDown(keyCode, event);
		} else {
			return false;
		}

	}

	CustomOptionMenu mCustomOptionMenu;

	// @Override
	// public boolean onMenuOpened(int featureId, Menu menu) {
	// // TODO Auto-generated method stub
	// return CustomOptionMenu.showCustomMenu(featureId, menu, this,
	// findViewById(R.id.bookstore));
	// }

	class BtnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.rightbtn:
				// new Thread(new Runnable() {
				// @Override
				// public void run() {
				// TODO Auto-generated method stub
				mWebView.loadUrl("javascript:addFav()");
				// }
				// }).start();
				break;
			case R.id.show_playing:
				if (mService != null) {
					mService.showPlaying(BookStore.this);
				}
				break;
			}
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_RELOAD, MENU_RELOAD, "刷新").setIcon(R.drawable.menu_refresh);
		if (Cfg.DEBUG) {
			menu.add(0, MENU_TO_3G_HOST, MENU_TO_3G_HOST, getSwitchHostMenuName());
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_RELOAD:
			mHasSwitchedHost = false;
			mWebView.reload();
			break;
		case MENU_TO_3G_HOST:
			Cfg.mHostType++;

			Cfg.mHostType %= 4;

			if (mWebView != null && mWebView.getUrl() != null) {
				String url = mWebView.getUrl();
				switch (Cfg.mHostType) {
				case Cfg.HOST_TYPE_3G:
					url = url.replace(Cfg.WEB_HOME_COM, Cfg.WEB_HOME_3G);
					break;
				case Cfg.HOST_TYPE_TEMP:
					url = url.replace(Cfg.WEB_HOME_3G, Cfg.WEB_HOME_TEMP);
					break;
				case Cfg.HOST_TYPE_TEST:
					url = url.replace(Cfg.WEB_HOME_TEMP, Cfg.WEB_HOME_TEST);
					break;
				case Cfg.HOST_TYPE_COM:
					Cfg.mHostType = Cfg.HOST_TYPE_3G;
					url = url.replace(Cfg.WEB_HOME_TEST, Cfg.WEB_HOME_3G);
					break;
				}

				mWebView.clearHistory();
				mWebView.loadUrl(url);

			}
			// 不切换到com
			if (Cfg.mHostType == Cfg.HOST_TYPE_COM) {
				Cfg.mHostType = Cfg.HOST_TYPE_3G;
			}
			item.setTitle(getSwitchHostMenuName());
			Utils.switchHost(BookStore.this, true);
			MyToast.showShort(BookStore.this, "VIP本地数据已重置，重启应用后生效");
			Cfg.saveInt(BookStore.this, Cfg.PREF_HOST_TYPE, Cfg.mHostType);

			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mWebView.clearCache(false);
		// mWebView.destroy(); //可能导致FC
		mWebView = null;
		System.gc();
		// log.e("bookStore destroyed!");
	}

	// private void setCacheMaxSize(Object Object,Method me,Object arg){
	// log.d("setCatchMaxSize");
	// invokeMethod(Object, me, arg);
	// }

	/**
	 * 兼容1.6老版本系统
	 * 
	 * @param method
	 * @param args
	 */
	@SuppressWarnings("unused")
	private void invokeMethod(Object methodObject, Method method, Object args) {
		try {
			Log.i("WebView", "invokeMethod");
			method.invoke(methodObject, args);
		} catch (InvocationTargetException e) {
			// Should not happen.
			Log.w("ApiDemos", "Unable to invoke method", e);
		} catch (IllegalAccessException e) {
			// Should not happen.
			Log.w("ApiDemos", "Unable to invoke method", e);
		}
	}

	// @Override
	// public void onOptionsMenuClosed(Menu menu) {
	// // TODO Auto-generated method stub
	// if(mWebView != null && Cfg.DEBUG){
	// String url = mWebView.getUrl();
	// if(url != null && !url.contains(Cfg.WEB_HOME)){
	// if(url.contains(Cfg.WEB_HOME_3G)){
	// url.replace(Cfg.WEB_HOME_3G, Cfg.WEB_HOME);
	// }else if(url.contains(Cfg.WEB_HOME_TEST)){
	// url.replace(Cfg.WEB_HOME_TEST, Cfg.WEB_HOME);
	// }else if(url.contains(Cfg.WEB_HOME_TEMP)){
	// url.replace(Cfg.WEB_HOME_TEMP, Cfg.WEB_HOME);
	// }else if(url.contains(Cfg.WEB_HOME_COM)) {
	// url.replace(Cfg.WEB_HOME_COM, Cfg.WEB_HOME);
	// }
	//
	// if(url != null){
	// mWebView.clearHistory();
	// mWebView.loadUrl(url);
	// }
	// }
	//
	// }
	// }

	@Override
	public void onResume() {
		super.onResume();

		// 模式改变了
		if (mUsingNoPicMode != Cfg.mWebUsingNoPicMode) {
			mUsingNoPicMode = Cfg.mWebUsingNoPicMode;
			if (mWebView != null) {
				String newUrlString = Utils.switchUrlByPicMode(mWebView.getUrl(), mUsingNoPicMode);
				if (newUrlString != mWebView.getUrl()) {
					mWebView.loadUrl(newUrlString);
					MyToast.showLong(this, "已改为" + (mUsingNoPicMode ? "无图" : "有图") + "模式，正在进入....");
				}

			}
		}

	}

	private String getSwitchHostMenuName() {
		switch (Cfg.mHostType) {
		case Cfg.HOST_TYPE_TEMP:
			return "temp到test";
		case Cfg.HOST_TYPE_TEST:
			return "test到3G";
			// case Cfg.HOST_TYPE_COM:
			// return "com到3g";
		default:
			return "3g到temp";
		}
	}

}