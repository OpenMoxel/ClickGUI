/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.notification;

import android.view.View;
import android.view.animation.PathInterpolator;

public final class NotificationAnimator {

    private static final PathInterpolator easeOut = new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    private static final PathInterpolator easeIn = new PathInterpolator(0.7f, 0f, 0.84f, 0f);

    private NotificationAnimator() {
    }

    public static void playEnter(View view, Runnable onEnd) {
        view.setTranslationX(view.getWidth() + 50f * view.getResources().getDisplayMetrics().density);
        view.setAlpha(1f);
        view.animate().translationX(0f).setDuration(300)
                .setInterpolator(easeOut).withEndAction(() -> {
                    if (onEnd != null) onEnd.run();
                }).start();
    }

    public static void playEnter(View view) {
        playEnter(view, null);
    }

    public static void playExit(View view, Runnable onEnd) {
        view.animate()
                .translationX(view.getWidth() + 50f * view.getResources().getDisplayMetrics().density)
                .alpha(0f).setDuration(250)
                .setInterpolator(easeIn).withEndAction(() -> {
                    if (onEnd != null) onEnd.run();
                }).start();
    }

    public static void playExit(View view) {
        playExit(view, null);
    }

    public static void playShiftUp(View view, float dy, Runnable onEnd) {
        view.animate().translationY(view.getTranslationY() + dy)
                .setDuration(250).setInterpolator(easeOut)
                .withEndAction(() -> {
                    if (onEnd != null) onEnd.run();
                }).start();
    }

    public static void playShiftUp(View view, float dy) {
        playShiftUp(view, dy, null);
    }
}
