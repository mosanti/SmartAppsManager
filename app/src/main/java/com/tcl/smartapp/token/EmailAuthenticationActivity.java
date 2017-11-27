package com.tcl.smartapp.token;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tcl.smartapp.R;
import com.tcl.smartapp.utils.Constants;

import java.util.Random;
import javax.mail.MessagingException;

/**
 * Created by user on 5/25/16.
 */
public class EmailAuthenticationActivity extends Activity {
    private Button sendAuthCode, okButton, cancelButton;
    private TextView emailText, inputAuthCodeText;
    private EditText authCodeEditText;
    private SharedPreferences sp = null;
    private String email_address = null;
    private SharedPreferences.Editor editor;
    private Context context;
    private int code;
    private ProgressDialog mProgress;
    private Handler handler;
    private static int SEND_EMAIL_FINISH = 1;
    private static int SEND_EMAIL_ERROR = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email_authentication);
        final Intent intentX = getIntent();
        context = this;
        sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        editor = sp.edit();
        email_address = sp.getString(Constants.EMAIL_ADDRESS_FOR_RETRIEVE_PASSWORD, "");
        inputAuthCodeText = (TextView)findViewById(R.id.inputAuthCodeText);
        inputAuthCodeText.setVisibility(View.GONE);
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                super.handleMessage(msg);
                if(msg.what == SEND_EMAIL_FINISH) {
                    hideProgress();
                    inputAuthCodeText.setVisibility(View.VISIBLE);
                    authCodeEditText.setVisibility(View.VISIBLE);
                    Md5Utils md5 = new Md5Utils();
                    String encodedCode = md5.toMd5(Integer.toString(code), "");
                    editor.putString(Constants.AUTHENTICATION_CODE, encodedCode);
                    editor.commit();
                    Toast.makeText(context, getText(R.string.send_email_success), Toast.LENGTH_SHORT).show();
                } else if(msg.what == SEND_EMAIL_ERROR) {
                    hideProgress();
                    Toast.makeText(context, getText(R.string.send_email_error), Toast.LENGTH_SHORT).show();
                }
            }
        };
        sendAuthCode = (Button)findViewById(R.id.sendEmailButton);
        sendAuthCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(email_address != null && !email_address.isEmpty())
                {
                    Random random = new Random();
                    code = random.nextInt(10000);
                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                SendMail.sendMessage(email_address, "Authen code", "Authority code : " + code);
                                handler.sendEmptyMessage(SEND_EMAIL_FINISH);
                            } catch(MessagingException e) {
                                e.printStackTrace();
                                handler.sendEmptyMessage(SEND_EMAIL_ERROR);
                            }
                        }}.start();
                    showProgress(context, getString(R.string.send_email), getString(R.string.send_auth_code) + email_address);
                }
            }
        });
        emailText = (TextView)findViewById(R.id.emailAddressText);
        emailText.setText(email_address);
        authCodeEditText = (EditText)findViewById(R.id.authCodeText);
        authCodeEditText.setVisibility(View.GONE);
        okButton = (Button)findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String authCode = authCodeEditText.getText().toString();
                if(authCode == null || authCode.isEmpty())
                {
                    Toast.makeText(context, R.string.auth_code_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                String encodedCode = sp.getString(Constants.AUTHENTICATION_CODE, "");
                if(encodedCode == null || encodedCode.isEmpty())
                {
                    Toast.makeText(context, R.string.send_auth_code_first, Toast.LENGTH_SHORT).show();
                    return;
                }
                Md5Utils md5 = new Md5Utils();
                String encodedAuthCode = md5.toMd5(authCode, "");
                if(!encodedAuthCode.equals(encodedCode))
                {
                    Toast.makeText(context, R.string.auth_code_wrong, Toast.LENGTH_SHORT).show();
                    return;
                }
                int lockPattern = sp.getInt(Constants.LOCK_STYLE, Constants.PATTERN_TYPE);
                if(lockPattern == Constants.PATTERN_TYPE)
                {
                    editor.putString("status", "1");
                    editor.commit();
                    Intent intent = new Intent(context, SetupPwdActivity.class);
                    startActivity(intent);
                    finish();
                }
                else
                {
                    Intent intent = new Intent(context, JrdChooseLockPIN.class);
                    intent.putExtra("change_lock_type", true);
                    startActivity(intent);
                    finish();
                }
            }
        });
        cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intentX.setClass(context, DrawPwdActivity.class);
                startActivity(intentX);
                finish();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showProgress(Context context, String title, String message) {
        hideProgress();
        if (mProgress == null) {
            mProgress = new ProgressDialog(context);
        }
        mProgress.setTitle(title);
        mProgress.setMessage(message);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);
        mProgress.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mProgress.show();
    }

    private void hideProgress() {
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
    }
}
