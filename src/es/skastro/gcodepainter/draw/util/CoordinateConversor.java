package es.skastro.gcodepainter.draw.util;

import es.skastro.gcodepainter.draw.document.Point;

public class CoordinateConversor {
    double a_height, a_width, b_height, b_width, ab_heigh_relation, ab_width_relation;
    Point bottom_left_a, top_right_a, bottom_left_b, top_right_b;

    public CoordinateConversor(Point bottom_left_a, Point top_right_a, Point bottom_left_b, Point top_right_b) {
        this.bottom_left_a = bottom_left_a;
        this.top_right_a = top_right_a;
        this.bottom_left_b = bottom_left_b;
        this.top_right_b = top_right_b;

        a_height = top_right_a.getY() - bottom_left_a.getY();
        a_width = top_right_a.getX() - bottom_left_a.getX();

        b_height = top_right_b.getY() - bottom_left_b.getY();
        b_width = top_right_b.getX() - bottom_left_b.getX();

        ab_heigh_relation = b_height / a_height;
        ab_width_relation = b_width / a_width;
    }

    /***
     * a1 and a2 are the A system vectors
     * 
     * @param bottom_left_a
     * @param top_right_a
     * @param bottom_left_b
     * @param top_right_b
     * @return
     */
    public Point calculate(Point x) {
        Point res = new Point();
        res.setX(bottom_left_b.getX() + (x.getX() - bottom_left_a.getX()) * ab_width_relation);
        res.setY(bottom_left_b.getY() + (x.getY() - bottom_left_a.getY()) * ab_heigh_relation);
        return res;
    }
}
