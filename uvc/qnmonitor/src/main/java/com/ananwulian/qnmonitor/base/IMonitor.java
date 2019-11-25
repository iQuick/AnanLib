package com.ananwulian.qnmonitor.base;

public interface IMonitor {

    /**
     * 更换推流地址
     * @param publishUrl
     */
    public void changePublishUrl(String publishUrl);

    /**
     * 设置推流地址
     * @param publishUrl
     */
    public void setPublishUrl(String publishUrl);

//    /**
//     * 开启监控
//     */
//    public void startMonitor();

    /**
     * 恢复监控
     */
    public void resumeMonitor();

    /**
     * 暂停监控
     */
    public void pauseMonitor();

//    /**
//     * 停止监控
//     */
//    public void stopMonitor();

    /**
     * 销毁
     */
    public void destroy();


}
