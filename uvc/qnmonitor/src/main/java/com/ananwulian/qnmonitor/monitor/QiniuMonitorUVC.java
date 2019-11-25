package com.ananwulian.qnmonitor.monitor;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;

import com.qiniu.pili.droid.streaming.StreamingManager;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;
import com.qiniu.pili.droid.streaming.av.common.PLFourCC;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;

import me.imli.qnmonitor.R;
import com.ananwulian.qnmonitor.base.QiniuMonitor;
import com.ananwulian.qnmonitor.utils.HandlerThreadHandler;

public class QiniuMonitorUVC extends QiniuMonitor implements StreamingSessionListener {

    /**
     *
     */
    private StreamingManager mStreamingManager;

    /**
     * UVC
     */
    private final Object mSync = new Object();
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private SurfaceView mUVCCameraView;

    private Handler mWorkerHandler;
    private long mWorkerThreadID = -1;

    public QiniuMonitorUVC(Context context, SurfaceView surfaceView) {
        super(context, surfaceView);
    }

    @Override
    protected void initMonitor() {
        super.initMonitor();
//        this.initAudio();
    }

    @Override
    protected void initStreamingManager() {

        getStreamingProfile().setPreferredVideoEncodingSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);

        mStreamingManager = new StreamingManager(getContext());
        mStreamingManager.prepare(getStreamingProfile());
        mStreamingManager.setStreamingSessionListener(this);
        mStreamingManager.setStreamingStateListener(this);

        // handler
        mWorkerHandler = HandlerThreadHandler.createHandler(getTag());
        mWorkerThreadID = mWorkerHandler.getLooper().getThread().getId();

        // UVC
        OnMyDeviceConnectListener onMyDevConnectListener = new OnMyDeviceConnectListener();
        mUSBMonitor = new USBMonitor(getContext(), onMyDevConnectListener);
        mUVCCameraView = getSurfaceView();
    }

    /**
     * 初始化声音采集
     */
    protected void initAudio() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        final int frequency = 44100;
        final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

        int recBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding)*2;
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, audioEncoding, recBufSize);

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] recBuf = new byte[recBufSize];
                while(true){
                    audioRecord.read(recBuf, 0, recBufSize);
                    mStreamingManager.inputAudioFrame(recBuf, System.nanoTime(), true);
                }
            }
        }).start();
    }


    /**
     *
     * @return
     */
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }


    /**
     *
     * @return
     */
    public StreamingManager getStreamingManager() {
        return mStreamingManager;
    }


    /**
     * 检查相机
     * @param canceled
     */
    private void checkRequestCamera(boolean canceled) {
        if (canceled && mUSBMonitor != null) {
            UsbDevice usbdevice = getUsbDevice();
            if (usbdevice != null) {
                mUSBMonitor.requestPermission(usbdevice);
            }
        }
    }

    @Override
    public void changePublishUrl(String publishUrl) {
        try {
            Log.d(getTag(), "set publish url : " + publishUrl);
            setPublishUrl(publishUrl);
            getStreamingProfile().setPublishUrl(publishUrl);
            getStreamingManager().pause();
            getStreamingManager().setStreamingProfile(getStreamingProfile());
        } catch (Exception e) {
            Log.e(getTag(), "set publish url error:", e);
        }
    }

    @Override
    public void resumeMonitor() {
        synchronized (lock) {
            try {
                mStreamingManager.resume();
                if (getOnMonitorListener() != null) {
                    getOnMonitorListener().onMonitorStart();
                }
            } catch (Exception e) {
                Log.e(getTag(), "startMonitor", e);
            }
        }
    }

    @Override
    public void pauseMonitor() {
        synchronized (lock) {
            try {
                mStreamingManager.pause();
                if (getOnMonitorListener() != null) {
                    getOnMonitorListener().onMonitorStop();
                }
            } catch (Exception e) {
                Log.e(getTag(), "pauseMonitor", e);
            }
        }
    }



    @Override
    public void startStreamingInternal() {
        new Thread(() -> {
            if (mStreamingManager != null) {
                mStreamingManager.startStreaming();
            }
        }).start();
    }


    @Override
    public void destroy() {
        if (mStreamingManager != null) {
            mStreamingManager.destroy();
        }

        releaseUvcCamera();
        releaseUsbMonitor();
    }

    @Override
    public boolean onRecordAudioFailedHandled(int i) {
        return false;
    }

    @Override
    public boolean onRestartStreamingHandled(int i) {
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

    // ========================= UVC =========================

    /**
     * 打开 UVC 摄像头
     * @param ctrlBlock
     */
    private void openUvcCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        final UVCCamera camera = new UVCCamera();
        try {
            camera.open(ctrlBlock);
            Log.i(getTag(), "supportedSize:" + camera.getSupportedSize());
            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
        } catch (final IllegalArgumentException e) {
            // fallback to YUV mode
            try {
                camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
            } catch (final IllegalArgumentException e1) {
                camera.destroy();
                return;
            }
        }
        final SurfaceView sfv = mUVCCameraView;
        if (sfv != null) {
            camera.setPreviewDisplay(sfv.getHolder());
            camera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_YUV420SP);
            camera.startPreview();
        }
        synchronized (mSync) {
            mUVCCamera = camera;
        }
        startStreamingInternal();
    }

    /**
     * 释放 UVC 摄像头
     */
    private synchronized void releaseUvcCamera() {
        if (mUVCCamera != null) {
            try {
                mUVCCamera.close();
                mUVCCamera.destroy();
            } catch (final Exception e) {
                //
                e.printStackTrace();
            }
            mUVCCamera = null;
        }
    }

    private synchronized void releaseUsbMonitor() {
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
    }

    private CameraDialog.CameraDialogParent mUVCCameraDialogParent = new CameraDialog.CameraDialogParent() {
        @Override
        public USBMonitor getUSBMonitor() {
            return mUSBMonitor;
        }

        @Override
        public void onDialogResult(boolean canceled) {
            checkRequestCamera(canceled);
        }
    };


    /**
     * 获取 USB 设备列表
     * @return
     */
    private UsbDevice getUsbDevice() {
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(getContext(), R.xml.device_filter);
        if (filter != null && filter.size() > 0 && mUSBMonitor != null) {
            List<UsbDevice> devices = (List<UsbDevice>) mUSBMonitor.getDeviceList(filter.get(0));
            if (devices != null && devices.size() > 0) {
                return devices.get(0);
            }
        }
        return null;
    }

    /**
     * 预览回调
     */
    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            // TODO

            int len = frame.capacity();
            byte[] yuv = new byte[len];
            frame.get(yuv);

            int captureWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH;
            int captureHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;
//            int captureWidth = mUVCCamera.getPreviewSize().width;
//            int captureHeight = mUVCCamera.getPreviewSize().height;
            int rotation = 0;
            boolean mirror = false;
            int fmt = PLFourCC.FOURCC_NV12;
            long tsInNanoTime = System.nanoTime();

//            Log.d("inputVideoFrame", "" +
//                    "\n=================================" +
//                    "\nyuv length : " + len +
//                    "\ncaptureWidth : " + captureWidth +
//                    "\ncaptureHeight : " + captureHeight +
//                    "\nrotation : " + rotation +
//                    "\nfmt : " + fmt +
//                    "\ntime : " + tsInNanoTime +
//                    "\n=================================");

            mStreamingManager.inputVideoFrame(yuv, captureWidth, captureHeight, rotation, mirror, fmt, tsInNanoTime);
        }
    };


    /**
     *
     */
    class OnMyDeviceConnectListener implements USBMonitor.OnDeviceConnectListener {

        @Override
        public void onAttach(UsbDevice device) {
            synchronized (mSync) {
                mUVCCameraDialogParent.onDialogResult(true);
            }
        }

        @Override
        public void onDettach(UsbDevice device) {
            releaseUvcCamera();
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            releaseUvcCamera();
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    openUvcCamera(ctrlBlock);
                }
            }, 0);
        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    releaseUvcCamera();
                }
            }, 0);
        }

        @Override
        public void onCancel(UsbDevice device) {
        }

    }



    /**
     * ワーカースレッド上で指定したRunnableを実行する
     * 未実行の同じRunnableがあればキャンセルされる(後から指定した方のみ実行される)
     * @param task
     * @param delayMillis
     */
    protected final synchronized void queueEvent(final Runnable task, final long delayMillis) {
        if ((task == null) || (mWorkerHandler == null)) return;
        try {
            mWorkerHandler.removeCallbacks(task);
            if (delayMillis > 0) {
                mWorkerHandler.postDelayed(task, delayMillis);
            } else if (mWorkerThreadID == Thread.currentThread().getId()) {
                task.run();
            } else {
                mWorkerHandler.post(task);
            }
        } catch (final Exception e) {
            // ignore
        }
    }

}
