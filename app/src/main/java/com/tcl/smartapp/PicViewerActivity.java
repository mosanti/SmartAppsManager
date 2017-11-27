package com.tcl.smartapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tcl.smartapp.domain.ImageInfo;
import com.tcl.smartapp.token.TokenUtils;
import com.tcl.smartapp.utils.Constants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created  on 5/24/16.
 */
public class PicViewerActivity extends Activity {
    private static final String TAG = "PicViewerActivity";
    private static final String WarningPicPath = "/sdcard/" + Constants.MY_PACKAGE_NAME;
    private ListView mLvImages;
    private TextView mTv_empty;
    private TextView mTv_loading;
    private List<ImageInfo> mImageInfoList;
    private static int mPendingDelNum = 0;
    private MyAdapter adapter = null;
    private Context mContext;
    private Handler mUIHandler = null;
    private static final int MSG_UPDATE_UI = 10001;
    private static final int MSG_EMPTY_FOLDER = 10002;
    private static final String JPEG_TYPE = ".jpg";
    private static final String LOCK_PIC_PRE_TEXT = "LOCK_";
    private boolean mIsDelMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mUIHandler = new UIHandler();

        setContentView(R.layout.warning_pic_viewer);
        mLvImages = (ListView) findViewById(R.id.lv_image);
        mTv_empty = (TextView) findViewById(R.id.tv_empty);
        mTv_loading = (TextView) findViewById(R.id.tv_loading);

        mLvImages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick position:" + position);
                if (mImageInfoList == null) {
                    return;
                }
                if (position < mImageInfoList.size()) {
                    if (mIsDelMode) {
                        ImageInfo info = mImageInfoList.get(position);

                        ViewHolder holder = (ViewHolder) view.getTag();
                        boolean checked = holder.cb_del.isChecked();
                        if (checked) {
                            holder.cb_del.setChecked(false);
                            info.setPendingDel(false);
                            mPendingDelNum--;
                        } else {
                            holder.cb_del.setChecked(true);
                            info.setPendingDel(true);
                            mPendingDelNum++;
                        }
                        Log.d(TAG, "mPendingDelNum:" + mPendingDelNum);
                        updateMenu();
                    } else {
                        ImageInfo info = mImageInfoList.get(position);
                        String fileName = info.getFileName();

                        Log.d(TAG, "onItemClick packageName:" + fileName);
                        File file = new File(WarningPicPath + "/" + fileName);
                        Log.d("path", file.toString());
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(file), "image/*");
                        /**
                         * startActivity will print Throwable as below.
                         * As we use the File path in Intent. We can ignore this.
                         * java.lang.Throwable: file:// Uri exposed through Intent.getData()
                         * */
                        startActivity(intent);

                    }
                }
            }
        });
        mIsDelMode = false;
        mPendingDelNum = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLvImages.setVisibility(View.GONE);
        mTv_empty.setVisibility(View.GONE);
        mTv_loading.setVisibility(View.VISIBLE);
        getFiles();
        TokenUtils.startSelfLock(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        menu.findItem(R.id.actionBar_delete).setVisible(true);
        menu.findItem(R.id.actionBar_select_all).setVisible(true);
        menu.findItem(R.id.actionBar_unselect_all).setVisible(true);
        if (mIsDelMode) {
            if (mImageInfoList == null || mImageInfoList.size() <= mPendingDelNum) {
                menu.findItem(R.id.actionBar_select_all).setVisible(false);
                Log.d(TAG, "remove select_all");
            }
            if (mPendingDelNum == 0) {
                menu.findItem(R.id.actionBar_unselect_all).setVisible(false);
                Log.d(TAG, "remove unselect_all");
            }
        } else {
            if (mImageInfoList == null || mImageInfoList.size() == 0) {
                Log.d(TAG, "remove delete");
                menu.findItem(R.id.actionBar_delete).setVisible(false);
            }
            menu.findItem(R.id.actionBar_select_all).setVisible(false);
            menu.findItem(R.id.actionBar_unselect_all).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void updateMenu() {
        Log.d(TAG, "updateMenu");
        getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionBar_delete:
                Log.d(TAG, "click del");
                if (!mIsDelMode) {
                    enterDelMode();
                    updateUI();
                    break;
                }

                if (mPendingDelNum <= 0) {
                    Toast.makeText(this, R.string.del_list_empty, Toast.LENGTH_SHORT).show();
                    break;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.confirmDelTitle);
                String message = String.format(this.getString(R.string.confirmDelMessage),
                        mPendingDelNum);
                builder.setMessage(message);

                // Cancel, do nothing
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                // confirm delete
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mLvImages.setVisibility(View.GONE);
                        mTv_empty.setVisibility(View.GONE);
                        mTv_loading.setVisibility(View.VISIBLE);
                        dialog.dismiss();
                        confirmDelFile();
                        exitDelMode();
                        getFiles();
                    }

                });
                builder.create().setCanceledOnTouchOutside(false);
                builder.create().show();

                break;
            case R.id.actionBar_select_all:
                Log.d(TAG, "click select all");
                selectAll(true);
                updateUI();
                break;
            case R.id.actionBar_unselect_all:
                Log.d(TAG, "click unselect all");
                selectAll(false);
                updateUI();
                break;
            default:
                Log.e(TAG, "unknown menuItem:" + item.getItemId());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateUI() {
        if (mImageInfoList != null) {
            Collections.sort(mImageInfoList, MyComparator);
        }
        Message msg = mUIHandler.obtainMessage(MSG_UPDATE_UI);
        mUIHandler.sendMessage(msg);
    }

    private void showEmptyFolder() {
        Message msg = mUIHandler.obtainMessage(MSG_EMPTY_FOLDER);
        mUIHandler.sendMessage(msg);
    }

    private void enterDelMode() {
        if (mIsDelMode) {
            return;
        }
        selectAll(false);
        mIsDelMode = true;
    }

    private void selectAll(boolean flag) {
        if (mImageInfoList == null) {
            return;
        }
        for (int i = 0; i < mImageInfoList.size(); i++) {
            mImageInfoList.get(i).setPendingDel(flag);
        }
        mPendingDelNum = flag ? mImageInfoList.size() : 0;
    }

    private void exitDelMode() {
        if (!mIsDelMode) {
            return;
        }
        selectAll(false);
        mIsDelMode = false;
    }

    private void confirmDelFile() {
        if (!mIsDelMode) {
            Log.d(TAG, "It not del mode.");
            return;
        }

        if (mPendingDelNum <= 0) {
            Log.d(TAG, "The del file list is empty.");
            return;
        }
        for (int i = 0; i < mImageInfoList.size(); i++) {
            if (!mImageInfoList.get(i).getPendingDel()) {
                continue;
            }
            String fileName = mImageInfoList.get(i).getFileName();
            if (fileName == null || "".equals(fileName)) {
                continue;
            }
            File file = new File(WarningPicPath + "/" + fileName);
            if (file.delete()) {
                Log.d(TAG, "del file[" + fileName + "] OK.");
            } else {
                Log.e(TAG, "del file[" + fileName + "] failed!!");
            }
        }
        mPendingDelNum = 0;
        return;
    }

    private void getFiles() {
        new Thread() {
            @Override
            public void run() {
                File dir = new File(WarningPicPath);
                Log.d(TAG, "getFiles dir.exists():" + dir.exists() + ", dir.isDirectory():"
                        + dir.isDirectory());
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files == null || files.length <= 0) {
                        if(mImageInfoList == null){
                            mImageInfoList = new ArrayList<>();
                        } else {
                            mImageInfoList.clear();
                        }
                        showEmptyFolder();
                        Log.d(TAG, "empty folder!");
                        return;
                    }
                    mImageInfoList = new ArrayList<>();
                    for (int i = 0; i < files.length; i++) {
                        File file = files[i];
                        //file name example:LOCK_20161231_114040.jpg
                        if (file.getName().indexOf(JPEG_TYPE) == -1
                                || file.getName().indexOf(LOCK_PIC_PRE_TEXT) == -1) {
                            Log.d(TAG, "item[" + i + "] invalid file name:" + file.getName());
                            continue;
                        }
                        String temp = file.getName().replace(LOCK_PIC_PRE_TEXT, "");
                        temp = temp.replace(JPEG_TYPE, "");
                        String[] timeStamp = temp.split("_");
                        if (timeStamp.length != 2) {
                            Log.d(TAG, "item[" + i + "] invalid timeStamp:" + file.getName());
                            continue;
                        }

                        String dateStr = timeStamp[0];
                        String timeStr = timeStamp[1];

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                        Date date;
                        try {
                            date = sdf.parse(dateStr + timeStr);
                        } catch (ParseException e) {
                            Log.d(TAG, "item[" + i + "] invalid timeStamp:" + file.getName());
                            continue;
                        }
                        Bitmap bitmap = getimage(WarningPicPath + "/" + file.getName(), 40f, 40f);
                        ImageInfo image = new ImageInfo(bitmap, file.getName(), date);

                        mImageInfoList.add(image);
                    }
                    updateUI();
                } else {//if(dir.isDirectory())
                    if(mImageInfoList == null){
                        mImageInfoList = new ArrayList<>();
                    } else {
                        mImageInfoList.clear();
                    }
                    showEmptyFolder();
                }

            }// run()
        }.start();
    }

    private Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 50) {  //循环判断如果压缩后图片是否大于50kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        try {
            isBm.close();
            baos.close();
        }catch (IOException e){}
        return bitmap;
    }

    /**
     * get image from srcPath, ang compress to the width and height
     */
    private Bitmap getimage(String srcPath, float width, float height) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);//此时返回bm为空
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;

        newOpts.inJustDecodeBounds = false;
        float hh = height;
        float ww = width;
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
    }

    private class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if (mImageInfoList == null) {
                return 0;
            }
            return mImageInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;
            if (convertView != null && convertView instanceof RelativeLayout) {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            } else {
                view = View.inflate(mContext, R.layout.warning_pic_viewer_item, null);
                holder = new ViewHolder();
                holder.iv_pic = (ImageView) view.findViewById(R.id.iv_pic);
                holder.tv_fileName = (TextView) view.findViewById(R.id.tv_fileName);
                holder.cb_del = (CheckBox) view.findViewById(R.id.cb_del);
                view.setTag(holder);
            }
            if (position < mImageInfoList.size()) {
                ImageInfo info = mImageInfoList.get(position);
                holder.iv_pic.setImageBitmap(info.getImage());
                holder.tv_fileName.setText(info.getFileName());
                holder.cb_del.setVisibility(mIsDelMode ? View.VISIBLE : View.GONE);
                holder.cb_del.setChecked(info.getPendingDel());
            } else {
                Log.e(TAG, "MyAdapter-getView Error position:" + position);
            }
            return view;
        }
    }

    private static class ViewHolder {
        ImageView iv_pic;
        TextView tv_fileName;
        CheckBox cb_del;
    }

    private class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_UI:
                    if (adapter == null) {
                        adapter = new MyAdapter();
                        mLvImages.setAdapter(adapter);
                    } else {
                        Log.d(TAG, "change adapter");
                        adapter.notifyDataSetChanged();
                    }
                    if (mImageInfoList == null || mImageInfoList.size() == 0) {
                        mLvImages.setVisibility(View.GONE);
                        mTv_empty.setVisibility(View.VISIBLE);
                    } else {
                        mLvImages.setVisibility(View.VISIBLE);
                        mTv_empty.setVisibility(View.GONE);
                    }
                    mTv_loading.setVisibility(View.GONE);
                    updateMenu();
                    break;
                case MSG_EMPTY_FOLDER:
                    mLvImages.setVisibility(View.GONE);
                    mTv_empty.setVisibility(View.VISIBLE);
                    mTv_loading.setVisibility(View.GONE);
                    if (adapter != null) {
                        Log.d(TAG, "change adapter");
                        adapter.notifyDataSetChanged();
                    }
                    updateMenu();
                    break;
                default:
                    Log.e(TAG, "UIHandler: unknown msg:" + msg.what);
                    break;
            }
        }
    }

    private Comparator<ImageInfo> MyComparator = new Comparator<ImageInfo>() {
        @Override
        public int compare(ImageInfo image1, ImageInfo image2) {
            return image2.getCreateTime().compareTo(image1.getCreateTime());
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mIsDelMode) {
                exitDelMode();
                updateUI();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
