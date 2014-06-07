package com.aronbordin.robo.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.lang.Runnable;


/**
 * Created by neo on 06/06/14.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, View.OnTouchListener{
    private int mResolucaoWidth = 320, mResolucaoHeight = 240;
    private boolean isRunning = false;
    private boolean isCalibrando = false;
    private boolean isCalibrado = false;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int mImageRGB[] = new int[mResolucaoHeight*mResolucaoWidth];
    private Activity parent;
    private FrameLayout mBloco0, mBloco1, mBloco2, mBloco3, mBloco4;
    private double[] blocosMedia = new double[5];
    private double[] blocosQtdSoma = new double[5];
    private int[] blocosMenor = new int[5];
    private int[] blocosMaior = new int[5];
    private int blocosDivisor;
    private int calibrarContador = 0;

    CameraPreview(Context context, Camera camera, Activity p){
        super(context);
        parent = p;
        mCamera = camera;


        mHolder = getHolder();//holder para ler se a surface foi criada/destruida
        mHolder.addCallback(this);//add os envetos aqui
    //    mHolder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
      //  FrameLayout frame = (FrameLayout)findViewById(R.id.frameCamera2);

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        synchronized (this) {
            if(isRunning)
                return;
            try {//surface criada, tem q fazer a camera mostrar os dados
               this.setWillNotDraw(false);//desabilita o draw auto do canvas
                isRunning = true;

                Camera.Parameters p = mCamera.getParameters();

                p.setPreviewSize(mResolucaoWidth, mResolucaoHeight);
//                p.setPreviewFormat(ImageFormat.YV12);
                mCamera.setParameters(p);


                mCamera.setPreviewDisplay(surfaceHolder);//camera mostra aqui
                mCamera.setPreviewCallback(this);
                mCamera.startPreview();

                loadBlocos();
            } catch (Exception e) {
                Log.e("ERR", "Erro ao setar preview da camera: " + e.getMessage());
            }
        }

    }

    private void loadBlocos(){
        mBloco0 = (FrameLayout)parent.findViewById(R.id.frameBloco0);
        mBloco1 = (FrameLayout)parent.findViewById(R.id.frameBloco1);
        mBloco2 = (FrameLayout)parent.findViewById(R.id.frameBloco2);
        mBloco3 = (FrameLayout)parent.findViewById(R.id.frameBloco3);
        mBloco4 = (FrameLayout)parent.findViewById(R.id.frameBloco4);

        mBloco0.setOnTouchListener(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        synchronized (this) {
            try {
                if (mCamera != null) {
                    //mHolder.removeCallback(this);
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    isRunning  = false;
                    mCamera.release();
                }
            } catch (Exception e) {
                Log.e("Camera", e.getMessage());
            }
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(!isRunning)
            return;
        Canvas canvas = null;
        if(mHolder == null)
            return;
        try{
            synchronized (mHolder){
                int i, k, j = -1;
                for (i = 0; i < 5; i++) {
                    blocosMedia[i] = 0;
                    blocosQtdSoma[i] = 0;
                }
                for (i = 20; i < 40; i++) {
                    for (k = mResolucaoWidth * i; k < mResolucaoWidth * (i + 1); k++) {
                        if (k % ((int) mResolucaoWidth / 5) == 0)
                            j++;
                        blocosMedia[j] += ((int) data[k]) & 0xff;
                        blocosQtdSoma[j]++;
                    }
                    j = -1;
                }

                for (i = 0; i < 5; i++)
                    blocosMedia[i] = blocosMedia[i] / blocosQtdSoma[i];


                if(isCalibrando) {
                    Calibrar();
                    return;
                }
                mBloco0.setBackgroundColor(Color.RED);


            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(canvas != null){
                mHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    protected void Calibrar(){
        int i;
        for(i = 0; i < 5; i++){
            if(blocosMedia[i] > blocosMaior[i])
                blocosMaior[i] = (int)blocosMedia[i];
            if(blocosMedia[i] < blocosMenor[i])
                blocosMenor[i] = (int)blocosMedia[i];
        }
        calibrarContador++;
        isCalibrando = true;
        if(calibrarContador == 50)
            CalibrarFim();
    }

    protected void CalibrarFim(){
        int somaMenor = 0, somaMaior = 0;
        int i;
        for(i = 0; i < 5; i++){
            somaMaior += blocosMaior[i];
            somaMenor += blocosMenor[i];
        }
        somaMaior /= 5;
        somaMenor /= 5;
        blocosDivisor = (somaMaior + somaMenor)/2;
        isCalibrando = false;
        isCalibrado = true;
        calibrarContador = 0;
        Log.d("OK", "Calibrado!");
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v == mBloco0){
            Log.d("CAB", "Calibrando...");
            isCalibrando = true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
       // canvas.drawRect(new Rect((int) Math.random() * 100,
            //    (int) Math.random() * 100, 200, 200), rectanglePaint);
     //  canvas.drawRect(0, 0, 200, 200,rectanglePaint);
    }


    //  Byte decoder : ---------------------------------------------------------------------
    void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        // Pulled directly from:
        // http://ketai.googlecode.com/svn/trunk/ketai/src/edu/uic/ketai/inputService/KetaiCamera.java
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }


}

