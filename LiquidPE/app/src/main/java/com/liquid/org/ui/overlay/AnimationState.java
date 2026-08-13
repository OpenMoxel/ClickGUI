/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.overlay;

import android.os.SystemClock;

public final class AnimationState {
    private float startValue;
    private float value;
    private float targetValue;
    private long startTime;
    private long duration;

    public AnimationState(float initialValue) {
        startValue = value = targetValue = initialValue;
    }

    public float get() { return get(SystemClock.uptimeMillis()); }

    public float get(long now) {
        if (value == targetValue || duration <= 0L) return targetValue;
        float t = Math.min(1f, (now - startTime) / (float) duration);
        float eased = easeOutCubic(t);
        value = startValue + (targetValue - startValue) * eased;
        if (t >= 1f) value = targetValue;
        return value;
    }

    public void animateTo(float target, long durationMs) {
        long now = SystemClock.uptimeMillis();
        startValue = get(now);
        targetValue = target;
        startTime = now;
        duration = durationMs;
    }

    public void snapTo(float target) {
        startValue = value = targetValue = target;
        startTime = 0L;
        duration = 0L;
    }

    public boolean isRunning(long now) {
        return value != targetValue && now - startTime < duration;
    }

    public float getTarget() { return targetValue; }

    public static float easeOutCubic(float t) {
        float p = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - p * p * p;
    }
}
