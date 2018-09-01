package tiger.unfamous.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.utils.ShanTingAccount;
import tiger.unfamous.utils.Utils;


public class MoreActivity extends BasicActivity {
    private ListView mlvSetting;
    private Button mShare;
	
	/**
	 * 更多里面菜单项，如果需要注释某项菜单，
	 * 1.在map中注释菜单项的添加动作
	 * 2.把该MENU_后面的 + 1 改为 + 0
	 * 3.onItemClick里的case语句注释
	 */
	public static final int MENU_START = 0;
	public static final int MENU_SETTING = MENU_START;
//	public static final int MENU_BUY_SERVICE = MENU_SETTING + 0;
	public static final int MENU_ACCOUNT = MENU_SETTING + 1;
	public static final int MENU_SNS_ACCOUNT = MENU_ACCOUNT + 1;
	public static final int MENU_RATE_STAR = MENU_SNS_ACCOUNT + 1;
	public static final int MENU_FEEDBACK = MENU_RATE_STAR + 1;
//	public static final int MENU_BBS = MENU_FEEDBACK + 1;
	public static final int MENU_FAQ = MENU_FEEDBACK + 1;
	public static final int MENU_ABOUT_US = MENU_FAQ + 1;
//	public static final int MENU_GOOD_APPS = MENU_ABOUT_US + 1;
//	public static final int MENU_ALIPAY_TEST = MENU_ABOUT_US + 0;
	
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.more);

		
		Utils.addAdView(this);
		
		mTopTitle = (TextView) findViewById(R.id.title);
		mTopTitle.setText("更多");
		
		mShowPlaying =(Button)findViewById(R.id.show_playing);
		mShare =(Button)findViewById(R.id.rightbtn);
		
		OnClickListener clickListener = new BtnOnClickListener();
		mShowPlaying.setOnClickListener(clickListener);
		mShare.setOnClickListener(clickListener);
		mShare.setText("分享");
		
		mlvSetting = (ListView)findViewById(R.id.listview);

		SimpleAdapter adapter = new SimpleAdapter(this ,mapListSetting() , R.layout.moreitem,
				new String[]{"img","title"}, new int[]{R.id.img,R.id.title});
		mlvSetting.setAdapter(adapter);
		
		mlvSetting.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				Intent i = null;
				switch (arg2) {
				case MENU_SETTING:
					i = new Intent(MoreActivity.this,Preferences.class);
					startActivity(i);
					break;

                case MENU_FEEDBACK:
                	com.umeng.fb.UMFeedbackService.openUmengFeedbackSDK(MoreActivity.this);
	              break;
				case MENU_SNS_ACCOUNT:// 社区账号绑定
					i = new Intent(MoreActivity.this,
							SNSAccountActivity.class);
					startActivity(i);
					break;

                case MENU_RATE_STAR:
                	rateStar();
					break;
                case MENU_ABOUT_US:
	                Intent iabout = new Intent(MoreActivity.this,WebBrowser.class);
	                String str = getString(R.string.app_name) + "(" + Utils.getAppVersionName(MoreActivity.this) + ")";
	                iabout.putExtra(DN.TITLE, str);
	                if(Cfg.IS_HIAPK){
	                	iabout.putExtra(DN.URL, Cfg.WEB_HOME + "/about/about-hiapk.html"); 
	        		}else{
	        			iabout.putExtra(DN.URL, Cfg.WEB_HOME + "/about/about.html");
	        		}
	                startActivity(iabout);                	
                  break;
                  
                case MENU_FAQ:
                	Intent iFaq = new Intent(MoreActivity.this,WebBrowser.class);
                	iFaq.putExtra(DN.TITLE, getResources().getString(R.string.faq));
                	iFaq.putExtra(DN.URL, Cfg.FAQ_URL);
	                startActivity(iFaq);  
	                
                	break;
                case MENU_ACCOUNT:
                	String url = ShanTingAccount.instance().getAccountCenterUrl();
                	Intent acount = new Intent(MoreActivity.this,WebBrowser.class);
                	acount.putExtra(DN.TITLE, getResources().getString(R.string.userCenter));
	        		acount.putExtra(DN.URL, url);
	        		
	                startActivity(acount);    
                	break;
				}
			}
		});
		
		if(Utils.needNoticeRateStars()){
			Cfg.mRateTipsShown = true;
			Cfg.saveBool(getApplicationContext(), Cfg.PREF_RATE_TIPS_SHOWN, Cfg.mRateTipsShown);
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("您已启动善听超过").append(Cfg.RATE_STAR_CONDITION_START_TIMES)
							.append("次. 去市场发表一下使用感受吧，这对我们很有用哦！^_^ 成功之后，我们还会赠送您")
							.append(Cfg.AWARD_RATE_STAR_POINTS)
							.append("扇贝表示感谢（下次启动程序自动发放）");
			
			tiger.unfamous.utils.CommonDlg.showConfirmDlg(this, -1, stringBuilder.toString(), "支持一下", null,
					new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							Utils.rateStarOK(MoreActivity.this);
							rateStar();
						}
					}, null, null, null, "再说吧");
		}

	}
	
	private void rateStar(){
		try {						
        	Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));              	
            startActivity(i);
//            Utils.rateStarOK(MoreActivity.this);
            
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			MyToast.showShort(MoreActivity.this, "很抱歉，没有找到应用商店！");
		}
	}
	public List<Map<String,Object>> mapListSetting() {

		List<Map<String,Object>> mapList = new ArrayList<Map<String,Object>>();

		HashMap<String,Object> map = new HashMap<String, Object>();
		map.put("img", R.drawable.right_arrow);
		map.put("title", "设置");
		mapList.add(map);

		
		map = new HashMap<String, Object>();
		map.put("img", R.drawable.right_arrow);
		map.put("title", "个人中心");
		mapList.add(map);
		
		map = new HashMap<String, Object>();
		map.put("img", R.drawable.right_arrow);
		map.put("title", "社区账号");
		mapList.add(map);
		
		map = new HashMap<String, Object>();
		map.put("img", R.drawable.right_arrow);
		map.put("title", "欢迎拍砖");
		mapList.add(map);
		
		map = new HashMap<String, Object>();
		map.put("img", R.drawable.right_arrow);
		map.put("title", "反馈意见");
		mapList.add(map);

		
		map = new HashMap<String, Object>();
		map.put("img", R.drawable.right_arrow);
		map.put("title", "常见问题");
		mapList.add(map);	
		
		map = new HashMap<String, Object>();
		map.put("img", R.drawable.right_arrow);
		map.put("title", "关于我们");
		mapList.add(map);

		
		return mapList;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP){
			return super.onKeyDown(keyCode, event);
		}
		return false;
	}
	
	class BtnOnClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.rightbtn:
				MobclickAgent.onEvent(MoreActivity.this, Cfg.UM_SHARE,
						Cfg.UM_SHARE1);
				Utils.share(MoreActivity.this, null);
//				KuguoAdsManager adsMgr = KuguoAdsManager.getInstance();
//				adsMgr.showAppList(MoreActivity.this);
				break;

			case R.id.show_playing:
				if (mService != null) {
					mService.showPlaying(MoreActivity.this);

				}
			}
		}
	}
}
