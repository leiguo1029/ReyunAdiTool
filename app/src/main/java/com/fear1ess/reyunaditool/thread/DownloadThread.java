package com.fear1ess.reyunaditool.thread;

import android.content.Context;

import android.os.Handler;
import android.util.Log;

import com.fear1ess.reyunaditool.AppInfo;
import com.fear1ess.reyunaditool.DoCommandService;
import com.fear1ess.reyunaditool.ExecuteCmdUtils;
import com.fear1ess.reyunaditool.NetWorkUtils;
import com.fear1ess.reyunaditool.state.AppState;
import com.fear1ess.reyunaditool.utils.PushMsgUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


public class DownloadThread extends Thread {
    private Handler mainUiHandler;
    private Context appContext;
    private DoCommandService mService;
    private volatile boolean needRunning = true;

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
        NetWorkUtils.Response res = NetWorkUtils.download(downloadUrl,null,null,downloadPath);
        if(res == null) return null;
        return res.resFileName;
    }

    public void renameFile(String srcPath,String reNamePath){
        File fi = new File(srcPath);
        File fi2 = new File(reNamePath);
        fi.renameTo(fi2);
    }

    public void exit() {
        needRunning = false;
    }

    public void handleLocalApp(){
        File fi = new File(downloadDir);
        String[] fileNames = fi.list();
        if(fileNames == null) return;
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
                mService.sendMsgToClient(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void downloadAppLoop(){
        while(needRunning){
            try {
                Thread.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            setNetWorkCallback();

            String ts = String.valueOf(System.currentTimeMillis());
            String downloadPath = downloadDir + "/" + "rycache_" + ts + ".apk";
            String pkgName = downloadApp(downloadAppUrlStr, downloadPath);

            if(pkgName == null) {
                ExecuteCmdUtils.deletePkg(downloadPath);
                continue;
            }
            String newPath = downloadDir + "/" + pkgName + ".apk";
            renameFile(downloadPath, newPath);

            uploadDownLoadSuccessData(pkgName);

            try {
                mAppInfoQueue.put(new AppInfo(pkgName, newPath));
                String msg = PushMsgUtils.createPushMsg(pkgName, null, null,
                        AppState.APP_DOWNLOADED_AND_PARPARE_TO_INSTALL);
                mService.sendMsgToClient(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadDownLoadSuccessData(String pkgName) {
        Log.d(TAG, "start upload download success data...");
        ExecutorService es = Executors.newSingleThreadExecutor();
        JSONObject jo = new JSONObject();
        try {
            jo.put("app_id", pkgName);
            jo.put("download_success", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        es.execute(new UploadAdsDataProceduce(jo.toString()));
        es.shutdown();
    }

    public void setNetWorkCallback() {
        NetWorkUtils.setNetWorkCallback(new NetWorkUtils.NetWorkCallback() {
            @Override
            public void onDownloadPkgNameGetSuccess(String name) {
                String msg = PushMsgUtils.createPushMsg(name, null, null, 0,
                        AppState.APP_DOWNLOADING);
                mService.sendMsgToClient(msg);
            }

            @Override
            public void onDownloadPkgNameGetfailed() {
            }

            @Override
            public void onDownloadBytesUpdate(String name, int bytesDownloaded) {
                String msg = PushMsgUtils.createPushMsg(name, null, null, bytesDownloaded,
                        AppState.APP_DOWNLOADING);
                mService.sendMsgToClient(msg);
            }
        });
    }

    @Override
    public void run() {

        //handle local app
        handleLocalApp();

        //download app loop...
        downloadAppLoop();
    }
}
