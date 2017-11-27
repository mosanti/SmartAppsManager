package com.tcl.smartapp.token;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tcl.smartapp.R;
import com.tcl.smartapp.utils.Constants;

/**
 * Created by user on 5/19/16.
 */
public class SecurityQuestionActivity extends Activity {
    private TextView questionText;
    private EditText answerText;
    private Button cancelButton, okButton;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private String answer;
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.security_question);
        final Intent intentX = getIntent();
        context = this;
        sp = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        editor = sp.edit();
        String question = sp.getString(Constants.QUESTION_FOR_RETRIEVE_PASSWORD, "");
        answer = sp.getString(Constants.ANSWER_FOR_RETRIEVE_PASSWORD, "");
        if(question == null || question.isEmpty())
        {
            Toast.makeText(this, R.string.no_question, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if(answer == null || answer.isEmpty())
        {
            Toast.makeText(this, R.string.no_answer, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        questionText = (TextView)findViewById(R.id.questionText);
        questionText.setText(question);
        answerText = (EditText)findViewById(R.id.answerText);
        cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intentX.setClass(context, DrawPwdActivity.class);
                startActivity(intentX);
                finish();
                return;
            }
        });
        okButton = (Button)findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userAnswer = answerText.getText().toString();
                if(userAnswer == null || userAnswer.isEmpty())
                {
                    Toast.makeText(context, R.string.answer_invalid, Toast.LENGTH_SHORT).show();
                    return;
                }
                if(answer.equals(userAnswer))
                {
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
                else
                {
                    Toast.makeText(context, R.string.answer_wrong, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK)
            return true;
        return super.onKeyDown(keyCode, event);
    }
}
