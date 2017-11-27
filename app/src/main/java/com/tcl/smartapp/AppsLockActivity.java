package com.tcl.smartapp;

import com.slidingmenu.lib.SlidingMenu;
import com.tcl.smartapp.db.SmartappDBOperator;
import com.tcl.smartapp.domain.AppInfo;
import com.tcl.smartapp.token.DrawPwdActivity;
import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.AccessibilityUtils;
import com.tcl.smartapp.utils.AppUtil;
import com.tcl.smartapp.utils.Constants;
import com.tcl.smartapp.utils.MySlidingMenu;
import com.tcl.smartapp.view.SwitchButton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class AppsLockActivity extends Activity implements OnClickListener {
    private static final String TAG = "AppsLockActivity";

    private Context context;
    private MySlidingMenu mMySlidingMenu;
    public static AppsLockActivity mAppLockInstance;

    private LinearLayout mLoadingLayout;
    private LinearLayout mAccessibilityDisableLayout;
    private LinearLayout mLockAppListLayout;
    private ListView mLvApp;
    private Button mStartAppLock;
    private TextView mAppLockInfoText;
    private TextView mAppSortText;
    private TextView mEmptyAppLockedText;

    private Handler mUIHandler = null;
    private static final int MSG_UPDATE_UI = 10001;

    private SmartappDBOperator mDbo;

    private SharedPreferences mPrefs;

    private int mLockedAppNum = 0;
    private List<AppInfo> mAppInfos;
    private MyAdapter adapter = null;
    private boolean mSortedByLocked = false;

    private static final String[] IGNORE_PACKAGE_LIST = {
            "com.android.systemui"
    };

    private Comparator<AppInfo> MyComparator = new Comparator<AppInfo>() {
        @Override
        public int compare(AppInfo app1, AppInfo app2) {
            if (mSortedByLocked) {
                if ((app1.isLocked() && app2.isLocked()) ||
                        (!app1.isLocked() && !app2.isLocked())) {
                    return app1.getName().compareTo(app2.getName());
                } else if (app1.isLocked()) {
                    return -1;
                } else if (app2.isLocked()) {
                    return 1;
                }
            }

            return app1.getName().compareTo(app2.getName());
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mMySlidingMenu.getSlidingMenu().showMenu(false);
        mMySlidingMenu.getSlidingMenu().showContent(false);
        String tellMsg = "";
        if (AccessibilityUtils.getmObserverService() == null) {
            tellMsg = getString(R.string.accessibility_disabled);
        }
        if (!SettingsActivity.isEnabledLock()) {
            tellMsg = tellMsg + getString(R.string.lock_status_disabled);
        }
        if (AccessibilityUtils.getmObserverService() == null
                || !SettingsActivity.isEnabledLock()) {
            mAccessibilityDisableLayout.setVisibility(View.VISIBLE);
            mLockAppListLayout.setVisibility(View.GONE);
            mEmptyAppLockedText.setText(tellMsg);
        } else {
            mAccessibilityDisableLayout.setVisibility(View.GONE);
            mLockAppListLayout.setVisibility(View.VISIBLE);
        }
        TokenUtils.startSelfLock(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_app_lock);
        context = this;
        mAppLockInstance = this;

        mMySlidingMenu = new MySlidingMenu(context);
        mMySlidingMenu.getSlidingMenu().attachToActivity(this, SlidingMenu.SLIDING_WINDOW);

        mLoadingLayout = (LinearLayout) findViewById(R.id.loadingLayout);
        mAccessibilityDisableLayout = (LinearLayout) findViewById(R.id.emptyLayout);
        mStartAppLock = (Button) findViewById(R.id.btn_startAppLock);
        mStartAppLock.setOnClickListener(this);
        mLockAppListLayout = (LinearLayout) findViewById(R.id.hasLockedAppLayout);
        mLvApp = (ListView) findViewById(R.id.lv_app);
        mAppLockInfoText = (TextView) findViewById(R.id.tv_appLockInfo);
        mAppSortText = (TextView) findViewById(R.id.tv_appsort);
        mAppSortText.setOnClickListener(this);

        mEmptyAppLockedText = (TextView) findViewById(R.id.TextV_emptyAppLocked);

        mUIHandler = new UIHandler();

        mDbo = new SmartappDBOperator(context);

        mPrefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        getAppInfos();

        mLvApp.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick position:" + position);
                if (position < mAppInfos.size()) {
                    AppInfo info = mAppInfos.get(position);
                    String packageName = info.getPackageName();
                    boolean isLocked = info.isLocked();
                    Log.d(TAG, "onItemClick packageName:" + packageName + ", isLocked:" + isLocked);
                    ViewHolder holder = (ViewHolder) view.getTag();
                    if (isLocked) {
                        mDbo.delete(packageName);
                        mLockedAppNum--;
                        info.setLocked(false);
                    } else {
                        //check the AccessibilityService
                        if (AccessibilityUtils.getmObserverService() == null) {
                            AccessibilityUtils.showAccessibilityPrompt(context);
                            return;
                        }
                        mDbo.add(packageName);
                        mLockedAppNum++;
                        info.setLocked(true);
                    }
                    doAsRelatedPackage(packageName, !isLocked);
                    holder.iv_applock.setChecked(info.isLocked());
                    holder.tv_app_summary.setText(info.isLocked() ? R.string.text_app_locked : R.string.text_app_unlocked);
                    updateLockedAppNumTV();
                    AccessibilityUtils.setHasLockedApp(mDbo.hasLockedApp());
                    if (mSortedByLocked) {
                        sortAppInfos();
                    }
                }
            }
        });
    }

    public static boolean isIgnorePackage(String packageName) {
        if (Constants.MY_PACKAGE_NAME.equals(packageName)) {
            return true;
        }
        if (AccessibilityUtils.isInputMethodPackage(packageName)) {
            return true;
        }
        for (String ignored : IGNORE_PACKAGE_LIST) {
            if (ignored.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void getAppInfos() {
        mLoadingLayout.setVisibility(View.VISIBLE);
        mAccessibilityDisableLayout.setVisibility(View.GONE);
        mLockAppListLayout.setVisibility(View.GONE);
        new Thread() {
            public void run() {
                mLockedAppNum = 0;
                mAppInfos = new ArrayList<>();
                PackageManager manager = context.getPackageManager();
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> resolveInfoLists = getPackageManager()
                        .queryIntentActivities(mainIntent, 0);
                for (ResolveInfo resolveInfo : resolveInfoLists) {
                    String pkgName = resolveInfo.activityInfo.packageName;
                    //ignore self defined package
                    if (isIgnorePackage(pkgName)) {
                        continue;
                    }
                    if (isExistAppInfo(pkgName)) {
                        continue;
                    }
                    boolean isLocked = mDbo.isLockedApp(pkgName);
                    if (isLocked) {
                        mLockedAppNum++;
                    }
                    ApplicationInfo appInfo;
                    try {
                        appInfo = getPackageManager().getApplicationInfo(pkgName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "can not found appInfo about package:" + pkgName);
                        continue;
                    }
                    if (appInfo == null) {
                        Log.e(TAG, "appInfo is null about package:" + pkgName);
                        continue;
                    }
                    mAppInfos.add(new AppInfo(pkgName,
                            (String) appInfo.loadLabel(manager),
                            appInfo.loadIcon(manager),
                            isLocked));
                }
                AccessibilityUtils.setHasLockedApp(mDbo.hasLockedApp());
                Collections.sort(mAppInfos, MyComparator);
                Message msg = mUIHandler.obtainMessage(MSG_UPDATE_UI);
                mUIHandler.sendMessage(msg);
            }
        }.start();
    }

    private void sortAppInfos() {
        new Thread() {
            public void run() {
                Collections.sort(mAppInfos, MyComparator);
                Message msg = mUIHandler.obtainMessage(MSG_UPDATE_UI);
                mUIHandler.sendMessage(msg);
            }
        }.start();
    }

    private class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mAppInfos.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;
            if (convertView != null && convertView instanceof RelativeLayout) {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            } else {
                view = View.inflate(context, R.layout.list_item_applock, null);
                holder = new ViewHolder();
                holder.iv_app_icon = (ImageView) view.findViewById(R.id.iv_app_icon);
                holder.tv_app_name = (TextView) view.findViewById(R.id.tv_app_name);
                holder.tv_app_summary = (TextView) view.findViewById(R.id.tv_app_summary);
                holder.iv_applock = (SwitchButton) view.findViewById(R.id.iv_applock);
                view.setTag(holder);
            }
            if (position < mAppInfos.size()) {
                AppInfo info = mAppInfos.get(position);
                holder.iv_app_icon.setImageDrawable(info.getAppIcon());
                holder.tv_app_name.setText(info.getName());
                holder.tv_app_summary.setText(info.isLocked() ? R.string.text_app_locked : R.string.text_app_unlocked);
                holder.iv_applock.setChecked(info.isLocked());
            } else {
                Log.e(TAG, "MyAdapter-getView Error position:" + position);
            }

            return view;
        }
    }

    private static class ViewHolder {
        ImageView iv_app_icon;
        TextView tv_app_name;
        TextView tv_app_summary;
        SwitchButton iv_applock;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public MySlidingMenu getMySlidingMenu() {
        return this.mMySlidingMenu;
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick:" + v.getId());
        switch (v.getId()) {
            case R.id.btn_startAppLock:
                if (!SettingsActivity.isEnabledLock()) {
                    Intent settingIntent = new Intent(context, SettingsActivity.class);
                    context.startActivity(settingIntent);
                    return;
                }
                if (AccessibilityUtils.getmObserverService() == null) {
                    AccessibilityUtils.showAccessibilityPrompt(context);
                    return;
                }
                mAccessibilityDisableLayout.setVisibility(View.GONE);
                mLockAppListLayout.setVisibility(View.VISIBLE);
                break;
            case R.id.tv_appsort:
                mSortedByLocked = !mSortedByLocked;
                mAppSortText.setText(mSortedByLocked ? R.string.text_appsorted_locked :
                        R.string.text_appsorted_name);
                sortAppInfos();
                break;
            default:
                Log.e(TAG, "unknown view id:" + v.getId());
                break;
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (AppUtil.onBackKeyDown(this, keyCode, event)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void updateLockedAppNumTV() {
        String tmp = context.getString(R.string.text_appLockInfo);
        tmp = String.format(tmp, mLockedAppNum);
        mAppLockInfoText.setText(tmp);
    }

    private class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_UI:
                    if (adapter == null) {
                        adapter = new MyAdapter();
                        mLvApp.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                    updateLockedAppNumTV();

                    mLoadingLayout.setVisibility(View.GONE);
                    String tellMsg = "";
                    if (AccessibilityUtils.getmObserverService() == null) {
                        tellMsg = getString(R.string.accessibility_disabled);
                    }
                    if (!SettingsActivity.isEnabledLock()) {
                        tellMsg = tellMsg + getString(R.string.lock_status_disabled);
                    }
                    /* check AccessibilityService if has lock app */
                    if (AccessibilityUtils.getmObserverService() == null
                            || !SettingsActivity.isEnabledLock()) {
                        mAccessibilityDisableLayout.setVisibility(View.VISIBLE);
                        mLockAppListLayout.setVisibility(View.GONE);
                        mEmptyAppLockedText.setText(tellMsg);
                        //AccessibilityUtils.showAccessibilityPrompt(context);
                        break;
                    } else {
                        mAccessibilityDisableLayout.setVisibility(View.GONE);
                        mLockAppListLayout.setVisibility(View.VISIBLE);
                    }
                    break;
                default:
                    Log.e(TAG, "UIHandler: unknown msg:" + msg.what);
                    break;
            }
        }
    }

    private boolean isExistAppInfo(String packageName) {
        if (mAppInfos == null || packageName == null || "".equals(packageName)) {
            return false;
        }
        for (int i = 0; i < mAppInfos.size(); i++) {
            AppInfo temp = mAppInfos.get(i);
            if (temp != null && packageName.equals(temp.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void doAsRelatedPackage(String packageName, boolean locked) {
        String[][] relatedPackageName = TokenUtils.getRelatedPackageList();
        if (packageName == null || relatedPackageName == null) {
            return;
        }
        for (int i = 0; i < relatedPackageName.length; i++) {
            if (packageName.equals(relatedPackageName[i][0])) {
                if (locked) {
                    mDbo.add(relatedPackageName[i][1]);
                } else {
                    mDbo.delete(relatedPackageName[i][1]);
                }
            }
        }
    }
}
