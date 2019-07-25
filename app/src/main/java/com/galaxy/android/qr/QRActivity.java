package com.galaxy.android.qr;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.common.componentes.activity.ImmersiveFragmentActivity;
import com.galaxy.asia.R;

import org.jetbrains.annotations.Nullable;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;

public class QRActivity extends ImmersiveFragmentActivity {

    QRCodeView qrCodeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);


        qrCodeView = (ZXingView) findViewById(R.id.zbarview);
        qrCodeView.setDelegate(new QRCodeView.Delegate() {
            @Override
            public void onScanQRCodeSuccess(String result) {
                Toast.makeText(QRActivity.this, result, Toast.LENGTH_SHORT).show();
                AlertDialog.Builder builder = new AlertDialog.Builder(QRActivity.this);
                builder.setTitle("Result")
                        .setMessage(result)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            qrCodeView.startSpot();
                        });
            }

            @Override
            public void onScanQRCodeOpenCameraError() {
                Toast.makeText(QRActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermission = true;
        for (int grant : grantResults) {
            if (grant == PackageManager.PERMISSION_DENIED) {
                hasPermission = false;
            }
        }

        if (hasPermission) {
            qrCodeView.startCamera();//打开相机
            qrCodeView.showScanRect();//显示扫描框
            qrCodeView.startSpot();//开始识别二维码
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            qrCodeView.startCamera();//打开相机
            qrCodeView.showScanRect();//显示扫描框
            qrCodeView.startSpot();//开始识别二维码
        }
        //qrCodeView.openFlashlight();//开灯
        //qrCodeView.closeFlashlight();//关灯
    }

    @Override
    protected void onStop() {
        qrCodeView.stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        qrCodeView.onDestroy();
        super.onDestroy();
    }

}
