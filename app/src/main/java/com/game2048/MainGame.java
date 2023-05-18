package com.game2048;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.game2048.AI.AI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainGame {
    public boolean isAIRunning = false;
    public static final int SPAWN_ANIMATION = -1;
    public static final int MOVE_ANIMATION = 0;
    public static final int MERGE_ANIMATION = 1;

    public static final int FADE_GLOBAL_ANIMATION = 0;

    public static long MOVE_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    public static final long SPAWN_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    public static final long NOTIFICATION_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME * 5;
    public static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME;
    private static final String HIGH_SCORE = "high score";


    public static final int GAME_LOST = -1;
    public static final int GAME_NORMAL = 0;

    public Grid grid = null;
    public AnimationGrid aGrid;
    public int numSquaresX;
    public int numSquaresY;
    public int startTiles;

    public int gameState = 0;

    public long score = 0;
    public long highScore = 0;

    public List<Long> lastScore = new ArrayList<>();
    public int lastGameState = 0;

    private long bufferScore = 0;

    public int TimeMoved = 0;
    public int poss;
    public boolean soundIsOn;
    private final Context mContext;

    private final MainView mView;
    private int numTilesAddedPerMove;
    SharedPreferences settings = null;
    private MediaPlayer mediaPlayer;

    static {
        System.loadLibrary("2048");
    }

    public MainGame(Context context, MainView view) {
        mContext = context;
        mView = view;
    }

    private static native int nativeGetBestMove(int[][] grid);

    private static native void nativeSetMaxDepth(int depth);

    private boolean considerFour;
    private boolean usePowerfulAI = false;
    private long lastPowerfulAIMoveTime = 0;
    private final Handler gHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {

            if (isAIRunning) {

                new Thread(() -> {
                    if (usePowerfulAI) {
                       final long stopTime = lastPowerfulAIMoveTime + timeIntervalPerMove;
                        while (System.nanoTime() < stopTime) ;
                        final int direction = nativeGetBestMove(grid.getCellMatrix());
                        lastPowerfulAIMoveTime = System.nanoTime();
                        ((Activity) mContext).runOnUiThread(() -> {
                            move(direction);
                            continueRunAi();
                        });
                        return;
                    }
                    final int direction = new AI(grid.getCellMatrix(), smoothWeight, monoWeight, emptyWeight, maxWeight, considerFour).getBestMove(timeIntervalPerMove);
                    ((Activity) mContext).runOnUiThread(() -> {
                        move(direction);
                        continueRunAi();
                    });
                }).start();

            } else {
                mView.setOnTouchListener(new InputListener(mView));
            }
        }
    };


    public void newGame() {
        if (settings == null) {
            settings = PreferenceManager.getDefaultSharedPreferences(mContext);
            numSquaresX = settings.getInt("x", 4);
            numSquaresY = settings.getInt("y", 4);
        }
        numTilesAddedPerMove = settings.getInt("time", 1);
        startTiles = settings.getInt("start", 2);
        poss = settings.getInt("poss", 90);
        considerFour = settings.getBoolean("considerFour", true);
        soundIsOn = settings.getBoolean("soundIsOn", true);
        mediaPlayer = MediaPlayer.create(mContext, R.raw.sound);
        if (grid == null) {
            grid = new Grid(numSquaresX, numSquaresY);
        } else {
            prepareUndoState();
            saveUndoState();
            grid.clearGrid();
            grid.clearUndoList();
        }
        aGrid = new AnimationGrid(numSquaresX, numSquaresY);
        highScore = getHighScore();
        if (score >= highScore) {
            highScore = score;
            recordHighScore();
        }
        score = 0;
        gameState = GAME_NORMAL;
        addStartTiles();
        mView.refreshLastTime = true;
        mView.resyncTime();
        mView.invalidate();
        TimeMoved = 0;
        lastScore.clear();
        lastScore = new ArrayList<>();
    }


    private void addStartTiles() {
        for (int xx = 0; xx < startTiles; xx++) {
            addRandomTile();
        }
    }

    private void addRandomTile() {
        if (grid.isCellsAvailable()) {
            int value = Math.random() < poss / 100f ? 2 : 4;
            Tile tile = new Tile(grid.randomAvailableCell(), value);
            spawnTile(tile);
        }

    }

    private void spawnTile(Tile tile) {
        grid.insertTile(tile);
        aGrid.startAnimation(tile.getX(), tile.getY(), SPAWN_ANIMATION, SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null); // Direction:
        // -1 =
        // EXPANDING
    }

    private void recordHighScore() {
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putLong(HIGH_SCORE, highScore).apply();
    }

    private long getHighScore() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getLong(HIGH_SCORE, -1);
    }

    private void prepareTiles() {
        for (Tile[] array : grid.field) {
            for (Tile tile : array) {
                if (grid.isCellOccupied(tile)) {
                    tile.setMergedFrom(null);
                }
            }
        }
    }

    private void moveTile(Tile tile, Cell cell) {

        grid.field[tile.getX()][tile.getY()] = null;
        grid.field[cell.getX()][cell.getY()] = tile;
        tile.updatePosition(cell);
    }

    private void saveUndoState() {
        grid.saveTiles();
        lastScore.add(bufferScore);
        TimeMoved++;
    }

    // cheat remove 2
    public void cheat() {
        ArrayList<Cell> notAvailableCell = grid.getNotAvailableCells();
        Tile tile;
        prepareUndoState();
        for (Cell cell : notAvailableCell) {
            tile = grid.getCellContent(cell);
            if (2 == tile.getValue()) {
                grid.removeTile(tile);
                gameState = GAME_NORMAL;
            }
        }

        if (grid.getNotAvailableCells().size() == 0) {
            addStartTiles();
        }
        saveUndoState();
        mView.resyncTime();
        mView.invalidate();
    }

    private void prepareUndoState() {
        grid.prepareSaveTiles();
        bufferScore = score;
    }

    public void revertUndoState() {
        if (TimeMoved > 0) {
            aGrid.cancelAnimations();
            grid.revertTiles();
            TimeMoved--;
            score = lastScore.get(lastScore.size() - 1);
            lastScore.remove(lastScore.size() - 1);
            gameState = lastGameState;
            mView.refreshLastTime = true;
            mView.invalidate();
        }
    }

    public boolean gameLost() {
        return (gameState == GAME_LOST);
    }

    public boolean isActive() {
        return !gameLost();
    }

    public void move(int direction) {
        aGrid.cancelAnimations();
        // 0: up, 1: right, 2: down, 3: left
        if (!isActive()) {
            return;
        }
        prepareUndoState();
        Cell vector = getVector(direction);
        List<Integer> traversalsX = buildTraversalsX(vector);
        List<Integer> traversalsY = buildTraversalsY(vector);
        boolean moved = false;

        prepareTiles();

        for (int xx : traversalsX) {
            for (int yy : traversalsY) {
                Cell cell = new Cell(xx, yy);
                Tile tile = grid.getCellContent(cell);

                if (tile != null) {
                    Cell[] positions = findFarthestPosition(cell, vector);
                    Tile next = grid.getCellContent(positions[1]);

                    if (next != null && next.getValue() == tile.getValue() && next.getMergedFrom() == null) {

                        Tile merged = new Tile(positions[1], tile.getValue() * 2);
                        Tile[] temp = {tile, next};
                        merged.setMergedFrom(temp);

                        grid.insertTile(merged);
                        grid.removeTile(tile);

                        // Converge the two tiles' positions
                        tile.updatePosition(positions[1]);

                        int[] extras = {xx, yy};
                        aGrid.startAnimation(merged.getX(), merged.getY(), MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras); // Direction:
                        // 0
                        // =
                        // MOVING
                        // MERGED
                        aGrid.startAnimation(merged.getX(), merged.getY(), MERGE_ANIMATION, SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);

                        // Update the score
                        score += merged.getValue();
                        highScore = Math.max(score, highScore);

                        // The mighty 2048 tile

                    } else {
                        moveTile(tile, positions[0]);
                        int[] extras = {xx, yy, 0};
                        aGrid.startAnimation(positions[0].getX(), positions[0].getY(), MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras); // Direction: 1
                        // = MOVING
                        // NO MERGE
                    }

                    if (!positionsEqual(cell, tile)) {
                        moved = true;
                    }
                }
            }
        }

        if (moved) {
            if (soundIsOn) {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }
            if (!isAIRunning) saveUndoState();
            for (int i = 0; i < numTilesAddedPerMove; i++) {
                addRandomTile();
            }
            checkLose();
        }
        mView.resyncTime();
        mView.invalidate();

    }

    private void checkLose() {
        if (!movesAvailable()) {
            gameState = GAME_LOST;
            endGame();
        }
    }

    private void endGame() {
        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null);
        if (score >= highScore) {
            highScore = score;
            recordHighScore();
        }
    }

    private Cell getVector(int direction) {
        Cell[] map = {new Cell(0, -1), // up
                new Cell(1, 0), // right
                new Cell(0, 1), // down
                new Cell(-1, 0) // left
        };
        return map[direction];
    }

    private List<Integer> buildTraversalsX(Cell vector) {
        List<Integer> traversals = new ArrayList<>();

        for (int xx = 0; xx < numSquaresX; xx++) {
            traversals.add(xx);
        }
        if (vector.getX() == 1) {
            Collections.reverse(traversals);
        }

        return traversals;
    }

    private List<Integer> buildTraversalsY(Cell vector) {
        List<Integer> traversals = new ArrayList<>();

        for (int xx = 0; xx < numSquaresY; xx++) {
            traversals.add(xx);
        }
        if (vector.getY() == 1) {
            Collections.reverse(traversals);
        }

        return traversals;
    }

    private Cell[] findFarthestPosition(Cell cell, Cell vector) {
        Cell previous;
        Cell nextCell = new Cell(cell.getX(), cell.getY());
        do {
            previous = nextCell;
            nextCell = new Cell(previous.getX() + vector.getX(), previous.getY() + vector.getY());
        } while (grid.isCellWithinBounds(nextCell) && grid.isCellAvailable(nextCell));

        return new Cell[]{previous, nextCell};
    }

    private boolean movesAvailable() {
        return grid.isCellsAvailable() || tileMatchesAvailable();
    }

    private boolean tileMatchesAvailable() {
        Tile tile;

        for (int xx = 0; xx < numSquaresX; xx++) {
            for (int yy = 0; yy < numSquaresY; yy++) {
                tile = grid.getCellContent(new Cell(xx, yy));

                if (tile != null) {
                    for (int direction = 0; direction < 4; direction++) {
                        Cell vector = getVector(direction);
                        Cell cell = new Cell(xx + vector.getX(), yy + vector.getY());

                        Tile other = grid.getCellContent(cell);

                        if (other != null && other.getValue() == tile.getValue()) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean positionsEqual(Cell first, Cell second) {
        return first.getX() == second.getX() && first.getY() == second.getY();
    }


    public void openSetting(int x, int y) {
        mContext.startActivity(new Intent(mContext, SetActivity.class).putExtra("startX", x).putExtra("startY", y));
    }

    long timeIntervalPerMove;
    int smoothWeight, monoWeight, emptyWeight, maxWeight;

    public void runAi() {
        isAIRunning = true;
        Toast.makeText(mContext, R.string.press_anywhere_to_stop, Toast.LENGTH_SHORT).show();
        timeIntervalPerMove = settings.getInt("AItime", 100) * 1000000L;

        usePowerfulAI = (numSquaresY == 4 && numSquaresX == 4);
        if (usePowerfulAI) {
            nativeSetMaxDepth(settings.getInt("maxDepth", 5));
            lastPowerfulAIMoveTime = System.nanoTime();
        } else {
            considerFour = settings.getBoolean("considerFour", true);
            smoothWeight = settings.getInt("smooth", 1); //平滑性权重系数
            monoWeight = settings.getInt("mono", 40); //单调性权重系数
            emptyWeight = settings.getInt("empty", 27); //空格数权重系数
            maxWeight = settings.getInt("max", 10); //最大数权重系数
        }

        continueRunAi();
    }

    public void continueRunAi() {
        if (gameState == 0) {
            gHandler.sendMessage(new Message());
        } else stopAi();
    }

    public void stopAi() {
        Toast.makeText(mContext, R.string.stopped, Toast.LENGTH_SHORT).show();
        mView.setOnTouchListener(new InputListener(mView));
        isAIRunning = false;
    }
}