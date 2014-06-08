package es.skastro.gcodepainter.draw.tool.zoom;

import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.tool.Tool;
import es.skastro.gcodepainter.draw.util.PointFUtils;
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
            drawView.setZoomFactor(Math.max(1f, Math.min(scale, maxScale)));
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
            PointF point = new PointF(event.getX(0), -event.getY(0));
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                firstTouchPoint = point;
                oldTranslation = drawView.getZoomTranslate();
                break;

            case MotionEvent.ACTION_MOVE:
                if (firstTouchPoint != null) {
                    PointF move = PointFUtils.plus(oldTranslation, PointFUtils.minus(point, firstTouchPoint));
                    drawView.setZoomTranslate(move);
                    setChanged();
                    notifyObservers();
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
            float scaleFactor = drawView.getZoomFactor() * detector.getScaleFactor();
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