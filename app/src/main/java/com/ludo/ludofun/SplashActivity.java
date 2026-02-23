package com.ludo.ludofun;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView ivLogo = findViewById(R.id.ivLogo);
        TextView tvAppName = findViewById(R.id.tvAppName);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView tvLoading = findViewById(R.id.tvLoading);

        // Entrance Animations
        ivLogo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(1000).setInterpolator(new OvershootInterpolator()).start();
        tvAppName.animate().alpha(1f).translationY(0).setDuration(1000).setStartDelay(300).start();
        progressBar.animate().alpha(1f).setDuration(500).setStartDelay(800).start();
        tvLoading.animate().alpha(1f).setDuration(500).setStartDelay(1000).start();

        // Logo Pulse
        new Handler().postDelayed(() -> {
            ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(ivLogo,
                    PropertyValuesHolder.ofFloat("scaleX", 1.1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.1f));
            pulse.setDuration(1000);
            pulse.setRepeatCount(ObjectAnimator.INFINITE);
            pulse.setRepeatMode(ObjectAnimator.REVERSE);
            pulse.setInterpolator(new AccelerateDecelerateInterpolator());
            pulse.start();
        }, 1000);

        // PLAY SOUND (Make sure you add 'splash_sound' to res/raw folder)
        try {
            int soundId = getResources().getIdentifier("splash_sound", "raw", getPackageName());
            if (soundId != 0) {
                mediaPlayer = MediaPlayer.create(this, soundId);
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    mediaPlayer.start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Transition to MainActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 4000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}