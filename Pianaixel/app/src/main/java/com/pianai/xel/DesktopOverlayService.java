/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */

package com.pianai.xel;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;

/**
 * Hosts the existing Canvas ClickGUI above the launcher or another app after the user grants
 * Android's overlay permission. The compact window is the only touchable surface while the full
 * panel is hidden, so transparent desktop pixels remain available to the app below.
 */
public final class DesktopOverlayService extends Service {

    private static final int TOGGLE_SIZE_DP = 40;
    private static final int TOGGLE_MARGIN_END_DP = 36;
    private static final int TOGGLE_TOP_DP = 360;
    private static final int HOTKEY_MIN_WIDTH_DP = 126;
    private static final int HOTKEY_HEIGHT_DP = 47;
    private static final float HOTKEY_REFERENCE_X = 814f;
    private static final float HOTKEY_REFERENCE_Y = 539f;
    private static final float REFERENCE_WIDTH = 1920f;
    private static final float REFERENCE_HEIGHT = 1080f;

    private WindowManager windowManager;
    private ClickGuiView clickGuiView;
    private DesktopToggleOverlayView desktopToggleView;
    private DesktopHotkeyOverlayView desktopHotkeyView;
    private WindowManager.LayoutParams panelLayoutParams;
    private WindowManager.LayoutParams toggleLayoutParams;
    private WindowManager.LayoutParams hotkeyLayoutParams;
    private boolean panelAttached;
    private boolean toggleAttached;
    private boolean hotkeyAttached;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        panelLayoutParams = createPanelLayoutParams();
        toggleLayoutParams = createToggleLayoutParams();
        hotkeyLayoutParams = createHotkeyLayoutParams();

        clickGuiView = new ClickGuiView(this);
        // The external desktop button is topmost in overlay mode, so the Canvas version must not
        // be drawn a second time inside the full-screen host.
        clickGuiView.setPanelToggleButtonVisible(false);
        clickGuiView.setPanelVisible(false);

        desktopToggleView = new DesktopToggleOverlayView(this, windowManager, toggleLayoutParams,
                this::togglePanel);
        desktopHotkeyView = new DesktopHotkeyOverlayView(this, clickGuiView, windowManager,
                hotkeyLayoutParams);
        // The shortcut has its own overlay window. The full-screen ClickGUI window can therefore
        // be removed without removing an already enabled "显示快捷键" control from the desktop.
        clickGuiView.setFloatingHotkeyDrawnExternally(true);
        clickGuiView.setHotkeyStateListener(this::syncDesktopHotkey);
        attachDesktopToggle();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (clickGuiView != null) {
            clickGuiView.setHotkeyStateListener(null);
        }
        detachPanel();
        detachDesktopHotkey();
        detachDesktopToggle();
        super.onDestroy();
    }

    private WindowManager.LayoutParams createPanelLayoutParams() {
        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        return params;
    }

    private WindowManager.LayoutParams createToggleLayoutParams() {
        int size = dp(TOGGLE_SIZE_DP);
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Math.max(0, getResources().getDisplayMetrics().widthPixels
                - size - dp(TOGGLE_MARGIN_END_DP));
        params.y = dp(TOGGLE_TOP_DP);
        return params;
    }

    private WindowManager.LayoutParams createHotkeyLayoutParams() {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(HOTKEY_MIN_WIDTH_DP),
                dp(HOTKEY_HEIGHT_DP),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        int displayWidth = getResources().getDisplayMetrics().widthPixels;
        int displayHeight = getResources().getDisplayMetrics().heightPixels;
        params.x = Math.round(displayWidth * HOTKEY_REFERENCE_X / REFERENCE_WIDTH);
        params.y = Math.round(displayHeight * HOTKEY_REFERENCE_Y / REFERENCE_HEIGHT);
        return params;
    }

    private void togglePanel() {
        if (panelAttached) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    private void showPanel() {
        if (panelAttached || !Settings.canDrawOverlays(this)) {
            return;
        }
        clickGuiView.setPanelVisible(true);
        try {
            windowManager.addView(clickGuiView, panelLayoutParams);
            panelAttached = true;
            desktopToggleView.setPanelOpen(true);
            // Re-add the compact controller after the full window so it remains the topmost
            // desktop control and can close the panel again. The independent shortcut needs the
            // same ordering so it remains usable while the full panel is present.
            detachDesktopToggle();
            detachDesktopHotkey();
            attachDesktopToggle();
            syncDesktopHotkey();
        } catch (WindowManager.BadTokenException | SecurityException exception) {
            clickGuiView.setPanelVisible(false);
            panelAttached = false;
            desktopToggleView.setPanelOpen(false);
            syncDesktopHotkey();
        }
    }

    private void hidePanel() {
        if (!panelAttached) {
            return;
        }
        detachPanel();
        clickGuiView.setPanelVisible(false);
        desktopToggleView.setPanelOpen(false);
        attachDesktopToggle();
        // Do not detach desktopHotkeyView here. It remains on screen until the user turns off
        // "显示快捷键" in the settings panel (or the overlay service itself is stopped).
        syncDesktopHotkey();
    }

    private void attachDesktopToggle() {
        if (toggleAttached || windowManager == null || desktopToggleView == null) {
            return;
        }
        try {
            windowManager.addView(desktopToggleView, toggleLayoutParams);
            toggleAttached = true;
            desktopToggleView.setAttached(true);
        } catch (WindowManager.BadTokenException | SecurityException exception) {
            stopSelf();
        }
    }

    private void detachDesktopToggle() {
        if (!toggleAttached) {
            return;
        }
        try {
            windowManager.removeView(desktopToggleView);
        } catch (IllegalArgumentException ignored) {
            // The system may have already detached a revoked overlay window.
        }
        toggleAttached = false;
        desktopToggleView.setAttached(false);
    }

    /** Reflects the controller's single shortcut state in a compact independent overlay. */
    private void syncDesktopHotkey() {
        if (clickGuiView == null || desktopHotkeyView == null || hotkeyLayoutParams == null) {
            return;
        }
        if (!clickGuiView.shouldShowFloatingHotkey()) {
            detachDesktopHotkey();
            return;
        }

        int preferredWidth = desktopHotkeyView.getPreferredWidth();
        if (hotkeyLayoutParams.width != preferredWidth) {
            hotkeyLayoutParams.width = preferredWidth;
            if (hotkeyAttached) {
                try {
                    windowManager.updateViewLayout(desktopHotkeyView, hotkeyLayoutParams);
                } catch (IllegalArgumentException ignored) {
                    hotkeyAttached = false;
                    desktopHotkeyView.setAttached(false);
                }
            }
        }
        desktopHotkeyView.refreshAccessibilityDescription();
        desktopHotkeyView.invalidate();
        attachDesktopHotkey();
    }

    private void attachDesktopHotkey() {
        if (hotkeyAttached || windowManager == null || desktopHotkeyView == null) {
            return;
        }
        try {
            windowManager.addView(desktopHotkeyView, hotkeyLayoutParams);
            hotkeyAttached = true;
            desktopHotkeyView.setAttached(true);
        } catch (WindowManager.BadTokenException | SecurityException exception) {
            stopSelf();
        }
    }

    private void detachDesktopHotkey() {
        if (!hotkeyAttached) {
            return;
        }
        try {
            windowManager.removeView(desktopHotkeyView);
        } catch (IllegalArgumentException ignored) {
            // The system may have already detached a revoked overlay window.
        }
        hotkeyAttached = false;
        desktopHotkeyView.setAttached(false);
    }

    private void detachPanel() {
        if (!panelAttached) {
            return;
        }
        try {
            windowManager.removeView(clickGuiView);
        } catch (IllegalArgumentException ignored) {
            // The system may have already detached a revoked overlay window.
        }
        panelAttached = false;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}
