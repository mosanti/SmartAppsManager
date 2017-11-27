package com.tcl.smartapp.token;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.tcl.smartapp.R;
import com.tcl.smartapp.utils.Constants;

public class JrdChooseLockPIN extends PreferenceActivity {
    private static final String TAG = "JrdChooseLockPIN";
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, JrdChooseLockPINFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Disable IME on our window since we provide our own keyboard
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                //WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_choose_your_pin_header);
        showBreadCrumbs(msg, msg);
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
        actionbar.setTitle(R.string.lockpassword_pin_title);
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
	
    public static class JrdChooseLockPINFragment extends Fragment
            implements OnClickListener, OnEditorActionListener,  TextWatcher {
        private static final String KEY_FIRST_PIN = "jrd_first_pin";
        private static final String KEY_UI_STAGE = "jrd_ui_stage";
        private TextView mPasswordEntry;
        private int mPasswordOnlyLength = 4;
        private int mRequestedQuality = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
        private Stage mUiStage = Stage.Introduction;
        private TextView mHeaderText;
        private String mFirstPin;
        private boolean mIsAlphaMode = false;
        private Button mCancelButton;
        private Button mNextButton;
        private static final long ERROR_MESSAGE_TIMEOUT = 3000;
        private static final int MSG_SHOW_ERROR = 1;
        private boolean mIsOldPwdWrong = false;

        private SharedPreferences sp;
        private SharedPreferences.Editor editor;

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_SHOW_ERROR) {
                    updateStage((Stage) msg.obj);
                }
            }
        };

        /**
         * Keep track internally of where the user is in choosing a pattern.
         */
        protected enum Stage {

            Introduction(
                    R.string.lockpassword_choose_your_pin_header,
                    R.string.lockpassword_continue_label),

            NeedToConfirm(
                    R.string.lockpassword_confirm_your_pin_header,
                    R.string.lockpassword_ok_label),

            ConfirmWrong(
                    R.string.lockpassword_confirm_pins_dont_match,
                    R.string.lockpassword_continue_label),

            InputOldPassword(
                    R.string.lockpassword_input_your_pin_header,
                    R.string.lockpassword_continue_label);

            /**
             * @param headerMessage The message displayed at the top.
             */
            Stage(int hintInNumeric, int nextButtonText) {
                this.numericHint = hintInNumeric;
                this.buttonText = nextButtonText;
            }

            public final int numericHint;
            public final int buttonText;
        }

        // required constructor for fragments
        public JrdChooseLockPINFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            sp = getContext().getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            editor = sp.edit();

            View view = inflater.inflate(R.layout.jrd_choose_lock_password, null);

            mCancelButton = (Button) view.findViewById(R.id.cancel_button);
            mCancelButton.setOnClickListener(this);
            mNextButton = (Button) view.findViewById(R.id.next_button);
            mNextButton.setOnClickListener(this);
            mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntry.addTextChangedListener(this);
            mHeaderText = (TextView) view.findViewById(R.id.headerText);

            int currentType = mPasswordEntry.getInputType();
            mPasswordEntry.setInputType(mIsAlphaMode ? currentType
                    : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));

            boolean isChangeLockType = false;
            Intent intent = getActivity().getIntent();
            if (intent != null && intent.getExtras() != null) {
                isChangeLockType = intent.getExtras().getBoolean("change_lock_type");
            }

            if (savedInstanceState == null) {
                String pin_password = sp.getString("pin_pwd", "");

                if(pin_password != null && !pin_password.isEmpty() && !isChangeLockType) {
                    updateStage(Stage.InputOldPassword);
                }
                else {
                    updateStage(Stage.Introduction);
                }
            } else {
                mFirstPin = savedInstanceState.getString(KEY_FIRST_PIN);
                final String state = savedInstanceState.getString(KEY_UI_STAGE);
                if (state != null) {
                    mUiStage = Stage.valueOf(state);
                    updateStage(mUiStage);
                }
            }

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
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
            outState.putString(KEY_FIRST_PIN, mFirstPin);
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
         * @param password the raw password the user typed in
         * @return error message to show to user or null if password is OK
         */
        private String validatePassword(String password) {
            if (password.length() != 4){
                return getString(R.string.lockpassword_choose_your_pin_header, mPasswordOnlyLength);
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
            if (mUiStage == Stage.Introduction) {
                errorMsg = validatePassword(pin);
                if (errorMsg == null) {
                    mFirstPin = pin;
                    mPasswordEntry.setText("");
                    updateStage(Stage.NeedToConfirm);
                }
            } else if (mUiStage == Stage.NeedToConfirm) {
                if (mFirstPin.equals(pin)) {
                    Md5Utils md5 = new Md5Utils();
                    String encodedPwd = md5.toMd5(pin, "");
                    editor.putString("pin_pwd", encodedPwd);
                    editor.putInt(Constants.LOCK_STYLE, Constants.PIN_TYPE);
                    editor.putBoolean(Constants.KEY_LOCK_SUCCESS,true);
                    editor.putBoolean(Constants.SELF_LOCK, true);
                    editor.commit();
                    Toast.makeText(getContext(), getText(R.string.set_pin_password_success),
                            Toast.LENGTH_SHORT).show();

                    boolean isRetrievePasswordEmpty = false;
                    int get_password_method = sp.getInt(Constants.RETRIEVE_PASSWORD_METHOD, Constants.GET_PASSWORD_BY_EMAIL);
                    if(get_password_method == Constants.GET_PASSWORD_BY_EMAIL)
                    {
                        String email_address = sp.getString(Constants.EMAIL_ADDRESS_FOR_RETRIEVE_PASSWORD, "");
                        if(email_address == null || email_address.isEmpty())
                            isRetrievePasswordEmpty = true;
                    }
                    else
                    {
                        String question = sp.getString(Constants.QUESTION_FOR_RETRIEVE_PASSWORD, "");
                        String answer = sp.getString(Constants.ANSWER_FOR_RETRIEVE_PASSWORD, "");
                        if(question == null || question.isEmpty() || answer == null || answer.isEmpty())
                            isRetrievePasswordEmpty = true;
                    }
                    if(isRetrievePasswordEmpty)
                    {
                        Intent intent = new Intent(getActivity(), RetrievePasswordActivity.class);
                        startActivity(intent);
                    }

                    getActivity().finish();
                } else {
                    editor.putBoolean(Constants.KEY_LOCK_SUCCESS,false);
                    editor.commit();
                    CharSequence tmp = mPasswordEntry.getText();
                    if (tmp != null) {
                        Selection.setSelection((Spannable) tmp, 0, tmp.length());
                    }
                    mIsOldPwdWrong = false;
                    updateStage(Stage.ConfirmWrong);
                }
            } else if(mUiStage == Stage.InputOldPassword) {
                String pwd = sp.getString("pin_pwd", "");
                Md5Utils md5 = new Md5Utils();
                String encodedPwd = md5.toMd5(pin, "");
                if(encodedPwd.equals(pwd))
                {
                    mPasswordEntry.setText("");
                    updateStage(Stage.Introduction);
                } else {
                    CharSequence tmp = mPasswordEntry.getText();
                    if (tmp != null) {
                        Selection.setSelection((Spannable) tmp, 0, tmp.length());
                    }
                    mIsOldPwdWrong = true;
                    updateStage(Stage.ConfirmWrong);
                }

            }

            if (errorMsg != null) {
                showError(errorMsg, mUiStage);
            }
        }

        public void onClick(View v) {
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
            if ((mUiStage == Stage.Introduction || mUiStage == Stage.InputOldPassword) && length > 0) {
                if (length < mPasswordOnlyLength) {
                    String msg = getString((mUiStage == Stage.Introduction ?
                            R.string.lockpassword_choose_your_pin_header :
                            R.string.lockpassword_input_your_pin_header), mPasswordOnlyLength);
                    mHeaderText.setText(msg);
                    mNextButton.setEnabled(false);
                } else if (length == mPasswordOnlyLength){
                    String error = validatePassword(password);
                    if (error != null) {
                        mHeaderText.setText(error);
                        mNextButton.setEnabled(false);
                    } else {
                        mHeaderText.setText(R.string.lockpassword_press_continue);
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
                if(!mIsOldPwdWrong)
                    mUiStage = Stage.NeedToConfirm;
                else
                    mUiStage = Stage.InputOldPassword;
            }
            if (s.length() > 4){
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

    }
}

