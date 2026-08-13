/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public class ToastManager {

    private final Context context;
    private final WindowManager windowManager;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ToastEntry> entries = new ArrayList<>();
    private final int maxToasts = 6;
    private final long durationMs = 2500L;

    private final float density;

    private final int toastWidth;
    private final int toastHeight;
    private final int gap;
    private final int bottomMargin;
    private final int rightMargin;
    private final int cornerRadius;

    public ToastManager(Context context, WindowManager windowManager) {
        this.context = context;
        this.windowManager = windowManager;
        this.density = context.getResources().getDisplayMetrics().density;
        this.toastWidth = (int) (280 * density);
        this.toastHeight = (int) (44 * density);
        this.gap = (int) (6 * density);
        this.bottomMargin = (int) (72 * density);
        this.rightMargin = (int) (12 * density);
        this.cornerRadius = (int) (10 * density);
    }

    public void show(String moduleName, boolean isEnabled) {
        handler.post(() -> showInternal(moduleName, isEnabled));
    }

    private void showInternal(String moduleName, boolean isEnabled) {
        // 超过上限时移除最早的一条
        if (entries.size() >= maxToasts) {
            forceRemove(entries.get(0));
        }

        ToastView view = new ToastView(context);
        view.moduleName = moduleName;
        view.moduleOn = isEnabled;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                toastWidth,
                toastHeight,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        windowManager.addView(view, params);

        ToastEntry entry = new ToastEntry(view, params, false);
        entries.add(entry);

        layoutEntries();

        view.playEnter(() -> scheduleAutoDismiss(entry));
    }

    private void scheduleAutoDismiss(ToastEntry entry) {
        entry.timerRunnable = () -> dismissEntry(entry);
        handler.postDelayed(entry.timerRunnable, durationMs);
    }

    private void dismissEntry(ToastEntry entry) {
        if (entry.exiting) return;
        entry.exiting = true;
        if (entry.timerRunnable != null) handler.removeCallbacks(entry.timerRunnable);

        entry.view.playExit(() -> {
            try {
                windowManager.removeView(entry.view);
            } catch (Throwable ignored) {
            }
            entries.remove(entry);
            layoutEntries();
        });
    }

    private void forceRemove(ToastEntry entry) {
        if (entry.timerRunnable != null) handler.removeCallbacks(entry.timerRunnable);
        try {
            windowManager.removeView(entry.view);
        } catch (Throwable ignored) {
        }
        entries.remove(entry);
    }

    private void layoutEntries() {
        android.util.DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenW = displayMetrics.widthPixels;
        int screenH = displayMetrics.heightPixels;

        for (int index = 0; index < entries.size(); index++) {
            ToastEntry entry = entries.get(index);
            // 从底部向上堆叠
            int yOffset = (entries.size() - 1 - index) * (toastHeight + gap);
            entry.params.x = screenW - toastWidth - rightMargin;
            entry.params.y = screenH - bottomMargin - toastHeight - yOffset;
            windowManager.updateViewLayout(entry.view, entry.params);
        }
    }

    public void destroy() {
        for (ToastEntry entry : new ArrayList<>(entries)) {
            if (entry.timerRunnable != null) handler.removeCallbacks(entry.timerRunnable);
            try {
                windowManager.removeView(entry.view);
            } catch (Throwable ignored) {
            }
        }
        entries.clear();
        handler.removeCallbacksAndMessages(null);
    }

    private static final class ToastEntry {
        final ToastView view;
        final WindowManager.LayoutParams params;
        boolean exiting;
        Runnable timerRunnable = null;

        ToastEntry(ToastView view, WindowManager.LayoutParams params, boolean exiting) {
            this.view = view;
            this.params = params;
            this.exiting = exiting;
        }
    }
}
