package com.ananwulian.gpio;

/**
 * 头盔一代
 */
public class GPIO_V1 {

    static {
        System.loadLibrary("native-lib");
    }

    private volatile static GPIO_V1 mInstance;
    public static GPIO_V1 getInstance() {
        if (mInstance == null) {
            synchronized (GPIO_V1.class) {
                if (mInstance == null) {
                    mInstance = new GPIO_V1();
                }
            }
        }
        return mInstance;
    }


    // 灯光状态
    private boolean lightFlag = false;
    // 镭射状态
    private boolean irFlag = false;


    public static final int OPEN_LIGHT = 1;
    public static final int CLOSE_LIGHT = 0;

    public static final int STRONG_LIGHT = 3;
    public static final int WHITE_LIGHT = 2;


    /**
     * 开启灯光
     */
    public void lightOpen() {
        light(OPEN_LIGHT);
    }

    /**
     * 关闭灯光
     */
    public void lightClose() {
        light(CLOSE_LIGHT);
    }

    /**
     * 开关led灯
     *
     * @param flag
     */
    public void light(int flag) {
        lightFlag = flag == OPEN_LIGHT;
        if (lightFlag) {
            ioctl(WHITE_LIGHT, OPEN_LIGHT);
        } else {
            ioctl(WHITE_LIGHT, CLOSE_LIGHT);
        }
    }


    /**
     * 开启镭射
     */
    public void irOpen() {
        ir(OPEN_LIGHT);
    }

    /**
     * 关闭镭射
     */
    public void irClose() {
        ir(CLOSE_LIGHT);
    }


    /**
     * 开关镭射灯
     *
     * @param flag
     */
    public void ir(int flag) {
        irFlag = flag == OPEN_LIGHT;
        if (irFlag) {
            ioctl(STRONG_LIGHT, OPEN_LIGHT);
        } else {
            ioctl(STRONG_LIGHT, CLOSE_LIGHT);
        }
    }


    /**
     * 开启
     * @return
     */
    private native int open();

    /**
     * 关闭
     * @return
     */
    private native int close();

    /**
     * 引脚操作
     * @param cmd
     * @param flag
     * @return
     */
    private native int ioctl(int cmd, int flag);


}
