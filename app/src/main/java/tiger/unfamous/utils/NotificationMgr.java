package tiger.unfamous.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.gson.stream.JsonReader;
import com.umeng.analytics.MobclickAgent;

import java.io.InputStream;
import java.io.InputStreamReader;

import tiger.unfamous.Cfg;
import tiger.unfamous.R;
import tiger.unfamous.services.MainService;

//显示推送消息
public class NotificationMgr implements Runnable {
	
	private Context mContext;
	
	public static final int INVALID_NOTIFICATION_ID = -1;
	
	public static String NOTIFICATION_URL = Cfg.WEB_HOME + "/e/extend/interface/notice_new.php";
	
	public NotificationMgr(Context context) {
		mContext = context;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		InputStream inputStream = MyHttpClient.getInstance().sendHttpGet(NOTIFICATION_URL, null);

		String id = "";
		String text = "";
		String action = "";
		String actionText = "";
		String oldVersion = "";
		String sendGroup ="";
		
		String notificationStatus = "true";
		
		try {
			if (inputStream != null) {
				JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "GBK"));
				reader.beginObject();
			    while (reader.hasNext()) {
			    	String name = reader.nextName();
			    	if (name.equals("id")) {
				    	id = reader.nextString();
			    	} else if (name.equals("text")) {
				    	text = reader.nextString();
			    	} else if (name.equals("action")) {
				    	action = reader.nextString();
			    	} else if (name.equals("actiontext")) {
				    	actionText = reader.nextString();
			    	} else if (name.equals("oldversion")) {
				    	oldVersion = reader.nextString();
			    	} else if (name.equals("status")) {
			    		notificationStatus = reader.nextString();
			    	} else if (name.equals("group")) {
			    		sendGroup = reader.nextString();
			    	} else {
			    		reader.skipValue();
			    	}
			    }
			    reader.endObject();
			    
			    Cfg.mNotificationId = Cfg.loadInt(mContext, Cfg.PREF_NOTIFICATION_ID, INVALID_NOTIFICATION_ID);
			    
			    if (notificationStatus.equals("true")) {
			    	if ((Cfg.mNotificationId != INVALID_NOTIFICATION_ID
			    				&& id != ""
			    				&& Integer.parseInt(id) > Cfg.mNotificationId) 
			    		    || Cfg.mNotificationId == INVALID_NOTIFICATION_ID) {
			    		int verCode = Utils.getVersionCode(mContext);
			    		if ((oldVersion.trim().equals("") || verCode > Integer.parseInt(oldVersion))//没有版本过滤或者是可以显示的版本
			    				&& ((sendGroup == null || sendGroup.length() <= 0) //全员发送
			    						||((sendGroup.equalsIgnoreCase("common") && !Cfg.mIsVIP))//对象是common的，且用户是普通用户
			    							|| ((sendGroup.equalsIgnoreCase("vip") && Cfg.mIsVIP)))//对象是vip且用户就是vip
			    				){
			    			sendNotification(text, action, actionText);
			    			MobclickAgent.onEvent(this.mContext, Cfg.UM_NOTIFICATION_COUNT, id);
			    		}
			    		
			    		Cfg.mNotificationId = Integer.parseInt(id);
			    		Cfg.saveInt(mContext, Cfg.PREF_NOTIFICATION_ID, Cfg.mNotificationId );
			    	}
			    }
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void sendNotification(String text, String action, String actiontext) {
		Notification notify = new Notification();
		Intent i = new Intent(mContext, MainService.class);
		i.setAction(Cfg.ACTION_SHOW_NOTIFICATION);
		i.putExtra("action", action);
		i.putExtra("actiontext", actiontext);
		PendingIntent contentIntent = PendingIntent.getService(mContext, 0, i,
				PendingIntent.FLAG_UPDATE_CURRENT);
		notify.contentIntent = contentIntent;
//		notify.icon = Cfg.IS_LEPHONE ? R.drawable.status_icon_lephone : R.drawable.status_play;
		notify.icon = R.drawable.notice;
		notify.flags = Notification.FLAG_AUTO_CANCEL;

		notify.setLatestEventInfo(mContext, mContext.getString(R.string.notification_name),
				text, contentIntent);
		NotificationManager notificationManager = (NotificationManager)mContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
		notificationManager.notify(Cfg.NOTIFICATION_ID, notify);
	}
	
}