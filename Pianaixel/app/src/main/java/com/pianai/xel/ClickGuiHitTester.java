/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import static com.pianai.xel.ClickGuiLayout.*;

import android.graphics.RectF;

/** Converts measured ClickGUI geometry into reusable touch and accessibility hit regions. */
final class ClickGuiHitTester {

    private ClickGuiHitTester() {
    }

    static RectF searchBounds() {
        return new RectF(SEARCH_X, SEARCH_Y, SEARCH_X + SEARCH_WIDTH,
                SEARCH_Y + SEARCH_HEIGHT);
    }

    static RectF categoryBounds(int index) {
        float top = categoryTop(index);
        return new RectF(CATEGORY_X, top, CATEGORY_X + CATEGORY_WIDTH,
                top + categoryHeight(index));
    }

    static float categoryTop(int index) {
        return CATEGORY_TOPS[Math.max(0, Math.min(index, CATEGORY_TOPS.length - 1))];
    }

    static float categoryHeight(int index) {
        return index == 6 ? CONFIGURATION_CATEGORY_HEIGHT : CATEGORY_HEIGHT;
    }

    static RectF settingsBounds(ClickGuiState state) {
        return new RectF(state.settingsPanelX, state.settingsPanelY,
                state.settingsPanelX + SETTINGS_WIDTH, state.settingsPanelY + SETTINGS_HEIGHT);
    }

    static RectF settingsDragRailBounds(ClickGuiState state) {
        return new RectF(state.settingsPanelX + SETTINGS_DRAG_RAIL_X - SETTINGS_DRAG_HIT_INSET_X,
                state.settingsPanelY + SETTINGS_DRAG_RAIL_Y - SETTINGS_DRAG_HIT_INSET_Y,
                state.settingsPanelX + SETTINGS_DRAG_RAIL_X + SETTINGS_DRAG_RAIL_WIDTH
                        + SETTINGS_DRAG_HIT_INSET_X,
                state.settingsPanelY + SETTINGS_DRAG_RAIL_Y + SETTINGS_DRAG_RAIL_HEIGHT
                        + SETTINGS_DRAG_HIT_INSET_Y);
    }

    static RectF settingsRelativeBounds(ClickGuiState state, float left, float top,
                                        float right, float bottom) {
        return new RectF(state.settingsPanelX + left, state.settingsPanelY + top,
                state.settingsPanelX + right, state.settingsPanelY + bottom);
    }

    static float toSettingsLocalX(ClickGuiState state, float x) {
        return x - state.settingsPanelX;
    }

    static float toSettingsLocalY(ClickGuiState state, float y) {
        return y - state.settingsPanelY;
    }

    static RectF panelToggleBounds(ClickGuiState state) {
        return new RectF(state.panelToggleX, state.panelToggleY,
                state.panelToggleX + PANEL_TOGGLE_SIZE, state.panelToggleY + PANEL_TOGGLE_SIZE);
    }

    static RectF hotkeyBounds(ClickGuiRenderer renderer, ClickGuiState state) {
        return renderer.hotkeyBounds(ClickGuiModuleBrowser.findModule(state,
                state.activeSettingsModuleId), state.hotkeyX, state.hotkeyY);
    }

    static RectF switchBoundsFor(ClickGuiState state, ClickGuiModule item) {
        float rowTop = ClickGuiModuleBrowser.rowTop(state, item);
        return new RectF(SWITCH_X, rowTop + 10.5f,
                SWITCH_X + SWITCH_WIDTH, rowTop + 10.5f + SWITCH_HEIGHT);
    }

    static RectF gearBoundsFor(ClickGuiState state, ClickGuiModule item) {
        float rowTop = ClickGuiModuleBrowser.rowTop(state, item);
        float centerY = rowTop + LIST_ROW_HEIGHT * 0.5f;
        float centerX = item.hasToggle ? GEAR_X : GEAR_ONLY_X;
        return new RectF(centerX - 26f, centerY - 26f, centerX + 26f, centerY + 26f);
    }

    static RectF runBoundsFor(ClickGuiState state, ClickGuiModule item) {
        float rowTop = ClickGuiModuleBrowser.rowTop(state, item);
        float top = rowTop + (LIST_ROW_HEIGHT - RUN_HEIGHT) * 0.5f;
        return new RectF(RUN_X, top, RUN_X + RUN_WIDTH, top + RUN_HEIGHT);
    }
}
