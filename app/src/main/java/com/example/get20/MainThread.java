package com.example.get20;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class MainThread extends Thread {

    private SurfaceHolder surfaceHolder;
    private GamePanel gamePanel;

    private boolean running;
    private final long targetFrameNanos;
    private long lastTimeNanos;

    public MainThread(SurfaceHolder surfaceHolder, GamePanel gamePanel, float refreshRate) {
        this.surfaceHolder = surfaceHolder;
        this.gamePanel = gamePanel;

        if (refreshRate <= 0f) refreshRate = 60f;
        this.targetFrameNanos = (long) (1_000_000_000L / refreshRate);
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
    public boolean isRunning() {return running;}

    @Override
    public void run() {

        Canvas canvas;

        lastTimeNanos = System.nanoTime();

        while (running) {

            long frameStart = System.nanoTime();

            // Calculate delta time in seconds
            long now = frameStart;
            float dt = (now - lastTimeNanos) / 1_000_000_000f;
            lastTimeNanos = now;

            // Clamp dt to avoid huge jumps (e.g., after pause/resume)
            if (dt > 0.05f) dt = 0.05f;

            canvas = null;

            try {
                canvas = surfaceHolder.lockCanvas();

                synchronized (surfaceHolder) {
                    gamePanel.update(dt);

                    if (canvas != null) {
                        gamePanel.draw(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            // Frame limiting based on display refresh rate
            long frameTime = System.nanoTime() - frameStart;
            long sleepNanos = targetFrameNanos - frameTime;

            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
