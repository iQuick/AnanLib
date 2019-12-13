package com.ananwulian.mqpush.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public abstract class MqttBroadcastReceiver extends BroadcastReceiver {

    /**
     * 默认消息
     */
    public static final String ACTION_DEF = "mqtt_action_def";

    /**
     * 注册广播
     * @param context
     */
    public static void register(Context context, MqttBroadcastReceiver receiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DEF);
        context.registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String data = intent.getStringExtra("data");
        onReceive(context, data);
    }

    /**
     *
     * @param context
     * @param data
     */
    public abstract void onReceive(Context context, String data);

}
