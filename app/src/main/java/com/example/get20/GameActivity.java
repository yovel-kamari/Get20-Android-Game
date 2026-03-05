package com.example.get20;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private TextView scoreText;
    private Bitmap[] images;
    private GameRepository repository;
    private boolean isDarkMode = true;
    private ObjectAnimator hintAnimator;
    private View instructionContainer;
    
    private final Handler hintHandler = new Handler(Looper.getMainLooper());
    private Runnable hintRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        repository = new GameRepository(this);

        scoreText = findViewById(R.id.scoreText);
        gameView = findViewById(R.id.gameView);
        instructionContainer = findViewById(R.id.instructionContainer);
        LinearLayout scoreContainer = findViewById(R.id.scoreContainer);
        ImageButton restartButton = findViewById(R.id.restartButton);
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton themeButton = findViewById(R.id.themeButton);
        FrameLayout rootLayout = findViewById(R.id.rootLayout);

        loadImages();
        gameView.setImages(images);

        applyDarkMode(rootLayout, gameView, scoreContainer, scoreText, themeButton, restartButton, homeButton);
        playEntranceAnimation(scoreContainer, homeButton, restartButton);
        
        // Initial hint setup
        setupHintTimer();
        // Show hint immediately on start
        showTutorialHint();
        gameView.showHint();

        gameView.setScoreListener(score -> {
            resetHintTimer(); // Reset the 10s timer on any activity
            
            scoreText.setText(String.valueOf(score));
            scoreText.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction(() ->
                    scoreText.animate().scaleX(1f).scaleY(1f).setDuration(100));

            // Hide hint message and stop highlighting if a move was made
            hideTutorialHint();
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

    private void setupHintTimer() {
        hintRunnable = () -> {
            if (!isFinishing()) {
                showTutorialHint();
                gameView.showHint(); // Highlight a group on the board
            }
        };
        resetHintTimer();
    }

    private void resetHintTimer() {
        hintHandler.removeCallbacks(hintRunnable);
        hintHandler.postDelayed(hintRunnable, 10000); // 10 seconds of inactivity
    }

    private void showTutorialHint() {
        if (instructionContainer.getVisibility() == View.GONE) {
            instructionContainer.setVisibility(View.VISIBLE);
            instructionContainer.setAlpha(0f);
            instructionContainer.setScaleX(0.8f);
            instructionContainer.setScaleY(0.8f);
        }
        
        instructionContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start();
                
        if (hintAnimator == null || !hintAnimator.isRunning()) {
            hintAnimator = ObjectAnimator.ofFloat(instructionContainer, "alpha", 0.6f, 1.0f);
            hintAnimator.setDuration(1000);
            hintAnimator.setRepeatMode(ValueAnimator.REVERSE);
            hintAnimator.setRepeatCount(ValueAnimator.INFINITE);
            hintAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            hintAnimator.start();
        }
    }

    private void hideTutorialHint() {
        if (instructionContainer.getVisibility() == View.VISIBLE) {
            if (hintAnimator != null) hintAnimator.cancel();
            gameView.clearHint(); // Remove board highlighting
            instructionContainer.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(400)
                    .withEndAction(() -> instructionContainer.setVisibility(View.GONE));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hintHandler.removeCallbacks(hintRunnable);
    }

    private void playEntranceAnimation(View score, View home, View restart) {
        score.setTranslationY(-300f);
        home.setTranslationY(-300f);
        restart.setTranslationY(-300f);
        score.setAlpha(0f);
        home.setAlpha(0f);
        restart.setAlpha(0f);

        score.animate().translationY(0f).alpha(1f).setDuration(700).setStartDelay(200).setInterpolator(new DecelerateInterpolator()).start();
        home.animate().translationY(0f).alpha(1f).setDuration(700).setStartDelay(400).setInterpolator(new DecelerateInterpolator()).start();
        restart.animate().translationY(0f).alpha(1f).setDuration(700).setStartDelay(500).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void applyDarkMode(View root, GameView game, View scoreBox, TextView scoreTxt, ImageButton themeBtn, ImageButton restartBtn, ImageButton homeBtn) {
        root.setBackgroundResource(R.drawable.background_main);
        game.setBackgroundColorCustom(Color.TRANSPARENT);
        scoreBox.setBackgroundResource(R.drawable.stats_card);
        scoreTxt.setTextColor(0xFFFFD54F);
        int white = Color.WHITE;
        themeBtn.setBackgroundResource(R.drawable.stats_card);
        themeBtn.setColorFilter(white);
        restartBtn.setBackgroundResource(R.drawable.stats_card);
        restartBtn.setColorFilter(white);
        homeBtn.setBackgroundResource(R.drawable.stats_card);
        homeBtn.setColorFilter(white);

        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setStatusBarColor(Color.BLACK);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightNavigationBars(false);
        controller.setAppearanceLightStatusBars(false);
    }

    private void applyLightMode(View root, GameView game, View scoreBox, TextView scoreTxt, ImageButton themeBtn, ImageButton restartBtn, ImageButton homeBtn) {
        int creamColor = 0xFFFAF6ED;
        root.setBackground(null);
        root.setBackgroundColor(creamColor); 
        game.setBackgroundColorCustom(creamColor);
        scoreBox.setBackgroundResource(R.drawable.stats_card_light);
        scoreTxt.setTextColor(0xFF004062);
        int darkBlue = 0xFF004062;
        themeBtn.setBackgroundResource(R.drawable.stats_card_light);
        themeBtn.setColorFilter(darkBlue);
        restartBtn.setBackgroundResource(R.drawable.stats_card_light);
        restartBtn.setColorFilter(darkBlue);
        homeBtn.setBackgroundResource(R.drawable.stats_card_light);
        homeBtn.setColorFilter(darkBlue);

        getWindow().setNavigationBarColor(creamColor);
        getWindow().setStatusBarColor(creamColor);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightNavigationBars(true);
        controller.setAppearanceLightStatusBars(true);
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