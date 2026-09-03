package com.ideal.base.state;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistent storage for UI module state and normalized overlay coordinates. */
public final class OverlayStateStore {

    private static final String FILE_NAME = "ideal_overlay_state";
    private final SharedPreferences preferences;

    public OverlayStateStore(Context context) {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public boolean getBoolean(String moduleId, String field, boolean defaultValue) {
        return preferences.getBoolean("module." + moduleId + "." + field, defaultValue);
    }

    public void putBoolean(String moduleId, String field, boolean value) {
        preferences.edit().putBoolean("module." + moduleId + "." + field, value).apply();
    }

    public float getFloat(String moduleId, String field, float defaultValue) {
        return preferences.getFloat("module." + moduleId + "." + field, defaultValue);
    }

    public void putFloat(String moduleId, String field, float value) {
        preferences.edit().putFloat("module." + moduleId + "." + field, value).apply();
    }

    public String getString(String moduleId, String field, String defaultValue) {
        return preferences.getString("module." + moduleId + "." + field, defaultValue);
    }

    public void putString(String moduleId, String field, String value) {
        preferences.edit().putString("module." + moduleId + "." + field, value).apply();
    }

    /** HUD configuration shares this existing preference file but has a distinct key prefix. */
    public boolean getHudBoolean(String field, boolean defaultValue) {
        return preferences.getBoolean("hud." + field, defaultValue);
    }

    public void putHudBoolean(String field, boolean value) {
        preferences.edit().putBoolean("hud." + field, value).apply();
    }

    public float getHudFloat(String field, float defaultValue) {
        return preferences.getFloat("hud." + field, defaultValue);
    }

    public void putHudFloat(String field, float value) {
        preferences.edit().putFloat("hud." + field, value).apply();
    }

    public boolean hasPosition(String key) {
        return preferences.contains("position." + key + ".x")
                && preferences.contains("position." + key + ".y");
    }

    public Position getPosition(String key) {
        return new Position(
                preferences.getFloat("position." + key + ".x", 0.5f),
                preferences.getFloat("position." + key + ".y", 0.5f)
        );
    }

    public void putPosition(String key, float normalizedX, float normalizedY) {
        preferences.edit()
                .putFloat("position." + key + ".x", clamp(normalizedX))
                .putFloat("position." + key + ".y", clamp(normalizedY))
                .apply();
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static final class Position {
        public final float x;
        public final float y;

        public Position(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
