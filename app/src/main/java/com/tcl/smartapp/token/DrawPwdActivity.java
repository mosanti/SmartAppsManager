package com.tcl.smartapp.token;

/**
 * Created  on 4/21/16.
 */

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tcl.smartapp.*;
import com.tcl.smartapp.R;
import com.tcl.smartapp.utils.AccessibilityUtils;
import com.tcl.smartapp.utils.CameraManager;
import com.tcl.smartapp.utils.Constants;
import com.tcl.smartapp.utils.FingerPrintLock;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class DrawPwdActivity extends Activity {
    private LocusPassWordView mPwdView;
    private Context mContext;
    private static final String TAG = "DrawPwdActivity";
    private int style = Constants.PATTERN_TYPE;
    private static boolean IS_WORKING = false;
    private BroadcastReceiver mReceiver;
    private TextView forgotPasswordBtn;
    private TextView mUnlockTipTextView;
    private ImageView mAppIconImageView;
    private Drawable mAppIcon;
    private CameraManager frontCameraManager;
    private SurfaceView frontSurfaceView;
    private SurfaceHolder frontHolder;
    private static final String[] REQUEST_PERMISSIONS = {Manifest.permission.SEND_SMS};
    private FingerPrintLock mFingerPrintLock;
    private ImageView mFingerPrintIndicator;
    private int password_count_down;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TokenUtils.UPDATE_UNlOCK_ERROR_CONTENT:
                    SharedPreferences sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                    boolean isEnabledWarning = sp.getBoolean(WarningCameraActivity.KEY_WARNING_STATUS, false);
                    String errorContent = (String) msg.obj;
                    mUnlockTipTextView.setTextColor(getColor(R.color.unlock_tips_color));
                    mUnlockTipTextView.setText(errorContent);
                    if (frontCameraManager != null && isEnabledWarning) {
                        frontCameraManager.takeFrontPhotoWork();
                    }
                    break;
                case TokenUtils.UPDATE_UNlOCK_FAIL_CONTENT:
                    String failContent = (String) msg.obj;
                    mUnlockTipTextView.setTextColor(getColor(R.color.unlock_tips_color));
                    mUnlockTipTextView.setText(failContent);
                    break;
                case TokenUtils.UPDATE_UNlOCK_SUCCESS_CONTENT:
                    String successContent = (String) msg.obj;
                    mUnlockTipTextView.setTextColor(Color.GREEN);
                    mUnlockTipTextView.setText(successContent);
                    break;
                default:
                    break;
            }
        }
    };
    private Handler mTimerHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(mPwdView == null || forgotPasswordBtn == null || mUnlockTipTextView == null)
                return;
            if(msg.what < Constants.PASSWORD_ERROR_COUNT_DOWN && msg.what > 0) {
                mPwdView.disableTouch();
                forgotPasswordBtn.setVisibility(View.GONE);
                mUnlockTipTextView.setText(getText(R.string.password_error_beyond_max_count) + "(" + msg.what + ")");
            } else if(msg.what <= 0) {
                mPwdView.enableTouch();
                forgotPasswordBtn.setVisibility(View.VISIBLE);
                mUnlockTipTextView.setText(R.string.plz_draw_code);
            }
        }
    };
    private Timer mPasswordTimer = new Timer();
    private TimerTask mPasswordTask = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TokenUtils.setStatusBarTranslucent(this);
        final String lockType, packageName;
        final boolean enableFlag;
        final Intent intent = getIntent();
        if (intent == null || intent.getExtras() == null) {
            finish();
            return;
        }
        SharedPreferences sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        style = sp.getInt(Constants.LOCK_STYLE, Constants.PATTERN_TYPE);
        if (style == Constants.PIN_TYPE) {
            Intent newIntent = getIntent();
            newIntent.setClass(this, PINLockActivity.class);
            startActivity(newIntent);
            finish();
            return;
        }

        setWorkingFlag(true);

        lockType = intent.getExtras().getString(Constants.LOCK_TYPE);
        packageName = intent.getExtras().getString(Constants.PACKAGE_NAME);
        //This value is only use in wifi and bt lock type
        enableFlag = intent.getBooleanExtra(Constants.ENABLE_FALG, false);
        Log.d(TAG, "type:" + lockType + ", package:" + packageName + ", enableFlag:" + enableFlag);

        mContext = this;

        setContentView(R.layout.draw_pwd);

        if (!TokenUtils.lockTypeHandleBeforeUnlock(mContext, intent)) {
            finish();
            return;
        }

        initView();

        mUnlockTipTextView = (TextView) findViewById(R.id.multi_tv_token_time_hint);
        mAppIconImageView = (ImageView) findViewById(R.id.app_icon);
        mFingerPrintIndicator = (ImageView) findViewById(R.id.fingerprint_indicator);
        mAppIconImageView.setImageDrawable(TokenUtils.getAppIcon(mContext, lockType, packageName));

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "onReceive action:" + action);
                if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    Log.d(TAG, "onReceive state:" + state);
                    if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                        AccessibilityUtils.setIncomingPhone(false);
                        finish();
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        forgotPasswordBtn = (TextView) this.findViewById(R.id.forgot_password_button);
        forgotPasswordBtn.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        forgotPasswordBtn.setVisibility(View.VISIBLE);
        forgotPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                int method = sp.getInt(Constants.RETRIEVE_PASSWORD_METHOD, Constants.GET_PASSWORD_BY_EMAIL);
                if (method == Constants.GET_PASSWORD_BY_EMAIL) {
                    Intent intent = new Intent(getApplicationContext(), EmailAuthenticationActivity.class);
                    intent.putExtra(Constants.LOCK_TYPE, lockType);
                    intent.putExtra(Constants.PACKAGE_NAME, packageName);
                    intent.putExtra(Constants.ENABLE_FALG, enableFlag);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getApplicationContext(), SecurityQuestionActivity.class);
                    intent.putExtra(Constants.LOCK_TYPE, lockType);
                    intent.putExtra(Constants.PACKAGE_NAME, packageName);
                    intent.putExtra(Constants.ENABLE_FALG, enableFlag);
                    startActivity(intent);
                }
            }
        });

        mPwdView = (LocusPassWordView) this.findViewById(R.id.mPassWordView);
        mPwdView.setOnCompleteListener(new LocusPassWordView.OnCompleteListener() {
            @Override
            public void onComplete(String mPassword) {
                int error_count = 0;
                SharedPreferences sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                String pwd = sp.getString("password", "");
                boolean isEnabledWarning = sp.getBoolean(WarningCameraActivity.KEY_WARNING_STATUS, false);
                Md5Utils md5 = new Md5Utils();
                boolean passed = false;
                if (pwd.length() == 0) {
                    passed = true;
                } else {
                    String encodedPwd = md5.toMd5(mPassword, "");
                    if (encodedPwd.equals(pwd)) {
                        passed = true;
                        mUnlockTipTextView.setTextColor(Color.GREEN);
                        mUnlockTipTextView.setText(R.string.unlock_right_text);
                    } else {
                        mPwdView.markError();
                        mUnlockTipTextView.setTextColor(getColor(R.color.unlock_tips_color));
                        error_count = sp.getInt(Constants.PASSWORD_ERROR_COUNT, 0);
                        error_count++;
                        if(error_count <= 0) {
                            error_count = 1;
                            mUnlockTipTextView.setText(R.string.pls_try_again);
                        }
                        else if(error_count < Constants.PASSWORD_ERROR_MAX_COUNT) {
                            mUnlockTipTextView.setText(R.string.pls_try_again);
                        }
                        else if(error_count >= Constants.PASSWORD_ERROR_MAX_COUNT) {
                            error_count = 0;
                            startPasswordCountDownTimer(Constants.PASSWORD_ERROR_COUNT_DOWN);
                            if (frontCameraManager != null && isEnabledWarning) {
                                frontCameraManager.takeFrontPhotoWork();
                            }
                        }
                    }
                }
                editor.putInt(Constants.PASSWORD_ERROR_COUNT, error_count);
                editor.commit();

                if (passed) {
                    TokenUtils.lockTypeHandleAfterUnlock(mContext, intent);
                    finish();
                }
            }
        });
        if (!hasRequiredPermission(REQUEST_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, REQUEST_PERMISSIONS, 0);
        }
        String strDate = sp.getString(Constants.LOCK_WINDOW_EXIT_TIME, "");
        int remainCount = sp.getInt(Constants.PASSWORD_ERROR_COUNT_DOWN_REMAIN, 0);
        Date exitDate, nowDate;
        if(strDate != null && !strDate.isEmpty()) {
            exitDate = new Date(strDate);
            nowDate = Calendar.getInstance().getTime();
            long val = nowDate.getTime() - exitDate.getTime();
            val = val / 1000;
            if((int)val < remainCount) {
                startPasswordCountDownTimer(remainCount - (int)val);
            }
        }

    }

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
    protected void onResume() {
        super.onResume();
        TokenUtils.setLockScreenOperaterStatus(this, false);
        if (style == Constants.PATTERN_TYPE) {
            setWorkingFlag(true);
        }
        if (mFingerPrintLock == null) {
            mFingerPrintLock = new FingerPrintLock(this, mHandler);
            mFingerPrintLock.startListening();
        }
        if (mFingerPrintLock != null && mFingerPrintLock.isFingerprintAuthAvailable()) {
            mFingerPrintIndicator.setVisibility(View.VISIBLE);
        } else {
            mFingerPrintIndicator.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: keyCode = " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "onKeyDown: KEYCODE_BACK");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        TokenUtils.setLockScreenOperaterStatus(this, true);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (style == Constants.PATTERN_TYPE) {
            try {
                final Object lockObj = new Object();
                synchronized (lockObj) {
                    lockObj.wait(200);//wait 0.2s
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            DrawPwdActivity.setWorkingFlag(false);
			if(mReceiver != null)
            	unregisterReceiver(mReceiver);
        }
        if (mFingerPrintLock != null) {
            mFingerPrintLock.stopListening();
            mFingerPrintLock = null;
        }
        if(password_count_down > 0 && password_count_down < Constants.PASSWORD_ERROR_COUNT_DOWN) {
            Date date = Calendar.getInstance().getTime();
            SharedPreferences sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(Constants.PASSWORD_ERROR_COUNT_DOWN_REMAIN, password_count_down);
            editor.putString(Constants.LOCK_WINDOW_EXIT_TIME, date.toString());
            editor.commit();
        }
    }

    public static synchronized void setWorkingFlag(boolean flag) {
        IS_WORKING = flag;
    }

    public static boolean getWorkingFlag() {
        return IS_WORKING;
    }

    private void initView() {
        /**
         * 初始化前置相机参数
         */
        // 初始化surface view
        frontSurfaceView = (SurfaceView) findViewById(R.id.front_surfaceview);
        // 初始化surface holder
        frontHolder = frontSurfaceView.getHolder();
        frontCameraManager = new CameraManager(frontHolder);
    }

    private void startPasswordCountDownTimer(int count_down) {
        if(count_down <= 1 || count_down > Constants.PASSWORD_ERROR_COUNT_DOWN)
            return;
        password_count_down = count_down;
        if(mUnlockTipTextView != null)
            mUnlockTipTextView.setText(getText(R.string.password_error_beyond_max_count) + "(" + password_count_down + ")");
        if(mPasswordTask != null)
            mPasswordTask.cancel();
        mPasswordTask = new TimerTask() {
            @Override
            public void run() {
                password_count_down--;
                mTimerHandler.sendEmptyMessage(password_count_down);
                if(password_count_down <= 0) {
                    password_count_down = 0;
                    cancel();
                }
            }
        };
        mPasswordTimer.schedule(mPasswordTask, 1000, 1000);
    }
}
