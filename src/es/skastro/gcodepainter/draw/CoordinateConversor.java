package es.skastro.gcodepainter.draw;

public class CoordinateConversor {
    double a_height, a_width, b_height, b_width, ab_heigh_relation, ab_width_relation;
    Point a1, a2, b1, b2;

    public CoordinateConversor(Point a1, Point a2, Point b1, Point b2) {
        this.a1 = a1;
        this.a2 = a2;
        this.b1 = b1;
        this.b2 = b2;

        a_height = Math.abs(a2.y - a1.y);
        a_width = Math.abs(a2.x - a1.x);

        b_height = Math.abs(b2.y - b1.y);
        b_width = Math.abs(b2.x - b1.x);

        ab_heigh_relation = b_height / a_height;
        ab_width_relation = b_width / a_width;
    }

    /***
     * a1 and a2 are the A system vectors
     * 
     * @param a1
     * @param a2
     * @param b1
     * @param b2
     * @return
     */
    // TODO: is only valid for this case
    public Point calculate(Point x) {
        Point res = new Point();
        res.setX(x.getX() * ab_heigh_relation);
        res.setY((a1.getY() - x.getY()) * ab_width_relation);
        return res;
    }
}
