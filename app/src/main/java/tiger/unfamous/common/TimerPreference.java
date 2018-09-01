package tiger.unfamous.common;

import tiger.unfamous.Cfg;
import tiger.unfamous.DN;
import tiger.unfamous.services.MainService;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.TimePicker;

public class TimerPreference extends Preference {

	Context mContext;

	public TimerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	/**
	 * 将protected改成public以便PlayActivity调用
	 * @author mtfabc@hotmail.com
	 */
	
	@Override
	public void onClick() {
		final Context ctx = getContext();
		new MyTimePickerDlg(ctx, new TimePickerDialog.OnTimeSetListener() {

			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				Intent i = new Intent(ctx, MainService.class);
				i.setAction(Cfg.ACTION_SCHEDULE_STOP);
				i.putExtra(DN.TIME_LEFT, hourOfDay * 60 + minute + 1);
				ctx.startService(i);
				Cfg.saveInt(mContext, Cfg.PREF_LAST_HOUR, hourOfDay);
				Cfg.saveInt(mContext, Cfg.PREF_LAST_MINUTE, minute);
				MyToast.showShort(ctx, "" + (hourOfDay * 60 + minute) + "分钟后停止播放");
			}
		}, Cfg.loadInt(mContext, Cfg.PREF_LAST_HOUR, 0), Cfg.loadInt(mContext,
				Cfg.PREF_LAST_MINUTE, 30), true).show();
	}

	class MyTimePickerDlg extends TimePickerDialog {

		public MyTimePickerDlg(Context context, OnTimeSetListener callBack,
				int hourOfDay, int minute, boolean is24HourView) {
			super(context, callBack, hourOfDay, minute, is24HourView);
		}

		@Override
		public void setTitle(int titleId) {
			super.setTitle("时长设定(小时/分钟)");
		}

		@Override
		public void setTitle(CharSequence title) {
			super.setTitle("时长设定(小时/分钟)");
		}
	}
}
