#include <android/log.h>
#include <jni.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>

#define FLASH_path  "/dev/flashlight"
#define IR_path  "/dev/virtual"

#define GPIO_IOC_MAGIC 0x92
#define OPEN_IR        _IOW(GPIO_IOC_MAGIC, 0,int)
#define CLOSE_IR        _IOW(GPIO_IOC_MAGIC, 1, int)

#define FLASHLIGHT_MAGIC 'S'
#define FLASH_IOC_SET_ONOFF                _IOR(FLASHLIGHT_MAGIC, 115, int)
#define FLASH_CLOSE           _IOWR(FLASHLIGHT_MAGIC, 246, int)
#define FLASH_OPEN           _IOWR(FLASHLIGHT_MAGIC, 247, int)

#define GPIO_NAME_PATH "/sys/bus/platform/drivers/mediatek-pinctrl/10005000.pinctrl/mt_gpio"
int gpio_fb_63 = -1;


//led灯开关控制
JNIEXPORT jboolean JNICALL Java_com_ananwulian_gpio_GPIO_1V2_light
        (JNIEnv *env, jobject jobj, jint n) {
    __android_log_print(ANDROID_LOG_DEBUG, "gpio", "flashlightControl n %d", n);

    int flashlightFd = -1;

    flashlightFd = open(FLASH_path, O_RDWR);
    if (flashlightFd < 0) {
        __android_log_print(ANDROID_LOG_DEBUG, "gpio", "open %s failed", FLASH_path);
        return 0;
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, "gpio", "open %s success", FLASH_path);
    }
    if (n) {
        ioctl(flashlightFd, FLASH_OPEN, (unsigned long) 0);

    } else {
        ioctl(flashlightFd, FLASH_CLOSE, (unsigned long) 0);
    }
    close(flashlightFd);
    __android_log_print(ANDROID_LOG_DEBUG, "gpio", "close %s success", FLASH_path);
    return 1;
}

//镭射灯开关控制
JNIEXPORT jboolean JNICALL Java_com_ananwulian_gpio_GPIO_1V2_ir
        (JNIEnv *env, jobject jobj, jint n) {

    __android_log_print(ANDROID_LOG_DEBUG, "gpio", "IRControl n %d", n);

    int fd = -1;

    fd = open(IR_path, O_RDWR);
    if (fd < 0) {
        __android_log_print(ANDROID_LOG_DEBUG, "gpio", "open %s failed", IR_path);
        return 0;
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, "gpio", "open %s success", IR_path);
    }

    if (n) {
        ioctl(fd, OPEN_IR, 0);

    } else {
        ioctl(fd, CLOSE_IR, 0);
    }

    close(fd);

    return 1;
}


/*
	setting GPIO_V2 mode
	gpio : gpio num
	modem: gpio mode
*/

void set_GPIO_mode(int fd,int gpio,int mode){
    int ret;
    char buf[16]={0};
    sprintf(buf,"mode %d %d",gpio,mode);
    ret = write(fd,buf,16);
    __android_log_print(ANDROID_LOG_DEBUG, "gpio", "set_GPIO_mode gpio %d ret %d",gpio,ret);
}

/*
	setting GPIO_V2 dir
	gpio : gpio num
	dir : gpio dir 0:in or 1:out
*/

void set_GPIO_dir(int fd,int gpio,int dir){
    int ret;
    char buf[16]={0};
    sprintf(buf,"dir %d %d",gpio,dir);
    ret = write(fd,buf,16);
    __android_log_print(ANDROID_LOG_DEBUG, "gpio", "set_GPIO_dir gpio %d ret %d",gpio,ret);
}


/*
	setting GPIO_V2 output
	gpio : gpio num
	status : 1:High or 0:Low
*/
void set_GPIO_out(int fd,int gpio,int status){
    int ret;
    char buf[16]={0};
    sprintf(buf,"out %d %d",gpio,status);
    ret = write(fd,buf,16);
    __android_log_print(ANDROID_LOG_DEBUG, "gpio", "set_GPIO_out gpio %d ret %d",gpio,ret);
}

JNIEXPORT jboolean JNICALL Java_com_ananwulian_gpio_GPIO_1V2_xy6763GpioInit
        (JNIEnv *env, jobject jobj) {
    __android_log_print(ANDROID_LOG_DEBUG, "gpio", "xy6763_gpio_init");

    gpio_fb_63 = open(GPIO_NAME_PATH, O_RDWR | O_NOCTTY | O_NONBLOCK);

    __android_log_print(ANDROID_LOG_DEBUG, "gpio", "gpio_fb_63 %d",gpio_fb_63);

    return (jboolean) gpio_fb_63;
}

JNIEXPORT jboolean JNICALL Java_com_ananwulian_gpio_GPIO_1V2_xy6763setGpioDataHigh
        (JNIEnv *env, jobject jobj, jint n)
{

    __android_log_print(ANDROID_LOG_DEBUG, "gpio", "xy6763_setgpio_high");

    set_GPIO_mode(gpio_fb_63, n, 0);
    set_GPIO_dir(gpio_fb_63, n, 1);
    set_GPIO_out(gpio_fb_63,n,1);

    return 1;

}
JNIEXPORT jboolean JNICALL Java_com_ananwulian_gpio_GPIO_1V2_xy6763setGpioDataLow
        (JNIEnv *env, jobject jobj, jint n){

    __android_log_print(ANDROID_LOG_DEBUG, "gpio", "xy6763_setgpio_low");

    set_GPIO_mode(gpio_fb_63, n, 0);
    set_GPIO_dir(gpio_fb_63, n, 1);
    set_GPIO_out(gpio_fb_63,n,0);

    return 1;
}