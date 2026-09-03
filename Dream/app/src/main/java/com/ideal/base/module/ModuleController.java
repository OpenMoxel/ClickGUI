package com.ideal.base.module;

import com.ideal.base.state.OverlayStateStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** One shared state source for list switches, detail switches, and shortcut buttons. */
public final class ModuleController {

    private final OverlayStateStore stateStore;
    private final Map<String, ModuleState> states = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<EnabledStateListener> enabledStateListeners =
            new CopyOnWriteArrayList<>();

    public ModuleController(ModuleRepository repository, OverlayStateStore stateStore) {
        this.stateStore = stateStore;
        for (ModuleRepository.ModuleDefinition module : repository.getAllModules()) {
            ModuleState state = new ModuleState();
            state.enabled = stateStore.getBoolean(module.id, "enabled", module.defaultEnabled);
            state.shortcutVisible = stateStore.getBoolean(module.id, "shortcut", false);
            state.range = stateStore.getFloat(module.id, "range", 3.5f);
            state.fov = stateStore.getFloat(module.id, "fov", 180f);
            state.delay = stateStore.getFloat(module.id, "delay", 100f);
            state.target = stateStore.getString(module.id, "target", "Head");
            state.scaffoldMode = stateStore.getString(module.id, "scaffold_mode", "Normal");
            states.put(module.id, state);
        }
    }

    public ModuleState stateOf(String moduleId) {
        return states.get(moduleId);
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    /**
     * Receives only real enable/disable transitions. This stays in the existing controller
     * rather than creating a parallel event bus, so every UI entry shares the same source.
     */
    public void addEnabledStateListener(EnabledStateListener listener) {
        enabledStateListeners.add(listener);
    }

    public void removeEnabledStateListener(EnabledStateListener listener) {
        enabledStateListeners.remove(listener);
    }

    public void toggleEnabled(String moduleId) {
        ModuleState state = stateOf(moduleId);
        if (state != null) {
            setEnabled(moduleId, !state.enabled);
        }
    }

    public void setEnabled(String moduleId, boolean enabled) {
        ModuleState state = stateOf(moduleId);
        if (state == null || state.enabled == enabled) {
            return;
        }
        state.enabled = enabled;
        stateStore.putBoolean(moduleId, "enabled", enabled);
        notifyChanged();
        notifyEnabledStateChanged(moduleId, enabled);
    }

    public void toggleShortcut(String moduleId) {
        ModuleState state = stateOf(moduleId);
        if (state != null) {
            setShortcutVisible(moduleId, !state.shortcutVisible);
        }
    }

    public void setShortcutVisible(String moduleId, boolean visible) {
        ModuleState state = stateOf(moduleId);
        if (state == null || state.shortcutVisible == visible) {
            return;
        }
        state.shortcutVisible = visible;
        stateStore.putBoolean(moduleId, "shortcut", visible);
        notifyChanged();
    }

    public void setRange(String moduleId, float value) {
        setFloat(moduleId, "range", round(clamp(value, 1f, 8f), 10f));
    }

    public void setFov(String moduleId, float value) {
        setFloat(moduleId, "fov", round(clamp(value, 30f, 180f), 10f));
    }

    public void setDelay(String moduleId, float value) {
        setFloat(moduleId, "delay", Math.round(clamp(value, 0f, 500f)));
    }

    public void setTarget(String moduleId, String target) {
        ModuleState state = stateOf(moduleId);
        if (state == null || state.target.equals(target)) {
            return;
        }
        state.target = target;
        stateStore.putString(moduleId, "target", target);
        notifyChanged();
    }

    public void setScaffoldMode(String moduleId, String mode) {
        ModuleState state = stateOf(moduleId);
        if (state == null || state.scaffoldMode.equals(mode)) {
            return;
        }
        state.scaffoldMode = mode;
        stateStore.putString(moduleId, "scaffold_mode", mode);
        notifyChanged();
    }

    private void setFloat(String moduleId, String field, float value) {
        ModuleState state = stateOf(moduleId);
        if (state == null) {
            return;
        }
        float oldValue;
        if ("range".equals(field)) {
            oldValue = state.range;
            state.range = value;
        } else if ("fov".equals(field)) {
            oldValue = state.fov;
            state.fov = value;
        } else {
            oldValue = state.delay;
            state.delay = value;
        }
        if (Math.abs(oldValue - value) < 0.001f) {
            return;
        }
        stateStore.putFloat(moduleId, field, value);
        notifyChanged();
    }

    private void notifyChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    private void notifyEnabledStateChanged(String moduleId, boolean enabled) {
        for (EnabledStateListener listener : enabledStateListeners) {
            listener.onEnabledStateChanged(moduleId, enabled);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float round(float value, float multiplier) {
        return Math.round(value * multiplier) / multiplier;
    }

    public static final class ModuleState {
        public boolean enabled;
        public boolean shortcutVisible;
        public float range;
        public float fov;
        public float delay;
        public String target;
        public String scaffoldMode;
    }

    public interface EnabledStateListener {
        void onEnabledStateChanged(String moduleId, boolean enabled);
    }
}
