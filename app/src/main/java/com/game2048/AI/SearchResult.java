package com.game2048.AI;

/**
 * Created by admin on 2018/1/9.
 */
public class SearchResult {
    public int move;
    public double score;
    public int positions;
    public int cutoffs;

    public SearchResult() {
    }

    public SearchResult(int move, double score) {
        this.move = move;
        this.score = score;
    }

    public SearchResult(int move, double score, int positions, int cutoffs) {
        this.move = move;
        this.score = score;
        this.positions = positions;
        this.cutoffs = cutoffs;
    }
}
