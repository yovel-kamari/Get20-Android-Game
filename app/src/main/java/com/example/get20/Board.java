package com.example.get20;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Board {

    private Cell[][] board;

    private int boardSize;
    private int cellSize;
    private int score;
    private int maxValueOnBoard;
    private boolean gameOver;
    private boolean expansionTriggered;
    private boolean reached10;
    private boolean reached15;

    private Bitmap[] images;
    private Random random;

    private int startX;
    private int startY;

    private int screenWidth;
    private int screenHeight;

    private boolean hasSelection; // True if a group is selected and waiting for second click (merge)
    private int selectedRow;
    private int selectedCol;
    private ArrayList<int[]> selectedGroup; // positions of selected cells (row,col)

    public Board(int screenWidth, int screenHeight, Bitmap[] images) {
        this.boardSize = 5;
        this.maxValueOnBoard = 3;

        this.reached10 = false;
        this.reached15 = false;

        this.images = images;
        this.random = new Random();

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        this.hasSelection = false;
        this.selectedRow = -1;
        this.selectedCol = -1;
        this.selectedGroup = new ArrayList<>();

        this.score = 0;
        this.gameOver = false;
        this.expansionTriggered = false;


        initBoard();
    }

    // Initializes the board with random values
    private void initBoard() {
        calculateLayout();
        createEmptyBoard();
        fillBoardRandom();
    }

    private void calculateLayout() {
        int margin = 40;
        int topSpace = 150;

        int boardArea = Math.min(screenWidth, screenHeight - topSpace) - 2 * margin;

        cellSize = boardArea / boardSize;

        startX = (screenWidth - (boardSize * cellSize)) / 2;
        startY = topSpace + margin;
    }

    private void createEmptyBoard() {
        board = new Cell[boardSize][boardSize];
    }

    private void fillBoardRandom() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {

                int value = getRandomValue();

                int x = startX + j * cellSize;
                int y = startY + i * cellSize;

                board[i][j] = new Cell(value, x, y, cellSize, images);
            }
        }
    }

    // Returns random value between 1 and maxValueOnBoard
    private int getRandomValue() {
        return random.nextInt(maxValueOnBoard) + 1;
    }

    // Handle the new value created after a merge
    public void handleNewValue(int value) {
        updateMaxValue(value);
        checkBoardExpansion(value);
    }

    public void updateMaxValue(int value) {
        if (value > maxValueOnBoard) {
            maxValueOnBoard = Math.min(value, 20);
        }
    }

    private void checkBoardExpansion(int value) {
        if (value >= 10 && !reached10) {
            reached10 = true;
            expandBoard(6);
        }

        if (value >= 15 && !reached15) {
            reached15 = true;
            expandBoard(7);
        }
    }

    private void expandBoard(int newSize) {
        if (newSize <= boardSize) {
            return;
        }
        expansionTriggered = true;

        int oldSize = boardSize;
        Cell[][] oldBoard = board;

        boardSize = newSize;

        // Recalculate layout for the new size
        calculateLayout();

        board = new Cell[boardSize][boardSize];

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {

                int x = startX + j * cellSize;
                int y = startY + i * cellSize;

                int value;
                if (i < oldSize && j < oldSize) {
                    value = oldBoard[i][j].getValue();
                } else {
                    value = getRandomValue();
                }

                board[i][j] = new Cell(value, x, y, cellSize, images);
            }
        }

        // Clear any selection after resizing
        clearSelection();
    }

    // -----------------------------
    // Touch handling (two-click logic)
    // -----------------------------

    public void handleTouch(float x, float y) {
        int[] pos = findCellByTouch(x, y);
        if (pos == null) {
            return;
        }

        int row = pos[0];
        int col = pos[1];

        // Ignore empty cells created temporarily during merge process
        if (board[row][col].getValue() == 0) {
            return;
        }

        // If we already have a selection
        if (hasSelection) {

            // Second click on the SAME main cell -> merge
            if (row == selectedRow && col == selectedCol) {
                mergeSelectedGroup();
                return;
            }

            // Clicked a different cell -> new selection
            selectGroup(row, col);
            return;
        }

        // No selection yet -> first click selects group
        selectGroup(row, col);
    }

    private int[] findCellByTouch(float x, float y) {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board[i][j].isInside(x, y)) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    private void selectGroup(int row, int col) {
        clearSelection();

        ArrayList<int[]> group = bfsGroupPositions(row, col);

        // Only select if group size >= 2
        if (group.size() < 2) {
            hasSelection = false;
            return;
        }

        // Mark all cells in group as picked (highlight)
        for (int[] cellPosition : group) {
            board[cellPosition[0]][cellPosition[1]].setPicked(true);
        }

        hasSelection = true;
        selectedRow = row;
        selectedCol = col;
        selectedGroup = group;
    }

    private void clearSelection() {
        // Clear picked state for all cells
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j].setPicked(false);
            }
        }

        hasSelection = false;
        selectedRow = -1;
        selectedCol = -1;
        selectedGroup.clear();
    }

    // BFS that returns positions (row,col) of all connected cells with same value
    private ArrayList<int[]> bfsGroupPositions(int row, int col) {
        ArrayList<int[]> group = new ArrayList<>();
        boolean[][] visited = new boolean[boardSize][boardSize];

        int targetValue = board[row][col].getValue();

        int[][] neighborOffsets = {
                {1, 0},   // down
                {-1, 0},  // up
                {0, 1},   // right
                {0, -1}   // left
        };

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{row, col});
        visited[row][col] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int currentRow = current[0];
            int currentCol = current[1];

            // Safety check: ignore empty cells
            if (board[currentRow][currentCol].getValue() == 0) {
                continue;
            }

            group.add(new int[]{currentRow, currentCol});


            for (int[] offset : neighborOffsets) {
                int neighborRow = currentRow + offset[0];
                int neighborCol = currentCol + offset[1];

                if (isInsideBoard(neighborRow, neighborCol) &&
                        !visited[neighborRow][neighborCol] &&
                        board[neighborRow][neighborCol].getValue() == targetValue) {

                    visited[neighborRow][neighborCol] = true;
                    queue.add(new int[]{neighborRow, neighborCol});
                }
            }

        }

        return group;
    }

    private boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < boardSize && col >= 0 && col < boardSize;
    }

    // -----------------------------
    // Merge + Gravity + Fill
    // -----------------------------

    private void mergeSelectedGroup() {
        if (!hasSelection || selectedGroup.size() < 2) {
            clearSelection();
            return;
        }

        int baseValue = board[selectedRow][selectedCol].getValue();
        int newValue = baseValue + 1;

        if (newValue > 20) {newValue = 20;}

        // Update score (simple formula)
        score += selectedGroup.size() * newValue;

        // Main cell gets the new value
        board[selectedRow][selectedCol].setValue(newValue);

        // All other cells in group become empty
        for (int[] cellPosition : selectedGroup) {

            int row = cellPosition[0];
            int col = cellPosition[1];

            if (row == selectedRow && col == selectedCol) {
                continue;
            }

            board[row][col].setValue(0);
        }

        clearSelection();

        // First: apply gravity
        applyGravity();

        // Then: fill empty cells
        fillEmptyCells();

        // Update max value and possibly expand board
        handleNewValue(newValue);

        // Check if no moves left
        if (!hasAvailableMove()) {
            gameOver = true;
        }
    }



    // Moves cells down in each column to fill empty spaces (value=0)
    private void applyGravity() {
        for (int col = 0; col < boardSize; col++) {
            // Points to the next row from the bottom where a non-empty value should be written
            int writeRow = boardSize - 1;

            // Move non-empty values down
            for (int row = boardSize - 1; row >= 0; row--) {
                int v = board[row][col].getValue();
                if (v != 0) {
                    if (writeRow != row) {
                        board[writeRow][col].setValue(v);
                        board[row][col].setValue(0);
                    }
                    writeRow--;
                }
            }
        }
    }

    // Fills empty cells (value=0) with new random values (1..maxValueOnBoard)
    private void fillEmptyCells() {
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (board[row][col].getValue() == 0) {
                    board[row][col].setValue(getRandomValue());
                }
            }
        }
    }

    public boolean hasAvailableMove() {

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {

                int value = board[i][j].getValue();
                // Safety check: ignore empty cells
                if (value == 0) continue;

                // Check right neighbor
                if (j + 1 < boardSize &&
                        board[i][j + 1].getValue() == value) {
                    return true;
                }

                // Check down neighbor
                if (i + 1 < boardSize &&
                        board[i + 1][j].getValue() == value) {
                    return true;
                }
            }
        }

        return false;
    }

    public void draw(Canvas canvas) {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j].draw(canvas);
            }
        }
    }

    public void update() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j].update();
            }
        }
    }

    public int getBoardSize() {
        return boardSize;
    }

    public int getMaxValueOnBoard() {
        return maxValueOnBoard;
    }

    public Cell[][] getBoard() {
        return board;
    }

    public int getScore() {return score;}

    public boolean isGameOver() {return gameOver;}

    public boolean isExpansionTriggered() {return expansionTriggered;}

    public void resetExpansionFlag() {expansionTriggered = false;}
    public int getMaxTileCurrentGame() {return maxValueOnBoard;}

}
