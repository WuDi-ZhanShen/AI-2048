package com.game2048.AI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by admin on 2018/1/8.
 */
public class AI {

    private final GameState grid;
    double smoothWeight, //平滑性权重系数
            monoWeight, //单调性权重系数
            emptyWeight, //空格数权重系数
            maxWeight; //最大数权重系数
    long start;

    public AI(int[][] grid, Double smoothWeight, Double monoWeight, Double emptyWeight, Double maxWeight ) {
        this.grid = new GameState(grid);
        this.smoothWeight=smoothWeight;
        this.monoWeight=monoWeight;
        this.emptyWeight=emptyWeight;
        this.maxWeight=maxWeight;
    }

    private int getEmptyNum(int[][] matrix) {
        int sum = 0;
        for (int[] ints : matrix)
            for (int j = 0; j < matrix[0].length; j++)
                if (ints[j] == 0) sum++;
        return sum;
    }

    /**
     * 格局评估函数
     *
     * @return 返回当前格局的评估值，用于比较判断格局的好坏
     */
    private double evaluate() {
        return grid.smoothness() * smoothWeight
                + grid.monotonicity() * monoWeight
                + Math.log(getEmptyNum(grid.getCellMatrix())) * emptyWeight
                + grid.maxValue() * maxWeight;
    }

    private SearchResult search(int depth, double alpha, double beta, int positions, int cutoffs) {
        double bestScore;
        int bestMove = -1;
        SearchResult result = new SearchResult();
        int[] directions = {0, 1, 2, 3};
        if (this.grid.playerTurn) {  // Max 层
            bestScore = alpha;
            for (int direction : directions) {  // 玩家遍历四个滑动方向，找出一个最好的
                GameState newGrid = new GameState(this.grid.getCellMatrix());
                if (newGrid.move(direction)) {
                    positions++;
//                    if (newGrid.isWin()) {
//                        return new SearchResult(direction, 10000, positions, cutoffs);
//                    }
                    AI newAI = new AI(newGrid.getCellMatrix(), smoothWeight,monoWeight,emptyWeight,maxWeight);
                    newAI.grid.playerTurn = false;

                    if (depth == 0) { //如果depth=0,搜索到该层后不再向下搜索
                        result.move = direction;
                        result.score = newAI.evaluate();
                    } else { //如果depth>0,则继续搜索下一层，下一层为电脑做出决策的层
                        result = newAI.search(depth - 1, bestScore, beta, positions, cutoffs);
                        if (result.score > 9900) { // 如果赢得游戏
                            result.score--; // 轻微地惩罚因为更大的搜索深度
                        }
                        positions = result.positions;
                        cutoffs = result.cutoffs;
                    }

                    //如果当前搜索分支的格局分数要好于之前得到的分数，则更新决策，同时更新bestScore，也即alpha的值
                    if (result.score > bestScore) {
                        bestScore = result.score;
                        bestMove = direction;
                    }
                    //如果当前bestScore也即alpha>beta时，表明这个节点下不会再有更好解，于是剪枝
                    if (bestScore > beta) {
                        cutoffs++;  //剪枝
                        return new SearchResult(bestMove, beta, positions, cutoffs);
                    }
                }
            }
        } else {
            // Min 层，该层为电脑层(也即我们的对手)，这里我们假设对手(电脑)足够聪明，总是能做出使格局变到最坏的决策
            bestScore = beta;

            // 尝试给每个空闲块填入2或4，然后计算格局的评估值
            List<Candidate> candidates = new ArrayList<>();
            List<int[]> cells = this.grid.getAvailableCells();
            int[] fill = {2, 4};
            List<Double> scores_2 = new ArrayList<>();
            List<Double> scores_4 = new ArrayList<>();
            for (int value : fill) {
                for (int i = 0; i < cells.size(); i++) {
                    this.grid.insertTitle(cells.get(i)[0], cells.get(i)[1], value);
                    if (value == 2) scores_2.add(i, -this.grid.smoothness() + this.grid.islands());
                    if (value == 4) scores_4.add(i, -this.grid.smoothness() + this.grid.islands());
                    this.grid.removeTile(cells.get(i)[0], cells.get(i)[1]);
                }
            }

            // 找出使格局变得最坏的所有可能操作
            double maxScore = Math.max(Collections.max(scores_2), Collections.max(scores_4));
            for (int value : fill) {
                if (value == 2) {
                    for (Double fitness : scores_2) {
                        if (fitness == maxScore) {
                            int index = scores_2.indexOf(fitness);
                            candidates.add(new Candidate(cells.get(index)[0], cells.get(index)[1], value));
                        }
                    }
                }
                if (value == 4) {
                    for (Double fitness : scores_4) {
                        if (fitness == maxScore) {
                            int index = scores_4.indexOf(fitness);
                            candidates.add(new Candidate(cells.get(index)[0], cells.get(index)[1], value));
                        }
                    }
                }
            }

            // 然后遍历这些操作，基于这些操作向下搜索，找到使得格局最坏的分支
            for (int i = 0; i < candidates.size(); i++) {
                int pos_x = candidates.get(i).x;
                int pos_y = candidates.get(i).y;
                int value = candidates.get(i).value;
                GameState newGrid = new GameState(this.grid.getCellMatrix());
                // 电脑即对手做出一个可能的对于电脑来说最好的（对于玩家来说最坏的）决策
                newGrid.insertTitle(pos_x, pos_y, value);
                positions++;
                AI newAI = new AI(newGrid.getCellMatrix(), smoothWeight,monoWeight,emptyWeight,maxWeight);
                // 向下搜索，下一层为Max层，轮到玩家进行决策
                newAI.grid.playerTurn = true;
                // 这里depth没有减1是为了保证搜索到最深的层为Max层
                result = newAI.search(depth, alpha, bestScore, positions, cutoffs);
                positions = result.positions;
                cutoffs = result.cutoffs;

                // 该层为Min层，哪个分支的局势最不好，就选哪个分支，这里的bestScore代表beta
                if (result.score < bestScore) {
                    bestScore = result.score;
                }
                // 如果当前bestScore也即beta<alpha时，表明这个节点下不会再有更好解，于是剪枝
                if (bestScore < alpha) {
                    cutoffs++;  //减枝
                    return new SearchResult(-1, alpha, positions, cutoffs);
                }
            }
        }

        return new SearchResult(bestMove, bestScore, positions, cutoffs);
    }

    // 执行搜索操作，返回最好的移动方向
    public int getBestMove(int minSearchTime) {
        start = new Date().getTime();
        int depth = 0;
        int best = -1;
        do {
            SearchResult newBest = this.search(depth, -10000, 10000, 0, 0);
            if (newBest.move == -1) break;
            else best = newBest.move;
            depth++;

        } while (new Date().getTime() - start < minSearchTime);
        switch (best) {
            case 3:
                return 0;
            case 2:
                return 1;
            case 1:
                return 2;
            default:
                return 3;

        }
    }

    // 基于alpha-beta的Minimax搜索，进行迭代深搜，搜索时间设定为0.1秒，即决策的思考时间为0.1秒

}
