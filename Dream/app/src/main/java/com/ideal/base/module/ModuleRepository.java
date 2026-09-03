package com.ideal.base.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the small, UI-only module catalog used by the empty project.
 * A real host can replace this class with an adapter to its existing module backend.
 */
public final class ModuleRepository {

    public static final String COMBAT = "Combat";
    public static final String MOVEMENT = "Movement";
    public static final String PLAYER = "Player";
    public static final String RENDER = "Render";
    public static final String MISC = "Misc";
    public static final String WORLD = "World";
    public static final String CLIENT = "Client";

    private final List<String> categories = Collections.unmodifiableList(Arrays.asList(
            COMBAT, MOVEMENT, PLAYER, RENDER, MISC, WORLD, CLIENT
    ));
    private final Map<String, List<ModuleDefinition>> modulesByCategory = new LinkedHashMap<>();
    private final Map<String, ModuleDefinition> modulesById = new LinkedHashMap<>();

    public ModuleRepository() {
        add(COMBAT, "killaura", "KillAura", 5);
        add(COMBAT, "autoclicker", "AutoClicker", 0);
        add(COMBAT, "reach", "Reach", 0);
        add(COMBAT, "velocity", "Velocity", 0);
        add(COMBAT, "criticals", "Criticals", 0);

        add(MOVEMENT, "sprint", "Sprint", 0);
        add(MOVEMENT, "speed", "Speed", 0);
        add(MOVEMENT, "fly", "Fly", 0);
        add(MOVEMENT, "noslow", "NoSlow", 0);
        add(MOVEMENT, "step", "Step", 0);

        add(PLAYER, "autoarmor", "AutoArmor", 0);
        add(PLAYER, "cheststealer", "ChestStealer", 1);
        add(PLAYER, "invmanager", "InvManager", 0);
        add(PLAYER, "fasteat", "FastEat", 0);
        add(PLAYER, "autorespawn", "AutoRespawn", 0);

        // These are UI-preview controls only. This project has no game/module backend;
        // they configure the Canvas HUD and deliberately do not claim game functionality.
        add(RENDER, "hud_notifications", "通知", 3, true);
        add(RENDER, "hud_module_list", "模块列表", 8, true);

        modulesByCategory.put(MISC, new ArrayList<>());

        add(WORLD, "noweather", "NoWeather", 0);
        add(WORLD, "nofall", "NoFall", 0);
        add(WORLD, "scaffold", "Scaffold", 1);
        add(WORLD, "fastbreak", "FastBreak", 0);
        add(WORLD, "nuker", "Nuker", 0);

        add(CLIENT, "clientmanager", "ClientManager", 0);
        add(CLIENT, "keybinds", "Keybinds", 0);
        add(CLIENT, "profiles", "Profiles", 0);
        add(CLIENT, "settings", "Settings", 0);
        add(CLIENT, "credits", "Credits", 0);
    }

    private void add(String category, String id, String name, int settingsCount) {
        add(category, id, name, settingsCount, false);
    }

    private void add(String category, String id, String name, int settingsCount,
                     boolean defaultEnabled) {
        ModuleDefinition definition = new ModuleDefinition(id, category, name, settingsCount,
                defaultEnabled);
        List<ModuleDefinition> categoryModules = modulesByCategory.get(category);
        if (categoryModules == null) {
            categoryModules = new ArrayList<>();
            modulesByCategory.put(category, categoryModules);
        }
        categoryModules.add(definition);
        modulesById.put(id, definition);
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<ModuleDefinition> getModules(String category) {
        List<ModuleDefinition> modules = modulesByCategory.get(category);
        return modules == null ? Collections.emptyList() : Collections.unmodifiableList(modules);
    }

    public ModuleDefinition getModule(String id) {
        return modulesById.get(id);
    }

    public List<ModuleDefinition> getAllModules() {
        return Collections.unmodifiableList(new ArrayList<>(modulesById.values()));
    }

    public static final class ModuleDefinition {
        public final String id;
        public final String category;
        public final String name;
        public final int settingsCount;
        /** True only for the two explicitly UI-preview HUD controls above. */
        public final boolean defaultEnabled;

        ModuleDefinition(String id, String category, String name, int settingsCount,
                         boolean defaultEnabled) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.settingsCount = settingsCount;
            this.defaultEnabled = defaultEnabled;
        }
    }
}
