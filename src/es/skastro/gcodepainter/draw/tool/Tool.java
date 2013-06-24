package es.skastro.gcodepainter.draw.tool;

import android.content.Context;
import android.view.MotionEvent;
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.document.Point;
import es.skastro.gcodepainter.view.DrawView;

public abstract class Tool {
    public Tool(Context context, Document document) {
        this.context = context;
        setDocument(document);
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    protected Document document;
    protected Context context;

    public abstract void onTouch(DrawView drawView, MotionEvent event, Point translatedPoint);
}
