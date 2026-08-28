/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import java.util.ArrayList;
import java.util.List;

/** A sidebar category and its independently owned module collection. */
final class ClickGuiCategory {

    final String label;
    final List<ClickGuiModule> modules = new ArrayList<>();

    ClickGuiCategory(String label) {
        this.label = label;
    }
}
