package com.tcl.smartapp.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.tcl.smartapp.token.TokenUtils;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: action:" + action);
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                TokenUtils.relockSelf(context);
            }else if(Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)){
                String reason = intent.getStringExtra("reason");
                if(reason == null){
                    Log.d(TAG, "reason is null");
                    return;
                }
                if("homekey".equals(reason)){
                    TokenUtils.relockSelf(context);
                }
            }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}
