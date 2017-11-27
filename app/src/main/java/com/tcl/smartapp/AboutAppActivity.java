package com.tcl.smartapp;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.tcl.smartapp.token.TokenUtils;

/**
 * Created by user on 16-5-26.
 */
public class AboutAppActivity extends Activity {

    private static final String TAG = "AboutAppActivity";
    private ActionBar mActionBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aboutapp);
        Log.i(TAG, "onCreate(), Create AboutAppActivity ui!");

        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_actionbar_back);
            mActionBar.setDisplayShowHomeEnabled(false);
            mActionBar.setTitle(R.string.features_explain);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TokenUtils.startSelfLock(this);
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
