package me.tivanov.cardocr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Date;

import me.tivanov.cardocr.Helper.Misc;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "MainActivity";

    private static final int    OVERLAY_PADDING_TOP     = 90;
    private static final int    OVERLAY_LINE_THICKNESS  = 30;
    private static final Scalar OVERLAY_LINE_COLOR      = new Scalar(237, 238, 239);
    private static final double CARD_SIDES_RATIO        = 0.628;

    private CameraBridgeViewBase mOpenCvCameraView;

    private FloatingActionButton fabRunOcr;
    private FloatingActionButton fabThreshSetup;

    private Mat mRgba;

    private boolean shouldTakePicture;
    private boolean goToSettings;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        fabRunOcr = (FloatingActionButton) findViewById(R.id.fabRunOcr);
        fabThreshSetup = (FloatingActionButton) findViewById(R.id.fabThreshSetup);
        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.jcvImage);
        mOpenCvCameraView.setCvCameraViewListener(this);

        fabRunOcr.setOnClickListener(this);
        fabThreshSetup.setOnClickListener(this);
    }

    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            if (mOpenCvCameraView != null)
                mOpenCvCameraView.enableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onClick(View v) {
        if (v == this.fabRunOcr) {
            shouldTakePicture = true;
        } else if (v == fabThreshSetup) {
            goToSettings = true;
        }

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        //overlay lines setup
        int height = mRgba.rows() - 2 * OVERLAY_PADDING_TOP;
        int width  = (int)(height/CARD_SIDES_RATIO);
        int distanceToEdge = (mRgba.cols() - width)/2;

        Point p1 = new Point(distanceToEdge, OVERLAY_PADDING_TOP);
        Point p4 = new Point(mRgba.cols() - distanceToEdge, mRgba.rows() - OVERLAY_PADDING_TOP);


        if (goToSettings){
            goToSettings = false;
            Mat mRgbaSub =  mRgba.submat(new Rect(p1, p4));
            Bitmap bmp = Bitmap.createBitmap(mRgbaSub.cols(), mRgbaSub.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgbaSub, bmp);
            startNextActivity(bmp, SettingsActivity.class);
        } else if (shouldTakePicture) {
            int unitSize = (int)((p4.y - p1.y)/4);

            p1.y = p1.y + unitSize*2;
            p4.y = p4.y - unitSize;

            Mat mRgbaSub =  mRgba.submat(new Rect(p1, p4));
            Bitmap bmp = Bitmap.createBitmap(mRgbaSub.cols(), mRgbaSub.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgbaSub, bmp);

            shouldTakePicture = false;
            startNextActivity(bmp, ShowResultsActivity.class);
        }
        Imgproc.rectangle(mRgba, p1, p4, OVERLAY_LINE_COLOR, OVERLAY_LINE_THICKNESS);
        return mRgba;
    }

    private void startNextActivity(Bitmap bmp, Class ctx) {
        String filename = "bitmap"+ new Date().getTime() + ".png";
        String path = Misc.saveToInternalStorage(filename, bmp, getApplicationContext());
        bmp.recycle();

        //Pop intent
        Intent intent = new Intent(this, ctx);
        intent.putExtra("image", filename);
        intent.putExtra("path", path);
        startActivity(intent);
    }
}