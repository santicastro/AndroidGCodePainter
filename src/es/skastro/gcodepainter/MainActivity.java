package es.skastro.gcodepainter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
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
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;
import es.skastro.android.util.alert.SimpleListAdapter;
import es.skastro.android.util.alert.SimpleOkAlertDialog;
import es.skastro.android.util.alert.StringPrompt;
import es.skastro.android.util.bluetooth.BluetoothService;
import es.skastro.android.util.bluetooth.DeviceListActivity;
import es.skastro.gcodepainter.draw.CoordinateConversor;
import es.skastro.gcodepainter.draw.DrawFile;
import es.skastro.gcodepainter.draw.Point;
import es.skastro.gcodepainter.draw.inkpad.Inkpad;
import es.skastro.gcodepainter.util.BitmapUtils;
import es.skastro.gcodepainter.view.DrawView;
import es.skastro.gcodepainter.view.DrawView.DrawMode;

/***
 * Real etch-a-sketch drawable dimmensions: 15.5cm x 10.5cm (aspect ratio: 1,4762)
 * 
 * @author Santi
 * 
 */

public class MainActivity extends Activity implements Observer {

    final static int GET_IMAGE_FROM_GALLERY_RESPONSE = 99;
    final static int GET_IMAGE_FROM_CAMERA_RESPONSE = 98;
    View view;
    DrawView drawView;
    DrawFile drawFile;
    Inkpad inkpad;
    String currentDrawFilename = null;
    Button btnUndo, btnBackground, btnConnect, btnSend, btnDrawLine, btnDrawInkpad;
    ImageView drawBackground;
    CheckBox chkAutomaticSend;

    Point bottomLeft = new Point(0.0, 625.0);
    Point topRight = new Point(922.0, 0.0);

    Point sketch_bottomLeft = new Point(0.0, 0.0);
    Point sketch_topRight = new Point(125.0, 85.6);

    final static int CONNECT_BLUETOOTH_SECURE = 100;
    final static int CONNECT_BLUETOOTH_INSECURE = 101;
    BluetoothAdapter mBluetoothAdapter;
    // Member object for the chat services
    private BluetoothService mChatService = null;
    CoordinateConversor conv = new CoordinateConversor(bottomLeft, topRight, sketch_bottomLeft, sketch_topRight);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set full screen view
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        chkAutomaticSend = (CheckBox) findViewById(R.id.chkAutomaticSend);
        chkAutomaticSend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sendCommitedPoints();
                }
            }
        });

        btnDrawLine = (Button) findViewById(R.id.buttonDrawLine);
        btnDrawLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectToolLine();
            }
        });

        btnDrawInkpad = (Button) findViewById(R.id.buttonDrawInkpad);
        btnDrawInkpad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectToolInkpad();
            }
        });

        btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawFile.commitUndoPoints();
                sendCommitedPoints();
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
                if (drawFile != null)
                    drawFile.undoLastAdd();
            }
        });

        drawBackground = (ImageView) findViewById(R.id.drawBackground);

        drawView = (DrawView) findViewById(R.id.drawView);

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
    }

    private void changeDraw(DrawFile drawFile) {
        if (drawFile == null) {
            SimpleOkAlertDialog.show(MainActivity.this, "Erro",
                    "Houbo un problema cambiando a imaxe. Volva a intentalo.");
            finish();
        } else {
            if (this.drawFile != null)
                this.drawFile.deleteObservers();
            lastSentIndex = -1;
            this.drawFile = drawFile;
            drawView.setDrawFile(drawFile);
            drawFile.addObserver(this);
            update(drawFile, null);
        }
    }

    private void selectToolLine() {
        drawView.setDrawMode(DrawMode.LINE);
        btnDrawLine.setBackgroundColor(getResources().getColor(R.color.light_blue));
        btnDrawInkpad.setBackgroundColor(Color.WHITE);
    }

    private void selectToolInkpad() {
        openInkpad();
    }

    private void useInkpad(Inkpad inkpad, String name) {
        this.inkpad = inkpad;
        drawView.setInkpad(inkpad);
        drawView.setDrawMode(DrawMode.INKPAD);
        btnDrawInkpad.setBackgroundColor(getResources().getColor(R.color.light_blue));
        btnDrawLine.setBackgroundColor(Color.WHITE);
        btnDrawInkpad.setText(name);
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
            final int MAX_SIZE = 1024;
            File file = new File(filename);
            Bitmap imageBitmap;
            try {
                imageBitmap = BitmapUtils.decodeSampledBitmapFromFile(file, MAX_SIZE, MAX_SIZE);
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
        drawFile = new DrawFile();
        drawFile.addPoint(bottomLeft, DrawFile.LAST_POSITION);
        drawFile.clearUndoInfo();
        changeDraw(drawFile);
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
                    DrawFile opened = DrawFile.fromFile(files[which]);
                    if (opened == null) {
                        SimpleOkAlertDialog
                                .show(MainActivity.this, "Non se puido abrir o arquivo",
                                        "Houbo un problema tentando cargar a imaxe. O arquivo pode estar danado ou non se recoñece o seu formato");
                        newDraw();
                    } else {
                        setCurrentDrawFilename(files[which].getName().replace(".ske", ""));
                        changeDraw(opened);
                    }
                }
            });
            builder.create().show();
            setCurrentDrawFilename(null);
        }
    }

    private void saveDraw() {
        try {
            if (drawFile != null) {
                final File dir = getDrawsDirectory();
                if (currentDrawFilename == null) {
                    StringPrompt sp = new StringPrompt(this, "Nome do arquivo",
                            "Escriba o nome co que quere gardar o debuxo", "") {
                        @Override
                        public boolean onOkClicked(String value) {
                            try {
                                Pattern filenamePattern = Pattern.compile("^[a-z0-9]+$");
                                if (filenamePattern.matcher(value).matches()) {
                                    setCurrentDrawFilename(value);
                                    File target = new File(dir.getAbsoluteFile() + File.separator + currentDrawFilename
                                            + ".ske");
                                    if (target.exists()) {
                                        SimpleOkAlertDialog.show(MainActivity.this, "Arquivo existente",
                                                "Xa existe un debuxo con ese nome, non se vai gardar.");
                                    } else {
                                        drawFile.saveToDisk(target);
                                        Toast.makeText(MainActivity.this, "Debuxo gardado", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    SimpleOkAlertDialog
                                            .show(MainActivity.this, "Nome inválido",
                                                    "O nome seleccionado non é válido, só se poden utilizar letras e números. Volva a intentalo.");
                                }
                            } catch (Exception e) {
                                SimpleOkAlertDialog
                                        .show(MainActivity.this, "Error",
                                                "Houbo un erro mentres se gardaba o arquivo e a operación non se puido completar");
                                Log.e("MainActivity", e.getMessage());
                            }
                            return true;
                        }
                    };
                    sp.show();
                } else {
                    File target = new File(dir.getAbsoluteFile() + File.separator + currentDrawFilename + ".ske");
                    drawFile.saveToDisk(target);
                    Toast.makeText(MainActivity.this, "Debuxo gardado", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            SimpleOkAlertDialog.show(MainActivity.this, "Error",
                    "Houbo un erro mentres se gardaba o arquivo e a operación non se puido completar");
            Log.e("MainActivity", e.getMessage());
        }
    }

    private void openInkpad() {
        File dir = getInkpadsDirectory();
        final File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".ipa") && !pathname.getName().startsWith(".");
            }
        });
        if (files.length == 0) {
            SimpleOkAlertDialog.show(this, "Non hai tampóns",
                    "Non se atoparon tampóns na tarxeta de memoria:\n " + dir.getAbsolutePath());
        } else {
            SimpleListAdapter<File> fileAdapter = new SimpleListAdapter<File>(MainActivity.this, Arrays.asList(files),
                    new SimpleListAdapter.StringGenerator<File>() {
                        @Override
                        public String getString(File addr) {
                            return addr.getName();
                        }
                    });
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Abrir tampón...").setAdapter(fileAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Inkpad opened = Inkpad.fromFile(files[which]);
                    if (opened == null) {
                        SimpleOkAlertDialog
                                .show(MainActivity.this, "Non se puido abrir o arquivo",
                                        "Houbo un problema tentando cargar o tampón. O arquivo pode estar danado ou non se recoñece o seu formato");
                    } else {
                        useInkpad(Inkpad.fromFile(files[which]), files[which].getName().replace(".ipa", ""));
                    }
                }
            });
            builder.create().show();
            setCurrentDrawFilename(null);
        }
    }

    private void saveInkpad() {
        try {
            if (drawFile != null) {
                final File dir = getInkpadsDirectory();
                StringPrompt sp = new StringPrompt(this, "Nome do tampón",
                        "Escriba o nome co que quere gardar o tampón.", "") {
                    @Override
                    public boolean onOkClicked(String value) {
                        try {
                            Pattern filenamePattern = Pattern.compile("^[a-z0-9]+$");
                            if (filenamePattern.matcher(value).matches()) {
                                String inkpadName = value;
                                File target = new File(dir.getAbsoluteFile() + File.separator + inkpadName + ".ipa");
                                if (target.exists()) {
                                    SimpleOkAlertDialog.show(MainActivity.this, "Arquivo existente",
                                            "Xa existe un debuxo con ese nome, non se vai gardar");
                                } else {
                                    Inkpad.fromDrawFile(drawFile).saveToDisk(target);
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
        // photoPickerIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/Pictures/image.jpg"));
        startActivityForResult(photoPickerIntent, GET_IMAGE_FROM_GALLERY_RESPONSE);
    }

    public void selectBackgroundFromCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, GET_IMAGE_FROM_CAMERA_RESPONSE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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
        case R.id.mnuBackgroundFile:
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
        btnUndo.setEnabled(((DrawFile) observable).canUndo());
        if (chkAutomaticSend.isChecked()) {
            sendCommitedPoints();
        }
    }

    int lastSentIndex = -1;

    private void sendCommitedPoints() {
        for (int i = lastSentIndex + 1; i < drawFile.getPointCount(); i++) {
            if (!drawFile.isCommited(i))
                return;
            sendMessage("G1 " + conv.calculate(drawFile.getPoint(i)).toString());
            lastSentIndex = i;
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

    private synchronized void sendMessage(String message) {
        if (messageQueue == null) {
            messageQueue = new MessageQueue(messagesToSend);
            messageQueue.start();
        }
        if (message.length() > 0) {
            messagesToSend.add(message + "\n");
        }
    }

    List<String> messagesToSend = Collections.synchronizedList(new ArrayList<String>());
    MessageQueue messageQueue;

    private class MessageQueue extends Thread {
        List<String> messagesToSend;

        public MessageQueue(List<String> messagesToSend) {
            this.messagesToSend = messagesToSend;
        }

        boolean WAIT_MODE = true;

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
                            sleep(1000);
                        } else {
                            Log.w("MessageQueue", "MessageQueue: sending " + messagesToSend.get(0));
                            if (WAIT_MODE) {
                                while (!mChatService.writeAndWait(messagesToSend.get(0).getBytes())) {
                                    Thread.sleep(500);
                                    Log.w("MessageQueue", "MessageQueue: retrying " + messagesToSend.get(0));
                                }

                            } else {
                                mChatService.write(messagesToSend.get(0).getBytes());
                                sleep(20);
                            }
                            Log.w("MessageQueue", "MessageQueue: removing " + messagesToSend.get(0));
                            messagesToSend.remove(0);
                        }
                    } else {
                        // Log.w("MessageQueue", "MessageQueue: No messages ");
                        sleep(200);
                    }
                }
            } catch (InterruptedException e) {
            }
            super.run();
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

}
