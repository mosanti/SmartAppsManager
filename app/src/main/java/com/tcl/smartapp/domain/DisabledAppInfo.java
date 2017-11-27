package com.tcl.smartapp.domain;

import android.graphics.drawable.Drawable;

/**
 * Created on 4/21/16.
 */
public class DisabledAppInfo {
    private String packageName;
    private String appName;
    private Drawable appIcon;
    private boolean disabled;

    public DisabledAppInfo(String packageName, String appName, Drawable appIcon,
                           boolean disabled) {
        this.appName = appName;
        this.packageName = packageName;
        this.appIcon = appIcon;
        this.disabled = disabled;
    }

    public String getAppName() {
        return appName;
    }
    public void setAppName(String name) {
        this.appName = name;
    }
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packName) {
        this.packageName = packName;
    }
    public Drawable getAppIcon() {
        return appIcon;
    }
    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public void setDisabled(boolean locked) { this.disabled = disabled;}
    public boolean isDisabled() { return this.disabled;}
    @Override
    public String toString() {
        return "DisabledAppInfo [name=" + this.appName + ", packName=" + this.packageName
                + ", appIcon=" + this.appIcon + ", disabled=" + this.disabled +"]";
    }
}
