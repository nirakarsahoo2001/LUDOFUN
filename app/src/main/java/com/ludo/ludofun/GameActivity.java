package com.ludo.ludofun;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private TextView tvTurn, tvCountdown;
    private View countdownContainer;
    private SceneView diceRed, diceGreen, diceYellow, diceBlue;
    private ImageView winRed, winGreen, winYellow, winBlue;
    private Node diceNodeRed, diceNodeGreen, diceNodeYellow, diceNodeBlue;
    private SceneView activeDiceView;
    private BoardView boardView;

    private int currentPlayer = 1;
    private int lastDiceValue = 0;
    private int consecutiveSixes = 0;
    private int totalPlayers;
    private boolean isVsComputer = false;
    private Random random = new Random();
    private Handler handler = new Handler();
    private AnimatorSet idleAnimator;
    private boolean isGameOverFlag = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Start background music
        MusicManager.playMusic(this, R.raw.splash_sound);

        boolean isResume = getIntent().getBooleanExtra("resume", false);
        if (isResume) {
            SharedPreferences prefs = getSharedPreferences("LudoPrefs", MODE_PRIVATE);
            totalPlayers = prefs.getInt("totalPlayers", 4);
            isVsComputer = prefs.getBoolean("isVsComputer", false);
            currentPlayer = prefs.getInt("currentPlayer", 1);
        } else {
            totalPlayers = getIntent().getIntExtra("players", 4);
            isVsComputer = getIntent().getBooleanExtra("vsComputer", false);
            currentPlayer = random.nextInt(totalPlayers) + 1;
        }

        tvTurn = findViewById(R.id.tvTurn);
        tvTurn.setVisibility(View.GONE);
        
        countdownContainer = findViewById(R.id.countdown_container);
        tvCountdown = findViewById(R.id.tvCountdown);

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

        if (isResume) {
            loadGameState();
            updateDiceVisibility();
        } else {
            saveGameState(); // Initial save
            startStartGameCountdown();
        }

        boardView.setOnMoveFinishedListener((killed, reachedGoal) -> {
            checkAndShowWinners();
            saveGameState(); // Save state after every move

            int winner = boardView.getWinner();
            if (winner != 0) {
                isGameOverFlag = true;
                boardView.setGameOver();
                hideAllDice();
                clearGameState(); // Game over, clear the save
                showStylishGameOver(); // Stylish Game Over Message
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
                    checkAndPerformAiAction();
                }
            } else {
                nextPlayer();
            }
        });

        boardView.setOnWaitingForMoveListener(() -> {
            if (isVsComputer && isComputerTurn()) {
                boardView.performAiMove();
            }
        });

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (!isGameOverFlag) {
                            saveGameState();
                        }
                        finish();
                    }
                });
    }

    private void applyStylishGradient(TextView textView) {
        textView.post(() -> {
            Shader shader = new LinearGradient(0, 0, 0, textView.getHeight(),
                    new int[]{0xFFFFD700, 0xFFFF8C00, 0xFFFF4500}, 
                    null, Shader.TileMode.CLAMP);
            textView.getPaint().setShader(shader);
            textView.invalidate();
        });
    }

    private void startStartGameCountdown() {
        if (countdownContainer == null || tvCountdown == null) return;
        countdownContainer.setVisibility(View.VISIBLE);
        hideAllDice(); // Initially hide dice until countdown finishes

        final String[] sequence = {"3", "2", "1", "LET'S PLAY!"};
        animateSequence(sequence, 0);
    }

    private void showStylishGameOver() {
        if (countdownContainer == null || tvCountdown == null) return;
        countdownContainer.setVisibility(View.VISIBLE);
        tvCountdown.setText("GAME OVER");
        tvCountdown.setTextSize(70);
        applyStylishGradient(tvCountdown);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvCountdown, "scaleX", 0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvCountdown, "scaleY", 0f, 1.2f, 1.0f);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(tvCountdown, "rotation", -10f, 10f, 0f);
        
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, rotate);
        set.setDuration(1500);
        set.setInterpolator(new AnticipateOvershootInterpolator());
        set.start();
        
        MusicManager.playSound(this, R.raw.win_sound_end);
    }

    private void animateSequence(final String[] sequence, final int index) {
        if (index >= sequence.length) {
            countdownContainer.setVisibility(View.GONE);
            updateDiceVisibility();
            return;
        }

        tvCountdown.setText(sequence[index]);
        if (index == sequence.length - 1) {
            tvCountdown.setTextSize(60);
        } else {
            tvCountdown.setTextSize(120);
        }
        applyStylishGradient(tvCountdown);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvCountdown, "scaleX", 0f, 1.5f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvCountdown, "scaleY", 0f, 1.5f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(tvCountdown, "alpha", 0f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(tvCountdown, "rotation", -30f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha, rotation);
        set.setDuration(800);
        set.setInterpolator(new OvershootInterpolator(1.5f));
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                new Handler().postDelayed(() -> {
                    ObjectAnimator fadeOutAlpha = ObjectAnimator.ofFloat(tvCountdown, "alpha", 1f, 0f);
                    ObjectAnimator fadeOutScaleX = ObjectAnimator.ofFloat(tvCountdown, "scaleX", 1f, 2f);
                    ObjectAnimator fadeOutScaleY = ObjectAnimator.ofFloat(tvCountdown, "scaleY", 1f, 2f);
                    
                    AnimatorSet fadeOut = new AnimatorSet();
                    fadeOut.playTogether(fadeOutAlpha, fadeOutScaleX, fadeOutScaleY);
                    fadeOut.setDuration(300);
                    fadeOut.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            tvCountdown.setScaleX(1f);
                            tvCountdown.setScaleY(1f);
                            animateSequence(sequence, index + 1);
                        }
                    });
                    fadeOut.start();
                }, 400);
            }
        });
        set.start();
    }

    private void saveGameState() {
        if (isGameOverFlag) return;
        
        SharedPreferences prefs = getSharedPreferences("LudoPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        try {
            editor.putBoolean("hasSavedGame", true);
            editor.putInt("totalPlayers", totalPlayers);
            editor.putBoolean("isVsComputer", isVsComputer);
            editor.putInt("currentPlayer", currentPlayer);
            
            editor.putString("redTokens", arrayToJson(boardView.getRedTokens()));
            editor.putString("greenTokens", arrayToJson(boardView.getGreenTokens()));
            editor.putString("yellowTokens", arrayToJson(boardView.getYellowTokens()));
            editor.putString("blueTokens", arrayToJson(boardView.getBlueTokens()));
            
            JSONArray winnersArray = new JSONArray();
            for (int w : boardView.getWinnersList()) winnersArray.put(w);
            editor.putString("winners", winnersArray.toString());
            
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String arrayToJson(int[] tokens) {
        JSONArray arr = new JSONArray();
        for (int t : tokens) arr.put(t);
        return arr.toString();
    }

    private void loadGameState() {
        SharedPreferences prefs = getSharedPreferences("LudoPrefs", MODE_PRIVATE);
        try {
            boardView.setRedTokens(jsonToArray(prefs.getString("redTokens", "[-1,-1,-1,-1]")));
            boardView.setGreenTokens(jsonToArray(prefs.getString("greenTokens", "[-1,-1,-1,-1]")));
            boardView.setYellowTokens(jsonToArray(prefs.getString("yellowTokens", "[-1,-1,-1,-1]")));
            boardView.setBlueTokens(jsonToArray(prefs.getString("blueTokens", "[-1,-1,-1,-1]")));
            
            JSONArray winnersArray = new JSONArray(prefs.getString("winners", "[]"));
            boardView.getWinnersList().clear();
            for (int i = 0; i < winnersArray.length(); i++) {
                boardView.getWinnersList().add(winnersArray.getInt(i));
            }
            checkAndShowWinners();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int[] jsonToArray(String json) throws JSONException {
        JSONArray arr = new JSONArray(json);
        int[] res = new int[arr.length()];
        for (int i = 0; i < arr.length(); i++) res[i] = arr.getInt(i);
        return res;
    }

    private void clearGameState() {
        SharedPreferences prefs = getSharedPreferences("LudoPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("hasSavedGame", false).apply();
    }

    private boolean isComputerTurn() {
        return isVsComputer && currentPlayer == 2;
    }

    private void checkAndPerformAiAction() {
        if (isVsComputer && isComputerTurn() && !isGameOverFlag) {
            handler.postDelayed(() -> {
                if (activeDiceView != null && activeDiceView.isEnabled()) {
                    rollDice3D();
                }
            }, 1000);
        }
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

    private void startIdleAnimation() {
        stopIdleAnimation();
        if (activeDiceView == null) return;

        int ludoPlayer = getLudoPlayer(currentPlayer);
        int color;
        switch (ludoPlayer) {
            case 1: color = Color.parseColor("#E53935"); break;
            case 2: color = Color.parseColor("#43A047"); break;
            case 3: color = Color.parseColor("#FFB300"); break;
            case 4: color = Color.parseColor("#1E88E5"); break;
            default: color = Color.WHITE;
        }

        GradientDrawable border = new GradientDrawable();
        border.setShape(GradientDrawable.RECTANGLE);
        border.setStroke(15, color);
        border.setColor(Color.parseColor("#1AFFFFFF"));
        border.setCornerRadius(0f);

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
                    node.setLocalRotation(getRotationForFace(6));

                    if (isVsComputer && isComputerTurn() && activeDiceView != null && activeDiceView.isEnabled()) {
                        checkAndPerformAiAction();
                    }
                })
                .exceptionally(throwable -> null);
    }

    private void rollDice3D() {
        if (activeDiceView == null || isGameOverFlag) return;
        activeDiceView.setEnabled(false);
        stopIdleAnimation();

        MusicManager.playSound(this, R.raw.dice_roll);

        final int result = random.nextInt(6) + 1;
        
        if (result == 6) {
            consecutiveSixes++;
            if (consecutiveSixes > 2) {
                rollDice3D();
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
            case 1: return Quaternion.axisAngle(new Vector3(0, 1, 0), -90);
            case 2: return Quaternion.identity();
            case 3: return Quaternion.axisAngle(new Vector3(1, 0, 0), -90);
            case 4: return Quaternion.axisAngle(new Vector3(1, 0, 0), 90);
            case 5: return Quaternion.axisAngle(new Vector3(0, 1, 0), 180);
            case 6: return Quaternion.axisAngle(new Vector3(0, 1, 0), 90);
            default: return Quaternion.identity();
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
        
        consecutiveSixes = 0;

        int nextP = currentPlayer;
        do {
            nextP++;
            if (nextP > totalPlayers) nextP = 1;
        } while (boardView.isPlayerFinished(getLudoPlayer(nextP)));
        currentPlayer = nextP;
        updateDiceVisibility();
        saveGameState();
    }

    private int getLudoPlayer(int logicalPlayer) {
        if (totalPlayers == 2) {
            return (logicalPlayer == 1) ? 3 : 2;
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

            checkAndPerformAiAction();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.resumeMusic(this);
        try {
            if (diceRed != null) diceRed.resume();
            if (diceGreen != null) diceGreen.resume();
            if (diceYellow != null) diceYellow.resume();
            if (diceBlue != null) diceBlue.resume();
        } catch (Exception e) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pauseMusic();
        if (!isGameOverFlag) {
            saveGameState();
        }
        try {
            if (diceRed != null) diceRed.pause();
            if (diceGreen != null) diceGreen.pause();
            if (diceYellow != null) diceYellow.pause();
            if (diceBlue != null) diceBlue.pause();
        } catch (Exception e) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (diceRed != null) diceRed.destroy();
            if (diceGreen != null) diceGreen.destroy();
            if (diceYellow != null) diceYellow.destroy();
            if (diceBlue != null) diceBlue.destroy();
        } catch (Exception e) {}
    }
}
