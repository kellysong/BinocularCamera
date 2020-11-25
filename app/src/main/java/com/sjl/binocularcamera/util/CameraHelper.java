package com.sjl.binocularcamera.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


/**
 * 相机辅助类
 *
 * @author Kelly
 * @version 1.0.0
 * @filename CameraHelper.java
 * @time 2017年11月21日 下午5:43:45
 * @copyright(C) 2017 song
 */
public class CameraHelper {
    private CameraSizeComparator sizeComparator = new CameraSizeComparator();
    private static CameraHelper myCamPara = null;
    public static final String TAG = CameraHelper.class.getSimpleName();


    private CameraHelper() {

    }


    public static CameraHelper getInstance() {
        if (myCamPara == null) {
            myCamPara = new CameraHelper();
        }
        return myCamPara;
    }


    /**
     * 获取最接近的size
     *
     * @param context         上下文
     * @param list            支持的预览尺寸集合或支持的照片尺寸集合
     * @param maxWithOrHeight 最大允许的照片尺寸的长度 ,宽或者高
     * @return
     */
    public Size getNearestSize(Context context, List<Size> list, int maxWithOrHeight) {
        Activity activity = (Activity) context;
        WindowManager windowManager = activity.getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        //获取屏幕的宽和高
        display.getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int tempW, tempH;
        if (screenWidth > screenHeight) {//横屏
            tempW = screenWidth;
            tempH = screenHeight;
        } else {//竖屏,调整宽>高
            tempW = screenHeight;
            tempH = screenWidth;
        }
        float rate;
        if (equalRate(tempW, tempH, 1.33f)) {
            rate = (float) 4 / 3;
        } else if (equalRate(tempW, tempH, 1.77f)) {
            rate = (float) 16 / 9;
        } else {
            rate = (float) 4 / 3;
        }
        Collections.sort(list, sizeComparator);
        Camera.Size resultSize = null;
        for (Camera.Size size : list) {
            boolean b = equalRate(size, rate);

            if (b && size.width <= maxWithOrHeight && size.height <= maxWithOrHeight) {
                if (resultSize == null) {
                    resultSize = size;
                } else if (size.width > resultSize.width) {
                    resultSize = size;
                }
            }
        }
        if (resultSize == null) {
            return list.get(list.size() - 1);
        }
        return resultSize;
    }

    /**
     * 保证Size的长宽比率,一般而言这个比率为1.333/1.7777即通常说的4:3和16:9(竖屏，高/宽，横屏，宽/高)比率
     *
     * @param s
     * @param rate
     * @return
     */
    public boolean equalRate(Size s, float rate) {
        float r = (float) (s.width) / (float) (s.height);
        if (Math.abs(r - rate) <= 0.2) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 根据指定宽高获取比例
     *
     * @param width
     * @param height
     * @param rate
     * @return
     */
    public boolean equalRate(int width, int height, float rate) {
        float r = (float) width / (float) height;
        if (Math.abs(r - rate) <= 0.2) {
            return true;
        } else {
            return false;
        }
    }

    public Point findBestPointByRate(List<Size> rawSupportedSizes, float rate, int previewSize) {
        Size nearestSize = getSizeByRate(rawSupportedSizes, rate, previewSize);
        Point point = new Point(nearestSize.width, nearestSize.height);
        return point;
    }


    public Size getSizeByRate(List<Size> list, float rate, int maxWithOrHeight) {
        Collections.sort(list, sizeComparator);
        Camera.Size resultSize = null;
        for (Camera.Size size : list) {

            if (equalRate(size,rate) && size.width <= maxWithOrHeight && size.height <= maxWithOrHeight) {
                if (resultSize == null) {
                    resultSize = size;
                } else if (size.width > resultSize.width) {
                    resultSize = size;
                }
            }
        }
        if (resultSize == null) {
            return list.get(list.size() - 1);
        }
        return resultSize;
    }

    /**
     * 获取最佳预览帧范围
     *
     * @param ranges
     * @param startFps
     * @param endFps
     * @return
     */
    public int[] getNearestFpsRange(List<int[]> ranges, int startFps, int endFps) {
        int[] currentRange = new int[2];
        List<Integer> tempRanges = new ArrayList<>();
        for (int[] range : ranges) {
            for (int r : range) {
                tempRanges.add(r);
            }
        }
        int minvalue = Collections.min(tempRanges);

        int maxvalue = Collections.max(tempRanges);
        if (startFps < minvalue) {
            startFps = maxvalue;
        }
        if (endFps > maxvalue) {
            endFps = maxvalue;
        }
        currentRange[0] = startFps;
        currentRange[1] = endFps;
        return currentRange;
    }

    /**
     * 预览尺寸的list进行升序排序
     */
    private class CameraSizeComparator implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            if (lhs.width == rhs.width) {// 按升序排列
                return 0;
            } else if (lhs.width > rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }

    }


    /**
     * 解决相机预览方向的问题
     *
     * @param context      上下文
     * @param cameraFacing 相机前后标志
     * @return
     */
    public int getPreviewDegree(Context context, int cameraFacing) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraFacing, info);
        int rotation = 0;
        if (context instanceof Activity) {
            rotation = ((Activity) context).getWindowManager().getDefaultDisplay().getRotation();
        }
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result = 0;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {//前置
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else { // 后置
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 盘点是否是竖屏
     *
     * @param context
     * @return
     */
    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT;
    }


    private static boolean checkCameraFacing(final int facing) {

        if (getSdkVersion() < Build.VERSION_CODES.GINGERBREAD) {

            return false;

        }

        final int cameraCount = Camera.getNumberOfCameras();

        CameraInfo info = new CameraInfo();

        for (int i = 0; i < cameraCount; i++) {

            Camera.getCameraInfo(i, info);

            if (facing == info.facing) {

                return true;

            }

        }

        return false;
    }

    public static boolean hasBackFacingCamera() {

        final int CAMERA_FACING_BACK = 0;

        return checkCameraFacing(CAMERA_FACING_BACK);
    }

    public static boolean hasFrontFacingCamera() {

        final int CAMERA_FACING_BACK = 1;

        return checkCameraFacing(CAMERA_FACING_BACK);
    }

    public static int getSdkVersion() {

        return android.os.Build.VERSION.SDK_INT;

    }

    public static int getNumberOfCameras() {
        int numCameras = Camera.getNumberOfCameras();
        return numCameras;
    }

    private static final int MIN_PREVIEW_PIXELS = 307200;
    private static final int MAX_PREVIEW_PIXELS = 921600;


    public static Point getBestPreview(Camera.Parameters parameters, Point screenResolution) {
        List<Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            return new Point(640, 480);
        } else {
            List<Size> supportedPictureSizes = new ArrayList(rawSupportedSizes);
            Collections.sort(supportedPictureSizes, new Comparator<Size>() {
                public int compare(Size a, Size b) {
                    int aPixels = a.height * a.width;
                    int bPixels = b.height * b.width;
                    if (bPixels < aPixels) {
                        return -1;
                    } else {
                        return bPixels > aPixels ? 1 : 0;
                    }
                }
            });
            double screenAspectRatio = screenResolution.x > screenResolution.y ? (double)screenResolution.x / (double)screenResolution.y : (double)screenResolution.y / (double)screenResolution.x;
            Size selectedSize = null;
            double selectedMinus = -1.0D;
            Iterator it = supportedPictureSizes.iterator();

            while(true) {
                Size supportedPreviewSize;
                while(it.hasNext()) {
                    supportedPreviewSize = (Size)it.next();
                    int realWidth = supportedPreviewSize.width;
                    int realHeight = supportedPreviewSize.height;
                    if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
                        it.remove();
                    } else if (realWidth * realHeight > MAX_PREVIEW_PIXELS) {
                        it.remove();
                    } else if (realHeight % 16 == 0 && realWidth % 16 == 0) {
                        double aRatio = supportedPreviewSize.width > supportedPreviewSize.height ? (double)supportedPreviewSize.width / (double)supportedPreviewSize.height : (double)supportedPreviewSize.height / (double)supportedPreviewSize.width;
                        double minus = Math.abs(aRatio - screenAspectRatio);
                        boolean selectedFlag = false;
                        if (selectedMinus == -1.0D && minus <= 0.25D || selectedMinus >= minus && minus <= 0.25D) {
                            selectedFlag = true;
                        }

                        if (selectedFlag) {
                            selectedMinus = minus;
                            selectedSize = supportedPreviewSize;
                        }
                    } else {
                        it.remove();
                    }
                }

                if (selectedSize != null) {
                    return new Point(selectedSize.width, selectedSize.height);
                }
                return new Point(640, 480);
            }
        }
    }

}
