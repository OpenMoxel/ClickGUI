/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

import java.util.concurrent.CopyOnWriteArrayList;

/** 全局主题/质感状态管理器：仅控制 ClickGUI 面板，不影响通知/ArrayList */
public final class ThemeManager {
    private static volatile boolean isDark = false;
    public static int accentColor = 0xFF1C1B1F;

    // ---- Glass ----
    private static volatile boolean isGlass = false;
    private static volatile boolean isTransitioning = false;
    private static volatile float glassProgress = 0f;
    private static ValueAnimator glassAnimator;
    private static final CopyOnWriteArrayList<OnGlassChangeListener> glassListeners = new CopyOnWriteArrayList<>();
    private static final int GLASS_DURATION_MS = 400;

    /** 玻璃态背景 alpha 范围：240(几乎不透明) → 140(通透) */
    public static final int GLASS_ALPHA_MAX = 240;
    public static final int GLASS_ALPHA_MIN = 140;

    /** cubic-ease-out 曲线：加速衰减，毛玻璃质感更快显现 */
    public static float glassEasedProgress() {
        float t = glassProgress;
        return (float) (1 - Math.pow(1 - t, 3));
    }

    public static boolean isDark() { return isDark; }

    public static void init(AppPreferences prefs) {
        String saved = prefs.setting("iface_theme", "light");
        isDark = "dark".equals(saved);
        updateAccent();
        String style = prefs.setting("style", "solid");
        isGlass = "glass".equals(style);
        glassProgress = isGlass ? 1f : 0f;
    }

    public static void setDark(boolean dark, AppPreferences prefs) {
        if (isDark == dark) return;
        isDark = dark;
        prefs.setSetting("iface_theme", dark ? "dark" : "light");
        updateAccent();
    }

    private static void updateAccent() {
        accentColor = isDark ? 0xFFFFFFFF : 0xFF1C1B1F;
    }

    public static int themedColor(int darkVal, int lightVal) {
        return isDark ? darkVal : lightVal;
    }

    // ==================== Glass 动画系统 ====================

    public static boolean isGlass() { return isGlass; }
    public static float getGlassProgress() { return glassProgress; }

    public static void setGlass(boolean glass) {
        if (isGlass == glass && !isTransitioning) return;
        isGlass = glass;

        if (glassAnimator != null && glassAnimator.isRunning()) {
            glassAnimator.cancel();
        }

        float from = glassProgress;
        float to = glass ? 1f : 0f;
        isTransitioning = true;

        glassAnimator = ValueAnimator.ofFloat(from, to);
        glassAnimator.setDuration(GLASS_DURATION_MS);
        glassAnimator.setInterpolator(new DecelerateInterpolator());
        glassAnimator.addUpdateListener(animation -> {
            glassProgress = (float) animation.getAnimatedValue();
            for (OnGlassChangeListener l : glassListeners) {
                l.onGlassProgress(glassProgress);
            }
        });
        glassAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                isTransitioning = false;
                glassProgress = to;
            }
        });
        glassAnimator.start();
    }

    public static void saveGlass(AppPreferences prefs) {
        prefs.setSetting("style", isGlass ? "glass" : "solid");
    }

    public static void addOnGlassChangeListener(OnGlassChangeListener l) {
        if (l != null && !glassListeners.contains(l)) glassListeners.add(l);
    }

    public static void removeOnGlassChangeListener(OnGlassChangeListener l) {
        glassListeners.remove(l);
    }

    private ThemeManager() {}
}
