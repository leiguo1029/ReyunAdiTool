package com.fear1ess.reyunaditool.utils;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Base64;

import com.fear1ess.reyunaditool.AdiToolApp;
import com.fear1ess.reyunaditool.AppInfo;
import com.fear1ess.reyunaditool.cmd.WSConnectCmd.ServerCmd;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class PushMsgUtils {
    public static String createPushMsg(String pkgName, String appName, Drawable icon, int state){
        if(pkgName == null) return null;
        try {
            JSONObject jo = new JSONObject();
            jo.put("package_name",pkgName);
            jo.put("state", state);
            if(appName != null){
                jo.put("app_name", appName);
            }
            if(icon != null){
                Bitmap bm = getAppIconBitmap(icon);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                String icon_b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
                jo.put("icon", icon_b64);
                bos.close();
            }
            JSONObject jo2 = new JSONObject();
            jo2.put("data", jo);
            jo2.put("cmd", ServerCmd.NEW_PUSH_MSG);
            return jo2.toString();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getAppIconBitmap(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            } else if (drawable instanceof AdaptiveIconDrawable) {
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                return bitmap;
            }
        } else {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        return null;
    }

    public static ApplicationInfo getApplicationInfo(String pkgName){
        PackageManager pm = AdiToolApp.getAppContext().getPackageManager();
        List<ApplicationInfo> packageInfos = pm.getInstalledApplications(0);
        for(ApplicationInfo ai : packageInfos) {
            if(pkgName.equals(ai.packageName)) return ai;
            /*
                Drawable icon = ai.loadIcon(pm);
            String appName = appInfo.loadLabel(pm).toString();
            appInfos.add(new AppInfo(pkgName,appName,icon));*/
        }
        return null;
    }
}
