package com.tcl.smartapp.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created  on 4/20/16.
 */
public class SmartappDBOperator {
    private static final String TAG = "SmartappDBOperator";
    private SmartappDBOpenHelper helper;
    private Context context;

    public static final String DB_UPDATE_ACTION = "com.tcl.smartapp.dbupdate";

    public SmartappDBOperator(Context context){
        helper = new SmartappDBOpenHelper(context);
        this.context = context;
    }

    public boolean isLockedApp(String appName){
        boolean ret = false;
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cur = db.rawQuery("select * from " + SmartappDBOpenHelper.TABLE_LOCKED_APP +
                " where " + SmartappDBOpenHelper.LOCKED_APP_FIELD_NAME + "=?",
                new String[]{appName});
        if(cur.moveToNext()){
            ret = true;
        }
        cur.close();
        db.close();
        return ret;
    }
    public boolean hasLockedApp(){
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cur = db.rawQuery("select * from " + SmartappDBOpenHelper.TABLE_LOCKED_APP, null);
        boolean ret = false;
        if(cur.moveToNext()){
            ret = true;
        }
        cur.close();
        db.close();
        return ret;
    }

    public List<String> getAllLockedApp(){
        List<String> appLists = new ArrayList<String>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cur = db.rawQuery("select " + SmartappDBOpenHelper.LOCKED_APP_FIELD_NAME + " from "
                + SmartappDBOpenHelper.TABLE_LOCKED_APP, null);
        while(cur != null && cur.moveToNext()){
            appLists.add(cur.getString(0));
        }
        cur.close();
        db.close();
        return appLists;
    }

    public void add(String appName){
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(SmartappDBOpenHelper.LOCKED_APP_FIELD_NAME, appName);

        long ret = db.insert(SmartappDBOpenHelper.TABLE_LOCKED_APP, null, values);
        if(ret != -1){
            Log.d(TAG, "insert success id:" + ret);
        }else{
            Log.e(TAG, "insert failed!!!");
        }
        db.close();

        notifyDBUpdate();
    }

    public void delete(String appName){
        SQLiteDatabase db = helper.getWritableDatabase();

        int ret = db.delete(SmartappDBOpenHelper.TABLE_LOCKED_APP,
                SmartappDBOpenHelper.LOCKED_APP_FIELD_NAME + "=?",
                new String[]{appName});
        Log.d(TAG, "del " + ret + " row");
        db.close();

        notifyDBUpdate();
    }

    public boolean isUnlocked(String appName){
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cur = db.rawQuery("select * from "
                + SmartappDBOpenHelper.TABLE_LOCKED_APP + " where " +
                SmartappDBOpenHelper.LOCKED_APP_FIELD_NAME + "=?", new String[]{appName});
        boolean isUnlocked = true;
        while(cur.moveToNext()){
            if (cur.getInt(cur
                    .getColumnIndex(SmartappDBOpenHelper
                            .LOCKED_APP_FIELD_UNLOCKED)) == 0 ){
                isUnlocked = false;
            }
        }
        cur.close();
        db.close();
        return isUnlocked;
    }
    public void updateAppUnlockStatus(String appName, boolean unlockStatus){
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SmartappDBOpenHelper.LOCKED_APP_FIELD_UNLOCKED, unlockStatus);
        int ret = db.update(SmartappDBOpenHelper.TABLE_LOCKED_APP, values,
                SmartappDBOpenHelper.LOCKED_APP_FIELD_NAME + "=?",
                new String[]{appName});
        Log.d(TAG, "updateAppUnlockStatus " + ret + " row");
        db.close();
        notifyDBUpdate();
    }

    public void lockAllApp(){
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SmartappDBOpenHelper.LOCKED_APP_FIELD_UNLOCKED, false);
        int ret = db.update(SmartappDBOpenHelper.TABLE_LOCKED_APP, values, null, null);
        Log.d(TAG, "lockAllApp " + ret + " row");
        db.close();
        notifyDBUpdate();
    }
    public void notifyDBUpdate(){
//        Intent intent = new Intent();
//        intent.setAction(DB_UPDATE_ACTION);
//        context.sendBroadcast(intent);
    }
}
