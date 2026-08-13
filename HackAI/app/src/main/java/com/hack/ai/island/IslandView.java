/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.island;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;

import java.util.Random;

/**
 * Dynamic Island HUD — flagship-tier dark glass material with:
 * <ul>
 *   <li>Multi-layer ambient glow (HDR-style inverse-square falloff)</li>
 *   <li>Soft diffuse drop shadow</li>
 *   <li>5-stop glass-material body gradient (specular → body → AO → bounce)</li>
 *   <li>Fresnel rim highlight</li>
 *   <li>Subtle noise texture (film-like grain)</li>
 *   <li>Smooth ease-out animation, zero bounce</li>
 * </ul>
 *
 * <p>Collapsed: [HackAI │ 00:00:00 │ 60Hz]
 * <br>Expanded:  same top row + divider + version line + Ping/CPS line
 */
public class IslandView extends View {

    // ── layout constants (body size, without glow padding) ────────────
    private static final float BODY_W_COLLAPSED_DP = 188f;
    private static final float BODY_H_COLLAPSED_DP = 28f;
    private static final float BODY_W_EXPANDED_DP  = 212f;
    private static final float BODY_H_EXPANDED_DP  = 56f;

    /** Extra space on each side so the ambient glow is not clipped. */
    public static final float GLOW_PAD_DP = 46f;

    // ── animation timing ───────────────────────────────────────────────
    private static final long DUR_EXPAND   = 540L;
    private static final long DUR_COLLAPSE = 400L;

    // ── glow palette (cool blue-grey, HDR-style falloff) ───────────────
    private static final int GLOW_R = 52;
    private static final int GLOW_G = 55;
    private static final int GLOW_B = 65;

    // ── shadow palette ─────────────────────────────────────────────────
    private static final int SHADOW_R = 0;
    private static final int SHADOW_G = 0;
    private static final int SHADOW_B = 0;

    // ── body: 5-stop glass-material gradient ───────────────────────────
    //  0% — specular highlight (light catches top edge of glass)
    // 18% — base body tone
    // 48% — ambient occlusion (darkest, where body meets air below)
    // 74% — secondary reflection (subtle bounce light from surface below)
    // 100% — bottom edge re-darkens
    private static final int GLASS_SPECULAR = Color.argb(238, 22, 24, 32);
    private static final int GLASS_BODY     = Color.argb(246, 8,  9,  14);
    private static final int GLASS_AO       = Color.argb(252, 3,  4,  7);
    private static final int GLASS_BOUNCE   = Color.argb(242, 11, 12, 18);
    private static final int GLASS_BOTTOM   = Color.argb(248, 4,  5,  9);

    // ── rim / Fresnel ──────────────────────────────────────────────────
    private static final int FRESNEL_COLOR = Color.argb(48, 255, 255, 255);
    private static final int FRESNEL_GLOW  = Color.argb(32, 220, 225, 235);
    private static final int RIM_COLOR     = Color.argb(24, 255, 255, 255);

    // ── noise ──────────────────────────────────────────────────────────
    private static final int NOISE_ALPHA  = 7;   // very subtle grain
    private static final int NOISE_SIZE   = 64;  // tile size in px

    /** Callback invoked every frame during expand/collapse animation. */
    public interface OnSizeChangeListener {
        void onSizeChanged(int width, int height);
    }

    // ── density-normalised pixel dimensions ────────────────────────────
    public final int collapsedW;
    public final int collapsedH;
    public final int expandedW;
    public final int expandedH;

    private final float bodyCollapsedW;
    private final float bodyCollapsedH;
    private final float bodyExpandedW;
    private final float bodyExpandedH;

    // ── data ───────────────────────────────────────────────────────────
    private String username    = "HackAI";
    private String timeText    = "00:00:00";
    private String refreshRate = "60Hz";
    private String versionText = "v26.x";
    private String pingText    = "30ms";
    private String cpsText     = "5";

    // ── animation ──────────────────────────────────────────────────────
    private float expandedFraction = 0f;
    private ValueAnimator animator;
    private OnSizeChangeListener onSizeChangeListener;

    // Expand: gentle overshoot (~2.5 %) then settle — gives material weight.
    // Collapse: smooth ease-in-out, no overshoot.
    private final PathInterpolator expandInterp =
            new PathInterpolator(0.22f, 0.0f, 0.39f, 1.025f);
    private final PathInterpolator collapseInterp =
            new PathInterpolator(0.30f, 0.0f, 0.70f, 1.0f);

    // ── micro-animation (idle "living" motion, 12 s period) ────────────
    private final ValueAnimator microAnimator;
    private float specularPhase = 0.5f;  // drives slow specular movement
    private float ambientPhase  = 0.5f;  // drives subtle glow breathing

    // ═══════════════════════════════════════════════════════════════════
    // Paints — all allocated in constructor, zero allocations in onDraw
    // ═══════════════════════════════════════════════════════════════════

    // ---- glow (6 layers, inverse-square radius spacing) ----
    private final Paint gUltraFar  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gFar       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gMidOuter  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gMidInner  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gInner     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gBloom     = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        gUltraFar.setStyle(Paint.Style.FILL);
        gFar.setStyle(Paint.Style.FILL);
        gMidOuter.setStyle(Paint.Style.FILL);
        gMidInner.setStyle(Paint.Style.FILL);
        gInner.setStyle(Paint.Style.FILL);
        gBloom.setStyle(Paint.Style.FILL);
    }

    // ---- drop shadow (2 layers) ----
    private final Paint shadowFar  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowNear = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        shadowFar.setStyle(Paint.Style.FILL);
        shadowNear.setStyle(Paint.Style.FILL);
    }

    // ---- body (gradient shader swapped per-frame) ----
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ---- Fresnel rim (bright stroke + soft shadow halo) ----
    private final Paint fresnelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        fresnelPaint.setStyle(Paint.Style.STROKE);
    }

    // ---- outer rim (subtle constant metallic edge) ----
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        rimPaint.setColor(RIM_COLOR);
        rimPaint.setStyle(Paint.Style.STROKE);
    }

    // ---- slow-moving specular highlight (glass surface sheen) ----
    private final Paint specHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        specHighlightPaint.setColor(Color.argb(16, 255, 255, 255));
        specHighlightPaint.setStyle(Paint.Style.FILL);
    }

    // ---- noise overlay (BitmapShader, tiled, slowly drifting) ----
    private final Paint noisePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap noiseBitmap;
    private final BitmapShader noiseShader;
    private final Matrix noiseMatrix = new Matrix();

    // ---- text / separator ----
    private final Paint rowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        rowPaint.setColor(Color.WHITE);
        rowPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        rowPaint.setTextAlign(Paint.Align.LEFT);
    }
    private final Paint sepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        sepPaint.setColor(0x44FFFFFF);
        sepPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        sepPaint.setTextAlign(Paint.Align.LEFT);
    }
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        labelPaint.setColor(0x80FFFFFF);
        labelPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        labelPaint.setTextAlign(Paint.Align.LEFT);
    }
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        valuePaint.setColor(0xDDFFFFFF);
        valuePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        valuePaint.setTextAlign(Paint.Align.LEFT);
    }
    private final Paint divPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        divPaint.setColor(0x18FFFFFF);
        divPaint.setStyle(Paint.Style.STROKE);
    }

    // ── reusable objects ───────────────────────────────────────────────
    private final RectF rect  = new RectF();
    private final Path clipPath = new Path();
    private final float dp;
    private final float glowPad;

    private LinearGradient bodyGradient;
    private float lastBodyH = -1f;

    // ── constructors ───────────────────────────────────────────────────

    public IslandView(Context context) {
        this(context, null);
    }

    public IslandView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IslandView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dp = context.getResources().getDisplayMetrics().density;
        glowPad = GLOW_PAD_DP * dp;

        // Total view = body + glow padding × 2
        collapsedW = (int) (BODY_W_COLLAPSED_DP * dp + glowPad * 2f);
        collapsedH = (int) (BODY_H_COLLAPSED_DP * dp + glowPad * 2f);
        expandedW  = (int) (BODY_W_EXPANDED_DP  * dp + glowPad * 2f);
        expandedH  = (int) (BODY_H_EXPANDED_DP  * dp + glowPad * 2f);

        bodyCollapsedW = BODY_W_COLLAPSED_DP * dp;
        bodyCollapsedH = BODY_H_COLLAPSED_DP * dp;
        bodyExpandedW  = BODY_W_EXPANDED_DP  * dp;
        bodyExpandedH  = BODY_H_EXPANDED_DP  * dp;

        // Text / stroke sizing
        rowPaint.setTextSize(11f * dp);
        sepPaint.setTextSize(11f * dp);
        labelPaint.setTextSize(10f * dp);
        valuePaint.setTextSize(10f * dp);
        divPaint.setStrokeWidth(0.8f * dp);
        rimPaint.setStrokeWidth(Math.max(0.4f, 0.5f * dp));
        fresnelPaint.setStrokeWidth(Math.max(0.8f, 1.0f * dp));

        // Pre-generate noise texture (film-grain, fixed seed for determinism)
        noiseBitmap = createNoiseBitmap(NOISE_SIZE);
        noiseShader = new BitmapShader(noiseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        noisePaint.setShader(noiseShader);
        noisePaint.setAlpha(NOISE_ALPHA);

        // Slow idle micro-animation (12 s full cycle) — drives specular
        // movement and ambient breathing so the glass feels alive.
        microAnimator = ValueAnimator.ofFloat(0f, 1f);
        microAnimator.setDuration(12000);
        microAnimator.setRepeatMode(ValueAnimator.REVERSE);
        microAnimator.setRepeatCount(ValueAnimator.INFINITE);
        microAnimator.setInterpolator(new LinearInterpolator());
        microAnimator.addUpdateListener(anim -> {
            specularPhase = (float) anim.getAnimatedValue();
            ambientPhase  = (specularPhase * 0.83f) % 1f;
            invalidate();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (microAnimator != null && !microAnimator.isStarted()) {
            microAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (microAnimator != null && microAnimator.isRunning()) {
            microAnimator.pause();
        }
    }

    /** Generates a small greyscale random-noise bitmap for the grain overlay. */
    private static Bitmap createNoiseBitmap(int size) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Random rng = new Random(0x2A);
        int[] pixels = new int[size * size];
        for (int i = 0; i < pixels.length; i++) {
            int v = rng.nextInt(256);
            pixels[i] = Color.argb(v, 255, 255, 255);
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size);
        return bmp;
    }

    // ── public setters ─────────────────────────────────────────────────

    public void setUsername(String v)    { this.username    = v; invalidate(); }
    public void setTimeText(String v)    { this.timeText    = v; invalidate(); }
    public void setRefreshRate(String v) { this.refreshRate = v; invalidate(); }
    public void setVersionText(String v) { this.versionText = v; invalidate(); }
    public void setPingText(String v)    { this.pingText    = v; invalidate(); }
    public void setCpsText(String v)     { this.cpsText     = v; invalidate(); }

    public String getUsername()    { return username; }
    public String getTimeText()    { return timeText; }
    public String getRefreshRate() { return refreshRate; }
    public String getVersionText() { return versionText; }
    public String getPingText()    { return pingText; }
    public String getCpsText()     { return cpsText; }

    // ── animation state ────────────────────────────────────────────────

    public float getExpandedFraction() { return expandedFraction; }
    public boolean isExpanded()        { return expandedFraction > 0.5f; }

    public void setOnSizeChangeListener(OnSizeChangeListener listener) {
        this.onSizeChangeListener = listener;
    }

    // ── public API ─────────────────────────────────────────────────────

    public void toggle()   { if (isExpanded()) collapse(); else expand(); }
    public void expand()   { animateTo(1f); }
    public void collapse() { animateTo(0f); }

    public int currentWidth()  { return (int) lerp(collapsedW, expandedW, expandedFraction); }
    public int currentHeight() { return (int) lerp(collapsedH, expandedH, expandedFraction); }

    public int currentBodyWidth() {
        return (int) lerp(bodyCollapsedW, bodyExpandedW, expandedFraction);
    }

    // ── animation core ─────────────────────────────────────────────────

    private void animateTo(float target) {
        if (animator != null) animator.cancel();

        float from = expandedFraction;
        long  dur  = target > from ? DUR_EXPAND : DUR_COLLAPSE;
        PathInterpolator interp = target > from ? expandInterp : collapseInterp;
        long duration = (long) (dur * Math.abs(target - from));
        duration = clamp(duration, 80L, dur);

        animator = ValueAnimator.ofFloat(from, target);
        animator.setDuration(duration);
        animator.setInterpolator(interp);
        animator.addUpdateListener(animation -> {
            expandedFraction = (Float) animation.getAnimatedValue();
            if (onSizeChangeListener != null) {
                onSizeChangeListener.onSizeChanged(currentWidth(), currentHeight());
            }
            invalidate();
        });
        animator.start();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Draw — flagship-quality layer stack
    // ═══════════════════════════════════════════════════════════════════

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w  = getWidth();
        float h  = getHeight();
        float fc = clamp01(expandedFraction);

        // ── layer-specific response curves ───────────────────────────────
        // Different material properties respond at different rates.
        // Body is the primary driver; glow and shadow follow with subtle
        // phase offsets, simulating real physical material behaviour.
        float bodyFc   = fc;
        float glowFc   = responseCurve(fc, 0.055f);
        float shadowFc = responseCurve(fc, 0.030f);

        // Body bounds (inset by glowPad)
        float bl = glowPad;
        float bt = glowPad;
        float br = w - glowPad;
        float bb = h - glowPad;
        float bw = br - bl;
        float bh = bb - bt;

        // Body corner radius: perfect pill → smooth rounded rect
        float bodyRadius = lerp(bh / 2f, 23f * dp, bodyFc);
        float bodyCx = bl + bw / 2f;
        float bodyCy = bt + bh / 2f;

        rect.set(bodyCx - bw / 2f, bodyCy - bh / 2f, bodyCx + bw / 2f, bodyCy + bh / 2f);

        // Dynamic scaling — inner glow responds more than outer (natural diffusion)
        float bodySizeRatio = lerp(1f, bodyExpandedH / bodyCollapsedH, glowFc);
        float glowScaleInner = 1f + (bodySizeRatio - 1f) * 0.17f;
        float glowScaleOuter = 1f + (bodySizeRatio - 1f) * 0.08f;
        float shadowScale     = 1f + (bodySizeRatio - 1f) * 0.22f;

        // Subtle ambient breathing (±3.5 %) + specular opacity variation
        float breathe = 1f + 0.035f * (float) Math.sin(ambientPhase * 2.0 * Math.PI);
        float specAlphaVar = 0.65f + 0.35f * (float) Math.sin(ambientPhase * 2.2 * Math.PI + 0.5f);

        // ═══════════════════════════════════════════════════════════════
        // 1. Drop shadow — driven by shadowFc (slightly trails body)
        // ═══════════════════════════════════════════════════════════════
        float sdyFar  = 6f  * dp * shadowScale;
        float sdyNear = 3f  * dp * shadowScale;
        float srFar   = 25f * dp * shadowScale;
        float srNear  = 13f * dp * shadowScale;
        shadowFar.setShadowLayer(srFar, 0f, sdyFar,
                Color.argb(18, SHADOW_R, SHADOW_G, SHADOW_B));
        shadowFar.setColor(Color.argb(18, SHADOW_R, SHADOW_G, SHADOW_B));
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, shadowFar);
        shadowFar.clearShadowLayer();
        shadowNear.setShadowLayer(srNear, 0f, sdyNear,
                Color.argb(32, SHADOW_R, SHADOW_G, SHADOW_B));
        shadowNear.setColor(Color.argb(32, SHADOW_R, SHADOW_G, SHADOW_B));
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, shadowNear);
        shadowNear.clearShadowLayer();

        // ═══════════════════════════════════════════════════════════════
        // 2. Ambient glow — driven by glowFc (leads body slightly)
        //    Outer layers use glowScaleOuter; inner use glowScaleInner.
        //    Natural diffusion: inner glow concentrates more than outer.
        // ═══════════════════════════════════════════════════════════════

        float go  = glowScaleOuter;  // outer layers: subtle spread
        float gi  = glowScaleInner;  // inner layers: more pronounced
        int   brI = Math.round(45 * breathe);
        int   brT = Math.round(65 * breathe);
        int   brB = Math.round(95 * breathe);

        // Ultra-far — environmental halo (outer scale, static alpha)
        gUltraFar.setShadowLayer(52f * dp * go, 0f, 0f, glowColor(10));
        gUltraFar.setColor(glowColor(10));
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, gUltraFar);
        gUltraFar.clearShadowLayer();

        // Far ambient (outer scale)
        gFar.setShadowLayer(36f * dp * go, 0f, 0f, glowColor(17));
        gFar.setColor(glowColor(17));
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, gFar);
        gFar.clearShadowLayer();

        // Mid-outer (transitional: blend of outer and inner)
        float gmBlend = go * 0.6f + gi * 0.4f;
        gMidOuter.setShadowLayer(24f * dp * gmBlend, 0f, 0f, glowColor(28));
        gMidOuter.setColor(glowColor(28));
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, gMidOuter);
        gMidOuter.clearShadowLayer();

        // Mid-inner (inner scale + breathing)
        gMidInner.setShadowLayer(15f * dp * gi, 0f, 0f, glowColor(brI));
        gMidInner.setColor(glowColor(brI));
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, gMidInner);
        gMidInner.clearShadowLayer();

        // Tight inner (inner scale + breathing)
        gInner.setShadowLayer(8f * dp * gi, 0f, 0f, glowColor(brT));
        gInner.setColor(glowColor(brT));
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, gInner);
        gInner.clearShadowLayer();

        // Edge bloom (inner scale + breathing)
        gBloom.setShadowLayer(3f * dp * gi, 0f, 0f, glowColor(brB));
        gBloom.setColor(glowColor(brB));
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, gBloom);
        gBloom.clearShadowLayer();

        // ═══════════════════════════════════════════════════════════════
        // 3. Body — dynamic 5-stop glass-material gradient
        //    Stops shift with bodyFc so the material "stretches" naturally.
        // ═══════════════════════════════════════════════════════════════
        rect.set(bl, bt, br, bb);
        float aoStop    = lerp(0.48f, 0.55f, bodyFc);
        float bounceStop = lerp(0.74f, 0.82f, bodyFc);
        if (bh != lastBodyH) {
            bodyGradient = new LinearGradient(bl, bt, bl, bb,
                    new int[]{GLASS_SPECULAR, GLASS_BODY, GLASS_AO, GLASS_BOUNCE, GLASS_BOTTOM},
                    new float[]{0f, 0.18f, aoStop, bounceStop, 1f},
                    Shader.TileMode.CLAMP);
            lastBodyH = bh;
        }
        bodyPaint.setShader(bodyGradient);
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, bodyPaint);
        bodyPaint.setShader(null);

        // ═══════════════════════════════════════════════════════════════
        // 3b. Slow-moving specular highlight — faint bright band drifting
        //     0–3 dp over ~12 s, with subtle opacity variation so it
        //     doesn't look like a fixed scan-line.
        // ═══════════════════════════════════════════════════════════════
        float specBandTop = bt + 1.5f * dp + specularPhase * 3f * dp;
        float specBandH   = 5f * dp;
        specHighlightPaint.setAlpha(Math.round(16 * specAlphaVar));
        int saveSpec = canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(rect, bodyRadius, bodyRadius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        canvas.clipRect(bl, specBandTop, br, specBandTop + specBandH);
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, specHighlightPaint);
        canvas.restoreToCount(saveSpec);

        // ═══════════════════════════════════════════════════════════════
        // 4. Noise texture — clipped to body, slowly drifting so it
        //    doesn't feel static. Drift speed: ~2–3 dp peak per 12 s.
        // ═══════════════════════════════════════════════════════════════
        noiseMatrix.setTranslate(ambientPhase * 2.5f * dp, ambientPhase * 3.5f * dp);
        noiseShader.setLocalMatrix(noiseMatrix);
        int saveNoise = canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(rect, bodyRadius, bodyRadius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, noisePaint);
        canvas.restoreToCount(saveNoise);

        // ═══════════════════════════════════════════════════════════════
        // 5. Fresnel rim — bright edge catch with soft bloom halo
        //    A subtle stroke + setShadowLayer gives the glass-edge look.
        // ═══════════════════════════════════════════════════════════════
        fresnelPaint.setColor(FRESNEL_COLOR);
        fresnelPaint.setShadowLayer(2.8f * dp, 0f, 0f, FRESNEL_GLOW);
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, fresnelPaint);
        fresnelPaint.clearShadowLayer();

        // ═══════════════════════════════════════════════════════════════
        // 6. Outer rim — constant subtle metallic edge
        // ═══════════════════════════════════════════════════════════════
        canvas.drawRoundRect(rect, bodyRadius, bodyRadius, rimPaint);

        // ═══════════════════════════════════════════════════════════════
        // 7. Top-row text (driven by bodyFc)
        // ═══════════════════════════════════════════════════════════════
        float topRowCy = lerp(bodyCy, bt + bodyCollapsedH / 2f, bodyFc);
        float rowBase  = baseline(rowPaint, topRowCy);
        float sepBase  = baseline(sepPaint, topRowCy);

        String SEP   = " │ ";
        float swSep  = sepPaint.measureText(SEP);
        float lwUser = rowPaint.measureText(username);
        float lwTime = rowPaint.measureText(timeText);
        float lwHz   = rowPaint.measureText(refreshRate);
        float totalW = lwUser + swSep + lwTime + swSep + lwHz;

        float x = bodyCx - totalW / 2f;
        canvas.drawText(username,    x, rowBase, rowPaint); x += lwUser;
        canvas.drawText(SEP,         x, sepBase, sepPaint); x += swSep;
        canvas.drawText(timeText,    x, rowBase, rowPaint); x += lwTime;
        canvas.drawText(SEP,         x, sepBase, sepPaint); x += swSep;
        canvas.drawText(refreshRate, x, rowBase, rowPaint);

        // ═══════════════════════════════════════════════════════════════
        // 8. Expanded detail area — single-row layout
        //    Divider → [version │ Ping xx │ CPS xx] on one line.
        // ═══════════════════════════════════════════════════════════════
        if (fc < 0.02f) return;

        float divProgress = smoothstep(clamp01((fc - 0.18f) / 0.32f));
        float rowProgress = smoothstep(clamp01((fc - 0.30f) / 0.42f));

        float divY = bt + bodyCollapsedH;
        divPaint.setAlpha((int) (divProgress * 50));
        canvas.drawLine(bl + 12f * dp, divY, br - 12f * dp, divY, divPaint);

        float slide = lerp(8f * dp, 0f, rowProgress);
        float pad   = bl + 14f * dp;
        float rowY  = divY + 10f * dp - slide;

        valuePaint.setAlpha((int) (rowProgress * 210));
        labelPaint.setAlpha((int) (rowProgress * 128));
        sepPaint.setAlpha((int) (rowProgress * 55));

        float sx = pad;
        String midSep = "  │  ";
        float midSw = sepPaint.measureText(midSep);

        // Version
        canvas.drawText(versionText, sx, baseline(valuePaint, rowY), valuePaint);
        sx += valuePaint.measureText(versionText);
        // Separator
        canvas.drawText(midSep, sx, baseline(sepPaint, rowY), sepPaint);
        sx += midSw;
        // Ping
        canvas.drawText("Ping ", sx, baseline(labelPaint, rowY), labelPaint);
        sx += labelPaint.measureText("Ping ");
        canvas.drawText(pingText, sx, baseline(valuePaint, rowY), valuePaint);
        sx += valuePaint.measureText(pingText);
        // Separator
        canvas.drawText(midSep, sx, baseline(sepPaint, rowY), sepPaint);
        sx += midSw;
        // CPS
        canvas.drawText("CPS ", sx, baseline(labelPaint, rowY), labelPaint);
        sx += labelPaint.measureText("CPS ");
        canvas.drawText(cpsText, sx, baseline(valuePaint, rowY), valuePaint);

        sepPaint.setAlpha(0x44);
    }

    // ── internal helpers ───────────────────────────────────────────────

    /** Convenience: builds a glow colour from the base RGB + varying alpha. */
    private static int glowColor(int alpha) {
        return Color.argb(alpha, GLOW_R, GLOW_G, GLOW_B);
    }

    /**
     * Material-response curve: creates a subtle phase offset between
     * different visual layers so they don't all move in perfect lock-step.
     * At t=0.5 the offset peaks at {@code amplitude}; at t=0 and t=1 the
     * output matches the input exactly (no endpoint error).
     */
    private static float responseCurve(float t, float amplitude) {
        return t + amplitude * (float) Math.sin(t * Math.PI);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float smoothstep(float t) {
        float c = clamp01(t);
        return c * c * (3f - 2f * c);
    }

    private static float baseline(Paint paint, float cy) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return cy - (fm.ascent + fm.descent) / 2f;
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static long clamp(long v, long min, long max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
