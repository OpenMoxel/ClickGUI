/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.notification;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.WindowManager;

/**
 * HackAI 通知系统
 *
 * 用法:
 *   NotificationManager.init(context)
 *   NotificationManager.show("KillAura", "Enabled", NotificationType.SUCCESS)
 *   NotificationManager.destroy()
 */
public final class NotificationManager {

    private static WindowManager windowManager = null;
    private static NotificationContainer container = null;
    private static boolean isShowing = false;

    private NotificationManager() {
    }

    public static void init(Context context) {
        if (windowManager != null) return;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        container = new NotificationContainer(context.getApplicationContext());
    }

    public static void show(String title, String message, NotificationType type) {
        WindowManager wm = windowManager;
        if (wm == null) return;
        NotificationContainer c = container;
        if (c == null) return;

        if (!isShowing) {
            float D = c.getResources().getDisplayMetrics().density;
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    (int) (246 * D),
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.BOTTOM | Gravity.END;
            params.x = (int) (16 * D);
            params.y = (int) (16 * D);
            wm.addView(c, params);
            isShowing = true;
        }

        NotificationData data = new NotificationData(
                System.nanoTime(),
                title,
                message,
                type
        );
        c.show(data);
    }

    public static void destroy() {
        if (container != null) {
            container.destroy();
            try {
                if (windowManager != null) windowManager.removeView(container);
            } catch (Throwable ignored) {
            }
        }
        container = null;
        windowManager = null;
        isShowing = false;
    }
}
