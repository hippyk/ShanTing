package tiger.unfamous.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieSyncManager;

import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXMusicObject;
import com.tencent.mm.sdk.openapi.WXWebpageObject;
import com.tencent.tauth.TAuthView;
import com.tencent.tauth.TencentOpenAPI;
import com.tencent.tauth.bean.OpenId;
import com.tencent.tauth.http.AsyncHttpPostRunner;
import com.tencent.tauth.http.Callback;
import com.tencent.tauth.http.IRequestListener;
import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.WeiboParameters;
import com.weibo.sdk.android.net.HttpManager;
import com.weibo.sdk.android.sso.SsoHandler;

/**
 * 与其他网站账号互联
 * 
 * @author MyAdmin
 * 
 */
public class AccountConnect {

	public static final String TAG = "AccountConnect";
	private static final MyLog log = new MyLog();
	
	/* 微博认证失效时间容错（以秒为单位） */
	public static final int AUTH_TIME_TOLERANCE_SECS=300;
	
	
	public static final byte SHARE_TO_Q_WEIBO = 1;
	public static final byte SHARE_TO_Q_ZONE = 2;
	public static final byte SHARE_TO_WEIBO = 3;

	public static final int SNS_TYPE_QQ = 0;
	public static final int SNS_TYPE_SINA_WEIBO = 1;

	//
	public static final byte SHARE_FROM_PLAYING_LIST = 1;
	public static final byte SHARE_FROM_PLAYING_MORE = 2;

	public static final String INTENT_SHARE_TYPE = "shareType";
	public static final String INTENT_SHARE_CONTENT = "shareContent";
	public static final String INTENT_SHARE_URL = "shareResUrl";
	public static final String INTENT_SHARE_TITLE = "shareResTitle";
	public static final String INTENT_SHARE_PICURL = "shareResPic";
	public static final String INTENT_SHARE_DESCRIPTION = "shareDescription";
	public static final String INTENT_SHARE_SONG_NAME = "shareSongName";
	public static final String INTENT_IS_WX_TIMELINE = "shareIsWxTimeline";

	public static final String INTENT_WEIBO_NAME = "name";
	public static final String INTENT_WEIBO_TOKEN = "access_token";
	public static final String INTENT_WEIBO_UID = "uid";

	public static final String SINA_OFFICIAL_ID = "1917593165";
	public static final String QQ_WEIBO_OFFICIAL_ID = "shantingtingshu";

	public static final int ACCOUNT_NUM = 2;

	/**
	 * 向QQ申请的应用ID
	 */
	public static final String Q_APP_ID = "100253131";

	/**
	 * 向ＱＱ申请的key，访问/修改QQ空间图片时需要
	 */
	public static final String Q_APP_KEY = "7764ca6c1d449a47acf48f94ba3a63f3";

	/**
	 * 新浪微博的应用 ID
	 */
	public static final String SINA_APP_KEY = "2905103180";

	/**
	 * 新浪微博的应用密钥
	 */
	private static final String SINA_SECRET = "eb6fed9866357e9d8631188c4029d1dc";

	/**
	 * 新浪微博应用回调地址，
	 */
	public static final String SINA_CALLBACK_URL = "http://3g.shanting.mobi/e/extend/interface/sina.php";

	/** 
	 * sina微博server地址
	 */
	public static String SERVER = "https://api.weibo.com/2/";
	/**
	 * 新浪微博 accesstoken实例
	 */
	public Oauth2AccessToken accessToken = null;
	
	/**
	 * 登陆QQ成功之后返回的token授权，使用QQ接口时需要
	 */
	public String mQAccessToken;

	/**
	 * 登陆QQ成功之后返回的用户ID，跟QQ号一一对应
	 */
	public String mQOpenId;

	/**
	 * 权限范围
	 */
	private final String scope = "get_user_info,get_user_profile,add_share,add_t,add_pic_t,add_idol";

	/**
	 * 授权结果侦听
	 */
	private AuthReceiver mQAuthReceiver;
	/**
	 * 
	 */
	private boolean mQShareUsingMainSiteUrl = false;

	// 每个账号都有一个用户名, 用户名列表
	private String mNickNames[] = new String[ACCOUNT_NUM];

	// sina微博token
	private String mWeiboUid;
	private String mWeiboToken;
//	private String mWeiboAuthTime;
//	private String mWeiboExpiresIn;
	private long mExpiresTime = 0;

	Context mContext;

	ILoginCallback mLoginCallback;

	/**
	 * 
	 * @param url
	 * @param parameters
	 * @param listener
	 */

	private static void asyncPost(String url, Bundle parameters, IRequestListener listener) {
		(new AsyncHttpPostRunner()).request(url, parameters, listener);
	}

	public AccountConnect(Context ctx) {
		mContext = ctx;
		mQAccessToken = Cfg.loadStr(ctx, Cfg.PREF_QQ_ACCESS_TOKEN, "");
		mQOpenId = Cfg.loadStr(ctx, Cfg.PREF_QQ_OPEN_ID, "");
		mQShareUsingMainSiteUrl = Cfg.loadBool(ctx, Cfg.PREF_QQ_USING_MAINSITE_URL);
		mNickNames[SNS_TYPE_QQ] = Cfg.loadStr(ctx, Cfg.PREF_QQ_NICK_NAME, "");

		mWeiboToken = Cfg.loadStr(ctx, Cfg.PREF_WEIBO_ACCESS_TOKEN, "");
		mNickNames[SNS_TYPE_SINA_WEIBO] = Cfg.loadStr(ctx, Cfg.PREF_WEIBO_NICK_NAME, "");
		mWeiboUid = Cfg.loadStr(ctx, Cfg.PREF_WEIBO_UID, "");
//		mWeiboAuthTime = Cfg.loadStr(ctx, Cfg.PREF_WEIBO_ACCESS_TOKEN, "");
//		mWeiboExpiresIn = Cfg.loadStr(ctx, Cfg.PREF_WEIBO_AUTHORIZATION_TIME, "");
		mExpiresTime = Cfg.loadLong(ctx, Cfg.PREF_WEIBO_EXPIRES_TIME, 0l);
		
		mWXApi = WXAPIFactory.createWXAPI(ctx, WX_APP_ID, true);
		mWXApi.registerApp(WX_APP_ID);

	}

	/**
	 * 发表一条微博
	 * 
	 * @param token
	 * @param appID
	 * @param openID
	 * @param bundle
	 * @param callback
	 */
	public void addWeibo(String token, String appID, String openID, Bundle bundle, final Callback callback) {
		String qqTUrlString = "https://graph.qq.com/t/add_t";
		putQAuthCommonAttr(token, appID, openID, bundle);
		asyncPost(qqTUrlString, bundle, new myRequestListener(callback));

	}

	/**
	 * 发布一个带图片的微博
	 * 
	 * @param token
	 * @param appID
	 * @param openID
	 * @param bundle
	 * @param callback
	 */
	public void addPicWeibo(String token, String appID, String openID, Bundle bundle, final Callback callback) {
		String qqTUrlString = "https://graph.qq.com/t/add_pic_t";
		putQAuthCommonAttr(token, appID, openID, bundle);
		asyncPost(qqTUrlString, bundle, new myRequestListener(callback));
	}

	/**
	 * 收听善听QQ微博
	 * 
	 * @param token
	 * @param appID
	 * @param openID
	 * @param bundle
	 * @param callback
	 */
	public void followQQShantingWeibo(final Callback callback) {

		String followUrlString = "https://graph.qq.com/relation/add_idol";
		Bundle parameters = new Bundle();

		putQAuthCommonAttr(mQAccessToken, Q_APP_ID, mQOpenId, parameters);
		parameters.putString("name", QQ_WEIBO_OFFICIAL_ID);

		asyncPost(followUrlString, parameters, new myRequestListener(callback));

	}

	/**
	 * 新浪关注
	 * 
	 * @param uid
	 */
	public boolean followSinaWeibo() {
		try {
			WeiboParameters followParameters = new WeiboParameters();
			followParameters.add("access_token", mWeiboToken);
			followParameters.add("uid", SINA_OFFICIAL_ID);
			JSONObject result = weiboRequest("friendships/create.json", HttpManager.HTTPMETHOD_POST, followParameters);
		} catch (Exception e) {
			if (e instanceof WeiboException) {
				WeiboException wException = (WeiboException) e;
				if (wException.getStatusCode() == 20506) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	/**
	 * OAuth 2.0 公共参数
	 * 
	 * @param token
	 * @param appID
	 * @param openID
	 * @param bundle
	 */
	private void putQAuthCommonAttr(String token, String appID, String openID, Bundle bundle) {

		bundle.putString("oauth_consumer_key", appID);
		bundle.putString("openid", openID);
		bundle.putString("access_token", token);
		bundle.putString("format", "json");
	}

	private class myRequestListener implements IRequestListener {

		private final Callback mCallback;

		public myRequestListener(Callback callback) {
			mCallback = callback;
		}

		@Override
		public void onIOException(IOException arg0, Object arg1) {
			Log.d("AccountConnect", "fail io:" + arg0);
			if (mCallback != null) {
				mCallback.onFail(1, arg0.getMessage());
			}

		}

		@Override
		public void onFileNotFoundException(FileNotFoundException arg0, Object arg1) {
			Log.d(TAG, "fail file:" + arg0);
			if (mCallback != null) {
				mCallback.onFail(1, arg0.getMessage());
			}
		}

		@Override
		public void onComplete(String arg0, Object arg1) {
			Log.d(TAG, "good:" + arg0);
			if (mCallback != null) {
				mCallback.onSuccess(arg0);
			}

		}
	}

	/**
	 * 获取空间分享时可以点击的那个URL，由于TencentOpenAPI不能连续分享两条同样地址
	 * 
	 * @return
	 */
	public String getQZoneShareUrl() {

		mQShareUsingMainSiteUrl = !mQShareUsingMainSiteUrl;
		Cfg.saveBool(mContext, Cfg.PREF_QQ_USING_MAINSITE_URL, mQShareUsingMainSiteUrl);

		if (mQShareUsingMainSiteUrl) {
			return DN.OFFICAL_SITE;
		} else {
			return DN.OFFICAL_DOWNLOAD_PAGE;
		}

	}

	/**
	 * 判断是否有QQ已经登陆
	 * 
	 * @return
	 */
	public boolean hasQQAlreadyLogin() {
		return this.mQOpenId != null && !this.mQOpenId.equals("") && this.mQAccessToken != null && !this.mQAccessToken.equals("");
		// && mNickNames[SNS_TYPE_QQ] != null &&
		// mNickNames[SNS_TYPE_QQ].length() > 0;
	}

	/**
	 * 判断腾讯微博是否还可用
	 * 
	 * @return
	 */
	public boolean isQQAvailable() {
		return false;
		// TODO 实现此方法
	}

	public boolean hasSinaWeiboLogin() {
		return mWeiboToken != null && mWeiboToken.length() > 0 && mWeiboUid != null && mWeiboUid.length() > 0 
				&& (mExpiresTime == 0 || (System.currentTimeMillis() < mExpiresTime));
		// && mNickNames[SNS_TYPE_SINA_WEIBO] != null &&
		// mNickNames[SNS_TYPE_SINA_WEIBO].length() > 0;
	}
	
	/**
	 * 打开登录认证与授权页面
	 * 
	 * @param String
	 *            clientId 申请时分配的appid
	 * @param String
	 *            target 打开登录页面的方式：“_slef”以webview方式打开; "_blank"以内置安装的浏览器方式打开
	 * @date 2011-9-5
	 */
	public void qqLoginReq(ILoginCallback iLoginCallback) {
		if (iLoginCallback == null)
			return;
		mLoginCallback = iLoginCallback;

		Intent intent = new Intent(mContext, com.tencent.tauth.TAuthView.class);

		intent.putExtra(TAuthView.CLIENT_ID, Q_APP_ID);
		intent.putExtra(TAuthView.SCOPE, scope);
		intent.putExtra(TAuthView.TARGET, "_self");
		// intent.putExtra(TAuthView.CALLBACK, CALLBACK);

		registerIntentReceivers();
		mContext.startActivity(intent);

	}

	/**
	 * QQ 分享，
	 * 
	 * @param token
	 *            授权ID
	 * @param openID
	 *            QQ用户ID
	 * @param shareFrom
	 *            分享内容来源, 2:分享软件地址，1:分享播放资源的地址
	 */
	public void qShare(int shareTo, String content, String resUrl, String title, String referPicUrl, Callback callback, boolean followWeibo) {

		content = Utils.replaceMicroBlogName(content, shareTo);
		Bundle bundle = null;
		bundle = new Bundle();

		// 等待Dialog
		log.d(content);
		switch (shareTo) {

		case AccountConnect.SHARE_TO_Q_WEIBO:
			if (content.length() >= 140) {
				content = content.substring(0, 139);
			}
			bundle.putString("content", content);
			addWeibo(mQAccessToken, Q_APP_ID, mQOpenId, bundle, callback);
			// AccountConnect.addPicWeibo(mQAccessToken, Q_APP_ID, mQOpenId,
			// bundle,
			// shareCallback);

			break;
		case AccountConnect.SHARE_TO_Q_ZONE:

			bundle.putString("url", resUrl);
			bundle.putString("title", title);
			bundle.putString("comment", content);// 用户评论内容，也叫发表分享时的分享理由。禁止使用系统生产的语句进行代替。最长40个中文字，超出部分会被截断。
			bundle.putString("summary", "善听是一款安卓手机听书应用！在谷歌官方市场得到5星好评(四舍五入后)！并且轻松跻入“音乐与音频”分类top15！ 进入google市场查看：http://t.cn/zOJDB6x");// 所分享的网页资源的摘要内容，或者是网页的概要描述。
			bundle.putString("images", referPicUrl);// 所分享的网页资源的代表性图片链接"，请以http://开头，长度限制255字符。多张图片以竖线（|）分隔，目前只有第一张图片有效，图片规格100*100为佳。

			bundle.putString("site", DN.OFFICAL_SITE);
			bundle.putString("source", "1");
			bundle.putString("type", "4");// 分享内容的类型。
			TencentOpenAPI.addShare(mQAccessToken, Q_APP_ID, mQOpenId, bundle, callback);
			break;

		default:
			break;

		}

		// 勾选了收听微博，
		if (followWeibo) {
			followQQShantingWeibo(callback);
		}

	}

	/**
	 * 微博分享
	 * 
	 * @param content
	 * @param url
	 * @param followOfficalWeibo
	 */
	public void shareToWeibo(final String content, final String shareUrl, final boolean followOfficalWeibo, final Callback cb) {

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				String errorMsg = "";
				WeiboParameters p = new WeiboParameters();
				p.add("access_token", mWeiboToken);
				p.add("url_long", URLEncoder.encode(shareUrl));
				// 链接转换
				JSONObject result;
				// result= weiboRequest("short_url/shorten.json",
				// Utility.HTTPMETHOD_GET, p);
				// if(result != null){
				try {
					// JSONArray urls = result.getJSONArray("urls");
					// if(urls != null && urls.length() > 0){
					// JSONObject urlObject = (JSONObject) urls.get(0);
					// String shortUrl = urlObject.getString("url_short");
					// if(shortUrl != null && shortUrl.length() > 0){
					// p.add("status", content + shortUrl);
					// }else {
					// p.add("status", content + shareUrl);
					// }
					// }

					p.add("status", content + shareUrl);

					// 关注官方
					if (followOfficalWeibo) {
						followSinaWeibo();
					}

					result = weiboRequest("statuses/update.json", HttpManager.HTTPMETHOD_POST, p);
					if (result != null) {
						String id = result.getString("id");
						if (id != null && id.length() > 0) {
							cb.onSuccess(result);
							return;
						}
					}

					errorMsg = "not find id";

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					errorMsg = e.getMessage();
				} catch (WeiboException e) {
					// TODO Auto-generated catch block
					errorMsg = e.getMessage();
				}

				// }

				cb.onFail(-1, errorMsg);
			}
		}, "WeiboShare").start();

	}

	/**
	 * 
	 * @param token
	 */
	void saveQToken(String token) {
		mQAccessToken = token;
		Cfg.saveStr(mContext, Cfg.PREF_QQ_ACCESS_TOKEN, mQAccessToken);
	}

	void saveQNickName(String nickName) {
		mNickNames[SNS_TYPE_QQ] = nickName;
		Cfg.saveStr(mContext, Cfg.PREF_QQ_NICK_NAME, mNickNames[SNS_TYPE_QQ]);
	}

	void saveQOpenId(String id) {
		mQOpenId = id;
		Cfg.saveStr(mContext, Cfg.PREF_QQ_OPEN_ID, mQOpenId);
	}

	void saveWeiboToken(String token) {
		mWeiboToken = token;
		Cfg.saveStr(mContext, Cfg.PREF_WEIBO_ACCESS_TOKEN, mWeiboToken);
	}

	void saveWeiboNickName(String name) {
		mNickNames[SNS_TYPE_SINA_WEIBO] = name;
		Cfg.saveStr(mContext, Cfg.PREF_WEIBO_NICK_NAME, mNickNames[SNS_TYPE_SINA_WEIBO]);
	}

	void saveWeiboUid(String id) {
		mWeiboUid = id;
		Cfg.saveStr(mContext, Cfg.PREF_WEIBO_UID, mWeiboUid);
	}

	/**
	 * 保存新浪微博的授权时间和有效期，用以验证授权是否过期，数据保存的格式为字符串。
	 */
	void saveWeiboAuthTimeAndExpiresIn(String authTime, String expiresIn) {
//		Cfg.saveStr(mContext, Cfg.PREF_WEIBO_AUTHORIZATION_TIME, authTime);
//		Cfg.saveStr(mContext, Cfg.PREF_WEIBO_EXPIRES_IN, expiresIn);
		long expireTime = System.currentTimeMillis() + Long.parseLong(expiresIn)*1000;
		Cfg.saveLong(mContext, Cfg.PREF_WEIBO_EXPIRES_TIME, expireTime);
	}

	public String[] getNickNames() {
		return mNickNames;
	}

	public void realse(int type) {
		if (type == SNS_TYPE_SINA_WEIBO) {
			saveWeiboNickName(null);
			saveWeiboToken(null);
		} else {
			saveQNickName(null);
			saveQToken(null);
		}
	}

	/**
	 * 广播的侦听，授权完成后的回调是以广播的形式将结果返回
	 * 
	 * @author John.Meng<arzen1013@gmail> QQ:3440895
	 * @date 2011-9-5
	 */
	public class AuthReceiver extends BroadcastReceiver {

		private static final String TAG = "AuthReceiver";

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle exts = intent.getExtras();
			String raw = exts.getString("raw");
			String access_token = exts.getString(TAuthView.ACCESS_TOKEN);
			String expires_in = exts.getString(TAuthView.EXPIRES_IN);
			// String error_des = exts.getString(TAuthView.ERROR_DES);
			Log.i(TAG, String.format("raw: %s, access_token:%s, expires_in:%s", raw, access_token, expires_in));

			if (access_token != null) {
				saveQToken(access_token);

				// 用access token 来获取open id
				TencentOpenAPI.openid(access_token, new Callback() {
					@Override
					public void onSuccess(final Object obj) {
						saveQOpenId(((OpenId) obj).getOpenId());

						getQUserInfo(new Callback() {

							@Override
							public void onSuccess(Object arg0) {
								// TODO Auto-generated method stub
								if (arg0 == null) {
									return;
								}
								JSONObject json;
								try {
									json = new JSONObject((String) arg0);

									int ret = json.getInt("ret");
									if (ret == 0) {
										saveQNickName(json.getString("nickname"));
										mLoginCallback.onSuccess(json);
									} else {
										mLoginCallback.onFail(ret, "没有找到您的用户名");
									}

								} catch (JSONException e) {
									// TODO Auto-generated catch
									// block
									e.printStackTrace();
									mLoginCallback.onFail(-2, e.getMessage());
								}

							}

							@Override
							public void onFail(int arg0, String arg1) {
								// TODO Auto-generated method stub
								Log.d(TAG, "error: " + arg0 + arg1);

							}
						});

					}

					@Override
					public void onFail(int ret, final String msg) {
						mLoginCallback.onFail(ret, msg);
					}
				});
			}

			unregisterIntentReceivers();
		}

	}

	/**
	 * 注册QQ登陆接收器
	 */
	private void registerIntentReceivers() {
		mQAuthReceiver = new AuthReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(TAuthView.AUTH_BROADCAST);
		mContext.registerReceiver(mQAuthReceiver, filter);
	}

	/**
	 * 取消QQ登陆信息接收器
	 */
	private void unregisterIntentReceivers() {
		try {
			mContext.unregisterReceiver(mQAuthReceiver);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}

	}

	public interface ILoginCallback {

		/**
		 * 获取成功，返回的数据很可能为JSON数据
		 * 
		 * @param result
		 */
		public void onSuccess(JSONObject result);

		/**
		 * 获取失败
		 * 
		 * @param ret
		 * @param msg
		 */
		public void onFail(int ret, final String msg);
	}

	/**
	 * 获取QQ用户信息
	 */
	public void getQUserInfo(Callback callback) {
		Bundle bundle = new Bundle();
		putQAuthCommonAttr(mQAccessToken, Q_APP_ID, mQOpenId, bundle);

		asyncPost("https://graph.qq.com/user/get_user_info", bundle, new myRequestListener(callback));
	}

	/**
	 * 新浪微博登录请求
	 * 
	 * @param act
	 * @param iCallback
	 */
	public void weiboLoginReq(SsoHandler mSsoHandler,ILoginCallback iCallback) {
        mSsoHandler.authorize( new AuthDialogListener());
		mLoginCallback = iCallback;
	}

	/**
	 * 微博授权认证回调
	 */
	class AuthDialogListener implements WeiboAuthListener {

		@Override
		public void onWeiboException(WeiboException e) {
			// TODO Auto-generated method stub
			Log.d("weibo:exception", e.getMessage());
			mLoginCallback.onFail(-1, e.getMessage());
		}

		@Override
		public void onError(WeiboDialogError arg0) {
			Log.d("weibo:error", arg0.getMessage());
			mLoginCallback.onFail(-1, arg0.getMessage());
		}

		@Override
		public void onComplete(Bundle values) {
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");
			String uid = values.getString("uid");
			CookieSyncManager.getInstance().sync();
			if (null == accessToken) {
				accessToken = new Oauth2AccessToken();
			}
			accessToken.setToken(values.getString(Weibo.KEY_TOKEN));
			accessToken.setExpiresIn(values.getString(Weibo.KEY_EXPIRES));
			accessToken.setRefreshToken(values.getString(Weibo.KEY_REFRESHTOKEN));
			if (accessToken.isSessionValid()) {
				// 存储微博相关数据
				saveWeiboToken(token);
				saveWeiboUid(uid);
//				String authTimeStr = Utils.getCurrSecTime() + "";
//				saveWeiboAuthTimeAndExpiresIn(authTimeStr, expires_in);
				mExpiresTime = System.currentTimeMillis() + Long.parseLong(expires_in) * 1000;
				Cfg.saveLong(mContext, Cfg.PREF_WEIBO_EXPIRES_TIME, mExpiresTime);
			} else {
				Log.d("Weibo-authorize", "Failed to receive access token");
			}
			
			// 开始获取新浪账户用户名
			WeiboParameters bundle = new WeiboParameters();
			bundle.add("access_token", token);
			bundle.add("uid", uid);
			String url = "users/show.json";

			JSONObject jsonObject;
			try {
				jsonObject = weiboRequest(url, HttpManager.HTTPMETHOD_GET, bundle);
				if (jsonObject != null) {
					String nickName;
					nickName = jsonObject.getString("name");
					if (nickName == null || nickName.length() <= 0) {
						nickName = jsonObject.getString("screen_name");
					}
					if (nickName != null) {
						saveWeiboNickName(nickName);
						mLoginCallback.onSuccess(jsonObject);
						return;
					}

				} else {
					mLoginCallback.onFail(-1, "Cant get nick name");
				}

			} catch (WeiboException e1) {
				mLoginCallback.onFail(-1, e1.getMessage());
			} catch (JSONException e) {
				mLoginCallback.onFail(-1, e.getMessage());
			}

		}

		@Override
		public void onCancel() {
			Log.d(TAG, "onCancel: = ");
		}
	};

	private JSONObject weiboRequest(String url, String method, WeiboParameters p) throws WeiboException {
		try {
			String result = HttpManager.openUrl(SERVER + url, method, p, null);
			JSONObject jsonObject = new JSONObject(result);
			return jsonObject;

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	private IWXAPI mWXApi;
	public static final String WX_APP_ID = "wxd4b85085b3bd7126";

	public boolean shareToWeiXin(Activity act, String title, String url, String description, boolean isToTimeLine) {

		if (mWXApi == null) {
			mWXApi = WXAPIFactory.createWXAPI(act, WX_APP_ID, true);
			mWXApi.registerApp(WX_APP_ID);
		}

		if (!mWXApi.isWXAppInstalled()) {
			return false;
		}

		if (!mWXApi.isWXAppSupportAPI()) {
			return false;
		}

		WXMediaMessage msg = new WXMediaMessage();
		msg.title = title == null ? "善听" : title;
		// 限制最长1KB即约500个字，对介绍来说足够了
		if (description == null) {
			msg.description = act.getString(R.string.default_wx_recommend_content);

		} else {
			int length = description.length();
			msg.description = description.substring(0, length < 500 ? length : 500);

		}

		Bitmap thumb = BitmapFactory.decodeResource(act.getResources(), R.drawable.icon);
		msg.thumbData = bmpToByteArray(thumb, true);

		if (url != null && url.contains("/player/xml.php?url=")) {
			int start = url.lastIndexOf("/player/xml.php?url=") + "/player/xml.php?url=".length();
			int end = url.lastIndexOf("&title=");
			url = url.substring(start, end);

			WXMusicObject music = new WXMusicObject();
			music.musicUrl = url;

			msg.mediaObject = music;

		} else {// 本地分享
			// WXFileObject file = new WXFileObject();
			WXWebpageObject webpageObject = new WXWebpageObject();
			webpageObject.webpageUrl = url;
			// file.filePath = url;
			// msg.mediaObject = file;
			msg.mediaObject = webpageObject;
		}

		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = buildTransaction("music");
		req.message = msg;
		req.scene = isToTimeLine ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;

		return mWXApi.sendReq(req);

	}

	private String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
	}

	public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		bmp.compress(CompressFormat.PNG, 100, output);
		if (needRecycle) {
			bmp.recycle();
		}

		byte[] result = output.toByteArray();
		try {
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}
}
