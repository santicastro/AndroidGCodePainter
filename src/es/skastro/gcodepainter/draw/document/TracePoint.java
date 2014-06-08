package es.skastro.gcodepainter.draw.document;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import android.graphics.PointF;

public class TracePoint {
    private PointF point;
    private boolean isMovement = false;

    public TracePoint() {
    }

    public TracePoint(PointF point) {
        super();
        this.point = point;
    }

    public PointF getPoint() {
        return point;
    }

    public boolean isMovement() {
        return isMovement;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).appendSuper(super.toString())
                .append("point", point).toString();
    }

}
