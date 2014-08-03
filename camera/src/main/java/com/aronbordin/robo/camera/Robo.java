package com.aronbordin.robo.camera;

import android.util.Log;

import java.util.ArrayList;
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


    /**
     * Construtor do objeto.
     * @param p parent = MainActivity
     */
    public Robo(MainActivity p){
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
            Logar("->Iniciando robô");
        }
    }

    /**
     * Método para parar a execução do robô
     */
    public void PararRobo(){
        if(isRodando) {
            isRodando = false;
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
                if(isRodando)
                    Loop();
                sleep(20);
            }
        } catch (Exception e){
            LogarErro("Erro de execução: " + e.getMessage());
        }
    }

    /**
     * Loop principal do robô
     * Executado a cada 20 ms
     */
    protected void Loop(){
        interacao++;
        if((isSeguindoLinha) && (!isEncruzilhada) && (!isDesviando)){
            seguirLinha();
            chamarFuncao();
        }

    }

    /**
     * Lógica para seguir linha.
     * Irá analisar a camera e tomar as decisões
     */
    private void seguirLinha(){
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
                        try {
                            isEncruzilhada = true;
                            String cmd = "3@19@" + msgPedida + "#";//frente
                            Logar("-->Encruzilhada:  - >" + pedirValor(cmd, msgPedida++));
                            isEncruzilhada = false;
                            mFuncoes.clear();
//                            this.stop();
//                            this.destroy();
                        } catch (Exception e){
//                            Thread.currentThread().interrupt();
                        }
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
                cmd = "3@7#";
                if(!ultimaMsg.equals(cmd)) {
                    Logar("->Virar Esquerda!");
                    mFuncoes.add(cmd);
                    ultimaMsg = cmd;
                }
                break;
            case 4:
            case 14://##
                Logar("->ir Frente!!");
                cmd = "3@4#";
                if(!ultimaMsg.equals(cmd)) {
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
                cmd = "3@5#";
                if(!ultimaMsg.equals(cmd)) {
                    Logar("->Virar Direita!!");
                    mFuncoes.add(cmd);
                    ultimaMsg = cmd;
                }
                break;
            case 28:
//                Logar("->Esquerda 90");
//                mFuncoes.add("3@4#");
//                mFuncoes.add("3@18@750#");
//                mFuncoes.add("3@8#");
//                mFuncoes.add("3@18@500#");
//                ultimaMsg = "3@18@500#";
                isEncruzilhada = true;
                cmd = "3@23@" + msgPedida + "#";//frente
                Logar("-->Encruzilhada:  - >" + pedirValor(cmd, msgPedida++));
                isEncruzilhada = false;
                mFuncoes.clear();

        }

        new Thread() {
            @Override
            public void run() {
                try {
                    if ((interacao % 10) == 0)
                        checarDistancia();
//                    this.stop();
//                    this.destroy();
                } catch (Exception e) {
//                    Thread.currentThread().interrupt();
                }
            }
        }.start();

    }

    /**
     * Irá ler o ultrassom e testar se existe um obstáculo
     */
    private void checarDistancia(){
        int dist = getDistancia();
        if((dist<20) && (dist >= 5)){
            isDesviando = true;

            new Thread(){
                public void run() {
                    try {
                        Desviar();
//                        this.stop();
//                        this.destroy();
                    } catch (Exception e) {
                        //Thread.currentThread().interrupt();
                    }
                }
            }.start();

        }
    }

    /**
     * Helper para pedir ao robô a distancia atual
     * @return
     */
    private int getDistancia(){
        String msg = "3@17@" + String.valueOf(msgPedida) + "#";
        String valor;
        valor = pedirValor(msg, msgPedida++);
        valor = valor.trim();
        return Integer.valueOf(valor);
    }

    /**
     * Helper para desviar de obstáculo
     */
    protected void DesviarNovo(){
        Logar("->Desviando.......");
        mFuncoes.clear();
        mFuncoes.add("3@6#");//direita forte
        mFuncoes.add("3@18@1000#");
        mFuncoes.add("3@4#");//frente
        mFuncoes.add("3@18@2500#");
        mFuncoes.add("3@8#");//esquerda
        mFuncoes.add("3@18@1500#");
        mFuncoes.add("3@4#");//frente
        mFuncoes.add("3@18@1500#");
        mFuncoes.add("3@4#");
        mFuncoes.add("3@18@2500#");
        mFuncoes.add("3@10#");
        chamarFuncao();
    }

    /**
     * Helper para desviar de obstáculo
     */
    public void Desviar(){
        isDesviando = true;
        String cmd = "3@24@" + msgPedida + "#";//frente
        Logar("-->Desviar:  - >" + pedirValor(cmd, msgPedida++));
        isDesviando = false;

//        Logar("->Desviando...");
//        mFuncoes.clear();
//        //girando para a direita e iniciando desvio
//        mFuncoes.add("3@22@180#"); //gira sendor 90º = esquerda
//        mFuncoes.add("3@6#"); //direita forte
//        mFuncoes.add("3@18@1500#"); //delay(1000)
//
//        //girou direita, irá para frente
//        mFuncoes.add("3@4#"); //ir frente
//        chamarFuncao();
//        String foi = pedirValor("3@666@" + msgPedida + "#", msgPedida++);
//        Logar("\t\tPassando obstáculo...");
//        while(getDistancia() < 30){ //frente até passar o obstaculo
//            try {
//                sleep(100);
//            } catch (InterruptedException e){
//                //
//            }
//        }
//        Logar("\t\tVirando...");
//        mFuncoes.clear();
//        mFuncoes.add("3@4#");//andar mais um pouco, por seguranca
//        mFuncoes.add("3@18@2000#");
//        //inicia girar esquerda para passar pelo obstaculo
//        mFuncoes.add("3@8#");//esqueda forte
//        mFuncoes.add("3@18@1500#"); //delay
//        //girou, anda frente até passar obstáculo
//        mFuncoes.add("3@4#");
//        chamarFuncao();
//        foi = pedirValor("3@666@" + msgPedida + "#", msgPedida++);
//        while(getDistancia() > 30)//anda até estar alinhado com o obstaculo
//        {
//            try {
//                sleep(100);
//            } catch (InterruptedException e){
//                //
//            }
//        }
//
//        while(getDistancia() < 30){//já encontrou, agr qdo for maior q 30 ele já ultrapassou o obs
//            try{
//                sleep(100);
//            } catch (InterruptedException e){
//                //
//            }
//        }
//
//        mFuncoes.clear();
//        mFuncoes.add("3@4#");//ir mais para frente
//        mFuncoes.add("3@18@1500#"); //delay
//
//        //virar esquerda pra voltar linha
//        mFuncoes.add("3@8#");
//        mFuncoes.add("3@18@1000#");//delay
//        mFuncoes.add("3@4#"); //anda frente
//        chamarFuncao();
//        foi = pedirValor("3@666@" + msgPedida + "#", msgPedida++);
//        while (mCameraPreview.isPreto(2) == 0){//anda reto até achar a linha
//            try{
//                sleep(50);
//            } catch(InterruptedException e){
//                //
//            }
//        }
//        //vira direita para voltar na pista
//        mFuncoes.add("3@6#");//direita forte
//        mFuncoes.add("3@18@750#");
//        mFuncoes.add("3@22@90#");
//        chamarFuncao();
//        foi = pedirValor("3@666@" + msgPedida + "#", msgPedida++);
//        isDesviando = false;
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

    /**
     * Irá enviar todas as funções na fila por bluetooth.
     * Após enviar as mensagems, irá limpar a fila
     */
    private void chamarFuncao(){
        Iterator i = mFuncoes.iterator();
        while(i.hasNext()){
            String f = (String)i.next();
            mBluetoothRobo.enviarMsg(f);
        }
        mFuncoes.clear();
    }
}
