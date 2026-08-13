/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org.ui.overlay;

import com.liquid.org.ui.overlay.LiquidBounceModels.BindSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.CategoryPanel;
import com.liquid.org.ui.overlay.LiquidBounceModels.ColorSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.DropdownSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.ModuleEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.MultiSelectSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.RangeSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.SettingGroup;
import com.liquid.org.ui.overlay.LiquidBounceModels.SliderSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.ToggleSetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LiquidBounceDataStore {
    public enum BindsVisibility { ALL_BOUND, ENABLED_BOUND }

    /** Single state-change stream consumed by every visual projection of a module. */
    public interface ModuleStateListener {
        void onModuleEnabledChanged(ModuleEntry module, boolean enabled);
        void onModuleMetadataChanged(ModuleEntry module);
    }

    private final List<CategoryPanel> categories = new ArrayList<>();
    private final Map<String, ModuleEntry> modulesById = new LinkedHashMap<>();
    private final List<ModuleStateListener> listeners = new ArrayList<>();
    private BindsVisibility bindsVisibility = BindsVisibility.ALL_BOUND;

    public List<CategoryPanel> getCategories() { return Collections.unmodifiableList(categories); }
    public List<ModuleEntry> getModules() { return Collections.unmodifiableList(new ArrayList<>(modulesById.values())); }

    public CategoryPanel addCategory(String id, String name, float x, float y) {
        CategoryPanel category = new CategoryPanel(id, name, x, y);
        categories.add(category);
        return category;
    }

    public ModuleEntry addModule(String id, String name, String categoryId) {
        if (modulesById.containsKey(id)) throw new IllegalArgumentException("Duplicate module id " + id);
        CategoryPanel category = findCategory(categoryId);
        if (category == null) throw new IllegalArgumentException("Unknown category " + categoryId);
        ModuleEntry module = new ModuleEntry(id, name, categoryId);
        modulesById.put(id, module);
        category.modules.add(module);
        return module;
    }

    public ModuleEntry findModule(String idOrName) {
        ModuleEntry direct = modulesById.get(idOrName);
        if (direct != null) return direct;
        for (ModuleEntry module : modulesById.values()) if (module.name.equalsIgnoreCase(idOrName)) return module;
        return null;
    }

    public CategoryPanel findCategory(String idOrName) {
        for (CategoryPanel category : categories) if (category.id.equals(idOrName) || category.name.equalsIgnoreCase(idOrName)) return category;
        return null;
    }

    public List<ModuleEntry> getEnabledModules() {
        List<ModuleEntry> result = new ArrayList<>();
        for (ModuleEntry module : modulesById.values()) if (module.enabled && module.showInArrayList) result.add(module);
        return result;
    }

    public List<ModuleEntry> getBoundModules() {
        List<ModuleEntry> result = new ArrayList<>();
        for (ModuleEntry module : modulesById.values()) {
            if (module.showInBinds && module.hasBind()
                    && (bindsVisibility == BindsVisibility.ALL_BOUND || module.enabled)) result.add(module);
        }
        return result;
    }

    public void setBindsVisibility(BindsVisibility visibility) { bindsVisibility = visibility; }
    public BindsVisibility getBindsVisibility() { return bindsVisibility; }

    public void addModuleStateListener(ModuleStateListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeModuleStateListener(ModuleStateListener listener) { listeners.remove(listener); }

    public boolean toggleModule(String idOrName) {
        ModuleEntry module = findModule(idOrName);
        return module != null && setModuleEnabled(module.id, !module.enabled);
    }

    public boolean setModuleEnabled(String idOrName, boolean enabled) {
        ModuleEntry module = findModule(idOrName);
        if (module == null || module.enabled == enabled) return false;
        module.enabled = enabled;
        module.enabledProgress.animateTo(enabled ? 1f : 0f, LiquidBounceUiDurations.MODULE_TOGGLE);
        for (ModuleStateListener listener : new ArrayList<>(listeners)) listener.onModuleEnabledChanged(module, enabled);
        return true;
    }

    public boolean setModuleSuffix(String idOrName, String suffix) {
        ModuleEntry module = findModule(idOrName);
        String normalized = suffix == null ? "" : suffix;
        if (module == null || normalized.equals(module.arrayListSuffix)) return false;
        module.arrayListSuffix = normalized;
        notifyModuleMetadataChanged(module);
        return true;
    }

    public boolean setModuleKeyBind(String idOrName, String keyBind) {
        ModuleEntry module = findModule(idOrName);
        String normalized = keyBind == null || keyBind.isEmpty() ? "None" : keyBind;
        if (module == null || normalized.equals(module.keyBind)) return false;
        module.keyBind = normalized;
        notifyModuleMetadataChanged(module);
        return true;
    }

    public boolean setShowInArrayList(String idOrName, boolean visible) {
        ModuleEntry module = findModule(idOrName);
        if (module == null || module.showInArrayList == visible) return false;
        module.showInArrayList = visible;
        notifyModuleMetadataChanged(module);
        return true;
    }

    public boolean setShowInBinds(String idOrName, boolean visible) {
        ModuleEntry module = findModule(idOrName);
        if (module == null || module.showInBinds == visible) return false;
        module.showInBinds = visible;
        notifyModuleMetadataChanged(module);
        return true;
    }

    private void notifyModuleMetadataChanged(ModuleEntry module) {
        for (ModuleStateListener listener : new ArrayList<>(listeners)) listener.onModuleMetadataChanged(module);
    }

    public static LiquidBounceDataStore createDemo() {
        LiquidBounceDataStore store = new LiquidBounceDataStore();
        store.addCategory("combat", "Combat", 30, 194);
        store.addCategory("movement", "Movement", 435, 194);
        store.addCategory("world", "World", 840, 194);
        store.addCategory("render", "Render", 1245, 194);
        store.addCategory("player", "Player", 1650, 194);
        store.addCategory("misc", "Misc", 2055, 194);

        // ── 战斗 Combat ─────────────────────────────────────────────────
        store.addModule("combat.kill_aura", "KillAura", "combat").settings(
                new SliderSetting("attack_range", "Attack Range", 1, 6, 3.2f, "blocks"),
                new SliderSetting("cps", "CPS", 1, 20, 8, "cps"),
                new DropdownSetting("priority", "Target Priority", "Distance", "Distance", "Health", "Angle"),
                new ToggleSetting("players", "Players", true),
                new ToggleSetting("mobs", "Mobs", false));
        store.addModule("combat.auto_aim", "AutoAim", "combat").settings(
                new SliderSetting("lock_range", "Lock Range", 1, 8, 4, "blocks"),
                new SliderSetting("smoothness", "Smoothness", 0, 20, 8, ""),
                new ToggleSetting("silent", "Silent", true),
                new DropdownSetting("lock_point", "Lock Point", "Head", "Head", "Chest", "Feet"));
        store.addModule("combat.auto_click", "AutoClick", "combat").settings(
                new SliderSetting("cps", "CPS", 1, 20, 8, "cps"),
                new ToggleSetting("randomize", "Randomize", false),
                new DropdownSetting("click_type", "Click Type", "Left", "Left", "Right", "Both"));
        store.addModule("combat.hitbox", "Hitbox", "combat").settings(
                new SliderSetting("expand_x", "X Expand", 0, 2, .2f, "x"),
                new SliderSetting("expand_y", "Y Expand", 0, 2, .1f, "x"),
                new SliderSetting("expand_z", "Z Expand", 0, 2, .2f, "x"),
                new ToggleSetting("players", "Players", true),
                new ToggleSetting("mobs", "Mobs", false));
        store.addModule("combat.infinite_aura", "InfiniteAura", "combat").settings(
                new SliderSetting("range", "Range", 4, 12, 8, "blocks"),
                new SliderSetting("speed", "Speed", 0, 20, 10, ""),
                new ToggleSetting("through_walls", "Through Walls", false));
        store.addModule("combat.anti_knockback", "AntiKnockback", "combat").settings(
                new SliderSetting("reduction", "Reduction", 0, 100, 100, "%"),
                new ToggleSetting("horizontal", "Horizontal", true),
                new ToggleSetting("vertical", "Vertical", true));
        store.addModule("combat.hammer_aura", "HammerAura", "combat").settings(
                new SliderSetting("damage", "Damage Multiplier", 1, 10, 2, "x"),
                new ToggleSetting("ground_only", "Ground Only", true));
        store.addModule("combat.surround", "Surround", "combat").settings(
                new SliderSetting("speed", "Speed", 0, 5, 1, "x"));
        store.addModule("combat.back_track", "BackTrack", "combat");
        store.addModule("combat.criticals", "Criticals", "combat").settings(
                new DropdownSetting("mode", "Trigger Mode", "Packet", "Jump", "Ground", "Packet"),
                new ToggleSetting("weapon_only", "Weapon Only", true));
        store.addModule("combat.blink", "Blink", "combat").settings(
                new DropdownSetting("mode", "Mode", "Normal", "Normal", "Legit", "Teleport", "FakeLag"),
                new SliderSetting("delay", "Delay", 0, 40, 10, "ticks"));

        // ── 移动 Movement ────────────────────────────────────────────────
        store.addModule("movement.fly", "Fly", "movement").settings(
                new DropdownSetting("mode", "Mode", "Normal", "Normal", "Teleport", "Glide", "ZoomFly"),
                new SliderSetting("speed", "Speed", .1f, 5, 1, "x"),
                new ToggleSetting("anti_kick", "Anti Kick", false),
                new ToggleSetting("zoom_fov", "Zoom FOV", false));
        store.addModule("movement.air_jump", "AirJump", "movement");
        store.addModule("movement.bhop", "Bhop", "movement").settings(
                new SliderSetting("height", "Jump Height", 0, 2, 1, "x"),
                new SliderSetting("speed", "Speed Bonus", 0, 2, 1.2f, "x"));
        store.addModule("movement.jesus", "Jesus", "movement").settings(
                new DropdownSetting("mode", "Mode", "Solid", "Solid", "Jump", "Float"),
                new ToggleSetting("water_jump", "Water Jump", true));
        store.addModule("movement.no_slow", "NoSlow", "movement").settings(
                new ToggleSetting("eating", "Eating", true),
                new ToggleSetting("blocking", "Blocking", true),
                new ToggleSetting("aiming", "Aiming", false),
                new ToggleSetting("web", "Web", true),
                new ToggleSetting("blocks", "Blocks", false));
        store.addModule("movement.auto_sprint", "AutoSprint", "movement").settings(
                new ToggleSetting("all_directions", "All Directions", false),
                new ToggleSetting("keep_sprint", "Keep Sprint", true));
        store.addModule("movement.sprint_backpack", "SprintBackpack", "movement").settings(
                new SliderSetting("speed", "Speed", 0, 5, 1, "x"),
                new ToggleSetting("auto_switch", "Auto Switch", true));
        store.addModule("movement.void_bounce", "VoidBounce", "movement").settings(
                new SliderSetting("bounce", "Bounce Force", 0, 2, 1, "x"),
                new ToggleSetting("auto_detect", "Auto Detect Void", true));
        store.addModule("movement.no_fall", "NoFall", "movement").settings(
                new DropdownSetting("mode", "Mode", "Packet", "Packet", "Ground", "AntiCheat"),
                new ToggleSetting("water_bucket", "Water Bucket", true));

        store.addModule("movement.scaffold", "Scaffold", "movement").settings(
                new SliderSetting("place_delay", "Place Delay", 0, 200, 50, "ms"),
                new ToggleSetting("sprint_place", "Sprint Place", true),
                new ToggleSetting("silent_place", "Silent Place", false));
        store.addModule("movement.crawler", "Crawler", "movement").settings(
                new SliderSetting("speed", "Crawl Speed", 0, 2, 1, "x"),
                new ToggleSetting("auto_wall", "Auto Wall", true));
        store.addModule("movement.phase", "Phase", "movement").settings(
                new DropdownSetting("mode", "Mode", "Packet", "Packet", "Pullback", "Teleport"),
                new SliderSetting("distance", "Distance", 0, 2, 1, "blocks"));
        store.addModule("movement.click_tp", "ClickTP", "movement").settings(
                new ToggleSetting("through_walls", "Through Walls", false));

        // ── 世界 World ───────────────────────────────────────────────────
        store.addModule("world.area_break", "AreaBreak", "world").settings(
                new SliderSetting("radius", "Radius", 1, 6, 4, "blocks"),
                new ToggleSetting("smart_filter", "Smart Filter", true),
                new ToggleSetting("instant_break", "Instant Break", false));
        store.addModule("world.auto_bed", "AutoBed", "world").settings(
                new SliderSetting("delay", "Delay", 0, 500, 150, "ms"),
                new ToggleSetting("auto_disable", "Auto Disable", true));
        store.addModule("world.god_apple", "GodApple", "world").settings(
                new SliderSetting("health", "Health", 1, 20, 8, ""),
                new ToggleSetting("hotbar_only", "Hotbar Only", true));
        store.addModule("world.chest_aura", "ChestAura", "world").settings(
                new SliderSetting("range", "Range", 1, 8, 5, "blocks"),
                new ToggleSetting("auto_close", "Auto Close", true));
        store.addModule("world.steal_items", "StealItems", "world").settings(
                new SliderSetting("delay", "Delay", 0, 200, 50, "ms"),
                new ToggleSetting("valuables_only", "Valuables Only", false));
        store.addModule("world.remote_shop", "RemoteShop", "world");
        store.addModule("world.game_time", "GameTime", "world").settings(
                new DropdownSetting("preset", "Preset", "Day", "Day", "Night", "Sunset", "Sunrise"),
                new ToggleSetting("freeze", "Freeze Time", false));
        store.addModule("world.coord_display", "CoordDisplay", "world").settings(
                new ToggleSetting("biome", "Show Biome", true));
        store.addModule("world.crash_server", "CrashServer", "world").settings(
                new DropdownSetting("method", "Method", "Packet", "Packet", "Entity", "Particle"),
                new SliderSetting("amount", "Particle Amount", 1, 1000, 100, ""),
                new DropdownSetting("particle_type", "Particle Type", "Explosion", "Explosion", "Flame", "Spark", "Cloud"),
                new SliderSetting("strength", "Strength", 1, 100, 10, "x"));
        store.addModule("world.griefing", "Griefing", "world").settings(
                new DropdownSetting("method", "Method", "Lag", "Lag", "Crash", "Kick"));

        // ── 杂项 Misc ────────────────────────────────────────────────────
        store.addModule("misc.anti_kick", "AntiKick", "misc").settings(
                new ToggleSetting("timeout", "Timeout Protection", true),
                new ToggleSetting("packet", "Packet Protection", true));
        store.addModule("misc.bypass_anti_cheat", "BypassAntiCheat", "misc").settings(
                new DropdownSetting("target", "Target", "Server", "Server", "Client"),
                new ToggleSetting("auto_detect", "Auto Detect", true));
        store.addModule("misc.bypass_motion", "BypassMotion", "misc");
        store.addModule("misc.cookie_login", "CookieLogin", "misc").settings(
                new ToggleSetting("auto_login", "Auto Login", true));
        store.addModule("misc.face_bypass", "FaceBypass", "misc").settings(
                new ToggleSetting("spoof", "Spoof", true),
                new DropdownSetting("mode", "Mode", "Simulate", "Simulate", "Skip", "Intercept"));
        store.addModule("misc.chat", "Chat", "misc").settings(
                new ToggleSetting("spam", "Spam", false),
                new SliderSetting("interval", "Send Interval", 0, 10000, 3000, "ms"));
        store.addModule("misc.language", "Language", "misc").settings(
                new DropdownSetting("lang", "Language", "English", "Chinese", "English"));
        store.addModule("misc.watermark", "Watermark", "misc");
        store.addModule("misc.theme", "Theme", "misc").settings(
                new DropdownSetting("theme", "Theme", "Dark", "Dark", "Light"));
        store.addModule("misc.array_list", "ArrayList", "misc");
        store.addModule("misc.music_player", "MusicPlayer", "misc").settings(
                new SliderSetting("volume", "Volume", 0, 100, 50, "%"));

        // ── 玩家 Player ─────────────────────────────────────────────────
        store.addModule("player.suicide_aura", "SuicideAura", "player").settings(
                new SliderSetting("range", "Range", 1, 8, 4, "blocks"),
                new ToggleSetting("suicide_mode", "Suicide Mode", false));
        store.addModule("player.inventory_sort", "InventorySort", "player").settings(
                new DropdownSetting("layout", "Layout", "Compact", "Compact", "Balanced", "Wide"),
                new ToggleSetting("sound", "Sound", true));
        store.addModule("player.game_mode", "GameMode", "player").settings(
                new DropdownSetting("mode", "Mode", "Creative", "Creative", "Survival", "Adventure", "Spectator"));
        store.addModule("player.anti_bot", "AntiBot", "player").settings(
                new MultiSelectSetting("detect", "Detect", new String[]{"Teammates", "Bots"}, "Teammates", "Bots"),
                new ToggleSetting("friendly_fire", "Friendly Fire Protection", true));
        store.addModule("player.potion_effects", "PotionEffects", "player").settings(
                new ToggleSetting("timer", "Show Timer", true));
        store.addModule("player.motion_cam", "MotionCam", "player").settings(
                new SliderSetting("strength", "Strength", 0, 2, 1, "x"),
                new ToggleSetting("sway", "View Sway", true));
        store.addModule("player.player_panel", "PlayerPanel", "player").settings(
                new ToggleSetting("health", "Health", true),
                new ToggleSetting("armor", "Armor", true),
                new ToggleSetting("effects", "Effects", true));
        store.addModule("player.no_hurt_cam", "NoHurtCam", "player");
        store.addModule("player.name_protect", "NameProtect", "player");
        // ── 渲染 Render ──────────────────────────────────────────────────
        // HUD Binds 面板的独立显示开关；它本身不应出现在 ArrayList 或 Binds 列表中。
        ModuleEntry bindsPanel = store.addModule("render.binds", "Binds", "render");
        bindsPanel.showInArrayList = false;
        bindsPanel.showInBinds = false;
        store.addModule("render.swing_anim", "SwingAnim", "render").settings(
                new DropdownSetting("style", "Style", "Default", "Default", "Smooth", "Fast", "Slow"),
                new SliderSetting("speed", "Speed", .5f, 3, 1, "x"));
        store.addModule("render.gyro", "Gyro", "render").settings(
                new SliderSetting("speed", "Spin Speed", 0, 20, 5, ""),
                new ToggleSetting("reverse", "Reverse", false));
        store.addModule("render.esp", "ESP", "render").settings(
                new DropdownSetting("mode", "Mode", "Box", "Box", "Fill", "Outline", "Glow"),
                new MultiSelectSetting("targets", "Targets", new String[]{"Players", "Mobs"}, "Players", "Mobs"),
                new ColorSetting("color", "Border Color", 0xFF3C69FC, false));
        store.addModule("render.tracers", "Tracers", "render").settings(
                new SliderSetting("width", "Width", 1, 5, 2, "px"),
                new ColorSetting("color", "Color", 0xFF3C69FC, false),
                new ToggleSetting("players", "Players", true));
        store.addModule("render.game_render", "GameRender", "render").settings(
                new ToggleSetting("fullbright", "Fullbright", true),
                new ToggleSetting("no_fog", "No Fog", true),
                new ToggleSetting("no_weather", "No Weather", true),
                new SliderSetting("gamma", "Gamma", 0, 10, 5, ""));
        store.addModule("render.zoomr", "Zoomr", "render").settings(
                new SliderSetting("zoom", "Zoom", 1, 20, 5, "x"),
                new ToggleSetting("smooth", "Smooth Zoom", true));
        store.addModule("render.trails", "Trails", "render").settings(
                new SliderSetting("length", "Length", 1, 100, 20, ""),
                new ColorSetting("color", "Color", 0xFF3C69FC, false),
                new SliderSetting("duration", "Duration", 0, 5, 1, "s"));
        store.addModule("render.xray", "X-ray", "render").settings(
                new SliderSetting("block_count", "Block Count", 1, 100, 20, ""),
                new MultiSelectSetting("types", "Filter Types", new String[]{"Ores", "Basic Blocks", "Chests"}, "Ores"));
        store.addModule("render.damage_particles", "DamageParticles", "render").settings(
                new DropdownSetting("mode", "Mode", "Default", "Default", "Critical", "Sharpness"),
                new SliderSetting("amount", "Particle Amount", 1, 100, 20, ""));
        store.addModule("render.hit_color", "HitColor", "render").settings(
                new ColorSetting("color", "Hit Color", 0xFFFF5A5A, false),
                new SliderSetting("duration", "Duration", 0, 3, 1, "s"));
        // 清单外保留
        store.addModule("render.jump_circle", "Jump Circle", "render").settings(
                new ColorSetting("color", "Color", 0xFF3C69FC, false),
                new SliderSetting("duration", "Duration", 0, 2, 1, "s"));

        configureModule(store, "movement.fly", true, "Normal", "H");
        configureModule(store, "combat.kill_aura", false, "", "R");
        configureModule(store, "combat.criticals", true, "Packet", "C");
        return store;
    }

    private static void configureModule(LiquidBounceDataStore store, String id, boolean enabled, String suffix, String bind) {
        ModuleEntry module = store.findModule(id);
        if (module == null) return;
        module.enabled = enabled;
        module.arrayListSuffix = suffix;
        module.keyBind = bind;
        module.enabledProgress.snapTo(enabled ? 1f : 0f);
    }
}
