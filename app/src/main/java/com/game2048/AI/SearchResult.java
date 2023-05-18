package com.game2048.AI;

/**
 * Created by admin on 2018/1/9.
 */
public class SearchResult {
    public int move;
    public int score;
    public int positions;
    public int cutoffs;

    public SearchResult() {
    }

    public SearchResult(int move, int score) {
        this.move = move;
        this.score = score;
    }

    public SearchResult(int move, int score, int positions, int cutoffs) {
        this.move = move;
        this.score = score;
        this.positions = positions;
        this.cutoffs = cutoffs;
    }
}
