package com.ananwulian.mqpush.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.ananwulian.mqpush.MqttSetting;
import com.ananwulian.mqpush.been.HeartBeat;
import com.ananwulian.mqpush.receiver.MqttBroadcastReceiver;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

public class MqttService extends Service {

    /**
     * client
     */
    private MqttClient mMqttClient;
    private MqttBinder mMqttBinder;

    // params
    private String SERVER_URL;
    private String USERNAME;
    private String PASSWORD;
    private String TOPIC;
    private String CLIENT_ID;

    // gson
    private Gson mGson;
    private HeartBeat mHeartBeat;

    // Head
    private HeartBeatRunnable mHeartBeatRunnable;
    private Thread mHeartBeatThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMqttBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectMqttService();
        if (mHeartBeatRunnable != null) {
            mHeartBeatRunnable.stop();
            mHeartBeatRunnable = null;
            mHeartBeatThread = null;
        }
    }

    /**
     * 开启心跳
     */
    private void startHeartBeat() {
        mHeartBeatRunnable = new HeartBeatRunnable();
        mHeartBeatThread = new Thread(mHeartBeatRunnable);
        mHeartBeatThread.start();
    }

    /**
     * 初始化
     */
    private void init(String url, String clientId, String username, String password, String topic) {
        this.SERVER_URL = url;
        this.CLIENT_ID = clientId;
        this.USERNAME = username;
        this.PASSWORD = password;
        this.TOPIC = topic;
        this.mMqttBinder = new MqttBinder();
        this.mGson = new Gson();
        this.mHeartBeat = new HeartBeat();
        this.mHeartBeat.imei = clientId;
        /// init connect
        this.initMqttClient(url, clientId);
        this.connectMqttService(username, password, topic);
    }


    /**
     * 初始化 client
     * @param serverUrl
     * @param clientId
     */
    private void initMqttClient(String serverUrl, String clientId) {
        try {
            final String tmpDir = System.getProperty("java.io.tmpdir");
            MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
            mMqttClient = new MqttClient(serverUrl, clientId, dataStore);
            mMqttClient.setCallback(new MyMqttCallback());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接服务
     * @param username
     * @param password
     * @param topic
     */
    private void connectMqttService(String username, String password, String topic) {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        // 重置服务端
        connectOptions.setServerURIs(MqttSetting.serverURIs);
        // 设置Mqtt版本
        connectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        // 设置清空Session，false表示服务器会保留客户端的连接记录，true表示每次以新的身份连接到服务器
        connectOptions.setCleanSession(false);
        // 设置会话心跳时间，单位为秒
        // 客户端每隔10秒向服务端发送心跳包判断客户端是否在线
        connectOptions.setKeepAliveInterval(10);
        // 设置账号
        connectOptions.setUserName(username);
        // 设置密码
        connectOptions.setPassword(password.toCharArray());
        // 最后的遗言(连接断开时， 发动"close"给订阅了topic该主题的用户告知连接已中断)
        connectOptions.setWill(topic, "close".getBytes(), 2, true);

        try {
            mMqttClient.connect();
            mMqttClient.subscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    /**
     * 取消连接
     */
    public void disconnectMqttService() {
        try {
            if (mMqttClient != null && mMqttClient.isConnected()) {
                mMqttClient.disconnect();
            }

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发布消息
     *
     * @param topicName 主题名称
     * @param qos       质量(0,1,2)
     * @param payload   发送的内容
     */
    private void publish(String topicName, int qos, String payload) {
        this.publish(topicName, qos, payload.getBytes());
    }

    /**
     * 发布消息
     *
     * @param topicName
     * @param qos
     * @param bytes
     */
    private void publish(String topicName, int qos, byte[] bytes) {
        if (mMqttClient != null && mMqttClient.isConnected()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(bytes);
            mqttMessage.setQos(qos);

            try {
                mMqttClient.publish(topicName, mqttMessage);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Mqtt 监听
     */
    public class MyMqttCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            // TODO 连接断开，可以在此次执行重连机制
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            // TODO 接收到信息，消息处理
            if (TOPIC.equals(topic)) {
                String data = new String(message.getPayload());
                Intent intent = new Intent();
                intent.setAction(MqttBroadcastReceiver.ACTION_DEF);
                intent.putExtra("data", data);
                sendBroadcast(intent);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }


    /**
     * MqttBinder
     */
    public class MqttBinder extends Binder {

        /**
         * 启动服务
         * @param url
         * @param clientId
         * @param username
         * @param password
         * @param topic
         */
        public void startService(String url, String clientId, String username, String password, String topic) {
            MqttService.this.init(url, clientId, username, password, topic);
        }

        /**
         * 开启心跳
         */
        public void startHeartBeat() {
            MqttService.this.startHeartBeat();
        }

        /**
         * 更新线路信息
         * @param line
         */
        public void updateLine(int line) {
            mHeartBeat.line = line;
        }

        /**
         * 更新定位信息
         * @param latitude
         * @param longitude
         * @param speed
         */
        public void updateLocation(double latitude, double longitude, float speed) {
            mHeartBeat.latitude = latitude;
            mHeartBeat.longitude = longitude;
            mHeartBeat.speed = speed;
        }

        /**
         * 更新电量信息
         * @param electricQuantity
         */
        public void updateElectricQuantity(int electricQuantity) {
            mHeartBeat.electricQuantity = electricQuantity;
        }

    }

    /**
     * 心跳
     */
    private class HeartBeatRunnable implements Runnable {

        /**
         * 是否停止
         */
        private boolean isStop = false;

        /**
         * 间隔时间 5s
         */
        private final int INTERVAL_TIME = 5;

        @Override
        public void run() {
            while (isStop) {
                try {
                    sleep(INTERVAL_TIME);
                    sendHeartBeatData();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 发送心跳数据包
         */
        private void sendHeartBeatData() {
            if (mHeartBeat != null) {
                String data = mGson.toJson(mHeartBeat);
                publish(TOPIC, 0, data);
            }
        }


        /**
         * 停止
         */
        public void stop() {
            isStop = true;
        }


        private void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}
