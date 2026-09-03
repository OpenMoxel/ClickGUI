/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import static com.pianai.xel.ClickGuiLayout.*;

import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.List;
import java.util.Locale;

/** Exposes the Canvas-only ClickGUI as virtual accessibility nodes. */
final class ClickGuiAccessibilityController extends AccessibilityNodeProvider {

    private static final int CATEGORY_BASE = 1000;
    private static final int MODULE_BASE = 1100;
    private static final int HOTKEY_VISIBILITY = 1200;
    private static final int SLIDER_BASE = 1210;
    private static final int TRACE_BOXES = 1220;
    private static final int TRACE_OUTLINE = 1221;
    private static final int FLOATING_HOTKEY = 1300;
    private static final int PANEL_TOGGLE = 1400;

    private final ClickGuiView host;

    ClickGuiAccessibilityController(ClickGuiView host) {
        this.host = host;
    }

    void populateHostNode(AccessibilityNodeInfo info) {
        ClickGuiState state = host.uiState;
        info.setClassName("android.widget.ScrollView");
        info.setContentDescription("Pianaixel ClickGUI 演示。主面板当前"
                + (state.panelVisible ? "已展开。" : "已收起。"));
        if (state.panelToggleButtonVisible) {
            info.addChild(host, PANEL_TOGGLE);
        }
        if (!state.panelVisible) {
            info.setScrollable(false);
            return;
        }
        float maxScroll = host.getMaxScroll(host.getDisplayedModules());
        info.setScrollable(maxScroll > 0f);
        if (maxScroll > 0f) {
            info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }

        for (int index = 0; index < state.categories.size(); index++) {
            info.addChild(host, CATEGORY_BASE + index);
        }
        List<ClickGuiModule> displayed = host.getDisplayedModules();
        for (int index = 0; index < displayed.size(); index++) {
            float top = LIST_Y + index * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - state.listScrollY;
            if (top + LIST_ROW_HEIGHT >= LIST_VIEW_TOP && top <= LIST_VIEW_BOTTOM) {
                info.addChild(host, MODULE_BASE + index);
            }
        }
        if (state.settingsOpen) {
            info.addChild(host, HOTKEY_VISIBILITY);
            for (int index = 0; index < SLIDER_Y.length; index++) {
                info.addChild(host, SLIDER_BASE + index);
            }
            info.addChild(host, TRACE_BOXES);
            info.addChild(host, TRACE_OUTLINE);
        }
        if (state.hotkeyActivated && state.showHotkey && !state.floatingHotkeyDrawnExternally) {
            info.addChild(host, FLOATING_HOTKEY);
        }
    }

    @Override
    public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
        if (virtualViewId == View.NO_ID) {
            AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain(host);
            populateHostNode(node);
            return node;
        }
        return createVirtualNodeInfo(virtualViewId);
    }

    @Override
    public boolean performAction(int virtualViewId, int action, Bundle arguments) {
        ClickGuiState state = host.uiState;
        if (virtualViewId == View.NO_ID) {
            if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                state.listScrollY = clamp(state.listScrollY
                                + (LIST_VIEW_BOTTOM - LIST_VIEW_TOP) * 0.78f,
                        0f, host.getMaxScroll(host.getDisplayedModules()));
                host.invalidate();
                host.notifyAccessibilityContentChanged();
                return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
                state.listScrollY = clamp(state.listScrollY
                                - (LIST_VIEW_BOTTOM - LIST_VIEW_TOP) * 0.78f,
                        0f, host.getMaxScroll(host.getDisplayedModules()));
                host.invalidate();
                host.notifyAccessibilityContentChanged();
                return true;
            }
            return host.performAccessibilityAction(action, arguments);
        }

        if (state.panelToggleButtonVisible && virtualViewId == PANEL_TOGGLE
                && action == AccessibilityNodeInfo.ACTION_CLICK) {
            host.togglePanelVisibility();
            return true;
        }
        if (!state.panelVisible) {
            return false;
        }

        if (virtualViewId >= CATEGORY_BASE && virtualViewId < CATEGORY_BASE + state.categories.size()
                && action == AccessibilityNodeInfo.ACTION_CLICK) {
            state.selectedCategory = virtualViewId - CATEGORY_BASE;
            state.searchQuery = "";
            state.composingSearchText = "";
            state.listScrollY = 0f;
            state.settingsOpen = false;
            host.clearSearchFocus();
            host.invalidate();
            host.notifyAccessibilityContentChanged();
            return true;
        }

        List<ClickGuiModule> displayed = host.getDisplayedModules();
        if (virtualViewId >= MODULE_BASE && virtualViewId < MODULE_BASE + displayed.size()) {
            ClickGuiModule item = displayed.get(virtualViewId - MODULE_BASE);
            if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                if (item.hasToggle) {
                    host.toggleModule(item);
                } else if (item.hasSettings) {
                    host.openSettings(item);
                } else if (item.hasRunAction) {
                    host.runModule(item);
                }
                return true;
            }
            if (action == AccessibilityNodeInfo.ACTION_LONG_CLICK && item.hasSettings) {
                host.openSettings(item);
                return true;
            }
        }

        if (state.settingsOpen && virtualViewId == HOTKEY_VISIBILITY
                && action == AccessibilityNodeInfo.ACTION_CLICK) {
            state.showHotkey = !state.showHotkey;
            host.invalidate();
            host.notifyAccessibilityContentChanged();
            host.notifyHotkeyStateChanged();
            return true;
        }

        if (state.settingsOpen && virtualViewId >= SLIDER_BASE
                && virtualViewId < SLIDER_BASE + SLIDER_Y.length
                && action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()
                && arguments != null
                && arguments.containsKey(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE)) {
            int sliderIndex = virtualViewId - SLIDER_BASE;
            state.sliderValues[sliderIndex] = clamp(
                    arguments.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE),
                    state.sliderMinimum[sliderIndex], state.sliderMaximum[sliderIndex]);
            host.invalidate();
            host.notifyAccessibilityContentChanged();
            return true;
        }

        if (state.settingsOpen && virtualViewId == TRACE_BOXES
                && action == AccessibilityNodeInfo.ACTION_CLICK) {
            state.traceBoxes = !state.traceBoxes;
            host.invalidate();
            host.notifyAccessibilityContentChanged();
            return true;
        }
        if (state.settingsOpen && virtualViewId == TRACE_OUTLINE
                && action == AccessibilityNodeInfo.ACTION_CLICK) {
            state.traceOutline = !state.traceOutline;
            host.invalidate();
            host.notifyAccessibilityContentChanged();
            return true;
        }
        if (state.hotkeyActivated && state.showHotkey && !state.floatingHotkeyDrawnExternally
                && virtualViewId == FLOATING_HOTKEY
                && action == AccessibilityNodeInfo.ACTION_CLICK) {
            ClickGuiModule active = host.findModule(state.activeSettingsModuleId);
            if (active != null) {
                host.toggleModule(active);
                return true;
            }
        }
        return false;
    }

    private AccessibilityNodeInfo createVirtualNodeInfo(int virtualId) {
        ClickGuiState state = host.uiState;
        if (virtualId == PANEL_TOGGLE) {
            if (!state.panelToggleButtonVisible) {
                return null;
            }
            AccessibilityNodeInfo node = createNode(virtualId, host.panelToggleBounds(),
                    "主面板悬浮窗按钮，当前"
                            + (state.panelVisible ? "已展开，双击收起" : "已收起，双击展开"),
                    "android.widget.Button");
            node.setClickable(true);
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            return node;
        }
        if (!state.panelVisible) {
            return null;
        }
        if (virtualId >= CATEGORY_BASE && virtualId < CATEGORY_BASE + state.categories.size()) {
            int categoryIndex = virtualId - CATEGORY_BASE;
            ClickGuiCategory category = state.categories.get(categoryIndex);
            AccessibilityNodeInfo node = createNode(virtualId, host.categoryBounds(categoryIndex),
                    "分类：" + category.label, "android.widget.Button");
            node.setSelected(categoryIndex == state.selectedCategory && state.searchQuery.isEmpty());
            node.setClickable(true);
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            return node;
        }

        List<ClickGuiModule> displayed = host.getDisplayedModules();
        if (virtualId >= MODULE_BASE && virtualId < MODULE_BASE + displayed.size()) {
            int moduleIndex = virtualId - MODULE_BASE;
            ClickGuiModule item = displayed.get(moduleIndex);
            float top = LIST_Y + moduleIndex * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - state.listScrollY;
            AccessibilityNodeInfo node = createNode(virtualId,
                    new RectF(LIST_X, top, LIST_X + LIST_WIDTH, top + LIST_ROW_HEIGHT),
                    moduleDescription(item),
                    item.hasToggle ? "android.widget.Switch" : "android.widget.Button");
            node.setCheckable(item.hasToggle);
            node.setChecked(item.hasToggle && item.enabled);
            node.setClickable(true);
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (item.hasSettings) {
                node.setLongClickable(true);
                node.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
            }
            return node;
        }

        if (state.settingsOpen && virtualId == HOTKEY_VISIBILITY) {
            AccessibilityNodeInfo node = createNode(virtualId,
                    host.settingsRelativeBounds(SETTINGS_SWITCH_X, SETTINGS_SWITCH_Y,
                            SETTINGS_SWITCH_X + SETTINGS_SWITCH_WIDTH,
                            SETTINGS_SWITCH_Y + SETTINGS_SWITCH_HEIGHT),
                    "显示快捷键，" + (state.showHotkey ? "已开启" : "已关闭"),
                    "android.widget.Switch");
            node.setCheckable(true);
            node.setChecked(state.showHotkey);
            node.setClickable(true);
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            return node;
        }
        if (state.settingsOpen && virtualId >= SLIDER_BASE
                && virtualId < SLIDER_BASE + SLIDER_Y.length) {
            int sliderIndex = virtualId - SLIDER_BASE;
            String[] names = {"范围", "最大追踪量", "刷新间隔(秒)", "线宽"};
            AccessibilityNodeInfo node = createNode(virtualId,
                    host.settingsRelativeBounds(SETTINGS_SLIDER_LINE_LEFT - 27f,
                            SLIDER_Y[sliderIndex] - 24f, SETTINGS_SLIDER_LINE_RIGHT + 25f,
                            SLIDER_Y[sliderIndex] + 24f),
                    names[sliderIndex] + "，当前 " + formatSliderValue(state, sliderIndex),
                    "android.widget.SeekBar");
            node.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                    AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT, state.sliderMinimum[sliderIndex],
                    state.sliderMaximum[sliderIndex], state.sliderValues[sliderIndex]));
            node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
            node.setScrollable(true);
            return node;
        }
        if (state.settingsOpen && (virtualId == TRACE_BOXES || virtualId == TRACE_OUTLINE)) {
            boolean isBoxes = virtualId == TRACE_BOXES;
            boolean selected = isBoxes ? state.traceBoxes : state.traceOutline;
            AccessibilityNodeInfo node = createNode(virtualId,
                    isBoxes ? host.settingsRelativeBounds(86f, 357f, 296f, 399f)
                            : host.settingsRelativeBounds(328f, 357f, 538f, 399f),
                    (isBoxes ? "追踪箱子" : "轮廓线") + "，" + (selected ? "True" : "False"),
                    "android.widget.Switch");
            node.setCheckable(true);
            node.setChecked(selected);
            node.setClickable(true);
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            return node;
        }
        if (state.hotkeyActivated && state.showHotkey && !state.floatingHotkeyDrawnExternally
                && virtualId == FLOATING_HOTKEY) {
            ClickGuiModule active = host.findModule(state.activeSettingsModuleId);
            boolean enabled = active != null && active.enabled;
            String label = active == null ? "快捷键" : active.label;
            AccessibilityNodeInfo node = createNode(virtualId, host.hotkeyBounds(),
                    label + "悬浮快捷键，" + (enabled ? "已开启" : "已关闭"),
                    "android.widget.Switch");
            node.setCheckable(true);
            node.setChecked(enabled);
            node.setClickable(true);
            node.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            return node;
        }
        return null;
    }

    private AccessibilityNodeInfo createNode(int virtualId, RectF virtualBounds,
                                             String description, String className) {
        AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
        node.setPackageName(host.getContext().getPackageName());
        node.setSource(host, virtualId);
        node.setParent(host);
        node.setClassName(className);
        node.setContentDescription(description);
        node.setEnabled(true);
        node.setVisibleToUser(true);
        Rect bounds = host.toPhysicalBounds(virtualBounds);
        node.setBoundsInParent(bounds);
        int[] hostLocation = new int[2];
        host.getLocationOnScreen(hostLocation);
        Rect screenBounds = new Rect(bounds);
        screenBounds.offset(hostLocation[0], hostLocation[1]);
        node.setBoundsInScreen(screenBounds);
        return node;
    }

    private static String formatSliderValue(ClickGuiState state, int sliderIndex) {
        return sliderIndex < 2 ? String.valueOf(Math.round(state.sliderValues[sliderIndex]))
                : String.format(Locale.US, "%.2f", state.sliderValues[sliderIndex]);
    }

    private static String moduleDescription(ClickGuiModule item) {
        if (item.hasToggle) {
            return item.label + "，功能开关，" + (item.enabled ? "已开启" : "已关闭");
        }
        if (item.hasRunAction) {
            return item.label + "，Run 演示操作";
        }
        return item.label + "，打开功能设置";
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
