/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hack.ai.R;
import com.hack.ai.arraylist.manager.ArrayListManager;
import com.hack.ai.data.AppPreferences;
import com.hack.ai.data.Category;
import com.hack.ai.data.ModuleItem;
import com.hack.ai.data.ModuleRepository;
import com.hack.ai.data.SubSetting;
import com.hack.ai.data.ThemeManager;
import com.hack.ai.data.UiState;
import com.hack.ai.manager.IslandManager;
import com.hack.ai.manager.SoundManager;
import com.hack.ai.ui.CategoryAdapter;
import com.hack.ai.ui.ClickGuiPanel;
import com.hack.ai.ui.HackAIBrandView;
import com.hack.ai.ui.GlowButton;
import com.hack.ai.ui.ModuleAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FloatingService extends Service {
    // 等价于 Kotlin 的 CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)：
    // ioExecutor 承担 Dispatchers.IO 的读写，mainHandler 承担 Main 的 UI 回调
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private AppPreferences prefs;
    private GlowButton buttonView = null;
    private ClickGuiPanel panelView = null;
    private WindowManager.LayoutParams buttonParams = null;
    private WindowManager.LayoutParams panelParams = null;
    private CategoryAdapter categoryAdapter = null;
    private ModuleAdapter moduleAdapter = null;
    private final Map<String, Boolean> enabledStates = new LinkedHashMap<>();
    private final Map<String, Float> sliderStates = new LinkedHashMap<>();
    private final Map<String, String> modeStates = new LinkedHashMap<>();
    private final Map<String, Boolean> shortcutStates = new LinkedHashMap<>();
    private final Map<String, String> subSettingStates = new LinkedHashMap<>();
    private final Map<String, View> shortcutButtons = new LinkedHashMap<>();
    private final Map<String, WindowManager.LayoutParams> shortcutParams = new LinkedHashMap<>();
    // 灵动岛：窗口/时间/刷新率/电量逻辑已收敛至 IslandManager
    // ----
    /** 控制 ArrayList HUD 开关的功能模块 id（Visual 类目），该模块本身不进入 HUD 列表 */
    private static final String ARRAYLIST_MODULE_ID = "arraylist";
    private static final String THEME_MODULE_ID = "interface_theme";
    private Category currentCategory = Category.Combat;
    private String searchQuery = "";
    private boolean expanded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        startForeground(1001, notification());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        prefs = new AppPreferences(getApplicationContext());
        com.hack.ai.notification.NotificationManager.init(this);
        ArrayListManager.getInstance().init(this);
        IslandManager.getInstance().init(this);
        SoundManager.init(this);
        ThemeManager.init(prefs);
        ioExecutor.execute(() -> {
            restoreState();
            mainHandler.post(() -> {
                addFloatingButton();
                addWatermark();
                restoreShortcutButtons();
                restoreArrayListFeatures();
                if (expanded) showPanel(false);
            });
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (panelView != null) {
            try {
                windowManager.removeView(panelView);
            } catch (Throwable ignored) {
            }
        }
        if (buttonView != null) {
            try {
                windowManager.removeView(buttonView);
            } catch (Throwable ignored) {
            }
        }
        for (View view : shortcutButtons.values()) {
            try {
                windowManager.removeView(view);
            } catch (Throwable ignored) {
            }
        }
        shortcutButtons.clear();
        com.hack.ai.notification.NotificationManager.destroy();
        ArrayListManager.getInstance().hide();
        IslandManager.getInstance().release();
        if (watermarkView != null) {
            try {
                windowManager.removeView(watermarkView);
            } catch (Throwable ignored) {
            }
        }
        if (sidebarView != null) {
            try {
                windowManager.removeView(sidebarView);
            } catch (Throwable ignored) {
            }
        }
        ioExecutor.shutdownNow();
    }

    /** 在 ioExecutor 上调用（等价于 Kotlin 各 withContext(Dispatchers.IO) 读取） */
    private void restoreState() {
        UiState ui = prefs.uiStateOnce();
        currentCategory = ui.getCategory();
        expanded = ui.getExpanded();
        for (ModuleItem module : ModuleRepository.allModules()) {
            boolean enabled = THEME_MODULE_ID.equals(module.getId())
                    ? ThemeManager.isDark()
                    : prefs.isModuleEnabled(module);
            enabledStates.put(module.getId(), enabled);
            if (module.getSlider() != null) {
                sliderStates.put(module.getId(), prefs.sliderValue(module));
                modeStates.put(module.getId(), prefs.modeValue(module));
            }
            boolean shortcutOn = prefs.isShortcutEnabled(module);
            shortcutStates.put(module.getId(), shortcutOn);
            if (shortcutOn) {
                int sx = prefs.shortcutX(module);
                int sy = prefs.shortcutY(module);
                int displayWidth = getResources().getDisplayMetrics().widthPixels;
                int displayHeight = getResources().getDisplayMetrics().heightPixels;
                // 仅存储位置，实际尺寸在 createShortcutButton 中测量文本后计算
                WindowManager.LayoutParams sp = overlayParams(0, 0, false);
                sp.x = sx >= 0 ? sx : displayWidth / 2;
                sp.y = sy >= 0 ? sy : displayHeight / 2;
                shortcutParams.put(module.getId(), sp);
            }
        }
        // 恢复子设置状态
        for (ModuleItem module : ModuleRepository.allModules()) {
            if (module.getSubSettings() != null) {
                for (SubSetting sub : module.getSubSettings()) {
                    String compositeKey = module.getId() + "/" + sub.getKey();
                    String saved = prefs.subSetting(module.getId(), sub.getKey(), sub.defaultStringValue());
                    subSettingStates.put(compositeKey, saved);
                }
            }
        }
        WindowManager.LayoutParams bp = overlayParams(dp(40), dp(40), false);
        bp.x = ui.getButtonX();
        bp.y = ui.getButtonY();
        buttonParams = bp;
    }

    private void addFloatingButton() {
        GlowButton view = (GlowButton) LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null);
        buttonView = view;
        WindowManager.LayoutParams existing = buttonParams;
        final WindowManager.LayoutParams params = existing != null ? existing : overlayParams(dp(40), dp(40), false);
        buttonParams = params;
        final int screenW = getResources().getDisplayMetrics().widthPixels;
        final int screenH = getResources().getDisplayMetrics().heightPixels;
        final float threshold = (float) dp(8);
        view.setOnTouchListener(new View.OnTouchListener() {
            private int startX = 0;
            private int startY = 0;
            private float downRawX = 0f;
            private float downRawY = 0f;
            private boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        startX = params.x;
                        startY = params.y;
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        moved = false;
                        return true;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        float dx = event.getRawX() - downRawX;
                        float dy = event.getRawY() - downRawY;
                        if (Math.abs(dx) > threshold || Math.abs(dy) > threshold) moved = true;
                        if (moved) {
                            params.x = coerceIn(startX + (int) dx, 0, screenW - view.getWidth());
                            params.y = coerceIn(startY + (int) dy, 0, screenH - view.getHeight());
                            windowManager.updateViewLayout(view, params);
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP: {
                        if (!moved) togglePanel();
                        else ioExecutor.execute(() -> prefs.setButtonPosition(params.x, params.y));
                        return true;
                    }
                    default:
                        return false;
                }
            }
        });
        windowManager.addView(view, params);
    }

    private View dismissOverlay = null;
    private WindowManager.LayoutParams dismissParams = null;
    /** 关闭动画进行中标志，防止连点导致重复 playHide/重复写状态 */
    private boolean panelHiding = false;

    /** 主题切换：立即替换面板，无缝过渡 */
    private void recreatePanel() {
        if (panelView == null || panelHiding) return;
        // 移除旧遮罩
        if (dismissOverlay != null) {
            try { windowManager.removeView(dismissOverlay); } catch (Throwable ignored) {}
            dismissOverlay = null;
            dismissParams = null;
        }
        // 立即移除旧面板（跳过退场动画）
        try { windowManager.removeView(panelView); } catch (Throwable ignored) {}
        panelView = null;
        panelHiding = false;
        // 立即创建新面板（入场动画提供丝滑过渡）
        showPanel(true);
    }

    private View watermarkView = null;
    private View sidebarView = null;

    private void addWatermark() {
        com.hack.ai.ui.HackAIWatermark view = new com.hack.ai.ui.HackAIWatermark(this);
        watermarkView = view;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(8);
        params.y = statusBarH() + dp(4);
        windowManager.addView(view, params);
    }

    private void togglePanel() {
        if (expanded) hidePanel();
        else showPanel(true);
    }

    /** 返回主题感知的 Context：Light 用默认，Dark 强制 Night 模式以使用 values-night 色 */
    private Context panelContext() {
        if (!ThemeManager.isDark()) return this;
        Configuration cfg = new Configuration(getResources().getConfiguration());
        cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | Configuration.UI_MODE_NIGHT_YES;
        return createConfigurationContext(cfg);
    }

    private void showPanel(boolean animated) {
        if (panelView != null) return;
        Context ctx = panelContext();
        final ClickGuiPanel panel = (ClickGuiPanel) LayoutInflater.from(ctx).inflate(R.layout.layout_clickgui, null);
        panelView = panel;
        bindPanel(panel);
        final int displayWidth = getResources().getDisplayMetrics().widthPixels;
        final int displayHeight = getResources().getDisplayMetrics().heightPixels;
        final int panelWidth = panelWidthPx();
        final int panelHeight = panelHeightPx();
        WindowManager.LayoutParams params = overlayParams(panelWidth, panelHeight, true);
        params.x = Math.max(dp(8), (displayWidth - panelWidth) / 2);
        params.y = Math.max(dp(18), (displayHeight - panelHeight) / 2);
        panelParams = params;

        // 整面板空白区域拖动：点击交互控件时不参与拖动
        final WindowManager.LayoutParams p = params;
        final float threshold = (float) dp(12);
        panel.setOnTouchListener(new View.OnTouchListener() {
            private float dragStartRawX = 0f;
            private float dragStartRawY = 0f;
            private int dragStartX = 0;
            private int dragStartY = 0;
            private boolean dragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        float x = event.getX();
                        float y = event.getY();
                        // 触摸点在交互控件上 → 不参与拖动，让子 View 处理
                        if (isOverInteractiveChild((ViewGroup) v, x, y)) return false;
                        dragStartRawX = event.getRawX();
                        dragStartRawY = event.getRawY();
                        dragStartX = p.x;
                        dragStartY = p.y;
                        dragging = false;
                        return true;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        float dx = event.getRawX() - dragStartRawX;
                        float dy = event.getRawY() - dragStartRawY;
                        if (!dragging && (Math.abs(dx) > threshold || Math.abs(dy) > threshold)) dragging = true;
                        if (dragging) {
                            p.x = coerceIn(dragStartX + (int) dx, 0, displayWidth - panelWidth);
                            p.y = coerceIn(dragStartY + (int) dy, 0, displayHeight - panelHeight);
                            windowManager.updateViewLayout(panel, p);
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP: {
                        dragging = false;
                        return true;
                    }
                    default:
                        return false;
                }
            }
        });

        // 外部点击关闭遮罩
        View overlay = new View(this);
        overlay.setBackgroundColor(0x00000000);
        dismissOverlay = overlay;
        WindowManager.LayoutParams overlayLp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        dismissParams = overlayLp;
        overlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // 触摸点在悬浮按钮区域 → 走 togglePanel（关闭时与退出按钮同一 hidePanel 路径）
                    if (buttonView != null && buttonParams != null) {
                        int bx = buttonParams.x;
                        int by = buttonParams.y;
                        int bw = buttonView.getWidth();
                        int bh = buttonView.getHeight();
                        float rx = event.getRawX();
                        float ry = event.getRawY();
                        if (rx >= bx && rx < bx + bw && ry >= by && ry < by + bh) {
                            togglePanel();
                            return true;
                        }
                    }
                    hidePanel();
                    return true;
                }
                return false;
            }
        });
        windowManager.addView(overlay, overlayLp);
        windowManager.addView(panel, params);
        expanded = true;
        ioExecutor.execute(() -> prefs.setExpanded(true));
        buttonView.setActive(true);
        if (animated) panel.post(panel::playReveal);
    }

    private void hidePanel() {
        final ClickGuiPanel panel = panelView;
        if (panel == null || panelHiding) return;
        panelHiding = true;
        if (moduleAdapter != null) moduleAdapter.collapseDetails();
        // 移除外部点击遮罩
        if (dismissOverlay != null) {
            try {
                windowManager.removeView(dismissOverlay);
            } catch (Throwable ignored) {
            }
        }
        dismissOverlay = null;
        dismissParams = null;
        panel.playHide(() -> {
            try {
                windowManager.removeView(panel);
            } catch (Throwable ignored) {
            }
            panelView = null;
            panelHiding = false;
            expanded = false;
            ioExecutor.execute(() -> prefs.setExpanded(false));
            if (buttonView != null) buttonView.setActive(false);
        });
    }

    private void bindPanel(ClickGuiPanel panel) {
        RecyclerView categoryRecycler = panel.findViewById(R.id.categoryRecycler);
        final RecyclerView moduleRecycler = panel.findViewById(R.id.moduleRecycler);
        final EditText searchBox = panel.findViewById(R.id.searchBox);

        categoryAdapter = new CategoryAdapter(ModuleRepository.categories, currentCategory, category -> {
            if (currentCategory == category) return;
            currentCategory = category;
            if (categoryAdapter != null) categoryAdapter.setSelectedCategory(category);
            moduleRecycler.animate().alpha(0f).translationX(16f).setDuration(120).withEndAction(() -> {
                if (moduleAdapter != null) moduleAdapter.submitModules(filteredModules(category), true);
                moduleRecycler.setTranslationX(-16f);
                moduleRecycler.animate().alpha(1f).translationX(0f).setDuration(180).start();
            }).start();
            ioExecutor.execute(() -> prefs.setCategory(category));
        });
        categoryRecycler.setLayoutManager(new LinearLayoutManager(this));
        categoryRecycler.setAdapter(categoryAdapter);

        moduleAdapter = new ModuleAdapter(
                filteredModules(currentCategory),
                enabledStates,
                sliderStates,
                modeStates,
                shortcutStates,
                subSettingStates,
                (module, enabled) -> saveToggle(module, enabled),
                (module, value) -> saveSlider(module, value),
                (module, mode) -> saveMode(module, mode),
                (module, enabled) -> saveShortcutToggle(module, enabled),
                (module, subKey, newValue) -> saveSubSetting(module, subKey, newValue)
        );
        moduleRecycler.setLayoutManager(new LinearLayoutManager(this));
        moduleRecycler.setAdapter(moduleAdapter);

        searchBox.setText(searchQuery);
        searchBox.setSelection(searchBox.getText().length());
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString() : "";
                if (moduleAdapter != null) moduleAdapter.submitModules(filteredModules(currentCategory));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // CN/EN 语言切换
        final TextView cnBtn = panel.findViewById(R.id.langCN);
        final TextView enBtn = panel.findViewById(R.id.langEN);
        final Runnable refreshLangUI = () -> {
            boolean cn = "CN".equals(com.hack.ai.data.LocaleHelper.currentLanguage);
            cnBtn.setBackgroundResource(cn ? R.drawable.bg_lang_selected : android.R.color.transparent);
            cnBtn.setTextColor(cn ? getColor(R.color.hack_ai_on_selected) : getColor(R.color.hack_ai_muted));
            enBtn.setBackgroundResource(!cn ? R.drawable.bg_lang_selected : android.R.color.transparent);
            enBtn.setTextColor(!cn ? getColor(R.color.hack_ai_on_selected) : getColor(R.color.hack_ai_muted));
        };
        refreshLangUI.run();
        cnBtn.setOnClickListener(v -> {
            if (com.hack.ai.data.LocaleHelper.switchTo("CN")) {
                ioExecutor.execute(() -> prefs.setLanguage("CN"));
                refreshLangUI.run();
                refreshAllText();
            }
        });
        enBtn.setOnClickListener(v -> {
            if (com.hack.ai.data.LocaleHelper.switchTo("EN")) {
                ioExecutor.execute(() -> prefs.setLanguage("EN"));
                refreshLangUI.run();
                refreshAllText();
            }
        });
        // 加载保存的语言
        ioExecutor.execute(() -> {
            String lang = prefs.language();
            mainHandler.post(() -> {
                if (com.hack.ai.data.LocaleHelper.switchTo(lang)) {
                    refreshLangUI.run();
                    refreshAllText();
                }
            });
        });

        panel.<TextView>findViewById(R.id.actionClose).setOnClickListener(v -> hidePanel());
    }

    private void saveToggle(ModuleItem module, boolean enabled) {
        saveToggle(module, enabled, true);
    }

    private void saveToggle(ModuleItem module, boolean enabled, boolean playSound) {
        ioExecutor.execute(() -> prefs.setModuleEnabled(module.getId(), enabled));
        updateShortcutAppearance(module);
        if (playSound) {
            if (enabled) SoundManager.getInstance().playEnable();
            else SoundManager.getInstance().playDisable();
        }
        if (THEME_MODULE_ID.equals(module.getId())) {
            ThemeManager.setDark(enabled, prefs);
            recreatePanel();
        } else if (ARRAYLIST_MODULE_ID.equals(module.getId())) {
            // 该模块控制 HUD 总开关：挂载/卸载整个 ArrayList HUD，自身不进条目列表
            ArrayListManager.getInstance().setEnabled(enabled);
        } else if (enabled) {
            // ArrayList HUD 联动：开启滑入、关闭滑出
            ArrayListManager.getInstance().addFeature(module.getId(),
                    com.hack.ai.data.LocaleHelper.moduleName(module));
            if (module.getSubSettings() != null && !module.getSubSettings().isEmpty()) {
                ArrayListManager.getInstance().updateFeatureDisplay(module.getId(),
                        buildSubSettingSummary(module));
            } else if (module.getSlider() != null) {
                Float sliderState = sliderStates.get(module.getId());
                float sliderValue = sliderState != null ? sliderState : module.getSlider().getDefaultValue();
                ArrayListManager.getInstance().updateFeatureDisplay(module.getId(),
                        module.formattedValue(sliderValue));
            }
        } else {
            ArrayListManager.getInstance().removeFeature(module.getId());
        }
        com.hack.ai.notification.NotificationManager.show(
                com.hack.ai.data.LocaleHelper.moduleName(module),
                com.hack.ai.data.LocaleHelper.stateText(enabled),
                enabled ? com.hack.ai.notification.NotificationType.SUCCESS
                        : com.hack.ai.notification.NotificationType.WARNING
        );
    }

    private void saveSlider(ModuleItem module, float value) {
        ioExecutor.execute(() -> prefs.setSliderValue(module.getId(), value));
        // ArrayList HUD 联动：更新条目旁的数值文本
        ArrayListManager.getInstance().updateFeatureDisplay(module.getId(),
                module.formattedValue(value));
    }

    private void saveMode(ModuleItem module, String mode) {
        ioExecutor.execute(() -> prefs.setModeValue(module.getId(), mode));
    }

    private void saveSubSetting(ModuleItem module, String subKey, String newValue) {
        String compositeKey = module.getId() + "/" + subKey;
        subSettingStates.put(compositeKey, newValue);
        ioExecutor.execute(() -> prefs.setSubSetting(module.getId(), subKey, newValue));
        // 仅在 ArrayList 启用时更新 HUD 显示
        if (module.getSubSettings() != null
                && Boolean.TRUE.equals(enabledStates.get(ARRAYLIST_MODULE_ID))) {
            ArrayListManager.getInstance().updateFeatureDisplay(module.getId(),
                    buildSubSettingSummary(module));
        }
    }

    /** 构建子设置摘要字符串，如 "CPS:10 玩家 距离 普通" */
    private String buildSubSettingSummary(ModuleItem module) {
        if (module.getSubSettings() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (SubSetting sub : module.getSubSettings()) {
            String key = module.getId() + "/" + sub.getKey();
            String val = subSettingStates.get(key);
            if (val == null) val = sub.defaultStringValue();

            switch (sub.getType()) {
                case SLIDER:
                    try {
                        float f = Float.parseFloat(val);
                        String num = sub.getStep() >= 1f
                                ? String.valueOf((int) f)
                                : String.format(java.util.Locale.US, "%.1f", f);
                        String suffix = sub.getSuffix() != null ? sub.getSuffix() : "";
                        sb.append(sub.displayLabel()).append(":")
                                .append(num).append(suffix);
                    } catch (NumberFormatException e) {
                        sb.append(sub.displayLabel()).append(":").append(val);
                    }
                    break;
                case COMBO:
                    sb.append(val);
                    break;
                case MULTI:
                    if (!val.isEmpty()) {
                        sb.append(val.replace(",", " "));
                    }
                    break;
                case TOGGLE:
                default:
                    break;
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    // ---- 快捷按钮管理 ----

    private void restoreShortcutButtons() {
        for (ModuleItem module : ModuleRepository.allModules()) {
            if (Boolean.TRUE.equals(shortcutStates.get(module.getId()))) {
                createShortcutButton(module);
            }
        }
    }

    /** 服务启动时恢复 HUD 总开关状态，并把已开启的模块恢复到 ArrayList HUD（主线程调用） */
    private void restoreArrayListFeatures() {
        // 先按持久化状态恢复 HUD 总开关；setEnabled 仅在变化时触发监听，
        // 再补一次幂等的 show/hide 覆盖"状态无变化"的场景（如进程存活的服务重启）
        boolean hudOn = Boolean.TRUE.equals(enabledStates.get(ARRAYLIST_MODULE_ID));
        ArrayListManager.getInstance().setEnabled(hudOn);
        if (hudOn) ArrayListManager.getInstance().show();
        else ArrayListManager.getInstance().hide();

        for (ModuleItem module : ModuleRepository.allModules()) {
            if (ARRAYLIST_MODULE_ID.equals(module.getId())
                    || THEME_MODULE_ID.equals(module.getId())) continue;
            if (Boolean.TRUE.equals(enabledStates.get(module.getId()))) {
                ArrayListManager.getInstance().addFeature(module.getId(),
                        com.hack.ai.data.LocaleHelper.moduleName(module));
                if (module.getSubSettings() != null && !module.getSubSettings().isEmpty()) {
                    ArrayListManager.getInstance().updateFeatureDisplay(module.getId(),
                            buildSubSettingSummary(module));
                } else if (module.getSlider() != null) {
                    Float sliderState = sliderStates.get(module.getId());
                    float sliderValue = sliderState != null ? sliderState : module.getSlider().getDefaultValue();
                    ArrayListManager.getInstance().updateFeatureDisplay(module.getId(),
                            module.formattedValue(sliderValue));
                }
            }
        }
    }

    private void saveShortcutToggle(ModuleItem module, boolean enabled) {
        ioExecutor.execute(() -> prefs.setShortcutEnabled(module.getId(), enabled));
        if (enabled) {
            createShortcutButton(module);
        } else {
            removeShortcutButton(module);
        }
    }

    private void createShortcutButton(ModuleItem module) {
        if (shortcutButtons.containsKey(module.getId())) return;
        final LinearLayout view = (LinearLayout) LayoutInflater.from(this)
                .inflate(R.layout.layout_shortcut_button, null);
        TextView label = view.findViewById(R.id.shortcutLabel);
        label.setText(com.hack.ai.data.LocaleHelper.moduleName(module));
        shortcutButtons.put(module.getId(), view);

        // 测量文本实际宽度，自适应按钮大小
        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        int measuredW = Math.max(view.getMeasuredWidth(), dp(60));
        int measuredH = view.getMeasuredHeight();

        WindowManager.LayoutParams existing = shortcutParams.get(module.getId());
        final WindowManager.LayoutParams params;
        if (existing != null) {
            params = existing;
            params.width = measuredW;
            params.height = measuredH;
        } else {
            params = overlayParams(measuredW, measuredH, false);
            int displayWidth = getResources().getDisplayMetrics().widthPixels;
            int displayHeight = getResources().getDisplayMetrics().heightPixels;
            params.x = (displayWidth - measuredW) / 2;
            params.y = displayHeight / 2;
        }
        shortcutParams.put(module.getId(), params);

        applyShortcutStyle(view, module);
        installShortcutDragTouch(view, params, module);
        view.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            Boolean state = enabledStates.get(module.getId());
            boolean current = state != null ? state : module.getDefaultEnabled();
            boolean toggled = !current;
            enabledStates.put(module.getId(), toggled);
            saveToggle(module, toggled, false);
            applyShortcutStyle(view, module);
            if (moduleAdapter != null) {
                int pos = moduleAdapter.findModulePosition(module.getId());
                if (pos >= 0) moduleAdapter.notifyItemChanged(pos);
            }
        });

        windowManager.addView(view, params);
    }

    private void applyShortcutStyle(View view, ModuleItem module) {
        Boolean state = enabledStates.get(module.getId());
        boolean enabled = state != null ? state : module.getDefaultEnabled();
        TextView label = view.findViewById(R.id.shortcutLabel);
        view.setBackgroundResource(enabled ? R.drawable.bg_shortcut_enabled : R.drawable.bg_shortcut_disabled);
        label.setTextColor(enabled ? 0xFFFFFFFF : (int) 0xB3FFFFFFL);
    }

    private void updateShortcutAppearance(ModuleItem module) {
        View view = shortcutButtons.get(module.getId());
        if (view == null) return;
        applyShortcutStyle(view, module);
    }

    private void removeShortcutButton(ModuleItem module) {
        View view = shortcutButtons.remove(module.getId());
        if (view == null) return;
        shortcutParams.remove(module.getId());
        try {
            windowManager.removeView(view);
        } catch (Throwable ignored) {
        }
    }

    private void installShortcutDragTouch(View view, WindowManager.LayoutParams params, ModuleItem module) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int startX = 0;
            private int startY = 0;
            private float downRawX = 0f;
            private float downRawY = 0f;
            private boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        startX = params.x;
                        startY = params.y;
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        moved = false;
                        return true;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        float dx = event.getRawX() - downRawX;
                        float dy = event.getRawY() - downRawY;
                        if (Math.abs(dx) > 6 || Math.abs(dy) > 6) moved = true;
                        // 双轴钳制在屏幕范围内，防止按钮被拖出屏幕无法找回
                        int screenW = getResources().getDisplayMetrics().widthPixels;
                        int screenH = getResources().getDisplayMetrics().heightPixels;
                        params.x = coerceIn(startX + (int) dx, 0, Math.max(0, screenW - view.getWidth()));
                        params.y = coerceIn(startY + (int) dy, 0, Math.max(0, screenH - view.getHeight()));
                        windowManager.updateViewLayout(view, params);
                        return true;
                    }
                    case MotionEvent.ACTION_UP: {
                        if (!moved) {
                            view.performClick();
                        } else {
                            ioExecutor.execute(() ->
                                    prefs.setShortcutPosition(module.getId(), params.x, params.y));
                        }
                        return true;
                    }
                    default:
                        return false;
                }
            }
        });
    }

    // ---- 灵动岛：见 com.hack.ai.manager.IslandManager ----

    private int statusBarH() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    // ----

    /** 语言切换后刷新所有 UI 文本 */
    private void refreshAllText() {
        if (categoryAdapter != null) categoryAdapter.notifyDataSetChanged();
        if (moduleAdapter != null) moduleAdapter.submitModules(filteredModules(currentCategory), false);
        // ArrayList HUD：刷新已注册条目的显示名
        for (ModuleItem module : ModuleRepository.allModules()) {
            ArrayListManager.getInstance().updateFeatureName(module.getId(),
                    com.hack.ai.data.LocaleHelper.moduleName(module));
        }
        if (panelView != null) {
            TextView homeLabel = panelView.findViewById(R.id.homeLabel);
            if (homeLabel != null) homeLabel.setText(com.hack.ai.data.LocaleHelper.get("home"));
            EditText searchBox = panelView.findViewById(R.id.searchBox);
            if (searchBox != null) searchBox.setHint(com.hack.ai.data.LocaleHelper.get("search"));
        }
    }

    private List<ModuleItem> filteredModules(Category category) {
        String query = searchQuery.trim();
        List<ModuleItem> modules = ModuleRepository.modulesFor(category);
        if (query.isEmpty()) return modules;
        List<ModuleItem> filtered = new ArrayList<>();
        for (ModuleItem module : modules) {
            String keyBind = module.getKeyBind() != null ? module.getKeyBind() : "";
            if (containsIgnoreCase(module.getName(), query)
                    || containsIgnoreCase(module.getDescription(), query)
                    || containsIgnoreCase(module.getId(), query)
                    || containsIgnoreCase(keyBind, query)) {
                filtered.add(module);
            }
        }
        return filtered;
    }

    /** 等价于 Kotlin 的 String.contains(other, ignoreCase = true) */
    private static boolean containsIgnoreCase(String haystack, String needle) {
        if (needle.isEmpty()) return true;
        int max = haystack.length() - needle.length();
        for (int i = 0; i <= max; i++) {
            if (haystack.regionMatches(true, i, needle, 0, needle.length())) return true;
        }
        return false;
    }

    /**
     * 递归检查触摸点是否落在交互控件区域内。
     * 交互控件包括：Button、Switch、SeekBar、EditText、RecyclerView、ScrollView
     * 以及任何注册了 onClickListener 的 View（TextButton 等）。
     * 这些控件不参与面板拖动，保持原有点击和滑动行为。
     */
    private boolean isOverInteractiveChild(ViewGroup root, float x, float y) {
        for (int i = root.getChildCount() - 1; i >= 0; i--) {
            View child = root.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            float childX = x - child.getLeft() + child.getScrollX();
            float childY = y - child.getTop() + child.getScrollY();
            if (childX >= 0 && childX < child.getWidth() && childY >= 0 && childY < child.getHeight()) {
                if (child instanceof EditText
                        || child instanceof RecyclerView
                        || child instanceof AbsListView
                        || child instanceof ScrollView
                        || child instanceof Button
                        || child instanceof Switch
                        || child instanceof SeekBar
                        || child.hasOnClickListeners()) {
                    return true;
                }
                if (child instanceof ViewGroup) {
                    if (isOverInteractiveChild((ViewGroup) child, childX, childY)) return true;
                }
            }
        }
        return false;
    }

    private static int coerceIn(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private WindowManager.LayoutParams overlayParams(int width, int height, boolean focusable) {
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | (focusable ? 0 : WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width,
                height,
                type,
                flags,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        return params;
    }

    private Notification notification() {
        String channelId = "hack_ai_overlay";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "HackAI Overlay",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_stat_hack_ai)
                .setContentTitle("HackAI")
                .setContentText("ClickGUI 悬浮窗运行中")
                .setOngoing(true)
                .build();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private int panelWidthPx() {
        int screen = getResources().getDisplayMetrics().widthPixels;
        int maxWidth = Math.min(screen - dp(24), dp(820));
        int minWidth = Math.min(dp(360), maxWidth);
        return coerceIn((int) (screen * 0.8f), minWidth, maxWidth);
    }

    private int panelHeightPx() {
        int screen = getResources().getDisplayMetrics().heightPixels;
        int maxHeight = Math.min(screen - dp(24), dp(500));
        int minHeight = Math.min(dp(420), maxHeight);
        return coerceIn((int) (screen * 0.82f), minHeight, maxHeight);
    }
}
