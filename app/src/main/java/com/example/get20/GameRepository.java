package com.example.get20;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists game data across app sessions using SharedPreferences.
 * Stores: high score, max tile, dark/light theme, and the current board state (so the game can be resumed).
 * Used by both MainActivity (to display stats) and GameActivity (to save and restore the game).
 */
public class GameRepository {

    private static final String PREF_NAME = "get20_prefs";
    private static final String KEY_HIGH_SCORE = "high_score";
    private static final String KEY_MAX_TILE = "max_tile";
    private static final String KEY_IS_DARK_MODE = "is_dark_mode";

    // keys for the saved (resumable) game
    private static final String KEY_BOARD_SIZE = "board_size";
    private static final String KEY_CURRENT_SCORE = "current_score";
    private static final String KEY_SAVED_MAX_VALUE = "saved_max_value";
    private static final String KEY_BOARD_DATA = "board_data";
    private static final String KEY_HAS_SAVED_GAME = "has_saved_game";

    private final SharedPreferences preferences;

    public GameRepository(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getHighScore() {
        return preferences.getInt(KEY_HIGH_SCORE, 0);
    }

    public void saveHighScore(int score) {
        if (score > getHighScore()) { // only keep the best
            preferences.edit().putInt(KEY_HIGH_SCORE, score).apply();
        }
    }

    public int getMaxTile() {
        return preferences.getInt(KEY_MAX_TILE, 0);
    }

    public void saveMaxTile(int value) {
        if (value > getMaxTile()) { // only keep the best
            preferences.edit().putInt(KEY_MAX_TILE, value).apply();
        }
    }

    public void setDarkMode(boolean isDark) {
        preferences.edit().putBoolean(KEY_IS_DARK_MODE, isDark).apply();
    }

    public boolean isDarkMode() {
        return preferences.getBoolean(KEY_IS_DARK_MODE, true); // dark by default
    }

    // save the current game so it can be resumed later
    public void saveGameState(int boardSize, int score, int maxValue, String boardData) {
        preferences.edit()
                .putInt(KEY_BOARD_SIZE, boardSize)
                .putInt(KEY_CURRENT_SCORE, score)
                .putInt(KEY_SAVED_MAX_VALUE, maxValue)
                .putString(KEY_BOARD_DATA, boardData)
                .putBoolean(KEY_HAS_SAVED_GAME, true)
                .apply();
    }

    public boolean hasSavedGame() {
        return preferences.getBoolean(KEY_HAS_SAVED_GAME, false);
    }

    public int getSavedBoardSize() {
        return preferences.getInt(KEY_BOARD_SIZE, 5);
    }

    public int getSavedScore() {
        return preferences.getInt(KEY_CURRENT_SCORE, 0);
    }

    public int getSavedMaxValue() {
        return preferences.getInt(KEY_SAVED_MAX_VALUE, 0);
    }

    public String getSavedBoardData() {
        return preferences.getString(KEY_BOARD_DATA, "");
    }

    public void clearSavedGame() {
        preferences.edit().putBoolean(KEY_HAS_SAVED_GAME, false).apply();
    }
}
