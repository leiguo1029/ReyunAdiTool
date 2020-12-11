package com.fear1ess.reyunaditool;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fear1ess.reyunaditool.MainActivity.MainActivityHandler;

public class DoWorkThread implements Runnable {
    public Handler mainUiHandler = null;
    public Context appContext = null;
    public static String getAppInfoUrlStr = "http://adfly-api.adinsights-global.com/get_app_res/?count=1&ua=android";
    public static String postAppInfoUrlStr = "http://adfly-api.adinsights-global.com/get_app_res/?ua=android";

    public DoWorkThread(Context cxt,Handler handler){
        mainUiHandler = handler;
        appContext = cxt;
    }

    @Override
    public void run() {

        NetWorkUtils.Response res = NetWorkUtils.get(getAppInfoUrlStr,null);
        if(res.resCode != 200){
            Message message = Message.obtain();
            message.what = MainActivityHandler.NETWORK_ERROR;
            Bundle bundle = new Bundle();
            bundle.putCharSequence("errText","获取app下载列表失败！请检查网络连接");
            message.setData(bundle);
            mainUiHandler.sendMessage(message);
            return;
        }
        try {
            JSONArray appInfoArray = new JSONObject(new String(res.resData)).getJSONArray("res");
            ExecutorService es = Executors.newSingleThreadExecutor();
            for(int i = 0;i<appInfoArray.length();++i){
                JSONObject item = appInfoArray.getJSONObject(i);
              //  HandleAppThread tr = new HandleAppThread(appContext,mainUiHandler);
             //   es.execute(tr);
            }
            es.shutdown();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
