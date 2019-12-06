package com.ananwulian.keydown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.Map;

public class KeyPress {

    /**
     * 注册的广播
     */
    private static Map<String, BroadcastReceiver> mBroadcastReceiverList = new HashMap<>();

    /**
     * 按键键值
     */
    private static final String KEY_CODE = "KEY_CODE";

    /**
     * 按键类型
     */
    private static final String EVENT_TYPE = "EVENT_TYPE";

    /**
     * 广播 Action
     */
    private static final String BROADCAST_ACTION = "KEYDOWN_EVENT";


    /**
     * 发送广播
     * @param context
     * @param eventType
     * @param keyCode
     */
    protected static void sendKeyEventBroadcast(Context context, PressType eventType, int keyCode) {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION);
        intent.putExtra(KEY_CODE, keyCode);
        intent.putExtra(EVENT_TYPE, eventType);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * 注册事件
     * @param context
     */
    public static void registerKeyEvent(Context context, final OnKeyPressListener listener) {
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (listener != null) {
                    int keycode = intent.getIntExtra(KEY_CODE, 0);
                    PressType eventType = (PressType) intent.getSerializableExtra(EVENT_TYPE);
                    listener.onKeyPress(keycode, eventType);
                }
            }
        };
        mBroadcastReceiverList.put(context.getClass().getName(), receiver);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intentFilter);
    }

    /**
     * 注册事件
     * @param context
     */
    public static void unregisterKeyEvent(Context context) {
        String name = context.getClass().getName();
        BroadcastReceiver receiver = mBroadcastReceiverList.get(name);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }


    /**
     * 事件监听器
     */
    public interface OnKeyPressListener {

        public void onKeyPress(int keyCode, PressType eventType);

    }

}
