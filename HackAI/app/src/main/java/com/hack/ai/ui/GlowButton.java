/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.hack.ai.R;

public class GlowButton extends View {

    private final float D = getResources().getDisplayMetrics().density;
    private final float radius = 4f * D;
    private final RectF rect = new RectF();

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap hack_aiBitmap;
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public GlowButton(Context context) {
        this(context, null);
    }

    public GlowButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        bgPaint.setColor((int) 0xFF1A0B2EL);
        bgPaint.setStyle(Paint.Style.FILL);
        hack_aiBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hack_ai);
    }

    /** 切换激活/休眠视觉状态：激活时亮色，关闭时恢复默认 */
    public void setActive(boolean active) {
        bgPaint.setColor(active ? (int) 0xFF3D1F7EL : (int) 0xFF1A0B2EL);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect.set(0f, 0f, (float) w, (float) h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 圆角矩形背景
        canvas.drawRoundRect(rect, radius, radius, bgPaint);

        // 居中绘制 hack_ai.png，保持原始比例（fitCenter）
        if (hack_aiBitmap != null) {
            float bw = hack_aiBitmap.getWidth();
            float bh = hack_aiBitmap.getHeight();
            if (bw > 0 && bh > 0) {
                float pad = 6f * D;
                float drawW = getWidth() - pad * 2f;
                float drawH = getHeight() - pad * 2f;
                float scale = Math.min(drawW / bw, drawH / bh);
                float tw = bw * scale;
                float th = bh * scale;
                float left = (getWidth() - tw) / 2f;
                float top = (getHeight() - th) / 2f;
                canvas.drawBitmap(hack_aiBitmap, null,
                        new RectF(left, top, left + tw, top + th), bitmapPaint);
            }
        }
    }
}
