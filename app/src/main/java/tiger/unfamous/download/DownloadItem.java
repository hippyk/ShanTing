package tiger.unfamous.download;

import tiger.unfamous.utils.Utils;

public class DownloadItem implements Comparable<Object> {

	/*
	 * Download Item Table in DB.
	 * 
	 * id | status | url | timestep | filename | showname | filesize |
	 * downloadsize | type | contentid | groupcode| attach id
	 */

	// status
	public final static int RUNNING = 1;
	public final static int WAITING_HIGH = 2;
	public final static int WAITING_LOW = 3;
	public final static int PAUSED = 4;
	public final static int FINISHED = 5;

	// type for TINGTONE, dcf file use its pid.
	public static final int ITEM_TYPE_ANDROID = 0;
	public static final int ITEM_TYPE_WIDGET = 1;

	// type for download or upload
	public static final int UDTYPE_DOWNLOADING = 0;
	public static final int UDTYPE_UPLOADING = 1;

	// type for status of install
	public static final int UNINSTALL = 1;
	public static final int INSTALLED = 2;
	
	// type for download file type
	/**
	 * Audio file, for example mp3 file.
	 */
	public static final String AUDIO_FILE = "0";

	/**
	 * Install package file, for example apk file.
	 */
	public static final String INSTALL_PACKAGE_FILE = "1";

	/**
	 * The id of this item in db.
	 */
	private long m_itemId;

	/**
	 * If the download status of the item.
	 */
	private int m_status;

	private String dir_name;

	private String unit_name;

	private long download_pos;

	private String url;

	/**
	 * Time step for status modify time.
	 */
	private long m_timestep;

	/**
	 * The total file size.
	 */
	private long m_fileSize;

	private String fileType;
	/**
	 * Current file size that has been downloaded from server.
	 */
	private long m_downloadSize;

	private String part_name;

	public String getPartName() {
		return part_name;
	}

	public void setPartName(String name) {
		this.part_name = name;
	}

	public DownloadItem() {

	}

	public DownloadItem(long id, String url, int status, String dir_name,
			String unit_name, String fileType, long download_pos,
			long timeStep, long fileSize, long downloadSize) {

		this.m_itemId = id;
		this.url = url;
		this.m_status = status;
		this.dir_name = dir_name;
		this.unit_name = unit_name;
		this.download_pos = download_pos;
		this.m_timestep = timeStep;
		this.m_fileSize = fileSize;
		this.m_downloadSize = downloadSize;
		this.fileType = fileType;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public long getItemId() {
		return m_itemId;
	}

	public void setItemId(long id) {
		m_itemId = id;
	}

	public int getStatus() {
		return m_status;
	}

	public void setStatus(int status) {
		m_status = status;
	}

	public long getTimeStep() {
		return m_timestep;
	}

	public void setTimeStep(long time) {
		m_timestep = time;
	}
/**
 * 在还没有下载前,fileSize 是从列表中获取到的一个大概的数字
 * 下载后，会从http头部获取真正的大小，覆盖
 * @return
 */
	public long getFileSize() {
		return m_fileSize;
	}

	public void setFileSize(long size) {
		m_fileSize = size;
	}

	public long getDownloadSize() {
		return m_downloadSize;
	}

	public void setDownloadSize(long size) {
		m_downloadSize = size;
	}

	public String getDir_name() {
		return dir_name;
	}

	public void setDir_name(String dirName) {
		dir_name = dirName;
	}

	public String getUnit_name() {
		return unit_name;
	}

	public void setUnit_name(String unitName) {
		unit_name = unitName;
	}

	public long getDownload_pos() {
		return download_pos;
	}

	public void setDownload_pos(long download_pos) {
		this.download_pos = download_pos;
	}

	// For sort.
	public int compareTo(Object obj) {

		if (obj == null) {
			return -1;
		}

		DownloadItem other = (DownloadItem) obj;

		int oStatus = other.getStatus();
		long oTime = other.getTimeStep();

		if (this.getStatus() < oStatus) {
			return -1;
		}

		if (this.getStatus() > oStatus) {
			return 1;
		}

		if (this.getStatus() == WAITING_LOW) {

			if (this.getTimeStep() < oTime) {
				return -1;
			}

			if (this.getTimeStep() > oTime) {
				return 1;
			}
		} else {
			if (this.getTimeStep() < oTime) {
				return 1;
			}

			if (this.getTimeStep() > oTime) {
				return -1;
			}
		}

		return 0;

	}

	public String getExtention() {
		// Log.e("getExtention:", Utils.getExtention(url));
		return Utils.getExtention(url);
	}
}
