package com.example.capture;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

public class Camera2Opener {
    TextureView textureView;
    Context context;
    Size bestPreviewSize;
    Point previewViewSize;
    HandlerThread handlerThread;
    Handler handler;
    CameraDevice cameraDevice;
    CaptureRequest.Builder previewRequestBuilder;
    ImageReader imageReader;
    PreviewListener listener;
    CameraCaptureSession captureSession;
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice device) {
            cameraDevice = device;
            createPreviewRequest();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice device) {
            device.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice device, int i) {
            device.close();
            cameraDevice =null;
        }
    };
    CameraCaptureSession.StateCallback sessionCallBack = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if(cameraDevice == null){
                return;
            }
            try {
                captureSession = session;
                captureSession.setRepeatingRequest(previewRequestBuilder.build(), new CameraCaptureSession.CaptureCallback(){},handler);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

        }
    };
    public Camera2Opener(Context cont) {
        context = cont;
        listener = (PreviewListener) cont;
    }

    public synchronized void startPreview(TextureView texview) {
        textureView = texview;
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics("1");
            StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            bestPreviewSize = getBestSupportedSize(new ArrayList<Size>(Arrays.asList(streamConfigurationMap.getOutputSizes(SurfaceTexture.class))));
            imageReader = ImageReader.newInstance(bestPreviewSize.getWidth(),
                    bestPreviewSize.getHeight(),
                    ImageFormat.JPEG, 2
            );
            handlerThread = new HandlerThread("CameraBackground");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
            imageReader.setOnImageAvailableListener(new OnImageAvailableListenerImpl(),handler);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera("1", stateCallback, handler);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void createPreviewRequest() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(bestPreviewSize.getWidth(),bestPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewRequestBuilder.addTarget(surface);
            Surface surface1 = imageReader.getSurface();
            previewRequestBuilder.addTarget(surface1);
            cameraDevice.createCaptureSession(Arrays.asList(surface,surface1),sessionCallBack,handler);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    private Size getBestSupportedSize(List<Size> sizes) {
        Point maxPreviewSize = new Point(1920, 1080);
        Point minPreviewSize = new Point(1280, 720);
        Size defaultSize = sizes.get(0);
        Size[] tempSizes = sizes.toArray(new Size[0]);
        Arrays.sort(tempSizes, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                if (o1.getWidth() > o2.getWidth()) {
                    return -1;
                } else if (o1.getWidth() == o2.getWidth()) {
                    return o1.getHeight() > o2.getHeight() ? -1 : 1;
                } else {
                    return 1;
                }
            }
        });
        sizes = new ArrayList<>(Arrays.asList(tempSizes));
        for (int i = sizes.size() - 1; i >= 0; i--) {
            if (maxPreviewSize != null) {
                if (sizes.get(i).getWidth() > maxPreviewSize.x || sizes.get(i).getHeight() > maxPreviewSize.y) {
                    sizes.remove(i);
                    continue;
                }
            }
            if (minPreviewSize != null) {
                if (sizes.get(i).getWidth() < minPreviewSize.x || sizes.get(i).getHeight() < minPreviewSize.y) {
                    sizes.remove(i);
                }
            }
        }
        if (sizes.size() == 0) {
            return defaultSize;
        }
        Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null) {
            previewViewRatio = (float) previewViewSize.x / (float) previewViewSize.y;
        } else {
            previewViewRatio = (float) bestSize.getWidth() / (float) bestSize.getHeight();
        }

        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }

        for (Size s : sizes) {
            if (Math.abs((s.getHeight() / (float) s.getWidth()) - previewViewRatio) < Math.abs(bestSize.getHeight() / (float) bestSize.getWidth() - previewViewRatio)) {
                bestSize = s;
            }
        }
        return bestSize;
    }

    private class OnImageAvailableListenerImpl implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image= reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            //Bitmap bitmap = bytes2bitmap(bytes);
            if(listener!=null){
                listener.onCpature(bitmap);
            }
            image.close();
        }
    }
    public interface PreviewListener{
        void onPreview(byte[] y, byte[] u, byte[] v, Size previewSize, int stride);
        void onCpature(Bitmap bitmap);
    }
    public Bitmap bytes2bitmap(byte[] bytes){
        InputStream inputStream = null;
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        inputStream = new ByteArrayInputStream(bytes);
        SoftReference softReference = new SoftReference(BitmapFactory.decodeStream(inputStream,null,options));
        bitmap = (Bitmap) softReference.get();
        try {
            if(inputStream!=null){
                inputStream.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return bitmap;
    }
}
