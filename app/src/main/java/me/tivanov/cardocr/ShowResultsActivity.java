package me.tivanov.cardocr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.widget.TextView;

public class ShowResultsActivity extends AppCompatActivity {
    private AppCompatImageView imageTaken;
    private TextView tvOcrText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_results);
        imageTaken = (AppCompatImageView) findViewById(R.id.imageTaken);
        tvOcrText = (TextView) findViewById(R.id.tvOcrText);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            byte[] byteArray = getIntent().getByteArrayExtra("imageTaken");
            //Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            //imageTaken.setImageBitmap(bmp);
            tvOcrText.setText(extras.getString("OcrText"));
        }
    }
}
