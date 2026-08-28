/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */

/*
 * THESIS: A video-faithful control surface, not an Android template screen.
 * OWN-WORLD: transparent host surface, charcoal translucent planes, frosted white controls.
 * STORY: browse a category, toggle a demo feature, then adjust its visible controls.
 * FIRST VIEWPORT: 1920x1080 panel with search/categories left and scrolling rows right.
 * FORM: reference-pinned Canvas reproduction, code-led implementation.
 * FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
 */
package com.pianai.xel;

import static com.pianai.xel.ClickGuiLayout.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.List;

/**
 * Standalone Canvas recreation of the reference ClickGUI. Module names and controls are visual
 * demo content only; they intentionally have no game-process or game-changing behavior.
 */
public final class ClickGuiView extends View {

    /**
     * Lets the desktop-overlay host mirror the shortcut in its own small window. This keeps the
     * shortcut alive after the full-screen ClickGUI window is removed, while both controls still
     * read and mutate this view's single module-state model.
     */
    public interface HotkeyStateListener {
        void onHotkeyStateChanged();
    }

    private final ClickGuiRenderer renderer =
            new ClickGuiRenderer(new ClickGuiCanvasPainter());
    final ClickGuiState uiState = new ClickGuiState();
    private final ClickGuiSearchController searchController;
    private final ClickGuiTouchController touchController;
    private final ClickGuiAccessibilityController accessibilityProvider =
            new ClickGuiAccessibilityController(this);

    private float contentScale = 1f;
    private float contentOffsetX;
    private float contentOffsetY;
    private float bottomAnchoredScale = 1f;
    private float bottomAnchoredOffsetY;
    private HotkeyStateListener hotkeyStateListener;

    public ClickGuiView(Context context) {
        super(context);
        searchController = new ClickGuiSearchController(context, uiState, this::invalidate);
        touchController = new ClickGuiTouchController(this, context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        // This view is an overlay surface. Leave all pixels outside its Canvas controls untouched.
        setBackground(null);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        updateReferenceTransform(width, height);
    }

    private void updateReferenceTransform(int width, int height) {
        contentScale = Math.min(width / BASE_WIDTH, height / BASE_HEIGHT);
        if (contentScale <= 0f) {
            contentScale = 1f;
        }
        contentOffsetX = (width - BASE_WIDTH * contentScale) * 0.5f;
        contentOffsetY = (height - BASE_HEIGHT * contentScale) * 0.5f;

        // The information bar is intentionally not vertically letterboxed with the panel.
        // It uses width as its scale reference, then anchors the 1080px baseline to the
        // physical bottom of every landscape screen.
        bottomAnchoredScale = width / BASE_WIDTH;
        if (bottomAnchoredScale <= 0f) {
            bottomAnchoredScale = 1f;
        }
        bottomAnchoredOffsetY = height - BASE_HEIGHT * bottomAnchoredScale;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(contentOffsetX, contentOffsetY);
        canvas.scale(contentScale, contentScale);

        if (uiState.panelVisible) {
            drawClickGui(canvas);
            if (uiState.settingsOpen) {
                drawSettingsPanel(canvas);
            }
            if (uiState.hotkeyActivated && uiState.showHotkey && !uiState.floatingHotkeyDrawnExternally) {
                drawFloatingHotkey(canvas);
            }
        }
        drawWatermark(canvas);
        if (uiState.panelToggleButtonVisible) {
            drawPanelToggleButton(canvas);
        }

        canvas.restore();

        // Keep the bottom information surface and the notification lane flush with the real
        // screen edge even when the 16:9 ClickGUI reference is letterboxed vertically.
        canvas.save();
        canvas.translate(0f, bottomAnchoredOffsetY);
        canvas.scale(bottomAnchoredScale, bottomAnchoredScale);
        drawBottomInfoBar(canvas);
        drawStatusNotices(canvas);
        canvas.restore();
        if (uiState.searchFocused) {
            postInvalidateDelayed(450L);
        }
    }

    private void drawClickGui(Canvas canvas) {
        List<ClickGuiModule> modules = getDisplayedModules();
        float maxScroll = getMaxScroll(modules);
        uiState.listScrollY = clamp(uiState.listScrollY, 0f, maxScroll);
        renderer.drawMainPanel(canvas, uiState.categories, uiState.selectedCategory, uiState.searchQuery, uiState.searchFocused,
                modules, uiState.listScrollY, maxScroll);
    }

    private void drawWatermark(Canvas canvas) {
        renderer.drawWatermark(canvas);
    }

    private void drawBottomInfoBar(Canvas canvas) {
        renderer.drawBottomInfoBar(canvas);
    }

    private void drawStatusNotices(Canvas canvas) {
        if (renderer.drawStatusNotices(canvas, uiState.statusNotices)) {
            postInvalidateOnAnimation();
        }
    }

    private void drawSettingsPanel(Canvas canvas) {
        renderer.drawSettingsPanel(canvas, uiState.settingsPanelX, uiState.settingsPanelY,
                findModule(uiState.activeSettingsModuleId), uiState.showHotkey, uiState.sliderMinimum, uiState.sliderMaximum,
                uiState.sliderValues, uiState.traceBoxes, uiState.traceOutline);
    }

    private void drawFloatingHotkey(Canvas canvas) {
        renderer.drawFloatingHotkey(canvas, findModule(uiState.activeSettingsModuleId), uiState.hotkeyX, uiState.hotkeyY);
    }

    private void drawPanelToggleButton(Canvas canvas) {
        renderer.drawPanelToggleButton(canvas, uiState.panelVisible, uiState.panelToggleX, uiState.panelToggleY);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    float toContentX(float x) {
        return (x - contentOffsetX) / contentScale;
    }

    float toContentY(float y) {
        return (y - contentOffsetY) / contentScale;
    }

    float contentScale() {
        return contentScale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return touchController.onTouchEvent(event);
    }

    RectF switchBoundsFor(ClickGuiModule item, float y) {
        return ClickGuiHitTester.switchBoundsFor(uiState, item);
    }

    RectF gearBoundsFor(ClickGuiModule item, float y) {
        return ClickGuiHitTester.gearBoundsFor(uiState, item);
    }

    RectF runBoundsFor(ClickGuiModule item) {
        return ClickGuiHitTester.runBoundsFor(uiState, item);
    }

    ClickGuiModule getModuleAt(float x, float y) {
        return ClickGuiModuleBrowser.moduleAt(uiState, x, y);
    }

    void toggleModule(ClickGuiModule item) {
        if (!item.hasToggle) {
            return;
        }
        item.enabled = !item.enabled;
        uiState.statusNotices.add(new ClickGuiStatusNotice(item.enabled, SystemClock.uptimeMillis()));
        invalidate();
        notifyAccessibilityContentChanged();
        notifyHotkeyStateChanged();
    }

    void runModule(ClickGuiModule item) {
        // Run is a visual demo action in the reference; it must not mutate the feature switch.
        invalidate();
        notifyAccessibilityContentChanged();
    }

    void openSettings(ClickGuiModule item) {
        uiState.activeSettingsModuleId = item.id;
        uiState.settingsOpen = true;
        uiState.hotkeyActivated = true;
        invalidate();
        notifyAccessibilityContentChanged();
        notifyHotkeyStateChanged();
    }

    /** Returns whether the shortcut should exist independently of the full ClickGUI panel. */
    public boolean shouldShowFloatingHotkey() {
        return uiState.hotkeyActivated && uiState.showHotkey;
    }

    /** The desktop host uses the same active module label as the in-panel Canvas shortcut. */
    public String getFloatingHotkeyLabel() {
        ClickGuiModule active = findModule(uiState.activeSettingsModuleId);
        return active == null ? "快捷键" : active.label;
    }

    /** The desktop host uses the same module state to select its black or white border. */
    public boolean isFloatingHotkeyEnabled() {
        ClickGuiModule active = findModule(uiState.activeSettingsModuleId);
        return active != null && active.enabled;
    }

    /** Toggles the very same active module used by the list switch. */
    public void toggleFloatingHotkey() {
        ClickGuiModule active = findModule(uiState.activeSettingsModuleId);
        if (active != null) {
            toggleModule(active);
        }
    }

    /**
     * Desktop overlay mode draws the shortcut in an independent compact window so hiding the
     * main panel cannot remove it. Regular activity mode keeps the existing Canvas shortcut.
     */
    public void setFloatingHotkeyDrawnExternally(boolean externallyDrawn) {
        if (uiState.floatingHotkeyDrawnExternally == externallyDrawn) {
            return;
        }
        uiState.floatingHotkeyDrawnExternally = externallyDrawn;
        invalidate();
        notifyAccessibilityContentChanged();
    }

    /** Registers the desktop host for visibility, label, and border-state updates. */
    public void setHotkeyStateListener(HotkeyStateListener listener) {
        hotkeyStateListener = listener;
        notifyHotkeyStateChanged();
    }

    void notifyHotkeyStateChanged() {
        if (hotkeyStateListener != null) {
            hotkeyStateListener.onHotkeyStateChanged();
        }
    }

    /** Used by the desktop overlay host to show or hide the same Canvas state. */
    public void setPanelVisible(boolean visible) {
        if (uiState.panelVisible == visible) {
            return;
        }
        uiState.panelVisible = visible;
        if (!uiState.panelVisible) {
            clearSearchFocus();
        }
        invalidate();
        notifyAccessibilityContentChanged();
    }

    /** The desktop overlay supplies its own topmost button, avoiding a duplicate Canvas button. */
    public void setPanelToggleButtonVisible(boolean visible) {
        if (uiState.panelToggleButtonVisible == visible) {
            return;
        }
        uiState.panelToggleButtonVisible = visible;
        invalidate();
        notifyAccessibilityContentChanged();
    }

    void togglePanelVisibility() {
        setPanelVisible(!uiState.panelVisible);
    }

    List<ClickGuiModule> getDisplayedModules() {
        return ClickGuiModuleBrowser.displayedModules(uiState);
    }

    float getMaxScroll(List<ClickGuiModule> items) {
        return ClickGuiModuleBrowser.maxScroll(items);
    }

    ClickGuiModule findModule(String id) {
        return ClickGuiModuleBrowser.findModule(uiState, id);
    }

    RectF searchBounds() {
        return ClickGuiHitTester.searchBounds();
    }

    RectF categoryBounds(int index) {
        return ClickGuiHitTester.categoryBounds(index);
    }

    RectF settingsBounds() {
        return ClickGuiHitTester.settingsBounds(uiState);
    }

    RectF settingsDragRailBounds() {
        return ClickGuiHitTester.settingsDragRailBounds(uiState);
    }

    RectF settingsRelativeBounds(float left, float top, float right, float bottom) {
        return ClickGuiHitTester.settingsRelativeBounds(uiState, left, top, right, bottom);
    }

    float toSettingsLocalX(float x) {
        return ClickGuiHitTester.toSettingsLocalX(uiState, x);
    }

    float toSettingsLocalY(float y) {
        return ClickGuiHitTester.toSettingsLocalY(uiState, y);
    }

    RectF panelToggleBounds() {
        return ClickGuiHitTester.panelToggleBounds(uiState);
    }

    RectF hotkeyBounds() {
        return ClickGuiHitTester.hotkeyBounds(renderer, uiState);
    }

    boolean isInListViewport(float x, float y) {
        return ClickGuiModuleBrowser.isInListViewport(x, y);
    }

    @Override
    public void computeScroll() {
        touchController.computeScroll();
    }

    @Override
    protected void onDetachedFromWindow() {
        touchController.dispose();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return accessibilityProvider;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        accessibilityProvider.populateHostNode(info);
    }

    Rect toPhysicalBounds(RectF virtualBounds) {
        return new Rect(
                Math.round(virtualBounds.left * contentScale + contentOffsetX),
                Math.round(virtualBounds.top * contentScale + contentOffsetY),
                Math.round(virtualBounds.right * contentScale + contentOffsetX),
                Math.round(virtualBounds.bottom * contentScale + contentOffsetY)
        );
    }

    void notifyAccessibilityContentChanged() {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return uiState.searchFocused;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return searchController.createInputConnection(this, outAttrs);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (searchController.onKeyUp(keyCode)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    void focusSearch() {
        searchController.focus(this);
    }

    void clearSearchFocus() {
        searchController.clearFocus(this);
    }

    public boolean handleBack() {
        if (uiState.settingsOpen) {
            uiState.settingsOpen = false;
            invalidate();
            return true;
        }
        if (uiState.searchFocused) {
            clearSearchFocus();
            return true;
        }
        return false;
    }

    public void saveState(Bundle outState) {
        uiState.saveTo(outState);
    }

    public void restoreState(Bundle savedState) {
        uiState.restoreFrom(savedState);
        invalidate();
        notifyHotkeyStateChanged();
    }


}
