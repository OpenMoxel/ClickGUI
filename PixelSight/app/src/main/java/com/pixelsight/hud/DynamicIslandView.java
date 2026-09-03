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

import com.pixelsight.gui.ClickGuiView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DynamicIslandView extends View {
    private Paint paint, textPaint, iconPaint;
    private float animProgress = 0f; 
    private RectF pillRect = new RectF();

    private static class IslandNotif {
        String modName; boolean enabled; long startTime;
        IslandNotif(String m, boolean e) { modName = m; enabled = e; startTime = System.currentTimeMillis(); }
    }
    private static List<IslandNotif> queue = new ArrayList<>();

    public static Runnable onLongPressListener;
    private boolean isTouchingPill = false;
    private float touchDownX, touchDownY;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable longPressRunnable = () -> {
        if (isTouchingPill && onLongPressListener != null) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            onLongPressListener.run();
            isTouchingPill = false;
        }
    };

    public DynamicIslandView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG); textPaint = new Paint(Paint.ANTI_ALIAS_FLAG); iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setStyle(Paint.Style.STROKE); iconPaint.setStrokeWidth(5f); iconPaint.setStrokeCap(Paint.Cap.ROUND); iconPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public static void push(String modName, boolean enabled) {
        ClickGuiView.Module islandMod = ClickGuiView.getModule("DynamicIsland");
        if (islandMod != null && islandMod.enabled) {
            int showAlerts = ((ClickGuiView.Mode) islandMod.settings.get(1)).current;
            if (showAlerts == 1) {
                queue.add(new IslandNotif(modName, enabled));
                if (queue.size() > 5) queue.remove(0); 
            }
        }
    }

    public float getIslandCenterX() { return pillRect.centerX(); }
    public float getIslandCenterY() { return pillRect.centerY(); }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (pillRect.contains(x, y)) {
                    isTouchingPill = true; touchDownX = x; touchDownY = y;
                    handler.postDelayed(longPressRunnable, 400); return true; 
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isTouchingPill && (Math.abs(x - touchDownX) > 15 || Math.abs(y - touchDownY) > 15)) { isTouchingPill = false; handler.removeCallbacks(longPressRunnable); }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouchingPill = false; handler.removeCallbacks(longPressRunnable); break;
        }
        return false; 
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ClickGuiView.Module islandMod = ClickGuiView.getModule("DynamicIsland");
        if (islandMod == null || !islandMod.enabled) return;

        int mode = ((ClickGuiView.Mode) islandMod.settings.get(0)).current; 
        boolean needsUpdate = false;
        long time = System.currentTimeMillis();

        Iterator<IslandNotif> it = queue.iterator();
        while(it.hasNext()) { if(time - it.next().startTime > 2000) it.remove(); }

        float targetProgress = queue.isEmpty() ? 0f : 1f;

        if (Math.abs(animProgress - targetProgress) > 0.01f) {
            animProgress += (targetProgress - animProgress) * 0.15f; needsUpdate = true;
        } else {
            animProgress = targetProgress; if (animProgress > 0) needsUpdate = true;
        }

        float cx = getWidth() / 2f; float top = 30f; 
        float baseWidth = mode == 1 ? 400f : 140f; float baseHeight = mode == 1 ? 55f : 45f;
        float expandWidth = 460f; 
        float activeExpandHeight = Math.max(1, queue.size()) * 65f + 25f; 

        float curW = baseWidth + (expandWidth - baseWidth) * animProgress;
        float curH = baseHeight + (activeExpandHeight - baseHeight) * animProgress;

        pillRect.set(cx - curW / 2f, top, cx + curW / 2f, top + curH);

        // 绝对高级暗黑质感，脱离主题变换
        paint.setColor(Color.parseColor("#99000000")); // 高透暗黑
        paint.setShadowLayer(25f, 0, 10f, Color.parseColor("#60000000"));
        canvas.drawRoundRect(pillRect, 40f, 40f, paint);
        paint.clearShadowLayer();

        if (mode == 1 && animProgress < 0.9f) {
            float alpha = 1f - animProgress;
            paint.setColor(Color.parseColor("#007AFF")); paint.setAlpha((int)(255 * alpha));
            canvas.drawCircle(pillRect.left + 35f, pillRect.centerY(), 18f, paint);
            
            textPaint.setColor(Color.WHITE); textPaint.setAlpha((int)(255 * alpha));
            textPaint.setTextSize(24f); textPaint.setTypeface(Typeface.DEFAULT_BOLD); textPaint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            canvas.drawText("P", pillRect.left + 35f, pillRect.centerY() - (fm.descent + fm.ascent)/2f, textPaint); textPaint.setTextAlign(Paint.Align.LEFT);
            
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(24f); textPaint.setAlpha((int)(255 * alpha));
            canvas.drawText("PixelSight", cx - textPaint.measureText("PixelSight")/2f, pillRect.centerY() - (fm.descent + fm.ascent)/2f, textPaint);
            
            textPaint.setColor(Color.parseColor("#10B981")); textPaint.setAlpha((int)(255 * alpha));
            canvas.drawText("60 FPS", pillRect.right - 95f, pillRect.centerY() - (fm.descent + fm.ascent)/2f, textPaint);
        }

        if (animProgress > 0.1f && !queue.isEmpty()) {
            float itemY = pillRect.top + 45f;
            for (IslandNotif n : queue) {
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(32f); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
                textPaint.setAlpha((int)(255 * animProgress));
                
                float iconX = pillRect.left + 50f;
                iconPaint.setColor(n.enabled ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
                iconPaint.setAlpha((int)(255 * animProgress));
                
                if (n.enabled) {
                    canvas.drawLine(iconX - 10f, itemY, iconX - 2f, itemY + 10f, iconPaint);
                    canvas.drawLine(iconX - 2f, itemY + 10f, iconX + 14f, itemY - 8f, iconPaint);
                } else {
                    canvas.drawLine(iconX - 8f, itemY - 8f, iconX + 8f, itemY + 8f, iconPaint);
                    canvas.drawLine(iconX + 8f, itemY - 8f, iconX - 8f, itemY + 8f, iconPaint);
                }

                Paint.FontMetrics fm = textPaint.getFontMetrics();
                canvas.drawText(n.modName + (n.enabled ? " Enabled" : " Disabled"), iconX + 40f, itemY - (fm.descent + fm.ascent)/2f, textPaint);
                itemY += 65f; 
            }
        }
        if (needsUpdate || !queue.isEmpty()) postInvalidateOnAnimation();
    }
}
