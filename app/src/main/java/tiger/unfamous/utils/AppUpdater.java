/**
 * 
 */
package tiger.unfamous.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import tiger.unfamous.Cfg;
import tiger.unfamous.R;
import tiger.unfamous.download.DownloadItem;
import tiger.unfamous.download.DownloadService;
import tiger.unfamous.download.DownloadTask;
import tiger.unfamous.ui.DownloadManager;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;


public class AppUpdater implements Runnable {
	public static String VERSION_URL = Cfg.WEB_HOME + "/others/upinfo.txt";
	
	Activity mActivity;
	
	String mStrVersion;
	String mSize;
    String mStrUrl;
    String mUpgradeLog;
    boolean mForeceUp;
    String mReadableVersion;
    
	DialogInterface.OnClickListener mBtnCancelListener = null;
	DialogInterface.OnCancelListener mKeyCancelListener = null;
    
    public AppUpdater(Activity activity) {
    	mActivity = activity;
    }
    
    

	@Override
	public void run() {
		try {
            InputStream inputStream = MyHttpClient.getInstance().sendHttpGet(VERSION_URL, null);
            if(inputStream == null){
            	return ;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GBK"), Cfg.SIZE_1K);	    
            
        	mStrVersion = reader.readLine();
        	mSize = reader.readLine();
        	mStrUrl = reader.readLine();
        	mUpgradeLog = reader.readLine();
        	String forceUp = reader.readLine();
        	mReadableVersion = reader.readLine();
        	
        	mUpgradeLog = mUpgradeLog.replace("\\n", "\n");
        	
        	if (!TextUtils.isEmpty(forceUp)) {
        		mForeceUp = forceUp.equals("on");
        	}
        	
        	if (!TextUtils.isEmpty(mStrUrl) && mStrUrl.contains("apk")) {	        		
            	compareVersion(mStrVersion, mStrUrl);
        	}
        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

    
    private void compareVersion(String curVer, final String url) {
    	int lcVersion = Utils.getVersionCode(mActivity);
    	final int curVerInt = Integer.valueOf(curVer);
    	boolean needUp = false;
		final File installFile = new File(Cfg.mInnerRootPath + "/" + mActivity.getString(R.string.app_name) + mReadableVersion + ".apk");
		final File installPartFile = new File(Cfg.mInnerRootPath + "/" + mActivity.getString(R.string.app_name) + mReadableVersion + DownloadTask.TAG_SUFFIX);
    	if (curVerInt == lcVersion) {
    		needUp = false;
    		if (installFile.exists()) {
    			installFile.delete();
    		}
    	} else if (curVerInt > lcVersion) {
    		needUp = true;
    	} else if (mForeceUp){   //强制为on的时候服务端版本号小也升级
    		needUp = true;
    	}
		// String size = param.getString(DN.VersionInfo.SIZE);
		// String time = param.getString(DN.VersionInfo.TIME);
		if (null != url && url.length() > 0 && null != curVer
				&& curVer.length() > 0 && needUp) {

			if(mActivity == null || mActivity.isFinishing()) {
				return;
			}
			
			String str = mActivity.getString(R.string.versioninfo_newversion_msg);

			
			if (mForeceUp) {
				str = "当前版本善听已停用，必须升级到最新版本！如果下载失败，请访问shanting.mobi，立即升级？";
				
				mBtnCancelListener = new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						mActivity.finish();

					}
				};
				
				mKeyCancelListener = new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						mActivity.finish();
					}
				};
			}
			
			final String msg = str;
			
            mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					CommonDlg.showConfirmDlg(mActivity, -1, msg + "\n\n更新内容：" + "\n" + mUpgradeLog + "\n\n(包大小： " + mSize + ")",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								//Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
								//i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								//mActivity.startActivity(i);

								
								DownloadItem item = DataBaseHelper.getInstance()
										.getDownloadItemByUnitUrl(url);

								if (item != null) {
									if (item.getStatus() == DownloadItem.RUNNING) {
										return;
									}
//									log.e("installFile path is " + installFile.getAbsolutePath());
//									log.e("installPartFile path is " + installPartFile.getAbsolutePath());
									if (installFile.exists()) {
										if (Utils.getVersionCode(mActivity, installFile.getAbsolutePath()) == curVerInt) {
											Uri uri = Uri.fromFile(installFile);   
											Intent installIntent = new Intent(Intent.ACTION_VIEW);
											installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
											installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
											mActivity.startActivity(installIntent);
										} else {
											installFile.delete();
											item.setDownload_pos(0);
											item.setFileSize(0);
											item.setDownloadSize(0);
											
											Intent it = new Intent(mActivity, DownloadService.class);
											it.putExtra(DownloadManager.LAUNCH_UDTYPE,
													DownloadManager.LAUNCH_DOWNLOADING);
											it.putExtra(DownloadService.DOWNLOAD_DIR_NAME, "");
											it.putExtra(DownloadService.DOWNLOAD_NAME, mActivity.getString(R.string.app_name) + mReadableVersion);
											it.putExtra(DownloadService.DOWNLOAD_FILE_TYPE, DownloadItem.INSTALL_PACKAGE_FILE);
											it.putExtra(DownloadService.DOWNLOAD_URL, url);
											mActivity.startService(it);
										}
									} else {
										if (installPartFile.exists()) {
											installPartFile.delete();
										}
										
										item.setDownload_pos(0);
										item.setFileSize(0);
										item.setDownloadSize(0);
										
										Intent it = new Intent(mActivity, DownloadService.class);
										it.putExtra(DownloadManager.LAUNCH_UDTYPE,
												DownloadManager.LAUNCH_DOWNLOADING);
										it.putExtra(DownloadService.DOWNLOAD_DIR_NAME, "");
										it.putExtra(DownloadService.DOWNLOAD_NAME, mActivity.getString(R.string.app_name) + mReadableVersion);
										it.putExtra(DownloadService.DOWNLOAD_FILE_TYPE, DownloadItem.INSTALL_PACKAGE_FILE);
										it.putExtra(DownloadService.DOWNLOAD_URL, url);
										mActivity.startService(it);
									}
								} else {
//									log.e("installFile path is " + installFile.getAbsolutePath());
//									log.e("installPartFile path is " + installPartFile.getAbsolutePath());

									if (installFile.exists()) {
										installFile.delete();
									}
									
									if (installPartFile.exists()) {
										installPartFile.delete();
									}
									
									Intent it = new Intent(mActivity, DownloadService.class);
									it.putExtra(DownloadManager.LAUNCH_UDTYPE,
											DownloadManager.LAUNCH_DOWNLOADING);
									it.putExtra(DownloadService.DOWNLOAD_DIR_NAME, "");
									it.putExtra(DownloadService.DOWNLOAD_NAME, mActivity.getString(R.string.app_name) + mReadableVersion);
									it.putExtra(DownloadService.DOWNLOAD_FILE_TYPE, DownloadItem.INSTALL_PACKAGE_FILE);
									it.putExtra(DownloadService.DOWNLOAD_URL, url);
									mActivity.startService(it);
								}
								
								if (mForeceUp) {
									mActivity.finish();
								}
							}	
					}, mBtnCancelListener, mKeyCancelListener, "下次再说");
				}
            });
		}
    }
}
