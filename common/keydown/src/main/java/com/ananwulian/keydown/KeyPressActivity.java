package com.ananwulian.keydown;


import android.app.Activity;
import android.view.KeyEvent;

import java.util.List;

public class KeyPressActivity extends Activity {

    private final String TAG = getClass().getName();

    // 锁定长按事件
    private boolean lockLongPressKey = false;

    /**
     * 注册 监听事件的 key
     * @return
     */
    protected List<Integer> registerKey() {
        return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        List<Integer> list = registerKey();
        if (list == null || list.size() == 0 || list.contains(keyCode)) {
            if (event.getRepeatCount() < 5) {//识别长按短按的代码
                event.startTracking();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (lockLongPressKey) {
            lockLongPressKey = false;
            return true;
        }
        KeyPress.sendKeyEventBroadcast(this, PressType.SHORT, keyCode);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        lockLongPressKey = true;
        KeyPress.sendKeyEventBroadcast(this, PressType.LONG, keyCode);
        return super.onKeyLongPress(keyCode, event);
    }


}
