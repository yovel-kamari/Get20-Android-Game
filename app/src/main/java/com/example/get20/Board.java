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

    private Bitmap[] images;        // original images (256x256)
    private Bitmap[] scaledImages;  // scaled once to cellSize

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
        scaleImagesOnce();   // scale bitmaps once according to cellSize
        createEmptyBoard();
        fillBoardRandom();
    }

    private void calculateLayout() {

        // Use 95% of the smallest screen dimension
        int usableWidth = (int)(screenWidth * 0.95f);
        int usableHeight = (int)(screenHeight * 0.75f);

        int boardArea = Math.min(usableWidth, usableHeight);

        cellSize = boardArea / boardSize;

        int boardWidth = boardSize * cellSize;
        int boardHeight = boardSize * cellSize;

        // Center horizontally
        startX = (screenWidth - boardWidth) / 2;

        // Center vertically
        startY = (screenHeight - boardHeight) / 2;
    }

    // Scale all images once to match current cellSize
    private void scaleImagesOnce() {
        scaledImages = new Bitmap[images.length];

        for (int i = 0; i < images.length; i++) {
            scaledImages[i] = Bitmap.createScaledBitmap(
                    images[i],
                    cellSize,
                    cellSize,
                    true
            );
        }
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

                board[i][j] = new Cell(value, x, y, cellSize, scaledImages);
            }
        }
    }

    private int getRandomValue() {
        return random.nextInt(3) + 1;
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

        // Re-scale images according to new cellSize
        scaleImagesOnce();

        board = new Cell[boardSize][boardSize];

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {

                int x = startX + j * cellSize;
                int y = startY + i * cellSize;

                int value;

                // Shift old board down by 1 row so new row appears at the top
                if (i > 0 && i <= oldSize && j < oldSize) {
                    value = oldBoard[i - 1][j].getValue();
                } else {
                    value = getRandomValue();
                }

                board[i][j] = new Cell(value, x, y, cellSize, scaledImages);
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

    private int[] findCellByTouch(float touchX, float touchY) {
        float localX = touchX - startX;
        float localY = touchY - startY;

        float boardPixelSize = boardSize * cellSize;

        // Bounding-box check
        if (localX < 0 || localY < 0 ||
                localX >= boardPixelSize || localY >= boardPixelSize) {
            return null;
        }

        int col = (int)(localX / cellSize);
        int row = (int)(localY / cellSize);

        return new int[]{row, col};
    }

    private void selectGroup(int row, int col) {
        clearSelection();

        ArrayList<int[]> group = bfsGroupPositions(row, col);

        // Only select if group size >= 2
        if (group.size() < 2) {
            hasSelection = false;
            return;
        }

        for (int[] cellPosition : group) {
            board[cellPosition[0]][cellPosition[1]].setPicked(true);
        }

        hasSelection = true;
        selectedRow = row;
        selectedCol = col;
        selectedGroup = group;
    }

    private void clearSelection() {
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
                {1, 0}, {-1, 0}, {0, 1}, {0, -1}
        };

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{row, col});
        visited[row][col] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int r = current[0];
            int c = current[1];

            if (board[r][c].getValue() == 0) continue;

            group.add(new int[]{r, c});

            for (int[] offset : neighborOffsets) {
                int nr = r + offset[0];
                int nc = c + offset[1];

                if (isInsideBoard(nr, nc) &&
                        !visited[nr][nc] &&
                        board[nr][nc].getValue() == targetValue) {

                    visited[nr][nc] = true;
                    queue.add(new int[]{nr, nc});
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
        int newValue = Math.min(baseValue + 1, 20);

        // Update score (simple formula)
        score += selectedGroup.size() * newValue;

        board[selectedRow][selectedCol].setValue(newValue);
        board[selectedRow][selectedCol].startPop();

        for (int[] cellPosition : selectedGroup) {
            int r = cellPosition[0];
            int c = cellPosition[1];

            if (r == selectedRow && c == selectedCol) continue;

            board[r][c].setValue(0);
        }

        clearSelection();

        applyGravity();
        fillEmptyCells();

        handleNewValue(newValue);

        if (!hasAvailableMove()) {
            gameOver = true;
        }
    }

    private void applyGravity() {
        for (int col = 0; col < boardSize; col++) {
            int writeRow = boardSize - 1;

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
                if (value == 0) continue;

                if (j + 1 < boardSize &&
                        board[i][j + 1].getValue() == value) return true;

                if (i + 1 < boardSize &&
                        board[i + 1][j].getValue() == value) return true;
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

    public void update(float dt) {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j].update(dt);
            }
        }
    }

    public int getScore() { return score; }
    public boolean isGameOver() {return gameOver;}

    public int getMaxValueOnBoard() {return maxValueOnBoard;}
}