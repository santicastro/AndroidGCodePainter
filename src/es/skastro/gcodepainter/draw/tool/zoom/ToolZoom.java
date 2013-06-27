package es.skastro.gcodepainter.draw.tool.zoom;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.document.Point;
import es.skastro.gcodepainter.draw.tool.Tool;
import es.skastro.gcodepainter.view.DrawView;

public class ToolZoom extends Tool {

    public ToolZoom(Context context, Document document, DrawView drawView) {
        super(context, document);
        this.drawView = drawView;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        maxScale = 5f;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public void changeScale(float scale) {
        if (drawView != null) {
            Log.d("changeScale", "changeScale: " + scale);
            drawView.setScaleFactor(Math.max(1f, Math.min(scale, maxScale)));
            setChanged();
            notifyObservers();
        }
    }

    @Override
    public void onTouch(DrawView drawView, MotionEvent event, PointF translatedPoint) {
        if (event.getPointerCount() > 1) {
            firstTouchPoint = null;
            mScaleDetector.onTouchEvent(event);
        } else {
            PointF point = new PointF(event.getX(), -event.getY());
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                firstTouchPoint = point;
                oldTranslation = drawView.getTranslate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (firstTouchPoint != null) {
                    PointF move = Point.plus(oldTranslation, Point.minus(point, firstTouchPoint));
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
            changeScale(scaleFactor);
            return true;
        }
    }

    private ScaleGestureDetector mScaleDetector;

    private DrawView drawView;
    private PointF firstTouchPoint;
    private PointF oldTranslation;
    private float maxScale;

}