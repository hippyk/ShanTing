package tiger.unfamous.wxapi;


import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.utils.AccountConnect;
import tiger.unfamous.utils.CommonDlg;

import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.ConstantsAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.ShowMessageFromWX;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXAppExtendObject;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.umeng.analytics.MobclickAgent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler{
	
	
    private IWXAPI api;
    public static final String WX_APP_ID = "wxd4b85085b3bd7126";
    
    /**
     * 低于此版本的微信不支持分享到朋友圈
     */
    private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;
    public static final int MSG_SHOW_TOAST = 0;
    public static final int MSG_SHOW_DIALOG = 1;
    public static final int MSG_SHARE_DONE = 2;
    String mShareUrl;
    String mTitle;
    boolean mIsSupportTimeLine = true;
    CheckBox mTimeLineBox;
    EditText mContentView;
    boolean mIsToTimeLine;
    
    public View.OnClickListener mDoShareBtnListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			String content = "";
//			boolean isTimeLine = false;
			if(mContentView != null){
				content = mContentView.getText().toString();
			}
//			if(mIsSupportTimeLine && mTimeLineBox != null && mTimeLineBox.isChecked()){
//				isTimeLine = true;
//			}
			AccountConnect accouts = new AccountConnect(WXEntryActivity.this);
			
			boolean result = accouts.shareToWeiXin(WXEntryActivity.this, mTitle, mShareUrl, content, mIsToTimeLine);
			MobclickAgent.onEvent(WXEntryActivity.this, Cfg.UM_WX_SHARE_CLICK);
			if(!result){
				mHandler.obtainMessage(MSG_SHOW_DIALOG, "分享失败，可能的原因是：\n1.您还没有装微信或版本太低\n2.您的善听认证错误，可能已被修改，请从官方渠道下载").sendToTarget();
			} 
		}
	};
    
    private Handler mHandler = new Handler(new Handler.Callback() {
		
		@Override
		public boolean handleMessage(Message msg) {
			switch(msg.what){
			case MSG_SHOW_TOAST:
				MyToast.showLong(WXEntryActivity.this,  (String)msg.obj);
				break;
			case MSG_SHOW_DIALOG:
				CommonDlg.showInfoDlg(WXEntryActivity.this, "分享结果", (String)msg.obj);
//				MyToast.showShort(WXEntryActivity.this,  (String)msg.obj);
				break;
			case MSG_SHARE_DONE:
				MyToast.showLong(WXEntryActivity.this,  "分享成功");
				finish();
				break;
			}
			return false;
		}
	});
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_detail);
        
        Intent intent = getIntent();
        String contentString = null;
        
        if( intent != null ){
        	if(intent.hasExtra(DN.URL)){
        		mShareUrl = intent.getStringExtra(DN.URL);
        	}/* else if(intent.hasExtra(DN.EXTRA_SHORTCUT_SONG_PATH)){
        		mShareUrl = intent.getStringExtra(DN.EXTRA_SHORTCUT_SONG_PATH);
        	}*/
        	
        	if(intent.hasExtra(AccountConnect.INTENT_SHARE_TITLE)){
        		mTitle = intent.getStringExtra(AccountConnect.INTENT_SHARE_TITLE);
        	}
        	
        	if(intent.hasExtra(AccountConnect.INTENT_SHARE_CONTENT)){
        		contentString = intent.getStringExtra(AccountConnect.INTENT_SHARE_CONTENT);
        	}
        	if(intent.hasExtra(AccountConnect.INTENT_IS_WX_TIMELINE)){
        		mIsToTimeLine = intent.getBooleanExtra(AccountConnect.INTENT_IS_WX_TIMELINE, false);
        	}
        }
        
        api = WXAPIFactory.createWXAPI(this, WX_APP_ID, true);
        api.registerApp(WX_APP_ID);
//        if(api.getWXAppSupportAPI() >= TIMELINE_SUPPORTED_VERSION){
//        	mIsSupportTimeLine = true;
//        } else {
        	mIsSupportTimeLine = false;
//        }
        
        mTimeLineBox = (CheckBox) findViewById(R.id.share_qfollow);
        if(mTimeLineBox != null ){
        	mTimeLineBox.setVisibility(View.GONE);
        }
//        else if(mTimeLineBox != null){
//        	mTimeLineBox.setText("分享到朋友圈");
//        	mTimeLineBox.setChecked(true);
//        }
        
        Button doShareBtn = (Button) findViewById(R.id.share_do);
        mContentView = (EditText)findViewById(R.id.share_content);
        if(contentString != null){
        	mContentView.setText(contentString);
        }
        
        doShareBtn.setOnClickListener(mDoShareBtnListener);
        Button mBack = (Button) findViewById(R.id.rightbtn);
		mBack.setText(getString(R.string.download_back));
		mBack.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(
						KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
			}
		});
		TextView mTopTitle = (TextView) findViewById(R.id.title);
		mTopTitle.setText(mIsToTimeLine ? R.string.share_weixin_timeline : R.string.share_weixin);
		
        api.handleIntent(getIntent(), this);
    }

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
        api.handleIntent(intent, this);
	}

	@Override
	public void onReq(BaseReq req) {
		switch (req.getType()) {
		case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
			goToGetMsg();		
			break;
		case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
			goToShowMsg((ShowMessageFromWX.Req) req);
			break;
		default:
			break;
		}
		
		mHandler.obtainMessage(MSG_SHOW_TOAST, "onReq: " + req.getType()).sendToTarget();
	}

	@Override
	public void onResp(BaseResp resp) {
		int result = 0;
		String msg = "";
		switch (resp.errCode) {
		case BaseResp.ErrCode.ERR_OK:
			msg = "分享成功";
			result = 1;
			break;
		case BaseResp.ErrCode.ERR_USER_CANCEL:
			result = 2;
			msg = "已取消分享";
			break;
		case BaseResp.ErrCode.ERR_AUTH_DENIED:
			result = 3;
			msg = "分享失败：授权被拒绝";
			break;
		default:
			result = 4;
			msg = "分享失败：" + resp.errStr;
			break;
		}
		if(resp.errCode == BaseResp.ErrCode.ERR_OK){
			mHandler.obtainMessage(MSG_SHARE_DONE).sendToTarget();
			MobclickAgent.onEvent(WXEntryActivity.this, Cfg.UM_WX_SHARE_DONE);
		} else {
			mHandler.obtainMessage(MSG_SHOW_TOAST, msg).sendToTarget();
		}
	}
	
	private void goToGetMsg() {
//		Intent intent = new Intent(this, GetFromWXActivity.class);
//		intent.putExtras(getIntent());
//		startActivity(intent);
//		finish();
	}
	
	private void goToShowMsg(ShowMessageFromWX.Req showReq) {
		WXMediaMessage wxMsg = showReq.message;		
		WXAppExtendObject obj = (WXAppExtendObject) wxMsg.mediaObject;
		
		StringBuffer msg = new StringBuffer(); // ��֯һ�����ʾ����Ϣ����
		msg.append("description: ");
		msg.append(wxMsg.description);
		msg.append("\n");
		msg.append("extInfo: ");
		msg.append(obj.extInfo);
		msg.append("\n");
		msg.append("filePath: ");
		msg.append(obj.filePath);
//		mHandler.obtainMessage(MSG_SHOW_TOAST, msg.toString());
	}
	@Override
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
}