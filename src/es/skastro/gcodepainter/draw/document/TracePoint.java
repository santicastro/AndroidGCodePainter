package es.skastro.gcodepainter.draw.document;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class TracePoint {
    private Point point;

    public TracePoint() {

    }

    public TracePoint(Point point) {
        super();
        this.point = point;
    }

    public Point getPoint() {
        return point;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).appendSuper(super.toString())
                .append("point", point).toString();
    }

}
