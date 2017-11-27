package com.tcl.smartapp.domain;

import android.graphics.Bitmap;

import java.util.Date;

/**
 * Created  on 5/24/16.
 */
public class ImageInfo {
    private Bitmap image;
    private Date createTime;
    private String fileName;
    private boolean pendingDel;

    public ImageInfo(Bitmap pic, String fName, Date cTime) {
        image = pic;
        fileName = fName;
        createTime = cTime;
        pendingDel = false;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean getPendingDel() {
        return pendingDel;
    }

    public void setPendingDel(boolean del){
        pendingDel = del;
    }

}
