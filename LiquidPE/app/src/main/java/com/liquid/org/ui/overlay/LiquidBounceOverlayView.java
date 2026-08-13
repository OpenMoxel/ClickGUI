/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.liquid.org.ui.overlay.LiquidBounceModels.TopTab;
import com.liquid.org.ui.overlay.LiquidBounceModels.ModuleEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.NotificationSpec;

import java.util.Locale;

public class LiquidBounceOverlayView extends View implements ClickGuiRenderer.Listener {
    public interface SearchRequestListener { void onSearchRequested(String currentQuery); }

    private static final float CLICK_GUI_MIN_SCALE = 0.85f;
    private static final float CLICK_GUI_TRANSLATION_PX = 8f;
    private static final int CLICK_GUI_BACKDROP_MAX_ALPHA = 120;

    private final ReferenceViewport viewport = new ReferenceViewport();
    private final ResponsiveTypography typography;
    private final LiquidBounceDataStore dataStore;
    private final ClickGuiRenderer clickGuiRenderer;
    private final HudRenderer hudRenderer;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint debugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF contentClip = new RectF(0, 0, LiquidBounceUiMetrics.CONTENT_WIDTH, LiquidBounceUiMetrics.CONTENT_HEIGHT);
    private final ClickGuiTransition clickGuiTransition = new ClickGuiTransition(true);
    private boolean clickGuiVisible = true;
    private boolean hudVisible = true;
    private boolean transparentBase;
    private boolean debugBounds;
    private boolean referenceGrid;
    private boolean hudTouchCaptured;
    private float logicalTouchX;
    private float logicalTouchY;
    private SearchRequestListener searchRequestListener;
    private Runnable clickGuiClosedListener;
    private boolean closedCallbackDispatched;
    private long previousFrame;
    private float smoothedFrameMs = 16.67f;

    public LiquidBounceOverlayView(Context context) { this(context, null); }
    public LiquidBounceOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LiquidBounceFonts.initialize(context);
        typography = new ResponsiveTypography(context);
        dataStore = LiquidBounceDataStore.createDemo();
        clickGuiRenderer = new ClickGuiRenderer(context, dataStore, this, typography);
        hudRenderer = new HudRenderer(dataStore, typography);
        dataStore.addModuleStateListener(new LiquidBounceDataStore.ModuleStateListener() {
            @Override public void onModuleEnabledChanged(ModuleEntry module, boolean enabled) {
                hudRenderer.onModuleChanged(module);
                hudRenderer.pushModuleNotification(module, enabled);
                invalidate();
            }

            @Override public void onModuleMetadataChanged(ModuleEntry module) {
                hudRenderer.onModuleMetadataChanged(module);
                invalidate();
            }
        });
        setFocusable(true);
        setFocusableInTouchMode(true);
        debugPaint.setTypeface(LiquidBounceFonts.regular());
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        viewport.update(w, h);
        typography.update(getContext(), w, h);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        float clickGuiProgress = clickGuiTransition.get(now);
        if (previousFrame != 0) smoothedFrameMs = smoothedFrameMs * .9f + (now - previousFrame) * .1f;
        previousFrame = now;
        if (transparentBase) canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        else canvas.drawColor(Color.WHITE);
        if (clickGuiProgress > 0f) {
            canvas.drawColor(Color.argb(Math.round(CLICK_GUI_BACKDROP_MAX_ALPHA * clickGuiProgress), 0, 0, 0));
        }
        int save = canvas.save();
        viewport.apply(canvas);
        canvas.clipRect(contentClip);
        if (hudVisible) hudRenderer.draw(canvas, debugBounds, now);
        if (clickGuiProgress > 0f) drawAnimatedClickGui(canvas, clickGuiProgress, now);
        if (hudVisible) hudRenderer.drawBindsOverlay(canvas, debugBounds);
        if (referenceGrid) drawReferenceGrid(canvas);
        if (debugBounds || referenceGrid) drawDebugInfo(canvas, now);
        canvas.restoreToCount(save);
        boolean transitionRunning = clickGuiTransition.isRunning(now);
        if (!transitionRunning && !clickGuiVisible && clickGuiProgress <= 0f && !closedCallbackDispatched) {
            closedCallbackDispatched = true;
            if (clickGuiClosedListener != null) post(clickGuiClosedListener);
        }
        if (transitionRunning || clickGuiRenderer.hasActiveAnimations(now) || hudRenderer.hasActiveAnimations(now)) {
            postInvalidateOnAnimation();
        }
    }


    /** One transform and one alpha layer keep tabs, search, panels, and controls perfectly synchronized. */
    private void drawAnimatedClickGui(Canvas canvas, float progress, long now) {
        float scale = CLICK_GUI_MIN_SCALE + (1f - CLICK_GUI_MIN_SCALE) * progress;
        // Convert the requested screen-pixel drift into logical canvas units after viewport scaling.
        float translationY = CLICK_GUI_TRANSLATION_PX * (1f - progress) / Math.max(0.001f, viewport.getScale());
        float centerX = LiquidBounceUiMetrics.CONTENT_WIDTH * 0.5f;
        float centerY = LiquidBounceUiMetrics.CONTENT_HEIGHT * 0.5f;
        int transformSave = canvas.save();
        canvas.translate(0f, translationY);
        canvas.scale(scale, scale, centerX, centerY);
        int alphaLayer = canvas.saveLayerAlpha(contentClip, Math.round(255f * progress));
        clickGuiRenderer.draw(canvas, debugBounds, now);
        canvas.restoreToCount(alphaLayer);
        canvas.restoreToCount(transformSave);
    }

    private void drawReferenceGrid(Canvas canvas) {
        debugPaint.setStyle(Paint.Style.STROKE); debugPaint.setStrokeWidth(1); debugPaint.setColor(0x3F55D8FF);
        for (int x = 0; x <= 2940; x += 100) canvas.drawLine(x, 0, x, 1837, debugPaint);
        for (int y = 0; y <= 1837; y += 100) canvas.drawLine(0, y, 2940, y, debugPaint);
        debugPaint.setColor(0xA0FF3CAC); debugPaint.setStrokeWidth(2);
        canvas.drawLine(logicalTouchX - 12, logicalTouchY, logicalTouchX + 12, logicalTouchY, debugPaint);
        canvas.drawLine(logicalTouchX, logicalTouchY - 12, logicalTouchX, logicalTouchY + 12, debugPaint);
    }

    private void drawDebugInfo(Canvas canvas, long now) {
        debugPaint.setStyle(Paint.Style.FILL); debugPaint.setColor(0xD9000000); canvas.drawRoundRect(18, 1730, 620, 1818, 8, 8, debugPaint);
        debugPaint.setColor(Color.WHITE); debugPaint.setTextSize(typography.size(18));
        float fps = 1000f / Math.max(1f, smoothedFrameMs);
        String line1 = String.format(Locale.US, "scale %.4f  fps %.1f  frame %.2f ms", viewport.getScale(), fps, smoothedFrameMs);
        String line2 = String.format(Locale.US, "logical %.1f, %.1f  uptime %d", logicalTouchX, logicalTouchY, now);
        canvas.drawText(line1, 32, 1765, debugPaint); canvas.drawText(line2, 32, 1798, debugPaint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        PointF p = viewport.toLogical(event.getX(), event.getY());
        logicalTouchX = p.x; logicalTouchY = p.y;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            requestFocus();
            hudTouchCaptured = hudVisible && hudRenderer.onTouchDown(p.x, p.y);
            if (!hudTouchCaptured && clickGuiVisible) clickGuiRenderer.onTouchDown(p.x, p.y);
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (hudTouchCaptured) hudRenderer.onTouchMove(p.x, p.y);
            else if (clickGuiVisible) clickGuiRenderer.onTouchMove(p.x, p.y);
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (hudTouchCaptured) hudRenderer.onTouchUp();
            else if (clickGuiVisible) clickGuiRenderer.onTouchUp(p.x, p.y);
            hudTouchCaptured = false;
            performClick();
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (hudTouchCaptured) hudRenderer.onTouchUp();
            else if (clickGuiVisible) clickGuiRenderer.onTouchUp(p.x, p.y);
            hudTouchCaptured = false;
        }
        invalidate();
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override public void onTopTabChanged(TopTab tab) {
        invalidate();
    }

    @Override public void onSearchRequested() {
        if (searchRequestListener != null) searchRequestListener.onSearchRequested(clickGuiRenderer.getSearchQuery());
    }

    @Override public void onLanguageChanged() {
        hudRenderer.onLanguageChanged();
        invalidate();
    }

    /** Animate the existing ClickGUI as a single object; rapid reversals continue from the current value. */
    public void setClickGuiVisible(boolean visible) {
        if (clickGuiVisible == visible && clickGuiTransition.getTarget() == (visible ? 1f : 0f)) return;
        clickGuiVisible = visible;
        closedCallbackDispatched = false;
        clickGuiTransition.animateTo(visible ? 1f : 0f,
                visible ? LiquidBounceUiDurations.CLICK_GUI_OPEN : LiquidBounceUiDurations.CLICK_GUI_CLOSE);
        postInvalidateOnAnimation();
    }

    /** Used before a detached overlay is first shown, so no invisible transition runs in the background. */
    public void setClickGuiVisibleImmediately(boolean visible) {
        clickGuiVisible = visible;
        closedCallbackDispatched = !visible;
        clickGuiTransition.snapTo(visible ? 1f : 0f);
        invalidate();
    }

    public boolean isClickGuiVisibleRequested() { return clickGuiVisible; }
    public void setTransparentBase(boolean transparent) { transparentBase = transparent; invalidate(); }
    public void setOnClickGuiClosedListener(@Nullable Runnable listener) { clickGuiClosedListener = listener; }
    public void setHudVisible(boolean visible) { hudVisible = visible; invalidate(); }
    public void setActiveTopTab(TopTab tab) { clickGuiRenderer.setActiveTab(tab); }
    public void setModuleEnabled(String name, boolean enabled) { dataStore.setModuleEnabled(name, enabled); }
    public void setModuleSuffix(String name, String suffix) { dataStore.setModuleSuffix(name, suffix); }
    public void setModuleKeyBind(String name, String keyName) { dataStore.setModuleKeyBind(name, keyName); }
    public void setModuleShownInArrayList(String name, boolean shown) { dataStore.setShowInArrayList(name, shown); }
    public void setModuleShownInBinds(String name, boolean shown) { dataStore.setShowInBinds(name, shown); }
    public void setCategoryExpanded(String categoryName, boolean expanded) { clickGuiRenderer.setCategoryExpanded(categoryName, expanded); invalidate(); }
    public void setModuleSettingsExpanded(String categoryName, String moduleName, boolean expanded) { clickGuiRenderer.setModuleSettingsExpanded(categoryName, moduleName, expanded); invalidate(); }
    public void pushModuleNotification(String moduleName, boolean enabled) { ModuleEntry module = dataStore.findModule(moduleName); if (module != null) hudRenderer.pushModuleNotification(module, enabled); invalidate(); }
    public void pushNotification(NotificationSpec notification) { hudRenderer.pushNotification(notification); invalidate(); }
    public void setDebugBoundsEnabled(boolean enabled) { debugBounds = enabled; invalidate(); }
    public void setReferenceGridEnabled(boolean enabled) { referenceGrid = enabled; invalidate(); }
    /** 进入当前功能面板的布局编辑模式；网格仅在该模式或拖动期间显示。 */
    public void enterEditMode() { clickGuiRenderer.enterEditMode(); invalidate(); }
    public void exitEditMode() { clickGuiRenderer.exitEditMode(); invalidate(); }
    public boolean isEditMode() { return clickGuiRenderer.isEditMode(); }
    public void saveLayout() { clickGuiRenderer.savePanelLayout(); }
    public void restoreLayout() { clickGuiRenderer.restorePanelLayout(); invalidate(); }
    public void resetLayout() { clickGuiRenderer.resetPanelLayout(); invalidate(); }
    public void setSearchQuery(String query) { clickGuiRenderer.setSearchQuery(query); invalidate(); }
    public void setSearchRequestListener(SearchRequestListener listener) { searchRequestListener = listener; }
    public LiquidBounceDataStore getDataStore() { return dataStore; }
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && clickGuiRenderer.onBackPressed()) {
            invalidate();
            return true;
        }
        String keyName = KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "").replace('_', ' ');
        ModuleEntry changed = clickGuiRenderer.onKeyPressed(keyName);
        if (changed != null) {
            invalidate();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /** Time-based visibility interpolation with direction-specific Solar-style easing. */
    private static final class ClickGuiTransition {
        private float startValue;
        private float value;
        private float targetValue;
        private long startTime;
        private long duration;

        ClickGuiTransition(boolean visible) {
            startValue = value = targetValue = visible ? 1f : 0f;
        }

        float get(long now) {
            if (value == targetValue || duration <= 0L) return targetValue;
            float t = Math.max(0f, Math.min(1f, (now - startTime) / (float) duration));
            float eased = targetValue > startValue ? easeOutCubic(t) : easeInCubic(t);
            value = startValue + (targetValue - startValue) * eased;
            if (t >= 1f) value = targetValue;
            return value;
        }

        void animateTo(float target, long fullDuration) {
            long now = SystemClock.uptimeMillis();
            float current = get(now);
            startValue = current;
            targetValue = target;
            startTime = now;
            duration = Math.max(1L, Math.round(fullDuration * Math.abs(target - current)));
        }

        void snapTo(float target) {
            startValue = value = targetValue = target;
            startTime = 0L;
            duration = 0L;
        }

        boolean isRunning(long now) {
            return value != targetValue && now - startTime < duration;
        }

        float getTarget() { return targetValue; }

        private static float easeOutCubic(float t) {
            float p = 1f - t;
            return 1f - p * p * p;
        }

        private static float easeInCubic(float t) { return t * t * t; }
    }
}
