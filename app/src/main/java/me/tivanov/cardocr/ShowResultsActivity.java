package me.tivanov.cardocr;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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
    private TextView        tvOcrText;
    private Mat             mRgba;
    private Session         session;
    private String          datapath;
    private TessBaseAPI     mTess;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_results);
        imageTaken = (ImageView) findViewById(R.id.imageTaken);
        tvOcrText = (TextView) findViewById(R.id.tvOcrText);
        (findViewById(R.id.btnDoOcr)).setOnClickListener(this);

        session = new Session(getApplicationContext());

        if (getIntent().getExtras() == null) finishCancel();

        String filename = getIntent().getStringExtra("image");
        String filePath = getIntent().getStringExtra("path");
        Bitmap originalImage = Misc.loadImageFromStorage(filename, filePath);
        if (originalImage == null) finishCancel();

        imageTaken.setImageBitmap(originalImage);
        mRgba = new Mat();
        Utils.bitmapToMat(originalImage, mRgba);
        tessInit();

        Misc.deleteImageFromStorage(filename, filePath);
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

        Mat preprocessed = Misc.preProcessImage(session.getThreshMethod(), session.getThreshType(), mRgba, session);
        Bitmap bmp = Bitmap.createBitmap(preprocessed.cols(), preprocessed.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(preprocessed, bmp);

        String OCRresult;
        mTess.setImage(bmp);
        OCRresult = mTess.getUTF8Text();
        Log.d(TAG, OCRresult);
        imageTaken.setImageBitmap(bmp);
        tvOcrText.setText(OCRresult);
        dismissProgressDialog();
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
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
