package es.skastro.gcodepainter.view;

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
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.document.Point;
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
        mZoomTranslate = new Point(0.f, 0.f);
    }

    public float getScaleFactor() {
        return mZoomFactor;
    }

    public void setScaleFactor(float newFactor) {
        newFactor = Math.max(1.f, Math.min(newFactor, 5.0f));

        if (Float.compare(mZoomFactor, newFactor) != 0) {
            double relation = newFactor - mZoomFactor;
            double deltaX = 0f, deltaY = 0f;

            // TODO: correct this, it's not exact
            deltaX = view_topRight.getX() / mZoomFactor * relation / 2;
            deltaY = view_topRight.getY() / mZoomFactor * relation / 2;
            Point newZoomTranslate = new Point(mZoomTranslate.getX() - deltaX, mZoomTranslate.getY() - deltaY);

            this.mZoomFactor = newFactor;

            checkTranslate(newZoomTranslate);
            this.mZoomTranslate = newZoomTranslate;
            invalidate();
        }
    }

    public Point getTranslate() {
        return mZoomTranslate;
    }

    public void setTranslate(Point mTranslate) {
        checkTranslate(mTranslate);
        if (!mZoomTranslate.equals(mTranslate)) {
            this.mZoomTranslate = mTranslate;
            invalidate();
        }
    }

    private boolean checkTranslate(Point mTranslate) {
        boolean res = false;
        // assure translation is on limits
        if (mTranslate.getX() > 0.0) {
            mTranslate.setX(0.0f);
            res = true;
        }
        if (mTranslate.getY() > 0.0) {
            mTranslate.setY(0.0f);
            res = true;
        }

        if ((view_topRight.getX() - mTranslate.getX()) / mZoomFactor > view_topRight.getX()) {
            mTranslate.setX(view_topRight.getX() - view_topRight.getX() * mZoomFactor);
            res = true;
        }
        if ((view_topRight.getY() - mTranslate.getY()) / mZoomFactor > view_topRight.getY()) {
            mTranslate.setY(view_topRight.getY() - view_topRight.getY() * mZoomFactor);
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
        coordinateDocument2View = new CoordinateConversor(document.bottomLeft, document.topRight, view_bottomLeft,
                view_topRight);
        coordinateView2Document = new CoordinateConversor(view_bottomLeft, view_topRight, document.bottomLeft,
                document.topRight);
        this.document = document;
        document.addObserver(this);
        invalidate();
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    // @Override
    // protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // if (view_topRight == null)
    //
    // super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    // }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        view_topRight = new Point(getWidth(), getHeight());
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void onDraw(Canvas canvas) {

        canvas.save();
        canvas.scale(1.0f, -1.0f);
        canvas.translate(0f, -(float) view_topRight.getY());
        canvas.translate((float) mZoomTranslate.getX(), (float) mZoomTranslate.getY());
        canvas.scale(mZoomFactor, mZoomFactor);

        canvas.drawLine((float) borders[0].getX(), (float) borders[0].getY(), (float) borders[1].getX(),
                (float) borders[1].getY(), paint_border);
        canvas.drawLine((float) borders[0].getX(), (float) borders[0].getY(), (float) borders[3].getX(),
                (float) borders[3].getY(), paint_border);
        canvas.drawLine((float) borders[2].getX(), (float) borders[2].getY(), (float) borders[1].getX(),
                (float) borders[1].getY(), paint_border);
        canvas.drawLine((float) borders[2].getX(), (float) borders[2].getY(), (float) borders[3].getX(),
                (float) borders[3].getY(), paint_border);
        // canvas.drawLine((float) borders[2].getX(), (float) borders[2].getY(), (float) borders[0].getX(),
        // (float) borders[0].getY(), paint_border);

        if (document != null) {
            Point start;
            Point end = null;
            int pointCount = document.getPointCount();
            if (pointCount > 0) {
                end = coordinateDocument2View.calculate(document.getPoint(0).getPoint());
                if (point_thickness > 0)
                    canvas.drawCircle((float) end.getX(), (float) end.getY(), (int) (point_thickness * 1.5),
                            paint_highlight_points);
                for (int i = 1; i < pointCount; i++) {
                    start = end;
                    end = coordinateDocument2View.calculate(document.getPoint(i).getPoint());
                    // if (drawFile.isCommited(i))
                    auxDrawLine(canvas, start, end, paint_sent_lines, paint_points);
                    // else
                    // auxDrawLine(canvas, start, end, paint_unsent_lines, paint_points);
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
        Point translatedPoint = new Point((event.getX() - mZoomTranslate.getX()) / mZoomFactor, (view_topRight.getY()
                - event.getY() - mZoomTranslate.getY())
                / mZoomFactor);
        tool.onTouch(DrawView.this, event, translatedPoint);
        return true;
    }

    @Override
    public void update(Observable observable, Object data) {
        invalidate();
    }

    // @Override
    // protected void onAttachedToWindow() {
    // super.onAttachedToWindow();
    // // RelativeLayout.LayoutParams actual = (RelativeLayout.LayoutParams) this.getLayoutParams();
    // // this.setLayoutParams(new RelativeLayout.LayoutParams((int) ((double) actual.height * 0.4762), actual.width));
    // // this.setLayoutParams(new RelativeLayout.LayoutParams(500, 500));
    // }

    private void auxDrawLine(Canvas canvas, Point start, Point end, Paint paint_lines, Paint paint_points) {
        canvas.drawLine((float) start.getX(), (float) start.getY(), (float) end.getX(), (float) end.getY(), paint_lines);
        if (point_thickness > 0 && paint_points != null)
            canvas.drawCircle((float) end.getX(), (float) end.getY(), point_thickness, paint_points);
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

    private Point view_bottomLeft = new Point(0.0, 0.0);
    private Point view_topRight = new Point(922.0, 625.0);

    final int BORDER_WEIGHT = 3;
    Point[] borders = { new Point(view_bottomLeft.getX() - BORDER_WEIGHT, (view_bottomLeft.getY() - BORDER_WEIGHT)),
            new Point(view_bottomLeft.getX() - BORDER_WEIGHT, (view_topRight.getY() + BORDER_WEIGHT)),
            new Point(view_topRight.getX() + BORDER_WEIGHT, (view_topRight.getY() + BORDER_WEIGHT)),
            new Point(view_topRight.getX() + BORDER_WEIGHT, (view_bottomLeft.getY() - BORDER_WEIGHT)) };

    private float mZoomFactor;
    private Point mZoomTranslate;

    private CoordinateConversor coordinateDocument2View;
    private CoordinateConversor coordinateView2Document;

}