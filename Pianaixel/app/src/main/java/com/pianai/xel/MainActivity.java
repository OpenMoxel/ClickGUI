/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */

package com.pianai.xel;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

/** Hosts the canvas-only ClickGUI demonstration in an immersive landscape window. */
public final class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 7101;

    private ClickGuiView clickGuiView;
    private boolean awaitingOverlayPermission;
    private boolean desktopOverlayStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        clickGuiView = new ClickGuiView(this);
        setContentView(clickGuiView);
        // The decor view exists only after content is installed. Some Android 12 builds throw
        // from Window.getInsetsController() before this point, so configure immersion here.
        configureImmersiveWindow();
        if (savedInstanceState != null) {
            clickGuiView.restoreState(savedInstanceState);
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!clickGuiView.handleBack()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        requestDesktopOverlayIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Some OEM Settings screens do not reliably dispatch onActivityResult. Check once when
        // control returns, while still leaving a denied request as the normal in-app fallback.
        if (awaitingOverlayPermission && Settings.canDrawOverlays(this)) {
            awaitingOverlayPermission = false;
            startDesktopOverlay();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            awaitingOverlayPermission = false;
            if (Settings.canDrawOverlays(this)) {
                startDesktopOverlay();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            configureImmersiveWindow();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (clickGuiView != null) {
            clickGuiView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    private void requestDesktopOverlayIfNeeded() {
        if (Settings.canDrawOverlays(this)) {
            startDesktopOverlay();
            return;
        }
        if (awaitingOverlayPermission) {
            return;
        }
        awaitingOverlayPermission = true;
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } catch (ActivityNotFoundException ignored) {
            // Keep the existing in-app transparent ClickGUI available if Settings is unavailable.
            awaitingOverlayPermission = false;
        }
    }

    private void startDesktopOverlay() {
        if (desktopOverlayStarted || !Settings.canDrawOverlays(this)) {
            return;
        }
        desktopOverlayStarted = true;
        startService(new Intent(this, DesktopOverlayService.class));
        // The service owns the visible UI from this point, so return to the actual desktop.
        moveTaskToBack(true);
    }

    private void configureImmersiveWindow() {
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setFormat(PixelFormat.TRANSLUCENT);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0f);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            // Query the already-created DecorView directly. Window.getInsetsController() can
            // dereference a missing decor view on affected Android 12 vendor implementations.
            View decorView = window.getDecorView();
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }
}
