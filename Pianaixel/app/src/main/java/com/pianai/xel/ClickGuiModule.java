/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

/** Immutable metadata and mutable visual state for one ClickGUI module. */
final class ClickGuiModule {

    final String id;
    final String label;
    final ClickGuiIconKind iconKind;
    final boolean hasSettings;
    final boolean hasToggle;
    final boolean hasRunAction;
    boolean enabled;

    ClickGuiModule(String id, String label, ClickGuiIconKind iconKind, boolean hasSettings,
                   boolean hasToggle, boolean hasRunAction) {
        this.id = id;
        this.label = label;
        this.iconKind = iconKind;
        this.hasSettings = hasSettings;
        this.hasToggle = hasToggle;
        this.hasRunAction = hasRunAction;
    }
}
