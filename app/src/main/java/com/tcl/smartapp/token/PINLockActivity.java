package com.tcl.smartapp.token;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tcl.smartapp.R;
import com.tcl.smartapp.WarningCameraActivity;
import com.tcl.smartapp.utils.CameraManager;
import com.tcl.smartapp.utils.Constants;
import com.tcl.smartapp.utils.FingerPrintLock;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class PINLockActivity extends Activity implements View.OnClickListener {
    private final static String TAG = "PINLockActivity";
    private LinearLayout mZeroTextView;
    private LinearLayout mOneTextView;
    private LinearLayout mTwoTextView;
    private LinearLayout mThreeTextView;
    private LinearLayout mFourTextView;
    private LinearLayout mFiveTextView;
    private LinearLayout mSixTextView;
    private LinearLayout mSevenTextView;
    private LinearLayout mEightTextView;
    private LinearLayout mNineTextView;
    private LinearLayout mConfirmTextView;
    private LinearLayout mCancelTextView;
    private EditText mPinEditText;
    private String mPinContent = "";
    private ImageView mDeletePinImageView;

    private TextView mUnlockTipTextView;
    private ImageView mAppIconImageView;
    private Intent mIntent;
    private BroadcastReceiver mReceiver;
    private TextView forgotPasswordBtn;
    private CameraManager frontCameraManager;
    private SurfaceView frontSurfaceView;
    private SurfaceHolder frontHolder;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private Context context;
    private FingerPrintLock mFingerPrintLock;
    private ImageView mFingerPrintIndicator;
    private int password_count_down;
    private boolean keypad_enable = true;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case TokenUtils.UPDATE_UNlOCK_ERROR_CONTENT:
                    String errorContent = (String)msg.obj;
                    mUnlockTipTextView.setTextColor(getColor(R.color.unlock_tips_color));
                    mUnlockTipTextView.setText(errorContent);
                    SharedPreferences sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                    boolean isEnabledWarning = sp.getBoolean(WarningCameraActivity.KEY_WARNING_STATUS,false);
                    if(frontCameraManager != null && isEnabledWarning){
                        frontCameraManager.takeFrontPhotoWork();
                    }
                    break;
                case TokenUtils.UPDATE_UNlOCK_FAIL_CONTENT:
                    String failContent = (String)msg.obj;
                    mUnlockTipTextView.setTextColor(getColor(R.color.unlock_tips_color));
                    mUnlockTipTextView.setText(failContent);
                    break;
                case TokenUtils.UPDATE_UNlOCK_SUCCESS_CONTENT:
                    String successContent = (String)msg.obj;
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
            if(mUnlockTipTextView == null || mPinEditText == null)
                return;

            if(msg.what < Constants.PASSWORD_ERROR_COUNT_DOWN && msg.what > 0) {
                forgotPasswordBtn.setVisibility(View.GONE);
                keypad_enable = false;
                mPinEditText.setText("");
                mUnlockTipTextView.setTextColor(getColor(R.color.unlock_tips_color));
                mUnlockTipTextView.setText(getText(R.string.password_error_beyond_max_count) + "(" + msg.what + ")");
            } else if(msg.what <= 0) {
                forgotPasswordBtn.setVisibility(View.VISIBLE);
                keypad_enable = true;
                mUnlockTipTextView.setTextColor(Color.WHITE);
                mUnlockTipTextView.setText(R.string.input_pin_tip_text);
            }
        }
    };
    private Timer mPasswordTimer = new Timer();
    private TimerTask mPasswordTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TokenUtils.setStatusBarTranslucent(this);
        setContentView(R.layout.activity_pinlock);
        sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        editor = sp.edit();
        context = this;
        mZeroTextView = (LinearLayout) findViewById(R.id.keybord_0);
        mZeroTextView.setOnClickListener(this);
        mOneTextView = (LinearLayout) findViewById(R.id.keybord_1);
        mOneTextView.setOnClickListener(this);
        mTwoTextView = (LinearLayout) findViewById(R.id.keybord_2);
        mTwoTextView.setOnClickListener(this);
        mThreeTextView = (LinearLayout) findViewById(R.id.keybord_3);
        mThreeTextView.setOnClickListener(this);
        mFourTextView = (LinearLayout) findViewById(R.id.keybord_4);
        mFourTextView.setOnClickListener(this);
        mFiveTextView = (LinearLayout) findViewById(R.id.keybord_5);
        mFiveTextView.setOnClickListener(this);
        mSixTextView = (LinearLayout) findViewById(R.id.keybord_6);
        mSixTextView.setOnClickListener(this);
        mSevenTextView = (LinearLayout) findViewById(R.id.keybord_7);
        mSevenTextView.setOnClickListener(this);
        mEightTextView = (LinearLayout) findViewById(R.id.keybord_8);
        mEightTextView.setOnClickListener(this);
        mNineTextView = (LinearLayout) findViewById(R.id.keybord_9);
        mNineTextView.setOnClickListener(this);
        mConfirmTextView = (LinearLayout) findViewById(R.id.keybord_confirm);
        mConfirmTextView.setOnClickListener(this);
        mCancelTextView = (LinearLayout) findViewById(R.id.keybord_cancel);
        mCancelTextView.setOnClickListener(this);
        mPinEditText = (EditText) findViewById(R.id.pin_content);
        mDeletePinImageView = (ImageView) findViewById(R.id.delete_pin);
        mDeletePinImageView.setOnClickListener(this);
        mDeletePinImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!TextUtils.isEmpty(mPinContent)) {
                    mPinContent = "";
                    mPinEditText.setText(mPinContent);
                }
                Log.d(TAG, "onClick: pin is clear all.");
                return true;
            }
        });
        mAppIconImageView = (ImageView) findViewById(R.id.app_icon);
        mUnlockTipTextView = (TextView) findViewById(R.id.unlock_tip);
        mFingerPrintIndicator = (ImageView)findViewById(R.id.fingerprint_indicator);
        mIntent = getIntent();
        final String lockType = mIntent.getExtras().getString(Constants.LOCK_TYPE);
        final String packageName = mIntent.getExtras().getString(Constants.PACKAGE_NAME);
        final boolean enableFlag;
        //This value is only use in wifi and bt lock type
        enableFlag = mIntent.getBooleanExtra(Constants.ENABLE_FALG, false);
        mAppIconImageView.setImageDrawable(TokenUtils.getAppIcon(this, lockType, packageName));
        DrawPwdActivity.setWorkingFlag(true);


        if(!TokenUtils.lockTypeHandleBeforeUnlock(this, mIntent)){
            finish();
            return;
        }
        initView();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "onReceive action:" + action);
                if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    Log.d(TAG, "onReceive state:" + state);
                    if(TelephonyManager.EXTRA_STATE_IDLE.equals(state)){
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
                int method = sp.getInt(Constants.RETRIEVE_PASSWORD_METHOD, Constants.GET_PASSWORD_BY_EMAIL);
                if(method == Constants.GET_PASSWORD_BY_EMAIL)
                {
                    Intent intent = new Intent(context, EmailAuthenticationActivity.class);
                    intent.putExtra(Constants.LOCK_TYPE, lockType);
                    intent.putExtra(Constants.PACKAGE_NAME, packageName);
                    intent.putExtra(Constants.ENABLE_FALG, enableFlag);
                    startActivity(intent);
                }
                else
                {
                    Intent intent = new Intent(context, SecurityQuestionActivity.class);
                    intent.putExtra(Constants.LOCK_TYPE, lockType);
                    intent.putExtra(Constants.PACKAGE_NAME, packageName);
                    intent.putExtra(Constants.ENABLE_FALG, enableFlag);
                    startActivity(intent);
                }
            }
        });
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

    /**
     * Check pin if it is right
     * @param pin
     * @return
     */
    private boolean isPinRight(String pin) {
        SharedPreferences sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        Md5Utils md5 = new Md5Utils();
        String encodedPwd = md5.toMd5(pin, "");
        String pwd = sp.getString("pin_pwd", "");

        if (pwd.equals(encodedPwd)) {
            TokenUtils.lockTypeHandleAfterUnlock(this, mIntent);
            return true;
        } else {
            return false;
        }

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
        try {
            final Object lockObj = new Object();
            synchronized (lockObj) {
                lockObj.wait(200);//wait 0.2s
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        DrawPwdActivity.setWorkingFlag(false);
        unregisterReceiver(mReceiver);
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

    @Override
    protected void onResume() {
        super.onResume();
        TokenUtils.setLockScreenOperaterStatus(this, false);
        if (mFingerPrintLock == null) {
            mFingerPrintLock = new FingerPrintLock(this,mHandler);
            mFingerPrintLock.startListening();
        }
        if (mFingerPrintLock != null && mFingerPrintLock.isFingerprintAuthAvailable()) {
            mFingerPrintIndicator.setVisibility(View.VISIBLE);
        }else {
            mFingerPrintIndicator.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: keyCode = " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK){
            Log.d(TAG,"onKeyDown: KEYCODE_BACK");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onClick(View v) {
        if(!keypad_enable)
            return;

        switch (v.getId()) {
            case R.id.keybord_0:
                mPinContent = mPinContent + 0;
                setCheckResult();
                Log.d(TAG, "onClick: keybord_0");
                break;

            case R.id.keybord_1:
                mPinContent = mPinContent + 1;
                setCheckResult();
                Log.d(TAG, "onClick: keybord_1");
                break;

            case R.id.keybord_2:
                mPinContent = mPinContent + 2;
                setCheckResult();
                Log.d(TAG, "onClick: keybord_2");
                break;

            case R.id.keybord_3:
                mPinContent = mPinContent + 3;
                setCheckResult();
                Log.d(TAG, "onClick: keybord_3");
                break;

            case R.id.keybord_4:
                mPinContent = mPinContent + 4;
                setCheckResult();
                Log.d(TAG, "onClick: keybord_4");
                break;

            case R.id.keybord_5:
                mPinContent = mPinContent + 5;
                setCheckResult();
                Log.d(TAG, "onClick: keybord_5");
                break;

            case R.id.keybord_6:
                mPinContent = mPinContent + 6;
                setCheckResult();
                Log.d(TAG, "onClick: keybord_6");
                break;

            case R.id.keybord_7:
                mPinContent = mPinContent + 7;
                setCheckResult();
                Log.d(TAG, "onClick: keybord_7");
                break;

            case R.id.keybord_8:
                mPinContent = mPinContent + 8;
                setCheckResult();
                Log.d(TAG, "onClick: keybord_8");
                break;

            case R.id.keybord_9:
                mPinContent = mPinContent + 9;
                setCheckResult();
                Log.d(TAG, "onClick: keybord_9");
                break;

            case R.id.delete_pin:
                if (!TextUtils.isEmpty(mPinContent)) {
                    mPinContent = mPinContent.substring(0, mPinContent.length() - 1);
                    mPinEditText.setText(mPinContent);
                }
                Log.d(TAG, "onClick: keybord_delete");
                break;
        }

    }

    private void setCheckResult(){
        int error_count = 0;
        mPinEditText.setText(mPinContent);
        if (isPinRight(mPinContent)) {
            mUnlockTipTextView.setTextColor(Color.GREEN);
            mUnlockTipTextView.setText(R.string.unlock_right_text);
            finish();
        }else if (mPinContent.length() >= 4){
            SharedPreferences sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            boolean isEnabledWarning = sp.getBoolean(WarningCameraActivity.KEY_WARNING_STATUS,false);
            mUnlockTipTextView.setTextColor(getColor(R.color.unlock_tips_color));

            error_count = sp.getInt(Constants.PASSWORD_ERROR_COUNT, 0);
            error_count++;
            mPinEditText.setText("");
            mPinContent = "";
            if(error_count < Constants.PASSWORD_ERROR_MAX_COUNT) {
                if(error_count <= 0)
                    error_count = 1;
                mUnlockTipTextView.setText(R.string.pls_try_again);
            }
            else if(error_count >= Constants.PASSWORD_ERROR_MAX_COUNT) {
                error_count = 0;
                startPasswordCountDownTimer(Constants.PASSWORD_ERROR_COUNT_DOWN);
                if(frontCameraManager != null && isEnabledWarning){
                    frontCameraManager.takeFrontPhotoWork();
                }
            }
        }
        editor.putInt(Constants.PASSWORD_ERROR_COUNT, error_count);
        editor.commit();
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
