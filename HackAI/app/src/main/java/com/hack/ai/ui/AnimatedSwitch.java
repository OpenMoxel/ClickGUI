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
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.function.Consumer;

public class AnimatedSwitch extends View {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float progress = 0f;
    private boolean isChecked = false;
    public Consumer<Boolean> onCheckedChange = null;

    public AnimatedSwitch(Context context) {
        this(context, null);
    }

    public AnimatedSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        thumbPaint.setColor(Color.WHITE);
        thumbPaint.setShadowLayer(5f, 0f, 1f, Color.parseColor("#66333A44"));
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setOnClickListener(v -> setChecked(!isChecked, true, true));
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked, boolean animated) {
        setChecked(checked, animated, false);
    }

    public void setChecked(boolean checked, boolean animated, boolean notify) {
        if (checked == isChecked && progress == (checked ? 1f : 0f)) return;
        isChecked = checked;
        float target = checked ? 1f : 0f;
        if (animated) {
            ValueAnimator animator = ValueAnimator.ofFloat(progress, target);
            animator.setDuration(220);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(it -> {
                progress = (float) it.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            progress = target;
            invalidate();
        }
        if (notify && onCheckedChange != null) onCheckedChange.accept(checked);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0f, 0f, (float) getWidth(), (float) getHeight());
        if (progress > 0f) {
            int c = 0xFF007AFF;
            trackPaint.setShader(new LinearGradient(0f, 0f, (float) getWidth(), 0f, c, c, Shader.TileMode.CLAMP));
        } else {
            trackPaint.setShader(null);
        }
        trackPaint.setColor(Color.parseColor("#4D545D69"));
        canvas.drawRoundRect(rect, getHeight() / 2f, getHeight() / 2f, trackPaint);
        float thumbRadius = getHeight() / 2f - 3f;
        float x = 3f + thumbRadius + Math.max(getWidth() - getHeight(), 0) * progress;
        canvas.drawCircle(x, getHeight() / 2f, thumbRadius, thumbPaint);
    }
}
