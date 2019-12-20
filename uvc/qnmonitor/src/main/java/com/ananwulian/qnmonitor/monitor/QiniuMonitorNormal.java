package com.ananwulian.qnmonitor.monitor;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.SurfaceView;

import com.qiniu.pili.droid.streaming.MediaStreamingManager;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;

import java.util.List;

import com.ananwulian.qnmonitor.base.QiniuMonitor;

import static com.qiniu.pili.droid.streaming.AVCodecType.SW_VIDEO_WITH_SW_AUDIO_CODEC;

public class QiniuMonitorNormal extends QiniuMonitor implements StreamingSessionListener {


    /**
     * MediaStreamingManager
     */
    private MediaStreamingManager mMediaStreamingManager;

    public QiniuMonitorNormal(Context context, GLSurfaceView glSurfaceView) {
        super(context, glSurfaceView);
    }

    @Override
    protected void initStreamingManager() {
        mMediaStreamingManager = new MediaStreamingManager(getContext(), getGLSurfaceView(), SW_VIDEO_WITH_SW_AUDIO_CODEC);
        mMediaStreamingManager.mute(false);
        mMediaStreamingManager.setStreamingSessionListener(this);
        mMediaStreamingManager.setStreamingStateListener(this);
    }

    @Override
    public void prepare() {
        mMediaStreamingManager.prepare(getStreamingProfile());
    }

    @Override
    public void changePublishUrl(String publishUrl) {
        try {
            Log.d(getTag(), "change publish url : " + publishUrl);
            setPublishUrl(publishUrl);
            getStreamingProfile().setPublishUrl(publishUrl);
            getStreamingManager().stopStreaming();
            getStreamingManager().setStreamingProfile(getStreamingProfile());
            resumeMonitor();
        } catch (Exception e) {
            Log.e(getTag(), "change publish url error:", e);
        }
    }

    /**
     * 切换摄像头
     * @param cameraId
     * @return
     */
    public boolean setCameraId(int cameraId) {
        return mMediaStreamingManager.switchCamera();
    }


    /**
     *
     * @return
     */
    public MediaStreamingManager getStreamingManager() {
        return mMediaStreamingManager;
    }

    @Override
    public void resumeMonitor() {
        synchronized (lock) {
            try {
                mMediaStreamingManager.resume();
                if (getOnMonitorListener() != null) {
                    getOnMonitorListener().onMonitorStart();
                }
            } catch (Exception e) {
                Log.e(getTag(), "resumeMonitor", e);
            }
        }
    }

    @Override
    public void pauseMonitor() {
        synchronized (lock) {
            try {
                mMediaStreamingManager.pause();
                if (getOnMonitorListener() != null) {
                    getOnMonitorListener().onMonitorStop();
                }
            } catch (Exception e) {
                Log.e(getTag(), "pauseMonitor", e);
            }
        }
    }

    @Override
    public void destroy() {
        if (mMediaStreamingManager != null) {
            mMediaStreamingManager.destroy();
        }
    }


    @Override
    public boolean onRecordAudioFailedHandled(int i) {
        return false;
    }

    @Override
    public boolean onRestartStreamingHandled(int i) {
        Log.d(getTag(), "onRestartStreamingHandled");
        startStreamingInternal();
        return false;
    }

    @Override
    public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
        return null;
    }

    @Override
    public int onPreviewFpsSelected(List<int[]> list) {
        return 0;
    }

    @Override
    public void startStreamingInternal() {
        new Thread(() -> {
            if (mMediaStreamingManager != null) {
                mMediaStreamingManager.startStreaming();
            }
        }).start();
    }

}
