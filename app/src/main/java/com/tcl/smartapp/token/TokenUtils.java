package com.tcl.smartapp.token;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.tcl.smartapp.PasswordSettingsActivity;
import com.tcl.smartapp.R;
import com.tcl.smartapp.SettingsActivity;
import com.tcl.smartapp.SmartApplication;
import com.tcl.smartapp.db.SmartappDBOperator;
import com.tcl.smartapp.utils.AccessibilityUtils;
import com.tcl.smartapp.utils.Constants;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Created  on 5/7/16.
 */
public class TokenUtils {
    private static final String TAG = "TokenUtils";
    public static final int TAKE_PICTURE = 1;
    public static final String ACTION_IMAGE_CAPTURE = "android.media.action.IMAGE_CAPTURE";
    public static final int STATUS_BAR_DISABLE_HOME = 0x00200000;
    public static final int STATUS_BAR_DISABLE_RECENT = 0x01000000;
    public static final int STATUS_BAR_DISABLE_BACK = 0x00400000;
    public static final int STATUS_BAR_DISABLE_EXPAND = 0x00010000;
    public static final int UPDATE_UNlOCK_FAIL_CONTENT = 0x00000100;
    public static final int UPDATE_UNlOCK_SUCCESS_CONTENT = 0x00001000;
    public static final int UPDATE_UNlOCK_ERROR_CONTENT = 0x00010000;
    private static String[][] mRelatedPackageName = null;

    public static void takePic(Intent data) {
        Bitmap bm = (Bitmap) data.getExtras().get("data");
        String picName = "123456.jpg";
        File myCaptureFile = new File(Constants.PIC_SAVE_PATH + "/" + picName);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
            /* 采用压缩转档方法 */
            bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);

            /* 调用flush()方法，更新BufferStream */
            bos.flush();

            /* 结束OutputStream */
            bos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public static boolean lockTypeHandleBeforeUnlock(Context context, Intent intent) {
        String lockType = intent.getExtras().getString(Constants.LOCK_TYPE);
        boolean enableFlag = intent.getBooleanExtra(Constants.ENABLE_FALG, false);

        if (Constants.APP_LOCK.equals(lockType)
                || Constants.SELF_LOCK.equals(lockType)
                || Constants.RES_LOCK.equals(lockType)
                || Constants.SYS_LOCK.equals(lockType)
                || Constants.CHANGE_LOCK_STYLE.equals(lockType)
                || Constants.ACCESSIBILITY_WINDOW_LOCK.equals(lockType)
                || Constants.INCOMING_PHONE_LOCK.equals(lockType)
                || Constants.RECENT_APP_LOCK.equals(lockType)
                || Constants.INSTALL_UNINSTALL_LOCK.equals(lockType)
                || Constants.RETRIEVE_PASSWORD.equals(lockType)
                || Constants.CANCEL_ACTIVE_WINDOW_LOCK.equals(lockType)
                || lockType.equals(Constants.UNINSTALL_LOCK)
                || Constants.CLEAR_DATA_WINDOW_LOCK.equals(lockType)) {
            //do nothing
        } else if (Constants.WIFI_LOCK.equals(lockType)) {
            //fallback wifi state
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(!enableFlag);
        } else if (Constants.BT_LOCK.equals(lockType)) {
            //fallback bt state
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (enableFlag) {
                bluetoothAdapter.disable();
            } else {
                bluetoothAdapter.enable();
            }
        } else {
            Log.e(TAG, "unknown lockType!" + lockType);
            return false;
        }

        return true;
    }

    public static void lockTypeHandleAfterUnlock(Context context, Intent intent) {
        String lockType = intent.getExtras().getString(Constants.LOCK_TYPE);
        String packageName = intent.getExtras().getString(Constants.PACKAGE_NAME);
        boolean enableFlag = intent.getBooleanExtra(Constants.ENABLE_FALG, false);
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        if (lockType.equals(Constants.CHANGE_LOCK_STYLE)) {
            Intent i = new Intent(context.getApplicationContext(),
                    PasswordSettingsActivity.class);
            context.startActivity(i);
        } else if (lockType.equals(Constants.RES_LOCK) || lockType.equals(Constants.SYS_LOCK)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(Constants.SYS_LOCK_RESULT, packageName);
            editor.commit();
        } else if (lockType.equals(Constants.APP_LOCK)) {
            SmartappDBOperator dbo = new SmartappDBOperator(context);
            dbo.updateAppUnlockStatus(packageName, true);
        } else if (Constants.WIFI_LOCK.equals(lockType)) {
            if (enableFlag) {
                unlockWifiOpen(context);
                relockWifiClose(context);
            } else {
                unlockWifiClose(context);
                relockWifiOpen(context);
            }
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(enableFlag);
        } else if (Constants.BT_LOCK.equals(lockType)) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (enableFlag) {
                unlockBtOpen(context);
                relockBtClose(context);
                bluetoothAdapter.enable();
            } else {
                unlockBtClose(context);
                relockBtOpen(context);
                bluetoothAdapter.disable();
            }
        } else if (lockType.equals(Constants.RETRIEVE_PASSWORD)) {
            Intent i = new Intent(context.getApplicationContext(), RetrievePasswordActivity.class);
            context.startActivity(i);
        } else if (Constants.INCOMING_PHONE_LOCK.equals(lockType)
                || Constants.RECENT_APP_LOCK.equals(lockType)
                || Constants.INSTALL_UNINSTALL_LOCK.equals(lockType)
                || Constants.ACCESSIBILITY_WINDOW_LOCK.equals(lockType)
                || Constants.SELF_LOCK.equals(lockType)
                || Constants.CANCEL_ACTIVE_WINDOW_LOCK.equals(lockType)
                || Constants.CLEAR_DATA_WINDOW_LOCK.equals(lockType)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(lockType, true);
            editor.commit();
        }else if (lockType.equals(Constants.UNINSTALL_LOCK)) {
            SmartApplication.deActiveDeviceAdmin();
            }
        else {
            Log.e(TAG, "unknown lockType:" + lockType);
        }

        return;
    }

    /**
     * set lock screen operations status
     *
     * @param enable :true enable LockScreenOperater
     *               false disable LockScreenOperater
     */
    public static synchronized void setLockScreenOperaterStatus(Context context, boolean enable) {
        try {
            Object service = context.getSystemService("statusbar");

            if (service != null) {
                Class clsStatusBarManager = Class.forName("android.app.StatusBarManager");
                int what = STATUS_BAR_DISABLE_HOME | STATUS_BAR_DISABLE_RECENT |
                        STATUS_BAR_DISABLE_BACK | STATUS_BAR_DISABLE_EXPAND;
                if (enable) {
                    what = 0;
                }
                Method disableMethod = clsStatusBarManager.getMethod("disable", int.class);
                Log.d(TAG, "setLockScreenOperaterStatus: what = " + what);
                disableMethod.invoke(service, what);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * set status bar translucent.
     *
     */
    public static void setStatusBarTranslucent(Activity activity) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    public static Drawable getAppIcon(Context context, String lockType, String packageName) {
        Log.d(TAG, "setAppIcon: packageName = " + packageName);
        Drawable appIcon = null;
        if (lockType.equals(Constants.APP_LOCK)) {
            ApplicationInfo appInfo = null;
            try {
                appInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (appInfo != null) {
                appIcon = appInfo.loadIcon(context.getPackageManager());
            }
            Drawable RelatedIcon = getIconFromRelatedPackage(packageName, context);
            if (RelatedIcon != null) {
                appIcon = RelatedIcon;
            }
        } else if (lockType.equals(Constants.BT_LOCK)) {
            appIcon = context.getResources().getDrawable(R.drawable.ic_bluetooth_big);
        } else if (lockType.equals(Constants.WIFI_LOCK)) {
            appIcon = context.getResources().getDrawable(R.drawable.ic_wifi_big);
        } else if (lockType.equals(Constants.INCOMING_PHONE_LOCK)) {
            appIcon = context.getResources().getDrawable(R.drawable.ic_incoming_call_big);
        } else if (lockType.equals(Constants.INSTALL_UNINSTALL_LOCK)) {
            appIcon = context.getResources().getDrawable(R.drawable.ic_uninstall);
        } else if (lockType.equals(Constants.RECENT_APP_LOCK)) {
            appIcon = context.getResources().getDrawable(R.drawable.ic_recent_tasks);
        } else {
            appIcon = context.getResources().getDrawable(R.drawable.ic_launcher);
        }
        return appIcon;
    }

    private static Drawable getIconFromRelatedPackage(String packageName, Context context) {
        if (packageName == null || mRelatedPackageName == null) {
            return null;
        }
        Drawable appIcon = null;
        for (int i = 0; i < mRelatedPackageName.length; i++) {
            if (packageName.equals(mRelatedPackageName[i][1])) {
                ApplicationInfo appInfo = null;
                try {
                    appInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (appInfo != null) {
                    appIcon = appInfo.loadIcon(context.getPackageManager());
                }
            }
        }
        return appIcon;
    }

    public static void initRelatedPackage(Context context) {
        if (mRelatedPackageName == null) {
            String[] orangePackageName = context.getResources().getStringArray(R.array.orange_package_name_arrays);
            String[] relatedPackageName = context.getResources().getStringArray(R.array.related_package_name_arrays);
            int len = orangePackageName.length;
            if (orangePackageName.length != relatedPackageName.length) {
                len = (orangePackageName.length > relatedPackageName.length)
                        ? relatedPackageName.length : orangePackageName.length;
            }
            mRelatedPackageName = new String[len][2];
            for (int i = 0; i < len; i++) {
                mRelatedPackageName[i][0] = orangePackageName[i];
                mRelatedPackageName[i][1] = relatedPackageName[i];
            }
        }
    }
    public static void startUninstallLock(Context context) {
        Intent intent;
        if (!DrawPwdActivity.getWorkingFlag()) {
            intent = new Intent(context, DrawPwdActivity.class);
            intent.putExtra(Constants.LOCK_TYPE, Constants.UNINSTALL_LOCK);
            intent.putExtra(Constants.PACKAGE_NAME, Constants.MY_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
    public static String[][] getRelatedPackageList() {
        return mRelatedPackageName;
    }

    public static void startSelfLock(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        //already set password
        boolean isLockSettingsSuccess = sp.getBoolean(Constants.KEY_LOCK_SUCCESS, false);
        boolean unLocked = sp.getBoolean(Constants.SELF_LOCK, false);
        Intent intent;
        if (isLockSettingsSuccess
                && !DrawPwdActivity.getWorkingFlag()
                && !unLocked
                && SettingsActivity.isEnableSelfLock()) {
            intent = new Intent(context, DrawPwdActivity.class);
            intent.putExtra(Constants.LOCK_TYPE, Constants.SELF_LOCK);
            intent.putExtra(Constants.PACKAGE_NAME, Constants.MY_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * relock self
     */
    public static void relockSelf(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.SELF_LOCK, false);
        editor.commit();
    }

    /**
     * set status bar color.
     *
     */
    public static void setStatusBarColor(Activity activity) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(activity.getColor(R.color.colorPrimary));
    }

    public static void relockWifiOpen(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.UNLOCKED_WIFI_OPEN, false);
        editor.commit();
    }

    public static void unlockWifiOpen(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.UNLOCKED_WIFI_OPEN, true);
        editor.commit();
    }

    public static boolean isUnlockWifiOpen(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.UNLOCKED_WIFI_OPEN, false);
    }

    public static void relockWifiClose(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.UNLOCKED_WIFI_CLOSE, false);
        editor.commit();
    }

    public static void unlockWifiClose(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.UNLOCKED_WIFI_CLOSE, true);
        editor.commit();
    }

    public static boolean isUnlockWifiClose(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.UNLOCKED_WIFI_CLOSE, false);
    }

    public static void relockBtOpen(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.UNLOCKED_BT_OPEN, false);
        editor.commit();
    }

    public static void unlockBtOpen(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.UNLOCKED_BT_OPEN, true);
        editor.commit();
    }

    public static boolean isUnlockBtOpen(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.UNLOCKED_BT_OPEN, false);
    }

    public static void relockBtClose(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.UNLOCKED_BT_CLOSE, false);
        editor.commit();
    }

    public static void unlockBtClose(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constants.UNLOCKED_BT_CLOSE, true);
        editor.commit();
    }

    public static boolean isUnlockBtClose(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(Constants.UNLOCKED_BT_CLOSE, false);
    }

}
