package com.ananwulian.qnmonitor.listener;

public interface OnMonitorListener {

    /**
     * 准备完成
     */
    public void onReady();

    /**
     * 监控开启
     */
    public void onMonitorStart();

    /**
     * 监控关闭
     */
    public void onMonitorStop();

}
