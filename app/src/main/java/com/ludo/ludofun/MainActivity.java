package com.ludo.ludofun;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnOffline, btnOnline, btnJoinRoom, btnCreateRoom;
    private TextView tvTitle;
    private LinearLayout buttonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTitle = findViewById(R.id.tvTitle);
        buttonContainer = findViewById(R.id.buttonContainer);
        btnOffline = findViewById(R.id.btnOffline);
        btnOnline = findViewById(R.id.btnOnline);
        btnJoinRoom = findViewById(R.id.btnJoinRoom);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);

        // Apply Stylish Gradient to the Title
        applyTitleGradient();

        // Stylish Animations
        animateUI();

        // Setup Stylish Click Animations
        setupButtonClick(btnOffline, () -> startActivity(new Intent(this, OfflineActivity.class)));
        setupButtonClick(btnOnline, () -> startActivity(new Intent(this, OnlineActivity.class)));
        setupButtonClick(btnJoinRoom, () -> {});
        setupButtonClick(btnCreateRoom, () -> {});
    }

    private void applyTitleGradient() {
        tvTitle.post(() -> {
            Shader shader = new LinearGradient(0, 0, 0, tvTitle.getHeight(),
                    new int[]{0xFFFFD700, 0xFFFF8C00}, // Gold to Dark Orange
                    null, Shader.TileMode.CLAMP);
            tvTitle.getPaint().setShader(shader);
            tvTitle.invalidate();
        });
    }

    private void setupButtonClick(View view, Runnable action) {
        view.setOnClickListener(v -> {
            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                    v,
                    PropertyValuesHolder.ofFloat("scaleX", 0.9f),
                    PropertyValuesHolder.ofFloat("scaleY", 0.9f)
            );
            scaleDown.setDuration(100);
            scaleDown.setRepeatCount(1);
            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
            scaleDown.setInterpolator(new OvershootInterpolator());
            
            scaleDown.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    action.run();
                }
            });
            scaleDown.start();
        });
    }

    private void animateUI() {
        // Entrance Animation for Title
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(-100f);
        tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .withEndAction(this::startFloatingAnimation)
                .start();

        // Entrance Animation for Buttons
        for (int i = 0; i < buttonContainer.getChildCount(); i++) {
            View child = buttonContainer.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(150f);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setStartDelay(500 + (i * 200))
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }
    }

    private void startFloatingAnimation() {
        // Continuous floating and pulse effect for the title
        ObjectAnimator floating = ObjectAnimator.ofFloat(tvTitle, "translationY", -15f, 15f);
        floating.setDuration(2500);
        floating.setRepeatCount(ObjectAnimator.INFINITE);
        floating.setRepeatMode(ObjectAnimator.REVERSE);
        floating.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(tvTitle,
                PropertyValuesHolder.ofFloat("scaleX", 1.05f),
                PropertyValuesHolder.ofFloat("scaleY", 1.05f));
        pulse.setDuration(1250);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(floating, pulse);
        animatorSet.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.resumeMusic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pauseMusic();
    }
}
