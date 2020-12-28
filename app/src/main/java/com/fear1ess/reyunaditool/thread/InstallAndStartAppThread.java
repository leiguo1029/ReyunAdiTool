package com.fear1ess.reyunaditool.thread;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.fear1ess.reyunaditool.AdiToolApp;
import com.fear1ess.reyunaditool.AppInfo;
import com.fear1ess.reyunaditool.DoCommandService;
import com.fear1ess.reyunaditool.ExecuteCmdUtils;
import com.fear1ess.reyunaditool.NetWorkUtils;
import com.fear1ess.reyunaditool.cmd.OperateCmd;
import com.fear1ess.reyunaditool.state.AppState;
import com.fear1ess.reyunaditool.utils.PushMsgUtils;

import java.io.IOException;
import java.util.List;


public class InstallAndStartAppThread extends Thread {
    public static String TAG = "reyunaditool_log";
    private String mPkgName;
    private String mDownloadPath;
    private Handler mUiHandler;
    private DoCommandService mService;
    private Context appContext;
    private DownloadThread mDownloadThread;
    private volatile boolean mNeedWait = false;
    private volatile boolean needRunning = true;

    public static String postAppInfoUrlStr = "http://adfly-api.adinsights-global.com/get_app_res/?ua=android";

    public HandleAppHandler hah;

    public InstallAndStartAppThread(DownloadThread dt, Context cxt, Handler uiHandler, DoCommandService service) {
        mDownloadThread = dt;
        mUiHandler = uiHandler;
        mService = service;
        appContext = cxt;
    }

    public void setNeedWait(boolean needWait){
        mNeedWait = needWait;
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
    }

    public void exit() {
        needRunning = false;
    }

    @Override
    public void run() {
        while(needRunning){
            try {
                installAndStartNewApp();
                sleep(80*1000);
                removeApp();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mNeedWait = true;
        }

        /*
        Looper.prepare();

        mService.setHandleAppHandler(new HandleAppHandler());

        mDownloadThread = new DownloadThread(appContext, mUiHandler);

        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(mDownloadThread);
        es.shutdown();

        installAndStartNewApp();

        Looper.loop();*/
    }

    public boolean isAppExists(String pkgName){
        PackageManager pm = AdiToolApp.getAppContext().getPackageManager();
        List<ApplicationInfo> packageInfos = pm.getInstalledApplications(0);
        for(ApplicationInfo ai : packageInfos) {
            if(ai.packageName.equals(pkgName)) return true;
        }
        return false;
    }

    public synchronized void installAndStartNewApp() {
        AppInfo appInfo = null;
        try {
            while(true){
                Thread.sleep(3000);
                appInfo = mDownloadThread.mAppInfoQueue.take();

                mPkgName = appInfo.mPkgName;
                mDownloadPath = appInfo.mDownloadPath;
                mService.setCurApp(mPkgName,mDownloadPath);

                //send app installing msg...
                String msg = PushMsgUtils.createPushMsg(mPkgName, null, null,
                        AppState.APP_INSTALLING);
                mService.sendMsgToClient(msg);


                if(!isAppExists(mPkgName)){
                    int res = ExecuteCmdUtils.installApp(mDownloadPath);
                    if(res == 0){
                        Log.d(TAG, "install app " + mPkgName + "success!");
                    } else {
                        Log.e(TAG, "install app " + mPkgName + " failed!");
                        if(ExecuteCmdUtils.deletePkg(mDownloadPath) != 0){
                            Log.e(TAG, "delete pkg " + mDownloadPath + " failed!");
                        }
                        //send app installing msg...
                        msg = PushMsgUtils.createPushMsg(mPkgName, null, null,
                                AppState.APP_INSTALL_FAILED);
                        mService.sendMsgToClient(msg);
                        continue;
                    }
                }else{
                    Log.d(TAG, mPkgName + " is already existed, skip install");
                }

                //send app installed msg, update appname and appicon...
                PackageManager pm = appContext.getPackageManager();
                ApplicationInfo info = PushMsgUtils.getApplicationInfo(mPkgName);
                msg = PushMsgUtils.createPushMsg(mPkgName, info.loadLabel(pm).toString(), info.loadIcon(pm),
                        AppState.APP_INSTALLED_AND_OPEN);
                mService.sendMsgToClient(msg);

                if(ExecuteCmdUtils.startApp(appContext,mPkgName) != 0){
                    Log.e(TAG, "start app " + mPkgName + " failed!");
                    if(ExecuteCmdUtils.uninstallApp(mPkgName) != 0){
                        Log.e(TAG, "uninstall app " + mPkgName + " failed!");
                    }
                    if(ExecuteCmdUtils.deletePkg(mDownloadPath) != 0){
                        Log.e(TAG, "delete pkg " + mDownloadPath + " failed!");
                    }
                    continue;
                }

                //send checking msg
                msg = PushMsgUtils.createPushMsg(mPkgName, null, null,
                        AppState.APP_ADSDK_CHECKING);
                mService.sendMsgToClient(msg);
                break;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }





    public class HandleAppHandler extends Handler{

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case OperateCmd.SHUTDOWN_APP: {
                    Log.d(TAG, "handleMessage: shutdown app");
                    if(ExecuteCmdUtils.finishApp(mPkgName) != 0){
                        Log.e(TAG, "finish app " + mPkgName + " failed!");
                    }
                    if(ExecuteCmdUtils.uninstallApp(mPkgName) != 0){
                        Log.e(TAG, "uninstall app " + mPkgName + " failed!");
                    }
                    if(ExecuteCmdUtils.deletePkg(mDownloadPath) != 0){
                        Log.e(TAG, "delete pkg " + mDownloadPath + " failed!");
                    }
                    installAndStartNewApp();
                    break;
                }

                case OperateCmd.UPLOAD_ADSDK_INFO: {
                    Log.d(TAG, "handleMessage: upload data");
                    byte[] data = msg.getData().getByteArray("data");
                    Log.d(TAG, "ready to post appInfo to server,data: " + new String(data));
                    NetWorkUtils.post(postAppInfoUrlStr, null, data);
                    break;
                }

                default: break;
            }
        }
    }
}


//download app...
        /*
        NetWorkUtils.Response res = NetWorkUtils.get(urlStr1,null);
        if(res.resCode != 200){
            Message message = Message.obtain();
            message.what = MainActivity.MainActivityHandler.NETWORK_ERROR;
            Bundle bundle = new Bundle();
            bundle.putCharSequence("errText","获取app下载链接失败！请检查网络连接");
            message.setData(bundle);
            mainUiHandler.sendMessage(message);
            return;
        }
        String htmlStr = new String(res.resData);
        Document doc = Jsoup.parse(htmlStr);
        Element element = doc.select("a[id=download_link]").get(0);
        String downloadUrl = element.attr("href");
        mAppName = "test001";
        String downloadPath = "/sdcard/" + mAppName.replace(' ','_') + ".apk";*/