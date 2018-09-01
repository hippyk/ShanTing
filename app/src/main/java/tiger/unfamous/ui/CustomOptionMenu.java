package tiger.unfamous.ui;

import tiger.unfamous.Cfg;
import tiger.unfamous.R;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class CustomOptionMenu extends PopupWindow {

	/**
	 * 菜单的布局，包含一行或者两行线性布局
	 */
	private LinearLayout mLayout;

	/**
	 * 菜单项
	 */
	Menu mMenu;

	/**
	 * 小于3个菜单项则只显示一行
	 */
	boolean mTwoLine = false;
	/**
	 * 是否夜间模式
	 */
	boolean mIsNightMode = false;
	/**
	 * 菜单第一行的布局
	 */
	LinearLayout m1stLineLayout;
	/**
	 * 菜单第二行的布局
	 */
	LinearLayout m2ndLineLayout;
	
	/**
	 * 显示菜单的Activity，供菜单点击的时候用到
	 */
	static Activity mActivity;
	
	static CustomOptionMenu mInstance;

	int mBgColor;
	int mTextColor;
//	int mSelectedColor;
//	int mItemBgColor;

//	Drawable mBgDrawable;
//	Drawable mNightBgDrawable;
	
//	Drawable mBgSelDrawable;
//	Drawable mNightSelBgDrawable;
	
//	Drawable mItemBgDrawable;
//	Drawable mItemNightBgDrawable;
	
	/**
	 * 构造方法
	 * 
	 * @param context
	 *            调用方的上下文
	 * @param myMenuAnim
	 *            菜单需要的动画效果
	 */
	public CustomOptionMenu(Context context) {
		super(context);
		mLayout = new LinearLayout(context);
		mLayout.setOrientation(LinearLayout.VERTICAL);
		
		mLayout.setBackgroundResource(mIsNightMode ? R.drawable.menubg_night : R.drawable.menubg_day);
		
		m1stLineLayout = new LinearLayout(context);
		m2ndLineLayout = new LinearLayout(context);
		
		int menuHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.custom_menu_height);
		
		LinearLayout.LayoutParams lineLayoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
				menuHeight);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);
		layoutParams.leftMargin = 0;
		setBackgroundDrawable(context.getResources().getDrawable(R.drawable.menubg_day));

		m1stLineLayout.setOrientation(LinearLayout.HORIZONTAL);
		m2ndLineLayout.setOrientation(LinearLayout.HORIZONTAL);

		mLayout.setLayoutParams(layoutParams);
		m1stLineLayout.setLayoutParams(lineLayoutParams);
		m2ndLineLayout.setLayoutParams(lineLayoutParams);

		this.mLayout.addView(m1stLineLayout);

		// 设置菜单的特征
		setContentView(this.mLayout);
		setWidth(LayoutParams.FILL_PARENT);
		setHeight(LayoutParams.WRAP_CONTENT);
		setFocusable(true);

		setNightMode(Cfg.mIsNightMode);
		initColors(context);
		mLayout.setFocusableInTouchMode(true);
		mLayout.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {

				if (keyCode == KeyEvent.KEYCODE_MENU && isShowing()) {
					dismiss();
					return true;
				}
				return false;
			}
		});
	}

	void initColors(Context ctx) {
		Resources resources = ctx.getResources();
		if (mIsNightMode) {
			mBgColor = resources.getColor(R.color.menu_bg_night);
			mTextColor = resources.getColor(R.color.menu_text_night);
//			mSelectedColor = resources
//					.getColor(R.color.menu_item_bg_selected_night);
//			mItemBgColor = resources
//					.getColor(R.color.menu_item_bg_night);
			
		} else {
			mBgColor = resources.getColor(R.color.menu_bg_normal);
			mTextColor = resources.getColor(R.color.menu_text);
//			mSelectedColor = resources.getColor(R.color.menu_item_bg_selected);
//			mItemBgColor = resources
//					.getColor(R.color.menu_item_bg);
		}
		
//		if(mBgDrawable == null){
//			mBgDrawable = resources.getDrawable(R.drawable.menubg_day);
//		}
//		if(mNightBgDrawable == null){
//			mNightBgDrawable = resources.getDrawable(R.drawable.menubg_night);
//		}
//
//		if(mItemBgDrawable == null){
//			mItemBgDrawable = resources.getDrawable(R.drawable.menuitem_day_bg);
//		}
//		if(mItemNightBgDrawable == null){
//			mItemNightBgDrawable = resources.getDrawable(R.drawable.menuitem_night_bg);
//		}
//		
//		if(mBgSelDrawable == null){
//			mBgSelDrawable = resources.getDrawable(R.drawable.menu_day_sel);
//		}
//		if(mNightSelBgDrawable == null){
//			mNightSelBgDrawable = resources.getDrawable(R.drawable.menu_night_sel);
//		}
		
	}

	public void setNightMode(boolean isNightMode) {
		if(mIsNightMode != isNightMode){
			mLayout.setBackgroundResource(isNightMode ? R.drawable.menubg_night : R.drawable.menubg_day);
			mIsNightMode = isNightMode;
		}
		
		initColors(mActivity);
	}

	public void setMenu(Menu menu) {
		mMenu = menu;
		if (mMenu != null) {
			int firstLineNum = 0;
			if (mMenu.size() > 3) {
				mTwoLine = true;
				firstLineNum = (mMenu.size()) / 2;
			}else{
				mTwoLine = false;
				firstLineNum = mMenu.size();
			}
			
			android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1);
			int menuHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.custom_menu_height);
			
			
			if (m1stLineLayout.getChildCount() != firstLineNum) {

				m1stLineLayout.removeAllViews();
				for (int i = 0; i < firstLineNum; i++) {
					LinearLayout optionMenuItem = (LinearLayout) LayoutInflater
							.from(mActivity).inflate(R.layout.custom_menu_item, null);
//					optionMenuItem.setWeightSum((float) 1.0);
					optionMenuItem.setOnTouchListener(mOptionMenuTouchListener);
					m1stLineLayout.addView(optionMenuItem, params);
				}
			}
			//添加第二行
			if (!mTwoLine && mLayout.getChildCount() > 1) {
				mLayout.removeView(m2ndLineLayout);
			} else if (mTwoLine && mLayout.getChildCount() < 2) {
				mLayout.addView(m2ndLineLayout);
			}
			
			if (mTwoLine) {
				m2ndLineLayout.removeAllViews();
				for (int i = firstLineNum; i < mMenu.size(); i++) {
					LinearLayout optionMenuItem = (LinearLayout) LayoutInflater
							.from(mActivity).inflate(R.layout.custom_menu_item, null);
					optionMenuItem.setOnTouchListener(mOptionMenuTouchListener);
//					optionMenuItem.setWeightSum((float) 1.0);
					m2ndLineLayout.addView(optionMenuItem, params);
				}
			} else {
				setHeight(menuHeight);
			}

			ImageView iconImageView;
			TextView textView;
			for (int i = 0; i < mMenu.size(); i++) {
				View optioView;
				if (i < firstLineNum) {
					optioView = m1stLineLayout.getChildAt(i);
				} else {
					optioView = m2ndLineLayout.getChildAt(i - firstLineNum);
				}
				
				optioView.setBackgroundResource(Cfg.mIsNightMode ? R.drawable.menuitem_night_bg : R.drawable.menuitem_day_bg);
				
				iconImageView = (ImageView) optioView.findViewById(R.id.icon);
				textView = (TextView) optioView.findViewById(R.id.name);
				textView.setTextColor(mTextColor);

				MenuItem item = mMenu.getItem(i);
				optioView.setTag(i);
				if (item.getIcon() != null) {
					iconImageView.setImageDrawable(item.getIcon());
					iconImageView.setVisibility(View.VISIBLE);
				} else {
					iconImageView.setVisibility(View.INVISIBLE);
				}

				textView.setText(item.getTitle());
			}

		}
	}

	View.OnTouchListener mOptionMenuTouchListener = new View.OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
//				v.setBackgroundColor(Cfg.mIsNightMode ? 0xFF3C3F42 : 0xFFD7D7D7);
				v.setBackgroundResource(Cfg.mIsNightMode ? R.drawable.menu_night_sel : R.drawable.menu_day_sel);
				break;
			case MotionEvent.ACTION_UP:
				mActivity.onOptionsItemSelected(mMenu.getItem((Integer) v
						.getTag()));
				if (isShowing()) {
					dismiss();
				}
				mActivity.onOptionsMenuClosed(mMenu);
				mActivity = null;
				mMenu = null;
				break;
			}
			return true;
		}
	};
	
	/**
	 * 显示自定义的menu
	 * 
	 * @param featureId
	 * @param menu
	 * @param activity
	 * @return true 显示系统Menu,false显示自定义的menu
	 */
	public static boolean showCustomMenu(int featureId, Menu menu, Activity activity, View parentView){
		if(parentView == null){
			return true;
		}
		if (mActivity != activity || mInstance == null){
			mInstance = null;
			mActivity = activity;
			mInstance = new CustomOptionMenu(activity);
		} 
		
		if(mActivity != null && mInstance != null){
			mInstance.setNightMode(Cfg.mIsNightMode);
			mInstance.setMenu(menu);
			mInstance.update();
			mInstance.showAtLocation(parentView,Gravity.BOTTOM, 0, 0); 
			return false;
		} else {
			return true;
		}
	}

}
