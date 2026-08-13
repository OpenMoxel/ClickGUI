/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.model;

public class SliderSetting extends Setting<Float> {

    private final float min;
    private final float max;
    private final float step;

    public SliderSetting(String name, String displayName, float defaultValue,
                         float min, float max, float step) {
        super(name, displayName, defaultValue);
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
        this.step = step <= 0.0f ? 0.01f : step;
        setValueSilently(defaultValue);
    }

    @Override
    public SettingType getType() { return SettingType.SLIDER; }

    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getStep() { return step; }

    public float getRatio() {
        float range = max - min;
        if (range <= 0.0f) return 0.0f;
        return (getValue() - min) / range;
    }

    public void setFromRatio(float ratio) {
        float clampedRatio = clamp(ratio, 0.0f, 1.0f);
        setValue(min + (max - min) * clampedRatio);
    }

    @Override
    protected Float sanitizeValue(Float value) {
        float safe = value == null ? min : value;
        float clamped = clamp(safe, min, max);
        float snapped = Math.round((clamped - min) / step) * step + min;
        return clamp(snapped, min, max);
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
