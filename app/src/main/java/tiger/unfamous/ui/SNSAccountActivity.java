package tiger.unfamous.ui;

import org.json.JSONObject;

import tiger.unfamous.Cfg;
import tiger.unfamous.R;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.utils.AccountConnect;
import tiger.unfamous.utils.AccountConnect.ILoginCallback;
import tiger.unfamous.utils.CommonDlg;
import tiger.unfamous.utils.ShanTingAccount;
import tiger.unfamous.utils.ShanTingAccount.AccountListener;
import tiger.unfamous.utils.Utils;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.sso.SsoHandler;

public class SNSAccountActivity extends Activity implements AccountListener {

	// private static final String TAG = "SNSAccountActivity";
	Button mBindbtn;
	AccountConnect mAccouts;
	ListView mAccoutListView;

	static final int BUTTON_ID_QQ = 1;
	static final int BUTTON_ID_SINA = 2;

	int bindQQAwardPoint = 50;
	int bindSinaAwardPoint = 50;

	int followQQAwardPoint = 50;
	int followSinaAwardPoint = 50;

	// 昵称
	String mNickName[];
	Button mBackBtn;
	Button mAwardBtn;

	ProgressDialog processDialog;
	private Context mContext;

	// boolean mFollowedQQ = false;
	// boolean mFollowedSina = false;

	// sina weibo sso
	private SsoHandler mSsoHandler;
		
	private final ILoginCallback mILoginCallback = new ILoginCallback() {

		@Override
		public void onSuccess(JSONObject result) {
			// 获取QQ昵称
			mHandle.sendEmptyMessage(STOP_WAITING);
			if (result.has("name")) {
				mHandle.obtainMessage(BINDING_OK, AccountConnect.SNS_TYPE_SINA_WEIBO, 0).sendToTarget();
			} else {
				mHandle.obtainMessage(BINDING_OK, AccountConnect.SNS_TYPE_QQ, 0).sendToTarget();
			}
		}

		@Override
		public void onFail(int ret, String msg) {
			mHandle.obtainMessage(STOP_WAITING, ret, 0, msg).sendToTarget();
		}
	};

	private final OnClickListener mLoginBtnListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Integer type = (Integer) v.getTag();
			String nickName;
			nickName = mNickName[type];

			if (nickName == null || nickName.length() <= 0) {
				switch (type) {
				case AccountConnect.SNS_TYPE_SINA_WEIBO:
					if(mSsoHandler == null) {
						Weibo weibo = Weibo.getInstance(AccountConnect.SINA_APP_KEY,AccountConnect.SINA_CALLBACK_URL);
					    mSsoHandler = new SsoHandler((Activity) mContext,weibo);
					}
					mAccouts.weiboLoginReq(mSsoHandler,mILoginCallback);
					break;
				default:
					mAccouts.qqLoginReq(mILoginCallback);
				}

				// mHandle.sendEmptyMessage(WAITING);
			} else {
				mNickName[type] = null;
				mAccouts.realse(type);

				mHandle.sendEmptyMessage(RELEASE_BINDING);
			}

		}
	};

	private AccountListener mAwardListener = new AccountListener() {

		@Override
		public boolean onPointsOperationResult(int resultCode, int operType, int wastePoints, int totalPoints) {
			Log.d("operType", "operType = " + operType + " totalPoints: " + totalPoints);
			int type = Utils.AWARD_FOLLOW_QQ;
			// int awardType = type;

			switch (operType) {
			case ShanTingAccount.OPER_AWARD_FOLLOW_QQ:
				type = Utils.AWARD_FOLLOW_QQ;
				break;
			case ShanTingAccount.OPER_AWARD_FOLLOW_SINA:
				type = Utils.AWARD_FOLLOW_SINA;
				break;
			case ShanTingAccount.OPER_AWARD_SHARE_WEIBO:
				type = Utils.AWARD_TYPE_SINA;
				break;
			case ShanTingAccount.OPER_AWARD_SHARE_QQ:
				type = Utils.AWARD_TYPE_QQ;
				break;
			default:
				break;
			}

			StringBuilder sBuilder = new StringBuilder();
			mHandle.sendEmptyMessage(STOP_WAITING);

			if (resultCode == 0 || Utils.hasAwardBefore(type)) {
				Utils.onAwardDone(SNSAccountActivity.this, type);

				sBuilder.append("恭喜！成功领取奖励，您当前扇贝余额为：").append(totalPoints);

			} else if (resultCode == AccountListener.RESULT_NETWORK_ERR) {
				sBuilder.append(getString(R.string.award_fail_unknow));
			} else if (resultCode == AccountListener.RESULT_EMPTY_USER_ID) {
				sBuilder.append("您的账号为空，无法发放奖励，请您联系我们的工作人员");
			} else {
				Utils.onAwardDone(SNSAccountActivity.this, type);
				sBuilder.append(getString(R.string.award_fail_already_awarded));
			}

			mHandle.obtainMessage(SHOW_DIALOG, type, 0, sBuilder.toString()).sendToTarget();

			return false;
		}
	};

	public static final int WAITING = 1;
	public static final int STOP_WAITING = 2;
	public static final int BINDING_OK = 0;
	public static final int RELEASE_BINDING = 3;
	public static final int SHOW_DIALOG = 4;
	public static final int FOLLOW_QQ_DONE = 5;
	public static final int FOLLOW_SINA_DONE = 6;
	// public static final int FOLLOW_AWARD_DONE = 7;
	// public static final int FOLLOW_AWARD_FAIL = 8;
	public static final int FOLLOW_FAIL = 9;

	private final Handler mHandle = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case BINDING_OK: // 刷新页面
				if (mAccoutListView.isShown()) {
					AccoutAdapter adapter = (AccoutAdapter) mAccoutListView.getAdapter();
					adapter.notifyDataSetChanged();

				}

				mNickName = mAccouts.getNickNames();
				MyToast.showShort(SNSAccountActivity.this, "绑定成功");
				setAwardButton();
				final int bindType = msg.arg1;
				String content;

				int awardMoney = 50;
				if (AccountConnect.SNS_TYPE_QQ == bindType) {
					awardMoney = followQQAwardPoint;
				} else if (AccountConnect.SNS_TYPE_SINA_WEIBO == bindType) {
					awardMoney = followSinaAwardPoint;
				}
				content = String.format(getString(R.string.follow_to_get_award), awardMoney);

				CommonDlg.showConfirmDlg(SNSAccountActivity.this, R.string.follow_confrim, content, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						mHandle.obtainMessage(WAITING, "正在关注，请稍候...").sendToTarget();
						// TODO Auto-generated method stub
						switch (bindType) {
						case AccountConnect.SNS_TYPE_SINA_WEIBO:

							mHandle.obtainMessage(STOP_WAITING).sendToTarget();
							mAccouts.shareToWeibo(getString(R.string.default_recommend_content), mAccouts.getQZoneShareUrl(), true, new com.tencent.tauth.http.Callback() {

								@Override
								public void onFail(int arg0, String arg1) {
									mHandle.sendEmptyMessage(FOLLOW_FAIL);

								}

								@Override
								public void onSuccess(Object arg0) {
									mHandle.sendEmptyMessage(FOLLOW_SINA_DONE);
								}

							});

							break;
						case AccountConnect.SNS_TYPE_QQ:

							mAccouts.qShare(AccountConnect.SHARE_TO_Q_WEIBO, getString(R.string.default_recommend_content), Utils.replaceMicroBlogName(mAccouts.getQZoneShareUrl(), AccountConnect.SHARE_TO_Q_WEIBO), getString(R.string.default_share_content_title), "", new com.tencent.tauth.http.Callback() {

								@Override
								public void onSuccess(Object arg0) {
									mHandle.obtainMessage(STOP_WAITING).sendToTarget();
									mHandle.sendEmptyMessage(FOLLOW_QQ_DONE);
								}

								@Override
								public void onFail(int arg0, String arg1) {
									mHandle.obtainMessage(STOP_WAITING).sendToTarget();
									mHandle.sendEmptyMessage(FOLLOW_FAIL);
								}
							}, true);

							break;
						}

					}
				});
				break;
			case WAITING:// 等待
				if (processDialog == null) {
					processDialog = new ProgressDialog(mContext);
				}
				processDialog.setMessage(msg.obj.toString());
				processDialog.show();
				break;
			case STOP_WAITING:// 停止等待
				try {
					if (processDialog != null) {
						processDialog.dismiss();
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
				processDialog = null;

				if (msg.arg1 < 0 && msg.obj != null) {
					MyToast.showLong(SNSAccountActivity.this, "登陆失败: " + msg.obj);
				}
				break;
			case RELEASE_BINDING:
				if (mAccoutListView.isShown()) {
					AccoutAdapter adapter = (AccoutAdapter) mAccoutListView.getAdapter();
					adapter.notifyDataSetChanged();

				}
				break;
			case SHOW_DIALOG:
				String title;
				switch (msg.arg1) {
				case Utils.AWARD_TYPE_QQ:
					title = "QQ绑定奖励";
					break;
				case Utils.AWARD_TYPE_SINA:
					title = "新浪绑定奖励";
					break;
				case Utils.AWARD_FOLLOW_QQ:
					title = "QQ关注奖励";
					break;
				case Utils.AWARD_FOLLOW_SINA:
					title = "新浪关注奖励";
					break;
				default:
					title = "信息";
				}
				CommonDlg.showInfoDlg(SNSAccountActivity.this, title, (String) msg.obj);
				setAwardButton();
				break;
			case FOLLOW_FAIL:
				MyToast.showShort(SNSAccountActivity.this, "关注失败，请您稍后再试");
				break;
			case FOLLOW_SINA_DONE:
				Cfg.mHasFollowedSina = true;
				Cfg.saveBool(SNSAccountActivity.this, Cfg.PREF_AWARD_FOLLOW_SINA, true);
				// mHandle.obtainMessage(WAITING,
				// getString(R.string.waiting_award)).sendToTarget();
				setAwardButton();
				MyToast.showShort(SNSAccountActivity.this, getString(R.string.thanks_for_follow));
				break;
			case FOLLOW_QQ_DONE:
				Cfg.mHasFollowedQQ = true;
				Cfg.saveBool(SNSAccountActivity.this, Cfg.PREF_AWARD_FOLLOW_QQ, true);
				MyToast.showShort(SNSAccountActivity.this, getString(R.string.thanks_for_follow));
				setAwardButton();
				break;
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sns_account_center);
		mContext = this;

		mAccouts = new AccountConnect(this);
		mNickName = mAccouts.getNickNames();

		mAccoutListView = (ListView) findViewById(R.id.accout_list);

		mAwardBtn = (Button) findViewById(R.id.get_award);
		setAwardButton();
		mAccoutListView.setAdapter(new AccoutAdapter());
		ShanTingAccount.instance().pointsOperation(ShanTingAccount.OPER_QUERY_AWARD_BIND_QQ, this);
		ShanTingAccount.instance().pointsOperation(ShanTingAccount.OPER_QUERY_AWARD_BIND_SINA, this);
		ShanTingAccount.instance().pointsOperation(ShanTingAccount.OPER_QUERY_AWARD_FOLLOW_QQ, this);
		ShanTingAccount.instance().pointsOperation(ShanTingAccount.OPER_QUERY_AWARD_FOLLOW_SINA, this);

		mBackBtn = (Button) findViewById(R.id.rightbtn);
		mBackBtn.setText("返回");
		mBackBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
			}
		});

		TextView mTopTitle = (TextView) findViewById(R.id.title);
		mTopTitle.setText("社区账号");

	}

	private class AccoutAdapter extends BaseAdapter {

		LayoutInflater mInflater;

		public AccoutAdapter() {
			mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.account_item, null);
			}

			ImageView figureImage = (ImageView) convertView.findViewById(R.id.sns_logo);
			TextView nameView = (TextView) convertView.findViewById(R.id.sns_name);
			Button loginBtn = (Button) convertView.findViewById(R.id.sns_binding_btn);

			int type = getSnsTypeByPosition(position);

			int logoResId = getLogoResIdByType(type);
			loginBtn.setTag(type);

			figureImage.setImageResource(logoResId);

			if (mNickName[type] != null && mNickName[type].length() > 0) {
				nameView.setText(mNickName[type]);
				nameView.setTextColor(android.graphics.Color.BLACK);
				loginBtn.setText("注销");
			} else {
				nameView.setText("未绑定");
				nameView.setTextColor(android.graphics.Color.GRAY);
				loginBtn.setText("绑定");
			}

			// loginBtn.setId(1);
			loginBtn.setOnClickListener(mLoginBtnListener);
			return convertView;

		}

		@Override
		public int getCount() {
			return AccountConnect.ACCOUNT_NUM;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		try {
			if (processDialog != null) {
				processDialog.dismiss();
			}
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		processDialog = null;
		Utils.setColorTheme(findViewById(R.id.night_mask));
		MobclickAgent.onResume(this);
	}

	int getLogoResIdByType(int type) {
		switch (type) {
		case AccountConnect.SNS_TYPE_SINA_WEIBO:
			return R.drawable.sina_logo;
		default:
			return R.drawable.qq_logo;
		}
	}

	int getSnsTypeByPosition(int pos) {
		switch (pos) {
		case 0:
			return AccountConnect.SNS_TYPE_SINA_WEIBO;
		case 1:
			return AccountConnect.SNS_TYPE_QQ;
		}
		return pos;
	}

	private void setAwardButton() {
		if (mAwardBtn == null) {
			return;
		}
		int awardMoney = 0;

		boolean qqLogin = mAccouts.hasQQAlreadyLogin();
		boolean sinaLogin = mAccouts.hasSinaWeiboLogin();

		final boolean qqAward = qqLogin && !Utils.hasAwardBefore(Utils.AWARD_TYPE_QQ);
		final boolean sinaAward = sinaLogin && !Utils.hasAwardBefore(Utils.AWARD_TYPE_SINA);
		final boolean qqFollowAward = (Cfg.mHasFollowedQQ && !Utils.hasAwardBefore(Utils.AWARD_FOLLOW_QQ));
		final boolean sinaFollowAward = (Cfg.mHasFollowedSina && !Utils.hasAwardBefore(Utils.AWARD_FOLLOW_SINA));

		if (qqAward) {
			awardMoney += bindQQAwardPoint;
		}
		if (sinaAward) {
			awardMoney += bindSinaAwardPoint;
		}
		if (qqFollowAward) {
			awardMoney += followQQAwardPoint;
		}
		if (sinaFollowAward) {
			awardMoney += followSinaAwardPoint;
		}

		if (awardMoney > 0) {
			mAwardBtn.setText("领取奖励 ( " + awardMoney + "扇贝 )");
			mAwardBtn.setVisibility(View.VISIBLE);
			mAwardBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					StringBuilder awardMsgSb = new StringBuilder("money money home...");
					if (sinaAward || qqAward || qqFollowAward || sinaFollowAward) {
						mHandle.obtainMessage(WAITING, awardMsgSb.toString()).sendToTarget();

					}

					if (qqAward) {
						ShanTingAccount.instance().pointsOperation(ShanTingAccount.OPER_AWARD_SHARE_QQ, mAwardListener);

					}
					if (sinaAward) {
						ShanTingAccount.instance().pointsOperation(ShanTingAccount.OPER_AWARD_SHARE_WEIBO, mAwardListener);
					}

					if (qqFollowAward) {
						ShanTingAccount.instance().pointsOperation(ShanTingAccount.OPER_AWARD_FOLLOW_QQ, mAwardListener);
					}

					if (sinaFollowAward) {
						ShanTingAccount.instance().pointsOperation(ShanTingAccount.OPER_AWARD_FOLLOW_SINA, mAwardListener);
					}

				}
			});

		} else {
			mAwardBtn.setVisibility(View.GONE);
		}
		// 提示刷新
		mAwardBtn.invalidate();

	}

	@Override
	public boolean onPointsOperationResult(int resultCode, int operType, int wastePoints, int totalPoints) {
		switch (operType) {
		case ShanTingAccount.OPER_QUERY_AWARD_BIND_QQ:
			if (resultCode == AccountListener.RESULT_OK) {
				bindQQAwardPoint = wastePoints;
			}

			break;
		case ShanTingAccount.OPER_QUERY_AWARD_BIND_SINA:
			if (resultCode == AccountListener.RESULT_OK) {
				bindSinaAwardPoint = wastePoints;
			}

			break;
		case ShanTingAccount.OPER_QUERY_AWARD_FOLLOW_QQ:
			if (resultCode == AccountListener.RESULT_OK) {
				followQQAwardPoint = wastePoints;
			}
			break;
		case ShanTingAccount.OPER_QUERY_AWARD_FOLLOW_SINA:
			if (resultCode == AccountListener.RESULT_OK) {
				followSinaAwardPoint = wastePoints;
			}
			break;
		}
		return false;
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
