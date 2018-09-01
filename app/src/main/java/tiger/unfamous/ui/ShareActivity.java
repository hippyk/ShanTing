package tiger.unfamous.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.utils.AccountConnect;
import tiger.unfamous.utils.AccountConnect.ILoginCallback;
import tiger.unfamous.utils.Utils;
import tiger.unfamous.wxapi.WXEntryActivity;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.tencent.tauth.http.Callback;
import com.umeng.analytics.MobclickAgent;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.WeiboErrorCode;
import com.weibo.sdk.android.sso.SsoHandler;

public class ShareActivity extends Activity implements OnClickListener{

//	private static final String TAG = "ShareActivity";
//	private static final String CALLBACK = "http://shantingshu.com";
	
	private static final int TOAST_SUCCESS = 0;
	private static final int TOAST_FAIL = 1;
	private static final int WAITING = 2;
	private static final int STOP_WAITING = 3;
	private static final int ENTER_SHARE_DETAIL = 4;
	
	
	private static final int SHARE_QZONE = 3;
	private static final int SHARE_QQ_WEIBO = 4;
	private static final int SHARE_SINA_WEIBO = 2;
	private static final int SHARE_WEIXIN = 1;
	private static final int SHARE_WEIXIN_TIMELINE = 0;
	private static final int SHARE_MORE = 5;
	
	private static final int MAX_CONTENT_LENGTH = 130;

	private Context mContext;

	private View qShareMenuView;
	private ListView qShareListView;
	private View qShareDetailView;
	private CheckBox qFollowBox;
	
	private EditText mShareContent;
	private Button mBack;
	
	private String  mDescription = "";
	private String  mSongName = "";
	private String  mShareResUrl;
	private String  mShareResTitle;
	private String mShareReferPicUrl;
	private String  mShareDetailCacheString;
	// private byte mShareEntrance;
	private AccountConnect mAccountConnectInstance;
	
	private int mShareTo;
	
	ProgressDialog mProcessDialog;
	
	// sina weibo sso
	private SsoHandler mSsoHandler;
	
	private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {

			String content;
			String blogName = "@善听听书";
			if(position == SHARE_QQ_WEIBO){
				blogName = "@shantingtingshu";
			}
			if(mSongName != null && mSongName.length() > 0){
				content = "我正在用 " + blogName + "( " + DN.OFFICAL_SITE + " )收听 《" + mSongName + "》";
				if(mDescription != null && mDescription.length() > 0){ 
					content += " ," + mDescription;
				} else {//本地资源没有描述
					content += " , 新潮的”阅读“方式，快来体验一下吧！";
				}
			} else { //分享软件本身是没有名字也没有描述的
				content = getString(R.string.default_recommend_content);
			}
			mShareDetailCacheString = content;
			
			switch (position) {
			case SHARE_QZONE: // 分享到QQ空间
			case SHARE_QQ_WEIBO: // 分享到微博
				if (position == SHARE_QZONE) {
					mShareTo = AccountConnect.SHARE_TO_Q_ZONE;
				} else {
					mShareTo = AccountConnect.SHARE_TO_Q_WEIBO;
				}

				if (mAccountConnectInstance.hasQQAlreadyLogin()) {
					enterShareDetail(content);
				} else {

					mHandler.obtainMessage(WAITING, "正在登陆...")
							.sendToTarget();

					mAccountConnectInstance
							.qqLoginReq(mLoginCallback);
					// auth(Q_APP_ID, "_self");
				}
				break;
			case SHARE_SINA_WEIBO:	//分享到新浪微博
				mShareTo = AccountConnect.SHARE_TO_WEIBO;
				if(mAccountConnectInstance.hasSinaWeiboLogin()){
					enterShareDetail(content);
				} else {
					if(mSsoHandler == null) {
						Weibo weibo = Weibo.getInstance(AccountConnect.SINA_APP_KEY,AccountConnect.SINA_CALLBACK_URL);
					    mSsoHandler = new SsoHandler((Activity) mContext,weibo);
					}
					mAccountConnectInstance.weiboLoginReq(mSsoHandler,mLoginCallback);
				}
						
				break;
			case SHARE_MORE: // 分享更多
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_SUBJECT, "分享");

				intent.putExtra(Intent.EXTRA_TEXT, content);
				startActivity(Intent.createChooser(intent, "分享"));

				break;
			case SHARE_WEIXIN: //分享到微信
			case SHARE_WEIXIN_TIMELINE:
			{
				Intent i = new Intent(ShareActivity.this, WXEntryActivity.class);
				i.putExtra(DN.URL, mShareResUrl);
				content = "";
				
				if(mDescription != null && mDescription.length() > 0){
					content += mDescription;
					i.putExtra(AccountConnect.INTENT_SHARE_TITLE, "《" + mSongName + "》");
					
				} else {
					content += getString(R.string.default_wx_recommend_content);
					i.putExtra(AccountConnect.INTENT_SHARE_TITLE, "我在听《" + mSongName + "》");
					
				}
				
				if(content != null && content.length() > 0){
					i.putExtra(AccountConnect.INTENT_SHARE_CONTENT, content);
				} else {
					i.putExtra(AccountConnect.INTENT_SHARE_CONTENT, getString(R.string.default_wx_recommend_content));
				}
				if(position == SHARE_WEIXIN_TIMELINE){
					i.putExtra(AccountConnect.INTENT_IS_WX_TIMELINE, true);
				}
				
				startActivity(i);
			}
				
				break;

			default:
				break;
			}
		}
	};

	private final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case TOAST_SUCCESS:
				MyToast.showShort(mContext, (String) msg.obj);
				if (msg.arg1 == 1) {
					setContentView(qShareMenuView);
					initShareMenuList();
				}
				break;
			case TOAST_FAIL:
				/*if (msg.arg1 == 3012) {
					MyToast.showShort(mContext, "重复分享！");
				} else */
				if (msg.arg1 == WeiboErrorCode.EXPIRED_TOKEN || msg.arg1 == WeiboErrorCode.EXPIRED_TOKEN) {  //token过期
					mAccountConnectInstance.realse(AccountConnect.SNS_TYPE_SINA_WEIBO);
					if(mSsoHandler == null) {
						Weibo weibo = Weibo.getInstance(AccountConnect.SINA_APP_KEY,AccountConnect.SINA_CALLBACK_URL);
					    mSsoHandler = new SsoHandler((Activity) mContext,weibo);
					}
					mAccountConnectInstance.weiboLoginReq(mSsoHandler,mLoginCallback);
					MyToast.showShort(mContext, "新浪微博授权过期，请重新登陆！");
				} else {
					MyToast.showShort(mContext, (String) msg.obj + msg.arg1);
				}
				
				break;

			case WAITING:
				if(mProcessDialog == null){
					mProcessDialog = new ProgressDialog(mContext);
				}
				mProcessDialog.setMessage((String) msg.obj);
				mProcessDialog.show();
				break;
			case STOP_WAITING:
				try{
					if(mProcessDialog != null){
						mProcessDialog.dismiss();
					}
				}catch (Exception e) {
					// TODO: handle exception
				}
				mProcessDialog = null;
				break;
			case ENTER_SHARE_DETAIL:
				enterShareDetail(null);
				break;
			}
		};
	};
	
	ILoginCallback mLoginCallback = new ILoginCallback() {

		@Override
		public void onSuccess(JSONObject result) {
			mHandler.obtainMessage(TOAST_SUCCESS,
					"成功登录").sendToTarget();
			mHandler.sendEmptyMessage(STOP_WAITING);
			mHandler.sendEmptyMessage(ENTER_SHARE_DETAIL);
		}

		@Override
		public void onFail(int ret, String msg) {
			mHandler.obtainMessage(TOAST_FAIL, ret,
					0, "登录失败:" + msg)
					.sendToTarget();
			mHandler.sendEmptyMessage(STOP_WAITING);

		}
	};
	
	Callback mShareCallback = new Callback() {

		@Override
		public void onSuccess(final Object obj) {
			mHandler.obtainMessage(TOAST_SUCCESS, 1, 0, "分享成功")
					.sendToTarget();
			mHandler.sendEmptyMessage(STOP_WAITING);
		}

		@Override
		public void onFail(final int ret, final String msg) {
			mHandler.obtainMessage(TOAST_FAIL, ret, 0, "分享失败:" + msg)
					.sendToTarget();
			mHandler.sendEmptyMessage(STOP_WAITING);
		}
	};
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

		mAccountConnectInstance = new AccountConnect(mContext);

		// 初始化传递过来的分享内容
        Bundle bundle = getIntent().getExtras();
        mDescription = getStringInBundle(bundle,
				AccountConnect.INTENT_SHARE_DESCRIPTION,
				"");
        
        mSongName = getStringInBundle(bundle,
				AccountConnect.INTENT_SHARE_SONG_NAME,
				"");

		mShareResUrl = getStringInBundle(bundle,
				AccountConnect.INTENT_SHARE_URL,
				mAccountConnectInstance
						.getQZoneShareUrl());
		mShareResTitle = getStringInBundle(bundle,
				AccountConnect.INTENT_SHARE_TITLE, getString(R.string.default_share_content_title));
		mShareReferPicUrl = getStringInBundle(bundle,
				AccountConnect.INTENT_SHARE_PICURL, "");
		// url 编码
		mShareReferPicUrl = Utils.encodeURL(mShareReferPicUrl);
		// mShareResUrl = Utils.encodeURL(mShareResUrl);

		// 初始化QAuth说需要的内容
        
        LayoutInflater inflater = LayoutInflater.from(this);
		qShareMenuView = inflater.inflate(R.layout.share_menu, null);
		qShareListView = (ListView) qShareMenuView
				.findViewById(R.id.sharemenu);
		qShareDetailView = inflater.inflate(R.layout.share_detail, null);
        
		setContentView(qShareMenuView);
		initShareMenuList();

		mBack = (Button) findViewById(R.id.rightbtn);
		mBack.setText("返回");
		mBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(
						KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
			}
		});
		
		TextView mTopTitle = (TextView) findViewById(R.id.title);
		mTopTitle.setText("分享");

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (!qShareMenuView.isShown()) {
				setContentView(qShareMenuView);
				initShareMenuList();
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);

	}

	/**
	 * 进入分享详情界面
	 * 
	 * @param shareType
	 */
	public void enterShareDetail(String defaultConent) {
		if(defaultConent == null || defaultConent.length() <= 0){
			defaultConent = mShareDetailCacheString;
			mShareDetailCacheString = null;
		}
		
		if (defaultConent.length() > MAX_CONTENT_LENGTH) {
			defaultConent = defaultConent.substring(0, MAX_CONTENT_LENGTH);
		}
		
		setContentView(qShareDetailView);
		Utils.setColorTheme(findViewById(R.id.night_mask));
		qFollowBox = (CheckBox) qShareDetailView
				.findViewById(R.id.share_qfollow);
		Button doShareButton = (Button) qShareDetailView
				.findViewById(R.id.share_do);
		doShareButton.setOnClickListener(this);

		mShareContent = (EditText) findViewById(R.id.share_content);
		mShareContent.setText(defaultConent);

		switch (mShareTo) {
		case AccountConnect.SHARE_TO_Q_WEIBO:
		case AccountConnect.SHARE_TO_WEIBO:
			qFollowBox.setVisibility(View.VISIBLE);
			break;
		default:
			qFollowBox.setVisibility(View.GONE);
			break;
		}
		TextView mTopTitle = (TextView) findViewById(R.id.title);
		mTopTitle.setText("分享");
		mBack = (Button) findViewById(R.id.rightbtn);
		mBack.setText("返回");

		mBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(
						KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
			}
		});

	}

	private void initShareMenuList() {

		SimpleAdapter adapter = new SimpleAdapter(this, mapListSetting(),
				R.layout.share_item, new String[] { "logo", "text" },
				new int[] {
						R.id.share_logo, R.id.share_text });
		qShareListView.setAdapter(adapter);
		qShareListView.setOnItemClickListener(mOnItemClickListener);

	}

	private List<Map<String, Object>> mapListSetting() {

		List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();

		
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		map.put("logo", R.drawable.wx_py_icon);
		map.put("text", getString(R.string.share_weixin_timeline));
		mapList.add(map);
		
		map = new HashMap<String, Object>();
		map.put("logo", R.drawable.logo_weixin);
		map.put("text", getString(R.string.share_weixin));
		mapList.add(map);
		
		map = new HashMap<String, Object>();
		map.put("logo", R.drawable.sina_logo);
		map.put("text", "分享到新浪微博");
		mapList.add(map);
		
		map = new HashMap<String, Object>();
		map.put("logo", R.drawable.logo_qzone);
		map.put("text", "分享到QQ空间");
		mapList.add(map);

		map = new HashMap<String, Object>();
		map.put("logo", R.drawable.logo_tencent);
		map.put("text", "分享到腾讯微博");
		mapList.add(map);

		
		map = new HashMap<String, Object>();
		map.put("logo", R.drawable.logo_sharemore);
		map.put("text", "分享到更多地方");
		mapList.add(map);

		return mapList;
	}
	
	@Override
	public void onClick(View v) {
		if(v == null){
			return ;
		}
		// 隐藏输入法
		if (mShareContent != null) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mShareContent.getWindowToken(), 0);
		}
		
		mHandler.obtainMessage(WAITING, "正在分享...").sendToTarget();

		switch(v.getId()){

		case R.id.share_do:
			boolean isFollow = false;
			if (qFollowBox != null && qFollowBox.isShown()
					&& qFollowBox.isChecked()) {
				isFollow = true;
			}

			String content;
			if (mShareContent != null
					&& mShareContent.getText().toString().length() > 0) {
				content = mShareContent.getText().toString();
			} else {
				content = mDescription;
			}
			

			switch(mShareTo){
			case AccountConnect.SHARE_TO_Q_ZONE:
			case AccountConnect.SHARE_TO_Q_WEIBO:
				mAccountConnectInstance.qShare(mShareTo, content,
						mShareResUrl, mShareResTitle, mShareReferPicUrl,
						mShareCallback,
						isFollow);
				break;
			case AccountConnect.SHARE_TO_WEIBO:
				mAccountConnectInstance.shareToWeibo(content, mShareResUrl, isFollow, mShareCallback);
				break;
			}
			break;
		}
}


	
	private String getStringInBundle(Bundle bundle, String key,
			String defaultValue) {
		if (bundle == null || false == bundle.containsKey(key)) {
			return defaultValue;
		}
		String value = bundle.getString(key);
		if (value != null && value.length() > 0) {
			return value;
		}
		return defaultValue;
	}
	
	@Override
		protected void onResume() {
			// TODO Auto-generated method stub
			Utils.setColorTheme(findViewById(R.id.night_mask));
			super.onResume();
			MobclickAgent.onResume(this);
		}

	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// 新浪微博sso授权后跳转回来
		if (mSsoHandler != null) {
			mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
		}
	}
}
