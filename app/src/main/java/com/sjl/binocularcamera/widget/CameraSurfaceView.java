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
    public static final int CAMERA_FACING_BACK = 0;

    public static final int CAMERA_FACING_FRONT = 1;

    public static final int CAMERA_USB = 2;

    // 相机
    public Camera mCamera;
    protected Camera.Parameters mCameraParam;
    protected int mCameraId;

    protected SurfaceHolder mSurfaceHolder;
    protected int mPreviewDegree;
    /**
     * 预览宽
     */
    public int mPreviewWidth;
    /**
     * 预览高
     */
    public int mPreviewHeight;
    protected boolean mIsCompletion = false;
    protected DisplayMetrics dm;
    /**
     * true Ir,falseRgb
     */
    private boolean ir;
    /**
     * 当前相机的ID。
     */
    private int cameraFacing = CAMERA_FACING_FRONT;

    private FaceView faceView;
    private boolean faceDetect;
    private int screenHeight, screenWidth;
    private Context mContext;

    public CameraSurfaceView(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * 创建预览view
     */
    public void createPreview() {
        SurfaceView surfaceView = new SurfaceView(mContext);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        dm = new DisplayMetrics();
        if (mContext instanceof Activity) {
            Activity activity = (Activity) mContext;
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getMetrics(dm);
            screenHeight = display.getHeight();
            screenWidth = display.getWidth();
        }
        addView(surfaceView);
        faceView = new FaceView(mContext);
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
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder,
                               int format,
                               int width,
                               int height) {
//        if (holder.getSurface() == null) {
//            return;
//        }
//        startPreview();
        Log.i(TAG, "mCameraId:" + mCameraId + ",surfaceChanged,width:" + width + ",height:" + height);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "mCameraId:" + mCameraId + ",surfaceDestroyed");

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

    /***
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

    private synchronized Camera open() {
        Camera camera = null;
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras == 0) {
            return null;
        }
        Log.i(TAG, "cameraFacing:" + cameraFacing);

        int countFront = 0, countBack = 0;
        for (int i = 0; i < numCameras; i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            Log.i(TAG, "摄像头facing:" + cameraInfo.facing);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                countFront++;
            } else {
                countBack++;
            }
        }
        if (countFront == 1 && countBack == 1){
            camera = Camera.open(1);
            mCameraId = 1;
            return camera;
        }
        switch (cameraFacing) {

            case CAMERA_FACING_BACK: {
                camera = Camera.open(0);
                mCameraId = 0;
                break;
            }
            //双目
            case CAMERA_FACING_FRONT: {
                camera = Camera.open(0);
                mCameraId = 0;
                break;
            }
            case CAMERA_USB: {
                camera = Camera.open(1);
                mCameraId = 1;
                break;
            }

            default:
                break;

        }
        return camera;
    }


    public synchronized void startPreview() {
        if (mCamera == null) {
            return;
        }
        mCameraParam = mCamera.getParameters();
        mPreviewDegree = getRotateAngle();
        mCamera.setDisplayOrientation(mPreviewDegree);
        mCameraParam.setPictureFormat(ImageFormat.JPEG);

//        print();
        // 图片越大，性能消耗越大，也可以选择640*480， 1280*720
//        int maxWithOrHeight = 1280;16/9
        int maxWithOrHeight = 640;
        float rate= 4/3f;
        List<Camera.Size> supportedPictureSizes = mCameraParam.getSupportedPictureSizes();
        Camera.Size  pictureSize = CameraHelper.getInstance().getSizeByRate(supportedPictureSizes,rate, maxWithOrHeight);


        mCameraParam.setPictureSize(pictureSize.width, pictureSize.height);

        List<Camera.Size> supportedPreviewSizes = mCameraParam.getSupportedPreviewSizes();
        Camera.Size previewSize =CameraHelper.getInstance().getSizeByRate(supportedPreviewSizes,rate, maxWithOrHeight);
        mPreviewWidth = previewSize.width;
        mPreviewHeight = previewSize.height;

     /*   Point point = CameraHelper.getBestPreview(mCameraParam,
                new Point(screenWidth, mPreviewHeight));
        mPreviewWidth = point.x;
        mPreviewHeight = point.y;*/

        mCameraParam.setPreviewSize(mPreviewWidth, mPreviewHeight);
        Log.e(TAG, "pictureSize,width = " + pictureSize.width + ", height = " + pictureSize.height);
        Log.e(TAG, "previewSize,width=" + mPreviewWidth + ", height = " + mPreviewHeight + ",previewDegree=" + mPreviewDegree);
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
            mIsCompletion = false;
        } catch (Exception e) {
            e.printStackTrace();
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }

        }
    }


    public void startFaceDetect() {
        if (mCamera == null) {
            return;
        }
        Log.i(TAG, "mCameraId:" + mCameraId + ",开始人脸检测");

        mCamera.startFaceDetection();   //开始人脸检测
        mCamera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
            @Override
            public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                if (faceView != null) {
//                    Log.i(TAG, "mCameraId:" + mCameraId + ",检测到人脸数:" + faces.length);
                    List<RectF> rectFS = transForm(faces);
                    faceView.setFaces(rectFS);
                }
            }
        });
    }

    public void stopFaceDetect() {
        if (mCamera == null) {
            return;
        }
        mCamera.stopFaceDetection();
        mCamera.setFaceDetectionListener(null);
    }

    public synchronized void stopPreview() {
        mIsCompletion = false;
        if (mCamera != null) {
            try {
                stopFaceDetect();
                mCamera.setErrorCallback(null);
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mIsCompletion = true;
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


    public void print() {
//        setScaleX(-1);
        if (mCameraParam == null){
            return;
        }
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

    public CameraSurfaceView setFaceDetect(boolean faceDetect) {
        this.faceDetect = faceDetect;
        return this;
    }
    public CameraSurfaceView setCameraFacing(int cameraFacing) {
        this.cameraFacing = cameraFacing;
        return this;
    }

    public int getCameraFacing() {
        return cameraFacing;
    }

    public boolean isIr() {
        return ir;
    }

    public void setIr(boolean ir) {
        this.ir = ir;
    }


}
