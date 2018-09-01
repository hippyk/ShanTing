package tiger.unfamous.data;

import java.io.Serializable;

import tiger.unfamous.Cfg;
import tiger.unfamous.utils.Utils;

//已废弃，仅用于老版本用户升级
public class Dir implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4867719118435967196L;

	public enum ChildType {
		DIR, SONG
	};

	private String name;

	public String getName() {
		return name;
	}

	private String address;

	public String getAddress() {
		return address;
	}

	private String listFileAddr;

	private int pageCount = 1;

	public int getPageCount() {
		return pageCount;
	}

	private ChildType childType;

	public ChildType getChildType() {
		return childType;
	}

	public Dir(String name, String addr, int page, ChildType type) {
		if (name == null)
			name = "";
		if (addr == null)
			addr = "";

		this.name = name;
		// if (addr.contains("/")) {
		this.address = Utils.fixUrlHost(addr, Cfg.WEB_HOME);
		this.listFileAddr = address + "/" + Cfg.LIST_FILE_NAME;
		// }
		// else {
		// this.listFileAddr = Cfg.LIST_URL + address;
		// }

		this.pageCount = page;
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
