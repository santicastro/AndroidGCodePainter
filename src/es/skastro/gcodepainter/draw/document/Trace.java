package es.skastro.gcodepainter.draw.document;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

public class Trace {
    private int traceId;
    private List<TracePoint> points;

    public Trace() {
    }

    public Trace(int traceId) {
        this.traceId = traceId;
        points = new ArrayList<TracePoint>();
    }

    public int getTraceId() {
        return traceId;
    }

    public List<TracePoint> getPoints() {
        return points;
    }

    @JsonIgnore
    public int getPointCount() {
        return points.size();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + traceId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Trace other = (Trace) obj;
        if (traceId != other.traceId)
            return false;
        return true;
    }

}
