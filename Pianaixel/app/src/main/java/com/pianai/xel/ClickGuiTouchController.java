/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import static com.pianai.xel.ClickGuiLayout.*;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

/** Owns ClickGUI touch dispatch, free dragging, sliders, and list momentum. */
final class ClickGuiTouchController {

    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_LIST = 1;
    private static final int TOUCH_HOTKEY = 2;
    private static final int TOUCH_SETTINGS = 3;
    private static final int TOUCH_PANEL_TOGGLE = 4;
    private static final int TOUCH_SETTINGS_DRAG = 5;
    private static final int TOUCH_SETTINGS_DISMISS = 6;
    private static final int INVALID_POINTER_ID = -1;

    private final ClickGuiView host;
    private final ClickGuiState state;
    private final OverScroller listScroller;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final int touchSlop;

    private float downX;
    private float downY;
    private float lastY;
    private android.view.VelocityTracker velocityTracker;
    private final Runnable panelToggleLongPress;

    ClickGuiTouchController(ClickGuiView host, Context context) {
        this.host = host;
        state = host.uiState;
        listScroller = new OverScroller(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        panelToggleLongPress = () -> {
            if (state.touchTarget == TOUCH_PANEL_TOGGLE
                    && !state.panelToggleMovedBeforeLongPress) {
                state.panelToggleDragging = true;
                host.invalidate();
            }
        };
    }

    boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            beginTouch(event, host.toContentX(event.getX()), host.toContentY(event.getY()));
            return true;
        }

        if (state.touchTarget == TOUCH_SETTINGS_DRAG
                && state.settingsDragPointerId != INVALID_POINTER_ID) {
            return dispatchSettingsDragPointer(event, action);
        }

        float x = host.toContentX(event.getX());
        float y = host.toContentY(event.getY());
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                moveTouch(event, x, y);
                return true;
            case MotionEvent.ACTION_UP:
                boolean isClick = !(state.touchTarget == TOUCH_LIST && state.listDragging)
                        && !(state.touchTarget == TOUCH_HOTKEY && state.hotkeyDragging)
                        && state.touchTarget != TOUCH_SETTINGS_DRAG
                        && state.touchTarget != TOUCH_SETTINGS_DISMISS
                        && !(state.touchTarget == TOUCH_PANEL_TOGGLE
                        && (state.panelToggleDragging || state.panelToggleMovedBeforeLongPress));
                endTouch(event, x, y, false);
                if (isClick) {
                    host.performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                endTouch(event, x, y, true);
                return true;
            default:
                return true;
        }
    }

    void computeScroll() {
        if (listScroller.computeScrollOffset()) {
            state.listScrollY = listScroller.getCurrY();
            host.invalidate();
            host.postInvalidateOnAnimation();
        }
    }

    void dispose() {
        mainHandler.removeCallbacks(panelToggleLongPress);
        recycleVelocityTracker();
    }

    private boolean dispatchSettingsDragPointer(MotionEvent event, int action) {
        int actionIndex = event.getActionIndex();
        if (action == MotionEvent.ACTION_POINTER_UP
                && event.getPointerId(actionIndex) == state.settingsDragPointerId) {
            float x = host.toContentX(event.getX(actionIndex));
            float y = host.toContentY(event.getY(actionIndex));
            state.settingsDragLastX = x;
            state.settingsDragLastY = y;
            endTouch(event, x, y, false);
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) {
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            endTouch(event, state.settingsDragLastX, state.settingsDragLastY, true);
            return true;
        }
        int pointerIndex = event.findPointerIndex(state.settingsDragPointerId);
        if (pointerIndex < 0) {
            endTouch(event, state.settingsDragLastX, state.settingsDragLastY, true);
            return true;
        }
        float x = host.toContentX(event.getX(pointerIndex));
        float y = host.toContentY(event.getY(pointerIndex));
        state.settingsDragLastX = x;
        state.settingsDragLastY = y;
        if (action == MotionEvent.ACTION_MOVE) {
            moveTouch(event, x, y);
        } else if (action == MotionEvent.ACTION_UP) {
            endTouch(event, x, y, false);
        }
        return true;
    }

    private void beginTouch(MotionEvent event, float x, float y) {
        mainHandler.removeCallbacks(panelToggleLongPress);
        if (!listScroller.isFinished()) {
            listScroller.abortAnimation();
        }
        recycleVelocityTracker();
        velocityTracker = android.view.VelocityTracker.obtain();
        velocityTracker.addMovement(event);
        downX = x;
        downY = y;
        lastY = y;
        state.activeSlider = -1;
        state.touchTarget = TOUCH_NONE;
        state.listDragging = false;
        state.hotkeyDragging = false;
        state.panelToggleDragging = false;
        state.panelToggleMovedBeforeLongPress = false;
        state.settingsDragging = false;
        state.settingsDragPointerId = INVALID_POINTER_ID;
        state.settingsDragLastX = x;
        state.settingsDragLastY = y;

        if (state.panelVisible && state.settingsOpen) {
            if (state.hotkeyActivated && state.showHotkey && !state.floatingHotkeyDrawnExternally
                    && host.hotkeyBounds().contains(x, y)) {
                state.touchTarget = TOUCH_HOTKEY;
                return;
            }
            if (!host.settingsBounds().contains(x, y)) {
                dismissSettingsFromOutsideTap();
                return;
            }
            if (handleSettingsDown(x, y)) {
                if (state.touchTarget == TOUCH_SETTINGS_DRAG) {
                    beginSettingsPanelDrag(event, x, y);
                }
                return;
            }
        }

        if (state.panelToggleButtonVisible && host.panelToggleBounds().contains(x, y)) {
            state.touchTarget = TOUCH_PANEL_TOGGLE;
            state.panelDragOffsetX = x - state.panelToggleX;
            state.panelDragOffsetY = y - state.panelToggleY;
            mainHandler.postDelayed(panelToggleLongPress, ViewConfiguration.getLongPressTimeout());
            return;
        }
        if (!state.panelVisible) {
            return;
        }
        if (state.hotkeyActivated && state.showHotkey && !state.floatingHotkeyDrawnExternally
                && host.hotkeyBounds().contains(x, y)) {
            state.touchTarget = TOUCH_HOTKEY;
            return;
        }
        if (host.searchBounds().contains(x, y)) {
            host.focusSearch();
            return;
        }
        for (int index = 0; index < state.categories.size(); index++) {
            if (host.categoryBounds(index).contains(x, y)) {
                state.selectedCategory = index;
                state.searchQuery = "";
                state.listScrollY = 0f;
                state.settingsOpen = false;
                host.clearSearchFocus();
                host.performClick();
                host.invalidate();
                host.notifyAccessibilityContentChanged();
                return;
            }
        }
        if (host.isInListViewport(x, y)) {
            state.touchTarget = TOUCH_LIST;
            host.clearSearchFocus();
            return;
        }
        if (state.searchFocused) {
            host.clearSearchFocus();
        }
    }

    private void beginSettingsPanelDrag(MotionEvent event, float x, float y) {
        state.settingsDragPointerId = event.getPointerId(event.getActionIndex());
        state.settingsDragStartX = x;
        state.settingsDragStartY = y;
        state.settingsPanelStartX = state.settingsPanelX;
        state.settingsPanelStartY = state.settingsPanelY;
        state.settingsDragOffsetX = x - state.settingsPanelStartX;
        state.settingsDragOffsetY = y - state.settingsPanelStartY;
        state.settingsDragLastX = x;
        state.settingsDragLastY = y;
    }

    private void dismissSettingsFromOutsideTap() {
        state.settingsOpen = false;
        state.activeSlider = -1;
        state.settingsDragging = false;
        state.settingsDragPointerId = INVALID_POINTER_ID;
        state.touchTarget = TOUCH_SETTINGS_DISMISS;
        host.invalidate();
        host.notifyAccessibilityContentChanged();
    }

    private boolean handleSettingsDown(float x, float y) {
        if (!host.settingsBounds().contains(x, y)) {
            return false;
        }
        if (host.settingsDragRailBounds().contains(x, y)) {
            state.touchTarget = TOUCH_SETTINGS_DRAG;
            return true;
        }
        state.touchTarget = TOUCH_SETTINGS;
        float localX = host.toSettingsLocalX(x);
        float localY = host.toSettingsLocalY(y);
        if (new RectF(SETTINGS_SWITCH_X, SETTINGS_SWITCH_Y,
                SETTINGS_SWITCH_X + SETTINGS_SWITCH_WIDTH,
                SETTINGS_SWITCH_Y + SETTINGS_SWITCH_HEIGHT).contains(localX, localY)) {
            state.showHotkey = !state.showHotkey;
            host.performClick();
            host.invalidate();
            host.notifyAccessibilityContentChanged();
            host.notifyHotkeyStateChanged();
            return true;
        }
        for (int index = 0; index < SLIDER_Y.length; index++) {
            if (Math.abs(localY - SLIDER_Y[index]) <= 24f
                    && localX >= SETTINGS_SLIDER_LINE_LEFT - 27f
                    && localX <= SETTINGS_SLIDER_LINE_RIGHT + 25f) {
                state.activeSlider = index;
                applySliderTouch(index, localX);
                return true;
            }
        }
        if (new RectF(86f, 357f, 296f, 399f).contains(localX, localY)) {
            state.traceBoxes = localX < 191f;
            host.performClick();
            host.invalidate();
            return true;
        }
        if (new RectF(328f, 357f, 538f, 399f).contains(localX, localY)) {
            state.traceOutline = localX < 433f;
            host.performClick();
            host.invalidate();
            return true;
        }
        return true;
    }

    private void moveTouch(MotionEvent event, float x, float y) {
        if (velocityTracker != null) {
            velocityTracker.addMovement(event);
        }
        if (state.touchTarget == TOUCH_SETTINGS && state.activeSlider >= 0) {
            applySliderTouch(state.activeSlider, host.toSettingsLocalX(x));
            return;
        }
        if (state.touchTarget == TOUCH_SETTINGS_DRAG) {
            if (!state.settingsDragging
                    && (Math.abs(x - downX) > touchSlop / host.contentScale()
                    || Math.abs(y - downY) > touchSlop / host.contentScale())) {
                state.settingsDragging = true;
            }
            if (state.settingsDragging) {
                state.settingsPanelX = state.settingsPanelStartX + (x - state.settingsDragStartX);
                state.settingsPanelY = state.settingsPanelStartY + (y - state.settingsDragStartY);
                host.invalidate();
            }
            return;
        }
        if (state.touchTarget == TOUCH_PANEL_TOGGLE) {
            if (!state.panelToggleDragging
                    && (Math.abs(x - downX) > touchSlop / host.contentScale()
                    || Math.abs(y - downY) > touchSlop / host.contentScale())) {
                state.panelToggleMovedBeforeLongPress = true;
                mainHandler.removeCallbacks(panelToggleLongPress);
            }
            if (state.panelToggleDragging) {
                state.panelToggleX = clamp(x - state.panelDragOffsetX, 0f,
                        BASE_WIDTH - PANEL_TOGGLE_SIZE);
                state.panelToggleY = clamp(y - state.panelDragOffsetY, 0f,
                        BASE_HEIGHT - PANEL_TOGGLE_SIZE);
                host.invalidate();
            }
            return;
        }
        if (state.touchTarget == TOUCH_LIST) {
            float delta = y - lastY;
            if (Math.abs(y - downY) > touchSlop / host.contentScale()) {
                state.listDragging = true;
            }
            if (state.listDragging) {
                state.listScrollY = clamp(state.listScrollY - delta, 0f,
                        host.getMaxScroll(host.getDisplayedModules()));
                host.invalidate();
            }
            lastY = y;
            return;
        }
        if (state.touchTarget == TOUCH_HOTKEY) {
            if (Math.abs(x - downX) > touchSlop / host.contentScale()
                    || Math.abs(y - downY) > touchSlop / host.contentScale()) {
                state.hotkeyDragging = true;
            }
            if (state.hotkeyDragging) {
                float width = host.hotkeyBounds().width();
                state.hotkeyX = clamp(x - width * 0.5f, 0f, BASE_WIDTH - width);
                state.hotkeyY = clamp(y - 24f, 0f, BASE_HEIGHT - 48f);
                host.invalidate();
            }
        }
    }

    private void endTouch(MotionEvent event, float x, float y, boolean cancelled) {
        mainHandler.removeCallbacks(panelToggleLongPress);
        if (velocityTracker != null) {
            velocityTracker.addMovement(event);
        }
        if (!cancelled && state.touchTarget == TOUCH_PANEL_TOGGLE) {
            if (!state.panelToggleDragging && !state.panelToggleMovedBeforeLongPress) {
                host.togglePanelVisibility();
            }
        } else if (!cancelled && state.touchTarget == TOUCH_LIST) {
            if (state.listDragging) {
                if (velocityTracker != null) {
                    velocityTracker.computeCurrentVelocity(1000);
                    float velocityY = velocityTracker.getYVelocity();
                    if (Math.abs(velocityY) > 420f) {
                        listScroller.fling(0, Math.round(state.listScrollY), 0,
                                Math.round(-velocityY), 0, 0, 0,
                                Math.round(host.getMaxScroll(host.getDisplayedModules())));
                        host.postInvalidateOnAnimation();
                    }
                }
            } else {
                ClickGuiModule tapped = host.getModuleAt(x, y);
                if (tapped != null) {
                    if (tapped.hasRunAction && host.runBoundsFor(tapped).contains(x, y)) {
                        host.runModule(tapped);
                    } else if (tapped.hasToggle && host.switchBoundsFor(tapped, y).contains(x, y)) {
                        host.toggleModule(tapped);
                    } else if (tapped.hasSettings && host.gearBoundsFor(tapped, y).contains(x, y)) {
                        host.openSettings(tapped);
                    } else if (tapped.hasToggle) {
                        host.toggleModule(tapped);
                    } else if (tapped.hasSettings) {
                        host.openSettings(tapped);
                    } else if (tapped.hasRunAction) {
                        host.runModule(tapped);
                    }
                    host.performClick();
                }
            }
        } else if (!cancelled && state.touchTarget == TOUCH_HOTKEY && !state.hotkeyDragging) {
            ClickGuiModule active = host.findModule(state.activeSettingsModuleId);
            if (active != null) {
                host.toggleModule(active);
                host.performClick();
            }
        }
        if (state.touchTarget == TOUCH_SETTINGS_DRAG && state.settingsDragging) {
            host.notifyAccessibilityContentChanged();
        }
        state.activeSlider = -1;
        state.settingsDragging = false;
        state.settingsDragPointerId = INVALID_POINTER_ID;
        state.touchTarget = TOUCH_NONE;
        recycleVelocityTracker();
        host.invalidate();
    }

    private void applySliderTouch(int index, float x) {
        float fraction = clamp((x - SETTINGS_SLIDER_LINE_LEFT)
                / (SETTINGS_SLIDER_LINE_RIGHT - SETTINGS_SLIDER_LINE_LEFT), 0f, 1f);
        state.sliderValues[index] = state.sliderMinimum[index]
                + (state.sliderMaximum[index] - state.sliderMinimum[index]) * fraction;
        host.invalidate();
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
