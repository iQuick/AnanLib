package com.ananwulian.takephoto;

import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.serenegiant.common.BaseActivity;

public abstract class TakePhotoActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());
        this.init();
    }


    /**
     * 获取布局
     * @return
     */
    protected abstract @LayoutRes int getLayoutRes();

    /**
     * 初始化
     */
    protected abstract void init();


    /**
     * 拍照
     * @param savePth
     */
    public abstract void takePhoto(String savePth);


    /**
     * 开始录像
     * @param savePath
     */
    public abstract void startRecord(String savePath);

    /**
     * 停止录像
     */
    public abstract void stopRecord();


}
