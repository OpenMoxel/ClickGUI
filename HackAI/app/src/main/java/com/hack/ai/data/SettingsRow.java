/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

import java.util.List;
import java.util.Objects;

public final class SettingsRow {
    private final String key;
    private final String labelKey;
    private final List<SettingOption> options;
    private final String defaultValue;

    public SettingsRow(String key, String labelKey, List<SettingOption> options, String defaultValue) {
        this.key = key;
        this.labelKey = labelKey;
        this.options = options;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public List<SettingOption> getOptions() {
        return options;
    }

    public String getDefault() {
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SettingsRow)) return false;
        SettingsRow other = (SettingsRow) o;
        return key.equals(other.key)
                && labelKey.equals(other.labelKey)
                && options.equals(other.options)
                && defaultValue.equals(other.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, labelKey, options, defaultValue);
    }

    @Override
    public String toString() {
        return "SettingsRow(key=" + key + ", labelKey=" + labelKey
                + ", options=" + options + ", default=" + defaultValue + ")";
    }
}
