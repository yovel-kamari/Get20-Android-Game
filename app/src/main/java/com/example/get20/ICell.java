package com.example.get20;

import android.graphics.Canvas;

public interface ICell {

    // Update cell state (animations, effects)
    void update(float dt);

    // Draw the cell on the screen
    void draw(Canvas canvas);
}
