package com.ananwulian.demo.demo.uvc.qiniu;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.ananwulian.demo.R;
import com.ananwulian.demo.api.Api;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import org.json.JSONException;
import org.json.JSONObject;

public class QiniuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_click_monitor).setOnClickListener(clickMonitor());

    }

    private View.OnClickListener clickMonitor() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Api.getPublishUrl(QiniuActivity.this, new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {

                        try {
                            String body = response.body();
                            JSONObject jsonObject = new JSONObject(body);
                            String publishUrl = jsonObject.getString("rtmpPublishUrl");


                            Intent intent = new Intent(QiniuActivity.this, MyMonitorActivity.class);
                            intent.putExtra("url", publishUrl);
                            startActivity(intent);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        };
    }




}
