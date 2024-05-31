package de.uni_stuttgart.ipvs.mc.task1;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import android.widget.TextView;
import android.widget.Toast;

import com.neovisionaries.bluetooth.ble.util.Bytes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity
{
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private float distance;
    private String beaconID;
    private String url;
    private int voltage;
    private float temperature;
    private boolean viewIsEnabled = false;

    private static String TAG = "DeviceScanActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 100000000;
    private static final UUID[] uuid = new UUID[] {UUID.fromString("0000feaa-0000-1000-8000-00805f9b34fb")};
    private static final int A = -64;
    private static final float n = 2;
    private static final String[] SCHEME_PREFIXES =
    {
            "http://www.",      // 0
            "https://www.",     // 1
            "http://",          // 2
            "https://",         // 3
    };
    private static final String[] EXPANSION_CODES =
     {
            ".com/",            //  0, 0x00
            ".org/",            //  1, 0x01
            ".edu/",            //  2, 0x02
            ".net/",            //  3, 0x03
            ".info/",           //  4, 0x04
            ".biz/",            //  5, 0x05
            ".gov/",            //  6, 0x06
            ".com",             //  7, 0x07
            ".org",             //  8, 0x08
            ".edu",             //  9, 0x09
            ".net",             // 10, 0x0A
            ".info",            // 11, 0x0B
            ".biz",             // 12, 0x0C
            ".gov",             // 13, 0x0D
     };

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

    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(uuid,mLeScanCallback);
        }
        else
        {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
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

                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.mBeaconID = (TextView) view.findViewById(R.id.beacon_id);
                viewHolder.mUrl = (TextView) view.findViewById(R.id.url);
                viewHolder.mVoltage = (TextView) view.findViewById(R.id.voltage);
                viewHolder.mTemperature = (TextView) view.findViewById(R.id.temperature);
                viewHolder.mDistance = (TextView) view.findViewById(R.id.distance);
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
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewIsEnabled = true;
            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =new BluetoothAdapter.LeScanCallback()
    {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
                {
                    distance = getDistance(rssi);
                    byte[] data = null;
                    int length = scanRecord.length;
                    for(int i = 0; i < length;)
                    {
                        int len =  scanRecord[i] & 0xFF;
                        // If the length is zero.
                        if (len == 0)
                        {
                            break;
                        }
                        if ((length - i - 1) < len)
                        {
                            break;
                        }
                        data = Arrays.copyOfRange(scanRecord, i + 2, i + len + 1);
                        i += 1 + len;
                    }

                    int frameType = (data[2] & 0xF0);
                    switch (frameType)
                    {
                        // Eddystone UID
                        case 0x00:
                            beaconID =  Bytes.toHexString(copyOfRange(data, 4, 20),true);
                            break;

                        // Eddystone URL
                        case 0x10:
                            url = extractURL(data).toString();
                            break;

                        // Eddystone TLM
                        case 0x20:
                            voltage = Bytes.parseBE2BytesAsInt(data, 4);
                            temperature =  Bytes.convertFixedPointToFloat(data, 6);
                            break;

                        default:
                            break;
                    }

                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    if(viewIsEnabled)
                    {
                        update();
                    }
                }
            };

    private URL extractURL(byte[] data)
    {
        StringBuilder builder = new StringBuilder();

        // URL Scheme Prefix
        String prefix = extractSchemePrefix(data);
        if (prefix != null)
        {
            builder.append(prefix);
        }

        for (int i = 5; i < data.length; ++i)
        {
            int ch = data[i];

            if (0 <= ch && ch < EXPANSION_CODES.length)
            {
                builder.append(EXPANSION_CODES[ch]);
            }
            else if (0x20 < ch && ch < 0x7F)
            {
                builder.append((char)ch);
            }
        }

        if (builder.length() == 0)
        {
            return null;
        }

        try
        {
            return new URL(builder.toString());
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }

    private String extractSchemePrefix(byte[] data)
    {
        // data[4] = URL Scheme Prefix

        if (data.length < 5)
        {
            return null;
        }

        int code = data[4];

        if (code < 0 || SCHEME_PREFIXES.length <= code)
        {
            return null;
        }

        return SCHEME_PREFIXES[code];
    }

    private byte[] copyOfRange(byte[] source, int from, int to)
    {
        if (source == null || source.length < to)
        {
            return null;
        }

        int length = to - from;

        byte[] destination = new byte[length];

        System.arraycopy(source, from, destination, 0, length);

        return destination;
    }


    private void update()
    {
        ViewHolder.mBeaconID.setText(beaconID);
        ViewHolder.mUrl.setText(url);
        ViewHolder.mVoltage.setText(String.valueOf(voltage));
        ViewHolder.mTemperature.setText(String.valueOf(temperature));
        ViewHolder.mDistance.setText(String.valueOf(distance));
    }

    private float getDistance(int rssi)
    {
        return (float) Math.pow(10,(float)(rssi - A)/(-10 * n));
    }

    static class ViewHolder
    {
        TextView deviceName;
        TextView deviceAddress;
        static TextView mBeaconID;
        static TextView mUrl;
        static TextView mVoltage;
        static TextView mTemperature;
        static TextView mDistance;
    }
}


