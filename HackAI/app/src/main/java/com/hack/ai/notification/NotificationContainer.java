/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.notification;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public class NotificationContainer extends FrameLayout {

    private final float D = getResources().getDisplayMetrics().density;
    private final int itemH = (int) (44 * D);
    private final int gap = (int) (8 * D);
    private final int maxVisible = 5;
    private final long duration = 2500L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final PathInterpolator ease = new PathInterpolator(0.16f, 1f, 0.3f, 1f);

    private final List<Entry> entries = new ArrayList<>();

    public NotificationContainer(Context context) {
        super(context);
    }

    public void show(NotificationData data) {
        // 超过上限移除最旧
        while (entries.size() >= maxVisible) {
            forceRemove(entries.get(entries.size() - 1));
        }

        NotificationView view = new NotificationView(getContext());
        view.title = data.getTitle();
        view.message = data.getMessage();
        view.gradientStart = data.getType().getGradientStart();
        view.gradientEnd = data.getType().getGradientEnd();

        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, itemH);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        // 初始定位：底部开始
        lp.bottomMargin = 0;
        addView(view, lp);
        Entry entry = new Entry(data, view, lp);
        // 新通知插入队首（视觉最下方）
        entries.add(0, entry);

        // 所有条目重新定位（底部为锚点，向上堆叠）
        relayout(true);

        // 入场动画
        NotificationAnimator.playEnter(view, () -> {
            entry.timerRunnable = () -> dismiss(entry);
            handler.postDelayed(entry.timerRunnable, duration);
        });
    }

    private void dismiss(Entry entry) {
        if (entry.done) return;
        entry.done = true;
        if (entry.timerRunnable != null) handler.removeCallbacks(entry.timerRunnable);
        if (!entries.contains(entry)) return;

        NotificationAnimator.playExit(entry.view, () -> {
            removeView(entry.view);
            // 按对象移除：退出动画期间可能有新通知插入队首，索引已失效
            entries.remove(entry);
            relayout(true);
        });
    }

    private void forceRemove(Entry entry) {
        if (entry.timerRunnable != null) handler.removeCallbacks(entry.timerRunnable);
        entry.done = true;
        removeView(entry.view);
        entries.remove(entry);
    }

    /** 统一重排：底部锚点，向上堆叠，bottomMargin 递增 */
    private void relayout(boolean animate) {
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            int target = i * (itemH + gap);
            if (animate && e.getBottomMargin() != target) {
                animateMargin(e, target);
            } else {
                e.setBottomMargin(target);
                e.view.setLayoutParams(e.params);
            }
        }
    }

    /** ValueAnimator 平滑过渡 bottomMargin */
    private void animateMargin(Entry entry, int target) {
        int from = entry.getBottomMargin();
        ValueAnimator animator = ValueAnimator.ofInt(from, target);
        animator.setDuration(250);
        animator.setInterpolator(ease);
        animator.addUpdateListener(animation -> {
            entry.setBottomMargin((int) animation.getAnimatedValue());
            entry.view.setLayoutParams(entry.params);
        });
        animator.start();
    }

    public void destroy() {
        handler.removeCallbacksAndMessages(null);
        entries.clear();
        removeAllViews();
    }

    // ---- Entry ----

    private final class Entry {
        final NotificationData data;
        final NotificationView view;
        final LayoutParams params;
        Runnable timerRunnable = null;
        boolean done = false;

        Entry(NotificationData data, NotificationView view, LayoutParams params) {
            this.data = data;
            this.view = view;
            this.params = params;
        }

        int getBottomMargin() {
            return params.bottomMargin;
        }

        void setBottomMargin(int v) {
            params.bottomMargin = v;
        }
    }
}
