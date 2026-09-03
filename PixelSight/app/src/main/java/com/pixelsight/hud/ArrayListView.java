package com.pixelsight.hud;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import com.pixelsight.gui.ClickGuiView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayListView extends View {
    private Paint textPaint, tagPaint, bgPaint, linePaint;
    private Map<String, Float> animMap = new HashMap<>();

    public ArrayListView(Context context) {
        super(context);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG); tagPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG); linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        ClickGuiView.Module arrayListMod = ClickGuiView.getModule("ArrayList");
        if (arrayListMod == null || !arrayListMod.enabled) return;

        // 绝对安全读取，防闪退
        int style = ((ClickGuiView.Mode) arrayListMod.settings.get(0)).current; 
        int colorMode = ((ClickGuiView.Mode) arrayListMod.settings.get(1)).current; 
        int hueVal = (int) ((ClickGuiView.ColorPicker) arrayListMod.settings.get(2)).hue; 
        float posX = ((ClickGuiView.Slider) arrayListMod.settings.get(3)).val; 
        float posY = ((ClickGuiView.Slider) arrayListMod.settings.get(4)).val; 
        float scale = ((ClickGuiView.Slider) arrayListMod.settings.get(5)).val / 100f; 
        int glowInt = ((ClickGuiView.Slider) arrayListMod.settings.get(6)).val; 
        int font = ((ClickGuiView.Mode) arrayListMod.settings.get(7)).current; 
        int showTags = ((ClickGuiView.Mode) arrayListMod.settings.get(8)).current; 
        
        int lang = 0; // 默认英文
        if (arrayListMod.settings.size() > 9) {
            lang = ((ClickGuiView.Mode) arrayListMod.settings.get(9)).current; 
        }

        Typeface tf = Typeface.DEFAULT_BOLD;
        textPaint.setAntiAlias(true); tagPaint.setAntiAlias(true);
        if (font == 1) tf = Typeface.SERIF; 
        else if (font == 2) tf = Typeface.MONOSPACE;
        else if (font == 3) {
            tf = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
            textPaint.setAntiAlias(false); tagPaint.setAntiAlias(false); 
        }
        textPaint.setTypeface(tf); textPaint.setTextSize(36f);
        tagPaint.setTypeface(tf); tagPaint.setTextSize(36f);

        boolean isRight = posX > 50f; boolean isBottom = posY > 50f;
        long time = System.currentTimeMillis(); boolean needsUpdate = true; 

        List<DisplayItem> items = new ArrayList<>();
        for (ClickGuiView.Module m : ClickGuiView.allModules) {
            if (m.nameEn.equals("ArrayList") || m.nameEn.equals("DynamicIsland") || m.nameEn.equals("Notifications")) continue;
            
            String name = lang == 0 ? m.nameEn : m.nameZh;
            String tag = "";
            if (showTags == 1 && !m.settings.isEmpty()) {
                ClickGuiView.Setting s = m.settings.get(0);
                if (s instanceof ClickGuiView.Mode) tag = " " + ((ClickGuiView.Mode)s).modesEn[((ClickGuiView.Mode)s).current];
                else if (s instanceof ClickGuiView.Slider) tag = " " + ((ClickGuiView.Slider)s).val;
            }
            items.add(new DisplayItem(m, name, tag, textPaint.measureText(name), tagPaint.measureText(tag)));
        }

        items.sort((i1, i2) -> Float.compare(i2.totalWidth, i1.totalWidth));

        canvas.save();
        float pivotX = getWidth() * (posX / 100f); float pivotY = getHeight() * (posY / 100f);
        canvas.scale(scale, scale, pivotX, pivotY);

        float currentY = pivotY; int renderIndex = 0;

        for (DisplayItem item : items) {
            ClickGuiView.Module m = item.mod;
            float target = m.enabled ? 1f : 0f;
            float current = animMap.containsKey(m.nameEn) ? animMap.get(m.nameEn) : 0f;

            if (current != target) {
                current += (target > current) ? 0.08f : -0.08f;
                if (Math.abs(current - target) < 0.08f) current = target;
                animMap.put(m.nameEn, current);
            }

            if (current > 0f) {
                float offsetX = (1f - current) * 150f * (isRight ? -1 : 1);
                float bgLeft = isRight ? (pivotX - item.totalWidth - 25f + offsetX) : (pivotX + offsetX);
                float bgRight = isRight ? (pivotX + offsetX) : (pivotX + item.totalWidth + 25f + offsetX);

                int color;
                if (colorMode == 1) color = Color.parseColor("#4A80F6"); 
                else if (colorMode == 2) color = Color.HSVToColor(new float[]{hueVal, 1f, 1f}); 
                else color = Color.HSVToColor(new float[] { ((time - renderIndex * 250) % 3500) / 3500f * 360f, 0.6f, 1f });

                if (style == 2) { 
                    textPaint.setShadowLayer(glowInt / 3f, 0, 0, color); tagPaint.setShadowLayer(glowInt / 3f, 0, 0, Color.parseColor("#AAAAAA"));
                } else if (style == 1 && glowInt > 0) { 
                    textPaint.setShadowLayer(glowInt / 3f, 0, 0, color); tagPaint.setShadowLayer(glowInt / 4f, 0, 0, Color.parseColor("#AAAAAA"));
                } else { 
                    textPaint.setShadowLayer(4f, 2f, 2f, Color.parseColor("#B3000000")); tagPaint.setShadowLayer(4f, 2f, 2f, Color.parseColor("#B3000000"));
                }

                if (style != 2) { 
                    if (style == 1) { 
                        int alphaColor = Color.argb((int)(80 * current), Color.red(color), Color.green(color), Color.blue(color));
                        bgPaint.setShader(new LinearGradient(isRight ? bgLeft : bgRight, 0, isRight ? bgRight : bgLeft, 0, Color.TRANSPARENT, alphaColor, Shader.TileMode.CLAMP));
                        canvas.drawRect(bgLeft, currentY - 35f, bgRight, currentY + 15f, bgPaint);
                    } else if (style == 3) { 
                        bgPaint.setShader(null); bgPaint.setColor(Color.argb((int)(120 * current), 20, 20, 20));
                        canvas.drawRect(bgLeft, currentY - 35f, bgRight, currentY + 15f, bgPaint);
                        linePaint.setColor(color); linePaint.setAlpha((int)(255 * current)); linePaint.clearShadowLayer();
                        canvas.drawLine(bgLeft + 2f, currentY - 35f, bgLeft + 2f, currentY + 15f, linePaint); 
                    } else { 
                        bgPaint.setShader(null); bgPaint.setColor(Color.argb((int)(100 * current), 0, 0, 0));
                        canvas.drawRect(bgLeft, currentY - 35f, bgRight, currentY + 15f, bgPaint);
                    }

                    if (style != 3) { 
                        linePaint.setColor(color); linePaint.setAlpha((int)(255 * current));
                        if (style == 1 && glowInt > 0) linePaint.setShadowLayer(glowInt / 4f, 0, 0, color); else linePaint.clearShadowLayer();
                        float lineX = isRight ? pivotX + offsetX : pivotX + offsetX;
                        canvas.drawLine(lineX, currentY - 35f, lineX, currentY + 15f, linePaint);
                    }
                }

                textPaint.setColor(color); textPaint.setAlpha((int)(255 * current));
                tagPaint.setColor(Color.parseColor("#AAAAAA")); tagPaint.setAlpha((int)(255 * current));

                float nameX = isRight ? (pivotX - item.tagWidth - item.nameWidth - 12f + offsetX) : (pivotX + 12f + offsetX);
                float tagX = nameX + item.nameWidth;

                canvas.drawText(item.name, nameX, currentY, textPaint);
                if (!item.tag.isEmpty()) canvas.drawText(item.tag, tagX, currentY, tagPaint);

                currentY += (isBottom ? -45f : 45f) * current;
                renderIndex++;
            }
        }
        canvas.restore();
        if (needsUpdate) postInvalidateOnAnimation();
    }

    private static class DisplayItem {
        ClickGuiView.Module mod; String name, tag; float nameWidth, tagWidth, totalWidth;
        DisplayItem(ClickGuiView.Module m, String n, String t, float nw, float tw) { mod=m; name=n; tag=t; nameWidth=nw; tagWidth=tw; totalWidth=nw+tw; }
    }
}
