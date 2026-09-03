package com.pixelsight;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.pixelsight.gui.ClickGuiView;
import com.pixelsight.gui.SolsticeGuiView;
import com.pixelsight.gui.FloatingButtonView;
import com.pixelsight.hud.ArrayListView;
import com.pixelsight.hud.DynamicIslandView;
import com.pixelsight.hud.NotificationView;
import com.pixelsight.hud.ShortcutView;

public class MainActivity extends AppCompatActivity {
    
    private ClickGuiView clickGuiView;
    private SolsticeGuiView solsticeGuiView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        FrameLayout rootLayout = new FrameLayout(this);
        GradientDrawable gradientBackground = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] { Color.parseColor("#FFB6C1"), Color.parseColor("#A8E6CF") }
        );
        rootLayout.setBackground(gradientBackground);

        ArrayListView arrayListView = new ArrayListView(this);
        rootLayout.addView(arrayListView, new FrameLayout.LayoutParams(-1, -1));

        ShortcutView shortcutView = new ShortcutView(this);
        shortcutView.setElevation(35f);
        rootLayout.addView(shortcutView, new FrameLayout.LayoutParams(-1, -1));

        EditText hiddenInput = new EditText(this);
        hiddenInput.setAlpha(0f); hiddenInput.setSingleLine(true);
        rootLayout.addView(hiddenInput, new FrameLayout.LayoutParams(1, 1));

        clickGuiView = new ClickGuiView(this);
        clickGuiView.setElevation(20f); 
        rootLayout.addView(clickGuiView, new FrameLayout.LayoutParams(-1, -1));

        solsticeGuiView = new SolsticeGuiView(this);
        solsticeGuiView.setElevation(20f); 
        rootLayout.addView(solsticeGuiView, new FrameLayout.LayoutParams(-1, -1));

        DynamicIslandView islandView = new DynamicIslandView(this);
        islandView.setElevation(15f);
        rootLayout.addView(islandView, new FrameLayout.LayoutParams(-1, -1));

        NotificationView notifView = new NotificationView(this);
        notifView.setElevation(25f);
        rootLayout.addView(notifView, new FrameLayout.LayoutParams(-1, -1));

        FloatingButtonView floatingButton = new FloatingButtonView(this);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(160, 160);
        btnParams.gravity = Gravity.TOP | Gravity.START; 
        btnParams.leftMargin = 80; btnParams.topMargin = 80; 
        floatingButton.setElevation(30f); 
        rootLayout.addView(floatingButton, btnParams);

        ClickGuiView.onModuleToggled = () -> {
            arrayListView.invalidate(); notifView.invalidate(); islandView.invalidate(); shortcutView.invalidate();
        };

        ClickGuiView.onTriggerChanged = () -> {
            if (ClickGuiView.triggerMode != 0) { 
                floatingButton.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(300).withEndAction(() -> floatingButton.setVisibility(View.GONE)).start();
            } else {
                floatingButton.setVisibility(View.VISIBLE);
                floatingButton.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).start();
            }
        };

        ClickGuiView.onGuiModeSwitched = () -> {
            if (ClickGuiView.guiMode == 0) { 
                if (solsticeGuiView.isVisible) solsticeGuiView.closeGui(); 
                clickGuiView.openGui(0, 0); 
            } else { 
                if (clickGuiView.isVisible) clickGuiView.closeGui(); 
                solsticeGuiView.openGui(0, 0); 
            }
        };

        DynamicIslandView.onLongPressListener = () -> {
            if (ClickGuiView.triggerMode == 2) { 
                if (ClickGuiView.guiMode == 0) clickGuiView.toggle(islandView.getIslandCenterX(), islandView.getIslandCenterY());
                else solsticeGuiView.toggle(islandView.getIslandCenterX(), islandView.getIslandCenterY());
                hideKeyboard(hiddenInput);
            }
        };

        hiddenInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { clickGuiView.setSearchText(s.toString()); }
        });

        clickGuiView.setOnSearchRequestListener(() -> {
            hiddenInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(hiddenInput, InputMethodManager.SHOW_IMPLICIT);
        });

        // 核心修复：还原经典的简单点击与缩放动画
        floatingButton.setOnClickAction(() -> {
            floatingButton.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> {
                floatingButton.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                float cx = floatingButton.getX() + floatingButton.getWidth() / 2f;
                float cy = floatingButton.getY() + floatingButton.getHeight() / 2f;
                if (ClickGuiView.guiMode == 0) clickGuiView.toggle(cx, cy); else solsticeGuiView.toggle(cx, cy);
                hideKeyboard(hiddenInput);
            }).start();
        });

        setContentView(rootLayout);
    }

    private void hideKeyboard(EditText hiddenInput) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(hiddenInput.getWindowToken(), 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (ClickGuiView.triggerMode == 1 && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (ClickGuiView.guiMode == 0 && clickGuiView != null) clickGuiView.toggle(0, 0); 
            else if (ClickGuiView.guiMode == 1 && solsticeGuiView != null) solsticeGuiView.toggle(0, 0);
            return true; 
        }
        return super.onKeyDown(keyCode, event);
    }
}
