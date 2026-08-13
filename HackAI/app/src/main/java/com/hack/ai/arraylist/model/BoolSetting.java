/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.model;

public class BoolSetting extends Setting<Boolean> {

    public BoolSetting(String name, boolean defaultValue) {
        super(name, name, defaultValue);
    }

    public BoolSetting(String name, String displayName, boolean defaultValue) {
        super(name, displayName, defaultValue);
    }

    @Override
    public SettingType getType() {
        return SettingType.BOOL;
    }

    public void toggle() {
        setValue(!getValue());
    }
}
