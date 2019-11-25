package com.ananwulian.qnmonitor;

import android.content.Context;

import com.qiniu.pili.droid.streaming.StreamingEnv;

public class Monitor {


    /**
     * 初始化stream七牛云
     */
    public static void initQiniuStream(Context context) {
        StreamingEnv.init(context);
        StreamingEnv.setLogLevel(5);
    }

}
