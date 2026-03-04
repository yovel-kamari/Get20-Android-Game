package com.example.get20;

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

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private GameRepository repository;
    private TextView highScoreText;
    private TextView maxTileText;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new GameRepository(this);

        highScoreText = findViewById(R.id.txtHighScore);
        maxTileText = findViewById(R.id.txtMaxTile);
        Button playButton = findViewById(R.id.playButton);

        // Background views for animation
        View logoGlow = findViewById(R.id.logoGlow);
        View tile1 = findViewById(R.id.bgTile1);
        View tile2 = findViewById(R.id.bgTile2);
        View tile3 = findViewById(R.id.bgTile3);
        View tile4 = findViewById(R.id.bgTile4);
        View tile5 = findViewById(R.id.bgTile5);

        updateStatsDisplay();
        
        // Start Animations with much larger movement and diagonal paths
        startIntenseAnimation(tile1, 150f, 100f, 15f, 5000);
        startIntenseAnimation(tile2, -120f, -80f, -20f, 4500);
        startIntenseAnimation(tile3, 180f, 50f, 10f, 6000);
        startIntenseAnimation(tile4, -140f, 120f, 25f, 5500);
        startIntenseAnimation(tile5, 130f, -150f, -15f, 4800);
        
        startGlowPulse(logoGlow);

        // Press effect for play button
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

        playButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });
    }

    private void startIntenseAnimation(View view, float transY, float transX, float rotation, int duration) {
        // Y Movement
        ObjectAnimator animY = ObjectAnimator.ofFloat(view, "translationY", -transY, transY);
        animY.setDuration(duration);
        animY.setRepeatMode(ValueAnimator.REVERSE);
        animY.setRepeatCount(ValueAnimator.INFINITE);
        animY.setInterpolator(new AccelerateDecelerateInterpolator());
        animY.start();

        // X Movement
        ObjectAnimator animX = ObjectAnimator.ofFloat(view, "translationX", -transX, transX);
        animX.setDuration(duration + 500); // Slightly different timing for more organic feel
        animX.setRepeatMode(ValueAnimator.REVERSE);
        animX.setRepeatCount(ValueAnimator.INFINITE);
        animX.setInterpolator(new AccelerateDecelerateInterpolator());
        animX.start();

        // Rotation Movement
        ObjectAnimator animRot = ObjectAnimator.ofFloat(view, "rotation", view.getRotation() - rotation, view.getRotation() + rotation);
        animRot.setDuration(duration + 1000);
        animRot.setRepeatMode(ValueAnimator.REVERSE);
        animRot.setRepeatCount(ValueAnimator.INFINITE);
        animRot.setInterpolator(new AccelerateDecelerateInterpolator());
        animRot.start();
    }

    private void startGlowPulse(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 0.05f, 0.15f);
        animator.setDuration(2000);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    private void updateStatsDisplay() {
        int highScore = repository.getHighScore();
        int maxTile = repository.getMaxTile();
        highScoreText.setText(String.valueOf(highScore));
        maxTileText.setText("🥇 MAX TILE: " + maxTile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatsDisplay();
    }
}