package main.NeuronTracer;

public class Trips<x, y, d> {
    public final int x;
    public final int y;
    public int d;

    public Trips(int x, int y, int d) {
        this.x = x;
        this.y = y;
        this.d = d;
    }

    public boolean equals(Object temp) {
        Trips trips = (Trips) temp;
        if (x==trips.x && y == trips.y && d == trips.d)
            return true;
        return false;
    }

    public String toString() {
        return "(" + x + "," + y + "," + d + ")";
    }
}