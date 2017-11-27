package com.tcl.smartapp.token;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.tcl.smartapp.*;

import com.tcl.smartapp.R;
import com.tcl.smartapp.utils.Constants;

/**
 * Created on 4/20/16.
 */
public class SetupPwdActivity extends Activity {
    private LocusPassWordView mPwdView;
    private TextView token_hint;
    private PwdStatus setupStatus;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_draw_pwd);

        sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        editor = sp.edit();
        String status = sp.getString("status", "");

        if(status.equals("") || status.equals("1"))
            setupStatus = PwdStatus.DRAW_NEW_PWD;
        else if(status.equals("0"))
            setupStatus = PwdStatus.CONFIRM_OLD_PWD;
        else if(status.equals("2"))
            setupStatus = PwdStatus.CONFIRM_NEW_PWD;
        else if(status.equals("3"))
            setupStatus = PwdStatus.DISPLAY_NEW_PWD;

        token_hint = (TextView) this.findViewById(R.id.multi_tv_token_time_hint);
        if(setupStatus == PwdStatus.CONFIRM_OLD_PWD)
                token_hint.setText(getText(R.string.confirm_old_pattern));
        else if(setupStatus == PwdStatus.DRAW_NEW_PWD)
            token_hint.setText(getText(R.string.draw_new_pattern));
        else if(setupStatus == PwdStatus.CONFIRM_NEW_PWD)
            token_hint.setText(getText(R.string.confirm_new_pattern));
        else if(setupStatus == PwdStatus.DISPLAY_NEW_PWD)
            token_hint.setText(getText(R.string.display_new_pattern));

        mPwdView = (LocusPassWordView) this.findViewById(R.id.mPassWordView);
        mPwdView.setOnCompleteListener(new LocusPassWordView.OnCompleteListener() {
            @Override
            public void onComplete(String mPassword) {
                Md5Utils md5 = new Md5Utils();
                if(setupStatus == PwdStatus.DRAW_NEW_PWD)
                {
                    String encodedPwd = md5.toMd5(mPassword, "");
                    editor.putString("pre_password", encodedPwd);
                    editor.putString("status", "2");
                    editor.commit();
                    Intent intent = new Intent(SetupPwdActivity.this, SetupPwdActivity.class);
                    startActivity(intent);
                    finish();
                }
                else if(setupStatus == PwdStatus.CONFIRM_NEW_PWD)
                {
                    String pwd = sp.getString("pre_password", "");
                    String encodedPwd = md5.toMd5(mPassword, "");
                    if(encodedPwd.equals(pwd))
                    {
                        editor.putString("password", encodedPwd);
                        editor.putString("status", "0");
                        editor.putInt(Constants.LOCK_STYLE, Constants.PATTERN_TYPE);
                        editor.putBoolean(Constants.KEY_LOCK_SUCCESS, true);
                        editor.putBoolean(Constants.SELF_LOCK, true);
                        editor.commit();
                        Toast.makeText(SetupPwdActivity.this, getText(R.string.set_locus_token_success),
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
                            Intent intent = new Intent(SetupPwdActivity.this, RetrievePasswordActivity.class);
                            startActivity(intent);
                        }

                        finish();
                    }
                    else
                    {
                        editor.putString("pre_password", "");
                        editor.putString("status", "1");
                        editor.putBoolean(Constants.KEY_LOCK_SUCCESS, false);
                        editor.commit();
                        Toast.makeText(SetupPwdActivity.this, getText(R.string.two_locus_different),
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SetupPwdActivity.this, SetupPwdActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
                else if(setupStatus == PwdStatus.CONFIRM_OLD_PWD)
                {
                    String encodedPwd = md5.toMd5(mPassword, "");
                    String pwd = sp.getString("password", "");
                    if(pwd.equals(encodedPwd))
                    {
                        editor.putString("pre_password", "");
                        editor.putString("status", "1");
                        editor.commit();
                        Intent intent = new Intent(SetupPwdActivity.this, SetupPwdActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        mPwdView.markError();
                        Toast.makeText(SetupPwdActivity.this, getText(R.string.pls_try_again),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }

}
