package com.example.get20;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * The app's entry screen.
 * Shows the player's high score and max tile, and a Play button to start the game.
 * Runs decorative animations in the background (floating tiles + logo glow).
 * Animations are paused when the app goes to the background to save battery.
 */
public class MainActivity extends AppCompatActivity {
    private GameRepository repository;
    private TextView highScoreText;
    private TextView maxTileText;

    private final List<Animator> animators = new ArrayList<>(); // looping decorations, paused in the background

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // draw behind the system bars so the background fills the whole screen
        setContentView(R.layout.activity_main);

        // add the system-bar insets on top of the layout's own padding (background still shows behind them)
        View mainRoot = findViewById(R.id.mainRoot);
        final int padL = mainRoot.getPaddingLeft();
        final int padT = mainRoot.getPaddingTop();
        final int padR = mainRoot.getPaddingRight();
        final int padB = mainRoot.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(padL + bars.left, padT + bars.top, padR + bars.right, padB + bars.bottom);
            return insets;
        });
        // menu background is always dark -> keep light (white) bar icons
        WindowInsetsControllerCompat barController =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        barController.setAppearanceLightStatusBars(false);
        barController.setAppearanceLightNavigationBars(false);

        repository = new GameRepository(this);

        highScoreText = findViewById(R.id.txtHighScore);
        maxTileText = findViewById(R.id.txtMaxTile);
        Button playButton = findViewById(R.id.playButton);

        View logoGlow = findViewById(R.id.logoGlow);
        View tile1 = findViewById(R.id.bgTile1);
        View tile2 = findViewById(R.id.bgTile2);
        View tile3 = findViewById(R.id.bgTile3);
        View tile4 = findViewById(R.id.bgTile4);
        View tile5 = findViewById(R.id.bgTile5);

        updateStatsDisplay();

        // floating background tiles. args: view, Y distance, X distance, rotation, duration
        startDecorativeAnimation(tile1, 150f, 100f, 15f, 5000);
        startDecorativeAnimation(tile2, -120f, -80f, -20f, 4500);
        startDecorativeAnimation(tile3, 180f, 50f, 10f, 6000);
        startDecorativeAnimation(tile4, -140f, 120f, 25f, 5500);
        startDecorativeAnimation(tile5, 130f, -150f, -15f, 4800);

        startGlowPulse(logoGlow);

        // shrink the Play button a bit while it's pressed
        playButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });

        playButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, GameActivity.class)));
    }

    // float a tile up/down, side to side, and rotate it - forever, with offset timings for a natural feel
    private void startDecorativeAnimation(View view, float transY, float transX, float rotation, int duration) {
        ObjectAnimator animY = ObjectAnimator.ofFloat(view, "translationY", -transY, transY);
        animY.setDuration(duration);
        animY.setRepeatMode(ValueAnimator.REVERSE);
        animY.setRepeatCount(ValueAnimator.INFINITE);
        animY.setInterpolator(new AccelerateDecelerateInterpolator());
        animY.start();

        ObjectAnimator animX = ObjectAnimator.ofFloat(view, "translationX", -transX, transX);
        animX.setDuration(duration + 500);
        animX.setRepeatMode(ValueAnimator.REVERSE);
        animX.setRepeatCount(ValueAnimator.INFINITE);
        animX.setInterpolator(new AccelerateDecelerateInterpolator());
        animX.start();

        ObjectAnimator animRot = ObjectAnimator.ofFloat(view, "rotation", view.getRotation() - rotation, view.getRotation() + rotation);
        animRot.setDuration(duration + 1000);
        animRot.setRepeatMode(ValueAnimator.REVERSE);
        animRot.setRepeatCount(ValueAnimator.INFINITE);
        animRot.setInterpolator(new AccelerateDecelerateInterpolator());
        animRot.start();

        animators.add(animY);
        animators.add(animX);
        animators.add(animRot);
    }

    // fade the logo glow in and out forever (a "breathing" effect)
    private void startGlowPulse(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 0.05f, 0.15f);
        animator.setDuration(2500);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        animators.add(animator);
    }

    // reads the latest stats from storage and updates the text on screen
    private void updateStatsDisplay() {
        highScoreText.setText(String.valueOf(repository.getHighScore()));
        maxTileText.setText(getString(R.string.menu_max_tile, repository.getMaxTile()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatsDisplay();                 // refresh stats (they may have changed in a game)
        for (Animator a : animators) if (a.isPaused()) a.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (Animator a : animators) if (a.isRunning()) a.pause(); // save battery while backgrounded
    }
}
