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

import com.ananwulian.mqpush.service.MqttService;
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

    // 是否初始化过
    private boolean isInit = false;
    // context
    private Context mContext;
    // clientid
    private String mClientId;
    // topic
    private String mTopic;
    // ServiceConnection
    private ServiceConnection mServiceConnection;
    // Binder
    private MqttService.MqttBinder mMqttBinder;

    /**
     * 初始化
     * @param context
     */
    public void init(Context context, String clientId, String topic) {
        this.mContext = context;
        this.mClientId = clientId;
        this.mTopic = topic;
        this.initLocation(mContext);
        this.registerElectricQuantity(context);
        this.isInit = true;
    }


    /**
     * 初始化定位
     */
    private void initLocation(Context context) {
        MapLocationUtil.getMapLocationUtil().init(context, new MapLocationUtil.MyLocationCall() {
            @Override
            public void setMapLocation(double latitude, double longitude, float speed, float accuracy) {
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
     * 是否初始化过
     * @return
     */
    public boolean isInit() {
        return isInit;
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
     * 更新 Imei
     * @param imei
     */
    public void updateImei(String imei) {
        if (mMqttBinder != null) {
            mMqttBinder.updateImei(imei);
        }
    }

    /**
     * 更新订阅
     * @param topics
     */
    public void updateSubscribe(String[] topics) {
        if (mMqttBinder != null) {
            mMqttBinder.updateSubscribe(topics);
        }
    }

    /**
     * Service 连接监听
     */
    private class MqttServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMqttBinder = (MqttService.MqttBinder) service;
            mMqttBinder.startService(MqttSetting.serverURIs[0], mClientId,  MqttSetting.username, MqttSetting.password, mTopic);
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
