package es.skastro.gcodepainter.view;

import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.tool.Tool;
import es.skastro.gcodepainter.draw.util.CoordinateConversor;

public class DrawView extends View implements OnTouchListener, Observer {

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

    public void resetZoom() {
        mZoomFactor = 1.f;
        mZoomTranslate = new PointF(0.f, 0.f);
    }

    public float getScaleFactor() {
        return mZoomFactor;
    }

    public void setScaleFactor(float newFactor) {
        newFactor = Math.max(1.f, Math.min(newFactor, 5.0f));
        if (Float.compare(mZoomFactor, newFactor) != 0) {
            float relation = newFactor - mZoomFactor;
            float deltaX = 0f, deltaY = 0f;

            // TODO: correct this, it's not exact
            deltaX = margins.right / mZoomFactor * relation / 2;
            deltaY = margins.top / mZoomFactor * relation / 2;
            PointF newZoomTranslate = new PointF(mZoomTranslate.x - deltaX, mZoomTranslate.y - deltaY);

            this.mZoomFactor = newFactor;

            checkTranslate(newZoomTranslate);
            this.mZoomTranslate = newZoomTranslate;
            invalidate();
        }
    }

    public PointF getTranslate() {
        return mZoomTranslate;
    }

    public void setTranslate(PointF mTranslate) {
        checkTranslate(mTranslate);
        if (!mZoomTranslate.equals(mTranslate)) {
            this.mZoomTranslate = mTranslate;
            invalidate();
        }
    }

    private boolean checkTranslate(PointF mTranslate) {
        boolean res = false;
        if (mTranslate.x > 0.0) {
            mTranslate.x = 0f;
            res = true;
        }
        if (mTranslate.y > 0.0) {
            mTranslate.y = 0f;
            res = true;
        }

        if ((margins.right - mTranslate.x) / mZoomFactor > margins.right) {
            mTranslate.x = margins.right - margins.right * mZoomFactor;
            res = true;
        }
        if ((margins.top - mTranslate.y) / mZoomFactor > margins.top) {
            mTranslate.y = margins.top - margins.top * mZoomFactor;
            res = true;
        }
        return res;
    }

    public CoordinateConversor getCoordinateDocument2View() {
        return coordinateDocument2View;
    }

    public CoordinateConversor getCoordinateView2Document() {
        return coordinateView2Document;
    }

    public void setDocument(Document document) {
        resetZoom();
        coordinateDocument2View = new CoordinateConversor(document.getMargins(), this.margins);
        coordinateView2Document = new CoordinateConversor(this.margins, document.getMargins());
        this.document = document;
        document.addObserver(this);
        invalidate();
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        margins = new RectF(0f, getHeight(), getWidth(), 0f);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void onDraw(Canvas canvas) {

        canvas.save();
        canvas.scale(1.0f, -1.0f);
        canvas.translate(0f, -(float) margins.top);
        canvas.translate((float) mZoomTranslate.x, (float) mZoomTranslate.y);
        canvas.scale(mZoomFactor, mZoomFactor);

        canvas.drawLine((float) borders[0].x, (float) borders[0].y, (float) borders[1].x, (float) borders[1].y,
                paint_border);
        canvas.drawLine((float) borders[0].x, (float) borders[0].y, (float) borders[3].x, (float) borders[3].y,
                paint_border);
        canvas.drawLine((float) borders[2].x, (float) borders[2].y, (float) borders[1].x, (float) borders[1].y,
                paint_border);
        canvas.drawLine((float) borders[2].x, (float) borders[2].y, (float) borders[3].x, (float) borders[3].y,
                paint_border);

        if (document != null) {
            PointF start;
            PointF end = null;
            int pointCount = document.getPointCount();
            if (pointCount > 0) {
                end = coordinateDocument2View.calculate(document.getPoint(0).getPoint());
                if (point_thickness > 0)
                    canvas.drawCircle(end.x, end.y, (int) (point_thickness * 1.5), paint_highlight_points);
                for (int i = 1; i < pointCount; i++) {
                    start = end;
                    end = coordinateDocument2View.calculate(document.getPoint(i).getPoint());
                    auxDrawLine(canvas, start, end, paint_sent_lines, paint_points);
                }
            }

            if (document.getTemporalPoints() != null) {
                for (int i = 0; i < document.getTemporalPoints().size(); i++) {
                    start = end;
                    end = coordinateDocument2View.calculate(document.getTemporalPoints().get(i).getPoint());
                    if (start != null) {
                        auxDrawLine(canvas, start, end, paint_temporal_lines, null);
                    }
                }
            }
        }
        canvas.restore();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        PointF translatedPoint = new PointF((event.getX() - mZoomTranslate.x) / mZoomFactor, (margins.top
                - event.getY() - mZoomTranslate.y)
                / mZoomFactor);
        tool.onTouch(DrawView.this, event, translatedPoint);
        return true;
    }

    @Override
    public void update(Observable observable, Object data) {
        invalidate();
    }

    private void auxDrawLine(Canvas canvas, PointF start, PointF end, Paint paint_lines, Paint paint_points) {
        canvas.drawLine((float) start.x, (float) start.y, (float) end.x, (float) end.y, paint_lines);
        if (point_thickness > 0 && paint_points != null)
            canvas.drawCircle((float) end.x, (float) end.y, point_thickness, paint_points);
    }

    private void DrawViewInit() {

        resetZoom();
        setFocusable(true);
        setFocusableInTouchMode(true);

        this.setOnTouchListener(this);

        paint_highlight_points.setColor(Color.BLUE);

        paint_points.setColor(Color.BLUE);

        paint_border.setColor(Color.RED);
        paint_border.setStrokeWidth(BORDER_WEIGHT * 2);
        paint_border.setAntiAlias(true);

        paint_sent_lines.setColor(Color.BLACK);
        paint_sent_lines.setStrokeWidth(2);
        paint_sent_lines.setAntiAlias(true);

        paint_unsent_lines.setColor(Color.BLUE);
        paint_unsent_lines.setStrokeWidth(2);
        paint_unsent_lines.setAntiAlias(true);

        paint_temporal_lines.setColor(Color.RED);
        paint_temporal_lines.setAntiAlias(true);
    }

    private Tool tool;
    private Document document;

    private Paint paint_sent_lines = new Paint();
    private Paint paint_border = new Paint();
    private Paint paint_unsent_lines = new Paint();
    private Paint paint_points = new Paint();
    private Paint paint_highlight_points = new Paint();
    final int point_thickness = 3;

    private Paint paint_temporal_lines = new Paint();

    // private Point view_bottomLeft = new Point(0.0, 0.0);
    // private Point view_topRight = new Point(922.0, 625.0);

    private RectF margins = new RectF(0f, 625f, 922f, 0);

    final int BORDER_WEIGHT = 3;
    PointF[] borders = { new PointF(margins.left - BORDER_WEIGHT, (margins.bottom - BORDER_WEIGHT)),
            new PointF(margins.left - BORDER_WEIGHT, (margins.top + BORDER_WEIGHT)),
            new PointF(margins.right + BORDER_WEIGHT, (margins.top + BORDER_WEIGHT)),
            new PointF(margins.right + BORDER_WEIGHT, (margins.bottom - BORDER_WEIGHT)) };

    private float mZoomFactor;
    private PointF mZoomTranslate;

    private CoordinateConversor coordinateDocument2View;
    private CoordinateConversor coordinateView2Document;

}