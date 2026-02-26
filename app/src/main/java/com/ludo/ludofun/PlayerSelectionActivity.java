package com.ludo.ludofun;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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

public class PlayerSelectionActivity extends AppCompatActivity {

    private Button btn2, btn3, btn4;
    private TextView tvTitle;
    private LinearLayout buttonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);

        tvTitle = findViewById(R.id.tvOfflineTitle);
        tvTitle.setText("Select Players");
        buttonContainer = findViewById(R.id.buttonContainer);
        
        btn2 = findViewById(R.id.btnNewGame);
        btn2.setText("2 PLAYER");
        btn2.setBackgroundResource(R.drawable.stylish_button_bg);
        
        btn3 = findViewById(R.id.btnResume);
        btn3.setText("3 PLAYER");
        btn3.setVisibility(View.VISIBLE);
        btn3.setBackgroundResource(R.drawable.stylish_button_green);
        
        btn4 = findViewById(R.id.btnSnakeGame);
        btn4.setText("4 PLAYER");
        btn4.setBackgroundResource(R.drawable.stylish_button_blue);

        // Hide sound toggle on this page if it's there
        View soundToggle = findViewById(R.id.btnSoundToggle);
        if (soundToggle != null) soundToggle.setVisibility(View.GONE);

        applyTitleGradient();
        animateUI();

        setupButtonClick(btn2, () -> startGame(2));
        setupButtonClick(btn3, () -> startGame(3));
        setupButtonClick(btn4, () -> startGame(4));
    }

    private void startGame(int players) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("players", players);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.resumeMusic(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pauseMusic();
    }

    private void applyTitleGradient() {
        tvTitle.post(() -> {
            Shader shader = new LinearGradient(0, 0, 0, tvTitle.getHeight(),
                    new int[]{0xFFFFFFFF, 0xFFB0BEC5},
                    null, Shader.TileMode.CLAMP);
            tvTitle.getPaint().setShader(shader);
            tvTitle.invalidate();
        });
    }

    private void setupButtonClick(View view, Runnable action) {
        view.setOnClickListener(v -> {
            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                    v,
                    PropertyValuesHolder.ofFloat("scaleX", 0.92f),
                    PropertyValuesHolder.ofFloat("scaleY", 0.92f)
            );
            scaleDown.setDuration(120);
            scaleDown.setRepeatCount(1);
            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);
            scaleDown.setInterpolator(new AccelerateDecelerateInterpolator());
            
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
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(-100f);
        tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .withEndAction(this::startFloatingAnimation)
                .start();

        for (int i = 0; i < buttonContainer.getChildCount(); i++) {
            View child = buttonContainer.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.setAlpha(0f);
                child.setTranslationY(200f);
                child.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(900)
                        .setStartDelay(400 + (i * 250))
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .start();
            }
        }
    }

    private void startFloatingAnimation() {
        ObjectAnimator floating = ObjectAnimator.ofFloat(tvTitle, "translationY", -10f, 10f);
        floating.setDuration(2500);
        floating.setRepeatCount(ObjectAnimator.INFINITE);
        floating.setRepeatMode(ObjectAnimator.REVERSE);
        floating.setInterpolator(new AccelerateDecelerateInterpolator());
        floating.start();
    }
}
