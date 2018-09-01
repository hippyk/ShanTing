/**
 * 
 */
package tiger.unfamous.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import tiger.unfamous.Cfg;
import tiger.unfamous.R;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.services.MainService;

public abstract class BasicActivity extends Activity {
	protected TextView mTopTitle;
	protected Button mShowPlaying;

	protected MainService mService;
	boolean mRestored;// if the activity is restored from prev
	ProgressDialog mProgressDialog = null;
	protected MyToast mMyToast;
	protected ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			mService = ((MainService.LocalBinder) arg1).getService();
			BasicActivity.this.onServiceConnected();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mService = null;
		}

	};;

	protected void onServiceConnected() {

	}

	protected void onContentProgressCanceled() {

	}



	// 保证在oncreate最开始做的工作
	protected void onPreCreate() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 禁止横屏
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// request features
		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		// Cfg中的一些变量需要初始化后才可以使用
		Cfg.init(this);
		// TODO Auto-generated method stub
		Intent intent = new Intent(BasicActivity.this, MainService.class);
//		startService(intent);
		getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		// Log.e(TAG, "onCreate");
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		getApplicationContext().unbindService(mConnection);
		// GuoheAdManager.finish(this);
	}

	@Override
	protected void onStop() {
		if (mMyToast != null)
			mMyToast.cancel();
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

//	protected void showTitleProgress(boolean bShow) {
//		setProgressBarIndeterminateVisibility(bShow);
//	}

	protected void showContentProgress(boolean bShow) {
		try {
			showContentProgress(bShow, getResources().getString(R.string.dialog_msg_wait));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	protected void showContentProgress(boolean bShow, String msg) {
		try {
			if (bShow && mProgressDialog == null) {
				// Log.e(TAG, "progress show!");
				mProgressDialog = ProgressDialog.show(this, "", msg, true, true, new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						try {
							arg0.dismiss();
							mProgressDialog = null;
							onContentProgressCanceled();
						} catch (Exception e) {
							// TODO: handle exception
						}

					}
				});
			} else {
				if (null != mProgressDialog) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	DialogInterface.OnClickListener mHideListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			if (!mService.isPlayStopped()) {
				mService.saveHistory();
				mService.stopMusic();
			}
			Intent i = new Intent(BasicActivity.this, MainService.class);
			stopService(i);
			finish();
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {

		try {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				int curVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);

				if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
					curVolume++;
				} else {
					curVolume--;
				}
				am.setStreamVolume(AudioManager.STREAM_MUSIC, curVolume, AudioManager.FLAG_SHOW_UI);

				return true;
			}

			return super.onKeyDown(keyCode, event);
		} catch (Exception e) {
			// TODO: handle exception
			return false;
		}

	};



	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		// TODO Auto-generated method stub
		return CustomOptionMenu.showCustomMenu(featureId, menu, BasicActivity.this, ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0));
	}
}
