package es.skastro.gcodepainter.draw.tool.inkpad;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.document.Point;
import es.skastro.gcodepainter.draw.tool.Tool;
import es.skastro.gcodepainter.view.DrawView;

public class ToolInkpad extends Tool {

    public ToolInkpad(Context context, Document document, Inkpad inkpad) {
        super(context, document);
        this.inkpad = inkpad;
    }

    @Override
    public void onTouch(DrawView drawView, MotionEvent event, Point translatedPoint) {
        double distance, scale, angle;
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            firstTouchPoint = translatedPoint;
        case MotionEvent.ACTION_MOVE:
            traceId = document.createTrace();
            document.getTemporalPoints().clear();
            distance = Point.distance(firstTouchPoint, translatedPoint);
            scale = Math.max(0.15, distance / 250);
            if (scale > 0.95 && scale < 1.05)
                scale = 1; // this allows you to make a 1:1 clone easier
            if (firstTouchPoint.equals(translatedPoint)) {
                angle = 0.0;
            } else {
                vector = Point.minus(translatedPoint, firstTouchPoint);
                angle = Point.angle(compareVector, vector);
                if (angle > -0.1 && angle < 0.1)
                    angle = 0;
            }
            Log.w("angle", "angle: " + angle);
            document.addPoints(traceId,
                    inkpad.getPoints(drawView.getCoordinateView2Document().calculate(firstTouchPoint), scale, angle));
            break;
        case MotionEvent.ACTION_UP:
            document.commitTrace(traceId);
            break;
        }

    }

    private Point firstTouchPoint, vector;
    private Point compareVector = new Point(0, 1);
    private int traceId = -1;
    private Inkpad inkpad;
}
