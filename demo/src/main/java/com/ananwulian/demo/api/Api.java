package com.ananwulian.demo.api;

import android.content.Context;
import android.content.Intent;

import com.ananwulian.demo.Const;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.HttpParams;
import com.ananwulian.qnmonitor.utils.Md5Util;
import com.ananwulian.qnmonitor.utils.Utils;

public class Api {

    public static void getPublishUrl(Context context, StringCallback callback) {

        long t = System.currentTimeMillis();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put("reqKey", md5(t));
        httpHeaders.put("reqDate", String.valueOf(t));
        HttpParams httpParams = new HttpParams();
        httpParams.put("imei", Utils.getDeviceId(context));
        OkGo.<String>post("https://api.ananwulian.com/monitor/getPublishUrl")
                .params(httpParams)
                .tag(context.getClass().getName())
                .headers(httpHeaders)
                .execute(callback);

    }



    /**
     * md5
     *
     * @param reqDate 时间
     * @return 字符串
     */
    private static String md5(long reqDate) {
        return Md5Util.md5(Const.APP_ID + Const.APP_YAN + Const.APP_KEY + Const.APP_YAN + reqDate);
    }

}
