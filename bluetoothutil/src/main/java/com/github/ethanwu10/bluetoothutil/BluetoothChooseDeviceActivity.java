package com.github.ethanwu10.bluetoothutil;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class BluetoothChooseDeviceActivity extends Activity {

    public static final String TAG = "ChooseDevice";

    public static final String EXTRA_DEVICE_ADDRESS = "com.github.ethanwu10.bluetoothutil.EXTRA_DEVICE_ADDRESS";
    public static final String EXTRA_HAS_CLASS_FILTER = "com.github.ethanwu10.bluetoothutil.EXTRA_HAS_CLASS_FILTER";
    public static final String EXTRA_CLASS_FILTER = "com.github.ethanwu10.bluetoothutil.EXTRA_CLASS_FILTER";

    public static final int REQUEST_DEVICE   = 153;

    private BluetoothAdapter mBluetoothAdapter;

    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    private boolean hasClassFilter;
    private int classFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_device);

        Intent intent = getIntent();
        hasClassFilter = intent.getBooleanExtra(EXTRA_HAS_CLASS_FILTER, false);
        if (hasClassFilter) {
            if (!intent.hasExtra(EXTRA_CLASS_FILTER)) {
                throw new IllegalArgumentException("bluetoothutil::ChooseDevice: no class filter provided");
            } else {
                classFilter = intent.getIntExtra(EXTRA_CLASS_FILTER, 0);
            }
        }

        ListView listView_pairedDevices = ((ListView) findViewById(R.id.listView_pairedDevices));
        ListView listView_newDevices    = ((ListView) findViewById(R.id.listView_newDevices));
        //listView_newDevices.addFooterView(findViewById(R.id.newDevices_footer_progress_bar));
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.bt_device_name);
        mNewDevicesArrayAdapter    = new ArrayAdapter<String>(this, R.layout.bt_device_name);
        listView_pairedDevices.setAdapter(mPairedDevicesArrayAdapter);
        listView_newDevices.setAdapter(mNewDevicesArrayAdapter);
        TextView textView_pairedDevices_noDevices = ((TextView) findViewById(R.id.textView_pairedDevices_noDevices));
        listView_pairedDevices.setOnItemClickListener(mOnDeviceClickListener);
        listView_newDevices.setOnItemClickListener(mOnDeviceClickListener);
        listView_pairedDevices.setOnItemLongClickListener(mOnPairedDeviceLongClickListener);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean hasPairedDevices = false;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if ((device.getBluetoothClass() != null) && isCorrectClass(device)) {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    hasPairedDevices = true;
                }
            }
        }
        if (hasPairedDevices) {
            listView_pairedDevices.setVisibility(View.VISIBLE);
            textView_pairedDevices_noDevices.setVisibility(View.GONE);
        }
        else {
            listView_pairedDevices.setVisibility(View.INVISIBLE);
            textView_pairedDevices_noDevices.setVisibility(View.VISIBLE);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
    }


    private String currentlyRemovingDeviceAddress = null;

    private AdapterView.OnItemClickListener mOnDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            if (currentlyRemovingDeviceAddress == null) {
                mBluetoothAdapter.cancelDiscovery();

                String deviceInfo = ((TextView) v).getText().toString();
                String deviceAddress = deviceInfo.substring(deviceInfo.length() - 17);

                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress);
                setResult(Activity.RESULT_OK, intent);

                finish();
            }
        }
    };

    private AdapterView.OnItemLongClickListener mOnPairedDeviceLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

            String deviceInfo = ((TextView) view).getText().toString();
            String deviceName = deviceInfo.substring(0, deviceInfo.length() - 18);
            String deviceAddress = deviceInfo.substring(deviceInfo.length() - 17);
            currentlyRemovingDeviceAddress = deviceAddress;

            AlertDialog.Builder builder = new AlertDialog.Builder(BluetoothChooseDeviceActivity.this);
            builder.setTitle("Unpair Device?");
            builder.setMessage("Unpair device " + deviceName + "?");
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        BluetoothDevice.class.getMethod("removeBond", (Class[]) null).invoke(mBluetoothAdapter.getRemoteDevice(currentlyRemovingDeviceAddress));
                        currentlyRemovingDeviceAddress = null;
                        boolean hasPairedDevices = false;
                        Thread.sleep(25);
                        mPairedDevicesArrayAdapter.clear();
                        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                        if (pairedDevices.size() > 0) {
                            for (BluetoothDevice device : pairedDevices) {
                                if ((device.getBluetoothClass() != null) && isCorrectClass(device)) {
                                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                                    hasPairedDevices = true;
                                }
                            }
                        }
                        if (hasPairedDevices) {
                            ((ListView) findViewById(R.id.listView_pairedDevices)).setVisibility(View.VISIBLE);
                            ((TextView) findViewById(R.id.textView_pairedDevices_noDevices)).setVisibility(View.GONE);
                        }
                        else {
                            ((ListView) findViewById(R.id.listView_pairedDevices)).setVisibility(View.INVISIBLE);
                            ((TextView) findViewById(R.id.textView_pairedDevices_noDevices)).setVisibility(View.VISIBLE);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            builder.create().show();

            return false;
        }
    };

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_choose_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }


    private Map<String, Integer> discoveredDevices = new TreeMap<String, Integer>();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if ((device.getBondState() != BluetoothDevice.BOND_BONDED) && (device.getBluetoothClass() != null) && isCorrectClass(device)) {
                    if (device.getName() == null) {
                        Log.d(TAG, "device name is null");
                    }
                    if (!discoveredDevices.containsKey(device.getAddress())) {
                        String tmpItem;
                        if (device.getName() != null) {
                            tmpItem = device.getName() + "\n" + device.getAddress();
                        } else {
                            tmpItem = "\n" + device.getAddress();
                        }
                        mNewDevicesArrayAdapter.add(tmpItem);
                        discoveredDevices.put(device.getAddress(), mNewDevicesArrayAdapter.getPosition(tmpItem));
                    }
                    else {
                        mNewDevicesArrayAdapter.remove(mNewDevicesArrayAdapter.getItem(discoveredDevices.get(device.getAddress())));
                        mNewDevicesArrayAdapter.insert(device.getName() + "\n" + device.getAddress(), discoveredDevices.get(device.getAddress()));
                    }
                   ((TextView) findViewById(R.id.textView_newDevices_noDevices)).setVisibility(View.GONE);
                   ((ListView) findViewById(R.id.listView_newDevices)).setVisibility(View.VISIBLE);
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                //findViewById(R.id.newDevices_footer_progress_bar).setVisibility(View.GONE);
                findViewById(R.id.linearLayout_scanning).setVisibility(View.GONE);
                findViewById(R.id.button_scan).setVisibility(View.VISIBLE);
            }
        }
    };


    public void onClick_button_scan(View view) {
        if (mBluetoothAdapter.startDiscovery()) {
            findViewById(R.id.linearLayout_new_devices).setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.text_discoveryStarted, Toast.LENGTH_SHORT).show();
            //findViewById(R.id.newDevices_footer_progress_bar).setVisibility(View.VISIBLE);
            view.setVisibility(View.GONE);
            findViewById(R.id.linearLayout_scanning).setVisibility(View.VISIBLE);
        }
        else {
            Log.e(TAG, "discovery failed to start");
            Toast.makeText(this, R.string.text_discoveryFailed, Toast.LENGTH_SHORT).show();
        }
    }

    public void onClick_cancel_scanning(View view) {
        if (mBluetoothAdapter.cancelDiscovery()) {
            Toast.makeText(this, R.string.text_discoveryCanceled, Toast.LENGTH_SHORT).show();
            findViewById(R.id.linearLayout_scanning).setVisibility(View.GONE);
            findViewById(R.id.button_scan).setVisibility(View.VISIBLE);
        }
        else {
            Log.e(TAG, "discovery failed to cancel");
        }
    }


    protected boolean isCorrectClass(BluetoothDevice device) {
        if (hasClassFilter) {
            return device.getBluetoothClass().getDeviceClass() == classFilter;
        } else {
            return true;
        }
    }
}
