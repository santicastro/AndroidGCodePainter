package es.skastro.gcodepainter.draw.tool.inkpad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.graphics.PointF;
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.document.Point;
import es.skastro.gcodepainter.draw.document.TracePoint;

public class Inkpad {

    @JsonProperty("points")
    List<TracePoint> points;

    private static ObjectMapper mapper;
    static {
        mapper = new ObjectMapper();
    }

    public static Inkpad fromDrawFile(Document file) {
        List<TracePoint> points = file.getPoints();
        if (points.size() > 0)
            points.remove(0);
        return new Inkpad(points);
    }

    public static Inkpad fromFile(File file) {
        try {
            return mapper.readValue(file, Inkpad.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Inkpad() {
    }

    public Inkpad(List<TracePoint> points) {
        this.points = new ArrayList<TracePoint>();
        if (points.size() > 0) {

            this.points.add(new TracePoint(new PointF(0f, 0f)));
            PointF origin = points.get(0).getPoint();
            for (int i = 1; i < points.size(); i++) {
                this.points.add(new TracePoint(Point.minus(points.get(i).getPoint(), origin)));
            }
        }
    }

    public void saveToDisk(File file) throws IOException {
        File tmp = File.createTempFile("tampon", ".ske");
        OutputStream output = new FileOutputStream(tmp);

        mapper.writeValue(output, this);
        output.close();

        if (file.exists())
            file.delete();
        FileUtils.copyFile(tmp, file);
    }

    public List<TracePoint> getPoints(PointF basePoint, double scale, double angle) {
        List<TracePoint> res = new ArrayList<TracePoint>(points.size());
        for (TracePoint p : points) {
            PointF rotated = Point.rotate(p.getPoint(), angle);
            res.add(new TracePoint(new PointF((float) (rotated.x * scale + basePoint.x),
                    (float) (rotated.y * scale + basePoint.y))));
        }
        return res;
    }

}
