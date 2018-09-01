package tiger.unfamous.data;

import java.io.Serializable;

import tiger.unfamous.Cfg;
import tiger.unfamous.data.Dir.ChildType;
import tiger.unfamous.utils.Utils;

//已废弃，仅用于老版本用户升级
public class Dir1 implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4867719118435967197L;

	private String name;

	public String getName() {
		return name;
	}

	private String address;

	public String getAddress() {
		return address;
	}

	private String listFileAddr;

	private int childCount;

	public int getChildCount() {
		return childCount;
	}

	private boolean noListFile;

	public boolean noListFile() {
		return noListFile;
	}

	private int pageCount;

	public int getPageCount() {
		return pageCount;
	}

	private ChildType childType;

	public ChildType getChildType() {
		return childType;
	}

	public Dir1(String name, String addr, int childCount, int pageCount,
			ChildType type) {
		if (name == null)
			name = "";
		if (addr == null)
			addr = "";

		this.name = name;
		// if (addr.contains("/")) {
		this.address = Utils.fixUrlHost(addr, Cfg.LISTS_ROOT);
		this.listFileAddr = address + "/" + Cfg.LIST_FILE_NAME;
		// }
		// else {
		// this.listFileAddr = Cfg.LIST_URL + address;
		// }

		this.childCount = childCount;
		if (pageCount < 1) {
			noListFile = true;
			pageCount = childCount / Cfg.SONGS_PER_PAGE;
			if (childCount % Cfg.SONGS_PER_PAGE > 0) {
				pageCount++;
			}
		}
		this.pageCount = pageCount;
		this.childType = type;
	}

	public String getListFileAddr(int page) {
		if (pageCount > 1) {
			return listFileAddr + "_" + page;
		} else {
			return listFileAddr;
		}
	}

	// dir is "list"
	// public boolean noAddr() {
	// return (address == null);
	// }
}
