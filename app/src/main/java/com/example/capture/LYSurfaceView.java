package com.example.capture;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

public class LYSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{
    private Camera.Size size;
    private Camera mCamera;
    private MediaCodec mediaCodec;
    byte[] buffer;
    byte[] nv21_rotated;
    byte[] nv12;
    private volatile boolean isCaptrue;
    private String TAG = "LYSurfaceView";

    public LYSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (isCaptrue) {
            isCaptrue = false;
            nv21_rotated = new byte[bytes.length];
            nv21_rotated= portraitData2Raw(bytes);
            captrue(nv21_rotated);
//            byte[] temp = nv21toNV12(nv21_rotated);
//            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//            int inIndex = mediaCodec.dequeueInputBuffer(100000);
//            if (inIndex >= 0) {
//                ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inIndex);
//                byteBuffer.clear();
//                byteBuffer.put(temp, 0, temp.length);
//                mediaCodec.queueInputBuffer(inIndex, 0, temp.length,
//                        0, 0);
//            }
//            int outIndex = mediaCodec.dequeueOutputBuffer(info, 100000);
//            if (outIndex >= 0) {
//                ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
//                byte[] ba = new byte[byteBuffer.remaining()];
//                byteBuffer.get(ba);
//                writeBytes(ba);
//                writeContent(ba);
//                mediaCodec.releaseOutputBuffer(outIndex, false);
//            }
        }
        mCamera.addCallbackBuffer(bytes);
    }
    int index = 0;
    public void captrue(byte[] temp){
        //保存一张照片
        String fileName = "IMG_" + String.valueOf(index++) + ".jpg";  //jpeg文件名定义
        File sdRoot = Environment.getExternalStorageDirectory();    //系统路径

        File pictureFile = new File(sdRoot,   fileName);
        if (!pictureFile.exists()) {
            try {
                pictureFile.createNewFile();

                FileOutputStream filecon = new FileOutputStream(pictureFile);
//ImageFormat.NV21 and ImageFormat.YUY2 for now
                YuvImage image = new YuvImage(temp, ImageFormat.NV21,size.height,size.width, null);   //将NV21 data保存成YuvImage
                //图像压缩
                image.compressToJpeg(
                        new Rect(0, 0, image.getWidth(), image.getHeight()),
                        100, filecon);   // 将NV21格式图片，以质量70压缩成Jpeg，并得到JPEG数据流
                isCaptrue = false;
            }catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        startPreview();
        //initCodec();
    }

    private void startPreview() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        Camera.Parameters parameters = mCamera.getParameters();
        size = parameters.getPreviewSize();
        try {
            mCamera.setPreviewDisplay(getHolder());
            mCamera.setDisplayOrientation(90);
            buffer = new byte[size.width * size.height * 3 / 2];
            nv21_rotated = new byte[size.width * size.height * 3 / 2];
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initCodec() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            final MediaFormat format = MediaFormat.createVideoFormat("video/avc", size.height, size.width);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 4000_000);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);//2s一个I帧
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }

    public void startCaptrue() {
        isCaptrue = true;
    }

    private byte[] portraitData2Raw(byte[] data) {
        int width = size.width;
        int height = size.height;
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;
        int i = 0;
        int startPos = (height - 1)*width;
        for (int x = 0; x < width; x++)
        {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--)
            {
                nv21_rotated[i] = data[offset + x];
                i++;
                offset -= width;
            }
        }
        i = buffser_size - 1;
        for (int x = width - 1; x > 0; x = x - 2)
        {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++)
            {
                nv21_rotated[i] = data[offset + x];
                i--;
                nv21_rotated[i] = data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }
        return nv21_rotated;
    }

    byte[] nv21toNV12(byte[] nv21) {
        int size = nv21.length;
        nv12 = new byte[size];
        int len = size * 2 / 3;
        System.arraycopy(nv21, 0, nv12, 0, len);

        int i = len;
        while (i < size - 1) {
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
        return nv12;
    }

    public void writeBytes(byte[] array) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(Environment.getExternalStorageDirectory()+"/codec.h264", true);
            writer.write(array);
            writer.write('\n');


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(writer != null){
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public   String writeContent(byte[] array) {
        char[] HEX_CHAR_TABLE = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        FileWriter writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileWriter(Environment.getExternalStorageDirectory()+"/codec.txt", true);
            writer.write(sb.toString());
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(writer != null){
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
