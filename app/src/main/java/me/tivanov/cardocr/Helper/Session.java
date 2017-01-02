package me.tivanov.cardocr.Helper;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class Session {
    private SharedPreferences prefs;
    private static final float  DEFAULT_THRESHOLD = 10;
    private static final int    DEFAULT_BLOCK_SIZE = 11;
    private static final float  DEFAULT_C = 12;
    private static final float  DEFAULT_MAX_VAL = 255;

    public Session(Context cntx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(cntx);
        if (this.getThreshold() == -1 ) this.setThreshold(DEFAULT_THRESHOLD);
        if (this.getBlockSize() == -1 ) this.setBlockSize(DEFAULT_BLOCK_SIZE);
        if (this.getConst() == -1 ) this.setConst(DEFAULT_C);
        if (this.getMaxVal() == -1 ) this.setMaxVal(DEFAULT_MAX_VAL);
    }

    public void setThreshold(float val) {
        prefs.edit().putFloat("threshold", val).apply();
    }

    public double getThreshold() {
        return prefs.getFloat("threshold", -1);
    }

    public void setBlockSize(int val) {
        prefs.edit().putInt("BlockSize", val).apply();
    }

    public int getBlockSize() {
        return prefs.getInt("BlockSize", -1);
    }


    public void setConst(float val) {
        prefs.edit().putFloat("Const", val).apply();
    }

    public double getConst() {
        return prefs.getFloat("Const", -1);
    }

    public void setMaxVal(float val) {
        prefs.edit().putFloat("MaxVal", val).apply();
    }

    public double getMaxVal() {
        return prefs.getFloat("MaxVal", -1);
    }
}