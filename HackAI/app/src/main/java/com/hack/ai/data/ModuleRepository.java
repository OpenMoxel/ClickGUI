/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

import com.hack.ai.data.modules.Combat;
import com.hack.ai.data.modules.Misc;
import com.hack.ai.data.modules.Motion;
import com.hack.ai.data.modules.Player;
import com.hack.ai.data.modules.Visual;
import com.hack.ai.data.modules.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ModuleRepository {
    public static final List<Category> categories =
            Collections.unmodifiableList(Arrays.asList(Category.values()));

    private ModuleRepository() {
    }

    public static List<ModuleItem> modulesFor(Category category) {
        switch (category) {
            case Combat:
                return Combat.modules;
            case Motion:
                return Motion.modules;
            case Visual:
                return Visual.modules;
            case Player:
                return Player.modules;
            case World:
                return World.modules;
            case Misc:
                return Misc.modules;
            default:
                throw new IllegalStateException("Unknown category: " + category);
        }
    }

    public static List<ModuleItem> allModules() {
        List<ModuleItem> all = new ArrayList<>();
        for (Category category : categories) {
            all.addAll(modulesFor(category));
        }
        return all;
    }
}
