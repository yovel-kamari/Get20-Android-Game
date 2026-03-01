package com.example.get20;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

    private Board board;
    private Bitmap[] images;

    private long lastTimeNanos;
    private ScoreListener scoreListener;
    private int lastScore = -1;
    private GameOverListener gameOverListener;
    private boolean gameOverNotified = false;


    // Constructor when creating view programmatically
    public GameView(Context context) {
        super(context);
    }

    // Required constructor when inflating from XML
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Best practice constructor (style support)
    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setImages(Bitmap[] images) {
        this.images = images;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Board can only be created when:
        // 1. Images are loaded
        // 2. View has valid size (>0)
        if (images != null && w > 0 && h > 0) {
            board = new Board(w, h, images);
            lastTimeNanos = System.nanoTime();
        }
    }

    public void setScoreListener(ScoreListener listener) {
        this.scoreListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (board == null) {
            // Keep requesting frames until board is ready
            postInvalidateOnAnimation();
            return;
        }

        long now = System.nanoTime();
        float dt = (now - lastTimeNanos) / 1_000_000_000f;
        lastTimeNanos = now;

        // Prevent big animation jumps after pause
        if (dt > 0.05f) dt = 0.05f;

        board.update(dt);
        board.draw(canvas);
        // Check game over BEFORE scheduling next frame
        if (board.isGameOver() && !gameOverNotified) {

            gameOverNotified = true;

            if (gameOverListener != null) {
                gameOverListener.onGameOver(
                        board.getScore(),
                        board.getMaxValueOnBoard()
                );
            }

            return; // stop drawing loop
        }

        // Notify activity only when score changes
        int currentScore = board.getScore();
        if (currentScore != lastScore && scoreListener != null) {
            scoreListener.onScoreChanged(currentScore);
            lastScore = currentScore;
        }

        // Schedule next frame (simple game loop)
        postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (board == null || board.isGameOver())
            return true;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            board.handleTouch(event.getX(), event.getY());
            invalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }

    public interface ScoreListener {
        void onScoreChanged(int score);
    }
    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }
    public interface GameOverListener {
        void onGameOver(int finalScore, int maxTile);
    }
}