package main.NeuronTracer;

public class Pair<x, y> {
    public final int x;
    public final int y;
    public Pair(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(Object temp) {
        Pair pair = (Pair) temp;
        if (x==pair.x && y == pair.y)
            return true;
        return false;
    }

    public String toString(){
        return "(" + this.x + "," + this.y + ")";
    }
}

