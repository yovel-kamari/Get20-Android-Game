package com.example.get20;

import android.graphics.*;
import java.util.*;

/**
 * Manages the game logic, including the grid state, merging tiles,
 * gravity physics, and game-over conditions.
 */
public class Board {
    private Cell[][] board; // 2D array representing the game grid
    private int boardSize = 5, cellSize, score, maxValueOnBoard = 0; // Initialized to 0 to track initial tiles
    private boolean gameOver, gameWon, hasSelection; // Added gameWon state
    private final Bitmap[] images;        // Original tile images
    private Bitmap[] scaledImages;  // Images resized to fit the current cellSize
    private final Random random = new Random();
    private int startX, startY;     // Offset coordinates to center the board on screen
    private final int screenWidth, screenHeight;
    private int selectedRow, selectedCol; // Coordinates of the destination tile for merging
    private final ArrayList<int[]> selectedGroup = new ArrayList<>(); // List of all tiles in the current selection

    public Board(int sw, int sh, Bitmap[] imgs) {
        this.screenWidth = sw; 
        this.screenHeight = sh; 
        this.images = imgs;
        initBoard();
    }

    /**
     * Probability logic: Returns 1-3 most of the time (90%), and 4 occasionally (10%).
     * Automatically updates the maxValueOnBoard if a higher tile is generated.
     */
    private int getRandomInitialValue() {
        int chance = random.nextInt(100);
        int val = (chance < 90) ? (random.nextInt(3) + 1) : 4;

        // Update max tile tracker
        if (val > maxValueOnBoard) maxValueOnBoard = val;

        return val;
    }

    /**
     * Initializes the board: calculates layout, scales images, and fills the grid.
     */
    private void initBoard() {
        calculateLayout();
        scaledImages = new Bitmap[images.length];
        for (int i = 0; i < images.length; i++) 
            scaledImages[i] = Bitmap.createScaledBitmap(images[i], cellSize, cellSize, true);
        
        board = new Cell[boardSize][boardSize];
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                // Tiles are created and tracked for maxValueOnBoard via getRandomInitialValue
                board[i][j] = new Cell(getRandomInitialValue(), startX + j * cellSize, startY + i * cellSize, cellSize);
                board[i][j].startDropAnimation(-1500f - (i * 400f) - (random.nextFloat() * 500f));
            }
        }
    }

    /**
     * Calculates optimal cell size and centers the board based on screen dimensions.
     */
    private void calculateLayout() {
        cellSize = Math.min((int)(screenWidth * 0.95f), (int)(screenHeight * 0.75f)) / boardSize;
        startX = (screenWidth - boardSize * cellSize) / 2;
        startY = (screenHeight - boardSize * cellSize) / 2;
    }

    public void handleTouch(float x, float y) {
        // Translate screen touch coordinates to grid indices
        int col = (int)((x - startX) / cellSize);
        int row = (int)((y - startY) / cellSize);

        // Bounds checking
        if (row < 0 || row >= boardSize || col < 0 || col >= boardSize || board[row][col].getValue() == 0) return;

        if (hasSelection) {
            // Check if the clicked tile is part of the currently selected group
            boolean clickedInGroup = false;
            for (int[] p : selectedGroup) {
                if (p[0] == row && p[1] == col) {
                    clickedInGroup = true;
                    break;
                }
            }

            if (clickedInGroup) {
                // Set the merge point to the tile that was clicked second
                selectedRow = row;
                selectedCol = col;
                mergeGroup();
                return;
            }
        }

        // If no selection exists or the user clicked a tile outside the group, start a new selection
        selectGroup(row, col);
    }

    private void selectGroup(int r, int c) {
        clearSelection();
        ArrayList<int[]> group = findGroup(r, c);
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

    private ArrayList<int[]> findGroup(int row, int col) {
        ArrayList<int[]> group = new ArrayList<>();
        boolean[][] visited = new boolean[boardSize][boardSize];
        int val = board[row][col].getValue();
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{row, col});
        visited[row][col] = true;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!q.isEmpty()) {
            int[] curr = q.poll();
            group.add(curr);
            for (int[] d : dirs) {
                int nr = curr[0]+d[0], nc = curr[1]+d[1];
                if (nr>=0 && nr<boardSize && nc>=0 && nc<boardSize && !visited[nr][nc] && board[nr][nc].getValue()==val) {
                    visited[nr][nc] = true;
                    q.add(new int[]{nr, nc});
                }
            }
        }
        return group;
    }

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

    public void setHintGroup(ArrayList<int[]> group, boolean isHint) {
        if (group == null) return;
        for (int[] p : group) board[p[0]][p[1]].setHinted(isHint);
    }

    private void mergeGroup() {
        int newVal = Math.min(board[selectedRow][selectedCol].getValue() + 1, 20);
        score += selectedGroup.size() * newVal;
        board[selectedRow][selectedCol].setValue(newVal); 
        board[selectedRow][selectedCol].startPop();
        for (int[] p : selectedGroup) if (p[0] != selectedRow || p[1] != selectedCol) board[p[0]][p[1]].setValue(0);

        clearSelection();
        applyGravity();
        fillEmpty();

        // Update the maximum value tracked on board
        if (newVal > maxValueOnBoard) maxValueOnBoard = Math.min(newVal, 20);
        
        // WIN CONDITION: Reached 20
        if (newVal == 20) {
            gameWon = true;
            gameOver = true;
            return;
        }

        if (newVal >= 10 && boardSize == 5) expand(6);
        else if (newVal >= 15 && boardSize == 6) expand(7);
        if (!hasMove()) gameOver = true;
    }

    private void applyGravity() {
        for (int col = 0; col < boardSize; col++) {
            int write = boardSize - 1;
            for (int row = boardSize - 1; row >= 0; row--) {
                if (board[row][col].getValue() != 0) {
                    board[write][col].setValue(board[row][col].getValue());
                    if (write != row) board[row][col].setValue(0);
                    write--;
                }
            }
        }
    }

    private void fillEmpty() {
        for (Cell[] rowArr : board) 
            for (Cell cell : rowArr) 
                if (cell.getValue() == 0) cell.setValue(getRandomInitialValue());
    }

    private void expand(int n) {
        Cell[][] old = board; int oldS = boardSize; boardSize = n; calculateLayout();
        scaledImages = new Bitmap[images.length];
        for (int i = 0; i < images.length; i++) scaledImages[i] = Bitmap.createScaledBitmap(images[i], cellSize, cellSize, true);

        board = new Cell[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int val = (i > 0 && i <= oldS && j < oldS) ? old[i - 1][j].getValue() : getRandomInitialValue();
                board[i][j] = new Cell(val, startX + j * cellSize, startY + i * cellSize, cellSize);
                // Track max value during grid expansion
                if (val > maxValueOnBoard) maxValueOnBoard = val;
            }
        }
    }

    private boolean hasMove() {
        for (int i = 0; i < boardSize; i++) for (int j = 0; j < boardSize; j++) {
            int v = board[i][j].getValue(); if (v == 0) continue;
            if ((j+1 < boardSize && board[i][j+1].getValue() == v) || (i+1 < boardSize && board[i+1][j].getValue() == v)) return true;
        }
        return false;
    }

    public void draw(Canvas c) { for (Cell[] rowArr : board) for (Cell cell : rowArr) cell.draw(c, scaledImages); }
    public void update(float dt) { for (Cell[] rowArr : board) for (Cell cell : rowArr) cell.update(dt); }
    public int getScore() { return score; }
    public boolean isGameOver() { return gameOver; }
    public boolean isGameWon() { return gameWon; } // Getter for win state
    public int getMaxValueOnBoard() { return maxValueOnBoard; }
}
