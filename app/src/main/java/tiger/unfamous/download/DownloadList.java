package tiger.unfamous.download;

import java.util.ArrayList;
import java.util.Iterator;

import tiger.unfamous.utils.DataBaseHelper;
import tiger.unfamous.utils.MyLog;

/*
 * All operation should sync with DB.
 */
public class DownloadList {

	private static final MyLog logger = new MyLog("DownloadList");

	public static final int DOWNLOAD_LIST_EVNET_ADD_ITEM = 1;
	public static final int DOWNLOAD_LIST_EVNET_REMOVE_ITEM = 2;
	public static final int DOWNLOAD_LIST_EVNET_UPDATE_ITEM = 3;
	public static final int DOWNLOAD_LIST_EVNET_REMOVE_ITEMS = 4;

	// hold the down load items
	private ArrayList<DownloadItem> m_list;

	// list change listener
	private ArrayList<DownloadListListener> m_listenerList = null;

	// private OMSDownloadListListener m_listener = null;

	// singleton
	private static DownloadList m_instance = null;

	/*
	 * Note, app should not use this to get the download list directly. Instead,
	 * app should get the download list by download service.
	 */
	public static DownloadList getInstance() {
		if (m_instance == null) {
			m_instance = new DownloadList();
		}
		return m_instance;
	}
	
	public static void Init() {
		m_instance = null;
	}

	private DownloadList() {
		m_list = new ArrayList<DownloadItem>();
		m_listenerList = new ArrayList<DownloadListListener>();
		initFromDB();
	}

	/*
	 * Interface for sync with DB
	 */
	/*
	 * Init the download list from DB.
	 */
	private boolean initFromDB() {

		logger.v("initFromDB() ---> Enter");
		ArrayList<DownloadItem> list = DataBaseHelper.getInstance()
				.queryDownloadList(null);
		if (list != null) {
			for (DownloadItem t : list) {
				m_list.add(t);
			}
		}
		sortList();
		logger.v("initFromDB() ---> Exit");
		return true;
	}

	public boolean checkDuplicateItem(DownloadItem item) {
		synchronized(this) {
			Iterator<DownloadItem> it = m_list.iterator();
			while (it.hasNext()) {
				DownloadItem i = it.next();
				if (i.getItemId() != item.getItemId()
						&& i.getUnit_name() != item.getUnit_name()) {
					return true;
				}
			}
		}
		return false;
	}

	public void processDuplicateItem(DownloadItem item) {
		int i = 0;
		String unitName = item.getUnit_name();
		if (unitName != null) {

			while (checkDuplicateItem(item)) {
				logger.v("Item has fileName existed ---> rename");
				item.setUnit_name(unitName + "(" + (++i) + ")");
			}
		}
	}

	/*
	 * Add download item to download list.
	 */
	public long addItem(DownloadItem item) {

		logger.v("addItem() ---> Enter");
		// processDuplicateItem(item);
		long id = 0;
		DownloadItem ditem = DataBaseHelper.getInstance()
				.getDownloadItemByUnitUrl(item.getUrl());
		if (ditem != null) {
			id = ditem.getItemId();
		} else {

			id = DataBaseHelper.getInstance().insertDownloadItem(item);
		}
		item.setItemId(id);
		
		synchronized(this) {
			m_list.add(item);

			/*
			 * if(m_listener != null) {
			 * m_listener.DownloadListChanged(DOWNLOAD_LIST_EVNET_ADD_ITEM, item); }
			 */

			if (!m_listenerList.isEmpty()) {
				Iterator<DownloadListListener> itl = m_listenerList.iterator();
				while (itl.hasNext()) {
					DownloadListListener listener = itl.next();
					listener
							.DownloadListChanged(DOWNLOAD_LIST_EVNET_ADD_ITEM, item);
				}
			}
		}

		logger.v("Add item id: " + String.valueOf(id));
		logger.v("addItem() ---> Exit");

		return id;
	}

	/*
	 * Remove the item from the list.
	 */
	public boolean removeItem(long item_id) {
		DownloadItem item = getItemById(item_id);
		if (item == null) {
			return true;
		}
		DataBaseHelper.getInstance().deleteDownloadItem(item_id);

		synchronized(this) {
			boolean result = m_list.remove(item);
			if (!m_listenerList.isEmpty()) {
				Iterator<DownloadListListener> itl = m_listenerList.iterator();
				while (itl.hasNext()) {
					DownloadListListener listener = itl.next();
					listener.DownloadListChanged(DOWNLOAD_LIST_EVNET_REMOVE_ITEM,
							item);
				}
			}

			return result;			
		}
	}

	public boolean removeItems(ArrayList<DownloadItem> list) {
		Iterator<DownloadItem> it = list.iterator();

		if (list.isEmpty()) {
			return true;
		}

		synchronized(this) {
			while (it.hasNext()) {
				DownloadItem downloadItem = it.next();
				m_list.remove(downloadItem);
				DataBaseHelper.getInstance().deleteDownloadItem(
						downloadItem.getItemId());
			}
			/*
			 * if(m_listener != null) {
			 * m_listener.DownloadListChanged(DOWNLOAD_LIST_EVNET_REMOVE_ITEMS,
			 * null); }
			 */
			if (!m_listenerList.isEmpty()) {
				Iterator<DownloadListListener> itl = m_listenerList.iterator();
				while (it.hasNext()) {
					DownloadListListener listener = itl.next();
					listener.DownloadListChanged(DOWNLOAD_LIST_EVNET_REMOVE_ITEMS,
							null);
				}
			}
		}

		return true;

	}

	public DownloadItem getItemById(long item_id) {
		synchronized(this) {
			Iterator<DownloadItem> it = m_list.iterator();
			while (it.hasNext()) {
				DownloadItem item = it.next();
				if (item.getItemId() == item_id) {
					return item;
				}
			}
		}
		return null;
	}

	/*
	 * update the item in DB. item should in item list. return the number of the
	 * affect item. It should always be 0 or 1.
	 */
	public int updateItem(DownloadItem item) {
		int ret = DataBaseHelper.getInstance().updateDownloadItem(item);

		synchronized(this) {
			if (!m_listenerList.isEmpty()) {
				Iterator<DownloadListListener> itl = m_listenerList.iterator();
				while (itl.hasNext()) {
					DownloadListListener listener = itl.next();
					listener.DownloadListChanged(DOWNLOAD_LIST_EVNET_UPDATE_ITEM,
							item);
				}
			}
		}

		/*
		 * if(m_listener != null) {
		 * m_listener.DownloadListChanged(DOWNLOAD_LIST_EVNET_UPDATE_ITEM,
		 * item); }
		 */

		return ret;
	}

	public ArrayList<DownloadItem> getItemsByStatus(int status) {
		ArrayList<DownloadItem> list = new ArrayList<DownloadItem>();
		
		synchronized(this) {
			Iterator<DownloadItem> it = m_list.iterator();
			while (it.hasNext()) {
				DownloadItem item = it.next();
				if (item.getStatus() == status) {
					list.add(item);
				}
			}
		}
		return list;
	}

	public ArrayList<DownloadItem> getAllNonCompleteItems() {
		ArrayList<DownloadItem> list = new ArrayList<DownloadItem>();
		
		synchronized(this) {
			Iterator<DownloadItem> it = m_list.iterator();
			while (it.hasNext()) {
				DownloadItem item = it.next();
				if (item.getStatus() != DownloadItem.FINISHED) {
					list.add(item);
				}
			}
		}
		return list;
	}

	public ArrayList<DownloadItem> getAllNonCompleteItems(int udtype) {
		ArrayList<DownloadItem> list = new ArrayList<DownloadItem>();
		
		synchronized(this) {
			Iterator<DownloadItem> it = m_list.iterator();
			while (it.hasNext()) {
				DownloadItem item = it.next();
				if (item.getStatus() != DownloadItem.FINISHED/*
															 * && item.getUDType()
															 * == udtype
															 */) {
					list.add(item);
				}
			}
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<DownloadItem> getList() {
		synchronized(this) {
			return (ArrayList<DownloadItem>)m_list.clone();
		}
	}

	public void sortList() {
		// Collections.sort(m_list);
	}

	public void setListListener(DownloadListListener lis) {
		synchronized(this) {
			m_listenerList.add(lis);
			// m_listener = lis;
		}
	}

	public void clearListListener(DownloadListListener lis) {
		synchronized(this) {
			m_listenerList.remove(lis);
		}
	}

	/*
	 * public OMSDownloadListListener getListListener() { return m_list; }
	 */
}
