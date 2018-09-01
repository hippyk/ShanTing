package tiger.unfamous.common;

import tiger.unfamous.Cfg;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class TextViewPreference extends Preference {
	private Context mContext;
	private String mCurDir;
	
	public TextViewPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
		init();
	}
	
	public void init() {
		mCurDir = Cfg.loadStr(mContext, "mCurrentDir", Cfg.SDCARD_PATH+ "/善听") ;
		this.setSummary(mCurDir);
	}

	@Override
	protected void onClick() {
		super.onClick();
		/*Intent intent = new Intent(mContext,SelectDownloadPathActivity.class);
		mContext.startActivity(intent);*/
	}
}
