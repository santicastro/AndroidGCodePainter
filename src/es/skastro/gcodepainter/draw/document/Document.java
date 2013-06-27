package es.skastro.gcodepainter.draw.document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.graphics.PointF;
import android.graphics.RectF;

public class Document extends Observable {

    public enum ActionType {
        ACTION_NONE, POLYLINE_START, POLYLINE_POINT, POLYLINE_END
    };

    static {
        mapper = new ObjectMapper();
    }

    public Document() {
        traces = new ArrayList<Trace>();
    }

    public static Document fromFile(File file) {
        try {
            return mapper.readValue(file, Document.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveToDisk(File file) throws IOException {
        File tmp = File.createTempFile("drawfile", ".ske");
        OutputStream output = new FileOutputStream(tmp);

        mapper.writeValue(output, this);
        output.close();

        if (file.exists())
            file.delete();
        FileUtils.copyFile(tmp, file);
    }

    @JsonIgnore
    private int getNewTraceId() {
        if (traces.size() == 0) {
            return 1;
        } else {
            return traces.get(traces.size() - 1).getTraceId() + 1;
        }
    }

    @JsonIgnore
    public int createTrace() {
        if (currentTrace != null) {
            currentTrace = null;
            setChanged();
            notifyObservers();
        }
        while (canRedo()) {
            traces.remove(traces.size() - 1);
        }
        currentTrace = new Trace(getNewTraceId());
        return currentTrace.getTraceId();
    }

    public void commitTrace(int id) {
        if (currentTrace != null && currentTrace.getTraceId() == id) {
            traces.add(currentTrace);
            deleteOutboundPoints(currentTrace.getPoints());
            simplifyPoints(currentTrace.getPoints());
            currentTrace = null;
            showingTraceId = id;
            setChanged();
            notifyObservers();
        } else {
            throw new RuntimeException("Not implemented. Can't commit a not final trace");
        }
    }

    @JsonIgnore
    public boolean canRedo() {
        return traces.size() > 0 && traces.get(traces.size() - 1).getTraceId() > showingTraceId;
    }

    @JsonIgnore
    public boolean canUndo() {
        return traces.size() > 0 && showingTraceId > traces.get(0).getTraceId();
    }

    public void redo() {
        if (canRedo()) {
            for (int i = traces.size() - 2; i >= 0; i--) {
                if (traces.get(i).getTraceId() == showingTraceId) {
                    showingTraceId = traces.get(i + 1).getTraceId();
                    setChanged();
                    notifyObservers();
                    return;
                }
            }
        }
    }

    public void undo() {
        if (canUndo()) {
            for (int i = traces.size() - 1; i > 0; i--) {
                if (traces.get(i).getTraceId() == showingTraceId) {
                    showingTraceId = traces.get(i - 1).getTraceId();
                    setChanged();
                    notifyObservers();
                    return;
                }
            }
        }
    }

    @JsonIgnore
    public List<TracePoint> getTemporalPoints() {
        if (currentTrace != null)
            return currentTrace.getPoints();
        else
            return null;
    }

    private boolean isOutbounds(PointF p) {
        return Double.compare(p.x, margins.left) < 0 || Double.compare(p.x, margins.right) > 0
                || Double.compare(p.y, margins.left) < 0 || Double.compare(p.y, margins.top) > 0;
    }

    private final int LEFT = 1;
    private final int TOP = 2;
    private final int RIGHT = 4;
    private final int BOTTOM = 16;

    private int getOnEdge(PointF p) {
        int res = 0;
        if (!isOutbounds(p)) {
            if (Float.compare(p.x, margins.left) == 0)
                res |= LEFT;

            if (Float.compare(p.x, margins.right) == 0)
                res |= RIGHT;

            if (Float.compare(p.y, margins.left) == 0)
                res |= BOTTOM;

            if (Float.compare(p.y, margins.top) == 0)
                res |= TOP;
        }
        return res;
    }

    public void deleteOutboundPoints(List<TracePoint> list) {
        PointF p0, p1;
        boolean out0, out1, lim0, lim1;
        // remove outside points
        if (list.size() > 0) {
            p0 = list.get(0).getPoint();
            if (isOutbounds(p0)) { // this means that the first point is outside. We don't know the previous point
                // so we only get the nearest inside point
                p0.x = Math.max(margins.left, Math.min(margins.right, p0.x));
                p0.y = Math.max(margins.left, Math.min(margins.top, p0.y));
            }

            if (list.size() > 1) {
                p0 = list.get(list.size() - 1).getPoint();
                if (isOutbounds(p0)) {
                    list.remove(list.size() - 1);
                }
            }

            for (int i = 1; i < list.size(); i++) {
                p0 = list.get(i - 1).getPoint();
                out0 = isOutbounds(p0);
                lim0 = getOnEdge(p0) != 0;
                p1 = list.get(i).getPoint();
                out1 = isOutbounds(p1);
                lim1 = getOnEdge(p1) != 0;
                if (!lim0 && !lim1 && (out0 != out1)) {
                    PointF in, out;
                    if (out0) {
                        out = p0;
                        in = p1;
                    } else {
                        out = p1;
                        in = p0;
                    }
                    PointF v = Point.minus(out, in);
                    float m = v.y / v.x;
                    PointF intersec = new PointF();
                    intersec.set(out);
                    boolean top = (in.y - margins.top) * (intersec.y - margins.top) < 0;
                    if (top) {
                        // x1 = (y1 - y0)/m + x0, with y1 = topRight.y
                        intersec = new PointF((float) ((margins.top - in.y) / m + in.x), margins.top);
                    }
                    boolean bottom = (in.y - margins.left) * (intersec.y - margins.left) < 0;
                    if (bottom) {
                        // x1 = (y1 - y0)/m + x0, with y1 = bottomLeft.y
                        intersec = new PointF((float) ((margins.left - in.y) / m + in.x), margins.left);
                    }

                    boolean right = (in.x - margins.right) * (intersec.x - margins.right) < 0;
                    if (right) {
                        // y1 = (x1 - x0)*m + y0, with x1 = topRight.x
                        intersec = new PointF(margins.right, (float) ((margins.right - in.x) * m + in.y));
                    }

                    boolean left = (in.x - margins.left) * (intersec.x - margins.left) < 0;
                    if (left) {
                        // y1 = (x1 - x0)*m + y0, with x1 = bottomLeft.x
                        intersec = new PointF(margins.left, (float) ((margins.left - in.x) * m + in.y));
                    }
                    list.add(i, new TracePoint(intersec));
                    i++;
                }
            }

            // we can delete now the points out of the bounds
            for (int i = list.size() - 1; i >= 0; i--) {
                if (isOutbounds(list.get(i).getPoint())) {
                    if (i > 0 && i < list.size() - 1) {
                        // control if the trace went outside on a different edge than the inside trace
                        PointF previous = list.get(i - 1).getPoint();
                        PointF next = list.get(i + 1).getPoint();
                        int limit0 = getOnEdge(previous);
                        int limit1 = getOnEdge(next);
                        if (limit0 != limit1 && limit0 != 0 && limit1 != 0) {
                            PointF newPoint0 = null, newPoint1 = null;
                            switch (limit0 * limit1) {
                            case LEFT * TOP:
                                newPoint0 = new PointF(margins.left, margins.top);
                                break;
                            case RIGHT * TOP:
                                newPoint0 = new PointF(margins.right, margins.top);
                                break;
                            case RIGHT * BOTTOM:
                                newPoint0 = new PointF(margins.right, margins.left);
                                break;
                            case LEFT * BOTTOM:
                                newPoint0 = new PointF(margins.left, margins.bottom);
                                break;
                            case LEFT * RIGHT:
                                newPoint0 = new PointF(previous.x, margins.top);
                                newPoint1 = new PointF(next.x, margins.top);
                                break;
                            case TOP * BOTTOM:
                                newPoint0 = new PointF(margins.right, previous.y);
                                newPoint1 = new PointF(margins.right, next.y);
                                break;
                            }
                            if (newPoint1 != null) {
                                list.add(i + 1, new TracePoint(newPoint1));
                            }
                            if (newPoint0 != null) {
                                list.add(i + 1, new TracePoint(newPoint0));
                            }
                        }
                    }
                    list.remove(i);
                }
            }
        }
    }

    public int simplifyPoints(List<TracePoint> list) {
        PointF p0, p1, p2;

        int res = list.size();
        // simplifyPoints
        if (list.size() > 2) {
            for (int i = 2; i < list.size(); i++) {
                p0 = list.get(i - 2).getPoint();
                p1 = list.get(i - 1).getPoint();
                p2 = list.get(i).getPoint();
                float deltaAngle = (float) Math.abs(Point.angle(Point.minus(p1, p0), Point.minus(p2, p1)));
                // double distance1 = Point.distance(p0, p1);
                // double distance2 = Point.distance(p1, p2);
                // double scaleDistance = distance1 / (distance2 + 0.0001);
                // if (deltaAngle < deltaAngleIgnore && (scaleDistance > 0.33 && scaleDistance < 3)) {
                if (deltaAngle < deltaAngleIgnore) {
                    list.remove(i - 1);
                    i--;
                } else if (deltaAngle < deltaAngleForceNotIgnore) {
                    if (Point.distance(p0, p2) < minPointDistance) {
                        list.remove(i - 1);
                        i--;
                    }
                }

            }
        }
        return list.size() - res;
    }

    public void addPoint(int traceId, TracePoint p) {
        if (currentTrace != null && currentTrace.getTraceId() == traceId) {
            currentTrace.getPoints().add(p);
            setChanged();
            notifyObservers();
        } else {
            throw new RuntimeException("Not implemented. A point can't be added to an intermediate trace");
        }
    }

    public void addPoints(int traceId, List<TracePoint> list) {
        if (currentTrace != null && currentTrace.getTraceId() == traceId) {
            currentTrace.getPoints().addAll(list);
            setChanged();
            notifyObservers();
        } else {
            throw new RuntimeException("Not implemented. A point can't be added to an intermediate trace");
        }
    }

    @JsonIgnore
    public TracePoint getPoint(int location) {
        if (traces.size() == 0)
            throw new IndexOutOfBoundsException();
        int idx = 0;
        Trace tr = traces.get(idx);
        while (tr.getPointCount() <= location) {
            location -= tr.getPointCount();
            tr = traces.get(++idx);
        }
        return tr.getPoints().get(location);
    }

    @JsonIgnore
    public List<TracePoint> getPoints() {
        List<TracePoint> points = new ArrayList<TracePoint>();
        for (Trace tr : traces) {
            if (tr.getTraceId() <= showingTraceId)
                points.addAll(tr.getPoints());
        }
        return points;
    }

    @JsonIgnore
    public int getTraceCount() {
        if (traces.size() > 0 && traces.get(traces.size() - 1).getTraceId() == showingTraceId)
            return traces.size();
        else {
            for (int i = 0; i < traces.size(); i++) {
                if (traces.get(i).getTraceId() > showingTraceId)
                    return i;
            }
            return traces.size();
        }
    }

    @JsonIgnore
    public Trace getTrace(int location) {
        return traces.get(location);
    }

    @JsonIgnore
    public int getPointCount() {
        int res = 0;
        for (Trace tr : traces) {
            if (tr.getTraceId() <= showingTraceId)
                res += tr.getPointCount();
        }
        return res;
    }

    public void removePoint(int location) {
        if (traces.size() == 0)
            throw new IndexOutOfBoundsException();
        int idx = 0;
        Trace tr = traces.get(idx);
        while (tr.getPointCount() <= location) {
            location -= tr.getPointCount();
            tr = traces.get(++idx);
        }
        tr.getPoints().remove(location);

        setChanged();
        notifyObservers();
    }

    public RectF getMargins() {
        return margins;
    }

    @JsonProperty("traces")
    ArrayList<Trace> traces;

    @JsonProperty("minPointDistance")
    private float minPointDistance = 2.5f;
    // if the angle between 2 consecutive lines is less than this value the middle point is deleted
    @JsonProperty("deltaAngleIgnore")
    private float deltaAngleIgnore = 0.2f; // radians

    // if the angle between 2 lines is more than this value, the middle point is never deleted
    @JsonProperty("deltaAngleForceNotIgnore")
    private float deltaAngleForceNotIgnore = 0.1f;

    @JsonProperty("margins")
    private final RectF margins = new RectF(0f, 67.8f, 100f, 0f);

    @JsonIgnore
    private Trace currentTrace;

    @JsonProperty("showing")
    private int showingTraceId;

    private static ObjectMapper mapper;
}
