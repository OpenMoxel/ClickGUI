/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ModuleItem {
    private final String id;
    private final String name;
    private final String description;
    private final boolean defaultEnabled;
    private final String keyBind;
    private final SliderSetting slider;
    private final String nameEn;
    private final String descEn;
    private final List<SubSetting> subSettings;

    public ModuleItem(String id, String name, String description, boolean defaultEnabled,
                      String keyBind, SliderSetting slider, String nameEn, String descEn) {
        this(id, name, description, defaultEnabled, keyBind, slider, nameEn, descEn, null);
    }

    public ModuleItem(String id, String name, String description, boolean defaultEnabled,
                      String keyBind, SliderSetting slider, String nameEn, String descEn,
                      List<SubSetting> subSettings) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultEnabled = defaultEnabled;
        this.keyBind = keyBind;
        this.slider = slider;
        this.nameEn = nameEn;
        this.descEn = descEn;
        this.subSettings = subSettings != null
                ? Collections.unmodifiableList(subSettings) : null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean getDefaultEnabled() {
        return defaultEnabled;
    }

    public String getKeyBind() {
        return keyBind;
    }

    public SliderSetting getSlider() {
        return slider;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getDescEn() {
        return descEn;
    }

    public List<SubSetting> getSubSettings() {
        return subSettings;
    }

    public String formattedValue(float value) {
        SliderSetting setting = slider;
        if (setting == null) return "";
        String rounded;
        if (setting.getStep() >= 1f) {
            rounded = String.valueOf((int) value);
        } else {
            rounded = String.format(java.util.Locale.US, "%.1f", value);
        }
        return rounded + setting.getSuffix();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleItem)) return false;
        ModuleItem other = (ModuleItem) o;
        return defaultEnabled == other.defaultEnabled
                && id.equals(other.id)
                && name.equals(other.name)
                && description.equals(other.description)
                && Objects.equals(keyBind, other.keyBind)
                && Objects.equals(slider, other.slider)
                && nameEn.equals(other.nameEn)
                && descEn.equals(other.descEn)
                && Objects.equals(subSettings, other.subSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, defaultEnabled, keyBind,
                slider, nameEn, descEn, subSettings);
    }

    @Override
    public String toString() {
        return "ModuleItem(id=" + id + ", name=" + name + ", description=" + description
                + ", defaultEnabled=" + defaultEnabled + ", keyBind=" + keyBind
                + ", slider=" + slider + ", nameEn=" + nameEn + ", descEn=" + descEn
                + ", subSettings=" + subSettings + ")";
    }
}
