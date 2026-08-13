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

public final class Combat {
    public static final List<ModuleItem> modules = Collections.unmodifiableList(Arrays.asList(
            new ModuleItem("killaura", "杀戮光环", "自动攻击附近实体", true,
                    "R", null, "KillAura", "Auto-attack nearby entities",
                    Arrays.asList(
                            SubSetting.combo("targetFilter", "目标筛选", "Target Filter",
                                    Arrays.asList("玩家", "敌对", "全部生物", "全部"),
                                    Arrays.asList("Players", "Hostile", "All Mobs", "All"),
                                    "玩家"),
                            SubSetting.combo("priority", "优先级", "Priority",
                                    Arrays.asList("距离", "血量", "角度"),
                                    Arrays.asList("Distance", "Health", "Angle"),
                                    "距离"),
                            SubSetting.combo("rotation", "旋转模式", "Rotation",
                                    Arrays.asList("普通", "平滑", "无"),
                                    Arrays.asList("Normal", "Smooth", "None"),
                                    "普通"),
                            SubSetting.slider("range", "攻击范围", "Range",
                                    1f, 6f, 0.1f, 3.5f, "m"),
                            SubSetting.slider("cps", "攻击速度", "CPS",
                                    1f, 20f, 0.5f, 10f, "CPS"),
                            SubSetting.slider("maxTargets", "最大目标数", "Max Targets",
                                    1f, 10f, 1f, 3f, ""),
                            SubSetting.toggle("autoWeapon", "自动切换武器", "Auto Weapon", true),
                            SubSetting.toggle("ignoreTeammates", "无视队友", "Ignore Teammates", true),
                            SubSetting.toggle("attackSync", "攻击延迟同步", "Attack Sync", false),
                            SubSetting.toggle("onlyOnKey", "仅攻击键触发", "Only On Key", false)
                    )),
            new ModuleItem("infiniteaura", "无限光环", "持续攻击范围内所有实体", false,
                    null, null, "InfiniteAura", "Attack all entities in range continuously",
                    Arrays.asList(
                            SubSetting.slider("aps", "攻击速度", "APS",
                                    1f, 20f, 0.5f, 10f, "APS"),
                            SubSetting.combo("targetFilter", "目标筛选", "Target Filter",
                                    Arrays.asList("玩家", "敌对", "全部"),
                                    Arrays.asList("Players", "Hostile", "All"),
                                    "全部")
                    )),
            new ModuleItem("triggerbot", "自瞄", "对准目标时自动攻击", false,
                    null, null, "TriggerBot", "Auto-attack when crosshair on target",
                    Arrays.asList(
                            SubSetting.slider("delay", "延迟", "Delay",
                                    50f, 500f, 10f, 100f, "ms"),
                            SubSetting.slider("range", "范围", "Range",
                                    1f, 6f, 0.5f, 3f, "m"),
                            SubSetting.combo("targetFilter", "目标筛选", "Target Filter",
                                    Arrays.asList("玩家", "敌对", "全部"),
                                    Arrays.asList("Players", "Hostile", "All"),
                                    "全部")
                    )),
            new ModuleItem("autoclicker", "自动连点", "自动点击鼠标左键", false,
                    "G", null, "AutoClicker", "Auto-click left mouse button",
                    Arrays.asList(
                            SubSetting.slider("cps", "CPS", "CPS",
                                    8f, 20f, 1f, 14f, "CPS"),
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("普通", "蝴蝶点击"),
                                    Arrays.asList("Normal", "Butterfly"),
                                    "普通"),
                            SubSetting.toggle("randomize", "随机化", "Randomize", true)
                    )),
            new ModuleItem("criticals", "暴击", "每次攻击触发暴击效果", false,
                    null, null, "Criticals", "Every attack is a critical hit",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("跳跃", "落地前"),
                                    Arrays.asList("Jump", "OnGround"),
                                    "跳跃"),
                            SubSetting.toggle("particles", "粒子效果", "Particles", true)
                    )),
            new ModuleItem("blink", "Blink", "延迟发送移动数据实现瞬移", false,
                    null, null, "Blink", "Delay movement packets for teleport effect",
                    Arrays.asList(
                            SubSetting.slider("delay", "延迟", "Delay",
                                    50f, 500f, 10f, 100f, "ms")
                    )),
            new ModuleItem("hitbox", "碰撞箱扩大", "扩大实体碰撞箱体积", false,
                    null, null, "HitBox", "Expand entity hitbox size",
                    Arrays.asList(
                            SubSetting.slider("expand", "扩大数值", "Expand",
                                    0.1f, 1f, 0.05f, 0.3f, "m")
                    )),
            new ModuleItem("antiknockback", "防击退", "完全抵消击退效果", false,
                    null, null, "AntiKnockback", "Completely negate knockback",
                    Arrays.asList(
                            SubSetting.toggle("horizontal", "水平抵消", "Horizontal", true),
                            SubSetting.toggle("vertical", "垂直抵消", "Vertical", true)
                    ))
    ));

    private Combat() {
    }
}
