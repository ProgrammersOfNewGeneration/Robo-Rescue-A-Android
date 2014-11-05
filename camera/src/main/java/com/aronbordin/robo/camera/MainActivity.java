package com.aronbordin.robo.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * @author Aron Bordin <aron.bordin@gmail.com>
 * Classe principal, irá iniciar o UI e depois a lógica
 */
public class MainActivity extends Activity {
    private Camera mCamera = null;
    public CameraRobo mPreview;
    public BluetoothRobo mBluetooth;
    private FrameLayout mFrameCamera;
    public Robo mRobo;
    public Logger mLogger;
    private MainActivity self;
    protected PowerManager.WakeLock mWakeLock;

    /**
     * Ao ser criado, inicia todos os objetos do projeto e realiza a conexão
     * Caso tenha algum problema, informa no log     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        self = this;
        final PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock  = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Tag");
        mWakeLock.acquire();

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

        conecta();
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBluetooth = new BluetoothRobo(self);
                mBluetooth.Conectar();
            }
        });*/

        mLogger.Logar("\t\tCriando robô...");
        mRobo = new Robo(this);
        mLogger.Logar("\t\tOk!");



        mLogger.Logar("->Ok! Objetos criados!");
    }

    public void conecta()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBluetooth = new BluetoothRobo(self);
                mBluetooth.Conectar();
            }
        });
    }

    @Override
    public void onDestroy(){
        mWakeLock.release();
        super.onDestroy();
    }

    /**
     * Cria o objeto camera, pegando a camera padrão do sistema e rotacionando corretamente
     */
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


    /**
     * Método para iniciar a execução do rob^o
     */
    public void iniciarRobo(){
        mRobo.IniciarRobo();
    }

    /**
     * Método para parar o execução do robô
     */
    public void pararRobo(){
        mRobo.PararRobo();
    }
}
