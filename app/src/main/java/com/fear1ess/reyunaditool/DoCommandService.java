package com.fear1ess.reyunaditool;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.fear1ess.reyunaditool.cmd.OperateCmd;
import com.fear1ess.reyunaditool.cmd.WSConnectCmd;
import com.fear1ess.reyunaditool.cmd.WSConnectCmd.ClientCmd;
import com.fear1ess.reyunaditool.server.NanoWSD;
import com.fear1ess.reyunaditool.thread.DownloadThread;
import com.fear1ess.reyunaditool.thread.InstallAndStartAppThread;
import com.fear1ess.reyunaditool.thread.RemoveAppProceduce;
import com.fear1ess.reyunaditool.thread.UploadAdsDataProceduce;
import com.fear1ess.reyunaditool.utils.PushMsgUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DoCommandService extends Service  {
    public static String TAG = "reyunaditool_log";

    private String mCurPkgName;
    private String mCurDownloadPath;

    private String mCurAdsStateStr;

    private WSCommandServer.WSCommandWebSocket mWebSocket;
    private volatile boolean isWSConnected = false;

    private Handler mUiHandler;

    private InstallAndStartAppThread mInstallAndStartAppThread;
    private DownloadThread mDownloadAppThread;

    private Binder mBinder = new DoCommandBinder();

    public NanoWSD.WebSocket getWebSocket(){
        return mWebSocket;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind...");
        return mBinder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        String CHANNEL_ONE_ID = "10086";
        String CHANNEL_ONE_NAME= "ReyunDoCommandService";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel= new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(notificationChannel);
            }
        }
        Notification notification= new Notification.Builder(this, CHANNEL_ONE_ID)
                .setChannelId(CHANNEL_ONE_ID)
                .setTicker("Nature")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("启动服务")
                .setContentText("ReyunAdiDommandService")
                .build();
        startForeground(1,notification);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand...");

        //start websocket service
        WSCommandServer chs = new WSCommandServer(2020);
        try {
            Log.d(TAG, "start websocket server on port 2020...");
            chs.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        startOperateAppThread();

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ....");
        mInstallAndStartAppThread.exit();
        mDownloadAppThread.exit();
        stopForeground(true);
    }

    public void startOperateAppThread(){
        Log.d(TAG, "startOperateAppThread...");
        AdiToolApp app = (AdiToolApp) getApplication();
        mUiHandler = app.getUiHandler();
        Context cxt = AdiToolApp.getAppContext();
        ExecutorService es = Executors.newCachedThreadPool();
        DownloadThread dlt = new DownloadThread(cxt, mUiHandler, this);
        InstallAndStartAppThread hat = new InstallAndStartAppThread(dlt, cxt, mUiHandler,this);
        es.execute(dlt);
        es.execute(hat);
        mInstallAndStartAppThread = hat;
        mDownloadAppThread = dlt;
        es.shutdown();
    }


    public void sendMsgToClient(String str){
        if(!isWSConnected) return;
        try {
            mWebSocket.send(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCurApp(String pkgName,String downloadPath){
        mCurPkgName = pkgName;
        mCurDownloadPath = downloadPath;
    }

    public class DoCommandBinder extends IDoCommandService.Stub{

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public String doCommand(int cmd, String str) throws RemoteException {
            switch (cmd) {
                case OperateCmd.QUERY_CURRENT_PKGNAME: {
                    Log.d(TAG, "query current pkgname: " + mCurPkgName);
                    return mCurPkgName;
                }
                case OperateCmd.UPLOAD_ADSDK_INFO: {
                    ExecutorService es = Executors.newSingleThreadExecutor();
                    Log.d(TAG, "start upload ads data: " + str);
                    es.execute(new UploadAdsDataProceduce(str));
                    es.shutdown();
                    return "success";
                }
                case OperateCmd.SHUTDOWN_APP: {
                    ExecutorService es = Executors.newSingleThreadExecutor();
                    es.execute(new RemoveAppProceduce(mInstallAndStartAppThread, mCurPkgName, mCurDownloadPath, DoCommandService.this));
                    es.shutdown();
                    return "success";
                }
                case OperateCmd.UPLOAD_ADSDK_EXISTS_STATE: {
                    try {
                        if(mWebSocket != null){
                            mCurAdsStateStr = str;
                            JSONObject sendjo = new JSONObject(str);
                            sendjo.put("cmd", WSConnectCmd.ServerCmd.NEW_ADS_PUSH_MSG);
                            sendMsgToClient(sendjo.toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return "success";
                }

                default: return null;
            }
        }
    }


    public class WSCommandServer extends NanoWSD {
        public WSCommandServer(int port) {
            super(port);
        }

        @Override
        protected WebSocket openWebSocket(IHTTPSession handshake) {
            return new WSCommandWebSocket(handshake);
        }

        public class WSCommandWebSocket extends WebSocket{

            public WSCommandWebSocket(IHTTPSession handshakeRequest) {
                super(handshakeRequest);
            }

            @Override
            protected void onOpen() {
                if(isWSConnected){
                    try {
                        String str = "server has been connected by other device...";
                        send(new WebSocketFrame.CloseFrame(WebSocketFrame.CloseCode.GoingAway, str).getBinaryPayload());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "a client success to connect server...");
                mWebSocket = this;
            }

            @Override
            protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
                Log.d(TAG, "onClose: ");
                isWSConnected = false;
                mWebSocket = null;
            }

            @Override
            protected void onMessage(WebSocketFrame message) {
                Log.d(TAG, "onMessage: ");
                String data = message.getTextPayload();
                try {
                    JSONObject jo = new JSONObject(data);
                    if(!jo.has("cmd")) send("error data format!");
                    int cmd = jo.getInt("cmd");
                    switch (cmd){
                        case ClientCmd.START_OPERATE_APP:
                            startOperateAppThread();
                            break;
                        case ClientCmd.STOP_SERVICE:
                            stopSelf();
                            break;
                        case ClientCmd.REQ_APP_ADS_STATE:
                            JSONObject sendjo = new JSONObject(mCurAdsStateStr);
                            sendjo.put("cmd", WSConnectCmd.ServerCmd.NEW_ADS_PUSH_MSG);
                            sendMsgToClient(sendjo.toString());
                            break;
                        default: break;
                    }
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void onPong(WebSocketFrame pong) {
                Log.d(TAG, "onPong: ");
            }

            @Override
            protected void onException(IOException exception) {
                Log.d(TAG, "onException: ");
            }
        }

        /*
        @Override
        public Response serve(IHTTPSession sessionBase) {
            if ((sessionBase instanceof HTTPSession) == false)
                return newFixedLengthResponse("method error!");
            HTTPSession session = ((HTTPSession) sessionBase);
            Map<String, List<String>> queryMap = session.getParameters();
            String cmdStr = queryMap.get("cmd").get(0);
            int cmd = new Integer(cmdStr);
            switch (cmd) {
                case OperateCmd.QUERY_CURRENT_PKGNAME:
                    Log.d(TAG, "query current pkgname...");
                    return newFixedLengthResponse(mCurPkgName);
                default: {
                    Message msg = Message.obtain();
                    msg.what = cmd;
                    Bundle bundle = new Bundle();
                    byte[] data = null;
                    if (Method.POST.equals(session.getMethod())) {
                        InputStream in = session.getInputStream();
                        Log.d(TAG, "body size: " + session.getBodySize());
                        data = new byte[(int) session.getBodySize()];
                        try {
                            in.read(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    bundle.putByteArray("data", data);
                    msg.setData(bundle);
                    mHandleAppHandler.sendMessage(msg);
                    return newFixedLengthResponse("success");
                }
            }
        }*/
    }
}
