/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.manager;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import com.hack.ai.R;

/** 音效管理：功能开关 / 子设置 / 快捷键 */
public final class SoundManager {
    private static volatile SoundManager instance;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;

    private SoundManager(Context ctx) {
        appContext = ctx.getApplicationContext();
    }

    public static SoundManager getInstance() {
        return instance;
    }

    public static void init(Context ctx) {
        if (instance == null) {
            synchronized (SoundManager.class) {
                if (instance == null) {
                    instance = new SoundManager(ctx);
                }
            }
        }
    }

    public void playEnable() {
        play(R.raw.enable);
    }

    public void playDisable() {
        play(R.raw.disable);
    }

    public void playCombo() {
        play(R.raw.combo);
    }

    public void playToggle() {
        play(R.raw.toggle);
    }

    public void playClick() {
        play(R.raw.click);
    }

    private void play(int resId) {
        mainHandler.post(() -> {
            releasePlayer();
            try {
                player = MediaPlayer.create(appContext, resId);
                if (player != null) {
                    player.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build());
                    player.setOnCompletionListener(MediaPlayer::release);
                    player.start();
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }
}
