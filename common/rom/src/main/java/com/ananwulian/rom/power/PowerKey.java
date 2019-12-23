package com.ananwulian.rom.power;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class PowerKey {

    // 包名
    private static final String PACKNAME = "com.android.settings";
    // 广播名
    private static final String BROADCAST_ACTION = "com.xinyi.action.PowerKey";
    // 目标
    private static final String TARGET_CLASS = "com.android.settings.PowerKeyReceiver";

    // key
    private static final String ENABLE_POWER_KEY = "ENABLE_POWER_KEY";


    private volatile static PowerKey mInstance;
    private PowerKey() {}
    public static PowerKey getInstance() {
        if (mInstance == null) {
            synchronized (PowerKey.class) {
                if (mInstance == null) {
                    mInstance = new PowerKey();
                }
            }
        }
        return mInstance;
    }

    /**
     * 是否开启 power key
     */
    private boolean isEnablePowerKey = true;


    /**
     * 启用 power key 功能
     * @param context
     */
    public void enablePowerKey(Context context) {
        midPowerKey(context, true);
    }

    /**
     * 禁用 power key 功能
     * @param context
     */
    public void disenablePowerKey(Context context) {
        midPowerKey(context, false);
    }


    /**
     * 修改 power key 状态
     * @param context
     */
    public void midPowerKey(Context context) {
        midPowerKey(context, !isEnablePowerKey);
    }

    /**
     * 修改 power key 状态
     * @param context
     * @param isEnable
     */
    private void midPowerKey(Context context, boolean isEnable) {
        this.isEnablePowerKey = isEnable;
        Intent intent = new Intent(BROADCAST_ACTION);
        ComponentName componentName = new ComponentName(PACKNAME, TARGET_CLASS);
        intent.setComponent(componentName);
        intent.putExtra(ENABLE_POWER_KEY, isEnable?0:1);
        context.sendBroadcast(intent);
    }

}
