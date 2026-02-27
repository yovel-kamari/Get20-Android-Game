package com.example.get20;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class Cell implements ICell {

    private int value;
    private boolean picked;

    private int x;
    private int y;
    private int size;

    private Bitmap[] images;

    // Animation state (simple pop)
    private float scale = 1f;
    private float popTimeLeft = 0f;
    private static final float POP_DURATION = 0.15f; // seconds
    private static final float POP_PEAK = 1.5f;

    public Cell(int value, int x, int y, int size, Bitmap[] images) {
        this.value = value;
        this.x = x;
        this.y = y;
        this.size = size;
        this.images = images;
        this.picked = false;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setPicked(boolean picked) {
        this.picked = picked;
    }

    // Checks if a touch point is inside this cell
    public boolean isInside(float touchX, float touchY) {
        return touchX >= x && touchX <= x + size &&
                touchY >= y && touchY <= y + size;
    }
    public void startPop() {
        popTimeLeft = POP_DURATION;
        scale = 1f;
    }

    @Override
    public void update(float dt) {
        if (popTimeLeft > 0f) {
            popTimeLeft -= dt;
            if (popTimeLeft < 0f) popTimeLeft = 0f;

            float t = 1f - (popTimeLeft / POP_DURATION);
            float pulse = (float)Math.sin(t * Math.PI);
            scale = 1f + (POP_PEAK - 1f) * pulse;

            if (popTimeLeft == 0f) {
                scale = 1f;
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {

        if (value <= 0) return;

        int index = value - 1;
        if (picked) index += images.length / 2;

        int left = x;
        int top = y;
        int right = x + size;
        int bottom = y + size;

        // Apply scale around center (simple pop effect)
        float cx = (left + right) * 0.5f;
        float cy = (top + bottom) * 0.5f;

        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);
        android.graphics.Rect dest = new android.graphics.Rect(
                (int) (-size * 0.5f),
                (int) (-size * 0.5f),
                (int) ( size * 0.5f),
                (int) ( size * 0.5f)
        );
        canvas.drawBitmap(images[index], null, dest, null);
        canvas.restore();
    }
}
