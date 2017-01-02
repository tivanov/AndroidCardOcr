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

    private CameraBridgeViewBase mOpenCvCameraView;

    private FloatingActionButton fabRunOcr;
    private FloatingActionButton fabThreshSetup;

    private Mat mRgba;
    private Mat mGray;

    private Point p1;
    private Point p2;
    private Point p3;
    private Point p4;
    private Scalar lineColor;
    private int lineThickness;

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

        lineColor = new Scalar(237, 238, 239);
        lineThickness = 30;
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
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        //overlay lines setup
        //for line on top
        p1 = new Point(60, 95);
        p2 = new Point(mRgba.cols() - 60, 95);
        //for line on bottom
        p3 = new Point(60, mRgba.rows() - 95);
        p4 = new Point(mRgba.cols() - 60, mRgba.rows() - 95);

        if (goToSettings || shouldTakePicture) {
            Mat mRgbaSub =  mRgba.submat(new Rect(p1,p4));
            Bitmap bmp = Bitmap.createBitmap(mRgbaSub.cols(), mRgbaSub.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mRgbaSub, bmp);

            if (goToSettings){
                goToSettings = false;
                startNextActivity(bmp, SettingsActivity.class);
            } else if (shouldTakePicture) {
                shouldTakePicture = false;
                startNextActivity(bmp, ShowResultsActivity.class);
            }
        }

        //line on top
        Imgproc.line(mRgba,p1, p2, lineColor, lineThickness);
        //line on bottom
        Imgproc.line(mRgba,p3, p4, lineColor, lineThickness);
        //left line
        Imgproc.line(mRgba,p1, p3, lineColor, lineThickness);
        //right line
        Imgproc.line(mRgba,p2, p4, lineColor, lineThickness);
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