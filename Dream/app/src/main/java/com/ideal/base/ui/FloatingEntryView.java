package com.ideal.base.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/** Draggable, tap-to-toggle purple overlay entry with no text label. */
public final class FloatingEntryView extends View {

    public interface Callback {
        void onClick();
        void onDragTo(int x, int y);
        void onDragReleased(int x, int y);
    }

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

    public FloatingEntryView(Context context, Callback callback) {
        super(context);
        this.callback = callback;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setWindowPosition(int x, int y) {
        windowX = x;
        windowY = y;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float radius = Math.min(width, height) * 12f / 82f;
        paint.setShader(new LinearGradient(0, 0, width, height,
                new int[]{0xFFA84A9E, 0xFF586FB8}, null, Shader.TileMode.CLAMP));
        paint.setShadowLayer(Math.max(3f, width * 0.07f), 0, width * 0.035f, 0x80000000);
        canvas.drawRoundRect(0, 0, width, height, radius, radius, paint);
        paint.clearShadowLayer();
        paint.setShader(null);

        // Local Canvas paths keep the entry icon deterministic and independent of system glyphs.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, width * 0.045f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xFFF2EFF7);
        float cx = width / 2f;
        float cy = height / 2f;
        canvas.drawCircle(cx, cy, width * 0.205f, paint);
        canvas.drawCircle(cx - width * 0.072f, cy - height * 0.025f, width * 0.016f, paint);
        canvas.drawCircle(cx + width * 0.072f, cy - height * 0.025f, width * 0.016f, paint);
        Path smile = new Path();
        smile.moveTo(cx - width * 0.09f, cy + height * 0.06f);
        smile.quadTo(cx, cy + height * 0.14f, cx + width * 0.09f, cy + height * 0.06f);
        canvas.drawPath(smile, paint);
        paint.setStyle(Paint.Style.FILL);
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
                    callback.onDragTo(windowX, windowY);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    callback.onDragReleased(windowX, windowY);
                } else {
                    performClick();
                    callback.onClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    callback.onDragReleased(windowX, windowY);
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
