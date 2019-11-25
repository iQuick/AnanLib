package com.ananwulian.demo;

import android.app.Application;

import com.ananwulian.qnmonitor.Monitor;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        this.initQiniuStream();
    }


    /**
     * 初始化stream七牛云
     */
    private void initQiniuStream() {
        Monitor.initQiniuStream(getApplicationContext());
    }

}
