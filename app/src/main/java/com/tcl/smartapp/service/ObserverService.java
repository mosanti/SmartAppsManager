package com.tcl.smartapp.service;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tcl.smartapp.AppsLockActivity;
import com.tcl.smartapp.R;
import com.tcl.smartapp.SettingsActivity;
import com.tcl.smartapp.SmartApplication;
import com.tcl.smartapp.SystemLockActivity;
import com.tcl.smartapp.db.SmartappDBOperator;
import com.tcl.smartapp.token.DrawPwdActivity;
import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.AccessibilityUtils;
import com.tcl.smartapp.utils.Constants;

import java.util.List;

public class ObserverService extends AccessibilityService {

    private static final String TAG = "ObserverService";

    private static final int CHECK_INSTALL_UNINSTALL = 10002;
    private static final int CHECK_APP_LOCK = 10003;
    private static final int CHECK_RECENT_APPS = 10004;
    private static final int CHECK_ACCESSIBILITY_WINDOW = 10005;
    private static final int CHECK_CLEAR_DATA_WINDOW = 10006;
    private static final int CHECK_CANCEL_ACTIVE_WINDOW = 10007;

    private static final String PACKAGE_NAME_KEY = "PACKAGE_NAME_KEY";
    private static final String TOP_ACTIVITY_KEY = "TOP_ACTIVITY_KEY";
    private static final String INSTALL_UNINSTALL_CONTENT_KEY = "INSTALL_UNINSTALL_CONTENT_KEY";
    private static final String ACCESSIBILITY_WINDOW_KEY = "ACCESSIBILITY_WINDOW_KEY";
    private static final String INSTALL_UNINSTALL_PACKAGE = "com.google.android.packageinstaller";
    private static final String RECENT_APPS_ACTIVITY = "com.android.systemui.recent.RecentsActivity";
    private static final String RECENTS_APPS_ACTIVITY = "com.android.systemui.recents.RecentsActivity";
    private static final String CLEAR_DATA_WINDOW_KEY = "CLEAR_DATA_WINDOW_KEY";
    private static final String CANCEL_ACTIVE_KEY = "CANCEL_ACTIVE_WINDOW_KEY";
    private static String UNINSTALL_BUTTON_TEXT;
    private static String INSTALL_BUTTON_TEXT;
    private static String mAccessibilityDescription;
    private static String mAccessibilityPopupTitle;
    private static String smClearDataButtonText;
    private static String mCancelActiveText;
    private static String smSelfAppName;
    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private SmartappDBOperator mDbo;
    private static String mLastTopPackageName = null;
    private static String mScreenOffPackageName = null;
    private BroadcastReceiver mReceiver;
    private SharedPreferences mSharedPreferences;
    private static String mLastInstallorUninstallPackageName = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mAccessibilityDescription = getBaseContext()
                .getText(R.string.accessibility_description).toString();
        mAccessibilityPopupTitle = getBaseContext()
                .getText(R.string.accessibility_popup_title).toString();
        UNINSTALL_BUTTON_TEXT = getBaseContext()
                .getText(R.string.uninstall_button_text).toString();
        INSTALL_BUTTON_TEXT = getBaseContext()
                .getText(R.string.install_button_text).toString();
        smClearDataButtonText = getBaseContext().getText(R.string.clear_data_button_text).toString();
        smSelfAppName = getBaseContext().getText(R.string.app_name).toString();
        mCancelActiveText = getBaseContext().getText(R.string.cancel_active_adim).toString();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        Log.d(TAG, "onAccessibilityEvent: eventType = " + Integer.toString(eventType));

        String className = event.getClassName().toString();
        String packageName = event.getPackageName().toString();
        boolean isInstallOrUninstallButton = false;
        boolean isAccessibilityWindow = false;
        boolean isClearDataWindow = false;
        Log.d(TAG, "onAccessibilityEvent: packageName = " + packageName + ",className = " + className);

        if (AccessibilityUtils.getIncomingPhone()) {
            if (className != null && className.indexOf("com.tcl.smartapp.token") == -1 && SettingsActivity.isEnabledLock()) {
                Intent incomingPhoneIntent = new Intent(getApplicationContext(), DrawPwdActivity.class);
                incomingPhoneIntent.putExtra(Constants.LOCK_TYPE, Constants.INCOMING_PHONE_LOCK);
                incomingPhoneIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Log.d(TAG, "onAccessibilityEvent incoming phone,restartActivity: DrawPwdActivity");
                getApplicationContext().startActivity(incomingPhoneIntent);
                return;
            }
        }

        Bundle data = new Bundle();
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        if (rootNodeInfo != null) {
            //Accessibility windows
            List<AccessibilityNodeInfo> nodeInfos = rootNodeInfo
                    .findAccessibilityNodeInfosByText(mAccessibilityDescription);
            if (nodeInfos != null) {
                for (AccessibilityNodeInfo info : nodeInfos) {
                    Log.d(TAG, "nodeInfos class name:" + info.getClassName());
                    if ("android.widget.TextView".equals(info.getClassName())) {
                        relockAccessibilityWindow();
                    }
                }
            }
            //Accessibility stop popup windows
            nodeInfos = rootNodeInfo
                    .findAccessibilityNodeInfosByText(mAccessibilityPopupTitle);
            if (nodeInfos != null) {
                for (AccessibilityNodeInfo info : nodeInfos) {
                    Log.d(TAG, "nodeInfos class name:" + info.getClassName());
                    if ("android.widget.TextView".equals(info.getClassName())) {
                        isAccessibilityWindow = true;
                    }
                }
            }

            //uninstall button
            List<AccessibilityNodeInfo> uninstallNodeInfos = rootNodeInfo
                    .findAccessibilityNodeInfosByText(UNINSTALL_BUTTON_TEXT);
            if (uninstallNodeInfos != null) {
                Log.d(TAG, "uninstallNodeInfos sizes:" + uninstallNodeInfos.size());
                for (AccessibilityNodeInfo info : uninstallNodeInfos) {
                    Log.d(TAG, "uninstallNodeInfos class name:" + info.getClassName());
                    if ("android.widget.Button".equals(info.getClassName())) {
                        isInstallOrUninstallButton = true;
                    }
                }
            }

            //install button
            List<AccessibilityNodeInfo> installNodeInfos = rootNodeInfo
                    .findAccessibilityNodeInfosByText(INSTALL_BUTTON_TEXT);
            if (installNodeInfos != null) {
                for (AccessibilityNodeInfo info : installNodeInfos) {
                    Log.d(TAG, "installNodeInfos class name:" + info.getClassName());
                    if ("android.widget.Button".equals(info.getClassName())) {
                        isInstallOrUninstallButton = true;
                    }
                }
            }

            //check clear data window
            List<AccessibilityNodeInfo> appNameNodeInfos = rootNodeInfo
                    .findAccessibilityNodeInfosByText(smSelfAppName);
            if (appNameNodeInfos != null) {
                for (AccessibilityNodeInfo info : appNameNodeInfos) {
                    Log.d(TAG, "appNameNodeInfos app name:" + info.getClassName());
                    List<AccessibilityNodeInfo> clearDataNodeInfos = rootNodeInfo
                            .findAccessibilityNodeInfosByText(smClearDataButtonText);
                    if(clearDataNodeInfos != null){
                        for (AccessibilityNodeInfo info1 : clearDataNodeInfos) {
                            if ("android.widget.Button".equals(info1.getClassName())) {
                                isClearDataWindow = true;
                            }
                        }
                    }

                }
            }

            //check cancel active window
            List<AccessibilityNodeInfo> appNameNodeInfos1 = rootNodeInfo
                    .findAccessibilityNodeInfosByText(smSelfAppName);
            if (appNameNodeInfos1 != null) {
                for (AccessibilityNodeInfo info : appNameNodeInfos1) {
                    Log.d(TAG, "appNameNodeInfos1 app name:" + info.getClassName());
                    List<AccessibilityNodeInfo> clearDataNodeInfos = rootNodeInfo
                            .findAccessibilityNodeInfosByText(mCancelActiveText);
                    if(clearDataNodeInfos != null){
                        for (AccessibilityNodeInfo info1 : clearDataNodeInfos) {
                            if ("android.widget.Button".equals(info1.getClassName())) {
                                Log.d(TAG, "appNameNodeInfos1 cancel active");
                                data.putBoolean(CANCEL_ACTIVE_KEY, true);
                            }
                        }
                    }

                }
            }

        }

        data.putString(PACKAGE_NAME_KEY, packageName);
        data.putString(TOP_ACTIVITY_KEY, className);
        data.putBoolean(ACCESSIBILITY_WINDOW_KEY, isAccessibilityWindow);
        data.putBoolean(INSTALL_UNINSTALL_CONTENT_KEY, isInstallOrUninstallButton);
        data.putBoolean(CLEAR_DATA_WINDOW_KEY, isClearDataWindow);
        checkData(data);
    }

    private void checkData(Bundle data) {

        String packageName = data.getString(PACKAGE_NAME_KEY);
        String topActivity = data.getString(TOP_ACTIVITY_KEY);
        boolean isInstallOrUninstall = data.getBoolean(INSTALL_UNINSTALL_CONTENT_KEY, false);
        boolean isAccessibilityWindow = data.getBoolean(ACCESSIBILITY_WINDOW_KEY, false);
        boolean isClearDataWindow = data.getBoolean(CLEAR_DATA_WINDOW_KEY, false);
        boolean isCancelActiveWindow = data.getBoolean(CANCEL_ACTIVE_KEY, false);
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(ObserverService.this);
        boolean isInstallOrUnistallAppEnabled = sharedPreferences
                .getBoolean(SystemLockActivity.KEY_INSTALL_UNINSTALL_APPS, false);
        boolean isRecentAppsLockEnabled = sharedPreferences
                .getBoolean(SystemLockActivity.KEY_RECENT_APPS_LOCK, false);
        boolean isUnlockAccessibilityWindow = sharedPreferences
                .getBoolean(Constants.ACCESSIBILITY_WINDOW_LOCK, false);
        boolean isUnlockRecentApp = sharedPreferences
                .getBoolean(Constants.RECENT_APP_LOCK, false);
        boolean isUnlockInstallUninstall = sharedPreferences
                .getBoolean(Constants.INSTALL_UNINSTALL_LOCK, false);
        boolean isUnlockClearDataWindow = sharedPreferences
                .getBoolean(Constants.CLEAR_DATA_WINDOW_LOCK, false);
        Log.d(TAG, "isInstallOrUninstall:" + isInstallOrUninstall);
        Log.d(TAG, "isAccessibilityWindow:" + isAccessibilityWindow);

        Log.d(TAG, "isInstallOrUnistallAppEnabled:" + isInstallOrUnistallAppEnabled);
        Log.d(TAG, "isRecentAppsLockEnabled:" + isRecentAppsLockEnabled);
        Log.d(TAG, "isUnlockAccessibilityWindow:" + isUnlockAccessibilityWindow);
        Log.d(TAG, "isUnlockRecentApp:" + isUnlockRecentApp);
        Log.d(TAG, "isUnlockInstallUninstall:" + isUnlockInstallUninstall);
        Log.d(TAG, "isClearDataWindow:" + isClearDataWindow);
        Log.d(TAG, "topPackageName:" + packageName + ", topActivity:" + topActivity);
        Log.d(TAG, "isCancelActiveWindow:" + isCancelActiveWindow);
        isInstallOrUnistallAppEnabled = (INSTALL_UNINSTALL_PACKAGE.equals(packageName) || isInstallOrUninstall)
                && isInstallOrUnistallAppEnabled;

        if (!isInstallOrUnistallAppEnabled && !AppsLockActivity.isIgnorePackage(packageName)) {
            relockInstallUninstall();
        }

        if(!isClearDataWindow){
            relockClearDataWindow();
        }

        if (isInstallOrUnistallAppEnabled && !isUnlockInstallUninstall) {
            mLastInstallorUninstallPackageName = packageName;
            Message msg1 = mServiceHandler.obtainMessage(CHECK_INSTALL_UNINSTALL);
            mServiceHandler.sendMessage(msg1);
        } else if (isAccessibilityWindow) {
            if (!isUnlockAccessibilityWindow) {
                Message msg1 = mServiceHandler.obtainMessage(CHECK_ACCESSIBILITY_WINDOW);
                mServiceHandler.sendMessage(msg1);
            }
        } else if ((RECENT_APPS_ACTIVITY.equals(topActivity)
                || RECENTS_APPS_ACTIVITY.equals(topActivity)) && isRecentAppsLockEnabled) {
            if (!isUnlockRecentApp) {
                Message msg1 = mServiceHandler.obtainMessage(CHECK_RECENT_APPS);
                mServiceHandler.sendMessage(msg1);
            }
        } else if(isClearDataWindow) {
            if(!isUnlockClearDataWindow){
                Message msg1 = mServiceHandler.obtainMessage(CHECK_CLEAR_DATA_WINDOW);
                mServiceHandler.sendMessage(msg1);
            }
        } else if(isCancelActiveWindow) {
                Message msg1 = mServiceHandler.obtainMessage(CHECK_CANCEL_ACTIVE_WINDOW);
                mServiceHandler.sendMessage(msg1);
        } else {
            if (AppsLockActivity.isIgnorePackage(packageName)) {
                Log.d(TAG, "It is ignored package:" + packageName);
            } else if (AccessibilityUtils.hasLockedApp()) {
                Message msg2 = mServiceHandler.obtainMessage(CHECK_APP_LOCK);
                msg2.setData(data);
                mServiceHandler.sendMessage(msg2);
            } else {
                Log.d(TAG, "There is no locked app.");
            }
        }
    }

    @Override
    public void onServiceConnected() {
        Log.i(TAG, "onServiceConnected()");

        mDbo = new SmartappDBOperator(getApplicationContext());

        AccessibilityUtils.setObserverService(this);
        AccessibilityUtils.setHasLockedApp(mDbo.hasLockedApp());

        HandlerThread thread = new HandlerThread(TAG,
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        if (null != mServiceLooper) {
            mServiceHandler = new ServiceHandler(mServiceLooper);
        }
        mSharedPreferences = getBaseContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        relockAccessibilityWindow();

        TokenUtils.initRelatedPackage(getApplicationContext());

        AccessibilityUtils.updateInputMehodList(getApplicationContext());

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "onReceive: action:" + action);
                if (mSharedPreferences == null) {
                    Log.e(TAG, "onReceive mSharedPreferences is null");
                    return;
                }

                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    relockAccessibilityWindow();
                    relockRecentApp();
                    relockInstallUninstall();
                }else if(Intent.ACTION_SCREEN_ON.equals(action)){
                    AccessibilityUtils.updateInputMehodList(getApplicationContext());
                }

                boolean isRelockAfterScreenOff = mSharedPreferences
                        .getBoolean(SettingsActivity.KEY_APPLOCK_RELOCK_AFTER_SCREEN_OFF, false);
                Log.d(TAG, "onReceive: isRelockAfterScreenOff:" + isRelockAfterScreenOff);
                if (!isRelockAfterScreenOff) {
                    return;
                }

                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    mDbo.lockAllApp();
                    Log.d(TAG, "onReceive: lock all app done.");
                    mScreenOffPackageName = mLastTopPackageName;
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    Log.d(TAG, "mScreenOffPackageName :" + mScreenOffPackageName);
                    Log.d(TAG, "mLastTopPackageName:" + mLastTopPackageName);
                    if (mScreenOffPackageName != null
                            && mScreenOffPackageName.equals(mLastTopPackageName)) {
                        if (mScreenOffPackageName.equals(mLastInstallorUninstallPackageName)) {
                            if (!DrawPwdActivity.getWorkingFlag()
                                    && SettingsActivity.isEnabledLock()) {
                                intent = new Intent(getBaseContext(), DrawPwdActivity.class);
                                intent.putExtra(Constants.LOCK_TYPE, Constants.INSTALL_UNINSTALL_LOCK);
                                intent.putExtra(Constants.PACKAGE_NAME, mScreenOffPackageName);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                getApplication().startActivity(intent);
                            } else {
                                Log.d(TAG, "DrawPwdActivity is working");
                            }
                        } else {
                        mLastTopPackageName = "";
                        checkAppLock(mScreenOffPackageName);
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "onInterrupt()");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind()");
        AccessibilityUtils.setObserverService(null);
        mServiceLooper.quit();
        unregisterReceiver(mReceiver);
        return false;
    }

    /**
     * check the top package name,
     * record the last top package name
     * if locked app, check the unlock status
     *
     * @param packageName String
     */
    private void checkAppLock(String packageName) {
        Log.d(TAG, "checkAppLock begin:" + packageName);
        boolean isRelockAfterScreenOff = mSharedPreferences
                .getBoolean(SettingsActivity.KEY_APPLOCK_RELOCK_AFTER_SCREEN_OFF, false);
        Log.d(TAG, "checkAppLock isRelockAfterScreenOff:" + isRelockAfterScreenOff);
        Log.d(TAG, "checkAppLock mLastTopPackageName:" + mLastTopPackageName);
        Log.d(TAG, "checkAppLock packageName:" + packageName);

        if (mLastTopPackageName == null || "".equals(mLastTopPackageName)) {
            mLastTopPackageName = packageName;
        } else if (!mLastTopPackageName.equals(packageName)) {
            if (mDbo.isLockedApp(mLastTopPackageName)) {
                if (!isRelockAfterScreenOff) {
                    mDbo.updateAppUnlockStatus(mLastTopPackageName, false);
                    Log.d(TAG, "checkAppLock lock " + mLastTopPackageName);
                }
            }
            mLastTopPackageName = packageName;
        }

        if (mDbo.isLockedApp(packageName)) {
            Log.d(TAG, "checkAppLock is locked app");
            if (!mDbo.isUnlocked(packageName)) {
                Log.d(TAG, "checkAppLock start DrawPwdActivity");
                startDrawPwdActivity(packageName);
            } else {
                Log.d(TAG, "checkAppLock is unlocked");
            }
        }
    }

    private void startDrawPwdActivity(String packageName) {
        Log.d(TAG, "startDrawPwdActivity package:" + packageName);
        if(SettingsActivity.isEnabledLock()) {
            Intent intent = new Intent(getBaseContext(), DrawPwdActivity.class);
            intent.putExtra(Constants.LOCK_TYPE, Constants.APP_LOCK);
            intent.putExtra(Constants.PACKAGE_NAME, packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplication().startActivity(intent);
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            Intent intent;
            String packageName = data.getString(PACKAGE_NAME_KEY);
            switch (msg.what) {
                case CHECK_INSTALL_UNINSTALL:
                    Log.d(TAG, "do CHECK_INSTALL_UNINSTALL");
                    if (!DrawPwdActivity.getWorkingFlag()
                            && SettingsActivity.isEnabledLock()) {
                        intent = new Intent(getBaseContext(), DrawPwdActivity.class);
                        intent.putExtra(Constants.LOCK_TYPE, Constants.INSTALL_UNINSTALL_LOCK);
                        intent.putExtra(Constants.PACKAGE_NAME, packageName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(intent);
                    } else {
                        Log.d(TAG, "DrawPwdActivity is working");
                    }
                    break;
                case CHECK_RECENT_APPS:
                    Log.d(TAG, "do CHECK_RECENT_APPS");
                    if (!DrawPwdActivity.getWorkingFlag() && SettingsActivity.isEnabledLock()) {
                        intent = new Intent(getBaseContext(), DrawPwdActivity.class);
                        intent.putExtra(Constants.LOCK_TYPE, Constants.RECENT_APP_LOCK);
                        intent.putExtra(Constants.PACKAGE_NAME, packageName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(intent);
                    } else {
                        Log.d(TAG, "DrawPwdActivity is working");
                    }
                    break;
                case CHECK_ACCESSIBILITY_WINDOW:
                    Log.d(TAG, "do CHECK_ACCESSIBILITY_WINDOW");
                    if (!DrawPwdActivity.getWorkingFlag() && SettingsActivity.isEnabledLock()) {
                        intent = new Intent(getBaseContext(), DrawPwdActivity.class);
                        intent.putExtra(Constants.LOCK_TYPE, Constants.ACCESSIBILITY_WINDOW_LOCK);
                        intent.putExtra(Constants.PACKAGE_NAME, packageName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(intent);
                    } else {
                        Log.d(TAG, "DrawPwdActivity is working");
                    }
                    break;
                case CHECK_CLEAR_DATA_WINDOW:
                    Log.d(TAG, "do CHECK_CLEAR_DATA_WINDOW");
                    if (!DrawPwdActivity.getWorkingFlag() && SettingsActivity.isEnabledLock()) {
                        intent = new Intent(getBaseContext(), DrawPwdActivity.class);
                        intent.putExtra(Constants.LOCK_TYPE, Constants.CLEAR_DATA_WINDOW_LOCK);
                        intent.putExtra(Constants.PACKAGE_NAME, packageName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(intent);
                    } else {
                        Log.d(TAG, "DrawPwdActivity is working");
                    }
                    break;
                case CHECK_CANCEL_ACTIVE_WINDOW:
                    Log.d(TAG, "do CHECK_CANCEL_ACTIVE_WINDOW");
                    if (!DrawPwdActivity.getWorkingFlag()) {
                        intent = new Intent(getBaseContext(), DrawPwdActivity.class);
                        intent.putExtra(Constants.LOCK_TYPE, Constants.CANCEL_ACTIVE_WINDOW_LOCK);
                        intent.putExtra(Constants.PACKAGE_NAME, packageName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(intent);
                    } else {
                        Log.d(TAG, "DrawPwdActivity is working");
                    }
                    break;
                case CHECK_APP_LOCK:
                    checkAppLock(packageName);
                    break;
                default:
                    break;
            }
        }
    }

    private void relockAccessibilityWindow() {
        mSharedPreferences = getBaseContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Constants.ACCESSIBILITY_WINDOW_LOCK, false);
        editor.commit();
    }

    private void relockRecentApp() {
        mSharedPreferences = getBaseContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Constants.RECENT_APP_LOCK, false);
        editor.commit();
    }

    private void relockInstallUninstall() {
        mSharedPreferences = getBaseContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Constants.INSTALL_UNINSTALL_LOCK, false);
        editor.commit();
    }

    private void relockClearDataWindow() {
        mSharedPreferences = getBaseContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        Editor editor = mSharedPreferences.edit();
        editor.putBoolean(Constants.CLEAR_DATA_WINDOW_LOCK, false);
        editor.commit();
    }

}
