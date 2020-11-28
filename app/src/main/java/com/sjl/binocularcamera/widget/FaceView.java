package com.sjl.binocularcamera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.List;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename FaceView
 * @time 2020/11/14 17:21
 * @copyright(C) 2020 song
 */
public class FaceView extends View {
    private Paint mPaint;
    private String mColor = "#42ed45";
    private  List<RectF> mFaces = null; //人脸信息
    private FaceRect faceRect;

    public FaceView(Context context) {
        super(context);
        init();
    }

    private void init() {
        faceRect = FaceRect.RECT_SEGMENT;
        mPaint = new Paint();
        mPaint.setColor(Color.parseColor(mColor));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.5f, getContext().getResources().getDisplayMetrics()));
        mPaint.setAntiAlias(true);
    }

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.rotate(-0);   //Canvas.rotate()默认是逆时针
        if (mFaces == null || mFaces.size() == 0) {
            canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
        } else {
            for (RectF rect : mFaces) {
                drawRect(canvas, rect, faceRect);
            }
        }

    }

    private void drawRect(Canvas canvas, RectF rect, FaceRect faceRect) {
        if (FaceRect.RECT_SOLID == faceRect) {
            canvas.drawRect(rect, mPaint);   //绘制人脸所在位置的矩形
        } else {
            /**
             * 左上角的竖线
             */
            canvas.drawLine(rect.left, rect.top, rect.left, rect.top + 20, mPaint);
            /**
             * 左上角的横线
             */
            canvas.drawLine(rect.left, rect.top, rect.left + 20, rect.top, mPaint);

            /**
             * 右上角的竖线
             */
            canvas.drawLine(rect.right, rect.top, rect.right - 20, rect.top, mPaint);
            /**
             * 右上角的横线
             */
            canvas.drawLine(rect.right, rect.top, rect.right, rect.top + 20, mPaint);
            /**
             * 左下角的竖线
             */
            canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - 20, mPaint);
            /**
             * 左下角的横线
             */
            canvas.drawLine(rect.left, rect.bottom, rect.left + 20, rect.bottom, mPaint);

            /**
             * 右下角的竖线
             */
            canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - 20, mPaint);
            /**
             * 右下角的横线
             */
            canvas.drawLine(rect.right, rect.bottom, rect.right - 20, rect.bottom, mPaint);
        }

    }

    public void setFaces(List<RectF> mFaces) {
        this.mFaces = mFaces;
        invalidate();
    }

    public void setFaceRect(FaceRect faceRect) {
        this.faceRect = faceRect;
    }

    /**
     * 人脸框样式
     */
    public enum  FaceRect{
        RECT_SOLID,
        RECT_SEGMENT
    }
}
