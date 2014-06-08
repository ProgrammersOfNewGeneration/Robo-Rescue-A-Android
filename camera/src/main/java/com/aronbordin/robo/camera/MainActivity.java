package com.aronbordin.robo.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;


public class MainActivity extends Activity {
    private Camera mCamera = null;
    public CameraRobo mPreview;
    public BluetoothRobo mBluetooth;
    private FrameLayout mFrameCamera;
    private Robo mRobo;
    public Logger mLogger;
    private MainActivity self;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        self = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLogger = new Logger((TextView)findViewById(R.id.txtLogger), (ScrollView)findViewById(R.id.scrollTxtLogger));
        mLogger.Logar("->Criando objetos...");


        createCamera();
        if(mCamera != null) {
            mPreview = new CameraRobo(this, mCamera, this);
            mFrameCamera = (FrameLayout) findViewById(R.id.frameCamera);
            mFrameCamera.addView(mPreview);
        }


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBluetooth = new BluetoothRobo(self);
                mBluetooth.Conectar();
            }
        });

        mLogger.Logar("\t\tCriando robô...");
        mRobo = new Robo(this);
        mLogger.Logar("\t\tOk!");



        mLogger.Logar("->Ok! Objetos criados!");
    }

    protected void createCamera(){
        try{
            mCamera = Camera.open(0);
            mCamera.setDisplayOrientation(90);
            mLogger.Logar("\t\tCriando camera...");
        } catch (Exception e){
            mLogger.LogarErro("->Erro ao abrir a camera!");
            mCamera = null;
        }
    }

    public void iniciarRobo(){
        mRobo.IniciarRobo();
    }

    public void pararRobo(){
        mRobo.PararRobo();
    }
}
