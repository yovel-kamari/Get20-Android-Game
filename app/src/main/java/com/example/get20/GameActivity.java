package com.example.get20;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Random;

/** The game screen: sets up the board view, theme, score, save/restore, hints, and the end dialog. */
public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    private TextView scoreText;
    private GameRepository repository;
    private boolean isDarkMode;
    private ObjectAnimator hintAnimator;
    private View instructionContainer;
    private View winGlow;
    private FrameLayout rootLayout;

    private boolean isRestarting = false; // true while restarting, so onPause skips the auto-save

    private final Handler hintHandler = new Handler(Looper.getMainLooper());
    private Runnable hintRunnable;

    private static final int HINT_IDLE_DELAY_MS = 10000; // show a hint after this much inactivity
    private static final int CONFETTI_COUNT = 60;        // confetti pieces spawned on a win

    private static Bitmap[] cachedImages; // tiles decoded once, reused on restart/re-entry (see loadImages())

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // draw behind the system bars so the background fills the whole screen
        setContentView(R.layout.activity_game);

        repository = new GameRepository(this);
        isDarkMode = repository.isDarkMode();

        scoreText = findViewById(R.id.scoreText);
        gameView = findViewById(R.id.gameView);
        instructionContainer = findViewById(R.id.instructionContainer);
        winGlow = findViewById(R.id.winGlow);
        rootLayout = findViewById(R.id.rootLayout);

        LinearLayout scoreContainer = findViewById(R.id.scoreContainer);
        ImageButton restartButton = findViewById(R.id.restartButton);
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton themeButton = findViewById(R.id.themeButton);

        // pad content away from the system bars (the background still shows behind them)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        gameView.setImages(loadImages());

        // resume a saved game if there is one
        boolean hasSavedGame = repository.hasSavedGame() && !repository.getSavedBoardData().isEmpty();
        int initialScore = hasSavedGame ? repository.getSavedScore() : 0; // score we start at
        if (hasSavedGame) {
            gameView.loadSavedGame(
                    repository.getSavedBoardSize(),
                    repository.getSavedScore(),
                    repository.getSavedMaxValue(),
                    repository.getSavedBoardData()
            );
            scoreText.setText(String.valueOf(repository.getSavedScore()));
        }

        if (isDarkMode) {
            applyDarkMode(rootLayout, gameView, scoreContainer, scoreText, themeButton, restartButton, homeButton);
        } else {
            applyLightMode(rootLayout, gameView, scoreContainer, scoreText, themeButton, restartButton, homeButton);
        }

        playEntranceAnimation(scoreContainer, homeButton, restartButton);

        setupHintTimer();
        if (initialScore == 0) showTutorialHint(); // only on a brand-new game
        gameView.post(() -> gameView.showHint());  // highlight a mergeable group once the board exists

        gameView.setScoreListener(score -> {
            resetHintTimer(); // keep the inactivity timer fresh
            scoreText.setText(String.valueOf(score));
            // small pop on the score text
            scoreText.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction(() ->
                    scoreText.animate().scaleX(1f).scaleY(1f).setDuration(100));
            hideTutorialHint(); // hide the help box once the player makes a move
        });

        gameView.setGameOverListener(this::onGameOver);

        restartButton.setOnClickListener(v -> {
            isRestarting = true;
            repository.clearSavedGame();
            recreate(); // rebuild the activity for a fresh game
        });

        homeButton.setOnClickListener(v -> finish());

        themeButton.setOnClickListener(v -> { // toggle and save dark/light
            isDarkMode = !isDarkMode;
            repository.setDarkMode(isDarkMode);
            if (isDarkMode) {
                applyDarkMode(rootLayout, gameView, scoreContainer, scoreText, themeButton, restartButton, homeButton);
            } else {
                applyLightMode(rootLayout, gameView, scoreContainer, scoreText, themeButton, restartButton, homeButton);
            }
        });
    }

    // auto-save when leaving the app, unless we're restarting or the game is already over
    @Override
    protected void onPause() {
        super.onPause();
        if (!isRestarting && gameView != null && gameView.getBoard() != null && !gameView.getBoard().isGameOver()) {
            repository.saveGameState(
                    gameView.getBoard().getBoardSize(),
                    gameView.getBoard().getScore(),
                    gameView.getBoard().getHighestTileValue(),
                    gameView.getBoard().getBoardData()
            );
        }
    }

    // called by GameView when the game ends - saves stats, triggers effects, and shows the dialog
    private void onGameOver(int score, int maxTile, boolean isWin) {
        // game over -> stop the hint system and drop the saved game
        hintHandler.removeCallbacks(hintRunnable);
        if (hintAnimator != null) hintAnimator.cancel();
        gameView.clearHint();
        repository.clearSavedGame();

        boolean isNewBest = score > repository.getHighScore();
        repository.saveHighScore(score);
        repository.saveMaxTile(maxTile);

        if (isWin) triggerWinEffects();

        // wait a moment on a win so the confetti starts before the dialog
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                showGameOverDialog(score, maxTile, isNewBest, isWin), isWin ? 1000 : 0);
    }

    // win: glow + falling confetti
    private void triggerWinEffects() {
        winGlow.setVisibility(View.VISIBLE);
        ObjectAnimator glowAnim = ObjectAnimator.ofFloat(winGlow, "alpha", 0f, 0.4f);
        glowAnim.setDuration(1000);
        glowAnim.setRepeatMode(ValueAnimator.REVERSE);
        glowAnim.setRepeatCount(3);
        glowAnim.start();

        final int[] colors = {Color.YELLOW, Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN};
        final Random rand = new Random();

        for (int i = 0; i < CONFETTI_COUNT; i++) {
            final View confetti = new View(this);
            int size = rand.nextInt(15) + 10;
            confetti.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            confetti.setBackgroundColor(colors[rand.nextInt(colors.length)]);
            confetti.setX(rand.nextInt(Math.max(1, rootLayout.getWidth())));
            confetti.setY(-100); // start above the screen
            confetti.setRotation(rand.nextInt(360));
            rootLayout.addView(confetti);

            confetti.animate()
                    .translationY(rootLayout.getHeight() + 200) // fall down
                    .rotationBy(rand.nextInt(1000) - 500)        // spin while falling
                    .setDuration(rand.nextInt(2000) + 2000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> rootLayout.removeView(confetti)) // remove when done
                    .start();
        }
    }

    // shows a dialog with the final score - title changes based on win, new record, or regular game over
    private void showGameOverDialog(int finalScore, int maxTile, boolean isNewBest, boolean isWin) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_game_over);
        if (dialog.getWindow() != null) { // transparent so the custom glass design shows
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);

        TextView title = dialog.findViewById(R.id.dialogTitle);
        TextView finalScoreText = dialog.findViewById(R.id.finalScoreText);
        TextView maxTileLabel = dialog.findViewById(R.id.maxTileLabel);
        Button backHome = dialog.findViewById(R.id.btnBackHome);

        finalScoreText.setText(String.valueOf(finalScore));
        maxTileLabel.setText(getString(R.string.dialog_max_tile, maxTile));

        // title depends on the result
        if (isWin) {
            title.setText(R.string.dialog_you_won);
            title.setTextColor(0xFFFFD54F);
        } else if (isNewBest) {
            title.setText(R.string.dialog_new_record);
            title.setTextColor(0xFFFFD54F);
        } else {
            title.setText(R.string.dialog_game_over);
            title.setTextColor(Color.WHITE);
        }

        backHome.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    // starts an inactivity timer - if the player does nothing for 10 seconds, show a hint
    private void setupHintTimer() {
        hintRunnable = () -> {
            // never show a hint once the activity is closing or the game is over
            if (!isFinishing()
                    && gameView.getBoard() != null
                    && !gameView.getBoard().isGameOver()) {
                showTutorialHint();
                gameView.showHint();
            }
        };
        resetHintTimer();
    }

    // restart the inactivity countdown
    private void resetHintTimer() {
        hintHandler.removeCallbacks(hintRunnable);
        hintHandler.postDelayed(hintRunnable, HINT_IDLE_DELAY_MS);
    }

    // fades in the instruction box and pulses it to draw the player's attention
    private void showTutorialHint() {
        if (instructionContainer.getVisibility() == View.GONE) {
            instructionContainer.setVisibility(View.VISIBLE);
            instructionContainer.setAlpha(0f);
        }
        instructionContainer.animate().alpha(1f).setDuration(500).start();

        if (hintAnimator == null || !hintAnimator.isRunning()) {
            // pulse the help badge to draw attention
            hintAnimator = ObjectAnimator.ofFloat(instructionContainer, "alpha", 0.6f, 1.0f);
            hintAnimator.setDuration(1000);
            hintAnimator.setRepeatMode(ValueAnimator.REVERSE);
            hintAnimator.setRepeatCount(ValueAnimator.INFINITE);
            hintAnimator.start();
        }
    }

    // fades out and hides the instruction box once the player makes their first move
    private void hideTutorialHint() {
        if (instructionContainer.getVisibility() == View.VISIBLE) {
            if (hintAnimator != null) hintAnimator.cancel();
            gameView.clearHint();
            instructionContainer.animate().alpha(0f).setDuration(400)
                    .withEndAction(() -> instructionContainer.setVisibility(View.GONE));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hintHandler.removeCallbacks(hintRunnable); // avoid leaks when the activity closes
        if (hintAnimator != null) hintAnimator.cancel();
    }

    // slide the top UI in from above on entry
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

    // dark theme colours (and dark system-bar icons)
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

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightNavigationBars(false);
        controller.setAppearanceLightStatusBars(false);
    }

    // light theme colours (and light system-bar icons)
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

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightNavigationBars(true);
        controller.setAppearanceLightStatusBars(true);
    }

    // decode the 40 tile images once, then reuse the cached array (avoids re-decoding on every restart)
    private Bitmap[] loadImages() {
        if (cachedImages != null) return cachedImages;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565; // half the memory of the default
        int[] normal = {R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4, R.drawable.p5, R.drawable.p6, R.drawable.p7, R.drawable.p8, R.drawable.p9, R.drawable.p10, R.drawable.p11, R.drawable.p12, R.drawable.p13, R.drawable.p14, R.drawable.p15, R.drawable.p16, R.drawable.p17, R.drawable.p18, R.drawable.p19, R.drawable.p20};
        int[] selected = {R.drawable.p1a, R.drawable.p2a, R.drawable.p3a, R.drawable.p4a, R.drawable.p5a, R.drawable.p6a, R.drawable.p7a, R.drawable.p8a, R.drawable.p9a, R.drawable.p10a, R.drawable.p11a, R.drawable.p12a, R.drawable.p13a, R.drawable.p14a, R.drawable.p15a, R.drawable.p16a, R.drawable.p17a, R.drawable.p18a, R.drawable.p19a, R.drawable.p20a};
        Bitmap[] images = new Bitmap[40];
        for (int i = 0; i < 20; i++) {
            images[i] = BitmapFactory.decodeResource(getResources(), normal[i], options);
            images[i + 20] = BitmapFactory.decodeResource(getResources(), selected[i], options);
        }
        cachedImages = images;
        return cachedImages;
    }
}
