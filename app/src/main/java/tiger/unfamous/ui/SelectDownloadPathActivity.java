package tiger.unfamous.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tiger.unfamous.Cfg;
import tiger.unfamous.R;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.common.TextViewPreference;
import tiger.unfamous.utils.Utils;

public class SelectDownloadPathActivity extends Activity implements
		AdapterView.OnItemClickListener {
	public static final String TAG = "SelectDownloadPathActivity";

	public static final int MAX_SEEK_VALUE = 1000;
	public static final int TIME_1000MS = 1000;

//	private static final int REFRESH = 1;
//	private static boolean isTwo = false;
//	private long mTime = 0;
	protected TextView mTopTitle;
	private ListView mListView;
//	private boolean mIsInMainTab;
	private Button mNewFolderBtn;
//	private Button mShowPlaying;
	private Button mConfirmBtn;
	private Button mCancelBtn;
	private EditText mEnterText;
	private String mFolderName;
	private Button mUpDir;
	protected TextView mCurPath;
//	private Button mRight ;
	/*public void updateListView() {
		((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
	}
*/
	//String path = new TextViewPreference(this, null).getSummary().toString()+"/";
	private File mCurrentDir ;
	private ArrayList<File> mDirFiles = new ArrayList<File>();


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		

		setContentView(R.layout.select_download_path_list);
		mTopTitle = (TextView) findViewById(R.id.title);
		mTopTitle.setTextSize(20);
		mTopTitle.setText("选择保存路径");
		
		mCurPath = (TextView) findViewById(R.id.currentdir);
		mUpDir = (Button)findViewById(R.id.updir);
		mUpDir.setOnClickListener(new BtnOnClickListener());
		
		ImageGetter imgGetter = new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                    Drawable drawable = null;
                    drawable = SelectDownloadPathActivity.this.getResources().getDrawable(
                                    Integer.parseInt(source));
                    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                                    drawable.getIntrinsicHeight());
                    return drawable;
            }
		};
		
		StringBuffer strBuf = new StringBuffer();
		strBuf.append("<img src=\"").append(R.drawable.updirectory).append("\"/>");
        Spanned span = Html.fromHtml(strBuf.toString(), imgGetter, null);
        mUpDir.setText(span);
		
		mListView = (ListView) findViewById(R.id.songslist);
		mListView.setOnItemClickListener(this);
		mNewFolderBtn = (Button) findViewById(R.id.rightbtn);
	//	mRight = (Button) findViewById(R.id.rightbtn1);
		
//		mRight.setVisibility(View.GONE);
		mNewFolderBtn.setText("新建文件夹");
		mCancelBtn = (Button) findViewById(R.id.cancel);
		mConfirmBtn = (Button) findViewById(R.id.confirm);
		mCancelBtn.setOnClickListener(new BtnOnClickListener());
		mConfirmBtn.setOnClickListener(new BtnOnClickListener());
		mNewFolderBtn.setOnClickListener(new BtnOnClickListener());
//		mShowPlaying.setOnClickListener(new BtnOnClickListener());
//		mCurrentDir = new File(new TextViewPreference(this, null).getSummary().toString());
		mCurrentDir = new File(new TextViewPreference(this,null).getSummary().toString());
		if (mCurrentDir != null) {
			browseTo(mCurrentDir);
		} else {
			browseTo(new File(Cfg.DOWNLOAD_DIR));
		}
		
		Utils.setColorTheme(findViewById(R.id.night_mask));
		//setTitle("自定义下载路径");
	}

	public void refresh() {
		if (mCurrentDir != null) {
			browseTo(mCurrentDir);
		}
	}
	
	private void setText(String fileDir) {
		mCurPath.setText(fileDir);
	}
	
	private void sortFiles(ArrayList<File> files) {
		try {
			Collections.sort(files, new Comparator<File>() {
				@Override
				public int compare(File object1, File object2) {
					String s1 = object1.getName();
					String s2 = object2.getName();
					Pattern p = Pattern.compile("\\d+");
					Matcher m1 = p.matcher(s1);
					Matcher m2 = p.matcher(s2);
					int ret = 0;
					boolean completed = false;
					while (!completed) {
						int intVal = 0;
						if (m1.find()) {
							s1 = m1.group();
							intVal++;
						}
						if (m2.find()) {
							s2 = m2.group();
							intVal++;
						}
						if (intVal == 2) {
							try {
								ret = Integer.parseInt(s1) - Integer.parseInt(s2);
								if (ret != 0) {
									completed = true;
								} else {
									completed = false;
								}
							} catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
								completed = false;
								// MobclickAgent.onEvent(LocalBrowser.this,
								// Cfg.UM_EXPTION_LABEL, "sortFiles: " +
								// object1.getName() + "_" + object2.getName());
							}
						} else {
							completed = true;
						}
					}
					return ret;
				}
			});
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	private synchronized void browseTo(final File location) {
		if (location == null) {
			return;
		}

		mCurrentDir = location;

		mDirFiles.clear();
		setText(mCurrentDir.getPath().toString());
//		if (location.getParentFile() != null) {
//			mDirFiles.add(mCurrentDir.getParentFile());
//		}	

		File[] files = mCurrentDir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory() && !file.getName().startsWith(".")) {
					if (!"dev".equals(file.getName()) && 
					   !"root".equals(file.getName()) &&
					   !"data".equals(file.getName()) &&
					   !"proc".equals(file.getName()) &&
					   !"sbin".equals(file.getName()) &&
					   !"sys".equals(file.getName())  &&
					   !"system".equals(file.getName()) &&
					   !"devlog".equals(file.getName()) &&
					   !"cache".equals(file.getName()) &&
					   !"etc".equals(file.getName()) &&
					   !"d".equals(file.getName()) &&
					   !"acct".equals(file.getName()) &&
					   !"config".equals(file.getName()) &&
					   !"secure".equals(file.getName()) &&
					   !"asec".equals(file.getName()) &&
					   !"obb".equals(file.getName()) &&
					   !"app-cache".equals(file.getName())) {
						mDirFiles.add(file);
					}
				} 

			}
		}
	
		sortFiles(mDirFiles);
		SongsListAdapter adapter = ((SongsListAdapter) mListView.getAdapter());
		if (adapter == null) {
			mListView.setAdapter(new SongsListAdapter(this));
		} else {
			adapter.notifyDataSetChanged();
		}
	}

	protected class SongsListAdapter extends BaseAdapter {
		protected Context context;
		LayoutInflater mInflate;

		public SongsListAdapter(Context context) {
			this.context = context;
			mInflate = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mDirFiles.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mDirFiles.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		public void setColor(TextView tv, boolean playing) {
			if (playing) {
				tv.setTextColor(Color.RED);
			} else {
				tv.setTextColor(Color.BLACK);
			}
		}

		@Override
		public View getView(final int index, View convertView, ViewGroup parent) {
			File currentFile = mDirFiles.get(index);
			LinearLayout ll = (LinearLayout) convertView;
			if (ll == null) {
				ll = (LinearLayout) mInflate.inflate(R.layout.local_file_item,
						null, false);
			}
			ImageView iconImg = (ImageView) ll.findViewById(R.id.icon);
			TextView songName = (TextView) ll.findViewById(R.id.file_name);
//			songName.setTextColor(Color.WHITE);

			String filename;
//			if (index == 0
//					&& (currentFile.getParentFile() == null || currentFile
//							.getParentFile().getAbsolutePath()
//							.compareTo(mCurrentDir.getAbsolutePath()) != 0)) {
//				iconImg.setImageResource(R.drawable.updirectory);
//				filename = new String("..");
//			} else {
			if (currentFile.isDirectory()) {
				iconImg.setImageResource(R.drawable.directory);
			}
			filename = currentFile.getName();
//			}

			songName.setText(filename);

			return ll;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long id) {
		File file = mDirFiles.get((int) id);
		if (file.isDirectory()) {
			browseTo(file);
		}
	}

	
	class BtnOnClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.rightbtn:
				File f = new File(mCurrentDir.getPath().toString());
				if (f.canWrite()) {
					LayoutInflater inflater = getLayoutInflater();
			          final View layout = inflater.inflate(R.layout.input_dialog,
			            (ViewGroup) findViewById(R.id.dialog)); 
			          new AlertDialog.Builder(SelectDownloadPathActivity.this).setTitle("新建文件夹").setView(layout)
			            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
			      
				      @Override
				      public void onClick(DialogInterface dialog, int which) {
					       mEnterText=(EditText) layout.findViewById(R.id.etname);
					       mFolderName = mEnterText.getText().toString();
					       buildNewFolder();
				      }
				     })
				            .setNegativeButton("取消", null).show();
				} else {
//					Toast.makeText(SelectDownloadPathActivity.this, "该文件夹没有写入权限", Toast.LENGTH_LONG).show();
					MyToast.showShort(SelectDownloadPathActivity.this, "该文件夹没有写入权限");
				}
				break;
			case R.id.confirm:
				File ff = new File(mCurrentDir.getPath().toString());
				if (ff.canWrite()) {
					Cfg.saveStr(SelectDownloadPathActivity.this, "mCurrentDir", mCurrentDir.getPath().toString());
					Cfg.DOWNLOAD_DIR = Cfg.loadStr(SelectDownloadPathActivity.this, "mCurrentDir", Cfg.SDCARD_PATH + "/善听");
					Intent i  = new Intent();
					i.putExtra("mCurrentDir", mCurrentDir.getPath().toString());
					setResult(RESULT_OK, i);
//					new TextViewPreference(SelectDownloadPathActivity.this, null).getSummary();
					finish();
				} else {
//					Toast.makeText(SelectDownloadPathActivity.this, "该文件夹没有权限", Toast.LENGTH_LONG).show();
					MyToast.showShort(SelectDownloadPathActivity.this, "该文件夹没有写入权限");
				}
				break;
			case R.id.cancel:
				finish();
				break;
			case R.id.updir:
				File parentDir = mCurrentDir.getParentFile();
				if (parentDir != null) {
					browseTo(parentDir);
				}
				break;
			}
		}
		
	}

	private void buildNewFolder() {
		String path = mCurrentDir.getPath().toString();//+File.separator+mFolderName;
		File f = new File(path+File.separator+mFolderName);
			if (!f.exists()) {
				f.mkdirs();
			} else {
				MyToast.showShort(this, "该文件夹已存在");
			}
			browseTo(f);
			
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Utils.setColorTheme(findViewById(R.id.night_mask));
	}
}