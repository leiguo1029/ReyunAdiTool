package com.fear1ess.reyunaditool.thread;

import android.util.Log;

import com.fear1ess.reyunaditool.NetWorkUtils;

public class UploadAdsDataProceduce implements Runnable {
    public static String TAG = "reyunaditool_log";
    public static String postAppInfoUrlStr = "http://adfly-api.adinsights-global.com/get_app_res/?ua=android";

    public String mData;

    public UploadAdsDataProceduce(String str) {
        mData = str;
    }

    @Override
    public void run() {
        Log.d(TAG, "ready to post appInfo to server, data: " + mData);
        NetWorkUtils.post(postAppInfoUrlStr, null, mData.getBytes());
    }
}
