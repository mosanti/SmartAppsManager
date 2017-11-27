package com.tcl.smartapp.token;

/**
 * Created on 5/4/16.
 */

import android.app.ActionBar;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.tcl.smartapp.PasswordSettingsActivity;
import com.tcl.smartapp.R;
import com.tcl.smartapp.WarningCameraActivity;
import com.tcl.smartapp.utils.AccessibilityUtils;
import com.tcl.smartapp.utils.CameraManager;
import com.tcl.smartapp.utils.Constants;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class PINPwdActivity extends PreferenceActivity {
    private static final String TAG = "PINPwdActivity";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, PINPwdFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Disable IME on our window since we provide our own keyboard
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
        //WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        super.onCreate(savedInstanceState);

        CharSequence msg = getText(R.string.lockpassword_input_pin_password);
        showBreadCrumbs(msg, msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }


    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionbar = getActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setTitle(R.string.lockpassword_check_pin_title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean isValidFragment(String fragmentName) {
        return true;
    }

    public static class PINPwdFragment extends Fragment
            implements OnClickListener, OnEditorActionListener, TextWatcher {
        private TextView mPasswordEntry;
        private static final String KEY_UI_STAGE = "jrd_ui_stage";
        private int mPasswordOnlyLength = 4;
        private int mRequestedQuality = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
        private Stage mUiStage = Stage.InputPassword;
        private TextView mHeaderText;
        private boolean mIsAlphaMode = false;
        private Button mCancelButton;
        private Button mNextButton;
        private Button mForgotPINButton;
        private static final long ERROR_MESSAGE_TIMEOUT = 3000;
        private static final int MSG_SHOW_ERROR = 1;

        private SharedPreferences sp;
        private String lockType, packageName;
        private Context mContext;
        private Intent mIntent;
        private BroadcastReceiver mReceiver;
        private CameraManager frontCameraManager;
        private SurfaceView frontSurfaceView;
        private SurfaceHolder frontHolder;

        private int password_count_down;
        private boolean keypad_enable = true;
        private Handler mTimerHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(mPasswordEntry == null || mHeaderText == null || mContext == null)
                    return;

                if(msg.what < Constants.PASSWORD_ERROR_COUNT_DOWN && msg.what > 0) {
                    keypad_enable = false;
                    mPasswordEntry.setText("");
                    mPasswordEntry.setEnabled(false);
                    mHeaderText.setText(mContext.getText(R.string.password_error_beyond_max_count) + "(" + msg.what + ")");
                } else if(msg.what <= 0) {
                    keypad_enable = true;
                    mPasswordEntry.setEnabled(true);
                    mUiStage = Stage.InputPassword;
                    updateStage(mUiStage);
                }
            }
        };
        private Timer mPasswordTimer = new Timer();
        private TimerTask mPasswordTask = null;

        @Override
        public void onStop() {
            super.onStop();
            TokenUtils.setLockScreenOperaterStatus(getActivity(), false);
            getActivity().finish();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (style == Constants.PIN_TYPE) {
                try {
                    final Object lockObj = new Object();
                    synchronized (lockObj) {
                        lockObj.wait(200);//wait 0.2s
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                DrawPwdActivity.setWorkingFlag(false);
                getActivity().unregisterReceiver(mReceiver);
            }

            if(password_count_down > 0 && password_count_down < Constants.PASSWORD_ERROR_COUNT_DOWN) {
                Date date = Calendar.getInstance().getTime();
                SharedPreferences sp = getActivity().getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt(Constants.PASSWORD_ERROR_COUNT_DOWN_REMAIN, password_count_down);
                editor.putString(Constants.LOCK_WINDOW_EXIT_TIME, date.toString());
                editor.commit();
            }
        }

        private boolean enableFlag;
        private int style = Constants.PIN_TYPE;

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_SHOW_ERROR) {
                    updateStage((Stage) msg.obj);
                }
            }
        };

        protected enum Stage {

            InputPassword(
                    R.string.lockpassword_input_pin_password,
                    R.string.lockpassword_ok_label),

            ConfirmWrong(
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_ok_label);

            /**
             * headerMessage The message displayed at the top.
             */
            Stage(int hintInNumeric, int nextButtonText) {
                this.numericHint = hintInNumeric;
                this.buttonText = nextButtonText;
            }

            public final int numericHint;
            public final int buttonText;
        }

        // required constructor for fragments
        public PINPwdFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mIntent = getActivity().getIntent();
            mContext = getActivity();
            if (mIntent == null || mIntent.getExtras() == null) {
                getActivity().finish();
                return null;
            }
            if (style == Constants.PIN_TYPE) {
                DrawPwdActivity.setWorkingFlag(true);
            }
            lockType = mIntent.getExtras().getString(Constants.LOCK_TYPE);
            packageName = mIntent.getExtras().getString(Constants.PACKAGE_NAME);
            //This value is only use in wifi and bt lock type
            enableFlag = mIntent.getBooleanExtra(Constants.ENABLE_FALG, false);
            Log.d(TAG, "type:" + lockType + ", package:" + packageName + ", enableFlag:" + enableFlag);

            if (!TokenUtils.lockTypeHandleBeforeUnlock(mContext, mIntent)) {
                getActivity().finish();
                return null;
            }

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
                            getActivity().finish();
                        }
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            getActivity().registerReceiver(mReceiver, filter);

            sp = getContext().getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

            View view = inflater.inflate(R.layout.jrd_choose_lock_password, null);

            mCancelButton = (Button) view.findViewById(R.id.cancel_button);
            mCancelButton.setOnClickListener(this);
            mNextButton = (Button) view.findViewById(R.id.next_button);
            mNextButton.setOnClickListener(this);
            mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntry.addTextChangedListener(this);
            mHeaderText = (TextView) view.findViewById(R.id.headerText);
            mForgotPINButton = (Button) view.findViewById(R.id.forgot_pin_button);
            mForgotPINButton.setVisibility(View.VISIBLE);
            mForgotPINButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!keypad_enable)
                        return;

                    int method = sp.getInt(Constants.RETRIEVE_PASSWORD_METHOD, Constants.GET_PASSWORD_BY_EMAIL);
                    if (method == Constants.GET_PASSWORD_BY_EMAIL) {
                        Intent intent = new Intent(mContext, EmailAuthenticationActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(mContext, SecurityQuestionActivity.class);
                        startActivity(intent);
                    }
                }
            });

            int currentType = mPasswordEntry.getInputType();
            mPasswordEntry.setInputType(mIsAlphaMode ? currentType
                    : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));

            if (savedInstanceState == null) {
                updateStage(Stage.InputPassword);
            } else {
                final String state = savedInstanceState.getString(KEY_UI_STAGE);
                if (state != null) {
                    mUiStage = Stage.valueOf(state);
                    updateStage(mUiStage);
                }
            }
            initView(view);

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

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            TokenUtils.setLockScreenOperaterStatus(getActivity(), false);
            if (style == Constants.PIN_TYPE) {
                DrawPwdActivity.setWorkingFlag(true);
            }
            updateStage(mUiStage);
        }

        @Override
        public void onPause() {
            mHandler.removeMessages(MSG_SHOW_ERROR);
            super.onPause();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(KEY_UI_STAGE, mUiStage.name());
        }

        protected void updateStage(Stage stage) {
            final Stage previousStage = mUiStage;
            mUiStage = stage;
            updateUi();

            // If the stage changed, announce the header for accessibility. This
            // is a no-op when accessibility is disabled.
            if (previousStage != stage) {
                mHeaderText.announceForAccessibility(mHeaderText.getText());
            }
        }

        /**
         * Validates PIN and returns a message to display if PIN fails test.
         *
         * @param password the raw password the user typed in
         * @return error message to show to user or null if password is OK
         */
        private String validatePassword(String password) {
            if (password.length() != 4) {
                return getString(R.string.lockpassword_input_pin_password);
            }
            int letters = 0;
            int numbers = 0;
            int lowercase = 0;
            int symbols = 0;
            int uppercase = 0;
            int nonletter = 0;
            for (int i = 0; i < password.length(); i++) {
                char c = password.charAt(i);
                // allow non control Latin-1 characters only
                if (c < 32 || c > 127) {
                    return getString(R.string.lockpassword_illegal_character);
                }
                if (c >= '0' && c <= '9') {
                    numbers++;
                    nonletter++;
                } else if (c >= 'A' && c <= 'Z') {
                    letters++;
                    uppercase++;
                } else if (c >= 'a' && c <= 'z') {
                    letters++;
                    lowercase++;
                } else {
                    symbols++;
                    nonletter++;
                }
            }
            if (DevicePolicyManager.PASSWORD_QUALITY_NUMERIC == mRequestedQuality
                    && (letters > 0 || symbols > 0)) {
                // This shouldn't be possible unless user finds some way to bring up
                // soft keyboard
                return getString(R.string.lockpassword_pin_contains_non_digits);
            }
            return null;
        }

        private void handleNext() {
            final String pin = mPasswordEntry.getText().toString();
            if (TextUtils.isEmpty(pin)) {
                return;
            }
            String errorMsg = null;
            if (mUiStage == Stage.InputPassword) {
                SharedPreferences sp = getActivity().getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                boolean isEnabledWarning = sp.getBoolean(WarningCameraActivity.KEY_WARNING_STATUS, false);
                Md5Utils md5 = new Md5Utils();
                String encodedPwd = md5.toMd5(pin, "");
                String pwd = sp.getString("pin_pwd", "");
                boolean passed = false;
                int error_count = 0;

                if (pwd.length() == 0) {
                    passed = true;
                } else {
                    if (pwd.equals(encodedPwd))
                        passed = true;
                    else {
                        CharSequence tmp = mPasswordEntry.getText();
                        if (tmp != null) {
                            Selection.setSelection((Spannable) tmp, 0, tmp.length());
                        }

                        error_count = sp.getInt(Constants.PASSWORD_ERROR_COUNT, 0);
                        error_count++;
                        if(error_count < Constants.PASSWORD_ERROR_MAX_COUNT) {
                            if(error_count <= 0)
                                error_count = 1;
                            updateStage(Stage.ConfirmWrong);
                        }
                        else if(error_count >= Constants.PASSWORD_ERROR_MAX_COUNT) {
                            error_count = 0;
                            keypad_enable = false;
                            mPasswordEntry.setEnabled(false);
                            startPasswordCountDownTimer(Constants.PASSWORD_ERROR_COUNT_DOWN);
                            if (frontCameraManager != null && isEnabledWarning) {
                                frontCameraManager.takeFrontPhotoWork();
                            }
                        }
                    }
                }
                editor.putInt(Constants.PASSWORD_ERROR_COUNT, error_count);
                editor.commit();

                if (passed == true) {
                    TokenUtils.lockTypeHandleAfterUnlock(mContext, mIntent);
                    getActivity().finish();
                }
            }

            if (errorMsg != null) {
                showError(errorMsg, mUiStage);
            }
        }

        public void onClick(View v) {
            if(!keypad_enable)
                return;

            switch (v.getId()) {
                case R.id.next_button:
                    handleNext();
                    break;

                case R.id.cancel_button:
                    getActivity().finish();
                    break;
            }
        }

        private void showError(String msg, final Stage next) {
            mHeaderText.setText(msg);
            mHeaderText.announceForAccessibility(mHeaderText.getText());
            Message mesg = mHandler.obtainMessage(MSG_SHOW_ERROR, next);
            mHandler.removeMessages(MSG_SHOW_ERROR);
            mHandler.sendMessageDelayed(mesg, ERROR_MESSAGE_TIMEOUT);
        }

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Check if this was the result of hitting the enter or "done" key
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                handleNext();
                return true;
            }
            return false;
        }

        /**
         * Update the hint based on current Stage and length of password entry
         */
        private void updateUi() {
            String password = mPasswordEntry.getText().toString();
            final int length = password.length();
            if (mUiStage == Stage.InputPassword && length > 0) {
                if (length < mPasswordOnlyLength) {
                    String msg = getString(R.string.lockpassword_input_pin_password);
                    mHeaderText.setText(msg);
                    mNextButton.setEnabled(false);
                } else if (length == mPasswordOnlyLength) {
                    String error = validatePassword(password);
                    if (error != null) {
                        mHeaderText.setText(error);
                        mNextButton.setEnabled(false);
                    } else {
                        mHeaderText.setText(R.string.lockpassword_press_ok);
                        mNextButton.setEnabled(true);
                    }
                }

            } else {
                mHeaderText.setText(mUiStage.numericHint);
                mNextButton.setEnabled(length > 0);
            }
            mNextButton.setText(mUiStage.buttonText);
        }

        public void afterTextChanged(Editable s) {
            // Changing the text while error displayed resets to NeedToConfirm state
            if (mUiStage == Stage.ConfirmWrong) {
                mUiStage = Stage.InputPassword;
            }
            if (s.length() > 4) {
                s.delete(4, 5);
            }
            updateUi();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        private void initView(View view) {
            /**
             * 初始化前置相机参数
             */
            // 初始化surface view
            frontSurfaceView = (SurfaceView) view.findViewById(R.id.front_surfaceview);
            // 初始化surface holder
            frontHolder = frontSurfaceView.getHolder();
            frontCameraManager = new CameraManager(frontHolder);
        }

        private void startPasswordCountDownTimer(int count_down) {
            if(count_down <= 1 || count_down > Constants.PASSWORD_ERROR_COUNT_DOWN)
                return;
            password_count_down = count_down;
            if(mHeaderText != null)
                mHeaderText.setText(getText(R.string.password_error_beyond_max_count) + "(" + password_count_down + ")");
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

}
