/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

public enum Category {
    Combat("combat", "Combat", "⚔", "Modules"),
    Motion("motion", "Motion", "➤", "Modules"),
    Visual("visual", "Visual", "◈", "Modules"),
    Player("player", "Player", "●", "Modules"),
    World("world", "World", "◇", "Modules"),
    Misc("misc", "Misc", "⚙", "Modules");

    private final String id;
    private final String label;
    private final String icon;
    private final String sectionTitle;
    private final String labelEn;

    Category(String id, String label, String icon, String sectionTitle, String labelEn) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.sectionTitle = sectionTitle;
        this.labelEn = labelEn;
    }

    Category(String id, String label, String icon, String sectionTitle) {
        this(id, label, icon, sectionTitle, label);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public static Category fromId(String id) {
        for (Category category : values()) {
            if (category.id.equals(id)) return category;
        }
        return Combat;
    }
}
