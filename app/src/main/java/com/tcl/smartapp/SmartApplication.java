package com.tcl.smartapp;
import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.iflytek.cloud.SpeechUtility;
import com.tcl.smartapp.receivers.PolicyAdminReceiver;
import com.tcl.smartapp.utils.TtsUtils;


/**
 * Created  on 16-4-26.
 */
public class SmartApplication extends Application {
    public TtsUtils mTtsUtils;
    public static Context mContext;
    public static DevicePolicyManager mDpm;
    private static ComponentName mPolicyAdmin;

	@Override
	public void onCreate() {
		// 应用程序入口处调用，避免手机内存过小，杀死后台进程后通过历史intent进入Activity造成SpeechUtility对象为null
		// 如在Application中调用初始化，需要在Mainifest中注册该Applicaiton
		// 注意：此接口在非主进程调用会返回null对象，如需在非主进程使用语音功能，请增加参数：SpeechConstant.FORCE_LOGIN+"=true"
		// 参数间使用半角“,”分隔。
		// 设置你申请的应用appid,请勿在'='与appid之间添加空格及空转义符
		
		// 注意： appid 必须和下载的SDK保持一致，否则会出现10407错误
        mContext = this;
		SpeechUtility.createUtility(SmartApplication.this, "appid=" + getString(R.string.app_id));
        mTtsUtils = new TtsUtils(this);
        mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		// 以下语句用于设置日志开关（默认开启），设置成false时关闭语音云SDK日志打印
		// Setting.setShowLog(false);
		super.onCreate();
	}

    public static void deActiveDeviceAdmin() {

        if(mPolicyAdmin == null) {
            mPolicyAdmin = new ComponentName(mContext,
                    PolicyAdminReceiver.class);
        }
        //Log.d(TAG,"deActiveDeviceAdmin: mPolicyAdmin ="+mPolicyAdmin+", isActive = "+mDpm.isAdminActive(mPolicyAdmin));

        if (mDpm.isAdminActive(mPolicyAdmin)) {
            mDpm.removeActiveAdmin(mPolicyAdmin);
            //Log.d(TAG, "deActiveDeviceAdmin --> success!!");
        }
    }
}
