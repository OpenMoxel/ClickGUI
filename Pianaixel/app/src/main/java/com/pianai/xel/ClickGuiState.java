/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import static com.pianai.xel.ClickGuiLayout.SETTINGS_X;
import static com.pianai.xel.ClickGuiLayout.SETTINGS_Y;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/** Owns mutable ClickGUI data independently from Android View lifecycle plumbing. */
final class ClickGuiState {

    final List<ClickGuiCategory> categories = new ArrayList<>(ClickGuiModuleCatalog.create());
    final List<ClickGuiStatusNotice> statusNotices = new ArrayList<>();
    final float[] sliderMinimum = {0f, 1f, 0.10f, 0.25f};
    final float[] sliderMaximum = {50f, 50f, 1.50f, 2.00f};
    final float[] sliderValues = {25f, 25f, 0.50f, 1.00f};

    float listScrollY;
    float hotkeyX = 814f;
    float hotkeyY = 539f;
    float settingsPanelX = SETTINGS_X;
    float settingsPanelY = SETTINGS_Y;
    float settingsDragOffsetX;
    float settingsDragOffsetY;
    float settingsDragStartX;
    float settingsDragStartY;
    float settingsPanelStartX;
    float settingsPanelStartY;
    float settingsDragLastX;
    float settingsDragLastY;
    int settingsDragPointerId = -1;
    float panelToggleX = 1782f;
    float panelToggleY = 580f;
    float panelDragOffsetX;
    float panelDragOffsetY;
    int selectedCategory;
    int activeSlider = -1;
    int touchTarget;
    boolean listDragging;
    boolean hotkeyDragging;
    boolean panelToggleDragging;
    boolean panelToggleMovedBeforeLongPress;
    boolean settingsDragging;
    boolean panelVisible = true;
    boolean panelToggleButtonVisible = true;
    boolean settingsOpen;
    boolean showHotkey = true;
    boolean hotkeyActivated;
    boolean floatingHotkeyDrawnExternally;
    boolean traceBoxes;
    boolean traceOutline;
    boolean searchFocused;
    String activeSettingsModuleId = "block_tracer";
    String searchQuery = "";
    String composingSearchText = "";
    void saveTo(Bundle outState) {
        outState.putInt("click_gui_category", selectedCategory);
        outState.putBoolean("click_gui_settings_open", settingsOpen);
        outState.putString("click_gui_settings_module", activeSettingsModuleId);
        outState.putFloat("click_gui_settings_x", settingsPanelX);
        outState.putFloat("click_gui_settings_y", settingsPanelY);
        outState.putBoolean("click_gui_hotkey_visible", showHotkey);
        outState.putBoolean("click_gui_hotkey_activated", hotkeyActivated);
        outState.putString("click_gui_search", searchQuery);
        outState.putFloat("click_gui_hotkey_x", hotkeyX);
        outState.putFloat("click_gui_hotkey_y", hotkeyY);
        outState.putBoolean("click_gui_trace_boxes", traceBoxes);
        outState.putBoolean("click_gui_trace_outline", traceOutline);
        outState.putFloat("click_gui_list_scroll", listScrollY);
        outState.putBoolean("click_gui_panel_visible", panelVisible);
        outState.putFloat("click_gui_panel_toggle_x", panelToggleX);
        outState.putFloat("click_gui_panel_toggle_y", panelToggleY);
        for (int index = 0; index < sliderValues.length; index++) {
            outState.putFloat("click_gui_slider_" + index, sliderValues[index]);
        }
        for (ClickGuiCategory category : categories) {
            for (ClickGuiModule item : category.modules) {
                outState.putBoolean("click_gui_enabled_" + item.id, item.enabled);
            }
        }
    }

    void restoreFrom(Bundle savedState) {
        selectedCategory = clamp(savedState.getInt("click_gui_category", selectedCategory),
                0, categories.size() - 1);
        settingsOpen = savedState.getBoolean("click_gui_settings_open", false);
        activeSettingsModuleId = savedState.getString("click_gui_settings_module",
                activeSettingsModuleId);
        settingsPanelX = savedState.getFloat("click_gui_settings_x", settingsPanelX);
        settingsPanelY = savedState.getFloat("click_gui_settings_y", settingsPanelY);
        showHotkey = savedState.getBoolean("click_gui_hotkey_visible", true);
        hotkeyActivated = savedState.getBoolean("click_gui_hotkey_activated", settingsOpen);
        searchQuery = savedState.getString("click_gui_search", "");
        hotkeyX = savedState.getFloat("click_gui_hotkey_x", hotkeyX);
        hotkeyY = savedState.getFloat("click_gui_hotkey_y", hotkeyY);
        traceBoxes = savedState.getBoolean("click_gui_trace_boxes", false);
        traceOutline = savedState.getBoolean("click_gui_trace_outline", false);
        listScrollY = savedState.getFloat("click_gui_list_scroll", 0f);
        panelVisible = savedState.getBoolean("click_gui_panel_visible", true);
        panelToggleX = clamp(savedState.getFloat("click_gui_panel_toggle_x", panelToggleX),
                0f, ClickGuiLayout.BASE_WIDTH - ClickGuiLayout.PANEL_TOGGLE_SIZE);
        panelToggleY = clamp(savedState.getFloat("click_gui_panel_toggle_y", panelToggleY),
                0f, ClickGuiLayout.BASE_HEIGHT - ClickGuiLayout.PANEL_TOGGLE_SIZE);
        for (int index = 0; index < sliderValues.length; index++) {
            sliderValues[index] = savedState.getFloat("click_gui_slider_" + index,
                    sliderValues[index]);
        }
        for (ClickGuiCategory category : categories) {
            for (ClickGuiModule item : category.modules) {
                String stateKey = "click_gui_enabled_" + item.id;
                if (savedState.containsKey(stateKey)) {
                    item.enabled = savedState.getBoolean(stateKey);
                }
            }
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

}
