/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Minecraft 成就风格 Toast
 * - 金色边框，深色背景
 * - Icon(30dp) + Title(黄) + Description(白)
 * - 滑入 animationX: +2000→0, Overshoot
 * - 停留 3000ms, 滑出
 */
public class McToastView extends View {

    private final float D = getResources().getDisplayMetrics().density;
    private final int W = (int) (280 * D);
    private final int H = (int) (56 * D);

    public String title = "";
    public String desc = "";
    public Runnable onAnimEnd = null;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint descPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public McToastView(Context context) {
        this(context, null);
    }

    public McToastView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public McToastView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        bgPaint.setColor(Color.parseColor("#E6212121"));
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(Color.parseColor("#D9A334"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f * D);

        titlePaint.setColor(Color.parseColor("#D9A334"));
        titlePaint.setTextSize(13f * D);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);

        descPaint.setColor(Color.WHITE);
        descPaint.setTextSize(11f * D);
    }

    @Override
    protected void onMeasure(int wms, int hms) {
        setMeasuredDimension(W, H);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        rect.set(0f, 0f, (float) w, (float) h);
    }

    public void show(Runnable onDone) {
        setTranslationX(2000f * D);
        animate().translationX(0f).setDuration(600).setInterpolator(new OvershootInterpolator(0.8f))
                .withEndAction(() -> handler.postDelayed(() ->
                        animate().translationX(W + 100 * D).setDuration(500)
                                .setInterpolator(new AccelerateInterpolator())
                                .withEndAction(() -> {
                                    if (onDone != null) onDone.run();
                                }).start(), 3000))
                .start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 背景
        canvas.drawRoundRect(rect, 2f * D, 2f * D, bgPaint);
        canvas.drawRoundRect(rect, 2f * D, 2f * D, borderPaint);
        // icon
        float cx = 22f * D;
        float cy = H / 2f;
        borderPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 15f * D, borderPaint);
        borderPaint.setStyle(Paint.Style.STROKE);
        // 文字
        canvas.drawText(title, 46f * D, cy - 2f * D, titlePaint);
        canvas.drawText(desc, 46f * D, cy + 15f * D, descPaint);
    }
}
