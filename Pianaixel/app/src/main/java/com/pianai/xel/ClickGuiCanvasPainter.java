/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import static com.pianai.xel.ClickGuiLayout.*;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Shared Canvas primitives, controls, text, and module icons. */
final class ClickGuiCanvasPainter {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF rect = new RectF();
    private final Path iconPath = new Path();

    ClickGuiCanvasPainter() {
        textPaint.setTypeface(android.graphics.Typeface.create(
                "sans-serif", android.graphics.Typeface.NORMAL));
    }

    void drawRect(Canvas canvas, float left, float top, float right, float bottom, int color) {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(color);
        canvas.drawRect(left, top, right, bottom, fillPaint);
    }

    void drawRunButton(Canvas canvas, float rowTop) {
        float top = rowTop + (LIST_ROW_HEIGHT - RUN_HEIGHT) * 0.5f;
        drawRounded(canvas, RUN_X, top, RUN_X + RUN_WIDTH, top + RUN_HEIGHT, 9f,
                Color.argb(150, 160, 158, 165));
        drawText(canvas, "Run", RUN_X + RUN_WIDTH * 0.5f, top + RUN_HEIGHT * 0.5f,
                21f, Color.rgb(248, 248, 251), Paint.Align.CENTER, true);
    }

    void drawGearButton(Canvas canvas, float centerX, float centerY) {
        float half = GEAR_BUTTON_SIZE * 0.5f;
        drawRounded(canvas, centerX - half, centerY - half, centerX + half, centerY + half,
                10f, Color.argb(138, 132, 130, 137));
        drawGear(canvas, centerX, centerY, 14f);
    }

    void drawSwitch(Canvas canvas, float x, float y, float width, float height, boolean on) {
        int trackColor = on
                ? Color.argb(206, 132, 130, 142)
                : Color.argb(171, 57, 56, 63);
        drawRounded(canvas, x, y, x + width, y + height, 16f, trackColor);
        float side = height - 12f;
        float knobLeft = on ? x + width - side - 6f : x + 6f;
        drawRounded(canvas, knobLeft, y + 6f, knobLeft + side, y + height - 6f,
                8f, Color.argb(246, 242, 242, 247));
    }

    void drawGear(Canvas canvas, float centerX, float centerY, float radius) {
        iconPath.reset();
        for (int index = 0; index < 16; index++) {
            double angle = -Math.PI / 2d + Math.PI * 2d * index / 16d;
            float gearRadius = index % 2 == 0 ? radius : radius * 0.75f;
            float x = centerX + (float) Math.cos(angle) * gearRadius;
            float y = centerY + (float) Math.sin(angle) * gearRadius;
            if (index == 0) {
                iconPath.moveTo(x, y);
            } else {
                iconPath.lineTo(x, y);
            }
        }
        iconPath.close();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(238, 225, 225, 232));
        canvas.drawPath(iconPath, fillPaint);
        fillPaint.setColor(Color.argb(235, 70, 70, 80));
        canvas.drawCircle(centerX, centerY, radius * 0.34f, fillPaint);
        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStrokeWidth(1.5f);
        fillPaint.setColor(Color.argb(205, 44, 44, 52));
        canvas.drawCircle(centerX, centerY, radius * 0.70f, fillPaint);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    void drawFeatureIcon(Canvas canvas, ClickGuiIconKind icon, float centerX, float centerY) {
        float r = 16f;
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.argb(245, 229, 229, 235));
        switch (icon) {
            case MAGNIFIER:
                fillPaint.setColor(Color.rgb(158, 213, 250));
                canvas.drawCircle(centerX - 3f, centerY - 3f, 10f, fillPaint);
                fillPaint.setStyle(Paint.Style.STROKE);
                fillPaint.setStrokeWidth(3f);
                fillPaint.setColor(Color.WHITE);
                canvas.drawCircle(centerX - 3f, centerY - 3f, 8f, fillPaint);
                canvas.drawLine(centerX + 4f, centerY + 5f, centerX + 15f, centerY + 16f, fillPaint);
                break;
            case BLOCK:
            case CUBE:
                fillPaint.setColor(Color.rgb(184, 166, 111));
                canvas.drawRoundRect(centerX - r, centerY - r, centerX + r, centerY + r, 5f, 5f, fillPaint);
                fillPaint.setStyle(Paint.Style.STROKE);
                fillPaint.setStrokeWidth(2f);
                fillPaint.setColor(Color.rgb(64, 53, 50));
                canvas.drawLine(centerX - r + 4f, centerY - 3f, centerX + r - 4f, centerY - 3f, fillPaint);
                canvas.drawLine(centerX, centerY - r + 4f, centerX, centerY + r - 4f, fillPaint);
                break;
            case TARGET:
                fillPaint.setStyle(Paint.Style.STROKE);
                fillPaint.setStrokeWidth(3f);
                fillPaint.setColor(Color.rgb(225, 228, 239));
                canvas.drawCircle(centerX, centerY, 14f, fillPaint);
                canvas.drawCircle(centerX, centerY, 5f, fillPaint);
                canvas.drawLine(centerX - 19f, centerY, centerX + 19f, centerY, fillPaint);
                canvas.drawLine(centerX, centerY - 19f, centerX, centerY + 19f, fillPaint);
                break;
            case SPARK:
                fillPaint.setColor(Color.rgb(238, 216, 110));
                iconPath.reset();
                iconPath.moveTo(centerX, centerY - 19f);
                iconPath.lineTo(centerX + 5f, centerY - 5f);
                iconPath.lineTo(centerX + 19f, centerY);
                iconPath.lineTo(centerX + 5f, centerY + 5f);
                iconPath.lineTo(centerX, centerY + 19f);
                iconPath.lineTo(centerX - 5f, centerY + 5f);
                iconPath.lineTo(centerX - 19f, centerY);
                iconPath.lineTo(centerX - 5f, centerY - 5f);
                iconPath.close();
                canvas.drawPath(iconPath, fillPaint);
                break;
            case BOLT:
                fillPaint.setColor(Color.rgb(243, 226, 117));
                iconPath.reset();
                iconPath.moveTo(centerX + 2f, centerY - 20f);
                iconPath.lineTo(centerX - 13f, centerY + 2f);
                iconPath.lineTo(centerX - 3f, centerY + 2f);
                iconPath.lineTo(centerX - 7f, centerY + 20f);
                iconPath.lineTo(centerX + 15f, centerY - 7f);
                iconPath.lineTo(centerX + 4f, centerY - 7f);
                iconPath.close();
                canvas.drawPath(iconPath, fillPaint);
                break;
            case EYE:
                fillPaint.setStyle(Paint.Style.STROKE);
                fillPaint.setStrokeWidth(3f);
                fillPaint.setColor(Color.rgb(219, 225, 242));
                rect.set(centerX - 19f, centerY - 11f, centerX + 19f, centerY + 11f);
                canvas.drawOval(rect, fillPaint);
                fillPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(centerX, centerY, 6f, fillPaint);
                break;
            case TRACER:
                fillPaint.setStyle(Paint.Style.STROKE);
                fillPaint.setStrokeWidth(3f);
                fillPaint.setColor(Color.rgb(101, 221, 222));
                canvas.drawRect(centerX - 12f, centerY - 12f, centerX + 12f, centerY + 12f, fillPaint);
                canvas.drawLine(centerX - 19f, centerY + 19f, centerX - 7f, centerY + 7f, fillPaint);
                canvas.drawLine(centerX + 19f, centerY + 19f, centerX + 7f, centerY + 7f, fillPaint);
                break;
            case SWORD:
            case PICK:
                fillPaint.setStyle(Paint.Style.STROKE);
                fillPaint.setStrokeWidth(5f);
                fillPaint.setStrokeCap(Paint.Cap.SQUARE);
                fillPaint.setColor(Color.rgb(239, 239, 245));
                canvas.drawLine(centerX - 12f, centerY + 14f, centerX + 13f, centerY - 14f, fillPaint);
                fillPaint.setColor(Color.rgb(177, 147, 96));
                canvas.drawLine(centerX - 16f, centerY + 17f, centerX - 8f, centerY + 9f, fillPaint);
                fillPaint.setStrokeCap(Paint.Cap.BUTT);
                break;
            case CHEST:
                fillPaint.setColor(Color.rgb(176, 121, 54));
                canvas.drawRoundRect(centerX - 17f, centerY - 12f, centerX + 17f, centerY + 14f, 3f, 3f, fillPaint);
                fillPaint.setColor(Color.rgb(234, 194, 90));
                canvas.drawRect(centerX - 3f, centerY - 1f, centerX + 3f, centerY + 8f, fillPaint);
                break;
            case WINGS:
            case FEATHER:
                fillPaint.setColor(Color.rgb(225, 232, 246));
                iconPath.reset();
                iconPath.moveTo(centerX, centerY + 15f);
                iconPath.quadTo(centerX - 23f, centerY + 7f, centerX - 18f, centerY - 16f);
                iconPath.quadTo(centerX - 7f, centerY - 4f, centerX, centerY + 15f);
                iconPath.quadTo(centerX + 23f, centerY + 7f, centerX + 18f, centerY - 16f);
                iconPath.quadTo(centerX + 7f, centerY - 4f, centerX, centerY + 15f);
                canvas.drawPath(iconPath, fillPaint);
                break;
            case CLOCK:
                fillPaint.setStyle(Paint.Style.STROKE);
                fillPaint.setStrokeWidth(3f);
                fillPaint.setColor(Color.rgb(225, 228, 239));
                canvas.drawCircle(centerX, centerY, 16f, fillPaint);
                canvas.drawLine(centerX, centerY, centerX, centerY - 10f, fillPaint);
                canvas.drawLine(centerX, centerY, centerX + 8f, centerY + 5f, fillPaint);
                break;
            default:
                fillPaint.setColor(Color.rgb(219, 220, 230));
                canvas.drawCircle(centerX, centerY, 13f, fillPaint);
                fillPaint.setColor(Color.rgb(80, 79, 90));
                canvas.drawCircle(centerX, centerY, 4f, fillPaint);
                break;
        }
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setStrokeCap(Paint.Cap.BUTT);
    }

    void drawSeparator(Canvas canvas, float left, float y, float right) {
        fillPaint.setColor(Color.argb(80, 230, 230, 236));
        canvas.drawRect(left, y, right, y + 1.5f, fillPaint);
    }

    void drawRounded(Canvas canvas, float left, float top, float right, float bottom,
                             float radius, int color) {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(color);
        rect.set(left, top, right, bottom);
        canvas.drawRoundRect(rect, radius, radius, fillPaint);
    }

    void drawRoundedStroke(Canvas canvas, float left, float top, float right,
                                   float bottom, float radius, float strokeWidth, int color) {
        fillPaint.setStyle(Paint.Style.STROKE);
        fillPaint.setStrokeWidth(strokeWidth);
        fillPaint.setColor(color);
        rect.set(left + strokeWidth * 0.5f, top + strokeWidth * 0.5f,
                right - strokeWidth * 0.5f, bottom - strokeWidth * 0.5f);
        canvas.drawRoundRect(rect, radius - strokeWidth * 0.5f, radius - strokeWidth * 0.5f,
                fillPaint);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    void drawPixelText(Canvas canvas, String value, float x, float centerY, float size,
                               int color, Paint.Align align, boolean bold) {
        textPaint.setTypeface(android.graphics.Typeface.create("monospace",
                bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(align);
        textPaint.setAntiAlias(false);
        textPaint.setSubpixelText(false);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = centerY - (metrics.ascent + metrics.descent) * 0.5f;
        canvas.drawText(value, x, baseline, textPaint);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
    }

    float measurePixelText(String value, float size, boolean bold) {
        textPaint.setTypeface(android.graphics.Typeface.create("monospace",
                bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        textPaint.setTextSize(size);
        textPaint.setAntiAlias(false);
        textPaint.setSubpixelText(false);
        float measured = textPaint.measureText(value);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        return measured;
    }

    void drawText(Canvas canvas, String value, float x, float centerY, float size,
                          int color, Paint.Align align, boolean bold) {
        textPaint.setTypeface(android.graphics.Typeface.create(
                bold ? "sans-serif-medium" : "sans-serif",
                android.graphics.Typeface.NORMAL
        ));
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(align);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = centerY - (metrics.ascent + metrics.descent) * 0.5f;
        canvas.drawText(value, x, baseline, textPaint);
    }

    float measureText(String value, float size, boolean bold) {
        textPaint.setTypeface(android.graphics.Typeface.create(
                bold ? "sans-serif-medium" : "sans-serif",
                android.graphics.Typeface.NORMAL
        ));
        textPaint.setTextSize(size);
        return textPaint.measureText(value);
    }

}
