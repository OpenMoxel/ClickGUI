/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.hack.ai.R;
import com.hack.ai.data.Category;
import com.hack.ai.data.LocaleHelper;
import com.hack.ai.data.ThemeManager;

import java.util.List;
import java.util.function.Consumer;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<Category> categories;
    private final Consumer<Category> onSelected;
    private Category selectedCategory;

    public CategoryAdapter(List<Category> categories, Category selected, Consumer<Category> onSelected) {
        this.categories = categories;
        this.selectedCategory = selected;
        this.onSelected = onSelected;
    }

    public Category getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(Category value) {
        int oldIndex = categories.indexOf(selectedCategory);
        selectedCategory = value;
        if (oldIndex >= 0) notifyItemChanged(oldIndex);
        notifyItemChanged(categories.indexOf(value));
    }

    @Override
    public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    @Override
    public void onBindViewHolder(CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        boolean sel = category == selectedCategory;
        holder.name.setText(LocaleHelper.categoryLabel(category));
        holder.icon.setImageResource(iconResFor(category));
        holder.name.setTextColor(sel ? ThemeManager.accentColor
                : holder.itemView.getContext().getColor(R.color.hack_ai_muted));
        holder.root.setBackgroundResource(sel ? R.drawable.bg_category_active : R.drawable.bg_category_idle);
        holder.root.setAlpha(sel ? 1f : 0.78f);
        // 玻璃质感：cubic-ease 曲线
        float t = com.hack.ai.data.ThemeManager.getGlassProgress();
        if (sel && holder.root.getBackground() != null) {
            float eased = (float) (1 - Math.pow(1 - t, 3));
            holder.root.getBackground().setAlpha((int) (ThemeManager.GLASS_ALPHA_MAX
                    + (ThemeManager.GLASS_ALPHA_MIN - ThemeManager.GLASS_ALPHA_MAX) * eased));
        }
        holder.root.setOnClickListener(v -> onSelected.accept(category));
    }

    /** 分类标题头图标（drawable 中的彩色 PNG，不做染色，选中态由背景/文字色/透明度区分） */
    private static int iconResFor(Category category) {
        switch (category) {
            case Combat:
                return R.drawable.combat;
            case Motion:
                return R.drawable.motion;
            case Visual:
                return R.drawable.visual;
            case Player:
                return R.drawable.player;
            case World:
                return R.drawable.world;
            case Misc:
            default:
                return R.drawable.misc;
        }
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        public final LinearLayout root;
        public final ImageView icon;
        public final TextView name;

        public CategoryViewHolder(View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.categoryRoot);
            icon = itemView.findViewById(R.id.categoryIcon);
            name = itemView.findViewById(R.id.categoryName);
        }
    }
}
