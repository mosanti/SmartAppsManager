package com.tcl.smartapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.AppOpsManagerCompat;

/**
 * Created  on 4/25/16.
 */

public class EyeProtect {
    public static final String PREF_EYE_PROTECT = "eye_protect";

    public static final String PREF_FILTER_TRANSPARENCY = "filter_transparency";
    public static final String PREF_FILTER_RED = "filter_red";
    public static final String PREF_FILTER_GREEN = "filter_green";
    public static final String PREF_FILTER_BLUE = "filter_blue";

    public static final int MAX_ALPHA = 0xDF;

    public static final int DEFAULT_FILTER_TRANSPARENCY = 85;
    public static final int DEFAULT_FILTER_RED = 255;
    public static final int DEFAULT_FILTER_GREEN = 191;
    public static final int DEFAULT_FILTER_BLUE = 0;


    public static void startEyeProtect(Context context) {
        if (checkOrRequestSystemAlertPermission(context))
            context.startService(new Intent(context, EyeProtectService.class));
    }

    public static void stopEyeProtect(Context context) {
        context.stopService(new Intent(context, EyeProtectService.class));
    }

    public static int getFilterColor(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int transparency = sharedPreferences.getInt(PREF_FILTER_TRANSPARENCY, DEFAULT_FILTER_TRANSPARENCY);
        int red = sharedPreferences.getInt(PREF_FILTER_RED, DEFAULT_FILTER_RED);
        int green = sharedPreferences.getInt(PREF_FILTER_GREEN, DEFAULT_FILTER_GREEN);
        int blue = sharedPreferences.getInt(PREF_FILTER_BLUE, DEFAULT_FILTER_BLUE);
        int alpha = transparencyToAlpha(transparency);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int transparencyToAlpha(int transparency) {
        return (int) ((255 - (transparency / 100f * 255)) / 255f * MAX_ALPHA);
    }

    public static boolean checkSystemAlertPermission(Context context) {
        boolean allowAlert = Build.VERSION.SDK_INT < 23;
        if (!allowAlert) {
            try {
                allowAlert = AppOpsManagerCompat.noteProxyOp(context,
                        AppOpsManagerCompat.permissionToOp(Manifest.permission.SYSTEM_ALERT_WINDOW), context.getPackageName())
                        == AppOpsManagerCompat.MODE_ALLOWED;
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        return allowAlert;
    }

    private static boolean checkOrRequestSystemAlertPermission(Context context) {
        boolean allowAlert = Build.VERSION.SDK_INT < 23;
        if (!allowAlert) {
            try {
                allowAlert = AppOpsManagerCompat.noteProxyOp(context,
                        AppOpsManagerCompat.permissionToOp(Manifest.permission.SYSTEM_ALERT_WINDOW), context.getPackageName())
                        == AppOpsManagerCompat.MODE_ALLOWED;
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            if (!allowAlert) {
                Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION",
                        Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return allowAlert;
    }
}
