package com.xt.common.utils;

import android.util.Log;

import com.xt.samplebleconnect.BuildConfig;

/**
 * @author xt on 2020/5/7 13:16
 */
public class MyLogUtils {
    private static String getMsg(String msg) {
        return " \n\n======================================================================================\n" + msg + "\n======================================================================================\n ";
    }

    public static void d(String tag, String msg) {
        Log.d(tag, getMsg(msg));
    }

    public static void e(String tag, Throwable tr) {
        Log.e(tag, getMsg(Log.getStackTraceString(tr)));
    }
}
