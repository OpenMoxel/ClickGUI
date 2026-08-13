/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

/** 全局黑白主题管理器：仅控制 ClickGUI 面板，不影响通知/ArrayList */
public final class ThemeManager {
    private static volatile boolean isDark = false;
    public static int accentColor = 0xFF1C1B1F;

    public static boolean isDark() { return isDark; }

    public static void init(AppPreferences prefs) {
        String saved = prefs.setting("iface_theme", "light");
        isDark = "dark".equals(saved);
        updateAccent();
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

    private ThemeManager() {}
}
