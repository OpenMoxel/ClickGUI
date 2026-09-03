package com.ideal.base;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ideal.base.overlay.OverlayService;

/** Minimal, user-visible permission and service control surface. */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATIONS = 41;
    private TextView status;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.overlay_status);
        startButton = findViewById(R.id.start_overlay);
        startButton.setOnClickListener(v -> requestOverlayThenStart());
        findViewById(R.id.stop_overlay).setOnClickListener(v ->
                stopService(new Intent(this, OverlayService.class)));
        findViewById(R.id.open_overlay_settings).setOnClickListener(v -> openOverlaySettings());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }

    private void requestOverlayThenStart() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
        startForegroundService(new Intent(this, OverlayService.class)
                .setAction(OverlayService.ACTION_START));
        refreshState();
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void refreshState() {
        boolean granted = Settings.canDrawOverlays(this);
        status.setText(granted ? R.string.overlay_permission_granted : R.string.overlay_permission_needed);
        startButton.setEnabled(granted);
    }
}
