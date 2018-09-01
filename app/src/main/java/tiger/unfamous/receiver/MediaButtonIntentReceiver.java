package tiger.unfamous.receiver;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import tiger.unfamous.Cfg;
import tiger.unfamous.services.MainService;
import tiger.unfamous.utils.MyLog;

//线控
public class MediaButtonIntentReceiver extends BroadcastReceiver {
	MyLog log = new MyLog();
	public boolean mDown = false;
	public boolean misWork = false;
	private static final String TAG = "MediaButtonReceiver";
	private Method isOrderedBroadCast = null;
	private static final Class<?>[] args = null;
    public static final String SERVICE_PACKAGENAME = "tiger.unfamous.services.MainService";
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			isOrderedBroadCast = getClass().getMethod("isOrderedBroadcast", args);
			
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			Log.i(TAG, "no such method");
		}
		String intentAction = intent.getAction();
		Log.i(TAG, "receive mediaButton");
		misWork = isWorked(context);
//		Log.i(TAG, "是否正在运行"+misWork);
		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
			// 耳机拔出
//			Log.i(TAG, "耳机拔出");
		} else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)&&misWork) {
			// 获取耳机key
			KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (event == null) {
				return;
			}
           
			int keycode = event.getKeyCode();
			int action = event.getAction();
            Log.i(TAG, "keycode :"+keycode+",,action:"+action);
			String command = null;
			switch (keycode) {
			case KeyEvent.KEYCODE_MEDIA_STOP:
				command = MainService.CMDSTOP;
				break;

			case KeyEvent.KEYCODE_HEADSETHOOK:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				command = MainService.CMDTOGGLEPAUSE;
				break;

			case KeyEvent.KEYCODE_MEDIA_NEXT:
				command = MainService.CMDNEXT;
				break;

			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				command = MainService.PREVIOUS;
				break;
			}
			if(Cfg.SDK_VERSION < 8){
				TelephonyManager mTelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
				int state = mTelephonyMgr.getCallState();
				Log.i(TAG, "state:"+state);
				boolean off = (state != 0);
				Log.i(TAG, "calling state");
				if(off){
					Log.i("TAG", "calling someone");
					return;
				}
			}
			Log.i(TAG, "command:"+command);
			if (command != null && !Cfg.isFocusLosed()) {
				if (action == KeyEvent.ACTION_DOWN) {
					if (!mDown) {
//						Log.i(TAG, "进入启动service");
						Intent i = new Intent(context, MainService.class);
						i.setAction(Cfg.ACTION_HEADSET_KEYEVENT);
						i.putExtra(MainService.COMMAND, command);
						context.startService(i);
					} 
					mDown = true;
				}
				
			}
			if (isOrderedBroad() && !Cfg.isFocusLosed()) {
				abortBroadcast();
			}
		}
	}
	
	private boolean isOrderedBroad(){
		if(isOrderedBroadCast!=null){
			return invokeMethod(isOrderedBroadCast, null);
		}
		return false;
	}
	/**
	 * 兼容1.6老版本系统
	 * @param method
	 * @param args
	 */
	private boolean  invokeMethod(Method method, Object[] args) {
		try {
			return (Boolean) method.invoke(this, args);
		} catch (InvocationTargetException e) {
			// Should not happen.
			Log.w("ApiDemos", "Unable to invoke method", e);
		} catch (IllegalAccessException e) {
			// Should not happen.
			Log.w("ApiDemos", "Unable to invoke method", e);
		}
		return false;
	}
	
	/**
	 * 判断AnYueService是否在运行
	 */
	public  boolean isWorked(Context context)
	{
	  ActivityManager myManager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
	  ArrayList<RunningServiceInfo> runningService = (ArrayList<RunningServiceInfo>) myManager.getRunningServices(30);
	  for(int i = 0 ; i<runningService.size(); i++){
	      if(runningService.get(i).service.getClassName().toString().equals(SERVICE_PACKAGENAME)){
	            return true;
	       }
	  }
	  return false;
	}
}
