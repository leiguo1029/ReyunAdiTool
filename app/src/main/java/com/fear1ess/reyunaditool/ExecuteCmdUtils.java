package com.fear1ess.reyunaditool;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExecuteCmdUtils {
    public static String TAG = "reyunaditool_log";
    public static int executeCmd(String cmd){
        Process p = null;
        int exitCode = 0;
        try {
            p = Runtime.getRuntime().exec("su -c " + cmd);
            exitCode = p.waitFor();
            if(exitCode != 0){
                byte[] b = new byte[1024];
                InputStream is = p.getErrorStream();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int len = 0;
                while((len = is.read(b)) != -1){
                    bos.write(b,0,len);
                    bos.flush();
                }
                String cmdErrMsg = new String(bos.toByteArray());
                bos.close();
                is.close();
                Log.e(TAG, cmdErrMsg + " when execute cmd: " + cmd);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return exitCode;
    }

    public static int installApp(String downloadPath){
        String cmd = "pm install " + downloadPath;
        Log.d(TAG, "installApp " + downloadPath);
        return executeCmd(cmd);
    }

    public static int finishApp(String packageName){
        String cmd = "am force-stop " + packageName;
        Log.d(TAG, "finishApp " + packageName);
        return executeCmd(cmd);
    }

    public static int uninstallApp(String packageName){
        String cmd = "pm uninstall " + packageName;
        Log.d(TAG, "uninstallapp " + packageName);
        return executeCmd(cmd);
    }

    public static int deletePkg(String downloadPath){
        if(!downloadPath.startsWith("/sdcard/reyundownload")){
            Log.e(TAG, "delete path is not reyundownload!!!" );
            return -1;
        }
        String cmd = "rm -f " + downloadPath;
        return executeCmd(cmd);
    }

    public static int startApp(Context appContext, String packageName) {
        PackageManager pm = appContext.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        if(intent == null) return -1;
        ComponentName cn = intent.getComponent();
        String pkgName = cn.getPackageName();
        String className = cn.getClassName();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(intent);
        return 0;
     //   Log.d(TAG, "startApp " + packageName);
     //   String cmd = "am start -n " + pkgName + "/" + className;
      //  return executeCmd(cmd);
    }
}
