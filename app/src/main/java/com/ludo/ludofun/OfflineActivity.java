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

public class OfflineActivity extends AppCompatActivity {

    private Button btn2, btn3, btn4;
    private TextView tvTitle;
    private LinearLayout buttonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);

        tvTitle = findViewById(R.id.tvSelectPlayers);
        buttonContainer = findViewById(R.id.buttonContainer);
        btn2 = findViewById(R.id.btn2Player);
        btn3 = findViewById(R.id.btn3Player);
        btn4 = findViewById(R.id.btn4Player);

        // Apply Stylish Gradient to the Title
        applyTitleGradient();

        // Stylish Animations
        animateUI();

        // Setup Stylish Click Animations
        setupButtonClick(btn2, () -> startGame(2));
        setupButtonClick(btn3, () -> startGame(3));
        setupButtonClick(btn4, () -> startGame(4));
    }

    private void applyTitleGradient() {
        tvTitle.post(() -> {
            Shader shader = new LinearGradient(0, 0, 0, tvTitle.getHeight(),
                    new int[]{0xFFFFFFFF, 0xFFB0BEC5}, // White to Grey
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
        ObjectAnimator floating = ObjectAnimator.ofFloat(tvTitle, "translationY", -10f, 10f);
        floating.setDuration(2000);
        floating.setRepeatCount(ObjectAnimator.INFINITE);
        floating.setRepeatMode(ObjectAnimator.REVERSE);
        floating.setInterpolator(new AccelerateDecelerateInterpolator());
        floating.start();
    }

    private void startGame(int players) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("players", players);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.playMusic(this, R.raw.splash_sound);
    }
}
