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
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

/** A separate movable desktop window for the currently configured feature shortcut. */
final class DesktopHotkeyOverlayView extends View {

    private static final int MIN_WIDTH_DP = 126;

    private final ClickGuiView clickGuiView;
    private final WindowManager windowManager;
    private final WindowManager.LayoutParams layoutParams;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG
            | Paint.SUBPIXEL_TEXT_FLAG);
    private final int touchSlop;

    private boolean attached;
    private boolean pressed;
    private boolean dragging;
    private float downRawX;
    private float downRawY;
    private int downWindowX;
    private int downWindowY;

    DesktopHotkeyOverlayView(Context context, ClickGuiView clickGuiView,
                             WindowManager windowManager, WindowManager.LayoutParams layoutParams) {
        super(context);
        this.clickGuiView = clickGuiView;
        this.windowManager = windowManager;
        this.layoutParams = layoutParams;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClickable(true);
        refreshAccessibilityDescription();
    }

    void setAttached(boolean attached) {
        this.attached = attached;
    }

    int getPreferredWidth() {
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(dp(18f));
        return Math.max(dp(MIN_WIDTH_DP),
                dp(44f) + Math.round(paint.measureText(clickGuiView.getFloatingHotkeyLabel())));
    }

    void refreshAccessibilityDescription() {
        setContentDescription(clickGuiView.getFloatingHotkeyLabel() + "悬浮快捷键，"
                + (clickGuiView.isFloatingHotkeyEnabled() ? "已开启" : "已关闭"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float outline = Math.max(2f, dp(2f));
        float outerRadius = Math.min(width, height) * 0.34f;
        float innerRadius = Math.max(0f, outerRadius - outline);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(clickGuiView.isFloatingHotkeyEnabled() ? Color.WHITE : Color.BLACK);
        canvas.drawRoundRect(0f, 0f, width, height, outerRadius, outerRadius, paint);

        paint.setColor(Color.argb(220, 29, 29, 36));
        canvas.drawRoundRect(outline, outline, width - outline, height - outline,
                innerRadius, innerRadius, paint);

        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(dp(18f));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(246, 246, 250));
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = height * 0.5f - (metrics.ascent + metrics.descent) * 0.5f;
        canvas.drawText(clickGuiView.getFloatingHotkeyLabel(), width * 0.5f, baseline, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                dragging = false;
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                downWindowX = layoutParams.x;
                downWindowY = layoutParams.y;
                setPressed(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - downRawX;
                float deltaY = event.getRawY() - downRawY;
                if (!dragging && (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop)) {
                    dragging = true;
                }
                if (dragging) {
                    // Keep the last free position exactly: no clamping, snapping, or release
                    // animation is applied to the shortcut overlay.
                    layoutParams.x = downWindowX + Math.round(deltaX);
                    layoutParams.y = downWindowY + Math.round(deltaY);
                    if (attached) {
                        windowManager.updateViewLayout(this, layoutParams);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                boolean click = pressed && !dragging;
                pressed = false;
                dragging = false;
                setPressed(false);
                if (click) {
                    performClick();
                    clickGuiView.toggleFloatingHotkey();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                dragging = false;
                setPressed(false);
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

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
