package com.example.get20;

import android.graphics.*;

import java.util.*;

/**
 * Game logic: the grid, selecting and merging tiles, gravity, growing the board, and win/lose.
 */
public class Board {

    private static final int INITIAL_SIZE = 7;    // debug: start on 7x7 to demo win on largest board
    private static final int MAX_VALUE = 20;      // reaching this tile wins the game
    private static final int EXPAND_TO_6_AT = 10; // grow 5x5 -> 6x6 when a 10 is created
    private static final int EXPAND_TO_7_AT = 15; // grow 6x6 -> 7x7 when a 15 is created

    private Cell[][] board;
    private int boardSize, cellSize, score;
    private int highestTileValue = 0;             // highest tile ever this game (for the MAX TILE stat)
    private boolean gameOver, gameWon, hasSelection;
    private final Bitmap[] images;
    private Bitmap[] scaledImages;
    private final Random random = new Random();
    private int startX, startY;                   // top-left pixel of the grid on screen
    private final int screenWidth, screenHeight;
    private int selectedRow, selectedCol;         // the tile the merge result goes into
    private final ArrayList<int[]> selectedGroup = new ArrayList<>(); // currently selected tiles

    // fresh game - size 5
    public Board(int sw, int sh, Bitmap[] imgs) {
        this(sw, sh, imgs, INITIAL_SIZE, 0);
    }

    // load a saved game
    public Board(int sw, int sh, Bitmap[] imgs, int size, int initialScore) {
        this.screenWidth = sw;
        this.screenHeight = sh;
        this.images = imgs;
        this.boardSize = size;
        this.score = initialScore;
        initBoard();
    }

    private void updateHighestTile(int val) {
        if (val > highestTileValue) highestTileValue = Math.min(val, MAX_VALUE);
    }

    public void restoreHighestTile(int val) { // used when loading a saved game
        updateHighestTile(val);
    }

    private int getRandomInitialValue() {
        int val = 19; // debug: always spawn 19s so first merge triggers win at 20
        updateHighestTile(val);
        return val;
    }

    private void initBoard() {
        calculateLayout();
        rescaleImages();
        board = new Cell[boardSize][boardSize];
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                int val = getRandomInitialValue();
                board[i][j] = new Cell(val, startX + j * cellSize, startY + i * cellSize, cellSize);
                board[i][j].setImages(scaledImages);
                // -1100 = high they start, i*150 = lower rows start higher, random to spread out
                board[i][j].startDropFrom(-1100f - (i * 150f) - (random.nextFloat() * 400f));
            }
        }
    }

    // scale every image to the current cell size, used when the board resizes 5x5 -> 6x6
    private void rescaleImages() {
        Bitmap[] previous = scaledImages;          // keep the old copies so we can free them after
        scaledImages = new Bitmap[images.length];
        for (int i = 0; i < images.length; i++)
            scaledImages[i] = Bitmap.createScaledBitmap(images[i], cellSize, cellSize, true);
        if (previous != null)
            for (int i = 0; i < previous.length; i++)
                // free the old copies to save memory, but not the original images
                if (previous[i] != null && previous[i] != images[i]) previous[i].recycle();
    }

    // save the board as comma-separated values
    public String getBoardData() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                sb.append(board[i][j].getValue()).append(",");
            }
        }
        return sb.toString();
    }

    // read values from a saved string to restore the board state
    public void loadBoardData(String data) {
        if (data == null || data.isEmpty()) return;
        String[] values = data.split(",");
        int idx = 0;
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (idx < values.length) {
                    int val;
                    try {
                        val = Integer.parseInt(values[idx++].trim());
                    } catch (NumberFormatException e) {
                        val = getRandomInitialValue(); // bad data -> use a random tile
                    }
                    board[i][j].setValue(val);
                    updateHighestTile(val);
                }
            }
        }
    }

    // fit the grid to the screen and center it
    private void calculateLayout() {
        cellSize = Math.min((int) (screenWidth * 0.95f), (int) (screenHeight * 0.75f)) / boardSize;
        startX = (screenWidth - boardSize * cellSize) / 2;
        startY = (screenHeight - boardSize * cellSize) / 2;
    }

    public void handleTouch(float x, float y) {
        int col = (int) ((x - startX) / cellSize);
        int row = (int) ((y - startY) / cellSize);

        // ignore taps outside the grid or on empty cells
        if (row < 0 || row >= boardSize || col < 0 || col >= boardSize || board[row][col].getValue() == 0)
            return;

        // check if a group is already selected from a previous tap
        if (hasSelection) {
            // search: is this tap within the selected group?
            boolean clickedInGroup = false;
            for (int[] p : selectedGroup) {
                if (p[0] == row && p[1] == col) {
                    clickedInGroup = true;
                    break;
                }
            }
            // second tap: if we clicked inside the group again - trigger merge
            if (clickedInGroup) {
                selectedRow = row;
                selectedCol = col;
                mergeGroup();
                return;
            }
        }
        // first tap: select a new group of connected equal tiles
        selectGroup(row, col);
    }

    private void selectGroup(int r, int c) {
        clearSelection();
        ArrayList<int[]> group = findGroup(r, c);
        if (group.size() < 2) return; // need at least 2 tiles to be a group
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

    // BFS: spread out from (row,col) to all touching tiles with the same value
    private ArrayList<int[]> findGroup(int row, int col) {
        ArrayList<int[]> group = new ArrayList<>();
        boolean[][] visited = new boolean[boardSize][boardSize];
        int val = board[row][col].getValue();
        Queue<int[]> q = new ArrayDeque<>(); // ArrayDeque: O(1) add/remove
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // down, up, right, left
        // start with the current tile
        q.add(new int[]{row, col});
        visited[row][col] = true;
        // process all tiles in queue until empty
        while (!q.isEmpty()) {
            int[] curr = q.remove();
            group.add(curr);

            // check all 4 neighbors (down, up, right, left)
            for (int[] d : dirs) {
                // calculate neighbor row and column
                int nr = curr[0] + d[0], nc = curr[1] + d[1];

                // is neighbor: in bounds, unvisited, and has same value?
                if (nr >= 0 && nr < boardSize && nc >= 0 && nc < boardSize &&
                        !visited[nr][nc] && board[nr][nc].getValue() == val) {
                    // mark visited and enqueue for processing
                    visited[nr][nc] = true;
                    q.add(new int[]{nr, nc});
                }
            }
        }
        return group;
    }

    // find the first group of 2+ same tiles (used to show a hint)
    public ArrayList<int[]> getFirstAvailableGroup() {
        boolean[][] checked = new boolean[boardSize][boardSize];
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (checked[i][j] || board[i][j].getValue() == 0) continue;
                ArrayList<int[]> group = findGroup(i, j);
                if (group.size() >= 2) return group;
                checked[i][j] = true; // isolated tile, skip it next time
            }
        }
        return null;
    }

    public void setHintGroup(ArrayList<int[]> group, boolean isHint) {
        if (group == null) return;
        for (int[] p : group) board[p[0]][p[1]].setHinted(isHint);
    }

    private void mergeGroup() {
        int newVal = Math.min(board[selectedRow][selectedCol].getValue() + 1, MAX_VALUE); // value + 1, capped at 20
        score += selectedGroup.size() * newVal; // points = group size * new value
        board[selectedRow][selectedCol].setValue(newVal);
        board[selectedRow][selectedCol].startPop(); // animate the merge
        for (int[] p : selectedGroup) // empty all the other tiles in the group
            if (p[0] != selectedRow || p[1] != selectedCol) board[p[0]][p[1]].setValue(0);

        clearSelection();
        applyGravity(); // tiles fall to fill the gaps
        fillEmpty();    // new tiles drop in on top
        updateHighestTile(newVal);

        if (newVal == MAX_VALUE) { // reached 20 -> win
            gameWon = true;
            gameOver = true;
            return;
        }
        // grow the board at 10 or 15
        if (newVal >= EXPAND_TO_6_AT && boardSize == 5) expand(6);
        else if (newVal >= EXPAND_TO_7_AT && boardSize == 6) expand(7);
        if (!hasMove()) gameOver = true; // no moves left -> lose
    }

    // drop tiles down each column to fill empty gaps
    private void applyGravity() {
        for (int col = 0; col < boardSize; col++) {
            int write = boardSize - 1; // next free slot, starts at the bottom
            for (int row = boardSize - 1; row >= 0; row--) { // scan bottom to top, looking for non-empty tiles
                if (board[row][col].getValue() != 0) {
                    board[write][col].setValue(board[row][col].getValue()); // move tile down to write
                    if (write != row)
                        board[row][col].setValue(0); // clear old spot (skip if tile didn't move)
                    write--; // move write pointer up
                }
            }
        }
    }

    private void fillEmpty() {
        for (Cell[] rowArr : board)
            for (Cell cell : rowArr)
                if (cell.getValue() == 0) {
                    cell.setValue(getRandomInitialValue());
                    cell.startDropFrom(-1000f - random.nextFloat() * 400f);
                }
    }

    // grow the board from NxN to (N+1)x(N+1), preserving all existing tile values:
    //   O O O O O        N N N N N N
    //   O O O O O   ->   O O O O O N
    //   O O O O O        O O O O O N
    //   O O O O O        O O O O O N
    //   O O O O O        O O O O O N
    //                    O O O O O N
    // O = existing tile (shifted down one row), N = new random tile
    private void expand(int n) {
        Cell[][] old = board;
        int oldS = boardSize;
        boardSize = n;
        calculateLayout();     // recalculate cell size and grid position for the new size
        rescaleImages();       // resize tile images to fit the new cell size
        board = new Cell[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // i>0 (not top row), i<=oldS (within old bounds), j<oldS (not right column)
                boolean isNewTile = !(i > 0 && i <= oldS && j < oldS);
                int val;
                if (isNewTile) {
                    val = getRandomInitialValue();      // new tile -> random value 1-4
                } else {
                    val = old[i - 1][j].getValue();    // old tile -> copy from previous board
                }
                board[i][j] = new Cell(val, startX + j * cellSize, startY + i * cellSize, cellSize);
                board[i][j].setImages(scaledImages);
                if (isNewTile) board[i][j].startDropFrom(-1500f - (random.nextFloat() * 500f));
            }
        }
    }

    // is there any neighbouring equal pair left? if not, the game is over
    private boolean hasMove() {
        for (int i = 0; i < boardSize; i++)
            for (int j = 0; j < boardSize; j++) {
                int v = board[i][j].getValue();
                if (v == 0) continue;
                // only check right and down - left and up were already checked in previous iterations
                if ((j + 1 < boardSize && board[i][j + 1].getValue() == v) || // right neighbor has same value?
                    (i + 1 < boardSize && board[i + 1][j].getValue() == v))    // bottom neighbor has same value?
                    return true;
            }
        return false;
    }

    public void draw(Canvas c) {
        for (Cell[] rowArr : board) for (Cell cell : rowArr) cell.draw(c);
    }

    public void update(float dt) {
        for (Cell[] rowArr : board) for (Cell cell : rowArr) cell.update(dt);
    }

    public int getScore() {
        return score;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isGameWon() {
        return gameWon;
    }

    public int getHighestTileValue() {
        return highestTileValue;
    }
}
