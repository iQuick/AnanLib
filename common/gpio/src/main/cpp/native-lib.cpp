#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <termios.h>
#include <android/log.h>
#include <sys/ioctl.h>

#define SIXLEDS 0x6C
#define SIXLEDS_IOCTL_SET_LED1		_IOW(SIXLEDS, 0x1, int)
#define SIXLEDS_IOCTL_SET_LED2		_IOW(SIXLEDS, 0x2, int)
#define SIXLEDS_IOCTL_SET_LED3		_IOW(SIXLEDS, 0x3, int)
#define SIXLEDS_IOCTL_SET_LED4		_IOW(SIXLEDS, 0x4, int)
#define SIXLEDS_IOCTL_SET_LED5		_IOW(SIXLEDS, 0x5, int)
#define SIXLEDS_IOCTL_SET_LED6		_IOW(SIXLEDS, 0x6, int)

//#define HALL1   'Z'
//#define HALL1_IOCTL_GET_STATUS   _IOR(HALL1,0x1,int)

static int fd  = -1;
extern "C" JNIEXPORT jint JNICALL
Java_com_blackuio_center_power_util_LightControlUtil_open(
        JNIEnv *env,
        jobject /* this */) {
    fd = open("/dev/fourleds", O_RDWR|O_NDELAY );
    //fd = open("/dev/HALL_1", O_RDWR|O_NDELAY );
    return fd;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_blackuio_center_power_util_LightControlUtil_close(
        JNIEnv *env,
        jobject /* this */) {
    close(fd);
    fd = -1;
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_blackuio_center_power_util_LightControlUtil_ioctl(
        JNIEnv *env,
        jobject /* this */,
        jint cmd,
        jint flag) {
    jint dev_cmd = 0;
    switch (cmd){
        case 1:
            dev_cmd = SIXLEDS_IOCTL_SET_LED1;//led1 红外
            break;
        case 2:
            dev_cmd = SIXLEDS_IOCTL_SET_LED2;//LED2 白光
            break;
        case 3:
            dev_cmd = SIXLEDS_IOCTL_SET_LED3;//LED3  激光灯
            break;
        case 4:
            dev_cmd = SIXLEDS_IOCTL_SET_LED4;
            break;
        case 5:
            dev_cmd = SIXLEDS_IOCTL_SET_LED5;
            break;
        case 6:
            dev_cmd = SIXLEDS_IOCTL_SET_LED6;
            break;
        default:
            break;
    }
    __android_log_print(ANDROID_LOG_INFO, "DEV_SIXLEDS_CTL", " dev_cmd = 0x%x",dev_cmd);

    ioctl(fd, dev_cmd, &flag);
    __android_log_print(ANDROID_LOG_INFO, "DEV_SIXLEDS_CTL", "IOCTL + %d",flag);

    return flag;
}