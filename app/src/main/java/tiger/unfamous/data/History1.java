package tiger.unfamous.data;

import java.io.Serializable;

//已废弃，仅用于老版本用户升级
public class History1 implements Serializable {
	private static final long serialVersionUID = 8796027648610132463L;

	public boolean mOnline;
	public Dir1 mDir;
	public int mPage;
	public String mLcDirPath;
	public String mLcDirName;
	public String mSongName;
	public long mPosition;

	public History1(Dir1 dir, int page, String sName, long pos) {
		mOnline = true;
		mDir = dir;
		mPage = page;
		mSongName = sName;
		mPosition = pos;
	}

	public History1(String lcDirPath, String lcDirName, String sName, long pos) {
		mOnline = false;
		mLcDirPath = lcDirPath;
		mLcDirName = lcDirName;
		mSongName = sName;
		mPosition = pos;
	}
}
