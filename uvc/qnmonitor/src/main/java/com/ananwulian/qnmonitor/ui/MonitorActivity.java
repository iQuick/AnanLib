package com.ananwulian.qnmonitor.ui;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.SurfaceView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ananwulian.qnmonitor.monitor.QiniuMonitorUVC;
import com.ananwulian.qnmonitor.base.QiniuMonitor;
import com.ananwulian.qnmonitor.listener.OnMonitorListener;

public abstract class MonitorActivity extends AppCompatActivity implements OnMonitorListener {

    // 监控画面<GLSurfaceView>
    private SurfaceView mGlSV;

    private QiniuMonitor mMonitor = null;

    // 是否正在监控
    boolean isMonitor = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());
        this.init();
    }

    protected void init() {
        this.mGlSV = getMonitorGLSurfaceView();
        initMonitor();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mMonitor instanceof QiniuMonitorUVC) {
            ((QiniuMonitorUVC) mMonitor).getUSBMonitor().register();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mMonitor instanceof QiniuMonitorUVC) {
            ((QiniuMonitorUVC) mMonitor).getUSBMonitor().unregister();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMonitor != null) {
            mMonitor.pauseMonitor();
            mMonitor.destroy();
        }
    }

    /**
     * 获取布局 layout id
     * @return
     */
    protected abstract @LayoutRes int getLayoutRes();

    /**
     * 获取监控画面 GLSurfaceView
     * @return
     */
    protected abstract SurfaceView getMonitorGLSurfaceView();

    /**
     *
     * @return
     */
    protected QiniuMonitor getMonitor() {
        return mMonitor;
    }

    /**
     * 初始化监控
     */
    protected void initMonitor() {
        mMonitor = new QiniuMonitorUVC(this, this.mGlSV);
        mMonitor.setOnMonitorListener(this);
        String url = getIntent().getStringExtra("url");
        mMonitor.setPublishUrl(url);
        mMonitor.init();
//        if (mMonitor instanceof QiniuMonitorNormal) {
//            ((QiniuMonitorNormal) mMonitor).getStreamingManager().setStreamingStateListener(this);
//        } else if (mMonitor instanceof QiniuMonitorUVC) {
//            ((QiniuMonitorUVC) mMonitor).getStreamingManager().setStreamingStateListener(this);
//        }
    }

    /**
     * 设置推流地址
     * @param publishUrl
     */
    public void changePublishUrl(String publishUrl) {
        mMonitor.changePublishUrl(publishUrl);
    }

    /**
     * 开启监控
     */
    public void startMonitor() {
        mMonitor.resumeMonitor();
        isMonitor = true;
    }

    /**
     * 恢复监控
     */
    public void resumeMonitor() {
        mMonitor.resumeMonitor();
        isMonitor = true;
    }

    /**
     * 暂停监控
     */
    public void pauseMonitor() {
        mMonitor.pauseMonitor();
        isMonitor = false;
    }

    /**
     * 停止监控
     * @param status
     */
    public void stopMonitor(final int status) {
        mMonitor.pauseMonitor();
        isMonitor = false;
    }

    /**
     * 是否正在监控
     * @return
     */
    public boolean isMonitoring() {
        return mMonitor.isMonitoring();
    }

    /**
     * 是否正在推流
     * @return
     */
    public boolean isStreaming() {
        return mMonitor.isStreaming();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeMonitor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseMonitor();
    }

    @Override
    public void onReady() {
        // TODO
    }

    @Override
    public void onMonitorStart() {
        // TODO
    }

    @Override
    public void onMonitorStop() {
        // TODO
    }
}
