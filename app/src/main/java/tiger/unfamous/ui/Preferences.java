package tiger.unfamous.ui;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tiger.unfamous.Cfg;
import tiger.unfamous.R;
import tiger.unfamous.common.TextViewPreference;
import tiger.unfamous.utils.Utils;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity implements OnPreferenceChangeListener,OnPreferenceClickListener{
	
	private CheckBoxPreference mWifiAutoDownloadPreference;
	public static String WIFIAUTODOWNLOAD_ACTION = "WIFIAUTODOWNLOAD_ACTION";
	private TextViewPreference tv;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		if(Cfg.mIsNightMode){
//			setTheme(R.style.preference_category_night);
//		}
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
		CheckBoxPreference noPicModePreference = (CheckBoxPreference) findPreference(Cfg.PREF_WEB_NOPIC_MODE);
		mWifiAutoDownloadPreference = (CheckBoxPreference) findPreference(Cfg.PREF_WIFI_AUTO_DOWNLOAD);
		tv = (TextViewPreference) findPreference("textview_preference");
		tv.setOnPreferenceChangeListener(this);
		tv.setOnPreferenceClickListener(this);
		
		
		noPicModePreference.setOnPreferenceChangeListener(this);
		mWifiAutoDownloadPreference.setOnPreferenceChangeListener(this);
//		if(Cfg.mIsNightMode){
//			PreferenceScreen preferenceScreen = getPreferenceScreen();
//			setLayoutResource(preferenceScreen);
//		}
		
	}
	

	@Override
	public void onDestroy() {
		super.onDestroy();
		Cfg.loadSetting(this);
	}

	//侦听无图模式用户设置的改变，在onDestroy中读取设置，不够及时,有时BookStore不会刷新当前页面
	@Override
	public boolean onPreferenceChange(Preference preference, final Object newValue) {	
//		if (preference == mWifiAutoDownloadPreference) {
//			if (Cfg.mWifiAutoDownload != (Boolean)newValue) {
//				Cfg.mWifiAutoDownload = (Boolean)newValue;
//				Intent i = new Intent(WIFIAUTODOWNLOAD_ACTION);
//				sendBroadcast(i);
//			}
//		}
		try{
			
			if(Cfg.mWebUsingNoPicMode != (Boolean) newValue 
					//收藏夹数据库已更新
					&& Cfg.mFavoriteDatabaseVersion == Cfg.FAVORITE_DATABASE_VERSION){
				new Thread("updateFavorite"){
					@Override
					public void run() {
						SharedPreferences preferences = getSharedPreferences("favorite", Context.MODE_PRIVATE);
						@SuppressWarnings("unchecked")
						Map<String, String> map = (Map<String, String>) preferences.getAll();
			           
						if(map == null || map.isEmpty()){
			            	return ;
			            }
						
			            Set<Map.Entry<String,String>> entrys = map.entrySet();
			        	Iterator<Map.Entry<String, String>> iterator = entrys.iterator();
			        	Map.Entry<String, String> entry;
			        	
			        	String url = null;
			        	while(iterator.hasNext()){
			        		entry = iterator.next();
			        		url = Utils.switchUrlByPicMode(entry.getValue(), (Boolean) newValue);
			        		preferences.edit().putString(entry.getKey(), url).commit();
			        	}
			        		
					};
				}.start();
				
			}
				
			Cfg.mWebUsingNoPicMode = (Boolean) newValue;
		}catch (Exception e) {
		}
		
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		Intent intent = new Intent(getApplicationContext(),SelectDownloadPathActivity.class);
//		getApplicationContext().startActivity(intent);
		startActivityForResult(intent, 1);
		return false;
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		 switch (resultCode) {
		case RESULT_OK:
			tv.setSummary(data.getStringExtra("mCurrentDir"));
			break;
		}

	}
	
	/**
	 * setCustomPreference
	 * 
	 * @param preference
	 */
//	private void setLayoutResource(Preference preference) {
//		if (preference instanceof PreferenceScreen) {
//			PreferenceScreen ps = (PreferenceScreen) preference;
//			int cnt = ps.getPreferenceCount();
//			for (int i = 0; i < cnt; ++i) {
//				Preference p = ps.getPreference(i);
//				setLayoutResource(p);
//			}
//		} else if (preference instanceof PreferenceCategory) {
//			PreferenceCategory pc = (PreferenceCategory) preference;
//			pc.setLayoutResource(R.layout.preference_category);
//			int cnt = pc.getPreferenceCount();
//			for (int i = 0; i < cnt; ++i) {
//				Preference p = pc.getPreference(i);
//				setLayoutResource(p);
//			}
//		}
//	}
	
}
