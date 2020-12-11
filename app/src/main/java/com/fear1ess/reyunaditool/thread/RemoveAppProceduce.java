package com.fear1ess.reyunaditool.thread;

import android.util.Log;

import com.fear1ess.reyunaditool.ExecuteCmdUtils;

public class RemoveAppProceduce implements Runnable {
    public static String TAG = "reyunaditool_log";
    public String mPkgName;
    public String mDownloadPath;
    private InstallAndStartAppThread mInstallAndStartAppThread;

    public RemoveAppProceduce(InstallAndStartAppThread t, String pkgName, String downloadPath){
        mInstallAndStartAppThread = t;
        mPkgName = pkgName;
        mDownloadPath = downloadPath;
    }

    @Override
    public void run() {
        removeApp();
    }

    public void removeApp(){
        if(ExecuteCmdUtils.finishApp(mPkgName) != 0){
            Log.e(TAG, "finish app " + mPkgName + " failed!");
        }
        if(ExecuteCmdUtils.uninstallApp(mPkgName) != 0){
            Log.e(TAG, "uninstall app " + mPkgName + " failed!");
        }
        if(ExecuteCmdUtils.deletePkg(mDownloadPath) != 0){
            Log.e(TAG, "delete pkg " + mDownloadPath + " failed!");
        }

        mInstallAndStartAppThread.setNeedWait(false);
    }
}
