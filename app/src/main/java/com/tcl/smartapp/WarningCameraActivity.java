package com.tcl.smartapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import android.widget.LinearLayout;

import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.view.SwitchButton;

public class WarningCameraActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "WarningCameraActivity";
    private LinearLayout mEnableWarning;
    private SwitchButton mWarningSwitchButton;
    private LinearLayout mCheckWarner;
    private SharedPreferences mSharedPreferences;
    private boolean mIsEnabledWarning;
    public static final String KEY_WARNING_STATUS = "enabled_warning";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warning_camera);

        mEnableWarning = (LinearLayout)findViewById(R.id.enable_warning);
        mEnableWarning.setOnClickListener(this);
        mWarningSwitchButton = (SwitchButton)findViewById(R.id.warning_status_icon);
        mWarningSwitchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mWarningSwitchButton.setChecked(isChecked);
                mIsEnabledWarning = mSharedPreferences.getBoolean(KEY_WARNING_STATUS,false);
                Log.d(TAG,"setWarningStatusIcon: get mIsEnabledWarning = "+mIsEnabledWarning);
                mIsEnabledWarning = isChecked;
                Log.d(TAG,"setWarningStatusIcon: save mIsEnabledWarning = "+mIsEnabledWarning);
                mSharedPreferences.edit().putBoolean(KEY_WARNING_STATUS,mIsEnabledWarning).commit();
            }
        });
        mCheckWarner = (LinearLayout)findViewById(R.id.see_warner);
        mCheckWarner.setOnClickListener(this);
        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsEnabledWarning = mSharedPreferences.getBoolean(KEY_WARNING_STATUS,false);
        Log.d(TAG, "onResume: get mIsEnabledWarning = " + mIsEnabledWarning);

        if (mIsEnabledWarning) {
            mWarningSwitchButton.setChecked(true);

        }else {
            mWarningSwitchButton.setChecked(false);
        }
        TokenUtils.startSelfLock(this);
    }

    @Override
    public void onClick(View v) {
       switch (v.getId()){

           case R.id.enable_warning:
               setWarningStatus();
               break;

           case R.id.see_warner:
               Log.d(TAG, "click see_warner");
               Intent intent = new Intent(this, PicViewerActivity.class);
               startActivity(intent);
               break;
       }
    }

    private void setWarningStatus(){
        mIsEnabledWarning = mSharedPreferences.getBoolean(KEY_WARNING_STATUS,false);
        Log.d(TAG,"setWarningStatusIcon: get mIsEnabledWarning = "+mIsEnabledWarning);
        mIsEnabledWarning = !mIsEnabledWarning;
        Log.d(TAG,"setWarningStatusIcon: save mIsEnabledWarning = "+mIsEnabledWarning);
        mSharedPreferences.edit().putBoolean(KEY_WARNING_STATUS,mIsEnabledWarning).commit();
        if (mIsEnabledWarning) {
            mWarningSwitchButton.setChecked(true);
        }else {
            mWarningSwitchButton.setChecked(false);
        }
    }
}
