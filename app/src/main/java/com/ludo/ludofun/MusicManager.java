package com.ludo.ludofun;

import android.content.Context;
import android.media.MediaPlayer;

public class MusicManager {
    private static MediaPlayer backgroundPlayer;
    private static int currentResId = -1;
    private static boolean isSoundEnabled = true;

    public static void playMusic(Context context, int resId) {
        if (!isSoundEnabled) return;

        if (backgroundPlayer != null && currentResId == resId) {
            if (!backgroundPlayer.isPlaying()) {
                backgroundPlayer.start();
            }
            return;
        }

        stopMusic();

        backgroundPlayer = MediaPlayer.create(context.getApplicationContext(), resId);
        if (backgroundPlayer != null) {
            currentResId = resId;
            backgroundPlayer.setLooping(true);
            backgroundPlayer.start();
        }
    }

    public static void pauseMusic() {
        if (backgroundPlayer != null && backgroundPlayer.isPlaying()) {
            backgroundPlayer.pause();
        }
    }

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
            currentResId = -1;
        }
    }

    public static void toggleSound(Context context, int resId) {
        isSoundEnabled = !isSoundEnabled;
        if (isSoundEnabled) {
            playMusic(context, resId);
        } else {
            stopMusic();
        }
    }

    public static boolean isSoundEnabled() {
        return isSoundEnabled;
    }

    public static void playSound(Context context, int resId) {
        if (!isSoundEnabled) return;
        MediaPlayer mp = MediaPlayer.create(context, resId);
        if (mp != null) {
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.start();
        }
    }
}
