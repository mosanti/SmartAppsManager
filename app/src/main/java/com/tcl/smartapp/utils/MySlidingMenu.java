package com.tcl.smartapp.utils;

import com.slidingmenu.lib.SlidingMenu;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.tcl.smartapp.DisabledAppsActivity;
import com.tcl.smartapp.EyeProtectActivity;
import com.tcl.smartapp.FeedbackActivity;
import com.tcl.smartapp.R;
import com.tcl.smartapp.SettingsActivity;
import com.tcl.smartapp.TtsSettings;

/**
 * Created by user on 4/25/16.
 */
public class MySlidingMenu implements OnClickListener {
    private static final String TAG = "MySlidingMenu";
    private Context mContext;
    private Button mHideAppsBtn;
    private Button mEyeProtectBtn;
    private Button mMessageTtsBtn;
    private ImageButton mSettingsIBtn;
    private ImageButton mShareBtn;
    private ImageButton mFeedbackBtn;

    private SlidingMenu mSlidingMenu;

    public MySlidingMenu(Context context){
        mContext = context;
        mSlidingMenu = new SlidingMenu(mContext);

        mSlidingMenu.setMode(com.slidingmenu.lib.SlidingMenu.LEFT);
        mSlidingMenu.setTouchModeAbove(com.slidingmenu.lib.SlidingMenu.TOUCHMODE_FULLSCREEN);
        mSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        mSlidingMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        mSlidingMenu.setFadeDegree(0.5f);
        View slideMenuView = LayoutInflater.from(context).inflate(R.layout.additional_menu_items, null);
        mSlidingMenu.setMenu(slideMenuView);
        mHideAppsBtn = (Button) slideMenuView.findViewById(R.id.hide_app);
        mHideAppsBtn.setOnClickListener(this);
        mEyeProtectBtn = (Button) slideMenuView.findViewById(R.id.eye_protect);
        mEyeProtectBtn.setOnClickListener(this);
        mMessageTtsBtn = (Button) slideMenuView.findViewById(R.id.message_tts);
        mMessageTtsBtn.setOnClickListener(this);
        mSettingsIBtn = (ImageButton) slideMenuView.findViewById(R.id.settings);
        mSettingsIBtn.setOnClickListener(this);
        mShareBtn = (ImageButton) slideMenuView.findViewById(R.id.share);
        mShareBtn.setOnClickListener(this);
        mFeedbackBtn = (ImageButton) slideMenuView.findViewById(R.id.send_email);
        mFeedbackBtn.setOnClickListener(this);
    }

    public SlidingMenu getSlidingMenu(){
        return this.mSlidingMenu;
    }

    public void bindActionBar(ActionBar actionBar){
        if(mSlidingMenu == null){
            Log.d(TAG, "slide menu is null");
            return;
        }

        if(actionBar == null){
            Log.d(TAG, "actionBar is null");
            return;
        }

        final ActionBar myActionBar = actionBar;
        mSlidingMenu.setOnOpenListener(new SlidingMenu.OnOpenListener() {

            @Override
            public void onOpen() {
                // TODO Auto-generated method stub
                Log.d(TAG, "slide menu is open");
                myActionBar.setHomeAsUpIndicator(R.drawable.ic_actionbar_back);
            }
        });

        mSlidingMenu.setOnCloseListener(new SlidingMenu.OnCloseListener() {

            @Override
            public void onClose() {
                // TODO Auto-generated method stub
                Log.d(TAG, "slide menu is closed");
                myActionBar.setHomeAsUpIndicator(R.drawable.ic_actionbar_slidemenu);
            }
        });
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick:" + v.getId());
        switch (v.getId()) {
            case R.id.hide_app:
                Intent disabledAppIntent = new Intent(mContext,DisabledAppsActivity.class);
                mContext.startActivity(disabledAppIntent);
                mSlidingMenu.toggle(false);
                break;
            case R.id.eye_protect:
                mContext.startActivity(new Intent(mContext, EyeProtectActivity.class));
                break;
            case R.id.message_tts:
                Intent Ttsintent = new Intent(mContext,TtsSettings.class);
                mContext.startActivity(Ttsintent);
                mSlidingMenu.toggle(false);
                break;
            case R.id.settings:
                Intent intent = new Intent(mContext, SettingsActivity.class);
                mContext.startActivity(intent);
                break;
            case R.id.share:
                ShareToApp sharetoapp = new ShareToApp();
                sharetoapp.shareMsg(mContext);
                mSlidingMenu.toggle(false);
                break;
            case R.id.send_email:
                Intent feedbackintent = new Intent(mContext, FeedbackActivity.class);
                mContext.startActivity(feedbackintent);
            default:
                Log.e(TAG, "unknown view id:" + v.getId());
                break;
        }
    }

}
