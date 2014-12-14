package com.github.ethanwu10.bluetoothutil;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothSppClient {

    private static final String TAG = "BluetoothSppClient";

    public static final int MESSAGE_CONNECTION_FAILED = 2;

    public static final int MESSAGE_STATE_CHANGE = 1;

    public static final int STATE_NONE = 0, STATE_CONNECTING = 1, STATE_CONNECTED = 2;

    private Handler mHandler;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket;
    private int mBluetoothConnectionState;
    private UUID mUUID;

    private ConnectThread mConnectThread;

    public BluetoothSppClient(BluetoothDevice bluetoothDevice, Handler handler) {
        this(bluetoothDevice, handler, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
    }

    public BluetoothSppClient(BluetoothDevice bluetoothDevice, Handler handler, UUID uuid) {
        mHandler = handler;
        mBluetoothDevice = bluetoothDevice;
        mUUID = uuid;
    }

    public synchronized void connect() {
        if (mBluetoothConnectionState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        mConnectThread = new ConnectThread(mBluetoothDevice, mUUID);
        setState(STATE_CONNECTING);
        mConnectThread.start();

    }

    public synchronized void close() {
        try {
            if (mBluetoothSocket != null) {
                mBluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel_connection() {
        if (mConnectThread != null) {
            mConnectThread.interrupt();
            mConnectThread.cancel();
            mConnectThread = null;
            setState(STATE_NONE);
        }
    }

    public void write(byte[] buffer) {
        WriteThread writeThread = new WriteThread(mBluetoothSocket, buffer);
        writeThread.start();
    }

    public void write(byte buffer) {
        byte[] buf = new byte[1];
        buf[0] = buffer;
        write(buf);
    }

    public int getState() {
        return mBluetoothConnectionState;
    }


    private synchronized void setState(int bluetoothConnectionState) {
        mBluetoothConnectionState = bluetoothConnectionState;

        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, mBluetoothConnectionState, -1).sendToTarget();

        switch (mBluetoothConnectionState) {
            case STATE_NONE:
                Log.d(TAG, "State now NONE");
                break;
            case STATE_CONNECTED:
                Log.d(TAG, "State now CONNECTED");
                break;
            case STATE_CONNECTING:
                Log.d(TAG, "State now CONNECTING");
                break;
        }
    }

    private synchronized void connectionFailed() {
        setState(STATE_NONE);

        mHandler.obtainMessage(MESSAGE_CONNECTION_FAILED).sendToTarget();

        Log.w(TAG, "Connection FAILED");
    }

    private synchronized void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        if (mConnectThread != null) {
            //mConnectThread.cancel();
            mConnectThread = null;
        }

        mBluetoothSocket = mmSocket;

        setState(STATE_CONNECTED);

        Log.i(TAG, "Connected to " + mmDevice.getName());
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID mmUUID;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            mmDevice = device;
            mmUUID = uuid;
        }

        public void run() {
            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(mmUUID);
                mmSocket.connect();
            } catch (Exception e_connect) {
                e_connect.printStackTrace();
                connectionFailed();
                try {
                    mmSocket.close();
                } catch (Exception e_close) {
                    e_close.printStackTrace();
                }
                return;
            }
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ReadThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream  mmInStream;

        public ReadThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            try {
                tmpIn  = mmSocket.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
        }

        public void run() {
            //TODO: Read data and store in queue
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class WriteThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        private final byte[] mData;

        public WriteThread(BluetoothSocket socket, byte[] data) {
            mmSocket = socket;
            mData = data;
            OutputStream tmpOut = null;

            try {
                tmpOut = socket.getOutputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mmOutStream = tmpOut;
        }

        public void run() {
            try {
                mmOutStream.write(mData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
