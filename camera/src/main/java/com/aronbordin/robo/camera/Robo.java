package com.aronbordin.robo.camera;

import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by neo on 08/06/14.
 * @author Aron Bordin <aron.bordin@gmail.com>
 * Classe com toda a lógica do projeto.
 * Irá ler os dados via bluetooth, tomar decisões e movimentar o robô
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
    private String ultimaMsg = "";
    public Compass mCompass;
    private long lastTime = 0;
    private int FPS = 30;
    private boolean pLinha = false;


    /**
     * Construtor do objeto.
     * @param p parent = MainActivity
     */
    public Robo(MainActivity p){
        mCompass = Compass.getInstance(p);
        parent = p;
        mCameraPreview = p.mPreview;
        mBluetoothRobo = p.mBluetooth;
        mLogger = p.mLogger;
        try {
            this.start();
        } catch (Exception e){
            //
        }
    }


    /**
     * Método para iniciar a execução do robô
     */
    public void IniciarRobo(){
        if(!isRodando) {
            isRodando = true;
            isSeguindoLinha = true;
            isDesviando = false;
            ultimaMsg = "";
            isEncruzilhada = false;
            mCameraPreview.setIsRodando(true);
            Logar("->Iniciando robô");
        }
    }

    /**
     * Método para parar a execução do robô
     */
    public void PararRobo(){
        if(isRodando) {
            isRodando = false;
            mCameraPreview.setIsRodando(false);
            mFuncoes.clear();
            mFuncoes.add("3@10#");
            chamarFuncao();
            Logar("->Parando robô");
        }
    }

    /**
     * Thread para manter o robô em execução
     * Nunca chame esse método!! Usado internamente para manter o robô em execução
     */
    @Override
    public void run(){
        try{
            while(true) {
                lastTime = System.currentTimeMillis();
                if(isRodando)
                    Loop();
                int tempoDelay = FPS/1000 - (int)(lastTime - System.currentTimeMillis());
                if(tempoDelay < 0)
                    Logar("-Alerta!!! Lentidão no processamento! Tempo perdido: " + tempoDelay*-1);
                sleep(tempoDelay);
            }
        } catch (Exception e){
            LogarErro("Erro de execução: " + e.getMessage() + e.getStackTrace());
        }
    }

    /**
     * Loop principal do robô
     * Executado a cada 20 ms
     */
    protected void Loop(){
        interacao++;
        lerCamera();
        if((isSeguindoLinha == true) && (isEncruzilhada == false) && (isDesviando == false)){
            seguirLinha();
            chamarFuncao();
        }
    }

    /**
     * Ativa o processamento de imagens e analisa um frame
     */
    protected void lerCamera(){
        mCameraPreview.Processar();
        while (true){
            if(mCameraPreview.DadosProcessados())
                break;
        }
    }

    /**
     * Lógica para seguir linha.
     * Irá analisar a camera e tomar as decisões
     */
    private void seguirLinha(){
        if(isDesviando == true || isEncruzilhada == true)
            return;
        int c = 0, i;
        String cmd;

        for (i=0; i<5; i++)
            c = c*2 + mCameraPreview.isPreto(i);

        switch (c) {
            case 0:
               // Logar("->Gap...");
                cmd = "3@4#";
                if(!ultimaMsg.equals(cmd)) {
                    mFuncoes.add(cmd);
                    ultimaMsg = cmd;
                }
            break;

            case 7:
            case 15:
            case 31://##
                Logar("->Encruzilhada!!");
                new Thread() {
                    public void run() {
                        Encruzilhada();
                    }
                }.start();

                break;
            case 8:
            case 9:
            case 12:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 24:
                cmd = "3@8#";
                if(!ultimaMsg.equals(cmd)) {
                    Logar("->Virar Esquerda!");
                    mFuncoes.add(cmd);
                    ultimaMsg = cmd;
                }
                break;
            case 4:
            case 14://##
                cmd = "3@4#";
                if(!ultimaMsg.equals(cmd)) {
                    Logar("->ir Frente!!");
                    mFuncoes.add(cmd);
                    ultimaMsg = cmd;
                }
                break;
            case 1:
            case 2:
            case 3:
            case 5: //##
            case 6:
         //   case 7:
          //  case 15:
            case 23://##
                cmd = "3@6#";
                if(!ultimaMsg.equals(cmd)) {
                    Logar("->Virar Direita!!");
                    mFuncoes.add(cmd);
                    ultimaMsg = cmd;
                }
                break;
           case 28:
                Logar("->Encruzilhada Invertidat!!");
                new Thread() {
                    public void run() {
                        EncruzilhadaInvertida();
                    }
                }.start();
               break;
        }
        if(isDesviando == true || isEncruzilhada == true)
            return;

        new Thread() {
            @Override
            public void run() {
                try {
                    if ((interacao % 10) == 0)
                        checarDistancia();
                } catch (Exception e) {
                }
            }
        }.start();

    }

    /**
     * Irá ler o ultrassom e testar se existe um obstáculo
     */
    private void checarDistancia(){
        int dist = getDistancia();
        if((dist<=15) && (dist >= 5)){
            Desviar();
        }
    }

    /**
     * Helper para pedir ao robô a distancia atual
     * @return
     */
    private int getDistancia(){
        if (isSeguindoLinha) {
            String msg = "3@17@" + String.valueOf(msgPedida) + "#";
            String valor;
            valor = pedirValor(msg, msgPedida++);
            valor = valor.trim();
            return Integer.valueOf(valor);
        }
        return 0;
    }

    public void EncruzilhadaInvertida(){
        int c = 0, d = 0, i;
        isEncruzilhada = true;
        String msg;
        String v;

        for (int k = 0; k <= 3; k++){
            mFuncoes.clear();
            mFuncoes.add("3@4#");
            mFuncoes.add("3@18@50#");
            mFuncoes.add("3@10#");
            chamarFuncao();

            msg = "3@666@" + String.valueOf(msgPedida) + "#";
            v = pedirValor(msg, msgPedida++);
            lerCamera();
            try{
                sleep(200);
            } catch (InterruptedException e){
                //
            }

            d = d*2+    mCameraPreview.isPreto(4);
            if(d != 0) {
                //parent.mLogger.Logar("Vai encruzilhada");
                Encruzilhada();
                ultimaMsg = "";
                return;
            }

        }


        //for (i=0; i<5; i++)
            //c = c*2 + mCameraPreview.isPreto(3);




            mFuncoes.clear();
            mFuncoes.add("3@4#");
            mFuncoes.add("3@18@900#");
            mFuncoes.add("3@10#");
            chamarFuncao();

            msg = "3@666@" + String.valueOf(msgPedida) + "#";
            v = pedirValor(msg, msgPedida++);

            mFuncoes.clear();
            mFuncoes.add("3@8#");//direita forte
            mFuncoes.add("3@18@500#");

            mFuncoes.add("3@10#");

            chamarFuncao();

            msg = "3@666@" + String.valueOf(msgPedida) + "#";
            v = pedirValor(msg, msgPedida++);

            pLinha = true;
            int s;
            while (pLinha) {
                lerCamera();
                mFuncoes.clear();
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    //
                }
                s = 0;
                for (i = 0; i < 5; i++)
                    s = s * 2 + mCameraPreview.isPreto(i);
                    Logar(""+s);

                if (s == 0) {


                    mFuncoes.add("3@8#");//esquerda forte
                    mFuncoes.add("3@18@100#");

                    mFuncoes.add("3@10#");

                    chamarFuncao();

                    msg = "3@666@" + String.valueOf(msgPedida) + "#";
                    v = pedirValor(msg, msgPedida++);

                }
                else
                {
                    Logar("Saiu Encruzilhada");
                    pLinha = false;
                }

            }






        mFuncoes.clear();
        ultimaMsg = "";
        isEncruzilhada = false;

    }


    public void Encruzilhada(){
        isEncruzilhada = true;
        mFuncoes.clear();
        //String msg = "3@19@" + String.valueOf(msgPedida) + "#";
        //String v = pedirValor(msg, msgPedida++);

        mFuncoes.add("3@4#");//frente
        mFuncoes.add("3@18@20#");
        mFuncoes.add("3@10#");
        chamarFuncao();

        String msg = "3@666@" + String.valueOf(msgPedida) + "#";
        String v = pedirValor(msg, msgPedida++);

        lerCamera();
        mFuncoes.clear();
        try{
            sleep(200);
        } catch (InterruptedException e){
            //
        }
        int c = 0;
        for (int i=0; i<5; i++)
            c = c*2 + mCameraPreview.isPreto(i);

        if(c == 7 || c == 15 || c == 31) {
            //mFuncoes.add("3@4#");//frente
            //mFuncoes.add("3@18@1000#");


            mFuncoes.add("3@6#");//direita forte
            mFuncoes.add("3@18@100#");

            mFuncoes.add("3@10#");

            chamarFuncao();

            msg = "3@666@" + String.valueOf(msgPedida) + "#";
            v = pedirValor(msg, msgPedida++);
        }

        mFuncoes.clear();
        mFuncoes.add("3@4#");//frente
        mFuncoes.add("3@18@900#");
        chamarFuncao();

        msg = "3@666@" + String.valueOf(msgPedida) + "#";
        v = pedirValor(msg, msgPedida++);

        mFuncoes.clear();
        mFuncoes.add("3@6#");//direita forte
        mFuncoes.add("3@18@500#");

        mFuncoes.add("3@10#");

        chamarFuncao();

        msg = "3@666@" + String.valueOf(msgPedida) + "#";
        v = pedirValor(msg, msgPedida++);

        pLinha = true;

        while(pLinha)
        {
            lerCamera();
            mFuncoes.clear();
            try{
                sleep(200);
            } catch (InterruptedException e){
                //
            }
            c = 0;
            for (int i=0; i<5; i++)
                c = c*2 + mCameraPreview.isPreto(i);

            if(c == 0) {
                //mFuncoes.add("3@4#");//frente
                //mFuncoes.add("3@18@1000#");

                mFuncoes.add("3@6#");//direita forte
                mFuncoes.add("3@18@100#");

                mFuncoes.add("3@10#");

                chamarFuncao();

                 msg = "3@666@" + String.valueOf(msgPedida) + "#";
                 v = pedirValor(msg, msgPedida++);

            }
            else
            {
                pLinha = false;
            }

        }

        ultimaMsg = "";
        isEncruzilhada = false;
    }

    /**
     * Helper para desviar de obstáculo
     */
    public void Desviar(){
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.mLogger.Logar("->Desviar");
            }
        });
        isDesviando = true;
        mFuncoes.clear();
        mFuncoes.add("3@6#");//direita forte
        mFuncoes.add("3@18@800#");
        chamarFuncao();
        String msg = "3@666@" + String.valueOf(msgPedida) + "#";
        String valor = pedirValor(msg, msgPedida++);

        mFuncoes.add("3@4#");//frente
        mFuncoes.add("3@18@2500#");
        chamarFuncao();
        msg = "3@666@" + String.valueOf(msgPedida) + "#";
        valor = pedirValor(msg, msgPedida++);

        mFuncoes.add("3@8#");//esquerda
        mFuncoes.add("3@18@1000#");
        chamarFuncao();
        msg = "3@666@" + String.valueOf(msgPedida) + "#";
        valor = pedirValor(msg, msgPedida++);

        mFuncoes.add("3@4#");//frente
        mFuncoes.add("3@18@3500#");
        chamarFuncao();
        msg = "3@666@" + String.valueOf(msgPedida) + "#";
        valor = pedirValor(msg, msgPedida++);

        mFuncoes.clear();
        mFuncoes.add("3@8#");//Esquerda
        mFuncoes.add("3@18@1500#");

        mFuncoes.add("3@4#");//frente
        mFuncoes.add("3@18@1000#");

        chamarFuncao();


        msg = "3@666@" + String.valueOf(msgPedida) + "#";
        valor = pedirValor(msg, msgPedida++);

        isDesviando = false;

        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.mLogger.Logar("\t->Desviar ok");
            }
        });
       /* mFuncoes.add("3@6#");
        chamarFuncao();
        esperarAngulo(90);
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.mLogger.Logar("->90 ok");
            }
        });
        mFuncoes.add("3@4#");
        mFuncoes.add("3@18@2000#");
        mFuncoes.add("3@8#");
        chamarFuncao();
        esperarAngulo(90);
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.mLogger.Logar("->Desviar");
            }
        });
        mFuncoes.add("3@4#");
        mFuncoes.add("3@18@5000#");
        mFuncoes.add("3@8#");
        chamarFuncao();
        esperarAngulo(45);*/

    }

    /**
     * Para a execução até o sensor identificar a diferença de angulação
     * @param angulo angulo de diferença
     */
    private void esperarAngulo(int angulo){
        float anguloInicial, anguloAtual, diffAngulo;
        anguloInicial = mCompass.getDirection();
        while(true) {
            anguloAtual = mCompass.getDirection();
            if(anguloAtual != 0) {
                if (Math.abs(anguloAtual - anguloInicial) <= 180)
                    diffAngulo = Math.abs(anguloAtual - anguloInicial);
                else
                    diffAngulo = 360 - Math.abs(anguloAtual - anguloInicial);

                if (diffAngulo >= angulo)
                    break;
            }
        }
    }

    /**
     * Método para enviar uma mensagem ao robô e esperar um retorno
     * @param msg Mensagem a ser enviada
     * @param id ID do pedido, para identifiar a mensagem posteriormente
     * @return String com o valor pedido
     */
    public String pedirValor(String msg, int id){
       synchronized (mBluetoothRobo) {
            mBluetoothRobo.enviarMsg(msg);
        int k = 0;

            while (!mBluetoothRobo.hasMensagem(id))
                try {
                    mBluetoothRobo.wait(10);
                    if(k++ > 10000){
//                        mBluetoothRobo.notify();
                        return "-6676";
                    }
                } catch (InterruptedException e) {
                    LogarErro(e.getMessage());
                }
            mBluetoothRobo.notifyAll();
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

    /**
     * Irá enviar todas as funções na fila por bluetooth.
     * Após enviar as mensagems, irá limpar a fila
     */
    private void chamarFuncao(){
        Iterator i = mFuncoes.iterator();
        try {
            while (i.hasNext()) {
                String f = (String) i.next();
                mBluetoothRobo.enviarMsg(f);
            }
        } catch (ConcurrentModificationException e){
            //
        }
        mFuncoes.clear();
    }
}
