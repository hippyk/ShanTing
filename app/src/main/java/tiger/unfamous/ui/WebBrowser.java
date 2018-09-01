//这是“善听”源代码，欢迎访问www.shantingshu.com
package tiger.unfamous.ui;
//#if NOWAPS
//import cn.waps.AppConnect;
//#endif
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
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

import tiger.unfamous.AppConnect;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.alipay.AliPay;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.download.DownloadItem;
import tiger.unfamous.download.DownloadService;
import tiger.unfamous.utils.CommonDlg;
import tiger.unfamous.utils.ShanTingAccount;
import tiger.unfamous.utils.Utils;


public class WebBrowser extends Activity {

	private static final long PROGRESSLONG = 100;
	private static final int MENU_RELOAD = 0;
	private static final int MENU_BACK = 1;

	private TextView mTopTitle;
	private WebView mWebView;
//	BroadcastReceiver mBroadcastHistory;
	private Button mRightBtn;
	private ProgressBar mProgress, mReprogress;

	Class<WebSettings> setting;
	private boolean mNeedRefreshAfterResume = false;
//	private Method mMethodSetMaxSize;
//	private static final Class<?>[] args = new Class[]{long.class};

	@Override
	public void onCreate(Bundle savedInstanceState) {
//		获取Method ,兼容1.6老版本
//		try {
////			Log.i("WebView", "Load Class and getMethod");
//			setting = (Class<WebSettings>) Class.forName("android.webkit.WebSettings");
////			Log.i("BookStore", "class name:" + setting.getName());
//			mMethodSetMaxSize = setting.getMethod("setAppCacheMaxSize", args);
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SecurityException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchMethodException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 禁止横屏

		super.onCreate(savedInstanceState);
		setContentView(R.layout.web_layout_no_playing);
		Utils.addAdView(this);
//		findViewById(R.id.wy_title_bar).setVisibility(View.GONE);

		mWebView = (WebView) findViewById(R.id.webview);
		mTopTitle = (TextView) findViewById(R.id.title);
		

		mProgress = (ProgressBar) findViewById(R.id.progress);
		mReprogress = (ProgressBar) findViewById(R.id.reprogress);
		mRightBtn = (Button) findViewById(R.id.rightbtn);

		mRightBtn.setText("返回");
		mRightBtn.setOnClickListener(new BtnClickListener());

		WebSettings webSettings = mWebView.getSettings();
//		if(mMethodSetMaxSize != null){
////			Log.i("WebView", "设置缓存值");
//			setCacheMaxSize(webSettings, mMethodSetMaxSize, 3 * 1024 * 1024);
//		}
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(false);		
		 //default和normal的区别:1.default的reload会更新缓存，normal不会
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		
		mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);  
		mWebView.setWebChromeClient(new MyWebChromeClient());
		mWebView.setWebViewClient(new MyWebViewClient());
		mWebView.addJavascriptInterface(new JavaScriptInterface(this), "player");
		mWebView.addJavascriptInterface(new AliPay(this,this), "AliPay");
		//#if NOWAPS
		mWebView.addJavascriptInterface(new WapsScoreWall(this), "Waps");
		//#endif		
		
		Intent i = getIntent();
		String url = i.getStringExtra(DN.URL);
		if (!TextUtils.isEmpty(url)) {
			if(Utils.canLoadUrl(url)){
				mWebView.loadUrl(url);
			} else {
				Utils.loadErrorPage(mWebView, url);
			}
			
		}		
		
		String title = i.getStringExtra(DN.TITLE);
		mTopTitle.setText(title);
	}

	public class JavaScriptInterface {
    	Context mContext;

//	    /** Instantiate the interface and set the context */
	    JavaScriptInterface(Context c) {
	        mContext = c;
	    }
	    
        public void showToast(final String toast) {
        	runOnUiThread(new Runnable() {			
				@Override
				public void run() {
					// TODO Auto-generated method stub
					MyToast.showShort(mContext, toast);
				}
			});
        }
        
//        public void clearCache(){
//	    	showContentProgress(true, "正在清除，请稍候...");
//	    	new Thread(new Runnable() {
//				
//				@Override
//				public void run() {
//					Utils.clearWebViewCache(mContext);
//					
//					if(mWebView != null){
//						mWebView.reload();
//			    	}
//					
//					WebBrowser.this.runOnUiThread(new Runnable() {
//						
//						@Override
//						public void run() {
//							// TODO Auto-generated method stub
//							MyToast.showShort(mContext, "清除成功");	
//							WebBrowser.this.showContentProgress(false);
//						}
//					});
//				}
//			},"clearCache").start();
//	    	
//	    	
//	    }
//        public void showAd() {
//        	log.d("showad");
//        	runOnUiThread(new Runnable() {			
//				@Override
//				public void run() {
//					// TODO Auto-generated method stub					
//					Utils.addAdView(WebBrowser.this);
//				}
//			});
//        }
        public int getVersionCode(){
        	return Utils.getVersionCode(mContext);
        }
	}
	

	/**
	 * Provides a hook for calling "alert" from javascript. Useful for debugging
	 * your javascript.
	 */
	final class MyWebChromeClient extends WebChromeClient {
		@Override
		public boolean onJsAlert(WebView view, String url, String message,
				final JsResult result) {
			CommonDlg.showInfoDlg(WebBrowser.this, "提示", message, "确定", new DialogInterface.OnClickListener() {
				
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
			view.clearCache(false);
//			view.loadUrl(url);
			//若是apk，用第三方浏览器下载
			if(url != null && url.endsWith(".apk")){
				String nameString = url.substring(url.lastIndexOf("/") + 1);
				try{
					Intent it = new Intent(WebBrowser.this, DownloadService.class);

					it.putExtra(DownloadManager.LAUNCH_UDTYPE,
							DownloadManager.LAUNCH_DOWNLOADING);
					it.putExtra(DownloadService.DOWNLOAD_DIR_NAME,
							"");
					it.putExtra(DownloadService.DOWNLOAD_NAME,
							nameString);
					it.putExtra(DownloadService.DOWNLOAD_FILE_TYPE,
							DownloadItem.INSTALL_PACKAGE_FILE);
					it.putExtra(DownloadService.DOWNLOAD_URL, url);

					startService(it);
					
					MyToast.showLong(WebBrowser.this, "已添加到下载队列");
					
//					Intent intent =new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//		            startActivity(intent);
		            return false;
				}catch (Exception e) {
					// TODO: handle exception
				}
			}
			if(Utils.canLoadUrl(url)){
				return false;
			} else {
				Utils.loadErrorPage(view, url);
				return true;
			}
			
		}
		
		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			// TODO Auto-generated method stub
			Utils.loadErrorPage(view, failingUrl);
		}
		
	}


	class BtnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.rightbtn:
				finish();
				break;
			}
		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_RELOAD, MENU_RELOAD, "刷新").setIcon(R.drawable.menu_refresh);
		menu.add(0, MENU_BACK, MENU_BACK, "返回").setIcon(R.drawable.menu_exit);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case MENU_RELOAD:
			mWebView.reload();
			return true;
		case MENU_BACK:
			finish();
			return true;
		}
		return false;
	}
	
	public void refreshUserCenter(){
		mWebView.loadUrl(ShanTingAccount.instance().getAccountCenterUrl());
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
		    if(Utils.goBackHistory(mWebView)){
		    	return true;
		    }
//			mWebView.goBack();
//			return true;
		} 
			
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mWebView.clearCache(false);
//		mWebView.destroy();  //可能导致FC
		mWebView = null;
		System.gc();
//		log.e("bookStore destroyed!");
	}
	

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(mNeedRefreshAfterResume){
			mNeedRefreshAfterResume = false;
			if(mWebView != null){
				mWebView.reload();
			}
		}
		Utils.setColorTheme(findViewById(R.id.night_mask));
		
	}
	
	
//	private void setCacheMaxSize(Object Object,Method me,Object arg){
//		Log.i("WebView", "setCatchMaxSize");
//		invokeMethod(Object, me, arg);
//	}
//	/**
//	 * 兼容1.6老版本系统
//	 * @param method
//	 * @param args
//	 */
//	 private void  invokeMethod(Object methodObject ,Method method, Object args) {
//		try {
//			Log.i("WebView", "invokeMethod");
//			method.invoke(methodObject, args);
//		} catch (InvocationTargetException e) {
//			// Should not happen.
//			Log.w("ApiDemos", "Unable to invoke method", e);
//		} catch (IllegalAccessException e) {
//			// Should not happen.
//			Log.w("ApiDemos", "Unable to invoke method", e);
//		}
//	}

	//#if NOWAPS
	private class WapsScoreWall{
		private Context mContext;
		public WapsScoreWall(Context ctx){
			mContext = ctx;
		}

		@SuppressWarnings("unused")
		public void showScoreWall(){
			AppConnect appConnect = AppConnect.getInstance("8a8f5d16dbc458d39617aceecba245d4", "", mContext);
//			appConnect.setAdViewClassName("");
			appConnect.showOffers(mContext);
			mNeedRefreshAfterResume = true;
		}

	}
	//#endif
	
}