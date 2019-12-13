package com.ananwulian.demo.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.ananwulian.demo.R;
import com.ananwulian.demo.demo.mqtt.MqttActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findViewById(R.id.btn_mqtt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goAc(MqttActivity.class);
            }
        });
    }


    /**
     * 启动
     * @param clz
     */
    private void goAc(Class<?> clz) {
        Intent intent = new Intent(this, clz);
        startActivity(intent);
    }
}
