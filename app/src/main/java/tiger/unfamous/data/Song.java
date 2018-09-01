package tiger.unfamous.data;

import java.io.File;
import tiger.unfamous.Cfg;
import tiger.unfamous.utils.Utils;

public class Song {
	/**
	 * 
	 */
//	private static final long serialVersionUID = -7786609015810140530L;
	private String name;
	private String singer;
	private String netAddress;
	public String mDuration;
	public int	   mSize;
	private boolean isMp3;

	public boolean isMp3() {
		return isMp3;
	}

	public Song(String name, String singer, String netAddress, int size, String duration) {
		if (name == null)
			name = "";
		if (singer == null)
			singer = "";
		if (netAddress == null)
			netAddress = "";

		this.name = name;
		this.singer = singer;
		this.netAddress = Utils.encodeURL(netAddress);
		this.mSize = size;
		this.mDuration = duration;
		isMp3 = !netAddress.contains(".wma");
	}

	public String getName() {
		return name;
	}

	public String getSinger() {
		return singer;
	}

	public String getnetAddress() {
		return netAddress;
	}

	public boolean existLocally(String dir_name) {
		File file = new File(Cfg.DOWNLOAD_DIR + "/" + dir_name + "/" + name
				+ (isMp3 ? ".mp3" : ".wma"));
		// Log.e("song", file.getAbsolutePath() + "_" + file.exists());
		return file.exists();
	}
	
	public static String getSizeInMB(long size){
		int sizeKB = (int) (size / 1024);
		if(sizeKB <= 0){
			return "";
		}
		//不足0.1M显示为K
		if(sizeKB < 103){
			return "[" + sizeKB + "K]";
		}
		return "[" + sizeKB / 1024 + "." + (sizeKB % 1024) * 10 / 1024 + "M]";
	}
}
