/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified Module — only the parts needed by ArrayList HUD.
 */
public class Module {

    public interface EnabledStateListener {
        void onEnabledStateChanged(Module module, boolean enabled);
    }

    private final String name;
    // 适配：displayName 可变，支持界面语言切换时刷新 HUD 显示名
    private String displayName;
    private boolean enabled;
    private boolean visibleInArrayList = true;
    private float arrayListAnim;
    private String displayText = "";
    private final List<EnabledStateListener> enabledStateListeners = new ArrayList<>();
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String displayName) {
        this.name = name;
        this.displayName = displayName != null && !displayName.isEmpty() ? displayName : name;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }

    public void setDisplayName(String displayName) {
        this.displayName = displayName != null && !displayName.isEmpty() ? displayName : name;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            for (EnabledStateListener l : new ArrayList<>(enabledStateListeners)) {
                l.onEnabledStateChanged(this, enabled);
            }
        }
    }

    public void toggle() { setEnabled(!enabled); }

    public boolean isVisibleInArrayList() { return visibleInArrayList; }

    public void setVisibleInArrayList(boolean visible) { this.visibleInArrayList = visible; }

    public float getArrayListAnim() { return arrayListAnim; }

    public void setArrayListAnim(float anim) { this.arrayListAnim = anim; }

    public void addEnabledStateListener(EnabledStateListener listener) {
        if (listener != null && !enabledStateListeners.contains(listener)) {
            enabledStateListeners.add(listener);
        }
    }

    public void removeEnabledStateListener(EnabledStateListener listener) {
        enabledStateListeners.remove(listener);
    }

    public Module addSetting(Setting<?> setting) {
        if (setting != null) settings.add(setting);
        return this;
    }

    public List<Setting<?>> getSettings() { return settings; }

    public String getSettingDisplayText() { return displayText; }

    public void setDisplayText(String text) { this.displayText = text != null ? text : ""; }
}
