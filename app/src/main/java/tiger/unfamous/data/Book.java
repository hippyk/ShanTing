package tiger.unfamous.data;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import tiger.unfamous.Cfg;
import tiger.unfamous.Cfg.UpdateState;
import tiger.unfamous.DN;
import tiger.unfamous.utils.MyHttpClient;
import tiger.unfamous.utils.Utils;


public class Book implements Serializable, Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5545710765228307088L;
	
	public boolean mOnline;
	public String mTitle;
	
	//play info
	public String mSongName;
	public long mPosition;
	//默认均为第一页
	public int mCurPlayPage = 1;
	
	
	//<local>
	public String mLcDirPath;
	
	public Book(String dirPath, String songName) {
		mOnline = false;
		mLcDirPath = dirPath;
		mTitle = new File(dirPath).getName();
//		mLcDirName = dirName;
		mSongName = songName;
	}
	//</local>
	
	
	//online
	public String mID;
	public String mPicAddress;
	public String mWriter;
	public String mNarrator;

	public UpdateState mUpdateState;
	public String mIntro;
	public boolean mAutoPath;
	public int mSongCount;
	public int mPageCount = 1;
	public String mCommonTitle;
	public String mCommonAddr;
	/**
	 * 多保存几个用户最近更新过的页面，有用
	 */
	public HashMap<Integer, String[]> mSongStringsMap = new HashMap<Integer, String[]>();
	/**
	 * 歌曲的比特率
	 */
	public int mBitrate;
	
	/**
	 * 歌曲的大小列表
	 */
	public HashMap<Integer, int[]> mSongSizeMap = new HashMap<Integer, int[]>();
	
	public String[] mSongStrings; 
//	ArrayList<Song> mSongs = new ArrayList<Song>();  //保存上面的就可以，否则重复了
	
	public boolean equals (Book b) {
		if (b == null) {
			return false;
		} else if (mOnline && b.mOnline) {
//			if (mAutoPath && b.mAutoPath) {
//				return mCommonAddr.equals(b.mCommonAddr);
//			} else if (!mAutoPath && !b.mAutoPath) {
//				return mSongStrings[0].equals(b.mSongStrings[0]);
//			} else {
//				return false;
//			}
			return mID.equalsIgnoreCase(b.mID) && mAutoPath == b.mAutoPath;
			
		} else if (!mOnline && !b.mOnline) {
			return mLcDirPath.equals(b.mLcDirPath);
		} else {
			return false;
		}
	}
	
	
	void setMemberState(String state) {
		if (state.equals(UpdateState.IN_SERIES.name())) {
			mUpdateState = UpdateState.IN_SERIES;
		} else if (state.equals(UpdateState.DISCONTINUE.name())){
			mUpdateState = UpdateState.DISCONTINUE;
		} else {
			mUpdateState = UpdateState.END;
		}
//		switch (state) {
//		case 1:	
//			
//			break;
//		case -1:
//			mUpdateState = UpdateState.DISCONTINUE;
//			break;
//		default:
//			mUpdateState = UpdateState.END;
//			break;
//		}
	}
	public UpdateState getMemberState()
	{
		return mUpdateState;
	}
	public Book(String id, String state, String title, int count, String commonTitle, String commonAddr, int bitrate , int[] size) {
		mID = id;
		setMemberState(state);
		mOnline = true;
		mAutoPath = true;
		mTitle = title;
		mSongCount = count;
		mCommonTitle = commonTitle;
		mCommonAddr = commonAddr;
		mBitrate = bitrate;
		if(size != null){
			mSongSizeMap.put(1, size);
		}
		
		mPageCount = mSongCount / Cfg.SONGS_PER_PAGE;
		if (mSongCount % Cfg.SONGS_PER_PAGE > 0) {
			mPageCount++;
		}
	}
	
	public Book(String id, String state, String title, int count, String[] chapStrings, int bitrate, int[] songSize) {
		mID = id;
		setMemberState(state);	
		mOnline = true;
		mAutoPath = false;
		mTitle = title;
		mSongCount = count;
		mSongStrings = chapStrings;
		mCurPlayPage = 1;
		mPageCount = (mSongCount - 1) / Cfg.SONGS_PER_PAGE + 1;
		Utils.convertSongsToMap(this);
		
		mBitrate = bitrate;
		if(mSongSizeMap != null && songSize != null){
			mSongSizeMap.put(1, songSize);
		}
	}
	
	/**
	 * 
	 * @param page 初始值从1开始，Cfg.FIRST_PAGE
	 * @return
	 */
	public void getSongs(final int page, final GetMusicPathListener listener) {
		//already in getting paths
//		if(mInGettingPaths){
//			return ;
//		}
		if(mSongSizeMap == null){
			mSongSizeMap = new HashMap<Integer, int[]>();
		}
		if(mSongStringsMap == null){
			mSongStringsMap = new HashMap<Integer, String[]>();
		}
		//
		if(mAutoPath && this.mSongSizeMap.containsKey(page)){
//			mCurPlayPage = page;
			ArrayList<Song> songs = splitMusicPaths(page);
			listener.onGetMusicPath(0, page, songs);
			
			return ;
		}
		
		//already loaded paths before
		if( mSongStringsMap.containsKey(page) && this.mSongSizeMap.containsKey(page)){
//			mCurPlayPage = page;
			mSongStrings = mSongStringsMap.get(page);
			ArrayList<Song> songs = splitMusicPaths(page);
			if(songs != null && !songs.isEmpty()){
				listener.onGetMusicPath(0, page, songs);
				return ;	
			}
			
		}
		
		new Thread(new Runnable() {		
			@Override
			public void run() {
//				Looper.prepare();
				final int resultCode = requestMusicPaths(page);
				
				if(!listener.isCancelled()){
					final ArrayList<Song> songs = splitMusicPaths(page);
					Handler resultHandler = new Handler(Looper.getMainLooper());
					resultHandler.post(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							listener.onGetMusicPath(resultCode, page, songs);
						}
					});
					
				}
//				Looper.loop();
			}
		},"book.getPath").start();
		
			
	}
	
//	boolean mInGettingPaths = false;
	public abstract class GetMusicPathListener{
		boolean mCancelled = false;
		public void cancel(){
			mCancelled = true;
		}
		boolean isCancelled(){
			return mCancelled;
		}
		public abstract void onGetMusicPath(int resultCode, int curPage, ArrayList<Song> songs);
	}
	
	public ArrayList<Song> getCurPageSongs(){
		return splitMusicPaths(mCurPlayPage);
	}
	
	private ArrayList<Song> splitMusicPaths(int page){
		ArrayList<Song> songs  = new ArrayList<Song>();
		int sizeArray[] = null;
		if(mSongSizeMap != null){
			sizeArray = mSongSizeMap.get(page);
		}else{
			mSongSizeMap = new HashMap<Integer, int[]>();;
		}
		int size;
		String duration;
		if (mAutoPath) {
//			songs
			String songName, songAddr;
			int countOfLastPage = mSongCount % Cfg.SONGS_PER_PAGE;
			int end = page * Cfg.SONGS_PER_PAGE;
			if (page == mPageCount && countOfLastPage > 0) {
				end = (page - 1) * Cfg.SONGS_PER_PAGE + countOfLastPage;
			}
			for (int i = 1 + (page - 1) * Cfg.SONGS_PER_PAGE, j = 0; i <= end; i++,j++) {
				songName = mCommonTitle + i;
				songAddr = mCommonAddr + "/" + i + ".mp3";
				if(sizeArray != null && j < sizeArray.length){
					size = sizeArray[j];
					duration = convertSizeToTime(size);
				}else {
					size = 0;
					duration = null;
				}
				songs.add(new Song(songName, "", songAddr,size, duration));
			}
		} else {
//			songs = mSongs;
			if(mSongStringsMap.containsKey(page)){
				String song[] = mSongStringsMap.get(page);
				for (int i = 0; i < song.length; i++) {
					String line = song[i];
					int index = line.indexOf("|");
					if (index == -1) {
						continue;
					}
					String songName, songAddr;
					songName = line.substring(0, index);
					songAddr = line.substring(index + 1);
					if(sizeArray != null && i < sizeArray.length){
						size = sizeArray[i];
						duration = convertSizeToTime(size);
					}else {
						size = 0;
						duration = null;
					}
					
					songs.add(new Song(songName, "", songAddr, size, duration));
				}
			}
		
		}
		songs.trimToSize();
		return songs;
	}
	
	/**
	 * 	请求获得第page页的播放地址列表
	 * @param page
	 * @return
	 */
	public int requestMusicPaths(int page){
		
//		mCurPlayPage = page;
//		mInGettingPaths = true;
		int resultCode = 0;
		
		String urlWithID = Utils.setUrlParam(Cfg.GET_MUSIC_PATHS_URL, DN.BOOK_ID, mID);
		String url= Utils.setUrlParam(urlWithID, DN.URL_PARAM_PAGE, "" + page);
		InputStream in = MyHttpClient.getInstance().sendHttpGet(url, null);
		
		if(in == null){
			return 3;
		}
		try {
			JsonReader reader;
			reader = new JsonReader(new InputStreamReader(in, "GBK"));
			
			String paths = null;
			String sizes = null;
			reader.beginObject();
		    while (reader.hasNext()) {
		    	String name = reader.nextName();
		    	if (name.equals(DN.JSON_PARAM_MUSIC_PATH)) {
		    		paths = reader.nextString();
		    	} else if(name.equals(DN.JSON_PARAM_SIZE)){
		    		sizes = reader.nextString();
		    	} else {
		            reader.skipValue();
		        }
		    }
		    reader.endObject();
			
		    //autoPath获取到的地址可以不用保存，用不到，浪费内存
		    if (paths != null && !mAutoPath){
		    	if(paths.contains(Cfg.RET_SYMBOL_DOS)) {
		    		mSongStrings = paths.split(Cfg.RET_SYMBOL_DOS);
		    	} else {
		    		mSongStrings = paths.split(Cfg.RET_SYMBOL_UNIX);
		    	}
		    	
		    	mSongStringsMap.put(page, mSongStrings);
		    }	
		    
		    if(sizes != null){
	    		sizes = sizes.replace("\n|", "|");
	    		String sizeArray[] = sizes.split("\\|");
	    		
	    		if(sizeArray != null){
	    			int sizeIntArray[] = new int[sizeArray.length];
	    			for(int i = 0; i < sizeArray.length; i++){
	    				sizeIntArray[i] = Utils.parseInt(sizeArray[i], 0);
	    			}
	    			if(mSongSizeMap == null){
	    				mSongSizeMap = new HashMap<Integer, int[]>();;
	    			}
	    			mSongSizeMap.put(page, sizeIntArray);
	    		}
	    	}

		    
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			resultCode = 1;
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			resultCode = 1;
		}

//		if(mCurPlayPage != page){
//			resultCode = 2;
//		}
//		mInGettingPaths = false;	
		return resultCode;

	}
	
	private String convertSizeToTime(int size){
		if(mBitrate <= 0 || mBitrate > 128){
			mBitrate = 32;
		}
		
		//exclude IDV1 info
		size -= 128;
		
		if(size <= 0){
			return null;
		}
		int seconds = size * 8 / mBitrate /1000;
		int minutes = seconds / 60;
		seconds = (seconds % 60)/6;
		
		//四舍五入
//		seconds = seconds / 6 + (seconds % 6 >= 3 ? 1 : 0);
		if(seconds == 0 && minutes == 0){
			seconds = 1;
		}
//		seconds %= 60;
//		return (minutes < 10 ? "0" : "") + minutes + (seconds < 10 ? ":0" : ":") + seconds;
		return " [" + minutes + "." + seconds + "\']";
	}
	
	@Override
	public Object clone(){
		// TODO Auto-generated method stub
		Object o = null;
		try{
			o = super.clone();
		}catch (CloneNotSupportedException e) {
			// TODO: handle exception
		}
		return o;
		
	}
	
}
