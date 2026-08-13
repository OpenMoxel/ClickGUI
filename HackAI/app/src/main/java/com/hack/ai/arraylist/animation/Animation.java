/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.animation;

import android.os.SystemClock;

public class Animation {

    private final Easing easing;
    private long durationMillis;
    private long startTimeMillis;
    private float startValue;
    private float targetValue;
    private float value;
    private boolean finished = true;

    public Animation(Easing easing, long durationMillis) {
        this.easing = easing == null ? Easing.LINEAR : easing;
        this.durationMillis = Math.max(0L, durationMillis);
        this.startTimeMillis = SystemClock.uptimeMillis();
    }

    public void run(float targetValue) {
        long now = SystemClock.uptimeMillis();
        if (this.targetValue != targetValue) {
            this.targetValue = targetValue;
            this.startValue = value;
            this.startTimeMillis = now;
            this.finished = false;
        }

        if (durationMillis == 0L) {
            value = targetValue;
            finished = true;
            return;
        }

        float progress = (float) (now - startTimeMillis) / (float) durationMillis;
        if (progress >= 1.0f) {
            value = targetValue;
            finished = true;
            return;
        }

        float eased = easing.apply(progress);
        value = startValue + (targetValue - startValue) * eased;
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            value = targetValue;
            finished = true;
        }
    }

    public void snapTo(float value) {
        this.value = value;
        this.startValue = value;
        this.targetValue = value;
        this.startTimeMillis = SystemClock.uptimeMillis();
        this.finished = true;
    }

    public void resetFrom(float value) {
        this.value = value;
        this.startValue = value;
        this.startTimeMillis = SystemClock.uptimeMillis();
        this.finished = false;
    }

    public float getValue() { return value; }
    public float getTargetValue() { return targetValue; }
    public boolean isFinished() { return finished; }
    public long getDurationMillis() { return durationMillis; }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = Math.max(0L, durationMillis);
    }
}
