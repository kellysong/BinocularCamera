package com.sjl.binocularcamera.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename BitmapUtils
 * @time 2020/11/21 18:16
 * @copyright(C) 2020 song
 */
public class BitmapUtils {
    /**
     * 按比例缩放图片
     *
     * @param origin 原图
     * @param ratio  比例
     * @return 新的bitmap
     */
    public static Bitmap scaleBitmap(Bitmap origin, float ratio) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(ratio, ratio);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

}
