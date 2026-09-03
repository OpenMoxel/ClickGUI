package com.ideal.base.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/** A single independently movable module shortcut window. */
public final class ShortcutView extends View {

    public interface Callback {
        void onClick(String moduleId);
        void onDragTo(String moduleId, int x, int y);
        void onDragReleased(String moduleId, int x, int y);
    }

    private final String moduleId;
    private final String label;
    private final Callback callback;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int touchSlop;
    private int windowX;
    private int windowY;
    private float downRawX;
    private float downRawY;
    private int startX;
    private int startY;
    private boolean dragging;

    public ShortcutView(Context context, String moduleId, String label, Callback callback) {
        super(context);
        this.moduleId = moduleId;
        this.label = label;
        this.callback = callback;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public static int estimateWidth(String label, float scale) {
        float textWidth = label.length() * 15.5f * scale;
        return Math.round(Math.max(124f * scale, textWidth + 50f * scale));
    }

    public static int estimateHeight(float scale) {
        return Math.round(86f * scale);
    }

    public void setWindowPosition(int x, int y) {
        windowX = x;
        windowY = y;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float radius = Math.min(width, height) * 15f / 86f;
        paint.setColor(0xFF202C50);
        paint.setShadowLayer(Math.max(3f, height * 0.06f), 0, height * 0.035f, 0x75000000);
        canvas.drawRoundRect(0, 0, width, height, radius, radius, paint);
        paint.clearShadowLayer();
        paint.setColor(0xFFF2EFF7);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(Math.max(12f, height * 27f / 86f));
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = height / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(label, width / 2f, baseline, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                startX = windowX;
                startY = windowY;
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!dragging && Math.hypot(dx, dy) > touchSlop) {
                    dragging = true;
                }
                if (dragging) {
                    windowX = Math.round(startX + dx);
                    windowY = Math.round(startY + dy);
                    callback.onDragTo(moduleId, windowX, windowY);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    callback.onDragReleased(moduleId, windowX, windowY);
                } else {
                    performClick();
                    callback.onClick(moduleId);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    callback.onDragReleased(moduleId, windowX, windowY);
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
