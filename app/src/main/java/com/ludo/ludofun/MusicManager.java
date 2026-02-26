package com.ludo.ludofun;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

public class MusicManager {
    private static MediaPlayer backgroundPlayer;
    private static int currentResId = -1;
    private static boolean isSoundEnabled = true;
    private static boolean settingsLoaded = false;
    private static final String PREF_NAME = "MusicPrefs";
    private static final String KEY_SOUND_ENABLED = "isSoundEnabled";

    private static void loadSettings(Context context) {
        if (settingsLoaded) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        isSoundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true);
        settingsLoaded = true;
    }

    private static void saveSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, isSoundEnabled).apply();
    }

    public static void playMusic(Context context, int resId) {
        loadSettings(context);
        currentResId = resId;
        if (!isSoundEnabled) return;

        if (backgroundPlayer != null && backgroundPlayer.isPlaying()) {
            return;
        }

        stopMusic();

        backgroundPlayer = MediaPlayer.create(context.getApplicationContext(), resId);
        if (backgroundPlayer != null) {
            backgroundPlayer.setLooping(true);
            backgroundPlayer.start();
        }
    }

    public static void pauseMusic() {
        if (backgroundPlayer != null && backgroundPlayer.isPlaying()) {
            backgroundPlayer.pause();
        }
    }

    public static void resumeMusic(Context context) {
        loadSettings(context);
        if (!isSoundEnabled) {
            stopMusic();
            return;
        }
        if (backgroundPlayer != null) {
            if (!backgroundPlayer.isPlaying()) {
                backgroundPlayer.start();
            }
        } else if (currentResId != -1) {
            playMusic(context, currentResId);
        }
    }

    // Overloaded method for backward compatibility if needed, but Context is preferred
    public static void resumeMusic() {
        if (!isSoundEnabled) return;
        if (backgroundPlayer != null && !backgroundPlayer.isPlaying()) {
            backgroundPlayer.start();
        }
    }

    public static void stopMusic() {
        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
            backgroundPlayer.release();
            backgroundPlayer = null;
        }
    }

    public static void toggleSound(Context context, int resId) {
        loadSettings(context);
        isSoundEnabled = !isSoundEnabled;
        saveSettings(context);
        if (isSoundEnabled) {
            playMusic(context, resId);
        } else {
            stopMusic();
        }
    }

    public static boolean isSoundEnabled(Context context) {
        loadSettings(context);
        return isSoundEnabled;
    }

    public static void playSound(Context context, int resId) {
        loadSettings(context);
        if (!isSoundEnabled) return;
        MediaPlayer mp = MediaPlayer.create(context, resId);
        if (mp != null) {
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.start();
        }
    }
}
