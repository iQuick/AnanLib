package com.ananwulian.mqpush;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.ananwulian.mqpush.utils.MapLocationUtil;

public class MqttMgr {

    private static MqttMgr mInstance = null;
    private MqttMgr() {
        mServiceConnection = new MqttServiceConnection();
    }
    public static MqttMgr getInstance() {
        if (null == mInstance) {
            synchronized (MqttMgr.class) {
                if (mInstance == null) {
                    mInstance = new MqttMgr();
                }
            }
        }
        return mInstance;
    }

    // context
    private Context mContext;
    // clientid
    private String mClientId;
    // ServiceConnection
    private ServiceConnection mServiceConnection;
    // Binder
    private MqttService.MqttBinder mMqttBinder;

    /**
     * 初始化
     * @param context
     */
    public void init(Context context, String clientId) {
        this.mContext = context;
        this.mClientId = clientId;
        this.initLocation(mContext);
        this.registerElectricQuantity(context);
    }


    /**
     * 初始化定位
     */
    private void initLocation(Context context) {
        MapLocationUtil.getMapLocationUtil().init(context, new MapLocationUtil.MyLocationCall() {
            @Override
            public void setMapLocation(double latitude, double longitude, float speed, float accuracy) {
                Log.d("======", String.format("== accuracy:%s", accuracy));
                Log.d("======", String.format("== latitude:%s, longitude:%s, speed:%s", latitude, longitude, speed));
                if (mMqttBinder != null) {
                    mMqttBinder.updateLocation(latitude, longitude, speed);
                }
            }
        }).config().start();
    }

    /**
     * 注册电量广播
     * @param context
     */
    private void registerElectricQuantity(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        ElectricBroadCast electricBroadCast = new ElectricBroadCast();
        context.registerReceiver(electricBroadCast, intentFilter);
    }

    /**
     * 启动服务
     */
    public void start() {
        Intent intent = new Intent(mContext, MqttService.class);
        mContext.bindService(intent, mServiceConnection, Service.BIND_AUTO_CREATE);
    }

    /**
     * 启动心跳
     */
    public void startHeartBeat() {
        if (mMqttBinder != null) {
            mMqttBinder.startHeartBeat();
        }
    }


    /**
     * Service 连接监听
     */
    private class MqttServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMqttBinder = (MqttService.MqttBinder) service;
            mMqttBinder.startService(MqttSetting.serverURIs[0], mClientId,  MqttSetting.username, MqttSetting.password, MqttSetting.serverTopic);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMqttBinder = null;
        }
    }

    /**
     * 电量广播接收
     */
    public class ElectricBroadCast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int level = intent.getIntExtra("level", 0);
                if (mMqttBinder != null) {
                    mMqttBinder.updateElectricQuantity(level);
                }
            }
        }
    }


}
