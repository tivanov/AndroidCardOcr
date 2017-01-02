package me.tivanov.cardocr.Helper;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.opencv.imgproc.Imgproc.COLOR_RGBA2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class Misc {
    public static String saveToInternalStorage(String fileName, Bitmap bitmapImage, Context ctx){
        ContextWrapper cw = new ContextWrapper(ctx.getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    public static Bitmap loadImageFromStorage(String fileName, String path)
    {

        try {
            File f=new File(path, fileName);
            return BitmapFactory.decodeStream(new FileInputStream(f));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean deleteImageFromStorage(String fileName, String path)
    {

        try {
            File f=new File(path, fileName);
            return f.delete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static Mat preProcessImage (Mat mRgba, int blockSize, double Const) {
        Mat image = new Mat();
        Mat mGray = new Mat();
        cvtColor(mRgba, mGray, COLOR_RGBA2GRAY);
        Imgproc.adaptiveThreshold(mGray, image, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, Const);
        return image;
    }
}
