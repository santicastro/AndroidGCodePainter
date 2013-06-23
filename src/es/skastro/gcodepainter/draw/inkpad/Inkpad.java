package es.skastro.gcodepainter.draw.inkpad;

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

import es.skastro.gcodepainter.draw.DrawFile;
import es.skastro.gcodepainter.draw.Point;

public class Inkpad {

    @JsonProperty("points")
    List<Point> points;

    private static ObjectMapper mapper;
    static {
        mapper = new ObjectMapper();
    }

    public static Inkpad fromDrawFile(DrawFile file) {
        List<Point> points = file.getPoints();
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

    public Inkpad(List<Point> points) {
        this.points = new ArrayList<Point>();
        if (points.size() > 0) {

            this.points.add(new Point(0.0, 0.0));
            Point origin = points.get(0);
            for (int i = 1; i < points.size(); i++) {
                this.points.add(Point.minus(points.get(i), origin));
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

    public List<Point> getPoints(Point basePoint, double scale) {
        List<Point> res = new ArrayList<Point>(points.size());
        for (Point p : points) {
            res.add(new Point(p.getX() * scale + basePoint.getX(), p.getY() * scale + basePoint.getY()));
        }
        return res;
    }

}
