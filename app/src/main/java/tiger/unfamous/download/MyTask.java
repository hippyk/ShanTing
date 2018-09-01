package tiger.unfamous.download;

import java.util.Date;
import java.util.concurrent.Callable;

public abstract class MyTask<V> implements Callable<V>, Comparable<MyTask<V>>,
		Runnable {

	public static final int TRY_INTERVAL = 1000;

	protected MyTaskListener<MyTask<V>> listener;

	// default priority is zero, low number,high priority
	protected int priority = 0;

	// the lastest task has higher priority. please refer to compareTo();
	protected long timeStamp;

	// if the task is running.
	protected boolean m_isRunning;

	// if the task has been canceled.
	protected boolean m_isCanceled;
	protected boolean m_isRemoved;
	protected boolean m_isComplete;

	public MyTask(MyTaskListener<MyTask<V>> listener) {
		this.listener = listener;
		timeStamp = new Date().getTime();
		m_isRunning = false;
		m_isCanceled = false;
		m_isRemoved = false;
		m_isComplete = false;
	}

	public MyTask(MyTaskListener<MyTask<V>> listener, int _priority) {
		this(listener);
		this.priority = _priority;
		m_isRunning = false;
		m_isCanceled = false;
		m_isRemoved = false;
		m_isComplete = false;
	}

	// sub class must implement this method.
	public abstract V get() throws Exception;

	public void run() {
		
	}

	public V call() {
		V obj = null;
		try {
			if (m_isCanceled || m_isRemoved) {
				return null;
			}
			if (listener != null) {
				// Log.v("======excutor======", "taskStart");
				listener.taskStarted(this);
			}
			obj = get();
		} catch (Exception ex) {		
			if (m_isCanceled == false && m_isRemoved == false) {
//				Log.i("======excutor======", "taskFail");
				if (listener != null) {
					listener.taskFailed(this, ex);
				}
			}
			
			return null;
		}

		if (m_isComplete) {
			if (listener != null) {
				listener.taskCompleted(this, obj);
				return obj;
			}
		}

		return null;
	}

	@Override
	public int compareTo(MyTask<V> obj) {
		if (obj == null) {
			return -1;
		}

		// MyTask other = (MyTask) obj;
		int oPriority = obj.getPriority();
		long oTimeStamp = obj.getTimeStamp();

		// Priority Queue - Delete Min
		// Little number, higher priority
		if (this.priority < oPriority) {
			return -1;
		}

		if (this.priority > oPriority) {
			return 1;
		}

		// Latest time, higher priority
		if (this.timeStamp > oTimeStamp) {
			return -1;
		}

		if (this.timeStamp < oTimeStamp) {
			return 1;
		}

		return 0;
	}

	public MyTaskListener<MyTask<V>> getListener() {
		return listener;
	}

	public void setListener(MyTaskListener<MyTask<V>> listener) {
		this.listener = listener;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public boolean isCancel() {
		return m_isCanceled;
	}

	public boolean cancelTask() {
//		if (m_isRunning) {
//			m_isRunning = false;
//		} 
		m_isCanceled = true;
		return m_isCanceled;
	}

	public boolean removeTask() {
//		if (m_isRunning) {
//			m_isRunning = false;
//		}
		m_isRemoved = true;
		return m_isRemoved;
	}
	
	public boolean isRemove() {
		return m_isRemoved;
	}

	public boolean isRunning() {
		return m_isRunning;
	}

	public void setCancel(boolean cancel) {
		this.m_isCanceled = cancel;
	}

}
