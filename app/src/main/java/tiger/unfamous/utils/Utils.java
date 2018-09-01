package tiger.unfamous.utils;

/**
 * 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.adsmogo.adapters.AdsMogoCustomEventPlatformEnum;
import com.adsmogo.adview.AdsMogoLayout;
import com.adsmogo.controller.listener.AdsMogoListener;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.R;
import tiger.unfamous.alipay.AliPay;
import tiger.unfamous.data.Book;
import tiger.unfamous.data.History1;
import tiger.unfamous.data.Song;
import tiger.unfamous.download.DownloadList;
import tiger.unfamous.ui.ShareActivity;
import tiger.unfamous.ui.WebBrowser;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.net.Uri;
import android.os.StatFs;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
//import android.webkit.CacheManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


public class Utils {
//	private static final String TAG = "Utils";
	private static final MyLog log = new MyLog("Utils");


	/*
	 * Try to use String.format() as little as possible, because it creates a
	 * new Formatter every time you call it, which is very inefficient. Reusing
	 * an existing Formatter more than tripled the speed of makeTimeString().
	 * This Formatter/StringBuilder are also used by makeAlbumSongsLabel()
	 */
//	private static StringBuilder sFormatBuilder = new StringBuilder();
//	private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale
//			.getDefault());
//	private static final Object[] sTimeArgs = new Object[5];

	public static String makeTimeString(Context context, long secs) {
//		String durationformat = context.getString(R.string.durationformat);

		/*
		 * Provide multiple arguments so the format can be changed easily by
		 * modifying the xml.
		 */
//		sFormatBuilder.setLength(0);
//
//		final Object[] timeArgs = sTimeArgs;
//		timeArgs[0] = secs / 3600;
//		timeArgs[1] = secs / 60;
//		timeArgs[2] = (secs / 60) % 60;
//		timeArgs[3] = secs;
//		timeArgs[4] = secs % 60;
//
//		return sFormatter.format(durationformat, timeArgs).toString();
		long minute = secs / 60;
		secs %= 60;
		StringBuilder sBuilder = new StringBuilder(6);
		sBuilder.append(minute).append(":").append(secs >= 10 ? "" : "0").append(secs);
		
		return sBuilder.toString();
		
	}


	/**
	 * 获取以秒为单位的当前的时间。
	 * @return
	 */
	public static long getCurrSecTime(){
		return System.currentTimeMillis()/1000;
	}
	
	public static String encodeURL(String url, String enc) {
		String ret = url;
		try {
			Pattern p = Pattern.compile("[^\\x00-\\xff]");
			Matcher m = p.matcher(url);
			while (m.find()) {
				String str = m.group();
				ret = ret.replace(str, URLEncoder.encode(str, enc));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		log.d("encodeUrl:" + ret);
		return ret;
	}

	public static String encodeURL(String url) {
		return encodeURL(url, "UTF-8");
	}


	public static String urlEncode(String s) {
		String ret = s;
		try {
			ret = URLEncoder.encode(s, "UTF-8");
		} catch (Exception ex) {
//			Log.e(TAG, "Exception ", ex);
			ex.printStackTrace();
			ret = s;
		}

		return ret;
	}

	/**
	 * set url param, will replace old one
	 * 
	 * @param url
	 * @param paramName
	 * @param paramValue
	 * @return
	 */
	public static String setUrlParam(String url, String paramName,
			String paramValue) {
		if (null == url || url.length() < 1 || null == paramName
				|| paramName.length() < 1 || null == paramValue) {
			log.e("Invalid param. url: " + url + ", paramName: "
					+ paramName + ", paramValue: " + paramValue);
			return url;
		}

		StringBuffer ret = new StringBuffer(url.length() + paramName.length()
				+ paramValue.length() * 3 + 20);
		int idx = -1;
		String oldV = paramName + "=";

		// first remove old value
		idx = url.indexOf(oldV);
		if (idx >= 0) {
			ret.append(url.substring(0, idx));
			int idxend = url.indexOf("&", idx + paramName.length());
			if (idxend >= 0) {
				ret.append(url.substring(idxend + 1));
			}
		} else {
			ret.append(url);
		}

		// append new value
		idx = url.indexOf('?');
		if (idx >= 0) {
			ret.append('&');
		} else {
			ret.append('?');
		}
		ret.append(oldV);
		ret.append(urlEncode(paramValue));

		return ret.toString();

	}

	/**
	 * get Application's version string
	 * 
	 * @param context
	 * @return
	 */
	public static String getAppVersionName(Context context) {

		PackageManager pm = context.getPackageManager();
		try {
			PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
			return pi == null ? "00.00.00" : pi.versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "00.00.00";
	}

	public static int getVersionCode(Context context) {

		PackageManager pm = context.getPackageManager();
		try {
			PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
			return pi == null ? 0 : pi.versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return 0;
	}

	/**
	 * get apk version string
	 * 
	 * @param context
	 * @param archiveFilePath
	 * @return
	 */
	public static int getVersionCode(Context context, String archiveFilePath) {
		PackageManager pm = context.getPackageManager();
		PackageInfo pi = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES);
		return pi == null ? 0 : pi.versionCode;
	}
	
	public static void sendMail(Context ctx) {
		// Intent returnIt = new Intent(Intent.ACTION_SEND);
		// String[] tos = { "lxyyzm@gmail.com" };
		// // String[] ccs = { "lxyyzm@gmail.com" };
		// returnIt.putExtra(Intent.EXTRA_EMAIL, tos);
		// // returnIt.putExtra(Intent.EXTRA_CC, ccs);
		// // returnIt.putExtra(Intent.EXTRA_TEXT, "body");
		// returnIt.putExtra(Intent.EXTRA_SUBJECT,
		// ctx.getString(R.string.feed_back_subject));
		// // returnIt.setType("message/rfc882");
		// Intent intent = Intent.createChooser(returnIt, "��ѡ���ʼ��ͻ��ˣ�");
		// ctx.startActivity(intent);

		try {
			Uri uri = Uri.parse("mailto:shantingshu@gmail.com");
			Intent it = new Intent(Intent.ACTION_SENDTO, uri);
			it.putExtra(Intent.EXTRA_SUBJECT, ctx
					.getString(R.string.feed_back_subject));
			it.putExtra(Intent.EXTRA_TEXT, "\n\n\n\n" + "(系统版本："
					+ android.os.Build.VERSION.RELEASE + ")" + "\n" + "(应用版本："
					+ getAppVersionName(ctx) + ")");
			ctx.startActivity(it);
		} catch (ActivityNotFoundException e) {
			// TODO: handle exception
			CommonDlg.showErrorDlg(ctx, "抱歉，系统内找不到邮件发送应用！", null);
		}
	}

	public static void sendMail(Context ctx, String exception) {
		try {
			Uri uri = Uri.parse("mailto:shantingshu@gmail.com");
			Intent it = new Intent(Intent.ACTION_SENDTO, uri);
			it.putExtra(Intent.EXTRA_SUBJECT, ctx
					.getString(R.string.err_feed_back_subject));
			it
					.putExtra(
							Intent.EXTRA_TEXT,
					"（非常感谢您的支持！因为我们无法再现这个错误，如果您知道如何操作就能一定会导致此错误发生，请一定告诉我们操作方法！我们保证第一时间修复！如果您也没发现规律，请取消发送本邮件。）\n\n\n\n"
							+ "(系统版本："
									+ android.os.Build.VERSION.RELEASE
									+ ")"
									+ "\n"
							+ "(应用版本："
									+ getAppVersionName(ctx)
									+ ")" + "\n" + exception);
			ctx.startActivity(it);
		} catch (ActivityNotFoundException e) {
			// TODO: handle exception
			CommonDlg.showErrorDlg(ctx, "抱歉，系统内找不到邮件发送应用！", null);
		}
	}

	/**
	 * 
	 */
	public static void share(Context ctx, Intent i) {
		Intent intent = new Intent();
		if (i != null) {
			intent = (Intent) i.clone();
		}

		intent.setClass(ctx, ShareActivity.class);
		ctx.startActivity(intent);
	}

	public static final long getFreeSpace(String path) {
		if (path == null) {
			return 0;
		}

		try {
			// workaround for native exception...
			StatFs stat = new StatFs(path);
			long blockSize = stat.getBlockSize();
			long availableBlocks = stat.getAvailableBlocks();
			return availableBlocks * blockSize;
		} catch (Exception e) {
			return 0;
		}
	}

	public static int parseInt(String str, int defValue) {
		if (null == str || str.length() < 1) {
			return defValue;
		}
		try {
			return Integer.parseInt(str);
		} catch (Exception ex) {
		}
		return defValue;
	}

	/**
	 * check and append host into url. e.g. /sss/a.jpg ->
	 * http://www.mspaces.net/sss/a.jpg
	 * 
	 * @param url
	 * @return
	 */
	public static String fixUrlHost(String url, String host) {
		if (null == url || url.length() < 1 || null == host
				|| host.length() < 1) {
			return url;
		}

		if (url.startsWith("/")) {
			// append host
			return "http://" + host + url;
		}

		return url;
	}
	
    public static String getUrlHost(String url) {
        if (null == url || url.length() < 1) {
            return "";
        }

        int idx = url.indexOf("://");
        if (idx < 0) {
            return "";
        }
        idx += 3;

        int idx2 = url.indexOf(":", idx);
        if (idx2 < 0) {
        	idx2 = url.indexOf("/", idx);
        }
        if (idx2 < 0) {
            return "";
        }
        
        return url.substring(idx, idx2);
    }

	public static String getExtention(String fPath) {
		fPath = fPath.toLowerCase();
		if (!fPath.endsWith(".mp3") && !fPath.endsWith(".wma") && !fPath.endsWith(".apk")) {
			return ".mp3";
		}
		int idx = fPath.lastIndexOf(".");
		return fPath.substring(idx);
	}

	public static boolean isMp3orWma(String fPath) {
		fPath = fPath.toLowerCase();
		return (fPath.endsWith(".mp3") || fPath.endsWith(".wma"));
	}

	public static boolean canSeekPos(Song s, String dirname) {
//		if (s.existLocally(dirname)) {
//			return true;
//		} else if (Cfg.SDK_VERSION >=6 && s.isMp3()) {
//			return true;
//		} else {
//			return false;
//		}
		return true;

	}

	public int getSongIdx(ArrayList<Song> songs, String name) {
		if (songs != null) {
			for (Song s : songs) {
				if (name.equals(s.getName())) {
					return songs.indexOf(s);
				}
			}
		}
		return -1;
	}

	// public static boolean fileExist(String path) {
	// return new File(path).exists();
	// }
	
	/***
	 * 更新到115Ver，歌曲有详细地址也可以翻页
	 */
	private static boolean updateBookTo115Ver(Book book){
		if(book.mSongStringsMap == null){
			book.mSongStringsMap = new HashMap<Integer, String[]>();
		}
		if(book.mSongSizeMap == null){
			book.mSongSizeMap = new HashMap<Integer, int[]>();
		}
		if(book.mAutoPath){
			//TODO:zhaoyp:本地存储还是用autoPath，可以节省内存占用大小
//			String index = book.mSongName.substring(book.mCommonTitle.length());
//			if(index != null){
//				try {
//					int end = book.mCurPlayPage * Cfg.SONGS_PER_PAGE;
//					int start = (book.mCurPlayPage - 1) * Cfg.SONGS_PER_PAGE;
//					book.mSongStrings = new String[30];
//					for (int i = start ; i <= end; i++) {
//						book.mSongStrings[i-start] = book.mCommonTitle + (i) + "|"
//								+ book.mCommonAddr +  "/" + i + ".mp3";
//					}
//					book.mAutoPath = false;
//					return true;
//					
//				}catch (Exception e) {
//					// TODO: handle exception
//					return false;
//				}
//			}
			
			return false;
			
		}else {
			int index = -1;
			if(book.mCurPlayPage > 1 ||(book.mSongStrings == null) ||
					//本地类表SongStrings为空
					(book.mSongStrings != null && book.mSongCount != book.mSongStrings.length)){
				return false;
			}
			//计算当前播放歌曲是在第几页
			for(int i = 0; i < book.mSongStrings.length; i++ ){
				if(book.mSongStrings[i] != null 
						&& book.mSongStrings[i].startsWith(book.mSongName)){
					index = i ;
					break;
				}
			}
			if(index != -1){
				book.mCurPlayPage = index / Cfg.SONGS_PER_PAGE + 1;
				book.mPageCount = (book.mSongCount - 1) / Cfg.SONGS_PER_PAGE + 1; 
				
				convertSongsToMap(book);
				return true;
			}
			
			
			
			return true;
		}
	}
	
	/**
	 * 升级book，将单一的列表改为hashMap
	 * @param book
	 */
	public static void convertSongsToMap(Book book){
		String tempSongs[];
		String destPageSongs[] = null;
		
		if(book == null || book.mSongStrings == null){
			return ;
		}
		
		//1.如果小说一共只有一页
		//2.如果页面上只放了第一页的地址列表
		if((book.mPageCount == 1 && book.mSongCount <= Cfg.SONGS_PER_PAGE ) 
				||(book.mSongCount > book.mSongStrings.length)){
			book.mSongStringsMap.put(1, book.mSongStrings);
			book.mCurPlayPage = 1;
			return ;
		}
		
		for(int i = 1; i < book.mPageCount ; i++){
			tempSongs = new String[Cfg.SONGS_PER_PAGE];
			
			System.arraycopy(book.mSongStrings, (i - 1) * Cfg.SONGS_PER_PAGE, tempSongs, 0, Cfg.SONGS_PER_PAGE);
			book.mSongStringsMap.put(i, tempSongs);
			if(i == book.mCurPlayPage){
				destPageSongs = tempSongs;
			}
			
		}
		
		tempSongs = new String[book.mSongCount % Cfg.SONGS_PER_PAGE];
		System.arraycopy(book.mSongStrings, (book.mPageCount - 1) * Cfg.SONGS_PER_PAGE, tempSongs, 0, book.mSongCount % Cfg.SONGS_PER_PAGE);
		book.mSongStringsMap.put(book.mPageCount, tempSongs);
		if(destPageSongs != null){
			book.mSongStrings = destPageSongs;
		}
	}
//	static Object mPreLoadLock = new Object();
	public static ArrayList<Book> mBookListCache = new ArrayList<Book>();
	
	public static void refactorHisFiles(Context ctx) {
		
		//预先加载
		if (Cfg.mHistoryVersion > 0) {

			String commonPath = Cfg.mInnerRootPath + Cfg.HISTORY_FILE_DIR
				+ Cfg.HISTORY_FILE_FOREBODY;
			String destCommonPath = Cfg.mInnerRootPath + Cfg.BOOKSHELF_FILE_DIR
				+ Cfg.BOOKSHELF_FILE_FOREBODY;

			//大于99版本的，直接从book下取历史记录
			if(Cfg.mHistoryVersion > Cfg.HISTORY_VERSION_1){
				commonPath = destCommonPath;
			}
//			synchronized (mPreLoadLock) {
			
			for (int i = 0; i < Cfg.COUNT_BOOK_ON_SHELF; i++) {
				String path = commonPath + i;
				String destPath = destCommonPath + i;
				File f = new File(path);
				File destFile = new File(destPath);
				try {
					if (!f.exists()) {
						continue;
					}
//					Log.v(TAG, path);
					FileInputStream fis = new FileInputStream(f);
					ObjectInputStream in = new ObjectInputStream(fis);
					Book book = null;
					boolean needRewrite = false;
					
					//1. 99版本,需要把History1转换为Book
					if(Cfg.mHistoryVersion < Cfg.HISTORY_VERSION_1){
						History1 oldHis = (History1) in.readObject();	
						needRewrite |= true;
						if (oldHis.mOnline) {
							
						} else {
//							oldHis = new History1(oldHis.mLcDirPath, oldHis.mLcDirName,
//									oldHis.mSongName, oldHis.mPosition);
							book = new Book(oldHis.mLcDirPath, oldHis.mSongName);
							book.mPosition = oldHis.mPosition;
						}
					}else {
						book = (Book) in.readObject();
					}
					
					
					in.close();
					fis.close();

					//2. 115版本，需要更新有详细标题的分页
					if(Cfg.mHistoryVersion < Cfg.HISTORY_VERSION_2){
						needRewrite |= updateBookTo115Ver(book);
					}
					
					//3. 118版本，需要更新下载路径到“善听”
					if(Cfg.mHistoryVersion < Cfg.HISTORY_VERSION_MODIFY_DEFAULT_DOWNLOAD_DIRECTROY){
						// update local directory from "ShanTing" to "善听" for the books in bookshelf
						needRewrite |= updateBookLocalDir(book);
					}

					//放入到cache里
					if (book != null) {
						mBookListCache.add(book);
					}
					
					//有内容改变，需要重新写入
					if(needRewrite){
						FileOutputStream fileStream = null;
						fileStream = new FileOutputStream(destFile);
						ObjectOutputStream out = new ObjectOutputStream(fileStream);
						out.writeObject(book);
						out.close();
						fileStream.close();
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					mBookListCache = null;
				}
			}
			
			
//			}
			//低版本用户升级提示
			if(Cfg.mTipShownVer <= Cfg.HISTORY_VERSION_1 && Cfg.mTipShownVer > 0){
//				MyToast.showLong(ctx, "非常抱歉，由于本版改动较大，部分历史记录丢失，还请您多多包涵！");
			}
			
		}
		
//		Log.e("MyLog", "Cfg.mHistoryVersion is " + Cfg.mHistoryVersion);

//		if (Cfg.mHistoryVersion < Cfg.HISTORY_VERSION_MODIFY_DEFAULT_DOWNLOAD_DIRECTROY
//				&& Cfg.mHistoryVersion > 0) {
//			BookShelf.updateLocalDir();
//		}
		
		// delete all finished download item from database
		if (Cfg.mHistoryVersion < Cfg.HISTORY_VERSION_DELETE_FINISHED_DOWNLOAD_ITEMS
				&& Cfg.mHistoryVersion > 0) {
			deleteFinishedDownloadItem();
		}
		
		//update the final version
		if (Cfg.mHistoryVersion <  Cfg.HISTORY_VERSION_DELETE_FINISHED_DOWNLOAD_ITEMS) {
//			Cfg.mHistoryVersion  = Cfg.HISTORY_VERSION_2;
			Cfg.mHistoryVersion = Cfg.HISTORY_VERSION_DELETE_FINISHED_DOWNLOAD_ITEMS;
			Cfg.saveInt(ctx, Cfg.PREF_HISTORY_VERSION, Cfg.mHistoryVersion);
		}
	}

	/**
	 * 获取预加载的book列表，如果此刻正在升级，该线程会阻塞
	 * 
	 * @param books 
	 * 
	 * @return true:   成功使用了预先加载的列表
	 *         false:  预先加载的列表为空
	 */
	public static boolean getpreLoadBooksList(ArrayList<Book> books){
//		synchronized (mPreLoadLock) {
			if(mBookListCache != null){
//				books.addAll(mBookListCache);
				int count = mBookListCache.size();
				Book book;
				for(int i = 0; i < count; i++){
					book = mBookListCache.get(i);
					if(book != null){
						books.add(book);
					}
				}
				mBookListCache = null;
				return true;
			}
//		}
		
		
		
		return false;
	}
	/**
	 * 更新book的本地路径
	 * 
	 * @param book
	 * @return
	 */
	public static boolean updateBookLocalDir(Book book){
		if(book.mOnline){
			return false;
		}
		String oldString = Cfg.SDCARD_PATH + "/ShanTing/";
		String newString = Cfg.SDCARD_PATH + "/善听/";
		
		if (book.mLcDirPath.startsWith(oldString)) {
			int lastIdx = book.mLcDirPath.lastIndexOf(oldString);
			String bookRelativelyPath = book.mLcDirPath.substring(lastIdx + oldString.length());
			book.mLcDirPath = newString + bookRelativelyPath;
			return true;
		}
		
		return false;
	}
//	public static void addWqAd(final Activity act) {
//		if (!Cfg.IS_WITHAD || Cfg.mHiddenGuangGao) {
//			return;
//		}
//		ADView ad = (ADView) act.findViewById(R.id.adview);
//		ad.Init(act.getApplicationContext().getResources().openRawResource(R.raw.wqappsetting));
//	}
	
	public static boolean shouldShowPopAd() {
		return Cfg.mAccountInited && !Cfg.mIsVIP;
	}
	
	public static boolean shouldShowAd() {
		return Cfg.IS_WITHAD && Cfg.mAccountInited && !Cfg.mIsVIP;
	}

	public static void addAdView(final Activity act) {
		
		if (!shouldShowAd()) {
			return;
		}

				
		AdsMogoLayout adMogoLayoutCode = new AdsMogoLayout(act, Cfg.DEBUG ? "3d4f5d6e26a54cd38b3c619933a9f603" : "0235354b58aa4bbb95f44da0b6f0c99d", true);
		adMogoLayoutCode.downloadIsShowDialog = true;
		adMogoLayoutCode.setAdsMogoListener(new AdsMogoListener() {

			
			@Override
			public void onFailedReceiveAd() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onCloseMogoDialog() {
				// TODO Auto-generated method stub
			}
			
			@Override
			public boolean onCloseAd() {
				// TODO Auto-generated method stub
				if (!Cfg.mIsVIP) {
					CommonDlg.showConfirmDlg(act, -1, act.getString(R.string.dialog_title_kill_ad), new DialogInterface.OnClickListener() {					
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Intent intent = new Intent();
							intent.setClass(act, WebBrowser.class);
							intent.putExtra(DN.URL, ShanTingAccount.instance().getBuyServiceUrl());
							intent.putExtra(DN.TITLE, "个人中心");
							act.startActivity(intent);
						}
					});
				}
				return true;
			}

			@Override
			public void onClickAd(String arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onRealClickAd() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onReceiveAd(ViewGroup arg0, String arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onRequestAd(String arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Class getCustomEvemtPlatformAdapterClass(
					AdsMogoCustomEventPlatformEnum arg0) {
				// TODO Auto-generated method stub
				return null;
			}
			
		});
		
		LinearLayout layout = (LinearLayout) act.findViewById(R.id.adLayout);
		RelativeLayout.LayoutParams adViewLayoutParams = new RelativeLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		layout.addView(adMogoLayoutCode, adViewLayoutParams);
		layout.invalidate();
		
		
		//adview
//		LinearLayout layout = (LinearLayout) act.findViewById(R.id.adLayout);
//		LinearLayout adParent = (LinearLayout) act.findViewById(R.id.adParent);
//		final ImageView hideAdView = (ImageView)act.findViewById(R.id.hideAdIcon);
//		AdViewLayout adViewLayout;
//		
//		adParent.setVisibility(View.VISIBLE);
//
//		 /* 下面两行只用于测试,完成后一定要去掉,参考文挡说明 */
//		if (Cfg.DEBUG) {
//			AdViewTargeting.setUpdateMode(UpdateMode.EVERYTIME);
////			 AdViewTargeting.setRunMode(RunMode.TEST);
//			adViewLayout = new AdViewLayout(act,
//					"SDK20111815220310q4jcmagqevfk4v6"); // test
////			 adViewLayout = new AdViewLayout(act,
////			 "SDK20111216590946dtg2m1iw7fkcsv6"); //test2
//		} else {
//			adViewLayout = new AdViewLayout(act,
//					"SDK201116101303172p1sgorbgb82ny4");// normal
//		}
//		RelativeLayout.LayoutParams adViewLayoutParams = new RelativeLayout.LayoutParams(
//				LayoutParams.FILL_PARENT, Cfg.SDK_VERSION == 3 ? 48
//						: LayoutParams.WRAP_CONTENT);
//		layout.addView(adViewLayout, adViewLayoutParams);
//		if(hideAdView != null){
//			hideAdView.setVisibility(View.GONE);
//			adViewLayout.setAdViewInterface(new AdViewInterface() {
//				
//				@Override
//				public void onDisplayAd() {
//					// TODO Auto-generated method stub
//					hideAdView.setVisibility(View.VISIBLE);
//				}
//				
//				@Override
//				public void onClickAd() {
//					// TODO Auto-generated method stub
//					
//				}
//			});
//			
//			hideAdView.setOnClickListener(new View.OnClickListener() {
//				
//				@Override
//				public void onClick(View v) {
//					// TODO Auto-generated method stub
//					Intent intent = new Intent();
//					intent.setClass(act, WebBrowser.class);
//					intent.putExtra(DN.URL, ShanTingAccount.instance().getBuyServicePage());
//					intent.putExtra(DN.TITLE, "个人中心");
//					act.startActivity(intent);
//				}
//			});
//		}
		
	}

	/**
	 * 各个网站的微博名不一样，按照分享的类型进行替换
	 */
	public static String replaceMicroBlogName(String content, int shareType) {
		if(content == null){
			return null;
		}
		switch(shareType){
		case AccountConnect.SHARE_TO_Q_WEIBO:
			return content.replace("@善听听书",
					"@shantingtingshu");
		case AccountConnect.SHARE_TO_Q_ZONE:
			return content.replace("@善听听书( http://shanting.mobi )", "#善听#");
		}
		
		return content;
	}
	
	/**
	 * 更新1.12版本以前的收藏夹里的url，转换到新的支持有图无图切换的页面
	 * 
	 * @param oldData	收藏夹里的数据
	 * @param isWutu 	是否无图，
	 */
	public static void updateFavoritePages(Map<String, String> oldData, boolean isWutu){
		
		if(oldData == null || oldData.isEmpty()){
			return ;
		}
		
		HashMap<String, Integer> rule = new HashMap<String, Integer>(51){/**
			 * 
			 */
			private static final long serialVersionUID = -8035153864062469535L;

		{
			//百家讲坛
			put("bjjt/qita", 57);
			put("bjjt/lishishigeshenme", 91);
			//名家评书
			put("pingshu/shantianfang", 34);
			put("pingshu/tianlianyuan", 35);
			put("pingshu/liulanfang", 36);
			put("pingshu/zhangshaozuo", 37);
			put("pingshu/lianliru", 38);
			put("pingshu/yuankuocheng", 39);
			put("pingshu/wangyuebo", 61);
			//精品相声
			put("xiangsheng/qita", 42);
			put("xiangsheng/dankou", 83);
			put("xiangsheng/duikouxiangsheng", 88);
			put("xiangsheng/liubaorui", 94);
			//儿童读物
			put("ertong/qita", 43);
			put("ertong/tangshi", 92);
			//英语
			put("yingyu/qita", 74);
			put("yingyu/mansuyingyu", 90);
			put("yingyu/xingainian", 80);
			put("yingyu/lyfkyy", 81);
			
			//其他类别
			put("qt/qita", 59);
			put("qt/fjyy", 96);
			put("qt/gxt", 100);
			put("qt/yushiwei", 66);
			
			//恐怖悬疑
			put("kbxy/guichuideng", 49);
			put("kbxy/mizongzhiguo", 76);
			put("kbxy/tianji", 78);
			put("kbxy/daomubiji", 62);
			put("kbxy/qumoren", 68);
			put("kbxy/renjian", 70);
			put("kbxy/qita", 50);
			
			//
			put("dsyq/qita", 44);
			//
			put("xhwx/qita", 45);
			
			//
			put("lsjs/qita", 46);
			put("lsjs/mingchaonaxieshi", 60);
			
			//
			put("wycy/qita", 47);
			
			//
			put("mingzhu/jingdian", 87);
			put("mingzhu/pfdsj", 101);
			//
			put("zjjs/qita", 51);
			put("zjjs/zangao", 73);
			put("zjjs/dayan", 71);
			
			//
			put("zttl/qita", 48);
			put("zttl/meisen", 93);
			//
			put("gcsz/qita", 53);
			put("gcsz/gcbj", 95);
			//
			put("hyjt/qita", 52);
			
			//
			put("xzff/qita", 54);
			put("xzff/xingjing803", 55);
			//
			put("shsh/qita", 56);
			//
			put("qtxs/qita", 58);
			
		}
		};
		
		HashMap<String, Integer> ruleList = new HashMap<String, Integer>(51){/**
			 * 
			 */
			private static final long serialVersionUID = 1372757768222511686L;

		{
			put("zuire", 2);
			put("pinglun", 3);
			put("zuixin", 1);
		}
		};
		
		Set<Map.Entry<String,String>> entrys = oldData.entrySet();
		Iterator<Map.Entry<String, String>> iterator = entrys.iterator();
		
		Entry<String, String> fav;
		String url;
		int classID=0,pageID=0;
		
		
		while(iterator.hasNext()){
			fav = iterator.next();
			url = fav.getValue();
			String newUrl = null;
			Uri uri;
			try {
				uri = Uri.parse(url);
				
				if(uri.getEncodedQuery() != null){
					//需要输入参数
					newUrl = url;
					
				}else if(uri.getPathSegments() != null){
					List<?> paths = uri.getPathSegments();
					if(paths.size() >= 2){
						String pathString = paths.get(0) + "/" + paths.get(1);
						if(rule.containsKey(pathString)){
							//解析到classID
							classID = rule.get(pathString);
						}

						//最后一段匹配到 数字.html 认为是pageID
						String lastPath = uri.getLastPathSegment();
						if(lastPath != null){
							if(lastPath.matches("^[0-9]+.html$")){
								try {
									pageID = Integer.parseInt(lastPath.substring(0,lastPath.length()-5));
								} catch (Exception e) {
									// TODO: handle exception
								}
							}
						}
						
						if(classID != 0){
							if(pageID != 0){
								newUrl = Cfg.WEB_HOME + "/e/action/ShowInfo.php?classid=" 
								+ classID+"&id=" + pageID + (isWutu ? "&wutu=1" : "");
								//书籍详情的最新地址
							}else{
								//二级目录地址
								newUrl = Cfg.WEB_HOME +"/e/action/ListInfo/?classid="
								+ classID + (isWutu ? "&wutu=1" : "");
							}
						}
						
					}else {
						//最热、最新、排名等
						if(ruleList.containsKey(paths.get(0))){
							newUrl = Cfg.WEB_HOME + "/e/extend/paihang/index.php?ph=" + ruleList.get(paths.get(0))
							+ (isWutu ? "&wutu=1" : "");
						}
					}
					
				}
				
			
				log.d("url=" + url + " newUrl = " + newUrl);
				//新地址压入表中
				if(newUrl != null){
					oldData.put(fav.getKey(), newUrl);
				}
				
				
			}catch (Exception e) {
				// TODO: handle exception
			}
			
			newUrl = null;
			classID = 0;
			pageID = 0;
			
		}
		
		rule = null;
		ruleList = null;
		
	}
	
	
	/**
	 * 切换有图无图模式时进行地址更改替换
	 * 
	 * 
	 * @param isNoPicMode
	 */
	public static String switchUrlByPicMode(String url,boolean isNoPicMode) {
		if (url == null || url.length() <= 0){
			return url;
		}
		StringBuilder newUrl = new StringBuilder(url.length());

		Uri uri = Uri.parse(url);
		
		if (isNoPicMode) {
			if (url.contains("?") && !url.contains("wutu=1")){//动态页面，增加wutu=1
				if(uri.getQuery() != null){
					newUrl.append(url).append("&wutu=1");
				}else if(url.contains("?")){
					newUrl.append(url).append("wutu=1");
				}else{
					newUrl.append(url).append("?wutu=1");
				}
			} else {//绝对地址
				if (url.equalsIgnoreCase(Cfg.WEB_HOME) // 书城首页
						|| url.equalsIgnoreCase(Cfg.WEB_HOME + "/")
						//从有图版跳到分类页面，分类地址为Cfg.WEB_HOME_HASPIC
						|| url.equalsIgnoreCase(Cfg.WEB_HOME_HASPIC)) {
					newUrl.append(Cfg.WEB_HOME_NOPIC);
				}  else if (url.equalsIgnoreCase(Cfg.FAVORITE_URL)
						|| url.equalsIgnoreCase(Cfg.WEB_HOME + "/")){
					newUrl.append(Cfg.FAVORITE_URL_NOPIC);
				}
				
			}
			
		} else {
			//有无图的参数
			if(url.contains("wutu=1")){
				if(url.endsWith("?wutu=1")){
					newUrl.append(url.replace("wutu=1", ""));
				}else if(url.endsWith("&wutu=1")){
					newUrl.append(url.replace("&wutu=1", ""));
				}else {
					newUrl.append(url.replace("wutu=1&", ""));
				}
			}
			else if (url.equalsIgnoreCase(Cfg.WEB_HOME_NOPIC + "/")
					|| url.equalsIgnoreCase(Cfg.WEB_HOME_NOPIC)) {
				newUrl.append(Cfg.WEB_HOME);
				// 其他的静态页面
			}else if(url.equalsIgnoreCase(Cfg.FAVORITE_URL_NOPIC)
					||url.equalsIgnoreCase(Cfg.WEB_HOME_NOPIC + "/") ){
				
				newUrl.append(Cfg.FAVORITE_URL);
			}
		}
		
		if (newUrl.length() > 0) {
			return newUrl.toString();
		}
		
		return url;
	}
	
	public static String getDestHost(){
		String destHost = Cfg.WEB_HOME_TEST;		
		switch(Cfg.mHostType){
		case Cfg.HOST_TYPE_3G:
			destHost = Cfg.WEB_HOME_3G;
			break;
		case Cfg.HOST_TYPE_TEMP:
			destHost = Cfg.WEB_HOME_TEMP;
			break;
		case Cfg.HOST_TYPE_COM:
			destHost = Cfg.WEB_HOME_COM;
			break;
		}
		
		return destHost;
		
	}
	/*
	* 切换Host地址
	*/
	public static void switchHost(Context ctx, boolean clearVipData){
		
		String destHost = getDestHost();
		
		if(Cfg.WEB_HOME.equalsIgnoreCase(destHost)){
			return ;
		}
		
		//1.地址转换host
		Cfg.WEB_HOME_NOPIC = Cfg.WEB_HOME_NOPIC.replace(Cfg.WEB_HOME, destHost);
		Cfg.FAVORITE_URL = Cfg.FAVORITE_URL.replace(Cfg.WEB_HOME, destHost);
		Cfg.BOOK_QUERY_URL = Cfg.BOOK_QUERY_URL.replace(Cfg.WEB_HOME, destHost);
		Cfg.GET_MUSIC_PATHS_URL = Cfg.GET_MUSIC_PATHS_URL.replace(Cfg.WEB_HOME, destHost);
		Cfg.FAVORITE_URL_NOPIC = Cfg.FAVORITE_URL_NOPIC.replace(Cfg.WEB_HOME, destHost);
		Cfg.FAQ_URL = Cfg.FAQ_URL.replace(Cfg.WEB_HOME, destHost);
		
		AliPay.NOTIFY_URL = AliPay.NOTIFY_URL.replace(Cfg.WEB_HOME, destHost);
		
		ShanTingAccount.BUY_SERVICE_URL = ShanTingAccount.BUY_SERVICE_URL.replace(Cfg.WEB_HOME, destHost);
		ShanTingAccount.QUERY_URL = ShanTingAccount.QUERY_URL.replace(Cfg.WEB_HOME, destHost);
		ShanTingAccount.BUY_MONTHLY_RENT_SERVICE_PAGE = ShanTingAccount.BUY_MONTHLY_RENT_SERVICE_PAGE.replace(Cfg.WEB_HOME, destHost);
		ShanTingAccount.ACCOUNT_CENTER_URL = ShanTingAccount.ACCOUNT_CENTER_URL.replace(Cfg.WEB_HOME, destHost);
		ShanTingAccount.EARN_SHANBEI_URL = ShanTingAccount.EARN_SHANBEI_URL.replace(Cfg.WEB_HOME, destHost);
		AppUpdater.VERSION_URL = AppUpdater.VERSION_URL.replace(Cfg.WEB_HOME, destHost);
		NotificationMgr.NOTIFICATION_URL = NotificationMgr.NOTIFICATION_URL.replace(Cfg.WEB_HOME, destHost);
		
		
		Cfg.WEB_HOME = destHost;
		
		//2.VIP存储数据还原
		if(clearVipData){

			Cfg.mIsVIP = false;
			Cfg.saveBool(ctx, Cfg.IS_VIP, false);
			
//			Cfg.mUserID = null;
			Cfg.saveStr(ctx, Cfg.PREF_USER_ID, null);
			
			Cfg.mAccountInited = false;
			Cfg.saveBool(ctx, Cfg.ACCOUNT_INIT, Cfg.mAccountInited);
			
			Cfg.mServiceExpireTime = 0L;
			Cfg.saveLong(ctx, Cfg.SERVICE_EXPIRE_TIME, 0L);
	
			//清空推送信息
			Cfg.saveInt(ctx, Cfg.PREF_NOTIFICATION_ID, NotificationMgr.INVALID_NOTIFICATION_ID );
			
		}
				
	}
	
	/**
	 * 评星成功
	 * 
	 * @param ctx
	 */
	public static void rateStarOK(Context ctx){
		if (!Cfg.mHasUserRated) {
			Cfg.mHasUserRated = true;
			Cfg.saveBool(ctx, Cfg.PREF_USER_HAS_RATED, true);
		}
	}
	/**
	 * 是否需要弹出提示
	 * @return
	 */
	public static boolean needNoticeRateStars(){
//		log.d("times: " + Cfg.mAppStartTimes + ", has = " + Cfg.mHasUserRated);
		return (!Cfg.mRateTipsShown && Cfg.mAppStartTimes > Cfg.RATE_STAR_CONDITION_START_TIMES);
	}
	
	/**
	 * open buy-VIP url.
	 * 
	 * @param ctx
	 */
	public static void openBuyVipPage(Context ctx){
		Intent intent = new Intent(ctx, WebBrowser.class);
		String url = ShanTingAccount.instance().getBuyServiceUrl();
		intent.putExtra(DN.URL, url);
		intent.putExtra(DN.TITLE, ctx.getResources().getString(R.string.userCenter));
		ctx.startActivity(intent);
	}
	/**
	 * 高峰时段是否达到下载限制
	 * 
	 * @return
	 */
//	public static boolean isToDownloadLimitInPeakTimes(Context ctx){
//		log.d("Cfg.mCurCompleteTasks = " + Cfg.mCompletedTaskInPeakTimes);
//		return isInPeakTimes(ctx) && Cfg.mCompletedTaskInPeakTimes >= Cfg.MAX_DONWLOAD_TASKS_IN_PEAR_TIME;
//	}
	/**
	 * 单日下载是否达到限制
	 * @param ctx
	 * @return
	 */
	public static boolean isToDownloadDayLimit(Context ctx){
		return isInSameDayDownload(ctx) && Cfg.mCompletedTaskInOneDay >= Cfg.MAX_DONWLOAD_TASKS_IN_DAY;
	}
	
	/**
	 * 获取达到高峰时段时的提示语
	 * @return
	 */
//	public static String getPeakTimeLimitMsgString(){
//		return "非VIP会员高峰时段（晚8:00 - 凌晨1:00，下午1:00-2:00）最多可以下载" + Cfg.MAX_DONWLOAD_TASKS_IN_PEAR_TIME + "集，已达到限制。";
//	}
//	public static String getPeakTimeLimitNtfMsg(){
//		return "非VIP会员高峰时段（晚8:00 - 凌晨1:00，下午1:00-2:00）最多可以下载" + Cfg.MAX_DONWLOAD_TASKS_IN_PEAR_TIME + "集，已达到限制，点击查看详情。";
//	}
	/**
	 * 获取达到单日下载限制时的提示语
	 * @return
	 */
	public static String getDayLimitMsgString(){
		return "非VIP会员每天最多可以下载" + Cfg.MAX_DONWLOAD_TASKS_IN_DAY + "集，已达到限制。";
	}
	public static String getDayLimitNtfMsg(){
		return "非VIP会员每天最多可以下载" + Cfg.MAX_DONWLOAD_TASKS_IN_DAY + "集，已达到限制，点击查看详情。";
	}
	
	/**
	 * 完成了一个任务
	 */
	public static void onCompletedOneTask(Context ctx){
//		if(isInPeakTimes(ctx)){
//			Cfg.mCompletedTaskInPeakTimes++ ;
//			Cfg.saveInt(ctx, Cfg.PREF_CURRENT_DOWNLOADED_TASKS, Cfg.mCompletedTaskInPeakTimes);
//		}
		
		if(isInSameDayDownload(ctx)){
			Cfg.mCompletedTaskInOneDay ++;
			Cfg.saveLong(ctx, Cfg.PREF_LAST_DOWLOAD_DAY_LIMIT_DATE, Cfg.mLastDayLimitMills);
		}
		
	}
	
	/**
	 * 是否是同一天的下载
	 * @param ctx
	 * @return
	 */
	public static boolean isInSameDayDownload(Context ctx){
		if(Cfg.mIsVIP){
			return false; 
		}
		
		Time t=new Time(); //
		t.setToNow(); //
		Time last = new Time();
		last.set(Cfg.mLastDayLimitMills);
		boolean result = true;
		
		if(last.yearDay != t.yearDay){
			result = false;
			
			Cfg.mCompletedTaskInOneDay = 0;
			Cfg.saveInt(ctx, Cfg.PREF_DOWNLOADED_TASKS_BY_DAY, 0);
			
			Cfg.mLastDayLimitMills = t.toMillis(false);
			Cfg.saveLong(ctx, Cfg.PREF_LAST_DOWLOAD_DAY_LIMIT_DATE, Cfg.mLastDayLimitMills);
		}
		
		return result;
		
	}
	
	
	/**
	 * 是否为同一天的高峰时段
	 * @return
	 
	public static boolean isInPeakTimes(Context ctx){
		if(Cfg.mAdsHidden){
			return false; 
		}
		Time t=new Time(); //
		t.setToNow(); // 
		Time last = new Time();
		
		//高峰时段应该是13:00-14:00以及 20:00 - 次日1:00，
		//时间各减少1小时，只需要计算12:00 - 13:00 以及 19:00 - 24:00即可，不需要跨天;
		last.set(Cfg.mLastPeakLimitMills - Cfg.MS_PER_HOUR);
		t.set(t.toMillis(false) - Cfg.MS_PER_HOUR);
		
		boolean result = false;
		
		//9点到凌晨一点之间
		if((t.hour >= 19 && t.hour < 24) || t.hour == 12){
			result = true;
			//不是同一天的高峰时段，清零数据
//			if(!((t.hour == 0 && last.yearDay == (t.yearDay - 1) && last.hour >= 21)//前一天的21点以后和今天的0点
//					|| (t.yearDay == last.yearDay && (last.hour >= 21 && t.hour >= 21))//同一天的21点以后
//					|| (t.yearDay == last.yearDay && (last.hour == 0 && t.hour == 0)))){//同一天的0点
//			
			if(t.yearDay != last.yearDay){
				log.d("clear data");
				Cfg.mCompletedTaskInPeakTimes = 0;
				Cfg.saveInt(ctx, Cfg.PREF_CURRENT_DOWNLOADED_TASKS, 0);
				
				Cfg.mLastPeakLimitMills = t.toMillis(false) + Cfg.MS_PER_HOUR;
				Cfg.saveLong(ctx, Cfg.PREF_LAST_DOWNLOAD_LIMIT_DATE, Cfg.mLastPeakLimitMills);
				
			}
		}
		
		log.d("result = " + result + "t.yearDay = " + t.yearDay + ",last=" + last.yearDay);
		return result;
	}*/
	
	
	/**
	 * 加载自定义的错误页面
	 * 
	 * @param view
	 * @param url
	 */
	public static void loadErrorPage(WebView view, String url){
		String dataString = "<div align=\"center\" style=\"margin:0;padding:0\">" 
				+ "<br><br>"
				+ "<img src=\"file:///android_asset/error/404.png\" align=\"middle\" style=\"max-width:300px;max-height:215px\"/>"
				+ "<br><br><br>"
				+ "<a href=\""+ url + "\"><img src=\"file:///android_asset/error/refresh.png\" align=\"middle\" /></a>"
//				+ "<img src=\"file:///android_asset/error/refresh.png\" align=\"middle\" onclick=\"location.reload(true)\"/>"
//				+ "<p>或尝试</p>"
//				+ "<img src=\"file:///android_asset/error/clear.png\" align=\"middle\" onclick=\"player.clearCache();\""
//				+ "<br>"
				+ "</div>";
				
		//baseUrl如果设为url，则页面很容易先闪 默认的错误页面，再显示自定义页面，会闪屏
		//historyUrl如果设置为null，多刷新几次自定义页面，页面会变为空白	
		view.loadDataWithBaseURL(null, dataString, null, "utf-8", url);
		view.computeScroll();
	}
	/**
	 * webview执行一次后退操作
	 * 
	 * @param webView
	 * @return
	 */
	public static boolean goBackHistory(WebView webView){
		WebBackForwardList list = webView.copyBackForwardList();
		list.getCurrentItem();
		int index = list.getCurrentIndex() - 1;
		String url = list.getCurrentItem().getUrl();
		//由于出错自定页面，会多几条出错url的历史记录。因此后退时，应该跳过
		if(url != null && !url.equalsIgnoreCase(Cfg.WEB_HOME)){
			while(index >= 0){
				if(url.equalsIgnoreCase(list.getItemAtIndex(index).getUrl())){
					index -- ;
				}else{
					break;
				}
			}
			//无路可退
			if(index < 0){
				return false;
			}
			webView.goBackOrForward(index - list.getCurrentIndex());
		} else {
			webView.goBack();
		}
		return true;
	}
	
	/**
	 * 预判是否能够正常加载url
	 * 
	 * @param url
	 * @return
	 */
	public static boolean canLoadUrl(String url){

//		HttpGet get = new HttpGet(url);
//		DefaultHttpClient client = new DefaultHttpClient();
//		HttpResponse rsp = null;
//		boolean canLoad = false;
//		try {
//			rsp = client.execute(get);
//			if(rsp != null && rsp.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
//				canLoad = true;
//			}
//		} catch (ClientProtocolException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			canLoad = false;
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			canLoad = false;
//		}
//		
//		return canLoad;
		
		return true;
	}
	
	/**
	 * 清除webview的缓存
	 * 
	 * @param ctx
	 */
//	public static void clearWebViewCache(final Context ctx){
		
//		File file = CacheManager.getCacheFileBaseDir();
//    	   if (file != null && file.exists() && file.isDirectory()) {
//    		   if(file.listFiles() != null){
//    			   for (File item : file.listFiles()) {
//        			   item.delete();
//        		   }
//    		   }
//    	    file.delete();
//    	   }
//    	   
//    	  ctx.deleteDatabase("webview.db");
//    	  ctx.deleteDatabase("webviewCache.db");
//	}
	
	public static void setMusicVolumeByStep(Context ctx, int direction) {
		AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
		am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
	}
	
	public static void setMusicVolumeDirect(Context ctx, int volumeValue) {
		AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(AudioManager.STREAM_MUSIC, volumeValue, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);	
	}
	
	public static int getMusicVolume(Context ctx) {
		AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
		return am.getStreamVolume(AudioManager.STREAM_MUSIC);
	}
	
	public static int getMusicMaxVolume(Context ctx) {
		AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
		return am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	}

	/**
	 * Delete finished download item from database for version before 1.17(include 1.17), because in these version
	 * finished download item saved in the database.
	 */
	private static void deleteFinishedDownloadItem() {
//		ArrayList<DownloadItem> list = DownloadList.getInstance().getList();
		
//		Log.e("MyLog", "list before size is " + list.size());
		
		DataBaseHelper.getInstance().deleteAllFinishedDownloadItem();
		DownloadList.Init();
		
//		Log.e("MyLog", "list after size is " + list.size());
	}
	
	
	public static void setColorTheme(View maskView){
		if(maskView != null){
			
			if(Cfg.mIsNightMode ){
				if(maskView.getVisibility() != View.VISIBLE){
					maskView.setVisibility(View.VISIBLE);
					maskView.invalidate();
				}
				
			}else {
				if(maskView.getVisibility() != View.GONE){
					maskView.setVisibility(View.GONE);
					maskView.invalidate();
				}
			}
		}
		
	}

	public static final int AWARD_TYPE_QQ = 0;
	public static final int AWARD_TYPE_SINA = 1;
	public static final int AWARD_TYPE_START_200 = 2;
	public static final int AWARD_FOLLOW_QQ = 3;
	public static final int AWARD_FOLLOW_SINA = 4;
	
	
	/**
	 * 某一类型是否执行过奖励
	 * 
	 * @param type
	 * @return
	 */
	public static boolean hasAwardBefore(int type){
		return ((Cfg.mAwardFalgs >> type) & 1) == 1;
	}
	/**
	 * 奖励完成
	 * 
	 * @param ctx
	 * @param type
	 */
	public static void onAwardDone(Context ctx, int type){
		Log.d("AccountConnect", "award flags = " + Cfg.mAwardFalgs);
		Cfg.mAwardFalgs |= 1 << type;
		Cfg.saveInt(ctx, Cfg.PREF_ACCOUNTS_BIND_AWARD_FLAGS, Cfg.mAwardFalgs);
		Log.d("AccountConnect", "award flags = " + Cfg.mAwardFalgs);
	}

	/**
	 * 获取一本书的价格
	 * 
	 * 
	 * @param book
	 * @return
	 */
//	public static int getDownloadPriceByBook(Book book){
//		int price = 0;
//		
//		if(book != null && book.mID != null && Cfg.mSoldBookPrice != null && Cfg.mSoldBookPrice.containsKey(book.mID)){
//			price = Cfg.mSoldBookPrice.get(book.mID);
//		}
//		if(price <= 0){
//			price = Cfg.DOWNLOAD_COMMON_PRICE;
//		}
//		
//		if(Cfg.mAdsHidden && price > 0){
//			return Integer.MIN_VALUE;
//		}
//		return price;
//		
//	}
	
	public static final int PLAY_PRICE_FRONT_FREE = -101;
	public static final int PLAY_PRICE_VIP = Integer.MIN_VALUE;
	public static final int PLAY_PRICE_PAID = -100;
	/**
	 * 获取一本书中某一集的播放价格
	 * @param book
	 * @param songIndex
	 * @return
	 */
//	public static int getPlayPriceByBook(Book book, int songIndex){
//		if(book == null || book.mID == null){
//			return 0;
//		}
//		int price[] = Cfg.mPlayChargedBookPrice.get(book.mID);
//		String playedChaps = Cfg.mPlayedChapters.get(book.mID);
//		if(price == null || price.length < 2){
//			return 0;
//		}
//		
//		boolean hasRead = false;
//		//1：存储的是播放的集数
//		//2: 存储的是一个跟集数一样长的01字符串，第几个index为1，则表明这一集已播放过
//		if(playedChaps != null && (playedChaps.contains("|" + songIndex + "|") || playedChaps.endsWith("|" + songIndex)
//				|| playedChaps.startsWith(songIndex + "|") || playedChaps.equals("" + songIndex))){
//			hasRead = true;
//		}
//		
//		if(price.length >= 2 && price[0] > 0){
//			if((songIndex) < price[1]){
//				return PLAY_PRICE_FRONT_FREE;
//			}
//			if(shouldShowAd() && !hasRead){
//				return price[0];
//			} else if (hasRead){
//				return PLAY_PRICE_PAID;
//			} else {
//				return PLAY_PRICE_VIP;
//			}
//			
//		}
//		return 0;
//	}
	
	/**
	 * 保存新的播放记录
	 * @param book
	 * @param SongIndex
	 */
//	public static void addPlayedChapter(String bookId, int SongIndex){
//		if(bookId == null || bookId.length() <= 0){
//			return ;
//		}
//		String playedChaps = Cfg.mPlayedChapters.get(bookId);
//		if(playedChaps == null || playedChaps.length() <= 0){
//			playedChaps = "" + SongIndex;
//			Cfg.mPlayedChapters.put(bookId, playedChaps);
//			return ;
//		}
//		if(!(playedChaps.contains("|" + SongIndex + "|") || playedChaps.endsWith("|" + SongIndex)
//				|| playedChaps.startsWith(SongIndex + "|") || playedChaps.equals("" + SongIndex))){
//			
//			playedChaps += "|" + SongIndex;
//		}
//		Cfg.mPlayedChapters.put(bookId, playedChaps);
//	}
	/**
	 * 初始化播放记录
	 * 
	 * @param bookId
	 * @param listString
	 */
//	public static void initPlayedChapter(String bookId, String listString){
//		if(bookId == null || listString == null || listString.length() <= 0){
//			return ;
//		}
////		String oldPlayed = Cfg.mPlayedChapters.get(bookId);
////		if(oldPlayed == null || oldPlayed.length() <= 0){
//		//无条件更新最新的数据
//		Cfg.mPlayedChapters.put(bookId, listString);
////		}
//		
//	}
	
	
	static Timer mKillProcessTimer;
	
	/**
	 * 倒计时杀死主进程
	 * 
	 * @param delay
	 */
	public static void killProcessByTime(int delay){
		cancelKillProcess();
		
		TimerTask timerTask = new TimerTask() {
			
			@Override
			public void run() {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		};
		mKillProcessTimer = new Timer();
		mKillProcessTimer.schedule(timerTask, delay);
		
	}
	/**
	 * 取消杀死主进程
	 */
	public static void cancelKillProcess(){
		if(mKillProcessTimer != null){
			mKillProcessTimer.cancel();
			mKillProcessTimer = null;
		}
	}
	
	/**
	 * 结束推送服务
	 * @param ctx
	 */
//	public static void killByPushService(Context ctx){
//		try{
////			ActivityManager myManager=(ActivityManager)Main.this.getSystemService(Context.ACTIVITY_SERVICE);
////			myManager.restartPackage(this.getPackageName());
////			myManager.restartPackage("com.bypush.ByPushService");
//			ctx.stopService(new Intent(ctx, com.baitui.ByPushService.class)); 
//		}catch (Exception e) {
//			// TODO: handle exception
//		}
//	}
	
	/**
	 * 进入飞行模式
	 * 
	 * @param ctx
	 */
	public static void gotoFlightMode(Context ctx){
		if(Cfg.mToFlightModeAfterFinish){
			try{
				Settings.System.putInt(ctx.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 1);  
				Intent localIntent1 = new Intent("android.intent.action.AIRPLANE_MODE").putExtra("state", true);  
				ctx.sendBroadcast(localIntent1);  
			} catch (Exception e) {
			}
			
		}
	}
	
}
