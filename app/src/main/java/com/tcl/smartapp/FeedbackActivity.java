package com.tcl.smartapp;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tcl.smartapp.fileutils.FileUtils;
import com.tcl.smartapp.token.TokenUtils;
import com.umeng.fb.FeedbackAgent;
import com.umeng.fb.SyncListener;
import com.umeng.fb.model.Conversation;
import com.umeng.fb.model.Reply;
import com.umeng.fb.model.UserInfo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by user on 16-5-20.
 */
public class FeedbackActivity extends Activity implements View.OnClickListener,SyncListener {

    private static final String TAG = "FeedbackActivity";
    private Conversation conversation;
    private FeedbackAgent agent;

    private ActionBar mActionBar;

    private EditText mContactEdit = null;
    private EditText mContentEdit = null;
    private Button mSubmitBtn = null;

    private final String mFeedBackRecoderDirPath = Environment.getExternalStorageDirectory().getPath() + "/.encryptionStorage";
    private final String mFeedBackRecoderFilePath = Environment.getExternalStorageDirectory().getPath() + "/.encryptionStorage/.feedbackrecoder.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        Log.i(TAG, "onCreate(), Create FeedbackActivity ui!");

        initView();
        agent = new FeedbackAgent(this);
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
            mActionBar.setTitle(R.string.feedback_title);
        }

        mContactEdit = (EditText) findViewById(R.id.feedback_contact_edit);
        mContentEdit = (EditText) findViewById(R.id.feedback_content_edit);
        mContentEdit.requestFocus();

        mSubmitBtn = (Button) findViewById(R.id.submit_button);
        mSubmitBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.submit_button:
                Log.i(TAG, "onClick(), Click Submit Btn!");
                String content = mContentEdit.getText().toString().trim();
                String contact = mContactEdit.getText().toString().trim();
                if (content.equals("")) {
                    Toast.makeText(FeedbackActivity.this, R.string.request_content_empty, Toast.LENGTH_SHORT).show();
                    return;
                } else if (contact.equals("")) {
                    Toast.makeText(FeedbackActivity.this, R.string.request_contact_empty, Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    if (!isNetworkAvailable(this)) {
                        Toast.makeText(FeedbackActivity.this, R.string.networks_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    UserInfo info = new UserInfo();
                    Map<String, String> map = info.getContact();
                    if (contact == null)
                        map = new HashMap<String, String>();
                    map.put("plain", contact);
                    info.setContact(map);

                    agent.setUserInfo(info);

                    conversation = agent.getDefaultConversation();

                    conversation.addUserReply(content);

                    conversation.sync(this);

                    //save the feedback recoder
                    File bt_IsEncrypted_FileDirPath = new File(mFeedBackRecoderDirPath);
                    if (!bt_IsEncrypted_FileDirPath.exists()) {
                        bt_IsEncrypted_FileDirPath.mkdir();
                    }
                    if(FileUtils.checkFilePathExist(mFeedBackRecoderFilePath)){
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss  ");
                        Date curDate = new Date(System.currentTimeMillis());
                        String tempstr = formatter.format(curDate);
                        tempstr = tempstr + contact + "  " + content;

                        File mFeedBackRecoderFile = new File(mFeedBackRecoderFilePath);
                        FileUtils.saveStringToFile(mFeedBackRecoderFile,tempstr);
                    }

                }
                break;
            default:
                Log.d(TAG,"onClick :Unknown view is click !");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feedback_recoder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.feedback_recoder:
                Intent feedback_recoder_intent = new Intent(this,FeedbackRecoderActivity.class);
                startActivity(feedback_recoder_intent);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onReceiveDevReply(List<Reply> replies) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSendUserReply(List<Reply> arg0) {
        mContentEdit.setText("");
        mContactEdit.setText("");
        Toast.makeText(this, R.string.feedback_thanks, Toast.LENGTH_SHORT).show();
        finish();
    }




    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
        } else {
            //judge the internet
            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
