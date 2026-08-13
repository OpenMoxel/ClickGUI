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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.hack.ai.data.ThemeManager;

public class HackAIBrandView extends View {

    private final float D = getResources().getDisplayMetrics().density;
    private final int pad = (int) (8 * D);
    private final int barW = (int) (2 * D);

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Matrix borderMatrix = new Matrix();

    private float barAlpha = 0.5f;
    private float flowOffset = 0f;

    public HackAIBrandView(Context context) {
        this(context, null);
    }

    public HackAIBrandView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HackAIBrandView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        bgPaint.setColor(Color.parseColor("#AA000000"));
        bgPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(12f * D);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f * D);

        // 整体呼吸
        ValueAnimator breath = ValueAnimator.ofFloat(0.4f, 1f);
        breath.setDuration(1200);
        breath.setRepeatCount(ValueAnimator.INFINITE);
        breath.setRepeatMode(ValueAnimator.REVERSE);
        breath.setInterpolator(new LinearInterpolator());
        breath.addUpdateListener(animation -> setAlpha((float) animation.getAnimatedValue()));
        breath.start();

        // bar 呼吸
        ValueAnimator barBreath = ValueAnimator.ofFloat(0.2f, 1f);
        barBreath.setDuration(1200);
        barBreath.setRepeatCount(ValueAnimator.INFINITE);
        barBreath.setRepeatMode(ValueAnimator.REVERSE);
        barBreath.setInterpolator(new LinearInterpolator());
        barBreath.addUpdateListener(animation -> {
            barAlpha = (float) animation.getAnimatedValue();
            invalidate();
        });
        barBreath.start();

        // 跑马灯流动
        ValueAnimator flow = ValueAnimator.ofFloat(0f, 2f);
        flow.setDuration(2000);
        flow.setRepeatCount(ValueAnimator.INFINITE);
        flow.setInterpolator(new LinearInterpolator());
        flow.addUpdateListener(animation -> {
            flowOffset = (float) animation.getAnimatedValue();
            invalidate();
        });
        flow.start();
    }

    @Override
    protected void onMeasure(int wms, int hms) {
        int tw = (int) (textPaint.measureText("HackAI · v26.x") + 0.5f);
        setMeasuredDimension(tw + pad * 2 + barW + pad, (int) (textPaint.getTextSize() * 1.6f + pad * 2));
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        rect.set(0f, 0f, (float) w, (float) h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 背景
        canvas.drawRoundRect(rect, 4f * D, 4f * D, bgPaint);

        // 跑马灯边框
        float w = getWidth();
        int accent = ThemeManager.accentColor;
        LinearGradient shader = new LinearGradient(
                flowOffset * w - w, 0f, flowOffset * w + w, 0f,
                new int[]{Color.TRANSPARENT, accent, Color.TRANSPARENT},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP
        );
        borderPaint.setShader(shader);
        canvas.drawRoundRect(rect, 4f * D, 4f * D, borderPaint);
        borderPaint.setShader(null);

        // 左侧 bar
        float barX = (float) pad;
        float barH = textPaint.getTextSize() * 1.2f;
        float barY = (getHeight() - barH) / 2f;
        barPaint.setColor(accent);
        barPaint.setAlpha((int) (barAlpha * 255));
        float r = barW / 2f;
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, r, r, barPaint);

        // 文字
        float ty = getHeight() / 2f + textPaint.getTextSize() / 3f;
        canvas.drawText("HackAI · v26.x", barX + barW + pad, ty, textPaint);
    }
}
