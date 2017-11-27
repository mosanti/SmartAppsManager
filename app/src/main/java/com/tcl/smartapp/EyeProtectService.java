package com.tcl.smartapp;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Created  on 4/25/16.
 */
public class EyeProtectService extends Service {
    private static View mNightView;
    private static WindowManager mWindowManager;
    private static int mFilterColor;

    public static final int FOREGROUND_NOTIFICATION_ID = 1101;

    @Override
    public void onCreate() {
        super.onCreate();

        mFilterColor = EyeProtect.getFilterColor(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        putInForeground(true);
        addNightView(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeNightView();
        putInForeground(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        removeNightView();
        addNightView(this);
    }

    public void setFilterColor(int color) {
        mFilterColor = color;
        if (mNightView != null)
            mNightView.setBackgroundColor(mFilterColor);
    }

    public int getFilterColor() {
        return mFilterColor;
    }

    private static void addNightView(Context context) {
        if (mNightView != null || mWindowManager != null)
            return;

        WindowManager.LayoutParams nightViewParam = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 0x00000300,
                PixelFormat.TRANSPARENT);

        try {
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            mNightView = new FrameLayout(context.getApplicationContext());
            mNightView.setBackgroundColor(mFilterColor);
            Point point = new Point();
            mWindowManager.getDefaultDisplay().getRealSize(point);
            //nightViewParam.gravity = 8388659;
            nightViewParam.width = point.x;
            nightViewParam.height = point.y;
            mWindowManager.addView(mNightView, nightViewParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void removeNightView() {
        if (mNightView != null && mWindowManager != null) {
            try {
                mWindowManager.removeView(mNightView);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mNightView = null;
            mWindowManager = null;
        }
    }

    private void putInForeground(boolean foregroundFlag) {
        if (foregroundFlag) {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
            Intent contentIntent = new Intent(this, EyeProtectActivity.class);
            contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            notification.setContentTitle(getResources().getString(R.string.protect_eye))
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentText(getResources().getString(R.string.filter_notification_content))
                    .setContentIntent(PendingIntent.getActivity(this, 0,
                            contentIntent, PendingIntent.FLAG_UPDATE_CURRENT));
            startForeground(FOREGROUND_NOTIFICATION_ID, notification.build());
        } else {
            stopForeground(true);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new EyeProtectBinder();
    }

    public class EyeProtectBinder extends Binder {
        public EyeProtectService getService() {
            return EyeProtectService.this;
        }
    }
}
