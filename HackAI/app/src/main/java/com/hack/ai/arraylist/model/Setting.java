/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.model;

public abstract class Setting<T> {

    public interface Availability {
        boolean isAvailable();
    }

    private final String name;
    private final String displayName;
    private final T defaultValue;
    private final Availability availability;
    private T value;

    protected Setting(String name, String displayName, T defaultValue) {
        this(name, displayName, defaultValue, null);
    }

    protected Setting(String name, String displayName, T defaultValue, Availability availability) {
        this.name = name;
        this.displayName = displayName == null || displayName.isEmpty() ? name : displayName;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.availability = availability;
    }

    public abstract SettingType getType();

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }

    public T getValue() { return value; }

    public void setValue(T value) {
        this.value = sanitizeValue(value);
    }

    public void setValueSilently(T value) {
        this.value = sanitizeValue(value);
    }

    public T getDefaultValue() { return defaultValue; }

    public void reset() { setValue(defaultValue); }

    public boolean isAvailable() {
        return availability == null || availability.isAvailable();
    }

    protected T sanitizeValue(T value) { return value; }
}
