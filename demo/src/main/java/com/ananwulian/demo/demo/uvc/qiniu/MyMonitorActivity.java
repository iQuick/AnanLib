package com.ananwulian.demo.demo.uvc.qiniu;

import android.view.SurfaceView;
import android.view.View;

import com.ananwulian.demo.R;
import com.ananwulian.demo.api.Api;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import org.json.JSONException;
import org.json.JSONObject;

import com.ananwulian.qnmonitor.ui.MonitorActivity;

public class MyMonitorActivity extends MonitorActivity {

    @Override
    protected void init() {
        super.init();
        findViewById(R.id.change_publish_url).setOnClickListener(change());
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_monitor;
    }

    @Override
    protected SurfaceView getMonitorGLSurfaceView() {
        return findViewById(R.id.glsv_monitor);
    }


    private View.OnClickListener change() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Api.getPublishUrl(MyMonitorActivity.this, new StringCallback() {

                    @Override
                    public void onSuccess(Response<String> response) {

                        try {
                            String body = response.body();
                            JSONObject jsonObject = new JSONObject(body);
                            String publishUrl = jsonObject.getString("rtmpPublishUrl");

                            changePublishUrl(publishUrl);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
    }

}
