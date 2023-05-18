package com.game2048.AI;
/**
 * Created by admin on 2018/1/9.
 */
public class Candidate {
    public int x;
    public int y;
    public int value;

    public Candidate() {
    }

    public Candidate(int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }
}
