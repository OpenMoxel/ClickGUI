/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.overlay;

import android.graphics.Canvas;
import android.graphics.PointF;

public final class ReferenceViewport {
    private float scale = 1f;
    private float offsetX;
    private float offsetY;
    private int viewWidth;
    private int viewHeight;
    private final PointF reusablePoint = new PointF();

    public void update(int width, int height) {
        viewWidth = width;
        viewHeight = height;
        scale = Math.min(width / LiquidBounceUiMetrics.CONTENT_WIDTH,
                height / LiquidBounceUiMetrics.CONTENT_HEIGHT);
        float renderedWidth = LiquidBounceUiMetrics.CONTENT_WIDTH * scale;
        float renderedHeight = LiquidBounceUiMetrics.CONTENT_HEIGHT * scale;
        offsetX = (width - renderedWidth) * 0.5f;
        offsetY = (height - renderedHeight) * 0.5f;
    }

    public void apply(Canvas canvas) {
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);
    }

    public PointF toLogical(float screenX, float screenY) {
        reusablePoint.set((screenX - offsetX) / scale, (screenY - offsetY) / scale);
        return reusablePoint;
    }

    public float getScale() { return scale; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public int getViewWidth() { return viewWidth; }
    public int getViewHeight() { return viewHeight; }
}
