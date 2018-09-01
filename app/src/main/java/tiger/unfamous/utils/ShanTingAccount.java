package tiger.unfamous.utils;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import tiger.unfamous.Cfg;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.ui.Main;
import android.R.integer;
import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * 
 * 善听账户相关，包括购买服务，查询服务等操作
 */
public class ShanTingAccount {
	
	//VIP1 包括去广告和整页下载
	public static final int 		OPER_BUY_VIP1 = 2;
	public static final int 		OPER_BUY_PAGE_DOWNLOAD = 1;
	
	
	public static final int 		OPER_AWARD_SHARE_QQ = 1001;
	public static final int 		OPER_QUERY_PAGE_DOWNLOAD = 1002;
	public static final int 		OPER_QUERY_SERVICE = 1003;
	public static final int 		OPER_AWARD_SHARE_WEIBO = 1004;
	public static final int 		OPER_AWARD_RATE_STAR = 1005;
	public static final int 		OPER_DOWNLOAD_CHARGE = 1006;
	public static final int 		OPER_QUERY_AWARD_BIND_QQ = 1007;
	public static final int 		OPER_QUERY_AWARD_BIND_SINA = 1008;
	public static final int 		OPER_QUERY_AWARD_START_200 = 1009;
	
	public static final int 		OPER_QUERY_AWARD_FOLLOW_QQ = 1010;
	public static final int 		OPER_QUERY_AWARD_FOLLOW_SINA = 1011;
	public static final int 		OPER_AWARD_FOLLOW_QQ = 1012;
	public static final int 		OPER_AWARD_FOLLOW_SINA = 1013;
	public static final int			OPER_PLAY_CHARGE = 1014;
	
	MyLog log = new MyLog("ShantingAccount");
	
	
//	public static final String		PARAM_SERTICE_TYPE = "serviceType";
	public static final String		PARAM_UID = "uid=";
	public static final String		PARAM_SERTICE = "service=";
	public static final String		PARAM_WAPS = "waps=";
	public static final String		PARAM_ACT = "act=";
	public static final String		PARAM_VIP_EXPIRE = "vipExpire=";
	public static final String		PARAM_PRICE = "price=";
	public static final String		PARAM_NEW_UID = "meid=";
	public static final String		PARAM_PLAY_BOOK = "bookid=";
	public static final String		PARAM_PLAY_SONG_INDEX = "playnum=";
	
	
	public static final String		JSON_STATUS = "status";
	public static final String		JSON_PRICE = "price";
	public static final String		JSON_TOTAL_POINT = "totalPoint";
//	public static final String		PARAM_EXPIRE_DAY = "expireDay";
	public static final String 		JSON_SERVICE_STATUS = "serviceState";
	public static final String		JSON_WAPS_POINT = "wapsPoint";
	
	public static final String 		PARAM_VALUE_BIND_QQ = "bindqq";
	public static final String 		PARAM_VALUE_BIND_SINA = "bindsina";
	public static final String 		PARAM_VALUE_RATE_STAR = "start200";
	public static final String 		PARAM_VALUE_BUY_DOWNLOAD = "download";
	public static final String 		PARAM_VALUE_FOLLOW_QQ = "focussina";
	public static final String 		PARAM_VALUE_FOLLOW_SINA = "focusqq";
	public static final String 		PARAM_VALUE_PLAY_CHARGE = "play";
	
	
	public static final String		JSON_STATUS_OK = "OK";
	public static final String		JSON_STATUS_FAIL = "fail";
	public static final String		JSON_STATUS_POINT_NOT_ENOUGH = "notEnough";
	
	
	public static String 		BUY_SERVICE_URL = Cfg.WEB_HOME + "/e/payapi/payfen.php?";
	public static String		QUERY_URL = Cfg.WEB_HOME + "/e/extend/interface/imeiquery.php?";
	
	public static String 		BUY_MONTHLY_RENT_SERVICE_PAGE = Cfg.WEB_HOME + "/e/payapi/?type=vip&";
	public static String 		ACCOUNT_CENTER_URL = Cfg.WEB_HOME + "/e/member/my/?";
	
	public static String 		EARN_SHANBEI_URL = Cfg.WEB_HOME + "/e/payapi/?";
	
	static int mPointsRequired = 0;
	static String mBookId = "";
	static int mBookSongId = 0;
	public static ShanTingAccount 	mInstance;
	
	private ShanTingAccount(){
	}
	
	public static ShanTingAccount instance(){
		if(mInstance == null){
			mInstance = new ShanTingAccount();
		}
		
		return mInstance;
	}
	
	public boolean downloadChargeOper(final int oper, final int price, int count, final AccountListener listener){
		mPointsRequired = price * count;
		return pointsOperation(oper, listener);
	}
	
	public boolean playChargeOper( String bookId, int bookIndex, final int price, final AccountListener l){
		mBookId = bookId;
		mBookSongId = bookIndex;
		mPointsRequired = price;
		
		return pointsOperation(OPER_PLAY_CHARGE, l);
	}
	
	/**
	 * 积分操作，最终结果会进行回调
	 * 
	 * @param oper 服务类型
	 */
	public boolean pointsOperation(final int oper, final AccountListener listener){
		if(listener == null){
			return false;
		}
		
		if(Cfg.mUserID != null && Cfg.mUserID.length() > 0){
			
			String url;
			switch(oper){
			case OPER_QUERY_PAGE_DOWNLOAD:
				url = QUERY_URL + PARAM_UID + Cfg.mUserID 
							+ "&" + PARAM_SERTICE + 1;
				
				break;
			case OPER_BUY_PAGE_DOWNLOAD:
				url = BUY_SERVICE_URL + PARAM_UID + Cfg.mUserID
							+ "&" + PARAM_SERTICE + 1;
				break;
				
			case OPER_AWARD_SHARE_QQ:
				url = getAwardBindUrl(AccountConnect.SNS_TYPE_QQ);
				break;
				
			case OPER_AWARD_SHARE_WEIBO:
				url = getAwardBindUrl(AccountConnect.SNS_TYPE_SINA_WEIBO);
				break;
				
			case OPER_AWARD_RATE_STAR:
				StringBuilder sb = new StringBuilder(BUY_SERVICE_URL);
				sb.append(PARAM_UID).append(Cfg.mUserID)
					.append("&").append(PARAM_ACT).append(PARAM_VALUE_RATE_STAR);
				
				url = sb.toString();
				break;
			case OPER_DOWNLOAD_CHARGE:
				url = BUY_SERVICE_URL + PARAM_UID + Cfg.mUserID
						+ "&" + PARAM_ACT + PARAM_VALUE_BUY_DOWNLOAD
						+ "&" + PARAM_PRICE + mPointsRequired;
						
				break;
			case OPER_QUERY_AWARD_BIND_QQ:
				url = QUERY_URL + PARAM_UID + Cfg.mUserID 
						+ "&" + PARAM_SERTICE + PARAM_VALUE_BIND_QQ;
				break;
			case OPER_QUERY_AWARD_BIND_SINA:
				url = QUERY_URL + PARAM_UID + Cfg.mUserID 
						+ "&" + PARAM_SERTICE + PARAM_VALUE_BIND_SINA;
				break;
			case OPER_QUERY_AWARD_START_200:
				url = QUERY_URL + PARAM_UID + Cfg.mUserID 
						+ "&" + PARAM_SERTICE + PARAM_VALUE_RATE_STAR;
				break;
			case OPER_AWARD_FOLLOW_QQ:
				url = BUY_SERVICE_URL + PARAM_UID + Cfg.mUserID 
				+ "&" + PARAM_ACT + PARAM_VALUE_FOLLOW_QQ;
				break;
			case OPER_AWARD_FOLLOW_SINA:
				url = BUY_SERVICE_URL + PARAM_UID + Cfg.mUserID 
				+ "&" + PARAM_ACT + PARAM_VALUE_FOLLOW_SINA;
				break;
			case OPER_QUERY_AWARD_FOLLOW_QQ:
				url = QUERY_URL + PARAM_UID + Cfg.mUserID 
					+ "&" + PARAM_SERTICE + PARAM_VALUE_FOLLOW_QQ;
				break;
			case OPER_QUERY_AWARD_FOLLOW_SINA:
				url = QUERY_URL + PARAM_UID + Cfg.mUserID 
					+ "&" + PARAM_SERTICE + PARAM_VALUE_FOLLOW_SINA;
				break;
				
			case OPER_PLAY_CHARGE:
				url = BUY_SERVICE_URL + PARAM_UID + Cfg.mUserID
					+ "&" + PARAM_ACT + PARAM_VALUE_PLAY_CHARGE
					+ "&" + PARAM_PLAY_BOOK + mBookId
					+ "&" + PARAM_PLAY_SONG_INDEX + mBookSongId
					+ "&" + PARAM_PRICE + mPointsRequired;
				break;
				
			default:
				url = QUERY_URL + PARAM_UID + Cfg.mUserID;
				break;
			}
			
			Log.d("ShantingAccount", "pointsOperation, url =  " + url);
			httpRequestUrl(url, new httpListener() {
				
				@Override
				public boolean onGetData(JSONObject object) {
					// TODO Auto-generated method stub
					
					if(object == null){
						listener.onPointsOperationResult(AccountListener.RESULT_NETWORK_ERR, oper, -1, -1);
						return true;
					}
					
					 int resultCode = AccountListener.RESULT_FAIL;
	                 int points = -1;
	                 int wastePoints = -1;
	                 String tempString;
	                 try {
		                 if(object.has(JSON_STATUS)){
		                	 tempString = object.getString(JSON_STATUS);
		                	 resultCode = tempString.equalsIgnoreCase(JSON_STATUS_OK) ? 
		                			 AccountListener.RESULT_OK: AccountListener.RESULT_FALSE;
		                	 
		                 }
		                 if(object.has(JSON_TOTAL_POINT)){
		                	 tempString = object.getString(JSON_TOTAL_POINT);
		                	 tempString.trim();
		                	 try{
		                		 points = Integer.parseInt(tempString);
		                	 }catch (NumberFormatException e){
		                		 resultCode = AccountListener.RESULT_FAIL;
		                	 }
		                 }
		                 if(object.has(JSON_PRICE)){
		                	 tempString = object.getString(JSON_PRICE);
		                	 tempString.trim();
		                	 try{
		                		 wastePoints = Integer.parseInt(tempString);
		                	 }catch (NumberFormatException e){
		                		 resultCode = AccountListener.RESULT_FAIL;
		                	 }
		                	 
		                 }
		                 
		                listener.onPointsOperationResult(resultCode, oper, wastePoints, points);
	                } catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							listener.onPointsOperationResult(AccountListener.RESULT_FAIL, oper, wastePoints, points);
					}
	                
					return true;
				}
				}, oper);
		} else {
			listener.onPointsOperationResult(AccountListener.RESULT_EMPTY_USER_ID, oper, -1, -1);
		}
			
		return true;
		
	}
	
	public void init(final Main mainAct){
//		Cfg.init(act);
		log.d("account init");
		String registerUrl;
		if(Cfg.mOldUserID != null && Cfg.mOldUserID.length() > 0){
			registerUrl = QUERY_URL + PARAM_UID + Cfg.mOldUserID + "&" + PARAM_NEW_UID + Cfg.mUserID;
		} else {
			registerUrl = QUERY_URL + PARAM_UID + Cfg.mUserID;
		}
		
		
		//用户之前已经有包月了，expireDay为秒数
		if (Cfg.mIsVIP) {
			long expireSeconds = (Cfg.mServiceExpireTime - System.currentTimeMillis()) / 1000;
			
			//已过期
			if(expireSeconds > 0){
				registerUrl += "&" + PARAM_VIP_EXPIRE + expireSeconds;
			}
			
		}
		//增加查询vip服务，如果用户没有注册过，则这个参数无用
		//如果用户已经注册过，那么会保证用户以前的vip状态不丢失
		registerUrl += "&" + PARAM_SERTICE + "2";
		
		httpRequestUrl(registerUrl, new httpListener() {
			
			@Override
			public boolean onGetData(JSONObject object) {

				mainAct.showContentProgress(false, "");
				
				if(object != null){		
					try {
						String status = object.getString(JSON_STATUS);
						if(status.equalsIgnoreCase(JSON_STATUS_OK)){
							String serviceState;

							
							//用户本地数据丢失，重新注册时获取到了以前开通的vip
							if(object.has(JSON_SERVICE_STATUS)){
								serviceState = object.getString(JSON_SERVICE_STATUS);
								int expireDay = Integer.parseInt(serviceState);
								
								//返回的expireDay是秒数
								if(expireDay > 0){
									
									Cfg.mServiceExpireTime = System.currentTimeMillis() + expireDay * 1000L;
									Cfg.saveLong(mainAct.getApplicationContext(), Cfg.SERVICE_EXPIRE_TIME, Cfg.mServiceExpireTime);
									
									Cfg.mIsVIP = true;
									Cfg.saveBool(mainAct.getApplicationContext(), Cfg.IS_VIP, Cfg.mIsVIP);
								}
								
							}
							
	//						if(object.has(JSON_WAPS_POINT)){
	//							String wapsPoint = object.getString(JSON_WAPS_POINT);
	//							final int point = Integer.parseInt(wapsPoint);
	//							
	//							try {
	//								if(point > 0 ){
	//									mainAct.runOnUiThread(new Runnable() {
	//										@Override
	//										public void run() {
	//											CommonDlg.showInfoDlg(mainAct, "账户升级提示",
	//													"尊敬的用户您好，由于系统升级，您的积分已兑换为虚拟道具扇贝。为了答谢您的大力支持，我们再赠送您同等数量的扇贝。您之前的积分数为" 
	//													+ point + "个，现在扇贝数为" + point * 2 + "个。");
	//										}
	//									});
	//									
	//								}
	//							} catch (Exception e) {
	//								// TODO: handle exception
	//							}
	//						}										

//							mainAct.runOnUiThread(new Runnable() {
//								
//								@Override
//								public void run() {
//									// TODO Auto-generated method stub
							if (Cfg.SDK_VERSION > Build.VERSION_CODES.ECLAIR_MR1) {
								mainAct.onAccountInited();
							}
							
							if (Cfg.mIsVIP) {
								MyToast.showLong(mainAct, "欢迎回来，尊贵的VIP会员！");
							}
//								}
//							});							
							
							Cfg.mAccountInited = true;
							Cfg.saveBool(mainAct, Cfg.ACCOUNT_INIT, Cfg.mAccountInited);
							Cfg.saveBool(mainAct, Cfg.PREF_CHECK_CDMA_IMEI, true);
							Cfg.saveStr(mainAct, Cfg.PREF_USER_ID, Cfg.mUserID);
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				if (!Cfg.mAccountInited) {
					MyToast.showLong(mainAct, "善听：初始化失败，请检查网络连接！");
					mainAct.finish();
				}
				
				return true;
				
			}  //onGetData
		}, 0);
	}
	
	/**
	 * http请求操作
	 * 
	 * @param url
	 * @param listener
	 * @param oper
	 */
	public void httpRequestUrl(final String url, final httpListener listener, final int oper){
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
//				Looper.prepare();
				if (url != null) {
					HttpGet httpGet = new HttpGet(url);
					int res = 0;
					JSONObject resultObj = null;
					try {
						log.d("httpGet:" + url);
//						HttpClient httpClient = new DefaultHttpClient();
						MyHttpClient httpClient = MyHttpClient.getInstance();
						HttpResponse httpResponse = httpClient.execute(httpGet);
						res = httpResponse.getStatusLine().getStatusCode();
						if (res == 200) {

							StringBuilder builder = new StringBuilder();
							BufferedReader bufferedReader2 = new BufferedReader(
									new InputStreamReader(httpResponse
											.getEntity().getContent()));
							for (String s = bufferedReader2.readLine(); s != null; s = bufferedReader2
									.readLine()) {

								builder.append(s);
							}

							JSONObject jsonObject = new JSONObject(builder
									.toString());
							resultObj = jsonObject;
//							listener.onGetData(jsonObject);

						} else {
//							listener.onGetData(null);
						}

					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
//						listener.onGetData(null);
					}
					final JSONObject result = resultObj;
					//回调数据再handler中执行，已确保可以更新主线程的UI
					Handler resultHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
						
						@Override
						public boolean handleMessage(Message msg) {
							listener.onGetData(result);
							return false;
						}
					});
					resultHandler.sendEmptyMessage(0);
					
				}
//				Looper.loop();
			}
		}).start();
	}
	
	public String getBuyServiceUrl(){
		String url = BUY_MONTHLY_RENT_SERVICE_PAGE + PARAM_UID + Cfg.mUserID;
		//#if NOWAPS
		url += "&" + PARAM_WAPS + "1";
		//#endif
		return url;
	}
	
	public String getAccountCenterUrl(){
		String url = ACCOUNT_CENTER_URL + PARAM_UID + Cfg.mUserID;
		//#if NOWAPS
		url += "&" + PARAM_WAPS + "1";
		//#endif
		return url;
	}
	
	public String getEarnShanbeiUrl(){
		StringBuilder sb = new StringBuilder(EARN_SHANBEI_URL);
		sb.append(PARAM_UID).append(Cfg.mUserID);
		
		return sb.toString(); 
	}
	
	public String getAwardBindUrl(int type){
		String value = PARAM_VALUE_BIND_QQ;
		if(type == AccountConnect.SNS_TYPE_SINA_WEIBO){
			value = PARAM_VALUE_BIND_SINA;
		}
		StringBuilder sb = new StringBuilder(BUY_SERVICE_URL);
		sb.append(PARAM_UID).append(Cfg.mUserID)
			.append("&").append(PARAM_ACT).append(value);
		
		return sb.toString();
	}
	
	
	/**
	 * 
	 * 
	 */
	public interface AccountListener{
		
		public static final int RESULT_OK = 0;
		public static final int RESULT_FAIL = -1;
		public static final int RESULT_NETWORK_ERR = -2;
		public static final int RESULT_EMPTY_USER_ID = -3;
		public static final int RESULT_FALSE = -4;
		
		
		/**
		 * 积分操作回调
		 * 
		 * @param resultCode	结果
		 * @param operType		操作类型
		 * @param points		操作后的积分，resultCode=RESULT_OK时才有效
		 * @return
		 */
		public boolean onPointsOperationResult(int resultCode, int operType,  int wastePoints, int totalPoints);
	}
	
	public interface httpListener{
		public boolean onGetData(JSONObject object);
	}
	
}
