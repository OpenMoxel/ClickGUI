package com.pixelsight.hud;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.pixelsight.gui.ClickGuiView;

public class ShortcutView extends View {
    private Paint paint, textPaint, strokePaint;
    
    private ClickGuiView.Module draggingMod = null;
    private float touchDx, touchDy, initialX, initialY;
    private boolean isDragging = false;
    private boolean isLongPressed = false;
    private int touchSlop;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable longPressRunnable = () -> {
        if (draggingMod != null) {
            isLongPressed = true;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); 
        }
    };

    public ShortcutView(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(4f); // 鲜艳描边宽度
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX(), ty = event.getY();
        if (ClickGuiView.allModules == null) return super.onTouchEvent(event);

        float scale = ClickGuiView.shortcutSize == 0 ? 0.8f : (ClickGuiView.shortcutSize == 1 ? 1.0f : 1.25f);
        float h = 50f * scale;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (int i = ClickGuiView.allModules.size() - 1; i >= 0; i--) {
                    ClickGuiView.Module m = ClickGuiView.allModules.get(i);
                    if (m.showShortcut) {
                        textPaint.setTextSize(26f * scale);
                        float textW = textPaint.measureText(ClickGuiView.isEnglish ? m.nameEn : m.nameZh);
                        RectF r = new RectF(m.shortcutX, m.shortcutY, m.shortcutX + textW + (40f * scale), m.shortcutY + h);
                        
                        if (r.contains(tx, ty)) {
                            draggingMod = m; initialX = tx; initialY = ty;
                            touchDx = tx - m.shortcutX; touchDy = ty - m.shortcutY;
                            isDragging = false; isLongPressed = false;
                            handler.postDelayed(longPressRunnable, 350); 
                            return true; 
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (draggingMod != null) {
                    if (!isLongPressed && Math.hypot(tx - initialX, ty - initialY) > touchSlop) {
                        handler.removeCallbacks(longPressRunnable); draggingMod = null; 
                    } else if (isLongPressed) {
                        isDragging = true;
                        // 防止快捷键被拖出屏幕之外
                        float nx = tx - touchDx; 
                        float ny = ty - touchDy;
                        nx = Math.max(0, Math.min(nx, getWidth() - 150f));
                        ny = Math.max(0, Math.min(ny, getHeight() - h - 50f));
                        draggingMod.shortcutX = nx; 
                        draggingMod.shortcutY = ny;
                        invalidate();
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                if (draggingMod != null) {
                    if (!isLongPressed && !isDragging) {
                        ClickGuiView.toggleModule(draggingMod); // 完美同步开关
                    }
                    draggingMod = null; isLongPressed = false; isDragging = false;
                    invalidate(); return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (ClickGuiView.allModules == null) return;

        boolean isLight = ClickGuiView.isLightTheme;
        float scale = ClickGuiView.shortcutSize == 0 ? 0.8f : (ClickGuiView.shortcutSize == 1 ? 1.0f : 1.25f);
        float h = 50f * scale;
        int idx = 0; 
        
        // 核心修复：独立判断当前主题并给予最高级的色彩
        // 暗色主题：与呼吸悬浮球相匹配的高定紫粉色；亮色主题：科技蓝
        int accentColor = isLight ? Color.parseColor("#007AFF") : Color.parseColor("#B5179E");

        for (ClickGuiView.Module m : ClickGuiView.allModules) {
            if (m.showShortcut) {
                String name = ClickGuiView.isEnglish ? m.nameEn : m.nameZh;
                textPaint.setTextSize(26f * scale); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                float textW = textPaint.measureText(name);
                
                if (m.shortcutX == -1f) {
                    m.shortcutX = getWidth() / 2f - (textW + 40f * scale) / 2f; 
                    m.shortcutY = getHeight() / 2f + (idx * (h + 15f)) - 100f; 
                }
                
                RectF r = new RectF(m.shortcutX, m.shortcutY, m.shortcutX + textW + (40f * scale), m.shortcutY + h);
                
                paint.setColor(isLight ? Color.parseColor("#E6FFFFFF") : Color.parseColor("#E6121212"));
                if(isLight) paint.setShadowLayer(10f, 0, 5f, Color.parseColor("#15000000"));
                canvas.drawRoundRect(r, h/2f, h/2f, paint); paint.clearShadowLayer();

                if (m.enabled) {
                    strokePaint.setColor(accentColor); 
                    // 高级外发光阴影
                    strokePaint.setShadowLayer(10f, 0, 0, accentColor);
                    canvas.drawRoundRect(r, h/2f, h/2f, strokePaint); 
                    strokePaint.clearShadowLayer();
                    textPaint.setColor(accentColor);
                } else {
                    textPaint.setColor(isLight ? Color.parseColor("#55555A") : Color.parseColor("#A0A0A0"));
                }

                Paint.FontMetrics fm = textPaint.getFontMetrics();
                float textY = r.centerY() - (fm.descent + fm.ascent) / 2f;
                canvas.drawText(name, r.left + (20f * scale), textY, textPaint);
                idx++;
            }
        }
    }
}
