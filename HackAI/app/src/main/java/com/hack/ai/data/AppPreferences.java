/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.data;

import android.content.Context;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Consumer;

public class AppPreferences {

    public static final String DEFAULT_MODE = "Average";

    // 等价于 Kotlin 的 `val Context.hack_aiDataStore by preferencesDataStore(name = "hack_ai_config")`：
    // 进程内单例，指向同一个 datastore/hack_ai_config.preferences_pb 文件
    private static volatile RxDataStore<Preferences> sHackAIDataStore;

    public static RxDataStore<Preferences> hack_aiDataStore(Context context) {
        if (sHackAIDataStore == null) {
            synchronized (AppPreferences.class) {
                if (sHackAIDataStore == null) {
                    sHackAIDataStore = new RxPreferenceDataStoreBuilder(
                            context.getApplicationContext(), "hack_ai_config").build();
                }
            }
        }
        return sHackAIDataStore;
    }

    private final RxDataStore<Preferences> dataStore;

    public AppPreferences(Context context) {
        this.dataStore = hack_aiDataStore(context);
    }

    public Flowable<UiState> getUiState() {
        return dataStore.data().map(prefs -> {
            Integer buttonX = prefs.get(Keys.BUTTON_X);
            Integer buttonY = prefs.get(Keys.BUTTON_Y);
            Boolean expanded = prefs.get(Keys.EXPANDED);
            return new UiState(
                    Category.fromId(prefs.get(Keys.CATEGORY)),
                    buttonX != null ? buttonX : 24,
                    buttonY != null ? buttonY : 220,
                    expanded != null ? expanded : false
            );
        });
    }

    public UiState uiStateOnce() {
        return getUiState().blockingFirst();
    }

    public void setCategory(Category category) {
        edit(it -> it.set(Keys.CATEGORY, category.getId()));
    }

    public void setButtonPosition(int x, int y) {
        edit(it -> {
            it.set(Keys.BUTTON_X, x);
            it.set(Keys.BUTTON_Y, y);
        });
    }

    public void setExpanded(boolean expanded) {
        edit(it -> it.set(Keys.EXPANDED, expanded));
    }

    public boolean isModuleEnabled(ModuleItem module) {
        Boolean value = dataOnce().get(toggleKey(module.getId()));
        return value != null ? value : module.getDefaultEnabled();
    }

    public void setModuleEnabled(String id, boolean enabled) {
        edit(it -> it.set(toggleKey(id), enabled));
    }

    public float sliderValue(ModuleItem module) {
        SliderSetting setting = module.getSlider();
        if (setting == null) return 0f;
        Float value = dataOnce().get(sliderKey(module.getId()));
        return value != null ? value : setting.getDefaultValue();
    }

    public void setSliderValue(String id, float value) {
        edit(it -> it.set(sliderKey(id), value));
    }

    public String modeValue(ModuleItem module) {
        String value = dataOnce().get(modeKey(module.getId()));
        return value != null ? value : DEFAULT_MODE;
    }

    public void setModeValue(String id, String mode) {
        edit(it -> it.set(modeKey(id), mode));
    }

    public boolean isShortcutEnabled(ModuleItem module) {
        Boolean value = dataOnce().get(shortcutEnabledKey(module.getId()));
        return value != null ? value : false;
    }

    public void setShortcutEnabled(String id, boolean enabled) {
        edit(it -> it.set(shortcutEnabledKey(id), enabled));
    }

    public int shortcutX(ModuleItem module) {
        Integer value = dataOnce().get(shortcutXKey(module.getId()));
        return value != null ? value : -1;
    }

    public int shortcutY(ModuleItem module) {
        Integer value = dataOnce().get(shortcutYKey(module.getId()));
        return value != null ? value : -1;
    }

    public void setShortcutPosition(String id, int x, int y) {
        edit(it -> {
            it.set(shortcutXKey(id), x);
            it.set(shortcutYKey(id), y);
        });
    }

    private static Preferences.Key<Boolean> toggleKey(String id) {
        return PreferencesKeys.booleanKey("module_" + id + "_enabled");
    }

    private static Preferences.Key<Float> sliderKey(String id) {
        return PreferencesKeys.floatKey("module_" + id + "_slider");
    }

    private static Preferences.Key<String> modeKey(String id) {
        return PreferencesKeys.stringKey("module_" + id + "_mode");
    }

    public String language() {
        String value = dataOnce().get(Keys.LANGUAGE);
        return value != null ? value : "CN";
    }

    public void setLanguage(String lang) {
        edit(it -> it.set(Keys.LANGUAGE, lang));
    }

    public String setting(String key) {
        return setting(key, "");
    }

    public String setting(String key, String defaultValue) {
        String value = dataOnce().get(PreferencesKeys.stringKey("setting_" + key));
        return value != null ? value : defaultValue;
    }

    public void setSetting(String key, String value) {
        edit(it -> it.set(PreferencesKeys.stringKey("setting_" + key), value));
    }

    private static Preferences.Key<Boolean> shortcutEnabledKey(String id) {
        return PreferencesKeys.booleanKey("module_" + id + "_shortcut");
    }

    private static Preferences.Key<Integer> shortcutXKey(String id) {
        return PreferencesKeys.intKey("module_" + id + "_shortcut_x");
    }

    private static Preferences.Key<Integer> shortcutYKey(String id) {
        return PreferencesKeys.intKey("module_" + id + "_shortcut_y");
    }

    /** 等价于 Kotlin 的 dataStore.data.first()（调用方均位于后台线程） */
    private Preferences dataOnce() {
        return dataStore.data().blockingFirst();
    }

    /** 等价于 Kotlin 的 dataStore.edit { }，阻塞至写入完成（调用方均位于后台线程） */
    private void edit(Consumer<MutablePreferences> block) {
        dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutable = prefs.toMutablePreferences();
            block.accept(mutable);
            return Single.<Preferences>just(mutable);
        }).blockingGet();
    }

    /** 读取子设置值，未存储时返回 defaultValue */
    public String subSetting(String moduleId, String subKey, String defaultValue) {
        return setting("module_" + moduleId + "_sub_" + subKey, defaultValue);
    }

    /** 持久化子设置值 */
    public void setSubSetting(String moduleId, String subKey, String value) {
        setSetting("module_" + moduleId + "_sub_" + subKey, value);
    }

    private static final class Keys {
        static final Preferences.Key<String> LANGUAGE = PreferencesKeys.stringKey("ui_language");
        static final Preferences.Key<String> CATEGORY = PreferencesKeys.stringKey("current_category");
        static final Preferences.Key<Integer> BUTTON_X = PreferencesKeys.intKey("floating_button_x");
        static final Preferences.Key<Integer> BUTTON_Y = PreferencesKeys.intKey("floating_button_y");
        static final Preferences.Key<Boolean> EXPANDED = PreferencesKeys.booleanKey("clickgui_expanded");
    }
}
