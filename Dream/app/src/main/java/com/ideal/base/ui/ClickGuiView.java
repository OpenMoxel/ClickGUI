package com.ideal.base.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.ideal.base.module.ModuleController;
import com.ideal.base.module.ModuleRepository;
import com.ideal.base.state.OverlayStateStore;

import java.util.List;
import java.util.Locale;

/** Canvas panel whose complete geometry is expressed in the 1411 by 820 reference panel. */
public final class ClickGuiView extends View {

    public interface Callback {
        void onToggleEnabled(String moduleId);
        void onToggleShortcut(String moduleId);
        void onSetRange(String moduleId, float value);
        void onSetFov(String moduleId, float value);
        void onSetDelay(String moduleId, float value);
        void onSetScaffoldMode(String moduleId, String mode);
        void onSetHudHidden(String moduleId, boolean hidden);
        void onSetHudFloat(String setting, float value);
        void onCollapse();
    }

    public static final float REFERENCE_WIDTH = 1411f;
    public static final float REFERENCE_HEIGHT = 820f;

    private static final int COLOR_TOP = 0xFF242946;
    private static final int COLOR_BODY = 0xFF24253C;
    private static final int COLOR_SIDE = 0xFF252945;
    private static final int COLOR_CARD = 0xFF292B42;
    private static final int COLOR_SELECTED = 0xFF1B1E29;
    private static final int COLOR_TEXT = 0xFFF2EFF7;
    private static final int COLOR_SECONDARY = 0xFFAAA4B7;
    private static final int COLOR_MUTED = 0xFF858093;
    private static final int COLOR_OFF_TRACK = 0xFF49445C;
    private static final int COLOR_OFF_KNOB = 0xFF8E879C;
    private static final int COLOR_ACCENT_START = 0xFFA84A9E;
    private static final int COLOR_ACCENT_END = 0xFF586FB8;

    private static final float SIDE_WIDTH = 343f;
    private static final float CONTENT_LEFT = 365f;
    private static final float CONTENT_RIGHT = 1384f;
    private static final float TOP_BAR_HEIGHT = 100f;
    private static final float HUD_ROW_TOP = 205f;
    private static final float HUD_ROW_STEP = 73f;
    private static final float HUD_ROW_HEIGHT = 65f;
    private static final float HUD_SLIDER_START = 620f;
    private static final float HUD_SLIDER_END = 1280f;

    private final ModuleRepository repository;
    private final ModuleController controller;
    private final OverlayStateStore stateStore;
    private final Callback callback;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Path clipPath = new Path();
    private final RectF rect = new RectF();

    private String category = ModuleRepository.COMBAT;
    private String selectedModuleId;
    private String activeSlider;
    private boolean modeExpanded;
    private float scale = 1f;
    private float revealAlpha = 1f;
    private float contentAlpha = 1f;
    private float contentTranslation;
    private ValueAnimator contentAnimator;

    public ClickGuiView(Context context, ModuleRepository repository,
                        ModuleController controller, OverlayStateStore stateStore, Callback callback) {
        super(context);
        this.repository = repository;
        this.controller = controller;
        this.stateStore = stateStore;
        this.callback = callback;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    }

    public void setRevealAlpha(float alpha) {
        revealAlpha = Math.max(0f, Math.min(1f, alpha));
        invalidate();
    }

    public void animateCategoryChange(float direction, long duration) {
        if (contentAnimator != null) {
            contentAnimator.cancel();
        }
        contentAnimator = ValueAnimator.ofFloat(0f, 1f);
        contentAnimator.setDuration(duration);
        contentAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        contentAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            contentAlpha = fraction;
            contentTranslation = direction * (1f - fraction) * 18f;
            invalidate();
        });
        contentAnimator.start();
    }

    public String getSelectedModuleId() {
        return selectedModuleId;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        scale = Math.min(w / REFERENCE_WIDTH, h / REFERENCE_HEIGHT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(scale, scale);
        drawPanelShell(canvas);
        drawTopBar(canvas);
        drawSideBar(canvas);
        int layer = canvas.saveLayerAlpha(SIDE_WIDTH, TOP_BAR_HEIGHT, REFERENCE_WIDTH,
                REFERENCE_HEIGHT, Math.round(255f * revealAlpha * contentAlpha));
        canvas.translate(contentTranslation, 0f);
        if (selectedModuleId == null) {
            drawModuleList(canvas);
        } else {
            drawModuleDetail(canvas, repository.getModule(selectedModuleId));
        }
        canvas.restoreToCount(layer);
        canvas.restore();
    }

    private void drawPanelShell(Canvas canvas) {
        rect.set(0, 0, REFERENCE_WIDTH, REFERENCE_HEIGHT);
        clipPath.reset();
        clipPath.addRoundRect(rect, 22f, 22f, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clipPath);
        paint.setColor(COLOR_BODY);
        canvas.drawRect(0, 0, REFERENCE_WIDTH, REFERENCE_HEIGHT, paint);
        paint.setColor(COLOR_TOP);
        canvas.drawRect(0, 0, REFERENCE_WIDTH, TOP_BAR_HEIGHT, paint);
        paint.setColor(COLOR_SIDE);
        canvas.drawRect(0, TOP_BAR_HEIGHT, SIDE_WIDTH, REFERENCE_HEIGHT, paint);
        paint.setColor(0x553B3A5B);
        canvas.drawRect(SIDE_WIDTH, TOP_BAR_HEIGHT, SIDE_WIDTH + 1.5f, REFERENCE_HEIGHT - 14f, paint);
        paint.setColor(0x663A3C5D);
        canvas.drawRect(0, TOP_BAR_HEIGHT - 1f, REFERENCE_WIDTH, TOP_BAR_HEIGHT, paint);
        canvas.restore();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.3f);
        paint.setColor(0x558984B0);
        canvas.drawRoundRect(rect, 22f, 22f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawTopBar(Canvas canvas) {
        drawHamburger(canvas, 45f, 50f);
        paint.setColor(0xFF8E8AA2);
        canvas.drawCircle(99f, 50f, 15f, paint);
        paint.setColor(COLOR_TEXT);
        canvas.drawCircle(94f, 47f, 2.2f, paint);
        canvas.drawCircle(102f, 47f, 2.2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        canvas.drawArc(92f, 49f, 106f, 60f, 15f, 150f, false, paint);
        paint.setStyle(Paint.Style.FILL);
        drawText(canvas, "ideal", 128f, 59f, 25f, COLOR_TEXT, true);
        drawSearch(canvas, 319f, 50f);

        String crumb = selectedModuleId == null ? category : repository.getModule(selectedModuleId).name;
        rect.set(354f, 25f, 604f, 66f);
        paint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                COLOR_ACCENT_START, COLOR_ACCENT_END, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, 9f, 9f, paint);
        paint.setShader(null);
        drawCategoryIcon(canvas, selectedModuleId == null ? category
                : repository.getModule(selectedModuleId).category, 382f, 45f, 0xFFF2EFF7);
        drawText(canvas, crumb, 404f, 53f, 20f, COLOR_TEXT, false);
        drawChevron(canvas, 579f, 46f, COLOR_TEXT, false);

        drawMinimize(canvas, 1303f, 49f);
        drawOutlineSquare(canvas, 1367f, 49f);
    }

    private void drawSideBar(Canvas canvas) {
        List<String> categories = repository.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            String item = categories.get(i);
            float top = 105f + i * 50f;
            if (item.equals(category)) {
                paint.setColor(COLOR_SELECTED);
                canvas.drawRoundRect(15f, top, SIDE_WIDTH - 17f, top + 46f, 9f, 9f, paint);
                paint.setColor(COLOR_TEXT);
                canvas.drawRoundRect(25f, top + 7f, 28f, top + 39f, 2f, 2f, paint);
            }
            drawCategoryIcon(canvas, item, 55f, top + 23f, COLOR_TEXT);
            drawText(canvas, item, 82f, top + 30f, 21f,
                    item.equals(category) ? COLOR_TEXT : COLOR_SECONDARY, item.equals(category));
        }
        paint.setColor(0x3EAAA4B7);
        canvas.drawRect(26f, 470f, SIDE_WIDTH - 27f, 471f, paint);
        drawDocument(canvas, 55f, 507f, COLOR_TEXT);
        drawText(canvas, "Configs", 82f, 514f, 21f, COLOR_SECONDARY, false);
        rect.set(20f, 545f, SIDE_WIDTH - 18f, 592f);
        paint.setColor(0xFF736C85);
        canvas.drawRoundRect(rect, 12f, 12f, paint);
        drawTextCentered(canvas, "Import Config +", rect.centerX(), 575f, 19f, COLOR_TEXT, false);
        drawTextCentered(canvas, "ideal", SIDE_WIDTH / 2f, 774f, 21f, COLOR_TEXT, true);
        drawTextCentered(canvas, "v1.0.0", SIDE_WIDTH / 2f, 796f, 17f, COLOR_SECONDARY, false);
    }

    private void drawModuleList(Canvas canvas) {
        List<ModuleRepository.ModuleDefinition> modules = repository.getModules(category);
        for (int i = 0; i < modules.size(); i++) {
            ModuleRepository.ModuleDefinition module = modules.get(i);
            float top = 108f + i * 117f;
            drawListCard(canvas, module, top);
        }
        drawText(canvas, "Category: " + category, 1252f, 792f, 18f, COLOR_SECONDARY, false);
    }

    private void drawListCard(Canvas canvas, ModuleRepository.ModuleDefinition module, float top) {
        rect.set(CONTENT_LEFT, top, CONTENT_RIGHT, top + 106f);
        paint.setColor(COLOR_CARD);
        canvas.drawRoundRect(rect, 13f, 13f, paint);
        drawText(canvas, module.name, CONTENT_LEFT + 20f, top + 45f, 27f, COLOR_TEXT, false);
        drawText(canvas, "Click to configure", CONTENT_LEFT + 20f, top + 72f, 19f, COLOR_SECONDARY, false);
        ModuleController.ModuleState state = controller.stateOf(module.id);
        drawSwitch(canvas, CONTENT_RIGHT - 72f, top + 53f, state != null && state.enabled);
    }

    private void drawModuleDetail(Canvas canvas, ModuleRepository.ModuleDefinition module) {
        if (module == null) {
            return;
        }
        ModuleController.ModuleState state = controller.stateOf(module.id);
        drawDetailHeader(canvas, module, state);
        if (HudOverlayView.MODULE_LIST.equals(module.id)) {
            drawHudModuleListDetail(canvas, module, state);
            return;
        }
        if (HudOverlayView.MODULE_NOTIFICATIONS.equals(module.id)) {
            drawHudNotificationsDetail(canvas, module, state);
            return;
        }
        drawShortcutRow(canvas, module, state, 205f);
        if ("killaura".equals(module.id)) {
            drawEnabledRow(canvas, module, state, 302f);
            drawSliderRow(canvas, "Range", state.range, 1f, 8f, 399f);
            drawSliderRow(canvas, "FOV", state.fov, 30f, 180f, 496f);
            drawSliderRow(canvas, "Delay", state.delay, 0f, 500f, 593f);
            drawOptionRow(canvas, "Target", state.target, 690f);
        } else if ("cheststealer".equals(module.id)) {
            drawSliderRow(canvas, "Delay", state.delay, 0f, 500f, 302f);
        } else if ("scaffold".equals(module.id)) {
            drawOptionRow(canvas, "Scaffold Mode", state.scaffoldMode, 302f);
            if (modeExpanded) {
                drawModePopup(canvas);
            }
        } else {
            drawTextCentered(canvas, "No settings available", (CONTENT_LEFT + CONTENT_RIGHT) / 2f,
                    325f, 20f, COLOR_MUTED, false);
        }
        String settingLabel = module.settingsCount == 1 ? "1 setting" : module.settingsCount + " settings";
        drawText(canvas, settingLabel, 1266f, 792f, 18f, COLOR_SECONDARY, false);
    }


    private void drawHudNotificationsDetail(Canvas canvas, ModuleRepository.ModuleDefinition module,
                                            ModuleController.ModuleState state) {
        drawHudSwitchRow(canvas, "显示快捷按钮", state != null && state.shortcutVisible, HUD_ROW_TOP);
        drawHudValueRow(canvas, "按键绑定", "无", HUD_ROW_TOP + HUD_ROW_STEP);
        drawHudSwitchRow(canvas, "隐藏", isHudHidden(module.id), HUD_ROW_TOP + HUD_ROW_STEP * 2f);
        drawText(canvas, "UI 预览 · 无模块后端", 1160f, 792f, 18f, COLOR_SECONDARY, false);
    }

    private void drawHudModuleListDetail(Canvas canvas, ModuleRepository.ModuleDefinition module,
                                         ModuleController.ModuleState state) {
        drawHudSwitchRow(canvas, "显示快捷按钮", state != null && state.shortcutVisible, HUD_ROW_TOP);
        drawHudValueRow(canvas, "按键绑定", "无", HUD_ROW_TOP + HUD_ROW_STEP);
        drawHudSwitchRow(canvas, "隐藏", isHudHidden(module.id), HUD_ROW_TOP + HUD_ROW_STEP * 2f);
        drawHudValueRow(canvas, "背景模式", "新版", HUD_ROW_TOP + HUD_ROW_STEP * 3f);
        drawHudValueRow(canvas, "右边条模式", "新版", HUD_ROW_TOP + HUD_ROW_STEP * 4f);
        drawHudSliderRow(canvas, "缩放比",
                stateStore.getHudFloat(HudOverlayView.SETTING_LIST_SCALE, 1f), 0f, 2f,
                HUD_ROW_TOP + HUD_ROW_STEP * 5f);
        drawHudSliderRow(canvas, "透明度",
                stateStore.getHudFloat(HudOverlayView.SETTING_LIST_OPACITY, .72f), 0f, 1f,
                HUD_ROW_TOP + HUD_ROW_STEP * 6f);
        drawHudSliderRow(canvas, "渐变速度",
                stateStore.getHudFloat(HudOverlayView.SETTING_GRADIENT_SPEED, 20f), 0f, 40f,
                HUD_ROW_TOP + HUD_ROW_STEP * 7f);
        drawText(canvas, "UI 预览 · 无模块后端", 1160f, 792f, 18f, COLOR_SECONDARY, false);
    }

    private void drawHudSwitchRow(Canvas canvas, String label, boolean enabled, float top) {
        drawHudDetailCard(canvas, top);
        drawText(canvas, label, 386f, top + 42f, 23f, COLOR_TEXT, false);
        drawSwitch(canvas, 1327f, top + HUD_ROW_HEIGHT * .5f, enabled);
    }

    private void drawHudValueRow(Canvas canvas, String label, String value, float top) {
        drawHudDetailCard(canvas, top);
        drawText(canvas, label, 386f, top + 42f, 23f, COLOR_TEXT, false);
        drawText(canvas, value, 1240f, top + 42f, 22f, COLOR_TEXT, false);
    }

    private void drawHudSliderRow(Canvas canvas, String label, float value, float min, float max,
                                  float top) {
        drawHudDetailCard(canvas, top);
        drawText(canvas, label, 386f, top + 42f, 23f, COLOR_TEXT, false);
        float fraction = Math.max(0f, Math.min(1f, (value - min) / (max - min)));
        float y = top + HUD_ROW_HEIGHT * .5f;
        float knobX = HUD_SLIDER_START + (HUD_SLIDER_END - HUD_SLIDER_START) * fraction;
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(8f);
        paint.setColor(0xFF24263C);
        canvas.drawLine(HUD_SLIDER_START, y, HUD_SLIDER_END, y, paint);
        paint.setShader(new LinearGradient(HUD_SLIDER_START, y, HUD_SLIDER_END, y,
                COLOR_ACCENT_START, COLOR_ACCENT_END, Shader.TileMode.CLAMP));
        canvas.drawLine(HUD_SLIDER_START, y, knobX, y, paint);
        paint.setShader(null);
        paint.setColor(0xFFE5E0F5);
        canvas.drawCircle(knobX, y, 14f, paint);
        drawText(canvas, formatValue(value), 1300f, top + 42f, 20f, COLOR_TEXT, false);
    }

    private void drawHudDetailCard(Canvas canvas, float top) {
        rect.set(CONTENT_LEFT, top, CONTENT_RIGHT, top + HUD_ROW_HEIGHT);
        paint.setColor(COLOR_CARD);
        canvas.drawRoundRect(rect, 12f, 12f, paint);
    }

    private boolean isHudHidden(String moduleId) {
        String key = HudOverlayView.MODULE_LIST.equals(moduleId)
                ? HudOverlayView.SETTING_LIST_HIDDEN : HudOverlayView.SETTING_NOTIFICATIONS_HIDDEN;
        return stateStore.getHudBoolean(key, false);
    }

    private void drawDetailHeader(Canvas canvas, ModuleRepository.ModuleDefinition module,
                                  ModuleController.ModuleState state) {
        rect.set(CONTENT_LEFT, 108f, CONTENT_RIGHT, 197f);
        paint.setColor(COLOR_CARD);
        canvas.drawRoundRect(rect, 13f, 13f, paint);
        paint.setColor(0xFF303458);
        canvas.drawRoundRect(386f, 122f, 505f, 181f, 10f, 10f, paint);
        drawText(canvas, "< Back", 405f, 159f, 22f, COLOR_TEXT, true);
        drawText(canvas, module.name, 528f, 164f, 29f, COLOR_TEXT, true);
        drawText(canvas, "Enabled", 1185f, 161f, 20f, COLOR_SECONDARY, false);
        drawSwitch(canvas, 1327f, 153f, state != null && state.enabled);
    }

    private void drawShortcutRow(Canvas canvas, ModuleRepository.ModuleDefinition module,
                                 ModuleController.ModuleState state, float top) {
        drawDetailCard(canvas, top);
        drawText(canvas, "Show Shortcut", 386f, top + 56f, 24f, COLOR_TEXT, false);
        drawSwitch(canvas, 1327f, top + 44f, state != null && state.shortcutVisible);
    }

    private void drawEnabledRow(Canvas canvas, ModuleRepository.ModuleDefinition module,
                                ModuleController.ModuleState state, float top) {
        drawDetailCard(canvas, top);
        drawText(canvas, "Enable", 386f, top + 56f, 24f, COLOR_TEXT, false);
        drawSwitch(canvas, 1327f, top + 44f, state != null && state.enabled);
    }

    private void drawSliderRow(Canvas canvas, String label, float value, float min, float max, float top) {
        drawDetailCard(canvas, top);
        drawText(canvas, label, 386f, top + 56f, 23f, COLOR_TEXT, false);
        float start = 487f;
        float end = 1286f;
        float y = top + 45f;
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(10f);
        paint.setColor(0xFF24263C);
        canvas.drawLine(start, y, end, y, paint);
        float fraction = (value - min) / (max - min);
        float knobX = start + Math.max(0f, Math.min(1f, fraction)) * (end - start);
        paint.setShader(new LinearGradient(start, y, end, y,
                COLOR_ACCENT_START, COLOR_ACCENT_END, Shader.TileMode.CLAMP));
        canvas.drawLine(start, y, knobX, y, paint);
        paint.setShader(null);
        paint.setColor(0xFFE5E0F5);
        canvas.drawCircle(knobX, y, 16f, paint);
        paint.setColor(0x40383357);
        canvas.drawCircle(knobX, y + 2f, 18f, paint);
        drawText(canvas, formatValue(value), 1306f, top + 56f, 20f, COLOR_TEXT, false);
    }

    private void drawOptionRow(Canvas canvas, String label, String value, float top) {
        drawDetailCard(canvas, top);
        drawText(canvas, label, 386f, top + 56f, 23f, COLOR_TEXT, false);
        rect.set(1080f, top + 14f, 1368f, top + 75f);
        paint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                COLOR_ACCENT_START, COLOR_ACCENT_END, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, 10f, 10f, paint);
        paint.setShader(null);
        drawTextCentered(canvas, value + " >", rect.centerX(), top + 53f, 20f, COLOR_TEXT, false);
    }

    private void drawModePopup(Canvas canvas) {
        rect.set(1083f, 398f, 1371f, 541f);
        paint.setColor(0xFF2E3151);
        canvas.drawRoundRect(rect, 10f, 10f, paint);
        rect.set(1083f, 398f, 1371f, 465f);
        paint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                COLOR_ACCENT_START, COLOR_ACCENT_END, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, 10f, 10f, paint);
        paint.setShader(null);
        drawTextCentered(canvas, "Normal", 1227f, 440f, 21f, COLOR_TEXT, true);
        drawTextCentered(canvas, "Tower", 1227f, 511f, 21f, COLOR_TEXT, false);
    }

    private void drawDetailCard(Canvas canvas, float top) {
        rect.set(CONTENT_LEFT, top, CONTENT_RIGHT, top + 89f);
        paint.setColor(COLOR_CARD);
        canvas.drawRoundRect(rect, 13f, 13f, paint);
    }

    private void drawSwitch(Canvas canvas, float centerX, float centerY, boolean enabled) {
        rect.set(centerX - 40f, centerY - 25f, centerX + 40f, centerY + 25f);
        if (enabled) {
            paint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                    COLOR_ACCENT_START, COLOR_ACCENT_END, Shader.TileMode.CLAMP));
        } else {
            paint.setColor(COLOR_OFF_TRACK);
        }
        canvas.drawRoundRect(rect, 25f, 25f, paint);
        paint.setShader(null);
        paint.setColor(enabled ? 0xFFF2EFF7 : COLOR_OFF_KNOB);
        canvas.drawCircle(enabled ? centerX + 20f : centerX - 20f, centerY, 19f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() / scale;
        float y = event.getY() / scale;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            activeSlider = sliderAt(x, y);
            if (activeSlider != null) {
                updateSlider(x);
                return true;
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && activeSlider != null) {
            updateSlider(x);
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (activeSlider != null) {
                updateSlider(x);
                activeSlider = null;
                performClick();
                return true;
            }
            handleTap(x, y);
            performClick();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            activeSlider = null;
            return true;
        }
        return true;
    }

    private void handleTap(float x, float y) {
        if (x >= 1270f && x <= 1336f && y <= TOP_BAR_HEIGHT) {
            callback.onCollapse();
            return;
        }
        if (x < SIDE_WIDTH && y >= 105f && y < 455f) {
            int index = (int) ((y - 105f) / 50f);
            List<String> categories = repository.getCategories();
            if (index >= 0 && index < categories.size()) {
                category = categories.get(index);
                selectedModuleId = null;
                modeExpanded = false;
                animateCategoryChange(1f, 140L);
            }
            return;
        }
        if (selectedModuleId == null) {
            handleListTap(x, y);
        } else {
            handleDetailTap(x, y);
        }
    }

    private void handleListTap(float x, float y) {
        if (x < CONTENT_LEFT || x > CONTENT_RIGHT || y < 108f) {
            return;
        }
        int index = (int) ((y - 108f) / 117f);
        List<ModuleRepository.ModuleDefinition> modules = repository.getModules(category);
        if (index < 0 || index >= modules.size()) {
            return;
        }
        float top = 108f + index * 117f;
        if (y > top + 106f) {
            return;
        }
        ModuleRepository.ModuleDefinition module = modules.get(index);
        if (x > CONTENT_RIGHT - 130f) {
            callback.onToggleEnabled(module.id);
        } else {
            selectedModuleId = module.id;
            modeExpanded = false;
            animateCategoryChange(-1f, 160L);
        }
    }

    private void handleDetailTap(float x, float y) {
        ModuleRepository.ModuleDefinition module = repository.getModule(selectedModuleId);
        if (module == null) {
            return;
        }
        if (isHudModule(module.id)) {
            handleHudDetailTap(module, x, y);
            return;
        }
        if (between(y, 108f, 197f)) {
            if (x < 515f) {
                selectedModuleId = null;
                modeExpanded = false;
                animateCategoryChange(1f, 160L);
            } else if (x > 1240f) {
                callback.onToggleEnabled(module.id);
            }
            return;
        }
        if (between(y, 205f, 294f) && x > 1210f) {
            callback.onToggleShortcut(module.id);
            return;
        }
        if ("killaura".equals(module.id) && between(y, 302f, 391f) && x > 1210f) {
            callback.onToggleEnabled(module.id);
            return;
        }
        if ("scaffold".equals(module.id)) {
            if (between(y, 302f, 391f) && x > 1040f) {
                modeExpanded = !modeExpanded;
                animateCategoryChange(0f, 120L);
            } else if (modeExpanded && between(y, 398f, 465f) && x > 1060f) {
                modeExpanded = false;
                callback.onSetScaffoldMode(module.id, "Normal");
                animateCategoryChange(0f, 120L);
            } else if (modeExpanded && between(y, 466f, 541f) && x > 1060f) {
                modeExpanded = false;
                callback.onSetScaffoldMode(module.id, "Tower");
                animateCategoryChange(0f, 120L);
            }
        }
    }


    private boolean isHudModule(String moduleId) {
        return HudOverlayView.MODULE_LIST.equals(moduleId)
                || HudOverlayView.MODULE_NOTIFICATIONS.equals(moduleId);
    }

    private void handleHudDetailTap(ModuleRepository.ModuleDefinition module, float x, float y) {
        if (between(y, 108f, 197f)) {
            if (x < 515f) {
                selectedModuleId = null;
                modeExpanded = false;
                animateCategoryChange(1f, 160L);
            } else if (x > 1240f) {
                callback.onToggleEnabled(module.id);
            }
            return;
        }
        if (between(y, HUD_ROW_TOP, HUD_ROW_TOP + HUD_ROW_HEIGHT) && x > 1210f) {
            callback.onToggleShortcut(module.id);
            return;
        }
        float hideTop = HUD_ROW_TOP + HUD_ROW_STEP * 2f;
        if (between(y, hideTop, hideTop + HUD_ROW_HEIGHT) && x > 1210f) {
            callback.onSetHudHidden(module.id, !isHudHidden(module.id));
        }
    }

    private String sliderAt(float x, float y) {
        if (selectedModuleId == null || x < 455f || x > 1300f) {
            return null;
        }
        if (HudOverlayView.MODULE_LIST.equals(selectedModuleId)) {
            if (between(y, HUD_ROW_TOP + HUD_ROW_STEP * 5f,
                    HUD_ROW_TOP + HUD_ROW_STEP * 5f + HUD_ROW_HEIGHT)) return "hud_scale";
            if (between(y, HUD_ROW_TOP + HUD_ROW_STEP * 6f,
                    HUD_ROW_TOP + HUD_ROW_STEP * 6f + HUD_ROW_HEIGHT)) return "hud_opacity";
            if (between(y, HUD_ROW_TOP + HUD_ROW_STEP * 7f,
                    HUD_ROW_TOP + HUD_ROW_STEP * 7f + HUD_ROW_HEIGHT)) return "hud_gradient_speed";
        }
        if ("killaura".equals(selectedModuleId)) {
            if (between(y, 399f, 488f)) return "range";
            if (between(y, 496f, 585f)) return "fov";
            if (between(y, 593f, 682f)) return "delay";
        } else if ("cheststealer".equals(selectedModuleId) && between(y, 302f, 391f)) {
            return "delay";
        }
        return null;
    }

    private void updateSlider(float x) {
        if ("hud_scale".equals(activeSlider) || "hud_opacity".equals(activeSlider)
                || "hud_gradient_speed".equals(activeSlider)) {
            float fraction = Math.max(0f, Math.min(1f,
                    (x - HUD_SLIDER_START) / (HUD_SLIDER_END - HUD_SLIDER_START)));
            if ("hud_scale".equals(activeSlider)) {
                callback.onSetHudFloat(HudOverlayView.SETTING_LIST_SCALE, fraction * 2f);
            } else if ("hud_opacity".equals(activeSlider)) {
                callback.onSetHudFloat(HudOverlayView.SETTING_LIST_OPACITY, fraction);
            } else {
                callback.onSetHudFloat(HudOverlayView.SETTING_GRADIENT_SPEED, fraction * 40f);
            }
            return;
        }
        float fraction = Math.max(0f, Math.min(1f, (x - 487f) / (1286f - 487f)));
        if ("range".equals(activeSlider)) {
            callback.onSetRange(selectedModuleId, 1f + fraction * 7f);
        } else if ("fov".equals(activeSlider)) {
            callback.onSetFov(selectedModuleId, 30f + fraction * 150f);
        } else if ("delay".equals(activeSlider)) {
            callback.onSetDelay(selectedModuleId, fraction * 500f);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private static boolean between(float value, float min, float max) {
        return value >= min && value <= max;
    }

    private static String formatValue(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private void drawText(Canvas canvas, String text, float x, float baseline, float size,
                          int color, boolean medium) {
        paint.setShader(null);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(text, x, baseline, paint);
    }

    private void drawTextCentered(Canvas canvas, String text, float centerX, float baseline,
                                  float size, int color, boolean medium) {
        paint.setShader(null);
        paint.setColor(color);
        paint.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, centerX, baseline, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawHamburger(Canvas canvas, float x, float y) {
        paint.setColor(COLOR_TEXT);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.3f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x - 13f, y - 9f, x + 13f, y - 9f, paint);
        canvas.drawLine(x - 13f, y, x + 13f, y, paint);
        canvas.drawLine(x - 13f, y + 9f, x + 13f, y + 9f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSearch(Canvas canvas, float x, float y) {
        paint.setColor(COLOR_TEXT);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.6f);
        canvas.drawCircle(x - 2f, y - 3f, 8f, paint);
        canvas.drawLine(x + 4f, y + 3f, x + 12f, y + 11f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawMinimize(Canvas canvas, float x, float y) {
        paint.setColor(COLOR_TEXT);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.7f);
        canvas.drawLine(x - 10f, y, x + 10f, y, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawOutlineSquare(Canvas canvas, float x, float y) {
        paint.setColor(COLOR_TEXT);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.6f);
        canvas.drawRoundRect(x - 12f, y - 12f, x + 12f, y + 12f, 4f, 4f, paint);
        canvas.drawLine(x + 6f, y - 8f, x + 9f, y - 8f, paint);
        canvas.drawLine(x + 9f, y - 8f, x + 9f, y - 5f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawChevron(Canvas canvas, float x, float y, int color, boolean up) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.8f);
        Path path = new Path();
        if (up) {
            path.moveTo(x - 5f, y + 3f);
            path.lineTo(x, y - 3f);
            path.lineTo(x + 5f, y + 3f);
        } else {
            path.moveTo(x - 3f, y - 6f);
            path.lineTo(x + 4f, y);
            path.lineTo(x - 3f, y + 6f);
        }
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCategoryIcon(Canvas canvas, String categoryName, float x, float y, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (ModuleRepository.COMBAT.equals(categoryName)) {
            canvas.drawLine(x - 9f, y + 9f, x + 9f, y - 9f, paint);
            canvas.drawLine(x - 10f, y + 1f, x - 3f, y + 8f, paint);
            canvas.drawLine(x + 1f, y - 8f, x + 8f, y - 1f, paint);
        } else if (ModuleRepository.MOVEMENT.equals(categoryName)) {
            canvas.drawCircle(x, y - 8f, 3f, paint);
            canvas.drawLine(x, y - 4f, x - 2f, y + 3f, paint);
            canvas.drawLine(x - 2f, y + 3f, x - 9f, y + 8f, paint);
            canvas.drawLine(x - 1f, y + 2f, x + 7f, y + 8f, paint);
            canvas.drawLine(x - 1f, y - 1f, x + 7f, y + 1f, paint);
        } else if (ModuleRepository.PLAYER.equals(categoryName)) {
            canvas.drawCircle(x, y - 6f, 6f, paint);
            canvas.drawCircle(x, y + 9f, 10f, paint);
        } else if (ModuleRepository.RENDER.equals(categoryName)) {
            rect.set(x - 13f, y - 8f, x + 13f, y + 8f);
            canvas.drawOval(rect, paint);
            canvas.drawCircle(x, y, 4f, paint);
        } else if (ModuleRepository.MISC.equals(categoryName)) {
            canvas.drawRoundRect(x - 10f, y - 10f, x + 10f, y + 10f, 3f, 3f, paint);
            canvas.drawLine(x - 5f, y - 5f, x + 5f, y - 5f, paint);
            canvas.drawLine(x - 5f, y + 5f, x + 5f, y + 5f, paint);
        } else if (ModuleRepository.WORLD.equals(categoryName)) {
            canvas.drawCircle(x, y, 11f, paint);
            canvas.drawLine(x - 10f, y, x + 10f, y, paint);
            canvas.drawArc(x - 6f, y - 11f, x + 6f, y + 11f, 90f, 180f, false, paint);
        } else {
            drawDocument(canvas, x, y, color);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawDocument(Canvas canvas, float x, float y, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        Path path = new Path();
        path.moveTo(x - 8f, y - 11f);
        path.lineTo(x + 4f, y - 11f);
        path.lineTo(x + 9f, y - 6f);
        path.lineTo(x + 9f, y + 11f);
        path.lineTo(x - 8f, y + 11f);
        path.close();
        canvas.drawPath(path, paint);
        canvas.drawLine(x + 4f, y - 11f, x + 4f, y - 6f, paint);
        canvas.drawLine(x + 4f, y - 6f, x + 9f, y - 6f, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
