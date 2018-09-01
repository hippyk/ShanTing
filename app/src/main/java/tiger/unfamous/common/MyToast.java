package tiger.unfamous.common;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

public class MyToast {

	private static final String TAG = "MyToast";

	public static final int LENGTH_MAX = -1; // show until hide() function
												// invoked

	boolean mCanceled = true;
	Handler mHandler;
	Context mContext;
	Toast mToast;

	public MyToast(Context context) {
		this(context, new Handler());
	}

	public MyToast(Context context, Handler h) {
		mContext = context;
		mHandler = h;
		mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
		mToast.setGravity(Gravity.BOTTOM, 0, 0);
	}

	public void show(int resId, int duration) {
		mToast.setText(resId);
		mToast.getView().setVisibility(View.VISIBLE);
		if (duration != LENGTH_MAX) {
			mToast.setDuration(duration);
			mToast.show();
		} else if (mCanceled) {
			Log.d(TAG, "show length max");
			mToast.setDuration(Toast.LENGTH_LONG);
			mCanceled = false;
			showUntilCancel();
		}
	}

	public void show(String text, int duration) {
		mToast.setText(text);
		mToast.getView().setVisibility(View.VISIBLE);
		if (duration != LENGTH_MAX) {
			mToast.setDuration(duration);
			mToast.show();
		} else {
			if (mCanceled) {
				Log.d(TAG, "show length max");
				mToast.setDuration(Toast.LENGTH_LONG);
				mCanceled = false;
				showUntilCancel();
			}
		}
	}

	public void cancel() {
		Log.d(TAG, "cancel");
		mToast.getView().setVisibility(View.INVISIBLE);
		// mToast.cancel();
		mCanceled = true;
	}

	public boolean isShowing() {
		return !mCanceled;
	}

	private void showUntilCancel() {
		if (mCanceled) {
			return;
		}
		mToast.show();
		mHandler.postDelayed(new Runnable() {
			public void run() {
				showUntilCancel();
			}
		}, 3000);
	}

	public static void showShort(Context ctx, int resId) {
		Toast.makeText(ctx, resId, Toast.LENGTH_SHORT).show();
	}

	public static void showShort(Context ctx, String str) {
		Toast.makeText(ctx, str, Toast.LENGTH_SHORT).show();
	}

	public static void showLong(Context ctx, int resId) {
		Toast.makeText(ctx, resId, Toast.LENGTH_LONG).show();
	}

	public static void showLong(Context ctx, String str) {
		Toast.makeText(ctx, str, Toast.LENGTH_LONG).show();
	}
}
