package com.tcl.smartapp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tcl.smartapp.token.TokenUtils;

/**
 * Created by user on 16-5-21.
 */
public class FeedbackReplyActivity  extends Activity implements View.OnClickListener {

    private static final String TAG = "FeedbackReplyActivity";
    private Context mContext;
    private Button mBackbtn;
    private TextView mReplyTv;
    private ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_reply);
        Log.i(TAG, "onCreate(), Create FeedbackActivity ui!");

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
            mActionBar.setTitle(R.string.feedback_reply_title);
        }

        mReplyTv = (TextView) findViewById(R.id.feedback_reply);
        Intent intent = getIntent();
        mReplyTv.setText(intent.getStringExtra("Reply"));

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
