package com.example.get20;

import android.graphics.*;

public class Cell implements ICell {
    private int value;
    private boolean picked;
    private boolean hinted; // New state for tutorial highlighting
    private final int x, y, size;
    private float scale = 1f, popTime = 0f, offsetY = 0f, velocityY = 0f;
    private static final float POP_D = 0.15f, POP_P = 1.5f, G = 2500f, B = 0.3f;

    public Cell(int v, int x, int y, int s) { this.value = v; this.x = x; this.y = y; this.size = s; }

    public void startDropAnimation(float offset) { this.offsetY = offset; this.velocityY = 0f; }
    public void startPop() { popTime = POP_D; }
    public void setHinted(boolean hinted) { this.hinted = hinted; }

    @Override
    public void update(float dt) {
        if (popTime > 0) {
            popTime -= dt;
            float t = 1f - (Math.max(0, popTime) / POP_D);
            scale = 1f + (POP_P - 1f) * (float) Math.sin(t * Math.PI);
            if (popTime <= 0) scale = 1f;
        }
        if (offsetY < 0 || velocityY != 0) {
            velocityY += G * dt; offsetY += velocityY * dt;
            if (offsetY >= 0) {
                offsetY = 0;
                if (Math.abs(velocityY) > 500f) { velocityY = -velocityY * B; offsetY = -1f; }
                else velocityY = 0;
            }
        }
    }

    public void draw(Canvas c, Bitmap[] imgs) {
        if (value <= 0) return;
        int idx = value - 1; 
        if (picked || hinted) idx += imgs.length / 2; // Use highlighted image if hinted
        if (idx >= imgs.length) return;
        
        c.save();
        c.translate(x + size * 0.5f, y + size * 0.5f + offsetY);
        
        // If hinted, add a subtle pulse effect to the scale
        float finalScale = scale;
        if (hinted) {
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.1 + 1.0);
            finalScale *= pulse;
        }
        
        c.scale(finalScale, finalScale);
        c.drawBitmap(imgs[idx], -size * 0.5f, -size * 0.5f, null);
        c.restore();
    }

    public int getValue() { return value; }
    public void setValue(int v) { this.value = v; }
    public void setPicked(boolean p) { this.picked = p; }
}
