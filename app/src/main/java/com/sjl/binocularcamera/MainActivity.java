package com.sjl.binocularcamera;

import android.content.Intent;
import android.view.View;

/**
 * @author song
 */
public class MainActivity extends BaseActivity {


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initData() {
        requestPermissions(100);
    }

    public void btnOpenCamera(View view) {
        startActivity(new Intent(this,CameraActivity.class));
    }


    public void btnImgDetect(View view) {
        startActivity(new Intent(this,ImgDetectActivity.class));
    }
}
