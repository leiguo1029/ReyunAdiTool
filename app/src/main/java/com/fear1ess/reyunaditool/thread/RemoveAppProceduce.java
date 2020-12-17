package com.fear1ess.reyunaditool.thread;

import android.util.Log;

import com.fear1ess.reyunaditool.DoCommandService;
import com.fear1ess.reyunaditool.ExecuteCmdUtils;
import com.fear1ess.reyunaditool.server.NanoWSD;
import com.fear1ess.reyunaditool.state.AppState;
import com.fear1ess.reyunaditool.utils.PushMsgUtils;

import java.io.IOException;

public class RemoveAppProceduce implements Runnable {
    public static String TAG = "reyunaditool_log";
    public String mPkgName;
    public String mDownloadPath;
    public DoCommandService mService;
    private InstallAndStartAppThread mInstallAndStartAppThread;

    public RemoveAppProceduce(InstallAndStartAppThread t, String pkgName, String downloadPath, DoCommandService service){
        mInstallAndStartAppThread = t;
        mPkgName = pkgName;
        mDownloadPath = downloadPath;
        mService = service;
    }

    @Override
    public void run() {
        try {
            NanoWSD.WebSocket webSocket = mService.getWebSocket();
            String msg = PushMsgUtils.createPushMsg(mPkgName, null, null,
                    AppState.APP_REMOVING);
            mService.getWebSocket().send(msg);
            removeApp();
            msg = PushMsgUtils.createPushMsg(mPkgName, null, null, AppState.APP_REMOVED);
            mService.getWebSocket().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
