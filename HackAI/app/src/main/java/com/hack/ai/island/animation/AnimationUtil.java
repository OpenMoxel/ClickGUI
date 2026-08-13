/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.island.animation;

public final class AnimationUtil {
    public static long delta = 0L;

    private AnimationUtil() {
    }

    public static float smooth(float current, float target, float speed) {
        float cur = current;
        long dt = delta;
        float spd = Math.abs(target - cur) * speed;
        if (dt < 1L) dt = 1L;
        float smoothing = Math.max(spd * (dt / 16f), 0.15f);
        float diff = cur - target;
        if (diff > spd) {
            cur = Math.max(cur - smoothing, target);
        } else if (diff < -spd) {
            cur = Math.min(cur + smoothing, target);
        } else {
            cur = target;
        }
        return cur;
    }

    public static double smooth(double current, double target, double speed) {
        double cur = current;
        long dt = delta;
        double spd = Math.abs(target - cur) * speed;
        if (dt < 1L) dt = 1L;
        double smoothing = Math.max(spd * (dt / 16.0), 0.15);
        double diff = cur - target;
        if (diff > spd) {
            cur = Math.max(cur - smoothing, target);
        } else if (diff < -spd) {
            cur = Math.min(cur + smoothing, target);
        } else {
            cur = target;
        }
        return cur;
    }
}
