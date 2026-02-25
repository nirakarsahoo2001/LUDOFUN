package com.ludo.ludofun;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private TextView tvTurn;
    private SceneView diceRed, diceGreen, diceYellow, diceBlue;
    private ImageView winRed, winGreen, winYellow, winBlue;
    private Node diceNodeRed, diceNodeGreen, diceNodeYellow, diceNodeBlue;
    private SceneView activeDiceView;
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

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        totalPlayers = getIntent().getIntExtra("players", 4);

        tvTurn = findViewById(R.id.tvTurn);
        tvTurn.setVisibility(View.GONE);

        boardView = findViewById(R.id.boardView);
        boardView.setTotalPlayers(totalPlayers);

        diceRed = findViewById(R.id.dice_red);
        diceGreen = findViewById(R.id.dice_green);
        diceYellow = findViewById(R.id.dice_yellow);
        diceBlue = findViewById(R.id.dice_blue);

        winRed = findViewById(R.id.win_red);
        winGreen = findViewById(R.id.win_green);
        winYellow = findViewById(R.id.win_yellow);
        winBlue = findViewById(R.id.win_blue);

        setup3DDice(diceRed, 1);
        setup3DDice(diceGreen, 2);
        setup3DDice(diceYellow, 3);
        setup3DDice(diceBlue, 4);

        btnSoundToggle = findViewById(R.id.btnSoundToggle);
        updateSoundIcon();

        btnSoundToggle.setOnClickListener(v -> {
            MusicManager.toggleSound(this, R.raw.splash_sound);
            updateSoundIcon();
        });

        boardView.setOnMoveFinishedListener((killed, reachedGoal) -> {
            checkAndShowWinners();

            int winner = boardView.getWinner();
            if (winner != 0) {
                isGameOverFlag = true;
                boardView.setGameOver();
                hideAllDice();
                return;
            }

            int ludoPlayer = getLudoPlayer(currentPlayer);

            if (boardView.isPlayerFinished(ludoPlayer)) {
                nextPlayer();
                return;
            }

            if (killed || reachedGoal || lastDiceValue == 6) {
                if (activeDiceView != null) {
                    activeDiceView.setEnabled(true);
                    startIdleAnimation();
                }
            } else {
                nextPlayer();
            }
        });

        updateDiceVisibility();

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finish();
                    }
                });
    }

    private void checkAndShowWinners() {
        List<Integer> winners = boardView.getWinnersList();
        for (int i = 0; i < winners.size(); i++) {
            int player = winners.get(i);
            ImageView winImg = null;
            switch (player) {
                case 1: winImg = winRed; break;
                case 2: winImg = winGreen; break;
                case 3: winImg = winYellow; break;
                case 4: winImg = winBlue; break;
            }
            if (winImg != null && winImg.getVisibility() == View.GONE) {
                int resId = 0;
                if (i == 0) resId = R.drawable.win_1st;
                else if (i == 1) resId = R.drawable.win_2nd;
                else if (i == 2) resId = R.drawable.win_3rd;
                
                if (resId != 0) {
                    winImg.setImageResource(resId);
                    winImg.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void updateSoundIcon() {
        if (btnSoundToggle == null) return;
        if (MusicManager.isSoundEnabled()) {
            btnSoundToggle.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
        } else {
            btnSoundToggle.setImageResource(android.R.drawable.ic_lock_silent_mode);
        }
    }

    private void startIdleAnimation() {
        stopIdleAnimation();
        if (activeDiceView == null) return;

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
        border.setShape(GradientDrawable.RECTANGLE);   // change OVAL → RECTANGLE
        border.setStroke(15, color);
        border.setColor(Color.parseColor("#1AFFFFFF"));
        border.setCornerRadius(0f);                   //  no rounded corners

        activeDiceView.setBackground(border);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(activeDiceView, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(activeDiceView, "scaleY", 1f, 1.15f, 1f);

        idleAnimator = new AnimatorSet();
        idleAnimator.playTogether(scaleX, scaleY);
        idleAnimator.setDuration(1200);
        idleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        idleAnimator.start();
    }

    private void stopIdleAnimation() {
        if (idleAnimator != null) {
            idleAnimator.cancel();
            idleAnimator = null;
        }
        if (activeDiceView != null) {
            activeDiceView.setBackground(null);
            activeDiceView.setScaleX(1f);
            activeDiceView.setScaleY(1f);
        }
    }

    private void setup3DDice(SceneView sceneView, int player) {
        ModelRenderable.builder()
                .setSource(this, Uri.parse("dice (1).glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> {
                    Node node = new Node();
                    node.setRenderable(renderable);
                    node.setParent(sceneView.getScene());
                    node.setLocalPosition(new Vector3(0f, 0f, -3f));
                    node.setLocalScale(new Vector3(1.6f, 1.6f, 1.6f));

                    switch (player) {
                        case 1: diceNodeRed = node; break;
                        case 2: diceNodeGreen = node; break;
                        case 3: diceNodeYellow = node; break;
                        case 4: diceNodeBlue = node; break;
                    }
                    // Default to show 6
                    node.setLocalRotation(getRotationForFace(6));
                })
                .exceptionally(throwable -> null);
    }

    private void rollDice3D() {
        if (activeDiceView == null || isGameOverFlag) return;
        activeDiceView.setEnabled(false);
        stopIdleAnimation();

        MusicManager.playSound(this, R.raw.dice_roll);

        final int result = random.nextInt(6) + 1;
        
        // LIMITATION LOGIC: Max 2 consecutive sixes
        if (result == 6) {
            consecutiveSixes++;
            if (consecutiveSixes > 2) {
                // Force a different result if we got more than 2 sixes
                rollDice3D(); // Re-roll internally
                return;
            }
        } else {
            consecutiveSixes = 0;
        }

        Node activeNode = getActiveDiceNode();

        if (activeNode == null) {
            setFinalDiceResult(result);
            return;
        }

        Quaternion endRotation = getRotationForFace(result);

        // Randomized slanting axes for a unique, physical roll
        float randX = 0.5f + random.nextFloat();
        float randY = 0.5f + random.nextFloat();
        float randZ = random.nextFloat();
        Vector3 randomAxis = new Vector3(randX, randY, randZ).normalized();

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1500);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float v = (float) animation.getAnimatedValue();
            activeNode.setLocalRotation(Quaternion.axisAngle(randomAxis, v * 1800));
            float height = 0.4f;
            float verticalOffset = (float) (-4 * height * Math.pow(v - 0.5f, 2) + height);
            activeNode.setLocalPosition(new Vector3(0f, verticalOffset, -3f));
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activeNode.setLocalPosition(new Vector3(0f, 0f, -3f));
                activeNode.setLocalRotation(endRotation);
                setFinalDiceResult(result);
            }
        });
        animator.start();
    }

    private Quaternion getRotationForFace(int face) {
        switch (face) {
            case 1: // Left (-X) to Front (+Z)
                return Quaternion.axisAngle(new Vector3(0, 1, 0), -90);
            case 2: // Already Front (+Z)
                return Quaternion.identity();
            case 3: // Bottom (-Y) to Front (+Z)
                return Quaternion.axisAngle(new Vector3(1, 0, 0), -90);
            case 4: // Top (+Y) to Front (+Z)
                return Quaternion.axisAngle(new Vector3(1, 0, 0), 90);
            case 5: // Back (-Z) to Front (+Z)
                return Quaternion.axisAngle(new Vector3(0, 1, 0), 180);
            case 6: // Right (+X) to Front (+Z)
                return Quaternion.axisAngle(new Vector3(0, 1, 0), 90);
            default:
                return Quaternion.identity();
        }
    }

    private Node getActiveDiceNode() {
        int ludoPlayer = getLudoPlayer(currentPlayer);
        switch (ludoPlayer) {
            case 1: return diceNodeRed;
            case 2: return diceNodeGreen;
            case 3: return diceNodeYellow;
            case 4: return diceNodeBlue;
            default: return null;
        }
    }

    private void setFinalDiceResult(int result) {
        lastDiceValue = result;
        handler.postDelayed(() -> {
            int ludoPlayer = getLudoPlayer(currentPlayer);
            if (boardView.hasMovableToken(ludoPlayer, result)) {
                boardView.setTurn(ludoPlayer, result);
            } else {
                handler.postDelayed(() -> nextPlayer(), 500);
            }
        }, 600);
    }

    private void hideAllDice() {
        stopIdleAnimation();
        diceRed.setVisibility(View.GONE);
        diceGreen.setVisibility(View.GONE);
        diceYellow.setVisibility(View.GONE);
        diceBlue.setVisibility(View.GONE);
        activeDiceView = null;
    }

    private void nextPlayer() {
        if (isGameOverFlag) return;
        
        consecutiveSixes = 0; // Reset counter for next player

        int nextP = currentPlayer;
        do {
            nextP++;
            if (nextP > totalPlayers) nextP = 1;
        } while (boardView.isPlayerFinished(getLudoPlayer(nextP)));
        currentPlayer = nextP;
        updateDiceVisibility();
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
        switch (ludoPlayer) {
            case 1: activeDiceView = diceRed; break;
            case 2: activeDiceView = diceGreen; break;
            case 3: activeDiceView = diceYellow; break;
            case 4: activeDiceView = diceBlue; break;
        }
        if (activeDiceView != null) {
            activeDiceView.setVisibility(View.VISIBLE);
            activeDiceView.setEnabled(true);
            activeDiceView.setOnClickListener(v -> rollDice3D());
            startIdleAnimation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (diceRed != null) diceRed.resume();
            if (diceGreen != null) diceGreen.resume();
            if (diceYellow != null) diceYellow.resume();
            if (diceBlue != null) diceBlue.resume();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pauseMusic();
        try {
            if (diceRed != null) diceRed.pause();
            if (diceGreen != null) diceGreen.pause();
            if (diceYellow != null) diceYellow.pause();
            if (diceBlue != null) diceBlue.pause();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (diceRed != null) diceRed.destroy();
            if (diceGreen != null) diceGreen.destroy();
            if (diceYellow != null) diceYellow.destroy();
            if (diceBlue != null) diceBlue.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
