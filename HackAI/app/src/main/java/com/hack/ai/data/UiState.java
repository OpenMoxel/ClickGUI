/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

import java.util.Objects;

public final class UiState {
    private final Category category;
    private final int buttonX;
    private final int buttonY;
    private final boolean expanded;

    public UiState(Category category, int buttonX, int buttonY, boolean expanded) {
        this.category = category;
        this.buttonX = buttonX;
        this.buttonY = buttonY;
        this.expanded = expanded;
    }

    public Category getCategory() {
        return category;
    }

    public int getButtonX() {
        return buttonX;
    }

    public int getButtonY() {
        return buttonY;
    }

    public boolean getExpanded() {
        return expanded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UiState)) return false;
        UiState other = (UiState) o;
        return category == other.category
                && buttonX == other.buttonX
                && buttonY == other.buttonY
                && expanded == other.expanded;
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, buttonX, buttonY, expanded);
    }

    @Override
    public String toString() {
        return "UiState(category=" + category + ", buttonX=" + buttonX
                + ", buttonY=" + buttonY + ", expanded=" + expanded + ")";
    }
}
