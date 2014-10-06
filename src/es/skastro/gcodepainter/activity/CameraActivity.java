package es.skastro.gcodepainter.activity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.SeekBar;
import es.skastro.gcodepainter.R;

public class CameraActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    public static final int VIEW_MODE_RGBA = 0;
    public static final int VIEW_MODE_CANNY = 2;

    private MenuItem mItemPreviewRGBA;
    private MenuItem mItemPreviewCanny;
    private CameraBridgeViewBase mOpenCvCameraView;

    private SeekBar seekThreshold1;
    private SeekBar seekThreshold2;

    private Mat mIntermediateMat;

    public static int viewMode = VIEW_MODE_CANNY;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
            case LoaderCallbackInterface.SUCCESS: {
                Log.i(TAG, "OpenCV loaded successfully");
                mOpenCvCameraView.enableView();
            }
                break;
            default: {
                super.onManagerConnected(status);
            }
                break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_manipulations_surface_view);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        seekThreshold1 = (SeekBar) findViewById(R.id.seekBar1);
        seekThreshold1.setProgress(80);

        seekThreshold2 = (SeekBar) findViewById(R.id.seekBar2);
        seekThreshold2.setProgress(90);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewCanny = menu.add("Canny");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemPreviewRGBA)
            viewMode = VIEW_MODE_RGBA;
        else if (item == mItemPreviewCanny)
            viewMode = VIEW_MODE_CANNY;
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();
        mIntermediateMat = null;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Size sizeRgba = rgba.size();

        Mat rgbaInnerWindow;

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = cols / 10;
        int top = rows / 10;

        int width = cols * 3 / 4;
        int height = rows * 3 / 4;

        switch (CameraActivity.viewMode) {
        case CameraActivity.VIEW_MODE_RGBA:
            break;

        case CameraActivity.VIEW_MODE_CANNY:
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, seekThreshold1.getProgress(), seekThreshold2.getProgress());
            Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
            rgbaInnerWindow.release();
            break;
        }
        return rgba;
    }
}
