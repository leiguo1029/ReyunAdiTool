package com.fear1ess.reyunaditool;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AdiToolApp app = (AdiToolApp) getApplication();

        Button startBtn = findViewById(R.id.start_btn);
        Button stopBtn = findViewById(R.id.stop_btn);
        stopBtn.setEnabled(false);

        requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE"},100);


        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtn.setText("Working");
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                Context cxt = AdiToolApp.getAppContext();
                startForegroundService(new Intent(MainActivity.this, DoCommandService.class));
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context cxt = AdiToolApp.getAppContext();
                stopService(new Intent(cxt, DoCommandService.class));
                startBtn.setText("Start");
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
            }
        });

        new Thread(){
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startBtn.performClick();
                    }
                });
            }
        }.start();
    }

    public static class MainActivityHandler extends Handler{
        public static final int NETWORK_ERROR = 0;

        private WeakReference<Activity> mActivityWeakRef = null;

        public MainActivityHandler(Activity activity){
            mActivityWeakRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case NETWORK_ERROR:
                    CharSequence errText = msg.getData().getCharSequence("errText");
                    Toast t = Toast.makeText(mActivityWeakRef.get(),errText,Toast.LENGTH_SHORT);
                    t.show();
                    break;
                default:
                    break;
            }
        }
    }
}