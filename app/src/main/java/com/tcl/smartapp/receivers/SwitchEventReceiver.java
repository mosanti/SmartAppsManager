package com.tcl.smartapp.receivers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;

import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.tcl.smartapp.R;
import com.tcl.smartapp.SettingsActivity;
import com.tcl.smartapp.SmartApplication;
import com.tcl.smartapp.SystemLockActivity;
import com.tcl.smartapp.TtsSettings;
import com.tcl.smartapp.token.DrawPwdActivity;
import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.AccessibilityUtils;
import com.tcl.smartapp.utils.Constants;

/**
 * Created on 16-4-26.
 */
public class SwitchEventReceiver extends BroadcastReceiver {
    private static final String TAG = "SwitchEventReceiver";
    private static final String ACTION_WIFI_STATE_CHANGED = "android.net.wifi.WIFI_STATE_CHANGED";
    private static final String ACTION_BT_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";
    private static final String ACTION_DC_STATE_CHANGED = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
    public static final String ACTION_FORBID_SWITCHER_STATE_CHANGED = "android.forbid.switcher.state";

    public SwitchEventReceiver() {
    }

    /**
     * get contact name from contact list.
     *
     * @param context
     * @param number
     * @return contact name
     */
    private String getContactName(Context context, String number) {
        String contactName = "";
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.NUMBER + "=?", new String[]{number}, null);
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        }
        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contactName;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String action = intent.getAction();
        Log.d(TAG, "onReceive action:" + action);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isWifiLockEnabled = sharedPreferences.getBoolean(SystemLockActivity.KEY_WIFI_LOCK, false);
        boolean isBTLockEnabled = sharedPreferences.getBoolean(SystemLockActivity.KEY_BT_LOCK, false);
        boolean isIncomingCallLockEnabled = sharedPreferences.getBoolean(SystemLockActivity.KEY_INCOMING_CALL_LOCK, false);
        boolean isUnlockInComingPhoneLock = sharedPreferences.getBoolean(Constants.INCOMING_PHONE_LOCK, false);

        if (ACTION_WIFI_STATE_CHANGED.equals(action)) {
            if (!isWifiLockEnabled || !SettingsActivity.isEnabledLock()) {
                return;
            }

            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);
            Log.d(TAG, "ACTION_WIFI_STATE_CHANGED: state=" + state);
            /**
             * test found no WIFI_STATE_ENABLING received,
             * so we only check WIFI_STATE_ENABLED and WIFI_STATE_DISABLED
             * */
            if (state == WifiManager.WIFI_STATE_ENABLING
                    || state == WifiManager.WIFI_STATE_DISABLING) {
                //do nothing
                return;
            }
            Log.d(TAG, "isWifiLockEnabled:" + isWifiLockEnabled + ", DrawPwdActivity:"
                    + DrawPwdActivity.getWorkingFlag());

            if (DrawPwdActivity.getWorkingFlag()) {
                return;
            }
            boolean wifiEnable;
            boolean isUnlockWifiOpen = TokenUtils.isUnlockWifiOpen(context);
            boolean isUnlockWifiClose = TokenUtils.isUnlockWifiClose(context);
            Log.d(TAG, "isUnlockWifiOpen:" + isUnlockWifiOpen + ", isUnlockWifiClose:"
                    + isUnlockWifiClose);
            if (state == WifiManager.WIFI_STATE_ENABLED) {
                /**
                 * in DrawPwdActivity, we will do turn on wifi after passed pwd
                 * */
                if (isUnlockWifiOpen) {
                    return;
                }
                wifiEnable = true;
            } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                /**
                 * in DrawPwdActivity, we will do turn off wifi after passed pwd
                 * */
                if (isUnlockWifiClose) {
                    return;
                }
                wifiEnable = false;
            } else {
                Log.e(TAG, "unknown wifi state:" + state);
                return;
            }

            Intent wifiIntent = new Intent(context, DrawPwdActivity.class);
            wifiIntent.putExtra(Constants.LOCK_TYPE, Constants.WIFI_LOCK);
            wifiIntent.putExtra(Constants.ENABLE_FALG, wifiEnable);
            wifiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.d(TAG, "wifi,startActivity: DrawPwdActivity");
            context.startActivity(wifiIntent);

        } else if (ACTION_BT_STATE_CHANGED.equals(action)) {
            if (!isBTLockEnabled || !SettingsActivity.isEnabledLock()) {
                return;
            }
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothDevice.ERROR);
            Log.d(TAG, "BT state:" + state);

            if (state == BluetoothAdapter.STATE_ON
                    || state == BluetoothAdapter.STATE_OFF) {
                //do nothing
                return;
            }
            Log.d(TAG, "isBTLockEnabled:" + isBTLockEnabled + ", DrawPwdActivity:"
                    + DrawPwdActivity.getWorkingFlag());

            if (DrawPwdActivity.getWorkingFlag()) {
                return;
            }

            boolean btEnable;
            boolean isUnlockBtOpen = TokenUtils.isUnlockBtOpen(context);
            boolean isUnlockBtClose = TokenUtils.isUnlockBtClose(context);
            Log.d(TAG, "isUnlockBtOpen:" + isUnlockBtOpen + ", isUnlockBtClose:"
                    + isUnlockBtClose);
            if (state == BluetoothAdapter.STATE_TURNING_ON) {
                /**
                 * in DrawPwdActivity, we will do turn on bt after passed pwd
                 * */
                if (isUnlockBtOpen) {
                    return;
                }
                btEnable = true;
            } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                /**
                 * in DrawPwdActivity, we will do turn off bt after passed pwd
                 * */
                if (isUnlockBtClose) {
                    return;
                }
                btEnable = false;
            } else {
                Log.e(TAG, "unknown bt state:" + state);
                return;
            }

            Intent btIntent = new Intent(context, DrawPwdActivity.class);
            btIntent.putExtra(Constants.LOCK_TYPE, Constants.BT_LOCK);
            btIntent.putExtra(Constants.ENABLE_FALG, btEnable);
            btIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.d(TAG, "Bluetooth, startActivity: DrawPwdActivity");
            context.startActivity(btIntent);

        } else if (ACTION_FORBID_SWITCHER_STATE_CHANGED.equals(action)) {
            boolean flag = intent.getBooleanExtra(Constants.ENABLE_FALG, false);
            DrawPwdActivity.setWorkingFlag(flag);
        } else if (ACTION_DC_STATE_CHANGED.equals(action)) {
            Log.d(TAG, "ACTION_DC_STATE_CHANGED");
        } else if (ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            String name = getContactName(context, incomingNumber);
            SharedPreferences ttsSharedPreferences = context.getSharedPreferences(TtsSettings.PREFER_NAME, Context.MODE_PRIVATE);
            boolean isTtsEnabled = ttsSharedPreferences.getBoolean(TtsSettings.KEY_TTS_ENABLE, false);
            SmartApplication smartApplication = (SmartApplication) context.getApplicationContext();
            if (isTtsEnabled && TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                String ttsNumber = String.format(context.getString(R.string.incomingcall_text), incomingNumber);
                String ttsName = String.format(context.getString(R.string.incomingcall_text), name);
                String content = TextUtils.isEmpty(name) ? ttsNumber : ttsName;
                smartApplication.mTtsUtils.play(content);
                Log.d(TAG, "isTtsEnabled = " + isTtsEnabled + ",content = " + content);
            } else if (isTtsEnabled && TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                smartApplication.mTtsUtils.stop();
            }
            Log.d(TAG, "ACTION_PHONE_STATE_CHANGED: incomingNumber = " + incomingNumber
                    + ",name =" + name + ", state =" + state);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                // Do incoming call lock handle.
                Log.d(TAG, "isIncomingCallLockEnabled:" + isIncomingCallLockEnabled
                        + ", DrawPwdActivity:" + DrawPwdActivity.getWorkingFlag());
                if (isIncomingCallLockEnabled && !DrawPwdActivity.getWorkingFlag()
                        && !isUnlockInComingPhoneLock) {
                    AccessibilityUtils.setIncomingPhone(true);
                }
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                AccessibilityUtils.setIncomingPhone(false);
            }
        } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            Log.d(TAG, "ACTION_PACKAGE_ADDED");
            AccessibilityUtils.updateInputMehodList(context);
        }
    }
}
