/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

public class ClickGuiPanel extends LinearLayout {

    public ClickGuiPanel(Context context) {
        this(context, null);
    }

    public ClickGuiPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setAlpha(0f);
        setScaleX(0f);
        setScaleY(0f);
        setVisibility(View.GONE);
    }

    // ==================== 入场/退场动画 ====================

    public void playReveal() {
        setPivotX(getWidth() / 2f);
        setPivotY(getHeight() / 2f);
        setVisibility(View.VISIBLE);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(this, ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(this, SCALE_X, 0f, 1f),
                ObjectAnimator.ofFloat(this, SCALE_Y, 0f, 1f)
        );
        set.setDuration(350);
        set.setInterpolator(new OvershootInterpolator(1.1f));
        set.start();
    }

    public void playHide(Runnable onEnd) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(this, ALPHA, 1f, 0f),
                ObjectAnimator.ofFloat(this, SCALE_X, 1f, 0f),
                ObjectAnimator.ofFloat(this, SCALE_Y, 1f, 0f)
        );
        set.setDuration(250);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                setVisibility(View.GONE);
                onEnd.run();
            }
        });
        set.start();
    }
}
