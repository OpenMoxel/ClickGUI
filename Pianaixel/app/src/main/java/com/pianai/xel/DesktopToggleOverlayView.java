/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

/** Draws and owns touch handling for the 40dp desktop panel-toggle overlay. */
final class DesktopToggleOverlayView extends View {

    interface Listener {
        void onPanelToggleRequested();
    }

    private final WindowManager windowManager;
    private final WindowManager.LayoutParams layoutParams;
    private final Listener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final int touchSlop;

    private boolean attached;
    private boolean panelOpen;
    private boolean pressed;
    private boolean dragging;
    private boolean movedBeforeLongPress;
    private float downRawX;
    private float downRawY;
    private int downWindowX;
    private int downWindowY;
    private final Runnable enterDragMode = () -> {
        if (pressed && !movedBeforeLongPress) {
            dragging = true;
        }
    };

    DesktopToggleOverlayView(Context context, WindowManager windowManager,
                             WindowManager.LayoutParams layoutParams, Listener listener) {
        super(context);
        this.windowManager = windowManager;
        this.layoutParams = layoutParams;
        this.listener = listener;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClickable(true);
    }

    void setAttached(boolean attached) {
        this.attached = attached;
    }

    void setPanelOpen(boolean open) {
        if (panelOpen == open) {
            return;
        }
        panelOpen = open;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float inset = Math.max(2f, dp(2f));
        float corner = Math.min(width, height) * 0.31f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(panelOpen ? Color.argb(238, 242, 242, 248)
                : Color.argb(238, 8, 8, 12));
        canvas.drawRoundRect(0f, 0f, width, height, corner, corner, paint);

        paint.setColor(Color.argb(229, 28, 29, 36));
        canvas.drawRoundRect(inset, inset, width - inset, height - inset,
                corner - inset, corner - inset, paint);

        float iconLeft = width * 0.30f;
        float iconTop = height * 0.31f;
        float iconRight = width * 0.70f;
        float iconBottom = height * 0.69f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, dp(1.7f)));
        paint.setColor(Color.rgb(242, 242, 247));
        canvas.drawRoundRect(iconLeft, iconTop, iconRight, iconBottom,
                width * 0.08f, width * 0.08f, paint);
        canvas.drawLine(iconLeft, iconTop + (iconBottom - iconTop) * 0.34f,
                iconRight, iconTop + (iconBottom - iconTop) * 0.34f, paint);
        canvas.drawLine(iconLeft + (iconRight - iconLeft) * 0.35f,
                iconTop + (iconBottom - iconTop) * 0.34f,
                iconLeft + (iconRight - iconLeft) * 0.35f, iconBottom, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                dragging = false;
                movedBeforeLongPress = false;
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                downWindowX = layoutParams.x;
                downWindowY = layoutParams.y;
                postDelayed(enterDragMode, ViewConfiguration.getLongPressTimeout());
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - downRawX;
                float deltaY = event.getRawY() - downRawY;
                if (!dragging && (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop)) {
                    movedBeforeLongPress = true;
                    removeCallbacks(enterDragMode);
                }
                if (dragging) {
                    // No edge clamp, snap, inertia, or release animation: retain exact drag.
                    layoutParams.x = downWindowX + Math.round(deltaX);
                    layoutParams.y = downWindowY + Math.round(deltaY);
                    if (attached) {
                        windowManager.updateViewLayout(this, layoutParams);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                removeCallbacks(enterDragMode);
                boolean click = pressed && !dragging && !movedBeforeLongPress;
                pressed = false;
                dragging = false;
                if (click) {
                    performClick();
                    listener.onPanelToggleRequested();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(enterDragMode);
                pressed = false;
                dragging = false;
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(enterDragMode);
        super.onDetachedFromWindow();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
