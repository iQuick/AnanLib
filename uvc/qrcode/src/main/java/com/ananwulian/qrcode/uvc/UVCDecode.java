package com.ananwulian.qrcode.uvc;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.ananwulian.qrcode.CaptureActivity;
import com.dtr.zbar.build.ZBarDecoder;
import com.serenegiant.usb.UVCCamera;

import java.nio.ByteBuffer;

public class UVCDecode {

    private CaptureActivity activity;

    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;
    private Rect cropRect = null;
    private ZBarDecoder zBarDecoder = null;

    public UVCDecode(CaptureActivity activity, Rect cropRect) {
        activity = activity;
        this.cropRect = cropRect;
        this.zBarDecoder = new ZBarDecoder();
        this.mHandlerThread = new HandlerThread(getClass().getName());
        this.mHandlerThread.start();
        this.mHandler = new Handler(mHandlerThread.getLooper(), callback());
    }

    private Handler.Callback callback() {
        return new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                ByteBuffer frame = (ByteBuffer) msg.obj;
                frame.clear();
                int len = frame.capacity();
                byte[] yuv = new byte[len];
                frame.get(yuv);
                decode(yuv);
                return false;
            }
        };
    }

    public void postDecode(ByteBuffer buffer) {
        mHandler.sendMessage(mHandler.obtainMessage(1, buffer));
    }


    private void decode(byte[] yuv) {

        int captureWidth = UVCCamera.DEFAULT_PREVIEW_WIDTH;
        int captureHeight = UVCCamera.DEFAULT_PREVIEW_HEIGHT;
        Log.d("=====", "cropRect : " + cropRect.toString());
        String result = zBarDecoder.decodeCrop(yuv, captureWidth, captureHeight, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());

        if (!TextUtils.isEmpty(result)) {
            Log.d("=====", "result : " + result);
            activity.checkResult(result);
            mHandlerThread.quitSafely();
            mHandler = null;
        }
    }

}
