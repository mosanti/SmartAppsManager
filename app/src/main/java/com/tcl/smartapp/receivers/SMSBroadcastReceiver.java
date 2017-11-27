package com.tcl.smartapp.receivers;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import com.tcl.smartapp.R;
import com.tcl.smartapp.SmartApplication;
import com.tcl.smartapp.TtsSettings;
import com.tcl.smartapp.utils.TtsUtils;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class SMSBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSBroadcastReceiver";
    /**
     * get contact name from contact list.
     * @param context
     * @param number
     * @return contact name
     */
    private String getContactName(Context context,String number){
        String contactName = "";
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                ContactsContract.CommonDataKinds.Phone.NUMBER+"=?",new String[]{number},null);
        if(cursor.moveToFirst()){
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            cursor.close();
        }

        return contactName;
    }
	@Override
	public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(TtsSettings.PREFER_NAME, Context.MODE_PRIVATE);
        boolean isTtsEnabled = sharedPreferences.getBoolean(TtsSettings.KEY_TTS_ENABLE,false);
        if (!isTtsEnabled){
            Log.d(TAG, "isTtsEnabled = "+ isTtsEnabled);
            return;
        }

		Object[] pduses = (Object[])intent.getExtras().get("pdus");
        String content="";
        String mobile="";

		for(Object pdus : pduses){
			byte[] pdusmessage = (byte[]) pdus;
			SmsMessage sms = SmsMessage.createFromPdu(pdusmessage);
			mobile = sms.getOriginatingAddress();
			content = content + sms.getMessageBody();
		}
        String name = getContactName(context, mobile);
        String ttsNumber = String.format(context.getString(R.string.sms_coming_text), mobile);
        String ttsName = String.format(context.getString(R.string.sms_coming_text), name);
        content = TextUtils.isEmpty(name) ? (ttsNumber + content) : (ttsName + content);
        Log.d(TAG, "number = " + mobile + ",sms content = " + content);
        SmartApplication smartApplication = (SmartApplication)context.getApplicationContext();
        smartApplication.mTtsUtils.play(content);
	}

}
