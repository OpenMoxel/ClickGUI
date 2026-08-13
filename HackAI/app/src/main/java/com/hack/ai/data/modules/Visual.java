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

public final class Visual {
    public static final List<ModuleItem> modules = Collections.unmodifiableList(Arrays.asList(
            new ModuleItem("damageparticle", "粒子效果", "攻击时显示伤害粒子效果", false,
                    null, null, "DamageParticle", "Show damage particles on attack",
                    Arrays.asList(
                            SubSetting.slider("size", "大小", "Size",
                                    0.5f, 3f, 0.1f, 1f, "x")
                    )),
            new ModuleItem("jumpcircle", "跳跃圈", "显示跳跃落点圆圈", false,
                    null, null, "JumpCircle", "Show jump landing circle",
                    Arrays.asList(
                            SubSetting.slider("size", "大小", "Size",
                                    0.5f, 3f, 0.1f, 1f, "x")
                    )),
            new ModuleItem("swinganimation", "挥手动画", "自定义挥手动画样式", false,
                    null, null, "SwingAnimation", "Custom swing animation style",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("1.7", "1.8", "Smooth"),
                                    Arrays.asList("1.7", "1.8", "Smooth"),
                                    "1.8"),
                            SubSetting.slider("speed", "速度", "Speed",
                                    0.5f, 2f, 0.1f, 1f, "x")
                    )),
            new ModuleItem("arraylist", "功能列表显示", "在屏幕显示已开启模块列表", true,
                    null, null, "ArrayList", "Show enabled modules on HUD"),
            new ModuleItem("coordinates", "坐标显示", "在屏幕显示当前坐标信息", false,
                    null, null, "Coordinates", "Show current coordinates on screen"),
            new ModuleItem("X-ray·ESP", "X-ray·ESP", "透视显示轮廓", true,
                    null, null, "X-ray·ESP", "Highlight entities through walls",
                    Arrays.asList(
                            SubSetting.combo("mode", "渲染方式", "Render Mode",
                                    Arrays.asList("2D方框", "3D方框", "线条", "填充"),
                                    Arrays.asList("2D Box", "3D Box", "Outline", "Fill"),
                                    "2D方框"),
                            SubSetting.slider("maxDist", "最大距离", "Max Distance",
                                    10f, 200f, 10f, 100f, "m"),
                            SubSetting.multi("espTargets", "目标类型", "Targets",
                                    Arrays.asList("玩家", "生物", "动物", "箱子", "物品"),
                                    Arrays.asList("Players", "Mobs", "Animals", "Chests", "Items"),
                                    Arrays.asList("玩家"))
                    )),
            new ModuleItem("tracer", "轨迹线", "绘制指向目标实体的线条", false,
                    null, null, "Tracer", "Draw tracer lines to target entities",
                    Arrays.asList(
                            SubSetting.slider("width", "粗细", "Width",
                                    0.5f, 5f, 0.5f, 1f, "px"),
                            SubSetting.combo("origin", "起点", "Origin",
                                    Arrays.asList("屏幕底部", "玩家"),
                                    Arrays.asList("Screen Bottom", "Player"),
                                    "屏幕底部")
                    )),
            new ModuleItem("waypoints", "路径点", "标记和显示路径导航点", false,
                    null, null, "Waypoints", "Mark and display navigation waypoints"),
            new ModuleItem("gamma", "伽马值", "调整游戏亮度无视黑暗", false,
                    null, null, "Gamma", "Adjust brightness to ignore darkness",
                    Arrays.asList(
                            SubSetting.slider("brightness", "亮度", "Brightness",
                                    0f, 1000f, 10f, 300f, "%")
                    )),
            new ModuleItem("nohurtcam", "无受伤抖动", "取消受伤时的视角抖动", false,
                    null, null, "NoHurtCam", "Disable camera shake on damage"),
            new ModuleItem("wireframe", "线框", "以线框模式渲染实体", false,
                    null, null, "Wireframe", "Render entities in wireframe mode",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("全部实体", "仅玩家"),
                                    Arrays.asList("All Entities", "Players Only"),
                                    "仅玩家")
                    ))
    ));

    private Visual() {
    }
}
