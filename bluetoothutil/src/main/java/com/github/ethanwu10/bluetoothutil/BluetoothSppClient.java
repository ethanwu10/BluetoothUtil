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

/**
 * A client for Bluetooth SPP communication
 * 
 * @author Ethan 
 */
public class BluetoothSppClient {

    private static final String TAG = "BluetoothSppClient";

    /**
     * Message constant - connection to device failed
     */
    public static final int MESSAGE_CONNECTION_FAILED = 2;

    /**
     * Message constant - connection state changed
     *
     * @see #STATE_NONE
     * @see #STATE_CONNECTING
     * @see #STATE_CONNECTED
     */
    public static final int MESSAGE_STATE_CHANGE = 1;

    /**
     * State constant - not connected / idle
     *
     * @see #STATE_CONNECTING
     * @see #STATE_CONNECTED
     */
    public static final int STATE_NONE = 0;

    /**
     * State constant - currently connecting to a host
     *
     * @see #STATE_NONE
     * @see #STATE_CONNECTED
     */
    public static final int STATE_CONNECTING = 1;

    /**
     * State constant - currently connected to a host
     *
     * @see #STATE_NONE
     * @see #STATE_CONNECTING
     */
    public static final int STATE_CONNECTED = 2;

    private static final int QUEUE_MAX_SIZE = 100;

    private Handler mHandler;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket;
    private int mBluetoothConnectionState;
    private UUID mUUID;

    private ConnectThread mConnectThread;

    private ReadThread mReadThread;
    private WriteThread mWriteThread;

    /*
     * Constructs an SPP client using the default bluetooth SPP UUID<br>(<code>00001101-0000-1000-8000-00805F9B34FB</code>)
     * @param bluetoothDevice device to connect to
     * @param handler event handler - receives {@link #MESSAGE_CONNECTION_FAILED}
     *                and {@link #MESSAGE_STATE_CHANGE}
     * 
     * @see BluetoothSppClient#BluetoothSppClient(BluetoothDevice, Handler, UUID)
     */
    /**
     * Constructs an SPP client using the default bluetooth SPP UUID<br>(<code>00001101-0000-1000-8000-00805F9B34FB</code>)
     * @param bluetoothDevice device to connect to
     * @param handler event handler - receives {@link #MESSAGE_CONNECTION_FAILED}
     *                and {@link #MESSAGE_STATE_CHANGE}
     */
    public BluetoothSppClient(BluetoothDevice bluetoothDevice, Handler handler) {
        this(bluetoothDevice, handler, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
    }

    /*
     * Constructs an SPP client using a custom UUID
     *
     * @param bluetoothDevice device device to connect to
     * @param handler event handler - receives {@link #MESSAGE_CONNECTION_FAILED}
     *                and {@link #MESSAGE_STATE_CHANGE}
     * @param uuid UUID to connect to
     * 
     * @see BluetoothSppClient#BluetoothSppClient(BluetoothDevice, Handler)
     */
    /**
     * Constructs an SPP client using a custom UUID
     *
     * @param bluetoothDevice device device to connect to
     * @param handler event handler - receives {@link #MESSAGE_CONNECTION_FAILED}
     *                and {@link #MESSAGE_STATE_CHANGE}
     * @param uuid UUID to connect to
     */
    public BluetoothSppClient(BluetoothDevice bluetoothDevice, Handler handler, UUID uuid) {
        mHandler = handler;
        mBluetoothDevice = bluetoothDevice;
        mUUID = uuid;
    }

    /**
     * Initiates a connection
     * <br><br>
     * Also updates state to {@link #STATE_CONNECTING}
     */
    public synchronized void connect() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        mConnectThread = new ConnectThread(mBluetoothDevice, mUUID);
        setState(STATE_CONNECTING);
        mConnectThread.start();

    }

    /**
     * Closes the connection
     * <br><br>
     * Also updates state to {@link #STATE_NONE}
     */
    public synchronized void close() {
        try {
            if (mReadThread != null) {
                mReadThread.cancel();
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
        setState(STATE_NONE);
    }

    /**
     * Cancels an active BT connection attempt <br>
     * If no BT connection attempt is active (state is not
     * {@link #STATE_CONNECTING}, and so not {@link #STATE_CONNECTED}
     * or {@link #STATE_NONE}) it does nothing
     */
    public void cancel_connection() {
        if (mConnectThread != null) {
            mConnectThread.interrupt();
            mConnectThread.cancel();
            mConnectThread = null;
            setState(STATE_NONE);
        }
    }

    /**
     * Writes data through the SPP connection
     * 
     * @param buffer byte array of data to send
     * 
     * @see #write(byte)
     */
    public void write(byte[] buffer) {
        for (byte b : buffer) {
            write(b);
        }
    }

    /**
     * Writes a single byte through the SPP connection
     * 
     * @param buffer byte to send
     * 
     * @see #write(byte[])
     */
    public void write(byte buffer) {
        mWriteThread.write(buffer);
    }

    /**
     * Reads bytes from buffered received data
     *
     *
     * @param len number of bytes to read
     * 
     * @return byte[] - the data read
     * 
     * @throws IOException when no connection open
     * @throws IllegalStateException when <code>len</code> is longer than the number
     *         of bytes available to read
     * 
     * @see #read()
     */
    public byte[] read(int len) throws IOException, IllegalStateException {
        if (getState() != STATE_CONNECTED) {
            throw new IllegalStateException("BluetoothSppClient::read: attempt to read without connection open");
        }
        if (len > getReadBufferSize()) {
            throw new IOException("BluetoothSppClient::read: attempt to get more elements than available");
        }
        byte[] tmp = new byte[len];
        for (int i = 0; i < len; i++) {
            tmp[i] = mReadThread.getNextByte();
        }
        return tmp;
    }

    /**
     * Reads one byte from buffered received data
     *
     * @return byte - the byte read
     * 
     * @throws IOException when no connection open
     * @throws IllegalStateException when no bytes are available to be read
     * 
     * @see #read(int)
     */
    public byte read() throws IOException, IllegalStateException {
        return read(1)[0];
    }

    /**
     * Gets the number of bytes of data available in the received data buffer
     * 
     * @return int - number of bytes in buffer
     */
    public int getReadBufferSize() {
        return mReadThread.getBufferSize();
    }

    /**
     * Gets SPP client state
     * 
     * @return int - enumeration of current state
     * 
     * @see #STATE_NONE
     * @see #STATE_CONNECTING
     * @see #STATE_CONNECTED
     */
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
        boolean isRunning = true;

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
            int bytes_read = 0;

            while (isRunning) {
                try {
                    bytes_read = mmInStream.read(buf, 0, BUFFER_SIZE);

                    for (int i = 0; i < bytes_read; ++i) {
                        queued_bytes.add(buf[i]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (bytes_read == -1) { //if socket closed, quit
                    break;
                }
            }
        }

        public void cancel() {
            isRunning = false;
        }

        public synchronized byte getNextByte() throws IOException {
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
