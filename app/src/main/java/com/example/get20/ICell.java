package com.example.get20;

import android.graphics.Canvas;

public interface ICell {

    // Update cell state (animations, effects)
    void update();

    // Draw the cell on the screen
    void draw(Canvas canvas);
}
