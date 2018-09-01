package tiger.unfamous.download;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import com.umeng.analytics.MobclickAgent;

import android.os.Environment;
import android.util.Log;
import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.utils.DataBaseHelper;
import tiger.unfamous.utils.MyHttpClient;
import tiger.unfamous.utils.MyLog;
import tiger.unfamous.utils.Utils;

public class DownloadTask extends MyTask<DownloadItem> implements Runnable {

	private static final MyLog logger = new MyLog();
	public static final String TAG_SUFFIX = ".tep";
	public static final int PACKAGE_SIZE = 1024 * 1024 + 1024 * 512;
	private static final int TRY_TIMES = 3; 
	public static final String TAG_RANGE = "range";

	private String mHost;
	private DownloadItem m_item;
	private MyHttpClient httpClient;
	// private DataBaseHelper databaseHelper;
	private long m_downloadSize;
	private int statusCode;
	private boolean m_downloadWithWifi;
	private boolean m_isResourceChanged;
	private boolean m_isNotWifiProtectDetect;
	private boolean m_isSDEjectDetect;
	private int m_downloadResult;
	public static final int DOWNLOAD_RESULT_INVALID_VALUE = -1;
	public static final int DOWNLOAD_RESULT_SUCCESSFUL = 0;
	public static final int DOWNLOAD_RESULT_CANCEL = 1;
	public static final int DOWNLOAD_RESULT_WIFI_DISCONNECTED = 2;
	public static final int DOWNLOAD_RESULT_SDCARD_NOT_MOUNTED = 3;
	public static final int DOWNLOAD_RESULT_SDCARD_NO_SPACE = 4;
	public static final int DOWNLOAD_RESULT_NO_INTERNET = 5;
	public static final int DOWNLOAD_RESULT_FAILED = 6;
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DownloadTask(DownloadItem item, MyTaskListener listener) {
		super(listener);
		m_item = item;
		// m_downloadSize = 0;
		httpClient = MyHttpClient.getInstance();
		m_downloadWithWifi = false;
		m_isResourceChanged = false;
		m_isNotWifiProtectDetect = false;
		m_isSDEjectDetect = false;
		m_downloadResult = DOWNLOAD_RESULT_INVALID_VALUE;
		// databaseHelper = DataBaseHelper.getInstance();
	}

//	public void setDownloadListener(MyTaskListener listener) {
//		m_listener = listener;
//	}

	public DownloadItem getDownloadItem() {
		return m_item;
	}

	public boolean isDownloadWithWifi() {
		return m_downloadWithWifi;
	}
	
	public void setDwonloadWithWifi(boolean downloadWithWifi) {
		m_downloadWithWifi = downloadWithWifi;
	}
	
	public boolean isResourceChanged() {
		return m_isResourceChanged;
	}
	
	public void setNotWifiProtectDetect(boolean notWifiProtectDetect) {
		m_isNotWifiProtectDetect = notWifiProtectDetect;
	}
	
	public boolean isNotWifiProtectDetect() {
		return m_isNotWifiProtectDetect;
	}
	
	public void setSDEjectDetect(boolean sdEjectDetect) {
		m_isSDEjectDetect = sdEjectDetect;
	}
	
	public boolean isSDEjectDetect() {
		return m_isSDEjectDetect;
	}
	
	public int getDownloadResult() {
		return m_downloadResult;
	}
	
	@Override
	public DownloadItem get() throws Exception {
		logger.v("get() ---> Enter");
		download();
		logger.v("get() ---> Exit");
		return m_item;
	}

	private void download() throws Exception {
		String url = m_item.getUrl();
		mHost = Utils.getUrlHost(url);
		HttpGet req = new HttpGet(url);
		InputStream is = null;
		RandomAccessFile randFile = null;
		byte[] buf = null;
		try {

	//		logger.i("client.sendHttpGet face_url: " + url);
			// url get请求
			
			String downloadDir = null;
			if (m_item.getFileType().equals(DownloadItem.AUDIO_FILE)) {
				downloadDir = Cfg.DOWNLOAD_DIR;
			} else if (m_item.getFileType().equals(DownloadItem.INSTALL_PACKAGE_FILE)) {
				downloadDir = Cfg.mInnerRootPath;
			}

			logger
					.i("folder is : " + downloadDir + "/"
							+ m_item.getDir_name());
			// 建立下载目录
			File folder = new File(downloadDir + "/" + m_item.getDir_name());
			// System.out.println("DownloadTask--------->"+folder);
			if (!folder.exists()) {
				folder.mkdirs();
			}
			// 准备文件，如果不存在则创建新文件
			String tempfilename = m_item.getUnit_name() + TAG_SUFFIX;
			m_item.setPartName(tempfilename);
			File file = new File(downloadDir + "/" + m_item.getDir_name()
					+ "/" + tempfilename);
			if (!file.exists()) {
				randFile = new RandomAccessFile(file, "rw");
				randFile.close();
				m_item.setDownloadSize(0);
			}
			// 文件下载实际大小
			if (m_item.getDownloadSize() != file.length()) {
	
				m_item.setDownloadSize(file.length());
			}
			// open local file
			m_downloadSize = m_item.getDownloadSize();
	
			String range = "bytes=" + m_downloadSize + "-";
			req.setHeader(TAG_RANGE, range);
			logger.i("m_downloadSize is " +m_downloadSize);
			HttpResponse res = httpClient.execute(req);
			if (res == null) {
				throw new Exception(DN.HTTP_CONNECT_EXCEPTION);
			}
			statusCode = res.getStatusLine().getStatusCode();
			Log.v("Code", "" + statusCode);
			if (statusCode == HttpStatus.SC_OK
					|| statusCode == HttpStatus.SC_PARTIAL_CONTENT) {
				// Header[] hs = res.getHeaders("Content-Type");
				// if(hs != null && hs.length > 0)
				// {
				// String vv = hs[0].getValue();
				// if (!vv.contains("audio")) {
				// Exception ex = new Exception();
				// throw ex;
				// }
				// }
				
				Header[] hs = res.getHeaders("Content-Range");
				String fz = null;
				if (hs != null && hs.length > 0) {
					String vv = hs[0].getValue();
					String index = "/";
					int j = vv.lastIndexOf(index);
					int start = j + index.length();
					fz = vv.substring(start);
					m_item.setFileSize(Long.parseLong(fz));
					logger.i("Content-Range is " + vv);
				} else {
					hs = res.getHeaders("Content-Length");
					if (hs != null && hs.length > 0) {
						fz = hs[0].getValue();
						m_item.setFileSize(Long.parseLong(fz));
					}
				}
				logger.v("HttpResponse is " + res.getHeaders("Content-Length")[0].getValue());
				logger.v("getFileSize of database is " + DataBaseHelper.getInstance().getDownloadItem(m_item.getItemId()).getFileSize());
				logger.v("getFileSize of downloadItem is " + m_item.getFileSize());
				
				if (DataBaseHelper.getInstance().getDownloadItem(m_item.getItemId()).getFileSize() == 0) {
					DataBaseHelper.getInstance().updateDownloadItem(m_item);
				} else if (DataBaseHelper.getInstance().getDownloadItem(m_item.getItemId()).getFileSize()
						!= m_item.getFileSize()) {
					file.delete();
					m_isResourceChanged = true;
					m_item.setFileSize(0);
					DataBaseHelper.getInstance().updateDownloadItem(m_item);
					throw new Exception(DN.RESOURCE_CHANGED_EXCEPTION);
				}
				
				long fileSize = m_item.getFileSize();
				if (fileSize <= 0 || m_item.getUnit_name() == null) {
					throw new Exception(DN.HTTP_RESPONSE_FILE_SIZE_EXCEPTION);
				}

				if (m_downloadSize != file.length()) {
					m_downloadSize = file.length();
				}
				
				is = res.getEntity().getContent();
				buf = new byte[50 * 1024];
				randFile = new RandomAccessFile(file, "rw");
				
				if (fileSize > m_downloadSize && !m_isCanceled && !m_isRemoved) {
					if (is == null) {
						if (m_item.getDownloadSize() != m_item.getFileSize()) {
							throw new Exception(DN.HTTP_NO_RESPONSE_EXCEPTION);
						} else {
							m_isComplete = true;
							return;
						}
					}
					
					int size = 0;
					while (true) {
						if (m_isCanceled || m_isRemoved) {
							break;
						}
		
						try {
							size = is.read(buf, 0, buf.length);
						} catch (Exception e) {
							e.printStackTrace();
							throw new Exception(DN.HTTP_READ_CONTENT_EXCEPTION);
						}
						
						if (size == -1) {
							break;
						}
				
						randFile.seek(m_downloadSize);
						randFile.write(buf, 0, size);
						m_downloadSize += size;
						m_item.setDownloadSize(m_downloadSize);
						
						if (m_downloadSize > fileSize) {
							file.delete();
							m_isResourceChanged = true;
							m_item.setFileSize(0);
							DataBaseHelper.getInstance().updateDownloadItem(m_item);
							throw new Exception(DN.RESOURCE_CHANGED_EXCEPTION);
						}
						
						listener.taskProgress(this, m_downloadSize, fileSize);
					}

					m_item.setDownloadSize(m_downloadSize);
					if (m_downloadSize == fileSize) {
						m_isComplete = true;
					}
				} else if (fileSize <= m_downloadSize) {
					file.delete();
					m_isResourceChanged = true;
					m_item.setFileSize(0);
					DataBaseHelper.getInstance().updateDownloadItem(m_item);
					throw new Exception(DN.RESOURCE_CHANGED_EXCEPTION);
				}
			} else if (statusCode == HttpStatus.SC_NOT_FOUND) {
				Exception ex = new Exception(DN.HTTP_NOT_FOUND_EXCEPTION);
				throw ex;
			} else if (statusCode == HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE) {
				file.delete();
				m_isResourceChanged = true;
				m_item.setFileSize(0);
				DataBaseHelper.getInstance().updateDownloadItem(m_item);
				throw new Exception(DN.RESOURCE_CHANGED_EXCEPTION);				
			} else {
				Exception ex = new Exception(DN.HTTP_UNKNOWN_EXCEPTION);
				throw ex;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (!DN.HTTP_READ_CONTENT_EXCEPTION.equals(e.getMessage())) {
				throw e;
			}
			
			if(Cfg.mNotWifiProtect && m_downloadWithWifi) {
				boolean hasWifi = true;
				for (int i=0; i<50; i++) {
					if (!listener.isWifiConnected()) {
						hasWifi = false;
						break;
					}

					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				
				if (hasWifi) {
					throw e;
				}
			} else {
				throw e;
			}
		} finally {
			req.abort();
			buf = null;
			try {
				if (is != null) {
					is.close();				
				}
				
				if (randFile != null) {
					randFile.close();	
				}
			} catch (Exception e) {
				
			}
		}
	}

	/*
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		long oldFailTime = 0L;
		long failTime;
		boolean downLoadOK = false;
		logger.i("DownloadTask run");
		try {
			if (Cfg.mNotWifiProtect
					&& m_downloadWithWifi
					&& !listener.isWifiConnected()) {
				logger.i("wifi lost1");
				return;
			}
			
			if (!listener.hasInternet()) {
				return;
			}
			
			String status = Environment.getExternalStorageState();
			if (m_item.getFileType().equals(DownloadItem.AUDIO_FILE)
					&& !status.equals(Environment.MEDIA_MOUNTED)) {
				logger.i("sd can not use");
				return;
			}

			m_downloadResult = DOWNLOAD_RESULT_INVALID_VALUE;
			m_isRunning = true;
	
			for (int i = 1; i <= TRY_TIMES; i++) {//同一位置仅尝试TRY_TIMES次
				if (call() != null) {
					downLoadOK = true;
					m_downloadResult = DOWNLOAD_RESULT_SUCCESSFUL;
					break;
				} else if (m_isCanceled || m_isRemoved) {
					m_downloadResult = DOWNLOAD_RESULT_CANCEL;
					downLoadOK = true;
					break;
				} else if (Cfg.mNotWifiProtect
						&& m_downloadWithWifi
						&& !listener.isWifiConnected()) {
					downLoadOK = true;
					m_downloadResult = DOWNLOAD_RESULT_WIFI_DISCONNECTED;
					logger.i("wifi lost2");
					Thread.sleep(1000);
					break;
				} else if (!listener.hasInternet()) {
					downLoadOK = true;
					m_downloadResult = DOWNLOAD_RESULT_NO_INTERNET;
					break;
				} else if (m_item.getFileType().equals(DownloadItem.AUDIO_FILE)
						&& !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					m_downloadResult = DOWNLOAD_RESULT_SDCARD_NOT_MOUNTED;
					downLoadOK = true;
					break;
				} else if (m_item.getFileType().equals(DownloadItem.AUDIO_FILE)
						&& Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
						&& !(Utils.getFreeSpace(Cfg.SDCARD_PATH) > Cfg.sdSpaceLimitation * 1024)) {
					m_downloadResult = DOWNLOAD_RESULT_SDCARD_NO_SPACE;
					downLoadOK = true;
					break;
				} else if (m_isResourceChanged) {
					downLoadOK = true;
					i--;
				} else{
					logger.i("run failed");
					m_downloadResult = DOWNLOAD_RESULT_FAILED;
					failTime = System.currentTimeMillis();
					//距离上次出错大于17秒，则看做另一位置，重新计数
					if (i > 1 && failTime - oldFailTime > 17 * 1000L) {
						i = 0;
					}
					oldFailTime = failTime;
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			m_isRunning = false;
			
			if (m_downloadResult == DOWNLOAD_RESULT_CANCEL) {
				listener.taskCancelled(this, null);
			}
			
			if (m_downloadResult == DOWNLOAD_RESULT_FAILED) {
				listener.taskFailed(this, null);
			}
			
			if ((m_downloadResult == DOWNLOAD_RESULT_WIFI_DISCONNECTED)
					&& m_isNotWifiProtectDetect) {
				listener.taskWifiDisconnected(this);
			}
			
			if (m_downloadResult == DOWNLOAD_RESULT_NO_INTERNET) {
				listener.taskNoInternet(this);
			}
			
			//To prevent the task failed and retry after sd card eject detected
			if ((m_downloadResult == DOWNLOAD_RESULT_SDCARD_NOT_MOUNTED)
					&& m_isSDEjectDetect) {
				listener.taskSDEject(this);
			}
			
			if (m_downloadResult == DOWNLOAD_RESULT_SDCARD_NO_SPACE) {
				listener.taskSDNoSpace(this);
			}
	
			if (!downLoadOK) {
				String addr = InetAddress.getByName(mHost).getHostAddress();
				MobclickAgent.onEvent(listener.getAppContext(), Cfg.UM_DERR, addr + "[" + mHost + "]");	
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static int getInt(byte[] bb, int index) {
		if (bb == null || index < 0 || index + 4 > bb.length)
			return 0;
		else
			return ((((bb[index + 0] & 0xff) << 24)
					| ((bb[index + 1] & 0xff) << 16)
					| ((bb[index + 2] & 0xff) << 8) | ((bb[index + 3] & 0xff) << 0)));
	}
}
