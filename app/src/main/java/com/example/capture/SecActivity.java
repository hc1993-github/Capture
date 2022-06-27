package com.example.capture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.SyncStateContract;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;

public class SecActivity extends AppCompatActivity {
    PreviewView mPreviewView;
    ProcessCameraProvider mProcessCameraProvider;
    Preview mPreview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sec);
        mPreviewView = findViewById(R.id.view_finder);
        mPreviewView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
        checkPermission();
    }
    private void checkPermission(){
        if(Build.VERSION.SDK_INT>=23){
            boolean cameraPermission = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            if(cameraPermission){
                startCamera();
            }else {
                requestPermissions(new String[]{Manifest.permission.CAMERA},1);
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);
        try {
            mProcessCameraProvider = listenableFuture.get();
            mPreview = new Preview.Builder().setTargetResolution(new Size(640,480)).build();
            mPreview.setSurfaceProvider(mPreviewView.createSurfaceProvider());
            mProcessCameraProvider.bindToLifecycle(SecActivity.this,CameraSelector.DEFAULT_FRONT_CAMERA,mPreview);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void takePhoto() {
        ImageCapture  imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(new Size(640,480)).build();
        File file=new File(Environment.getExternalStorageDirectory(),System.currentTimeMillis()+".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        mProcessCameraProvider.bindToLifecycle(SecActivity.this,CameraSelector.DEFAULT_FRONT_CAMERA,imageCapture);
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(SecActivity.this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                Toast.makeText(SecActivity.this,"拍摄成功",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(ImageCaptureException exception) {
                Toast.makeText(SecActivity.this,"拍摄失败",Toast.LENGTH_SHORT).show();
            }
        });
    }
}