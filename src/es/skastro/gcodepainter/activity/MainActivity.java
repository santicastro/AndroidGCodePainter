package es.skastro.gcodepainter.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import es.skastro.android.util.alert.SimpleListAdapter;
import es.skastro.android.util.alert.SimpleOkAlertDialog;
import es.skastro.android.util.alert.StringPrompt;
import es.skastro.android.util.bitmap.BitmapUtils;
import es.skastro.android.util.bluetooth.BluetoothService;
import es.skastro.android.util.bluetooth.DeviceListActivity;
import es.skastro.android.util.component.VerticalSeekBar;
import es.skastro.gcodepainter.R;
import es.skastro.gcodepainter.draw.document.Document;
import es.skastro.gcodepainter.draw.document.Trace;
import es.skastro.gcodepainter.draw.document.TracePoint;
import es.skastro.gcodepainter.draw.tool.inkpad.Inkpad;
import es.skastro.gcodepainter.draw.tool.inkpad.ToolInkpad;
import es.skastro.gcodepainter.draw.tool.line.ToolLine;
import es.skastro.gcodepainter.draw.tool.text.ToolText;
import es.skastro.gcodepainter.draw.tool.zoom.ToolZoom;
import es.skastro.gcodepainter.draw.util.CoordinateConversor;
import es.skastro.gcodepainter.view.DrawView;
import es.skastro.gcodepainter.view.ScaleImageView;

/***
 * Real etch-a-sketch drawable dimmensions: 15.5cm x 10.5cm (aspect ratio: 1,4762)
 * 
 * @author Santi
 * 
 */

public class MainActivity extends Activity implements Observer {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set full screen view
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        messageManager = new MessageManager();
        messageManager.addObserver(this);

        pbSent = (ProgressBar) findViewById(R.id.pbSent);
        txtSent = (TextView) findViewById(R.id.txtSent);
        layoutSent = (LinearLayout) findViewById(R.id.sendLayout);

        chkAutomaticSend = (CheckBox) findViewById(R.id.chkAutomaticSend);
        chkAutomaticSend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // sendCommitedPoints();
                }
            }
        });

        btnToolLine = (ImageButton) findViewById(R.id.toolLine);
        btnToolLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectToolLine();
            }
        });

        btnToolInkpad = (ImageButton) findViewById(R.id.toolInkpad);
        registerForContextMenu(btnToolInkpad);
        btnToolInkpad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.openContextMenu(v);
            }
        });

        btnToolZoom = (ImageButton) findViewById(R.id.toolZoom);
        btnToolZoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectToolZoom();
            }
        });

        btnToolText = (ImageButton) findViewById(R.id.toolText);
        btnToolText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringPrompt sp = new StringPrompt(MainActivity.this, "Texto a escribir", "", "") {

                    @Override
                    public boolean onOkClicked(String value) {
                        selectToolText(value);
                        return true;
                    }
                };
                sp.show();
            }
        });

        btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendRemainPoints();
            }
        });
        btnConnect = (Button) findViewById(R.id.buttonConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent0 = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(intent0, CONNECT_BLUETOOTH_SECURE);
            }
        });

        btnUndo = (Button) findViewById(R.id.buttonUndo);
        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                document.undo();
            }
        });

        btnRedo = (Button) findViewById(R.id.buttonRedo);
        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                document.redo();
            }
        });

        drawBackground = (ScaleImageView) findViewById(R.id.drawBackground);

        drawView = (DrawView) findViewById(R.id.drawView);

        toolZoom = new ToolZoom(getApplicationContext(), null, drawView);
        toolZoom.addObserver(this);

        zoomBar = (VerticalSeekBar) findViewById(R.id.zoomBar);
        zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.isSelected())
                    toolZoom.changeScale(1 + (toolZoom.getMaxScale() - 1) * ((float) progress / seekBar.getMax()));
            }
        });

        zoomText = (TextView) findViewById(R.id.zoomText);

        newDraw();
        drawView.requestFocus();
        if ((mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            // finish();
            return;
        } else {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
            }
        }

        selectToolLine();
        updateZoomInfo();
    }

    private void changeDocument(Document document) {
        if (document == null) {
            SimpleOkAlertDialog.show(MainActivity.this, "Erro",
                    "Houbo un problema cambiando a imaxe. Volva a intentalo.");
            finish();
        } else {
            if (this.document != null)
                this.document.deleteObservers();
            this.document = document;
            // if(toolZoom!= null)
            toolZoom.setDocument(document);
            drawView.setDocument(document);
            document.addObserver(this);
            update(document, null);
            selectToolLine();
            resetGcodeConversion();
            updateZoomInfo();
        }
    }

    private void selectToolLine() {
        ToolLine tline = new ToolLine(getApplicationContext(), document);
        drawView.setTool(tline);
        btnToolLine.setBackground(getResources().getDrawable(R.drawable.pressed));
        btnToolInkpad.setBackground(getResources().getDrawable(R.drawable.not_pressed));
        btnToolZoom.setBackground(getResources().getDrawable(R.drawable.not_pressed));
        btnToolText.setBackground(getResources().getDrawable(R.drawable.not_pressed));
    }

    private void selectToolInkpad(Inkpad inkpad, String name) {
        ToolInkpad tinkpad = new ToolInkpad(getApplicationContext(), document, inkpad);
        drawView.setTool(tinkpad);
        btnToolInkpad.setBackground(getResources().getDrawable(R.drawable.pressed));
        btnToolLine.setBackground(getResources().getDrawable(R.drawable.not_pressed));
        btnToolZoom.setBackground(getResources().getDrawable(R.drawable.not_pressed));
        btnToolText.setBackground(getResources().getDrawable(R.drawable.not_pressed));
    }

    private void selectToolZoom() {
        drawView.setTool(toolZoom);
        btnToolZoom.setBackground(getResources().getDrawable(R.drawable.pressed));
        btnToolLine.setBackground(getResources().getDrawable(R.drawable.not_pressed));
        btnToolInkpad.setBackground(getResources().getDrawable(R.drawable.not_pressed));
        btnToolText.setBackground(getResources().getDrawable(R.drawable.not_pressed));
    }

    private void selectToolText(String text) {
        ToolText ttext = new ToolText(getApplicationContext(), document, getTextpadsDirectory(), text);
        drawView.setTool(ttext);
        btnToolZoom.setBackground(getResources().getDrawable(R.drawable.not_pressed));
        btnToolLine.setBackground(getResources().getDrawable(R.drawable.not_pressed));
        btnToolInkpad.setBackground(getResources().getDrawable(R.drawable.not_pressed));
        btnToolText.setBackground(getResources().getDrawable(R.drawable.pressed));
    }

    private void setCurrentDrawFilename(String filename) {
        currentDrawFilename = filename;
        if (currentDrawFilename == null) {
            this.setTitle(R.string.app_name);
        } else {
            this.setTitle(getResources().getString(R.string.app_name) + " (Arquivo: " + filename + ")");
        }
    }

    private void changeBackground(String filename) {
        if (filename == null) {
            changeBackground((Bitmap) null);
        } else {
            File file = new File(filename);
            Bitmap imageBitmap;
            try {
                imageBitmap = BitmapUtils.decodeSampledBitmapFromFile(file, drawView.getWidth(), drawView.getHeight());
                changeBackground(imageBitmap);
            } catch (OutOfMemoryError ex) {
                imageBitmap = null;
                SimpleOkAlertDialog.show(MainActivity.this, "Imaxe incorrecta", "Non se puido cargar a imaxe.");
                changeBackground((Bitmap) null);
            }
        }
    }

    private void changeBackground(Bitmap bitmap) {
        drawBackground.setImageBitmap(bitmap);
    }

    private void newDraw() {
        document = new Document();
        int id = document.createTrace();
        document.addPoint(id, new TracePoint(new PointF(0f, 0f)));
        document.commitTrace(id);
        changeDocument(document);
        setCurrentDrawFilename(null);
    }

    private void openDraw() {
        File dir = getDrawsDirectory();
        final File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".ske") && !pathname.getName().startsWith(".");
            }
        });
        if (files.length == 0) {
            SimpleOkAlertDialog.show(this, "Non hai debuxos",
                    "Non se atoparon debuxos na tarxeta de memoria:\n " + dir.getAbsolutePath());
        } else {
            SimpleListAdapter<File> fileAdapter = new SimpleListAdapter<File>(MainActivity.this, Arrays.asList(files),
                    new SimpleListAdapter.StringGenerator<File>() {
                        @Override
                        public String getString(File addr) {
                            return addr.getName();
                        }
                    });
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Abrir debuxo...").setAdapter(fileAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Document opened = Document.fromFile(files[which]);
                    if (opened == null) {
                        SimpleOkAlertDialog
                                .show(MainActivity.this, "Non se puido abrir o arquivo",
                                        "Houbo un problema tentando cargar a imaxe. O arquivo pode estar danado ou non se recoñece o seu formato");
                        newDraw();
                    } else {
                        setCurrentDrawFilename(files[which].getName().replace(".ske", ""));
                        changeDocument(opened);
                    }
                }
            });
            builder.create().show();
            setCurrentDrawFilename(null);
        }
    }

    private void printFile() {
        File dir = Environment.getExternalStorageDirectory();
        final File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (pathname.getName().toLowerCase().endsWith(".gcode") || pathname.getName().toLowerCase()
                        .endsWith(".gco"))
                        && !pathname.getName().startsWith(".");
            }
        });
        if (files.length == 0) {
            SimpleOkAlertDialog.show(this, "Non hai debuxos",
                    "Non se atoparon debuxos imprimibles na tarxeta de memoria:\n " + dir.getAbsolutePath());
        } else {
            SimpleListAdapter<File> fileAdapter = new SimpleListAdapter<File>(MainActivity.this, Arrays.asList(files),
                    new SimpleListAdapter.StringGenerator<File>() {
                        @Override
                        public String getString(File addr) {
                            return addr.getName();
                        }
                    });
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Abrir debuxo...").setAdapter(fileAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    printFile(files[which].getAbsolutePath());
                }
            });
            builder.create().show();
            setCurrentDrawFilename(null);
        }
    }

    private final double BAND_HEIGHT = 80.0;
    private final double PAINTER_SIZE_HEIGHT = 400.0;

    public static void main(String[] args) {
        Pattern.compile(".*X([-]?[0-9]*([.][0-9]+)?).*").matcher("G1 X333.720 Y163.483 E10.35530").group(1);
    }

    private void printFile(String filename) {
        BufferedReader b;
        final String penUpCode = "M300 S50";
        final String penDownCode = "M300 S40";

        final Pattern G28PAT = Pattern.compile("[ \t]*G28.*");
        final Pattern G92PAT = Pattern.compile("[ \t]*G92[ \t]+E0.*");
        final Pattern G1PAT = Pattern.compile("[ \t]*G1(.*)");
        final Pattern XPAT = Pattern.compile(".*X([-]?[0-9]*([.][0-9]+)?).*");
        final Pattern YPAT = Pattern.compile(".*Y([-]?[0-9]*([.][0-9]+)?).*");
        final Pattern EPAT = Pattern.compile(".*E([-]?[0-9]*([.][0-9]+)?).*");
        final Pattern COMMENTPAT = Pattern.compile("(;.*)");
        Matcher m;

        boolean byBand = PAINTER_SIZE_HEIGHT > BAND_HEIGHT;
        boolean proccessGCode = true;

        double currentX = 0.0;
        double currentY = 0.0;
        double currentE = 0.0;
        Double newX = null, newY = null, newE = null;

        Document d = new Document(new RectF(0f, 400f, 690f, 0f));
        d.setDeleteOutboundPoints(false);
        d.setSimplifyPoints(false);

        Integer traceId = null;
        try {
            b = new BufferedReader(new FileReader(new File(filename)));
            String line;
            while ((line = b.readLine()) != null) {
                line = line.replaceAll("\\([^(]*\\)", "").trim();
                m = COMMENTPAT.matcher(line);
                if (m.matches()) {
                    line = line.replace(m.group(1), "").trim();
                }
                if (line.length() > 1) {
                    if (!proccessGCode) {
                        messageManager.sendMessage(line);
                    } else {
                        if (G92PAT.matcher(line).matches()) {
                            currentE = 0;
                        } else if (G28PAT.matcher(line).matches()) {
                            currentX = 0;
                            currentY = 0;
                        } else if (G1PAT.matcher(line).matches()) {
                            // is movement. now i check if is print movement
                            newX = getDouble(XPAT, line);
                            newY = getDouble(YPAT, line);
                            newE = getDouble(EPAT, line);
                            boolean isMovement = (newE == null || newE < currentE);
                            if (newX != null || newY != null) {
                                if (newX == null) {
                                    newX = currentX;
                                }
                                if (newY == null) {
                                    newY = currentY;
                                }
                                if (!isMovement && traceId == null) {
                                    traceId = d.createTrace();
                                    d.addPoint(traceId, new TracePoint(new PointF((float) currentX, (float) currentY)));
                                }
                                if (isMovement && traceId != null) {
                                    d.commitTrace(traceId);
                                    traceId = null;
                                }
                                if (!isMovement) {
                                    d.addPoint(traceId,
                                            new TracePoint(new PointF(newX.floatValue(), newY.floatValue())));
                                }
                            }

                            if (newX != null) {
                                currentX = newX;
                            }
                            if (newY != null) {
                                currentY = newY;
                            }
                            if (newE != null) {
                                currentE = newE;
                            }
                        }
                    }
                }
                // System.out.println(line);
            }
            if (traceId != null) {
                d.commitTrace(traceId);
            }
            b.close();
        } catch (Exception e) {
            e.printStackTrace();
            SimpleOkAlertDialog
                    .show(this, "Erro abrindo o ficheiro",
                            "Houbo un problema tentando cargar a imaxe. O arquivo pode estar danado ou non se recoñece o seu formato");
            return;
        }
        try {
            if (proccessGCode) {

                messageManager.sendMessage(penUpCode);
                for (double topBand = PAINTER_SIZE_HEIGHT; topBand > 0; topBand -= BAND_HEIGHT) {
                    double bottomBand = topBand - BAND_HEIGHT;
                    boolean isPenUp = true;
                    for (int ti = 0; ti < d.getTraceCount(); ti++) {
                        Trace t = d.getTrace(ti);
                        if (t.getPointCount() > 0) {
                            int validPoints = 0;
                            for (int pi = 0; pi < t.getPointCount() - 1; pi++) {
                                TracePoint p0 = t.getPoints().get(pi);
                                TracePoint p1 = t.getPoints().get(pi + 1);
                                int p0pos = getPointRelativePosition(p0, topBand, bottomBand);
                                int p1pos = getPointRelativePosition(p1, topBand, bottomBand);
                                if (p0pos == 0) {
                                    validPoints++;
                                }
                                if ((p0pos == 0 || p1pos == 0) && !(p0pos == -1 || p1pos == -1)) {
                                    messageManager.sendMessage("G1 X" + p0.getPoint().x + " Y" + p0.getPoint().y + " F"
                                            + (isPenUp ? 2000 : 1500));
                                    if (isPenUp) {
                                        isPenUp = false;
                                        messageManager.sendMessage(penDownCode);
                                    }
                                    if (pi == t.getPointCount() - 2) {
                                        messageManager.sendMessage("G1 X" + p1.getPoint().x + " Y" + p1.getPoint().y
                                                + " F1500");
                                    }
                                } else if (!isPenUp) {
                                    messageManager.sendMessage("G1 X" + p0.getPoint().x + " Y" + p0.getPoint().y
                                            + " F1500");
                                    isPenUp = true;
                                    messageManager.sendMessage(penUpCode);
                                }
                            }
                            if (!isPenUp) {
                                messageManager.sendMessage(penUpCode);
                                isPenUp = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            SimpleOkAlertDialog.show(this, "Erro procesando por bloques",
                    "Houbo un problema tentando procesar a image por bloques.");
        }
    }

    private int getPointRelativePosition(TracePoint p, double topBand, double bottomBand) {
        if (p.getPoint().y > topBand) {
            return -1;
        } else if (p.getPoint().y >= bottomBand) {
            return 0;
        } else {
            return 1;
        }
    }

    private static Double getDouble(Pattern pat, String text) {
        Matcher m = pat.matcher(text);
        if (m.matches()) {
            return Double.valueOf(m.group(1));
        }
        return null;
    }

    private void saveDraw() {
        try {
            if (document != null) {
                final File dir = getDrawsDirectory();
                StringPrompt sp = new StringPrompt(this, "Nome do arquivo",
                        "Escriba o nome co que quere gardar o debuxo", currentDrawFilename) {
                    @Override
                    public boolean onOkClicked(String value) {
                        try {
                            if (filenamePattern.matcher(value).matches()) {
                                setCurrentDrawFilename(value);
                                File target = new File(dir.getAbsoluteFile() + File.separator + currentDrawFilename
                                        + ".ske");
                                if (target.exists()) {
                                    SimpleOkAlertDialog.show(MainActivity.this, "Arquivo existente",
                                            "Xa existe un debuxo con ese nome, non se vai gardar.");
                                } else {
                                    document.saveToDisk(target);
                                    Toast.makeText(MainActivity.this, "Debuxo gardado", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                SimpleOkAlertDialog
                                        .show(MainActivity.this, "Nome inválido",
                                                "O nome seleccionado non é válido, só se poden utilizar letras e números, espazos e guión baixo. Volva a intentalo.");
                            }
                        } catch (Exception e) {
                            SimpleOkAlertDialog.show(MainActivity.this, "Error",
                                    "Houbo un erro mentres se gardaba o arquivo e a operación non se puido completar");
                            Log.e("MainActivity", e.getMessage());
                        }
                        return true;
                    }
                };
                sp.show();
            }
        } catch (Exception e) {
            SimpleOkAlertDialog.show(MainActivity.this, "Error",
                    "Houbo un erro mentres se gardaba o arquivo e a operación non se puido completar");
            Log.e("MainActivity", e.getMessage());
        }
    }

    private void saveInkpad() {
        try {
            if (document != null) {
                final File dir = getInkpadsDirectory();
                StringPrompt sp = new StringPrompt(this, "Nome do tampón",
                        "Escriba o nome co que quere gardar o tampón.", "") {
                    @Override
                    public boolean onOkClicked(String value) {
                        try {
                            if (filenamePattern.matcher(value).matches()) {
                                String inkpadName = value;
                                File target = new File(dir.getAbsoluteFile() + File.separator + inkpadName + ".ipa");
                                if (target.exists()) {
                                    SimpleOkAlertDialog.show(MainActivity.this, "Arquivo existente",
                                            "Xa existe un debuxo con ese nome, non se vai gardar");
                                } else {
                                    Inkpad.fromDrawFile(document).saveToDisk(target);
                                    Toast.makeText(MainActivity.this, "Tampón gardado", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                SimpleOkAlertDialog
                                        .show(MainActivity.this, "Nome inválido",
                                                "O nome seleccionado non é válido, só se poden utilizar letras e números. Volva a intentalo.");
                            }
                        } catch (Exception e) {
                            SimpleOkAlertDialog.show(MainActivity.this, "Error",
                                    "Houbo un erro mentres se gardaba o arquivo e a operación non se puido completar");
                            Log.e("MainActivity", e.getMessage());
                        }
                        return true;
                    }
                };
                sp.show();
            }
        } catch (Exception e) {
            SimpleOkAlertDialog.show(MainActivity.this, "Error",
                    "Houbo un erro mentres se gardaba o arquivo e a operación non se puido completar");
            Log.e("MainActivity", e.getMessage());
        }
    }

    public void selectBackgroundFromGallery() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, GET_IMAGE_FROM_GALLERY_RESPONSE);
    }

    public void selectBackgroundFromCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, GET_IMAGE_FROM_CAMERA_RESPONSE);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Abrir tampón...");
        fillInkpadMenu(menu);
        if (menu.size() == 0) {
            menu.close();
            SimpleOkAlertDialog.show(this, "Non hai tampóns", "Non se atoparon tampóns gardados.");
        }
    }

    private File[] inkPads;
    final int INKPAD_GROUP = 1;

    private void fillInkpadMenu(ContextMenu menu) {
        File dir = getInkpadsDirectory();
        inkPads = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".ipa") && !pathname.getName().startsWith(".");
            }
        });
        int id = 0;
        for (File f : inkPads) {
            menu.add(INKPAD_GROUP, id++, Menu.NONE, f.getName());
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == INKPAD_GROUP) {
            Inkpad opened = Inkpad.fromFile(inkPads[item.getItemId()]);
            if (opened == null) {
                SimpleOkAlertDialog
                        .show(MainActivity.this, "Non se puido abrir o arquivo",
                                "Houbo un problema tentando cargar o tampón. O arquivo pode estar danado ou non se recoñece o seu formato");
            } else {
                selectToolInkpad(opened, item.getTitle().toString().replace(".ipa", ""));
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.mnuNew:
            newDraw();
            break;
        case R.id.mnuOpen:
            openDraw();
            break;
        case R.id.mnuSave:
            saveDraw();
            break;
        case R.id.mnuPrintFile:
            printFile();
            break;
        case R.id.mnuBackgroundFile:
            // changeBackground("/storage/emulated/0/DCIM/Camera/IMG_20130617_003844.jpg");
            selectBackgroundFromGallery();
            break;
        case R.id.mnuBackgroundCamera:
            selectBackgroundFromCamera();
            break;
        case R.id.mnuBackgroundRemove:
            changeBackground((Bitmap) null);
            break;
        case R.id.mnuInkpad:
            saveInkpad();
            break;
        case R.id.mnuQuit:
            finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String address;
            switch (requestCode) {
            case CONNECT_BLUETOOTH_SECURE:
                address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                startConnection(address, true);
                break;
            case CONNECT_BLUETOOTH_INSECURE:
                address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                startConnection(address, false);
                break;
            case GET_IMAGE_FROM_CAMERA_RESPONSE:
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                changeBackground(photo);
                break;
            case GET_IMAGE_FROM_GALLERY_RESPONSE:
                Uri selectedImageUri = data.getData();
                String filename = getPath(selectedImageUri);
                changeBackground(filename);
                break;
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        String res = null;
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    @Override
    public void update(Observable observable, Object data) {
        if (observable instanceof ToolZoom) {
            updateZoomInfo();
        } else if (observable instanceof MessageManager) {
            final int max = messageManager.getTotalMessages();
            final int value = Math.max(0, max - messageManager.getUnsentMessages());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (max != value) {
                        pbSent.setMax(max);
                        pbSent.setProgress(value);
                        txtSent.setText("Enviados: " + value + "/" + max);
                        layoutSent.setVisibility(View.VISIBLE);
                    } else {
                        layoutSent.setVisibility(View.GONE);
                    }
                }
            });

        } else {
            btnUndo.setEnabled(((Document) observable).canUndo());
            btnRedo.setEnabled(((Document) observable).canRedo());
            if (chkAutomaticSend.isChecked()) {
                sendCommitedPoints();
            }
        }
    }

    private void updateZoomInfo() {
        drawBackground.setZoomFactor(drawView.getZoomFactor());
        drawBackground.setmZoomTranslate(drawView.getZoomTranslate());

        zoomText.setText(df.format(drawView.getZoomFactor()) + "x");
        if (!zoomBar.isSelected()) {
            zoomBar.setProgress((int) (((drawView.getZoomFactor() - 1f) / (toolZoom.getMaxScale() - 1f)) * zoomBar
                    .getMax()));
        }
    }

    private int lastTraceIdSent = -1;
    private RectF gcode_margins = new RectF(0f, 85.6f, 125.0f, 0f);
    private CoordinateConversor gcode_conversor;

    private void resetGcodeConversion() {
        gcode_conversor = new CoordinateConversor(document.getMargins(), gcode_margins);
        lastTraceIdSent = -1;
    }

    private void sendCommitedPoints() {
        sendPoints(true);
    }

    private void sendRemainPoints() {
        sendPoints(false);
    }

    private void sendPoints(boolean excludeLastTrace) {
        int traces;
        if (excludeLastTrace)
            traces = document.getTraceCount() - 1;
        else
            traces = document.getTraceCount();

        for (int i = 0; i < traces; i++) {
            Trace tr = document.getTrace(i);
            if (lastTraceIdSent < tr.getTraceId()) {
                for (TracePoint p : tr.getPoints()) {
                    PointF point = gcode_conversor.calculate(p.getPoint());
                    messageManager.sendMessage("G1 X" + point.x + " Y" + point.y);
                }
                lastTraceIdSent = tr.getTraceId();
            }
        }

    }

    // /////////////////////////
    // DIRECTORIES
    // /////////////////////////

    File drawsDirectory = null;

    private File getDrawsDirectory() {
        if (drawsDirectory == null) {
            drawsDirectory = new File(getApplicationContext().getExternalFilesDir(null), "draws/");
            if (!drawsDirectory.exists() && !drawsDirectory.mkdir()) {
                SimpleOkAlertDialog.show(this, "Erro abrindo o cartafol",
                        "Houbo un problema abrindo o cartafol de debuxos");
                finish();
            }
            File noMediaFile = new File(drawsDirectory, ".Nomedia");
            if (!noMediaFile.exists())
                try {
                    noMediaFile.createNewFile();
                } catch (IOException e) {

                }
        }
        return drawsDirectory;
    }

    File inkpadsDirectory = null;

    private File getInkpadsDirectory() {
        if (inkpadsDirectory == null) {
            inkpadsDirectory = new File(getApplicationContext().getExternalFilesDir(null), "inkpads/");
            if (!inkpadsDirectory.exists() && !inkpadsDirectory.mkdir()) {
                SimpleOkAlertDialog.show(this, "Erro abrindo o cartafol",
                        "Houbo un problema abrindo o cartafol de tampóns de clonado");
                finish();
            }
            File noMediaFile = new File(inkpadsDirectory, ".Nomedia");
            if (!noMediaFile.exists())
                try {
                    noMediaFile.createNewFile();
                } catch (IOException e) {

                }
        }
        return inkpadsDirectory;
    }

    File textpadsDirectory = null;

    private File getTextpadsDirectory() {
        if (textpadsDirectory == null) {
            textpadsDirectory = new File(getApplicationContext().getExternalFilesDir(null), "textpads/");
            if (!textpadsDirectory.exists() && !textpadsDirectory.mkdir()) {
                SimpleOkAlertDialog.show(this, "Erro abrindo o cartafol",
                        "Houbo un problema abrindo o cartafol de caracteres");
                finish();
            }
            File noMediaFile = new File(textpadsDirectory, ".Nomedia");
            if (!noMediaFile.exists())
                try {
                    noMediaFile.createNewFile();
                } catch (IOException e) {

                }
        }
        return textpadsDirectory;
    }

    // /////////////////////////
    // BLUETOOTH
    // /////////////////////////

    private void startConnection(String address, boolean secure) {
        if (mChatService == null)
            mChatService = new BluetoothService(this, mHandler);

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device, secure);
    }

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    // ///////////
    MessageManager messageManager;

    private class MessageManager extends Observable {

        public synchronized void sendMessage(String message) {
            queueAwake();
            if (message.length() > 0) {
                Log.d("messageQueue", "messageQueue: " + message);
                if (messagesToSend.size() > 0) {
                    totalMessages++;
                } else {
                    totalMessages = 1;
                }
                messagesToSend.add(message + "\n");
                setChanged();
                notifyObservers();
            }
        }

        public void queueAwake() {
            if (messageQueue == null || !messageQueue.isAlive()) {
                messageQueue = new MessageQueue(messagesToSend);
                messageQueue.start();
            }
        }

        public int getUnsentMessages() {
            return messagesToSend.size();
        }

        public int getTotalMessages() {
            return totalMessages;
        }

        int totalMessages = 0;
        List<String> messagesToSend = Collections.synchronizedList(new ArrayList<String>());
        MessageQueue messageQueue;

        private class MessageQueue extends Thread {
            List<String> messagesToSend;

            public MessageQueue(List<String> messagesToSend) {
                this.messagesToSend = messagesToSend;
            }

            final boolean WAIT_MODE = true;
            final float MAX_SECONDS_TO_WAIT = 3.0f;
            float waited;

            @Override
            public void run() {
                boolean stop = (messagesToSend == null);
                try {
                    while (!stop) {
                        if (messagesToSend.size() > 0) {
                            if (isInterrupted()) {
                                stop = true;
                                break;
                            }
                            if (mChatService == null || mChatService.getState() != BluetoothService.STATE_CONNECTED) {
                                Log.w("MessageQueue ", "MessageQueue: Bluetooth not connected");
                                if (MAX_SECONDS_TO_WAIT < waited)
                                    return;
                                Thread.sleep(1000);
                                waited += 1.0f;
                            } else {
                                Log.w("MessageQueue", "MessageQueue: sending " + messagesToSend.get(0));
                                if (WAIT_MODE) {
                                    while (!mChatService.writeAndWait(messagesToSend.get(0).getBytes())) {
                                        Thread.sleep(500);
                                        waited += 0.5;
                                        Log.w("MessageQueue", "MessageQueue: retrying " + messagesToSend.get(0));
                                    }
                                    waited = 0;
                                } else {
                                    mChatService.write(messagesToSend.get(0).getBytes());
                                    sleep(20);
                                    waited = 0;
                                }
                                Log.w("MessageQueue", "MessageQueue: removing " + messagesToSend.get(0));
                                messagesToSend.remove(0);
                                setChanged();
                                notifyObservers();
                            }
                        } else {
                            // Log.w("MessageQueue", "MessageQueue: No messages ");
                            if (MAX_SECONDS_TO_WAIT < waited)
                                return;
                            waited += 0.2;
                            sleep(200);
                        }
                    }
                } catch (InterruptedException e) {
                }
                super.run();
            }
        }
    }

    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        String mConnectedDeviceName = "Etch";

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    btnConnect.setBackgroundColor(getResources().getColor(R.color.light_green));
                    btnConnect.setText("Conectado: " + mConnectedDeviceName);
                    messageManager.queueAwake();
                    break;
                case BluetoothService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    btnConnect.setBackgroundColor(getResources().getColor(R.color.light_red));
                    btnConnect.setText("Conectar");
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    btnConnect.setBackgroundColor(getResources().getColor(R.color.light_red));
                    btnConnect.setText("Conectar");
                    break;
                }
                break;
            case BluetoothService.MESSAGE_WRITE:
                break;
            case BluetoothService.MESSAGE_READ:
                try {
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // Toast.makeText(getApplicationContext(), "Received: " + readMessage, Toast.LENGTH_SHORT).show();
                    Log.d("ControlActivity", "Bluetooth received: " + readMessage);
                } catch (Exception e) {
                }
                break;
            case BluetoothService.MESSAGE_DEVICE_NAME:
                // // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                // Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT)
                // .show();
                break;
            case BluetoothService.MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.TOAST),
                        Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    final static int GET_IMAGE_FROM_GALLERY_RESPONSE = 99;
    final static int GET_IMAGE_FROM_CAMERA_RESPONSE = 98;
    private DrawView drawView;
    private Document document;
    private String currentDrawFilename = null;
    private Button btnUndo, btnRedo, btnConnect, btnSend;
    private ImageButton btnToolLine, btnToolInkpad, btnToolZoom, btnToolText;
    private ScaleImageView drawBackground;
    private CheckBox chkAutomaticSend;
    private VerticalSeekBar zoomBar;
    private ToolZoom toolZoom;
    private TextView zoomText;
    private ProgressBar pbSent;
    private TextView txtSent;
    private LinearLayout layoutSent;

    private final DecimalFormat df = new DecimalFormat("0.0");
    Pattern filenamePattern = Pattern.compile("^[A-ZÑña-z0-9_ \\-]+$");

    final static int CONNECT_BLUETOOTH_SECURE = 100;
    final static int CONNECT_BLUETOOTH_INSECURE = 101;
    BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mChatService = null;

}
