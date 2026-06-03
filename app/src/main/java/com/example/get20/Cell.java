package com.example.get20;

import android.graphics.*;

/** One tile on the board. Knows its number, draws itself, and animates itself. */
public class Cell {
    private int value;              // 0 = empty
    private boolean picked, hinted;
    private final int x, y, size;
    private Bitmap[] images;

    // animation state
    private float scale = 1f, popTime = 0f, offsetY = 0f, velocityY = 0f;

    private static final float POP_DURATION = 0.08f;
    private static final float POP_MAX_SCALE = 1.3f;
    private static final float GRAVITY = 30000f;      // higher = faster falling
    private static final float BOUNCE_DAMPING = 0.2f; // energy kept after a bounce

    public Cell(int v, int x, int y, int s) {
        this.value = v;
        this.x = x;
        this.y = y;
        this.size = s;
    }

    public void setImages(Bitmap[] images) {
        this.images = images;
    }

    public void startDropFrom(float initialOffset) {
        this.offsetY = initialOffset; // the height above the tile to start falling (negative = higher up)
        this.velocityY = 0f; // start at speed 0
    }

    public void startPop() {
        popTime = POP_DURATION;
    }

    public void setHinted(boolean hinted) {
        this.hinted = hinted;
    }

    // runs every frame; dt = seconds since the last frame
    public void update(float dt) {
        // pop: grow then shrink, smooth via sin (0 -> peak -> 0)
        if (popTime > 0) {
            popTime -= dt;
            float progress = 1f - (Math.max(0, popTime) / POP_DURATION);
            scale = 1f + (POP_MAX_SCALE - 1f) * (float) Math.sin(progress * Math.PI);
            if (popTime <= 0) scale = 1f;
        }

        // falling: speed grows from gravity, position moves by speed
        if (offsetY < 0 || velocityY != 0) {
            velocityY += GRAVITY * dt;
            offsetY += velocityY * dt;

            if (offsetY >= 0) {                              // hit the floor
                offsetY = 0;
                if (Math.abs(velocityY) > 500f) {
                    velocityY = -velocityY * BOUNCE_DAMPING; // bounce up with less energy
                    offsetY = -1f;
                } else velocityY = 0;
            }
        }
    }

    public void draw(Canvas canvas) {
        if (canvas == null || images == null || value <= 0) return;

        // 0-19 = normal picture, 20-39 = highlighted (picked/hint)
        int idx = value - 1 + ((picked || hinted) ? images.length / 2 : 0);
        if (idx < 0 || idx >= images.length || images[idx] == null) return;

        // normal tile (no pop, no hint): draw at its spot (offsetY shifts it up while it's still falling)
        if (scale == 1f && !hinted) {
            canvas.drawBitmap(images[idx], x, y + offsetY, null);
            return;
        }

        // pop (on merge) or hint pulse: needs scaling, done from the tile's center so it grows from the middle
        float s = scale;
        if (hinted) s *= (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.1 + 1.0); // hint pulse
        canvas.save();
        canvas.translate(x + size * 0.5f, y + size * 0.5f + offsetY);     // go to the tile's center
        canvas.scale(s, s);
        canvas.drawBitmap(images[idx], -size * 0.5f, -size * 0.5f, null); // draw centered on it
        canvas.restore();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int v) {
        this.value = v;
    }

    public void setPicked(boolean p) {
        this.picked = p;
    }
}
