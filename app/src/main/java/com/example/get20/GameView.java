package com.example.get20;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;
import java.util.ArrayList;

public class GameView extends View {
    private Board board;
    private Bitmap[] images;
    private long lastTimeNanos;
    private ScoreListener scoreListener;
    private GameOverListener gameOverListener;
    private int lastScore = -1;
    private boolean gameOverNotified;

    private float[] particleX, particleY, particleSpeed, particleRadius;
    private final int particleCount = 10;
    private boolean particlesInitialized;
    private int backgroundColor = Color.TRANSPARENT;
    private final Paint pPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    
    private ArrayList<int[]> currentHintGroup;

    public GameView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setImages(Bitmap[] images) { this.images = images; }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (images != null && w > 0) {
            board = new Board(w, h, images);
            initParticles(w, h);
            lastTimeNanos = System.nanoTime();
        }
    }

    private void initParticles(int w, int h) {
        particleX = new float[particleCount]; particleY = new float[particleCount];
        particleSpeed = new float[particleCount]; particleRadius = new float[particleCount];
        for (int i = 0; i < particleCount; i++) {
            particleX[i] = (float) (Math.random() * w);
            particleY[i] = (float) (Math.random() * h);
            particleSpeed[i] = 20 + (float) (Math.random() * 40);
            particleRadius[i] = 5 + (float) (Math.random() * 10);
        }
        particlesInitialized = true;
    }

    /**
     * Finds a possible move and highlights it on the board.
     */
    public void showHint() {
        if (board == null) return;
        clearHint(); // Clear old hint first
        currentHintGroup = board.getFirstAvailableGroup();
        if (currentHintGroup != null) {
            board.setHintGroup(currentHintGroup, true);
        }
    }

    /**
     * Removes any active move highlighting.
     */
    public void clearHint() {
        if (board != null && currentHintGroup != null) {
            board.setHintGroup(currentHintGroup, false);
            currentHintGroup = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (board == null) { postInvalidateOnAnimation(); return; }

        long now = System.nanoTime();
        float dt = Math.min((now - lastTimeNanos) / 1_000_000_000f, 0.05f);
        lastTimeNanos = now;

        board.update(dt);
        if (backgroundColor != Color.TRANSPARENT) canvas.drawColor(backgroundColor);
        
        if (particlesInitialized) {
            pPaint.setColor(backgroundColor == Color.TRANSPARENT ? 0x22FFFFFF : 0x22004062);
            for (int i = 0; i < particleCount; i++) {
                particleY[i] -= particleSpeed[i] * dt;
                if (particleY[i] < -particleRadius[i]) {
                    particleY[i] = getHeight();
                    particleX[i] = (float) (Math.random() * getWidth());
                }
                canvas.drawCircle(particleX[i], particleY[i], particleRadius[i], pPaint);
            }
        }
        board.draw(canvas);

        if (board.isGameOver() && !gameOverNotified) {
            gameOverNotified = true;
            if (gameOverListener != null) gameOverListener.onGameOver(board.getScore(), board.getMaxValueOnBoard());
            return;
        }

        if (board.getScore() != lastScore && scoreListener != null) {
            lastScore = board.getScore();
            scoreListener.onScoreChanged(lastScore);
        }
        postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (board != null && !board.isGameOver() && event.getAction() == MotionEvent.ACTION_DOWN) {
            clearHint(); // Remove hint as soon as user interacts
            board.handleTouch(event.getX(), event.getY());
            invalidate();
            
            // Notify activity that an interaction happened to reset hint timer
            if (scoreListener != null) scoreListener.onScoreChanged(board.getScore());
        }
        return true;
    }

    public interface ScoreListener { void onScoreChanged(int score); }
    public interface GameOverListener { void onGameOver(int score, int maxTile); }
    public void setScoreListener(ScoreListener l) { this.scoreListener = l; }
    public void setGameOverListener(GameOverListener l) { this.gameOverListener = l; }
    public void setBackgroundColorCustom(int c) { this.backgroundColor = c; invalidate(); }
}
