/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import static com.pianai.xel.ClickGuiLayout.*;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Draws ClickGUI visual regions while leaving interaction ownership to the controller View. */
final class ClickGuiRenderer {

    private final ClickGuiCanvasPainter painter;
    private final Paint controlPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    ClickGuiRenderer(ClickGuiCanvasPainter painter) {
        this.painter = painter;
    }

    void drawMainPanel(Canvas canvas, List<ClickGuiCategory> categories, int selectedCategory,
                       String searchQuery, boolean searchFocused, List<ClickGuiModule> modules,
                       float listScrollY, float maxScroll) {
        painter.drawRounded(canvas, MAIN_X, MAIN_Y, MAIN_X + MAIN_WIDTH, MAIN_Y + MAIN_HEIGHT,
                MAIN_RADIUS, Color.argb(166, 17, 16, 19));
        painter.drawRounded(canvas, LEFT_X, LEFT_Y, LEFT_X + LEFT_WIDTH, LEFT_Y + LEFT_HEIGHT,
                LEFT_RADIUS, Color.argb(96, 82, 80, 91));
        drawSearch(canvas, searchQuery, searchFocused);
        drawCategories(canvas, categories, selectedCategory, searchQuery);
        drawModuleList(canvas, categories, selectedCategory, searchQuery, modules, listScrollY,
                maxScroll);
    }

    void drawWatermark(Canvas canvas) {
        painter.drawPixelText(canvas, "Pianaixel v3 Dev", WATERMARK_X, WATERMARK_Y, 20f,
                Color.argb(244, 255, 255, 255), Paint.Align.LEFT, false);
    }

    void drawBottomInfoBar(Canvas canvas) {
        painter.drawRect(canvas, 0f, BOTTOM_INFO_Y, BASE_WIDTH, BASE_HEIGHT,
                Color.argb(112, 22, 18, 17));
        painter.drawRounded(canvas, 30f, 1013f, 80f, 1061f, 14f,
                Color.argb(232, 222, 224, 229));
        painter.drawPixelText(canvas, "PI", 55f, 1037f, 25f, Color.rgb(39, 40, 47),
                Paint.Align.CENTER, true);
        painter.drawPixelText(canvas, "Pianaixel", 89f, 1039f, 27f,
                Color.rgb(248, 248, 251), Paint.Align.LEFT, true);

        float titleCursor = INFO_RIGHT;
        titleCursor -= painter.measurePixelText("2025", 20f, true);
        painter.drawPixelText(canvas, "2025", titleCursor, 1031f, 20f,
                Color.rgb(211, 219, 246), Paint.Align.LEFT, true);
        titleCursor -= painter.measurePixelText("PianAI ", 20f, true);
        painter.drawPixelText(canvas, "PianAI ", titleCursor, 1031f, 20f,
                Color.rgb(129, 151, 242), Paint.Align.LEFT, true);
        titleCursor -= painter.measurePixelText("Minecraft ", 20f, true);
        painter.drawPixelText(canvas, "Minecraft ", titleCursor, 1031f, 20f,
                Color.rgb(121, 176, 101), Paint.Align.LEFT, true);
        painter.drawPixelText(canvas, "Netease Multifunctional Starter", INFO_RIGHT, 1057f,
                20f, Color.rgb(247, 247, 250), Paint.Align.RIGHT, true);
    }

    /** Returns true while the caller should keep rendering the notification animation. */
    boolean drawStatusNotices(Canvas canvas, List<ClickGuiStatusNotice> notices) {
        if (notices.isEmpty()) {
            return false;
        }
        long now = SystemClock.uptimeMillis();
        for (Iterator<ClickGuiStatusNotice> iterator = notices.iterator(); iterator.hasNext();) {
            ClickGuiStatusNotice notice = iterator.next();
            if (now - notice.createdAtMs
                    >= NOTICE_ENTER_DURATION_MS + NOTICE_HOLD_DURATION_MS + NOTICE_EXIT_DURATION_MS) {
                iterator.remove();
            }
        }
        if (notices.isEmpty()) {
            return false;
        }

        float baseX = BASE_WIDTH - NOTICE_RIGHT - NOTICE_WIDTH;
        int noticeCount = notices.size();
        for (int index = 0; index < noticeCount; index++) {
            ClickGuiStatusNotice notice = notices.get(index);
            int slotFromBottom = noticeCount - 1 - index;
            float targetY = NOTICE_BOTTOM - NOTICE_HEIGHT
                    - slotFromBottom * (NOTICE_HEIGHT + NOTICE_GAP);
            float drawY = notice.resolveY(targetY, now);
            long age = Math.max(0L, now - notice.createdAtMs);
            float entering = easeOutQuad(clamp(age / (float) NOTICE_ENTER_DURATION_MS, 0f, 1f));
            float exiting = easeInQuad(clamp(
                    (age - NOTICE_ENTER_DURATION_MS - NOTICE_HOLD_DURATION_MS)
                            / (float) NOTICE_EXIT_DURATION_MS,
                    0f, 1f));
            float opacity = entering * (1f - exiting);
            float drawX = baseX + (1f - entering + exiting) * NOTICE_WIDTH;
            drawStatusNotice(canvas, notice, drawX, drawY, opacity);
        }
        return true;
    }

    void drawSettingsPanel(Canvas canvas, float panelX, float panelY, ClickGuiModule active,
                           boolean showHotkey, float[] sliderMinimum, float[] sliderMaximum,
                           float[] sliderValues, boolean traceBoxes, boolean traceOutline) {
        canvas.save();
        canvas.translate(panelX, panelY);
        painter.drawRounded(canvas, 0f, 0f, SETTINGS_WIDTH, SETTINGS_HEIGHT, SETTINGS_RADIUS,
                Color.argb(202, 20, 20, 22));
        painter.drawRoundedStroke(canvas, 0f, 0f, SETTINGS_WIDTH, SETTINGS_HEIGHT,
                SETTINGS_RADIUS, SETTINGS_OUTLINE_WIDTH, Color.argb(204, 226, 226, 231));
        painter.drawRounded(canvas, SETTINGS_DRAG_RAIL_X, SETTINGS_DRAG_RAIL_Y,
                SETTINGS_DRAG_RAIL_X + SETTINGS_DRAG_RAIL_WIDTH,
                SETTINGS_DRAG_RAIL_Y + SETTINGS_DRAG_RAIL_HEIGHT,
                SETTINGS_DRAG_RAIL_HEIGHT * 0.5f, Color.argb(238, 246, 246, 249));

        String title = active == null ? "功能设置" : active.label + "设置";
        painter.drawText(canvas, title, SETTINGS_CONTENT_LEFT, SETTINGS_TITLE_Y, 21f,
                Color.rgb(246, 246, 250), Paint.Align.LEFT, true);
        painter.drawText(canvas, "显示快捷键", SETTINGS_HOTKEY_LABEL_X, SETTINGS_TITLE_Y, 17f,
                Color.argb(228, 238, 238, 243), Paint.Align.LEFT, false);
        painter.drawSwitch(canvas, SETTINGS_SWITCH_X, SETTINGS_SWITCH_Y, SETTINGS_SWITCH_WIDTH,
                SETTINGS_SWITCH_HEIGHT, showHotkey);

        painter.drawSeparator(canvas, SETTINGS_CONTENT_LEFT, 104f, SETTINGS_CONTENT_RIGHT);
        drawSlider(canvas, "范围", 0, SLIDER_Y[0], sliderMinimum, sliderMaximum, sliderValues);
        drawSlider(canvas, "最大追踪量", 1, SLIDER_Y[1], sliderMinimum, sliderMaximum,
                sliderValues);
        drawSlider(canvas, "刷新间隔(秒)", 2, SLIDER_Y[2], sliderMinimum, sliderMaximum,
                sliderValues);
        drawSlider(canvas, "线宽", 3, SLIDER_Y[3], sliderMinimum, sliderMaximum, sliderValues);

        painter.drawText(canvas, "追踪箱子", SETTINGS_SLIDER_LABEL_X, 343f, 16f,
                Color.argb(190, 235, 235, 240), Paint.Align.LEFT, false);
        drawBooleanChoice(canvas, 86f, 357f, traceBoxes);
        painter.drawText(canvas, "轮廓线", 306f, 343f, 16f,
                Color.argb(190, 235, 235, 240), Paint.Align.LEFT, false);
        drawBooleanChoice(canvas, 328f, 357f, traceOutline);

        painter.drawSeparator(canvas, SETTINGS_CONTENT_LEFT, 433f, SETTINGS_CONTENT_RIGHT);
        painter.drawText(canvas, "演示模式：设置仅影响本应用界面。", 74f, 467f, 15f,
                Color.argb(145, 222, 222, 229), Paint.Align.LEFT, false);
        canvas.restore();
    }

    void drawFloatingHotkey(Canvas canvas, ClickGuiModule active, float hotkeyX, float hotkeyY) {
        String label = active == null ? "快捷键" : active.label;
        float width = hotkeyWidth(label);
        float height = 47f;
        int border = active != null && active.enabled ? Color.WHITE : Color.BLACK;
        painter.drawRounded(canvas, hotkeyX - 3f, hotkeyY - 3f, hotkeyX + width + 3f,
                hotkeyY + height + 3f, 16f, border);
        painter.drawRounded(canvas, hotkeyX, hotkeyY, hotkeyX + width, hotkeyY + height,
                14f, Color.argb(220, 29, 29, 36));
        painter.drawText(canvas, label, hotkeyX + width * 0.5f, hotkeyY + height * 0.5f,
                18f, Color.rgb(246, 246, 250), Paint.Align.CENTER, true);
    }

    RectF hotkeyBounds(ClickGuiModule active, float hotkeyX, float hotkeyY) {
        String label = active == null ? "快捷键" : active.label;
        return new RectF(hotkeyX, hotkeyY, hotkeyX + hotkeyWidth(label), hotkeyY + 47f);
    }

    void drawPanelToggleButton(Canvas canvas, boolean panelVisible, float panelToggleX,
                               float panelToggleY) {
        int borderColor = panelVisible
                ? Color.argb(238, 242, 242, 248)
                : Color.argb(238, 8, 8, 12);
        painter.drawRounded(canvas, panelToggleX - 3f, panelToggleY - 3f,
                panelToggleX + PANEL_TOGGLE_SIZE + 3f, panelToggleY + PANEL_TOGGLE_SIZE + 3f,
                22f, borderColor);
        painter.drawRounded(canvas, panelToggleX, panelToggleY,
                panelToggleX + PANEL_TOGGLE_SIZE, panelToggleY + PANEL_TOGGLE_SIZE,
                19f, Color.argb(229, 28, 29, 36));

        float left = panelToggleX + 18f;
        float top = panelToggleY + 19f;
        float right = panelToggleX + PANEL_TOGGLE_SIZE - 18f;
        float bottom = panelToggleY + PANEL_TOGGLE_SIZE - 18f;
        controlPaint.setStyle(Paint.Style.STROKE);
        controlPaint.setStrokeWidth(3f);
        controlPaint.setColor(Color.rgb(242, 242, 247));
        canvas.drawRoundRect(left, top, right, bottom, 5f, 5f, controlPaint);
        canvas.drawLine(left, top + 10f, right, top + 10f, controlPaint);
        canvas.drawLine(left + 12f, top + 10f, left + 12f, bottom, controlPaint);
        if (panelVisible) {
            canvas.drawLine(right - 11f, top + 16f, right - 3f, top + 24f, controlPaint);
            canvas.drawLine(right - 3f, top + 16f, right - 11f, top + 24f, controlPaint);
        } else {
            controlPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(right - 7f, top + 20f, 4f, controlPaint);
        }
        controlPaint.setStyle(Paint.Style.FILL);
    }

    private void drawSearch(Canvas canvas, String searchQuery, boolean searchFocused) {
        int searchColor = searchFocused
                ? Color.argb(245, 242, 242, 246)
                : Color.argb(222, 232, 232, 236);
        painter.drawRounded(canvas, SEARCH_X, SEARCH_Y, SEARCH_X + SEARCH_WIDTH,
                SEARCH_Y + SEARCH_HEIGHT, SEARCH_RADIUS, searchColor);
        String shownText = searchQuery.isEmpty() ? "搜索..." : searchQuery;
        int textColor = searchQuery.isEmpty()
                ? Color.argb(150, 78, 77, 84)
                : Color.rgb(45, 44, 51);
        painter.drawText(canvas, shownText, SEARCH_X + 15f, SEARCH_Y + SEARCH_HEIGHT * 0.5f,
                20f, textColor, Paint.Align.LEFT, false);
        if (searchFocused && SystemClock.uptimeMillis() % 900L < 470L) {
            float cursorX = SEARCH_X + 15f + painter.measureText(shownText, 20f, false);
            painter.drawRounded(canvas, cursorX + 2f, SEARCH_Y + 13f, cursorX + 4.5f,
                    SEARCH_Y + SEARCH_HEIGHT - 13f, 1f, Color.rgb(44, 43, 49));
        }
    }

    private void drawCategories(Canvas canvas, List<ClickGuiCategory> categories,
                                int selectedCategory, String searchQuery) {
        for (int index = 0; index < categories.size(); index++) {
            float top = categoryTop(index);
            float height = categoryHeight(index);
            boolean selected = index == selectedCategory && searchQuery.isEmpty();
            if (index != 6 || selected) {
                int color = selected
                        ? Color.argb(220, 218, 218, 225)
                        : Color.argb(118, 83, 83, 92);
                painter.drawRounded(canvas, CATEGORY_X, top, CATEGORY_X + CATEGORY_WIDTH,
                        top + height, 9f, color);
            }
            int textColor = selected ? Color.rgb(250, 250, 253)
                    : Color.argb(238, 238, 238, 243);
            painter.drawText(canvas, categories.get(index).label,
                    CATEGORY_X + CATEGORY_WIDTH * 0.5f, top + height * 0.5f,
                    20f, textColor, Paint.Align.CENTER, true);
        }
    }

    private void drawModuleList(Canvas canvas, List<ClickGuiCategory> categories,
                                int selectedCategory, String searchQuery,
                                List<ClickGuiModule> modules, float listScrollY,
                                float maxScroll) {
        String title = searchQuery.isEmpty() ? categories.get(selectedCategory).label : "搜索结果";
        painter.drawText(canvas, title, LIST_X, LIST_TITLE_Y,
                28f, Color.rgb(244, 244, 248), Paint.Align.LEFT, true);

        canvas.save();
        canvas.clipRect(LIST_X, LIST_VIEW_TOP, LIST_X + LIST_WIDTH, LIST_VIEW_BOTTOM);
        for (int index = 0; index < modules.size(); index++) {
            float top = LIST_Y + index * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - listScrollY;
            if (top + LIST_ROW_HEIGHT < LIST_VIEW_TOP || top > LIST_VIEW_BOTTOM) {
                continue;
            }
            drawModuleRow(canvas, modules.get(index), top);
        }
        canvas.restore();
        drawScrollBar(canvas, listScrollY, maxScroll);
    }

    private void drawModuleRow(Canvas canvas, ClickGuiModule item, float top) {
        painter.drawRounded(canvas, LIST_X, top, LIST_X + LIST_WIDTH, top + LIST_ROW_HEIGHT,
                12f, Color.argb(134, 77, 76, 84));
        painter.drawFeatureIcon(canvas, item.iconKind, LIST_X + 38f,
                top + LIST_ROW_HEIGHT * 0.5f);
        painter.drawText(canvas, item.label, LIST_X + 69f, top + LIST_ROW_HEIGHT * 0.5f,
                20f, Color.rgb(243, 243, 247), Paint.Align.LEFT, true);
        if (item.hasSettings) {
            painter.drawGearButton(canvas, item.hasToggle ? GEAR_X : GEAR_ONLY_X,
                    top + LIST_ROW_HEIGHT * 0.5f);
        }
        if (item.hasRunAction) {
            painter.drawRunButton(canvas, top);
        }
        if (item.hasToggle) {
            painter.drawSwitch(canvas, SWITCH_X, top + (LIST_ROW_HEIGHT - SWITCH_HEIGHT) * 0.5f,
                    SWITCH_WIDTH, SWITCH_HEIGHT, item.enabled);
        }
    }

    private void drawScrollBar(Canvas canvas, float listScrollY, float maxScroll) {
        float trackHeight = LIST_VIEW_BOTTOM - LIST_VIEW_TOP;
        painter.drawRounded(canvas, SCROLL_X, LIST_VIEW_TOP, SCROLL_X + SCROLL_WIDTH,
                LIST_VIEW_BOTTOM, 6f, Color.argb(26, 255, 255, 255));
        float thumbHeight = Math.min(trackHeight, SCROLL_THUMB_HEIGHT);
        float thumbTop = maxScroll == 0f ? LIST_VIEW_TOP
                : LIST_VIEW_TOP + (trackHeight - thumbHeight) * (listScrollY / maxScroll);
        painter.drawRounded(canvas, SCROLL_X, thumbTop, SCROLL_X + SCROLL_WIDTH,
                thumbTop + thumbHeight, 6f, Color.argb(224, 240, 240, 244));
    }

    private void drawStatusNotice(Canvas canvas, ClickGuiStatusNotice notice, float x, float y,
                                  float opacity) {
        int card = withAlpha(Color.argb(205, 25, 22, 23), opacity);
        int rail = withAlpha(Color.argb(214, 10, 9, 10), opacity);
        int white = withAlpha(Color.WHITE, opacity);
        painter.drawRect(canvas, x, y, x + NOTICE_WIDTH, y + NOTICE_HEIGHT, card);
        painter.drawRect(canvas, x, y, x + 27f, y + NOTICE_HEIGHT, rail);
        painter.drawRect(canvas, x, y + NOTICE_HEIGHT - 3f, x + NOTICE_WIDTH,
                y + NOTICE_HEIGHT, white);
        painter.drawPixelText(canvas, notice.enabled ? "功能已启用" : "功能已关闭",
                x + 44f, y + NOTICE_HEIGHT * 0.5f, 23f, white, Paint.Align.LEFT, false);
        drawStatusSymbol(canvas, x + NOTICE_WIDTH - 46f, y + NOTICE_HEIGHT * 0.5f,
                notice.enabled, opacity);
    }

    private void drawStatusSymbol(Canvas canvas, float centerX, float centerY, boolean enabled,
                                  float opacity) {
        int color = withAlpha(enabled ? Color.rgb(137, 180, 96) : Color.rgb(239, 54, 56),
                opacity);
        float unit = 5f;
        if (enabled) {
            painter.drawRect(canvas, centerX - 22f, centerY - 2f, centerX - 17f, centerY + 3f,
                    color);
            painter.drawRect(canvas, centerX - 17f, centerY + 3f, centerX - 12f, centerY + 8f,
                    color);
            painter.drawRect(canvas, centerX - 12f, centerY + 8f, centerX - 7f, centerY + 13f,
                    color);
            painter.drawRect(canvas, centerX - 7f, centerY + 3f, centerX - 2f, centerY + 8f,
                    color);
            painter.drawRect(canvas, centerX - 2f, centerY - 2f, centerX + 3f, centerY + 3f,
                    color);
            painter.drawRect(canvas, centerX + 3f, centerY - 7f, centerX + 8f, centerY - 2f,
                    color);
            painter.drawRect(canvas, centerX + 8f, centerY - 12f, centerX + 13f, centerY - 7f,
                    color);
        } else {
            for (int index = 0; index < 4; index++) {
                float offset = -15f + index * 10f;
                painter.drawRect(canvas, centerX + offset, centerY + offset,
                        centerX + offset + unit, centerY + offset + unit, color);
                painter.drawRect(canvas, centerX - offset - unit, centerY + offset,
                        centerX - offset, centerY + offset + unit, color);
            }
        }
    }

    private void drawSlider(Canvas canvas, String label, int sliderIndex, float y,
                            float[] sliderMinimum, float[] sliderMaximum, float[] sliderValues) {
        String value = sliderIndex < 2 ? String.valueOf(Math.round(sliderValues[sliderIndex]))
                : String.format(Locale.US, "%.2f", sliderValues[sliderIndex]);
        painter.drawText(canvas, label + "： " + value, SETTINGS_SLIDER_LABEL_X, y, 16f,
                Color.argb(216, 237, 237, 242), Paint.Align.LEFT, false);
        float fraction = normalizedSliderValue(sliderIndex, sliderMinimum, sliderMaximum,
                sliderValues);
        controlPaint.setStyle(Paint.Style.STROKE);
        controlPaint.setStrokeWidth(5f);
        controlPaint.setStrokeCap(Paint.Cap.ROUND);
        controlPaint.setColor(Color.argb(215, 228, 228, 235));
        canvas.drawLine(SETTINGS_SLIDER_LINE_LEFT, y, SETTINGS_SLIDER_LINE_RIGHT, y,
                controlPaint);
        controlPaint.setStyle(Paint.Style.FILL);
        controlPaint.setColor(Color.rgb(230, 230, 237));
        canvas.drawCircle(SETTINGS_SLIDER_LINE_LEFT
                + (SETTINGS_SLIDER_LINE_RIGHT - SETTINGS_SLIDER_LINE_LEFT) * fraction,
                y, 10f, controlPaint);
        controlPaint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawBooleanChoice(Canvas canvas, float x, float y, boolean value) {
        painter.drawRounded(canvas, x, y, x + 98f, y + 42f, 7f,
                value ? Color.argb(195, 93, 92, 102) : Color.argb(40, 255, 255, 255));
        painter.drawRounded(canvas, x + 112f, y, x + 210f, y + 42f, 7f,
                value ? Color.argb(40, 255, 255, 255) : Color.argb(195, 93, 92, 102));
        painter.drawText(canvas, "True", x + 49f, y + 21f, 16f,
                Color.rgb(242, 242, 247), Paint.Align.CENTER, true);
        painter.drawText(canvas, "False", x + 161f, y + 21f, 16f,
                Color.rgb(242, 242, 247), Paint.Align.CENTER, true);
    }

    private float hotkeyWidth(String label) {
        return Math.max(126f, 44f + painter.measureText(label, 18f, true));
    }

    private static float normalizedSliderValue(int index, float[] minimum, float[] maximum,
                                               float[] values) {
        return (values[index] - minimum[index]) / (maximum[index] - minimum[index]);
    }

    private static float easeOutQuad(float value) {
        return 1f - (1f - value) * (1f - value);
    }

    private static float easeInQuad(float value) {
        return value * value;
    }

    private static int withAlpha(int color, float fraction) {
        return Color.argb(Math.round(Color.alpha(color) * clamp(fraction, 0f, 1f)),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float categoryTop(int index) {
        return CATEGORY_TOPS[Math.max(0, Math.min(index, CATEGORY_TOPS.length - 1))];
    }

    private static float categoryHeight(int index) {
        return index == 6 ? CONFIGURATION_CATEGORY_HEIGHT : CATEGORY_HEIGHT;
    }
}
