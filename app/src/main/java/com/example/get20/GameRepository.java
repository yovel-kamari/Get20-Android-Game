package com.example.get20;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Responsible for permanent data storage using SharedPreferences.
 * Stores the player's high score and the maximum tile value reached.
 */
public class GameRepository {

    private static final String PREF_NAME = "get20_prefs";
    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_MAX_TILE = "max_tile";

    private final SharedPreferences preferences;

    /**
     * Initializes the repository.
     * @param context Application context needed to access shared preferences.
     */
    public GameRepository(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * @return The highest score saved in storage. Defaults to 0.
     */
    public int getHighScore() {
        return preferences.getInt(KEY_HIGH_SCORE, 0);
    }

    /**
     * Saves a new score if it surpasses the existing high score.
     * @param score The score to evaluate for saving.
     */
    public void saveHighScore(int score) {
        if (score > getHighScore()) {
            preferences.edit()
                    .putInt(KEY_HIGH_SCORE, score)
                    .apply(); // 'apply' is asynchronous and preferred for UI thread
        }
    }

    /**
     * @return The highest tile value ever achieved.
     */
    public int getMaxTile() {
        return preferences.getInt(KEY_MAX_TILE, 0);
    }

    /**
     * Saves a new max tile value if it surpasses the existing one.
     * @param value The tile value to evaluate for saving.
     */
    public void saveMaxTile(int value) {
        if (value > getMaxTile()) {
            preferences.edit()
                    .putInt(KEY_MAX_TILE, value)
                    .apply();
        }
    }

    /**
     * Wipes all saved progress (Score and Max Tile).
     */
    public void resetAllStats() {
        preferences.edit()
                .putInt(KEY_HIGH_SCORE, 0)
                .putInt(KEY_MAX_TILE, 0)
                .apply();
    }
}
