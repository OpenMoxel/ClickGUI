/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.hack.ai.data.SliderSetting;

import java.util.function.Consumer;

public class SettingSlider extends View {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private SliderSetting setting = new SliderSetting(0f, 1f, 0.1f, 0f);
    private float value = 0f;
    public Consumer<Float> onValueChange = null;

    public SettingSlider(Context context) {
        this(context, null);
    }

    public SettingSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        knobPaint.setColor(Color.WHITE);
        knobPaint.setShadowLayer(4f, 0f, 1f, Color.parseColor("#77424952"));
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void configure(SliderSetting setting, float currentValue) {
        configure(setting, currentValue, false);
    }

    public void configure(SliderSetting setting, float currentValue, boolean animated) {
        this.setting = setting;
        setValue(currentValue, animated, false);
    }

    public void setValue(float newValue, boolean animated) {
        setValue(newValue, animated, true);
    }

    public void setValue(float newValue, boolean animated, boolean notify) {
        float snapped = snap(newValue);
        if (animated) {
            ValueAnimator animator = ValueAnimator.ofFloat(value, snapped);
            animator.setDuration(180);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(it -> {
                value = (float) it.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            value = snapped;
            invalidate();
        }
        if (notify && onValueChange != null) onValueChange.accept(snapped);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cy = getHeight() / 2f;
        float start = getPaddingLeft() + 8f;
        float end = getWidth() - getPaddingRight() - 15f;
        float percent = Math.min(Math.max((value - setting.getMin()) / (setting.getMax() - setting.getMin()), 0f), 1f);
        float x = start + (end - start) * percent;
        trackPaint.setColor(com.hack.ai.data.ThemeManager.themedColor(
                Color.parseColor("#3D3E4652"), Color.parseColor("#CAC4D0")));
        trackPaint.setStrokeWidth(15f);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        int c = com.hack.ai.data.ThemeManager.accentColor;
        fillPaint.setShader(new LinearGradient(start, 0f, end, 0f, c, c, Shader.TileMode.CLAMP));
        fillPaint.setStrokeWidth(8f);
        fillPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(start, cy, end, cy, trackPaint);
        canvas.drawLine(start, cy, x, cy, fillPaint);
        canvas.drawRoundRect(x - 15f, cy - 15f, x + 15f, cy + 15f, 7f, 7f, knobPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                updateFromX(event.getX());
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateFromX(float x) {
        float start = getPaddingLeft() + 8f;
        float end = getWidth() - getPaddingRight() - 8f;
        float percent = Math.min(Math.max((x - start) / (end - start), 0f), 1f);
        setValue(setting.getMin() + (setting.getMax() - setting.getMin()) * percent, false);
    }

    private float snap(float raw) {
        float clamped = Math.min(Math.max(raw, setting.getMin()), setting.getMax());
        return setting.getMin() + (float) Math.rint((clamped - setting.getMin()) / setting.getStep()) * setting.getStep();
    }
}
