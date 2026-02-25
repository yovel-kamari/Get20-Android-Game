package com.example.get20;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {

    private MainThread thread;
    private Board board;

    private GameActivity activity;

    public GamePanel(Context context, GameActivity activity, Bitmap[] images) {
        super(context);

        // Keep reference to the Activity for UI events (game over, expansion, etc.)
        this.activity = activity;

        // Register this GamePanel instance as a SurfaceHolder callback listener
        getHolder().addCallback(this);

        // Create the game loop thread
        thread = new MainThread(getHolder(), this);

        board = new Board(
                context.getResources().getDisplayMetrics().widthPixels,
                context.getResources().getDisplayMetrics().heightPixels,
                images
        );

        setFocusable(true); // Enable touch input
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Start the game loop thread when the surface is ready
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        // Flag to retry stopping the thread if needed
        boolean retry = true;

        // Stop the game loop
        thread.setRunning(false);

        // Wait until the thread finishes execution
        while (retry) {
            try {
                thread.join(); // Wait for thread to terminate
                retry = false;
            } catch (InterruptedException ignored) {
                // Retry if interruption occurs
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Called when the surface size or format changes (not used here)
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // Handle only the touch press event (ignore move or drag)
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            if (!board.isGameOver()) {
                board.handleTouch(event.getX(), event.getY());
            }

            return true;
        }

        return super.onTouchEvent(event);
    }

    public void update() {

        // If game ended -> notify activity and stop loop
        if (board.isGameOver()) {

            activity.onGameOver(
                    board.getScore(),
                    board.getMaxTileCurrentGame()
            );

            thread.setRunning(false);
            return;
        }

        board.update();
        activity.updateScore(board.getScore());

        if (board.isExpansionTriggered()) {
            activity.onBoardExpanded(board.getBoardSize());
            board.resetExpansionFlag();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (canvas != null) {
            board.draw(canvas);
        }
    }

    // Pause game loop when activity is paused
    public void pauseGame() {
        thread.setRunning(false);
    }

    // Resume game loop when activity resumes
    public void resumeGame() {
        if (!thread.isAlive()) {
            thread = new MainThread(getHolder(), this);
            thread.setRunning(true);
            thread.start();
        }
    }

    public Board getBoard() {
        return board;
    }
}