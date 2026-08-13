/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.hack.ai.island.IslandView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Manages the Dynamic Island overlay window.
 * Always-on HUD displayed at the top-centre of the screen.
 */
public final class IslandManager {

    private static final IslandManager INSTANCE = new IslandManager();

    private IslandView islandView;
    private WindowManager.LayoutParams islandParams;
    private WindowManager windowManager;
    private Context appContext;
    private boolean initialized;

    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormatter =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private DisplayManager displayManager;
    private DisplayManager.DisplayListener displayListener;
    private BroadcastReceiver batteryReceiver;
    private int lastBatteryLevel = 100;
    private int lastBatteryScale = 100;

    private IslandManager() {
    }

    public static IslandManager getInstance() {
        return INSTANCE;
    }

    // ── lifecycle ──────────────────────────────────────────────────────

    public synchronized void init(Context context) {
        if (initialized) return;
        appContext = context.getApplicationContext();
        windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);

        INSTANCE.createIsland(this);
        startTimeTicker();
        registerDisplayListener();
        registerBatteryReceiver();

        initialized = true;
    }

    public synchronized void release() {
        timeHandler.removeCallbacksAndMessages(null);

        if (displayListener != null && displayManager != null) {
            displayManager.unregisterDisplayListener(displayListener);
        }
        displayListener = null;
        displayManager = null;

        if (batteryReceiver != null && appContext != null) {
            try {
                appContext.unregisterReceiver(batteryReceiver);
            } catch (IllegalArgumentException ignored) {
            }
        }
        batteryReceiver = null;

        if (islandView != null && windowManager != null) {
            try {
                windowManager.removeView(islandView);
            } catch (IllegalArgumentException ignored) {
            }
        }
        islandView = null;
        islandParams = null;
        windowManager = null;
        appContext = null;
        initialized = false;
    }

    // ── island window ──────────────────────────────────────────────────

    // ── time ticker (also polls refresh rate for VRR panels) ────────────

    private final Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            updateTime();
            updateRefreshRate();
            timeHandler.postDelayed(this, 1000);
        }
    };

    private void startTimeTicker() {
        timeHandler.post(timeRunnable);
    }

    private void updateTime() {
        if (islandView != null) {
            islandView.setTimeText(timeFormatter.format(new Date()));
        }
    }

    // ── refresh rate ───────────────────────────────────────────────────

    /** Registers for major display-mode changes (resolution / orientation). */
    private void registerDisplayListener() {
        displayManager = (DisplayManager) appContext.getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager == null) return;

        displayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (displayId == Display.DEFAULT_DISPLAY) {
                    updateRefreshRate();
                }
            }
        };
        displayManager.registerDisplayListener(displayListener, timeHandler);
    }

    /**
     * Reads the device's instantaneous refresh rate and pushes it to the island view.
     * <p>
     * {@code Display.getRefreshRate()} is the only API that returns the actual,
     * moment-to-moment frame rate on VRR (Variable Refresh Rate) panels.
     * {@code Display.Mode.getRefreshRate()} returns the configured mode baseline
     * and does NOT reflect real-time VRR changes, so we prefer the deprecated
     * {@code getRefreshRate()} which still works correctly on all API levels.
     * <p>
     * Called every second from the time ticker + on major display-mode changes.
     */
    private void updateRefreshRate() {
        if (islandView == null) return;
        try {
            Display display = displayManager != null
                    ? displayManager.getDisplay(Display.DEFAULT_DISPLAY) : null;
            float rate = 60f;
            if (display != null) {
                // Prefer getRefreshRate() — returns instantaneous rate (VRR-aware).
                // Falls back to Display.Mode for the rare case where getRefreshRate returns 0.
                rate = display.getRefreshRate();
                if (rate <= 0f) {
                    Display.Mode mode = display.getMode();
                    if (mode != null) {
                        rate = mode.getRefreshRate();
                    }
                }
            }
            islandView.setRefreshRate(((int) rate) + "Hz");
        } catch (Exception ignored) {
        }
    }

    // ── battery ────────────────────────────────────────────────────────

    private void registerBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                lastBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, lastBatteryLevel);
                lastBatteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, lastBatteryScale);
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        appContext.registerReceiver(batteryReceiver, filter);
    }

    // ── utility ────────────────────────────────────────────────────────

    private int statusBarHeightPx() {
        int result = 0;
        if (appContext != null) {
            int resourceId = appContext.getResources().getIdentifier(
                    "status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = appContext.getResources().getDimensionPixelSize(resourceId);
            }
        }
        if (result == 0) {
            result = dpToPx(24, appContext.getResources().getDisplayMetrics().density);
        }
        return result;
    }

    private static int dpToPx(int dp, float density) {
        return Math.round(dp * density);
    }

    private void createIsland(IslandManager islandManager) {
        IslandView view = new IslandView(islandManager.appContext);
        islandManager.islandView = view;

        float density = islandManager.appContext.getResources().getDisplayMetrics().density;
        int displayWidth = islandManager.appContext.getResources().getDisplayMetrics().widthPixels;
        int iw = view.collapsedW;
        int ih = view.collapsedH;

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                iw, ih,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = (displayWidth - iw) / 2;
        // Subtract glow padding so the visible body sits at the desired position,
        // allowing ambient glow to bleed past screen edges naturally.
        params.y = islandManager.statusBarHeightPx() + dpToPx(2, density)
                - (int) (IslandView.GLOW_PAD_DP * density);
        islandManager.islandParams = params;

        // Sync window size with IslandView animation via callback
        view.setOnSizeChangeListener((w, h) -> {
            if (islandManager.islandParams != null && islandManager.windowManager != null) {
                islandManager.islandParams.width = w;
                islandManager.islandParams.height = h;
                islandManager.islandParams.x = (displayWidth - w) / 2;
                try {
                    islandManager.windowManager.updateViewLayout(view, islandManager.islandParams);
                } catch (IllegalArgumentException ignored) {
                }
            }
        });

        // Tap to toggle expand/collapse (with small-movement tolerance)
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    float[] down = new float[]{event.getX(), event.getY()};
                    v.setTag(down);
                    return false;
                }
                case MotionEvent.ACTION_UP: {
                    float[] down = (float[]) v.getTag();
                    if (down != null) {
                        float moveX = event.getX() - down[0];
                        float moveY = event.getY() - down[1];
                        float threshold = 8f * density;
                        if (moveX * moveX + moveY * moveY < threshold * threshold) {
                            v.performClick();
                            ((IslandView) v).toggle();
                        }
                    }
                    return true;
                }
                default:
                    return false;
            }
        });

        view.setVersionText("HackAI_26.x");

        islandManager.windowManager.addView(view, params);
        islandManager.updateTime();
        islandManager.updateRefreshRate();
    }
}
