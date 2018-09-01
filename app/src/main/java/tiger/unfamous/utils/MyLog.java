package tiger.unfamous.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import tiger.unfamous.Cfg;

import android.os.Environment;
import android.util.Log;

public class MyLog {
	private String tag = "MyLog";
	public static int logLevel = Log.VERBOSE;

	public static boolean writeFileFlag = false;
	public static String logFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/xs.log";
	public static FileOutputStream fileinput = null;

	public MyLog() {
		if (!Cfg.DEBUG) {
			logLevel = Log.INFO;
		}

		if (writeFileFlag) {
			File file = new File(logFile);
			if (file.exists()) {
				file.delete();
			}
			try {
				file.createNewFile();
				fileinput = new FileOutputStream(file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public MyLog(String tag) {

		if (!Cfg.DEBUG) {
			logLevel = Log.INFO;
		}

		this.tag = tag;
		if (writeFileFlag) {
			File file = new File(logFile);
			if (file.exists()) {
				file.delete();
			}
			try {
				file.createNewFile();
				fileinput = new FileOutputStream(file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String getFunctionName() {
		StackTraceElement[] sts = Thread.currentThread().getStackTrace();
		if (sts == null) {
			return null;
		}

		for (StackTraceElement st : sts) {
			if (st.isNativeMethod()) {
				continue;
			}
			if (st.getClassName().equals(Thread.class.getName())) {
				continue;
			}
			if (st.getClassName().equals(this.getClass().getName())) {
				continue;
			}
			return "[ " + Thread.currentThread().getId() + ": " + st.getFileName() + ":" + st.getLineNumber() + " ]";
		}
		return null;
	}

	public void info(Object str) {
		if (logLevel <= Log.INFO) {
			String name = getFunctionName();

			String ls = (name == null ? str.toString() : (name + " - " + str));
			Log.i(tag, ls);
			if (writeFileFlag) {
				writeToFile("Info", tag, name + " - " + str);
			}
		}
	}

	public void i(Object str) {
		info(str);
	}

	public void verbose(Object str) {
		if (logLevel <= Log.VERBOSE) {
			String name = getFunctionName();

			String ls = (name == null ? str.toString() : (name + " - " + str));
			Log.v(tag, ls);
			if (writeFileFlag) {
				writeToFile("Verbose", tag, ls);
			}
		}
	}

	public void v(Object str) {
		verbose(str);
	}

	public void warn(Object str) {
		if (logLevel <= Log.WARN) {
			String name = getFunctionName();

			String ls = (name == null ? str.toString() : (name + " - " + str));
			Log.w(tag, ls);
			if (writeFileFlag) {
				writeToFile("Warn", tag, ls);
			}

		}
	}

	public void w(Object str) {
		warn(str);
	}

	public void error(Object str) {
		if (logLevel <= Log.ERROR) {
			String name = getFunctionName();

			String ls = (name == null ? str.toString() : (name + " - " + str));
			Log.e(tag, ls);
			if (writeFileFlag) {
				writeToFile("Error", tag, ls);
			}
		}
	}

	public void error(Exception ex) {
		if (logLevel <= Log.ERROR) {

			StringBuffer sb = new StringBuffer();
			String name = getFunctionName();

			StackTraceElement[] sts = ex.getStackTrace();

			if (name != null) {
				sb.append(name + " - " + ex + "\r\n");
			} else {
				sb.append(ex + "\r\n");
			}

			if (sts != null && sts.length > 0) {
				for (StackTraceElement st : sts) {
					if (st != null) {
						sb.append("[ " + st.getFileName() + ":" + st.getLineNumber() + " ]\r\n");
					}
				}
			}
			Log.e(tag, sb.toString());
			if (writeFileFlag)
				writeToFile("Excep", tag, sb.toString());
		}
	}

	public void e(Object str) {
		error(str);
	}

	public void e(Exception ex) {
		error(ex);
	}

	public void debug(Object str) {
		Log.e("", "logLevel" + logLevel);
		if (logLevel <= Log.DEBUG) {
			String name = getFunctionName();
			String ls = (name == null ? str.toString() : (name + " - " + str));
			Log.d(tag, ls);
			if (writeFileFlag) {
				writeToFile("Debug", tag, ls);
			}
		}
	}

	public void d(Object str) {
		debug(str);
	}

	private void writeToFile(String level, String tag, String info) {
		SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd   hh:mm:ss");
		String date = sDateFormat.format(new Date());
		String msg = date + "  " + level + "--" + tag + ":" + info;

		try {
			fileinput.write(msg.toString().getBytes());
			fileinput.write("\r\n".getBytes());
			fileinput.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}