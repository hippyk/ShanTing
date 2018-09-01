package tiger.unfamous.common;

import java.io.File;
import java.util.ArrayList;

import android.widget.ListView;

import tiger.unfamous.data.Book;


public class ApplicationGlobalVariable {

	public static  boolean isBoolean = false;
	public static  Book mCurBook;
//	public static  File mCurBrowseDir;
	public static  ArrayList<File> mFiles = new ArrayList<File>();
//	public static  ListView mListView;
	public static boolean isBoolean() {
		return isBoolean;
	}
	public static void setBoolean(boolean isBoolean) {
		ApplicationGlobalVariable.isBoolean = isBoolean;
	}
	public static Book getmCurBook() {
		return mCurBook;
	}
	public static void setmCurBook(Book mCurBook) {
		ApplicationGlobalVariable.mCurBook = mCurBook;
	}
//	public static File getmCurBrowseDir() {
//		return mCurBrowseDir;
//	}
//	public static void setmCurBrowseDir(File mCurBrowseDir) {
//		ApplicationGlobalVariable.mCurBrowseDir = mCurBrowseDir;
//	}
	public static ArrayList<File> getmFiles() {
		return mFiles;
	}
	public static void setmFiles(ArrayList<File> mFiles) {
		ApplicationGlobalVariable.mFiles = mFiles;
	}
//	public static ListView getmListView() {
//		return mListView;
//	}
//	public static void setmListView(ListView mListView) {
//		ApplicationGlobalVariable.mListView = mListView;
//	}
	
	
	
}
