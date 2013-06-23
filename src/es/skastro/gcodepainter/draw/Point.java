package es.skastro.gcodepainter.draw;

import java.text.DecimalFormat;

public class Point {
    double x = 0.0, y = 0.0;

    DecimalFormat df = new DecimalFormat("0.0000");

    public static double distance(Point a, Point b) {
        return Math.sqrt(Math.pow((b.x - a.x), 2.0) + Math.pow((b.y - a.y), 2.0));
    }

    public static double angle(Point a, Point b) {
        return Math.atan((b.y - a.y) / (b.x - a.x));
    }

    public static Point minus(Point a, Point b) {
        return new Point(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public Point() {

    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "X" + df.format(x) + " Y" + df.format(y);
    }

    @Override
    public boolean equals(final Object other) {
        return (this == other);
    }

}