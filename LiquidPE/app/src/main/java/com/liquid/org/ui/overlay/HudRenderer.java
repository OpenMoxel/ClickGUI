/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.liquid.org.ui.overlay.LiquidBounceModels.ArrayListEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.ModuleEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.NotificationIcon;
import com.liquid.org.ui.overlay.LiquidBounceModels.NotificationItem;
import com.liquid.org.ui.overlay.LiquidBounceModels.NotificationSpec;
import com.liquid.org.ui.overlay.LiquidBounceModels.NotificationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class HudRenderer {
    private final LiquidBounceDataStore dataStore;
    private final ResponsiveTypography typography;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final List<ArrayListEntry> arrayEntries = new ArrayList<>();
    private final List<ModuleEntry> bindRows = new ArrayList<>();
    private final List<NotificationItem> notifications = new ArrayList<>();
    private boolean arrayDirty;
    private boolean bindsDirty = true;
    private float bindsX = LiquidBounceUiMetrics.BINDS_X;
    private float bindsY = LiquidBounceUiMetrics.BINDS_Y;
    private boolean draggingBinds;
    private float bindsDragOffsetX;
    private float bindsDragOffsetY;

    public HudRenderer(LiquidBounceDataStore dataStore, ResponsiveTypography typography) {
        this.dataStore = dataStore;
        this.typography = typography;
        stroke.setStyle(Paint.Style.STROKE);
        text.setTypeface(LiquidBounceFonts.medium());
        syncEnabledModules(true);
    }

    public void draw(Canvas canvas, boolean debugBounds, long now) {
        drawArrayList(canvas, debugBounds, now);
        drawNotifications(canvas, debugBounds, now);
    }

    /** 单独在功能菜单之后绘制，使 Binds 面板可见且可直接拖动。 */
    public void drawBindsOverlay(Canvas canvas, boolean debugBounds) {
        if (bindsDirty) refreshBinds();
        if (!isBindsPanelEnabled()) return;
        float width = getBindsWidth();
        float height = getBindsHeight();
        clampBindsPosition(width, height);
        float x = bindsX, y = bindsY;
        setFill(LiquidBounceUiColors.HUD_PANEL); rect.set(x, y, x + width, y + height); canvas.drawRoundRect(rect, 9, 9, fill);
        setText(Color.WHITE, 21, LiquidBounceFonts.bindsBold()); drawBaseline(canvas, LiquidBounceI18n.t("Binds"), x + 14, y + 3, 45, text);
        drawKeyboardIcon(canvas, x + width - 26, y + 26);
        float rowY = y + 52;
        if (bindRows.isEmpty()) {
            setText(0xFF8B8C93, 17, LiquidBounceFonts.bindsRegular());
            drawBaseline(canvas, LiquidBounceI18n.t("No binds"), x + 14, rowY, 31, text);
        } else {
            for (ModuleEntry module : bindRows) {
                setText(module.enabled ? LiquidBounceUiColors.SUCCESS : Color.WHITE, 20, LiquidBounceFonts.bindsMedium()); drawBaseline(canvas, LiquidBounceI18n.moduleName(module.id, module.name), x + 14, rowY, 31, text);
                int keyColor = module.keyBind.length() > 2 ? 0xFF8B8C93 : LiquidBounceUiColors.ACCENT_SOFT;
                setText(keyColor, 17, LiquidBounceFonts.bindsRegular()); drawRightBaseline(canvas, "[" + module.keyBind + "]", x + width - 14, rowY, 31, text);
                rowY += 33;
            }
        }
        if (debugBounds) drawDebugRect(canvas, x, y, width, height);
    }

    /** Binds 开关放在 Render 分类中，默认关闭。 */
    private boolean isBindsPanelEnabled() {
        ModuleEntry bindsModule = dataStore.findModule("render.binds");
        return bindsModule != null && bindsModule.enabled;
    }

    private float getBindsWidth() {
        setText(Color.WHITE, 20, LiquidBounceFonts.bindsMedium());
        float width = 270;
        for (ModuleEntry module : bindRows) {
            float left = text.measureText(LiquidBounceI18n.moduleName(module.id, module.name));
            setText(0xFF8B8C93, 17, LiquidBounceFonts.bindsRegular());
            float right = text.measureText("[" + module.keyBind + "]");
            width = Math.max(width, 28 + left + 24 + right);
            setText(Color.WHITE, 20, LiquidBounceFonts.bindsMedium());
        }
        return width;
    }

    private float getBindsHeight() { return 52 + Math.max(1, bindRows.size()) * 33f + 10; }

    /** Binds 标题栏的独立拖动处理；只有开关开启时才会接管事件。 */
    public boolean onTouchDown(float x, float y) {
        if (bindsDirty) refreshBinds();
        if (!isBindsPanelEnabled()) return false;
        float width = getBindsWidth();
        float height = getBindsHeight();
        clampBindsPosition(width, height);
        if (x < bindsX || x > bindsX + width || y < bindsY || y > bindsY + 52) return false;
        draggingBinds = true;
        bindsDragOffsetX = x - bindsX;
        bindsDragOffsetY = y - bindsY;
        return true;
    }

    public boolean onTouchMove(float x, float y) {
        if (!draggingBinds) return false;
        bindsX = x - bindsDragOffsetX;
        bindsY = y - bindsDragOffsetY;
        clampBindsPosition(getBindsWidth(), getBindsHeight());
        return true;
    }

    public boolean onTouchUp() {
        boolean consumed = draggingBinds;
        draggingBinds = false;
        return consumed;
    }

    private void clampBindsPosition(float width, float height) {
        bindsX = clamp(bindsX, 0, Math.max(0, LiquidBounceUiMetrics.CONTENT_WIDTH - width));
        bindsY = clamp(bindsY, 0, Math.max(0, LiquidBounceUiMetrics.CONTENT_HEIGHT - height));
    }

    private void drawKeyboardIcon(Canvas canvas, float cx, float cy) {
        stroke.setColor(Color.WHITE); stroke.setStrokeWidth(2); rect.set(cx - 12, cy - 8, cx + 12, cy + 8); canvas.drawRoundRect(rect, 2, 2, stroke);
        for (int row = 0; row < 2; row++) for (int col = 0; col < 5; col++) canvas.drawCircle(cx - 8 + col * 4, cy - 4 + row * 5, 1, stroke);
        canvas.drawLine(cx - 7, cy + 5, cx + 7, cy + 5, stroke);
    }

    private void drawArrayList(Canvas canvas, boolean debugBounds, long now) {
        if (arrayDirty) sortEntries();
        float right = LiquidBounceUiMetrics.CONTENT_WIDTH - LiquidBounceUiMetrics.ARRAY_RIGHT;
        int targetIndex = 0;
        for (int i = 0; i < arrayEntries.size(); i++) {
            ArrayListEntry entry = arrayEntries.get(i);
            float progress = entry.visibility.get(now);
            if (!entry.removing) entry.targetY = LiquidBounceUiMetrics.ARRAY_TOP + targetIndex++ * LiquidBounceUiMetrics.ARRAY_ROW_HEIGHT;
            entry.currentY += (entry.targetY - entry.currentY) * .22f;
            float fullWidth = entry.totalWidth + 38, drawWidth = Math.max(8, fullWidth * progress), slide = (1 - progress) * fullWidth, left = right - drawWidth + slide;
            setFill((Math.round(205 * progress) << 24) | 0x00030507); rect.set(left, entry.currentY, right + slide, entry.currentY + 38); canvas.drawRoundRect(rect, 4, 4, fill);
            setFill((Math.round(255 * progress) << 24) | 0x003C69FC); canvas.drawRoundRect(left, entry.currentY, left + 8, entry.currentY + 38, 4, 4, fill);
            if (progress > .05f) {
                float textX = right - entry.totalWidth - 10 + slide;
                text.setAlpha(Math.round(255 * progress)); setText(Color.WHITE, 20, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.moduleName(entry.moduleId, entry.moduleName), textX, entry.currentY, 38, text);
                if (!entry.suffix.isEmpty()) { setText(0xFF85868C, 20, LiquidBounceFonts.regular()); drawBaseline(canvas, LiquidBounceI18n.t(entry.suffix), textX + entry.moduleWidth + spaceWidth(), entry.currentY, 38, text); }
                text.setAlpha(255);
            }
            if (debugBounds) drawDebugRect(canvas, right - fullWidth, entry.currentY, fullWidth, 38);
        }
        for (int i = arrayEntries.size() - 1; i >= 0; i--) {
            ArrayListEntry e = arrayEntries.get(i);
            if (e.removing && !e.visibility.isRunning(now) && e.visibility.get(now) <= 0) arrayEntries.remove(i);
        }
    }

    private void drawNotifications(Canvas canvas, boolean debugBounds, long now) {
        int stack = 0;
        for (int i = notifications.size() - 1; i >= 0; i--) {
            NotificationItem item = notifications.get(i);
            NotificationSpec spec = item.spec;
            if (spec.autoDismiss && !item.exiting && now - item.createdAt > spec.displayDuration) { item.exiting = true; item.visibility.animateTo(0, LiquidBounceUiDurations.NOTIFICATION_ENTER_EXIT); }
            if (!item.exiting) {
                item.targetY = LiquidBounceUiMetrics.CONTENT_HEIGHT - LiquidBounceUiMetrics.NOTIFICATION_BOTTOM - LiquidBounceUiMetrics.NOTIFICATION_HEIGHT - stack * (LiquidBounceUiMetrics.NOTIFICATION_HEIGHT + LiquidBounceUiMetrics.NOTIFICATION_GAP);
                stack++;
            }
            float progress = item.visibility.get(now);
            item.currentY += (item.targetY - item.currentY) * .24f;
            float x = LiquidBounceUiMetrics.CONTENT_WIDTH - LiquidBounceUiMetrics.NOTIFICATION_RIGHT - LiquidBounceUiMetrics.NOTIFICATION_WIDTH + (1 - progress) * (LiquidBounceUiMetrics.NOTIFICATION_WIDTH + 30);
            setFill((Math.round(210 * progress) << 24) | 0x00030507); rect.set(x, item.currentY, x + 480, item.currentY + 103); canvas.drawRoundRect(rect, 10, 10, fill);
            setFill(resolveStatusColor(spec)); rect.set(x + 17, item.currentY + 21, x + 77, item.currentY + 81); canvas.drawRoundRect(rect, 8, 8, fill); drawNotificationIcon(canvas, rect.centerX(), rect.centerY(), spec.icon);
            text.setAlpha(Math.round(255 * progress)); setText(Color.WHITE, 25, LiquidBounceFonts.bold()); drawBaseline(canvas, spec.title == null ? "" : spec.title, x + 94, item.currentY + 14, 38, text); setText(0xFFD4D5D9, 19, LiquidBounceFonts.regular()); drawBaseline(canvas, spec.content == null ? "" : spec.content, x + 94, item.currentY + 52, 34, text); text.setAlpha(255);
            if (debugBounds) drawDebugRect(canvas, x, item.currentY, 480, 103);
        }
        for (int i = notifications.size() - 1; i >= 0; i--) { NotificationItem n = notifications.get(i); if (n.exiting && !n.visibility.isRunning(now) && n.visibility.get(now) <= 0) notifications.remove(i); }
    }

    private void drawNotificationIcon(Canvas canvas, float cx, float cy, NotificationIcon icon) {
        stroke.setColor(Color.WHITE); stroke.setStrokeWidth(4); stroke.setStrokeCap(Paint.Cap.ROUND);
        if (icon == NotificationIcon.NONE) return;
        if (icon == NotificationIcon.CHECK) { canvas.drawLine(cx - 14, cy, cx - 3, cy + 11, stroke); canvas.drawLine(cx - 3, cy + 11, cx + 16, cy - 12, stroke); }
        else if (icon == NotificationIcon.CROSS) { canvas.drawLine(cx - 13, cy - 13, cx + 13, cy + 13, stroke); canvas.drawLine(cx + 13, cy - 13, cx - 13, cy + 13, stroke); }
        else if (icon == NotificationIcon.INFO) { canvas.drawCircle(cx, cy - 12, 2, stroke); canvas.drawLine(cx, cy - 2, cx, cy + 15, stroke); }
        else { canvas.drawLine(cx - 17, cy - 9, cx + 12, cy - 9, stroke); canvas.drawCircle(cx + 12, cy - 9, 5, stroke); canvas.drawLine(cx - 12, cy + 9, cx + 17, cy + 9, stroke); canvas.drawCircle(cx - 12, cy + 9, 5, stroke); }
        stroke.setStrokeCap(Paint.Cap.BUTT);
    }

    public void onModuleChanged(ModuleEntry module) {
        ArrayListEntry entry = findEntry(module.id);
        boolean shouldShow = module.enabled && module.showInArrayList;
        if (shouldShow && entry == null) addEntry(module, false);
        else if (!shouldShow && entry != null && !entry.removing) { entry.removing = true; entry.visibility.animateTo(0, LiquidBounceUiDurations.ARRAY_ENTER_EXIT); arrayDirty = true; }
        else if (entry != null) { entry.moduleName = module.name; entry.suffix = module.arrayListSuffix == null ? "" : module.arrayListSuffix; measure(entry); arrayDirty = true; }
        bindsDirty = true;
    }

    public void syncEnabledModules(boolean snap) {
        for (ModuleEntry module : dataStore.getEnabledModules()) if (findEntry(module.id) == null) addEntry(module, snap);
        for (ArrayListEntry entry : arrayEntries) { ModuleEntry module = dataStore.findModule(entry.moduleId); if (module == null || !module.enabled || !module.showInArrayList) { entry.removing = true; entry.visibility.animateTo(0, LiquidBounceUiDurations.ARRAY_ENTER_EXIT); } }
        arrayDirty = true; bindsDirty = true;
    }

    public void onModuleMetadataChanged(ModuleEntry module) {
        ArrayListEntry entry = findEntry(module.id);
        boolean shouldShow = module.enabled && module.showInArrayList;
        if (shouldShow && entry == null) addEntry(module, false);
        else if (!shouldShow && entry != null && !entry.removing) { entry.removing = true; entry.visibility.animateTo(0, LiquidBounceUiDurations.ARRAY_ENTER_EXIT); arrayDirty = true; }
        else if (entry != null) { entry.suffix = module.arrayListSuffix == null ? "" : module.arrayListSuffix; measure(entry); arrayDirty = true; }
        bindsDirty = true;
    }
    public void pushNotification(NotificationSpec spec) { if (spec != null) notifications.add(new NotificationItem(spec)); }
    public void pushModuleNotification(ModuleEntry module, boolean enabled) {
        pushNotification(new NotificationSpec(LiquidBounceI18n.t(enabled ? "Enabled" : "Disabled"), LiquidBounceI18n.moduleName(module.id, module.name))
                .type(enabled ? NotificationType.SUCCESS : NotificationType.ERROR)
                .icon(NotificationIcon.TOGGLE));
    }
    public void clearNotifications() { notifications.clear(); }

    /** 语言切换后重测全部 ArrayList 宽度、刷新 Binds,保证新语言下宽度/排序正确。 */
    public void onLanguageChanged() {
        for (ArrayListEntry entry : arrayEntries) {
            ModuleEntry module = dataStore.findModule(entry.moduleId);
            if (module != null) {
                entry.moduleName = module.name;
                entry.suffix = module.arrayListSuffix == null ? "" : module.arrayListSuffix;
            }
            measure(entry);
        }
        arrayDirty = true;
        bindsDirty = true;
    }

    public boolean hasActiveAnimations(long now) {
        for (ArrayListEntry e : arrayEntries) if (e.visibility.isRunning(now) || Math.abs(e.currentY - e.targetY) > .5f) return true;
        for (NotificationItem n : notifications) if (n.visibility.isRunning(now) || Math.abs(n.currentY - n.targetY) > .5f || (n.spec.autoDismiss && !n.exiting)) return true;
        return false;
    }

    private void refreshBinds() { bindRows.clear(); bindRows.addAll(dataStore.getBoundModules()); bindsDirty = false; }
    private void addEntry(ModuleEntry module, boolean snap) { ArrayListEntry entry = new ArrayListEntry(module); measure(entry); entry.currentY = LiquidBounceUiMetrics.ARRAY_TOP + arrayEntries.size() * 40f; arrayEntries.add(entry); if (snap) entry.visibility.snapTo(1); else entry.visibility.animateTo(1, LiquidBounceUiDurations.ARRAY_ENTER_EXIT); arrayDirty = true; }
    private ArrayListEntry findEntry(String moduleId) { for (ArrayListEntry entry : arrayEntries) if (entry.moduleId.equals(moduleId) && !entry.removing) return entry; return null; }
    private void measure(ArrayListEntry entry) { setText(Color.WHITE, 20, LiquidBounceFonts.medium()); entry.moduleWidth = text.measureText(LiquidBounceI18n.moduleName(entry.moduleId, entry.moduleName)); setText(0xFF85868C, 20, LiquidBounceFonts.regular()); entry.suffixWidth = entry.suffix.isEmpty() ? 0 : text.measureText(LiquidBounceI18n.t(entry.suffix)); entry.totalWidth = entry.moduleWidth + (entry.suffix.isEmpty() ? 0 : spaceWidth() + entry.suffixWidth); }
    private float spaceWidth() { text.setTextSize(typography.size(20)); return text.measureText(" "); }
    private void sortEntries() { Collections.sort(arrayEntries, new Comparator<ArrayListEntry>() { @Override public int compare(ArrayListEntry a, ArrayListEntry b) { if (a.removing != b.removing) return a.removing ? 1 : -1; return Float.compare(b.totalWidth, a.totalWidth); }}); int i = 0; for (ArrayListEntry e : arrayEntries) if (!e.removing) e.targetY = 50 + i++ * 40f; arrayDirty = false; }
    private int resolveStatusColor(NotificationSpec spec) { if (spec.statusColor != 0) return spec.statusColor; if (spec.type == NotificationType.SUCCESS) return LiquidBounceUiColors.SUCCESS; if (spec.type == NotificationType.ERROR) return LiquidBounceUiColors.ERROR; if (spec.type == NotificationType.WARNING) return 0xFFF0A52B; return LiquidBounceUiColors.ACCENT; }
    private void setFill(int color) { fill.setStyle(Paint.Style.FILL); fill.setColor(color); }
    private void setText(int color, float size, Typeface face) { text.setColor(color); text.setTextSize(typography.size(size)); text.setTypeface(face); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private void drawBaseline(Canvas c, String s, float x, float y, float h, Paint p) { Paint.FontMetrics fm = p.getFontMetrics(); c.drawText(s, x, y + (h - (fm.descent - fm.ascent)) * .5f - fm.ascent, p); }
    private void drawRightBaseline(Canvas c, String s, float right, float y, float h, Paint p) { drawBaseline(c, s, right - p.measureText(s), y, h, p); }
    private void drawDebugRect(Canvas c, float x, float y, float w, float h) { stroke.setColor(0xCCFF3CAC); stroke.setStrokeWidth(2); c.drawRect(x, y, x + w, y + h, stroke); }
}
