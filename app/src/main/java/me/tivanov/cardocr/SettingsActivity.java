package me.tivanov.cardocr;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import me.tivanov.cardocr.Helper.Misc;
import me.tivanov.cardocr.Helper.Session;

import static org.opencv.imgproc.Imgproc.rectangle;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private class SpinnerMap {
        String name;
        int value;

        SpinnerMap(String name, int value) {
            this.name = name;
            this.value = value;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    private ArrayAdapter<SpinnerMap> threshKindsA;
    private ArrayAdapter<SpinnerMap> threshTypesA;
    private ArrayAdapter<SpinnerMap> adaptiveThreshTypesA;

    private ImageView imageTaken;
    private Button btnApply;
    private Button btnSave;
    private EditText etThresh;
    private EditText etMaxVal;
    private EditText etBlockSize;
    private EditText etConst;
    private Spinner spThresh;
    private Spinner spThreshType;

    private Session session;

    private Mat mRgba;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        session = new Session(getApplicationContext());

        imageTaken = (ImageView) findViewById(R.id.imageTaken2);
        btnApply = (Button) findViewById((R.id.btnApply));
        btnSave = (Button) findViewById((R.id.btnSave));
        etThresh = (EditText) findViewById((R.id.etThresh));
        etMaxVal = (EditText) findViewById((R.id.etMaxVal));
        etBlockSize = (EditText) findViewById((R.id.etBlockSize));
        etConst = (EditText) findViewById((R.id.etConst));
        spThresh = (Spinner) findViewById(R.id.spAdaptive) ;
        spThreshType = (Spinner) findViewById(R.id.spThreshType) ;

        etThresh.setText(String.valueOf(session.getThreshold()));
        etMaxVal.setText(String.valueOf(session.getMaxVal()));
        etBlockSize.setText(String.valueOf(session.getBlockSize()));
        etConst.setText(String.valueOf(session.getConst()));


        btnApply.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        fillLists();

        spThresh.setAdapter(threshKindsA);
        spThresh.setOnItemSelectedListener(this);
        spThresh.setSelection(0);
        setState(Misc.THRESH_TYPE_ADAPTIVE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String filename = getIntent().getStringExtra("image");
            String filePath = getIntent().getStringExtra("path");
            Bitmap originalImage = Misc.loadImageFromStorage(filename, filePath);
            if (originalImage == null)  finish();
            mRgba = new Mat();
            Utils.bitmapToMat(originalImage, mRgba);
            imageTaken.setImageBitmap(originalImage);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == spThresh) {
            SpinnerMap item = (SpinnerMap)spThresh.getSelectedItem();
            setState(item.value);
        }
    }

    private void setState(int threshType) {

        etThresh.setEnabled(false);
        etMaxVal.setEnabled(false);
        etBlockSize.setEnabled(false);
        etConst.setEnabled(false);

        if (threshType == Misc.THRESH_TYPE_ADAPTIVE) {
            spThreshType.setAdapter(adaptiveThreshTypesA);
            etBlockSize.setEnabled(true);
            etConst.setEnabled(true);

        } else if (threshType == Misc.THRESH_TYPE_NORMAL) {
            spThreshType.setAdapter(threshTypesA);
            etThresh.setEnabled(true);
            etMaxVal.setEnabled(true);
        }
        spThreshType.setSelection(0);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void fillLists () {
        ArrayList<SpinnerMap> threshKinds = new ArrayList<>();
        ArrayList<SpinnerMap> threshTypes = new ArrayList<>();
        ArrayList<SpinnerMap> adaptiveThreshTypes = new ArrayList<>();

        threshKinds.add(new SpinnerMap("ADAPTIVE", Misc.THRESH_TYPE_ADAPTIVE));
        threshKinds.add(new SpinnerMap("NORMAL", Misc.THRESH_TYPE_NORMAL));

        threshTypes.add(new SpinnerMap("BINARY",Imgproc.THRESH_BINARY));
        threshTypes.add(new SpinnerMap("BINARY INV",Imgproc.THRESH_BINARY_INV));
        threshTypes.add(new SpinnerMap("TRUNCATE",Imgproc.THRESH_TRUNC));
        threshTypes.add(new SpinnerMap("TO ZERO",Imgproc.THRESH_TOZERO));
        threshTypes.add(new SpinnerMap("TO ZERO INV",Imgproc.THRESH_TOZERO_INV));

        adaptiveThreshTypes.add(new SpinnerMap("MEAN",Imgproc.ADAPTIVE_THRESH_MEAN_C));
        adaptiveThreshTypes.add(new SpinnerMap("GAUSSIAN",Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C));

        threshKindsA = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, threshKinds);
        threshTypesA = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, threshTypes);
        adaptiveThreshTypesA = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, adaptiveThreshTypes);
    }

    @Override
    public void onClick(View v) {
        float threshold, maxVal, C;
        int blockSize, threshMethod, threshType;
        Mat image;

        if ((spThresh.getSelectedItem() == null) || (spThreshType.getSelectedItem() == null))return;

        maxVal          = Float.parseFloat(etMaxVal.getText().toString());
        threshold       = Float.parseFloat(etThresh.getText().toString());
        blockSize       = (int)Float.parseFloat(etBlockSize.getText().toString());
        C               = Float.parseFloat(etConst.getText().toString());
        threshMethod    = ((SpinnerMap) spThresh.getSelectedItem()).value;
        threshType      = ((SpinnerMap) spThreshType.getSelectedItem()).value;

        if (v == btnSave) {
            session.setThreshold(threshold);
            session.setMaxVal(maxVal);
            session.setBlockSize(blockSize);
            session.setConst(C);
            session.setThreshMethod(threshMethod);
            session.setThreshType(threshType);
            this.finish();
        } else if (v == btnApply) {
            //List<Rect> arreasOfInterest = Misc.findText(mRgba);
            ArrayList<Rect> arreasOfInterest = Misc.findText(mRgba);
            image = Misc.preProcessImage(threshMethod, threshType, mRgba, session);

            for (Rect r : arreasOfInterest) {
                Imgproc.rectangle(image, r.tl(), r.br(), new Scalar(0, 255, 0), 2);
            }

            Bitmap bmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image, bmp);
            imageTaken.setImageBitmap(bmp);

//            threshold = Float.parseFloat(etThresh.getText().toString());
//            maxVal = Float.parseFloat(etMaxVal.getText().toString());
//            Mat image = new Mat();
//            Mat mGray = new Mat();
//            cvtColor(mRgba, mGray, COLOR_RGBA2GRAY);
//            Imgproc.threshold(mGray, image,threshold, maxVal, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
//            Bitmap bmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(image, bmp);
//            imageTaken.setImageBitmap(bmp);

            //vaka dava ok rezultat so adaptive threshold
//            threshold = Float.parseFloat(etThresh.getText().toString());
//            maxVal = Float.parseFloat(etMaxVal.getText().toString());
//            Mat image = new Mat();
//            Mat mGray = new Mat();
//            cvtColor(mRgba, mGray, COLOR_RGBA2GRAY);
                                        //src, destt, maxValue, adaptiveMethod,             thresholdType,          blockSize,          C
//                                                                                                                //kolku pogolemo C, tolku pomalku detali
//            Imgproc.adaptiveThreshold(mGray, image, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, (int)threshold, (int)maxVal); //dobro e so (11, 12)
//            //Imgproc.threshold(mGray, image,threshold, maxVal, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);   //blockSize (paren),  C
//            Bitmap bmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(image, bmp);
//            imageTaken.setImageBitmap(bmp);
        }
    }
}
