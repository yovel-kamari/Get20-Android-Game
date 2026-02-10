package com.example.get20;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import java.util.Random;

public class Board {

    private Cell[][] board;

    private int boardSize;
    private int cellSize;

    private int maxValueOnBoard;

    private boolean reached10;
    private boolean reached15;

    private Bitmap[] images;
    private Random random;

    private int startX;
    private int startY;

    private int screenWidth;
    private int screenHeight;


    public Board(int screenWidth, int screenHeight, Bitmap[] images) {
        this.boardSize = 5;
        this.maxValueOnBoard = 3;

        this.reached10 = false;
        this.reached15 = false;

        this.images = images;
        this.random = new Random();

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

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

        // Use smaller dimension to ensure the square board always fits on screen
        int boardArea = Math.min(
                screenWidth,
                screenHeight - topSpace
        ) - 2 * margin;

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

                // Shift cell position based on its row and column
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


    // Expands the board to a new size
    private void expandBoard(int newSize) {
        if (newSize <= boardSize) {return;}

        int oldSize = boardSize;
        Cell[][] oldBoard = board;

        boardSize = newSize;

        // Recalculate new board size
        calculateLayout();

        // Create new board with updated size
        board = new Cell[boardSize][boardSize];

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {

                // Shift cell position based on its row and column
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
    }


    // Draws the board
    public void draw(Canvas canvas) {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j].draw(canvas);
            }
        }
    }

    // Updates the board
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
}
