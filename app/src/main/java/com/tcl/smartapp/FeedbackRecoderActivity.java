package com.tcl.smartapp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tcl.smartapp.token.TokenUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by user on 16-6-21.
 */
public class FeedbackRecoderActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "FeedbackRecoderActivity";
    private Context mContext;
    private Button mBackbtn;
    private TextView mRecoderTv;
    private ActionBar mActionBar;

    private final String mFeedBackRecoderDirPath = Environment.getExternalStorageDirectory().getPath() + "/.encryptionStorage";
    private final String mFeedBackRecoderFilePath = Environment.getExternalStorageDirectory().getPath() + "/.encryptionStorage/.feedbackrecoder.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_recode);

        mContext = getApplicationContext();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TokenUtils.startSelfLock(this);
    }

    private void initView() {
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_actionbar_back);
            mActionBar.setDisplayShowHomeEnabled(false);
            mActionBar.setTitle(R.string.feedback_recoder_title);
        }

        mRecoderTv = (TextView) findViewById(R.id.feedback_recode);

        try {
            File bt_IsEncrypted_FileDirPath = new File(mFeedBackRecoderDirPath);
            if (!bt_IsEncrypted_FileDirPath.exists()) {
                bt_IsEncrypted_FileDirPath.mkdir();
            }
            File file = new File(mFeedBackRecoderFilePath);
            if(file.exists()) {
                StringBuffer sb = new StringBuffer();
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line = "";
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append(System.getProperty("line.separator"));
                    sb.append("--------------------------------------------------------------");
                    sb.append(System.getProperty("line.separator"));
                }
                br.close();
                mRecoderTv.setText(sb.toString());
            } else {
                mRecoderTv.setText(getResources().getString(R.string.no_feedback_recoder));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecoderTv.setMovementMethod(ScrollingMovementMethod.getInstance());

        mBackbtn = (Button) findViewById(R.id.feedback_backbtn);
        mBackbtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.feedback_backbtn:
                Log.i(TAG, "onClick(), Click Back Btn!");
                finish();
                break;
            default:
                Log.d(TAG, "onClick :Unknown view is click !");
                finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}

