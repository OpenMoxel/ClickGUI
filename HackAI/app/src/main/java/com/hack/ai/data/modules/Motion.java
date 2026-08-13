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

public final class Motion {
    public static final List<ModuleItem> modules = Collections.unmodifiableList(Arrays.asList(
            new ModuleItem("fly", "飞行", "允许在生存模式飞行", false,
                    "F", null, "Fly", "Allow flight in survival mode",
                    Arrays.asList(
                            SubSetting.slider("hSpeed", "水平速度", "H-Speed",
                                    0.1f, 5f, 0.1f, 1f, "x"),
                            SubSetting.slider("vSpeed", "垂直速度", "V-Speed",
                                    0.1f, 3f, 0.1f, 0.5f, "x")
                    )),
            new ModuleItem("airjump", "空中跳跃", "在空中进行跳跃", false,
                    null, null, "AirJump", "Jump while in mid-air"),
            new ModuleItem("bunnyhop", "兔子跳", "自动兔子跳移动", false,
                    null, null, "BunnyHop", "Automatic bunny hopping",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("Normal", "Legit"),
                                    Arrays.asList("Normal", "Legit"),
                                    "Normal"),
                            SubSetting.slider("speed", "速度", "Speed",
                                    0.1f, 2f, 0.05f, 0.6f, "x"),
                            SubSetting.toggle("autoJump", "自动跳跃", "Auto Jump", true)
                    )),
            new ModuleItem("speed", "加速", "提升移动速度", false,
                    null, null, "Speed", "Increase movement speed",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("AAC", "NCP", "BHop"),
                                    Arrays.asList("AAC", "NCP", "BHop"),
                                    "AAC"),
                            SubSetting.slider("speed", "速度", "Speed",
                                    0.1f, 2f, 0.1f, 1f, "x")
                    )),
            new ModuleItem("nofall", "无摔落伤害", "取消所有摔落伤害", false,
                    null, null, "NoFall", "Cancel all fall damage"),
            new ModuleItem("jesus", "水面行走", "在水面上行走", false,
                    null, null, "Jesus", "Walk on water surface",
                    Arrays.asList(
                            SubSetting.slider("speed", "速度", "Speed",
                                    0.1f, 1.5f, 0.1f, 0.8f, "x")
                    )),
            new ModuleItem("wallclimb", "爬墙", "像蜘蛛一样爬墙", false,
                    null, null, "WallClimb", "Climb walls like a spider",
                    Arrays.asList(
                            SubSetting.slider("speed", "速度", "Speed",
                                    0.5f, 3f, 0.1f, 1f, "m/s")
                    )),
            new ModuleItem("safewalk", "安全行走", "防止从方块边缘掉落", false,
                    null, null, "SafeWalk", "Prevent walking off block edges",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("所有方块", "仅边缘"),
                                    Arrays.asList("All Blocks", "Edges Only"),
                                    "仅边缘")
                    )),
            new ModuleItem("targetstrafe", "目标环绕", "围绕目标自动环绕移动", false,
                    null, null, "TargetStrafe", "Strafe around target automatically",
                    Arrays.asList(
                            SubSetting.slider("distance", "距离", "Distance",
                                    1f, 5f, 0.1f, 2.5f, "格"),
                            SubSetting.slider("speed", "速度", "Speed",
                                    0.5f, 3f, 0.1f, 1f, "x")
                    )),
            new ModuleItem("phase", "穿墙", "穿透墙壁移动", false,
                    null, null, "Phase", "Phase through walls",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("Client", "Server"),
                                    Arrays.asList("Client", "Server"),
                                    "Client"),
                            SubSetting.slider("distance", "距离", "Distance",
                                    0.1f, 3f, 0.1f, 0.5f, "m")
                    )),
            new ModuleItem("teleport", "点击传送", "点击传送到指定位置", false,
                    null, null, "Teleport", "Click Teleport to target location")
    ));

    private Motion() {
    }
}
