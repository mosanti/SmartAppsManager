package com.tcl.smartapp;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.iflytek.autoupdate.IFlytekUpdate;
import com.iflytek.autoupdate.IFlytekUpdateListener;
import com.iflytek.autoupdate.UpdateConstants;
import com.iflytek.autoupdate.UpdateErrorCode;
import com.iflytek.autoupdate.UpdateInfo;
import com.iflytek.autoupdate.UpdateType;
import com.tcl.smartapp.receivers.PolicyAdminReceiver;
import com.tcl.smartapp.token.DrawPwdActivity;
import com.tcl.smartapp.token.JrdChooseLockPIN;
import com.tcl.smartapp.token.PINLockActivity;
import com.tcl.smartapp.token.PINPwdActivity;
import com.tcl.smartapp.token.RetrievePasswordActivity;
import com.tcl.smartapp.token.SetupPwdActivity;
import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.Constants;
import com.tcl.smartapp.utils.FingerPrintLock;
import com.tcl.smartapp.utils.LockParams;
import com.tcl.smartapp.utils.NetworkCheck;

/**
 * Created on 4/25/16.
 */
public class SettingsActivity extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener,Preference.OnPreferenceClickListener{
    private static final String TAG = "SettingsActivity";
    public static final String KEY_APPLOCK_RELOCK_AFTER_SCREEN_OFF = "key_applock_relock_after_screen_off";
    private SwitchPreference mRelockAfterScreenOff;

    private PreferenceScreen mSetRetrievePwdMethod;
    private static final String SET_RETRIEVE_PWD_METHOD = "set_retrieve_passwrod_method";
    private PreferenceScreen mChangeLockStyle;
    private static final String CHANGE_LOCK_STYLE = "change_lock_style";
    private static int style;
    private static SharedPreferences sp;
    private Context mContext;
    public static final String KEY_DEVICE_ADMIN = "key_do_not_uninstall";
    public static final String KEY_LOCK_STATUS = "lock_status";
    public static final String KEY_WARNING_CAMERA = "key_warning_camera";
    public static final String KEY_LOCK_SELF = "lock_Self";
    private SwitchPreference mDeviceAdmin;
    private SwitchPreference mLockStatus;
    private SwitchPreference mLockSelf;
    private ComponentName mPolicyAdmin;
    private PreferenceScreen mWarningCamera;
    private DevicePolicyManager mDpm;
    private ActionBar mActionBar;
    private PreferenceScreen mAppFeaturesExplain;
    private static final String APP_FEATURES_EXPLAIN = "key_app_features_explain";
    private PreferenceScreen mVersionUpdate;
    private static final String VERSION_UPDATE = "key_version_update";
    private IFlytekUpdate updManager;
    private Toast mToast;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setings_preference);
        mContext = this;
        mRelockAfterScreenOff = (SwitchPreference)findPreference(KEY_APPLOCK_RELOCK_AFTER_SCREEN_OFF);
        mRelockAfterScreenOff.setOnPreferenceChangeListener(this);
        mChangeLockStyle = (PreferenceScreen) findPreference(CHANGE_LOCK_STYLE);
        mChangeLockStyle.setOnPreferenceClickListener(this);
        mSetRetrievePwdMethod = (PreferenceScreen) findPreference(SET_RETRIEVE_PWD_METHOD);
        mSetRetrievePwdMethod.setOnPreferenceClickListener(this);

        mDeviceAdmin = (SwitchPreference)findPreference(KEY_DEVICE_ADMIN);
        mDeviceAdmin.setOnPreferenceChangeListener(this);
        mDeviceAdmin.setOnPreferenceClickListener(this);

        mLockStatus = (SwitchPreference)findPreference(KEY_LOCK_STATUS);
        mLockStatus.setOnPreferenceChangeListener(this);
        mLockStatus.setOnPreferenceClickListener(this);

        mLockSelf = (SwitchPreference)findPreference(KEY_LOCK_SELF);
        mLockSelf.setOnPreferenceChangeListener(this);
        mLockSelf.setOnPreferenceClickListener(this);

        mWarningCamera = (PreferenceScreen) findPreference(KEY_WARNING_CAMERA);
        mWarningCamera.setOnPreferenceClickListener(this);
        mWarningCamera.setOnPreferenceChangeListener(this);
        mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        mAppFeaturesExplain = (PreferenceScreen) findPreference(APP_FEATURES_EXPLAIN);
        mAppFeaturesExplain.setOnPreferenceClickListener(this);

        mVersionUpdate = (PreferenceScreen) findPreference(VERSION_UPDATE);
        mVersionUpdate.setOnPreferenceClickListener(this);

        String version = getAppVersionName(mContext);
        mVersionUpdate.setSummary(mContext.getString(R.string.app_version_name_title) + version);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_actionbar_back);
            mActionBar.setDisplayShowHomeEnabled(false);
        }
        mRelockAfterScreenOff.setSummary(mRelockAfterScreenOff.isChecked()
                ? R.string.summary_relock_after_screen_off
                : R.string.summary_relock_after_top_activity_change);
        sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        style = sp.getInt(Constants.LOCK_STYLE, Constants.PATTERN_TYPE);
        if(style == Constants.PATTERN_TYPE) {
            mChangeLockStyle.setSummary(R.string.pattern_lock_type);
        }else {
            mChangeLockStyle.setSummary(R.string.pin_lock_type);
        }
        if (mDpm.isAdminActive(mPolicyAdmin)){
            mDeviceAdmin.setChecked(true);
        }else{
            mDeviceAdmin.setChecked(false);
        }

        SharedPreferences sp = this.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        boolean isLockSettingsSuccess  = sp.getBoolean(Constants.KEY_LOCK_SUCCESS,false);
        Log.d(TAG,"onResume: isLockSettingsSuccess = "+isLockSettingsSuccess);
        if (!isLockSettingsSuccess){
            mLockStatus.setChecked(false);
            mLockSelf.setChecked(false);
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(SettingsActivity.KEY_LOCK_STATUS,false);
            editor.putBoolean(SettingsActivity.KEY_LOCK_SELF, false);
            editor.commit();
        }
        TokenUtils.startSelfLock(this);
    }


    /**
     * Check if set password cussefully.
     * @return
     */
    public static boolean isEnabledLock(){
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(SmartApplication.mContext);
        SharedPreferences sp = SmartApplication.mContext.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        boolean isLockSettingsSuccess  = sp.getBoolean(Constants.KEY_LOCK_SUCCESS, false);
        if (!isLockSettingsSuccess){
            sharedPreferences.edit().putBoolean(SettingsActivity.KEY_LOCK_STATUS,false).commit();
        }
        boolean isEnabledLock = sharedPreferences.getBoolean(SettingsActivity.KEY_LOCK_STATUS,false);
        Log.d(TAG, "isEnabledLock = " + isEnabledLock);
        return isEnabledLock;
    }

    public static boolean isEnableSelfLock(){
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(SmartApplication.mContext);
        SharedPreferences sp = SmartApplication.mContext.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        boolean isLockSettingsSuccess  = sp.getBoolean(Constants.KEY_LOCK_SUCCESS, false);
        if (!isLockSettingsSuccess){
            sharedPreferences.edit().putBoolean(SettingsActivity.KEY_LOCK_SELF,false).commit();
        }
        boolean isEnableSelfLock = sharedPreferences.getBoolean(SettingsActivity.KEY_LOCK_SELF,false);
        Log.d(TAG, "isEnableSelfLock = " + isEnableSelfLock);
        return isEnableSelfLock;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG,"onPreferenceChange: preference = "+preference);
        if(KEY_APPLOCK_RELOCK_AFTER_SCREEN_OFF.equals(preference.getKey())){
            preference.setSummary((Boolean) newValue
                    ? R.string.summary_relock_after_screen_off
                    : R.string.summary_relock_after_top_activity_change);
        }else if(KEY_DEVICE_ADMIN.equals(preference.getKey())){
            boolean isDisabled = (Boolean) newValue;
            SharedPreferences sp = this.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
            //already set password
            boolean isLockSettingsSuccess = sp.getBoolean(Constants.KEY_LOCK_SUCCESS, false);
            if(!isDisabled && isLockSettingsSuccess){
                TokenUtils.startUninstallLock(this);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.equals(mChangeLockStyle))
        {
            if(style == Constants.PATTERN_TYPE)
            {
                String pwd = sp.getString("password", "");
                if(pwd != null && !pwd.isEmpty()) {
                    Intent intent = new Intent(this, DrawPwdActivity.class);
                    intent.putExtra(Constants.LOCK_TYPE, Constants.CHANGE_LOCK_STYLE);
                    startActivity(intent);
                }
                else
                {
                    Intent intent = new Intent(getApplicationContext(), PasswordSettingsActivity.class);
                    startActivity(intent);
                }
            }
            else if(style == Constants.PIN_TYPE)
            {
                String pwd = sp.getString("pin_pwd", "");
                if(pwd != null && !pwd.isEmpty()) {
                    Intent intent = new Intent(this, PINPwdActivity.class);
                    intent.putExtra(Constants.LOCK_TYPE, Constants.CHANGE_LOCK_STYLE);
                    startActivity(intent);
                }
                else
                {
                    Intent intent = new Intent(getApplicationContext(), PasswordSettingsActivity.class);
                    startActivity(intent);
                }

            }
            return true;
        }
        else if(preference.equals(mSetRetrievePwdMethod))
        {
            if(style == Constants.PATTERN_TYPE)
            {
                String pwd = sp.getString("password", "");
                if(pwd != null && !pwd.isEmpty()) {
                    Intent intent = new Intent(this, DrawPwdActivity.class);
                    intent.putExtra(Constants.LOCK_TYPE, Constants.RETRIEVE_PASSWORD);
                    startActivity(intent);
                }
                else
                {
                    Intent intent = new Intent(getApplicationContext(), RetrievePasswordActivity.class);
                    startActivity(intent);
                }
            }
            else if(style == Constants.PIN_TYPE)
            {
                String pwd = sp.getString("pin_pwd", "");
                if(pwd != null && !pwd.isEmpty()) {
                    Intent intent = new Intent(this, PINPwdActivity.class);
                    intent.putExtra(Constants.LOCK_TYPE, Constants.RETRIEVE_PASSWORD);
                    startActivity(intent);
                }
                else
                {
                    Intent intent = new Intent(getApplicationContext(), RetrievePasswordActivity.class);
                    startActivity(intent);
                }
            }
            return true;
        }else if (preference == mDeviceAdmin){
            Log.d(TAG,"onPreferenceClick :mDeviceAdmin"+mDeviceAdmin);
            if(mDeviceAdmin.isChecked()) {
                requestDeviceAdmin();
            }else {
                deActiveDeviceAdmin();
            }
        }else if(preference == mLockStatus){
            if (mLockStatus.isChecked()){
                SharedPreferences sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                style = sp.getInt(Constants.LOCK_STYLE, Constants.PATTERN_TYPE);
                boolean isLockSettingsSuccess  = sp.getBoolean(Constants.KEY_LOCK_SUCCESS, false);

                if(isLockSettingsSuccess){
                    sp.edit().putBoolean(Constants.SELF_LOCK, true).commit();
                }else{
                    if (style == Constants.PIN_TYPE) {
                        Intent intent = new Intent(this, JrdChooseLockPIN.class);
                        intent.putExtra("change_lock_type", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        Intent newIntent = getIntent();
                        newIntent.setClass(this, SetupPwdActivity.class);
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(newIntent);
                    }
                }
            }
        } else if (preference == mLockSelf) {
            if (mLockSelf.isChecked()) {
                SharedPreferences sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                style = sp.getInt(Constants.LOCK_STYLE, Constants.PATTERN_TYPE);
                boolean isLockSettingsSuccess = sp.getBoolean(Constants.KEY_LOCK_SUCCESS, false);

                if (isLockSettingsSuccess) {
                    sp.edit().putBoolean(Constants.SELF_LOCK, true).commit();
                } else {
                    if (style == Constants.PIN_TYPE) {
                        Intent intent = new Intent(this, JrdChooseLockPIN.class);
                        intent.putExtra("change_lock_type", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        Intent newIntent = getIntent();
                        newIntent.setClass(this, SetupPwdActivity.class);
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(newIntent);
                    }
                }
            }
        } else if (preference == mWarningCamera)
        {
            Intent intent = new Intent(getApplicationContext(), WarningCameraActivity.class);
            startActivity(intent);
        }else if(preference == mAppFeaturesExplain){
            Intent intent = new Intent(getApplicationContext(), AboutAppActivity.class);
            startActivity(intent);
        }else if(preference == mVersionUpdate){
            CheckNetworkAndUpdate();
        }

        return false;
    }

    private void requestDeviceAdmin() {
        if(mPolicyAdmin == null) {
            mPolicyAdmin = new ComponentName(mContext,
                    PolicyAdminReceiver.class);
        }
        Log.d(TAG,"requestDeviceAdmin");

        if (!mDpm.isAdminActive(mPolicyAdmin)) {
            Intent activateDeviceAdminIntent = new Intent(
                    DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            activateDeviceAdminIntent.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN, mPolicyAdmin);
            activateDeviceAdminIntent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION, getResources().getString(R.string.description_device_admin2));
            startActivityForResult(activateDeviceAdminIntent, 0);
            Log.d(TAG, "requestDeviceAdmin --> success!!");
        }
    }

    private void deActiveDeviceAdmin() {

        if(mPolicyAdmin == null) {
            mPolicyAdmin = new ComponentName(mContext,
                    PolicyAdminReceiver.class);
        }
        Log.d(TAG,"deActiveDeviceAdmin: mPolicyAdmin ="+mPolicyAdmin+", isActive = "+mDpm.isAdminActive(mPolicyAdmin));

        if (mDpm.isAdminActive(mPolicyAdmin)) {
            mDpm.removeActiveAdmin(mPolicyAdmin);
            Log.d(TAG, "deActiveDeviceAdmin --> success!!");
        }
    }

    /**
     * return this app version name
     */
    public static String getAppVersionName(Context context) {
        String versionName = "";
        try {
            // ---get the package info---
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
        }
        return versionName;
    }

    private boolean CheckNetworkAndUpdate(){
        if (!NetworkCheck.isNetworkAvailable(this)) {
            Toast.makeText(SettingsActivity.this, R.string.networks_failed, Toast.LENGTH_SHORT).show();
            return false;
        }

        updManager = IFlytekUpdate.getInstance(mContext);
        updManager.setParameter(UpdateConstants.EXTRA_WIFIONLY,"false");
        updManager.setParameter(UpdateConstants.EXTRA_STYLE, UpdateConstants.UPDATE_UI_DIALOG);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        if(!NetworkCheck.isWifi(this)){
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(mContext.getString(R.string.notice_dialog_title));
            builder.setMessage(mContext.getString(R.string.no_wifi));
            // update
            builder.setPositiveButton(R.string.soft_update_updatebtn, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                    updManager.forceUpdate(SettingsActivity.this, updateListener);
                }
            });
            // update later
            builder.setNegativeButton(R.string.soft_update_later, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.dismiss();
                }
            });
            Dialog noticeDialog = builder.create();
            noticeDialog.show();

        } else {
            updManager.autoUpdate(SettingsActivity.this, updateListener);
        }
        return true;
    }

    private IFlytekUpdateListener updateListener = new IFlytekUpdateListener() {

        @Override
        public void onResult(int errorcode, UpdateInfo result) {

            Log.d(TAG, "IFlytekUpdateListener onResult : " + errorcode);
            if(errorcode == UpdateErrorCode.OK && result!= null) {
                if(result.getUpdateType() == UpdateType.NoNeed) {
                    showTip(mContext.getString(R.string.soft_update_no));
                    return;
                }
                updManager.showUpdateInfo(SettingsActivity.this, result);
            }
            else {
                showTip(mContext.getString(R.string.soft_update_error_code) + errorcode);
            }
        }
    };

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
