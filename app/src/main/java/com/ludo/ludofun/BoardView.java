package com.ludo.ludofun;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BoardView extends View {
    private Bitmap safeStarBitmap;


    private int cell;
    private int boardSize;
    private int offsetX, offsetY;

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Handler handler = new Handler();

    // ===== PATH SYSTEM =====
    private int[][] path;
    private final int[] safeSpots = {1, 9, 14, 22, 27, 35, 40, 48};
    private final int[] startIndex = {1, 14, 40, 27}; // Red, Green, Yellow, Blue

    // ===== TOKENS (Stored as stepsMoved: -1 = Home, 0-50 = Main Path, 51-56 = Home Path/Goal) =====
    private int[] redTokens    = {-1, -1, -1, -1};
    private int[] greenTokens  = {-1, -1, -1, -1};
    private int[] yellowTokens = {-1, -1, -1, -1};
    private int[] blueTokens   = {-1, -1, -1, -1};

    private int currentPlayer = 1;
    private int diceValue = 0;
    private boolean isAnimating = false;
    private int animatingTokenPlayer = -1;
    private int animatingTokenIndex = -1;
    private float animationScale = 1.0f;
    
    private List<Integer> movableTokenIndices = new ArrayList<>();
    private float hintScale = 1.0f;
    private ValueAnimator hintAnimator;
    
    private boolean moveMade = false;
    private boolean isGameOver = false;
    private int totalPlayers = 4;
    private List<Integer> winners = new ArrayList<>();

    public interface OnMoveFinishedListener {
        void onMoveFinished(boolean killed, boolean reachedGoal);
    }
    private OnMoveFinishedListener moveFinishedListener;

    public void setOnMoveFinishedListener(OnMoveFinishedListener listener) {
        this.moveFinishedListener = listener;
    }

    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
        invalidate();
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createMainPath();
        safeStarBitmap = BitmapFactory.decodeResource(
                getResources(),
                R.drawable.star_safe   // change if your name different
        );
    }


    private void createMainPath() {
        path = new int[][]{
            // Main Path (52 cells, index 0-51)
            {0,6}, {1,6}, {2,6}, {3,6}, {4,6}, {5,6}, // 0-5
            {6,5}, {6,4}, {6,3}, {6,2}, {6,1}, {6,0}, // 6-11
            {7,0}, // 12
            {8,0}, {8,1}, {8,2}, {8,3}, {8,4}, {8,5}, // 13-18
            {9,6}, {10,6}, {11,6}, {12,6}, {13,6}, {14,6}, // 19-24
            {14,7}, // 25
            {14,8}, {13,8}, {12,8}, {11,8}, {10,8}, {9,8}, // 26-31
            {8,9}, {8,10}, {8,11}, {8,12}, {8,13}, {8,14}, // 32-37
            {7,14}, // 38
            {6,14}, {6,13}, {6,12}, {6,11}, {6,10}, {6,9}, // 39-44
            {5,8}, {4,8}, {3,8}, {2,8}, {1,8}, {0,8}, // 45-50
            {0,7}, // 51

            // Home Paths (6 cells each)
            // Red (Player 1) - 52-57
            {1,7}, {2,7}, {3,7}, {4,7}, {5,7}, {6,7},
            // Green (Player 2) - 58-63
            {7,1}, {7,2}, {7,3}, {7,4}, {7,5}, {7,6},
            // Yellow (Player 3) - 64-69
            {7,13}, {7,12}, {7,11}, {7,10}, {7,9}, {7,8},
            // Blue (Player 4) - 70-75
            {13,7}, {12,7}, {11,7}, {10,7}, {9,7}, {8,7}
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boardSize = Math.min(getWidth(), getHeight());
        cell = boardSize / 15;
        offsetX = (getWidth() - boardSize) / 2;
        offsetY = (getHeight() - boardSize) / 2;

        canvas.save();
        canvas.translate(offsetX, offsetY);
        drawBoard(canvas);
        drawSafeStars(canvas);
        
        // Group tokens by their position to handle stacking
        Map<String, List<TokenInfo>> positionMap = new HashMap<>();
        
        if (isPlayerActive(1)) addTokensToMap(positionMap, redTokens, Color.parseColor("#E53935"), 1);
        if (isPlayerActive(2)) addTokensToMap(positionMap, greenTokens, Color.parseColor("#43A047"), 2);
        if (isPlayerActive(3)) addTokensToMap(positionMap, yellowTokens, Color.parseColor("#FFB300"), 3);
        if (isPlayerActive(4)) addTokensToMap(positionMap, blueTokens, Color.parseColor("#1E88E5"), 4);
        
        drawStackedTokens(canvas, positionMap);
        
        if (isGameOver) {
            drawGameOverMessage(canvas);
        }
        
        canvas.restore();
    }

    private boolean isPlayerActive(int ludoPlayer) {
        if (totalPlayers == 2) {
            return ludoPlayer == 2 || ludoPlayer == 3;
        }
        if (totalPlayers == 3) {
            return ludoPlayer == 1 || ludoPlayer == 2 || ludoPlayer == 3;
        }
        return true; // 4 players or default
    }
    
    private void drawGameOverMessage(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#80000000")); // semi-transparent black background
        canvas.drawRect(0, 0, 15 * cell, 15 * cell, paint);

        paint.setColor(Color.RED); // Bold red letters
        paint.setTextSize(cell * 2.5f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        float centerX = (15 * cell) / 2f;
        float centerY = (15 * cell) / 2f;

        canvas.drawText("GAME OVER", centerX, centerY, paint);
    }

    private String getPlayerColorName(int player) {
        switch (player) {
            case 1: return "RED";
            case 2: return "GREEN";
            case 3: return "YELLOW";
            case 4: return "BLUE";
            default: return "UNKNOWN";
        }
    }
    
    public void setGameOver() {
        this.isGameOver = true;
        invalidate();
        // Play game over sound
        int resId = getResources().getIdentifier("win_sound_end", "raw", getContext().getPackageName());
        if (resId != 0) {
            MusicManager.playSound(getContext(), resId);
        }
    }

    private void addTokensToMap(Map<String, List<TokenInfo>> map, int[] tokens, int color, int player) {
        for (int i = 0; i < 4; i++) {
            String posKey;
            if (tokens[i] == -1) {
                posKey = "home_" + player + "_" + i; // Unique key for home tokens
            } else {
                int pathIdx;
                if (tokens[i] <= 50) {
                    pathIdx = (startIndex[player - 1] + tokens[i]) % 52;
                } else {
                    pathIdx = 52 + (player - 1) * 6 + (tokens[i] - 51);
                }
                posKey = "path_" + pathIdx;
            }
            
            if (!map.containsKey(posKey)) {
                map.put(posKey, new ArrayList<>());
            }
            map.get(posKey).add(new TokenInfo(player, i, tokens[i], color));
        }
    }

    private void drawStackedTokens(Canvas canvas, Map<String, List<TokenInfo>> positionMap) {
        for (Map.Entry<String, List<TokenInfo>> entry : positionMap.entrySet()) {
            List<TokenInfo> tokens = entry.getValue();
            int count = tokens.size();
            
            for (int i = 0; i < count; i++) {
                TokenInfo token = tokens.get(i);
                PointF p = getTokenPosition(token.player, token.index, token.steps);

                float drawX = p.x;
                float drawY = p.y;
                float currentCellSize = cell;

                // Pop-up effect for the animating token
                if (isAnimating && token.player == animatingTokenPlayer && token.index == animatingTokenIndex) {
                    currentCellSize *= animationScale;
                } else if (!isAnimating && token.player == currentPlayer && movableTokenIndices.contains(token.index)) {
                    // Pop-up hint for movable tokens
                    currentCellSize *= hintScale;
                }

                // If stacked, offset tokens slightly to show all of them
                if (count > 1 && token.steps != -1) {
                    float offset = cell * 0.22f; // Increased offset for better visibility
                    if (count == 2) {
                        drawX += (i == 0 ? -offset : offset);
                    } else if (count == 3) {
                        // Arrange in triangle
                        if (i == 0) drawY -= offset;
                        else if (i == 1) { drawX -= offset; drawY += offset; }
                        else { drawX += offset; drawY += offset; }
                    } else if (count == 4) {
                        // Arrange in square
                        if (i == 0) { drawX -= offset; drawY -= offset; }
                        else if (i == 1) { drawX += offset; drawY -= offset; }
                        else if (i == 2) { drawX -= offset; drawY += offset; }
                        else { drawX += offset; drawY += offset; }
                    } else if (count == 5) {
                        // 4 in corners + 1 in center
                        if (i == 0) { drawX -= offset; drawY -= offset; }
                        else if (i == 1) { drawX += offset; drawY -= offset; }
                        else if (i == 2) { drawX -= offset; drawY += offset; }
                        else if (i == 3) { drawX += offset; drawY += offset; }
                        // i == 4 stays at center
                    } else {
                        // Circle arrangement for 6 or more tokens
                        double angle = Math.toRadians((i * 360.0 / count) - 90);
                        drawX += (float) (Math.cos(angle) * offset * 1.1f);
                        drawY += (float) (Math.sin(angle) * offset * 1.1f);
                    }
                    currentCellSize *= (count > 4 ? 0.65f : 0.75f); // Shrink stacked tokens
                }

                // Token Shadow
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#44000000"));
                canvas.drawCircle(drawX + 4, drawY + 4, currentCellSize / 2.2f, paint);

                // Token Outer Ring
                paint.setColor(Color.WHITE);
                canvas.drawCircle(drawX, drawY, currentCellSize / 2.2f, paint);

                // Token Main Body
                paint.setColor(token.color);
                canvas.drawCircle(drawX, drawY, currentCellSize / 2.6f, paint);

                // Inner decorative circle
                paint.setColor(Color.parseColor("#33000000"));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                canvas.drawCircle(drawX, drawY, currentCellSize / 4f, paint);
            }
        }
    }
    
    private static class TokenInfo {
        int player, index, steps, color;
        TokenInfo(int p, int i, int s, int c) {
            player = p; index = i; steps = s; color = c;
        }
    }

    private void drawBoard(Canvas canvas) {
        // Clear background
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, 15 * cell, 15 * cell, paint);

        // Highlight the Token Paths (Cross area) with a very light gray
        paint.setColor(Color.parseColor("#F5F5F5"));
        canvas.drawRect(6 * cell, 0, 9 * cell, 15 * cell, paint);
        canvas.drawRect(0, 6 * cell, 15 * cell, 9 * cell, paint);

        // Home Bases - Always visible
        drawHomeBase(canvas, 0, 0, Color.parseColor("#E53935")); // Red
        drawHomeBase(canvas, 9 * cell, 0, Color.parseColor("#43A047")); // Green
        drawHomeBase(canvas, 0, 9 * cell, Color.parseColor("#FFB300")); // Yellow
        drawHomeBase(canvas, 9 * cell, 9 * cell, Color.parseColor("#1E88E5")); // Blue

        // Path Highlights (Colored rows - Home paths) - Always visible
        paint.setStyle(Paint.Style.FILL);
        
        // Red
        paint.setColor(Color.parseColor("#FFCDD2")); // Light Red
        for(int i=1; i<=5; i++) canvas.drawRect(i*cell, 7*cell, (i+1)*cell, 8*cell, paint);
        paint.setColor(Color.parseColor("#E53935"));
        canvas.drawRect(1*cell, 6*cell, 2*cell, 7*cell, paint); // Red start
        
        // Green
        paint.setColor(Color.parseColor("#C8E6C9")); // Light Green
        for(int i=1; i<=5; i++) canvas.drawRect(7*cell, i*cell, 8*cell, (i+1)*cell, paint);
        paint.setColor(Color.parseColor("#43A047"));
        canvas.drawRect(8*cell, 1*cell, 9*cell, 2*cell, paint); // Green start
        
        // Yellow
        paint.setColor(Color.parseColor("#FFF9C4")); // Light Yellow
        for(int i=9; i<=13; i++) canvas.drawRect(7*cell, i*cell, 8*cell, (i+1)*cell, paint);
        paint.setColor(Color.parseColor("#FFB300"));
        canvas.drawRect(6*cell, 13*cell, 7*cell, 14*cell, paint); // Yellow start
        
        // Blue
        paint.setColor(Color.parseColor("#BBDEFB")); // Light Blue
        for(int i=9; i<=13; i++) canvas.drawRect(i*cell, 7*cell, (i+1)*cell, 8*cell, paint);
        paint.setColor(Color.parseColor("#1E88E5"));
        canvas.drawRect(13*cell, 8*cell, 14*cell, 9*cell, paint); // Blue start

        // Grid lines (Thicker and darker for the path)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(Color.parseColor("#BDBDBD"));

        // Path Grid
        for (int i = 0; i <= 15; i++) {
            if (i >= 6 && i <= 9) {
                canvas.drawLine(i * cell, 0, i * cell, 15 * cell, paint);
                canvas.drawLine(0, i * cell, 15 * cell, i * cell, paint);
            }
        }
        for (int i = 0; i <= 15; i++) {
            if (i < 6 || i > 9) {
                canvas.drawLine(i * cell, 6 * cell, i * cell, 9 * cell, paint);
                canvas.drawLine(6 * cell, i * cell, 9 * cell, i * cell, paint);
            }
        }

        // Home Triangle (Center) - Always visible
        paint.setStyle(Paint.Style.FILL);
        Path homePath = new Path();
        
        // Red Triangle
        homePath.reset(); homePath.moveTo(6*cell, 6*cell); homePath.lineTo(7.5f*cell, 7.5f*cell); homePath.lineTo(6*cell, 9*cell);
        paint.setColor(Color.parseColor("#E53935")); canvas.drawPath(homePath, paint);
        
        // Green Triangle
        homePath.reset(); homePath.moveTo(6*cell, 6*cell); homePath.lineTo(7.5f*cell, 7.5f*cell); homePath.lineTo(9*cell, 6*cell);
        paint.setColor(Color.parseColor("#43A047")); canvas.drawPath(homePath, paint);
        
        // Yellow Triangle
        homePath.reset(); homePath.moveTo(6*cell, 9*cell); homePath.lineTo(7.5f*cell, 7.5f*cell); homePath.lineTo(9*cell, 9*cell);
        paint.setColor(Color.parseColor("#FFB300")); canvas.drawPath(homePath, paint);
        
        // Blue Triangle
        homePath.reset(); homePath.moveTo(9*cell, 6*cell); homePath.lineTo(7.5f*cell, 7.5f*cell); homePath.lineTo(9*cell, 9*cell);
        paint.setColor(Color.parseColor("#1E88E5")); canvas.drawPath(homePath, paint);
        
        // Final Center Square border
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(4);
        canvas.drawRect(6*cell, 6*cell, 9*cell, 9*cell, paint);
    }

    private void drawHomeBase(Canvas canvas, int x, int y, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(x, y, x + 6 * cell, y + 6 * cell, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRect(x + cell, y + cell, x + 5 * cell, y + 5 * cell, paint);

        // Draw 4 circle holders in home base
        paint.setColor(Color.parseColor("#EEEEEE"));
        canvas.drawCircle(x + 2*cell, y + 2*cell, cell/1.2f, paint);
        canvas.drawCircle(x + 4*cell, y + 2*cell, cell/1.2f, paint);
        canvas.drawCircle(x + 2*cell, y + 4*cell, cell/1.2f, paint);
        canvas.drawCircle(x + 4*cell, y + 4*cell, cell/1.2f, paint);
    }

    private void drawSafeStars(Canvas canvas) {
        if (safeStarBitmap == null) return;
        int starSize = (int)(cell * 0.8f);
        Bitmap scaledStar = Bitmap.createScaledBitmap(safeStarBitmap, starSize, starSize, true);
        for (int idx : safeSpots) {
            float cx = path[idx][0] * cell + cell / 2f;
            float cy = path[idx][1] * cell + cell / 2f;
            canvas.drawBitmap(scaledStar, cx - starSize/2f, cy - starSize/2f, null);
        }
    }

    private PointF getTokenPosition(int player, int tokenIdx, int steps) {
        if (steps == -1) {
            float bx = (player == 2 || player == 4) ? 9 * cell : 0;
            float by = (player == 3 || player == 4) ? 9 * cell : 0;
            float tx = bx + cell * (2f + (tokenIdx % 2) * 2f);
            float ty = by + cell * (2f + (tokenIdx / 2) * 2f);
            return new PointF(tx, ty);
        }
        int pathIdx;
        if (steps <= 50) {
            pathIdx = (startIndex[player - 1] + steps) % 52;
        } else {
            pathIdx = 52 + (player - 1) * 6 + (steps - 51);
        }
        return new PointF(path[pathIdx][0] * cell + cell / 2f, path[pathIdx][1] * cell + cell / 2f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && !isAnimating && diceValue > 0 && !moveMade && !isGameOver) {
            float tx = (event.getX() - offsetX);
            float ty = (event.getY() - offsetY);
            int[] tokens = getTokens(currentPlayer);
            for (int i = 0; i < 4; i++) {
                PointF p = getTokenPosition(currentPlayer, i, tokens[i]);
                double dist = Math.sqrt(Math.pow(p.x - tx, 2) + Math.pow(p.y - ty, 2));
                if (dist < cell * 0.8f) {
                    if (canMove(tokens[i], diceValue)) {
                        stopHintAnimation();
                        moveToken(tokens, i);
                        return true;
                    }
                }
            }
        }
        return true;
    }

    private void moveToken(int[] tokens, int idx) {
        moveMade = true;
        isAnimating = true;
        animatingTokenPlayer = currentPlayer;
        animatingTokenIndex = idx;
        final int targetSteps = (tokens[idx] == -1) ? 0 : tokens[idx] + diceValue;
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (tokens[idx] < targetSteps) {
                    tokens[idx]++;
                    // Start scaling up for "pop" effect at the beginning of each step
                    animationScale = 1.3f;
                    invalidate();

                    // Scale back down halfway through the step duration
                    handler.postDelayed(() -> {
                        animationScale = 1.0f;
                        invalidate();
                    }, 175);
                    
                    int resId = getResources().getIdentifier("token_move", "raw", getContext().getPackageName());
                    if (resId != 0) {
                        MusicManager.playSound(getContext(), resId);
                    }
                    handler.postDelayed(this, 350);
                } else {
                    isAnimating = false;
                    animatingTokenPlayer = -1;
                    animatingTokenIndex = -1;
                    animationScale = 1.0f;
                    onMoveComplete(tokens[idx]);
                }
            }
        });
    }

    private void onMoveComplete(int finalSteps) {
        boolean killed = false;
        if (finalSteps <= 50) {
            int pathIdx = (startIndex[currentPlayer - 1] + finalSteps) % 52;
            killed = checkKill(pathIdx);
        }
        boolean reachedGoal = (finalSteps == 56);

        // Check if current player finished the game
        if (isAllFinished(getTokens(currentPlayer)) && !winners.contains(currentPlayer)) {
            winners.add(currentPlayer);
        }

        diceValue = 0;
        if (moveFinishedListener != null) {
            moveFinishedListener.onMoveFinished(killed, reachedGoal);
        }
    }

    private boolean checkKill(int pathIdx) {
        if (isSafe(pathIdx)) return false;
        
        // Find all different color tokens at this position
        List<OpponentToken> targetTokens = new ArrayList<>();
        for (int p = 0; p < 4; p++) {
            if (p + 1 == currentPlayer) continue;
            int[] oppTokens = getTokens(p + 1);
            for (int t = 0; t < 4; t++) {
                if (oppTokens[t] != -1 && oppTokens[t] <= 50) {
                    int oppPathIdx = (startIndex[p] + oppTokens[t]) % 52;
                    if (oppPathIdx == pathIdx) {
                        targetTokens.add(new OpponentToken(p + 1, t));
                    }
                }
            }
        }

        if (targetTokens.isEmpty()) return false;

        // Logic: Kill the LAST token that reached this position.
        OpponentToken target = targetTokens.get(targetTokens.size() - 1);
        getTokens(target.player)[target.index] = -1;
        
        // Play kill sound (using dice roll as placeholder since specialized kill sound is missing)
        int killResId = getResources().getIdentifier("kill_sound", "raw", getContext().getPackageName());
        if (killResId != 0) {
            MusicManager.playSound(getContext(), killResId);
        }
        
        invalidate();
        return true;
    }
    
    private static class OpponentToken {
        int player, index;
        OpponentToken(int p, int i) { player = p; index = i; }
    }

    private boolean isSafe(int pathIdx) {
        for (int s : safeSpots) if (pathIdx == s) return true;
        return false;
    }

    private boolean canMove(int steps, int dice) {
        if (steps == -1) return dice == 6;
        return steps + dice <= 56;
    }

    public boolean hasMovableToken(int player, int dice) {
        int[] tokens = getTokens(player);
        int unfinishedCount = 0;
        int lastTokenIndex = -1;
        for (int i = 0; i < 4; i++) {
            if (tokens[i] < 56) {
                unfinishedCount++;
                lastTokenIndex = i;
            }
        }

        // Special Rule: If last token is "inside" home path (distance < 6), rolling a 6 will not work.
        if (unfinishedCount == 1 && dice == 6) {
            if (tokens[lastTokenIndex] > 50) return false;
        }

        for (int t : tokens) if (canMove(t, dice)) return true;
        return false;
    }

    private int[] getTokens(int player) {
        switch (player) {
            case 1: return redTokens;
            case 2: return greenTokens;
            case 3: return yellowTokens;
            case 4: return blueTokens;
            default: return redTokens;
        }
    }

    private void startHintAnimation() {
        if (hintAnimator != null) hintAnimator.cancel();

        hintAnimator = ValueAnimator.ofFloat(1.0f, 1.25f, 1.0f);
        hintAnimator.setDuration(1000);
        hintAnimator.setRepeatCount(ValueAnimator.INFINITE);
        hintAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        hintAnimator.addUpdateListener(animation -> {
            hintScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        hintAnimator.start();
    }

    private void stopHintAnimation() {
        if (hintAnimator != null) {
            hintAnimator.cancel();
            hintAnimator = null;
        }
        hintScale = 1.0f;
        movableTokenIndices.clear();
        invalidate();
    }

    public void setTurn(int player, int dice) {
        this.currentPlayer = player;
        this.diceValue = dice;
        this.moveMade = false;
        
        movableTokenIndices.clear();
        int[] tokens = getTokens(player);
        for (int i = 0; i < 4; i++) {
            if (canMove(tokens[i], dice)) {
                movableTokenIndices.add(i);
            }
        }

        if (movableTokenIndices.isEmpty()) {
            handler.postDelayed(() -> {
                if (moveFinishedListener != null) moveFinishedListener.onMoveFinished(false, false);
            }, 1000);
        } else if (movableTokenIndices.size() == 1) {
            // Automatically move if only one token can move
            int targetIdx = movableTokenIndices.get(0);
            handler.postDelayed(() -> {
                movableTokenIndices.clear();
                moveToken(tokens, targetIdx);
            }, 500);
        } else {
            // Multiple tokens can move. Check if all movable tokens are at the same position.
            boolean allAtSamePos = true;
            int firstSteps = tokens[movableTokenIndices.get(0)];
            for (int i = 1; i < movableTokenIndices.size(); i++) {
                if (tokens[movableTokenIndices.get(i)] != firstSteps) {
                    allAtSamePos = false;
                    break;
                }
            }

            if (allAtSamePos) {
                // All movable tokens are at the same spot (stacked), move the first one automatically
                int targetIdx = movableTokenIndices.get(0);
                handler.postDelayed(() -> {
                    movableTokenIndices.clear();
                    moveToken(tokens, targetIdx);
                }, 500);
            } else {
                // Movable tokens are at different spots, show hints for selection
                if (dice == 6) {
                    // Special case: if all 4 tokens are in home and dice is 6, move the first one automatically
                    boolean allInHome = true;
                    for (int t : tokens) {
                        if (t != -1) {
                            allInHome = false;
                            break;
                        }
                    }
                    if (allInHome) {
                        handler.postDelayed(() -> {
                            movableTokenIndices.clear();
                            moveToken(tokens, 0);
                        }, 500);
                        return;
                    }
                }
                startHintAnimation();
            }
        }
    }

    public int getWinner() {
        int activeCount = 0;
        if (isPlayerActive(1)) activeCount++;
        if (isPlayerActive(2)) activeCount++;
        if (isPlayerActive(3)) activeCount++;
        if (isPlayerActive(4)) activeCount++;

        if (winners.size() >= activeCount - 1) {
            return winners.isEmpty() ? 0 : winners.get(0);
        }
        return 0;
    }

    public boolean isPlayerFinished(int player) {
        return winners.contains(player);
    }

    public List<Integer> getWinnersList() {
        return winners;
    }

    private boolean isAllFinished(int[] tokens) {
        for (int t : tokens) if (t < 56) return false;
        return true;
    }
}
