package com.fear1ess.reyunaditool;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

public class AdiToolApp extends Application {
    private static Context mAppContext = null;

    private Handler uiHandler = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        synchronized (this){
            if(mAppContext == null) mAppContext = base;
        }
    }

    public static Context getAppContext(){
        return mAppContext;
    }

    public Handler getUiHandler() {
        return uiHandler;
    }

    public void setUiHandler(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }
}
