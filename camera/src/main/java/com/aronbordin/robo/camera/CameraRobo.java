package com.aronbordin.robo.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by neo on 06/06/14.
 * @author Aron Bordin <aron.bordin@gmail.com>
 * Classe para gerenciar a camera do robô. Irá analisar as imagens, e contém helpers para ler seu conteúdo
 */
public class CameraRobo extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, View.OnTouchListener{
    private int mResolucaoWidth = 320, mResolucaoHeight = 240;
    private boolean isRunning = false;
    private boolean isCalibrando = false;
    private boolean isCalibrado = false;
    private boolean isFlashOn = false;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private MainActivity parent;
    private FrameLayout mBloco0, mBloco1, mBloco2, mBloco3, mBloco4;
    private List<FrameLayout> mBlocos = new ArrayList<FrameLayout>();
    private double[] blocosMedia = new double[5];
    private double[] blocosQtdSoma = new double[5];
    private int[] blocosMenor = new int[5];
    private int[] blocosMaior = new int[5];
    private int blocosDivisor = 100;
    private int calibrarContador = 0;
    private boolean podeProcessar = false;
    private boolean isRodando = false;



    /**
     * Construtor do objeto. Irá iniciar a camera e mostrar o resultado na tela
     * @param context Contextp do aplicativo
     * @param camera Objeto camera
     * @param p parent - Cópia do MainActivity
     */
    CameraRobo(Context context, Camera camera, MainActivity p){
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
                zerarCalibrar();

                Camera.Parameters p = mCamera.getParameters();

                p.setPreviewSize(mResolucaoWidth, mResolucaoHeight);


                mCamera.setParameters(p);

                mCamera.setPreviewDisplay(surfaceHolder);//camera mostra aqui
                mCamera.setPreviewCallback(this);
                mCamera.startPreview();

                loadBlocos();
            } catch (Exception e) {
                parent.mLogger.LogarErro("\t\tErro ao setar preview da camera: " + e.getMessage());
            }
        }

    }

    /**
     * Carrega os blocos de visualização para a memória, podendo acessá-los depois
     */
    private void loadBlocos(){
        mBloco0 = (FrameLayout)parent.findViewById(R.id.frameBloco0);
        mBloco1 = (FrameLayout)parent.findViewById(R.id.frameBloco1);
        mBloco2 = (FrameLayout)parent.findViewById(R.id.frameBloco2);
        mBloco3 = (FrameLayout)parent.findViewById(R.id.frameBloco3);
        mBloco4 = (FrameLayout)parent.findViewById(R.id.frameBloco4);

        mBloco0.setOnTouchListener(this);
        mBloco1.setOnTouchListener(this);
        mBloco2.setOnTouchListener(this);
        mBloco3.setOnTouchListener(this);
        mBloco4.setOnTouchListener(this);


        mBlocos.add(mBloco0);
        mBlocos.add(mBloco1);
        mBlocos.add(mBloco2);
        mBlocos.add(mBloco3);
        mBlocos.add(mBloco4);
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

    /**
     * Callback chamado a cada novo frame da camera
     * @param data array de bytes, onde cada byte representa um pixel
     * @param camera objeto camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(!isCalibrando)
            if(!podeProcessar && isRodando)
                return;
        if(!isRunning)
            return;
        Canvas canvas = null;
        if(mHolder == null)
            return;
        try{
            //impede que esse bloco mais de uma vez ao mesmo tempo
            synchronized (mHolder){
                int i, k, j = 5;
                for (i = 0; i < 5; i++) {
                    blocosMedia[i] = 0;
                    blocosQtdSoma[i] = 0;
                }

                int posFaixa = 300;
                int nivelPreto = 70;
                for (i=0; i<240; i++) {
                    if(i % 48 == 0)
                        j--;
                    blocosMedia[j] += ((int) data[posFaixa + 320 * i]) & 0xff;
                    blocosQtdSoma[j]++;
                }

                for (i = 0; i < 5; i++)
                    blocosMedia[i] = blocosMedia[i] / blocosQtdSoma[i];

                if(isCalibrando) {
                    Calibrar();
                    return;
                }
                for(i = 0; i < 5; i++)
                    mBlocos.get(i).setBackgroundColor(blocosMedia[i] > blocosDivisor ? Color.WHITE : Color.BLACK);

                if(!isCalibrando)
                    podeProcessar = false;
            }
        } catch (Exception e){
            e.printStackTrace();
            parent.mLogger.LogarErro("Erro leitura: " + e.getMessage());
        } finally {
            if(canvas != null){
                mHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * Limpa os dados calibrados
     */
    protected  void zerarCalibrar(){
        for(int i = 0; i < 5; i++){
            blocosMenor[i] = 1000;
            blocosMaior[i] = -1;
        }

    }

    /**
     * Seta se o robõ está em execução
     * @param rodando está rodando
     */
    public void setIsRodando(boolean rodando){
        isRodando = rodando;
    }

    /**
     * Calibra a camera com o ambiente
     */
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
        if(calibrarContador == 100)
            CalibrarFim();

    }

    /**
     * Termina a calibração, analisando os dados coletados e gerando os novos limites
     */
    protected void CalibrarFim(){
        int Menor = 1000, Maior = -1;
        int i;
        for(i = 0; i < 5; i++){
            if(blocosMaior[i] > Maior)
                Maior = blocosMaior[i];
            if(blocosMedia[i] < Menor)
                Menor = blocosMenor[i];
        }

        blocosDivisor = (Maior + Menor)/2;
        parent.mLogger.Logar("-> Maior: " + Maior + " Menor: "  + Menor + " Media: " + blocosDivisor);
//        String msg = "";
//        for(i = 0; i < 5; i++){
//            msg += blocosMaior[i] + " _ " ;
//        }
//        parent.mLogger.Logar("Maiores: " + msg);
//        msg = "";
//        for(i = 0; i < 5; i++){
//            somaMenor += blocosMenor[i];
//            msg += blocosMenor[i] + " _ ";
//        }
//        parent.mLogger.Logar("Menores " + msg);


        isCalibrando = false;
        isCalibrado = true;
        calibrarContador = 0;
        zerarCalibrar();
        parent.mLogger.Logar("Ok! Calibrado, divisor = " + blocosDivisor);

    }

    /**
     * Permite o processamento de imagens
     */
    public void Processar(){
        podeProcessar = true;
    }

    /**
     * Informa se os dados já foram processados
     * @return boolean se os dados já foram analisados
     */
    public boolean DadosProcessados(){
        return !podeProcessar;
    }

    /**
     * Testa se o bloco desejado é preto
     * @param k ID do bloco, de 0 à 4
     * @return 1 se igual a preto, 0 se branco
     */
    public int isPreto(int k){
        return blocosMedia[k] < blocosDivisor ? 1 : 0;
    }

    /**
     * Testa se o bloco desejado é branco
     * @param k ID do bloco, de 0 à 4
     * @return 1 se igual a branco, 0 se preto
     */
    public int isBranco(int k){
        return blocosMedia[k] > blocosDivisor ? 1 : 0;
    }

    /**
     * Gerencia os toques na tela
     * @param v Vie
     * @param event Evnto
     * @return True se o evento foi analisado
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v == mBloco0){
            parent.mLogger.Logar("->Calibrando...");
            isCalibrando = true;
        }

        if(v == mBloco1){
            try {
                if (isFlashOn) {
                    Camera.Parameters p = mCamera.getParameters();
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(p);
                    isFlashOn = false;
                    parent.mLogger.Logar("->Flash off");
                } else {
                    Camera.Parameters p = mCamera.getParameters();
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(p);
                    isFlashOn = true;
                    parent.mLogger.Logar("->Flash on");
                }
            } catch(Exception e){
                parent.mLogger.LogarErro("Erro ao ligar/desligar flash: " + e.getMessage());
            }
        }

        if(v == mBloco2){
            /*new Thread(new Runnable() {
                @Override
                public void run() {
                    parent.mRobo.Desviar();
                }
            }).start();*/






        }

        if(v == mBloco3){
            parent.iniciarRobo();
        }

        if(v == mBloco4){
            parent.pararRobo();
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
       // canvas.drawRect(new Rect((int) Math.random() * 100,
            //    (int) Math.random() * 100, 200, 200), rectanglePaint);
     //  canvas.drawRect(0, 0, 200, 200,rectanglePaint);
    }


    /**
     * Converte o formato da imagem
     * @param rgb
     * @param yuv420sp
     * @param width
     * @param height
     */
    protected void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
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

