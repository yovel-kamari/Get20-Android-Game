package com.example.get20;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private TextView scoreText;
    private Bitmap[] images;
    private GameRepository repository;
    private boolean isDarkMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        repository = new GameRepository(this);

        scoreText = findViewById(R.id.scoreText);
        gameView = findViewById(R.id.gameView);
        TextView instructionText = findViewById(R.id.instructionText);
        LinearLayout scoreContainer = findViewById(R.id.scoreContainer);
        ImageButton restartButton = findViewById(R.id.restartButton);
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton themeButton = findViewById(R.id.themeButton);
        FrameLayout rootLayout = findViewById(R.id.rootLayout);

        loadImages();
        gameView.setImages(images);

        // Initial Dark Mode Setup
        applyDarkMode(rootLayout, gameView, scoreContainer, scoreText, themeButton, restartButton, homeButton);

        gameView.setScoreListener(score -> {
            scoreText.setText(String.valueOf(score));
            scoreText.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction(() ->
                    scoreText.animate().scaleX(1f).scaleY(1f).setDuration(100));

            if (score > 0 && instructionText.getVisibility() == View.VISIBLE) {
                instructionText.animate().alpha(0f).setDuration(400).withEndAction(() ->
                        instructionText.setVisibility(View.GONE));
            }
        });

        gameView.setGameOverListener(this::onGameOver);

        restartButton.setOnClickListener(v -> recreate());
        homeButton.setOnClickListener(v -> finish());

        themeButton.setOnClickListener(v -> {
            isDarkMode = !isDarkMode;
            if (isDarkMode) {
                applyDarkMode(rootLayout, gameView, scoreContainer, scoreText, themeButton, restartButton, homeButton);
            } else {
                applyLightMode(rootLayout, gameView, scoreContainer, scoreText, themeButton, restartButton, homeButton);
            }
        });
    }

    private void applyDarkMode(View root, GameView game, View scoreBox, TextView scoreTxt, ImageButton themeBtn, ImageButton restartBtn, ImageButton homeBtn) {
        root.setBackgroundResource(R.drawable.background_main);
        game.setBackgroundColorCustom(Color.TRANSPARENT);
        scoreBox.setBackgroundResource(R.drawable.stats_card);
        scoreTxt.setTextColor(0xFFFFD54F); // Gold
        
        int white = Color.WHITE;
        themeBtn.setBackgroundResource(R.drawable.stats_card);
        themeBtn.setColorFilter(white);
        restartBtn.setBackgroundResource(R.drawable.stats_card);
        restartBtn.setColorFilter(white);
        homeBtn.setBackgroundResource(R.drawable.stats_card);
        homeBtn.setColorFilter(white);
    }

    private void applyLightMode(View root, GameView game, View scoreBox, TextView scoreTxt, ImageButton themeBtn, ImageButton restartBtn, ImageButton homeBtn) {
        root.setBackground(null);
        root.setBackgroundColor(0xFFFAF6ED); // Cream
        game.setBackgroundColorCustom(0xFFFAF6ED);
        scoreBox.setBackgroundResource(R.drawable.stats_card_light);
        scoreTxt.setTextColor(0xFF004062); // Dark Blue
        
        int darkBlue = 0xFF004062;
        themeBtn.setBackgroundResource(R.drawable.stats_card_light);
        themeBtn.setColorFilter(darkBlue);
        restartBtn.setBackgroundResource(R.drawable.stats_card_light);
        restartBtn.setColorFilter(darkBlue);
        homeBtn.setBackgroundResource(R.drawable.stats_card_light);
        homeBtn.setColorFilter(darkBlue);
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
        int[] normal = {R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4, R.drawable.p5, R.drawable.p6, R.drawable.p7, R.drawable.p8, R.drawable.p9, R.drawable.p10, R.drawable.p11, R.drawable.p12, R.drawable.p13, R.drawable.p14, R.drawable.p15, R.drawable.p16, R.drawable.p17, R.drawable.p18, R.drawable.p19, R.drawable.p20};
        int[] selected = {R.drawable.p1a, R.drawable.p2a, R.drawable.p3a, R.drawable.p4a, R.drawable.p5a, R.drawable.p6a, R.drawable.p7a, R.drawable.p8a, R.drawable.p9a, R.drawable.p10a, R.drawable.p11a, R.drawable.p12a, R.drawable.p13a, R.drawable.p14a, R.drawable.p15a, R.drawable.p16a, R.drawable.p17a, R.drawable.p18a, R.drawable.p19a, R.drawable.p20a};
        images = new Bitmap[40];
        for (int i = 0; i < 20; i++) {
            images[i] = BitmapFactory.decodeResource(getResources(), normal[i], options);
            images[i + 20] = BitmapFactory.decodeResource(getResources(), selected[i], options);
        }
    }
}