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
        scoreText.setTextSize(24);
        scoreText.setPadding(40, 40, 40, 40);
        scoreText.setText("Score: 0");

        // Create GamePanel (View + Game Engine bridge)
        gamePanel = new GamePanel(this, this, images);

        // Add views (GamePanel background, then score overlay)
        layout.addView(gamePanel);
        layout.addView(scoreText);

        setContentView(layout);
    }

    // Load all number images (normal + selected)
    private void loadImages() {

        images = new Bitmap[40];

        // Estimate tile size (safe approximation before board is created)
        int targetSize = getResources().getDisplayMetrics().widthPixels / 5;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565; // Uses half the memory of ARGB_8888

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

            // Scale bitmaps to tile size
            images[i - 1] = Bitmap.createScaledBitmap(normal, targetSize, targetSize, true);
            images[i - 1 + 20] = Bitmap.createScaledBitmap(selected, targetSize, targetSize, true);

            // Free original full-size bitmaps
            normal.recycle();
            selected.recycle();
        }
    }

    // Called from GamePanel when board expands
    public void onBoardExpanded(int newSize) {

        runOnUiThread(() ->
                Toast.makeText(
                        this,
                        "Board expanded to " + newSize + "x" + newSize,
                        Toast.LENGTH_SHORT
                ).show()
        );
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