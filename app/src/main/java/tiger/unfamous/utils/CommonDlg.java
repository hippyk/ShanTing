package tiger.unfamous.utils;

import tiger.unfamous.R;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.TextView;

public class CommonDlg {
	public static void showErrorDlg(Context ctx, String msg,
			DialogInterface.OnClickListener btnListener) {
		try {
			new AlertDialog.Builder(ctx)
					// .setIcon(android.R.drawable.alert_dialog_icon)
					.setIcon(android.R.drawable.ic_dialog_alert).setTitle(
							R.string.dialog_title_error).setMessage(msg)
					.setPositiveButton(
							ctx.getString(R.string.label_OK),
							btnListener).create().show();
		} catch (BadTokenException bte) {
			Log.e("CommonDlg", "BadTokenException", bte);
		} catch (Exception ex) {
			Log.e("CommonDlg", "Exception", ex);
		}
	}

	public static void showInfoDlg(Context ctx, String title, String msg) {
		showInfoDlg(ctx, title, msg, ctx.getString(R.string.label_OK), null);
	}

	public static void showInfoDlg(Context ctx, String title, String msg,
			String btnText, DialogInterface.OnClickListener btnListener) {
		try {
			LayoutInflater factory = LayoutInflater.from(ctx);
			final View textEntryView = factory.inflate(
					R.layout.about_dialog_view, null);
			TextView messageText = (TextView) textEntryView
					.findViewById(R.id.about_view);
			messageText.setAutoLinkMask(Linkify.WEB_URLS);
			messageText.setText(msg);
			// ScrollView sView = new ScrollView(ctx);
			// sView.addView(messageText);
			new AlertDialog.Builder(ctx).setIcon(
					android.R.drawable.ic_dialog_info).setTitle(title).setView(
					textEntryView).setPositiveButton(btnText, btnListener)
					.create().show();
		} catch (BadTokenException bte) {
			Log.e("CommonDlg", "BadTokenException", bte);
		} catch (Exception ex) {
			Log.e("CommonDlg", "Exception", ex);
		}
	}
	
	public static void showConfirmDlg(Context ctx, int title, String msg,
			DialogInterface.OnClickListener okListener) {
		showConfirmDlg(ctx, title, msg, okListener, null, (DialogInterface.OnCancelListener)null);
	}

	public static void showConfirmDlg(Context ctx, int title, String msg,
			String okStr,
			DialogInterface.OnClickListener okListener) {
		showConfirmDlg(ctx, title, msg, okStr, null, okListener, null, null, (DialogInterface.OnCancelListener)null, ctx.getString(R.string.label_CANCEL));	
	}

	public static void showConfirmDlg(Context ctx, int title, String msg,
			String okStr,
			String neutralStr,
			DialogInterface.OnClickListener okListener,
			DialogInterface.OnClickListener neutralListener,
			DialogInterface.OnClickListener cancelBtnListener,
			DialogInterface.OnCancelListener cancelListener,
			String cancelStr) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(title == -1 ? R.string.dialog_title_confirm : title)
					.setMessage(msg)
					.setPositiveButton(
							okStr,
							okListener)
					.setNegativeButton(
							cancelStr,
							cancelBtnListener)
					.setOnCancelListener(cancelListener);
			
			if(neutralStr != null && neutralStr.length() > 0){
				builder.setNeutralButton(neutralStr, neutralListener);
			}
			builder.create().show();
			
		} catch (BadTokenException bte) {
			Log.e("CommonDlg", "BadTokenException", bte);
		} catch (Exception ex) {
			Log.e("CommonDlg", "Exception", ex);
		}
	}
	

	public static void showConfirmDlg(Context ctx, int title, String msg,
			DialogInterface.OnClickListener okListener,
			DialogInterface.OnClickListener cancelBtnListener,
			DialogInterface.OnCancelListener cancelListener) {
		showConfirmDlg(ctx, title, msg, okListener, cancelBtnListener, 
				cancelListener, ctx.getString(R.string.label_CANCEL));
	}

	public static void showConfirmDlg(Context ctx, int title, String msg,
			DialogInterface.OnClickListener okListener,
			DialogInterface.OnClickListener cancelBtnListener,
			DialogInterface.OnCancelListener cancelListener, String cancelStr) {
		try {
			new AlertDialog.Builder(ctx)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(title == -1 ? R.string.dialog_title_confirm : title)
					.setMessage(msg)
					.setPositiveButton(
							ctx.getString(R.string.label_OK),
							okListener)
					.setNegativeButton(
							cancelStr,
							cancelBtnListener)
					.setOnCancelListener(cancelListener).create().show();
		} catch (BadTokenException bte) {
			Log.e("CommonDlg", "BadTokenException", bte);
		} catch (Exception ex) {
			Log.e("CommonDlg", "Exception", ex);
		}
	}

//	public static void showConfirmDlg(Context ctx, String msg,
//			DialogInterface.OnClickListener okListener) {
//		try {
//			new AlertDialog.Builder(ctx).setIcon(
//					android.R.drawable.ic_dialog_alert).setTitle(
//					R.string.dialog_title_confirm).setMessage(msg)
//					.setPositiveButton(
//							ctx.getString(R.string.label_OK),
//							okListener).create().show();
//		} catch (BadTokenException bte) {
//			Log.e("CommonDlg", "BadTokenException", bte);
//		} catch (Exception ex) {
//			Log.e("CommonDlg", "Exception", ex);
//		}
//	}
	
	public static ProgressDialog showProgressDlg(Context ctx, String msg,
			final DialogInterface.OnCancelListener cancelListener) {
		// return ProgressDialog.show(ctx, null, msg, true, true,
		// cancelListener);
		final ProgressDialog mProgress = new ProgressDialog(ctx);
		mProgress.setMessage(msg);
		mProgress.setIndeterminate(true);
		if (null != cancelListener) {
			mProgress.setButton(ctx.getString(
					R.string.label_CANCEL),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							mProgress.cancel();
							mProgress.dismiss();
							if (null != cancelListener) {
								cancelListener.onCancel(null);
							}
						}
					});
		}

		mProgress.show();
		return mProgress;
	}

	public static ProgressDialog showProgressDlg(Context ctx, String msg,
			final DialogInterface.OnCancelListener cancelListener, DialogInterface.OnKeyListener onKeyListener) {
		// return ProgressDialog.show(ctx, null, msg, true, true,
		// cancelListener);
		final ProgressDialog mProgress = new ProgressDialog(ctx);
		mProgress.setMessage(msg);
		mProgress.setIndeterminate(true);
		if (null != cancelListener) {
//			mProgress.setOnCancelListener(cancelListener);
			mProgress.setButton(ctx.getString(
					R.string.label_CANCEL),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							mProgress.cancel();
//							mProgress.dismiss();
							if (null != cancelListener) {
								cancelListener.onCancel(null);
							}
						}
					});
		}
		
		if (onKeyListener != null) {
			mProgress.setOnKeyListener(onKeyListener);
		}

		mProgress.show();
		return mProgress;
	}
}
