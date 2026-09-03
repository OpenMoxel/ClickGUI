/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import java.util.ArrayList;
import java.util.List;

/** Owns the visual-demo module catalog so feature data is not embedded in the View. */
final class ClickGuiModuleCatalog {

    private ClickGuiModuleCatalog() {
    }

    static List<ClickGuiCategory> create() {
        List<ClickGuiCategory> categories = new ArrayList<>();
        addCategory(categories, "世界",
                module("remote_store", "远程商店", ClickGuiIconKind.CHEST, false, false, true),
                module("auto_mine", "自动挖矿", ClickGuiIconKind.PICK, false),
                module("bed_destroy", "Automatic destruction of bed", ClickGuiIconKind.BLOCK, false),
                module("area_breaker", "范围破坏", ClickGuiIconKind.CUBE, true),
                module("destroy", "破坏", ClickGuiIconKind.GEM, true, false, false));

        addCategory(categories, "攻击",
                module("aim", "自动瞄准", ClickGuiIconKind.TARGET, true),
                module("aura", "无限光环", ClickGuiIconKind.SPARK, true),
                module("chest_aura", "箱子光环", ClickGuiIconKind.CHEST, true),
                module("chest_steal", "箱子小偷", ClickGuiIconKind.GEM, true),
                module("critical", "刀刃暴击", ClickGuiIconKind.SWORD, true),
                module("hitbox", "碰撞箱", ClickGuiIconKind.BLOCK, true),
                module("trigger", "触发连击", ClickGuiIconKind.BOLT, false),
                module("target_hud", "目标显示", ClickGuiIconKind.EYE, false),
                module("reach", "攻击距离", ClickGuiIconKind.ARROWS, true));

        addCategory(categories, "移动",
                module("sprint", "自动疾跑", ClickGuiIconKind.ARROWS, false),
                module("speed", "移动加速", ClickGuiIconKind.BOLT, true),
                module("high_jump", "高跳", ClickGuiIconKind.UP, true),
                module("no_slow", "无减速", ClickGuiIconKind.ARROWS, false),
                module("safe_walk", "边缘保护", ClickGuiIconKind.SHIELD, false),
                module("step", "自动跨越", ClickGuiIconKind.STEPS, true),
                module("glide", "滑翔", ClickGuiIconKind.FEATHER, true),
                module("phase", "相位移动", ClickGuiIconKind.CUBE, true),
                module("long_jump", "远跳", ClickGuiIconKind.UP, true));

        addCategory(categories, "渲染",
                module("magnifier", "放大镜", ClickGuiIconKind.MAGNIFIER, true),
                module("hand_animation", "手部动画", ClickGuiIconKind.HAND, true),
                module("swing_interval", "挥手间隔", ClickGuiIconKind.ARROWS, true),
                module("force_glint", "强制漆染", ClickGuiIconKind.SPARK, false),
                module("block_tracer", "方块追踪", ClickGuiIconKind.TRACER, true),
                module("coordinates", "强制坐标", ClickGuiIconKind.TARGET, false),
                module("esp", "实体透视", ClickGuiIconKind.EYE, true),
                module("full_bright", "全亮显示", ClickGuiIconKind.SUN, false),
                module("name_tags", "名称标签", ClickGuiIconKind.TAG, true));

        addCategory(categories, "脚本",
                module("anti_kick", "HYT防踢出", ClickGuiIconKind.SHIELD, false),
                module("delay_packet", "延迟发包", ClickGuiIconKind.PACKET, false),
                module("block_reach", "BlockReach", ClickGuiIconKind.BLOCK, true),
                module("entity_split", "实体分离", ClickGuiIconKind.CUBE, false),
                module("auto_chat", "自动消息", ClickGuiIconKind.CHAT, true),
                module("script_timer", "脚本计时器", ClickGuiIconKind.CLOCK, true),
                module("event_log", "事件日志", ClickGuiIconKind.LIST, false),
                module("macro", "演示宏", ClickGuiIconKind.BOLT, true),
                module("sandbox", "沙盒模式", ClickGuiIconKind.SHIELD, false));

        addCategory(categories, "界面",
                module("hud", "高阶模组设置", ClickGuiIconKind.SCREEN, true),
                module("panel", "参数窗口设置", ClickGuiIconKind.SLIDERS, true),
                module("notice", "开关提示", ClickGuiIconKind.BELL, false),
                module("keybind", "快捷键设置", ClickGuiIconKind.KEY, true),
                module("theme", "主题颜色", ClickGuiIconKind.PALETTE, true),
                module("compact", "紧凑布局", ClickGuiIconKind.LIST, false),
                module("scale", "界面缩放", ClickGuiIconKind.ARROWS, true),
                module("cursor", "光标反馈", ClickGuiIconKind.TARGET, false),
                module("language", "文字语言", ClickGuiIconKind.TAG, true));

        addCategory(categories, "配置",
                module("save", "保存当前配置", ClickGuiIconKind.SAVE, false),
                module("load", "加载演示配置", ClickGuiIconKind.FOLDER, false),
                module("reset", "重置界面状态", ClickGuiIconKind.RESET, false),
                module("profiles", "配置档案", ClickGuiIconKind.LIST, true),
                module("export", "导出布局", ClickGuiIconKind.UP, false),
                module("import", "导入布局", ClickGuiIconKind.DOWN, false),
                module("backup", "备份快照", ClickGuiIconKind.CLOUD, false),
                module("sync", "状态同步", ClickGuiIconKind.ARROWS, false),
                module("about", "关于演示", ClickGuiIconKind.INFO, false));

        addCategory(categories, "Mod",
                module("client", "客户端演示", ClickGuiIconKind.CUBE, false),
                module("inspector", "界面检查器", ClickGuiIconKind.EYE, true),
                module("debug", "调试信息", ClickGuiIconKind.LIST, false),
                module("layout_grid", "参考网格", ClickGuiIconKind.GRID, false),
                module("safe_mode", "安全模式", ClickGuiIconKind.SHIELD, false),
                module("motion", "动效预览", ClickGuiIconKind.SPARK, true),
                module("background", "背景强度", ClickGuiIconKind.SUN, true),
                module("touch", "触摸标记", ClickGuiIconKind.TARGET, false),
                module("version", "版本信息", ClickGuiIconKind.INFO, false));
        return categories;
    }

    private static void addCategory(List<ClickGuiCategory> categories, String label,
                                    ClickGuiModule... items) {
        ClickGuiCategory category = new ClickGuiCategory(label);
        for (ClickGuiModule item : items) {
            category.modules.add(item);
        }
        categories.add(category);
    }

    private static ClickGuiModule module(String id, String label, ClickGuiIconKind iconKind,
                                         boolean hasSettings) {
        return new ClickGuiModule(id, label, iconKind, hasSettings, true, false);
    }

    private static ClickGuiModule module(String id, String label, ClickGuiIconKind iconKind,
                                         boolean hasSettings, boolean hasToggle,
                                         boolean hasRunAction) {
        return new ClickGuiModule(id, label, iconKind, hasSettings, hasToggle, hasRunAction);
    }
}
