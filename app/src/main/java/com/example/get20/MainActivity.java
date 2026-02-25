package com.example.get20;

import android.content.Intent;
import android.os.Bundle;
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

        // Initialize persistent storage handler
        repository = new GameRepository(this);

        // Initialize persistent storage handler
        highScoreText = findViewById(R.id.highScoreText);
        maxTileText = findViewById(R.id.maxTileText);
        Button playButton = findViewById(R.id.playButton);

        updateStatsDisplay();

        // Reads stored values and updates the UI
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