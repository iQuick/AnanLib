package com.ananwulian.qrcode;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ananwulian.qrcode.camera.CameraManager;
import com.ananwulian.qrcode.decode.MainHandler;
import com.ananwulian.qrcode.utils.BeepManager;
import com.ananwulian.qrcode.uvc.UVCDecode;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Desc: 1:启动一个SurfaceView作为取景预览
 * 2:开启camera,在后台独立线程中完成扫描任务
 * 3:对解码返回的结果进行处理.
 * 4:释放资源
 */
public class CaptureActivity extends BaseActivity {
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 26;
    public static final String EXTRA_STRING = "content";
    public static final int TYPE_BOOK_COVER = 0x101;
    public static final int TYPE_BOOK_CHAPTER = 0x102;
    private static final String TAG = "CaptureActivity";
    private MainHandler mainHandler;
    private SurfaceHolder mHolder;

    // UVC
    private UVCDecode mUVCDecode = null;
    private final Object mSync = new Object();
    private boolean isUVC = false;
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private SurfaceView mUVCCameraView;


    private CameraManager mCameraManager;
    private BeepManager beepManager;

    private SurfaceView scanPreview;
    private ViewGroup scanContainer;
    private ViewGroup scanCropView;
    private ImageView scanLine;
    private Rect mCropRect = null;

    private boolean isHasSurface = false;
    private boolean isOpenCamera = false;

    public Handler getHandler() {
        return mainHandler;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_capture);
        initView();
        isOpenCamera = checkPermissionCamera();
        initUVC();
    }

    private void initView() {
        scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
        scanContainer = (ViewGroup) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);
        isHasSurface = false;
        beepManager = new BeepManager(this);

        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation
                .RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.9f);
        animation.setDuration(3000);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);

    }

    private void initUVC() {
        OnMyDeviceConnectListener onMyDevConnectListener = new OnMyDeviceConnectListener();
        mUSBMonitor = new USBMonitor(this, onMyDevConnectListener);
        mUVCCameraView = scanPreview;
    }

    @Override
    protected void onStart() {
        super.onStart();
        synchronized (mSync) {
            if (mUSBMonitor != null) {
                mUSBMonitor.register();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseUvcCamera();
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isOpenCamera) {
            mHolder = scanPreview.getHolder();
            mCameraManager = new CameraManager(getApplication());
            if (isHasSurface) {
                initCamera(mHolder);
            } else {
                mHolder.addCallback(shCallback);
                mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }
        }
    }

    @Override
    public void onPause() {
        releaseCamera();
        super.onPause();
        if (scanLine != null) {
            scanLine.clearAnimation();
            scanLine.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //remove SurfaceCallback
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(shCallback);
        }
    }

    private Activity getActivity() {
        return this;
    }

    private Context getContext() {
        return this;
    }

    //region 初始化和回收相关资源
    private void initCamera(SurfaceHolder surfaceHolder) {
        mainHandler = null;
        try {
            mCameraManager.openDriver(surfaceHolder);
            if (mainHandler == null) {
                mainHandler = new MainHandler(this, mCameraManager);
            }
        } catch (IOException ioe) {
            Log.e(TAG, "相机被占用", ioe);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.e(TAG, "Unexpected error initializing camera");
            displayFrameworkBugMessageAndExit();
        }

    }

    private void releaseCamera() {
        if (null != mainHandler) {
            //关闭聚焦,停止预览,清空预览回调,quit子线程looper
            mainHandler.quitSynchronously();
            mainHandler = null;
        }
//        //关闭声音
//        if (null != beepManager) {
//            Log.e(TAG, "releaseCamera: beepManager release" );
//            beepManager.releaseRing();
//            beepManager = null;
//        }
        //关闭相机
        if (mCameraManager != null) {
            mCameraManager.closeDriver();
            mCameraManager = null;
        }
    }
    //endregion

    private void displayFrameworkBugMessageAndExit() {
        String per = String.format(getString(R.string.permission), getString(R.string.camera), getString(R.string.camera));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.qr_name));
        builder.setMessage(per);
        builder.setPositiveButton(getString(R.string.i_know), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CaptureActivity.this.finish();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                CaptureActivity.this.finish();
            }
        });
        builder.show();
    }
    //endregion

    //region 扫描结果
    public void checkResult(final String result) {
        if (beepManager != null) {
            beepManager.startRing();
        }

        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                activityResult(result.trim());
            }
        }, beepManager.getTimeDuration());

//        mainHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                activityResult(result.trim());
//            }
//        }, beepManager.getTimeDuration());
    }

    private void activityResult(String result) {
        if (!isFinishing()) {
            Intent intent =new Intent();
            Bundle bundle = new Bundle();
//            bundle.putInt("width", mCropRect.width());
//            bundle.putInt("height", mCropRect.height());
            bundle.putString(EXTRA_STRING, result);
            intent.putExtras(bundle);
            setResult(RESULT_OK,intent);
            CaptureActivity.this.finish();
        }
    }

    //endregion

    private Rect calculateCrop() {
        int captureWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH;
        int captureHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;

        int previewWidth = scanPreview.getWidth();
        int previewHeight = scanPreview.getHeight();

        Log.d("==========", String.format("calculateCrop %s-%s, %s-%s", captureWidth, captureHeight, previewWidth, previewHeight));

        float zoomW = (float) captureWidth / previewWidth;
        float zoomH = (float) captureHeight / previewHeight;

        int[] scanCropLocation = new int[2];
        scanCropView.getLocationInWindow(scanCropLocation);

        int left = (int) (scanCropLocation[0] * zoomW);
        int top = (int) (scanCropLocation[1] * zoomH);
        int right = (int) ((scanCropLocation[0] + scanCropView.getWidth()) * zoomW);
        int bottom = (int) ((scanCropLocation[1] + scanCropView.getHeight()) * zoomH);

        return new Rect(left, top, right, bottom);
    }

    //region  初始化截取的矩形区域
    public Rect initCrop() {
        int cameraWidth = 0;
        int cameraHeight = 0;
        if (null != mCameraManager) {
            cameraWidth = mCameraManager.getCameraResolution().y;
            cameraHeight = mCameraManager.getCameraResolution().x;
        }

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
        return new Rect(x, y, width + x, height + y);
    }

    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    //endregion

    //region SurfaceHolder Callback 回调方法
    private SurfaceHolder.Callback shCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            if (surfaceHolder == null) {
                Log.e(TAG, "*** 没有添加SurfaceHolder的Callback");
            }
            if (isOpenCamera) {
                if (!isHasSurface) {
                    isHasSurface = true;
                    initCamera(surfaceHolder);
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            Log.e(TAG, "surfaceChanged: ");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            isHasSurface = false;
        }
    };
    //endregion


    // ====================================== UVC ======================================
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
        final UVCCamera camera = new UVCCamera();
        try {
            camera.open(ctrlBlock);
            Log.i(TAG, "supportedSize:" + camera.getSupportedSize());
            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_YUYV);
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
            mUVCDecode = new UVCDecode((CaptureActivity) getActivity(), calculateCrop());
        }
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
     * Usb 设备连接监听
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
            Toast.makeText(getActivity(), "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
            releaseUvcCamera();
            isUVC = false;
        }

        @Override
        public void onConnect(UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            isUVC = true;
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
     * 获取 USB 设备列表
     * @return
     */
    private UsbDevice getUsbDevice() {
        final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(getActivity(), R.xml.device_filter);
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
            if (mUVCDecode != null) {
                mUVCDecode.postDecode(frame);
            }
        }
    };





}
