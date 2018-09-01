package tiger.unfamous.alipay;

import java.net.URLEncoder;
import org.json.JSONObject;

import tiger.unfamous.Cfg;
import tiger.unfamous.ui.WebBrowser;
import tiger.unfamous.utils.ShanTingAccount;
import tiger.unfamous.utils.Utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

public class AliPay {

	public static final int RESULT_INVALID_PARAM = 0;
	public static final int RESULT_CHECK_SIGN_FAILED = 1;
	public static final int RESULT_CHECK_SIGN_SUCCEED = 2;
	

	public static String NOTIFY_URL = Cfg.WEB_HOME + "/e/payapi/payfen.php";
	
	
	Context				mContext;
//	Activity 			mActivity;
	public static final String TAG = "AliPay";
	
	
	private ProgressDialog mProgress = null;
	private final WebBrowser		mWebBrowser;
	
	
	//
	// the handler use to receive the pay result.
	// 这里接收支付结果，支付宝手机端同步通知
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			try {
				String strRet = (String) msg.obj;

				switch (msg.what) {
				case AlixId.RQF_PAY: {
					//
					closeProgress();

					BaseHelper.log(TAG, strRet);

					boolean isSuccess = false;
					int retVal = RESULT_CHECK_SIGN_SUCCEED;
					String showMsg = "支付失败";
					// 从通知中获取参数
					try {
					
						// 获取交易状态，具体状态代码请参看文档
						JSONObject msgJSON = BaseHelper.string2JSON(strRet, ";");
						String result = msgJSON.getString("result");
						String content = result.substring(0, result.indexOf("&sign_type"));
						
						//memo信息
//						String memo = msgJSON.getString("memo");
						//状态码
						String statusCode = msgJSON.getString("resultStatus");
						
						//结果JSON
						JSONObject resultJSON = BaseHelper.string2JSON(result, "&");
						
						String sign = resultJSON.getString("sign");
						String signType = resultJSON.getString("sign_type");
//						String outTradeNo = resultJSON.getString("out_trade_no");
//						String subject = resultJSON.getString("subject");
						String body = resultJSON.getString("body");
						String success = resultJSON.getString("success");
						
						//交易成功标识：1.状态码9000, 2.success=true, 3.验签通过
						//1和2成功则支付成功，3失败说明有可能订单被篡改
						isSuccess = statusCode.equalsIgnoreCase("9000") && success.equalsIgnoreCase("true");
						
						try{
							if (signType.equalsIgnoreCase("RSA")) {
								if (!Rsa.doCheck(content, sign,
										PartnerConfig.RSA_ALIPAY_PUBLIC))
									retVal = RESULT_CHECK_SIGN_FAILED;
							}else{
								retVal = RESULT_INVALID_PARAM;
							}
						}catch (Exception e) {
							retVal = RESULT_CHECK_SIGN_FAILED;
						}
						
						
						if(isSuccess && retVal == RESULT_CHECK_SIGN_SUCCEED){
							showMsg = body + "\r\n马上开通服务体验吧";
							mWebBrowser.refreshUserCenter();
						}else {
							if(isSuccess){
								showMsg = "支付成功，但校验失败，您的 订单有可能被篡改。\r\n马上开通服务体验吧";
							}else{
								showMsg = "错误状态：" + statusCode;
							}
							
						}                 
													

					} catch (Exception e) {
						e.printStackTrace();
					}
					
					BaseHelper.showDialog(mContext, isSuccess ? "支付成功" : "支付失败",
							showMsg, android.R.drawable.ic_dialog_info);
				}
					break;
				}

				super.handleMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	public AliPay(Context context, WebBrowser browser){
		mContext = context;
		mWebBrowser = browser;
		
		//1. 检测安全支付服务是否安装
//		MobileSecurePayHelper mspHelper = new MobileSecurePayHelper(mContext);
//		boolean isMobile_spExist = mspHelper.detectMobile_sp();
		
	}
	
	/**
	 * 
	 * service类型
	 * @param serviceType		服务类型
	 * @param expireDay			过期时间
	 */
	public void onBuyServiceDone(int serviceType, int expireDay){
		
		if(serviceType == ShanTingAccount.OPER_BUY_VIP1){
//			Log.d(TAG, "onBuyServiceDone: is=" + Cfg.mIsVIP + ",time=" + Cfg.mServiceExpireTime);
			if(Cfg.mIsVIP && Cfg.mServiceExpireTime > 0L){
				Cfg.mServiceExpireTime = Cfg.mServiceExpireTime + expireDay * Cfg.MS_PER_DAY;
			}else {
				Cfg.mServiceExpireTime = System.currentTimeMillis() + expireDay * Cfg.MS_PER_DAY;
			}
			Cfg.saveLong(mContext.getApplicationContext(), Cfg.SERVICE_EXPIRE_TIME, Cfg.mServiceExpireTime);
			
			Cfg.mIsVIP = true;
			Cfg.saveBool(mContext.getApplicationContext(), Cfg.IS_VIP, Cfg.mIsVIP);
			
//			Log.d(TAG, "onBuyServiceDone-2: is=" + Cfg.mIsVIP + ",time=" + Cfg.mServiceExpireTime);
		}
		
	}
	
	
	/**
	 * 支付请求
	 * 
	 * @param price		价格以分计
	 * @param msg		支付信息
	 * @param body		支付详情
	 * @param tradeNo	订单号
	 * 
	 */
	public void payRequest(int price, String subject, String body, String tradeNo){
		
		//1. 检测安全支付服务是否安装
		MobileSecurePayHelper mspHelper = new MobileSecurePayHelper(mContext);
		boolean isMobile_spExist = mspHelper.detectMobile_sp();
		
		
		if (!isMobile_spExist)
			return;
		
		if (!checkInfo()) {
			BaseHelper
					.showDialog(
							mContext,
							"提示",
							"缺少partner或者seller，请在src/com/alipay/android/appDemo4/PartnerConfig.java中增加。",
							android.R.drawable.ic_dialog_info);
			return;
		}
		
		
		// 根据订单信息开始进行支付
		try {
			// prepare the order info.
			// 准备订单信息
			String orderInfo = getOrderInfo(price, subject, body, tradeNo);
			// 这里根据签名方式对订单信息进行签名
			String signType = getSignType();
			String strsign = sign(signType, orderInfo);
			// 对签名进行编码
			strsign = URLEncoder.encode(strsign);
			// 组装好参数
			String info = orderInfo + "&sign=" + "\"" + strsign + "\"" + "&"
					+ getSignType();
			// start the pay.
			// 调用pay方法进行支付
			MobileSecurePayer msp = new MobileSecurePayer();
			boolean bRet = msp.pay(info, mHandler, AlixId.RQF_PAY, mWebBrowser);

			if (bRet) {
				// show the progress bar to indicate that we have started
				// paying.
				// 显示“正在支付”进度条
				closeProgress();
				mProgress = BaseHelper.showProgress(mContext, null, "正在支付", false,
						true);
			} else
				;
		} catch (Exception ex) {
			Toast.makeText(mWebBrowser, tiger.unfamous.R.string.remote_call_failed,
					Toast.LENGTH_SHORT).show();
		}
	}
	
	
	/**
	 * get the selected order info for pay. 获取商品订单信息
	 * 
	 * @param position
	 *            商品在列表中的位置
	 * @return
	 */
	String getOrderInfo(int price, String subject, String body, String tradeNo) {
		String strOrderInfo = "partner=" + "\"" + PartnerConfig.PARTNER + "\"";
		strOrderInfo += "&";
		strOrderInfo += "seller=" + "\"" + PartnerConfig.SELLER + "\"";
		strOrderInfo += "&";
		strOrderInfo += "out_trade_no=" + "\"" + tradeNo + "\"";
		strOrderInfo += "&";
		strOrderInfo += "subject=" + "\"" + subject + "\"";
		strOrderInfo += "&";
		strOrderInfo += "body=" + "\"" + body + "\"";
		strOrderInfo += "&";
		strOrderInfo += "total_fee=" + "\""
				+ price / 100.0 + "\"";
		strOrderInfo += "&";
		strOrderInfo += "notify_url=" + "\""
				+ NOTIFY_URL + "\"";

		return strOrderInfo;
	}
	
	
	/**
	 * get the out_trade_no for an order.
	 * 获取外部订单号,订单号格式：MMDDHHMMSS(userId)
	 * 
	 * @return
	 */
//	String getOutTradeNo() {
//		SimpleDateFormat format = new SimpleDateFormat("MMddHHmmss");
//		Date date = new Date();
//		String strKey = format.format(date);
//		strKey = strKey 
//		/*+ Cfg.mUserID */;
//		return strKey;
//	}
	
	/**
	 *  sign the order info.
	 *  对订单信息进行签名
	 *  
	 * @param signType	签名方式 
	 * @param content		待签名订单信息
	 * @return
	 */
	String sign(String signType, String content) {
		return Rsa.sign(content, PartnerConfig.RSA_PRIVATE);
	}
	
	
	/**
	 * check some info.the partner,seller etc.
	 * 检测配置信息
	 * partnerid商户id，seller收款帐号不能为空
	 * 
	 * @return
	 */
	private boolean checkInfo() {
		String partner = PartnerConfig.PARTNER;
		String seller = PartnerConfig.SELLER;
		if (partner == null || partner.length() <= 0 || seller == null
				|| seller.length() <= 0)
			return false;

		return true;
	}
	/**
	 * get the sign type we use.
	 * 获取签名方式
	 * 
	 * @return
	 */
	String getSignType() {
		String getSignType = "sign_type=" + "\"" + "RSA" + "\"";
		return getSignType;
	}
	
	void closeProgress() {
		try {
			if (mProgress != null) {
				mProgress.dismiss();
				mProgress = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static class AlixOnCancelListener implements DialogInterface.OnCancelListener {
		Activity mcontext;
		
		AlixOnCancelListener(Activity context) {
			mcontext = context;
		}
		
		@Override
		public void onCancel(DialogInterface dialog) {
			mcontext.onKeyDown(KeyEvent.KEYCODE_BACK, null);
		}
	}
}
	

