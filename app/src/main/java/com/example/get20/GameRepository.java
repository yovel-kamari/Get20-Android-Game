package com.example.get20;

import android.content.Context;
import android.content.SharedPreferences;

public class GameRepository {

    private static final String PREF_NAME = "get20_prefs";

    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_MAX_TILE = "max_tile";

    // SharedPreferences instance used for persistent storage
    private SharedPreferences preferences;

    // Constructor receives Context to access application storage
    public GameRepository(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Returns highest score ever achieved
    public int getHighScore() {
        return preferences.getInt(KEY_HIGH_SCORE, 0);
    }

    // Saves new high score if it is greater than the current one
    public void saveHighScore(int score) {
        int currentHigh = getHighScore();

        if (score > currentHigh) {
            preferences.edit()
                    .putInt(KEY_HIGH_SCORE, score)
                    .apply();
        }
    }

    // Returns highest tile value ever reached
    public int getMaxTile() {
        return preferences.getInt(KEY_MAX_TILE, 0);
    }

    // Saves new max tile if it is greater than the current one
    public void saveMaxTile(int value) {
        int current = getMaxTile();

        if (value > current) {
            preferences.edit()
                    .putInt(KEY_MAX_TILE, value)
                    .apply();
        }
    }

    // Resets only high score
    public void resetHighScore() {
        preferences.edit()
                .putInt(KEY_HIGH_SCORE, 0)
                .apply();
    }

    // Resets both high score and max tile
    public void resetAllStats() {
        preferences.edit()
                .putInt(KEY_HIGH_SCORE, 0)
                .putInt(KEY_MAX_TILE, 0)
                .apply();
    }
}