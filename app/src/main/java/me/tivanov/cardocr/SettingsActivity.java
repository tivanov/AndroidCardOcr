package me.tivanov.cardocr;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import me.tivanov.cardocr.Helper.Misc;
import me.tivanov.cardocr.Helper.Session;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "SettingsActivity";

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

    private ImageView   imageTaken;
    private Button      btnApply;
    private Button      btnSave;
    private EditText    etThresh;
    private EditText    etMaxVal;
    private EditText    etBlockSize;
    private EditText    etConst;
    private Spinner     spThresh;
    private Spinner     spThreshType;
    private CheckBox    cbFindText;
    private CheckBox    cbSaveSteps;

    private Session session;

    private Mat mRgba;
    private Bitmap imageBmp;
    private Bitmap imageOnDisplayBmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        imageTaken = (ImageView) findViewById(R.id.imageTaken2);

        if (savedInstanceState != null) {
            imageBmp = savedInstanceState.getParcelable("bitmap");
        }
        else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                String filename = extras.getString("image");
                String filePath = extras.getString("path");
                imageBmp = Misc.loadImageFromStorage(filename, filePath);
                Misc.deleteImageFromStorage(filename, filePath);
            }
        }

        if (imageBmp == null)
            finish();

        mRgba = new Mat();
        Utils.bitmapToMat(imageBmp, mRgba);
        imageTaken.setImageBitmap(imageBmp);

        registerForContextMenu(imageTaken);

        session = new Session(getApplicationContext());

        btnApply = (Button) findViewById((R.id.btnApply));
        btnSave = (Button) findViewById((R.id.btnSave));
        etThresh = (EditText) findViewById((R.id.etThresh));
        etMaxVal = (EditText) findViewById((R.id.etMaxVal));
        etBlockSize = (EditText) findViewById((R.id.etBlockSize));
        etConst = (EditText) findViewById((R.id.etConst));
        spThresh = (Spinner) findViewById(R.id.spAdaptive) ;
        spThreshType = (Spinner) findViewById(R.id.spThreshType) ;
        cbFindText =(CheckBox) findViewById(R.id.cbFindText) ;
        cbSaveSteps =(CheckBox) findViewById(R.id.cbSaveSteps) ;

        etThresh.setText(String.valueOf(session.getThreshold()));
        etMaxVal.setText(String.valueOf(session.getMaxVal()));
        etBlockSize.setText(String.valueOf(session.getBlockSize()));
        etConst.setText(String.valueOf(session.getConst()));

        btnApply.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        cbFindText.setOnCheckedChangeListener(this);

        fillLists();

        spThresh.setAdapter(threshKindsA);
        spThresh.setOnItemSelectedListener(this);
        spThresh.setSelection(0);
        setState(Misc.THRESH_TYPE_ADAPTIVE);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("bitmap", imageBmp);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v == imageTaken) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.image_long_press, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.image_menu_item_save:
                if (imageOnDisplayBmp != null) {
                    MediaStore.Images.Media.insertImage(getContentResolver(), imageOnDisplayBmp,
                            "CardOCRImage_" + String.valueOf(new Date().getTime()),
                            "Image from the app Card OCR");
                    Toast.makeText(this, "Image successfully saved!", Toast.LENGTH_SHORT).show();
                    return true;
                }
            default:
                return super.onContextItemSelected(item);
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == cbFindText)
        {
            cbSaveSteps.setEnabled(cbFindText.isChecked());
        }

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

    private boolean validateInput() {
        int blockSize;

        if (((SpinnerMap) spThresh.getSelectedItem()).value == Misc.THRESH_TYPE_ADAPTIVE)  //ADAPTIVE
        {
            if (etBlockSize.getText().toString().equals("")) {
                Toast.makeText(this, "Please input Block Size!", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (etConst.getText().toString().equals("")) {
                Toast.makeText(this, "Please input Const!", Toast.LENGTH_SHORT).show();
                return false;
            }

            blockSize = (int)Float.parseFloat(etBlockSize.getText().toString());
            if (blockSize % 2 == 0) {
                Toast.makeText(this, "Block Size should be odd number!", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        else if (((SpinnerMap) spThresh.getSelectedItem()).value == Misc.THRESH_TYPE_NORMAL)  //NORMAL
        {
            if (etMaxVal.getText().toString().equals("")) {
                Toast.makeText(this, "Please input Max. Value!", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (etThresh.getText().toString().equals("")) {
                Toast.makeText(this, "Please input Threshold!", Toast.LENGTH_SHORT).show();
                return false;
            }

        }
        return true;
    }

    @Override
    public void onClick(View v) {
        float threshold, maxVal, C;
        int blockSize, threshMethod, threshType;
        Mat image;

        if ((spThresh.getSelectedItem() == null) || (spThreshType.getSelectedItem() == null))return;
        if (!validateInput()) return;

        try {
            maxVal = Float.parseFloat(etMaxVal.getText().toString());
            threshold = Float.parseFloat(etThresh.getText().toString());
            blockSize = (int) Float.parseFloat(etBlockSize.getText().toString());
            C = Float.parseFloat(etConst.getText().toString());
        } catch (Exception e) {
            Toast.makeText(this, "Error!. Please check input values!", Toast.LENGTH_SHORT).show();
            return;
        }

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

            //prepare parameters
            Map<String, String> params = new HashMap<>();
            params.put("threshold", String.valueOf(threshold));
            params.put("maxVal", String.valueOf(maxVal));
            params.put("blockSize", String.valueOf(blockSize));
            params.put("const", String.valueOf(C));

            try {

                //do the preprocessing
                image = Misc.preProcessImage(threshMethod, threshType, mRgba, params);
                if (cbFindText.isChecked()) {
                    //find text on image
                    ArrayList<Rect> arreasOfInterest = Misc.findText(mRgba, cbSaveSteps.isChecked());
                    //show where the text is
                    for (Rect r : arreasOfInterest)
                        Imgproc.rectangle(image, r.tl(), r.br(), new Scalar(0, 255, 0), 2);
                }

                imageOnDisplayBmp = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(image, imageOnDisplayBmp);
                imageTaken.setImageBitmap(imageOnDisplayBmp);
                imageTaken.invalidate();
            } catch (Exception e) {
                Toast.makeText(this, "Unexpected error!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Exception while pre-processing image. Message:" + e.getMessage());
            }
        }
    }
}
