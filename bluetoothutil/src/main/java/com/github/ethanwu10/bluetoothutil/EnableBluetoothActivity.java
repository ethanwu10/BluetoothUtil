package com.github.ethanwu10.bluetoothutil;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class EnableBluetoothActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    public static final String KEY_DIALOG_STYLE = "EnableBluetoothActivity::KEY_DIALOG_STYLE";
    public static final String KEY_DIALOG_ENABLE_FAILED_TITLE = "EnableBluetoothActivity::KEY_DIALOG_ENABLE_FAILED_TITLE";
    public static final String KEY_DIALOG_ENABLE_FAILED_MESSAGE = "EnableBluetoothActivity::KEY_DIALOG_ENABLE_FAILED_MESSAGE";
    public static final String KEY_DIALOG_ENABLE_FAILED_POSITIVE = "EnableBluetoothActivity::KEY_DIALOG_ENABLE_FAILED_POSITIVE";
    public static final String KEY_DIALOG_ENABLE_FAILED_NEGATIVE = "EnableBluetoothActivity::KEY_DIALOG_ENABLE_FAILED_NEGATIVE";
    public static final String KEY_DIALOG_NO_BLUETOOTH_TITLE = "EnableBluetoothActivity::KEY_DIALOG_NO_BLUETOOTH_TITLE";
    public static final String KEY_DIALOG_NO_BLUETOOTH_MESSAGE = "EnableBluetoothActivity::KEY_DIALOG_NO_BLUETOOTH_MESSAGE";
    public static final String KEY_DIALOG_NO_BLUETOOTH_NEGATIVE = "EnableBluetoothActivity::KEY_DIALOG_NO_BLUETOOTH_NEGATIVE";
    public static final String KEY_FAILURE_REASON = "EnableBluetoothActivity::KEY_FAILURE_REASON";
    public static final int REASON_FAILURE_NO_BLUETOOTH = 0;
    public static final int REASON_FAILURE_ENABLE_FAILED = 1;

    private class DialogEnableFailedText {
        public CharSequence Title;
        public CharSequence Message;
        public CharSequence Positive;
        public CharSequence Negative;
    } private DialogEnableFailedText dialogEnableFailedText = new DialogEnableFailedText();
    private class DialogNoBluetoothText {
        public CharSequence Title;
        public CharSequence Message;
        public CharSequence Negative;
    } private DialogNoBluetoothText dialogNoBluetoothText = new DialogNoBluetoothText();

    protected BluetoothAdapter mBluetoothAdapter;
    protected int DialogTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enable_bluetooth);

        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        DialogTheme = bundle.getInt(KEY_DIALOG_STYLE, android.R.style.Theme_Holo_Dialog);
        dialogEnableFailedText.Title    = bundle.getCharSequence(KEY_DIALOG_ENABLE_FAILED_TITLE,    getResources().getString(R.string.dialog_bluetoothNotEnabled_title));
        dialogEnableFailedText.Message  = bundle.getCharSequence(KEY_DIALOG_ENABLE_FAILED_MESSAGE,  getResources().getString(R.string.dialog_bluetoothNotEnabled_message));
        dialogEnableFailedText.Positive = bundle.getCharSequence(KEY_DIALOG_ENABLE_FAILED_POSITIVE, getResources().getString(R.string.retry));
        dialogEnableFailedText.Negative = bundle.getCharSequence(KEY_DIALOG_ENABLE_FAILED_NEGATIVE, getResources().getString(R.string.cancel));
        dialogNoBluetoothText.Title     = bundle.getCharSequence(KEY_DIALOG_NO_BLUETOOTH_TITLE,     getResources().getString(R.string.dialog_noBluetooth_title));
        dialogNoBluetoothText.Message   = bundle.getCharSequence(KEY_DIALOG_NO_BLUETOOTH_MESSAGE,   getResources().getString(R.string.dialog_noBluetooth_message));
        dialogNoBluetoothText.Negative  = bundle.getCharSequence(KEY_DIALOG_NO_BLUETOOTH_NEGATIVE,  getResources().getString(R.string.cancel));


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // no Bluetooth adapter present

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, DialogTheme);
            dialogBuilder.setMessage(dialogNoBluetoothText.Message);
            dialogBuilder.setTitle(dialogNoBluetoothText.Title);
            dialogBuilder.setPositiveButton(dialogEnableFailedText.Negative, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent();
                    intent.putExtra(KEY_FAILURE_REASON, REASON_FAILURE_NO_BLUETOOTH);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                }
            });
            dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
            dialogBuilder.create().show();
        }
        else {
            if (!mBluetoothAdapter.isEnabled()) {
                requestEnableBt();
            }
            else {
                setResult(RESULT_OK);
                finish();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode != RESULT_OK) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, DialogTheme);
                    dialogBuilder.setMessage(dialogEnableFailedText.Message);
                    dialogBuilder.setTitle(dialogEnableFailedText.Title);
                    dialogBuilder.setNegativeButton(dialogEnableFailedText.Negative, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent();
                            intent.putExtra(KEY_FAILURE_REASON, REASON_FAILURE_ENABLE_FAILED);
                            setResult(RESULT_CANCELED, intent);
                            finish();
                        }
                    });
                    dialogBuilder.setPositiveButton(dialogEnableFailedText.Positive, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            requestEnableBt();
                        }
                    });
                    dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
                    dialogBuilder.create().show();
                }
                else {
                    setResult(RESULT_OK);
                    finish();
                }
                break;
        }
    }


    protected void requestEnableBt() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

}
