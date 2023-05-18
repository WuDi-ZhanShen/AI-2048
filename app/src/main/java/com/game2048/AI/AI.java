package com.game2048.AI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by admin on 2018/1/8.
 */
public class AI {

    private final GameState grid;
    int smoothWeight, //平滑性权重系数
            monoWeight, //单调性权重系数
            emptyWeight, //空格数权重系数
            maxWeight; //最大数权重系数
    long start;
    private final boolean considerFour;

    public AI(int[][] grid, int smoothWeight, int monoWeight, int emptyWeight, int maxWeight, boolean considerFour) {
        this.grid = new GameState(grid);
        this.smoothWeight = smoothWeight;
        this.monoWeight = monoWeight;
        this.emptyWeight = emptyWeight;
        this.maxWeight = maxWeight;
        this.considerFour = considerFour;
    }

    /**
     * 格局评估函数
     *
     * @return 返回当前格局的评估值，用于比较判断格局的好坏
     */
    private SearchResult search(int depth, int alpha, int beta, int positions, int cutoffs) {

        int bestScore;
        int bestMove = -1;
        SearchResult result = new SearchResult();
        int[] directions = {0, 1, 2, 3};
        if (grid.playerTurn) {  // Max 层
            bestScore = alpha;
            for (int direction : directions) {  // 玩家遍历四个滑动方向，找出一个最好的
                GameState newGrid = new GameState(grid.getCellMatrix());
                if (newGrid.move(direction)) {
                    positions++;
                    AI newAI = new AI(newGrid.getCellMatrix(), smoothWeight, monoWeight, emptyWeight, maxWeight, considerFour);

                    newAI.grid.playerTurn = false;

                    if (depth == 0) {
                        result.move = direction;
//                        Log.d("TAG", "search: "+direction);
                        result.score = newGrid.evaluate(smoothWeight, monoWeight, emptyWeight, maxWeight);
                    } else {
                        result = newAI.search(depth - 1, bestScore, beta, positions, cutoffs);
                        if (result.score > 9900) {
                            result.score--;
                        }
                        positions = result.positions;
                        cutoffs = result.cutoffs;
                    }


                    if (result.score > bestScore) {
                        bestScore = result.score;
                        bestMove = direction;
                    }
                    if (bestScore > beta) {
                        cutoffs++;  //剪枝

                        return new SearchResult(bestMove, beta, positions, cutoffs);
                    }
                }
            }
        } else {
            bestScore = beta;

            List<Candidate> candidates = new ArrayList<>();
            List<int[]> cells = grid.getAvailableCells();

            if (considerFour) {
                int[] fill = {2, 4};
                List<Integer> scores_2 = new ArrayList<>();
                List<Integer> scores_4 = new ArrayList<>();
                for (int value : fill) {
                    for (int i = 0; i < cells.size(); i++) {
                        grid.insertTitle(cells.get(i)[0], cells.get(i)[1], value);
                        if (value == 2) scores_2.add(i, -grid.smoothness() + grid.islands());
                        if (value == 4) scores_4.add(i, -grid.smoothness() + grid.islands());
                        grid.removeTile(cells.get(i)[0], cells.get(i)[1]);
                    }
                }

                // 找出使格局变得最坏的所有可能操作
                int maxScore = Math.max(Collections.max(scores_2), Collections.max(scores_4));
                List<Integer> maxIndices = new ArrayList<>();
                for (int value : fill) {
                    if (value == 2) {
                        for (int i = 0; i < scores_2.size(); i++) {
                            if (scores_2.get(i) == maxScore) {
                                maxIndices.add(i);
                            }
                        }
                    }
                    if (value == 4) {
                        for (int i = 0; i < scores_4.size(); i++) {
                            if (scores_4.get(i) == maxScore) {
                                maxIndices.add(i);
                            }
                        }
                    }
                }
                int randomIndex = maxIndices.get((int) (Math.random() * maxIndices.size()));
                candidates.add(new Candidate(cells.get(randomIndex)[0], cells.get(randomIndex)[1], fill[randomIndex % 2]));

            } else {

                List<Integer> scores_2 = new ArrayList<>();
                for (int i = 0; i < cells.size(); i++) {
                    grid.insertTitle(cells.get(i)[0], cells.get(i)[1], 2);
                    scores_2.add(i, -grid.smoothness() + grid.islands());
                    grid.removeTile(cells.get(i)[0], cells.get(i)[1]);
                }
                // 找出使格局变得最坏的所有可能操作
                int maxScore = Collections.max(scores_2);
                for (Integer fitness : scores_2) {
                    if (fitness == maxScore) {
                        int index = scores_2.indexOf(fitness);
                        candidates.add(new Candidate(cells.get(index)[0], cells.get(index)[1], 2));
                    }
                }
            }
            // 然后遍历这些操作，基于这些操作向下搜索，找到使得格局最坏的分支
            for (int i = 0; i < candidates.size(); i++) {
                int pos_x = candidates.get(i).x;
                int pos_y = candidates.get(i).y;
                int value = candidates.get(i).value;
                GameState newGrid = new GameState(grid.getCellMatrix());
                // 电脑即对手做出一个可能的对于电脑来说最好的（对于玩家来说最坏的）决策
                newGrid.insertTitle(pos_x, pos_y, value);
                positions++;
                AI newAI = new AI(newGrid.getCellMatrix(), smoothWeight, monoWeight, emptyWeight, maxWeight, considerFour);
                // 向下搜索，下一层为Max层，轮到玩家进行决策
                newAI.grid.playerTurn = true;
                // 这里depth没有减1是为了保证搜索到最深的层为Max层
                result = newAI.search(depth, alpha, bestScore, positions, cutoffs);
                positions = result.positions;
                cutoffs = result.cutoffs;

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
    public int getBestMove(long minSearchTime) {
        start = System.nanoTime();
        int depth = 5;
        int best;
        do {
            SearchResult newBest = search(depth, -10000, 10000, 0, 0);
            best = newBest.move == -1 ? new Random().nextInt(4)  : newBest.move;
            depth++;
        } while (System.nanoTime() - start < minSearchTime);

        return 3 - best;

    }
}
