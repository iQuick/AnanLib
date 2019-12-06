package com.ananwulian.gpio;


/**
 * 头盔二代（USBCamera 版本）
 */
public class GPIO_V2 {

    static {
        System.loadLibrary("newmobi_gpio");
    }

    private volatile static GPIO_V2 mInstance;
    public static GPIO_V2 getInstance() {
        if (mInstance == null) {
            synchronized (GPIO_V2.class) {
                if (mInstance == null) {
                    mInstance = new GPIO_V2();
                }
            }
        }
        return mInstance;
    }

    // 打开
    public static final int OPEN_LIGHT = 1;
    // 关闭
    public static final int CLOSE_LIGHT = 0;


    // 灯光状态
    private boolean lightFlag = false;
    // 镭射状态
    private boolean irFlag = false;


    /**
     * 镭射自动开启/关闭
     */
    public void ir() {
        irFlag = !irFlag;
        ir(irFlag?OPEN_LIGHT:CLOSE_LIGHT);
    }

    /**
     * 开启镭射
     */
    public void irOpen() {
        irFlag = true;
        ir(OPEN_LIGHT);
    }

    /**
     * 关闭镭射
     */
    public void irClose() {
        irFlag = false;
        ir(CLOSE_LIGHT);
    }

    /**
     * 灯光自动开启/关闭
     */
    public void light() {
        lightFlag = !lightFlag;
        light(lightFlag?OPEN_LIGHT:CLOSE_LIGHT);
    }

    /**
     * 开启灯光
     */
    public void lightOpen() {
        lightFlag = true;
        light(OPEN_LIGHT);
    }

    /**
     * 关闭灯光
     */
    public void lightClose() {
        lightFlag = false;
        light(CLOSE_LIGHT);
    }




    /**
     * 灯光控制
     * @param n
     * @return
     */
    private native boolean light(int n);

    /**
     * 镭射灯控制
     * @param n
     * @return
     */
    private native boolean ir(int n);

    /**
     * 初始化
     * @return
     */
    public native boolean xy6763GpioInit();


    /**
     * 引脚高电平
     * @param gpioIndex
     * @return
     */
    public native boolean xy6763setGpioDataHigh(int gpioIndex);

    /**
     * 引脚低电平
     * @param gpioIndex
     * @return
     */
    public native boolean xy6763setGpioDataLow(int gpioIndex);

}
