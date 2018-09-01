package tiger.unfamous.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import tiger.unfamous.AppConnect;
import tiger.unfamous.Cfg;
import tiger.unfamous.R;
import tiger.unfamous.common.MyToast;
import tiger.unfamous.common.TimerPreference;
import tiger.unfamous.data.Play_State;
import tiger.unfamous.services.MainService;
import tiger.unfamous.utils.MyLog;
import tiger.unfamous.utils.Utils;


public abstract class CommonPlayUI extends BasicActivity implements
		OnClickListener, SeekBar.OnSeekBarChangeListener, AdapterView.OnItemClickListener {

	private static final MyLog log = new MyLog();

	public static final int TIME_1000MS = 1000;
	// 按照音频时长为1小时，定制进度条的最大进度
	public static final int MAX_SEEK_VALUE = 100 * 36 * TIME_1000MS;

	protected static final int MENU_DOWNLOAD_ALL = Menu.FIRST + 3;
	protected static final int MENU_DOWNLOAD_MANAGER = Menu.FIRST + 4;
	protected static final int MENU_TIMER = Menu.FIRST + 1;// 添加一个Menu ：
															// mtfabc@hotmail.com
//	protected static final int MENU_SHARE = Menu.FIRST + 4;
	protected static final int MENU_CONTINUOUSLY_PLAY = Menu.FIRST + 2;
	protected static final int MENU_SHORTCUT = Menu.FIRST + 6;
	protected static final int MENU_NIGHT_MODE = Menu.FIRST + 5;

	protected int mProgressValue = 0;

	protected ProgressBar mProgressBar;
	protected TextView mCurrentTime;
	protected TextView mTotalTime;
	protected ListView mListView;
	protected Button mBtnSetting;
	protected Button mBtnExit;
	protected ImageButton mPauseButton;
	protected ImageButton mStopButton;
	protected Button mPrePageBtn;
	protected Button mNextPageBtn;
	protected Button mSkip;
	protected Button mSongShareBtn;
	protected ImageButton mNextSongBtn;
	protected TextView mTopTitle;
	protected SeekBar mSeekBar;

	protected long mPosOverride = -1;
	protected boolean mShowPlaying;
	protected boolean mFromNotify;
	protected boolean mLocateHistory;
	protected boolean mWaitDlgToShow;
	protected long mDuration;
	protected long mOldPos;

	protected Button mBtnErrorReport;
	protected EditText mBugDetail;
	protected RadioGroup mChoiceGroup;
	protected RadioButton mRadioOtherBtn;

	protected boolean mIsCurrentSongSkipPlay = false;

	protected TextView mSongName;
//	DomobInterstitialAd mInterstitialAd;

	MenuItem mTimerMenuItem;
	MenuItem mAutoPlayMenuItem;

	// 当前的avtivity是否仍然在运行
	static boolean mIsActivityRunning = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Cfg没有初始化，导致有可能不显示广告
		Cfg.init(this);
	}

	
	// @Override
	// public void onPause() {
	// // TODO Auto-generated method stub
	// super.onPause();
	// mIsActivityAlive = true;
	// }

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mIsActivityRunning = true;
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		mIsActivityRunning = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_DOWNLOAD_MANAGER, MENU_DOWNLOAD_MANAGER,
				R.string.downloading).setIcon(R.drawable.menu_download_manager);
		// menu.add(0, 1, 0,
		// R.string.completed).setIcon(R.drawable.ic_menu_my_downloads);
		// menu.add(0, MENU_DOWNLOAD_ALL, MENU_DOWNLOAD_ALL,
		// R.string.all_download);
		// menu.add(0, MENU_SHARE, MENU_SHARE,
		// R.string.menu_share).setIcon(R.drawable.menu_share);
		mTimerMenuItem = menu.add(0, MENU_TIMER, MENU_TIMER, "定时停止");
		mTimerMenuItem.setIcon(R.drawable.menu_schedule_stop);
		mAutoPlayMenuItem = menu.add(0, MENU_CONTINUOUSLY_PLAY,
				MENU_CONTINUOUSLY_PLAY, "定集停止");
		mAutoPlayMenuItem.setIcon(R.drawable.auto_play_next);
//		menu.add(0, MENU_SHORTCUT, MENU_SHORTCUT, R.string.create_shortcut)
//				.setIcon(R.drawable.shortcut);

		return true;
	}

	public void enableTimerAndAutoPlayOption(boolean enable) {
		if (mTimerMenuItem != null) {
			mTimerMenuItem.setEnabled(enable);
		}

		if (mAutoPlayMenuItem != null) {
			mAutoPlayMenuItem.setEnabled(enable);
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_DOWNLOAD_MANAGER:
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(), DownloadManager.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
			break;
		// mtf: 调用定时的方法
		case MENU_TIMER:
			new TimerPreference(this, null).onClick();
			break;
		case MENU_CONTINUOUSLY_PLAY:
			showContinuPlayDialog();
			break;
//		case MENU_SHORTCUT:
//			addShortcut();
//			break;
		case MENU_NIGHT_MODE:
			Cfg.mIsNightMode = !Cfg.mIsNightMode;
			Cfg.saveBool(CommonPlayUI.this, Cfg.PREF_NIGHT_MODE, Cfg.mIsNightMode);
			Utils.setColorTheme(findViewById(R.id.night_mask));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private final DialogInterface.OnClickListener mContinuePlayBtnListener = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which >= 0) {
				Cfg.mContinuouslyPlayChaps = which;
				Cfg.saveInt(CommonPlayUI.this, Cfg.PREF_CONTINOUSLY_CHAPS,
						Cfg.mContinuouslyPlayChaps);
			} else {
				Cfg.mContinuouslyPlayChaps = Cfg.loadInt(CommonPlayUI.this,
						Cfg.PREF_CONTINOUSLY_CHAPS, 0);
			}

			MyToast.showShort(CommonPlayUI.this, "播放"
					+ (Cfg.mContinuouslyPlayChaps + 1) + " 集后停止");
			if (mService.mCurPlayBook != null && !mService.isPlayStopped()) {
				mService.updateAutoNextNotifaction();
			}

			dialog.dismiss();
		}
	};

	/**
	 * 显示连播对话框
	 */
	public void showContinuPlayDialog() {
		Builder builder = new AlertDialog.Builder(this);
		final int defaultSelect = Cfg.loadInt(this, Cfg.PREF_CONTINOUSLY_CHAPS,
				0);
		String choiceItems[] = { " 1集", " 2集", " 3集", " 4集", " 5集" };

		builder.setTitle("选择播放多少集后停止");
		builder.setSingleChoiceItems(choiceItems, defaultSelect,
				mContinuePlayBtnListener);

		final AlertDialog alertDialog = builder.create();
		alertDialog.setButton("设置", mContinuePlayBtnListener);
		alertDialog.setButton2("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				// if(Cfg.mCoutinuouslyPlayChaps != Integer.MAX_VALUE){
				// MyToast.showShort(CommonPlayUI.this, "已取消定集停止设置");
				// }

				Cfg.mContinuouslyPlayChaps = Integer.MAX_VALUE;
				if (mService.mCurPlayBook != null && !mService.isPlayStopped()) {
					mService.updateAutoNextNotifaction();
				}
			}
		});
		
		try{
			alertDialog.show();
		}catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromTouch) {

		if (!fromTouch || mService == null) {
			return;
		}

		setSeekBarTouching(true);

		if (mProgressValue != 0) {
			mProgressValue = progress;
		}

		if (!mService.isPlayStopped() && mProgressValue != 0) {
			mPosOverride = mDuration * mProgressValue / MAX_SEEK_VALUE;
			mService.setPositon(mPosOverride);
			mCurrentTime.setText(Utils.makeTimeString(this,
					(int) (mPosOverride / TIME_1000MS)));
			mProgressBar.setVisibility(View.GONE);
			mTotalTime.setVisibility(View.VISIBLE);
			mTotalTime.setText(Utils.makeTimeString(this, mDuration
					/ TIME_1000MS));
		}

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		setSeekBarTouching(false);
		if (!mService.isPlayStopped()) {
			mPosOverride = mDuration * mProgressValue / MAX_SEEK_VALUE;
			mService.seek(mPosOverride);
			mCurrentTime.setText(R.string.wait);
			if (mService.isPlaying()) {
				mProgressBar.setVisibility(View.VISIBLE);
				mTotalTime.setVisibility(View.GONE);
				seekBar.setProgress(mProgressValue);
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mIsActivityRunning = false;
		if (mService != null && mService.isPlayStopped()
				&& mService.mMainActivity == null) {
			Intent intent = new Intent(this, MainService.class);
			stopService(intent);
		}
	}


	public void setmDuration(long duration) {
		// TODO Auto-generated method stub
		mDuration = duration;
		if(mProgressBar.getVisibility() != View.GONE){
			mProgressBar.setVisibility(View.GONE);
		}
		if(mTotalTime.getVisibility() != View.VISIBLE){
			mTotalTime.setVisibility(View.VISIBLE);
		}
		mTotalTime.setText(Utils.makeTimeString(this, mDuration / TIME_1000MS));
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub

		if (mService != null) {
			if (mService.isPlayStopped()) {
				enableTimerAndAutoPlayOption(false);
			} else {
				enableTimerAndAutoPlayOption(true);
			}

		}
		if (menu.findItem(MENU_NIGHT_MODE) != null){
			menu.removeItem(MENU_NIGHT_MODE);
		}
		menu.add(0, MENU_NIGHT_MODE, MENU_NIGHT_MODE, Cfg.mIsNightMode ? R.string.day_mode : R.string.night_mode)
			.setIcon(Cfg.mIsNightMode ? R.drawable.icon_day : R.drawable.icon_night);
		return super.onPrepareOptionsMenu(menu);
	}


	protected void reportResourceProblem() {
		if (mService.mCurPlayBook != null) {
			LinearLayout bugReportViewLayout = (LinearLayout) LinearLayout
					.inflate(this, R.layout.report_bug, null);
			mChoiceGroup = (RadioGroup) bugReportViewLayout
					.findViewById(R.id.bug_select);
			mRadioOtherBtn = (RadioButton) bugReportViewLayout
					.findViewById(R.id.radio2);

			// 选择第一个默认的
			RadioButton radioButton = (RadioButton) bugReportViewLayout
					.findViewById(R.id.radio4);
			if (radioButton != null) {
				radioButton.setChecked(true);
			}
			TextView name = (TextView) bugReportViewLayout
					.findViewById(R.id.song_name);

			name.setText(mService.mCurPlayBook.mSongName);

			mBugDetail = (EditText) bugReportViewLayout
					.findViewById(R.id.bug_detail);

			mBugDetail.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					mRadioOtherBtn.setChecked(true);
					return false;
				}
			});

			mRadioOtherBtn
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							if (isChecked) {
								mBugDetail.requestFocus();
								mBugDetail.setFocusableInTouchMode(true);
								InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
								imm.showSoftInput(mBugDetail,
										InputMethodManager.SHOW_FORCED);
							} else {
								mBugDetail.clearFocus();
								InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

								imm.hideSoftInputFromWindow(
										mBugDetail.getWindowToken(), 0);
							}
						}
					});

			AlertDialog.Builder b = new AlertDialog.Builder(this);
			b.setView(bugReportViewLayout);

			b.setPositiveButton("提交", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub

					int selectRadio = 1;
					String umEventString = Cfg.UM_BC_YINZHI;

					switch (mChoiceGroup.getCheckedRadioButtonId()) {
					case R.id.radio0:// 音质差
						selectRadio = 1;
						umEventString = Cfg.UM_BC_YINZHI;
						break;
					// case R.id.radio1:
					// selectRadio = 1;
					// break;
					// case R.id.radio3:
					// selectRadio = 2;
					// break;
					case R.id.radio4:// 音量小
						selectRadio = 2;
						umEventString = Cfg.UM_BC_YINLIANG;
						break;
					case R.id.radio5:// 标题内容不符
						selectRadio = 3;
						umEventString = Cfg.UM_BC_NEIRONG;
						break;
					case R.id.radio2: // 其他
						selectRadio = 4;
						umEventString = Cfg.UM_BC_QITA;
						break;
					case R.id.radio_ad:// 广告
						selectRadio = 5;
						umEventString = Cfg.UM_BC_GUANGGAO;
						break;
					}
					StringBuilder sb = new StringBuilder();
					String detail = mBugDetail.getText().toString();

					sb.append("【").append(mService.mCurPlayBook.mTitle)
							.append("】,")
							.append(mService.mCurPlayBook.mSongName);

					if (selectRadio == 4) {
						sb.append(",【").append(detail).append("】");

					}
					if (selectRadio != 4
							|| (detail != null && detail.length() > 0)) {
						MobclickAgent.onEvent(CommonPlayUI.this, umEventString,
								sb.toString());
					}

					MyToast.showShort(CommonPlayUI.this, "感谢您的参与！！这对我们很有用！:)");
				}

			});
			//
			b.setNegativeButton("取消", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.dismiss();
				}
			});
			b.show();
		}
	}


	public void updateCurPlaySongName(String name) {
		if (mSongName != null) {
			mSongName.setText(name);
		}
	}

	final View.OnClickListener mPauseListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
//			if (mService.isPlaying()) {
//				showInterstitialAd(CommonPlayUI.this);
//				showVideoAd(false);
//				MobclickAgent.onEvent(CommonPlayUI.this, Cfg.UM_PAUSE_CLICK);
//			}
			mService.doPauseResume();
			
		}
	};


	public void showPopAd() {
		AppConnect.getInstance(this).showPopAd(this);
	}

	boolean isPaused() {
		return mService.isPlayPaused();
	}

	public void setPauseButtonImage() {
		if (isPaused()) {
			mPauseButton.setImageResource(R.drawable.player_play_btn);
		} else {
			mPauseButton.setImageResource(R.drawable.button_pause);
		}
	}


	public void setFirstProgress(int progress) {
		// TODO Auto-generated method stub
		mProgressValue = progress;
		mSeekBar.setProgress(progress);
	}

	public void setSeekBarTouching(boolean isCurrentSongSkipPlay) {
		mIsCurrentSongSkipPlay = isCurrentSongSkipPlay;
	}



	public abstract boolean playNextSong();

	public abstract void setSecondProgress(int arg1);

	public abstract void onStateChanged(Play_State curState);

	public abstract boolean playPreviousSong();

//	public abstract boolean currentSongSkipPlay();
}
