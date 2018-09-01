package tiger.unfamous.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import tiger.unfamous.Cfg;
import tiger.unfamous.download.DownloadItem;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

public class DataBaseHelper {
	private static MyLog log = new MyLog();
	private static DataBaseHelper instance;
	private final int mNewVersion;
	private final ReentrantLock mLock = new ReentrantLock(true);
	private SQLiteDatabase mDatabase = null;
	private File mDbFile = null;

	public static final String[] tableList = new String[] { "t_download_list" };

	public ReentrantLock getLock() {
		return this.mLock;
	}

	public SQLiteDatabase getDatabase() {
		return this.mDatabase;
	}

	public static boolean forceUpgrade = false;
	static {
		if (forceUpgrade) {
			DataBaseHelper.getInstance().getWritableDatabase();
			DataBaseHelper.getInstance().onUpgrade(Cfg.DB_VERSION,
					Cfg.DB_VERSION);
		}

	}

	public static DataBaseHelper getInstance() {
		if (instance == null)
			instance = new DataBaseHelper();
		return instance;
	}

	public DataBaseHelper() {
		File dbDir = new File(Environment.getDataDirectory().getAbsolutePath()
				+ "/data/tiger.unfamous/databases/");
		if (!dbDir.exists()) {
			dbDir.mkdirs();
		}
		mDbFile = new File(dbDir, Cfg.DB_NAME);
		this.mNewVersion = Cfg.DB_VERSION;
	}

	public synchronized SQLiteDatabase getWritableDatabase() {
		if (mDatabase != null && mDatabase.isOpen() && !mDatabase.isReadOnly()) {
			return mDatabase; // The database is already open for business
		}

		if (mDatabase != null) {
			if (mDatabase.isOpen())
				mDatabase.close();
			mDatabase = null;
		}

		if (mDbFile == null) {
			mDatabase = SQLiteDatabase.create(null);
		} else {
			mDatabase = SQLiteDatabase.openOrCreateDatabase(mDbFile, null);
		}
		int version = mDatabase.getVersion();
		if (version != mNewVersion) {
			mDatabase.beginTransaction();
			if (version == 0) {
				onCreate();
			} else {
				onUpgrade(version, mNewVersion);
			}
			mDatabase.setVersion(mNewVersion);
			mDatabase.setTransactionSuccessful();
			mDatabase.endTransaction();
		}
		return mDatabase;
	}

	public void onCreate() {
		StringBuffer sb = new StringBuffer();

		sb.append("CREATE TABLE t_download_list");
		sb.append(" (");
		sb.append("id  INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,");
		sb.append("url TEXT DEFAULT NULL, ");
		sb.append("status INTEGER NOT NULL, ");
		sb.append("dir_name TEXT DEFAULT NULL,");
		sb.append("unit_name TEXT DEFAULT NULL,");
		sb.append("file_type TEXT DEFAULT NULL,");
		sb.append("download_pos INTEGER DEFAULT 0,");
		sb.append("timestep INTEGER DEFAULT 0,");
		sb.append("filesize INTEGER DEFAULT 0,");
		sb.append("downloadsize INTEGER DEFAULT 0");
		sb.append(");");
		mDatabase.execSQL(sb.toString());
	}

	public void onUpgrade(int oldVersion, int newVersion) {
		mDatabase.execSQL("DROP TABLE IF EXISTS t_download_list;");
		onCreate();
	}

	public ArrayList<DownloadItem> queryDownloadList(Integer type) {
		ArrayList<DownloadItem> dList = null;
		Cursor cs = null;
		try {
			mLock.lock();
			getWritableDatabase();
			if (null != mDatabase) {
				String select = null;
				String[] selectArgs = null;
				if (type != null) {
					select = "status=?";
					selectArgs = new String[] { type.toString() };
				}

				cs = mDatabase.query("t_download_list", null, select,
						selectArgs, null, null, "id");
				if (cs.getCount() > 0) {
					dList = new ArrayList<DownloadItem>();
					while (cs.moveToNext()) {
						DownloadItem item = new DownloadItem(
								cs.getInt(cs.getColumnIndexOrThrow("id")),
								cs.getString(cs.getColumnIndexOrThrow("url")),
								cs.getInt(cs.getColumnIndexOrThrow("status")),
								cs.getString(cs
										.getColumnIndexOrThrow("dir_name")),
								cs.getString(cs
										.getColumnIndexOrThrow("unit_name")),
								cs.getString(cs
										.getColumnIndexOrThrow("file_type")),
								cs.getLong(cs
										.getColumnIndexOrThrow("download_pos")),
								cs
										.getLong(cs
												.getColumnIndexOrThrow("timestep")),
								cs
										.getLong(cs
												.getColumnIndexOrThrow("filesize")),
								cs.getLong(cs
										.getColumnIndexOrThrow("downloadsize")));
						dList.add(item);
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.e(e);
		} finally {
			if (null != cs)
				cs.close();
			if (null != mDatabase && mDatabase.isOpen())
				mDatabase.close();
			mLock.unlock();
		}
		return dList;
	}

	public long insertDownloadItem(DownloadItem item) {
		long itemid = -1;
		try {
			mLock.lock();
			getWritableDatabase();
			if (null != mDatabase) {
				ContentValues cv = new ContentValues();
				cv.put("status", item.getStatus());
				cv.put("url", item.getUrl());
				cv.put("dir_name", item.getDir_name());
				cv.put("unit_name", item.getUnit_name());
				cv.put("download_pos", item.getDownload_pos());
				cv.put("filesize", item.getFileSize());
				cv.put("downloadsize", item.getDownloadSize());
				cv.put("file_type", item.getFileType());
				itemid = mDatabase.insert("t_download_list", null, cv);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.e(e);
		} finally {
			if (null != mDatabase && mDatabase.isOpen())
				mDatabase.close();
			mLock.unlock();
		}
		return itemid;
	}

	public int updateDownloadItem(DownloadItem item) {
		// log.d("updateDownloadItem Charpter_name: " + item.getUnit_name());
		int iRow = 0;
		try {
			mLock.lock();
			getWritableDatabase();
			if (null != mDatabase) {
				ContentValues cv = new ContentValues();
				cv.put("status", item.getStatus());
				cv.put("url", item.getUrl());
				cv.put("dir_name", item.getDir_name());
				cv.put("unit_name", item.getUnit_name());
				cv.put("filesize", item.getFileSize());
				cv.put("download_pos", item.getDownload_pos());
				cv.put("downloadsize", item.getDownloadSize());
				cv.put("file_type", item.getFileType());
				final String whereClause = "id = " + item.getItemId();
				iRow = mDatabase.update("t_download_list", cv, whereClause,
						null);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.e(e);
		} finally {
			if (null != mDatabase && mDatabase.isOpen())
				mDatabase.close();
			mLock.unlock();
		}
		return iRow;
	}

	public void deleteDownloadItem(long id) {
		try {
			mLock.lock();
			getWritableDatabase();
			if (null != mDatabase) {
				final String whereClause = "id = " + id;
				mDatabase.delete("t_download_list", whereClause, null);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.e(e);
		} finally {
			if (null != mDatabase && mDatabase.isOpen())
				mDatabase.close();
			mLock.unlock();
		}
	}
	
	// delete all finished download item from database
	public void deleteAllFinishedDownloadItem() {
		try {
			mLock.lock();
			getWritableDatabase();
			if (null != mDatabase) {
				final String whereClause = "status = " + DownloadItem.FINISHED;
				mDatabase.delete("t_download_list", whereClause, null);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.e(e);
		} finally {
			if (null != mDatabase && mDatabase.isOpen())
				mDatabase.close();
			mLock.unlock();
		}
	}

	public DownloadItem getDownloadItem(long id) {
		DownloadItem item = null;
		Cursor cs = null;
		try {
			mLock.lock();
			getWritableDatabase();
			if (null != mDatabase) {
				String select = "id=?";
				String[] selectArgs = new String[] { Long.toString(id) };
				cs = mDatabase.query("t_download_list", null, select,
						selectArgs, null, null, null);
				if (cs.getCount() > 0) {
					cs.moveToFirst();
					item = new DownloadItem(
							cs.getInt(cs.getColumnIndexOrThrow("id")),
							cs.getString(cs.getColumnIndexOrThrow("url")),
							cs.getInt(cs.getColumnIndexOrThrow("status")),
							cs.getString(cs.getColumnIndexOrThrow("dir_name")),
							cs.getString(cs.getColumnIndexOrThrow("unit_name")),
							cs.getString(cs.getColumnIndexOrThrow("file_type")),
							cs
									.getLong(cs
											.getColumnIndexOrThrow("download_pos")),
							cs.getLong(cs.getColumnIndexOrThrow("timestep")),
							cs.getLong(cs.getColumnIndexOrThrow("filesize")),
							cs
									.getLong(cs
											.getColumnIndexOrThrow("downloadsize")));
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.e(e);
		} finally {
			if (null != cs)
				cs.close();
			if (null != mDatabase && mDatabase.isOpen())
				mDatabase.close();
			mLock.unlock();
		}
		return item;
	}

	public DownloadItem getDownloadItemByUnitUrl(String fileurl) {
		DownloadItem item = null;
		Cursor cs = null;
		try {
			mLock.lock();
			getWritableDatabase();
			if (null != mDatabase) {
				String select = "url=?";
				String[] selectArgs = new String[] { fileurl };
				cs = mDatabase.query("t_download_list", null, select,
						selectArgs, null, null, null);
				if (cs.getCount() > 0) {
					cs.moveToFirst();
					item = new DownloadItem(
							cs.getInt(cs.getColumnIndexOrThrow("id")),
							cs.getString(cs.getColumnIndexOrThrow("url")),
							cs.getInt(cs.getColumnIndexOrThrow("status")),
							cs.getString(cs.getColumnIndexOrThrow("dir_name")),
							cs.getString(cs.getColumnIndexOrThrow("unit_name")),
							cs.getString(cs.getColumnIndexOrThrow("file_type")),
							cs
									.getLong(cs
											.getColumnIndexOrThrow("download_pos")),
							cs.getLong(cs.getColumnIndexOrThrow("timestep")),
							cs.getLong(cs.getColumnIndexOrThrow("filesize")),
							cs
									.getLong(cs
											.getColumnIndexOrThrow("downloadsize")));
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.e(e);
		} finally {
			if (null != cs)
				cs.close();
			if (null != mDatabase && mDatabase.isOpen())
				mDatabase.close();
			mLock.unlock();
		}
		return item;
	}

	public ArrayList<DownloadItem> queryAllDownloadItem() {
		ArrayList<DownloadItem> list = null;
		Cursor cs = null;
		try {
			mLock.lock();
			getWritableDatabase();
			if (null != mDatabase) {
				String select = null;
				String[] selectArgs = null;
				cs = mDatabase.query("t_download_list", null, select,
						selectArgs, null, null, null);
				if (cs.getCount() > 0) {
					list = new ArrayList<DownloadItem>();
					while (cs.moveToNext()) {
						DownloadItem item = new DownloadItem(
								cs.getInt(cs.getColumnIndexOrThrow("id")),
								cs.getString(cs.getColumnIndexOrThrow("url")),
								cs.getInt(cs.getColumnIndexOrThrow("status")),
								cs.getString(cs
										.getColumnIndexOrThrow("dir_name")),
								cs.getString(cs
										.getColumnIndexOrThrow("unit_name")),
								cs.getString(cs
										.getColumnIndexOrThrow("file_type")),
								cs.getLong(cs
										.getColumnIndexOrThrow("download_pos")),
								cs
										.getLong(cs
												.getColumnIndexOrThrow("timestep")),
								cs
										.getLong(cs
												.getColumnIndexOrThrow("filesize")),
								cs.getLong(cs
										.getColumnIndexOrThrow("downloadsize")));
						list.add(item);
					}
				}
			}
		} catch (Exception e) {
			log.e(e);
		} finally {
			if (null != cs)
				cs.close();
			if (null != mDatabase && mDatabase.isOpen())
				mDatabase.close();
			mLock.unlock();
		}
		return list;
	}
	
	public void close(){
		if(mDatabase != null){
			mDatabase.close();
		}
	}

}
