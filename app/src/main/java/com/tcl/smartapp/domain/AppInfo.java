package com.tcl.smartapp.domain;

import android.graphics.drawable.Drawable;

/**
 * Created on 4/21/16.
 */
public class AppInfo {
    private String packageName;
    private String name;
    private Drawable appIcon;
    private boolean isLocked;

    public AppInfo(String packageName, String name, Drawable appIcon,
                   boolean locked) {
        this.name = name;
        this.packageName = packageName;
        this.appIcon = appIcon;
        this.isLocked = locked;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    public Drawable getAppIcon() {
        return appIcon;
    }
    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public void setLocked(boolean locked) { isLocked = locked;}
    public boolean isLocked() { return isLocked;}
    @Override
    public String toString() {
        return "AppInfo [name=" + name + ", packageName=" + packageName
                + ", appIcon=" + appIcon + ", isLocked=" + isLocked +"]";
    }
}
