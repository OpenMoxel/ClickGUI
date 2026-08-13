/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.notification;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class NotificationView extends View {

    private final float D = getResources().getDisplayMetrics().density;
    private final int W = (int) (230 * D);
    private final int H = (int) (44 * D);
    private final float radius = 9f * D;
    private final float padH = 12f * D;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint msgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public String title = "";
    public String message = "";
    public int gradientStart = (int) 0xFF00FF88L;
    public int gradientEnd = (int) 0xFF00BFFFL;

    private float flowOffset = 0f;
    private final ValueAnimator flowAnim;

    public NotificationView(Context context) {
        this(context, null);
    }

    public NotificationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        bgPaint.setColor(Color.parseColor("#D90A0A0A"));
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f * D);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(2.5f * D);

        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(13f * D);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        titlePaint.setFakeBoldText(true);

        msgPaint.setColor(Color.parseColor("#B3FFFFFF"));
        msgPaint.setTextSize(11f * D);

        flowAnim = ValueAnimator.ofFloat(0f, 2f);
        flowAnim.setDuration(5000);
        flowAnim.setRepeatCount(ValueAnimator.INFINITE);
        flowAnim.setInterpolator(new LinearInterpolator());
        flowAnim.addUpdateListener(animation -> {
            flowOffset = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!flowAnim.isStarted()) flowAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        flowAnim.cancel();
    }

    @Override
    protected void onMeasure(int wms, int hms) {
        setMeasuredDimension(W, H);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.set(0f, 0f, (float) w, (float) h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float shift = flowOffset * w;
        LinearGradient gradient = new LinearGradient(shift - w, 0f, shift + w, 0f,
                gradientStart, gradientEnd, Shader.TileMode.CLAMP);

        // 发光
        glowPaint.setShader(gradient);
        glowPaint.setAlpha(30);
        canvas.drawRoundRect(-2f * D, -2f * D, w + 2f * D, h + 2f * D, radius + 2f, radius + 2f, glowPaint);

        // 背景
        canvas.drawRoundRect(rect, radius, radius, bgPaint);

        // 边框
        borderPaint.setShader(gradient);
        borderPaint.setAlpha(255);
        canvas.drawRoundRect(rect, radius, radius, borderPaint);

        glowPaint.setShader(null);
        borderPaint.setShader(null);

        // 文字
        float textX = padH;
        float titleY = h / 2f - 3f * D;
        canvas.drawText(title, textX, titleY, titlePaint);
        float msgY = h / 2f + 10f * D;
        canvas.drawText(message, textX, msgY, msgPaint);
    }
}
