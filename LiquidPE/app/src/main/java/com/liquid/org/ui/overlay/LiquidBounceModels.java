/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.overlay;

import android.graphics.Color;
import android.os.SystemClock;

import com.liquid.org.ui.grid.PanelLayoutInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LiquidBounceModels {
    private LiquidBounceModels() {}

    public enum TopTab { CLICK_GUI, HUD_EDITOR, SETTINGS }
    public enum SettingType { TOGGLE, SLIDER, RANGE, DROPDOWN, MULTI_SELECT, BIND, COLOR, GROUP }
    public enum NotificationType { SUCCESS, ERROR, INFO, WARNING, CUSTOM }
    public enum NotificationIcon { TOGGLE, CHECK, CROSS, INFO, NONE }

    public static final class CategoryPanel {
        public final String id;
        public final String name;
        /** 当前画布中的逻辑绘制坐标，会作为自由布局的位置持久化。 */
        public float x;
        public float y;
        public PanelLayoutInfo layoutInfo;
        public final List<ModuleEntry> modules = new ArrayList<>();
        public final AnimationState expansion = new AnimationState(1f);
        public boolean expanded = true;
        public float scrollOffset;
        public float targetScrollOffset;
        public float contentHeight;

        public CategoryPanel(String id, String name, float x, float y) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    public static final class ModuleEntry {
        public final String id;
        public final String categoryId;
        public final String name;
        public final List<SettingEntry> settings = new ArrayList<>();
        /** Metadata displayed by the right-side ArrayList. */
        public String arrayListSuffix = "";
        public String keyBind = "None";
        public boolean showInArrayList = true;
        public boolean showInBinds = true;
        public boolean enabled;
        public boolean settingsExpanded;
        public final AnimationState enabledProgress = new AnimationState(0f);
        public final AnimationState settingsProgress = new AnimationState(0f);

        public ModuleEntry(String id, String name, String categoryId) {
            this.id = id;
            this.name = name;
            this.categoryId = categoryId;
        }

        public ModuleEntry settings(SettingEntry... entries) {
            settings.addAll(Arrays.asList(entries));
            return this;
        }

        public boolean hasSettings() { return !settings.isEmpty(); }
        public boolean hasBind() { return keyBind != null && !keyBind.isEmpty() && !"None".equalsIgnoreCase(keyBind); }
    }

    public abstract static class SettingEntry {
        public final String id;
        public final String name;
        public final SettingType type;
        public boolean visible = true;
        public int level;

        protected SettingEntry(String id, String name, SettingType type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        public SettingEntry level(int value) { level = Math.max(0, value); return this; }
    }

    public static final class ToggleSetting extends SettingEntry {
        public final boolean defaultValue;
        public boolean value;
        public final AnimationState progress;
        public ToggleSetting(String id, String name, boolean value) {
            super(id, name, SettingType.TOGGLE);
            this.defaultValue = this.value = value;
            progress = new AnimationState(value ? 1f : 0f);
        }
    }

    public static final class SliderSetting extends SettingEntry {
        public final float min;
        public final float max;
        public final float defaultValue;
        public float value;
        public final String unit;
        public SliderSetting(String id, String name, float min, float max, float value, String unit) {
            super(id, name, SettingType.SLIDER);
            this.min = min; this.max = max; this.defaultValue = this.value = value; this.unit = unit == null ? "" : unit;
        }
    }

    public static final class RangeSetting extends SettingEntry {
        public final float min;
        public final float max;
        public final float defaultLow;
        public final float defaultHigh;
        public float low;
        public float high;
        public RangeSetting(String id, String name, float min, float max, float low, float high) {
            super(id, name, SettingType.RANGE);
            this.min = min; this.max = max; this.defaultLow = this.low = low; this.defaultHigh = this.high = high;
        }
    }

    public static final class DropdownSetting extends SettingEntry {
        public final List<String> options = new ArrayList<>();
        public final String defaultValue;
        public String value;
        public boolean open;
        public DropdownSetting(String id, String name, String value, String... options) {
            super(id, name, SettingType.DROPDOWN);
            this.defaultValue = this.value = value;
            this.options.addAll(Arrays.asList(options));
        }
    }

    public static final class MultiSelectSetting extends SettingEntry {
        public final List<String> options = new ArrayList<>();
        public final Set<String> defaultSelected = new LinkedHashSet<>();
        public final Set<String> selected = new LinkedHashSet<>();
        public MultiSelectSetting(String id, String name, String[] options, String... selected) {
            super(id, name, SettingType.MULTI_SELECT);
            this.options.addAll(Arrays.asList(options));
            this.defaultSelected.addAll(Arrays.asList(selected));
            this.selected.addAll(defaultSelected);
        }
    }

    public static final class BindSetting extends SettingEntry {
        public final String defaultValue;
        public String value;
        public boolean listening;
        public BindSetting(String id, String name, String value) {
            super(id, name, SettingType.BIND);
            this.defaultValue = this.value = value == null ? "None" : value;
        }
    }

    public static final class ColorSetting extends SettingEntry {
        public final int defaultColor;
        public int color;
        public float hue;
        public float saturation;
        public float brightness;
        public float alpha;
        public boolean expanded;
        public ColorSetting(String id, String name, int rgbaColor, boolean expanded) {
            super(id, name, SettingType.COLOR);
            defaultColor = color = rgbaColor;
            this.expanded = expanded;
            float[] hsv = new float[3];
            Color.colorToHSV(rgbaColor, hsv);
            hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]; alpha = Color.alpha(rgbaColor) / 255f;
        }
        public void updateColor() { color = Color.HSVToColor(Math.round(alpha * 255f), new float[]{hue, saturation, brightness}); }
    }

    public static final class SettingGroup extends SettingEntry {
        public final List<SettingEntry> children = new ArrayList<>();
        public final boolean defaultExpanded;
        public boolean expanded;
        public final AnimationState expansion;
        public SettingGroup(String id, String name, boolean expanded, SettingEntry... children) {
            super(id, name, SettingType.GROUP);
            this.defaultExpanded = this.expanded = expanded;
            this.children.addAll(Arrays.asList(children));
            expansion = new AnimationState(expanded ? 1f : 0f);
        }
    }

    public static final class ArrayListEntry {
        public final String moduleId;
        public String moduleName;
        public String suffix;
        public float moduleWidth;
        public float suffixWidth;
        public float totalWidth;
        public float currentY;
        public float targetY;
        public boolean removing;
        public final AnimationState visibility = new AnimationState(0f);
        public ArrayListEntry(ModuleEntry module) {
            moduleId = module.id;
            moduleName = module.name;
            suffix = module.arrayListSuffix == null ? "" : module.arrayListSuffix;
        }
    }

    public static final class NotificationSpec {
        public String title;
        public String content;
        public NotificationType type = NotificationType.INFO;
        public NotificationIcon icon = NotificationIcon.INFO;
        public int statusColor;
        public long displayDuration = LiquidBounceUiDurations.NOTIFICATION_VISIBLE;
        public boolean autoDismiss = true;

        public NotificationSpec(String title, String content) { this.title = title; this.content = content; }
        public NotificationSpec type(NotificationType value) { type = value; return this; }
        public NotificationSpec icon(NotificationIcon value) { icon = value; return this; }
        public NotificationSpec color(int value) { statusColor = value; return this; }
        public NotificationSpec duration(long value) { displayDuration = value; return this; }
        public NotificationSpec autoDismiss(boolean value) { autoDismiss = value; return this; }
    }

    public static final class NotificationItem {
        public final NotificationSpec spec;
        public final long createdAt = SystemClock.uptimeMillis();
        public float currentY = LiquidBounceUiMetrics.CONTENT_HEIGHT + LiquidBounceUiMetrics.NOTIFICATION_HEIGHT;
        public float targetY;
        public boolean exiting;
        public final AnimationState visibility = new AnimationState(0f);
        public NotificationItem(NotificationSpec spec) {
            this.spec = spec;
            visibility.animateTo(1f, LiquidBounceUiDurations.NOTIFICATION_ENTER_EXIT);
        }
    }
}
