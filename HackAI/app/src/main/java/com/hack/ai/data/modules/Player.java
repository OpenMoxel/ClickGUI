/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data.modules;

import com.hack.ai.data.ModuleItem;
import com.hack.ai.data.SubSetting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Player {
    public static final List<ModuleItem> modules = Collections.unmodifiableList(Arrays.asList(
            new ModuleItem("antiafk", "防挂机", "防止因挂机被踢出", false,
                    null, null, "AntiAFK", "Prevent AFK kick",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("随机移动", "旋转视角"),
                                    Arrays.asList("Random Move", "Spin View"),
                                    "随机移动"),
                            SubSetting.slider("delay", "延迟", "Delay",
                                    1f, 30f, 1f, 5f, "s")
                    )),
            new ModuleItem("autogapple", "自动金苹果", "生命值低时自动食用金苹果", false,
                    null, null, "AutoGapple", "Auto-eat golden apple when low HP",
                    Arrays.asList(
                            SubSetting.slider("health", "生命阈值", "Health",
                                    1f, 20f, 0.5f, 10f, "HP")
                    )),
            new ModuleItem("autoarmor", "自动穿甲", "自动装备最佳护甲", false,
                    null, null, "AutoArmor", "Auto-equip best armor pieces",
                    Arrays.asList(
                            SubSetting.combo("priority", "优先级", "Priority",
                                    Arrays.asList("护甲值", "耐久度"),
                                    Arrays.asList("Armor Value", "Durability"),
                                    "护甲值"),
                            SubSetting.toggle("replace", "自动替换", "Auto Replace", true)
                    )),
            new ModuleItem("autotool", "自动切换工具", "根据方块类型自动切换最佳工具", true,
                    null, null, "AutoTool", "Auto-switch to best tool for block",
                    Arrays.asList(
                            SubSetting.toggle("combatSwitch", "战斗时切换", "Switch In Combat", false)
                    )),
            new ModuleItem("autosprint", "自动疾跑", "自动保持疾跑状态", true,
                    null, null, "AutoSprint", "Automatically keep sprinting"),
            new ModuleItem("noslow", "无减速", "使用物品时无减速效果", false,
                    null, null, "NoSlow", "No slowdown when using items"),
            new ModuleItem("noweb", "无视蜘蛛网", "在蜘蛛网中无减速", false,
                    null, null, "NoWeb", "Ignore cobweb slowdown effect"),
            new ModuleItem("antivoid", "防虚空", "防止掉入虚空死亡", false,
                    null, null, "AntiVoid", "Prevent falling into the void"),
            new ModuleItem("cheststealer", "箱子掠夺", "自动快速搜刮箱子物品", false,
                    null, null, "ChestStealer", "Auto-loot items from chests",
                    Arrays.asList(
                            SubSetting.slider("delay", "速度", "Delay",
                                    20f, 200f, 5f, 80f, "ms"),
                            SubSetting.toggle("autoClose", "自动关闭", "Auto Close", true)
                    )),
            new ModuleItem("inventorycleaner", "背包整理", "自动丢弃无用物品", false,
                    null, null, "InventoryCleaner", "Auto-drop useless inventory items",
                    Arrays.asList(
                            SubSetting.slider("delay", "延迟", "Delay",
                                    50f, 500f, 10f, 200f, "ms")
                    ))
    ));

    private Player() {
    }
}
