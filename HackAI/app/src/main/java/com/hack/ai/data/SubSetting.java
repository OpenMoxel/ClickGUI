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

public final class SubSetting {

    public enum Type { SLIDER, COMBO, TOGGLE, MULTI }

    private final Type type;
    private final String key;
    private final String label;
    private final String labelEn;

    // SLIDER fields
    private final float min;
    private final float max;
    private final float step;
    private final float defaultFloat;
    private final String suffix;

    // COMBO fields
    private final List<String> options;
    private final List<String> optionsEn;
    private final String defaultOption;

    // TOGGLE fields
    private final boolean defaultToggle;

    // COLOR fields
    private final String defaultColor;

    private SubSetting(Type type, String key, String label, String labelEn,
                       float min, float max, float step, float defaultFloat, String suffix,
                       List<String> options, List<String> optionsEn, String defaultOption,
                       boolean defaultToggle, String defaultColor) {
        this.type = type;
        this.key = key;
        this.label = label;
        this.labelEn = labelEn;
        this.min = min;
        this.max = max;
        this.step = step;
        this.defaultFloat = defaultFloat;
        this.suffix = suffix;
        this.options = options != null ? Collections.unmodifiableList(options) : null;
        this.optionsEn = optionsEn != null ? Collections.unmodifiableList(optionsEn) : null;
        this.defaultOption = defaultOption;
        this.defaultToggle = defaultToggle;
        this.defaultColor = defaultColor;
    }

    // ---- Static factory methods ----

    public static SubSetting slider(String key, String label, String labelEn,
                                    float min, float max, float step,
                                    float defaultValue, String suffix) {
        return new SubSetting(Type.SLIDER, key, label, labelEn,
                min, max, step, defaultValue, suffix,
                null, null, null, false, null);
    }

    public static SubSetting combo(String key, String label, String labelEn,
                                   List<String> options, List<String> optionsEn,
                                   String defaultOption) {
        return new SubSetting(Type.COMBO, key, label, labelEn,
                0, 0, 0, 0, null,
                options, optionsEn, defaultOption, false, null);
    }

    public static SubSetting toggle(String key, String label, String labelEn,
                                    boolean defaultToggle) {
        return new SubSetting(Type.TOGGLE, key, label, labelEn,
                0, 0, 0, 0, null,
                null, null, null, defaultToggle, null);
    }

    public static SubSetting multi(String key, String label, String labelEn,
                                   List<String> options, List<String> optionsEn,
                                   List<String> defaultSelected) {
        return new SubSetting(Type.MULTI, key, label, labelEn,
                0, 0, 0, 0, null,
                options, optionsEn, String.join(",", defaultSelected), false, null);
    }

    // ---- Getters ----

    public Type getType() { return type; }
    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getLabelEn() { return labelEn; }

    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getStep() { return step; }
    public float getDefaultFloat() { return defaultFloat; }
    public String getSuffix() { return suffix; }

    public List<String> getOptions() { return options; }
    public List<String> getOptionsEn() { return optionsEn; }
    public String getDefaultOption() { return defaultOption; }

    public boolean getDefaultToggle() { return defaultToggle; }
    public String getDefaultColor() { return defaultColor; }

    // ---- Helpers ----

    /** 返回类型的默认值字符串，用于持久化 */
    public String defaultStringValue() {
        switch (type) {
            case SLIDER:
                return String.valueOf(defaultFloat);
            case COMBO:
                return defaultOption;
            case TOGGLE:
                return String.valueOf(defaultToggle);
            case MULTI:
                return defaultOption;
        }
        return "";
    }

    /** 根据当前语言返回标签 */
    public String displayLabel() {
        return "EN".equals(LocaleHelper.currentLanguage) ? labelEn : label;
    }

    /** 根据当前语言返回指定索引的选项文本 */
    public String optionLabel(int index) {
        if (optionsEn != null && index < optionsEn.size()
                && "EN".equals(LocaleHelper.currentLanguage)) {
            return optionsEn.get(index);
        }
        if (options != null && index < options.size()) {
            return options.get(index);
        }
        return "";
    }

    /** 将 SLIDER 类型转换为 SliderSetting（用于 SettingSlider 控件） */
    public SliderSetting toSliderSetting() {
        if (type != Type.SLIDER) return null;
        return new SliderSetting(min, max, step, defaultFloat, suffix);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubSetting)) return false;
        SubSetting other = (SubSetting) o;
        return type == other.type
                && key.equals(other.key)
                && label.equals(other.label)
                && labelEn.equals(other.labelEn)
                && Float.compare(min, other.min) == 0
                && Float.compare(max, other.max) == 0
                && Float.compare(step, other.step) == 0
                && Float.compare(defaultFloat, other.defaultFloat) == 0
                && Objects.equals(suffix, other.suffix)
                && Objects.equals(options, other.options)
                && Objects.equals(optionsEn, other.optionsEn)
                && Objects.equals(defaultOption, other.defaultOption)
                && defaultToggle == other.defaultToggle
                && Objects.equals(defaultColor, other.defaultColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, key, label, labelEn, min, max, step,
                defaultFloat, suffix, options, optionsEn, defaultOption,
                defaultToggle, defaultColor);
    }

    @Override
    public String toString() {
        return "SubSetting(type=" + type + ", key=" + key + ", label=" + label + ")";
    }
}
