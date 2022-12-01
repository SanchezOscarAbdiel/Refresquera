package com.example.refresquera;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.an.biometric.BiometricCallback;
import com.an.biometric.BiometricManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BiometricCallback {

    TextView Nivel, Temperatura;
    Button btnC1;
    Switch Suich;

    //CONEXION A BT
    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothSocket btSocket = null;

    //HANDLER
    private Handler mHandler = new Handler();

    //VARIABLES GLOBALES
        public static int x=0,delay, temperatura;
        long lastDown;
        long lastDuration;
        public static String mensaje,subNivel,subTemp;

    //ON CREATE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Nivel = (TextView) findViewById(R.id.tvNivel);
        Temperatura = (TextView) findViewById(R.id.tvTemperatura);
        btnC1 = (Button) findViewById(R.id.btnC1);
        Suich = (Switch) findViewById(R.id.SwC2);

        inst();



        //METODO QUE SE ACTUALIZA CADA 2 SEGUNDOS
        mHandler.postDelayed(RecibeDatos, 1000);

    }

    //==============METODOS==============//

    public void inst(){
        //INSTANCIA EL BLUETOOTH
        System.out.println("btAdapter.getBondedDevices()");
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        System.out.println(btAdapter.getBondedDevices()); //Encuentra las direcciones mac de los dispositivos

        BluetoothDevice hc05 = btAdapter.getRemoteDevice("98:D3:31:F5:B1:8E");
        System.out.println("hc05.getName()");
        System.out.println(hc05.getName());


        //SOCKET PARA LA COMUNICACION
        //BluetoothSocket btSocket = null;

        int cont = 0;
        do {
            try {
                btSocket = hc05.createRfcommSocketToServiceRecord(mUUID);
                System.out.println(btSocket);
                //Conexion a la placa "Servidor"
                btSocket.connect();
                System.out.println("btSocket.isConnected()");
                System.out.println(btSocket.isConnected());
            } catch (IOException e) {
                e.printStackTrace();
            }

            cont++;
        }while (!btSocket.isConnected());

    }




    private Runnable RecibeDatos = new Runnable() {
        @Override
        public void run() {
            //System.out.println("Recibo de datos");
            byte b = 0;
            int B=0;
            try { //Recibir datos

                InputStream inputStream = btSocket.getInputStream();
                inputStream.skip(inputStream.available()); //INSTANCIA COSAS

                byte[] byte_in = new byte[1];


                    try {
                        for (x=0; x<7;x++) {
                            inputStream.read(byte_in);
                            char ch = (char) byte_in[0];
                            mensaje = mensaje + ch;

                        }

                    }catch (IOException e){

                    }
                subNivel=mensaje.substring(0,3);
                subTemp=mensaje.substring(4,6);

                    Nivel.setText(subNivel);

                Temperatura.setText(subTemp);



                System.out.println(subTemp);
                System.out.println("=======");

            } catch (IOException e) {
                System.out.println(e);
            }

            mensaje = "";

            //-------------
            if(delay>2){
                temperatura = Integer.valueOf(subTemp);
            }

            //ACTIVA VENTILADORES
            if(temperatura>23 || Suich.isChecked()){

                try {
                    OutputStream outputStream = btSocket.getOutputStream();
                    outputStream.write(1);
                    System.out.println("49");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }



            btnC1.setOnTouchListener(new View.OnTouchListener() {

                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN) {

                        if(subNivel.equals("MID")){
                            AlertDialog.Builder alerta = new AlertDialog.Builder(MainActivity.this);
                            final EditText input = new EditText(MainActivity.this);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT);
                            input.setLayoutParams(lp);
                            alerta.setView(input);

                            alerta.setMessage("¡CUIDADO!: LLENAR EL GARABE A SU MAXIMA CAPACIDAD REQUIERE DE LA APROBACIÓN DE ALTO MANDO.").setCancelable(false)
                                        .setPositiveButton("APROBAR", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int i) {
                                            //HUELLA DACTILAR
                                            new BiometricManager.BiometricBuilder(MainActivity.this)
                                                    .setTitle("Add a title")
                                                    .setSubtitle("Add a subtitle")
                                                    .setDescription("Add a description")
                                                    .setNegativeButtonText("Add a cancel button")
                                                    .build()
                                                    .authenticate(MainActivity.this);

                                            dialog.cancel();
                                        }
                                    })
                                    .setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int i) {
                                            dialog.cancel();
                                        }
                                    });
                            AlertDialog titulo = alerta.create();
                            titulo.setTitle("JARABE MAXIMO");
                            titulo.show();
                        }else if(!subNivel.equals("MID")){

                            try {
                                OutputStream outputStream = btSocket.getOutputStream();
                                outputStream.write(2);
                                System.out.println("50");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }



                        lastDown = System.currentTimeMillis();
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        lastDuration = System.currentTimeMillis() - lastDown;
                    }

                    return true;
                }
            });

            if(delay<5)
                delay++;
            //-------------
            mHandler.postDelayed(this, 1000); //CICLO RECURSIVO DANDO DELAY.
        }
    };


    @Override
    public void onSdkVersionNotSupported() {

    }

    @Override
    public void onBiometricAuthenticationNotSupported() {

    }

    @Override
    public void onBiometricAuthenticationNotAvailable() {

    }

    @Override
    public void onBiometricAuthenticationPermissionNotGranted() {

    }

    @Override
    public void onBiometricAuthenticationInternalError(String error) {

    }

    @Override
    public void onAuthenticationFailed() {

    }

    @Override
    public void onAuthenticationCancelled() {

    }

    @Override
    public void onAuthenticationSuccessful() {
        try {
            OutputStream outputStream = btSocket.getOutputStream();
            outputStream.write(2);
            System.out.println("50");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {

    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {

    }
}
