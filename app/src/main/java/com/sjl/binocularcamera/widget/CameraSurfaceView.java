package com.sjl.binocularcamera.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.sjl.binocularcamera.util.CameraHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename MYSurfaceView
 * @time 2020/11/14 16:06
 * @copyright(C) 2020 泰中科技
 */
public class CameraSurfaceView extends FrameLayout implements SurfaceHolder.Callback,
        Camera.PreviewCallback, Camera.ErrorCallback {
    public static final String TAG = CameraSurfaceView.class.getSimpleName();

    // 相机
    public Camera mCamera;
    protected Camera.Parameters mCameraParam;
    protected int mCameraId;

    protected SurfaceHolder mSurfaceHolder;
    protected int mPreviewDegree;
    protected int mPreviewWidth;
    protected int mPreviewHeight;
    protected boolean mIsCompletion = false;
    protected DisplayMetrics dm;
    private boolean isPIR;
    private FaceView faceView;
    private boolean faceDetect;
    private int screenHeight, screenWidth;


    public CameraSurfaceView(Context context) {
        super(context);
        SurfaceView surfaceView = new SurfaceView(context);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        dm = new DisplayMetrics();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getMetrics(dm);
            screenHeight = display.getHeight();
            screenWidth = display.getWidth();
        }
        addView(surfaceView);
        faceView = new FaceView(context);
        addView(faceView);

    }

    @Override
    public void onError(int error, Camera camera) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mIsCompletion) {
            return;
        }
        if (onPreviewListener != null) {
            onPreviewListener.onPreviewFrame(data);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder,
                               int format,
                               int width,
                               int height) {
        if (holder.getSurface() == null) {
            return;
        }
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }


    /**
     * 将相机中用于表示人脸矩形的坐标转换成UI页面的坐标
     *
     * @param faces
     * @return
     */
    public List<RectF> transForm(Camera.Face[] faces) {
        Matrix matrix = new Matrix();
        // Need mirror for front camera.
        boolean mirror = (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT);
        prepareMatrix(matrix, mirror, mPreviewDegree, mPreviewWidth, mPreviewHeight);
        matrix.postRotate(0); //Matrix.postRotate默认是顺时针
        List<RectF> rectList = new ArrayList<RectF>();
        for (Camera.Face face : faces) {
            RectF srcRect = new RectF(face.rect);
            RectF dstRect = new RectF(0f, 0f, 0f, 0f);
            matrix.mapRect(dstRect, srcRect);
            rectList.add(dstRect);
        }
        return rectList;
    }

    /**
     * https://blog.csdn.net/yanzi1225627/article/details/38098729
     *
     * @param matrix
     * @param mirror
     * @param displayOrientation
     * @param viewWidth
     * @param viewHeight
     */
    private void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation,
                               int viewWidth, int viewHeight) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }

    private Camera open() {
        Camera camera;
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras == 0) {
            return null;
        }

        int index = 0;
        while (index < numCameras) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index, cameraInfo);
            Log.i(TAG, "摄像头facing:" + cameraInfo.facing);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                break;
            }
            index++;
        }
        if (index < numCameras) {
            camera = Camera.open(index);
            mCameraId = index;
        } else {
            camera = Camera.open(0);
            mCameraId = 0;
        }
        return camera;
    }

    public synchronized void startPreview() {
        if (mCamera == null) {
            try {
                mCamera = open();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mCamera == null) {
            Log.e(TAG, "相机未找到");
            return;
        }
        if (mCameraParam == null) {
            mCameraParam = mCamera.getParameters();
        }
        mCameraParam.setPictureFormat(ImageFormat.JPEG);
        mPreviewDegree = getRotateAngle();
        mCamera.setDisplayOrientation(mPreviewDegree);
        print();
        int maxWithOrHeight;
        if (screenWidth < screenHeight) {
            maxWithOrHeight = Math.min(screenWidth, screenHeight);
        } else {//横屏
            maxWithOrHeight = (int) (Math.max(screenWidth, screenHeight) * 0.8f);
        }
        List<Camera.Size> supportedPictureSizes = mCameraParam.getSupportedPictureSizes();
        Camera.Size pictureSize = CameraHelper.getInstance().getNearestSize(getContext(), supportedPictureSizes, maxWithOrHeight);
        mCameraParam.setPictureSize(pictureSize.width, pictureSize.height);

        List<Camera.Size> supportedPreviewSizes = mCameraParam.getSupportedPreviewSizes();
        Camera.Size previewSize = CameraHelper.getInstance().getNearestSize(getContext(), supportedPreviewSizes, maxWithOrHeight);
        mPreviewWidth = previewSize.width;
        mPreviewHeight = previewSize.height;

     /*   Point point = CameraHelper.getBestPreview(mCameraParam,
                new Point(screenWidth, mPreviewHeight));
        mPreviewWidth = point.x;
        mPreviewHeight = point.y;*/

        mCameraParam.setPreviewSize(mPreviewWidth, mPreviewHeight);
        Log.e(TAG, "pictureSize.width = " + pictureSize.width + ", pictureSize.height = " + pictureSize.height);

        Log.e(TAG, "mPreviewWidth = " + mPreviewWidth + ", mPreviewHeight = " + mPreviewHeight + ",mPreviewDegree=" + mPreviewDegree);
        // Preview 768,432


        // 计算缩放比例
        int width;
        int height;
        float sy;
        if (mPreviewWidth > screenWidth) {
            //等比缩放
            sy = (float) screenWidth / mPreviewWidth;
            width = screenWidth;
            height = (int) (mPreviewHeight * sy);
        } else {
            width = mPreviewWidth;
            height = mPreviewHeight;
        }


        LinearLayout.LayoutParams cameraFL = new LinearLayout.LayoutParams(width, height);
        setLayoutParams(cameraFL);
        mCamera.setParameters(mCameraParam);

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.stopPreview();
            mCamera.setErrorCallback(this);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
            startFaceDetect();
        } catch (Exception e) {
            e.printStackTrace();
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }

        }
    }


    private void startFaceDetect() {
        if (!faceDetect) {
            return;
        }
        if (mCamera == null) {
            startPreview();
            return;
        }
        mCamera.startFaceDetection();   //开始人脸检测
        mCamera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
            @Override
            public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                if (faceView != null) {
//                    Log.i(TAG, "检测到人脸数:" + faces.length);
                    List<RectF> rectFS = transForm(faces);
                    faceView.setFaces(rectFS);
                }
            }
        });

    }

    private void stopFaceDetect() {
        if (!faceDetect) {
            return;
        }
        mCamera.stopFaceDetection();
    }

    public synchronized void stopPreview() {
        mIsCompletion = false;
        if (mCamera != null) {
            try {
                stopFaceDetect();
                mCamera.setFaceDetectionListener(null);
                mCamera.setErrorCallback(null);
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            }
        }
        if (mSurfaceHolder != null) {
            mSurfaceHolder.removeCallback(this);
        }

    }

    private int getRotateAngle() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        // 获取当前手机的选装角度
        int rotation = ((WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay()
                .getRotation();
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

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }


    private void print() {
//        setScaleX(-1);
        List<Camera.Size> supportedPreviewSizes = sortSize(mCameraParam.getSupportedPreviewSizes());
        List<Camera.Size> supportedPictureSizes = sortSize(mCameraParam.getSupportedPictureSizes());
        StringBuilder sb = new StringBuilder("\n");
        for (Camera.Size size : supportedPreviewSizes) {
            sb.append(size.width + ":" + size.height + " ");
        }
        Log.i(TAG, "预览分辨率：" + sb.toString());

        StringBuilder sb2 = new StringBuilder("\n");
        for (Camera.Size size : supportedPictureSizes) {
            sb2.append(size.width + ":" + size.height + " ");
        }
        Log.i(TAG, "拍照分辨率：" + sb2.toString());

        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向
        if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {
            //横屏
            Log.e(TAG, "横屏");
        } else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
            //竖屏
            Log.e(TAG, "竖屏");
        }
    }

    private List<Camera.Size> sortSize(List<Camera.Size> sizeList) {
        List<Camera.Size> list = new ArrayList(sizeList);
        Collections.sort(list, new Comparator<Camera.Size>() {
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                } else {
                    return bPixels > aPixels ? 1 : 0;
                }
            }
        });
        return list;
    }

    private OnPreviewListener onPreviewListener;

    public void setOnPreviewListener(OnPreviewListener onPreviewListener) {
        this.onPreviewListener = onPreviewListener;
    }


    public interface OnPreviewListener {
        void onPreviewFrame(byte[] data);
    }

    public int getPreviewDegree() {
        return mPreviewDegree;
    }

    public boolean isCompletion() {
        return mIsCompletion;
    }

    public void setCompletion(boolean completion) {
        this.mIsCompletion = completion;
    }

    public Camera.Size getPreviewSize() {
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        return previewSize;
    }

    public void setFaceDetect(boolean faceDetect) {
        this.faceDetect = faceDetect;
        startFaceDetect();
    }
}
