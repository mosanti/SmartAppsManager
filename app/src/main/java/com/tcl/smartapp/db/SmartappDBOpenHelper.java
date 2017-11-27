package com.tcl.smartapp.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created  on 4/20/16.
 */
public class SmartappDBOpenHelper extends SQLiteOpenHelper {
    public static final String DB_NAME =  "smartapp.db";
    public static final String TABLE_LOCKED_APP = "lockApp";
    public static final String LOCKED_APP_FIELD_NAME = "name";
    public static final String LOCKED_APP_FIELD_UNLOCKED = "unlocked"; // app unlock status

    public SmartappDBOpenHelper(Context context) {
        super(context, DB_NAME, null, 1);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL("create table " + TABLE_LOCKED_APP + "(_id integer primary key autoincrement,"
                + LOCKED_APP_FIELD_NAME + " varchar(50),"
                + LOCKED_APP_FIELD_UNLOCKED + " BOOLEAN DEFAULT false);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
    }
}
