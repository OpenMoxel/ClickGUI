/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

/** Single source of truth for the 1920x1080 virtual ClickGUI coordinate system. */
final class ClickGuiLayout {

    static final float BASE_WIDTH = 1920f;
    static final float BASE_HEIGHT = 1080f;

    static final float MAIN_X = 377f;
    static final float MAIN_Y = 184f;
    static final float MAIN_WIDTH = 1165f;
    static final float MAIN_HEIGHT = 684f;
    static final float MAIN_RADIUS = 51f;

    static final float LEFT_X = 405f;
    static final float LEFT_Y = 213f;
    static final float LEFT_WIDTH = 339f;
    static final float LEFT_HEIGHT = 626f;
    static final float LEFT_RADIUS = 31f;
    static final float SEARCH_X = 434f;
    static final float SEARCH_Y = 242f;
    static final float SEARCH_WIDTH = 281f;
    static final float SEARCH_HEIGHT = 53f;
    static final float SEARCH_RADIUS = 11f;
    static final float CATEGORY_X = 434f;
    static final float CATEGORY_WIDTH = 281f;
    static final float CATEGORY_HEIGHT = 53f;
    static final float CONFIGURATION_CATEGORY_HEIGHT = 38f;
    static final float[] CATEGORY_TOPS = {
            308f, 374f, 440f, 506f, 572f, 638f, 699f, 744f
    };

    static final float LIST_X = 799f;
    static final float LIST_Y = 308f;
    static final float LIST_WIDTH = 641f;
    static final float LIST_ROW_HEIGHT = 66f;
    static final float LIST_ROW_GAP = 9f;
    static final float LIST_TITLE_Y = 258f;
    static final float LIST_VIEW_TOP = 308f;
    static final float LIST_VIEW_BOTTOM = 829f;
    static final float RUN_X = 1336f;
    static final float RUN_WIDTH = 88f;
    static final float RUN_HEIGHT = 37f;
    static final float GEAR_X = 1319f;
    static final float GEAR_ONLY_X = 1401f;
    static final float GEAR_BUTTON_SIZE = 44f;
    static final float SWITCH_X = 1350f;
    static final float SWITCH_WIDTH = 72f;
    static final float SWITCH_HEIGHT = 43f;
    static final float SCROLL_X = 1460f;
    static final float SCROLL_WIDTH = 13f;
    static final float SCROLL_THUMB_HEIGHT = 140f;

    static final float WATERMARK_X = 26f;
    static final float WATERMARK_Y = 35f;
    static final float BOTTOM_INFO_Y = 999f;
    static final float BOTTOM_INFO_HEIGHT = 81f;
    static final float INFO_RIGHT = 1908f;

    static final float NOTICE_WIDTH = 378f;
    static final float NOTICE_HEIGHT = 90f;
    static final float NOTICE_RIGHT = 0f;
    static final float NOTICE_BOTTOM = 990f;
    static final float NOTICE_GAP = 10f;
    static final long NOTICE_ENTER_DURATION_MS = 180L;
    static final long NOTICE_HOLD_DURATION_MS = 1500L;
    static final long NOTICE_EXIT_DURATION_MS = 210L;

    static final float SETTINGS_X = 400f;
    static final float SETTINGS_Y = 292f;
    static final float SETTINGS_WIDTH = 696f;
    static final float SETTINGS_HEIGHT = 529f;
    static final float SETTINGS_RADIUS = 30f;
    static final float SETTINGS_OUTLINE_WIDTH = 2.5f;
    static final float SETTINGS_CONTENT_LEFT = 44f;
    static final float SETTINGS_CONTENT_RIGHT = 628f;
    static final float SETTINGS_TITLE_Y = 61f;
    static final float SETTINGS_HOTKEY_LABEL_X = 334f;
    static final float SETTINGS_SWITCH_X = 546f;
    static final float SETTINGS_SWITCH_Y = 37f;
    static final float SETTINGS_SWITCH_WIDTH = 82f;
    static final float SETTINGS_SWITCH_HEIGHT = 45f;
    static final float SETTINGS_SLIDER_LABEL_X = 56f;
    static final float SETTINGS_SLIDER_LINE_LEFT = 183f;
    static final float SETTINGS_SLIDER_LINE_RIGHT = 576f;
    static final float[] SLIDER_Y = {132f, 185f, 238f, 291f};
    static final float SETTINGS_DRAG_RAIL_X = 257f;
    static final float SETTINGS_DRAG_RAIL_Y = 12f;
    static final float SETTINGS_DRAG_RAIL_WIDTH = 182f;
    static final float SETTINGS_DRAG_RAIL_HEIGHT = 5.5f;
    static final float SETTINGS_DRAG_HIT_INSET_X = 14f;
    static final float SETTINGS_DRAG_HIT_INSET_Y = 12f;

    static final float PANEL_TOGGLE_SIZE = 60f;

    private ClickGuiLayout() {
    }
}
