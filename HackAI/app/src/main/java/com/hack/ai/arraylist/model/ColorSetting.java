/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.model;

import android.graphics.Color;

public class ColorSetting extends Setting<Integer> {

    private final boolean alphaEnabled;

    public ColorSetting(String name, String displayName, int defaultValue, boolean alphaEnabled) {
        super(name, displayName, defaultValue);
        this.alphaEnabled = alphaEnabled;
        setValueSilently(defaultValue);
    }

    @Override
    public SettingType getType() { return SettingType.COLOR; }

    public boolean isAlphaEnabled() { return alphaEnabled; }

    @Override
    protected Integer sanitizeValue(Integer value) {
        int safe = value == null ? Color.WHITE : value;
        if (alphaEnabled) return safe;
        return Color.rgb(Color.red(safe), Color.green(safe), Color.blue(safe));
    }
}
