package com.tcl.smartapp.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.tcl.smartapp.R;
import com.tcl.smartapp.service.BackgroundService;
import com.tcl.smartapp.service.ObserverService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 4/23/16.
 */
public class AccessibilityUtils {
    private static final String TAG = "AccessibilityUtils";
    private static final Intent ACCESSIBILITY_INTENT = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
    private static ObserverService mObserverService = null;

    private static boolean mHasLockedApp = false;

    private static boolean mIncomingPhone = false;
    private static List<String> inputMethodPackageList = null;
    private static List<String> launchPackageList = null;

    /**
     * Save Oberser service instance.
     *
     * @param observerService
     */
    public static synchronized void setObserverService(ObserverService observerService) {
        mObserverService = observerService;
    }

    public static ObserverService getmObserverService() {
        return mObserverService;
    }

    public static boolean hasLockedApp() {
        return mHasLockedApp;
    }

    public static synchronized void setHasLockedApp(boolean mHasLockedApp) {
        AccessibilityUtils.mHasLockedApp = mHasLockedApp;
    }

    public static synchronized void setIncomingPhone(boolean flag) {
        mIncomingPhone = flag;
    }

    public static boolean getIncomingPhone() {
        return mIncomingPhone;
    }

    /**
     * show Accessibility enable Prompt
     *
     * @parm context Context, show implement method startActivity
     */
    public static void showAccessibilityPrompt(Context context) {
        final Context myContext = context;
        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);
        builder.setTitle(R.string.accessibility_prompt_title);
        builder.setMessage(R.string.accessibility_prompt_content);

        // Cancel, do nothing
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // Go to accessibility settings
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                myContext.startActivity(ACCESSIBILITY_INTENT);
            }
        });
        builder.create().setCanceledOnTouchOutside(false);
        builder.create().show();
        Log.d(TAG, "showAccessibilityPrompt");
    }

    public static synchronized void updateInputMehodList(Context context){
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> methodList = imm.getInputMethodList();
        if(inputMethodPackageList == null){
            inputMethodPackageList = new ArrayList();
        }else{
            inputMethodPackageList.clear();
        }
        Log.d(TAG, "updateInputMehodList begin");
        for (InputMethodInfo list : methodList){
            String packageName = list.getPackageName() + "";
            inputMethodPackageList.add(packageName);
            Log.d(TAG, "updateInputMehodList packageName:"+packageName);

        }
        Log.d(TAG, "updateInputMehodList end");
    }

    public static boolean isInputMethodPackage(String packageName){
        if(packageName == null){
            return false;
        }
        if (inputMethodPackageList == null) {
            return false;
        }
        for(String inputMehodPackageName : inputMethodPackageList){
            if(packageName.equals(inputMehodPackageName)){
                return true;
            }
        }
        return false;
    }

}
