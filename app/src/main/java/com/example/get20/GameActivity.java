package com.example.get20;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private TextView scoreText;
    private Bitmap[] images;
    private GameRepository repository;
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        repository = new GameRepository(this);

        scoreText = findViewById(R.id.scoreText);
        gameView = findViewById(R.id.gameView);
        TextView instructionText = findViewById(R.id.instructionText);

        loadImages();
        gameView.setImages(images);

        gameView.setScoreListener(score -> {

            // Update only the number (no "Score" text)
            scoreText.setText("⭐ "+ score);

            // Small "pop" animation when score changes
            // Scale up quickly, then return to normal size
            scoreText.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(100)
                    .withEndAction(() ->
                            scoreText.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                    );

            // Hide instruction after first merge (score > 0)
            // This makes it smarter than using a fixed timer
            if (score > 0 && instructionText.getVisibility() == View.VISIBLE) {
                instructionText.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction(() ->
                                instructionText.setVisibility(View.GONE)
                        );
            }
        });

        gameView.setGameOverListener(this::onGameOver);

        ImageButton restartButton = findViewById(R.id.restartButton);
        ImageButton homeButton = findViewById(R.id.homeButton);

        restartButton.setOnClickListener(v -> recreate());
        homeButton.setOnClickListener(v -> finish());

        ImageButton themeButton = findViewById(R.id.themeButton);
        FrameLayout rootLayout = findViewById(R.id.rootLayout);

        themeButton.setOnClickListener(v -> {

            isDarkMode = !isDarkMode;

            if (isDarkMode) {
                rootLayout.setBackgroundColor(0xFF121212);
                themeButton.setColorFilter(0xFFFFFFFF);
            } else {
                rootLayout.setBackgroundColor(0xFFFAF6ED);
                themeButton.setColorFilter(0xFF004062);
            }
        });
    }

    private void onGameOver(int score, int maxTile) {
        repository.saveHighScore(score);
        repository.saveMaxTile(maxTile);

        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("Score: " + score + "\nMax Tile: " + maxTile)
                .setPositiveButton("Back to Home", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void loadImages() {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        int[] normal = {
                R.drawable.p1, R.drawable.p2, R.drawable.p3,
                R.drawable.p4, R.drawable.p5, R.drawable.p6,
                R.drawable.p7, R.drawable.p8, R.drawable.p9,
                R.drawable.p10, R.drawable.p11, R.drawable.p12,
                R.drawable.p13, R.drawable.p14, R.drawable.p15,
                R.drawable.p16, R.drawable.p17, R.drawable.p18,
                R.drawable.p19, R.drawable.p20
        };

        int[] selected = {
                R.drawable.p1a, R.drawable.p2a, R.drawable.p3a,
                R.drawable.p4a, R.drawable.p5a, R.drawable.p6a,
                R.drawable.p7a, R.drawable.p8a, R.drawable.p9a,
                R.drawable.p10a, R.drawable.p11a, R.drawable.p12a,
                R.drawable.p13a, R.drawable.p14a, R.drawable.p15a,
                R.drawable.p16a, R.drawable.p17a, R.drawable.p18a,
                R.drawable.p19a, R.drawable.p20a
        };

        images = new Bitmap[40];

        for (int i = 0; i < 20; i++) {
            images[i] = BitmapFactory.decodeResource(getResources(), normal[i], options);
            images[i + 20] = BitmapFactory.decodeResource(getResources(), selected[i], options);
        }
    }
}