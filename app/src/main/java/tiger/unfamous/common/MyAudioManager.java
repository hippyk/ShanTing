package tiger.unfamous.common;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;

import tiger.unfamous.Cfg;
import tiger.unfamous.services.MainService;
import tiger.unfamous.utils.MyLog;

public class MyAudioManager {
	private static final MyLog log = new MyLog();

	private MainService mService;
	private AudioManager mAudioManager;
	private OnAudioFocusChangeListener mAudioFocusListener;
//	private ComponentName mMeidaButtonReceiver;
	private boolean mLoseBefore;

	public MyAudioManager(MainService service) {
		mService = service;
		mAudioFocusListener = new OnAudioFocusChangeListener() {
	        public void onAudioFocusChange(int focusChange) {
//	            mMediaplayerHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
	        	 switch (focusChange) {
	             case AudioManager.AUDIOFOCUS_LOSS:
	             case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
	             case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
	                 log.v("AudioFocus: received AUDIOFOCUS_LOSS");
	                 Cfg.setFocusLosed(true);
	                 if(mService.isPlaying()) {
	                	 mService.pauseMusic();
	                	 mLoseBefore = true;
	                 }
	                 break;
	             case AudioManager.AUDIOFOCUS_GAIN:
	                 log.v("AudioFocus: received AUDIOFOCUS_GAIN");
	                 Cfg.setFocusLosed(false);
	                 if(mLoseBefore && mService.isPlayPaused()) {
	                	 mLoseBefore = false;
	                	 mService.reSartMusic(); // also queues a fade-in
	                 }
	                 break;
	             default:
	                 log.e("Unknown audio focus change code");
	        	}
	       }
		};

//		mMeidaButtonReceiver = new ComponentName(service.getPackageName(),
//                MediaButtonIntentReceiver.class.getName());
	}

	public void requestAudioFocus () {
		if (mAudioManager == null) {
			mAudioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
		}
		mAudioManager.requestAudioFocus((OnAudioFocusChangeListener) mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
	}

	public void abandonAudioFocus() {
		mAudioManager.abandonAudioFocus(mAudioFocusListener);
	}

//	public void registerRemoteControl() {
//		if (mAudioManager == null) {
//			mAudioManager = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
//		}
//		mAudioManager.registerMediaButtonEventReceiver(mMeidaButtonReceiver);
//	}
//
//	public void unregisterRemoteControl() {
//		mAudioManager.unregisterMediaButtonEventReceiver(mMeidaButtonReceiver);
//	}
}
