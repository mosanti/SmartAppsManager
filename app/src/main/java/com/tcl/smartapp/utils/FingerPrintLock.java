package com.tcl.smartapp.utils;

import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Log;
import android.widget.Toast;

import com.tcl.smartapp.PasswordSettingsActivity;
import com.tcl.smartapp.R;
import com.tcl.smartapp.token.TokenUtils;

/**
 * Created  on 16-5-16.
 */
public class FingerPrintLock {
    private Activity mActivity;
    public static final String TAG = "FingerPrintLock";
    public FingerprintManagerCompat mFingerprintManager;
    private CancellationSignal mCancellationSignal;
    private boolean mSelfCancelled = false;
    private boolean mIsAuthenticationSucceeded = false;
    private final Handler mHandle;

    public FingerPrintLock(Activity activity, Handler handler) {
        mActivity = activity;
        mFingerprintManager = FingerprintManagerCompat.from(activity);
        mHandle = handler;
    }

    public boolean isFingerprintAuthAvailable() {
        boolean isAvailable = mFingerprintManager.isHardwareDetected()
                && mFingerprintManager.hasEnrolledFingerprints();
        Log.d(TAG, "isFingerprintAuthAvailable : isAvailable= " + isAvailable);
        return isAvailable;
    }

    public void startListening() {
        if (!isFingerprintAuthAvailable()) {
            return;
        }
        mSelfCancelled = false;
        mCancellationSignal = new CancellationSignal();
        mFingerprintManager
                .authenticate(null, 0, mCancellationSignal, new MyAuthenticationCallback(), null);
    }

    public void stopListening() {
        if (mCancellationSignal != null) {
            mSelfCancelled = true;
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    private void showTips(String text) {
        Toast.makeText(mActivity, text, Toast.LENGTH_SHORT).show();
    }

    public boolean getFingerPrintLockStatus() {
        return mIsAuthenticationSucceeded;
    }

    public class MyAuthenticationCallback extends FingerprintManagerCompat.AuthenticationCallback {
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            mIsAuthenticationSucceeded = false;
            //Log.d(TAG, "onAuthenticationError : msg = " + errString.toString());
            if (!mSelfCancelled) {
                String tips = errString.toString();
                Message msg = mHandle.obtainMessage(TokenUtils.UPDATE_UNlOCK_ERROR_CONTENT, tips);
                mHandle.sendMessage(msg);
                Log.d(TAG, "onAuthenticationError: sendMessage");
            }
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            Log.d(TAG, "onAuthenticationHelp : msg = " + helpString.toString());
            //showTips(helpString.toString());
        }

        @Override
        public void onAuthenticationFailed() {
            Log.d(TAG, "onAuthenticationFailed");
            mIsAuthenticationSucceeded = false;
            //showTips(mActivity.getResources().getString(R.string.fingerprint_fail));
            String tips = mActivity.getResources().getString(R.string.fingerprint_fail);
            Message msg = mHandle.obtainMessage(TokenUtils.UPDATE_UNlOCK_FAIL_CONTENT, tips);
            mHandle.sendMessage(msg);
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            Log.d(TAG, "onAuthenticationSucceeded");
            mIsAuthenticationSucceeded = true;
            if (mActivity != null) {
                Intent intent = mActivity.getIntent();
                String lockType = mActivity.getIntent().getExtras().getString(Constants.LOCK_TYPE);
                Log.d(TAG, "onAuthenticationSucceeded: lockType = " + lockType);
                TokenUtils.lockTypeHandleAfterUnlock(mActivity, intent);
                String tips = mActivity.getResources().getString(R.string.fingerprint_success);
                Message msg = mHandle.obtainMessage(TokenUtils.UPDATE_UNlOCK_SUCCESS_CONTENT, tips);
                mHandle.sendMessage(msg);

                mActivity.finish();
            }
            //showTips(mActivity.getResources().getString(R.string.fingerprint_success));
        }
    }


}
