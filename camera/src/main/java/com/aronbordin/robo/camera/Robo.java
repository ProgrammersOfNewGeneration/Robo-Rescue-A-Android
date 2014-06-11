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
    private boolean isEncruzilhada = false;
    private boolean isSeguindoLinha = true;
    private boolean isDesviando = false;
    private List<String> mFuncoes = new ArrayList<String>();
    private long interacao = 0;
    private int msgPedida = 0;


    public Robo(MainActivity p){
        parent = p;
        mCameraPreview = p.mPreview;
        mBluetoothRobo = p.mBluetooth;
        mLogger = p.mLogger;
    }

    public void IniciarRobo(){
        if(!isRodando) {
            isRodando = true;
            if(!this.isAlive())
                this.start();
            Logar("->Iniciando robô");
        }
    }

    public void PararRobo(){
        if(isRodando) {
            isRodando = false;
            mFuncoes.clear();
            mFuncoes.add("3@10#");
            chamarFuncao();
            Logar("->Parando robô");
        }
    }

    @Override
    public void run(){
        try{
            while(true) {
                if(isRodando)
                    Loop();
                sleep(30);
            }
        } catch (Exception e){
            LogarErro("Erro de execução: " + e.getMessage());
        }

    }

    public void Loop(){
        interacao++;
        if((isSeguindoLinha) && (!isEncruzilhada) && (!isDesviando)){
            seguirLinha();
            chamarFuncao();
        }

    }

    private void seguirLinha(){
        int c = 0, i;
        String cmd;

        for (i=0; i<5; i++)
            c = c*2 + mCameraPreview.isPreto(i);

        switch (c) {
            case 0:
                Logar("->Gap...");
                cmd = "3@4#";
                mFuncoes.add(cmd);
            break;
//            case 24:
//            case 25:
//            case 26:
//            case 27:
//            case 28:
//            case 29:
//            case 30://## encruzilhgadas!!!!
            case 7:
            case 15:
            case 31://##
                Logar("->Encruzilhada!!");
                new Thread() {
                    public void run() {
                        isEncruzilhada = true;
                        String cmd = "3@19@" + msgPedida + "#";//frente
                        Logar("-->Encruzilhada:  - >" + pedirValor(cmd, msgPedida++));
                        isEncruzilhada = false;
                    }
                }.start();

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
                cmd = "3@7#";
                mFuncoes.add(cmd);
                break;
            case 4:
            case 14://##
                Logar("->ir Frente!!");
                cmd = "3@4#";
                mFuncoes.add(cmd);
                break;
            case 1:
            case 2:
            case 3:
            case 5: //##
            case 6:
         //   case 7:
          //  case 15:
            case 23://##
                Logar("->Virar Direita!!");
                cmd = "3@5#";
                mFuncoes.add(cmd);
                break;
        }

        if((interacao % 10) == 0)
            checarDistancia();
    }

    private void checarDistancia(){
//        String msg = "3@17@" + String.valueOf(msgPedida) + "#";
//        String valor;
//        valor = pedirValor(msg, msgPedida++);
//        valor = valor.trim();
        int dist = getDistancia();
        if((dist<20) && (dist >= 5)){
            isDesviando = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Desviar();
                }
            }).start();

        }
    }

    private int getDistancia(){
        String msg = "3@17@" + String.valueOf(msgPedida) + "#";
        String valor;
        valor = pedirValor(msg, msgPedida++);
        valor = valor.trim();
        return Integer.valueOf(valor);
    }

    public void Desviar(){
        Logar("->Desviando...");
        mFuncoes.clear();
        //girando para a direita e iniciando desvio
        mFuncoes.add("3@22@90#"); //gira sendor 90º = esquerda
        mFuncoes.add("3@6#"); //direita forte
        mFuncoes.add("3@18@1500#"); //delay(1000)

        //girou direita, irá para frente
        mFuncoes.add("3@4#"); //ir frente
        chamarFuncao();
        Logar("\t\tPassando obstáculo...");
        while(getDistancia() < 30){ //frente até passar o obstaculo
            try {
                sleep(100);
            } catch (InterruptedException e){
                //
            }
        }
        Logar("\t\tVirando...");
        mFuncoes.clear();
        mFuncoes.add("3@4#");//andar mais um pouco, por seguranca
        mFuncoes.add("3@18@2000#");
        //inicia girar esquerda para passar pelo obstaculo
        mFuncoes.add("3@8#");//esqueda forte
        mFuncoes.add("3@18@1500#"); //delay
        //girou, anda frente até passar obstáculo
        mFuncoes.add("3@4#");
        chamarFuncao();
        while(getDistancia() > 30)//anda até estar alinhado com o obstaculo
        {
            try {
                sleep(100);
            } catch (InterruptedException e){
                //
            }
        }

        while(getDistancia() < 30){//já encontrou, agr qdo for maior q 30 ele já ultrapassou o obs
            try{
                sleep(100);
            } catch (InterruptedException e){
                //
            }
        }

        mFuncoes.clear();
        mFuncoes.add("3@4#");//ir mais para frente
        mFuncoes.add("3@18@3000#"); //delay

        //virar esquerda pra voltar linha
        mFuncoes.add("3@8#");
        mFuncoes.add("3@18@1500#");//delay
        mFuncoes.add("3@4#"); //anda frente
        chamarFuncao();
        while (mCameraPreview.isPreto(2) == 0){//anda reto até achar a linha
            try{
                sleep(50);
            } catch(InterruptedException e){
                //
            }
        }
        //vira direita para voltar na pista
        mFuncoes.add("3@6#");//direita forte
        mFuncoes.add("3@18@1000#");
        mFuncoes.add("3@22@0#");
        chamarFuncao();
        isDesviando = false;


    }

    public String pedirValor(String msg, int id){
        synchronized (mBluetoothRobo) {
            mBluetoothRobo.enviarMsg(msg);

            while (!mBluetoothRobo.hasMensagem(id))
                try {
                    mBluetoothRobo.wait(10);
                } catch (InterruptedException e) {
                    LogarErro(e.getMessage());
                }
            mBluetoothRobo.notify();
        }
        String retorno = mBluetoothRobo.getMensagem(id);
        retorno = retorno.split("@")[1];
        return  retorno;
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
            mBluetoothRobo.enviarMsg(f);
        }
        mFuncoes.clear();
    }
}
