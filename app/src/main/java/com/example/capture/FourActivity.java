package com.example.capture;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.hjimi.api.iminect.ImiDevice;
import com.hjimi.api.iminect.ImiDeviceState;
import com.hjimi.api.iminect.ImiNect;

import static com.hjimi.api.iminect.ImiUpgrade.UpgradeErrCode.IMI_UPGRADEERR_OK;

public class FourActivity extends AppCompatActivity {
    private static final String TAG = "FourActivity";
    ImiDevice imiDevice;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_four);
        ImiNect.initialize();
        imiDevice = ImiDevice.getInstance();
        imiDevice.open(this, ImiDevice.ImiStreamType.DEPTH.toNative(), new ImiDevice.OpenDeviceListener() {
            @Override
            public void onOpenDeviceSuccess() {
                //int startStream = imiDevice.startStream(ImiDevice.ImiStreamType.DEPTH.toNative());
                //imiDevice.readNextFrame(ImiDevice.ImiStreamType.DEPTH,1000);
                Toast.makeText(FourActivity.this,"打开成功",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenDeviceFailed(String s) {
                Toast.makeText(FourActivity.this,"打开失败",Toast.LENGTH_SHORT).show();
            }
        });
        imiDevice.addDeviceStateListener(new ImiDevice.DeviceStateListener() {
            @Override
            public void onDeviceStateChanged(String s, ImiDeviceState imiDeviceState) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImiNect.destroy();
    }
}