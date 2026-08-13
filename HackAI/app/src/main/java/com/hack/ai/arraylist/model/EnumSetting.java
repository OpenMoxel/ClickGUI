/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.model;

import static com.hack.ai.arraylist.model.SettingType.ENUM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnumSetting extends Setting<String> {

    private final List<String> values;
    private int columns;

    public EnumSetting(String name, String displayName, String defaultValue, List<String> values) {
        super(name, displayName, defaultValue);
        this.values = values == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
        setValueSilently(defaultValue);
    }

    public EnumSetting setColumns(int columns) {
        this.columns = Math.max(0, columns);
        return this;
    }

    public int getColumns() { return columns; }

    @Override
    public SettingType getType() { return ENUM; }

    public List<String> getValues() { return values; }

    public int getSelectedIndex() { return values.indexOf(getValue()); }

    public void selectNext() {
        if (values.isEmpty()) return;
        int currentIndex = Math.max(0, getSelectedIndex());
        setValue(values.get((currentIndex + 1) % values.size()));
    }

    @Override
    protected String sanitizeValue(String value) {
        if (values.isEmpty()) return value == null ? "" : value;
        return values.contains(value) ? value : values.get(0);
    }
}
