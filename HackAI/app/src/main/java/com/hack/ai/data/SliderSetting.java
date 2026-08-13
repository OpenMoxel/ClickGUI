/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

import java.util.Objects;

public final class SliderSetting {
    private final float min;
    private final float max;
    private final float step;
    private final float defaultValue;
    private final String suffix;

    public SliderSetting(float min, float max, float step, float defaultValue, String suffix) {
        this.min = min;
        this.max = max;
        this.step = step;
        this.defaultValue = defaultValue;
        this.suffix = suffix;
    }

    public SliderSetting(float min, float max, float step, float defaultValue) {
        this(min, max, step, defaultValue, "");
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getStep() {
        return step;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public String getSuffix() {
        return suffix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SliderSetting)) return false;
        SliderSetting other = (SliderSetting) o;
        return Float.compare(min, other.min) == 0
                && Float.compare(max, other.max) == 0
                && Float.compare(step, other.step) == 0
                && Float.compare(defaultValue, other.defaultValue) == 0
                && suffix.equals(other.suffix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max, step, defaultValue, suffix);
    }

    @Override
    public String toString() {
        return "SliderSetting(min=" + min + ", max=" + max + ", step=" + step
                + ", defaultValue=" + defaultValue + ", suffix=" + suffix + ")";
    }
}
