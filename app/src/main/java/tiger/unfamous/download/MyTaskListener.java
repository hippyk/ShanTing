package tiger.unfamous.download;

import android.content.Context;

public interface MyTaskListener<T extends MyTask<?>> {

	public void taskStarted(T task);

	// max means the max value in progress bar. value is the current value.
	public void taskProgress(T task, long value, long max);

	public void taskCompleted(T task, Object obj);

	public void taskFailed(T task, Throwable ex);

	public void taskCancelled(T task, Object obj);

	public void taskRemoved(T task, Object obj);
	
	public void taskWifiDisconnected(T task);
	
	public void taskSDEject(T task);
	
	public void taskNoInternet(T task);
	
	public void taskSDNoSpace(T task);
	
	public boolean isWifiConnected();
	
	public boolean hasInternet();

	public Context getAppContext();
}