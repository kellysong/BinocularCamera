package com.sjl.binocularcamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.sjl.binocularcamera.util.BitmapUtils;
import com.sjl.binocularcamera.util.CameraHelper;
import com.sjl.binocularcamera.widget.CameraSurfaceView;

import java.io.BufferedOutputStream;
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

    private CameraSurfaceView mCameraSurfaceView1, mCameraSurfaceView2;
    private ImageView iv_rgb, iv_ir;
    private LinearLayout ll_surface_layout;
    private int camera1DataMean, camera2DataMean;
    private volatile boolean rgbOrIrConfirm, camera1IsRgb;
    private Button btn_rgb,btn_ir;

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
        btn_rgb = findViewById(R.id.btn_rgb);
        btn_ir = findViewById(R.id.btn_ir);
    }


    @Override
    protected void initData() {
        int numberOfCameras = CameraHelper.getNumberOfCameras();
        if (numberOfCameras < 2) {
            Toast.makeText(this, "未检测到2个摄像头,请检查设备是否正常", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
//
//        String deviceModel = Build.MODEL;
//        if (TextUtils.isEmpty(deviceModel) || !(deviceModel.contains("3399") || deviceModel.contains("3288"))) {
//            Toast.makeText(this, "该系统不支持双目摄像头", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
        mCameraSurfaceView1 = new CameraSurfaceView(this);
        ll_surface_layout.addView(mCameraSurfaceView1);
        mCameraSurfaceView1.setFaceDetect(true)
                .setPreviewDegree(Constant.previewDegree)
                .setCameraFacing(CameraSurfaceView.CAMERA_FACING_FRONT)
                .createPreview();

        mCameraSurfaceView2 = new CameraSurfaceView(this);
        ll_surface_layout.addView(mCameraSurfaceView2);
//        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mCameraSurfaceView2.getLayoutParams();
//        layoutParams.setMargins(0, 20,0, 0);
        mCameraSurfaceView2.setFaceDetect(true)
                .setPreviewDegree(Constant.previewDegree)
                .setCameraFacing(CameraSurfaceView.CAMERA_USB)
              .createPreview();

        mCameraSurfaceView1.setOnPreviewListener(new CameraSurfaceView.OnPreviewListener() {
            @Override
            public void onPreviewFrame(byte[] data) {
//             convertBitmap(data,previewSize.width,previewSize.height,"rgbPreview.jpg");
                if (!rgbOrIrConfirm) {
                    rgbOrIr(0,mCameraSurfaceView1.mPreviewWidth, mCameraSurfaceView1.mPreviewHeight, data);
                    if (rgbOrIrConfirm) {
                        //要等两个摄像头都返回一帧数据，rgbOrIrConfirm才会被赋值，此时才能判断到底哪个是RGB摄像头
                        choiceRgbOrIrType(0, data);
                        if (camera1IsRgb) {
                            mCameraSurfaceView1.setIr(false);
                            btn_rgb.setText(R.string.rgb_capture);
                            btn_ir.setText(R.string.ir_capture);
                            Log.i(TAG, "mCameraSurfaceView1 is Rgb.mCameraSurfaceView2 is Ir.");
                        } else {
                            mCameraSurfaceView1.setIr(true);
                            btn_rgb.setText(R.string.ir_capture);
                            btn_ir.setText(R.string.rgb_capture);
                            Log.i(TAG, "mCameraSurfaceView1 is Ir.mCameraSurfaceView2 is Rgb.");
                        }
                    }
                }

            }


        });

        mCameraSurfaceView2.setOnPreviewListener(new CameraSurfaceView.OnPreviewListener() {
            @Override
            public void onPreviewFrame(byte[] data) {
//              convertBitmap(data,previewSize.width,previewSize.height,"irPreview.jpg");
                if (!rgbOrIrConfirm) {
                    rgbOrIr(1, mCameraSurfaceView2.mPreviewWidth, mCameraSurfaceView2.mPreviewHeight, data);
                    if (rgbOrIrConfirm) {
                        //要等两个摄像头都返回一帧数据，rgbOrIrConfirm才会被赋值，此时才能判断到底哪个是RGB摄像头
                        choiceRgbOrIrType(1, data);
                        if (camera1IsRgb) {
                            mCameraSurfaceView2.setIr(true);
                            btn_rgb.setText(R.string.ir_capture);
                            btn_ir.setText(R.string.rgb_capture);
                            Log.i(TAG, "mCameraSurfaceView1 is Ir.mCameraSurfaceView2 is Rgb.");
                        } else {
                            mCameraSurfaceView2.setIr(false);
                            btn_rgb.setText(R.string.rgb_capture);
                            btn_ir.setText(R.string.ir_capture);
                            Log.i(TAG, "mCameraSurfaceView1 is Rgb.mCameraSurfaceView2 is Ir.");

                        }
                    }
                }

            }
        });
    }


    /**
     * 参考：https://blog.csdn.net/yzzzza/article/details/107670521
     *
     * @param index
     * @param data
     */
    private synchronized void rgbOrIr(int index, int PREFER_WIDTH, int PREFER_HEIGHT, byte[] data) {
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
                camera1IsRgb = true; //把两个摄像头一帧数据的所有byte值加起来比大小
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
        mCameraSurfaceView1
                .setPreviewDegree(Constant.previewDegree)
                .startPreview();
        mCameraSurfaceView2
                .setPreviewDegree(Constant.previewDegree)
                .startPreview();
    }


    @Override
    public void onPause() {
        super.onPause();
        mCameraSurfaceView1.stopPreview();
        mCameraSurfaceView2.stopPreview();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ll_surface_layout != null) {
            ll_surface_layout.removeAllViews();
        }
        mCameraSurfaceView1.release();
        mCameraSurfaceView2.release();
    }

    public void btnRgbTakePhoto(View view) {
        if (mCameraSurfaceView1 == null ||  mCameraSurfaceView1.mCamera == null) {
            return;
        }
        mCameraSurfaceView1.mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                bitmap = convertBmp(bitmap);
                Matrix matrix = new Matrix();
                matrix.setRotate(mCameraSurfaceView1.getPreviewDegree());
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                bitmap = BitmapUtils.scaleBitmap(bitmap, 0.5f);

                iv_rgb.setImageBitmap(bitmap);

                mCameraSurfaceView1.startPreview();

                saveImg(bitmap, "rgb.jpg");


            }
        });
    }


    public void btnIrTakePhoto(View view) {
        if (mCameraSurfaceView2 == null ||  mCameraSurfaceView2.mCamera == null) {
            Toast.makeText(this,"未找到摄像头",Toast.LENGTH_LONG).show();
            return;
        }
        mCameraSurfaceView2.mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap bitmap = convertBmp(bm);
                Matrix matrix = new Matrix();
                matrix.setRotate(mCameraSurfaceView2.getPreviewDegree());
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                bitmap = BitmapUtils.scaleBitmap(bitmap, 0.5f);
                iv_ir.setImageBitmap(bitmap);
                mCameraSurfaceView2.startPreview();
                saveImg(bm, "ir.jpg");

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
     * 把预览帧转为图片并保存
     *
     * @param data
     * @param width
     * @param height
     * @param fileName
     */
    public void savePreviewFrameToBitmap(byte[] data, int width, int height, String fileName) {
        Bitmap bitmap = BitmapUtils.convertPreviewFrameToBitmap(data, width, height);
        saveImg(bitmap, fileName);
    }

    public void btnFaceDetect(View view) {
        if (mCameraSurfaceView1 != null) {
            mCameraSurfaceView1.setFaceDetect(true);
            mCameraSurfaceView1.stopFaceDetect();
            mCameraSurfaceView1.startPreview();
        }
        if (mCameraSurfaceView2 != null) {
            mCameraSurfaceView2.setFaceDetect(true);
            mCameraSurfaceView2.stopFaceDetect();
            mCameraSurfaceView2.startPreview();
        }
    }

    public void btnSetting(View view) {
        startActivity(new Intent(this,CameraSetting.class));
    }
}
