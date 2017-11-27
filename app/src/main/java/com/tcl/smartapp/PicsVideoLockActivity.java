package com.tcl.smartapp;

import com.slidingmenu.lib.SlidingMenu;
import com.tcl.smartapp.fileutils.EncryptionProgram;
import com.tcl.smartapp.fileutils.FileUtils;
import com.tcl.smartapp.service.BackToEncryptedService;
import com.tcl.smartapp.service.BackgroundService;
import com.tcl.smartapp.token.DrawPwdActivity;
import com.tcl.smartapp.token.PINPwdActivity;
import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.AppUtil;
import com.tcl.smartapp.utils.Constants;
import com.tcl.smartapp.utils.MySlidingMenu;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.content.pm.PackageManager.NameNotFoundException;

public class PicsVideoLockActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "PicsVideoLockActivity";

    private String AppPathName = "com.tcl.smartapp";

    public static PicsVideoLockActivity mPicsVideoLockInstance;
    private MySlidingMenu mMySlidingMenu;
    private Context context;
    private ActionBar mActionBar;

    private ListView mPicsVideoLockListView = null;
    private Button mPicsVideoLockBtnStart;
    private TextView mTVEncrypt;
    private TextView mTVPrompt;
    private ImageView mImageViewEncrypt;
    private Button mRoundBtn;
    EncryptAdapter mPicsVideoLockAdapter = null;

    private static final int FILE_SELECT_CODE = 0X111;
    private final String allPrivFilePath = Environment.getExternalStorageDirectory().getPath() + "/" + ".encryptionStorage";
    boolean allFileIsOk = false;
    private  String selectdFilePath = null;
    private String IsClickItemFileName = null;
    private String mDeleteFileName = null;
    File bt_recovery_dir_path = new File(allPrivFilePath + "/" + ".restore_path");
    String IsDeEncrypted_FileName = allPrivFilePath + "/" + ".DeEncryptedFileName";
    File DeEncryptFile = new File(IsDeEncrypted_FileName);
    List<HashMap<String,Object>> listData = new ArrayList<HashMap<String,Object>>();
    List<Boolean> listDeEncryptItem = new ArrayList<Boolean>();
    List<String> list_refresh_Gallery = new ArrayList<String>();
    private boolean FirstTimeCreateFile = false;
    private boolean mRoundBtnIsClick = false;
    private ProgressDialog progressDialog;
    private boolean mFileIsNotExist =false;
    private boolean mFileIsInListview =false;
    private boolean  mIsNotEncryptFile =false;
    private boolean mBoolActionMode = false;
    private boolean boolDeleteSourceFile =false;
    private boolean boolTempDeEncrypt = false;
    private boolean boolNeedToDeencryptFile =false;
    MultiChoiceModeListener mCallBack = null;

    public static final int FILE_NO_EXIST=1;
    public static final int FILE_IN_LISTVIEW=2;
    public static final int ADD_FILE_TO_LISTVIEW=3;
    public static final int ISNOT_DEENCRYPT_FILE=4;
    public static final int DEENCRYPT_FILE=5;
    public static final int UPDATE_UI=6;
    public static final int UPDATE_LISTVIEW=7;


    int [] image = {R.drawable.image,R.drawable.video,R.drawable.text};
    int [] imageCheck = {R.drawable.btn_check_off,R.drawable.btn_check_on};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pics_video_lock);

        mPicsVideoLockInstance = this;
        context = this;
        mMySlidingMenu = new MySlidingMenu(context);
        mMySlidingMenu.getSlidingMenu().attachToActivity(this, SlidingMenu.SLIDING_WINDOW);

        mPicsVideoLockBtnStart = (Button) findViewById(R.id.picsvideolockbtn);
        mTVEncrypt = (TextView) findViewById(R.id.textViewEncrypt);
        mImageViewEncrypt = (ImageView) findViewById(R.id.ImageViewEncrypt);
        mTVPrompt = (TextView) findViewById(R.id.textViewPrompt);
        mPicsVideoLockBtnStart.setOnClickListener(this);
        mRoundBtn = (Button) findViewById(R.id.roundButton);
        mRoundBtn.setOnClickListener(this);

        initViewIfHasItem();
        mActionBar = getParent().getActionBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMySlidingMenu.getSlidingMenu().showMenu(false);
        mMySlidingMenu.getSlidingMenu().showContent(false);
        TokenUtils.startSelfLock(this);
    }

    public MySlidingMenu getMySlidingMenu() {
        return this.mMySlidingMenu;
    }


    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick:" + v.getId());
        switch (v.getId()) {
            case R.id.hide_app:
                Toast.makeText(context, "1", Toast.LENGTH_SHORT).show();
                break;
            case R.id.eye_protect:
                Toast.makeText(context, "2", Toast.LENGTH_SHORT).show();
                break;
            case R.id.message_tts:
                Toast.makeText(context, "3", Toast.LENGTH_SHORT).show();
                break;
            case R.id.picsvideolockbtn:
                Encryptfile_btnClick(v);
                break;
            case R.id.roundButton:
                if(allFileIsOk) {
                    mRoundBtnIsClick = true;
                    OpenSystemFile(v);
                }else{
                    Toast.makeText(this,R.string.write_permission_prompt,Toast.LENGTH_SHORT).show();
                }
            default:
                Log.e(TAG, "unknown view id:" + v.getId());
                break;
        }

    }

    private void initViewIfHasItem() {
        try {
            BufferedReader bg = new BufferedReader(new FileReader(
                    bt_recovery_dir_path));
            if (bg.readLine() != null) {
                Encryptfile_btnClick(mPicsVideoLockBtnStart);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void Encryptfile_btnClick(View v) {
        mPicsVideoLockBtnStart.setVisibility(View.GONE);
        mTVEncrypt.setVisibility(View.GONE);
        mImageViewEncrypt.setVisibility(View.GONE);
        mPicsVideoLockListView = (ListView) findViewById(R.id.PicsVideoLockListView);
        mRoundBtn.setVisibility(View.VISIBLE);
        mPicsVideoLockAdapter = new EncryptAdapter(this,listData);
        mPicsVideoLockListView.setAdapter(mPicsVideoLockAdapter);
        checkFilePathExist();
        updateListView();
        mPicsVideoLockListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Log.d(TAG, "onItemClick position:" + position);
                if(position < mPicsVideoLockListView.getCount()){
                    TextView IsClickItemTv = (TextView) mPicsVideoLockListView.getChildAt(position - mPicsVideoLockListView.getFirstVisiblePosition()).findViewById(R.id.list_textView);
                    IsClickItemFileName = IsClickItemTv.getText().toString().replaceAll("\r|\n", "");
                    progressDialog = ProgressDialog.show(PicsVideoLockActivity.this,
                            getResources().getString(R.string.deencrypting_prompt), getResources().getString(R.string.waiting_prompt), true, false);
                    new Thread(){

                        @Override
                        public void run() {

                            if(!(FileUtils.stringIsInFile(DeEncryptFile,IsClickItemFileName))) {
                                boolTempDeEncrypt = true;
                                DeEncryptionFile(IsClickItemFileName);
                                boolTempDeEncrypt = false;
                            }
                            listDeEncryptItem.set(position,true);
                            Message msg= new Message();
                            if(mIsNotEncryptFile){
                                msg.what = ISNOT_DEENCRYPT_FILE;
                                mHandle.sendMessage(msg);
                            }else{
                                msg.what = DEENCRYPT_FILE;
                                msg.arg1 = position;
                                mHandle.sendMessage(msg);
                            }
                        }}.start();
                }
            }
        });

        mCallBack = new MultiChoiceModeListener();
        mPicsVideoLockListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mPicsVideoLockListView.setMultiChoiceModeListener(mCallBack);
    }

    public void OpenSystemFile(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.file_picker_prompt)),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.filemanager_install_prompt, Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            selectdFilePath = getAbsolutePath(uri).toString();
            if(mPicsVideoLockListView == null) {
                mPicsVideoLockListView.setVisibility(View.VISIBLE);
            }
            //judge the path whether in phone device SD card(First SD card) nor second SD card
            if(!selectdFilePath.contains("storage/emulated") && isUserApp()){
                Toast.makeText(this,R.string.sd_card_permission_prompt,Toast.LENGTH_LONG).show();
                return ;
            }
            if(mRoundBtnIsClick && (selectdFilePath != null)){
                AddNewFileToListView();
            }
        }
    }

    private void AddNewFileToListView() {
        if(mRoundBtnIsClick){
            progressDialog = ProgressDialog.show(PicsVideoLockActivity.this,
                    getResources().getString(R.string.encrypting_prompt), getResources().getString(R.string.waiting_prompt), true, false);
            new Thread(){
                @Override
                public void run() {
                    EncryptionFile(bt_recovery_dir_path,selectdFilePath);
                    Message msg= new Message();
                    if(mFileIsNotExist)
                    {
                        msg.what = FILE_NO_EXIST;
                        mHandle.sendMessage(msg);
                    }else if(mFileIsInListview){
                        msg.what = FILE_IN_LISTVIEW;
                        mHandle.sendMessage(msg);
                    }else {
                        msg.what = ADD_FILE_TO_LISTVIEW;
                        mHandle.sendMessage(msg);
                    }
                }}.start();
        }
    }

    public String getAbsolutePath(Uri uri) {
        String result = "";
        Cursor c = this.getContentResolver().query(uri, new String[] { "_data" }, null, null, null);
        if(c!=null){
            c.moveToFirst();
            result = c.getString(0);
            c.close();
        }else{
            result=uri.getPath();
        }
        return result;
    }


    private boolean checkFilePathExist(){
        File bt_allPrivFilePath = new File(allPrivFilePath);
        try {
            if (!bt_allPrivFilePath.exists()) {
                bt_allPrivFilePath.mkdir();
            }
            allFileIsOk = true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,R.string.write_permission_prompt,Toast.LENGTH_SHORT).show();
            return false;
        }

        String IsEncrypted_FileDirPath = allPrivFilePath + "/" + ".EncryptedFileDir";
        File bt_IsEncrypted_FileDirPath = new File(IsEncrypted_FileDirPath);
        try {
            if (!bt_IsEncrypted_FileDirPath.exists()) {
                bt_IsEncrypted_FileDirPath.mkdir();
            }
            allFileIsOk = true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,R.string.write_permission_prompt,Toast.LENGTH_SHORT).show();
            return false;
        }

        String recovery_dir_path;
        recovery_dir_path = allPrivFilePath + "/" + ".restore_path";
        File bt_recovery_dir_path = new File(recovery_dir_path);

        if (!bt_recovery_dir_path.exists()) {
            try {
                mTVPrompt.setVisibility(View.VISIBLE);
                bt_recovery_dir_path.createNewFile();
                FirstTimeCreateFile = true;
                allFileIsOk = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Toast.makeText(this,R.string.write_permission_prompt,Toast.LENGTH_SHORT).show();
                return false;
            }
        }else{
            try {
                StringBuffer sb = new StringBuffer();
                BufferedReader br = new BufferedReader(new FileReader(recovery_dir_path));
                String line = "";
                while ((line = br.readLine()) != null) {
                    listDeEncryptItem.add(false);
                    sb.append(line);
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String IsEncrypted_FileName = allPrivFilePath + "/" + ".DeEncryptedFileName";
        File bt_IsIsEncrypted_FileName = new File(IsEncrypted_FileName);

        if (!bt_IsIsEncrypted_FileName.exists()) {
            try {
                bt_IsIsEncrypted_FileName.createNewFile();
                allFileIsOk = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Toast.makeText(this,R.string.write_permission_prompt,Toast.LENGTH_SHORT).show();
                return false;
            }

        }

        return allFileIsOk;
    }


    private Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case FILE_NO_EXIST:
                    Toast.makeText(PicsVideoLockActivity.this,R.string.file_no_exist,Toast.LENGTH_SHORT).show();
                    mFileIsNotExist = false;
                    if(progressDialog != null){
                        progressDialog.dismiss();
                    }
                    break;
                case FILE_IN_LISTVIEW:
                    Toast.makeText(PicsVideoLockActivity.this,R.string.file_in_listview,Toast.LENGTH_SHORT).show();
                    mFileIsInListview = false;
                    if(progressDialog != null){
                        progressDialog.dismiss();
                    }
                    break;
                case ADD_FILE_TO_LISTVIEW:
                    progressDialog.dismiss();
                    listDeEncryptItem.add(false);
                    updateListView();
                    Toast.makeText(PicsVideoLockActivity.this,R.string.encrypt_success,Toast.LENGTH_SHORT).show();
                    //sendBroadcast to refresh the gallery
                    File source_file =new File(selectdFilePath);
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);   //, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    Uri uri = Uri.fromFile(new File(source_file.getPath()));
                    intent.setData(uri);
                    sendBroadcast(intent);

                    break;
                case ISNOT_DEENCRYPT_FILE:
                    if(progressDialog != null){
                        progressDialog.dismiss();
                    }
                    Toast.makeText(PicsVideoLockActivity.this, R.string.no_need_deencrypt, Toast.LENGTH_SHORT).show();
                    mIsNotEncryptFile = false;

                    break;
                case DEENCRYPT_FILE:
                    if(progressDialog != null){
                        progressDialog.dismiss();
                    }
                    if(!(FileUtils.stringIsInFile(DeEncryptFile,IsClickItemFileName))) {
                        Toast.makeText(PicsVideoLockActivity.this,R.string.deencrypt_success, Toast.LENGTH_SHORT).show();
                    }

                    //sendBroadcast to refresh the gallery
                    Intent intentRecovery = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri uriRecovery = Uri.fromFile(new File(IsClickItemFileName));
                    intentRecovery.setData(uriRecovery);
                    sendBroadcast(intentRecovery);

                    mPicsVideoLockAdapter.notifyDataSetChanged();

                    File file=new File(IsClickItemFileName);
                    Intent it =new Intent(Intent.ACTION_VIEW);
                    int start = IsClickItemFileName.lastIndexOf(".");
                    int end=IsClickItemFileName.length();
                    String nailname = null;
                    if (start != -1 && end != -1) {
                        nailname = IsClickItemFileName.substring(start + 1, end).toString();
                        nailname = nailname.replaceAll("\r|\n", "");
                    }
                    if(nailname.equals("jpg") || nailname.equals("png") || nailname.equals("jpeg") || nailname.equals("bmp")) {
                        try { it.setDataAndType(Uri.fromFile(file), "image/*");
                            it.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            startActivity(it);
                        } catch (ActivityNotFoundException e)
                        {
                            e.printStackTrace();
                        }

                    }else if(nailname.equals("3gp") || nailname.equals("mp4") || nailname.equals("avi") || nailname.equals("wmv")){
                        try {
                            it.setDataAndType(Uri.fromFile(file), "video/*");
                            it.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            startActivity(it);
                        } catch (ActivityNotFoundException e)
                        {
                            e.printStackTrace();
                        }
                    }else {
                        Toast.makeText(PicsVideoLockActivity.this, R.string.no_support_file_style, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case UPDATE_UI:
                    if(progressDialog != null){
                        progressDialog.dismiss();
                    }
                    if(mBoolActionMode == true){
                        mBoolActionMode = false;
                        mPicsVideoLockListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                        mPicsVideoLockListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                    }

                    refreshGallery(list_refresh_Gallery);

                    mActionBar.show();

                    break;
                case UPDATE_LISTVIEW:
                    if(progressDialog != null){
                        progressDialog.dismiss();
                    }
                    if(listData.size() > 0){
                        mTVPrompt.setVisibility(View.GONE);
                        mPicsVideoLockListView.setVisibility(View.VISIBLE);
                    }else {
                        mTVPrompt.setVisibility(View.VISIBLE);
                    }

                    if(FirstTimeCreateFile  || allFileIsOk){
                        mPicsVideoLockAdapter = new EncryptAdapter(PicsVideoLockActivity.this, listData);
                        mPicsVideoLockListView.setAdapter(mPicsVideoLockAdapter);
                    }

                    mPicsVideoLockAdapter.notifyDataSetChanged();
                    mActionBar.show();
                    mRoundBtn.setVisibility(View.VISIBLE);
                    mRoundBtnIsClick = false;

                    break;

                default:
                    break;
            }
        }
    };

    private void EncryptionFile(File recovery_dir_path_file,String filePath){

        File source_file =new File(filePath);
        if(!source_file.exists()){
            mFileIsNotExist = true;
            return;
        }
        if(!FileUtils.stringIsInFile(recovery_dir_path_file,filePath)) {
            FileUtils.saveStringToFile(recovery_dir_path_file, filePath);     //save the source path

            EncryptionProgram.encryption(source_file);
            String fileNameWithoutDir = null;
            String fileNameMoveTOEncry = null;
            fileNameWithoutDir = FileUtils.getFileName(source_file.getPath());
            String IsEncrypted_FileDirPath = allPrivFilePath + "/" + ".EncryptedFileDir";
            fileNameMoveTOEncry = IsEncrypted_FileDirPath + "/." + fileNameWithoutDir + ".hider";
            FileUtils.copyFile(source_file.getPath(), fileNameMoveTOEncry);    //copy to the encrypt dir
            boolean temp = false;
            temp = source_file.delete();
            Log.d(TAG, temp + " to delete the source file : " + filePath);
            deleteThumbnailsImage(filePath);
        }else{
            mFileIsInListview = true;
        }
    }

    private void updateListView(){
        try {
                StringBuffer sb = new StringBuffer();
                BufferedReader br = new BufferedReader(new FileReader(bt_recovery_dir_path));
                List<String> list = new ArrayList<String>();
                String line = "";
                while ((line = br.readLine()) != null) {
                list.add(line + "\r\n");
                sb.append(line);
            }
            br.close();


            listData.clear();
            for (int i = 0; i < list.size(); i++) {
                HashMap<String, Object> list_ = new HashMap<String, Object>();
                String pathname = list.get(i).toString();
                list_.put("mFilePath", pathname);
                String nailname = null;
                nailname = FileUtils.getNailName(pathname);
                if (nailname.equals("jpg") || nailname.equals("png") || nailname.equals("jpeg") || nailname.equals("bmp")) {
                    list_.put("image", image[0]);
                } else if (nailname.equals("3gp") || nailname.equals("mp4") || nailname.equals("avi") || nailname.equals("wmv")) {
                    list_.put("image", image[1]);
                } else {
                    list_.put("image", image[2]);
                }
                list_.put("mIsChecked", imageCheck[0]);
                listData.add(list_);
            }

            new Thread(){
                @Override
                public void run() {
                    Message msg1= new Message();
                    msg1.what = UPDATE_LISTVIEW;
                    mHandle.sendMessage(msg1);

                }}.start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param filePath String : the source file path before his wsa encrypt
     * @return void
     */
    private void DeEncryptionFile(String filePath){
        String fileNameTemp = null;
        String IsEncryptedFileName = null;
        fileNameTemp = FileUtils.getFileName(filePath);
        String IsEncrypted_FileDirPath = allPrivFilePath + "/.EncryptedFileDir";
        IsEncryptedFileName = IsEncrypted_FileDirPath + "/." + fileNameTemp + ".hider";
        File IsEncryptFile = new File(IsEncryptedFileName);
        if (!IsEncryptedFileName.substring(IsEncryptedFileName.lastIndexOf("."), IsEncryptedFileName.length()).equals(".hider")) {
            mIsNotEncryptFile = true;
            return;
        } else {
            EncryptionProgram.decryption(IsEncryptFile);
            FileUtils.copyFile(IsEncryptFile.getPath(), filePath);
            //write filepath to DeEncryptedFileName
            if (boolTempDeEncrypt && (!FileUtils.stringIsInFile(DeEncryptFile,filePath))) {
                FileUtils.saveStringToFile(DeEncryptFile, filePath);
            }
        }
    }


    class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener{

        private TextView title = null;
        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
            String tmp = context.getString(R.string.numbers_file_choose);
            tmp = String.format(tmp, mPicsVideoLockListView.getCheckedItemCount());
            title.setText(tmp);
            mPicsVideoLockAdapter.notifyDataSetChanged();
            actionMode.invalidate();
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mPicsVideoLockInstance.mBoolActionMode = true;
            title = new TextView(PicsVideoLockActivity.this);
            title.setTextColor(Color.WHITE);
            title.setTextSize(13);
            title.setText(getResources().getString(R.string.one_file_is_choose));
            actionMode.setCustomView(title);
            View view_title = actionMode.getCustomView();
            View view_bar = (View) view_title.getParent();
            view_bar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

            getMenuInflater().inflate(R.menu.picsvideo_actionmode_menu, menu);
            MenuItem item = menu.findItem(R.id.action_select_all);
            item.setVisible(true);

            mRoundBtn.setVisibility(View.GONE);
            mActionBar.hide();
            mPicsVideoLockAdapter.notifyDataSetChanged();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_delete:
                    Log.d(TAG, "onActionItemClicked:" + menuItem.getItemId());
                    AlertDialog.Builder builder = new AlertDialog.Builder(PicsVideoLockActivity.this);
                    builder.setTitle(getResources().getString(R.string.notice_title)).setMessage(getResources().getString(R.string.delete_encrypt_file_prompt)).setIcon(android.R.drawable.ic_menu_delete);
                    builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            progressDialog = ProgressDialog.show(PicsVideoLockActivity.this, getResources().getString(R.string.deleting_prompt),
                                    getResources().getString(R.string.waiting_prompt), true, false);
                            mBoolActionMode = true;

                            new Thread() {
                                @Override
                                public void run() {
                                    boolDeleteSourceFile = true;
                                    try {
                                        removeItemAndDelFile();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    boolDeleteSourceFile = false;

                                    Message msg1 = new Message();
                                    msg1.what = UPDATE_UI;
                                    mHandle.sendMessage(msg1);
                                }
                            }.start();
                        }
                    });
                    builder.setNegativeButton(getResources().getString(R.string.cancel), null);
                    builder.create().show();
                    break;
                case R.id.action_remove:
                    Log.d(TAG, "onActionItemClicked:" + menuItem.getItemId());
                    int deleteCount = mPicsVideoLockListView.getCheckedItemCount();
                    if (deleteCount <= 0) {
                        Toast.makeText(PicsVideoLockActivity.this, R.string.no_file_is_choose, Toast.LENGTH_SHORT).show();
                        return false;
                    } else {

                        AlertDialog.Builder builder_remove = new AlertDialog.Builder(PicsVideoLockActivity.this);
                        builder_remove.setTitle(getResources().getString(R.string.notice_title)).setMessage(getResources().getString(R.string.remove_encrypt_file_prompt)).setIcon(R.drawable.ic_remove);
                        builder_remove.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                progressDialog = ProgressDialog.show(PicsVideoLockActivity.this,getResources().getString(R.string.removing_prompt),
                                        getResources().getString(R.string.waiting_prompt), true, false);
                                mBoolActionMode = true;
                                new Thread() {
                                    @Override
                                    public void run() {
                                        boolNeedToDeencryptFile = true;
                                        try {
                                            removeItemAndDelFile();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        boolNeedToDeencryptFile = false;

                                        Message msg = new Message();
                                        msg.what = UPDATE_UI;
                                        mHandle.sendMessage(msg);
                                    }
                                }.start();
                            }
                        });
                        builder_remove.setNegativeButton(getResources().getString(R.string.cancel), null);
                        builder_remove.create().show();
                    }
                    break;
                case R.id.action_select_all:
                    Log.d(TAG, "onActionItemClicked:" + menuItem.getItemId());
                    int itemCount = mPicsVideoLockListView.getCount();
                    if (itemCount == mPicsVideoLockListView.getCheckedItemCount()) {
                        mPicsVideoLockListView.clearChoices();
                        for (int i = 0; i < itemCount; i++) {
                            mPicsVideoLockListView.setItemChecked(i, false);
                        }
                        mPicsVideoLockAdapter.notifyDataSetChanged();
                        title.setText(getResources().getString(R.string.zero_file_is_choose));
                        Toast.makeText(PicsVideoLockActivity.this, R.string.cancle_all_success, Toast.LENGTH_SHORT).show();

                        mActionBar.show();
                    } else {
                        SparseBooleanArray itemStates = mPicsVideoLockListView.getCheckedItemPositions();
                        for (int i = 0; i < itemCount; i++) {
                            if (itemStates.get(i) == false) {
                                mPicsVideoLockListView.setItemChecked(i, true);
                            }
                        }
                        String tmp = context.getString(R.string.numbers_file_choose);
                        tmp = String.format(tmp, itemCount);
                        title.setText(tmp);
                        menuItem.setIcon(R.drawable.ic_selectall_clear);
                        Toast.makeText(PicsVideoLockActivity.this, R.string.choose_all_success, Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    Log.d(TAG, "onActionItemClicked: unknown id" + menuItem.getItemId());
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionMode = null;
            mActionBar.show();
            mBoolActionMode = false;
            mRoundBtn.setVisibility(View.VISIBLE);
        }
}

    private void removeItemAndDelFile() throws IOException {

        int itemCount = mPicsVideoLockListView.getCount();
        SparseBooleanArray itemStates = mPicsVideoLockListView.getCheckedItemPositions();
        int j = 0;
        List<String> list = new ArrayList<String>();
        List<String> list_temp = new ArrayList<String>();
        List<String> list_deencryptFileName = new ArrayList<String>();
        String linetemp = "";
        BufferedReader bg = new BufferedReader(new FileReader(
                bt_recovery_dir_path));
        while ((linetemp = bg.readLine()) != null) {
            list.add(linetemp + "\r\n");
            list_temp.add(linetemp + "\r\n");
        }
        bg.close();

        BufferedReader br_deencryptFileName = new BufferedReader(new FileReader(
                DeEncryptFile));

        linetemp = null;
        while ((linetemp = br_deencryptFileName.readLine()) != null) {
            list_deencryptFileName.add(linetemp + "\r\n");
        }
        br_deencryptFileName.close();

        list_refresh_Gallery.clear();
        for (int i = 0; i < itemCount; i++) {
            if (itemStates.get(i) == true) {
                mDeleteFileName = list_temp.get(i).toString();
                list_refresh_Gallery.add(mDeleteFileName);

                String line = "";
                mDeleteFileName = mDeleteFileName.replaceAll("\r|\n", "");
                String sourceFilePath = mDeleteFileName;
                File bt_sourceFile = new File(sourceFilePath);
                String deleteEncryptedName = FileUtils.getFileName(mDeleteFileName);
                deleteEncryptedName = allPrivFilePath + "/.EncryptedFileDir" + "/." + deleteEncryptedName + ".hider";
                File bt_deleteEncryptedName = new File(deleteEncryptedName);
                for (int k = 0; k < list.size(); k++) {
                    if (list.get(k).toString().equals(mDeleteFileName + "\r\n")) {
                        list.remove(k);
                        if (bt_deleteEncryptedName.exists()) {
                            if ((boolNeedToDeencryptFile) &&     //need to deencrypt and it hasn't been deencrypt before
                                    !(FileUtils.stringIsInFile(DeEncryptFile, mDeleteFileName))) {
                                DeEncryptionFile(sourceFilePath);
                            }
                            FileUtils.fileDel(bt_deleteEncryptedName);
                        }
                        if (boolDeleteSourceFile && bt_sourceFile.exists()) {
                            FileUtils.fileDel(bt_sourceFile);
                        }
                        if (FileUtils.stringIsInFile(DeEncryptFile, mDeleteFileName)) {
                            for(int m = 0; m < list_deencryptFileName.size(); m++){
                                if(list_deencryptFileName.get(m).toString().equals(mDeleteFileName + "\r\n")){
                                    list_deencryptFileName.remove(m);
                                    break;
                                }
                            }
                        }
                    }
                }
                listDeEncryptItem.remove(i - j);
                j++;
            }
        }
        FileOutputStream outputStream = new FileOutputStream(bt_recovery_dir_path);
        if(list.size() > 0){
            for (String s : list) {
                outputStream.write(s.getBytes());
            }
        }
        outputStream.close();

        FileOutputStream outputStream_deencryptedname = new FileOutputStream(DeEncryptFile);
        if(list_deencryptFileName.size() >= 0){
            for (String s : list_deencryptFileName) {
                outputStream_deencryptedname.write(s.getBytes());
            }
        }
        outputStream_deencryptedname.close();

        updateListView();
    }

    public class EncryptAdapter extends BaseAdapter {
        private Context mContext;
        private List<HashMap<String, Object>> dataList;
        protected final static String BUNDLE_KEY_LISTDATA = "listdata";

        public EncryptAdapter(Context context, List<HashMap<String, Object>> dataList) {
            this.mContext = context;
            this.dataList = dataList;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return dataList.size();
        }

        @Override
        public Object getItem(int item) {
            // TODO Auto-generated method stub
            return dataList.get(item);
        }

        @Override
        public long getItemId(int itemId) {
            // TODO Auto-generated method stub
            return itemId;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listview_item, null);
                holder = new ViewHolder();
                holder.image = (ImageView) convertView.findViewById(R.id.list_imageView1);
                holder.filepath_text = (TextView) convertView.findViewById(R.id.list_textView);
                holder.btn_Check_Encrypt = (ImageButton) convertView.findViewById(R.id.btn_check_encrypt);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.image.setImageResource((Integer) dataList.get(position).get("image"));
            holder.filepath_text.setText((CharSequence) dataList.get(position).get("mFilePath"));
            holder.btn_Check_Encrypt.setImageResource((Integer) dataList.get(position).get("mIsChecked"));

            if(listDeEncryptItem.get(position)){
                String temp = holder.filepath_text.getText().toString().replaceAll("\r|\n","");
                int start = temp.lastIndexOf(".");
                int end = temp.length();
                String nailname = null;
                if (start != -1 && end != -1) {
                    nailname = temp.substring(start + 1, end).toString();
                }
                if (nailname.equals("jpg") || nailname.equals("png") || nailname.equals("jpeg") || nailname.equals("bmp")) {
                    Bitmap bitmap = getImageThumbnail(temp,30,30);
                    if(bitmap!=null) {
                        holder.image.setImageBitmap(bitmap);
                    }
                } else if (nailname.equals("3gp") || nailname.equals("mp4") || nailname.equals("avi") || nailname.equals("wmv")) {
                    Bitmap bitmap = getVideoThumbnail(temp,30,30, MediaStore.Images.Thumbnails.MICRO_KIND);
                    if(bitmap!=null) {
                        holder.image.setImageBitmap(bitmap);
                    }
                }

            }

            if(mBoolActionMode == true){
                if(mPicsVideoLockListView.isItemChecked(position)){
                    holder.btn_Check_Encrypt.setVisibility(View.VISIBLE);
                    holder.btn_Check_Encrypt.setImageResource(R.drawable.btn_check_on);
                }else{
                    holder.btn_Check_Encrypt.setVisibility(View.VISIBLE);
                    holder.btn_Check_Encrypt.setImageResource(R.drawable.btn_check_off);
                }
            }else{
                holder.btn_Check_Encrypt.setVisibility(View.GONE);
            }
            return convertView;

        }

        private final class ViewHolder {
            ImageView image;
            TextView filepath_text;
            ImageButton btn_Check_Encrypt;
        }
    }

    private Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false; // 设为 false
        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / width;
        int beHeight = h / height;
        int be = 1;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    private Bitmap getVideoThumbnail(String videoPath, int width, int height, int kind) {
        Bitmap bitmap = null;
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        System.out.println("w"+bitmap.getWidth());
        System.out.println("h"+bitmap.getHeight());
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

     private void refreshGallery(List<String> fileNamePath){
         Intent intentRefresh = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
         String tempPath = null;
         for(int i = 0; i < fileNamePath.size(); i++){
             tempPath = fileNamePath.get(i);
             tempPath = tempPath.replaceAll("\r|\n","");
             Uri uriRecovery = Uri.fromFile(new File(tempPath));
             intentRefresh.setData(uriRecovery);
             sendBroadcast(intentRefresh);
         }

     }

    /**
     * delete the thumbnails image who related with source file
     * String sourceFilePath source file path
     **/
    private void deleteThumbnailsImage(String sourceFilePath) {

        String fileNailName = FileUtils.getNailName(sourceFilePath);     //get the file nailname
        Log.d(TAG,"file type is : " + fileNailName);

        //throught the file nailname to distinguish the file type
        int imageOrVideoId = -1;
        if (fileNailName.equals("jpg") || fileNailName.equals("png")
                || fileNailName.equals("jpeg") || fileNailName.equals("bmp")) {    //delete the image type file thumbnails image
            String columns[] = new String[] { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};
            Cursor cursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    columns, null, null, null);

            if (cursor !=null) {
                cursor.moveToFirst();
                do {
                    //get the images._id connect with source file
                    if(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)).equals(sourceFilePath)) {     //check the source Image Id in MediaStore.Images.Media
                        imageOrVideoId = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));     //get the source Image Id
                        Log.d(TAG,"file path is : " + sourceFilePath +" and file _id is : " + imageOrVideoId);
                        break ;
                    }
                } while (cursor.moveToNext());

            }

            if(imageOrVideoId > 0) {
                String[] projection = {MediaStore.Images.Thumbnails.IMAGE_ID,MediaStore.Images.Thumbnails.DATA};
                Cursor cursorThumbnails = getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                        projection, null, null, null);
                if (cursorThumbnails != null) {
                    cursorThumbnails.moveToFirst();
                    do {
                        //thumbnails.image_id and images._id related together
                        if (cursorThumbnails.getInt(cursorThumbnails.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID))
                                == imageOrVideoId) {     //check the source file Id isn't same with thumbnails image_id
                            String strThumbnailImagePath = cursorThumbnails.getString(
                                    cursorThumbnails.getColumnIndex(MediaStore.Images.Thumbnails.DATA));   //get the thumbnails image path
                            Log.d(TAG,"thumbnails Image path is :" + strThumbnailImagePath);
                            File thumbnailsImageFile = new File(strThumbnailImagePath);
                            if(thumbnailsImageFile.exists()) {
                                boolean deleteThumbnailsIsSucess = false;
                                deleteThumbnailsIsSucess = thumbnailsImageFile.delete();    //delete thumbnails iamge
                                Log.d(TAG,"thumbnails Image delete is :" + deleteThumbnailsIsSucess);
                            }
                            break;
                        }
                    } while (cursorThumbnails.moveToNext());
                }
            }
        } else if (fileNailName.equals("3gp") || fileNailName.equals("mp4")
                || fileNailName.equals("avi") || fileNailName.equals("wmv")) {    //delete the video type file thumbnails image
            String columns[] = new String[] { MediaStore.Video.Media.DATA, MediaStore.Video.Media._ID};
            Cursor cursor = this.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    columns, null, null, null);

            if (cursor !=null) {
                cursor.moveToFirst();
                do {
                    //get the images._id connect with source file
                    if(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)).equals(sourceFilePath)) {     //check the source Image Id in MediaStore.Images.Media
                        imageOrVideoId = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media._ID));     //get the source Image Id
                        Log.d(TAG,"file path is : " + sourceFilePath +" and file _id is : " + imageOrVideoId);
                        break ;
                    }
                } while (cursor.moveToNext());
            }

            if(imageOrVideoId > 0) {
                String[] projection = {MediaStore.Video.Thumbnails.VIDEO_ID,MediaStore.Video.Thumbnails.DATA};
                Cursor cursorThumbnails = getContentResolver().query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                        projection, null, null, null);
                if (cursorThumbnails != null) {
                    cursorThumbnails.moveToFirst();
                    do {
                        //thumbnails.image_id and images._id related together
                        if (cursorThumbnails.getInt(cursorThumbnails.getColumnIndex(MediaStore.Video.Thumbnails.VIDEO_ID))
                                == imageOrVideoId) {     //check the source file Id isn't same with thumbnails image_id
                            String strThumbnailImagePath = cursorThumbnails.getString(
                                    cursorThumbnails.getColumnIndex(MediaStore.Video.Thumbnails.DATA));   //get the thumbnails image path
                            Log.d(TAG,"video_thumbnails Image path is :" + strThumbnailImagePath);
                            File thumbnailsImageFile = new File(strThumbnailImagePath);
                            if(thumbnailsImageFile.exists()) {
                                boolean deleteThumbnailsIsSucess = false;
                                deleteThumbnailsIsSucess = thumbnailsImageFile.delete();    //delete thumbnails iamge
                                Log.d(TAG,"video_thumbnails Image delete is :" + deleteThumbnailsIsSucess);
                            }
                            break;
                        }
                    } while (cursorThumbnails.moveToNext());
                }
            }
        } else {
            //not the media type, it hasn't thumbnails image
            Log.d(TAG,"file type is : " + fileNailName + " , not a Media type, it hasn't thumbnail image.");
        }

        return;
    }

    public boolean isSystemApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    public boolean isSystemUpdateApp(PackageInfo pInfo) {
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
    }

    public boolean isUserApp(){

        boolean boolUserApp = true;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(AppPathName, 0);
            boolUserApp = (!isSystemApp(pInfo) && !isSystemUpdateApp(pInfo));
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return boolUserApp;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(AppUtil.onBackKeyDown(this, keyCode, event)){
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
