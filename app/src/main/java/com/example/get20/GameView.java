package com.example.get20;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;

import androidx.annotation.NonNull;

import java.util.ArrayList;

/**
 * Draws the game ~60 times/second, handles touches, and tells the activity about score and game-over.
 */
public class GameView extends View {
    private Board board;
    private Bitmap[] images;
    private long lastTimeNanos;
    private ScoreListener scoreListener;
    private GameOverListener gameOverListener;
    private int lastScore = -1;          // last score we reported (so we don't report the same one twice)
    private boolean gameOverNotified;    // so game-over is fired only once

    // floating background circles
    private float[] particleX, particleY, particleSpeed, particleRadius;
    private final int particleCount = 10;
    private boolean particlesInitialized;
    private int backgroundColor = Color.TRANSPARENT;
    private final Paint pPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ArrayList<int[]> currentHintGroup;

    // holds saved game data until the board is created in onSizeChanged - can't create the board earlier because we need the view size
    private boolean isLoadingSavedGame = false;
    private int savedSize, savedScore, savedMaxValue;
    private String savedData;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // give the view the tile images before the board is created
    public void setImages(Bitmap[] images) {
        this.images = images;
    }

    // remember the saved game; it's actually loaded in onSizeChanged
    public void loadSavedGame(int size, int score, int maxValue, String data) {
        this.isLoadingSavedGame = true;
        this.savedSize = size;
        this.savedScore = score;
        this.savedMaxValue = maxValue;
        this.savedData = data;
    }

    // expose the board so GameActivity can read its state (score, game over, etc.)
    public Board getBoard() {
        return board;
    }

    // called once the view has a size - this is where we create the board and start the game
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // board is created here because it needs the view size to calculate cell dimensions
        if (images != null && w > 0) {
            if (board == null) { // create once - onSizeChanged can be called multiple times (e.g. screen rotation)
                if (isLoadingSavedGame) {
                    board = new Board(w, h, images, savedSize, savedScore);
                    board.loadBoardData(savedData);          // restore tile values
                    board.restoreHighestTile(savedMaxValue); // restore max tile stat
                    isLoadingSavedGame = false;
                } else {
                    board = new Board(w, h, images); // fresh game
                }
                lastScore = board.getScore(); // sync so we don't fire a fake "score changed" on the first frame
                initParticles(w, h);
                lastTimeNanos = System.nanoTime(); // start the dt clock
            }
        }
    }

    // background circles that float upward and loop back from the bottom
    private void initParticles(int w, int h) {
        particleX = new float[particleCount];
        particleY = new float[particleCount];
        particleSpeed = new float[particleCount];
        particleRadius = new float[particleCount];
        for (int i = 0; i < particleCount; i++) {
            particleX[i] = (float) (Math.random() * w);
            particleY[i] = (float) (Math.random() * h);
            particleSpeed[i] = 20 + (float) (Math.random() * 40);
            particleRadius[i] = 5 + (float) (Math.random() * 10);
        }
        particlesInitialized = true;
    }

    // highlight the first mergeable group on the board
    public void showHint() {
        if (board == null) return;
        clearHint();
        currentHintGroup = board.getFirstAvailableGroup();
        if (currentHintGroup != null) {
            board.setHintGroup(currentHintGroup, true);
        }
    }


    // remove the hint highlight from the board
    public void clearHint() {
        if (board != null && currentHintGroup != null) {
            board.setHintGroup(currentHintGroup, false);
            currentHintGroup = null;
        }
    }

    // main game loop - called ~60 times/second
    // each frame: update animations → draw background → draw tiles → notify activity → request next frame
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (board == null) {
            postInvalidateOnAnimation(); // board not ready yet, keep requesting frames until it is
            return;
        }

        float dt = calculateDeltaTime();
        board.update(dt);           // advance all tile animations
        drawBackground(canvas, dt); // background color + floating circles
        board.draw(canvas);         // draw all tiles on top
        notifyListeners();          // tell GameActivity about score changes and game over
        postInvalidateOnAnimation(); // ask for the next frame -> this is the game loop
    }


    // time since the last frame in seconds - used to keep animations speed-independent of frame rate
    private float calculateDeltaTime() {
        long now = System.nanoTime();
        float dt = Math.min((now - lastTimeNanos) / 1_000_000_000f, 0.05f); // capped at 50ms so animations don't jump if the phone freezes
        lastTimeNanos = now;
        return dt;
    }

    // fills the background color and moves/draws the floating circles on top
    private void drawBackground(Canvas canvas, float dt) {
        if (backgroundColor != Color.TRANSPARENT) canvas.drawColor(backgroundColor);

        if (particlesInitialized) {
            pPaint.setColor(backgroundColor == Color.TRANSPARENT ? 0x22FFFFFF : 0x22004062);
            for (int i = 0; i < particleCount; i++) {
                particleY[i] -= particleSpeed[i] * dt;   // float upward
                if (particleY[i] < -particleRadius[i]) { // off the top -> respawn at the bottom
                    particleY[i] = getHeight();
                    particleX[i] = (float) (Math.random() * getWidth());
                }
                canvas.drawCircle(particleX[i], particleY[i], particleRadius[i], pPaint);
            }
        }
    }

    // notify GameActivity about game over and score changes - we check every frame but fire each event only once
    private void notifyListeners() {
        if (board.isGameOver() && !gameOverNotified) {
            gameOverNotified = true;
            if (gameOverListener != null)
                gameOverListener.onGameOver(board.getScore(), board.getHighestTileValue(), board.isGameWon());
        }

        if (board.getScore() != lastScore && scoreListener != null) {
            lastScore = board.getScore();
            scoreListener.onScoreChanged(lastScore);
        }
    }

    // pass finger taps to the board
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (board != null && !board.isGameOver() && event.getAction() == MotionEvent.ACTION_DOWN) {
            clearHint();
            board.handleTouch(event.getX(), event.getY());
            invalidate();
        }
        if (event.getAction() == MotionEvent.ACTION_UP)
            performClick(); // required by Android accessibility guidelines
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    // callbacks so GameActivity can react to score changes and game over without GameView knowing about it
    public interface ScoreListener {
        void onScoreChanged(int score);
    }

    public interface GameOverListener {
        void onGameOver(int score, int maxTile, boolean isWin);
    }

    public void setScoreListener(ScoreListener l) {
        this.scoreListener = l;
    }

    public void setGameOverListener(GameOverListener l) {
        this.gameOverListener = l;
    }

    // used by GameActivity to switch between dark and light backgrounds
    public void setBackgroundColorCustom(int c) {
        this.backgroundColor = c;
        invalidate();
    }
}
