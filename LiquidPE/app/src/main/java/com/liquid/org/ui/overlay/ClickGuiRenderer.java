/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.liquid.org.ui.overlay.LiquidBounceModels.BindSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.CategoryPanel;
import com.liquid.org.ui.overlay.LiquidBounceModels.ColorSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.DropdownSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.ModuleEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.MultiSelectSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.RangeSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.SettingEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.SettingGroup;
import com.liquid.org.ui.overlay.LiquidBounceModels.SliderSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.ToggleSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.TopTab;
import com.liquid.org.ui.grid.PanelLayoutInfo;
import com.liquid.org.ui.grid.PanelLayoutStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClickGuiRenderer {
    /* 默认位置仅用于首次打开；之后每个面板都保存自己的自由画布坐标。 */
    private static final int PANEL_GRID_COLUMNS = 7;
    private static final float PANEL_GRID_LEFT = 30f;
    private static final float PANEL_GRID_TOP = 194f;
    private static final float PANEL_GRID_CELL_WIDTH = 375f;
    private static final float PANEL_GRID_CELL_HEIGHT = 451f;
    private static final float PANEL_GRID_GAP_X = 30f;
    private static final float PANEL_GRID_GAP_Y = 30f;
    private static final float PANEL_CONTENT_TOP = 60f;
    private static final float PANEL_CONTENT_BOTTOM_PADDING = 8f;
    private static final float PANEL_SCREEN_BOTTOM_INSET = 30f;
    public interface Listener {
        void onTopTabChanged(TopTab tab);
        void onSearchRequested();
        void onLanguageChanged();
    }

    private static final int HIT_TOGGLE = 1;
    private static final int HIT_SLIDER = 2;
    private static final int HIT_RANGE_LOW = 3;
    private static final int HIT_RANGE_HIGH = 4;
    private static final int HIT_DROPDOWN = 5;
    private static final int HIT_DROPDOWN_OPTION = 6;
    private static final int HIT_MULTI_OPTION = 7;
    private static final int HIT_BIND = 8;
    private static final int HIT_COLOR = 9;
    private static final int HIT_GROUP = 10;
    private static final int HIT_SV = 11;
    private static final int HIT_HUE = 12;
    private static final int HIT_ALPHA = 13;

    private static final class SettingHit {
        final RectF bounds = new RectF();
        SettingEntry setting;
        int action;
        int index;
    }

    private final LiquidBounceDataStore dataStore;
    private final ResponsiveTypography typography;
    private final List<CategoryPanel> panels;
    private final Listener listener;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final SettingHit[] settingHits = new SettingHit[128];
    private int settingHitCount;
    private SettingHit activeHit;
    private DropdownSetting overlayDropdown;
    private CategoryPanel overlayPanel;
    private final RectF overlayDropdownBounds = new RectF();
    private TopTab activeTab = TopTab.CLICK_GUI;
    private String searchQuery = "";
    private CategoryPanel scrollPanel;
    private CategoryPanel pressedPanel;
    private CategoryPanel layoutDraggingPanel;
    private float downY;
    private float lastY;
    private float downX;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean moved;
    private boolean isEditMode;
    private boolean longPressTriggered;
    private float originalPanelX;
    private float originalPanelY;
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private final PanelLayoutStorage layoutStorage;
    private final Runnable longPressRunnable = new Runnable() {
        @Override public void run() {
            if (pressedPanel != null && !moved && activeHit == null) beginPanelDrag(pressedPanel, downX, downY);
        }
    };

    public ClickGuiRenderer(Context context, LiquidBounceDataStore dataStore, Listener listener,
                            ResponsiveTypography typography) {
        this.dataStore = dataStore;
        this.panels = dataStore.getCategories();
        this.listener = listener;
        this.typography = typography;
        this.layoutStorage = new PanelLayoutStorage(context);
        text.setTypeface(LiquidBounceFonts.medium());
        stroke.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < settingHits.length; i++) settingHits[i] = new SettingHit();
        restorePanelLayout();
    }

    public void draw(Canvas canvas, boolean debugBounds, long now) {
        settingHitCount = 0;
        overlayDropdown = null;
        overlayPanel = null;
        fill.setShader(null);
        drawTopTabs(canvas);
        drawSearch(canvas);
        for (CategoryPanel panel : panels) drawPanel(canvas, panel, debugBounds, now);
        if (overlayDropdown != null && overlayPanel != null) drawDropdownOverlay(canvas, overlayPanel, overlayDropdown);
    }

    private void drawTopTabs(Canvas canvas) {
        setFill(LiquidBounceUiColors.PANEL_DEEP);
        rect.set(1320, 14, 1620, 56);
        canvas.drawRoundRect(rect, 21, 21, fill);
        String[] labels = {LiquidBounceI18n.t("ClickGUI"), LiquidBounceI18n.t("HUD Editor"), LiquidBounceI18n.t("Settings")};
        float[] widths = {94, 111, 95};
        float x = 1320;
        for (int i = 0; i < labels.length; i++) {
            boolean active = activeTab.ordinal() == i;
            if (active) {
                stroke.setColor(LiquidBounceUiColors.ACCENT); stroke.setStrokeWidth(2);
                rect.set(x + 3, 17, x + widths[i] - 3, 53); canvas.drawRoundRect(rect, 18, 18, stroke);
            }
            setText(active ? Color.WHITE : 0xFFB7B8BD, 14, active ? LiquidBounceFonts.bold() : LiquidBounceFonts.medium());
            drawCentered(canvas, labels[i], x, 14, widths[i], 42, text);
            x += widths[i];
        }
    }

    private void drawSearch(Canvas canvas) {
        setFill(0xCC010405); rect.set(1020, 104, 1920, 179); canvas.drawRoundRect(rect, 37, 37, fill);
        setText(searchQuery.isEmpty() ? LiquidBounceUiColors.TEXT_MUTED : LiquidBounceUiColors.TEXT_NORMAL, 24, LiquidBounceFonts.regular());
        drawBaseline(canvas, searchQuery.isEmpty() ? "Search" : searchQuery, 1058, 104, 75, text);
        if (!searchQuery.isEmpty()) {
            stroke.setColor(LiquidBounceUiColors.TEXT_MUTED); stroke.setStrokeWidth(3);
            canvas.drawLine(1874, 132, 1894, 152, stroke); canvas.drawLine(1894, 132, 1874, 152, stroke);
        }
    }

    private void drawPanel(Canvas canvas, CategoryPanel panel, boolean debugBounds, long now) {
        int save = canvas.save();
        if (panel == layoutDraggingPanel) {
            canvas.scale(1.04f, 1.04f, panel.x + 187.5f, panel.y + 120f);
        }
        float progress = panel.expansion.get(now);
        panel.contentHeight = calculateContentHeight(panel, now);
        float availableBodyHeight = calculateAvailableBodyHeight(panel);
        float bodyViewportHeight = Math.min(panel.contentHeight, availableBodyHeight);
        float bodyHeight = bodyViewportHeight * progress;
        float panelHeight = PANEL_CONTENT_TOP + bodyHeight;
        setFill(LiquidBounceUiColors.PANEL);
        rect.set(panel.x, panel.y, panel.x + LiquidBounceUiMetrics.PANEL_WIDTH, panel.y + panelHeight);
        canvas.drawRoundRect(rect, LiquidBounceUiMetrics.PANEL_RADIUS, LiquidBounceUiMetrics.PANEL_RADIUS, fill);
        setFill(LiquidBounceUiColors.PANEL_DEEP);
        rect.set(panel.x, panel.y, panel.x + LiquidBounceUiMetrics.PANEL_WIDTH,
                panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT);
        canvas.drawRoundRect(rect, LiquidBounceUiMetrics.PANEL_RADIUS, LiquidBounceUiMetrics.PANEL_RADIUS, fill);
        canvas.drawRect(panel.x,
                panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT - LiquidBounceUiMetrics.PANEL_RADIUS,
                panel.x + LiquidBounceUiMetrics.PANEL_WIDTH,
                panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT, fill);
        setFill(LiquidBounceUiColors.PANEL_HIGHLIGHT);
        rect.set(panel.x + 3f, panel.y + 1f,
                panel.x + LiquidBounceUiMetrics.PANEL_WIDTH - 3f, panel.y + 2f);
        canvas.drawRect(rect, fill);
        setFill(LiquidBounceUiColors.ACCENT);
        canvas.drawRect(panel.x,
                panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT - 2f,
                panel.x + LiquidBounceUiMetrics.PANEL_WIDTH,
                panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT + 1f, fill);
        drawCategoryIcon(canvas, panel.name, panel.x + 34, panel.y + 29);
        setText(Color.WHITE, 22, LiquidBounceFonts.bold()); drawBaseline(canvas, LiquidBounceI18n.t(panel.name), panel.x + 62, panel.y, 58, text);
        setText(Color.WHITE, 26, LiquidBounceFonts.regular()); drawCentered(canvas, panel.expanded ? "−" : "+", panel.x + 326, panel.y, 44, 58, text);
        if (bodyHeight > .5f) {
            int bodySave = canvas.save();
            canvas.clipRect(panel.x, panel.y + PANEL_CONTENT_TOP, panel.x + 375, panel.y + panelHeight);
            float maxScroll = Math.max(0, panel.contentHeight - bodyViewportHeight);
            // 搜索或折叠设置后内容可能立即变短，先同步截断当前位置，避免出现短暂空白区域。
            panel.scrollOffset = clamp(panel.scrollOffset, 0, maxScroll);
            panel.targetScrollOffset = clamp(panel.targetScrollOffset, 0, maxScroll);
            panel.scrollOffset += (panel.targetScrollOffset - panel.scrollOffset) * .28f;
            float y = panel.y + PANEL_CONTENT_TOP - panel.scrollOffset;
            for (ModuleEntry module : panel.modules) {
                if (!matches(module.name)) continue;
                drawModuleRow(canvas, panel, module, y, now);
                y += 53;
                float expansion = module.settingsProgress.get(now);
                if (module.hasSettings() && expansion > .001f) {
                    float fullHeight = measureSettings(module.settings, now);
                    int nested = canvas.save();
                    canvas.clipRect(panel.x, y, panel.x + 375, y + fullHeight * expansion);
                    drawSettings(canvas, panel, module.settings, y, 0, now);
                    canvas.restoreToCount(nested);
                    y += fullHeight * expansion;
                }
            }
            if (panel.contentHeight > bodyViewportHeight) drawScrollbar(canvas, panel, bodyHeight, bodyViewportHeight);
            canvas.restoreToCount(bodySave);
        }
        if (debugBounds) drawDebugRect(canvas, panel.x, panel.y, 375, panelHeight);
        canvas.restoreToCount(save);
    }

    /** 单一内容尺寸来源：仅统计搜索后仍可见模块及其当前展开的设置项。 */
    private float calculateContentHeight(CategoryPanel panel, long now) {
        float height = 0;
        for (ModuleEntry module : panel.modules) if (matches(module.name)) {
            height += 53;
            if (module.hasSettings()) height += measureSettings(module.settings, now) * module.settingsProgress.get(now);
        }
        return height > 0 ? height + PANEL_CONTENT_BOTTOM_PADDING : 0;
    }

    /** 最大高度随面板当前位置和当前可用屏幕高度变化，不再来自分类的固定高度。 */
    private float calculateAvailableBodyHeight(CategoryPanel panel) {
        return Math.max(0, LiquidBounceUiMetrics.CONTENT_HEIGHT - PANEL_SCREEN_BOTTOM_INSET - panel.y - PANEL_CONTENT_TOP);
    }

    /** 所有视觉、裁剪和触摸边界均使用该动态总高度。 */
    private float calculatePanelHeight(CategoryPanel panel, long now) {
        panel.contentHeight = calculateContentHeight(panel, now);
        return PANEL_CONTENT_TOP + Math.min(panel.contentHeight, calculateAvailableBodyHeight(panel)) * panel.expansion.get(now);
    }

    private float calculateMaxScroll(CategoryPanel panel, long now) {
        panel.contentHeight = calculateContentHeight(panel, now);
        return Math.max(0, panel.contentHeight - Math.min(panel.contentHeight, calculateAvailableBodyHeight(panel)));
    }

    private float measureSettings(List<SettingEntry> settings, long now) {
        float height = 10;
        for (SettingEntry setting : settings) if (setting.visible) height += measureSetting(setting, now);
        return height + 8;
    }

    private float measureSetting(SettingEntry setting, long now) {
        switch (setting.type) {
            case TOGGLE: return control(43);
            case SLIDER: return control(69);
            case RANGE: return control(72);
            case DROPDOWN: return control(61);
            case BIND: return control(79);
            case MULTI_SELECT:
                MultiSelectSetting multi = (MultiSelectSetting) setting;
                return control(77 + Math.max(0, (multi.options.size() - 1) / 3) * 38);
            case COLOR: return control(55 + (((ColorSetting) setting).expanded ? 216 : 0));
            case GROUP:
                SettingGroup group = (SettingGroup) setting;
                return control(46) + measureSettings(group.children, now) * group.expansion.get(now);
            default: return 0;
        }
    }

    private void drawModuleRow(Canvas canvas, CategoryPanel panel, ModuleEntry module, float y, long now) {
        int color = LiquidBounceUiColors.blend(LiquidBounceUiColors.TEXT_NORMAL, LiquidBounceUiColors.ACCENT, module.enabledProgress.get(now));
        setText(color, 19, LiquidBounceFonts.medium()); drawCentered(canvas, LiquidBounceI18n.moduleName(module.id, module.name), panel.x + 8, y, 332, 53, text);
        if (!module.hasSettings()) return;
        stroke.setColor(0xFF8A8B91); stroke.setStrokeWidth(2.4f);
        float cx = panel.x + 344, cy = y + 26;
        if (module.settingsExpanded) {
            canvas.drawLine(cx - 6, cy - 3, cx, cy + 3, stroke); canvas.drawLine(cx, cy + 3, cx + 6, cy - 3, stroke);
        } else {
            canvas.drawLine(cx - 3, cy - 6, cx + 3, cy, stroke); canvas.drawLine(cx + 3, cy, cx - 3, cy + 6, stroke);
        }
    }

    private float drawSettings(Canvas canvas, CategoryPanel panel, List<SettingEntry> settings, float startY, int level, long now) {
        float fullHeight = measureSettings(settings, now);
        float railX = panel.x + 17 + level * 13;
        setFill(0x4A06173E); canvas.drawRect(railX - 7, startY, railX + 6, startY + fullHeight, fill);
        setFill(LiquidBounceUiColors.ACCENT); canvas.drawRect(railX, startY, railX + 3, startY + fullHeight, fill);
        float y = startY + 10;
        for (SettingEntry setting : settings) {
            if (!setting.visible) continue;
            y = drawSetting(canvas, panel, setting, y, level, now);
        }
        return y + 8;
    }

    private float drawSetting(Canvas canvas, CategoryPanel panel, SettingEntry setting, float y, int level, long now) {
        float x = panel.x + 27 + level * 13;
        float right = panel.x + 358;
        switch (setting.type) {
            case BIND: return drawBind(canvas, (BindSetting) setting, x, right, y);
            case TOGGLE: return drawToggle(canvas, (ToggleSetting) setting, x, right, y, now);
            case SLIDER: return drawSlider(canvas, (SliderSetting) setting, x, right, y);
            case RANGE: return drawRange(canvas, (RangeSetting) setting, x, right, y);
            case DROPDOWN: return drawDropdown(canvas, panel, (DropdownSetting) setting, x, right, y);
            case MULTI_SELECT: return drawMulti(canvas, (MultiSelectSetting) setting, x, right, y);
            case COLOR: return drawColor(canvas, (ColorSetting) setting, x, right, y);
            case GROUP:
                SettingGroup group = (SettingGroup) setting;
                float groupHeight = control(46);
                setText(Color.WHITE, 19, LiquidBounceFonts.bold()); drawBaseline(canvas, LiquidBounceI18n.t(group.name), x, y, groupHeight, text); drawChevron(canvas, right - control(8), y + groupHeight * .5f, group.expanded);
                addHit(group, HIT_GROUP, -1, x - control(5), y, right, y + groupHeight);
                float childHeight = measureSettings(group.children, now) * group.expansion.get(now);
                if (childHeight > .5f) {
                    int save = canvas.save(); canvas.clipRect(panel.x, y + groupHeight, panel.x + 375, y + groupHeight + childHeight);
                    drawSettings(canvas, panel, group.children, y + groupHeight, level + 1, now); canvas.restoreToCount(save);
                }
                return y + groupHeight + childHeight;
            default: return y;
        }
    }

    private float drawBind(Canvas canvas, BindSetting setting, float x, float right, float y) {
        float height = control(63), radius = control(6);
        rect.set(x, y, right, y + height); setFill(0xFF010205); canvas.drawRoundRect(rect, radius, radius, fill); stroke.setColor(LiquidBounceUiColors.ACCENT); stroke.setStrokeWidth(control(2.5f)); canvas.drawRoundRect(rect, radius, radius, stroke);
        setText(Color.WHITE, 17, LiquidBounceFonts.medium()); drawCentered(canvas, LiquidBounceI18n.t(setting.name), x, y + control(5), right - x, control(25), text);
        setText(setting.listening ? LiquidBounceUiColors.ACCENT : LiquidBounceUiColors.TEXT_MUTED, 16, LiquidBounceFonts.regular()); drawCentered(canvas, setting.listening ? LiquidBounceI18n.t("Press a key") : setting.value, x, y + control(30), right - x, control(25), text);
        addHit(setting, HIT_BIND, -1, x, y, right, y + height); return y + control(79);
    }

    private float drawToggle(Canvas canvas, ToggleSetting setting, float x, float right, float y, long now) {
        float progress = setting.progress.get(now);
        float width = control(49), height = control(21), knobRadius = control(10.5f), top = y + control(10);
        setFill(LiquidBounceUiColors.blend(LiquidBounceUiColors.TOGGLE_OFF, LiquidBounceUiColors.ACCENT_DEEP, progress)); rect.set(x, top, x + width, top + height); canvas.drawRoundRect(rect, height * .5f, height * .5f, fill);
        setFill(LiquidBounceUiColors.blend(0xFFF4F4F4, LiquidBounceUiColors.ACCENT, progress)); canvas.drawCircle(x + control(11) + control(27) * progress, top + height * .5f, knobRadius, fill);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 18, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name), x + control(59), y, control(40), text);
        addHit(setting, HIT_TOGGLE, -1, x - control(5), y, right, y + control(42)); return y + control(43);
    }

    private float drawSlider(Canvas canvas, SliderSetting setting, float x, float right, float y) {
        float progress = (setting.value - setting.min) / Math.max(.0001f, setting.max - setting.min);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 18, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name), x, y, control(29), text);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 17, LiquidBounceFonts.regular()); drawRightBaseline(canvas, formatValue(setting.value) + (setting.unit.isEmpty() ? "" : " " + LiquidBounceI18n.t(setting.unit)), right, y, control(29), text);
        float ty = y + control(42), trackHeight = control(4), thumbRadius = control(9); setFill(LiquidBounceUiColors.TRACK); canvas.drawRoundRect(x, ty, right, ty + trackHeight, trackHeight * .5f, trackHeight * .5f, fill); setFill(LiquidBounceUiColors.ACCENT); canvas.drawRoundRect(x, ty, x + (right - x) * progress, ty + trackHeight, trackHeight * .5f, trackHeight * .5f, fill); canvas.drawCircle(x + (right - x) * progress, ty + trackHeight * .5f, thumbRadius, fill);
        addHit(setting, HIT_SLIDER, -1, x, y + control(25), right, y + control(59)); return y + control(69);
    }

    private float drawRange(Canvas canvas, RangeSetting setting, float x, float right, float y) {
        float span = Math.max(.0001f, setting.max - setting.min), low = (setting.low - setting.min) / span, high = (setting.high - setting.min) / span;
        setText(LiquidBounceUiColors.TEXT_NORMAL, 18, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name), x, y, control(29), text);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 17, LiquidBounceFonts.regular()); drawRightBaseline(canvas, formatValue(setting.low) + " - " + formatValue(setting.high), right, y, control(29), text);
        float ty = y + control(42), trackHeight = control(4), thumbRadius = control(9); setFill(LiquidBounceUiColors.TRACK); canvas.drawRoundRect(x, ty, right, ty + trackHeight, trackHeight * .5f, trackHeight * .5f, fill); setFill(LiquidBounceUiColors.ACCENT); canvas.drawRoundRect(x + (right - x) * low, ty, x + (right - x) * high, ty + trackHeight, trackHeight * .5f, trackHeight * .5f, fill); canvas.drawCircle(x + (right - x) * low, ty + trackHeight * .5f, thumbRadius, fill); canvas.drawCircle(x + (right - x) * high, ty + trackHeight * .5f, thumbRadius, fill);
        float mid = x + (right - x) * (low + high) * .5f; addHit(setting, HIT_RANGE_LOW, -1, x, y + control(25), mid, y + control(60)); addHit(setting, HIT_RANGE_HIGH, -1, mid, y + control(25), right, y + control(60)); return y + control(72);
    }

    private float drawDropdown(Canvas canvas, CategoryPanel panel, DropdownSetting setting, float x, float right, float y) {
        float height = control(42), radius = control(6);
        rect.set(x, y, right, y + height); setFill(LiquidBounceUiColors.ACCENT_SOFT); canvas.drawRoundRect(rect, radius, radius, fill);
        setText(Color.WHITE, 18, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name) + " • " + LiquidBounceI18n.t(setting.value), x + control(14), y, height, text); drawChevron(canvas, right - control(18), y + height * .5f, false);
        addHit(setting, HIT_DROPDOWN, -1, x, y, right, y + height);
        if (setting.open) { overlayDropdown = setting; overlayPanel = panel; overlayDropdownBounds.set(x, y, right, y + height); }
        return y + control(61);
    }

    private float drawMulti(Canvas canvas, MultiSelectSetting setting, float x, float right, float y) {
        setText(Color.WHITE, 18, LiquidBounceFonts.bold()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name), x, y, control(35), text);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 17, LiquidBounceFonts.medium()); drawRightBaseline(canvas, setting.selected.size() + " / " + setting.options.size(), right, y, control(35), text);
        float tx = x + control(22), ty = y + control(39);
        for (int i = 0; i < setting.options.size(); i++) {
            String option = setting.options.get(i); String display = LiquidBounceI18n.t(option); setText(LiquidBounceUiColors.ACCENT_SOFT, 17, LiquidBounceFonts.medium()); float w = text.measureText(display) + control(20);
            if (tx + w > right) { tx = x + control(22); ty += control(38); }
            float chipHeight = control(31), radius = control(5);
            rect.set(tx, ty, tx + w, ty + chipHeight); setFill(setting.selected.contains(option) ? 0xB2182A58 : 0x78101425); canvas.drawRoundRect(rect, radius, radius, fill); drawCentered(canvas, display, tx, ty, w, chipHeight, text);
            addHit(setting, HIT_MULTI_OPTION, i, tx, ty, tx + w, ty + chipHeight); tx += w + control(8);
        }
        return Math.max(y + control(77), ty + control(38));
    }

    private float drawColor(Canvas canvas, ColorSetting setting, float x, float right, float y) {
        setText(LiquidBounceUiColors.TEXT_NORMAL, 18, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name), x, y, control(42), text);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 17, LiquidBounceFonts.regular()); drawRightBaseline(canvas, toRgbaHex(setting.color), right - control(64), y, control(42), text);
        setFill(setting.color); rect.set(right - control(46), y + control(7), right, y + control(37)); canvas.drawRoundRect(rect, control(5), control(5), fill); stroke.setColor(LiquidBounceUiColors.ACCENT); stroke.setStrokeWidth(control(2)); canvas.drawRoundRect(rect, control(5), control(5), stroke);
        addHit(setting, HIT_COLOR, -1, x, y, right, y + control(48));
        if (!setting.expanded) return y + control(55);
        float pickerY = y + control(55), svRight = right - control(62), pickerHeight = control(192);
        RectF sv = addHit(setting, HIT_SV, -1, x, pickerY, svRight, pickerY + pickerHeight).bounds; drawChecker(canvas, sv, control(10));
        int pure = Color.HSVToColor(new float[]{setting.hue, 1, 1}); fill.setShader(new LinearGradient(sv.left, 0, sv.right, 0, Color.WHITE, pure, Shader.TileMode.CLAMP)); canvas.drawRect(sv, fill); fill.setShader(new LinearGradient(0, sv.top, 0, sv.bottom, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)); canvas.drawRect(sv, fill); fill.setShader(null);
        stroke.setColor(Color.WHITE); stroke.setStrokeWidth(control(3)); canvas.drawCircle(sv.left + setting.saturation * sv.width(), sv.top + (1 - setting.brightness) * sv.height(), control(11), stroke);
        RectF hue = addHit(setting, HIT_HUE, -1, right - control(48), pickerY, right - control(34), pickerY + pickerHeight).bounds; int[] rainbow = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED}; fill.setShader(new LinearGradient(0, hue.top, 0, hue.bottom, rainbow, null, Shader.TileMode.CLAMP)); canvas.drawRoundRect(hue, control(2), control(2), fill); fill.setShader(null); canvas.drawCircle(hue.centerX(), hue.top + setting.hue / 360f * hue.height(), control(11), stroke);
        RectF alpha = addHit(setting, HIT_ALPHA, -1, right - control(20), pickerY, right - control(6), pickerY + pickerHeight).bounds; drawChecker(canvas, alpha, control(7)); int opaque = Color.HSVToColor(new float[]{setting.hue, setting.saturation, setting.brightness}); fill.setShader(new LinearGradient(0, alpha.top, 0, alpha.bottom, opaque, Color.TRANSPARENT, Shader.TileMode.CLAMP)); canvas.drawRoundRect(alpha, control(2), control(2), fill); fill.setShader(null); canvas.drawCircle(alpha.centerX(), alpha.top + (1 - setting.alpha) * alpha.height(), control(11), stroke);
        return pickerY + control(216);
    }

    private void drawDropdownOverlay(Canvas canvas, CategoryPanel panel, DropdownSetting setting) {
        float panelBottom = panel.y + calculatePanelHeight(panel, android.os.SystemClock.uptimeMillis());
        int save = canvas.save(); canvas.clipRect(panel.x, panel.y + PANEL_CONTENT_TOP, panel.x + 375, panelBottom);
        float rowHeight = control(41), top = overlayDropdownBounds.bottom + control(3), h = setting.options.size() * rowHeight;
        setFill(0xFF030507); rect.set(overlayDropdownBounds.left, top, overlayDropdownBounds.right, top + h); canvas.drawRoundRect(rect, control(6), control(6), fill); stroke.setColor(LiquidBounceUiColors.ACCENT); stroke.setStrokeWidth(control(2)); canvas.drawRoundRect(rect, control(6), control(6), stroke);
        for (int i = 0; i < setting.options.size(); i++) {
            String option = setting.options.get(i); String display = LiquidBounceI18n.t(option); setText(option.equals(setting.value) ? LiquidBounceUiColors.ACCENT : LiquidBounceUiColors.TEXT_NORMAL, 18, LiquidBounceFonts.medium()); drawCentered(canvas, display, rect.left, top + i * rowHeight, rect.width(), rowHeight, text);
            addHit(setting, HIT_DROPDOWN_OPTION, i, rect.left, top + i * rowHeight, rect.right, top + (i + 1) * rowHeight);
        }
        canvas.restoreToCount(save);
    }

    private SettingHit addHit(SettingEntry setting, int action, int index, float left, float top, float right, float bottom) {
        SettingHit hit = settingHits[Math.min(settingHitCount++, settingHits.length - 1)]; hit.setting = setting; hit.action = action; hit.index = index; hit.bounds.set(left, top, right, bottom); return hit;
    }

    public boolean onTouchDown(float x, float y) {
        cancelPanelLongPress();
        moved = false; longPressTriggered = false; downX = x; downY = lastY = y; activeHit = findHit(x, y);
        if (activeHit != null && (activeHit.action == HIT_SLIDER || activeHit.action == HIT_RANGE_LOW || activeHit.action == HIT_RANGE_HIGH || activeHit.action == HIT_SV || activeHit.action == HIT_HUE || activeHit.action == HIT_ALPHA)) { updateDrag(activeHit, x, y); return true; }
        for (CategoryPanel panel : panels) {
            float height = calculatePanelHeight(panel, android.os.SystemClock.uptimeMillis());
            if (x >= panel.x && x <= panel.x + 375 && y >= panel.y && y <= panel.y + height) {
                scrollPanel = panel;
                /* 标题栏既是面板的空白拖拽区域，也是短按展开/收起的原有入口。 */
                if (y <= panel.y + 59) {
                    pressedPanel = panel;
                    longPressHandler.postDelayed(longPressRunnable, android.view.ViewConfiguration.getLongPressTimeout());
                }
                return true;
            }
        }
        return true;
    }

    public boolean onTouchMove(float x, float y) {
        if (layoutDraggingPanel != null) { updatePanelDrag(x, y); return true; }
        if (activeHit != null && (activeHit.action == HIT_SLIDER || activeHit.action == HIT_RANGE_LOW || activeHit.action == HIT_RANGE_HIGH || activeHit.action == HIT_SV || activeHit.action == HIT_HUE || activeHit.action == HIT_ALPHA)) { moved = true; updateDrag(activeHit, x, y); return true; }
        if (Math.hypot(x - downX, y - downY) > 7) { moved = true; cancelPanelLongPress(); }
        if (scrollPanel != null && Math.abs(y - downY) > 7) { moved = true; scrollPanel.targetScrollOffset -= y - lastY; lastY = y; return true; }
        return false;
    }

    public boolean onTouchUp(float x, float y) {
        cancelPanelLongPress();
        if (layoutDraggingPanel != null) finishPanelDrag(true);
        else if (!moved) {
            if (isEditMode && pressedPanel == null) exitEditMode();
            else handleTap(x, y);
        }
        activeHit = null;
        scrollPanel = null;
        pressedPanel = null;
        return true;
    }

    private void handleTap(float x, float y) {
        SettingHit hit = findHit(x, y);
        if (hit != null) { handleSettingTap(hit); return; }
        for (CategoryPanel panel : panels) for (ModuleEntry module : panel.modules) for (SettingEntry setting : module.settings) closeDropdowns(setting);
        if (y >= 14 && y <= 56 && x >= 1320 && x <= 1620) { float local = x - 1320; setActiveTab(local < 94 ? TopTab.CLICK_GUI : local < 205 ? TopTab.HUD_EDITOR : TopTab.SETTINGS); return; }
        if (x >= 1020 && x <= 1920 && y >= 104 && y <= 179) { if (!searchQuery.isEmpty() && x > 1850) setSearchQuery(""); else listener.onSearchRequested(); return; }
        for (CategoryPanel panel : panels) {
            if (x < panel.x || x > panel.x + 375) continue;
            float panelHeight = calculatePanelHeight(panel, android.os.SystemClock.uptimeMillis());
            if (y < panel.y || y > panel.y + panelHeight) continue;
            if (y >= panel.y && y <= panel.y + 59) { panel.expanded = !panel.expanded; panel.expansion.animateTo(panel.expanded ? 1 : 0, LiquidBounceUiDurations.PANEL); return; }
            float cy = panel.y + PANEL_CONTENT_TOP - panel.scrollOffset;
            for (ModuleEntry module : panel.modules) {
                if (!matches(module.name)) continue;
                if (y >= cy && y < cy + 53) {
                    if (module.hasSettings() && x > panel.x + 300) { module.settingsExpanded = !module.settingsExpanded; module.settingsProgress.animateTo(module.settingsExpanded ? 1 : 0, LiquidBounceUiDurations.GROUP); }
                    else { dataStore.toggleModule(module.id); }
                    return;
                }
                cy += 53 + (module.hasSettings() ? measureSettings(module.settings, android.os.SystemClock.uptimeMillis()) * module.settingsProgress.get() : 0);
            }
        }
    }

    private void handleSettingTap(SettingHit hit) {
        switch (hit.action) {
            case HIT_TOGGLE:
                ToggleSetting toggle = (ToggleSetting) hit.setting; toggle.value = !toggle.value; toggle.progress.animateTo(toggle.value ? 1 : 0, LiquidBounceUiDurations.TOGGLE); break;
            case HIT_DROPDOWN:
                DropdownSetting dropdown = (DropdownSetting) hit.setting; dropdown.open = !dropdown.open; break;
            case HIT_DROPDOWN_OPTION:
                DropdownSetting select = (DropdownSetting) hit.setting; select.value = select.options.get(hit.index); select.open = false;
                if ("lang".equals(select.id)) {
                    LiquidBounceI18n.setChinese("Chinese".equals(select.value));
                    if (listener != null) listener.onLanguageChanged();
                }
                break;
            case HIT_MULTI_OPTION:
                MultiSelectSetting multi = (MultiSelectSetting) hit.setting; String option = multi.options.get(hit.index); if (!multi.selected.remove(option)) multi.selected.add(option); break;
            case HIT_BIND:
                BindSetting bind = (BindSetting) hit.setting; bind.listening = !bind.listening; break;
            case HIT_COLOR:
                ColorSetting color = (ColorSetting) hit.setting; color.expanded = !color.expanded; break;
            case HIT_GROUP:
                SettingGroup group = (SettingGroup) hit.setting; group.expanded = !group.expanded; group.expansion.animateTo(group.expanded ? 1 : 0, LiquidBounceUiDurations.GROUP); break;
            default: break;
        }
    }

    private void updateDrag(SettingHit hit, float x, float y) {
        float p = clamp((x - hit.bounds.left) / Math.max(1, hit.bounds.width()), 0, 1);
        if (hit.action == HIT_SLIDER) { SliderSetting s = (SliderSetting) hit.setting; s.value = s.min + (s.max - s.min) * p; }
        else if (hit.action == HIT_RANGE_LOW) { RangeSetting s = (RangeSetting) hit.setting; s.low = Math.min(s.high, s.min + (s.max - s.min) * p); }
        else if (hit.action == HIT_RANGE_HIGH) { RangeSetting s = (RangeSetting) hit.setting; s.high = Math.max(s.low, s.min + (s.max - s.min) * p); }
        else {
            ColorSetting c = (ColorSetting) hit.setting;
            if (hit.action == HIT_SV) { c.saturation = p; c.brightness = 1 - clamp((y - hit.bounds.top) / hit.bounds.height(), 0, 1); }
            else if (hit.action == HIT_HUE) c.hue = 360 * clamp((y - hit.bounds.top) / hit.bounds.height(), 0, 1);
            else if (hit.action == HIT_ALPHA) c.alpha = 1 - clamp((y - hit.bounds.top) / hit.bounds.height(), 0, 1);
            c.updateColor();
        }
    }

    private void beginPanelDrag(CategoryPanel panel, float x, float y) {
        if (panel == null || panel.layoutInfo == null) return;
        isEditMode = true;
        longPressTriggered = true;
        layoutDraggingPanel = panel;
        originalPanelX = panel.x;
        originalPanelY = panel.y;
        dragOffsetX = x - panel.x;
        dragOffsetY = y - panel.y;
        updatePanelDrag(x, y);
    }

    /** 拖动时直接使用手指位置，不进行网格吸附或面板碰撞规避。 */
    private void updatePanelDrag(float x, float y) {
        if (layoutDraggingPanel == null || layoutDraggingPanel.layoutInfo == null) return;
        CategoryPanel panel = layoutDraggingPanel;
        panel.x = clamp(x - dragOffsetX, 0, LiquidBounceUiMetrics.CONTENT_WIDTH - 375);
        panel.y = clamp(y - dragOffsetY, PANEL_GRID_TOP, LiquidBounceUiMetrics.CONTENT_HEIGHT - PANEL_CONTENT_TOP);
        moved = true;
    }

    private void finishPanelDrag(boolean commit) {
        CategoryPanel panel = layoutDraggingPanel;
        if (panel == null || panel.layoutInfo == null) return;
        if (!commit) {
            panel.x = originalPanelX;
            panel.y = originalPanelY;
        }
        panel.layoutInfo.setFreePosition(panel.x, panel.y);
        layoutDraggingPanel = null;
        savePanelLayout();
    }

    public void restorePanelLayout() {
        Map<String, PanelLayoutInfo> persisted = layoutStorage.load();
        for (int index = 0; index < panels.size(); index++) {
            CategoryPanel panel = panels.get(index);
            PanelLayoutInfo saved = persisted.get(panel.id);
            int defaultSpanY = "player".equals(panel.id) ? 2 : 1;
            if (saved == null) saved = new PanelLayoutInfo(panel.id, index < PANEL_GRID_COLUMNS ? index : 0, index < PANEL_GRID_COLUMNS ? 0 : 1, 1, defaultSpanY, true);
            panel.layoutInfo = new PanelLayoutInfo(panel.id, saved.gridX, saved.gridY,
                    Math.min(PANEL_GRID_COLUMNS, Math.max(1, saved.spanX)), Math.max(1, saved.spanY), saved.visible);
            if (saved.hasFreePosition) {
                panel.x = clamp(saved.freeX, 0, LiquidBounceUiMetrics.CONTENT_WIDTH - 375);
                panel.y = clamp(saved.freeY, PANEL_GRID_TOP, LiquidBounceUiMetrics.CONTENT_HEIGHT - PANEL_CONTENT_TOP);
                panel.layoutInfo.setFreePosition(panel.x, panel.y);
            } else {
                // 兼容旧版网格布局：第一次升级时转换为对应的自由坐标。
                panel.x = gridToX(Math.max(0, Math.min(PANEL_GRID_COLUMNS - panel.layoutInfo.spanX, saved.gridX)));
                panel.y = gridToY(Math.max(0, saved.gridY));
                panel.layoutInfo.setFreePosition(panel.x, panel.y);
            }
        }
        savePanelLayout();
    }

    public void savePanelLayout() {
        List<PanelLayoutInfo> infos = new ArrayList<>();
        for (CategoryPanel panel : panels) if (panel.layoutInfo != null) infos.add(panel.layoutInfo.copy());
        layoutStorage.save(infos);
    }

    public void resetPanelLayout() {
        layoutStorage.clear();
        for (int index = 0; index < panels.size(); index++) {
            CategoryPanel panel = panels.get(index);
            int spanY = "player".equals(panel.id) ? 2 : 1;
            panel.layoutInfo = new PanelLayoutInfo(panel.id, -1, -1, 1, spanY, true);
            panel.x = gridToX(index < PANEL_GRID_COLUMNS ? index : 0);
            panel.y = gridToY(index < PANEL_GRID_COLUMNS ? 0 : 1);
            panel.layoutInfo.setFreePosition(panel.x, panel.y);
        }
        savePanelLayout();
    }

    public void enterEditMode() { isEditMode = true; }
    public void exitEditMode() {
        cancelPanelLongPress();
        if (layoutDraggingPanel != null) finishPanelDrag(false);
        isEditMode = false;
        savePanelLayout();
    }
    public boolean isEditMode() { return isEditMode; }
    public boolean onBackPressed() { if (!isEditMode) return false; exitEditMode(); return true; }

    private float gridToX(int gridX) { return PANEL_GRID_LEFT + gridX * (PANEL_GRID_CELL_WIDTH + PANEL_GRID_GAP_X); }
    private float gridToY(int gridY) { return PANEL_GRID_TOP + gridY * (PANEL_GRID_CELL_HEIGHT + PANEL_GRID_GAP_Y); }
    private float spanWidth(int spanX) { return spanX * PANEL_GRID_CELL_WIDTH + Math.max(0, spanX - 1) * PANEL_GRID_GAP_X; }
    private float spanHeight(int spanY) { return spanY * PANEL_GRID_CELL_HEIGHT + Math.max(0, spanY - 1) * PANEL_GRID_GAP_Y; }
    private void cancelPanelLongPress() { longPressHandler.removeCallbacks(longPressRunnable); }

    private SettingHit findHit(float x, float y) {
        if (!isInsideVisiblePanel(x, y)) return null;
        for (int i = Math.min(settingHitCount, settingHits.length) - 1; i >= 0; i--) if (settingHits[i].bounds.contains(x, y)) return settingHits[i];
        return null;
    }

    private boolean isInsideVisiblePanel(float x, float y) {
        long now = android.os.SystemClock.uptimeMillis();
        for (CategoryPanel panel : panels) {
            if (x >= panel.x && x <= panel.x + 375 && y >= panel.y && y <= panel.y + calculatePanelHeight(panel, now)) return true;
        }
        return false;
    }
    private void closeDropdowns(SettingEntry setting) { if (setting instanceof DropdownSetting) ((DropdownSetting) setting).open = false; else if (setting instanceof SettingGroup) for (SettingEntry child : ((SettingGroup) setting).children) closeDropdowns(child); }

    public void setActiveTab(TopTab tab) { activeTab = tab; listener.onTopTabChanged(tab); }
    public TopTab getActiveTab() { return activeTab; }
    public void setSearchQuery(String query) { searchQuery = query == null ? "" : query.trim(); }
    public String getSearchQuery() { return searchQuery; }
    public void setCategoryExpanded(String name, boolean expanded) { CategoryPanel p = dataStore.findCategory(name); if (p != null) { p.expanded = expanded; p.expansion.animateTo(expanded ? 1 : 0, LiquidBounceUiDurations.PANEL); } }
    public void setModuleEnabled(String name, boolean enabled) { dataStore.setModuleEnabled(name, enabled); }
    public void setModuleSuffix(String name, String suffix) { dataStore.setModuleSuffix(name, suffix); }
    public void setModuleKeyBind(String name, String bind) { dataStore.setModuleKeyBind(name, bind); }
    public void setModuleSettingsExpanded(String category, String moduleName, boolean expanded) { ModuleEntry m = dataStore.findModule(moduleName); if (m != null && m.categoryId.equals(dataStore.findCategory(category) == null ? category : dataStore.findCategory(category).id) && m.hasSettings()) { m.settingsExpanded = expanded; m.settingsProgress.animateTo(expanded ? 1 : 0, LiquidBounceUiDurations.GROUP); } }
    public void setPanelScroll(String category, float offset) {
        CategoryPanel panel = dataStore.findCategory(category);
        if (panel != null) panel.scrollOffset = panel.targetScrollOffset = clamp(Math.max(0, offset), 0, calculateMaxScroll(panel, android.os.SystemClock.uptimeMillis()));
    }
    public void setDropdownOpen(String module, String settingId, boolean open) { SettingEntry setting = findSetting(dataStore.findModule(module), settingId); if (setting instanceof DropdownSetting) ((DropdownSetting) setting).open = open; }
    public void setColorExpanded(String module, String settingId, boolean expanded) { SettingEntry setting = findSetting(dataStore.findModule(module), settingId); if (setting instanceof ColorSetting) ((ColorSetting) setting).expanded = expanded; }

    public ModuleEntry onKeyPressed(String keyName) {
        for (CategoryPanel panel : panels) for (ModuleEntry module : panel.modules) {
            BindSetting listening = findListeningBind(module.settings);
            if (listening != null) {
                listening.value = keyName == null || keyName.isEmpty() ? "None" : keyName;
                listening.listening = false;
                dataStore.setModuleKeyBind(module.id, listening.value);
                return module;
            }
        }
        return null;
    }

    private SettingEntry findSetting(ModuleEntry module, String id) { if (module == null) return null; return findSetting(module.settings, id); }
    private SettingEntry findSetting(List<SettingEntry> list, String id) { for (SettingEntry s : list) { if (s.id.equals(id)) return s; if (s instanceof SettingGroup) { SettingEntry child = findSetting(((SettingGroup) s).children, id); if (child != null) return child; } } return null; }
    private BindSetting findListeningBind(List<SettingEntry> list) { for (SettingEntry setting : list) { if (setting instanceof BindSetting && ((BindSetting) setting).listening) return (BindSetting) setting; if (setting instanceof SettingGroup) { BindSetting child = findListeningBind(((SettingGroup) setting).children); if (child != null) return child; } } return null; }
    public boolean hasActiveAnimations(long now) { for (CategoryPanel p : panels) { if (p.expansion.isRunning(now) || Math.abs(p.targetScrollOffset - p.scrollOffset) > .5f) return true; for (ModuleEntry m : p.modules) { if (m.enabledProgress.isRunning(now) || m.settingsProgress.isRunning(now)) return true; if (settingsAnimating(m.settings, now)) return true; } } return false; }
    private boolean settingsAnimating(List<SettingEntry> list, long now) { for (SettingEntry s : list) { if (s instanceof ToggleSetting && ((ToggleSetting) s).progress.isRunning(now)) return true; if (s instanceof SettingGroup && (((SettingGroup) s).expansion.isRunning(now) || settingsAnimating(((SettingGroup) s).children, now))) return true; } return false; }

    private void drawScrollbar(Canvas canvas, CategoryPanel panel, float bodyHeight, float bodyMax) { float ratio = bodyMax / panel.contentHeight, thumb = Math.max(40, bodyHeight * ratio), maxScroll = panel.contentHeight - bodyMax, travel = Math.max(1, bodyHeight - thumb), top = panel.y + 60 + travel * panel.scrollOffset / Math.max(1, maxScroll); setFill(LiquidBounceUiColors.ACCENT); canvas.drawRoundRect(panel.x + 371, top, panel.x + 374, top + thumb, 2, 2, fill); }
    private void drawChevron(Canvas canvas, float cx, float cy, boolean down) { float s = control(1); stroke.setColor(Color.WHITE); stroke.setStrokeWidth(control(2)); if (down) { canvas.drawLine(cx - 5 * s, cy - 3 * s, cx, cy + 2 * s, stroke); canvas.drawLine(cx, cy + 2 * s, cx + 5 * s, cy - 3 * s, stroke); } else { canvas.drawLine(cx - 3 * s, cy - 5 * s, cx + 2 * s, cy, stroke); canvas.drawLine(cx + 2 * s, cy, cx - 3 * s, cy + 5 * s, stroke); } }
    private void drawChecker(Canvas canvas, RectF b, float size) { for (float yy = b.top; yy < b.bottom; yy += size) for (float xx = b.left; xx < b.right; xx += size) { int row = (int) ((yy - b.top) / size), col = (int) ((xx - b.left) / size); setFill(((row + col) & 1) == 0 ? 0xFFB8B8B8 : 0xFF6D6D6D); canvas.drawRect(xx, yy, Math.min(xx + size, b.right), Math.min(yy + size, b.bottom), fill); } }
    private void drawCategoryIcon(Canvas canvas, String name, float cx, float cy) { stroke.setColor(Color.WHITE); stroke.setStrokeWidth(3); stroke.setStrokeCap(Paint.Cap.ROUND); path.reset(); if ("Render".equals(name)) { rect.set(cx - 10, cy - 7, cx + 10, cy + 7); canvas.drawOval(rect, stroke); setFill(Color.WHITE); canvas.drawCircle(cx, cy, 3, fill); } else if ("Combat".equals(name)) { canvas.drawLine(cx - 9, cy - 9, cx + 9, cy + 9, stroke); canvas.drawLine(cx + 9, cy - 9, cx - 9, cy + 9, stroke); } else if ("Player".equals(name)) { setFill(Color.WHITE); canvas.drawCircle(cx, cy - 7, 6, fill); canvas.drawRoundRect(cx - 9, cy + 1, cx + 9, cy + 12, 4, 4, fill); } else if ("Movement".equals(name)) { path.moveTo(cx - 10, cy + 8); path.lineTo(cx - 2, cy); path.lineTo(cx + 1, cy - 8); path.lineTo(cx + 9, cy - 10); path.moveTo(cx, cy - 2); path.lineTo(cx + 10, cy + 7); path.moveTo(cx - 2, cy + 1); path.lineTo(cx - 7, cy + 12); canvas.drawPath(path, stroke); } else if ("World".equals(name)) { canvas.drawCircle(cx, cy, 10, stroke); canvas.drawLine(cx, cy - 10, cx, cy + 10, stroke); canvas.drawOval(cx - 6, cy - 10, cx + 6, cy + 10, stroke); } else { canvas.drawCircle(cx, cy, 8, stroke); canvas.drawCircle(cx + 5, cy - 7, 3, stroke); } stroke.setStrokeCap(Paint.Cap.BUTT); }
    private boolean matches(String name) { return searchQuery.isEmpty() || name.toLowerCase(Locale.ROOT).contains(searchQuery.toLowerCase(Locale.ROOT)); }
    private static String formatValue(float value) { if (Math.abs(value - Math.round(value)) < .0001f) return Integer.toString(Math.round(value)); return String.format(Locale.US, value < 1 ? "%.3f" : "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    private static String toRgbaHex(int color) { return String.format(Locale.US, "#%02X%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color)); }
    private float control(float referenceSize) { return typography.control(referenceSize); }
    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private void setFill(int color) { fill.setShader(null); fill.setStyle(Paint.Style.FILL); fill.setColor(color); }
    private void setText(int color, float size, Typeface face) { text.setColor(color); text.setTextSize(typography.size(size)); text.setTypeface(face); }
    private void drawCentered(Canvas c, String s, float x, float y, float w, float h, Paint p) { Paint.FontMetrics fm = p.getFontMetrics(); c.drawText(s, x + (w - p.measureText(s)) * .5f, y + (h - (fm.descent - fm.ascent)) * .5f - fm.ascent, p); }
    private void drawBaseline(Canvas c, String s, float x, float y, float h, Paint p) { Paint.FontMetrics fm = p.getFontMetrics(); c.drawText(s, x, y + (h - (fm.descent - fm.ascent)) * .5f - fm.ascent, p); }
    private void drawRightBaseline(Canvas c, String s, float right, float y, float h, Paint p) { drawBaseline(c, s, right - p.measureText(s), y, h, p); }
    private void drawDebugRect(Canvas c, float x, float y, float w, float h) { stroke.setColor(0xCCFF3CAC); stroke.setStrokeWidth(2); c.drawRect(x, y, x + w, y + h, stroke); setFill(0xFFFF3CAC); c.drawCircle(x + w * .5f, y + h * .5f, 4, fill); }
}
