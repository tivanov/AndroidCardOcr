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
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import me.tivanov.cardocr.Helper.Misc;
import me.tivanov.cardocr.Helper.Session;

import static org.opencv.imgproc.Imgproc.COLOR_RGBA2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;

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

    private static int THRESH_TYPE_NORMAL = 1;
    private static int THRESH_TYPE_ADAPTIVE = 2;


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
        setState(THRESH_TYPE_ADAPTIVE);

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

        if (threshType == THRESH_TYPE_ADAPTIVE) {
            spThreshType.setAdapter(adaptiveThreshTypesA);
            etBlockSize.setEnabled(true);
            etConst.setEnabled(true);

        } else if (threshType == THRESH_TYPE_NORMAL) {
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

        threshKinds.add(new SpinnerMap("ADAPTIVE", THRESH_TYPE_ADAPTIVE));
        threshKinds.add(new SpinnerMap("NORMAL", THRESH_TYPE_NORMAL));

        threshTypes.add(new SpinnerMap("BINARY",Imgproc.THRESH_BINARY));
        threshTypes.add(new SpinnerMap("BINARY_INV",Imgproc.THRESH_BINARY_INV));
        threshTypes.add(new SpinnerMap("TRUNC",Imgproc.THRESH_TRUNC));
        threshTypes.add(new SpinnerMap("TOZERO",Imgproc.THRESH_TOZERO));
        threshTypes.add(new SpinnerMap("TOZERO_INV",Imgproc.THRESH_TOZERO_INV));

        adaptiveThreshTypes.add(new SpinnerMap("MEAN_C",Imgproc.ADAPTIVE_THRESH_MEAN_C));
        adaptiveThreshTypes.add(new SpinnerMap("GAUSSIAN_C",Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C));


        threshKindsA = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, threshKinds);
        threshTypesA = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, threshTypes);
        adaptiveThreshTypesA = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, adaptiveThreshTypes);
    }

    @Override
    public void onClick(View v) {
        float threshold, maxVal, C;
        int blockSize;
        Mat image, mGray;


        maxVal = Float.parseFloat(etMaxVal.getText().toString());
        threshold = Float.parseFloat(etThresh.getText().toString());
        blockSize = (int)Float.parseFloat(etBlockSize.getText().toString());
        C = Float.parseFloat(etConst.getText().toString());

        if (v == btnSave) {
            session.setThreshold(threshold);
            session.setMaxVal(maxVal);
            session.setBlockSize(blockSize);
            session.setConst(C);
            this.finish();
        } else if (v == btnApply) {
            SpinnerMap selectedThresh = (SpinnerMap) spThresh.getSelectedItem();
            if (selectedThresh == null) return;

            int type = ((SpinnerMap)spThreshType.getSelectedItem()).value;
            image = new Mat();
            mGray = new Mat();
            cvtColor(mRgba, mGray, COLOR_RGBA2GRAY);

            if (selectedThresh.value == THRESH_TYPE_ADAPTIVE) { //ADAPTIVE
                Imgproc.adaptiveThreshold(mGray, image, 255, type, Imgproc.THRESH_BINARY, blockSize, C);
            } else if (selectedThresh.value == THRESH_TYPE_NORMAL) { //NORMAL
                Imgproc.threshold(mGray, image,threshold, maxVal, type + Imgproc.THRESH_OTSU);
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
