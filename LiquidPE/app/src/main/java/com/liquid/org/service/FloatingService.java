/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.liquid.org.MainActivity;
import com.liquid.org.R;
import com.liquid.org.ui.overlay.LiquidBounceOverlayView;

/**
 * 常驻在其他应用上方的前台服务。按钮和 ClickGUI 使用独立的 WindowManager 图层：
 * GUI 关闭时会移除全屏图层，因此不会遮挡或吞掉其他界面的触摸事件。
 */
public class FloatingService extends Service {
    private static final String CHANNEL_ID = "liquidbounce_visual_overlay";
    private static final int NOTIFICATION_ID = 4301;
    private static final String PREFS_NAME = "floating_clickgui_button";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    private WindowManager windowManager;
    private WindowManager.LayoutParams buttonLayoutParams;
    private WindowManager.LayoutParams clickGuiLayoutParams;
    private ImageView floatingButton;
    private LiquidBounceOverlayView overlayView;
    private boolean buttonAttached;
    private boolean clickGuiAttached;

    /** 供 Activity 在已取得悬浮窗权限后启动；重复调用不会创建多个服务。 */
    public static boolean start(Context context) {
        if (!Settings.canDrawOverlays(context)) return false;
        ContextCompat.startForegroundService(context.getApplicationContext(), new Intent(context, FloatingService.class));
        return true;
    }

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createClickGuiLayer();
        createFloatingButton();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        // 前台服务在页面切换甚至进程回收后仍会尝试恢复悬浮按钮。
        return START_STICKY;
    }

    private void createClickGuiLayer() {
        // 复用现有 LiquidBounceOverlayView 及其 setClickGuiVisible 开关，不另写 ClickGUI。
        overlayView = new LiquidBounceOverlayView(this);
        overlayView.setHudVisible(true);
        overlayView.setTransparentBase(true);
        overlayView.setClickGuiVisibleImmediately(false);
        overlayView.setOnClickGuiClosedListener(this::detachClickGuiAfterAnimation);
        clickGuiLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        clickGuiLayoutParams.gravity = Gravity.TOP | Gravity.START;
    }

    private void createFloatingButton() {
        floatingButton = new ImageView(this);
        floatingButton.setImageResource(R.drawable.clickgui);
        floatingButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        floatingButton.setAdjustViewBounds(true);
        floatingButton.setBackgroundColor(Color.TRANSPARENT);
        floatingButton.setContentDescription(getString(R.string.floating_clickgui_button_description));
        floatingButton.setOnClickListener(v -> toggleClickGui());

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        buttonLayoutParams = new WindowManager.LayoutParams(
                dp(35),
                dp(35),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        buttonLayoutParams.gravity = Gravity.TOP | Gravity.START;
        buttonLayoutParams.x = preferences.getInt(KEY_X, dp(24));
        buttonLayoutParams.y = preferences.getInt(KEY_Y, dp(160));
        installDragListener();
        windowManager.addView(floatingButton, buttonLayoutParams);
        buttonAttached = true;
    }

    /** 以触摸阈值区分点击和拖动，拖动结束时保存按钮位置。 */
    private void installDragListener() {
        final int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        floatingButton.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX;
            private float downRawY;
            private int startX;
            private int startY;
            private boolean dragging;

            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        startX = buttonLayoutParams.x;
                        startY = buttonLayoutParams.y;
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downRawX;
                        float dy = event.getRawY() - downRawY;
                        if (!dragging && Math.hypot(dx, dy) > touchSlop) dragging = true;
                        if (dragging) {
                            buttonLayoutParams.x = startX + Math.round(dx);
                            buttonLayoutParams.y = startY + Math.round(dy);
                            if (buttonAttached) windowManager.updateViewLayout(floatingButton, buttonLayoutParams);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (dragging) saveButtonPosition();
                        else view.performClick();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        if (dragging) saveButtonPosition();
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private void toggleClickGui() {
        if (overlayView != null && overlayView.isClickGuiVisibleRequested()) hideClickGui();
        else showClickGui();
    }

    private void showClickGui() {
        if (overlayView == null) return;
        if (!clickGuiAttached) {
            windowManager.addView(overlayView, clickGuiLayoutParams);
            clickGuiAttached = true;
        }
        overlayView.setClickGuiVisible(true);
        // 全屏 GUI 后重新附加关闭按钮，确保按钮保持在最上层。
        bringButtonToFront();
    }

    private void hideClickGui() {
        if (!clickGuiAttached || overlayView == null) return;
        overlayView.setClickGuiVisible(false);
    }

    /** Called only after the 170 ms close animation reaches scale/alpha zero. */
    private void detachClickGuiAfterAnimation() {
        if (!clickGuiAttached || overlayView == null || overlayView.isClickGuiVisibleRequested()) return;
        windowManager.removeView(overlayView);
        clickGuiAttached = false;
    }

    private void bringButtonToFront() {
        if (!buttonAttached) return;
        windowManager.removeView(floatingButton);
        buttonAttached = false;
        windowManager.addView(floatingButton, buttonLayoutParams);
        buttonAttached = true;
    }

    private void saveButtonPosition() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putInt(KEY_X, buttonLayoutParams.x)
                .putInt(KEY_Y, buttonLayoutParams.y)
                .apply();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Notification createNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.overlay_notification_title))
                .setContentText(getString(R.string.overlay_notification_text))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.overlay_channel_name), NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override public void onDestroy() {
        if (windowManager != null && clickGuiAttached) windowManager.removeView(overlayView);
        if (windowManager != null && buttonAttached) windowManager.removeView(floatingButton);
        clickGuiAttached = false;
        buttonAttached = false;
        overlayView = null;
        floatingButton = null;
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
