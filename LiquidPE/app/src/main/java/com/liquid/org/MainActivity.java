/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org;

import android.os.Bundle;
import android.net.Uri;
import android.content.Intent;
import android.provider.Settings;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.WindowCompat;

import com.liquid.org.ui.overlay.LiquidBounceOverlayView;
import com.liquid.org.service.FloatingService;

public class MainActivity extends AppCompatActivity {
    private LiquidBounceOverlayView preview;
    private EditText searchInput;
    private boolean overlayPermissionRequested;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        hideSystemUi();
        setContentView(R.layout.activity_main);
        preview = findViewById(R.id.liquid_bounce_preview);
        preview.setTransparentBase(true);
        searchInput = findViewById(R.id.search_input);
        preview.setSearchRequestListener(current -> {
            searchInput.setText(current);
            searchInput.setSelection(searchInput.length());
            searchInput.requestFocus();
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        });
        searchInput.addTextChangedListener(new SimpleTextWatcher(value -> preview.setSearchQuery(value)));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (preview != null && preview.isEditMode()) {
                    preview.exitEditMode();
                    return;
                }
                // 暂时禁用自身后交回系统默认返回行为，避免递归调用。
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });
    }

    private void hideSystemUi() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideSystemUi(); }

    @Override protected void onResume() {
        super.onResume();
        ensureFloatingButton();
    }

    /** 首次启动自动请求悬浮窗权限；从系统授权页返回后立即启动前台悬浮窗服务。 */
    private void ensureFloatingButton() {
        if (Settings.canDrawOverlays(this)) {
            FloatingService.start(this);
            return;
        }
        if (overlayPermissionRequested) return;
        overlayPermissionRequested = true;
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private static final class SimpleTextWatcher implements android.text.TextWatcher {
        interface Listener { void onChanged(String value); }
        private final Listener listener;
        SimpleTextWatcher(Listener listener) { this.listener = listener; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { listener.onChanged(s.toString()); }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }

}
