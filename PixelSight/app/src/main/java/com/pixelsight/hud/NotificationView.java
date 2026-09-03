package com.pixelsight.hud;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import com.pixelsight.gui.ClickGuiView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationView extends View {
    private static List<Notif> queue = new ArrayList<>();
    private Paint paint, textPaint, barPaint;
    private static NotificationView instance;

    public NotificationView(Context context) {
        super(context); instance = this;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG); textPaint = new Paint(Paint.ANTI_ALIAS_FLAG); barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public static void show(String title, String desc, boolean enabled) {
        queue.add(new Notif(title, desc, enabled));
        if (instance != null) instance.postInvalidateOnAnimation();
    }
    public static boolean isEmpty() { return queue.isEmpty(); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ClickGuiView.Module notifMod = ClickGuiView.getModule("Notifications");
        if (notifMod == null || !notifMod.enabled) return;

        int style = ((ClickGuiView.Mode) notifMod.settings.get(0)).current; // 0=Classic, 1=Vape, 2=Outline
        float scale = ((ClickGuiView.Slider) notifMod.settings.get(1)).val / 100f; 

        boolean isLight = ClickGuiView.isLightTheme; long time = System.currentTimeMillis(); boolean needsUpdate = false;

        canvas.save(); canvas.scale(scale, scale, getWidth(), getHeight());
        float currentY = getHeight() - 40f; 

        Iterator<Notif> it = queue.iterator();
        while (it.hasNext()) {
            Notif n = it.next();
            float aliveTime = time - n.startTime;

            if (aliveTime < 350) {
                float t = aliveTime / 350f; n.animX = 1f - (float)(Math.sin(t * Math.PI / 2) * (1f + 0.2f * (1f - t))); n.alpha = t; needsUpdate = true;
            } else if (aliveTime > 2000) {
                float fadeOut = (aliveTime - 2000f) / 300f; n.animX = fadeOut; n.alpha = 1f - fadeOut; needsUpdate = true;
                if (aliveTime > 2300) { it.remove(); continue; }
            } else { n.animX = 0f; n.alpha = 1f; needsUpdate = true; }

            float width = 320f; float height = 80f;
            float x = getWidth() - width - 30f + (n.animX * 400f); 
            currentY -= (height + 15f);

            RectF rect = new RectF(x, currentY, x + width, currentY + height);
            int mainColor = n.enabled ? Color.parseColor("#10B981") : Color.parseColor("#EF4444");

            if (style == 0) { paint.setColor(Color.parseColor("#CC000000")); canvas.drawRoundRect(rect, 15f, 15f, paint); } 
            else if (style == 1) { paint.setColor(Color.parseColor("#E6121212")); canvas.drawRect(rect, paint); } 
            else if (style == 2) { paint.setColor(Color.parseColor("#99000000")); canvas.drawRoundRect(rect, 10f, 10f, paint); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3f); paint.setColor(mainColor); canvas.drawRoundRect(rect, 10f, 10f, paint); paint.setStyle(Paint.Style.FILL); } 

            if (style != 2) {
                barPaint.setColor(mainColor);
                if (style == 1) canvas.drawRect(x, currentY, x + 4f, currentY + height, barPaint); 
                else canvas.drawRoundRect(new RectF(x, currentY, x + 8f, currentY + height), 15f, 15f, barPaint);
            }

            textPaint.setColor(Color.WHITE); textPaint.setTypeface(Typeface.DEFAULT_BOLD); textPaint.setAlpha((int)(255 * n.alpha));
            textPaint.setTextSize(28f); canvas.drawText(n.title, x + 30f, currentY + 35f, textPaint);
            textPaint.setColor(Color.parseColor("#A0A0A0")); textPaint.setTextSize(20f); textPaint.setTypeface(Typeface.DEFAULT); textPaint.setAlpha((int)(255 * n.alpha));
            canvas.drawText(n.desc, x + 30f, currentY + 65f, textPaint);
            
            if (style != 1 && style != 2) { 
                float progress = Math.max(0f, 1f - (aliveTime / 2300f)); barPaint.setAlpha((int)(255 * n.alpha));
                canvas.drawRoundRect(new RectF(x + 10f, currentY + height - 4f, x + 10f + (width - 20f) * progress, currentY + height), 2f, 2f, barPaint);
            }
        }
        canvas.restore();
        if (needsUpdate || !queue.isEmpty()) postInvalidateOnAnimation();
    }
    private static class Notif { String title, desc; boolean enabled; long startTime; float animX = 1f, alpha = 0f; Notif(String t, String d, boolean e) { title=t; desc=d; enabled=e; startTime=System.currentTimeMillis(); } }
}
