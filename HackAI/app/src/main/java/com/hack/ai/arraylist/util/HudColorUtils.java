/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.util;

import android.graphics.Color;

/**
 * HUD color theming — 1:1 port of Solstice ColorUtils + Interface::ColorThemes.
 */
public final class HudColorUtils {

    public static final int THEME_TRANS = 0;
    public static final int THEME_RAINBOW = 1;
    public static final int THEME_BUBBLEGUM = 2;
    public static final int THEME_WATERMELON = 3;
    public static final int THEME_SUNSET = 4;
    public static final int THEME_POISON = 5;
    public static final int THEME_CUSTOM = 6;

    private static final int[][] PALETTES = {
            // Trans
            {argb(91, 206, 250), argb(245, 169, 184), argb(255, 255, 255), argb(245, 169, 184)},
            // Rainbow (empty — handled specially)
            {},
            // Bubblegum
            {argb(255, 99, 202), argb(255, 195, 195), argb(146, 245, 255), argb(249, 255, 148),
                    argb(135, 255, 176)},
            // Watermelon
            {argb(255, 70, 70), argb(139, 0, 0), argb(144, 238, 144), argb(34, 139, 34),
                    argb(204, 255, 204)},
            // Sunset
            {argb(213, 32, 0), argb(239, 118, 39), argb(255, 154, 86), argb(255, 255, 255),
                    argb(209, 98, 164), argb(181, 86, 144)},
            // Poison
            {argb(115, 222, 70), argb(67, 201, 89), argb(41, 230, 94), argb(12, 210, 83),
                    argb(87, 211, 72), argb(57, 210, 124)},
            // Custom (filled at runtime)
            {}
    };

    private static final float[] HSV = new float[3];

    private HudColorUtils() {}

    private static int argb(int r, int g, int b) {
        return Color.argb(255, r, g, b);
    }

    public static int rainbow(float seconds, float saturation, float brightness,
                               int index, long now) {
        if (seconds <= 0f) seconds = 0.01f;
        int period = (int) (seconds * 1000f);
        if (period <= 0) period = 1;
        float hue = ((now + index) % period) / (float) period;
        HSV[0] = hue * 360f;
        HSV[1] = saturation;
        HSV[2] = brightness;
        return Color.HSVToColor(HSV);
    }

    public static int lerpColors(float seconds, float index, int[] colors, long ms) {
        if (colors == null || colors.length == 0) return argb(255, 255, 255);
        if (seconds <= 0f) seconds = 0.01f;
        float time = 10000.0f / seconds;
        int timeInt = (int) time;
        if (timeInt <= 0) timeInt = 1;
        long base = (ms == 0L) ? System.currentTimeMillis() : ms;
        float angle = (base + (int) index) % timeInt;
        float segmentTime = time / colors.length;
        int segmentIndex = (int) (angle / segmentTime);
        float segmentIndexFloat = angle / segmentTime - segmentIndex;
        int startColor = colors[segmentIndex];
        int endColor = colors[(segmentIndex + 1) % colors.length];
        return lerpColor(startColor, endColor, segmentIndexFloat);
    }

    public static int getThemedColor(int theme, float colorSpeed, float saturation,
                                      int[] customColors, float index, long now) {
        if (theme == THEME_RAINBOW) {
            return rainbow(colorSpeed, saturation, 1f, (int) index, now);
        }
        int[] colors;
        if (theme == THEME_CUSTOM) {
            colors = customColors;
        } else if (theme >= 0 && theme < PALETTES.length) {
            colors = PALETTES[theme];
        } else {
            return argb(255, 255, 255);
        }
        if (colors == null || colors.length == 0) {
            return argb(255, 255, 255);
        }
        return lerpColors(colorSpeed, index, colors, now);
    }

    public static int lerpColor(int a, int b, float t) {
        if (t <= 0f) return a;
        if (t >= 1f) return b;
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a), aa = Color.alpha(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b), ba = Color.alpha(b);
        return Color.argb(
                Math.round(aa + (ba - aa) * t),
                Math.round(ar + (br - ar) * t),
                Math.round(ag + (bg - ag) * t),
                Math.round(ab + (bb - ab) * t));
    }

    public static int shade(int color, float mult) {
        return Color.argb(Color.alpha(color),
                clamp255(Math.round(Color.red(color) * mult)),
                clamp255(Math.round(Color.green(color) * mult)),
                clamp255(Math.round(Color.blue(color) * mult)));
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(clamp255(alpha), Color.red(color), Color.green(color),
                Color.blue(color));
    }

    public static int withAlpha(int color, float alpha) {
        return withAlpha(color, Math.round(clamp01(alpha) * 255f));
    }

    private static int clamp255(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
}
