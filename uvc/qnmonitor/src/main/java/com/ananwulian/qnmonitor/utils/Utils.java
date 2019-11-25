package com.ananwulian.qnmonitor.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;

public class Utils {

    /**
     * 获取设备 id
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    public static String getDeviceId(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

}
