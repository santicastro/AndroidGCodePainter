package es.skastro.gcodepainter.draw.document;

import java.text.DecimalFormat;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

public class Point {
    private double x = 0.0, y = 0.0;

    @JsonIgnore
    private DecimalFormat df = new DecimalFormat("0.0000");

    public static double distance(Point a, Point b) {
        return Math.sqrt(Math.pow((b.x - a.x), 2.0) + Math.pow((b.y - a.y), 2.0));
    }

    public static double angle(Point a, Point b) {
        return Math.atan2(b.getY(), b.getX()) - Math.atan2(a.getY(), a.getX());
    }

    /***
     * rotates a vector counterclockwise
     * 
     * @param v
     * @param n
     * @return
     */
    public static Point rotate(Point v, double n) {
        double rx = (v.x * Math.cos(n)) - (v.y * Math.sin(n));
        double ry = (v.x * Math.sin(n)) + (v.y * Math.cos(n));
        return new Point(rx, ry);
    }

    public static Point minus(Point a, Point b) {
        return new Point(a.getX() - b.getX(), a.getY() - b.getY());
    }

    public static Point plus(Point a, Point b) {
        return new Point(a.getX() + b.getX(), a.getY() + b.getY());
    }

    public Point(Point clone) {
        this.x = clone.x;
        this.y = clone.y;
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
        if (this == other)
            return true;
        if (!(other instanceof Point))
            return false;
        Point castOther = (Point) other;
        return new EqualsBuilder().append(x, castOther.x).append(y, castOther.y).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(1114199465, 738887065).append(x).append(y).toHashCode();
    }

}