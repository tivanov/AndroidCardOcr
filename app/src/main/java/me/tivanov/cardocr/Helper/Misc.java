package me.tivanov.cardocr.Helper;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.opencv.core.Core.FILLED;
import static org.opencv.core.Core.countNonZero;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.adaptiveThreshold;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.morphologyEx;
import static org.opencv.imgproc.Imgproc.threshold;

public class Misc {

    public static int THRESH_TYPE_NORMAL    = 1;
    public static int THRESH_TYPE_ADAPTIVE  = 2;

    public static String saveToInternalStorage(String fileName, Bitmap bitmapImage, Context ctx){
        ContextWrapper cw = new ContextWrapper(ctx.getApplicationContext());
        // path to /data/data/cardocr/app_data/imageDir
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

    public static Mat preProcessImage (int thresh, int threshType, Mat mRgba, Session session) {
        Mat image = new Mat();
        Mat mGray = new Mat();
        //convert to grayscale
        cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
        //Apply thresholding
        if (thresh == Misc.THRESH_TYPE_ADAPTIVE)  //ADAPTIVE
            adaptiveThreshold(mGray, image, 255, threshType, Imgproc.THRESH_BINARY, session.getBlockSize(), session.getConst());
        else if (thresh == Misc.THRESH_TYPE_NORMAL)  //NORMAL
            Imgproc.threshold(mGray, image,session.getThreshold(), session.getMaxVal(), threshType + Imgproc.THRESH_OTSU);

        return image;
    }

    public static Mat preProcessImage (int thresh, int threshType, Mat mRgba, Map<String, String> params) {
        Mat image = new Mat();
        Mat mGray = new Mat();
        //convert to grayscale
        cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);
        //Apply thresholding
        if (thresh == Misc.THRESH_TYPE_ADAPTIVE)  //ADAPTIVE
            adaptiveThreshold(mGray, image, 255, threshType, Imgproc.THRESH_BINARY, Integer.parseInt(params.get("blockSize")), Double.parseDouble(params.get("const")));
        else if (thresh == Misc.THRESH_TYPE_NORMAL)  //NORMAL
            Imgproc.threshold(mGray, image,Double.parseDouble(params.get("threshold")), Double.parseDouble(params.get("maxVal")), threshType + Imgproc.THRESH_OTSU);

        return image;
    }

    public static ArrayList<Rect> filteredRects(ArrayList<Rect> source) {
        ArrayList<Rect> dest = new ArrayList<>();
        boolean flag;
        for (Rect r1 : source) {
            flag = true;
            for (Rect r2 : source)
                if (r1 != r2 && r2.contains(r1.tl()) && r2.contains(r1.br()))
                    flag = false;
            if (flag) dest.add(r1);
        }
        return dest;
    }

    public static ArrayList<Rect> findText(Mat original) {
        ArrayList<Rect> ret = new ArrayList<>();
        Mat small = new Mat();
        cvtColor(original, small, Imgproc.COLOR_BGR2GRAY);
        // morphological gradient
        Mat grad = new Mat();
        Mat morphKernel = getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        morphologyEx(small, grad, Imgproc.MORPH_GRADIENT, morphKernel);
        // binarize
        Mat bw = new Mat();
        threshold(grad, bw, 0.0, 255.0, Imgproc.THRESH_OTSU);
        // connect horizontally oriented regions
        Mat connected = new Mat();
        morphKernel = getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 1));
        morphologyEx(bw, connected, Imgproc.MORPH_CLOSE, morphKernel);
        // find contours
        Mat mask = Mat.zeros(bw.size(), CV_8UC1);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        findContours(connected, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        // filter contours
        Rect rect;
        for(int idx = 0; idx < contours.size(); idx++)
        {
            rect = boundingRect(contours.get(idx));
            Mat maskROI = new Mat(mask, rect);
            // fill the contour
            drawContours(mask, contours, idx, new Scalar(255, 255, 255), FILLED);
            // ratio of nllon-zero pixels in the filled region
            double r = (double)countNonZero(maskROI)/(rect.width*rect.height);
            if (r > .45  && (rect.height > 8 && rect.width > 8))
                ret.add(rect);
            //    rectangle(original, rect.tl(), rect.br(), new Scalar(0, 255, 0), 2);
        }
        return filteredRects(ret);
    }
}
