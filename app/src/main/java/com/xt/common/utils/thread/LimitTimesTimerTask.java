package com.xt.common.utils.thread;

import java.util.TimerTask;

/**
 * @author xt on 2019/6/12 14:36
 */
public class LimitTimesTimerTask extends TimerTask {
    private volatile int                         mTimes;
    private          LimitTimesTimerTaskCallback mLimitTimesTimerTaskCallback;

    public LimitTimesTimerTask(int times, LimitTimesTimerTaskCallback limitTimesTimerTaskCallback) {
        super();
        this.mTimes = times;
        mLimitTimesTimerTaskCallback = limitTimesTimerTaskCallback;
    }

    @Override
    public void run() {
        if (mTimes < 1) {
            cancel();
            if (mLimitTimesTimerTaskCallback != null) {
                mLimitTimesTimerTaskCallback.onTimerTaskEnd();
            }
        } else {
            if (mLimitTimesTimerTaskCallback != null) {
                mLimitTimesTimerTaskCallback.onDoTimerTask(mTimes);
                mTimes = mTimes - 1;
            }
        }
    }

    public interface LimitTimesTimerTaskCallback {
        void onDoTimerTask(int times);

        void onTimerTaskEnd();
    }

    public int getTimes() {
        return mTimes;
    }
}
