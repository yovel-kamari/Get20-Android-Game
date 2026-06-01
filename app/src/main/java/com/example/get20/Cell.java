package com.example.get20;

import android.graphics.*;

/**
 * Represents a single tile on the board.
 * Manages its data, state, and visual animations (Gravity, Pop, Hint pulse).
 */
public class Cell implements ICell {
    private int value;
    private boolean picked;
    private boolean hinted; // Tutorial highlighting state
    private final int x, y, size;
    
    // Visual state variables
    private float scale = 1f;
    private float popTime = 0f;
    private float offsetY = 0f;
    private float velocityY = 0f;
    
    // Animation Constants - using descriptive names for easier learning
    private static final float POP_DURATION = 0.15f; 
    private static final float POP_MAX_SCALE = 1.5f;
    private static final float GRAVITY = 2500f; // Acceleration in pixels per second squared
    private static final float BOUNCE_DAMPING = 0.3f; // Percentage of velocity kept after a bounce

    public Cell(int v, int x, int y, int s) { 
        this.value = v; 
        this.x = x; 
        this.y = y; 
        this.size = s; 
    }

    /**
     * Triggers the falling animation from a negative vertical offset.
     */
    public void startDropAnimation(float initialOffset) { 
        this.offsetY = initialOffset; 
        this.velocityY = 0f; 
    }

    /**
     * Triggers the "Pop" animation (usually called after a successful merge).
     */
    public void startPop() { popTime = POP_DURATION; }

    public void setHinted(boolean hinted) { this.hinted = hinted; }

    @Override
    public void update(float dt) {
        // 1. Pop Animation Logic:
        // Uses a Sine wave based on the remaining time to create a smooth expand/contract effect.
        if (popTime > 0) {
            popTime -= dt; // Countdown the timer by elapsed time
            float progress = 1f - (Math.max(0, popTime) / POP_DURATION); // Progress from 0.0 to 1.0
            // Sine from 0 to PI creates a value that goes 0 -> 1 -> 0
            scale = 1f + (POP_MAX_SCALE - 1f) * (float) Math.sin(progress * Math.PI);
            if (popTime <= 0) scale = 1f; // Reset to normal size at the end
        }

        // 2. Gravity Drop Physics:
        // Simulates falling until the tile reaches its target Y position (offsetY = 0).
        if (offsetY < 0 || velocityY != 0) {
            velocityY += GRAVITY * dt; // Velocity increases by gravity over time
            offsetY += velocityY * dt; // Position changes by current velocity
            
            // Check for collision with the "floor" (the target position)
            if (offsetY >= 0) {
                offsetY = 0; // Snap tile to its target position
                // Bounce logic: if moving fast enough, reverse and reduce velocity
                if (Math.abs(velocityY) > 500f) { 
                    velocityY = -velocityY * BOUNCE_DAMPING; // Reverse speed and dampen it
                    offsetY = -1f; // Force the logic to continue for the bounce movement
                } else {
                    velocityY = 0; // Too slow to bounce, stop completely
                }
            }
        }
    }

    /**
     * Renders the cell on the canvas.
     * @param canvas Target drawing surface.
     * @param images Array of bitmaps containing tile skins (normal and selected).
     */
    public void draw(Canvas canvas, Bitmap[] images) {
        // Edge Case: Check for null array or empty cell
        if (value <= 0 || images == null) return;

        // Calculate correct bitmap index (offset by half if 'picked' or 'hinted' state is active)
        int index = value - 1; 
        if (picked || hinted) index += images.length / 2;
        
        // Safety Check: Ensure index is within array bounds and bitmap is valid
        if (index < 0 || index >= images.length || images[index] == null) return;
        
        canvas.save();
        // Translate canvas to the cell center, applying the current animation offset
        canvas.translate(x + size * 0.5f, y + size * 0.5f + offsetY);
        
        float finalScale = scale;
        // Tutorial pulse effect: subtle continuous scaling based on real time
        if (hinted) {
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.1 + 1.0);
            finalScale *= pulse;
        }
        
        canvas.scale(finalScale, finalScale);
        // Draw the bitmap centered at the current coordinate origin
        canvas.drawBitmap(images[index], -size * 0.5f, -size * 0.5f, null);
        canvas.restore();
    }

    public int getValue() { return value; }
    public void setValue(int v) { this.value = v; }
    public void setPicked(boolean p) { this.picked = p; }
}
