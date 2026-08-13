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

public final class Misc {
    public static final List<ModuleItem> modules = Collections.unmodifiableList(Arrays.asList(
            new ModuleItem("interface_theme", "黑白主题", "开启黑色主题，关闭白色主题", false,
                    null, null, "Black & White Theme", "Enable the dark theme or disable it for the light theme"),
            new ModuleItem("antibot", "反机器人", "检测并忽略假人/机器人实体", false,
                    null, null, "AntiBot", "Detect and ignore bot entities"),
            new ModuleItem("spammer", "刷屏器", "自动发送预设聊天消息", false,
                    null, null, "Spammer", "Auto-send preset chat messages",
                    Arrays.asList(
                            SubSetting.slider("delay", "延迟", "Delay",
                                    500f, 10000f, 100f, 3000f, "ms"),
                            SubSetting.toggle("randomize", "随机化", "Randomize", true))),
            new ModuleItem("musicplayer", "音乐播放器", "在游戏中播放音乐", false,
                    null, null, "MusicPlayer", "Play music in-game"),
            new ModuleItem("timer", "计时器加速", "修改客户端游戏刻速度", false,
                    null, null, "Timer", "Modify client game tick speed",
                    Arrays.asList(
                            SubSetting.slider("speed", "速度", "Speed",
                                    0.1f, 5f, 0.1f, 1f, "x"))),
            new ModuleItem("disabler", "禁用器", "禁用反作弊部分检测功能", false,
                    null, null, "Disabler", "Disable certain anti-cheat checks",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("EC", "布吉岛", "Cubecraft"),
                                    Arrays.asList("EC", "Buji Island", "Cubecraft"),
                                    "EC"))),
            new ModuleItem("anticheatdisabler", "反作弊禁用", "绕过特定反作弊系统检测", false,
                    null, null, "AntiCheatDisabler", "Bypass specific anti-cheat system detection",
                    Arrays.asList(
                            SubSetting.combo("mode", "模式", "Mode",
                                    Arrays.asList("AAC", "NCP", "Verus"),
                                    Arrays.asList("AAC", "NCP", "Verus"),
                                    "AAC")
                    ))
    ));

    private Misc() {
    }
}
