package tiger.unfamous.common;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

public class InternetStateMgr  {
	public static final int INVALID_STATE = -1;         //无效的连接状态
	public static final int INTERNET_NOTCONNECTED = 0;	//没有任何网络连接	
	public static final int WIFI_CONNECTED = 1;			//Wi-Fi连接			
	public static final int MOBILE_CONNECTED = 2;		//移动网络连接
	public static final int OTHER_CONNECTED = 3;		//除Wi-Fi和移动网络连接以外的其他方式的网络连接
	private Context mCtx;
	private ConnectivityManager mConnectMgr;
	
	public InternetStateMgr(Context ctx) {
		mCtx = ctx;
		mConnectMgr = (ConnectivityManager)mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	
	public boolean hasWifi() {
		NetworkInfo wifiInfo = mConnectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		NetworkInfo activeNetworkInfo = mConnectMgr.getActiveNetworkInfo();
		if (activeNetworkInfo != null && wifiInfo != null) {
			if (wifiInfo.isConnected()
					&& wifiInfo.isAvailable()
					&& (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public boolean hasMobile() {
		NetworkInfo mobileInfo = mConnectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		NetworkInfo activeNetworkInfo = mConnectMgr.getActiveNetworkInfo();
		if (activeNetworkInfo != null && mobileInfo != null) {
			if (mobileInfo.isConnected()
					&& mobileInfo.isAvailable()
					&& (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	public boolean hasInternet() {
		NetworkInfo networkInfo = mConnectMgr.getActiveNetworkInfo();
		if (networkInfo != null) {
			return networkInfo.isConnected() && networkInfo.isAvailable();
		} else {
			return false;
		}
	}
	
	public int getState() {
		int internetState = INTERNET_NOTCONNECTED;
		
		if (hasWifi()) {
			internetState = WIFI_CONNECTED;
		} else if (hasMobile()) {
			internetState = MOBILE_CONNECTED;
		} else if (hasInternet()) {
			internetState = OTHER_CONNECTED;
		}

		return internetState;
	}
	
	public boolean isFromWifiToMobile(int preState, int state) {
		boolean rst = false;

		if (preState==WIFI_CONNECTED && state==MOBILE_CONNECTED) {
			rst = true;
		}
		
		return rst;
	}

	public boolean isFromWifiToNotConnected(int preState, int state) {
		boolean rst = false;

		if (preState==WIFI_CONNECTED && state==INTERNET_NOTCONNECTED) {
			rst = true;
		}
		
		return rst;
	}	

	public boolean isFromWifi(int preState, int state) {
		boolean rst = false;

		if (preState==WIFI_CONNECTED && state!=WIFI_CONNECTED) {
			rst = true;
		}
		
		return rst;
	}

	public boolean isFromMobileToWifi(int preState, int state) {
		boolean rst = false;

		if (preState==MOBILE_CONNECTED && state==WIFI_CONNECTED) {
			rst = true;
		}
		
		return rst;
	}

	public boolean isFromNotConnectedToWifi(int preState, int state) {
		boolean rst = false;

		if (preState==INTERNET_NOTCONNECTED && state==WIFI_CONNECTED) {
			rst = true;
		}
		
		return rst;
	}

	public boolean isToWifi(int preState, int state) {
		boolean rst = false;

		if (preState!=WIFI_CONNECTED && state==WIFI_CONNECTED) {
			rst = true;
		}
		
		return rst;
	}
	
	public boolean isWifiConnected(int state) {
		if (state == WIFI_CONNECTED) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isMobiConnected(int state) {
		if (state == MOBILE_CONNECTED) {
			return true;
		} else {
			return false;
		}
	}
}