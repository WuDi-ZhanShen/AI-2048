package com.game2048.AI;

import java.util.ArrayList;
import java.util.List;

public class GameState {

    private final int[][] cellMatrix;

    public boolean playerTurn = true;

    private final int[][] vectors = {
            {0, -1}, // up
            {1, 0},  // right
            {0, 1},  // down
            {-1, 0}   // left
    };

    private boolean[][] marked;

    public GameState(int[][] grid) {
        this.cellMatrix = new int[grid.length][grid[0].length];
        for (int xx = 0; xx < grid.length; xx++) {
            System.arraycopy(grid[xx], 0, cellMatrix[xx], 0, grid[0].length);
        }

    }

    public int[][] getCellMatrix() {
        return cellMatrix;
    }

    private boolean isCellAvailable(int cnt_x, int cnt_y) {
        return cellMatrix[cnt_x][cnt_y] == 0;
    }

    private boolean isInBounds(int cnt_x, int cnt_y) {
        return cnt_x >= 0 && cnt_x < cellMatrix.length && cnt_y >= 0 && cnt_y < cellMatrix[0].length;
    }

    /**
     * 测量网格的平滑程度(这些块的值可以形象地解释为海拔)。
     * 相邻两个方块的值差异越小，格局就越平滑(在log空间中，所以它表示在合并之前需要进行的合并的数量)。
     */
    public double smoothness() {
        int smoothness = 0;
        for (int x = 0; x < cellMatrix.length; x++) {
            for (int y = 0; y < cellMatrix[0].length; y++) {
                if (this.cellMatrix[x][y] != 0) {
                    double value = Math.log(this.cellMatrix[x][y]) / Math.log(2);
                    // 计算水平方向和垂直方向的平滑性评估值
                    for (int direction = 1; direction <= 2; direction++) {
                        int[] vector = this.vectors[direction];
                        int cnt_x = x, cnt_y = y;
                        do {
                            cnt_x += vector[0];
                            cnt_y += vector[1];
                        } while (isInBounds(cnt_x, cnt_y) && isCellAvailable(cnt_x, cnt_y));
                        if (isInBounds(cnt_x, cnt_y)) {
                            if (cellMatrix[cnt_x][cnt_y] != 0) {
                                double targetValue = Math.log(cellMatrix[cnt_x][cnt_y]) / Math.log(2);
                                smoothness -= Math.abs(value - targetValue);
                            }
                        }
                    }
                }
            }
        }
        return smoothness;
    }

    /**
     * 测量网格的单调性。
     * 这意味着在向左/向右和向上/向下的方向，方块的值都是严格递增或递减的。
     */
    public double monotonicity() {
        // 保存四个方向格局单调性的评估值
        int[] totals = {0, 0, 0, 0};

        // 左/右 方向
        for (int[] matrix : cellMatrix) {
            int current = 0;
            int next = current + 1;
            while (next < cellMatrix[0].length) {
                while (next < cellMatrix[0].length && matrix[next] == 0) next++;
                if (next >= cellMatrix[0].length) next--;
                double currentValue = (matrix[current] != 0) ? Math.log(matrix[current]) / Math.log(2) : 0;
                double nextValue = (matrix[next] != 0) ? Math.log(matrix[next]) / Math.log(2) : 0;
                if (currentValue > nextValue) {
                    totals[0] += nextValue - currentValue;
                } else if (nextValue > currentValue) {
                    totals[1] += currentValue - nextValue;
                }
                current = next;
                next++;
            }
        }

        // 上/下 方向
        for (int y = 0; y < cellMatrix[0].length; y++) {
            int current = 0;
            int next = current + 1;
            while (next < cellMatrix.length) {
                while (next < cellMatrix.length && this.cellMatrix[next][y] == 0) next++;
                if (next >= cellMatrix.length) next--;
                double currentValue = (this.cellMatrix[current][y] != 0) ? Math.log(this.cellMatrix[current][y]) / Math.log(2) : 0;
                double nextValue = (this.cellMatrix[next][y] != 0) ? Math.log(this.cellMatrix[next][y]) / Math.log(2) : 0;
                if (currentValue > nextValue) {
                    totals[2] += nextValue - currentValue;
                } else if (nextValue > currentValue) {
                    totals[3] += currentValue - nextValue;
                }
                current = next;
                next++;
            }
        }

        // 取四个方向中最大的值为当前格局单调性的评估值
        return Math.max(totals[0], totals[1]) + Math.max(totals[2], totals[3]);
    }

    /**
     * 取最大数，这里取对数是为与前面其它指标的计算保持一致，均在log空间进行
     */
    public double maxValue() {
        int max = 0;
        for (int[] aMatrix : cellMatrix)
            for (int j = 0; j < cellMatrix[0].length; j++)
                if (aMatrix[j] > max) max = aMatrix[j];

        return Math.log(max) / Math.log(2);
    }

    private void merge(int[] row, int action) {

        int[] mergeRow = new int[row.length];
        System.arraycopy(row, 0, mergeRow, 0, row.length);

        int[] moveRow = new int[row.length];
        if (action == 3 || action == 0) {
            //进行合并，如 2 2 4 4，合并后为 4 0 8 0
            for (int i = 0; i < mergeRow.length - 1; i++) {
                if (mergeRow[i] == 0) continue;
                for (int j = i + 1; j < mergeRow.length; j++) {
                    if (mergeRow[j] == 0) continue;
                    if (mergeRow[i] == mergeRow[j]) {
                        mergeRow[i] *= 2;
                        mergeRow[j] = 0;
                    }
                    break;
                }
            }
            int k = 0;
            //移动，如 4 0 8 0，移动后为 4 8 0 0
            for (int j : mergeRow) {
                if (j != 0) moveRow[k++] = j;
            }
        }
        if (action == 1 || action == 2) {
            //进行合并，如 2 2 4 4，合并后为 0 4 0 8
            for (int i = mergeRow.length - 1; i > 0; i--) {
                if (mergeRow[i] == 0) continue;
                for (int j = i - 1; j >= 0; j--) {
                    if (mergeRow[j] == 0) continue;
                    if (mergeRow[i] == mergeRow[j]) {
                        mergeRow[i] *= 2;
                        mergeRow[j] = 0;
                    }
                    break;
                }
            }
            int k = row.length - 1;
            //移动，如 0 4 0 8，移动后为 0 0 4 8
            for (int i = k; i >= 0; i--) {
                if (mergeRow[i] != 0) moveRow[k--] = mergeRow[i];
            }
        }

        System.arraycopy(moveRow, 0, row, 0, moveRow.length);
    }

    public boolean move(int direction) {
        int[][] preMatrix = new int[cellMatrix.length][cellMatrix[0].length];
        for (int xx = 0; xx < cellMatrix.length; xx++) {
            System.arraycopy(cellMatrix[xx], 0, preMatrix[xx], 0, cellMatrix[0].length);
        }


        boolean moved = false;

        switch (direction) {
            case 0:
                antiClockwiseRotate90(cellMatrix);
                for (int[] matrix : cellMatrix) merge(matrix, 0);
                clockwiseRotate90(cellMatrix);
                break;
            case 1:
                for (int[] matrix : cellMatrix) merge(matrix, 1);
                break;
            case 2:
                antiClockwiseRotate90(cellMatrix);
                for (int[] matrix : cellMatrix) merge(matrix, 2);
                clockwiseRotate90(cellMatrix);
                break;
            case 3:
                for (int[] matrix : cellMatrix) merge(matrix, 3);
                break;
        }

        if (!isMatrixEquals(preMatrix, cellMatrix)) {
            moved = true;
            this.playerTurn = false;
        }

        return moved;
    }

    public static void antiClockwiseRotate90(int[][] matrix) {
        int[][] newMatrix = new int[matrix[0].length][matrix.length];
        for (int p = matrix[0].length - 1, i = 0; i < matrix[0].length; p--, i++) {
            for (int q = 0, j = 0; j < matrix.length; q++, j++) {
                newMatrix[p][q] = matrix[j][i];
            }
        }

        for (int i = 0; i < newMatrix[0].length; i++) {
            System.arraycopy(newMatrix[i], 0, matrix[i], 0, newMatrix[0].length);
        }
    }

    /**
     * 将矩阵顺时针旋转90度
     */
    public static void clockwiseRotate90(int[][] matrix) {
        int[][] newMatrix = new int[matrix[0].length][matrix.length];
        for (int p = 0, i = 0; i < matrix[0].length; p++, i++) {
            for (int q = matrix.length - 1, j = 0; j < matrix.length; q--, j++) {
                newMatrix[p][q] = matrix[j][i];
            }
        }
        for (int i = 0; i < newMatrix[0].length; i++) {
            System.arraycopy(newMatrix[i], 0, matrix[i], 0, newMatrix[0].length);
        }
    }

    public static boolean isMatrixEquals(int[][] matrix_1, int[][] matrix_2) {
        for (int i = 0; i < matrix_1.length; i++) {
            for (int j = 0; j < matrix_1[0].length; j++) {
                if (matrix_1[i][j] != matrix_2[i][j]) return false;
            }
        }
        return true;
    }

    public List<int[]> getAvailableCells() {
        List<int[]> cells = new ArrayList<>();
        for (int x = 0; x < cellMatrix.length; x++) {
            for (int y = 0; y < cellMatrix[0].length; y++) {
                if (cellMatrix[x][y] == 0) {
                    int[] tmp = {x, y};
                    cells.add(tmp);
                }
            }
        }
        return cells;
    }

    public void removeTile(int x, int y) {
        this.cellMatrix[x][y] = 0;
    }

    public void insertTitle(int x, int y, int value) {
        this.cellMatrix[x][y] = value;
    }

    public int islands() {
        int islands = 0;

        marked = new boolean[cellMatrix.length][cellMatrix[0].length];
        for (int x = 0; x < cellMatrix.length; x++) {
            for (int y = 0; y < cellMatrix[0].length; y++) {
                if (this.cellMatrix[x][y] != 0) {
                    this.marked[x][y] = false;
                }
            }
        }
        for (int x = 0; x < cellMatrix.length; x++) {
            for (int y = 0; y < cellMatrix[0].length; y++) {
                if (this.cellMatrix[x][y] != 0 && !this.marked[x][y]) {
                    islands++;
                    mark(x, y, this.cellMatrix[x][y]);
                }
            }
        }


        return islands;
    }

    private void mark(int x, int y, int value) {
        if (x >= 0 && x <= 3 && y >= 0 && y <= 3 && (this.cellMatrix[x][y] != 0)
                && (this.cellMatrix[x][y] == value) && (!this.marked[x][y])) {
            this.marked[x][y] = true;
            for (int direction = 0; direction < 4; direction++) {
                int[] vector = this.vectors[direction];
                mark(x + vector[0], y + vector[1], value);
            }
        }
    }
}
