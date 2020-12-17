package com.fear1ess.reyunaditool.thread;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import com.fear1ess.reyunaditool.AppInfo;
import com.fear1ess.reyunaditool.DoCommandService;
import com.fear1ess.reyunaditool.ExecuteCmdUtils;
import com.fear1ess.reyunaditool.NetWorkUtils;
import com.fear1ess.reyunaditool.state.AppState;
import com.fear1ess.reyunaditool.utils.PushMsgUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;


public class DownloadThread extends Thread {
    private Handler mainUiHandler;
    private Context appContext;
    private DoCommandService mService;

    private int MAX_APPHANDLE_SIZE = 2;
    public LinkedBlockingQueue<AppInfo> mAppInfoQueue = new LinkedBlockingQueue<>(MAX_APPHANDLE_SIZE);

    public static String downloadDir = "/sdcard/reyundownload";

    public static String TAG = "reyunaditool_log";

    public static String downloadAppUrlStr = "http://adfly-api.adinsights-global.com/get_app_res/?count=1&ua=android";

    static{
        File fi = new File(downloadDir);
        if(!fi.exists()) fi.mkdirs();
    }

    public DownloadThread(Context cxt, Handler handler, DoCommandService doCommandService) {
        appContext = cxt;
        mainUiHandler = handler;
        mService = doCommandService;
    }

    public String downloadApp(String downloadUrl, String downloadPath) {
        Log.d(TAG, "downloadApp...");
        String msg = PushMsgUtils.createPushMsg("unknown package", null, null,
                AppState.APP_INSTALLING);
        try {
            mService.getWebSocket().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        NetWorkUtils.Response res = NetWorkUtils.download(downloadUrl,null,null,downloadPath);
        if(res == null) return null;
        return res.resFileName;
    }

    public void renameFile(String srcPath,String reNamePath){
        File fi = new File(srcPath);
        File fi2 = new File(reNamePath);
        fi.renameTo(fi2);
    }

    public void handleLocalApp(){
        File fi = new File(downloadDir);
        String[] fileNames = fi.list();
        for(String fileName : fileNames){
            String packageName = fileName.replace(".apk","");
            String downloadPath = downloadDir + "/" + fileName;
            try {
                if(packageName.startsWith("rycache_")){
                    Log.d(TAG, "find app cache,delete it...");
                    ExecuteCmdUtils.deletePkg(downloadPath);
                    continue;
                }
                mAppInfoQueue.put(new AppInfo(packageName, downloadPath));
                String msg = PushMsgUtils.createPushMsg(packageName, null, null,
                        AppState.APP_DOWNLOADED_AND_PARPARE_TO_INSTALL);
                mService.getWebSocket().send(msg);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void downloadAppLoop(){
        while(true){
            try {
                Thread.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String ts = String.valueOf(System.currentTimeMillis());
            String downloadPath = downloadDir + "/" + "rycache_" + ts + ".apk";
            String res = null;
            res = downloadApp(downloadAppUrlStr, downloadPath);

            if(res == null) {
                ExecuteCmdUtils.deletePkg(downloadPath);
                continue;
            }
            String newName = res.replace("attachment; filename=","");
            String newPath = downloadDir + "/" + newName;
            renameFile(downloadPath, newPath);
            String pkgName = newName.replace(".apk","");

            try {
                mAppInfoQueue.put(new AppInfo(pkgName, newPath));
                String msg = PushMsgUtils.createPushMsg(pkgName, null, null,
                        AppState.APP_DOWNLOADED_AND_PARPARE_TO_INSTALL);
                mService.getWebSocket().send(msg);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {

        //handle local app
        handleLocalApp();

        //download app loop...
        downloadAppLoop();
    }
}
