package es.skastro.gcodepainter.draw;

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

public class DrawFile extends Observable {
    public static final int LAST_POSITION = -1;

    public enum ActionType {
        ACTION_NONE, POLYLINE_START, POLYLINE_POINT, POLYLINE_END
    };

    @JsonProperty("points")
    ArrayList<Point> points;
    @JsonIgnore
    List<Point> temporal_points, undo_points;

    @JsonProperty("minPointDistance")
    private double minPointDistance = 25.0;
    @JsonProperty("minTemporalPointDistance")
    private double minTemporalPointDistance = 4.0;
    // if the angle between 2 consecutive lines is less than this value the middle point is deleted
    @JsonProperty("deltaAngleIgnore")
    private double deltaAngleIgnore = 0.2; // radians

    // if the angle between 2 lines is more than this value, the middle point is never deleted
    @JsonProperty("deltaAngleForceNotIgnore")
    private double deltaAngleForceNotIgnore = 1.2;

    private static ObjectMapper mapper;
    static {
        mapper = new ObjectMapper();
    }

    public DrawFile() {
        points = new ArrayList<Point>();
        temporal_points = new ArrayList<Point>();
        undo_points = new ArrayList<Point>();
    }

    public static DrawFile fromFile(File file) {
        try {
            return mapper.readValue(file, DrawFile.class);
        } catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
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

    public void commitTemporalPoints(boolean forceExactPoints) {
        if (!forceExactPoints && temporal_points.size() > 2) {
            Point p0, p1, p2;
            for (int i = 2; i < temporal_points.size(); i++) {
                p0 = temporal_points.get(i - 2);
                p1 = temporal_points.get(i - 1);
                p2 = temporal_points.get(i);
                double angle1 = Point.angle(p0, p1);
                double angle2 = Point.angle(p1, p2);
                double deltaAngle = Math.abs(angle1 - angle2);
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
        if (temporal_points.size() > 0) {
            points.addAll(temporal_points);
            List<Point> swap = undo_points;
            swap.clear();
            undo_points = temporal_points;
            temporal_points = swap;
            setChanged();
            notifyObservers();
        }
    }

    public void commitUndoPoints() {
        if (undo_points.size() > 0) {
            undo_points.clear();
            setChanged();
            notifyObservers();
        }
    }

    public void replaceTemporalPoints(List<Point> points) {
        temporal_points.clear();
        temporal_points.addAll(points);
        setChanged();
        notifyObservers();
    }

    public void addTemporalPoint(Point p) {
        if (temporal_points.size() > 0) {
            Point lastPoint = temporal_points.get(temporal_points.size() - 1);
            double distance = Point.distance(p, lastPoint);
            if (distance > minTemporalPointDistance) {
                temporal_points.add(p);
                setChanged();
            }
        } else {
            temporal_points.add(p);
            setChanged();
        }
        notifyObservers();
    }

    public void clearTemporalPoints() {
        temporal_points.clear();
    }

    public void addPoint(Point p, int position) {
        if (position == LAST_POSITION) {
            points.add(p);
        } else {
            points.add(position, p);

        }
        temporal_points.clear();
        undo_points.clear();
        undo_points.add(p);
        notifyObservers();
    }

    public boolean canUndo() {
        return undo_points.size() > 0;
    }

    public void clearUndoInfo() {
        undo_points.clear();
    }

    public void undoLastAdd() {
        if (undo_points.size() > 0) {
            for (Point p : undo_points) {
                points.remove(p);
            }
            undo_points.clear();
            setChanged();
            notifyObservers();
        }
    }

    @JsonIgnore
    public Point getPoint(int location) {
        return points.get(location);
    }

    @SuppressWarnings("unchecked")
    @JsonIgnore
    public List<Point> getPoints() {
        return (List<Point>) points.clone();
    }

    @JsonIgnore
    public Point getTemporalPoint(int location) {
        return temporal_points.get(location);
    }

    public void removePoint(int location) {
        points.remove(location);
    }

    public void removeTemporalPoint(int location) {
        temporal_points.remove(location);
    }

    @JsonIgnore
    public int getPointCount() {
        return points.size();
    }

    @JsonIgnore
    public boolean isCommited(int location) {
        return !undo_points.contains(points.get(location));
    }

    @JsonIgnore
    public int getTemporalPointCount() {
        return temporal_points.size();
    }

}
