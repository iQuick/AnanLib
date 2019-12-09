package com.ananwulian.mqpush.utils;

import android.content.Context;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import static com.amap.api.location.AMapLocationClientOption.AMapLocationPurpose.Sport;


public class MapLocationUtil {

    private static final String TAG = "MapLocationUtil";

    //声明AMapLocationClient类对象
    private AMapLocationClient mLocationClient = null;

    //声明AMapLocationClientOption对象
    private AMapLocationClientOption mLocationOption = null;

    private static final MapLocationUtil mapLocationUtil = new MapLocationUtil();

    private MapLocationUtil() {
    }

    public static MapLocationUtil getMapLocationUtil() {
        return mapLocationUtil;
    }

    /**
     * 初始化定位
     *
     * @param context h
     */
    public MapLocationUtil init(Context context, MyLocationCall myLocationCall) {
        AMapLocationListener mLocationListener = new MapLocationListener(context, myLocationCall);
        //初始化定位
        mLocationClient = new AMapLocationClient(context);
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
        return this;
    }


    public MapLocationUtil config() {
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        /*//设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Sport);
        //获取一次定位结果：
        //该方法默认为false。
        mLocationOption.setOnceLocation(false);
        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true);
        mLocationOption.setInterval(3000);
        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(30000);
        //开启缓存机制
        mLocationOption.setLocationCacheEnable(false);*/
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mLocationOption.setGpsFirst(true);                                                                                                                   //可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mLocationOption.setHttpTimeOut(30000);                                                                                                                  //可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mLocationOption.setInterval(2000);                                                                                                                  //可选，设置定位间隔。默认为2秒
        mLocationOption.setNeedAddress(false);                                                                                                                  //可选，设置是否返回逆地理地址信息。默认是true
        mLocationOption.setOnceLocation(false);                                                                                                                  //可选，设置是否单次定位。默认是false
        mLocationOption.setLocationPurpose(Sport);
        mLocationOption.setOnceLocationLatest(true);
        mLocationOption.setSensorEnable(true);                                                                                                                  //可选，设置是否使用传感器。默认是false
        mLocationOption.setWifiScan(true);                                                                                                                   //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mLocationOption.setLocationCacheEnable(true);                                                                                                                   //可选，设置是否使用缓存定位，默认为true
        mLocationOption.setGeoLanguage(AMapLocationClientOption.GeoLanguage.DEFAULT);
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        if (null != mLocationClient) {
            mLocationClient.setLocationOption(mLocationOption);
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }
        return this;
    }

    public void start() {
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 通过经纬度获取距离(单位：米)
     *
     * @param lat1 .
     * @param lng1 .
     * @param lat2 .
     * @param lng2 .
     * @return 距离
     */
    public static double getDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        double earthRadius = 6378.137;
        s = s * earthRadius;
        s = Math.round(s * 10000d) / 10000d * 1000;
        return s;//米
    }


    class MapLocationListener implements AMapLocationListener {

        private Context context;
        private MyLocationCall myLocationCall;

        MapLocationListener(Context context, MyLocationCall myLocationCall) {
            this.context = context;
            this.myLocationCall = myLocationCall;
        }

        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if (aMapLocation.getSpeed() != 0)
                myLocationCall.setMapLocation(aMapLocation.getLatitude(), aMapLocation.getLongitude(), aMapLocation.getSpeed(), aMapLocation.getAccuracy());
        }

    }

    public interface MyLocationCall {
        void setMapLocation(double latitude, double longitude, float speed, float accuracy);
    }

}
