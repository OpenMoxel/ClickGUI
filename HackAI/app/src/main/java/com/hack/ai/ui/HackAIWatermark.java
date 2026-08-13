/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hack.ai.R;
import com.hack.ai.data.ThemeManager;

/**
 * 左上角水印 View：显示版本信息与官方群号，带流光边框动画与光污染背景。
 *
 * <p>支持 XML 属性自定义：
 * <ul>
 *   <li>app:watermarkText — 水印文本内容</li>
 *   <li>app:watermarkTextSize — 文本尺寸（sp）</li>
 *   <li>app:watermarkTextColor — 文本颜色</li>
 *   <li>app:watermarkBgColor — 背景颜色</li>
 *   <li>app:watermarkCornerRadius — 圆角半径（dp）</li>
 *   <li>app:watermarkBarWidth — 左侧 bar 宽度（dp）</li>
 *   <li>app:watermarkStrokeWidth — 流光边框宽度（dp）</li>
 *   <li>app:watermarkAnimationDuration — 动画周期（ms）</li>
 * </ul>
 */
public class HackAIWatermark extends View {

    // ==================== 默认常量 ====================
    private static final String DEFAULT_TEXT = "HackAI_v26.x | 官方群:784082313";
    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    // 背景带透明度：#2F2F2F → 0xCC2F2F2F (约 80% 不透明)
    private static final int DEFAULT_BG_COLOR = 0xCC2F2F2F;
    private static final float DEFAULT_TEXT_SIZE_SP = 12f;
    private static final float DEFAULT_CORNER_RADIUS_DP = 6f;
    private static final float DEFAULT_BAR_WIDTH_DP = 3f;
    private static final float DEFAULT_STROKE_WIDTH_DP = 2.2f;
    private static final long DEFAULT_ANIM_DURATION = 1600L;

    // 减少两边留白：18f → 10f
    private static final float PADDING_HORIZONTAL_DP = 10f;
    private static final float PADDING_VERTICAL_DP = 4f;
    private static final float BAR_MARGIN_START_DP = 6f;
    private static final float BAR_HEIGHT_RATIO = 0.65f;
    private static final float TEXT_BASELINE_OFFSET_RATIO = 1f / 3f;

    // ==================== Paint ====================
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // 光污染背景 Paint
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ==================== 尺寸缓存 ====================
    private final RectF bgRect = new RectF();
    private final RectF strokeRect = new RectF();
    private final RectF barRect = new RectF();
    private final RectF glowRect = new RectF();

    // ==================== Shader 缓存 ====================
    @Nullable
    private LinearGradient strokeShader;
    private int lastShaderWidth;
    private int lastShaderHeight;

    // ==================== 配置属性 ====================
    @NonNull
    private String watermarkText = DEFAULT_TEXT;
    private float textSizePx;
    private int textColor;
    private int bgColor;
    private float cornerRadiusPx;
    private float barWidthPx;
    private float strokeWidthPx;
    private long animationDuration;

    // ==================== 动画 ====================
    private float phase = 0f;
    @Nullable
    private ValueAnimator animator;

    // ==================== 构造 ====================

    public HackAIWatermark(@NonNull Context context) {
        this(context, null);
    }

    public HackAIWatermark(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HackAIWatermark(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.HackAIWatermark, defStyleAttr, 0);
            try {
                watermarkText = ta.getString(R.styleable.HackAIWatermark_watermarkText);
                if (watermarkText == null) watermarkText = DEFAULT_TEXT;

                textSizePx = ta.getDimension(
                        R.styleable.HackAIWatermark_watermarkTextSize,
                        spToPx(context, DEFAULT_TEXT_SIZE_SP)
                );
                textColor = ta.getColor(R.styleable.HackAIWatermark_watermarkTextColor, DEFAULT_TEXT_COLOR);
                bgColor = ta.getColor(R.styleable.HackAIWatermark_watermarkBgColor, DEFAULT_BG_COLOR);
                cornerRadiusPx = ta.getDimension(
                        R.styleable.HackAIWatermark_watermarkCornerRadius,
                        dpToPx(context, DEFAULT_CORNER_RADIUS_DP)
                );
                barWidthPx = ta.getDimension(
                        R.styleable.HackAIWatermark_watermarkBarWidth,
                        dpToPx(context, DEFAULT_BAR_WIDTH_DP)
                );
                strokeWidthPx = ta.getDimension(
                        R.styleable.HackAIWatermark_watermarkStrokeWidth,
                        dpToPx(context, DEFAULT_STROKE_WIDTH_DP)
                );
                animationDuration = ta.getInteger(
                        R.styleable.HackAIWatermark_watermarkAnimationDuration,
                        (int) DEFAULT_ANIM_DURATION
                );
            } finally {
                ta.recycle();
            }
        } else {
            textSizePx = spToPx(context, DEFAULT_TEXT_SIZE_SP);
            textColor = DEFAULT_TEXT_COLOR;
            bgColor = DEFAULT_BG_COLOR;
            cornerRadiusPx = dpToPx(context, DEFAULT_CORNER_RADIUS_DP);
            barWidthPx = dpToPx(context, DEFAULT_BAR_WIDTH_DP);
            strokeWidthPx = dpToPx(context, DEFAULT_STROKE_WIDTH_DP);
            animationDuration = DEFAULT_ANIM_DURATION;
        }

        bgPaint.setColor(bgColor);
        bgPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(textColor);
        textPaint.setTextSize(textSizePx);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setAntiAlias(true);

        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setAntiAlias(true);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidthPx);
        strokePaint.setAntiAlias(true);

        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setAntiAlias(true);

        setupAnimator();
    }

    private void setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(animationDuration);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            phase = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    // ==================== 公共 API ====================

    public void setWatermarkText(@NonNull String text) {
        if (!watermarkText.equals(text)) {
            watermarkText = text;
            requestLayout();
        }
    }

    @NonNull
    public String getWatermarkText() {
        return watermarkText;
    }

    public void setTextColor(@ColorInt int color) {
        if (textColor != color) {
            textColor = color;
            textPaint.setColor(color);
            invalidate();
        }
    }

    public void setBgColor(@ColorInt int color) {
        if (bgColor != color) {
            bgColor = color;
            bgPaint.setColor(color);
            invalidate();
        }
    }

    public void setAnimationDuration(long durationMs) {
        if (animationDuration != durationMs && animator != null) {
            animationDuration = durationMs;
            animator.setDuration(durationMs);
        }
    }

    // ==================== 生命周期 ====================

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isRunning()) {
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }

    // ==================== 测量 ====================

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float textWidth = textPaint.measureText(watermarkText);
        float desiredWidth = textWidth + dpToPx(getContext(), PADDING_HORIZONTAL_DP * 2)
                + barWidthPx + dpToPx(getContext(), BAR_MARGIN_START_DP * 2);
        float desiredHeight = textSizePx + dpToPx(getContext(), PADDING_VERTICAL_DP * 2);

        int width = resolveSize((int) Math.ceil(desiredWidth), widthMeasureSpec);
        int height = resolveSize((int) Math.ceil(desiredHeight), heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            strokeShader = null;
        }
    }

    // ==================== 绘制 ====================

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float w = getWidth();
        float h = getHeight();

        // 0. 光污染背景（最底层）
        drawGlowBackground(canvas, w, h);

        // 1. 背景
        bgRect.set(0f, 0f, w, h);
        canvas.drawRoundRect(bgRect, cornerRadiusPx, cornerRadiusPx, bgPaint);

        // 2. 左侧静止 bar（Light MD3 风格）
        drawStaticBar(canvas, w, h);

        // 3. 流光边框
        drawFlowingStroke(canvas, w, h);

        // 4. 文本
        drawText(canvas, w, h);
    }

    /**
     * 光污染背景：RadialGradient 呼吸辉光，中心点随 phase 轻微摆动。
     */
    private void drawGlowBackground(@NonNull Canvas canvas, float w, float h) {
        int accent = ThemeManager.accentColor;
        int r = Color.red(accent);
        int g = Color.green(accent);
        int b = Color.blue(accent);

        // 呼吸 alpha：8% ~ 14%
        float glowAlpha = 0.08f + 0.06f * (float) Math.sin(phase * Math.PI * 2);
        float glowRadius = Math.max(w, h) * (0.8f + 0.2f * (float) Math.sin(phase * Math.PI * 2));

        int centerColor = Color.argb((int) (glowAlpha * 255), r, g, b);
        int edgeColor = Color.TRANSPARENT;

        // 中心点轻微摆动，制造流动感
        float centerX = w / 2f + (float) Math.sin(phase * Math.PI * 2) * w * 0.15f;
        float centerY = h / 2f + (float) Math.cos(phase * Math.PI * 2) * h * 0.15f;

        Shader glowShader = new RadialGradient(
                centerX, centerY,
                glowRadius,
                new int[]{centerColor, edgeColor},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );
        glowPaint.setShader(glowShader);

        float glowPad = glowRadius * 0.5f;
        glowRect.set(-glowPad, -glowPad, w + glowPad, h + glowPad);
        canvas.drawRoundRect(glowRect, cornerRadiusPx, cornerRadiusPx, glowPaint);

        glowPaint.setShader(null);
    }

    /**
     * 左侧静止 bar，颜色经 lightenForMd3() 提亮以适配 Light MD3。
     */
    private void drawStaticBar(@NonNull Canvas canvas, float w, float h) {
        float barHeight = h * BAR_HEIGHT_RATIO;
        float barY = (h - barHeight) / 2f;

        int lightAccent = lightenForMd3(ThemeManager.accentColor);
        barPaint.setColor(lightAccent);
        barPaint.setAlpha(255); // 静止，固定不透明度

        float barX = dpToPx(getContext(), BAR_MARGIN_START_DP);

        barRect.set(barX, barY, barX + barWidthPx, barY + barHeight);
        canvas.drawRoundRect(barRect, barWidthPx / 2f, barWidthPx / 2f, barPaint);
    }

    /**
     * 将 accent 色提亮、略增饱和，适配 Light MD3 风格。
     */
    private int lightenForMd3(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(1f, hsv[1] * 1.15f);
        hsv[2] = Math.min(1f, hsv[2] * 1.25f);
        return Color.HSVToColor(hsv);
    }

    private void drawFlowingStroke(@NonNull Canvas canvas, float w, float h) {
        int lightAccent = lightenForMd3(ThemeManager.accentColor);

        if (strokeShader == null || w != lastShaderWidth || h != lastShaderHeight) {
            lastShaderWidth = (int) w;
            lastShaderHeight = (int) h;
        }

        float startX = -w + (w * 2 * phase);
        float endX = w * phase;

        strokeShader = new LinearGradient(
                startX, 0f,
                endX, h,
                new int[]{Color.TRANSPARENT, lightAccent, Color.TRANSPARENT},
                new float[]{0.2f, 0.5f, 0.8f},
                Shader.TileMode.CLAMP
        );

        strokePaint.setShader(strokeShader);

        float halfStroke = strokeWidthPx / 2f;
        strokeRect.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke);
        canvas.drawRoundRect(strokeRect, cornerRadiusPx, cornerRadiusPx, strokePaint);
    }

    private void drawText(@NonNull Canvas canvas, float w, float h) {
        float textX = dpToPx(getContext(), PADDING_HORIZONTAL_DP)
                + barWidthPx + dpToPx(getContext(), BAR_MARGIN_START_DP);
        float textY = h / 2f + textPaint.getTextSize() * TEXT_BASELINE_OFFSET_RATIO;
        canvas.drawText(watermarkText, textX, textY, textPaint);
    }

    // ==================== 工具方法 ====================

    private static float dpToPx(@NonNull Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    private static float spToPx(@NonNull Context context, float sp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }
}