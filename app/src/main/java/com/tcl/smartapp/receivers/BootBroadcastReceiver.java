package com.tcl.smartapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.tcl.smartapp.SystemLockActivity;
import com.tcl.smartapp.db.SmartappDBOperator;
import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.AccessibilityUtils;


public class BootBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "BootBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isInstallOrUnistallAppEnabled = sharedPreferences.getBoolean(SystemLockActivity.KEY_INSTALL_UNINSTALL_APPS, false);
        boolean isIncomingCallLockEnabled = sharedPreferences.getBoolean(SystemLockActivity.KEY_INCOMING_CALL_LOCK, false);
        boolean isRecentAppsLockEnabled = sharedPreferences.getBoolean(SystemLockActivity.KEY_RECENT_APPS_LOCK, false);
        Log.d(TAG, "BootBroadcastReceiver: onReceive(): isInstallOrUnistallAppEnabled = " + isInstallOrUnistallAppEnabled
                + ",isIncomingCallLockEnabled = " + isIncomingCallLockEnabled + ",isRecentAppsLockEnabled=" + isRecentAppsLockEnabled);

        SmartappDBOperator dbo = new SmartappDBOperator(context);
        AccessibilityUtils.setHasLockedApp(dbo.hasLockedApp());

        AccessibilityUtils.updateInputMehodList(context);

        TokenUtils.initRelatedPackage(context);
    }

}
