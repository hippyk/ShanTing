package tiger.unfamous.receiver;

import java.util.ArrayList;

import tiger.unfamous.Cfg;
import tiger.unfamous.common.InternetStateMgr;
import tiger.unfamous.download.DownloadItem;
import tiger.unfamous.download.DownloadList;
import tiger.unfamous.download.DownloadService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class ConnectivityChangeReceiver extends BroadcastReceiver {
	private InternetStateMgr mInternetStateMgr = null;
	private int mInternetState = InternetStateMgr.INVALID_STATE;
	private static final String TAG = "ConnectivityChangeReceiver";
	private static final String DOWNLAODSERVICE_PACKAGENAME = "tiger.unfamous.download.DownloadService";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		mInternetState = Cfg.loadInt(context, Cfg.PREF_INTERNET_STATE, InternetStateMgr.INVALID_STATE);
		mInternetStateMgr = new InternetStateMgr(context);
		Log.i(TAG, "onReceive enter, preInternetState is " + mInternetState + " nowInternetState is " + mInternetStateMgr.getState());
		int internetState = mInternetStateMgr.getState();
		
		if (mInternetState == internetState) {
			Log.i(TAG, "Internet state is same with the last state");
			return;
		}
		
		Cfg.init(context);

		if (Cfg.mWifiAutoDownload
				&& DownloadList.getInstance().getAllNonCompleteItems().size() > 0
				&& mInternetStateMgr.isToWifi(mInternetState, internetState)) {
			Intent i = new Intent(context, DownloadService.class);
			i.setAction(Cfg.ACTION_CONNECTIVITY_WIFICONNECTED);
			context.startService(i);
		} else if (Cfg.mNotWifiProtect
				&& DownloadList.getInstance().getItemsByStatus(DownloadItem.RUNNING).size() > 0
				&& mInternetStateMgr.isFromWifi(mInternetState, internetState)) {
			Intent i = new Intent(context, DownloadService.class);
			i.setAction(Cfg.ACTION_CONNECTIVITY_WIFIDISCONNECTED);
			context.startService(i);
		} else if (Cfg.mNotWifiProtect
				&& DownloadList.getInstance().getItemsByStatus(DownloadItem.RUNNING).size() == 0
				&& isDownlaodServiceWorked(context)
				&& !mInternetStateMgr.isWifiConnected(internetState)) {
			Intent i = new Intent(context, DownloadService.class);
			i.setAction(Cfg.ACTION_CHECK_STOP_SERVICE);
			context.startService(i);
		}
		
		

		Cfg.saveInt(context, Cfg.PREF_INTERNET_STATE, internetState);
		return;
	}

	public  boolean isDownlaodServiceWorked(Context context)
	{
	  ActivityManager myManager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
	  ArrayList<RunningServiceInfo> runningService = (ArrayList<RunningServiceInfo>) myManager.getRunningServices(30);
	  for(int i = 0 ; i<runningService.size(); i++){
	      if(runningService.get(i).service.getClassName().toString().equals(DOWNLAODSERVICE_PACKAGENAME)){
	            return true;
	       }
	  }
	  return false;
	}
}