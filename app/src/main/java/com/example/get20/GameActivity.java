package com.example.get20;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private GamePanel gamePanel;
    private TextView scoreText;

    private Bitmap[] images;
    private boolean gameOverDialogShown = false;
    private GameRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadImages();

        // Initialize persistent storage (High Score + Max Tile)
        repository = new GameRepository(this);

        // Root layout
        FrameLayout layout = new FrameLayout(this);

        // Current score display only
        scoreText = new TextView(this);

        scoreText.setText("Score: 0");
        scoreText.setTextSize(30);
        scoreText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

// Force visible text color (not theme-dependent)
        scoreText.setTextColor(0xFF111111);

// Center text
        scoreText.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);

// Make it look like a HUD card
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFFFFFFFF);          // solid white card
        bg.setCornerRadius(28f);          // rounded corners
        scoreText.setBackground(bg);

// Padding inside the card
        scoreText.setPadding(48, 26, 48, 26);

// Position top-center
        FrameLayout.LayoutParams scoreParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        scoreParams.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        scoreParams.topMargin = 24;       // pushes it below status bar a bit
        scoreText.setLayoutParams(scoreParams);

        // Create GamePanel (View + Game Engine bridge)
        gamePanel = new GamePanel(this, this, images);

        // Add views (GamePanel background, then score overlay)
        layout.addView(gamePanel);

        setContentView(layout);
    }

    // Load all number images (normal + selected)
    private void loadImages() {

        // Array contains:
        // index 0-19  -> normal tiles (1..20)
        // index 20-39 -> selected tiles (1a..20a)
        images = new Bitmap[40];

        BitmapFactory.Options options = new BitmapFactory.Options();

        // Prevent automatic scaling based on screen density
        options.inScaled = false;

        // Use RGB_565 to reduce memory usage (no alpha channel needed)
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        for (int i = 1; i <= 20; i++) {

            @SuppressLint("DiscouragedApi")
            int normalResId = getResources().getIdentifier(
                    "p" + i,
                    "drawable",
                    getPackageName()
            );

            @SuppressLint("DiscouragedApi")
            int selectedResId = getResources().getIdentifier(
                    "p" + i + "a",
                    "drawable",
                    getPackageName()
            );

            if (normalResId == 0 || selectedResId == 0) {
                throw new RuntimeException("Missing drawable resource for tile " + i);
            }

            Bitmap normal = BitmapFactory.decodeResource(getResources(), normalResId, options);
            Bitmap selected = BitmapFactory.decodeResource(getResources(), selectedResId, options);

            if (normal == null || selected == null) {
                throw new RuntimeException("Failed to decode bitmap for tile " + i);
            }

            // Store original bitmaps without scaling.
            // Scaling will be handled dynamically inside Cell.draw()
            images[i - 1] = normal;
            images[i - 1 + 20] = selected;
        }
    }

    // Called from GamePanel when board expands
    public void onBoardExpanded(int newSize) {

        runOnUiThread(() -> {

            Toast toast = Toast.makeText(
                    this,
                    "Board expanded to " + newSize + "x" + newSize,
                    Toast.LENGTH_SHORT
            );

            // Force toast to appear in the center of the screen
            toast.setGravity(android.view.Gravity.CENTER, 0, 0);

            toast.show();
        });
    }

    // Called when game ends (Score + Max Tile of current game)
    public void onGameOver(int finalScore, int maxTile) {

        if (gameOverDialogShown) return;
        gameOverDialogShown = true;

        // Save persistent records
        repository.saveHighScore(finalScore);
        repository.saveMaxTile(maxTile);

        runOnUiThread(() -> {

            new AlertDialog.Builder(this)
                    .setTitle("Game Over")
                    .setMessage(
                            "Score: " + finalScore +
                                    "\nMax Tile: " + maxTile
                    )
                    .setCancelable(false)
                    .setPositiveButton("Back to Menu", (dialog, which) -> {
                        finish();
                    })
                    .show();
        });
    }

    // Update current score during gameplay
    public void updateScore(int score) {
        runOnUiThread(() ->
                scoreText.setText("Score: " + score)
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        gamePanel.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gamePanel.resumeGame();
    }
}