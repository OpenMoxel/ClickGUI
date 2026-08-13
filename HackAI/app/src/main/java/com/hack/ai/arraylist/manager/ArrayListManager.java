/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.hack.ai.arraylist.model.ArrayListConfig;
import com.hack.ai.arraylist.model.Module;
import com.hack.ai.arraylist.overlay.ArrayListOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Arraylist HUD manager — singleton that owns the ArrayListOverlay,
 * wires it to the ArrayList module's enabled-state, and maintains the feature module list.
 */
public final class ArrayListManager {

    private static final ArrayListManager INSTANCE = new ArrayListManager();

    private ArrayListOverlay overlay;
    private Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Module.EnabledStateListener arraylistListener;
    private Module.EnabledStateListener moduleWakeListener;
    private boolean initialized;
    private final List<Module> featureModules = new ArrayList<>();

    private ArrayListManager() {}

    public static ArrayListManager getInstance() { return INSTANCE; }

    public synchronized void init(Context context) {
        if (initialized) return;
        appContext = context.getApplicationContext();

        Module arraylistModule = ArrayListConfig.INSTANCE.getModule();

        // Toggling the ArrayList module itself attaches/detaches the HUD.
        arraylistListener = (m, enabled) -> mainHandler.post(() -> {
            if (enabled) show(); else hide();
        });
        arraylistModule.addEnabledStateListener(arraylistListener);

        // Any feature module toggling wakes the HUD.
        moduleWakeListener = (m, enabled) -> mainHandler.post(() -> {
            if (arraylistModule.isEnabled()) {
                ArrayListOverlay o = getOverlay();
                if (o != null) o.wake();
            }
        });

        if (arraylistModule.isEnabled()) show();
        initialized = true;
    }

    private ArrayListOverlay getOverlay() {
        if (overlay == null && appContext != null) {
            overlay = new ArrayListOverlay(appContext);
        }
        return overlay;
    }

    public void show() {
        ArrayListOverlay o = getOverlay();
        if (o != null) o.show();
    }

    public void hide() {
        if (overlay != null) overlay.hide();
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(featureModules);
    }

    /**
     * Register a feature module. Called once per feature at init or first toggle.
     * If the module already exists, returns the existing one.
     */
    public Module registerFeature(String name) {
        return registerFeature(name, name);
    }

    /**
     * 适配：带独立显示名的注册（键稳定、显示名可本地化）。
     * If the module already exists, returns the existing one.
     */
    public Module registerFeature(String name, String displayName) {
        for (Module m : featureModules) {
            if (m.getName().equals(name)) return m;
        }
        Module module = new Module(name, displayName);
        module.addEnabledStateListener(moduleWakeListener);
        featureModules.add(module);
        // Rebuild overlay entries when a new feature is registered
        mainHandler.post(() -> {
            ArrayListOverlay o = overlay;
            if (o != null) o.rebuildEntries();
        });
        return module;
    }

    /** Called when a feature is toggled on. */
    public void addFeature(String name) {
        Module module = registerFeature(name);
        module.setEnabled(true);
    }

    /** 适配：带显示名版本 —— Called when a feature is toggled on. */
    public void addFeature(String name, String displayName) {
        Module module = registerFeature(name, displayName);
        module.setEnabled(true);
    }

    /** Called when a feature is toggled off. */
    public void removeFeature(String name) {
        Module module = registerFeature(name);
        module.setEnabled(false);
    }

    /** Update the display text shown next to a feature name in the HUD. */
    public void updateFeatureDisplay(String name, String displayText) {
        for (Module m : featureModules) {
            if (m.getName().equals(name)) {
                m.setDisplayText(displayText);
                ArrayListOverlay o = overlay;
                if (o != null) o.wake();
                return;
            }
        }
    }

    /** 适配：更新已注册条目的显示名（界面语言切换时调用），并重建 HUD 条目重测宽度。 */
    public void updateFeatureName(String name, String displayName) {
        for (Module m : featureModules) {
            if (m.getName().equals(name)) {
                m.setDisplayName(displayName);
                mainHandler.post(() -> {
                    ArrayListOverlay o = overlay;
                    if (o != null) o.rebuildEntries();
                });
                return;
            }
        }
    }

    public void setEnabled(boolean enabled) {
        ArrayListConfig.INSTANCE.getModule().setEnabled(enabled);
    }

    public boolean isEnabled() {
        return ArrayListConfig.INSTANCE.getModule().isEnabled();
    }
}
