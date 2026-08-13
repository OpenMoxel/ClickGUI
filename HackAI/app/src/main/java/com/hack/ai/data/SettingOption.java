/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

import java.util.Objects;

public final class SettingOption {
    private final int viewId;
    private final String value;

    public SettingOption(int viewId, String value) {
        this.viewId = viewId;
        this.value = value;
    }

    public int getViewId() {
        return viewId;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SettingOption)) return false;
        SettingOption other = (SettingOption) o;
        return viewId == other.viewId && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(viewId, value);
    }

    @Override
    public String toString() {
        return "SettingOption(viewId=" + viewId + ", value=" + value + ")";
    }
}
