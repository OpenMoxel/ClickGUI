/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 中英文切换管理器
 * - 单例，持有当前语言状态
 * - 提供所有 UI 文本的翻译字典
 */
public final class LocaleHelper {

    public static String currentLanguage = "CN";

    private LocaleHelper() {
    }

    /** 切换语言 */
    public static boolean switchTo(String lang) {
        if (lang.equals(currentLanguage)) return false;
        currentLanguage = lang;
        return true;
    }

    /** 获取翻译 */
    public static String get(String key) {
        if ("EN".equals(currentLanguage)) {
            String v = enDict.get(key);
            return v != null ? v : key;
        } else {
            String v = cnDict.get(key);
            return v != null ? v : key;
        }
    }

    /** 模块名翻译 */
    public static String moduleName(ModuleItem module) {
        return "EN".equals(currentLanguage) ? module.getNameEn() : module.getName();
    }

    /** 模块描述翻译 */
    public static String moduleDesc(ModuleItem module) {
        return "EN".equals(currentLanguage) ? module.getDescEn() : module.getDescription();
    }

    /** 分类名翻译 */
    public static String categoryLabel(Category category) {
        return "EN".equals(currentLanguage) ? category.getLabelEn() : category.getLabel();
    }

    /** 得到状态文本 */
    public static String stateText(boolean enabled) {
        return get(enabled ? "enabled" : "disabled");
    }

    // ---- 字典 ----

    private static final Map<String, String> cnDict = new LinkedHashMap<>();
    private static final Map<String, String> enDict = new LinkedHashMap<>();

    static {
        cnDict.put("enabled", "已开启");
        cnDict.put("disabled", "已关闭");
        cnDict.put("home", "Home");
        cnDict.put("search", "Search...");
        cnDict.put("show_shortcut", "显示快捷按钮");
        cnDict.put("refresh_rate", "刷新率");
        cnDict.put("settings", "设置");
        cnDict.put("style", "界面质感");
        cnDict.put("layout", "模块布局");
        cnDict.put("shortcut_style", "快捷键风格");
        cnDict.put("iface_theme", "界面主题");

        enDict.put("enabled", "Enabled");
        enDict.put("disabled", "Disabled");
        enDict.put("home", "Home");
        enDict.put("search", "Search...");
        enDict.put("show_shortcut", "Show Shortcut");
        enDict.put("refresh_rate", "Refresh Rate");
        enDict.put("settings", "Settings");
        enDict.put("style", "Style");
        enDict.put("layout", "Layout");
        enDict.put("shortcut_style", "Shortcut Style");
        enDict.put("iface_theme", "Interface Theme");
    }
}
