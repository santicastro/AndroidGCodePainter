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

public class Document extends Observable {

    public enum ActionType {
        ACTION_NONE, POLYLINE_START, POLYLINE_POINT, POLYLINE_END
    };

    @JsonProperty("traces")
    ArrayList<Trace> traces;

    @JsonProperty("minPointDistance")
    private double minPointDistance = 2.5;
    // if the angle between 2 consecutive lines is less than this value the middle point is deleted
    @JsonProperty("deltaAngleIgnore")
    private double deltaAngleIgnore = 0.2; // radians

    // if the angle between 2 lines is more than this value, the middle point is never deleted
    @JsonProperty("deltaAngleForceNotIgnore")
    private double deltaAngleForceNotIgnore = 0.1;

    @JsonIgnore
    public final Point bottomLeftCorner = new Point(0.0, 0.0);
    @JsonIgnore
    public final Point topRightCorner = new Point(100.0, 67.8);

    @JsonIgnore
    private Trace currentTrace;

    @JsonProperty("showing")
    private int showingTraceId;

    private static ObjectMapper mapper;
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

    // TODO: control outbounds
    public int simplifyPoints(List<TracePoint> temporal_points) {
        int res = temporal_points.size();
        if (temporal_points.size() > 2) {
            Point p0, p1, p2;
            for (int i = 2; i < temporal_points.size(); i++) {
                p0 = temporal_points.get(i - 2).getPoint();
                p1 = temporal_points.get(i - 1).getPoint();
                p2 = temporal_points.get(i).getPoint();
                double deltaAngle = Math.abs(Point.angle(Point.minus(p1, p0), Point.minus(p2, p1)));
                // double distance1 = Point.distance(p0, p1);
                // double distance2 = Point.distance(p1, p2);
                // double scaleDistance = distance1 / (distance2 + 0.0001);
                // if (deltaAngle < deltaAngleIgnore && (scaleDistance > 0.33 && scaleDistance < 3)) {
                if (deltaAngle < deltaAngleIgnore) {
                    temporal_points.remove(i - 1);
                    i--;
                } else if (deltaAngle < deltaAngleForceNotIgnore) {
                    if (Point.distance(p0, p2) < minPointDistance) {
                        temporal_points.remove(i - 1);
                        i--;
                    }
                }

            }
        }
        return temporal_points.size() - res;
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

}
