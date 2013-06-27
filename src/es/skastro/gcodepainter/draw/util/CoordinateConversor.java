package es.skastro.gcodepainter.draw.util;

import android.graphics.PointF;
import android.graphics.RectF;

public class CoordinateConversor {
    float origin_height, origin_width, target_height, target_width, heigh_relation, width_relation;
    RectF originRect, targetRect;

    public CoordinateConversor(RectF origin, RectF target) {
        this.originRect = origin;
        this.targetRect = target;
        origin_height = origin.top - origin.bottom;
        origin_width = origin.right - origin.left;

        target_height = target.top - target.bottom;
        target_width = target.right - target.left;

        heigh_relation = target_height / origin_height;
        width_relation = target_width / origin_width;

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
    public PointF calculate(PointF point) {
        PointF res = new PointF();
        res.x = targetRect.left + (point.x - originRect.left) * width_relation;
        res.y = targetRect.bottom + (point.y - originRect.bottom) * heigh_relation;
        return res;
    }
}
