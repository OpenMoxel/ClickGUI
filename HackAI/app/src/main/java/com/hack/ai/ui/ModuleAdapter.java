/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.hack.ai.R;
import com.hack.ai.data.LocaleHelper;
import com.hack.ai.data.ModuleItem;
import com.hack.ai.data.SliderSetting;
import com.hack.ai.data.SubSetting;
import com.hack.ai.data.ThemeManager;
import com.hack.ai.manager.SoundManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class ModuleAdapter extends RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder> {

    private static final String TAG = "HackAI";
    private static final String DEFAULT_MODE = "Average";

    private List<ModuleItem> modules;
    private final Map<String, Boolean> enabledStates;
    private final Map<String, Float> sliderStates;
    private final Map<String, String> modeStates;
    private final Map<String, Boolean> shortcutStates;
    private final Map<String, String> subSettingStates;
    private final BiConsumer<ModuleItem, Boolean> onToggle;
    private final BiConsumer<ModuleItem, Float> onSlider;
    private final BiConsumer<ModuleItem, String> onMode;
    private final BiConsumer<ModuleItem, Boolean> onShortcutToggle;
    private final OnSubSettingChange onSubSettingChange;

    private final Set<String> expandedModuleIds = new LinkedHashSet<>();

    public interface OnSubSettingChange {
        void onChange(ModuleItem module, String subKey, String newValue);
    }

    public ModuleAdapter(List<ModuleItem> modules,
                         Map<String, Boolean> enabledStates,
                         Map<String, Float> sliderStates,
                         Map<String, String> modeStates,
                         Map<String, Boolean> shortcutStates,
                         Map<String, String> subSettingStates,
                         BiConsumer<ModuleItem, Boolean> onToggle,
                         BiConsumer<ModuleItem, Float> onSlider,
                         BiConsumer<ModuleItem, String> onMode,
                         BiConsumer<ModuleItem, Boolean> onShortcutToggle,
                         OnSubSettingChange onSubSettingChange) {
        this.modules = modules;
        this.enabledStates = enabledStates;
        this.sliderStates = sliderStates;
        this.modeStates = modeStates;
        this.shortcutStates = shortcutStates;
        this.subSettingStates = subSettingStates;
        this.onToggle = onToggle;
        this.onSlider = onSlider;
        this.onMode = onMode;
        this.onShortcutToggle = onShortcutToggle;
        this.onSubSettingChange = onSubSettingChange;
    }

    public void submitModules(List<ModuleItem> newModules) {
        submitModules(newModules, false);
    }

    public void submitModules(List<ModuleItem> newModules, boolean collapseDetails) {
        modules = newModules;
        if (collapseDetails) expandedModuleIds.clear();
        notifyDataSetChanged();
    }

    public void collapseDetails() {
        if (expandedModuleIds.isEmpty()) return;
        expandedModuleIds.clear();
        notifyDataSetChanged();
    }

    /** 查找模块在當前列表中的位置，未找到返回 -1 */
    public int findModulePosition(String moduleId) {
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).getId().equals(moduleId)) return i;
        }
        return -1;
    }

    @Override
    public ModuleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_module, parent, false);
        return new ModuleViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    @Override
    public void onBindViewHolder(ModuleViewHolder holder, int position) {
        ModuleItem module = modules.get(position);
        Boolean enabledState = enabledStates.get(module.getId());
        boolean enabled = enabledState != null ? enabledState : module.getDefaultEnabled();
        boolean isExpanded = expandedModuleIds.contains(module.getId());
        boolean hasSubSettings = module.getSubSettings() != null && !module.getSubSettings().isEmpty();
        boolean hasLegacySlider = module.getSlider() != null;

        holder.name.setText(LocaleHelper.moduleName(module));
        holder.name.setTextColor(com.hack.ai.data.ThemeManager.accentColor);
        holder.desc.setText(LocaleHelper.moduleDesc(module));
        holder.bind.setText(module.getKeyBind() != null ? module.getKeyBind() : "");
        holder.bind.setVisibility(module.getKeyBind() == null || module.getKeyBind().trim().isEmpty()
                ? View.GONE : View.VISIBLE);
        holder.switchView.onCheckedChange = null;
        holder.switchView.setChecked(enabled, false);
        holder.switchView.onCheckedChange = checked -> {
            enabledStates.put(module.getId(), checked);
            onToggle.accept(module, checked);
        };

        // 子设置渲染（优先于旧模式）
        if (hasSubSettings) {
            // 隐藏旧式 slider / value / modeRow
            holder.value.setVisibility(View.GONE);
            holder.slider.setVisibility(View.GONE);
            holder.modeRow.setVisibility(View.GONE);
            holder.modeGroup.setOnCheckedChangeListener(null);
            // 渲染子设置
            populateSubSettings(holder.subSettingsContainer, module, isExpanded);
            // 通知 RecyclerView 该 item 需要重测高度
            if (isExpanded) {
                holder.itemView.post(() -> holder.itemView.requestLayout());
            }
        } else if (hasLegacySlider) {
            // 旧式 Slider + Mode：仅带 Slider 的模块展开时显示
            holder.subSettingsContainer.setVisibility(View.GONE);
            holder.subSettingsContainer.removeAllViews();
            Float sliderState = sliderStates.get(module.getId());
            float sliderValue = sliderState != null ? sliderState
                    : module.getSlider().getDefaultValue();
            holder.value.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.value.setText(module.formattedValue(sliderValue));
            holder.slider.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.modeRow.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.slider.onValueChange = null;
            holder.slider.configure(module.getSlider(), sliderValue, false);
            holder.slider.onValueChange = value -> {
                sliderStates.put(module.getId(), value);
                holder.value.setText(module.formattedValue(value));
                onSlider.accept(module, value);
            };
            holder.modeGroup.setOnCheckedChangeListener(null);
            String modeState = modeStates.get(module.getId());
            holder.modeGroup.check(modeIdFor(modeState != null ? modeState : DEFAULT_MODE));
            holder.modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
                RadioButton button = group.findViewById(checkedId);
                String selected = button != null ? button.getText().toString() : DEFAULT_MODE;
                modeStates.put(module.getId(), selected);
                onMode.accept(module, selected);
                Log.d(TAG, "Mode selected for " + module.getId() + ": " + selected);
            });
        } else {
            // 无子设置也无旧 slider
            holder.value.setVisibility(View.GONE);
            holder.slider.setVisibility(View.GONE);
            holder.modeRow.setVisibility(View.GONE);
            holder.modeGroup.setOnCheckedChangeListener(null);
            holder.subSettingsContainer.setVisibility(View.GONE);
            holder.subSettingsContainer.removeAllViews();
        }

        // 快捷按钮开关：所有模块展开时都显示
        holder.shortcutRow.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        Boolean shortcutState = shortcutStates.get(module.getId());
        boolean shortcutOn = shortcutState != null ? shortcutState : false;
        holder.shortcutSwitch.onCheckedChange = null;
        holder.shortcutSwitch.setChecked(shortcutOn, false);
        holder.shortcutSwitch.onCheckedChange = checked -> {
            shortcutStates.put(module.getId(), checked);
            onShortcutToggle.accept(module, checked);
        };

        holder.itemView.setOnClickListener(v -> {
            if (expandedModuleIds.remove(module.getId())) {
                int index = holder.getBindingAdapterPosition();
                if (index != RecyclerView.NO_POSITION) notifyItemChanged(index);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (expandedModuleIds.add(module.getId())) {
                int index = holder.getBindingAdapterPosition();
                if (index != RecyclerView.NO_POSITION) notifyItemChanged(index);
            }
            return true;
        });

        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(16f);
        holder.itemView.post(() -> {
            ObjectAnimator.ofFloat(holder.itemView, View.ALPHA, 0f, 1f).setDuration(180).start();
            ObjectAnimator.ofFloat(holder.itemView, View.TRANSLATION_Y, 16f, 0f).setDuration(180).start();
        });
    }

    // ---- 子设置动态渲染 ----

    private void populateSubSettings(LinearLayout container, ModuleItem module, boolean isExpanded) {
        container.removeAllViews();
        if (!isExpanded || module.getSubSettings() == null) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);
        Context ctx = container.getContext();
        for (SubSetting sub : module.getSubSettings()) {
            switch (sub.getType()) {
                case SLIDER:
                    addSliderRow(ctx, container, module, sub);
                    break;
                case COMBO:
                    addComboRow(ctx, container, module, sub);
                    break;
                case TOGGLE:
                    addToggleRow(ctx, container, module, sub);
                    break;
                case MULTI:
                    addMultiRow(ctx, container, module, sub);
                    break;
            }
        }
        // 强制重新布局，确保 RecyclerView 重测 item 高度
        container.requestLayout();
    }

    private String subState(ModuleItem module, SubSetting sub) {
        String key = module.getId() + "/" + sub.getKey();
        String val = subSettingStates.get(key);
        return val != null ? val : sub.defaultStringValue();
    }

    private int dp(float value, Context ctx) {
        return (int) (value * ctx.getResources().getDisplayMetrics().density);
    }

    // ---- SLIDER 行 ----
    private void addSliderRow(Context ctx, LinearLayout container, ModuleItem module, SubSetting sub) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dp(6, ctx), 0, 0);

        // 标签
        TextView label = new TextView(ctx);
        label.setText(sub.displayLabel());
        label.setTextColor(ctx.getResources().getColor(R.color.hack_ai_muted));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setPadding(0, 0, 0, dp(2, ctx));
        row.addView(label);

        // 数值文本
        TextView valueText = new TextView(ctx);
        valueText.setTextColor(ctx.getResources().getColor(R.color.hack_ai_text));
        valueText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        valueText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(18, ctx));
        row.addView(valueText);

        // 滑杆
        SettingSlider slider = new SettingSlider(ctx);
        slider.setMinimumHeight(dp(26, ctx));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(26, ctx));
        sp.topMargin = dp(-2, ctx);
        row.addView(slider);

        float currentVal;
        try {
            currentVal = Float.parseFloat(subState(module, sub));
        } catch (NumberFormatException e) {
            currentVal = sub.getDefaultFloat();
        }
        SliderSetting sliderSetting = sub.toSliderSetting();
        slider.configure(sliderSetting, currentVal, false);
        valueText.setText(formatSliderValue(currentVal, sub.getSuffix(), sub.getStep()));

        slider.onValueChange = value -> {
            String key = module.getId() + "/" + sub.getKey();
            subSettingStates.put(key, String.valueOf(value));
            valueText.setText(formatSliderValue(value, sub.getSuffix(), sub.getStep()));
            if (onSubSettingChange != null) {
                onSubSettingChange.onChange(module, sub.getKey(), String.valueOf(value));
            }
        };

        container.addView(row);
    }

    private String formatSliderValue(float value, String suffix, float step) {
        String num;
        if (step >= 1f) {
            num = String.valueOf((int) value);
        } else {
            num = String.format(Locale.US, "%.1f", value);
        }
        return num + (suffix != null ? suffix : "");
    }

    // ---- COMBO 行 ----
    private void addComboRow(Context ctx, LinearLayout container, ModuleItem module, SubSetting sub) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dp(6, ctx), 0, 0);

        // 标签
        TextView label = new TextView(ctx);
        label.setText(sub.displayLabel());
        label.setTextColor(ctx.getResources().getColor(R.color.hack_ai_muted));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setPadding(0, 0, 0, dp(4, ctx));
        row.addView(label);

        // RadioGroup
        RadioGroup group = new RadioGroup(ctx);
        group.setOrientation(LinearLayout.HORIZONTAL);
        group.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(28, ctx)));
        row.addView(group);

        String currentVal = subState(module, sub);
        List<String> options = sub.getOptions();
        for (int i = 0; i < options.size(); i++) {
            RadioButton btn = new RadioButton(ctx);
            btn.setId(View.generateViewId());
            btn.setText(sub.optionLabel(i));
            btn.setBackgroundResource(R.drawable.bg_mode_radio);
            btn.setButtonDrawable(null);
            btn.setGravity(Gravity.CENTER);
            btn.setTextColor(ctx.getResources().getColorStateList(R.color.mode_radio_text));
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            RadioGroup.LayoutParams bp = new RadioGroup.LayoutParams(
                    dp(68, ctx), dp(26, ctx));
            if (i > 0) bp.leftMargin = dp(6, ctx);
            btn.setLayoutParams(bp);
            btn.setMinWidth(0);
            btn.setMinimumWidth(0);
            btn.setPadding(dp(6, ctx), 0, dp(6, ctx), 0);

            if (options.get(i).equals(currentVal) || sub.optionLabel(i).equals(currentVal)) {
                btn.setChecked(true);
            }
            group.addView(btn);
        }

        group.setOnCheckedChangeListener((g, checkedId) -> {
            RadioButton selected = g.findViewById(checkedId);
            if (selected == null) return;
            String text = selected.getText().toString();
            // 找到对应的中文选项值
            String optionVal = null;
            for (int i = 0; i < options.size(); i++) {
                if (text.equals(sub.optionLabel(i))) {
                    optionVal = options.get(i);
                    break;
                }
            }
            if (optionVal == null) optionVal = text;
            String key = module.getId() + "/" + sub.getKey();
            subSettingStates.put(key, optionVal);
            SoundManager.getInstance().playCombo();
            if (onSubSettingChange != null) {
                onSubSettingChange.onChange(module, sub.getKey(), optionVal);
            }
        });

        container.addView(row);
    }

    // ---- TOGGLE 行 ----
    private void addToggleRow(Context ctx, LinearLayout container, ModuleItem module, SubSetting sub) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34, ctx)));
        row.setPadding(0, dp(4, ctx), 0, 0);

        // 标签
        TextView label = new TextView(ctx);
        label.setText(sub.displayLabel());
        label.setTextColor(ctx.getResources().getColor(R.color.hack_ai_muted));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        label.setLayoutParams(lp);
        row.addView(label);

        // 开关
        AnimatedSwitch toggle = new AnimatedSwitch(ctx);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(38, ctx), dp(22, ctx));
        toggle.setLayoutParams(tp);

        String currentVal = subState(module, sub);
        boolean checked = Boolean.parseBoolean(currentVal);
        toggle.onCheckedChange = null;
        toggle.setChecked(checked, false);
        toggle.onCheckedChange = isChecked -> {
            String key = module.getId() + "/" + sub.getKey();
            subSettingStates.put(key, String.valueOf(isChecked));
            SoundManager.getInstance().playToggle();
            if (onSubSettingChange != null) {
                onSubSettingChange.onChange(module, sub.getKey(), String.valueOf(isChecked));
            }
        };
        row.addView(toggle);

        container.addView(row);
    }

    // ---- MULTI 行（多选按钮） ----
    private void addMultiRow(Context ctx, LinearLayout container, ModuleItem module, SubSetting sub) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dp(6, ctx), 0, 0);

        // 标签
        TextView label = new TextView(ctx);
        label.setText(sub.displayLabel());
        label.setTextColor(ctx.getResources().getColor(R.color.hack_ai_muted));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setPadding(0, 0, 0, dp(4, ctx));
        row.addView(label);

        // 多选按钮组
        String currentVal = subState(module, sub);
        java.util.Set<String> selected = new java.util.LinkedHashSet<>();
        if (currentVal != null && !currentVal.isEmpty()) {
            for (String s : currentVal.split(",")) {
                if (!s.trim().isEmpty()) selected.add(s.trim());
            }
        }

        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(28, ctx)));

        List<String> options = sub.getOptions();
        for (int i = 0; i < options.size(); i++) {
            final String optVal = options.get(i);
            final RadioButton btn = new RadioButton(ctx);
            btn.setId(View.generateViewId());
            btn.setText(sub.optionLabel(i));
            btn.setBackgroundResource(R.drawable.bg_mode_radio);
            btn.setButtonDrawable(null);
            btn.setGravity(Gravity.CENTER);
            btn.setTextColor(ctx.getResources().getColorStateList(R.color.mode_radio_text));
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            boolean isSelected = selected.contains(optVal);
            btn.setChecked(isSelected);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    dp(72, ctx), dp(26, ctx));
            if (i > 0) bp.leftMargin = dp(6, ctx);
            btn.setLayoutParams(bp);
            btn.setMinWidth(0);
            btn.setMinimumWidth(0);
            btn.setPadding(dp(6, ctx), 0, dp(6, ctx), 0);

            final int index = i;
            btn.setOnClickListener(v -> {
                // 清除 RadioGroup 行为，允许取消选中
                if (selected.contains(optVal)) {
                    selected.remove(optVal);
                    btn.setChecked(false);
                } else {
                    selected.add(optVal);
                    btn.setChecked(true);
                }
                String newVal = String.join(",", selected);
                String key = module.getId() + "/" + sub.getKey();
                subSettingStates.put(key, newVal.isEmpty() ? "" : newVal);
                if (onSubSettingChange != null) {
                    onSubSettingChange.onChange(module, sub.getKey(), newVal);
                }
            });
            btnRow.addView(btn);
        }
        row.addView(btnRow);
        container.addView(row);
    }

    // ---- ViewHolder ----

    public static class ModuleViewHolder extends RecyclerView.ViewHolder {
        public final TextView name;
        public final TextView desc;
        public final TextView value;
        public final TextView bind;
        public final AnimatedSwitch switchView;
        public final SettingSlider slider;
        public final LinearLayout modeRow;
        public final RadioGroup modeGroup;
        public final LinearLayout shortcutRow;
        public final AnimatedSwitch shortcutSwitch;
        public final LinearLayout subSettingsContainer;

        public ModuleViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.moduleName);
            desc = itemView.findViewById(R.id.moduleDesc);
            value = itemView.findViewById(R.id.moduleValue);
            bind = itemView.findViewById(R.id.moduleBind);
            switchView = itemView.findViewById(R.id.moduleSwitch);
            slider = itemView.findViewById(R.id.moduleSlider);
            modeRow = itemView.findViewById(R.id.modeRow);
            modeGroup = itemView.findViewById(R.id.modeGroup);
            shortcutRow = itemView.findViewById(R.id.shortcutRow);
            shortcutSwitch = itemView.findViewById(R.id.shortcutSwitch);
            subSettingsContainer = itemView.findViewById(R.id.subSettingsContainer);
        }
    }

    private int modeIdFor(String mode) {
        switch (mode) {
            case "Second":
                return R.id.modeSecond;
            case "Random":
                return R.id.modeRandom;
            default:
                return R.id.modeAverage;
        }
    }
}
