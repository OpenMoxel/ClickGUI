/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.model;

import java.util.Arrays;

/**
 * Configuration container for the Arraylist HUD — 1:1 port of Solstice Arraylist.
 * Defaults hardcoded per user spec: Bubblegum, Bar, Both, 1.0 opacity, glow on, bold on, 25px font.
 */
public final class ArrayListConfig {

    public static final int BACKGROUND_OPACITY = 0;
    public static final int BACKGROUND_SHADOW = 1;
    public static final int BACKGROUND_BOTH = 2;

    public static final int DISPLAY_OUTLINE = 0;
    public static final int DISPLAY_BAR = 1;
    public static final int DISPLAY_SPLIT = 2;
    public static final int DISPLAY_NONE = 3;

    public static final ArrayListConfig INSTANCE = new ArrayListConfig();

    // ---- Theme ----
    public final EnumSetting theme;
    public final SliderSetting colorSpeed;
    public final SliderSetting saturation;
    public final BoolSetting customColor;
    public final ColorSetting color1, color2, color3, color4, color5, color6;

    // ---- Display ----
    public final EnumSetting background;
    public final SliderSetting backgroundOpacity;
    public final SliderSetting backgroundValue;
    public final SliderSetting blurStrength;
    public final EnumSetting display;
    public final BoolSetting renderMode;
    public final BoolSetting glow;
    public final SliderSetting glowStrength;
    public final BoolSetting boldText;
    public final SliderSetting fontSize;

    private final Module module;

    // Cached custom-color palette
    private int[] customColorsCache = new int[0];
    private long customColorsSig = -1;

    private ArrayListConfig() {
        theme = new EnumSetting("theme", "主题", "Bubblegum",
                Arrays.asList("Trans", "Rainbow", "Bubblegum", "Watermelon", "Sunset", "Poison"));
        theme.setColumns(3);
        colorSpeed = new SliderSetting("colorSpeed", "颜色速度", 3.0f, 0.01f, 20.0f, 0.01f);
        saturation = new SliderSetting("saturation", "饱和度", 1.0f, 0.0f, 1.0f, 0.01f);
        customColor = new BoolSetting("customColor", "自定义颜色", false);
        color1 = new ColorSetting("color1", "颜色 1", 0xFFFF0000, true);
        color2 = new ColorSetting("color2", "颜色 2", 0xFFFF7F00, true);
        color3 = new ColorSetting("color3", "颜色 3", 0xFFFFD600, true);
        color4 = new ColorSetting("color4", "颜色 4", 0xFF00FF00, true);
        color5 = new ColorSetting("color5", "颜色 5", 0xFF0000FF, true);
        color6 = new ColorSetting("color6", "颜色 6", 0xFF8B00FF, true);

        // Default: Bar mode, Both background, 1.0 opacity
        background = new EnumSetting("background", "背景", "Both",
                Arrays.asList("Opacity", "Shadow", "Both"));
        backgroundOpacity = new SliderSetting("backgroundOpacity", "背景不透明度", 1.0f, 0.0f, 1.0f, 0.01f);
        backgroundValue = new SliderSetting("backgroundValue", "背景明度", 0.0f, 0.0f, 1.0f, 0.01f);
        blurStrength = new SliderSetting("blurStrength", "模糊强度", 0.0f, 0.0f, 10.0f, 0.1f);
        display = new EnumSetting("display", "显示样式", "Bar",
                Arrays.asList("Outline", "Bar", "Split", "None"));
        renderMode = new BoolSetting("renderMode", "显示模式文本", true);
        glow = new BoolSetting("glow", "发光", true);
        glowStrength = new SliderSetting("glowStrength", "发光强度", 1.0f, 0.5f, 1.0f, 0.1f);
        boldText = new BoolSetting("boldText", "粗体", true);
        fontSize = new SliderSetting("fontSize", "字体大小", 25.0f, 10.0f, 40.0f, 0.01f);

        module = new Module("ArrayList", "HUD显示");
        module.setEnabled(true);
        module.setVisibleInArrayList(false);

        module.addSetting(theme).addSetting(colorSpeed).addSetting(saturation)
                .addSetting(customColor)
                .addSetting(color1).addSetting(color2).addSetting(color3)
                .addSetting(color4).addSetting(color5).addSetting(color6)
                .addSetting(background).addSetting(backgroundOpacity)
                .addSetting(backgroundValue).addSetting(blurStrength)
                .addSetting(display).addSetting(renderMode)
                .addSetting(glow).addSetting(glowStrength)
                .addSetting(boldText).addSetting(fontSize);
    }

    public Module getModule() { return module; }

    public int getThemeIndex() {
        if (customColor.getValue()) return 6;
        return theme.getSelectedIndex();
    }

    public boolean isCustomColor() { return customColor.getValue(); }

    public int getDisplayIndex() { return display.getSelectedIndex(); }

    public int getBackgroundIndex() { return background.getSelectedIndex(); }

    public int[] getCustomColors() {
        long sig = 0;
        sig = sig * 31L + color1.getValue();
        sig = sig * 31L + color2.getValue();
        sig = sig * 31L + color3.getValue();
        sig = sig * 31L + color4.getValue();
        sig = sig * 31L + color5.getValue();
        sig = sig * 31L + color6.getValue();
        if (sig != customColorsSig) {
            customColorsSig = sig;
            customColorsCache = new int[]{
                    color1.getValue(), color2.getValue(), color3.getValue(),
                    color4.getValue(), color5.getValue(), color6.getValue()
            };
        }
        return customColorsCache;
    }
}
