package com.ideal.base.overlay;

import android.animation.ValueAnimator;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import com.ideal.base.module.ModuleController;
import com.ideal.base.module.ModuleRepository;
import com.ideal.base.state.OverlayStateStore;
import com.ideal.base.ui.ClickGuiView;
import com.ideal.base.ui.FloatingEntryView;
import com.ideal.base.ui.HudOverlayView;
import com.ideal.base.ui.ShortcutView;

import java.util.LinkedHashMap;
import java.util.Map;

/** Owns every WindowManager view and removes all of them during service shutdown. */
public final class OverlayWindowController {

    private static final float CANVAS_WIDTH = 2532f;
    private static final float CANVAS_HEIGHT = 1170f;
    private static final int PANEL_LEFT = 576;
    private static final int PANEL_TOP = 181;

    private final Context context;
    private final WindowManager windowManager;
    private final DisplayManager displayManager;
    private final OverlayStateStore stateStore;
    private final ModuleRepository repository;
    private final ModuleController controller;
    private final Map<String, ShortcutRecord> shortcuts = new LinkedHashMap<>();
    private final Runnable stateListener = this::onModuleStateChanged;
    private final ModuleController.EnabledStateListener enabledStateListener =
            this::onModuleEnabledStateChanged;
    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            // The overlay always belongs to the default display.
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            // The service cleanup path owns removal of all overlay windows.
        }

        @Override
        public void onDisplayChanged(int displayId) {
            requestRelayout();
        }
    };

    private HudOverlayView hudView;
    private WindowManager.LayoutParams hudParams;
    private FloatingEntryView entryView;
    private WindowManager.LayoutParams entryParams;
    private ClickGuiView panelView;
    private WindowManager.LayoutParams panelParams;
    private boolean hudAttached;
    private boolean entryAttached;
    private boolean panelAttached;
    private boolean relayoutPosted;
    private ValueAnimator panelAnimator;

    public OverlayWindowController(Context context) {
        this.context = context.getApplicationContext();
        windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        displayManager = (DisplayManager) this.context.getSystemService(Context.DISPLAY_SERVICE);
        stateStore = new OverlayStateStore(this.context);
        repository = new ModuleRepository();
        controller = new ModuleController(repository, stateStore);
        controller.addListener(stateListener);
        controller.addEnabledStateListener(enabledStateListener);
        displayManager.registerDisplayListener(displayListener, new Handler(Looper.getMainLooper()));
    }

    public void show() {
        if (!hudAttached) {
            addHud();
        }
        if (!entryAttached) {
            addEntry();
        }
        syncShortcuts();
        requestRelayout();
    }

    public void onConfigurationChanged() {
        if (entryAttached || hudAttached) {
            requestRelayout();
        }
    }

    public void destroy() {
        controller.removeListener(stateListener);
        controller.removeEnabledStateListener(enabledStateListener);
        displayManager.unregisterDisplayListener(displayListener);
        if (panelAnimator != null) {
            panelAnimator.cancel();
            panelAnimator = null;
        }
        if (panelAttached) {
            safelyRemove(panelView);
            panelAttached = false;
        }
        for (ShortcutRecord shortcut : shortcuts.values()) {
            safelyRemove(shortcut.view);
        }
        shortcuts.clear();
        if (hudAttached) {
            hudView.release();
            safelyRemove(hudView);
            hudAttached = false;
        }
        if (entryAttached) {
            safelyRemove(entryView);
            entryAttached = false;
        }
    }

    private void addHud() {
        hudView = new HudOverlayView(context, repository, controller, stateStore);
        hudView.setOnApplyWindowInsetsListener((view, insets) -> {
            requestRelayout();
            return insets;
        });
        hudParams = newHudOverlayParams();
        windowManager.addView(hudView, hudParams);
        hudAttached = true;
        updateHudBounds();
    }

    private void addEntry() {
        entryView = new FloatingEntryView(context, new FloatingEntryView.Callback() {
            @Override
            public void onClick() {
                if (panelAttached) {
                    closePanel();
                } else {
                    openPanel();
                }
            }

            @Override
            public void onDragTo(int x, int y) {
                moveEntry(x, y, false);
            }

            @Override
            public void onDragReleased(int x, int y) {
                moveEntry(x, y, true);
            }
        });
        entryView.setOnApplyWindowInsetsListener((view, insets) -> {
            requestRelayout();
            return insets;
        });
        float scale = referenceScale();
        int size = Math.max(1, Math.round(82f * scale));
        entryParams = newOverlayParams(size, size);
        Point start = restoredPosition("entry", size, size, true);
        placeEntry(start);
        if (!stateStore.hasPosition("entry")) {
            persistPosition("entry", start.x, start.y, size, size);
        }
        windowManager.addView(entryView, entryParams);
        entryAttached = true;
    }

    private void openPanel() {
        if (!entryAttached) {
            addEntry();
        }
        if (panelAnimator != null) {
            panelAnimator.cancel();
        }
        if (!panelAttached) {
            panelView = new ClickGuiView(context, repository, controller, stateStore,
                    new ClickGuiView.Callback() {
                @Override
                public void onToggleEnabled(String moduleId) {
                    controller.toggleEnabled(moduleId);
                }

                @Override
                public void onToggleShortcut(String moduleId) {
                    controller.toggleShortcut(moduleId);
                }

                @Override
                public void onSetRange(String moduleId, float value) {
                    controller.setRange(moduleId, value);
                }

                @Override
                public void onSetFov(String moduleId, float value) {
                    controller.setFov(moduleId, value);
                }

                @Override
                public void onSetDelay(String moduleId, float value) {
                    controller.setDelay(moduleId, value);
                }

                @Override
                public void onSetScaffoldMode(String moduleId, String mode) {
                    controller.setScaffoldMode(moduleId, mode);
                }

                @Override
                public void onSetHudHidden(String moduleId, boolean hidden) {
                    String key = HudOverlayView.MODULE_LIST.equals(moduleId)
                            ? HudOverlayView.SETTING_LIST_HIDDEN
                            : HudOverlayView.SETTING_NOTIFICATIONS_HIDDEN;
                    stateStore.putHudBoolean(key, hidden);
                    refreshHudAndPanel();
                }

                @Override
                public void onSetHudFloat(String setting, float value) {
                    stateStore.putHudFloat(setting, value);
                    refreshHudAndPanel();
                }

                @Override
                public void onCollapse() {
                    closePanel();
                }
            });
            panelParams = newOverlayParams(panelWidth(), panelHeight());
            placePanel();
            panelView.setAlpha(0f);
            panelView.setScaleX(0.96f);
            panelView.setScaleY(0.96f);
            panelView.setRevealAlpha(0f);
            windowManager.addView(panelView, panelParams);
            panelAttached = true;
        }
        panelView.setVisibility(View.VISIBLE);
        ValueAnimator open = ValueAnimator.ofFloat(0f, 1f);
        panelAnimator = open;
        open.setDuration(200L);
        open.setInterpolator(new DecelerateInterpolator());
        open.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            panelView.setAlpha(value);
            panelView.setScaleX(0.96f + 0.04f * value);
            panelView.setScaleY(0.96f + 0.04f * value);
        });
        open.start();
        panelView.postDelayed(() -> {
            if (panelAttached) {
                ValueAnimator content = ValueAnimator.ofFloat(0f, 1f);
                content.setDuration(140L);
                content.setInterpolator(new DecelerateInterpolator());
                content.addUpdateListener(animation -> panelView.setRevealAlpha(
                        (float) animation.getAnimatedValue()));
                content.start();
            }
        }, 60L);
    }

    private void closePanel() {
        if (!panelAttached) {
            return;
        }
        if (panelAnimator != null) {
            panelAnimator.cancel();
        }
        ValueAnimator close = ValueAnimator.ofFloat(1f, 0f);
        panelAnimator = close;
        close.setDuration(160L);
        close.setInterpolator(new DecelerateInterpolator());
        close.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            panelView.setAlpha(value);
            panelView.setScaleX(0.96f + 0.04f * value);
            panelView.setScaleY(0.96f + 0.04f * value);
        });
        close.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (panelAttached && animation == panelAnimator) {
                    safelyRemove(panelView);
                    panelAttached = false;
                    panelAnimator = null;
                }
            }
        });
        close.start();
    }

    private void onModuleStateChanged() {
        if (panelAttached) {
            panelView.invalidate();
        }
        if (hudAttached) {
            hudView.refresh();
        }
        syncShortcuts();
    }

    private void onModuleEnabledStateChanged(String moduleId, boolean enabled) {
        if (hudAttached) {
            hudView.onModuleEnabledStateChanged(moduleId, enabled);
        }
    }

    private void refreshHudAndPanel() {
        if (panelAttached) {
            panelView.invalidate();
        }
        if (hudAttached) {
            hudView.refresh();
        }
    }

    private void syncShortcuts() {
        for (ModuleRepository.ModuleDefinition module : repository.getAllModules()) {
            ModuleController.ModuleState state = controller.stateOf(module.id);
            boolean required = state != null && state.shortcutVisible;
            ShortcutRecord existing = shortcuts.get(module.id);
            if (required && existing == null) {
                addShortcut(module, shortcuts.size());
            } else if (!required && existing != null) {
                safelyRemove(existing.view);
                shortcuts.remove(module.id);
            }
        }
    }

    private void addShortcut(ModuleRepository.ModuleDefinition module, int order) {
        ShortcutView view = new ShortcutView(context, module.id, module.name, new ShortcutView.Callback() {
            @Override
            public void onClick(String moduleId) {
                controller.toggleEnabled(moduleId);
            }

            @Override
            public void onDragTo(String moduleId, int x, int y) {
                moveShortcut(moduleId, x, y, false);
            }

            @Override
            public void onDragReleased(String moduleId, int x, int y) {
                moveShortcut(moduleId, x, y, true);
            }
        });
        float scale = referenceScale();
        int width = ShortcutView.estimateWidth(module.name, scale);
        int height = ShortcutView.estimateHeight(scale);
        WindowManager.LayoutParams params = newOverlayParams(width, height);
        ShortcutRecord record = new ShortcutRecord(module, view, params);
        shortcuts.put(module.id, record);
        Rect bounds = availableBounds();
        if (stateStore.hasPosition("shortcut." + module.id)) {
            placeShortcut(record, restoredPosition("shortcut." + module.id, width, height, false));
        } else {
            int x = bounds.left + Math.round(50f * scale);
            int y = bounds.top + Math.round((250f + order * 106f) * scale);
            placeShortcut(record, clampToBounds(x, y, width, height));
            persistPosition("shortcut." + module.id, record.params.x, record.params.y, width, height);
        }
        windowManager.addView(view, params);
    }

    private void moveEntry(int x, int y, boolean persist) {
        if (!entryAttached) {
            return;
        }
        Point point = clampToBounds(x, y, entryParams.width, entryParams.height);
        placeEntry(point);
        windowManager.updateViewLayout(entryView, entryParams);
        if (persist) {
            persistPosition("entry", point.x, point.y, entryParams.width, entryParams.height);
        }
    }

    private void moveShortcut(String moduleId, int x, int y, boolean persist) {
        ShortcutRecord record = shortcuts.get(moduleId);
        if (record == null) {
            return;
        }
        Point point = clampToBounds(x, y, record.params.width, record.params.height);
        placeShortcut(record, point);
        windowManager.updateViewLayout(record.view, record.params);
        if (persist) {
            persistPosition("shortcut." + moduleId, point.x, point.y,
                    record.params.width, record.params.height);
        }
    }

    private void relayoutForCurrentDisplay() {
        if (!entryAttached) {
            return;
        }
        float scale = referenceScale();
        int entrySize = Math.max(1, Math.round(82f * scale));
        entryParams.width = entrySize;
        entryParams.height = entrySize;
        placeEntry(restoredPosition("entry", entrySize, entrySize, true));
        windowManager.updateViewLayout(entryView, entryParams);

        if (hudAttached) {
            updateHudBounds();
            windowManager.updateViewLayout(hudView, hudParams);
        }
        if (panelAttached) {
            panelParams.width = panelWidth();
            panelParams.height = panelHeight();
            placePanel();
            windowManager.updateViewLayout(panelView, panelParams);
        }
        for (ShortcutRecord record : shortcuts.values()) {
            int width = ShortcutView.estimateWidth(record.module.name, scale);
            int height = ShortcutView.estimateHeight(scale);
            record.params.width = width;
            record.params.height = height;
            placeShortcut(record, restoredPosition("shortcut." + record.module.id, width, height, false));
            windowManager.updateViewLayout(record.view, record.params);
        }
    }

    private void requestRelayout() {
        if ((!entryAttached && !hudAttached) || relayoutPosted) {
            return;
        }
        relayoutPosted = true;
        View postSource = entryAttached ? entryView : hudView;
        postSource.post(() -> {
            relayoutPosted = false;
            relayoutForCurrentDisplay();
        });
    }

    private void updateHudBounds() {
        if (hudAttached) {
            hudView.setSafeBounds(availableBounds(), referenceScale());
        }
    }

    private void placePanel() {
        Rect bounds = availableBounds();
        float scale = referenceScale();
        float canvasWidth = CANVAS_WIDTH * scale;
        float canvasHeight = CANVAS_HEIGHT * scale;
        panelParams.x = bounds.left + Math.round((bounds.width() - canvasWidth) / 2f + PANEL_LEFT * scale);
        panelParams.y = bounds.top + Math.round((bounds.height() - canvasHeight) / 2f + PANEL_TOP * scale);
    }

    private void placeEntry(Point point) {
        entryParams.x = point.x;
        entryParams.y = point.y;
        entryView.setWindowPosition(point.x, point.y);
    }

    private void placeShortcut(ShortcutRecord record, Point point) {
        record.params.x = point.x;
        record.params.y = point.y;
        record.view.setWindowPosition(point.x, point.y);
    }

    private Point restoredPosition(String key, int width, int height, boolean entry) {
        Rect bounds = availableBounds();
        if (!stateStore.hasPosition(key)) {
            if (entry) {
                return clampToBounds(bounds.centerX() - width / 2, bounds.centerY() - height / 2, width, height);
            }
            return clampToBounds(bounds.left, bounds.top, width, height);
        }
        OverlayStateStore.Position position = stateStore.getPosition(key);
        int xRange = Math.max(0, bounds.width() - width);
        int yRange = Math.max(0, bounds.height() - height);
        return clampToBounds(bounds.left + Math.round(position.x * xRange),
                bounds.top + Math.round(position.y * yRange), width, height);
    }

    private void persistPosition(String key, int x, int y, int width, int height) {
        Rect bounds = availableBounds();
        float xRange = Math.max(1, bounds.width() - width);
        float yRange = Math.max(1, bounds.height() - height);
        stateStore.putPosition(key, (x - bounds.left) / xRange, (y - bounds.top) / yRange);
    }

    private Point clampToBounds(int x, int y, int width, int height) {
        Rect bounds = availableBounds();
        int maxX = Math.max(bounds.left, bounds.right - width);
        int maxY = Math.max(bounds.top, bounds.bottom - height);
        return new Point(Math.max(bounds.left, Math.min(maxX, x)),
                Math.max(bounds.top, Math.min(maxY, y)));
    }

    private WindowManager.LayoutParams newHudOverlayParams() {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        // Android's overlay touch-security policy considers the Window alpha, not per-pixel
        // Canvas transparency. Stay below its 0.80 obscuring threshold so this full-screen,
        // FLAG_NOT_TOUCHABLE HUD cannot block touches destined for the app underneath.
        params.alpha = .79f;
        params.setTitle("IdealClient HUD");
        return params;
    }

    private WindowManager.LayoutParams newOverlayParams(int width, int height) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.setTitle("ideal Overlay");
        return params;
    }

    @SuppressWarnings("deprecation")
    private Rect availableBounds() {
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        int left = 0;
        int top = 0;
        int right = size.x;
        int bottom = size.y;
        View insetSource = entryAttached ? entryView : (hudAttached ? hudView : panelView);
        if (insetSource != null) {
            WindowInsets insets = insetSource.getRootWindowInsets();
            if (insets != null) {
                int insetLeft = insets.getSystemWindowInsetLeft();
                int insetTop = insets.getSystemWindowInsetTop();
                int insetRight = insets.getSystemWindowInsetRight();
                int insetBottom = insets.getSystemWindowInsetBottom();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    DisplayCutout cutout = insets.getDisplayCutout();
                    if (cutout != null) {
                        insetLeft = Math.max(insetLeft, cutout.getSafeInsetLeft());
                        insetTop = Math.max(insetTop, cutout.getSafeInsetTop());
                        insetRight = Math.max(insetRight, cutout.getSafeInsetRight());
                        insetBottom = Math.max(insetBottom, cutout.getSafeInsetBottom());
                    }
                }
                left += insetLeft;
                top += insetTop;
                right -= insetRight;
                bottom -= insetBottom;
            }
        }
        return new Rect(left, top, Math.max(left + 1, right), Math.max(top + 1, bottom));
    }

    private float referenceScale() {
        Rect bounds = availableBounds();
        return Math.min(bounds.width() / CANVAS_WIDTH, bounds.height() / CANVAS_HEIGHT);
    }

    private int panelWidth() {
        return Math.max(1, Math.round(ClickGuiView.REFERENCE_WIDTH * referenceScale()));
    }

    private int panelHeight() {
        return Math.max(1, Math.round(ClickGuiView.REFERENCE_HEIGHT * referenceScale()));
    }

    private void safelyRemove(View view) {
        try {
            windowManager.removeViewImmediate(view);
        } catch (IllegalArgumentException ignored) {
            // The window may already have been removed by the system.
        }
    }

    private static final class ShortcutRecord {
        final ModuleRepository.ModuleDefinition module;
        final ShortcutView view;
        final WindowManager.LayoutParams params;

        ShortcutRecord(ModuleRepository.ModuleDefinition module, ShortcutView view,
                       WindowManager.LayoutParams params) {
            this.module = module;
            this.view = view;
            this.params = params;
        }
    }
}
