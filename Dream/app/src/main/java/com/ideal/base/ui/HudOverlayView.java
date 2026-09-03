package com.ideal.base.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

import com.ideal.base.R;
import com.ideal.base.module.ModuleController;
import com.ideal.base.module.ModuleRepository;
import com.ideal.base.state.OverlayStateStore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Non-interactive Canvas HUD. It reads the existing ModuleController and only renders UI;
 * it deliberately has no game, hook, injection, JNI, or system-Toast responsibilities.
 */
public final class HudOverlayView extends View {

    public static final String MODULE_NOTIFICATIONS = "hud_notifications";
    public static final String MODULE_LIST = "hud_module_list";
    public static final String SETTING_LIST_HIDDEN = "module_list.hidden";
    public static final String SETTING_NOTIFICATIONS_HIDDEN = "notifications.hidden";
    public static final String SETTING_LIST_SCALE = "module_list.scale";
    public static final String SETTING_LIST_OPACITY = "module_list.opacity";
    public static final String SETTING_GRADIENT_SPEED = "gradient.speed";

    private static final int[] ACCENT_COLORS = {
            0xFFFFB1E8, 0xFFC77CF0, 0xFF918EFF, 0xFF6DCBFF
    };
    private static final int MAX_NOTICES = 3;
    private static final long ROW_ENTER_MS = 240L;
    private static final long ROW_EXIT_MS = 240L;
    private static final long NOTICE_ENTER_MS = 240L;
    private static final long NOTICE_TOTAL_MS = 2_880L;
    private static final long NOTICE_EXIT_MS = 240L;

    private final ModuleRepository repository;
    private final ModuleController controller;
    private final OverlayStateStore stateStore;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final Rect safeBounds = new Rect();
    private final Map<String, RowNode> rowNodes = new LinkedHashMap<>();
    private final List<NoticeNode> notices = new ArrayList<>();
    private final ArrayDeque<NoticeRequest> pendingNotices = new ArrayDeque<>();
    private final String brandName;
    private final Runnable animationTick = new Runnable() {
        @Override
        public void run() {
            animationPosted = false;
            if (!released) {
                invalidate();
                ensureAnimationLoop();
            }
        }
    };

    private boolean animationPosted;
    private boolean released;
    private float referenceScale = 1f;
    private long lastFrameMs;

    public HudOverlayView(Context context, ModuleRepository repository,
                          ModuleController controller, OverlayStateStore stateStore) {
        super(context);
        this.repository = repository;
        this.controller = controller;
        this.stateStore = stateStore;
        brandName = context.getString(R.string.hud_brand_name);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        ensureAnimationLoop();
    }

    public void setSafeBounds(Rect bounds, float scale) {
        safeBounds.set(bounds);
        referenceScale = Math.max(0.01f, scale);
        refresh();
    }

    public void refresh() {
        if (!released) {
            invalidate();
            ensureAnimationLoop();
        }
    }

    /** Called by the one real module-state transition path in ModuleController. */
    public void onModuleEnabledStateChanged(String moduleId, boolean enabled) {
        ModuleRepository.ModuleDefinition module = repository.getModule(moduleId);
        if (module == null || released) {
            return;
        }
        NoticeRequest request = new NoticeRequest(module.name, enabled);
        if (notices.size() < MAX_NOTICES) {
            notices.add(0, new NoticeNode(request, SystemClock.uptimeMillis()));
        } else {
            // Do not lose rapid transitions: retain excess items and show them as space opens.
            pendingNotices.addLast(request);
        }
        refresh();
    }

    /** Cancels the single frame callback and clears transient animation state. */
    public void release() {
        released = true;
        removeCallbacks(animationTick);
        animationPosted = false;
        notices.clear();
        pendingNotices.clear();
        rowNodes.clear();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(animationTick);
        animationPosted = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (released) {
            return;
        }
        final long now = SystemClock.uptimeMillis();
        final long frameDelta = lastFrameMs == 0L ? 16L : Math.max(1L, now - lastFrameMs);
        lastFrameMs = now;
        final Rect bounds = resolvedBounds();
        final float base = referenceScale;
        final float phase = accentPhase(now);

        drawWatermark(canvas, bounds, base, phase);
        updateAndDrawModuleList(canvas, bounds, base, phase, now, frameDelta);
        updateAndDrawNotices(canvas, bounds, base, phase, now, frameDelta);
        ensureAnimationLoop();
    }

    private Rect resolvedBounds() {
        if (!safeBounds.isEmpty()) {
            return safeBounds;
        }
        return new Rect(0, 0, getWidth(), getHeight());
    }

    private void drawWatermark(Canvas canvas, Rect bounds, float scale, float phase) {
        float left = bounds.left + 14f * scale;
        float bottom = bounds.bottom - 28f * scale;
        float railWidth = 18f * scale;
        float railHeight = 90f * scale;

        rect.set(left, bottom - railHeight, left + railWidth, bottom);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(accentGradient(rect.left, rect.top, rect.right, rect.bottom, phase));
        paint.setShadowLayer(10f * scale, 0f, 2f * scale, 0x7A7B60B9);
        canvas.drawRoundRect(rect, railWidth * .5f, railWidth * .5f, paint);
        clearPaintEffects();

        String title = brandName + " Release";
        float titleX = left + 34f * scale;
        float titleBaseline = bottom - 33f * scale;
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(38f * scale);
        paint.setTextAlign(Paint.Align.LEFT);
        float titleWidth = paint.measureText(title);
        paint.setShader(accentGradient(titleX, titleBaseline - 38f * scale,
                titleX + titleWidth, titleBaseline, phase));
        paint.setShadowLayer(8f * scale, 0f, 2f * scale, 0x66442A6F);
        canvas.drawText(title, titleX, titleBaseline, paint);
        clearPaintEffects();
    }

    private void updateAndDrawModuleList(Canvas canvas, Rect bounds, float baseScale,
                                         float phase, long now, long frameDelta) {
        ModuleController.ModuleState controlState = controller.stateOf(MODULE_LIST);
        boolean listVisible = controlState != null && controlState.enabled
                && !stateStore.getHudBoolean(SETTING_LIST_HIDDEN, false);
        float configuredScale = clamp(stateStore.getHudFloat(SETTING_LIST_SCALE, 1f), 0f, 2f);
        float opacity = clamp(stateStore.getHudFloat(SETTING_LIST_OPACITY, .72f), 0f, 1f);
        if (!listVisible || configuredScale <= .001f || opacity <= .001f) {
            markRowsForExit(now);
            drawExitingRows(canvas, bounds, baseScale, phase, now, frameDelta, opacity);
            return;
        }

        List<RowSpec> specs = collectEnabledRows(baseScale * configuredScale, bounds);
        updateRows(specs, bounds, now);
        drawRows(canvas, bounds, baseScale, phase, now, frameDelta, opacity);
    }

    private List<RowSpec> collectEnabledRows(float requestedScale, Rect bounds) {
        List<ModuleRepository.ModuleDefinition> enabled = new ArrayList<>();
        for (ModuleRepository.ModuleDefinition module : repository.getAllModules()) {
            ModuleController.ModuleState state = controller.stateOf(module.id);
            if (state != null && state.enabled) {
                enabled.add(module);
            }
        }
        if (enabled.isEmpty()) {
            return Collections.emptyList();
        }
        float availableHeight = Math.max(1f, bounds.height() - 24f * referenceScale);
        float uncompressedHeight = enabled.size() * 58f * requestedScale;
        float fit = Math.min(1f, availableHeight / Math.max(1f, uncompressedHeight));
        float drawScale = requestedScale * fit;
        List<RowSpec> specs = new ArrayList<>();
        for (ModuleRepository.ModuleDefinition module : enabled) {
            ModuleController.ModuleState state = controller.stateOf(module.id);
            String summary = summaryFor(module, state);
            paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            paint.setTextSize(28f * drawScale);
            float nameWidth = paint.measureText(module.name);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            paint.setTextSize(26f * drawScale);
            float summaryWidth = summary.isEmpty() ? 0f : paint.measureText(summary);
            float gap = summary.isEmpty() ? 0f : 12f * drawScale;
            float width = nameWidth + gap + summaryWidth + 28f * drawScale;
            specs.add(new RowSpec(module.id, module.name, summary, width, drawScale));
        }
        Collections.sort(specs, new Comparator<RowSpec>() {
            @Override
            public int compare(RowSpec first, RowSpec second) {
                int widthOrder = Float.compare(second.width, first.width);
                return widthOrder != 0 ? widthOrder : first.id.compareTo(second.id);
            }
        });
        return specs;
    }

    private String summaryFor(ModuleRepository.ModuleDefinition module,
                              ModuleController.ModuleState state) {
        if (MODULE_LIST.equals(module.id)) {
            return brandName;
        }
        if ("killaura".equals(module.id)) {
            return String.format(Locale.US, "范围：%.2f", state.range);
        }
        if ("cheststealer".equals(module.id)) {
            return String.format(Locale.US, "延迟：%d", Math.round(state.delay));
        }
        if ("scaffold".equals(module.id)) {
            return state.scaffoldMode;
        }
        return "";
    }

    private void updateRows(List<RowSpec> specs, Rect bounds, long now) {
        for (RowNode node : rowNodes.values()) {
            node.present = false;
        }
        float y = bounds.top + 20f * referenceScale;
        for (RowSpec spec : specs) {
            RowNode node = rowNodes.get(spec.id);
            if (node == null) {
                node = new RowNode(spec, y, now);
                rowNodes.put(spec.id, node);
            }
            node.present = true;
            node.name = spec.name;
            node.summary = spec.summary;
            node.targetY = y;
            node.targetWidth = spec.width;
            node.drawScale = spec.drawScale;
            node.exitAt = 0L;
            y += 58f * spec.drawScale;
        }
        Iterator<RowNode> iterator = rowNodes.values().iterator();
        while (iterator.hasNext()) {
            RowNode node = iterator.next();
            if (!node.present && node.exitAt == 0L) {
                node.exitAt = now;
            }
            if (node.exitAt != 0L && now - node.exitAt >= ROW_EXIT_MS) {
                iterator.remove();
            }
        }
    }

    private void markRowsForExit(long now) {
        for (RowNode node : rowNodes.values()) {
            if (node.exitAt == 0L) {
                node.exitAt = now;
            }
        }
    }

    private void drawExitingRows(Canvas canvas, Rect bounds, float scale, float phase,
                                 long now, long frameDelta, float opacity) {
        Iterator<RowNode> iterator = rowNodes.values().iterator();
        while (iterator.hasNext()) {
            RowNode node = iterator.next();
            if (now - node.exitAt >= ROW_EXIT_MS) {
                iterator.remove();
                continue;
            }
            drawRow(canvas, node, bounds, phase, now, frameDelta, opacity);
        }
    }

    private void drawRows(Canvas canvas, Rect bounds, float scale, float phase,
                          long now, long frameDelta, float opacity) {
        for (RowNode node : rowNodes.values()) {
            drawRow(canvas, node, bounds, phase, now, frameDelta, opacity);
        }
    }

    private void drawRow(Canvas canvas, RowNode node, Rect bounds, float phase,
                         long now, long frameDelta, float opacity) {
        float follow = 1f - (float) Math.exp(-frameDelta / 150f);
        node.currentY += (node.targetY - node.currentY) * follow;
        node.currentWidth += (node.targetWidth - node.currentWidth) * follow;
        float lifeAlpha = 1f;
        float slide = 0f;
        float heightFactor = 1f;
        if (node.exitAt != 0L) {
            float progress = progress(now - node.exitAt, ROW_EXIT_MS);
            lifeAlpha = 1f - progress;
            slide = progress * 108f * node.drawScale;
            heightFactor = 1f - .28f * progress;
        } else {
            float progress = progress(now - node.enterAt, ROW_ENTER_MS);
            lifeAlpha = progress;
            slide = (1f - progress) * 108f * node.drawScale;
            heightFactor = .78f + .22f * progress;
        }
        float rowHeight = 52f * node.drawScale * heightFactor;
        float barWidth = 16f * node.drawScale;
        float right = bounds.right - 14f * referenceScale + slide;
        float barLeft = right - barWidth;
        float backgroundRight = barLeft - 7f * node.drawScale;
        float backgroundLeft = backgroundRight - node.currentWidth;
        float top = node.currentY + (52f * node.drawScale - rowHeight) * .5f;
        float alpha = lifeAlpha * opacity;
        if (alpha <= .002f || rowHeight <= .01f) {
            return;
        }

        rect.set(backgroundLeft, top, backgroundRight, top + rowHeight);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(withAlpha(0xA7070A11, alpha));
        paint.setShadowLayer(8f * node.drawScale, 0f, 2f * node.drawScale,
                withAlpha(0xAA000000, alpha));
        canvas.drawRoundRect(rect, 4f * node.drawScale, 4f * node.drawScale, paint);
        clearPaintEffects();

        float baseline = top + rowHeight * .68f;
        float textX = backgroundLeft + 14f * node.drawScale;
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(28f * node.drawScale);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setShader(accentGradient(textX, top, backgroundRight, top + rowHeight, phase));
        paint.setAlpha(Math.round(255f * alpha));
        canvas.drawText(node.name, textX, baseline, paint);
        float nameWidth = paint.measureText(node.name);
        paint.setShader(null);
        if (!node.summary.isEmpty()) {
            paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            paint.setTextSize(26f * node.drawScale);
            paint.setColor(withAlpha(0xFFF1EDF6, alpha));
            canvas.drawText(node.summary, textX + nameWidth + 12f * node.drawScale,
                    baseline, paint);
        }
        paint.setAlpha(255);

        rect.set(barLeft, top, right, top + rowHeight);
        paint.setShader(accentGradient(rect.left, rect.top, rect.right, rect.bottom, phase));
        paint.setAlpha(Math.round(255f * alpha));
        canvas.drawRoundRect(rect, 3f * node.drawScale, 3f * node.drawScale, paint);
        clearPaintEffects();
    }

    private void updateAndDrawNotices(Canvas canvas, Rect bounds, float scale, float phase,
                                      long now, long frameDelta) {
        pruneNotices(now);
        ModuleController.ModuleState controlState = controller.stateOf(MODULE_NOTIFICATIONS);
        boolean notificationsVisible = controlState != null && controlState.enabled
                && !stateStore.getHudBoolean(SETTING_NOTIFICATIONS_HIDDEN, false);
        if (!notificationsVisible) {
            return;
        }
        float bottom = bounds.bottom - 28f * scale;
        float height = 82f * scale;
        float gap = 12f * scale;
        int count = notices.size();
        for (int index = 0; index < count; index++) {
            NoticeNode node = notices.get(index);
            float targetTop = bottom - (count - index) * height - (count - index - 1) * gap;
            float follow = 1f - (float) Math.exp(-frameDelta / 150f);
            node.currentTop += (targetTop - node.currentTop) * follow;
            if (!node.initialized) {
                node.currentTop = targetTop;
                node.initialized = true;
            }
            long age = now - node.shownAt;
            float alpha = 1f;
            float slide = 0f;
            if (age < NOTICE_ENTER_MS) {
                float progress = progress(age, NOTICE_ENTER_MS);
                alpha = progress;
                slide = (1f - progress) * 110f * scale;
            } else if (age > NOTICE_TOTAL_MS - NOTICE_EXIT_MS) {
                float progress = progress(age - (NOTICE_TOTAL_MS - NOTICE_EXIT_MS), NOTICE_EXIT_MS);
                alpha = 1f - progress;
                slide = progress * 110f * scale;
            }
            drawNotice(canvas, node, bounds.right - 14f * scale + slide, node.currentTop,
                    height, scale, phase, alpha);
        }
    }

    private void pruneNotices(long now) {
        Iterator<NoticeNode> iterator = notices.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().shownAt >= NOTICE_TOTAL_MS) {
                iterator.remove();
            }
        }
        while (notices.size() < MAX_NOTICES && !pendingNotices.isEmpty()) {
            notices.add(0, new NoticeNode(pendingNotices.removeFirst(), now));
        }
    }

    private void drawNotice(Canvas canvas, NoticeNode node, float right, float top, float height,
                            float scale, float phase, float alpha) {
        if (alpha <= .002f) {
            return;
        }
        String title = "模块 " + (node.enabled ? "开启" : "关闭");
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(31f * scale);
        float titleWidth = paint.measureText(title);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(20f * scale);
        float labelWidth = paint.measureText(node.moduleName);
        float barWidth = 15f * scale;
        float contentWidth = Math.max(titleWidth, labelWidth);
        float backgroundRight = right - barWidth - 9f * scale;
        float backgroundLeft = backgroundRight - contentWidth - 30f * scale;

        rect.set(backgroundLeft, top, backgroundRight, top + height);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(withAlpha(0x8604070D, alpha));
        paint.setShadowLayer(14f * scale, 0f, 4f * scale, withAlpha(0xB0000000, alpha));
        canvas.drawRoundRect(rect, 8f * scale, 8f * scale, paint);
        clearPaintEffects();

        float textX = backgroundLeft + 15f * scale;
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(31f * scale);
        paint.setColor(withAlpha(0xFFF3EFF8, alpha));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(title, textX, top + 34f * scale, paint);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(20f * scale);
        paint.setColor(withAlpha(0xFFBDB5C9, alpha));
        canvas.drawText(node.moduleName, textX, top + 60f * scale, paint);

        drawNoticeBracket(canvas, right - barWidth, top, right, top + height, scale, phase, alpha);
    }

    /** A filled Canvas path with curved inward ends, matching the reference's right bracket. */
    private void drawNoticeBracket(Canvas canvas, float left, float top, float right, float bottom,
                                   float scale, float phase, float alpha) {
        float inset = 5f * scale;
        float curve = 13f * scale;
        path.reset();
        path.moveTo(right, top + curve);
        path.cubicTo(right, top + 3f * scale, left + inset, top,
                left + inset, top + curve);
        path.lineTo(left + inset, bottom - curve);
        path.cubicTo(left + inset, bottom, right, bottom - 3f * scale, right, bottom - curve);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(accentGradient(left, top, right, bottom, phase));
        paint.setAlpha(Math.round(255f * alpha));
        canvas.drawPath(path, paint);
        clearPaintEffects();
    }

    private float accentPhase(long now) {
        float speed = clamp(stateStore.getHudFloat(SETTING_GRADIENT_SPEED, 20f), 0f, 40f);
        return ((now / 1000f) * (speed / 300f)) % 1f;
    }

    private LinearGradient accentGradient(float x1, float y1, float x2, float y2, float phase) {
        return new LinearGradient(x1, y1, x2, y2, new int[]{
                accentAt(phase), accentAt(phase + .33f), accentAt(phase + .66f)
        }, null, Shader.TileMode.CLAMP);
    }

    private static int accentAt(float phase) {
        float normalized = phase - (float) Math.floor(phase);
        float position = normalized * ACCENT_COLORS.length;
        int first = (int) position;
        float fraction = position - first;
        int start = ACCENT_COLORS[first % ACCENT_COLORS.length];
        int end = ACCENT_COLORS[(first + 1) % ACCENT_COLORS.length];
        return Color.argb(255,
                Math.round(Color.red(start) + (Color.red(end) - Color.red(start)) * fraction),
                Math.round(Color.green(start) + (Color.green(end) - Color.green(start)) * fraction),
                Math.round(Color.blue(start) + (Color.blue(end) - Color.blue(start)) * fraction));
    }

    private void ensureAnimationLoop() {
        if (!released && !animationPosted) {
            animationPosted = true;
            postOnAnimation(animationTick);
        }
    }

    private void clearPaintEffects() {
        paint.setShader(null);
        paint.clearShadowLayer();
        paint.setAlpha(255);
    }

    private static float progress(long elapsed, long duration) {
        return clamp(elapsed / (float) duration, 0f, 1f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int withAlpha(int color, float alpha) {
        int sourceAlpha = (color >>> 24) & 0xFF;
        return (Math.round(sourceAlpha * clamp(alpha, 0f, 1f)) << 24) | (color & 0x00FFFFFF);
    }

    private static final class RowSpec {
        final String id;
        final String name;
        final String summary;
        final float width;
        final float drawScale;

        RowSpec(String id, String name, String summary, float width, float drawScale) {
            this.id = id;
            this.name = name;
            this.summary = summary;
            this.width = width;
            this.drawScale = drawScale;
        }
    }

    private static final class RowNode {
        final String id;
        long enterAt;
        long exitAt;
        boolean present;
        String name;
        String summary;
        float targetY;
        float currentY;
        float targetWidth;
        float currentWidth;
        float drawScale;

        RowNode(RowSpec spec, float y, long now) {
            id = spec.id;
            name = spec.name;
            summary = spec.summary;
            targetY = currentY = y;
            targetWidth = currentWidth = spec.width;
            drawScale = spec.drawScale;
            enterAt = now;
        }
    }

    private static class NoticeRequest {
        final String moduleName;
        final boolean enabled;

        NoticeRequest(String moduleName, boolean enabled) {
            this.moduleName = moduleName;
            this.enabled = enabled;
        }
    }

    private static final class NoticeNode extends NoticeRequest {
        final long shownAt;
        boolean initialized;
        float currentTop;

        NoticeNode(NoticeRequest request, long shownAt) {
            super(request.moduleName, request.enabled);
            this.shownAt = shownAt;
        }
    }
}
