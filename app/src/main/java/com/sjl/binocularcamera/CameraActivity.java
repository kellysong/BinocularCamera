package com.sjl.binocularcamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.sjl.binocularcamera.util.BitmapUtils;
import com.sjl.binocularcamera.util.CameraHelper;
import com.sjl.binocularcamera.widget.CameraSurfaceView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename CameraActivity
 * @time 2020/11/21 15:32
 * @copyright(C) 2020 song
 */
public class CameraActivity extends BaseActivity {
    public static final String TAG = CameraActivity.class.getSimpleName();

    private CameraSurfaceView mRgbSurfaceView, mIrSurfaceView;
    private ImageView iv_rgb, iv_ir;
    private LinearLayout ll_surface_layout;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_camera;
    }
    @Override
    protected void initView() {
        ll_surface_layout = findViewById(R.id.ll_surface_layout);
        ll_surface_layout.removeAllViews();
        iv_rgb = findViewById(R.id.iv_rgb);
        iv_ir = findViewById(R.id.iv_ir);
        mRgbSurfaceView = new CameraSurfaceView(this);
        mRgbSurfaceView.setFaceDetect(true);
        ll_surface_layout.addView(mRgbSurfaceView);
        if (CameraHelper.checkBinocularCamera()){
            mIrSurfaceView = new CameraSurfaceView(this);
            ll_surface_layout.addView(mIrSurfaceView);
            mIrSurfaceView.setFaceDetect(true);
        }

    }



    @Override
    protected void initData() {
        mRgbSurfaceView.setOnPreviewListener(new CameraSurfaceView.OnPreviewListener() {
            @Override
            public void onPreviewFrame(byte[] data) {
//                Camera.Size previewSize = mRgbSurfaceView.getPreviewSize();
//             convertBitmap(data,previewSize.width,previewSize.height,"rgbPreview.jpg");
              /*  rgbOrIr(0,data);
                if (rgbOrIrConfirm){
                    //要等两个摄像头都返回一帧数据，rgbOrIrConfirm才会被赋值，此时才能判断到底哪个是RGB摄像头
                    choiceRgbOrIrType(0,data);
                }*/
            }




        });

        if (mIrSurfaceView != null){
            mIrSurfaceView.setOnPreviewListener(new CameraSurfaceView.OnPreviewListener() {
                @Override
                public void onPreviewFrame(byte[] data) {
 //               Camera.Size previewSize = mIrSurfaceView.getPreviewSize();
//              convertBitmap(data,previewSize.width,previewSize.height,"irPreview.jpg");
                    rgbOrIr(1,data);
                    if (rgbOrIrConfirm){
                        //要等两个摄像头都返回一帧数据，rgbOrIrConfirm才会被赋值，此时才能判断到底哪个是RGB摄像头
                        choiceRgbOrIrType(1,data);
                    }
                }




            });
        }

    }
    int camera1DataMean,camera2DataMean;
    int PREFER_WIDTH,PREFER_HEIGHT;
    boolean rgbOrIrConfirm,camera1IsRgb;

    /**
     * 参考：https://blog.csdn.net/yzzzza/article/details/107670521
     * @param index
     * @param data
     */
    private synchronized void rgbOrIr(int index, byte[] data) {
        byte[] tmp = new byte[PREFER_WIDTH * PREFER_HEIGHT];
        try {
            System.arraycopy(data, 0, tmp, 0, PREFER_WIDTH * PREFER_HEIGHT);
        } catch (NullPointerException e) {
            Log.e(TAG, String.valueOf(e.getStackTrace()));
        }
        int count = 0;
        int total = 0;
        for (int i = 0; i < PREFER_WIDTH * PREFER_HEIGHT; i = i + 10) {
            total += tmp[i];
            count++;
        }
        if (index == 0) {
            camera1DataMean = total / count;
        } else {
            camera2DataMean = total / count;
        }
        if (camera1DataMean != 0 && camera2DataMean != 0) {
            if (camera1DataMean > camera2DataMean) {
                camera1IsRgb = true; //惊了，居然是把两个摄像头一帧数据的所有byte值加起来比大小
            } else {
                camera1IsRgb = false;
            }
            rgbOrIrConfirm = true;
        }
    }
    private void choiceRgbOrIrType(int index, byte[] data) {
        // camera1如果为rgb数据，调用dealRgb，否则为Ir数据，调用Ir
        if (index == 0) {
            if (camera1IsRgb) {
                dealRgb(data);
            } else {
                dealIr(data);
            }
        } else {
            if (camera1IsRgb) {
                dealIr(data);
            } else {
                dealRgb(data);
            }
        }
    }

    private void dealIr(byte[] data) {
    }

    private void dealRgb(byte[] data) {
    }


    @Override
    public void onResume() {
        super.onResume();
        mRgbSurfaceView.startPreview();
        Camera.Size previewSize = mRgbSurfaceView.getPreviewSize();
        PREFER_WIDTH = previewSize.width;
        PREFER_HEIGHT = previewSize.height;
        if (mIrSurfaceView != null){
            mIrSurfaceView.startPreview();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mRgbSurfaceView.stopPreview();
        if (mIrSurfaceView != null){
            mIrSurfaceView.stopPreview();
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ll_surface_layout != null) {
            ll_surface_layout.removeAllViews();
        }

    }
    public void btnRgbTakePhoto(View view) {
        if (mRgbSurfaceView == null) {
            return;
        }
        mRgbSurfaceView.mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                bitmap = convertBmp(bitmap);
                Matrix matrix = new Matrix();
                matrix.setRotate(mRgbSurfaceView.getPreviewDegree());
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                bitmap =BitmapUtils.scaleBitmap(bitmap,0.5f);

                iv_rgb.setImageBitmap(bitmap);

                mRgbSurfaceView.startPreview();

                saveImg(bitmap,"rgb.jpg");


            }
        });
    }







    public void btnIrTakePhoto(View view) {
        if (mIrSurfaceView == null) {
            return;
        }
        mIrSurfaceView.mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap bitmap = convertBmp(bm);
                Matrix matrix = new Matrix();
                matrix.setRotate(mIrSurfaceView.getPreviewDegree());
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                bitmap = BitmapUtils.scaleBitmap(bitmap,0.5f);
                iv_ir.setImageBitmap(bitmap);
                mIrSurfaceView.startPreview();
                saveImg(bm,"ir.jpg");

            }
        });
    }

    public Bitmap convertBmp(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();

        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1); // 镜像水平翻转
        Bitmap convertBmp = Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true);

        return convertBmp;
    }
    private void saveImg(Bitmap bm, String fileName) {
        File dir = new File(Environment.getExternalStorageDirectory(), "temp");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, fileName);
        BufferedOutputStream bos;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 把预览帧转为图片
     * @param data
     * @param width
     * @param height
     * @param fileName
     */
    public void convertPreviewFrameToBitmap(byte[] data, int width, int height,String fileName) {
        YuvImage image = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
        Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
        saveImg(bitmap,fileName);
    }

}
