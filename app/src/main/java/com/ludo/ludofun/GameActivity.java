package com.ludo.ludofun;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private TextView tvTurn;
    private ImageView imgDice; // Active dice reference
    private ImageView diceRed, diceGreen, diceYellow, diceBlue;
    private ImageButton btnSoundToggle;
    private BoardView boardView;

    private int currentPlayer = 1;
    private int lastDiceValue = 0;
    private int consecutiveSixes = 0;
    private int totalPlayers;
    private Random random = new Random();
    private Handler handler = new Handler();
    private AnimatorSet idleAnimator;
    private boolean isGameOverFlag = false;

    private int[] diceImages = {
            R.drawable.ludo_dice1,
            R.drawable.ludo_dice2,
            R.drawable.ludo_dice3,
            R.drawable.ludo_dice4,
            R.drawable.ludo_dice5,
            R.drawable.ludo_dice6
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        totalPlayers = getIntent().getIntExtra("players", 4);

        tvTurn = findViewById(R.id.tvTurn);
        boardView = findViewById(R.id.boardView);
        boardView.setTotalPlayers(totalPlayers);

        diceRed = findViewById(R.id.dice_red);
        diceGreen = findViewById(R.id.dice_green);
        diceYellow = findViewById(R.id.dice_yellow);
        diceBlue = findViewById(R.id.dice_blue);
        btnSoundToggle = findViewById(R.id.btnSoundToggle);

        updateSoundIcon();
        btnSoundToggle.setOnClickListener(v -> {
            MusicManager.toggleSound(this, R.raw.splash_sound);
            updateSoundIcon();
        });

        boardView.setOnMoveFinishedListener((killed, reachedGoal) -> {
            int winner = boardView.getWinner();
            if (winner != 0) {
                isGameOverFlag = true;
                boardView.setGameOver();
                tvTurn.setText(" Game Over! ");
                tvTurn.setTextColor(Color.MAGENTA);
                hideAllDice();
                return;
            }

            int ludoPlayer = getLudoPlayer(currentPlayer);
            
            if (boardView.isPlayerFinished(ludoPlayer)) {
                nextPlayer();
                return;
            }

            if (killed || reachedGoal || lastDiceValue == 6) {
                if (imgDice != null) {
                    imgDice.setEnabled(true);
                    startIdleAnimation();
                }
            } else {
                nextPlayer();
            }
        });

        updateDiceVisibility();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void hideAllDice() {
        stopIdleAnimation();
        diceRed.setVisibility(View.GONE);
        diceGreen.setVisibility(View.GONE);
        diceYellow.setVisibility(View.GONE);
        diceBlue.setVisibility(View.GONE);
        imgDice = null;
    }

    private void startIdleAnimation() {
        stopIdleAnimation();
        if (imgDice == null) return;

        int ludoPlayer = getLudoPlayer(currentPlayer);
        int color;
        switch (ludoPlayer) {
            case 1: color = Color.parseColor("#E53935"); break; // Red
            case 2: color = Color.parseColor("#43A047"); break; // Green
            case 3: color = Color.parseColor("#FFB300"); break; // Yellow
            case 4: color = Color.parseColor("#1E88E5"); break; // Blue
            default: color = Color.WHITE;
        }

        GradientDrawable border = new GradientDrawable();
        border.setShape(GradientDrawable.OVAL);
        border.setStroke(6, color);
        border.setColor(Color.parseColor("#1AFFFFFF"));
        
        imgDice.setBackground(border);
        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        imgDice.setPadding(padding, padding, padding, padding);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(imgDice, "scaleX", 1f, 1.25f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(imgDice, "scaleY", 1f, 1.25f, 1f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(imgDice, "rotation", -15f, 15f, -15f);

        idleAnimator = new AnimatorSet();
        idleAnimator.playTogether(scaleX, scaleY, rotate);
        idleAnimator.setDuration(1200);
        idleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        
        idleAnimator.start();
    }

    private void stopIdleAnimation() {
        if (idleAnimator != null) {
            idleAnimator.cancel();
            idleAnimator = null;
        }
        if (imgDice != null) {
            imgDice.setBackground(null);
            imgDice.setPadding(0, 0, 0, 0);
            imgDice.setScaleX(1f);
            imgDice.setScaleY(1f);
            imgDice.setRotation(0f);
        }
    }

    private void updateSoundIcon() {
        if (MusicManager.isSoundEnabled()) {
            btnSoundToggle.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
        } else {
            btnSoundToggle.setImageResource(android.R.drawable.ic_lock_silent_mode);
        }
    }

    private int getLudoPlayer(int logicalPlayer) {
        if (totalPlayers == 2) {
            return (logicalPlayer == 1) ? 2 : 3;
        }
        if (totalPlayers == 4) {
            if (logicalPlayer == 1) return 1;
            if (logicalPlayer == 2) return 2;
            if (logicalPlayer == 3) return 4;
            if (logicalPlayer == 4) return 3;
        }
        return logicalPlayer;
    }

    private void updateDiceVisibility() {
        if (isGameOverFlag) {
            hideAllDice();
            return;
        }

        stopIdleAnimation();
        diceRed.setVisibility(View.GONE);
        diceGreen.setVisibility(View.GONE);
        diceYellow.setVisibility(View.GONE);
        diceBlue.setVisibility(View.GONE);

        int ludoPlayer = getLudoPlayer(currentPlayer);

        if (boardView.isPlayerFinished(ludoPlayer)) {
            imgDice = null;
            return;
        }

        switch (ludoPlayer) {
            case 1: imgDice = diceRed; break;
            case 2: imgDice = diceGreen; break;
            case 3: imgDice = diceYellow; break;
            case 4: imgDice = diceBlue; break;
        }

        if (imgDice != null) {
            imgDice.setVisibility(View.VISIBLE);
            imgDice.setEnabled(true);
            imgDice.setOnClickListener(v -> rollDice());
            startIdleAnimation();

            String color = "Red";
            if (ludoPlayer == 2) color = "Green";
            if (ludoPlayer == 3) color = "Yellow";
            if (ludoPlayer == 4) color = "Blue";
            tvTurn.setText(color + "'s Turn");
        }
    }

    private void rollDice() {
        if (imgDice == null || isGameOverFlag) return;
        imgDice.setEnabled(false);
        stopIdleAnimation();

        for (int i = 0; i < 5; i++) {
            handler.postDelayed(() -> MusicManager.playSound(this, R.raw.dice_roll), i * 100);
        }

        ObjectAnimator rotateX = ObjectAnimator.ofFloat(imgDice, "rotationX", 0f, 360f);
        ObjectAnimator rotateY = ObjectAnimator.ofFloat(imgDice, "rotationY", 0f, 360f);
        rotateX.setDuration(500);
        rotateY.setDuration(500);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotateX, rotateY);
        animatorSet.start();

        handler.postDelayed(() -> {
            int diceValue = random.nextInt(6) + 1;
            
            if (diceValue == 6) {
                consecutiveSixes++;
            } else {
                consecutiveSixes = 0;
            }

            if (consecutiveSixes >= 3) {
                diceValue = random.nextInt(5) + 1; // Prevent 3rd consecutive six
                consecutiveSixes = 0;
            }

            lastDiceValue = diceValue;
            imgDice.setImageResource(diceImages[lastDiceValue - 1]);

            int ludoPlayer = getLudoPlayer(currentPlayer);
            if (boardView.hasMovableToken(ludoPlayer, lastDiceValue)) {
                boardView.setTurn(ludoPlayer, lastDiceValue);
            } else {
                handler.postDelayed(() -> nextPlayer(), 1000);
            }
        }, 500);
    }

    private void nextPlayer() {
        if (isGameOverFlag) return;
        
        consecutiveSixes = 0; // Reset on turn change
        int nextP = currentPlayer;
        int checkCount = 0;

        do {
            nextP++;
            if (nextP > totalPlayers) {
                nextP = 1;
            }
            checkCount++;
            if (checkCount > totalPlayers) break;
        } while (boardView.isPlayerFinished(getLudoPlayer(nextP)));
        
        currentPlayer = nextP;
        updateDiceVisibility();
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
