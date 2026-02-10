package com.example.get20;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class Cell implements ICell {

    private int value;
    private boolean picked;

    private int x;
    private int y;
    private int size;

    private Bitmap[] images;

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

    @Override
    public void update() {
        // Reserved for future cell state updates (animations, effects)
    }

    @Override
    public void draw(Canvas canvas) {
        int index = value - 1;

        // If the cell is selected, use the "selected" image
        if (picked) {
            index += images.length / 2;
        }

        canvas.drawBitmap(images[index], x, y, null);
    }
}
