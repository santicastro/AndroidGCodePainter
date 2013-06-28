package es.skastro.gcodepainter.draw.tool.text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.widget.Toast;
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.document.TracePoint;
import es.skastro.gcodepainter.draw.tool.inkpad.Inkpad;
import es.skastro.gcodepainter.draw.tool.inkpad.ToolInkpad;

public class ToolText extends ToolInkpad {
    private PointF offset = new PointF(0f, 0f);

    public ToolText(Context context, Document document, File textpadsDirectory, String text) {
        super(context, document, null);
        List<TracePoint> points = new ArrayList<TracePoint>();
        for (int i = 0; i < text.length(); i++) {
            Character c = text.charAt(i);
            Log.d("char", "char:" + (int) Character.toLowerCase(c));
            File charFile = new File(textpadsDirectory.getAbsolutePath() + File.separator
                    + (int) Character.toLowerCase(c) + ".ipa");
            if (charFile.exists()) {
                Inkpad opened = Inkpad.fromFile(charFile);
                PointF startOffset = opened.getStartOffset();
                offset.x -= startOffset.x;
                points.addAll(opened.getPoints(offset, 1f, 0f));
                RectF charBounds = opened.getBounds();
                offset.x += (charBounds.right - charBounds.left) * 1.22 + startOffset.x;
            } else {
                Toast.makeText(context, "Non existe o caracter " + c, Toast.LENGTH_SHORT).show();
            }
        }
        Inkpad inkpad = new Inkpad(points);
        super.setInkpad(inkpad);
    }
}
