/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class ToastView extends View {

    public String moduleName = "";
    public boolean moduleOn = false;

    // 0f = empty, 1f = fully filled (clipRect 控制填充区域)
    private float fillProgress = 0f;

    // 动态色调偏移 0..360，用于彩虹流动
    private float hueOffset = 0f;
    private ValueAnimator hueAnimator = null;
    private float viewWidth = 0f;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF rect = new RectF();
    private final float cornerRadius = 10f * getResources().getDisplayMetrics().density;

    private LinearGradient gradientShader = null;

    public ToastView(Context context) {
        this(context, null);
    }

    public ToastView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToastView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        bgPaint.setColor(Color.argb(Math.round(0.75f * 255), 20, 20, 20));
        bgPaint.setStyle(Paint.Style.FILL);

        fillPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setTextSize(13f * getResources().getDisplayMetrics().density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public float getFillProgress() {
        return fillProgress;
    }

    public void setFillProgress(float value) {
        fillProgress = value;
        invalidate();
    }

    /**
     * 入场：从左侧滑入 + 颜色向右填充
     */
    public void playEnter(Runnable onEnd) {
        final float w = viewWidth;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(it -> {
            float t = (float) it.getAnimatedValue();
            setFillProgress(t);
            setTranslationX((t - 1f) * w);
        });
        doOnEnd(animator, () -> {
            if (onEnd != null) onEnd.run();
            startHueCycle();
        });
        animator.start();
    }

    public void playEnter() {
        playEnter(null);
    }

    /**
     * 出场：向左侧滑出 + 颜色向左消退
     */
    public void playExit(Runnable onEnd) {
        stopHueCycle();
        final float w = viewWidth;
        ValueAnimator animator = ValueAnimator.ofFloat(fillProgress, 0f);
        animator.setDuration(200);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(it -> {
            float t = (float) it.getAnimatedValue();
            setFillProgress(t);
            setTranslationX((t - 1f) * w);
        });
        doOnEnd(animator, () -> {
            if (onEnd != null) onEnd.run();
        });
        animator.start();
    }

    public void playExit() {
        playExit(null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = (float) w;
        rect.set(0f, 0f, (float) w, (float) h);
        buildGradient();
    }

    private void buildGradient() {
        if (viewWidth <= 0) return;
        final float hueStep = 72f; // 360 / 5
        int[] colors = new int[5];
        for (int i = 0; i < 5; i++) {
            colors[i] = Color.HSVToColor(new float[]{(hueOffset + i * hueStep) % 360f, 0.9f, 1.0f});
        }
        gradientShader = new LinearGradient(0f, 0f, viewWidth, 0f, colors, null, Shader.TileMode.CLAMP);
        fillPaint.setShader(gradientShader);
    }

    private void startHueCycle() {
        if (hueAnimator != null) hueAnimator.cancel();
        hueAnimator = ValueAnimator.ofFloat(0f, 360f);
        hueAnimator.setDuration(2000);
        hueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        hueAnimator.setInterpolator(new LinearInterpolator());
        hueAnimator.addUpdateListener(it -> {
            hueOffset = (float) it.getAnimatedValue();
            buildGradient();
            invalidate();
        });
        hueAnimator.start();
    }

    private void stopHueCycle() {
        if (hueAnimator != null) hueAnimator.cancel();
        hueAnimator = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 背景底色
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint);

        // 渐变色填充（clipRect：fillProgress 控制从左到右的填充区域）
        float fillRight = rect.left + fillProgress * rect.width();
        if (fillRight > rect.left) {
            canvas.save();
            canvas.clipRect(rect.left, rect.top, fillRight, rect.bottom);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);
            canvas.restore();
        }

        // 文字
        float textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(labelText(), rect.centerX(), textY, textPaint);
    }

    private String labelText() {
        String state = moduleOn ? "enabled" : "disabled";
        return moduleName + " was " + state;
    }

    private void doOnEnd(ValueAnimator animator, Runnable action) {
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                action.run();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopHueCycle();
    }
}
