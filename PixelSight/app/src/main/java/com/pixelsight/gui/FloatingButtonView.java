package com.pixelsight.gui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

public class FloatingButtonView extends View {

    private Paint textPaint;
    private float lastX, lastY;
    private Runnable onClickAction;
    private boolean isDragging = false;
    private float glowRadius = 25f;

    public FloatingButtonView(Context context) {
        super(context);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        // 极美的高级紫色
        textPaint.setColor(Color.parseColor("#B5179E")); 
        textPaint.setTextSize(140f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));

        // 呼吸灯辉光动画
        ValueAnimator breathAnim = ValueAnimator.ofFloat(15f, 40f);
        breathAnim.setDuration(1200);
        breathAnim.setRepeatMode(ValueAnimator.REVERSE);
        breathAnim.setRepeatCount(ValueAnimator.INFINITE);
        breathAnim.addUpdateListener(a -> {
            glowRadius = (float) a.getAnimatedValue();
            invalidate();
        });
        breathAnim.start();
    }

    public void setOnClickAction(Runnable action) {
        this.onClickAction = action;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getRawX();
                lastY = event.getRawY();
                isDragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - lastX;
                float dy = event.getRawY() - lastY;
                if (Math.hypot(dx, dy) > 10f) {
                    isDragging = true;
                }
                if (isDragging) {
                    setX(getX() + dx);
                    setY(getY() + dy);
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!isDragging && onClickAction != null) {
                    onClickAction.run();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 动态设置发光半径
        textPaint.setShadowLayer(glowRadius, 0, 0, Color.parseColor("#C77DFF"));
        
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float x = (getWidth() - textPaint.measureText("P")) / 2f;
        float y = getHeight() / 2f - (fm.descent + fm.ascent) / 2f;
        canvas.drawText("P", x, y, textPaint);
    }
}
