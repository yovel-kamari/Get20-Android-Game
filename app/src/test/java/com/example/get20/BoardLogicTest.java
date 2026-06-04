package com.example.get20;

import static org.junit.Assert.*;

import android.graphics.Bitmap;

import org.junit.Test;

import java.util.ArrayList;

/**
 * Local JVM unit tests for the pure game logic in {@link Board}.
 *
 * The board is built with a dummy image array; the only Android dependency in the
 * construction path is Bitmap.createScaledBitmap, which returns null under
 * testOptions.unitTests.returnDefaultValues = true, so no device/emulator is needed.
 */
public class BoardLogicTest {

    private static final int SCREEN = 1000;          // any positive size works for the layout math
    private static final Bitmap[] DUMMY_IMAGES = new Bitmap[40]; // 40 nulls - never drawn during tests

    private Board newBoard() {
        return new Board(SCREEN, SCREEN, DUMMY_IMAGES);
    }

    /** Build a CSV (row-major) of the given values to feed loadBoardData(). */
    private String csv(int[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(values[i]);
        }
        return sb.toString();
    }

    // Verifies that a fresh game starts with score 0, not over and not won.
    @Test
    public void newGame_startsWithZeroScore_andIsNotOver() {
        Board b = newBoard();
        assertEquals(0, b.getScore());
        assertFalse(b.isGameOver());
        assertFalse(b.isGameWon());
    }

    // Verifies that the board starts at the initial 5x5 size.
    @Test
    public void newGame_startsAsFiveByFive() {
        assertEquals(5, newBoard().getBoardSize());
    }

    // Verifies that saving and loading the board state preserves the tile values.
    @Test
    public void boardData_roundTripsThroughSaveAndLoad() {
        Board b = newBoard();
        int n = b.getBoardSize() * b.getBoardSize(); // 25
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = (i % 4) + 1; // any valid tile values

        b.loadBoardData(csv(values));

        // getBoardData() appends a comma after every cell, so expected ends with one too
        assertEquals(csv(values) + ",", b.getBoardData());
    }

    // Verifies the BFS group detection: an all-equal board is one connected group of 25 tiles.
    @Test
    public void bfs_findsTheWholeBoardWhenAllTilesAreEqual() {
        Board b = newBoard();
        int n = b.getBoardSize() * b.getBoardSize(); // 25
        int[] allFives = new int[n];
        for (int i = 0; i < n; i++) allFives[i] = 5;
        b.loadBoardData(csv(allFives));

        ArrayList<int[]> group = b.getFirstAvailableGroup();
        assertNotNull("an all-equal board is one connected group", group);
        assertEquals(n, group.size()); // every tile is reachable from any other
    }

    // Verifies the BFS group detection: when no neighbours are equal, there is no mergeable group.
    @Test
    public void bfs_returnsNullWhenNoTwoNeighboursAreEqual() {
        Board b = newBoard();
        int size = b.getBoardSize();
        int[] values = new int[size * size];
        // pattern where every 4-neighbour differs: value depends on (row + col)
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                values[i * size + j] = ((i + j) % 4) + 1;
        b.loadBoardData(csv(values));

        assertNull("no adjacent equal tiles means no mergeable group",
                b.getFirstAvailableGroup());
    }
}
