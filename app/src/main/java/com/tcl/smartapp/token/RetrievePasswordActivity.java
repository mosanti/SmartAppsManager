package com.tcl.smartapp.token;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tcl.smartapp.R;
import com.tcl.smartapp.utils.Constants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 5/7/16.
 */
public class RetrievePasswordActivity extends Activity {
    private CheckBox emailAuthenCheckBox, qaCheckBox;
    private TextView emailTextView, questionTextView, answerTextView;
    private EditText emailAddressEditText, questionEditText, answerEditText;
    private Button okButton;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private int current_get_password_method, get_password_method;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.forgot_password);

        emailAuthenCheckBox = (CheckBox) findViewById(R.id.email_authen_checkbox);
        qaCheckBox = (CheckBox) findViewById(R.id.qa_checkbox);
        emailTextView = (TextView) findViewById(R.id.email_address);
        questionTextView = (TextView) findViewById(R.id.question);
        answerTextView = (TextView) findViewById(R.id.answer);
        emailAddressEditText = (EditText) findViewById(R.id.email_address_text);
        questionEditText = (EditText) findViewById(R.id.question_text);
        answerEditText = (EditText) findViewById(R.id.answer_text);
        okButton = (Button) findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(get_password_method == Constants.GET_PASSWORD_BY_EMAIL)
                {
                    String email_address = emailAddressEditText.getText().toString();
                    if(email_address != null && !email_address.isEmpty())
                    {
                        Pattern pattern = Pattern.compile("^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$");
                        Matcher matcher = pattern.matcher(email_address);
                        boolean result = matcher.matches();
                        if(result)
                        {
                            editor.putString(Constants.EMAIL_ADDRESS_FOR_RETRIEVE_PASSWORD, email_address);
                            editor.putInt(Constants.RETRIEVE_PASSWORD_METHOD, Constants.GET_PASSWORD_BY_EMAIL);
                            editor.commit();
                            finish();
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(), getText(R.string.email_address_error), Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(), getText(R.string.email_address_error), Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    String question = questionEditText.getText().toString();
                    String answer = answerEditText.getText().toString();
                    if(question != null && !question.isEmpty() && answer != null && !answer.isEmpty())
                    {
                        editor.putString(Constants.QUESTION_FOR_RETRIEVE_PASSWORD, question);
                        editor.putString(Constants.ANSWER_FOR_RETRIEVE_PASSWORD, answer);
                        editor.putInt(Constants.RETRIEVE_PASSWORD_METHOD, Constants.GET_PASSWORD_BY_QUESTION);
                        editor.commit();
                        finish();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), getText(R.string.question_answer_empty),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        emailAuthenCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUi(Constants.GET_PASSWORD_BY_EMAIL);
            }
        });
        qaCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUi(Constants.GET_PASSWORD_BY_QUESTION);
            }
        });

        sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        editor = sp.edit();
        current_get_password_method = sp.getInt(Constants.RETRIEVE_PASSWORD_METHOD, Constants.GET_PASSWORD_BY_EMAIL);

        updateUi(current_get_password_method);
    }

    private void updateUi(int method)
    {
        get_password_method = method;

        if(method == Constants.GET_PASSWORD_BY_EMAIL)
        {
            emailAuthenCheckBox.setChecked(true);
            qaCheckBox.setChecked(false);
            emailAuthenCheckBox.setChecked(true);
            emailTextView.setVisibility(View.VISIBLE);
            emailAddressEditText.setVisibility(View.VISIBLE);
            questionTextView.setVisibility(View.GONE);
            answerTextView.setVisibility(View.GONE);
            questionEditText.setVisibility(View.GONE);
            answerEditText.setVisibility(View.GONE);
            String mobile_number = sp.getString(Constants.EMAIL_ADDRESS_FOR_RETRIEVE_PASSWORD, "");
            emailAddressEditText.setText(mobile_number);
        }
        else
        {
            emailAuthenCheckBox.setChecked(false);
            qaCheckBox.setChecked(true);
            emailTextView.setVisibility(View.GONE);
            emailAddressEditText.setVisibility(View.GONE);
            questionTextView.setVisibility(View.VISIBLE);
            answerTextView.setVisibility(View.VISIBLE);
            questionEditText.setVisibility(View.VISIBLE);
            answerEditText.setVisibility(View.VISIBLE);
            String question = sp.getString(Constants.QUESTION_FOR_RETRIEVE_PASSWORD, "");
            questionEditText.setText(question);
            String answer = sp.getString(Constants.ANSWER_FOR_RETRIEVE_PASSWORD, "");
            answerEditText.setText(answer);
        }
    }
}
