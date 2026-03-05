package com.example.get20;

import android.graphics.*;
import java.util.*;

/**
 * Manages the game logic, including the grid state, merging tiles, 
 * gravity physics, and game-over conditions.
 */
public class Board {
    private Cell[][] board; // 2D array representing the game grid
    private int boardSize = 5, cellSize, score, maxValueOnBoard = 3;
    private boolean gameOver, hasSelection;
    private final Bitmap[] images;        // Original tile images
    private Bitmap[] scaledImages;  // Images resized to fit the current cellSize
    private final Random random = new Random();
    private int startX, startY;     // Offset coordinates to center the board on screen
    private final int screenWidth, screenHeight;
    private int selectedRow, selectedCol; // Coordinates of the currently selected tile
    private final ArrayList<int[]> selectedGroup = new ArrayList<>(); // List of all tiles in the current selection

    public Board(int sw, int sh, Bitmap[] imgs) {
        this.screenWidth = sw; 
        this.screenHeight = sh; 
        this.images = imgs;
        initBoard();
    }

    /**
     * Initializes the board: calculates layout, scales images, and fills the grid.
     */
    private void initBoard() {
        calculateLayout();
        scaledImages = new Bitmap[images.length];
        // Scale bitmaps once to save memory and processing power
        for (int i = 0; i < images.length; i++) 
            scaledImages[i] = Bitmap.createScaledBitmap(images[i], cellSize, cellSize, true);
        
        board = new Cell[boardSize][boardSize];
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                // Create a new cell with a random value (1-3)
                board[i][j] = new Cell(random.nextInt(3) + 1, startX + j * cellSize, startY + i * cellSize, cellSize);
                // Start the drop animation: higher rows start from higher up for a staggered effect
                board[i][j].startDropAnimation(-1500f - (i * 400f) - (random.nextFloat() * 500f));
            }
        }
    }

    /**
     * Calculates optimal cell size and centers the board based on screen dimensions.
     */
    private void calculateLayout() {
        // Fits the board within 95% of width or 75% of height
        cellSize = Math.min((int)(screenWidth * 0.95f), (int)(screenHeight * 0.75f)) / boardSize;
        // Centers the board by calculating remaining space
        startX = (screenWidth - boardSize * cellSize) / 2;
        startY = (screenHeight - boardSize * cellSize) / 2;
    }

    /**
     * Handles touch input: converts screen coordinates to board indices (row/col).
     */
    public void handleTouch(float x, float y) {
        // Translate screen touch coordinates to grid indices
        int col = (int)((x - startX) / cellSize);
        int row = (int)((y - startY) / cellSize);
        
        // Bounds checking
        if (row < 0 || row >= boardSize || col < 0 || col >= boardSize || board[row][col].getValue() == 0) return;

        // If clicking the same already selected tile -> Merge the group
        if (hasSelection && row == selectedRow && col == selectedCol) mergeGroup();
        // If clicking a different tile -> Select that tile's group
        else selectGroup(row, col);
    }

    /**
     * Selects a group of identical adjacent tiles.
     */
    private void selectGroup(int r, int c) {
        clearSelection();
        ArrayList<int[]> group = findGroup(r, c);
        // Only select if there are at least 2 connected tiles
        if (group.size() < 2) return; 
        
        for (int[] p : group) board[p[0]][p[1]].setPicked(true);
        hasSelection = true; 
        selectedRow = r; 
        selectedCol = c; 
        selectedGroup.addAll(group);
    }

    private void clearSelection() {
        for (Cell[] row : board) for (Cell cell : row) cell.setPicked(false);
        hasSelection = false; 
        selectedRow = -1;
        selectedCol = -1;
        selectedGroup.clear();
    }

    /**
     * BFS (Breadth-First Search) algorithm to find all connected tiles with the same value.
     */
    private ArrayList<int[]> findGroup(int row, int col) {
        ArrayList<int[]> group = new ArrayList<>();
        boolean[][] visited = new boolean[boardSize][boardSize];
        int val = board[row][col].getValue();
        Queue<int[]> q = new LinkedList<>(); 
        q.add(new int[]{row, col});
        visited[row][col] = true;
        
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}}; // Neighbors: Down, Up, Right, Left
        while (!q.isEmpty()) {
            int[] curr = q.poll(); 
            group.add(curr);
            for (int[] d : dirs) {
                int nr = curr[0]+d[0], nc = curr[1]+d[1];
                // Check if neighbor is within bounds, not visited, and has the same value
                if (nr>=0 && nr<boardSize && nc>=0 && nc<boardSize && !visited[nr][nc] && board[nr][nc].getValue()==val) {
                    visited[nr][nc] = true; 
                    q.add(new int[]{nr, nc});
                }
            }
        }
        return group;
    }

    /**
     * Finds the first available group of at least 2 tiles to show as a hint.
     */
    public ArrayList<int[]> getFirstAvailableGroup() {
        boolean[][] checked = new boolean[boardSize][boardSize];
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (checked[i][j] || board[i][j].getValue() == 0) continue;
                ArrayList<int[]> group = findGroup(i, j);
                if (group.size() >= 2) return group;
                for (int[] p : group) checked[p[0]][p[1]] = true;
            }
        }
        return null;
    }

    /**
     * Sets or clears the hint state for a group of tiles.
     */
    public void setHintGroup(ArrayList<int[]> group, boolean isHint) {
        if (group == null) return;
        for (int[] p : group) {
            board[p[0]][p[1]].setHinted(isHint);
        }
    }

    /**
     * Merges the selected group into the main tile, updates score, and applies gravity.
     */
    private void mergeGroup() {
        int newVal = Math.min(board[selectedRow][selectedCol].getValue() + 1, 20);
        score += selectedGroup.size() * newVal;
        
        board[selectedRow][selectedCol].setValue(newVal); 
        board[selectedRow][selectedCol].startPop(); // Visual feedback
        
        // Remove all other tiles in the merged group
        for (int[] p : selectedGroup) if (p[0] != selectedRow || p[1] != selectedCol) board[p[0]][p[1]].setValue(0);
        
        clearSelection(); 
        applyGravity(); // Shift tiles down to fill gaps
        fillEmpty();    // Add new tiles at the top
        
        maxValueOnBoard = Math.max(maxValueOnBoard, newVal);
        // Expansion triggers at specific value milestones
        if (newVal >= 10 && boardSize == 5) expand(6); 
        else if (newVal >= 15 && boardSize == 6) expand(7);
        
        // Check if the game is over (no more moves left)
        if (!hasMove()) gameOver = true;
    }

    /**
     * Shifts all tiles down to the lowest possible empty spot in their column.
     */
    private void applyGravity() {
        for (int col = 0; col < boardSize; col++) {
            int write = boardSize - 1; // Tracks the next empty spot from the bottom
            for (int row = boardSize - 1; row >= 0; row--) {
                if (board[row][col].getValue() != 0) {
                    board[write][col].setValue(board[row][col].getValue());
                    // If the tile moved down, clear its old position
                    if (write != row) board[row][col].setValue(0);
                    write--;
                }
            }
        }
    }

    /**
     * Fills empty spaces (value 0) with new random tiles (values 1-3).
     */
    private void fillEmpty() {
        for (Cell[] rowArr : board) 
            for (Cell cell : rowArr) 
                if (cell.getValue() == 0) cell.setValue(random.nextInt(3) + 1);
    }

    /**
     * Resizes the board while preserving existing tile values.
     */
    private void expand(int n) {
        Cell[][] old = board; int oldS = boardSize; boardSize = n; calculateLayout();
        scaledImages = new Bitmap[images.length];
        for (int i = 0; i < images.length; i++) scaledImages[i] = Bitmap.createScaledBitmap(images[i], cellSize, cellSize, true);
        
        board = new Cell[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // Map old tiles to new grid (shifted down by 1 row) or fill new spots with random values
                int val = (i > 0 && i <= oldS && j < oldS) ? old[i - 1][j].getValue() : random.nextInt(3) + 1;
                board[i][j] = new Cell(val, startX + j * cellSize, startY + i * cellSize, cellSize);
            }
        }
    }

    /**
     * Scans the board for any adjacent tiles with matching values.
     */
    private boolean hasMove() {
        for (int i = 0; i < boardSize; i++) for (int j = 0; j < boardSize; j++) {
            int v = board[i][j].getValue(); if (v == 0) continue;
            // Check right and down neighbors for a match
            if ((j+1 < boardSize && board[i][j+1].getValue() == v) || (i+1 < boardSize && board[i+1][j].getValue() == v)) return true;
        }
        return false;
    }

    public void draw(Canvas c) { for (Cell[] rowArr : board) for (Cell cell : rowArr) cell.draw(c, scaledImages); }
    public void update(float dt) { for (Cell[] rowArr : board) for (Cell cell : rowArr) cell.update(dt); }
    public int getScore() { return score; }
    public boolean isGameOver() { return gameOver; }
    public int getMaxValueOnBoard() { return maxValueOnBoard; }
}
