package com.ananwulian.qnmonitor.base;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.SurfaceView;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;

import com.ananwulian.qnmonitor.listener.OnMonitorListener;

import static com.qiniu.pili.droid.streaming.StreamingProfile.BitrateAdjustMode.Auto;
import static com.qiniu.pili.droid.streaming.StreamingProfile.VIDEO_ENCODING_HEIGHT_720;
import static com.qiniu.pili.droid.streaming.StreamingProfile.VIDEO_QUALITY_MEDIUM2;

public abstract class QiniuMonitor implements IMonitor, StreamingStateChangedListener {


    private StreamingProfile mStreamingProfile;


    public static final Object lock = new Object();



    // 是否正在监控
    private boolean isMonitoring = false;
    private boolean isStreaming = false;

    /**
     * Context
     */
    private Context mContext;

    /**
     *
     */
    private SurfaceView mSurfaceView;

    /**
     *
     */
    private String mPublishUrl = null;


    // Listener
    private OnMonitorListener onMonitorListener;

    public QiniuMonitor(Context context, SurfaceView surfaceView) {
        this.mContext = context;
        this.mSurfaceView = surfaceView;
    }


    /**
     * 初始化
     */
    public void init() {
        this.initMonitor();
        this.initImpl();
    }

    /**
     * 初始化监控
     */
    protected void initMonitor() {
        this.initStreamingProfile();
        this.initStreamingManager();
    }

    /**
     * 初始化 StreamingProfile
     */
    protected void initStreamingProfile() {
        mStreamingProfile = new StreamingProfile();
        // 视频质量
        mStreamingProfile.setVideoQuality(VIDEO_QUALITY_MEDIUM2);
        // 音频质量
        mStreamingProfile.setAudioQuality(StreamingProfile.AUDIO_QUALITY_HIGH1);
        // Encoding size 播放端看到的size 720p
        mStreamingProfile.setEncodingSizeLevel(VIDEO_ENCODING_HEIGHT_720);
        mStreamingProfile.setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.LAND);
        // 软编的 EncoderRCModes
        mStreamingProfile.setEncoderRCMode(StreamingProfile.EncoderRCModes.QUALITY_PRIORITY);
        mStreamingProfile.setFpsControllerEnable(true);
        mStreamingProfile.setBitrateAdjustMode(Auto);
        mStreamingProfile.setYuvFilterMode(StreamingProfile.YuvFilterMode.Bilinear);
        // QUIC 是基于 UDP 开发的可靠传输协议，在弱网下拥有更好的推流效果，相比于 TCP 拥有更低的延迟，可抵抗更高的丢包率。
        mStreamingProfile.setQuicEnable(true);

        try {
            if (mPublishUrl != null) {
                mStreamingProfile.setPublishUrl(mPublishUrl);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mStreamingProfile.setDnsManager(getMyDnsManager());
        mStreamingProfile.setStreamStatusConfig(new StreamingProfile.StreamStatusConfig(3));
        mStreamingProfile.setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));
    }

    /**
     *
     */
    protected abstract void initStreamingManager();

    /**
     *
     */
    protected void initImpl() {
        // TODO you init code write here!
    }


    /**
     *
     */
    public abstract void startStreamingInternal();


    /**
     * 是否正在监控
     * @return
     */
    public boolean isMonitoring() {
        return isMonitoring;
    }

    /**
     * 设置是否正在监控
     * @param monitoring
     */
    protected void setMonitoring(boolean monitoring) {
        isMonitoring = monitoring;
    }

    /**
     * 是否正在传输流
     * @return
     */
    public boolean isStreaming() {
        return isStreaming;
    }

    /**
     * 设置是否正在传输流
     * @param streaming
     */
    protected void setStreaming(boolean streaming) {
        isStreaming = streaming;
    }

    /**
     *
     * getStreamingProfile
     *
     * @return
     */
    protected StreamingProfile getStreamingProfile() {
        return mStreamingProfile;
    }

    /**
     *
     * @return
     */
    protected SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    /**
     *
     * @return
     */
    protected GLSurfaceView getGLSurfaceView() {
        if (mSurfaceView instanceof  GLSurfaceView) {
            return (GLSurfaceView) mSurfaceView;
        }
        throw new IllegalArgumentException("surfaceView is must GLSurfaceView!");
    }


    /**
     *
     * @return
     */
    protected Context getContext() {
        return mContext;
    }



    /**
     * GET TAG
     * @return
     */
    protected String getTag() {
        return getClass().getName();
    }


    /**
     *
     * @param url
     */
    @Override
    public void setPublishUrl(String url) {
        mPublishUrl = url;
    }

    /**
     * 获取推流地址
     * @return
     */
    public String getPublishUrl() {
        return mPublishUrl;
    }

    /**
     * 设置监控监听
     * @param listener
     */
    public void setOnMonitorListener(OnMonitorListener listener) {
        this.onMonitorListener = listener;
    }

    /**
     * 获取监控监听
     * @return
     */
    protected OnMonitorListener getOnMonitorListener() {
        return onMonitorListener;
    }


    /**
     * If you want to use a custom DNS server, config it
     * Not required.
     */
    private static DnsManager getMyDnsManager() {
        IResolver r0 = null;
        IResolver r1 = new DnspodFree();
        IResolver r2 = AndroidDnsServer.defaultResolver();
        try {
            r0 = new Resolver(InetAddress.getByName("119.29.29.29"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new DnsManager(NetworkInfo.normal, new IResolver[]{r0, r1, r2});
    }



    /**
     * 监控状态监听
     * @param streamingState
     * @param o
     */
    @Override
    public void onStateChanged(StreamingState streamingState, Object o) {
        Log.d(getTag(), "StreamingState :" + streamingState);
        switch (streamingState) {
            case READY:
                startStreamingInternal();
                if (getOnMonitorListener() != null) {
                    getOnMonitorListener().onReady();
                }
                break;
            case SHUTDOWN:
                setStreaming(false);
                break;
            case OPEN_CAMERA_FAIL:
                break;
            case CAMERA_SWITCHED:
                break;
            case TORCH_INFO:
                break;
            case IOERROR:
                setStreaming(false);
                // 过1分钟再次尝试发起监控
                try {
                    Thread.sleep(60000);
                } catch (Exception e) {
                    Log.e(getTag(), "IO_ERROR", e);
                }
                startStreamingInternal();
                break;
            case UNKNOWN:
                break;
            case CONNECTED:
                break;
            case PREPARING:
                break;
            case STREAMING:
                setStreaming(true);
                break;
            case CONNECTING:
                break;
            case DISCONNECTED:
                setStreaming(false);
                break;
        }
    }

}
