package com.ananwulian.demo.demo.mqtt;

import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.ananwulian.demo.R;
import com.ananwulian.mqpush.MqttMgr;

public class MqttActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt);

        MqttMgr.getInstance().init(this, "123456");
        MqttMgr.getInstance().start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MqttMgr.getInstance().startHeartBeat();
                MqttMgr.getInstance().getMqttBinder().updateImei("100000002000");
            }
        }, 5 * 1000);

    }
}
