package com.tcl.smartapp.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.SurfaceHolder;

import com.tcl.smartapp.domain.ImageInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created on 5/12/16.
 */
public class CameraManager {
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private static final String TAG = "CameraManager";
    private static boolean mIsWorking = false;
    private static boolean mIsPreviewStarted = false;
    private static boolean mIsAutoFocused = false;
    /**
     * 定义图片保存的路径和图片的名字
     */
    public final static String PHOTO_PATH = "/sdcard/com.tcl.smartapp/";

    /**
     * 自动对焦的回调方法，用来处理对焦成功/不成功后的事件
     */
    private Camera.AutoFocusCallback mAutoFocus = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (camera != null) {
                mIsAutoFocused = true;
            }
        }
    };

    public CameraManager(SurfaceHolder holder) {
        mHolder = holder;
    }

    /**
     * 打开相机
     *
     * @param tagInfo 摄像头信息，分为前置/后置摄像头 Camera.CameraInfo.CAMERA_FACING_FRONT：前置
     *                Camera.CameraInfo.CAMERA_FACING_BACK：后置
     * @return 是否成功打开某个摄像头
     */
    private boolean openCamera(int tagInfo) {
        mIsPreviewStarted = false;
        mIsAutoFocused = false;
        mIsWorking = true;

        // 尝试开启摄像头
        try {
            mCamera = Camera.open(getCameraId(tagInfo));
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
        // 开启前置失败
        if (mCamera == null) {
            return false;
        }
        // 将摄像头中的图像展示到holder中
        try {
            // 这里的myCamera为已经初始化的Camera对象
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
            // 如果出错立刻进行处理，停止预览照片
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        if (mCamera != null) {
            mCamera.setPreviewCallback(new PicPriviewCallback());
            return true;
        } else {
            return false;
        }
    }

    private void startPreview() {
        mIsPreviewStarted = false;
        Log.d(TAG, "mCamera:" + mCamera);
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    private void autoFocus() {
        mIsAutoFocused = false;
        Log.d(TAG, "mCamera:" + mCamera + ",mIsPreviewStarted:" + mIsPreviewStarted);
        if (mCamera != null && mIsPreviewStarted) {
            mCamera.autoFocus(mAutoFocus);
        }
    }

    private void takePhoto() {
        Log.d(TAG, "mCamera:" + mCamera + ",mIsPreviewStarted:" + mIsPreviewStarted + ",mIsAutoFocused:" + mIsAutoFocused);
        if (mCamera != null && mIsPreviewStarted && mIsAutoFocused) {
            mCamera.takePicture(null, null, new PicCallback(mCamera));
        }
    }

    public void takeFrontPhotoWork() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DelPicOver();
                int step = 0;
                //this step max 20s, 1s one time
                for (int i = 0; i < 20; i++) {
                    if (!mIsWorking) {
                        if (openCamera(getFrontCameraId())) {
                            Log.d(TAG, "startPreview");
                            startPreview();
                            step++;
                            break;
                        } else {
                            Log.d(TAG, "openCamera failed!");
                            return;
                        }
                    } else {
                        Log.d(TAG, "working wait " + i + " times");
                        try {
                            //waiting resource
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                //last step is not started
                if (step <= 0) {
                    Log.d(TAG, "don't finish takeFrontPhotoWork in 20s.");
                    return;
                }
                //this step max 5s, 200ms one time
                for (int i = 0; i < 25; i++) {
                    if (mIsPreviewStarted) {
                        Log.d(TAG, "autoFocus");
                        autoFocus();
                        step++;
                        break;
                    } else {
                        Log.d(TAG, "starting preview wait " + i + " times");
                        try {
                            //waiting resource
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                //last step is not started
                if (step <= 1) {
                    Log.d(TAG, "don't finish start preview in 5s.");
                    return;
                }
                //this step max 5s, 200ms one time
                for (int i = 0; i < 10; i++) {
                    if (mIsAutoFocused) {
                        Log.d(TAG, "takePhoto");
                        takePhoto();
                        step++;
                        break;
                    } else {
                        Log.d(TAG, "doing auto focus wait " + i + " times");
                        try {
                            //waiting resource
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                //last step is not started
                if (step <= 2) {
                    Log.d(TAG, "don't finish auto focus in 5s.");
                    return;
                }
            }
        }).start();
    }

    /**
     * @return 前置摄像头的ID
     */
    private int getFrontCameraId() {
        return getCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    /**
     * @return 后置摄像头的ID
     */
    private int getBackCameraId() {
        return getCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    /**
     * @param tagInfo
     * @return 得到特定camera info的id
     */
    private int getCameraId(int tagInfo) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        // 开始遍历摄像头，得到camera info
        int cameraId, cameraCount;
        for (cameraId = 0, cameraCount = Camera.getNumberOfCameras(); cameraId < cameraCount; cameraId++) {
            Camera.getCameraInfo(cameraId, cameraInfo);

            if (cameraInfo.facing == tagInfo) {
                break;
            }
        }
        return cameraId;
    }

    private static String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'LOCK'_yyyyMMdd_HHmmss");
        return dateFormat.format(date) + ".jpg";
    }

    /**
     * 拍照成功回调
     */
    private class PicCallback implements PictureCallback {
        private Camera mCamera;

        public PicCallback(Camera camera) {
            // TODO 自动生成的构造函数存根
            mCamera = camera;
        }

        /*
         * 将拍照得到的字节转为bitmap，然后旋转，接着写入SD卡
         * @param data
         * @param camera
         */
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // 将得到的照片进行270°旋转，使其竖直
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.preRotate(270);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            // 创建并保存图片文件
            File mFile = new File(PHOTO_PATH);
            if (!mFile.exists()) {
                mFile.mkdirs();
            }
            File pictureFile = new File(PHOTO_PATH, getPhotoFileName());
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                bitmap.recycle();
                fos.close();
                Log.i(TAG, "take photo OK！");
            } catch (Exception error) {
                Log.e(TAG, "take photo failed");
                error.printStackTrace();
            } finally {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                mIsWorking = false;
            }
        }

    }

    private class PicPriviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (camera != null) {
                mIsPreviewStarted = true;
            }
        }
    }

    private void DelPicOver() {
        File dir = new File(PHOTO_PATH);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files == null || files.length <= 0) {
                return;
            }
            List<String> mFileNames = new ArrayList<>();
            for (File file : files) {
                mFileNames.add(file.getName());
            }
            if (mFileNames.size() >= Constants.WARNING_PIC_MAX_NUM) {
                Collections.sort(mFileNames, MyComparator);
                for (int i = (Constants.WARNING_PIC_MAX_NUM - 1); i < mFileNames.size(); i++) {
                    File delFile = new File(PHOTO_PATH + "/" + mFileNames.get(i));
                    if (delFile.exists()) {
                        delFile.delete();
                    }
                }
            }
            Log.d(TAG, "mFileNames.size():" + mFileNames.size());
        }
    }

    private Comparator<String> MyComparator = new Comparator<String>() {
        @Override
        public int compare(String image1, String image2) {
            return image2.compareTo(image1);
        }
    };

}
