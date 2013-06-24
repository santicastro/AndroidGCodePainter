package es.skastro.gcodepainter.draw.tool.zoom;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.document.Point;
import es.skastro.gcodepainter.draw.tool.Tool;
import es.skastro.gcodepainter.view.DrawView;

public class ToolZoom extends Tool {

    public ToolZoom(Context context, Document document) {
        super(context, document);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    public void onTouch(DrawView drawView, MotionEvent event, Point translatedPoint) {
        this.drawView = drawView;
        if (event.getPointerCount() > 1) {
            firstTouchPoint = null;
            mScaleDetector.onTouchEvent(event);
        } else {
            Point point = new Point(event.getX(), event.getY());
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                firstTouchPoint = point;
                oldTranslation = drawView.getTranslate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (firstTouchPoint != null) {
                    Point move = Point.plus(oldTranslation, Point.minus(point, firstTouchPoint));
                    drawView.setTranslate(move);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_OUTSIDE:
                event.setAction(MotionEvent.ACTION_CANCEL);
                break;
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = drawView.getScaleFactor() * detector.getScaleFactor();
            scaleFactor = Math.max(1.f, Math.min(scaleFactor, 5.0f));
            drawView.setScaleFactor(scaleFactor);
            // Don't let the object get too small or too large.
            return true;
        }
    }

    private ScaleGestureDetector mScaleDetector;

    private DrawView drawView;
    private Point firstTouchPoint;
    private Point oldTranslation;
}
