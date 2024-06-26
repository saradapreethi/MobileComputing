package de.uni_stuttgart.ipvs.mc.task1;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity
{
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private boolean b;
    private Handler mHandler;
    private static final String Tag = "onCreate";

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("title_devices");
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, "error_bluetooth_not_supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Log.d("Tag","Entered oncreate");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning)
        {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }
        else
        {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        }

        Log.d("Tag","onCreateOptionsMenu");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        Log.d("Tag","onOptionsItemSelected");
        return true;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled())
        {
            if (!mBluetoothAdapter.isEnabled())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
        Log.d("Tag","onResume");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED)
        {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Tag","onActivityResult");
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning)
        {
           // mBluetoothAdapter.stopLeScan(mLeScanCallback);
            bluetoothLeScanner.stopScan(scanCallback);
            mScanning = false;
        }
        startActivity(intent);
        Log.d("Tag","onListItemClick");
    }

    private void scanLeDevice(final boolean enable)
    {
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (enable)
        {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mScanning = false;
                    // mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    bluetoothLeScanner.stopScan(scanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            try
            {
                // mBluetoothAdapter.startLeScan(mLeScanCallback);
                bluetoothLeScanner.startScan(scanCallback);
            }
            catch(Exception i)
            {
                Log.e("Tag","Error");
            }

            }
        else
        {
            mScanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
        invalidateOptionsMenu();
        Log.d("Tag","scanLeDevice");
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter
    {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter()
        {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device)
        {
            if(!mLeDevices.contains(device))
            {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position)
        {
            return mLeDevices.get(position);
        }

        public void clear()
        {
            mLeDevices.clear();
        }

        @Override
        public int getCount()
        {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i)
        {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null)
            {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView)view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView)view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("unknown_device");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }

    }

    // Device Scan callback - New version
    private ScanCallback scanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, final ScanResult result)
        {
            super.onScanResult(callbackType, result);
            Log.d("Tag","ScanCallback");

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mLeDeviceListAdapter.addDevice(result.getDevice());
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode)
        {
            super.onScanFailed(errorCode);
        }

    };

    static class ViewHolder
    {
        TextView deviceName;
        TextView deviceAddress;
    }
}


