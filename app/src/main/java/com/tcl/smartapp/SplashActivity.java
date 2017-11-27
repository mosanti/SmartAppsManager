package com.tcl.smartapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.Constants;

public class SplashActivity extends Activity {
    private TextView mVersionTextView;
    private final int SHOW_TIME_MIN = 1000;// 最小显示时间
    private long mStartTime;// 开始时间
    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.INIT_SCUESS:// 如果初始化完毕，就发送此消息
                    long loadingTime = System.currentTimeMillis() - mStartTime;// 计算一下总共花费的时间
                    if (loadingTime < SHOW_TIME_MIN) {// 如果比最小显示时间还短，就延时进入MainActivity，否则直接进入
                        mHandler.postDelayed(goToMainActivity, SHOW_TIME_MIN
                                - loadingTime);
                    } else {
                        mHandler.post(goToMainActivity);
                    }
                    break;
                default:
                    break;
            }
        }
    };
    //进入下一个Activity
  private  Runnable goToMainActivity = new Runnable() {

        @Override
        public void run() {
            SplashActivity.this.startActivity(new Intent(SplashActivity.this,
                    SmartAppsManagerActivity.class));
            finish();
            overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TokenUtils.setStatusBarColor(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mVersionTextView = (TextView)findViewById(R.id.app_version);
        setVersion();
        mStartTime = System.currentTimeMillis();//记录开始时间，
        Message msg = mHandler.obtainMessage(Constants.INIT_SCUESS);
        mHandler.sendMessage(msg);
    }

    private void setVersion(){
        String appName = this.getString(R.string.app_name);
        try {
            String content = this.getString(R.string.app_name);
            int textSize = this.getResources().getDimensionPixelSize(R.dimen.version_name_text_size);
            SpannableString tSpan = new SpannableString(content);
            tSpan.setSpan(new AbsoluteSizeSpan(textSize), appName.length(), appName.length(),
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            mVersionTextView.setText(tSpan);
        }catch (Exception e){
            e.printStackTrace();
            mVersionTextView.setText(appName);
        }
    }
}
