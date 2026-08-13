/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.overlay;

import android.graphics.Color;

public final class LiquidBounceUiColors {
    private LiquidBounceUiColors() {}

    public static final int ACCENT = 0xFF3C69FC;
    public static final int ACCENT_SOFT = 0xFF4778F8;
    public static final int ACCENT_DEEP = 0xFF12275F;
    /** 亚克力面板:降低不透明度以透出模糊背景,并带顶边高光。 */
    public static final int PANEL = 0xC00A0B0E;
    public static final int PANEL_DEEP = 0xCC010205;
    public static final int PANEL_HIGHLIGHT = 0x26FFFFFF;
    public static final int PANEL_ALT = 0xF2050507;
    public static final int HUD_PANEL = 0xCC050507;
    public static final int TEXT_PRIMARY = 0xFFFDFDFD;
    public static final int TEXT_NORMAL = 0xFFE1E1E3;
    public static final int TEXT_MUTED = 0xFF88888F;
    public static final int TEXT_DIM = 0xFF61636A;
    public static final int SUCCESS = 0xFF42A05B;
    public static final int ERROR = 0xFFFA3729;
    public static final int TRACK = 0xFF303237;
    public static final int TOGGLE_OFF = 0xFF55575E;

    public static int blend(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return Color.argb(
                Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t),
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t));
    }
}
