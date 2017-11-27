package com.tcl.smartapp;

import com.slidingmenu.lib.SlidingMenu;
import com.tcl.smartapp.service.BackToEncryptedService;
import com.tcl.smartapp.service.BackgroundService;
import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.AccessibilityUtils;
import com.tcl.smartapp.utils.AppUtil;
import com.tcl.smartapp.utils.MySlidingMenu;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.KeyEvent;

import android.widget.Toast;

public class SystemLockActivity extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = "SystemLockActivity";

    private Context context;
    public static SystemLockActivity mSystemLockInstance;
    private MySlidingMenu mMySlidingMenu;

    private SwitchPreference mInstallOrUninstallpref;
    public static final String KEY_INSTALL_UNINSTALL_APPS = "key_install_unistall_lock";
    private SwitchPreference mIncomingCallpref;
    public static final String KEY_INCOMING_CALL_LOCK = "key_incoming_call_lock";
    private SwitchPreference mRecentsAppLockpref;
    public static final String KEY_RECENT_APPS_LOCK = "key_recent_tasks_lock";
    private SwitchPreference mWifipref;
    public static final String KEY_WIFI_LOCK = "key_wifi_lock";
    private SwitchPreference mBtpref;
    public static final String KEY_BT_LOCK = "key_bt_lock";
    private static final String[] REQUEST_PHONE_PERMISSIONS = {Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.system_lock_preference);

        mSystemLockInstance = this;
        context = this;
        mMySlidingMenu = new MySlidingMenu(context);
        mMySlidingMenu.getSlidingMenu().attachToActivity(this, SlidingMenu.SLIDING_WINDOW);

        mInstallOrUninstallpref = (SwitchPreference) findPreference(KEY_INSTALL_UNINSTALL_APPS);
        mInstallOrUninstallpref.setOnPreferenceChangeListener(this);
        mInstallOrUninstallpref.setOnPreferenceClickListener(this);

        mIncomingCallpref = (SwitchPreference) findPreference(KEY_INCOMING_CALL_LOCK);
        mIncomingCallpref.setOnPreferenceChangeListener(this);
        mIncomingCallpref.setOnPreferenceClickListener(this);
        if (!hasRequiredPermission(REQUEST_PHONE_PERMISSIONS)) {
            Log.d(TAG, "set Incoming Call disabled");
            mIncomingCallpref.setChecked(false);
        }

        mRecentsAppLockpref = (SwitchPreference) findPreference(KEY_RECENT_APPS_LOCK);
        mRecentsAppLockpref.setOnPreferenceChangeListener(this);
        mRecentsAppLockpref.setOnPreferenceClickListener(this);

        mWifipref = (SwitchPreference) findPreference(KEY_WIFI_LOCK);
        mWifipref.setOnPreferenceChangeListener(this);
        mBtpref = (SwitchPreference) findPreference(KEY_BT_LOCK);
        mBtpref.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMySlidingMenu.getSlidingMenu().showMenu(false);
        mMySlidingMenu.getSlidingMenu().showContent(false);

        if (AccessibilityUtils.getmObserverService() == null) {
            mInstallOrUninstallpref.setChecked(false);
            mRecentsAppLockpref.setChecked(false);
            mIncomingCallpref.setChecked(false);

            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(this);
            Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_INSTALL_UNINSTALL_APPS, false);
            editor.putBoolean(KEY_RECENT_APPS_LOCK, false);
            editor.putBoolean(KEY_INCOMING_CALL_LOCK, false);
            editor.commit();
        }
        TokenUtils.startSelfLock(this);
    }

    public MySlidingMenu getMySlidingMenu() {
        return this.mMySlidingMenu;
    }

    /**
     * check requirement permissions
     *
     * @param permissions
     * @return
     */
    protected boolean hasRequiredPermission(String[] permissions) {
        for (String permission : permissions) {
            if (checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("hasRequiredPermission", " permission =" + permission);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange: preference = " + preference);

        if (mIncomingCallpref == preference) {
            boolean isEnable = (Boolean) newValue;
            Log.d(TAG, "onPreferenceClick: mIncomingCallpref isEnable = " + isEnable);
            if (!hasRequiredPermission(REQUEST_PHONE_PERMISSIONS)) {
                Toast.makeText(SystemLockActivity.this, R.string.has_no_phone_permission, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (AccessibilityUtils.getmObserverService() == null && isEnable) {
                AccessibilityUtils.showAccessibilityPrompt(this);
                return false;
            }
        } else if (mInstallOrUninstallpref == preference) {
            boolean isEnable = (Boolean) newValue;
            Log.d(TAG, "onPreferenceClick: mInstallOrUninstallpref isEnable = " + isEnable);
            if (AccessibilityUtils.getmObserverService() == null && isEnable) {
                AccessibilityUtils.showAccessibilityPrompt(this);
                return false;
            }
        } else if (mRecentsAppLockpref == preference) {
            boolean isEnable = (Boolean) newValue;
            Log.d(TAG, "onPreferenceClick: mRecentsAppLockpref isEnable = " + isEnable);
            if (AccessibilityUtils.getmObserverService() == null && isEnable) {
                AccessibilityUtils.showAccessibilityPrompt(this);
                return false;
            }
        } else if (mWifipref == preference) {
            boolean isEnable = (Boolean) newValue;
            if (isEnable) {
                TokenUtils.relockWifiOpen(context);
                TokenUtils.relockWifiClose(context);
            }
        } else if (mBtpref == preference) {
            boolean isEnable = (Boolean) newValue;
            if (isEnable) {
                TokenUtils.relockBtOpen(context);
                TokenUtils.relockBtClose(context);
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Log.d(TAG, "onPreferenceClick: preference = " + preference);
        if (mInstallOrUninstallpref == preference || mRecentsAppLockpref == preference) {
            boolean isInstallOrUninstallEnable = mInstallOrUninstallpref.isChecked();
            Log.d(TAG, "onPreferenceClick: mInstallOrUninstallpref isEnable = " + isInstallOrUninstallEnable);
            boolean isRecentAppsEnable = mRecentsAppLockpref.isChecked();
            Log.d(TAG, "onPreferenceClick: mRecentsAppLockpref isEnable = " + isRecentAppsEnable);

        } else if (mIncomingCallpref == preference) {

            boolean isEnable = mIncomingCallpref.isChecked();
            Log.d(TAG, "onPreferenceClick: mIncomingCallpref isEnable = " + isEnable);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (AppUtil.onBackKeyDown(this, keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
