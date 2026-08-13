/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.overlay;

import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import com.liquid.org.R;

public final class LiquidBounceFonts {
    private static Typeface regular;
    private static Typeface medium;
    private static Typeface bold;

    /** HUD Binds 面板专用：保留原 Roboto 字体。 */
    private static Typeface bindsRegular;
    private static Typeface bindsMedium;
    private static Typeface bindsBold;

    private LiquidBounceFonts() {}

    public static void initialize(Context context) {
        if (regular != null) return;
        Typeface bundled = ResourcesCompat.getFont(context, R.font.minecraft);
        if (bundled == null) bundled = Typeface.DEFAULT;
        regular = Typeface.create(bundled, Typeface.NORMAL);
        medium = Typeface.create(bundled, Typeface.BOLD);
        bold = Typeface.create(bundled, Typeface.BOLD);

        Typeface bindsBundled = ResourcesCompat.getFont(context, R.font.roboto_regular);
        if (bindsBundled == null) bindsBundled = Typeface.DEFAULT;
        bindsRegular = Typeface.create(bindsBundled, Typeface.NORMAL);
        bindsMedium = Typeface.create(bindsBundled, Typeface.BOLD);
        bindsBold = Typeface.create(bindsBundled, Typeface.BOLD);
    }

    public static Typeface regular() { return regular != null ? regular : Typeface.DEFAULT; }
    public static Typeface medium() { return medium != null ? medium : Typeface.DEFAULT_BOLD; }
    public static Typeface bold() { return bold != null ? bold : Typeface.DEFAULT_BOLD; }

    public static Typeface bindsRegular() { return bindsRegular != null ? bindsRegular : Typeface.DEFAULT; }
    public static Typeface bindsMedium() { return bindsMedium != null ? bindsMedium : Typeface.DEFAULT_BOLD; }
    public static Typeface bindsBold() { return bindsBold != null ? bindsBold : Typeface.DEFAULT_BOLD; }
}
