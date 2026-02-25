package com.example.get20;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class MainThread extends Thread {

    private SurfaceHolder surfaceHolder;
    private GamePanel gamePanel;

    private boolean running;

    public MainThread(SurfaceHolder surfaceHolder, GamePanel gamePanel) {
        this.surfaceHolder = surfaceHolder;
        this.gamePanel = gamePanel;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
    public boolean isRunning() {return running;}

    @Override
    public void run() {

        Canvas canvas;

        while (running) {

            canvas = null;

            try {
                // Lock the canvas so only one thread can draw at a time (may return null if surface is invalid)
                canvas = surfaceHolder.lockCanvas();
                // Prevent concurrent access during update and draw
                synchronized (surfaceHolder) {

                    gamePanel.update();

                    if (canvas != null) {
                        gamePanel.draw(canvas);
                    }
                }
            } finally {
                // Always release and post the canvas to display the frame
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}
