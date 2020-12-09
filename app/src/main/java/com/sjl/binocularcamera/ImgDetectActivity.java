package com.sjl.binocularcamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ImgDetectActivity
 * @time 2020/12/9 18:14
 * @copyright(C) 2020 song
 */
public class ImgDetectActivity extends BaseActivity {
    ImageView iv_img;
    TextView tv_msg;
    @Override
    protected int getLayoutId() {
        return R.layout.img_detect_activity;
    }

    @Override
    protected void initView() {
        iv_img = findViewById(R.id.iv_img);
        tv_msg = findViewById(R.id.tv_msg);
    }

    @Override
    protected void initData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initBitmap();
            }
        }).start();

    }

    private void initBitmap() {
        try {
            InputStream open = getAssets().open("test.jpg");
            Bitmap bitmap  = BitmapFactory.decodeStream(open);
            open.close();
            final Bitmap bitmap565 = bitmap.copy(Bitmap.Config.RGB_565, true);
            if (!bitmap.isRecycled())
                bitmap.recycle();
            // 识别图片
            detectFace(bitmap565);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    iv_img.setImageBitmap(bitmap565);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();

        }

    }


    private void detectFace(final Bitmap bitmap565) {
        int numberOfFace = 12;
        FaceDetector myFaceDetect;
        int imageWidth = bitmap565.getWidth();
        int imageHeight = bitmap565.getHeight();
        FaceDetector.Face[] faces = new FaceDetector.Face[numberOfFace];
        myFaceDetect = new FaceDetector(imageWidth, imageHeight, numberOfFace);
        final int numberOfFaceDetected = myFaceDetect.findFaces(bitmap565, faces);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_msg.setText(String.format("检测到%1$d个人脸", numberOfFaceDetected));
            }
        });
        // 画框
        drawFace(bitmap565,numberOfFaceDetected,faces);


    }

    private void drawFace(Bitmap bitmap565, int numberOfFaceDetected, FaceDetector.Face[] faces) {
        Canvas canvas = new Canvas(bitmap565);
        Paint myPaint = new Paint();
        myPaint.setColor(Color.GREEN);
        myPaint.setStyle(Paint.Style.STROKE);
        myPaint.setStrokeWidth(3);
        for (int i = 0; i < numberOfFaceDetected; i++) {
            FaceDetector.Face face = faces[i];
            PointF myMidPoint = new PointF();
            face.getMidPoint(myMidPoint);
            float myEyesDistance = face.eyesDistance();
            canvas.drawRect((int) (myMidPoint.x - myEyesDistance * 1.5),
                    (int) (myMidPoint.y - myEyesDistance * 1.5),
                    (int) (myMidPoint.x + myEyesDistance * 1.5),
                    (int) (myMidPoint.y + myEyesDistance * 1.8), myPaint);
        }

    }

}
