package tiger.unfamous.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

public class ExitDlg {
	  Context context;
	  public ExitDlg(Context context){
		  this.context=context;
	  }
	public void show3btn(DialogInterface.OnClickListener mHideListener){
			try{
				new AlertDialog.Builder(context).setIcon(
						android.R.drawable.ic_dialog_info).setTitle(
						"提示").setMessage("确定退出善听?")
						.setPositiveButton("退出" , mHideListener)
						.setNeutralButton("隐藏" , mCancelListener)
						.setNegativeButton("取消" , mCalDlgListener)
						.show();

			}catch (Exception e) {
				// TODO: handle exception
			}
	}
				
	DialogInterface.OnClickListener mCancelListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
//			((Activity) context).finish();
			Intent i= new Intent(Intent.ACTION_MAIN);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.addCategory(Intent.CATEGORY_HOME);
			((Activity) context).startActivity(i);
		}
	};
	DialogInterface.OnClickListener mCalDlgListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			
		}
	};
	
	public void show2btn(DialogInterface.OnClickListener mHideListener){
		new AlertDialog.Builder(context).setIcon(
		android.R.drawable.ic_dialog_info).setTitle(
		"提示").setMessage("确定退出善听?")
		.setPositiveButton("退出" , mHideListener)
		.setNegativeButton("取消" , mCalDlgListener)
		.show();
        }
}