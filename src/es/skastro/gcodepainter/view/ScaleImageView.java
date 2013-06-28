package es.skastro.gcodepainter.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ScaleImageView extends ImageView {

    public ScaleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        DrawViewInit();
    }

    public ScaleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        DrawViewInit();
    }

    public ScaleImageView(Context context) {
        super(context);
        DrawViewInit();
    }

    public void resetZoom() {
        mZoomFactor = 1.f;
        mZoomTranslate = new PointF(0.f, 0.f);
    }

    public float getZoomFactor() {
        return mZoomFactor;
    }

    public void setZoomFactor(float newFactor) {
        mZoomFactor = newFactor;
        invalidate();
    }

    public PointF getmZoomTranslate() {
        return mZoomTranslate;
    }

    public void setmZoomTranslate(PointF mZoomTranslate) {
        this.mZoomTranslate = mZoomTranslate;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap == null)
            bm = null;
        else {
            bitmap.prepareToDraw();
            float scale = getHeight() / (float) bitmap.getHeight();
            int width = (int) (bitmap.getWidth() * scale);
            marginLeft = (getWidth() - width) / 2;
            bm = Bitmap.createScaledBitmap(bitmap, width, getHeight(), true);
        }
        super.setImageBitmap(bm);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (bm != null) {
            canvas.save();
            canvas.translate(0, getHeight() * (1 - mZoomFactor));
            canvas.translate(mZoomTranslate.x, -mZoomTranslate.y);
            canvas.scale(mZoomFactor, mZoomFactor);
            canvas.translate(marginLeft, 0f);
            canvas.drawBitmap(bm, 0f, 0f, bitmapPaint);
            canvas.restore();
        }
    }

    private void DrawViewInit() {
        setAlpha(0.5f);
        resetZoom();
        setFocusable(false);
        setFocusableInTouchMode(false);
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);
    }

    private Bitmap bm;
    private Paint bitmapPaint = new Paint();
    private int marginLeft = 0;
    private float mZoomFactor;
    private PointF mZoomTranslate;
}