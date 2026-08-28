/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import static com.pianai.xel.ClickGuiLayout.LIST_ROW_GAP;
import static com.pianai.xel.ClickGuiLayout.LIST_ROW_HEIGHT;
import static com.pianai.xel.ClickGuiLayout.LIST_VIEW_BOTTOM;
import static com.pianai.xel.ClickGuiLayout.LIST_VIEW_TOP;
import static com.pianai.xel.ClickGuiLayout.LIST_WIDTH;
import static com.pianai.xel.ClickGuiLayout.LIST_X;
import static com.pianai.xel.ClickGuiLayout.LIST_Y;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Handles category/search filtering and list measurements independently from the View. */
final class ClickGuiModuleBrowser {

    private ClickGuiModuleBrowser() {
    }

    static List<ClickGuiModule> displayedModules(ClickGuiState state) {
        if (state.searchQuery.trim().isEmpty()) {
            return state.categories.get(state.selectedCategory).modules;
        }
        List<ClickGuiModule> results = new ArrayList<>();
        String query = state.searchQuery.trim().toLowerCase(Locale.ROOT);
        for (ClickGuiCategory category : state.categories) {
            for (ClickGuiModule item : category.modules) {
                if (item.label.toLowerCase(Locale.ROOT).contains(query)) {
                    results.add(item);
                }
            }
        }
        return results;
    }

    static float contentHeight(List<ClickGuiModule> items) {
        if (items.isEmpty()) {
            return 0f;
        }
        return items.size() * LIST_ROW_HEIGHT + (items.size() - 1) * LIST_ROW_GAP;
    }

    static float maxScroll(List<ClickGuiModule> items) {
        return Math.max(0f, contentHeight(items) - (LIST_VIEW_BOTTOM - LIST_VIEW_TOP));
    }

    static ClickGuiModule findModule(ClickGuiState state, String id) {
        for (ClickGuiCategory category : state.categories) {
            for (ClickGuiModule item : category.modules) {
                if (item.id.equals(id)) {
                    return item;
                }
            }
        }
        return null;
    }

    static float rowTop(ClickGuiState state, ClickGuiModule target) {
        List<ClickGuiModule> modules = displayedModules(state);
        int index = modules.indexOf(target);
        return LIST_Y + index * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - state.listScrollY;
    }

    static ClickGuiModule moduleAt(ClickGuiState state, float x, float y) {
        if (!isInListViewport(x, y)) {
            return null;
        }
        List<ClickGuiModule> modules = displayedModules(state);
        for (int index = 0; index < modules.size(); index++) {
            float top = LIST_Y + index * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - state.listScrollY;
            if (y >= top && y <= top + LIST_ROW_HEIGHT && x >= LIST_X
                    && x <= LIST_X + LIST_WIDTH) {
                return modules.get(index);
            }
        }
        return null;
    }

    static boolean isInListViewport(float x, float y) {
        return x >= LIST_X && x <= LIST_X + LIST_WIDTH + 44f
                && y >= LIST_VIEW_TOP && y <= LIST_VIEW_BOTTOM;
    }
}
