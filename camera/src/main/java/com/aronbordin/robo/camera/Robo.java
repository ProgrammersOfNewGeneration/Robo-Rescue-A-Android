package com.aronbordin.robo.camera;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by neo on 08/06/14.
 */
public class Robo extends Thread{
    private CameraRobo mCameraPreview;
    private BluetoothRobo mBluetoothRobo;
    private MainActivity parent;
    private Logger mLogger;
    private boolean isRodando = false;
    private boolean isSeguindoLinha = true;
    private List<String> mFuncoes = new ArrayList<String>();

    public Robo(MainActivity p){
        parent = p;
        mCameraPreview = p.mPreview;
        mBluetoothRobo = p.mBluetooth;
        mLogger = p.mLogger;
    }

    public void IniciarRobo(){
        if(!isRodando) {
            this.start();
            isRodando = true;
            Logar("->Iniciando robô");
        }
    }

    public void PararRobo(){
        if(isRodando) {
            this.stop();
            isRodando = false;
            Logar("->Parando robô");
        }
    }

    @Override
    public void run(){
        try{
            while(true) {
                Loop();
                sleep(30);
            }
        } catch (Exception e){
            LogarErro("Erro de execução: " + e.getMessage());
        }

    }

    public void Loop(){
        if(isSeguindoLinha){
            seguirLinha();
            chamarFuncao();
        }
    }

    private void seguirLinha(){
        int c = 0, i;
        for (i=0; i<5; i++)
            c = c*2 + mCameraPreview.isPreto(i);

        switch (c) {
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30://## encruzilhgadas!!!!
            case 31://##
                Logar("->Encruzilhada!!");
                break;
            case 8:
            case 12:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
                Logar("->Virar Esquerda!");
                break;
            case 4:
            case 14://##
                Logar("->ir Fretne!!");
                break;
            case 1:
            case 2:
            case 3:
            case 5: //##
            case 6:
            case 7:
            case 15:
            case 23://##
                Logar("->Virar Direita!!");
                break;
        }
    }

    private void Logar(String msg){
        final String log = msg;
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLogger.Logar(log);
            }
        });
    }

    private void LogarErro(String msg){
        final String log = msg;
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLogger.LogarErro(log);
            }
        });
    }
    public void chamarFuncao(){
        Iterator i = mFuncoes.iterator();
        while(i.hasNext()){
            String f = (String)i.next();
        }
    }

}
