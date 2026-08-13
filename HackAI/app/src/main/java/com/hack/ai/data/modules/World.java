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

public final class World {
    public static final List<ModuleItem> modules = Collections.unmodifiableList(Arrays.asList(
            new ModuleItem("nuker", "范围破坏", "破坏指定范围内的方块", false,
                    null, null, "Nuker", "Break blocks within specified radius",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("全部方块", "仅特定"),
                                    Arrays.asList("All Blocks", "Specific Only"),
                                    "全部方块"),
                            SubSetting.slider("range", "范围", "Range",
                                    1f, 6f, 1f, 3f, "格"),
                            SubSetting.slider("delay", "延迟", "Delay",
                                    50f, 500f, 10f, 100f, "ms")
                    )),
            new ModuleItem("scaffold", "自动搭路", "自动在脚下放置方块搭路", false,
                    null, null, "Scaffold", "Auto-place blocks under feet to bridge",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("正常", "延伸", "向上"),
                                    Arrays.asList("Normal", "Extend", "Upward"),
                                    "正常"),
                            SubSetting.slider("delay", "延迟", "Delay",
                                    1f, 10f, 1f, 3f, "tick"),
                            SubSetting.slider("range", "范围", "Range",
                                    1f, 10f, 1f, 3f, "格"),
                            SubSetting.toggle("autoRotate", "自动旋转", "Auto Rotate", true)
                    )),
            new ModuleItem("fastplace", "快速放置", "移除方块放置延迟", false,
                    null, null, "FastPlace", "Remove block placement delay",
                    Arrays.asList(
                            SubSetting.slider("delay", "延迟", "Delay",
                                    0f, 4f, 1f, 0f, "tick")
                    )),
            new ModuleItem("chestaura", "箱子光环", "自动打开附近箱子", false,
                    null, null, "ChestAura", "Auto-open nearby chests",
                    Arrays.asList(
                            SubSetting.slider("range", "范围", "Range",
                                    1f, 6f, 1f, 3f, "格")
                    )),
            new ModuleItem("crash", "崩溃器", "发送特殊数据包使服务器崩溃", false,
                    null, null, "Crash", "Send special packets to crash the server",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("书签", "烟花", "位置", "声音"),
                                    Arrays.asList("Book", "Firework", "Position", "Sound"),
                                    "书签")
                    ))
    ));

    private World() {
    }
}
