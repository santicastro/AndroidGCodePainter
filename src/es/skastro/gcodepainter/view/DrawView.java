package es.skastro.gcodepainter.view;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import es.skastro.gcodepainter.draw.CoordinateConversor;
import es.skastro.gcodepainter.draw.DrawFile;
import es.skastro.gcodepainter.draw.Point;
import es.skastro.gcodepainter.draw.inkpad.Inkpad;

// Code based on http://marakana.com/tutorials/android/2d-graphics-example.html

public class DrawView extends View implements OnTouchListener, Observer {

    public enum DrawMode {
        LINE, INKPAD
    }

    Inkpad inkpad;
    DrawMode drawMode;

    Paint paint_sent_lines = new Paint();
    Paint paint_unsent_lines = new Paint();
    Paint paint_points = new Paint();
    Paint paint_highlight_points = new Paint();
    final int point_thickness = 3;

    DrawFile drawFile;
    Paint paint_temporal_lines = new Paint();

    private void DrawViewInit() {
        setFocusable(true);
        setFocusableInTouchMode(true);

        this.setOnTouchListener(this);

        paint_highlight_points.setColor(Color.BLUE);

        paint_points.setColor(Color.BLACK);

        paint_sent_lines.setColor(Color.BLACK);
        paint_sent_lines.setStrokeWidth(2);
        paint_sent_lines.setAntiAlias(true);

        paint_unsent_lines.setColor(Color.BLUE);
        paint_unsent_lines.setStrokeWidth(2);
        paint_unsent_lines.setAntiAlias(true);

        paint_temporal_lines.setColor(Color.RED);
        paint_temporal_lines.setAntiAlias(true);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        DrawViewInit();
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        DrawViewInit();
    }

    public DrawView(Context context) {
        super(context);
        DrawViewInit();
    }

    public DrawFile getDrawFile() {
        return drawFile;
    }

    public void setDrawFile(DrawFile drawFile) {
        this.drawFile = drawFile;
        drawFile.addObserver(this);
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        // RelativeLayout.LayoutParams actual = (RelativeLayout.LayoutParams) this.getLayoutParams();
        // this.setLayoutParams(new RelativeLayout.LayoutParams((int) ((double) actual.height * 0.4762), actual.width));
        // this.setLayoutParams(new RelativeLayout.LayoutParams(500, 500));

    }

    public void setBackground(File f) {

    }

    @Override
    public void onDraw(Canvas canvas) {
        if (drawFile != null) {
            Point start;
            Point end = null;
            int pointCount = drawFile.getPointCount();
            if (pointCount > 0) {
                end = drawFile.getPoint(0);
                if (point_thickness > 0)
                    canvas.drawCircle((float) end.getX(), (float) end.getY(), (int) (point_thickness * 1.5),
                            paint_highlight_points);
                for (int i = 1; i < pointCount; i++) {
                    start = end;
                    end = drawFile.getPoint(i);
                    if (drawFile.isCommited(i))
                        auxDrawLine(canvas, start, end, paint_sent_lines, paint_points);
                    else
                        auxDrawLine(canvas, start, end, paint_unsent_lines, paint_points);
                }
            }

            for (int i = 0; i < drawFile.getTemporalPointCount(); i++) {
                start = end;
                end = drawFile.getTemporalPoint(i);
                if (start != null) {
                    auxDrawLine(canvas, start, end, paint_temporal_lines, null);
                }
            }
        }
    }

    private void auxDrawLine(Canvas canvas, Point start, Point end, Paint paint_lines, Paint paint_points) {
        canvas.drawLine((float) start.getX(), (float) start.getY(), (float) end.getX(), (float) end.getY(), paint_lines);
        if (point_thickness > 0 && paint_points != null)
            canvas.drawCircle((float) end.getX(), (float) end.getY(), point_thickness, paint_points);

    }

    Point a_bottomLeft = new Point(0.0, 625.0);
    Point a_topRight = new Point(922, 0);

    Point b_bottomLeft = new Point(0.0, 0.0);
    Point b_topRight = new Point(155.0, 105.0);

    CoordinateConversor conv = new CoordinateConversor(a_bottomLeft, a_topRight, b_bottomLeft, b_topRight);

    Point firstTouchPoint = null;

    public boolean onTouch(View view, MotionEvent event) {
        if (drawFile != null) {
            Point point = new Point(event.getX(), event.getY());
            if (drawMode == DrawMode.LINE) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    drawFile.commitUndoPoints();
                    drawFile.clearTemporalPoints();
                    drawFile.addTemporalPoint(point);
                    break;
                case MotionEvent.ACTION_MOVE:
                    drawFile.addTemporalPoint(point);
                    break;
                case MotionEvent.ACTION_UP:
                    drawFile.addTemporalPoint(point);
                    drawFile.commitTemporalPoints(false);
                    break;
                }
            } else if (drawMode == DrawMode.INKPAD) {
                double distance, scale;
                switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    firstTouchPoint = point;
                    drawFile.commitUndoPoints();
                    firstTouchPoint = new Point(event.getX(), event.getY());
                    distance = Point.distance(firstTouchPoint, point);
                    scale = Math.max(0.3, distance / 250);
                    drawFile.replaceTemporalPoints(inkpad.getPoints(firstTouchPoint, scale));
                    drawFile.addTemporalPoint(point);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (firstTouchPoint != null) {
                        distance = Point.distance(firstTouchPoint, point);
                        scale = Math.max(0.3, distance / 250);
                        drawFile.replaceTemporalPoints(inkpad.getPoints(firstTouchPoint, scale));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    drawFile.commitTemporalPoints(true);
                    break;
                }

            }

        }
        return true;
    }

    @Override
    public void update(Observable observable, Object data) {
        invalidate();
    }

    public void setDrawMode(DrawMode mode) {
        this.drawMode = mode;
    }

    public void setInkpad(Inkpad inkpad) {
        this.inkpad = inkpad;
    }
}
