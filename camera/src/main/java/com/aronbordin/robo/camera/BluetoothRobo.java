package com.aronbordin.robo.camera;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by neo on 08/06/14.
 * @author Aron Bordin <aron.bordin@gmail.com>
 * Classe para gerenciar conexão bluetooth.
 */
public class BluetoothRobo extends Thread {
    private BluetoothAdapter mBlueAdapter = null;
    private BluetoothSocket mBlueSocket = null;
    private BluetoothDevice mBlueRobo = null;
    OutputStream mOut;
    InputStream mIn;
    private MainActivity parent;
    private boolean encontrouRobo = false;
    private boolean conectado = false;
    private int REQUEST_BLUE_ATIVAR = 10;
    private String roboNome;
    private List<String> mMensgens = new ArrayList<String>();

    /**
     * Construtor público. Irá testar os dispositivos pareados para fazer a conexão.
     * Caso encontre algum problema, irá mostrar as informações no log
     * @param p parent - Classe principal da aplicação, a MainActiviry
     * @param Nome Nome do robô para realizar a conexão
     */
    BluetoothRobo(MainActivity p, String Nome){
        try {
            for(int i = 0; i < 2048; i++){
                mMensgens.add("");
            }
            p.mLogger.Logar("\t\tCriando bluetooth...");
            parent = p;
            roboNome = Nome;
            mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBlueAdapter == null) {
                p.mLogger.LogarErro("\t\t\tCelular não suporta bluetooth!!");
                return;
            }
            if (!mBlueAdapter.isEnabled()) {
                Intent ativarBlue = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                parent.startActivityForResult(ativarBlue, REQUEST_BLUE_ATIVAR);
                if (REQUEST_BLUE_ATIVAR == p.RESULT_CANCELED) {
                    p.mLogger.LogarErro("\t\t\tBluetooth não foi ativado!!");
                }
            }

            Set<BluetoothDevice> pareados = mBlueAdapter.getBondedDevices();
            if (pareados.size() > 0) {
                for (BluetoothDevice d : pareados) {
                    if (d.getName().equals(roboNome)) {
                        mBlueRobo = d;
                        encontrouRobo = true;
                        break;
                    }
                }
            }



            if (!encontrouRobo)
                p.mLogger.LogarErro("\t\tNenhum robô paredo!!");
            else
                p.mLogger.Logar("\t\tOk!! Criado e pareado!");
        }catch (Exception e){
            p.mLogger.LogarErro("\t\tErro ao criar Bluetooth! : " + e.getMessage());
        }

    }
    public boolean conectado ()
    {
        if(mBlueSocket.isConnected())
        {
            return true;
        }
        else
            return false;
    }

    /**
     * Construtor público. Irá testar os dispositivos pareados para fazer a conexão.
     * Caso encontre algum problema, irá mostrar as informações no log.
     * Irá usar o nome Robo1-TCC como padrão para a conexão
     * @param p parent - Classe principal da aplicação, a MainActiviry
     */
    BluetoothRobo(MainActivity p){
        this(p, "Robo1-TCC");
    }

    /**
     * Realiza a conexão com o robo via bluetooth. Caso tenha algum problema, informa no log.
     */
    public void Conectar(){
        if(!encontrouRobo)
            return;
        try{
            parent.mLogger.Logar("\t\tConectando ao robô...");

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            mBlueSocket = mBlueRobo.createRfcommSocketToServiceRecord(uuid);
            mBlueSocket.connect();
            mOut = mBlueSocket.getOutputStream();
            mIn = mBlueSocket.getInputStream();
            conectado = true;
            this.start();
            parent.mLogger.Logar("\t\t\t" + mBlueAdapter.getName());
            parent.mLogger.Logar("\t\tOk!!");

        }catch (Exception e){
            parent.mLogger.LogarErro("\t\tErro ao conectar: " + e.getMessage());
        }
    }


    /**
     * Inicia o Thread para ficar checando se novas mensagens chegaram.
     * Não use essa função!! Ela será usada internamente pela classe,
     * caso seja utilizada em outro local, não irá funcionar
     */
    public void run(){

        while (true) {
            if(conectado) {
                try {
                    byte ch, buffer[] = new byte[1024];
                    int i = 0;

                    String s = "";
                    while((ch=(byte)mIn.read()) != '#'){
                        buffer[i++] = ch;
                    }
                    buffer[i] = '\0';

                    final String msg = new String(buffer);

                    recebeuMsg(msg.trim());
                    parent.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            parent.mLogger.Logar("[Blue]:" + msg);



                        }
                    });

                } catch (IOException e) {
                    parent.mLogger.LogarErro("->Erro ao receber mensagem: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Callback do Thread, informando que recebeu uma nova mensagem
     * @param msg Mensagem recebida
     */
    private void recebeuMsg(String msg){
        try {

            int id = Integer.valueOf(msg.split("@")[0]);
            mMensgens.set(id, msg);

            try {
                this.notify();
            }catch (IllegalMonitorStateException e){
                //
            }
        } catch (Exception e){
            LogarErro("-> Erro ao receber msg: " + e.getMessage());
        }
    }

    /**
     * Testa se a mensage foi recebida de acordo com o ID do pedido
     * @param i ID da Mensagem
     * @return boolean, indicando se a mensagem foi recebida
     */
    public boolean hasMensagem(int i){
        try{
            String s = mMensgens.get(i);
            if(s.length() > 0)
                return true;
            else
                return false;
        } catch (Exception e){
            return false;
        }
    }

    /**
     * Retorna uma mensagem por id. Caso a mensagem não existe, será retornada uma string vazia
     * @param i ID da Mensagem
     * @return
     */
    public String getMensagem(int i){
        return mMensgens.get(i);
    }

    /**
     * Método para enviar mensagem por bluetooth
     * @param msg Mensagem a ser enviada
     */
    public void enviarMsg(String msg){
        try {
            if(conectado) {
                try {


                    mOut.write(msg.getBytes());
                }
                catch (NullPointerException e)
                {
                    LogarErro("Erro -> Menssagem nula");
                }
            //    Logar("[Blue-me]Msg enviada: " + msg);
            }

        } catch (IOException e){
            LogarErro("->Erro ao enviar mensagem: " + e.getMessage());
        }
    }


    private void Logar(String msg){
        final String log = msg;
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.mLogger.Logar(log);
            }
        });
    }

    private void LogarErro(String msg){
        final String log = msg;
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parent.mLogger.LogarErro(log);
            }
        });
    }
}
