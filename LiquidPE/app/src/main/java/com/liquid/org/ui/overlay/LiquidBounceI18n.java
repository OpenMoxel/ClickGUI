/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.overlay;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局中英切换。模块名按「模块 id」翻译(避免 Timer/Speed/Chat 等与设置项同名的歧义);
 * 分类、设置名、枚举选项、界面文案按「英文串」翻译;未收录的串回退英文。
 * 搜索框文本刻意不经过这里,保持英文。
 */
public final class LiquidBounceI18n {
    private static boolean chinese;
    private static final Map<String, String> MODULES = new HashMap<>();
    private static final Map<String, String> ZH = new HashMap<>();

    private LiquidBounceI18n() {}

    public static boolean isChinese() { return chinese; }
    public static void setChinese(boolean value) { chinese = value; }

    /** 模块显示名:优先按模块 id 查中文表。 */
    public static String moduleName(String moduleId, String fallback) {
        if (!chinese || moduleId == null) return fallback;
        String zh = MODULES.get(moduleId);
        return zh != null ? zh : fallback;
    }

    /** 通用字符串翻译:分类名、设置名、枚举选项、单位、界面文案。 */
    public static String t(String en) {
        if (en == null || !chinese) return en;
        String zh = ZH.get(en);
        return zh != null ? zh : en;
    }

    static {
        // ── 界面文案 ──
        ZH.put("HUD Editor", "HUD 编辑");
        ZH.put("Settings", "设置");
        ZH.put("Binds", "按键");
        ZH.put("No binds", "无按键");
        ZH.put("Enabled", "开启");
        ZH.put("Disabled", "关闭");
        ZH.put("Press a key", "按下一个键");
        ZH.put("Language", "语言");
        ZH.put("Chinese", "中文");
        ZH.put("English", "英语");

        // ── 分类名 ──
        ZH.put("Combat", "战斗");
        ZH.put("Movement", "移动");
        ZH.put("World", "世界");
        ZH.put("Render", "渲染");
        ZH.put("Player", "玩家");
        ZH.put("Misc", "杂项");

        // ── 通用设置名 / 单位 ──
        ZH.put("Bind", "绑定键");
        ZH.put("Mode", "模式");
        ZH.put("Speed", "速度");
        ZH.put("Range", "距离");
        ZH.put("Delay", "延迟");
        ZH.put("Interval", "间隔");
        ZH.put("Height", "高度");
        ZH.put("Distance", "距离");
        ZH.put("Target", "目标");
        ZH.put("Health", "血量");
        ZH.put("Color", "颜色");
        ZH.put("Name", "名称");
        ZH.put("Opacity", "透明度");
        ZH.put("Width", "宽度");
        ZH.put("Duration", "持续时间");
        ZH.put("Brightness", "亮度");
        ZH.put("Zoom", "缩放倍率");
        ZH.put("blocks", "格");
        ZH.put("ms", "毫秒");
        ZH.put("s", "秒");
        ZH.put("cps", "cps");
        ZH.put("ticks", "tick");
        ZH.put("px", "像素");
        ZH.put("x", "倍");
        ZH.put("%", "%");
        ZH.put("°", "°");

        // ── 新增设置名 / 选项 ──
        ZH.put("Zoom FOV", "缩放视角联动");
        ZH.put("Detect", "检测");
        ZH.put("Teammates", "队友");
        ZH.put("Bots", "假人");
        ZH.put("Particle Type", "粒子效果类型");
        ZH.put("Spark", "火花");
        ZH.put("Cloud", "云");

        // ── 模块名(按 id)──
        MODULES.put("combat.kill_aura", "杀戮光环");
        MODULES.put("combat.auto_aim", "自动瞄准");
        MODULES.put("combat.auto_click", "自动点击");
        MODULES.put("combat.hitbox", "碰撞箱");
        MODULES.put("combat.infinite_aura", "无限光环");
        MODULES.put("combat.anti_knockback", "反击退");
        MODULES.put("combat.hammer_aura", "重锤光环");
        MODULES.put("combat.surround", "环绕");
        MODULES.put("combat.back_track", "锁背");
        MODULES.put("combat.criticals", "刀刀暴击");
        MODULES.put("combat.blink", "Blink");
        MODULES.put("movement.fly", "飞行");
        MODULES.put("movement.air_jump", "空中跳跃");
        MODULES.put("movement.bhop", "移动跳跃");
        MODULES.put("movement.jesus", "水上行走");
        MODULES.put("movement.no_slow", "无减速");
        MODULES.put("movement.auto_sprint", "自动疾跑");
        MODULES.put("movement.sprint_backpack", "冲刺背包");
        MODULES.put("movement.void_bounce", "虚空回弹");
        MODULES.put("movement.no_fall", "无摔伤");
        MODULES.put("movement.scaffold", "自动搭路");
        MODULES.put("movement.crawler", "爬行者");
        MODULES.put("movement.phase", "穿墙");
        MODULES.put("movement.click_tp", "点击传送");
        MODULES.put("world.area_break", "范围破坏");
        MODULES.put("world.auto_bed", "自动挖床");
        MODULES.put("world.god_apple", "金苹果");
        MODULES.put("world.chest_aura", "箱子光环");
        MODULES.put("world.steal_items", "一键取物");
        MODULES.put("world.remote_shop", "远程商店");
        MODULES.put("world.game_time", "游戏时间");
        MODULES.put("world.coord_display", "显示坐标");
        MODULES.put("world.crash_server", "一键崩服");
        MODULES.put("world.griefing", "Griefing");
        MODULES.put("misc.anti_kick", "防踢出");
        MODULES.put("misc.bypass_anti_cheat", "绕反作弊");
        MODULES.put("misc.bypass_motion", "绕移动检测");
        MODULES.put("misc.cookie_login", "Cookie");
        MODULES.put("misc.face_bypass", "人脸绕过");
        MODULES.put("misc.chat", "聊天增强");
        MODULES.put("misc.language", "语言");
        MODULES.put("misc.watermark", "水印");
        MODULES.put("misc.theme", "ClickGUI主题");
        MODULES.put("misc.array_list", "模块列表");
        MODULES.put("misc.music_player", "音乐播放器");
        MODULES.put("player.suicide_aura", "自杀光环");
        MODULES.put("player.inventory_sort", "背包整理");
        MODULES.put("player.game_mode", "游戏模式");
        MODULES.put("player.anti_bot", "反假人");
        MODULES.put("player.potion_effects", "药水效果");
        MODULES.put("player.motion_cam", "运动相机");
        MODULES.put("player.player_panel", "人物面板");
        MODULES.put("player.no_hurt_cam", "无受击晃动");
        MODULES.put("player.name_protect", "昵称保护");
        MODULES.put("render.swing_anim", "挥手动画");
        MODULES.put("render.gyro", "大陀螺");
        MODULES.put("render.esp", "绘制ESP");
        MODULES.put("render.tracers", "人物线条");
        MODULES.put("render.game_render", "游戏渲染");
        MODULES.put("render.zoomr", "放大镜变焦");
        MODULES.put("render.trails", "拖尾");
        MODULES.put("render.xray", "X-ray");
        MODULES.put("render.damage_particles", "伤害粒子");
        MODULES.put("render.hit_color", "命中特效");
        MODULES.put("render.jump_circle", "跳跃圈");
        MODULES.put("render.binds", "按键");

        // ── Combat 设置与选项 ──
        ZH.put("Clicker", "点击器");
        ZH.put("Simulate Click", "模拟点击");
        ZH.put("Chance", "几率");
        ZH.put("Attack Range", "攻击范围");
        ZH.put("Rotations", "旋转");
        ZH.put("Rotation Speed", "旋转速度");
        ZH.put("FOV", "视场角");
        ZH.put("Target Mode", "目标模式");
        ZH.put("Single", "单个");
        ZH.put("Multi", "多个");
        ZH.put("Phase Through Blocks", "穿墙攻击");
        ZH.put("Requires", "需求条件");
        ZH.put("Click", "点击");
        ZH.put("Weapon", "手持武器");
        ZH.put("Empty Hand", "空手");
        ZH.put("Vanilla Name", "原版名称");
        ZH.put("Not Breaking", "未破方块");
        ZH.put("Raycast", "射线检测");
        ZH.put("All", "全部");
        ZH.put("None", "无");
        ZH.put("Enemy", "敌人");
        ZH.put("Keep Sprint", "保留疾跑");
        ZH.put("Ignore Open Inventory", "忽略打开背包");
        ZH.put("Max Range", "最大距离");
        ZH.put("Target Logic", "目标逻辑");
        ZH.put("Nearest", "最近");
        ZH.put("Lowest Health", "最低血量");
        ZH.put("Highest Health", "最高血量");
        ZH.put("CPS", "CPS");
        ZH.put("Click Delay", "点击延迟");
        ZH.put("Smoothness", "平滑度");
        ZH.put("Horizontal Speed", "水平速度");
        ZH.put("Vertical Speed", "垂直速度");
        ZH.put("Priority", "优先级");
        ZH.put("Closest", "最近");
        ZH.put("Angle", "角度");
        ZH.put("Aim Speed", "瞄准速度");
        ZH.put("Max Angle", "最大角度");
        ZH.put("Sync", "同步方式");
        ZH.put("Client", "客户端");
        ZH.put("Server", "服务端");
        ZH.put("Both", "两者");
        ZH.put("Trigger Mode", "触发模式");
        ZH.put("Jump", "跳跃");
        ZH.put("Fall", "下落");
        ZH.put("Packet", "数据包");
        ZH.put("Smart", "智能");
        ZH.put("Scale", "缩放倍数");
        ZH.put("Horizontal", "水平");
        ZH.put("Vertical", "垂直");
        ZH.put("Trigger", "触发条件");
        ZH.put("Always", "始终");
        ZH.put("Health Below", "血量低于");
        ZH.put("In Range", "范围内");
        ZH.put("Reverse Key", "反向按键");
        ZH.put("Lock Range", "锁定范围");
        ZH.put("Silent", "静默");
        ZH.put("Lock Point", "锁定点");
        ZH.put("Head", "头部");
        ZH.put("Chest", "胸部");
        ZH.put("Feet", "脚部");
        ZH.put("Damage Multiplier", "伤害倍率");
        ZH.put("Ground Only", "仅地面");
        ZH.put("Auto Switch Direction", "自动切换方向");
        ZH.put("Teleport Mode", "瞬移模式");
        ZH.put("Target Distance", "保持距离");
        ZH.put("Strafe Assist", "横移辅助");
        ZH.put("Random Direction", "随机方向");
        ZH.put("Players Only", "仅玩家");

        // ── Movement 设置与选项 ──
        ZH.put("Vanilla", "原版");
        ZH.put("Glide", "滑翔");
        ZH.put("Jetpack", "喷气背包");
        ZH.put("LegitHop", "常规跳跃");
        ZH.put("Strafe", "横向加速");
        ZH.put("Auto Jump", "自动跳跃");
        ZH.put("Full", "完全");
        ZH.put("Partial", "部分");
        ZH.put("Reduction", "减免比例");
        ZH.put("Edge Distance", "边缘检测距离");
        ZH.put("Normal", "普通");
        ZH.put("NCP", "NCP");
        ZH.put("Speed Bonus", "速度加成");
        ZH.put("Jump Distance", "跳跃距离");
        ZH.put("Burst", "爆发");
        ZH.put("Constant", "持续");
        ZH.put("Charge", "蓄力");
        ZH.put("Solid", "固体");
        ZH.put("Float", "悬浮");
        ZH.put("Water Jump", "水上跳跃");
        ZH.put("Fly Speed", "飞行速度");
        ZH.put("Sync Zoom", "缩放联动");
        ZH.put("All Directions", "全方向");
        ZH.put("Auto Switch", "自动切换");
        ZH.put("Bounce Force", "回弹力度");
        ZH.put("Auto Detect Void", "自动检测虚空");
        ZH.put("Crawl Speed", "爬行速度");
        ZH.put("Auto Wall", "自动贴墙");
        ZH.put("Fall Speed", "下落速度");
        ZH.put("Auto Glide", "自动滑翔");

        // ── World 设置与选项 ──
        ZH.put("Radius", "破坏半径");
        ZH.put("Blocks", "方块");
        ZH.put("Stone", "石头");
        ZH.put("Dirt", "泥土");
        ZH.put("Sand", "沙子");
        ZH.put("Wood", "木头");
        ZH.put("Ores", "矿石");
        ZH.put("Iron", "铁");
        ZH.put("Gold", "金");
        ZH.put("Diamond", "钻石");
        ZH.put("Emerald", "绿宝石");
        ZH.put("Coal", "煤");
        ZH.put("Redstone", "红石");
        ZH.put("Place Delay", "放置延迟");
        ZH.put("Extend", "扩展方向");
        ZH.put("Forward", "向前");
        ZH.put("Backward", "向后");
        ZH.put("Left", "向左");
        ZH.put("Right", "向右");
        ZH.put("Auto Place", "自动放置");
        ZH.put("Beds", "床");
        ZH.put("Auto Disable", "完成后关闭");
        ZH.put("Time Speed", "时间速度");
        ZH.put("Sync Server", "同步服务端");
        ZH.put("Preset", "预设");
        ZH.put("Day", "白天");
        ZH.put("Night", "夜晚");
        ZH.put("Sunset", "日落");
        ZH.put("Sunrise", "日出");
        ZH.put("Freeze Time", "冻结时间");
        ZH.put("Immune Damage", "免疫伤害");
        ZH.put("No Hunger", "免饥饿");
        ZH.put("No Void", "免虚空掉落");
        ZH.put("Auto Trade", "自动交易");
        ZH.put("Suicide Mode", "自毁模式");
        ZH.put("Method", "方式");
        ZH.put("Position", "位置");
        ZH.put("Entity", "实体");
        ZH.put("Chat", "聊天");
        ZH.put("Strength", "强度倍率");
        ZH.put("Particle Amount", "粒子数量");
        ZH.put("Effect", "粒子效果");
        ZH.put("Explosion", "爆炸");
        ZH.put("Flame", "火焰");
        ZH.put("Smoke", "烟雾");
        ZH.put("Portal", "传送门");
        ZH.put("Lag", "卡顿");
        ZH.put("Kick", "踢出");

        // ── Player 设置与选项 ──
        ZH.put("Quality", "品质");
        ZH.put("Defense", "防御力");
        ZH.put("Replace Damaged", "替换损坏装备");
        ZH.put("Whitelist", "白名单");
        ZH.put("Blacklist", "黑名单");
        ZH.put("Swords", "剑");
        ZH.put("Armor", "盔甲");
        ZH.put("Tools", "工具");
        ZH.put("Food", "食物");
        ZH.put("Junk", "垃圾");
        ZH.put("Cobblestone", "圆石");
        ZH.put("Seeds", "种子");
        ZH.put("Rotten Flesh", "腐肉");
        ZH.put("Items", "物品列表");
        ZH.put("Types", "回收类型");
        ZH.put("Amount", "回收数量");
        ZH.put("Bows", "弓");
        ZH.put("Rods", "鱼竿");
        ZH.put("Enchanted", "附魔");
        ZH.put("Auto", "自动");
        ZH.put("Friendly Fire Protection", "友伤保护");
        ZH.put("Creative", "创造");
        ZH.put("Survival", "生存");
        ZH.put("Adventure", "冒险");
        ZH.put("Spectator", "旁观");
        ZH.put("Slots", "显示部位");
        ZH.put("Helmet", "头盔");
        ZH.put("Chestplate", "胸甲");
        ZH.put("Leggings", "护腿");
        ZH.put("Boots", "靴子");
        ZH.put("Main Hand", "主手");
        ZH.put("Off Hand", "副手");

        // ── Render 设置与选项 ──
        ZH.put("Lapis", "青金石");
        ZH.put("Targets", "目标类型");
        ZH.put("Animals", "动物");
        ZH.put("Content", "显示内容");
        ZH.put("Custom", "自定义");
        ZH.put("Show Timer", "显示计时器");
        ZH.put("Strength", "动态强度");
        ZH.put("View Sway", "视角摆动");
        ZH.put("Spin Speed", "旋转速度");
        ZH.put("Reverse", "反转方向");
        ZH.put("Box", "方框");
        ZH.put("Fill", "填充");
        ZH.put("Outline", "轮廓");
        ZH.put("Glow", "发光");
        ZH.put("Border Color", "边框颜色");
        ZH.put("Fullbright", "全亮");
        ZH.put("No Fog", "去雾");
        ZH.put("No Weather", "无天气");
        ZH.put("Landing Mark", "落点标记");
        ZH.put("Meteor", "流星");
        ZH.put("Hearts", "爱心");
        ZH.put("Notes", "音符");
        ZH.put("Stars", "星星");
        ZH.put("Length", "拖尾长度");
        ZH.put("Path Width", "路径线宽度");
        ZH.put("Smooth Zoom", "平滑缩放");
        ZH.put("Hit Color", "受击颜色");

        // ── Misc 设置与选项 ──
        ZH.put("Messages", "消息列表");
        ZH.put("Randomize", "随机化");
        ZH.put("Anticheats", "反作弊系统");
        ZH.put("Global", "全局禁用");
        ZH.put("Bypasses", "绕过选项");
        ZH.put("Rotation", "旋转");
        ZH.put("Move", "移动");
        ZH.put("Timeout Protection", "超时保护");
        ZH.put("Packet Protection", "数据包保护");
        ZH.put("Auto Detect", "自动识别");
        ZH.put("Auto Login", "自动登陆");
        ZH.put("Spoof", "伪装");
        ZH.put("Simulate", "模拟");
        ZH.put("Skip", "跳过");
        ZH.put("Intercept", "拦截");
        ZH.put("Spam", "刷屏");
        ZH.put("Send Interval", "发送间隔");
        ZH.put("Show Biome", "显示生物群系");
        ZH.put("Config Hot Reload", "配置热加载");
        ZH.put("Script Hot Reload", "脚本热加载");
        ZH.put("Team Color", "队友颜色");
        ZH.put("Scoreboard", "计分板");
        ZH.put("Show Friends", "好友可见");
        ZH.put("Font Size", "字体大小");
        ZH.put("Text Color", "文字颜色");
        ZH.put("Theme", "主题");
        ZH.put("Dark", "深色");
        ZH.put("Light", "浅色");
        // ── 新增设置/选项翻译(按当前清单版本)──
        ZH.put("Target Priority", "目标优先级");
        ZH.put("Click Type", "点击类型");
        ZH.put("Left", "左键");
        ZH.put("Right", "右键");
        ZH.put("Both", "双键");
        ZH.put("X Expand", "X轴扩展");
        ZH.put("Y Expand", "Y轴扩展");
        ZH.put("Z Expand", "Z轴扩展");
        ZH.put("Through Walls", "穿墙");
        ZH.put("Weapon Only", "仅手持武器");
        ZH.put("Step Height", "跨越高度");
        ZH.put("Reverse Mode", "反向跳跃模式");
        ZH.put("Eating", "进食");
        ZH.put("Blocking", "格挡");
        ZH.put("Aiming", "瞄准");
        ZH.put("Web", "蜘蛛网");
        ZH.put("Water Bucket", "水桶落地");
        ZH.put("Ride Mode", "搭载模式");
        ZH.put("Sprint Place", "疾跑搭路");
        ZH.put("Silent Place", "静默放置");
        ZH.put("Smart Filter", "智能过滤");
        ZH.put("Instant Break", "瞬间破坏");
        ZH.put("Hotbar Only", "仅快捷栏");
        ZH.put("Auto Close", "自动关闭");
        ZH.put("Valuables Only", "仅贵重物品");
        ZH.put("Notifications", "检测通知");
        ZH.put("Smart", "智能");
        ZH.put("Strict", "严格");
        ZH.put("Relaxed", "宽松");
        ZH.put("Layout", "排列模式");
        ZH.put("Compact", "紧凑");
        ZH.put("Balanced", "平衡");
        ZH.put("Wide", "宽散");
        ZH.put("Sound", "整理音效");
        ZH.put("Instant", "立即复活");
        ZH.put("Effects", "状态效果");
        ZH.put("Filter Type", "过滤类型");
        ZH.put("Inventory Filter", "背包操作过滤");
        ZH.put("Times", "重复次数");
        ZH.put("Style", "动画风格");
        ZH.put("Default", "默认");
        ZH.put("Smooth", "平滑");
        ZH.put("Fast", "快速");
        ZH.put("Slow", "慢速");
        ZH.put("Block Count", "显示方块数量");
        ZH.put("Filter Types", "过滤显示类型");
        ZH.put("Basic Blocks", "基础方块");
        ZH.put("Chests", "箱子");
        ZH.put("Critical", "暴击");
        ZH.put("Volume", "播放音量");
        ZH.put("Color Theme", "颜色主题");
        ZH.put("Accent", "强调色");
        ZH.put("Rainbow", "彩虹");
        ZH.put("Background", "背景样式");
        ZH.put("Display Mode", "显示模式");
        ZH.put("Bold Text", "粗体文字");
        ZH.put("Teleport", "瞬移");
        ZH.put("Pullback", "拉回");
        ZH.put("Anti Cheat", "反作弊");
        ZH.put("Anti Kick", "防踢保护");
        ZH.put("Ground", "地面");
        ZH.put("Particle", "粒子");
        ZH.put("Crash", "崩溃");
        ZH.put("Ores", "矿石");
        ZH.put("Beds", "床");
        ZH.put("FakeLag", "FakeLag");
        ZH.put("Legit", "Legit");
        ZH.put("Sharpness", "Sharpness");

    }
}


