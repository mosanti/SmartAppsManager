package com.tcl.smartapp.utils;

/**
 * Created by user on 5/4/16.
 */
public class LockParams {
    public enum LockType {
        PATTERN,
        PIN
    }

    public static LockType mLockType = LockType.PATTERN;
}
