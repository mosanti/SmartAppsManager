package com.tcl.smartapp;

import android.Manifest;
import android.app.ActionBar;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TabHost;

import com.iflytek.autoupdate.IFlytekUpdate;
import com.iflytek.autoupdate.IFlytekUpdateListener;
import com.iflytek.autoupdate.UpdateConstants;
import com.iflytek.autoupdate.UpdateErrorCode;
import com.iflytek.autoupdate.UpdateInfo;
import com.iflytek.autoupdate.UpdateType;
import com.tcl.smartapp.fileutils.FileUtils;
import com.tcl.smartapp.service.BackgroundService;
import com.tcl.smartapp.token.TokenUtils;
import com.umeng.fb.FeedbackAgent;
import com.umeng.fb.SyncListener;
import com.umeng.fb.model.Conversation;
import com.umeng.fb.model.Reply;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;



public class SmartAppsManagerActivity extends TabActivity implements
        OnCheckedChangeListener, SyncListener {
    private TabHost mTabHost;
    private RadioGroup mRadioderGroup;
    private AppsLockActivity mAppLock;
    private SystemLockActivity mSystemLock;
    private PicsVideoLockActivity mPicsVideoLock;
    private ActionBar mActionBar;
    private static final String[] REQUEST_PERMISSIONS = {Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    private static final String TAB_APPS_LOCK = "AppsLock";
    private static final String TAB_SYSTEM_LOCK = "SystemLock";
    private static final String TAB_PICS_VIDEO_LOCK = "PicsVideoLock";

    public static Context mContext;
    private Conversation conversation;
    private FeedbackAgent agent;

    private IFlytekUpdate updManager;
    private final String mFeedBackRecoderDirPath = Environment.getExternalStorageDirectory().getPath() + "/.encryptionStorage";
    private final String mFeedBackRecoderFilePath = Environment.getExternalStorageDirectory().getPath() + "/.encryptionStorage/.feedbackrecoder.txt";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smartapp_activity_main);
        mTabHost = this.getTabHost();
        mTabHost.addTab(mTabHost.newTabSpec(TAB_APPS_LOCK)
                .setIndicator(TAB_APPS_LOCK)
                .setContent(new Intent(this, AppsLockActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec(TAB_SYSTEM_LOCK)
                .setIndicator(TAB_SYSTEM_LOCK)
                .setContent(new Intent(this, SystemLockActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec(TAB_PICS_VIDEO_LOCK)
                .setIndicator(TAB_PICS_VIDEO_LOCK)
                .setContent(new Intent(this, PicsVideoLockActivity.class)));

        mRadioderGroup = (RadioGroup) findViewById(R.id.main_radio);
        mRadioderGroup.setOnCheckedChangeListener(this);
        mRadioderGroup.check(R.id.mainTabs_app_lock);

        mContext = this;
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_actionbar_slidemenu);
            mActionBar.setDisplayShowHomeEnabled(false);
        }
        getAppLockSlideMenu();
        if(!hasRequiredPermission(REQUEST_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, REQUEST_PERMISSIONS, 0);
        }

        agent = new FeedbackAgent(this);
        conversation = agent.getDefaultConversation();
        conversation.sync(this);

        //update info popup only in wifi network state
        updManager = IFlytekUpdate.getInstance(mContext);
        updManager.setParameter(UpdateConstants.EXTRA_WIFIONLY,"true");
        updManager.setParameter(UpdateConstants.EXTRA_STYLE, UpdateConstants.UPDATE_UI_DIALOG);
        updManager.autoUpdate(SmartAppsManagerActivity.this, updateListener);

        TokenUtils.initRelatedPackage(mContext);

        TokenUtils.relockSelf(this);
        startService(new Intent(this, BackgroundService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        TokenUtils.startSelfLock(this);
    }

    /**
     * check requirement permissions
     * @param permissions
     * @return
     */
    protected boolean hasRequiredPermission(String[] permissions) {
        for (String permission : permissions) {
            if (checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("hasRequiredPermission"," permission ="+permission);
                return false;
            }
        }
        return true;
    }

    private void getAppLockSlideMenu() {
        mAppLock = AppsLockActivity.mAppLockInstance;
        if (mAppLock != null) {
            mAppLock.getMySlidingMenu().bindActionBar(mActionBar);
        }
    }

    private void getSystemLockSlideMenu() {
        mSystemLock = SystemLockActivity.mSystemLockInstance;
        if (mSystemLock != null) {
            mSystemLock.getMySlidingMenu().bindActionBar(mActionBar);
        }
    }

    private void getPicsVideoLockSlideMenu() {
        mPicsVideoLock = PicsVideoLockActivity.mPicsVideoLockInstance;
        if (mPicsVideoLock != null) {
            mPicsVideoLock.getMySlidingMenu().bindActionBar(mActionBar);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub

        switch (item.getItemId()) {
            case android.R.id.home:
                if (mTabHost.getCurrentTabTag().equals(TAB_APPS_LOCK)
                        && mAppLock != null) {
                    mAppLock.getMySlidingMenu().getSlidingMenu().toggle(true);
                } else if (mTabHost.getCurrentTabTag().equals(TAB_SYSTEM_LOCK)
                        && mSystemLock != null) {
                    mSystemLock.getMySlidingMenu().getSlidingMenu().toggle(true);
                } else if (mTabHost.getCurrentTabTag().equals(TAB_PICS_VIDEO_LOCK)
                        && mPicsVideoLock != null) {
                    mPicsVideoLock.getMySlidingMenu().getSlidingMenu().toggle(true);
                } else {
                    finish();
                }
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.mainTabs_app_lock:
                mTabHost.setCurrentTabByTag(TAB_APPS_LOCK);
                getAppLockSlideMenu();
                break;
            case R.id.mainTabs_system_lock:
                mTabHost.setCurrentTabByTag(TAB_SYSTEM_LOCK);
                getSystemLockSlideMenu();
                break;

            case R.id.mainTabs_pics_video_lock:
                mTabHost.setCurrentTabByTag(TAB_PICS_VIDEO_LOCK);
                getPicsVideoLockSlideMenu();
                break;

        }

    }

    @Override
    public void onReceiveDevReply(List<Reply> replies) {
        // TODO Auto-generated method stub
        String content = "";
        if (replies.size() == 1) {// one reply
            content = mContext.getString(R.string.developer) + replies.get(0).content;
        } else if (replies.size() > 1) {// more reply
            content = mContext.getString(R.string.have) + replies.size() + mContext.getString(R.string.new_reply) + "\n";
            for(int i = 0; i < replies.size(); i++){
                content = content + replies.get(i).content + "\n";
            }
        } else {// no reply
            return;
        }

        System.out.println(mContext.getString(R.string.receive_new_reply));
        // show the reply notification
        showDevReplyNotification(mContext.getString(R.string.fb_notification_title), content);
    }

    @Override
    public void onSendUserReply(List<Reply> list) {

    }

    /**
     * Notification display have new reply
     * When click notification , it will turn to FeedbackReplyActivity
     *
     * @param title
     *            notification title
     * @param content
     *            notification content
     */
    private void showDevReplyNotification(String title, String content) {
        // get NotificationManager service
        mContext = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) mContext.
                getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intentToLaunch = new Intent(mContext, FeedbackReplyActivity.class);
        intentToLaunch.putExtra("Reply",content);
        intentToLaunch.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        int requestCode = (int) SystemClock.uptimeMillis();
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, requestCode, intentToLaunch,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //save the answer from server to feedback recoder
        File bt_IsEncrypted_FileDirPath = new File(mFeedBackRecoderDirPath);
        if (!bt_IsEncrypted_FileDirPath.exists()) {
            bt_IsEncrypted_FileDirPath.mkdir();
        }
        if(FileUtils.checkFilePathExist(mFeedBackRecoderFilePath)){
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss  ");
            Date curDate = new Date(System.currentTimeMillis());
            String tempstr = formatter.format(curDate);
            tempstr = tempstr + "Server" + "  " + content;

            File mFeedBackRecoderFile = new File(mFeedBackRecoderFilePath);
            FileUtils.saveStringToFile(mFeedBackRecoderFile,tempstr);
        }

        try {
            // get app icon
            int appIcon = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).applicationInfo.icon;

            // display notification
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext).setSmallIcon(appIcon)
                    .setContentTitle(title).setTicker(title).setContentText(content).setAutoCancel(true)
                    .setContentIntent(contentIntent);
            notificationManager.notify(0, mBuilder.build());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private IFlytekUpdateListener updateListener = new IFlytekUpdateListener() {

        @Override
        public void onResult(int errorcode, UpdateInfo result) {

            if(errorcode == UpdateErrorCode.OK && result!= null) {
                if(result.getUpdateType() == UpdateType.NoNeed) {
                    return;
                }
                updManager.showUpdateInfo(SmartAppsManagerActivity.this, result);
            }
        }
    };
}
