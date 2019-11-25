package com.ananwulian.takephoto;

import android.hardware.usb.UsbDevice;
import android.os.Environment;
import android.util.Log;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.common.UVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.UVCCameraTextureView;

import java.nio.ByteBuffer;
import java.util.List;

public class  UVCTakePhotoActivity extends TakePhotoActivity {


    /**
     * UVC
     */
    private final Object mSync = new Object();
    private USBMonitor mUSBMonitor;
    private UVCCameraTextureView mUVCCameraView;
    private UVCCameraHandler mUvcCameraHandler;

    // 保存路径
    private String mSavePath = null;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_talk_photo_uvc;
    }

    @Override
    protected void init() {
        // UVC
        OnMyDeviceConnectListener onMyDevConnectListener = new OnMyDeviceConnectListener();
        mUSBMonitor = new USBMonitor(this, onMyDevConnectListener);
        mUVCCameraView = findViewById(R.id.camera_view);
    }

    @Override
    public void takePhoto(String savePth) {
        if (mUvcCameraHandler != null) {
            mUvcCameraHandler.captureStill(savePth, new AbstractUVCCameraHandler.OnCaptureListener() {
                @Override
                public void onCaptureResult(String picPath) {
                    onTakePhotoResult(picPath);
                }
            });
        }
    }

    @Override
    public void startRecord(String savePath) {
        if (mUvcCameraHandler != null) {
            RecordParams params = new RecordParams();
            params.setRecordPath(savePath);
            params.setRecordDuration(0);                        // 设置为0，不分割保存
            this.mSavePath = savePath;
            mUvcCameraHandler.startRecording(params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                @Override
                public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                }

                @Override
                public void onRecordResult(String videoPath) {
                }
            });
        }
    }

    @Override
    public void stopRecord() {
        if (mUvcCameraHandler != null) {
            mUvcCameraHandler.stopRecording();
            onRecordResult(mSavePath);
            mSavePath = null;
        }
    }


    /**
     * 结果回调方法
     * @param path
     */
    public void onTakePhotoResult(String path) {
        // TODO
    }

    /**
     * 录像回调结果
     * @param paht
     */
    public void onRecordResult(String paht) {
        // TODO
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mUSBMonitor != null) {
            mUSBMonitor.register();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
        }
    }

    @Override
    protected synchronized void onDestroy() {
        super.onDestroy();
        if (mUvcCameraHandler != null) {
            mUvcCameraHandler.stopPreview();
            mUvcCameraHandler.close();
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
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

    /**
     * 打开 UVC 摄像头
     * @param ctrlBlock
     */
    private void openUvcCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        mUvcCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView, 2, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
        mUvcCameraHandler.open(ctrlBlock);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // wait for camera created
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // start previewing
                mUvcCameraHandler.startPreview(mUVCCameraView.getSurface());
            }
        }).start();

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
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(this, R.xml.device_filter);
        if (filter != null && filter.size() > 0 && mUSBMonitor != null) {
            List<UsbDevice> devices = (List<UsbDevice>) mUSBMonitor.getDeviceList(filter.get(0));
            if (devices != null && devices.size() > 0) {
                return devices.get(0);
            }
        }
        return null;
    }

    /**
     *
     */
    private void releaseUvcCamera() {
        mUvcCameraHandler.stopPreview();
    }

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
        public void onConnect(UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
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



}
