package com.example.get20;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    // Repository responsible for persistent storage (SharedPreferences)
    private GameRepository repository;
    // UI elements displaying stored statistics
    private TextView highScoreText;
    private TextView maxTileText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize repository
        repository = new GameRepository(this);

        // Find views
        highScoreText = findViewById(R.id.txtHighScore);
        maxTileText = findViewById(R.id.txtMaxTile);
        Button playButton = findViewById(R.id.playButton);

        // Start pulse animation
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        playButton.startAnimation(pulse);

        // Update UI with saved stats
        updateStatsDisplay();

        // Start game on click
        playButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });
    }

    // Reads stored values and updates the UI
    private void updateStatsDisplay() {
        int highScore = repository.getHighScore();
        int maxTile = repository.getMaxTile();

        highScoreText.setText("High Score: " + highScore);
        maxTileText.setText("Max Tile: " + maxTile);
    }
}