/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.hack.ai.arraylist.animation.Animation;
import com.hack.ai.arraylist.animation.Easing;
import com.hack.ai.arraylist.manager.ArrayListManager;
import com.hack.ai.arraylist.model.ArrayListConfig;
import com.hack.ai.arraylist.model.Module;
import com.hack.ai.arraylist.util.HudColorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Arraylist HUD overlay — 1:1 port of Solstice Arraylist::onRenderEvent.
 */
public class ArrayListOverlay extends View {

    private static final float RIGHT_MARGIN = 10f;
    private static final float TOP_Y_PASS1 = 5f;
    private static final float TOP_Y_PASS2 = 10f;
    private static final float OFFSCREEN_X = 14f;
    private static final float BAR_SPLIT_PAD = 7f;
    private static final float RECT_PAD_LEFT = 3f;
    private static final float RECT_PAD_RIGHT = 4f;
    private static final float SPLIT_BAR_OFFSET = 2f;
    private static final float SPLIT_BAR_W = 4f;
    private static final float SPLIT_BAR_TOP = 4f;
    private static final float SPLIT_BAR_BOTTOM = 6f;
    private static final float BG_SHADOW_BLUR = 200f;
    private static final float SHADOW_LAYER_CAP = 80f;

    private static final float BASE_PANEL_ALPHA = 0.10f;
    private static final int BASE_PANEL_R = 12;
    private static final int BASE_PANEL_G = 12;
    private static final int BASE_PANEL_B = 12;
    private static final float AMBIENT_GLOW_BLUR = 70f;
    private static final float FROST_ALPHA = 0.04f;
    private static final float GLOW_ALPHA = 0.25f;
    private static final float RECT_CORNER_RADIUS_FRAC = 0.18f;
    private static final float ANIM_EPSILON = 0.01f;
    private static final long ANIM_DURATION_MS = 250L;
    private static final int SETTING_TEXT_COLOR = 0xFFFFFFFF;

    private final ArrayListConfig config;
    private final ArrayList<Entry> entries = new ArrayList<>();
    private final ArrayList<RectInfo> rects = new ArrayList<>();
    private final ArrayList<RectInfo> rectPool = new ArrayList<>();
    private final ArrayList<LineInfo> lines = new ArrayList<>();
    private final ArrayList<LineInfo> linePool = new ArrayList<>();
    private final RectF rectF = new RectF();
    private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);

    private float scale = 1f;
    private int screenW, screenH;
    private boolean attached;
    private float textAscent, textHeight;
    private long lastCacheSig = -1;
    private boolean sortDirty = true;
    private boolean startingRectSet;
    private float startingRectX, startingRectY;

    private static final Comparator<Entry> WIDTH_DESC = (a, b) ->
            Float.compare(b.sortWidth, a.sortWidth);

    /** Rebuild entries from the current module list. Called when features change. */
    public void rebuildEntries() {
        entries.clear();
        for (Module module : ArrayListManager.getInstance().getModules()) {
            Entry entry = new Entry(module);
            entries.add(entry);
        }
        sortDirty = true;
        lastCacheSig = -1;
        postInvalidateOnAnimation();
    }

    public ArrayListOverlay(Context context) {
        super(context);
        config = ArrayListConfig.INSTANCE;
        setWillNotDraw(false);
        setClickable(false);
        setLongClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
        rebuildEntries();
    }

    // ---- WindowManager lifecycle ----

    private void ensureAttached() {
        if (attached) return;
        // 仅在 ArrayList 模块启用时才允许挂载
        if (!ArrayListConfig.INSTANCE.getModule().isEnabled()) return;
        try {
            WindowManager wm = (WindowManager) getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) return;
            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            }
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            wm.addView(this, params);
            attached = true;
        } catch (Exception ignored) {}
    }

    private void detach() {
        if (!attached) return;
        try {
            WindowManager wm = (WindowManager) getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) wm.removeViewImmediate(this);
        } catch (Exception ignored) {}
        attached = false;
    }

    public void show() { ensureAttached(); postInvalidateOnAnimation(); }

    public void wake() { ensureAttached(); postInvalidateOnAnimation(); }

    public void hide() {
        for (Entry e : entries) {
            e.animation.snapTo(0f);
            e.anim = 0f;
        }
        detach();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            screenW = w;
            screenH = h;
            scale = Math.max(0.6f, Math.min(3f, screenH / 1080f));
        }
    }

    // ---- main render ----

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!attached || screenW <= 0 || screenH <= 0) return;

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        long now = System.currentTimeMillis();
        int display = config.getDisplayIndex();
        int background = config.getBackgroundIndex();
        boolean renderMode = config.renderMode.getValue();
        boolean glow = config.glow.getValue();
        float glowStrength = config.glowStrength.getValue() * 100f;
        float blurStrength = config.blurStrength.getValue();
        float bgOpacity = config.backgroundOpacity.getValue();
        float bgValue = config.backgroundValue.getValue();
        boolean bold = config.boldText.getValue();
        float fontSizeSetting = config.fontSize.getValue();
        float fontSizePx = fontSizeSetting * scale;
        int theme = config.getThemeIndex();
        float colorSpeed = config.colorSpeed.getValue();
        float saturation = config.saturation.getValue();
        int[] customColors = config.getCustomColors();

        long sig = fontSignature(fontSizeSetting, bold, renderMode);
        if (sig != lastCacheSig) {
            lastCacheSig = sig;
            configureText(fontSizePx, bold);
            Paint.FontMetrics fm = pText.getFontMetrics();
            textHeight = fm.descent - fm.ascent;
            textAscent = fm.ascent;
            recomputeWidths(renderMode);
            sortDirty = true;
        }

        // Per-module display text (empty for main project)
        for (Entry e : entries) {
            String raw = renderMode ? e.module.getSettingDisplayText() : "";
            if (raw != e.lastRaw) {
                e.lastRaw = raw;
                e.displayText = raw.isEmpty() ? "" : " " + raw;
                e.displayW = e.displayText.isEmpty() ? 0f : pText.measureText(e.displayText);
                e.sortWidth = e.nameW + e.displayW;
                sortDirty = true;
            }
        }

        // Update animations
        for (Entry e : entries) {
            if (!e.module.isVisibleInArrayList()) continue;
            float target = e.module.isEnabled() ? 1f : 0f;
            e.animation.run(target);
            e.anim = e.animation.getValue();
            e.module.setArrayListAnim(e.anim);
        }

        if (sortDirty) {
            Collections.sort(entries, WIDTH_DESC);
            sortDirty = false;
        }

        float displayResX = screenW;
        float posX = displayResX - s(RIGHT_MARGIN);

        // ===== Pass 1: backgrounds, shadows, glows, split bars =====
        float posY = s(TOP_Y_PASS1);
        for (Entry e : entries) {
            if (!e.module.isVisibleInArrayList()) continue;
            if (e.anim < ANIM_EPSILON) continue;

            int color = HudColorUtils.getThemedColor(theme, colorSpeed, saturation,
                    customColors, posY * 2f, now);

            float nameW = e.nameW;
            float displayW = currentDisplayW(e, renderMode);
            float textPosX = posX;
            if (display == ArrayListConfig.DISPLAY_BAR
                    || display == ArrayListConfig.DISPLAY_SPLIT) {
                textPosX -= s(BAR_SPLIT_PAD);
            }
            float endPos = textPosX - nameW - displayW;
            textPosX = lerp(displayResX + s(OFFSCREEN_X), endPos, e.anim);

            float rx = textPosX - s(RECT_PAD_LEFT);
            float ry = posY;
            float rw = nameW + displayW + s(RECT_PAD_LEFT) + s(RECT_PAD_RIGHT);
            float rh = textHeight;

            if (glow && display != ArrayListConfig.DISPLAY_BAR
                    && display != ArrayListConfig.DISPLAY_SPLIT) {
                int glowColor = HudColorUtils.withAlpha(color, 0.83f * e.anim);
                drawShadowRect(canvas, textPosX, posY, nameW + displayW, rh,
                        glowColor, glowStrength * e.anim, 0f);
            }

            if (display != ArrayListConfig.DISPLAY_OUTLINE) {
                drawPanelBackground(canvas, rx, ry, rw, rh, color, background,
                        bgOpacity, e.anim);
            }

            if (display == ArrayListConfig.DISPLAY_SPLIT) {
                float barX = rx + rw + s(SPLIT_BAR_OFFSET);
                float barY = posY + s(SPLIT_BAR_TOP);
                float barW = s(SPLIT_BAR_W);
                float barH = rh - s(SPLIT_BAR_TOP) - s(SPLIT_BAR_BOTTOM);
                drawFilledRoundRect(canvas, barX, barY, barW, barH, s(3f), color);
                if (glow) {
                    int barGlow = HudColorUtils.withAlpha(color, 0.7f * e.anim);
                    drawShadowRect(canvas, barX, barY, barW, barH,
                            barGlow, glowStrength * e.anim, 0f);
                }
            }

            posY += rh * e.anim;
        }

        // ===== Pass 2: text + outline rects =====
        posX = displayResX - s(RIGHT_MARGIN);
        posY = s(TOP_Y_PASS2);
        rects.clear();
        for (Entry e : entries) {
            if (!e.module.isVisibleInArrayList()) continue;
            if (e.anim < ANIM_EPSILON) continue;

            int color = HudColorUtils.getThemedColor(theme, colorSpeed, saturation,
                    customColors, posY * 2f, now);

            String name = e.module.getDisplayName();
            float nameW = e.nameW;
            float displayW = currentDisplayW(e, renderMode);
            float textPosX = posX;
            float textPosY = posY - s(TOP_Y_PASS1);
            boolean addedPadding = false;
            if (display == ArrayListConfig.DISPLAY_BAR
                    || display == ArrayListConfig.DISPLAY_SPLIT) {
                textPosX -= s(BAR_SPLIT_PAD);
                addedPadding = true;
            }
            float endPos = textPosX - nameW - displayW;
            textPosX = lerp(displayResX + s(OFFSCREEN_X), endPos, e.anim);

            float rx = textPosX - s(RECT_PAD_LEFT);
            float ry = textPosY;
            float rw = nameW + displayW + s(RECT_PAD_LEFT) + s(RECT_PAD_RIGHT);
            float rh = textHeight;

            float pad = addedPadding ? s(BAR_SPLIT_PAD) : 0f;
            RectInfo rectInfo = obtainRect();
            rectInfo.set(rx + pad, ry, rx + rw + pad, ry + rh, color);
            rects.add(rectInfo);

            if (display == ArrayListConfig.DISPLAY_OUTLINE) {
                int fill = HudColorUtils.withAlpha(HudColorUtils.shade(color, bgValue), 0.4f);
                drawFilledRect(canvas, rx, ry, rw + pad, rh, fill);
            }

            drawText(canvas, name, textPosX, textPosY, color);
            if (renderMode && displayW > 0f) {
                drawText(canvas, e.displayText, textPosX + nameW, textPosY,
                        SETTING_TEXT_COLOR);
            }

            posY += rh * e.anim;
        }

        // ===== Pass 3: Bar glow =====
        if (display == ArrayListConfig.DISPLAY_BAR) {
            posX = displayResX - s(RIGHT_MARGIN);
            posY = s(TOP_Y_PASS2);
            for (Entry e : entries) {
                if (!e.module.isVisibleInArrayList()) continue;
                if (e.anim < ANIM_EPSILON) continue;

                int color = HudColorUtils.getThemedColor(theme, colorSpeed, saturation,
                        customColors, posY * 2f, now);
                float nameW = e.nameW;
                float displayW = currentDisplayW(e, renderMode);
                float textPosX = posX;
                if (display == ArrayListConfig.DISPLAY_BAR
                        || display == ArrayListConfig.DISPLAY_SPLIT) {
                    textPosX -= s(BAR_SPLIT_PAD);
                }
                float endPos = textPosX - nameW - displayW;
                textPosX = lerp(displayResX + s(OFFSCREEN_X), endPos, e.anim);
                float textX = nameW + displayW;
                if (glow) {
                    int barGlow = HudColorUtils.withAlpha(color, 0.7f * e.anim);
                    drawShadowRect(canvas, textPosX + textX, posY, 0f, textHeight,
                            barGlow, glowStrength * e.anim, 0f);
                }
                posY += textHeight * e.anim;
            }
        }

        if (display == ArrayListConfig.DISPLAY_NONE) {
            releaseRects();
            postInvalidateOnAnimation();
            return;
        }

        buildLines(display);
        for (LineInfo line : lines) {
            pFill.setColor(line.color);
            pFill.setStyle(Paint.Style.STROKE);
            pFill.setStrokeWidth(s(line.thickness));
            canvas.drawLine(line.sx, line.sy, line.ex, line.ey, pFill);
        }

        releaseRects();
        releaseLines();
        postInvalidateOnAnimation();
    }

    // ---- line builder ----

    private void buildLines(int display) {
        lines.clear();
        startingRectSet = false;
        int bgi = 0;
        int n = rects.size();
        for (int i = 0; i < n; i++) {
            RectInfo cur = rects.get(i);
            float csx = cur.sx - s(2f);
            float cex = cur.ex - s(2f);
            float sy = cur.sy, ey = cur.ey;
            int color = cur.color;

            RectInfo next = null;
            float nextSx = 0f;
            for (int j = i + 1; j < n; j++) {
                RectInfo cand = rects.get(j);
                if (next == null || cand.sx < nextSx) {
                    next = cand;
                    nextSx = cand.sx;
                }
            }
            boolean hasNext = next != null;

            if (display == ArrayListConfig.DISPLAY_OUTLINE) {
                addLine(cex + s(2f), sy, cex + s(2f), ey, color, 2f);
                if ((hasNext && nextSx >= csx) || !hasNext) {
                    addLine(csx, sy, csx, ey, color, 2f);
                }
                if (bgi == 0) {
                    startingRectX = csx;
                    startingRectY = sy;
                    startingRectSet = true;
                    addLine(csx, sy, cex + s(2f), sy, color, 2f);
                }
                if (!hasNext) {
                    addLine(csx, ey, cex + s(2f), ey, color, 2f);
                } else if (nextSx >= csx) {
                    if (nextSx - csx > s(2f)) {
                        addLine(csx, ey, nextSx - s(1f), ey, color, 2f);
                    }
                }
            } else if (display == ArrayListConfig.DISPLAY_BAR) {
                addLine(cex, sy, cex, ey, color, 4f);
            }
            bgi++;
        }

        RectInfo lowest = null;
        for (RectInfo r : rects) {
            float ssx = r.sx - s(2f);
            if (lowest == null || ssx < (lowest.sx - s(2f))) lowest = r;
        }
        if (display == ArrayListConfig.DISPLAY_OUTLINE && lowest != null && startingRectSet) {
            addLine(lowest.sx - s(2f), lowest.sy, startingRectX + s(2f), lowest.sy,
                    lowest.color, 2f);
        }
    }

    // ---- drawing helpers ----

    private void drawText(Canvas canvas, String text, float xTop, float yTop, int color) {
        if (text == null || text.isEmpty()) return;
        pText.setColor(color);
        canvas.drawText(text, xTop, yTop - textAscent, pText);
    }

    private void drawFilledRect(Canvas canvas, float x, float y, float w, float h, int color) {
        pFill.setColor(color);
        pFill.setStyle(Paint.Style.FILL);
        canvas.drawRect(x, y, x + w, y + h, pFill);
    }

    private void drawFilledRoundRect(Canvas canvas, float x, float y, float w, float h,
                                      float r, int color) {
        pFill.setColor(color);
        pFill.setStyle(Paint.Style.FILL);
        rectF.set(x, y, x + w, y + h);
        canvas.drawRoundRect(rectF, r, r, pFill);
    }

    private void drawShadowRect(Canvas canvas, float x, float y, float w, float h,
                                 int color, float blur, float cornerRadius) {
        if (Color.alpha(color) <= 0) return;
        float radius = Math.min(SHADOW_LAYER_CAP * scale, blur * scale);
        if (radius < 1f) radius = 1f;
        pFill.setColor(color);
        pFill.setStyle(Paint.Style.FILL);
        pFill.setShadowLayer(radius, 0f, 0f, color);
        rectF.set(x, y, x + w, y + h);
        float cr = Math.max(0f, cornerRadius * scale);
        canvas.drawRoundRect(rectF, cr, cr, pFill);
        pFill.clearShadowLayer();
    }

    private void drawGlassPanel(Canvas canvas, float x, float y, float w, float h,
                                 int fillColor, int glowColor, float glowBlur,
                                 float cornerRadius) {
        pFill.setColor(fillColor);
        pFill.setStyle(Paint.Style.FILL);
        if (Color.alpha(glowColor) > 0 && glowBlur > 0f) {
            float radius = Math.min(SHADOW_LAYER_CAP * scale, glowBlur * scale);
            if (radius < 1f) radius = 1f;
            pFill.setShadowLayer(radius, 0f, 0f, glowColor);
        }
        rectF.set(x, y, x + w, y + h);
        float cr = Math.max(0f, cornerRadius * scale);
        canvas.drawRoundRect(rectF, cr, cr, pFill);
        pFill.clearShadowLayer();
    }

    private void drawPanelBackground(Canvas canvas, float x, float y, float w, float h,
                                      int themeColor, int background, float bgOpacity,
                                      float anim) {
        if (w <= 0f || h <= 0f || anim < ANIM_EPSILON) return;
        float cr = textHeight * RECT_CORNER_RADIUS_FRAC * scale;

        if (background == ArrayListConfig.BACKGROUND_OPACITY) {
            int fill = Color.argb(Math.round(bgOpacity * anim * 180f),
                    BASE_PANEL_R, BASE_PANEL_G, BASE_PANEL_B);
            pFill.setColor(fill);
            pFill.setStyle(Paint.Style.FILL);
            rectF.set(x, y, x + w, y + h);
            canvas.drawRoundRect(rectF, cr, cr, pFill);
            int frost = Color.argb(Math.round(FROST_ALPHA * anim * 255f), 255, 255, 255);
            pFill.setColor(frost);
            canvas.drawRoundRect(rectF, cr, cr, pFill);
        } else if (background == ArrayListConfig.BACKGROUND_SHADOW) {
            int drop = Color.argb(Math.round(0.25f * anim * 255f), 0, 0, 0);
            drawShadowRect(canvas, x, y, w, h, drop, BG_SHADOW_BLUR, cr);
        } else {
            int drop = Color.argb(Math.round(0.18f * bgOpacity * anim * 255f), 0, 0, 0);
            drawShadowRect(canvas, x, y, w, h, drop, BG_SHADOW_BLUR, cr);
            int baseFill = Color.argb(Math.round(BASE_PANEL_ALPHA * bgOpacity * anim * 255f),
                    BASE_PANEL_R, BASE_PANEL_G, BASE_PANEL_B);
            int glowColor = HudColorUtils.withAlpha(themeColor, GLOW_ALPHA * anim);
            drawGlassPanel(canvas, x, y, w, h, baseFill, glowColor, AMBIENT_GLOW_BLUR, cr);
            int frost = Color.argb(Math.round(FROST_ALPHA * anim * 255f), 255, 255, 255);
            pFill.setColor(frost);
            canvas.drawRoundRect(rectF, cr, cr, pFill);
        }
    }

    // ---- font / width caching ----

    private long fontSignature(float fontSize, boolean bold, boolean renderMode) {
        return (((long) Float.floatToIntBits(fontSize)) & 0xFFFFFFFFL)
                | ((bold ? 1L : 0L) << 33)
                | ((renderMode ? 1L : 0L) << 34);
    }

    private void configureText(float fontSizePx, boolean bold) {
        pText.setTextSize(fontSizePx);
        pText.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        pText.setAntiAlias(true);
        pText.setSubpixelText(true);
    }

    private void recomputeWidths(boolean renderMode) {
        for (Entry e : entries) {
            e.nameW = pText.measureText(e.module.getDisplayName());
            String display = e.module.getSettingDisplayText();
            if (renderMode && display != null && !display.isEmpty()) {
                e.displayText = " " + display;
                e.displayW = pText.measureText(e.displayText);
            } else {
                e.displayText = "";
                e.displayW = 0f;
            }
            e.lastRaw = (renderMode && display != null) ? display : "";
            e.sortWidth = e.nameW + e.displayW;
        }
    }

    private float currentDisplayW(Entry e, boolean renderMode) {
        return renderMode ? e.displayW : 0f;
    }

    // ---- pools ----

    private RectInfo obtainRect() {
        if (!rectPool.isEmpty()) return rectPool.remove(rectPool.size() - 1);
        return new RectInfo();
    }

    private void releaseRects() { rectPool.addAll(rects); rects.clear(); }

    private void releaseLines() { linePool.addAll(lines); lines.clear(); }

    private void addLine(float sx, float sy, float ex, float ey, int color, float thickness) {
        LineInfo line;
        if (!linePool.isEmpty()) {
            line = linePool.remove(linePool.size() - 1);
        } else {
            line = new LineInfo();
        }
        line.set(sx, sy, ex, ey, color, thickness);
        lines.add(line);
    }

    // ---- helpers ----

    private float s(float v) { return v * scale; }

    private static float lerp(float a, float b, float t) { return a + t * (b - a); }

    // ---- inner classes ----

    private static final class Entry {
        final Module module;
        final Animation animation;
        float anim, nameW, displayW, sortWidth;
        String displayText = "";
        String lastRaw = "";

        Entry(Module module) {
            this.module = module;
            this.animation = new Animation(Easing.EASE_OUT_EXPO, ANIM_DURATION_MS);
            float initial = module.isEnabled() ? 1f : 0f;
            this.animation.snapTo(initial);
            this.anim = initial;
        }
    }

    private static final class RectInfo {
        float sx, sy, ex, ey;
        int color;

        void set(float sx, float sy, float ex, float ey, int color) {
            this.sx = sx; this.sy = sy; this.ex = ex; this.ey = ey; this.color = color;
        }
    }

    private static final class LineInfo {
        float sx, sy, ex, ey;
        int color;
        float thickness;

        void set(float sx, float sy, float ex, float ey, int color, float thickness) {
            this.sx = sx; this.sy = sy; this.ex = ex; this.ey = ey;
            this.color = color; this.thickness = thickness;
        }
    }
}
