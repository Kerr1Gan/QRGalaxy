package com.galaxy.android.qr;

import android.Manifest;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.common.componentes.activity.ImmersiveFragmentActivity;
import com.galaxy.asia.R;

import org.jetbrains.annotations.Nullable;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;

public class QRActivity extends ImmersiveFragmentActivity {

    QRCodeView mQRCodeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        mQRCodeView = (ZXingView) findViewById(R.id.zbarview);
        mQRCodeView.setDelegate(new QRCodeView.Delegate() {
            @Override
            public void onScanQRCodeSuccess(String result) {
                Toast.makeText(QRActivity.this, result, Toast.LENGTH_SHORT).show();
                mQRCodeView.startSpot();
            }

            @Override
            public void onScanQRCodeOpenCameraError() {
                Toast.makeText(QRActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mQRCodeView.startCamera();//打开相机
        mQRCodeView.showScanRect();//显示扫描框
        mQRCodeView.startSpot();//开始识别二维码
        //mQRCodeView.openFlashlight();//开灯
        //mQRCodeView.closeFlashlight();//关灯
    }

    @Override
    protected void onStop() {
        mQRCodeView.stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mQRCodeView.onDestroy();
        super.onDestroy();
    }

}
