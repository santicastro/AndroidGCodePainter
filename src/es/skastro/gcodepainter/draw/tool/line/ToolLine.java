package es.skastro.gcodepainter.draw.tool.line;

import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.document.TracePoint;
import es.skastro.gcodepainter.draw.tool.Tool;
import es.skastro.gcodepainter.view.DrawView;

public class ToolLine extends Tool {

    public ToolLine(Context context, Document document) {
        super(context, document);
    }

    @Override
    public void onTouch(DrawView drawView, MotionEvent event, PointF translatedPoint) {
        TracePoint tpoint = new TracePoint(drawView.getCoordinateView2Document().calculate(translatedPoint));
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            traceId = document.createTrace();
            document.addPoint(traceId, tpoint);
            break;
        case MotionEvent.ACTION_MOVE:
            document.addPoint(traceId, tpoint);
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_OUTSIDE:
            document.commitTrace(traceId);
            event.setAction(MotionEvent.ACTION_CANCEL);
            traceId = -1;
            break;
        }

    }

    private int traceId = -1;
}
