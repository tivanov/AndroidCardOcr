package me.tivanov.cardocr;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.tivanov.cardocr.Helper.Misc;
import me.tivanov.cardocr.Helper.Session;

public class ShowResultsActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ShowResultsActivity";

    private ImageView       imageTaken;
    private EditText        tvOcrText;
    private Mat             mRgba;
    private Session         session;
    private String          datapath;
    private TessBaseAPI     mTess;
    private ProgressDialog  progressDialog;
    private Bitmap          originalImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_results);

        imageTaken = (ImageView) findViewById(R.id.imageTaken);

        if (savedInstanceState != null) {
            originalImage = savedInstanceState.getParcelable("bitmap");
        }
        else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                String filename = extras.getString("image");
                String filePath = extras.getString("path");
                originalImage = Misc.loadImageFromStorage(filename, filePath);
                Misc.deleteImageFromStorage(filename, filePath);
            }
        }

        if (originalImage == null)
            finish();
        mRgba = new Mat();
        Utils.bitmapToMat(originalImage, mRgba);
        imageTaken.setImageBitmap(originalImage);

        tvOcrText = (EditText) findViewById(R.id.tvOcrText);
        (findViewById(R.id.btnDoOcr)).setOnClickListener(this);
        session = new Session(getApplicationContext());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("bitmap", originalImage);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void tessInit() {
        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));
        mTess.init(datapath, language);
    }

    private void doOcr() {
        startProgressDialog();
        String OCRresult;

        try {
            tessInit();
            Mat preprocessed = Misc.preProcessImage(session.getThreshMethod(), session.getThreshType(), mRgba, session);
            Bitmap bmp = Bitmap.createBitmap(preprocessed.cols(), preprocessed.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(preprocessed, bmp);
            mTess.setImage(bmp);
            OCRresult = mTess.getUTF8Text();
            Log.d(TAG, OCRresult);
            imageTaken.setImageBitmap(bmp);
            tvOcrText.setText(OCRresult);
            imageTaken.setImageBitmap(bmp);
        } catch (Exception e) {
            Toast.makeText(this, "Error performing OCR!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception while performing OCR. Message:" + e.getMessage());
        } finally {
            dismissProgressDialog();
        }
    }

    private void startProgressDialog()
    {
        progressDialog = new ProgressDialog(ShowResultsActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle("Running OCR...");
        progressDialog.show();
    }

    private void dismissProgressDialog()
    {
        if (progressDialog != null)
            progressDialog.dismiss();
    }

    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }

            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException while loading Tesseract. Message:" + e.getMessage());
            //e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "IOException while loading Tesseract. Message:" + e.getMessage());
            //e.printStackTrace();
        }
    }

    private void finishCancel() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    public void onClick(View v) {
        doOcr();
    }
}
