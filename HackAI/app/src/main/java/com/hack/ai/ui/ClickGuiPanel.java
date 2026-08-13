/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hack.ai.data.OnGlassChangeListener;
import com.hack.ai.data.ThemeManager;

public class ClickGuiPanel extends LinearLayout implements OnGlassChangeListener {

    private float glassProgress = 0f;
    private float easedProgress = 0f;

    // ---- 多层光效 Paint ----
    private final Paint outerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mainStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint noisePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    // ---- 噪声纹理 ----
    private android.graphics.Bitmap noiseBitmap;
    private boolean noiseReady = false;

    public ClickGuiPanel(Context context) {
        this(context, null);
    }

    public ClickGuiPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setAlpha(0f);
        setScaleX(0f);
        setScaleY(0f);
        setVisibility(View.GONE);
        setWillNotDraw(false);

        // 外层辉光：白色 + OUTER 模糊
        outerGlowPaint.setStyle(Paint.Style.STROKE);
        outerGlowPaint.setStrokeWidth(6f);
        outerGlowPaint.setColor(Color.WHITE);
        outerGlowPaint.setMaskFilter(new BlurMaskFilter(8f, BlurMaskFilter.Blur.OUTER));

        // 主描边：2px 白色
        mainStrokePaint.setStyle(Paint.Style.STROKE);
        mainStrokePaint.setStrokeWidth(2f);
        mainStrokePaint.setColor(Color.WHITE);
        mainStrokePaint.setAntiAlias(true);

        // 内发光：白色 + INNER 模糊
        innerGlowPaint.setStyle(Paint.Style.STROKE);
        innerGlowPaint.setStrokeWidth(3f);
        innerGlowPaint.setColor(Color.WHITE);
        innerGlowPaint.setMaskFilter(new BlurMaskFilter(4f, BlurMaskFilter.Blur.INNER));

        // 顶部高光：细线模拟光线折射
        topHighlightPaint.setStyle(Paint.Style.STROKE);
        topHighlightPaint.setStrokeWidth(1.5f);
        topHighlightPaint.setColor(Color.WHITE);
        topHighlightPaint.setAntiAlias(true);

        // 噪声
        noisePaint.setAntiAlias(true);

        glassProgress = ThemeManager.getGlassProgress();
        easedProgress = ThemeManager.glassEasedProgress();
        ThemeManager.addOnGlassChangeListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ThemeManager.removeOnGlassChangeListener(this);
        if (noiseBitmap != null) {
            noiseBitmap.recycle();
            noiseBitmap = null;
        }
    }

    // ==================== Glass 回调 ====================

    @Override
    public void onGlassProgress(float progress) {
        glassProgress = progress;
        easedProgress = ThemeManager.glassEasedProgress();

        // 仅 progress > 0.05 时执行重操作
        if (progress > 0.05f || (glassProgress > 0f && progress == 0f)) {
            applyGlassToTree(this, progress);
        }
        invalidate();
    }

    /** 递归遍历 View 树，对带背景的 View 施加玻璃化 alpha */
    private void applyGlassToTree(ViewGroup root, float progress) {
        float eased = ThemeManager.glassEasedProgress();
        int count = root.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = root.getChildAt(i);
            Drawable bg = child.getBackground();
            if (bg != null) {
                int targetAlpha = (int) (ThemeManager.GLASS_ALPHA_MAX
                        + (ThemeManager.GLASS_ALPHA_MIN - ThemeManager.GLASS_ALPHA_MAX) * eased);
                bg.setAlpha(targetAlpha);
            }
            // 文字阴影
            if (child instanceof TextView) {
                float shadowRadius = 1f * eased;
                ((TextView) child).setShadowLayer(shadowRadius, 0f, 1f,
                        ((int) (0x40 * eased) << 24) | 0x000000);
            }
            if (child instanceof ViewGroup) {
                applyGlassToTree((ViewGroup) child, progress);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        post(() -> {
            if (glassProgress > 0f) applyGlassToTree(this, glassProgress);
        });
    }

    // ==================== 入场/退场动画 ====================

    public void playReveal() {
        setPivotX(getWidth() / 2f);
        setPivotY(getHeight() / 2f);
        setVisibility(View.VISIBLE);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(this, ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(this, SCALE_X, 0f, 1f),
                ObjectAnimator.ofFloat(this, SCALE_Y, 0f, 1f)
        );
        set.setDuration(350);
        set.setInterpolator(new OvershootInterpolator(1.1f));
        set.start();
        postDelayed(() -> {
            if (glassProgress > 0f) applyGlassToTree(this, glassProgress);
        }, 360);
    }

    public void playHide(Runnable onEnd) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(this, ALPHA, 1f, 0f),
                ObjectAnimator.ofFloat(this, SCALE_X, 1f, 0f),
                ObjectAnimator.ofFloat(this, SCALE_Y, 1f, 0f)
        );
        set.setDuration(250);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                setVisibility(View.GONE);
                onEnd.run();
            }
        });
        set.start();
    }

    // ==================== 毛玻璃渲染 ====================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (easedProgress <= 0.01f) return;

        float w = getWidth();
        float h = getHeight();
        float r = 12f + 4f * easedProgress; // 圆角：12dp → 16dp
        rect.set(1f, 1f, w - 1f, h - 1f);

        // 1. 外层辉光
        outerGlowPaint.setAlpha((int) (40 * easedProgress));
        canvas.drawRoundRect(rect, r, r, outerGlowPaint);

        // 2. 主描边
        mainStrokePaint.setAlpha((int) (180 * easedProgress));
        canvas.drawRoundRect(rect, r, r, mainStrokePaint);

        // 3. 内发光（缩进 2px）
        rect.set(3f, 3f, w - 3f, h - 3f);
        innerGlowPaint.setAlpha((int) (60 * easedProgress));
        canvas.drawRoundRect(rect, r, r, innerGlowPaint);

        // 4. 顶部高光线
        topHighlightPaint.setAlpha((int) (120 * easedProgress));
        float hlY = h * 0.25f;
        canvas.drawLine(0f, hlY, w, hlY, topHighlightPaint);

        // 5. 噪声纹理
        drawNoiseOverlay(canvas, w, h, r);
    }

    /** 叠加极淡噪声纹理 */
    private void drawNoiseOverlay(Canvas canvas, float w, float h, float r) {
        if (!noiseReady || noiseBitmap == null) {
            noiseBitmap = generateNoise((int) w, (int) h);
            noiseReady = true;
        }
        noisePaint.setAlpha((int) (10 * easedProgress));
        rect.set(0f, 0f, w, h);
        // clip 圆角
        canvas.save();
        canvas.clipRect(rect);
        android.graphics.Path path = new android.graphics.Path();
        path.addRoundRect(rect, r, r, android.graphics.Path.Direction.CW);
        canvas.clipPath(path);
        if (noiseBitmap != null) {
            canvas.drawBitmap(noiseBitmap, 0f, 0f, noisePaint);
        }
        canvas.restore();
    }

    /** 程序化生成 128x128 噪声位图 */
    private android.graphics.Bitmap generateNoise(int w, int h) {
        int size = Math.min(Math.max(w, h), 128);
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                size, size, android.graphics.Bitmap.Config.ARGB_8888);
        java.util.Random rng = new java.util.Random(42);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int gray = rng.nextInt(256);
                bmp.setPixel(x, y, (gray << 16) | (gray << 8) | gray | 0xFF000000);
            }
        }
        return bmp;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            if (noiseBitmap != null) {
                noiseBitmap.recycle();
            }
            noiseBitmap = generateNoise(w, h);
            noiseReady = true;
        }
    }
}
