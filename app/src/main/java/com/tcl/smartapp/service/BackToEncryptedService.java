package com.tcl.smartapp.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BackToEncryptedService extends Service {

    private static final String TAG = "BackToEncryptedService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"BackToEncryptedService is OnCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG,"BackToEncryptedService is onStartCommand");

        deleteTempDeencryptedFile(intent);

        stopSelf();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"BackToEncryptedService is onDestroy");
    }

    private void reWriteFile(List<String> list , File file) throws IOException {
        FileOutputStream outputStream = outputStream = new FileOutputStream(file);
        if (list.size() > 0) {
            for (String s : list) {
                outputStream.write(s.getBytes());
            }
        }
        outputStream.close();
    }


    private void deleteTempDeencryptedFile(Intent intent){
        String allPrivFilePath = Environment.getExternalStorageDirectory().getPath() + "/" + ".encryptionStorage";
        String IsDeEncrypted_FileName = allPrivFilePath + "/" + ".DeEncryptedFileName";
        File DeEncryptFile = new File(IsDeEncrypted_FileName);
        List<String> list_deencryptFileName = new ArrayList<String>();
        String linetemp = null;

        try {
            BufferedReader br_deencryptFileName = new BufferedReader(new FileReader(
                    DeEncryptFile));

            while ((linetemp = br_deencryptFileName.readLine()) != null) {
                list_deencryptFileName.add(linetemp + "\r\n");
            }
            br_deencryptFileName.close();
            Log.d(TAG,"BackToEncryptedService read DeEncryptedFileName success.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(list_deencryptFileName.size() > 0) {
            String temp_filename = null;
            for (int i = 0; i < list_deencryptFileName.size(); i++) {
                temp_filename = list_deencryptFileName.get(i).toString();
                temp_filename = temp_filename.replaceAll("\r|\n", "");
                final File tempfile_filename = new File(temp_filename);

                if (tempfile_filename.exists()) {
                    boolean temp = false;
                    temp = tempfile_filename.delete();
                    Log.d(TAG, "deencrypt file " + tempfile_filename + " is " + temp);

                    intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri uriRecovery = Uri.fromFile(tempfile_filename);
                    intent.setData(uriRecovery);
                    sendBroadcast(intent);
                }
            }

            int j = 0;
            int index = 0;
            index = list_deencryptFileName.size();
            for (int i = 0; i < index; i++) {
                list_deencryptFileName.remove(i-j);
                j++;
                Log.d(TAG, "remove" +  " is " + i );
            }

            try {
                reWriteFile(list_deencryptFileName, DeEncryptFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            Log.d(TAG,"No file decrypted temporary.");
        }
    }
}
