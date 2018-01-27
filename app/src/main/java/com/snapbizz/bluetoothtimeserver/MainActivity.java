package com.snapbizz.bluetoothtimeserver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static android.provider.Settings.NameValueTable.NAME;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter = null;
    private int REQUEST_ENABLE_BT = 1;
    BluetoothDevice device = null;
    private UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private long currentTimeMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setBluetoothAdapter();
        Thread t = new Thread() {
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                displayTime();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        t.start();
        new AcceptThread().start();
    }

    private void setBluetoothAdapter() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            enableBluetoothAdapter();
        }
    }

    private void enableBluetoothAdapter() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        getPairedDevice();
    }

    private void getPairedDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) device = (BluetoothDevice) pairedDevices.toArray()[0];
    }

    private void displayTime() {
        TextView textView = ((TextView) findViewById(R.id.textViewMain));
        currentTimeMillis = System.currentTimeMillis();
        Date date = new Date(currentTimeMillis);
        textView.setText(date.toString());
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, DEFAULT_UUID);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                if (socket != null) {
                    new ProcessClientRequestThread(socket).start();
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ProcessClientRequestThread extends Thread {
        private BluetoothSocket socket = null;
        private OutputStream outputStream = null;

        ProcessClientRequestThread(BluetoothSocket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                outputStream = socket.getOutputStream();
                outputStream.write(getCurrentTime());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private byte[] getCurrentTime() {
            Date date = new Date((System.currentTimeMillis()));
            return date.toString().getBytes();
        }
    }
}
