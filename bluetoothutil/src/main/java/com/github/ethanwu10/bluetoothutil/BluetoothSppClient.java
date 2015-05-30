package com.github.ethanwu10.bluetoothutil;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

public class BluetoothSppClient {

    private static final String TAG = "BluetoothSppClient";

    public static final int MESSAGE_CONNECTION_FAILED = 2;

    public static final int MESSAGE_STATE_CHANGE = 1;

    public static final int STATE_NONE = 0, STATE_CONNECTING = 1, STATE_CONNECTED = 2;

    public static final int QUEUE_MAX_SIZE = 100;

    private Handler mHandler;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket;
    private int mBluetoothConnectionState;
    private UUID mUUID;

    private ConnectThread mConnectThread;

    private ReadThread mReadThread;
    private WriteThread mWriteThread;

    public BluetoothSppClient(BluetoothDevice bluetoothDevice, Handler handler) {
        this(bluetoothDevice, handler, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
    }

    public BluetoothSppClient(BluetoothDevice bluetoothDevice, Handler handler, UUID uuid) {
        mHandler = handler;
        mBluetoothDevice = bluetoothDevice;
        mUUID = uuid;
    }

    public synchronized void connect() {
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
            if (mReadThread != null) {
                mReadThread.interrupt();
                mReadThread = null;
            }
            if (mWriteThread != null) {
                mWriteThread.close();
                mWriteThread = null;
            }
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
        for (byte b : buffer) {
            write(b);
        }
    }

    public void write(byte buffer) {
        mWriteThread.write(buffer);
    }

    public byte[] read(int len) throws IOException, IllegalStateException {
        if (getState() != STATE_CONNECTED) {
            throw new IllegalStateException("BluetoothSppClient::read: attempt to read without connection open");
        }
        if (mReadThread.getNextByte() < len) {
            throw new IOException("BluetoothSppClient::read: attempt to get more elements than available");
        }
        byte[] tmp = new byte[len];
        for (int i = 0; i < len; i++) {
            tmp[i] = mReadThread.getNextByte();
        }
        return tmp;
    }

    public byte read() throws IOException, IllegalStateException {
        return read(1)[0];
    }

    public int getReadBufferSize() {
        return mReadThread.getBufferSize();
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

        mReadThread = new ReadThread(mBluetoothSocket);
        mReadThread.start();
        mWriteThread = new WriteThread(mBluetoothSocket);

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

                    if (BluetoothSppClient.this.getState() != STATE_NONE) { //cause error state on close
                        setState(STATE_NONE);
                    }
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

                if (BluetoothSppClient.this.getState() != STATE_NONE) { //cause error state on close
                    setState(STATE_NONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ReadThread extends Thread {
        public static final int BUFFER_SIZE = 1024;
        private final BluetoothSocket mmSocket;
        private final InputStream  mmInStream;

        private Queue<Byte> queued_bytes;

        public ReadThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            try {
                tmpIn  = mmSocket.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            queued_bytes = new LinkedList<>();
        }

        public void run() {
            byte[] buf = new byte[BUFFER_SIZE];
            int bytes_read;

            while (true) {
                try {
                    bytes_read = mmInStream.read(buf, 0, BUFFER_SIZE);

                    for (int i = 0; i < bytes_read; ++i) {
                        queued_bytes.add(buf[i]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (e.getMessage().equals("socket closed")) { //if socket closed, quit
                        break;
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public byte getNextByte() throws IOException {
            if (!queued_bytes.isEmpty()) {
                Byte element = queued_bytes.element();
                queued_bytes.remove();
                return element;
            }
            else {
                throw new IOException("BluetoothSppClient::ReadThread::getNextByte: attempt to get element when queue empty");
            }
        }

        public int getBufferSize() {
            return queued_bytes.size();
        }

        public void clearBuffer() {
            queued_bytes.clear();
        }
    }

    private class WriteThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        private Queue<Byte> toSend;
        private boolean isRunning = true;

        public WriteThread(BluetoothSocket socket) {
            mmSocket = socket;
            OutputStream tmpOut = null;
            toSend = new ConcurrentLinkedQueue<>();

            try {
                tmpOut = socket.getOutputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mmOutStream = tmpOut;
            this.start();
        }

        public void run() {
            LockSupport.park();
            while (isRunning) {
                if (!toSend.isEmpty()) {
                    try {
                        mmOutStream.write(toSend.remove());
                        if (toSend.isEmpty()) {
                            LockSupport.park();
                        }
/*
                        else if (toSend.size() > QUEUE_MAX_SIZE) {
                            Log.w(TAG, "queue overloaded (" + toSend.size() + " elements); clearing...");
                            toSend.clear();
                        }
*/
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public synchronized void write(byte data) {
            toSend.add(data);
            LockSupport.unpark(this);
        }

        public void close() {
            isRunning = false;
            LockSupport.unpark(this);
        }
    }

}
