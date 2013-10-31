package com.rain.android.heartratemonitor.monitor;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.rain.android.heartratemonitor.bluetoothLE.BlueToothLEEvent;
import com.rain.android.heartratemonitor.bluetoothLE.BluetoothLeService;
import com.rain.android.heartratemonitor.bluetoothLE.GattAttributes;
import com.rain.android.heartratemonitor.utilities.BluetoothLEUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import de.greenrobot.event.EventBus;

@TargetApi(18)
public class BasicHeartRateProvider {

    private BluetoothLeService mBluetoothLeService;
    private int mBpm = HeartRateConstants.HEART_RATE_MONITOR_NOT_CONNECTED; // -1 = not connected, 0 = connected but not getting heart rate

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private int mCurrentRSSI = 0;

    private boolean mConnected = false;

    private BluetoothDevice mDevice;
    private Context mContext;

    private Timer mTimer;
    private Timer mDeviceTimer;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    public boolean isConnected() {
        return mConnected;
    }

    public int getBpm() {
        return mBpm;
    }

    public void start(Context context) {

        mContext = context;

        mHandler = new Handler();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothAdapter.cancelDiscovery();

        final Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
        context.bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);

        if (BluetoothLEUtils.hasBluetoothLE(context)) {
            registerReceivers();
        }
    }

    public void stop() {

        scanLeDevice(false);

        mConnected = false;
        mDevice = null;
        mCurrentRSSI = 0;
        mBpm = HeartRateConstants.HEART_RATE_MONITOR_NOT_CONNECTED;

        Log.d("BLE", "Stopping service");

        if (mContext != null) {
            mContext.unbindService(mServiceConnection);
            unRegisterReceivers();
        } else {
            Log.d("BLE", "No Context");
        }

        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        } else {
            Log.d("BLE", "No Timer");
        }

        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        } else {
            Log.d("BLE", "No Bluetooth Service");
        }

        mBluetoothLeService = null;

        Log.d("BLE", "Finished ending service");
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("BLE", "Unable to initialize Bluetooth");
            }

            scanLeDevice(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            if (mBluetoothLeService != null) {
                mBluetoothLeService.disconnect();
                mBluetoothLeService.close();
            }

            mBluetoothLeService = null;
            mBpm = HeartRateConstants.HEART_RATE_MONITOR_NOT_CONNECTED;
        }
    };

    private void scanLeDevice(final boolean enable) {

        if (enable) {

            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {

                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(mRunnable, SCAN_PERIOD);

                EventBus.getDefault().post(new HeartRateEvent("Scanning for devices"));

                mScanning = true;
                final UUID[] serviceUuids = new UUID[]{UUID.fromString(GattAttributes.HEART_RATE_SERVICE)};
                mBluetoothAdapter.startLeScan(serviceUuids, mLeScanCallback);
            }

        } else {
            mScanning = false;

            removeTimers();

            if (mBluetoothAdapter != null) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {

            EventBus.getDefault().post(new HeartRateEvent("Couldn't find anything"));

            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

            if (!mConnected) {

                EventBus.getDefault().post(new HeartRateEvent("Not connected, trying again..."));

                mBpm = HeartRateConstants.HEART_RATE_MONITOR_NOT_CONNECTED;
                mCurrentRSSI = 0;

                removeTimers();

                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        scanLeDevice(true);
                    }
                }, SCAN_PERIOD);
            }
        }
    };

    private void removeTimers() {

        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    private void setDevice(BluetoothDevice device) {

        EventBus.getDefault().post(new HeartRateEvent("Setting device: " + device.getAddress()));

        if (mDevice != null && device != null && !device.getAddress().equals(mDevice.getAddress()) &&
                mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
        }

        mDevice = device;

        if (mDevice != null && mBluetoothLeService != null) {

            mBluetoothLeService.connect(mDevice.getAddress());

            removeTimers();

            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!mConnected) {
                        scanLeDevice(true);
                    }
                }
            }, SCAN_PERIOD);
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_ERROR);
        return intentFilter;
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

                EventBus.getDefault().post(new HeartRateEvent("Connected to device: " + mDevice.getAddress()));

                removeTimers();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

                EventBus.getDefault().post(new HeartRateEvent("Disconnected from device: " + mDevice.getAddress()));

                mConnected = false;

                mBpm = HeartRateConstants.HEART_RATE_MONITOR_NOT_CONNECTED;
                EventBus.getDefault().post(new BlueToothLEEvent(mBpm));

                scanLeDevice(true);

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                mConnected = true;

                EventBus.getDefault().post(new HeartRateEvent("Gatt services connected"));

                mBpm = 0;
                EventBus.getDefault().post(new BlueToothLEEvent(mBpm));

                final List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices();

                // Loops through available GATT Services.
                boolean found = false;
                String uuid = "";
                for (BluetoothGattService gattService : gattServices) {

                    final List<BluetoothGattCharacteristic> gattCharacteristics =
                            gattService.getCharacteristics();

                    if (gattCharacteristics != null) {

                        // Loops through available Characteristics.
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                            if (gattCharacteristic != null) {

                                uuid = gattCharacteristic.getUuid().toString();
                                if (mBluetoothLeService != null && uuid != null &&
                                        uuid.equals(GattAttributes.HEART_RATE_MEASUREMENT)) {

                                    found = true;

                                    mBluetoothLeService.setCharacteristicNotification(
                                            gattCharacteristic, true);

                                }
                            }
                        }
                    }
                }

                if (!found) {
                    EventBus.getDefault().post(new HeartRateEvent("Connected to a non heart rate monitor"));
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                final String bpm = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);

                EventBus.getDefault().post(new HeartRateEvent("Heart rate received: " + bpm));

                try {
                    mBpm = Integer.parseInt(bpm);
                    EventBus.getDefault().post(new BlueToothLEEvent(mBpm));
                } catch (Exception exp) {
                    exp.printStackTrace();
                    mBpm = 0;
                }
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_ERROR.equals(action)) {

                EventBus.getDefault().post(new HeartRateEvent("Gatt services error"));

                removeTimers();

                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        scanLeDevice(true);
                    }
                }, SCAN_PERIOD);

            }
        }
    };

    private final BroadcastReceiver mBlueToothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        EventBus.getDefault().post(new HeartRateEvent("Bluetooth turned on"));
                        mBpm = HeartRateConstants.HEART_RATE_MONITOR_NOT_CONNECTED;
                        EventBus.getDefault().post(new BlueToothLEEvent(mBpm));
                        if (mContext != null) {
                            mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                        }
                        scanLeDevice(true);
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        EventBus.getDefault().post(new HeartRateEvent("Bluetooth turned off"));
                        stopListening();
                        if (mContext != null) {
                            try {
                                mContext.unregisterReceiver(mGattUpdateReceiver);
                            } catch (Exception exp) {
                                exp.printStackTrace();
                            }
                        }
                        break;
                }
            }
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            if ((Math.abs(rssi) < mCurrentRSSI || mCurrentRSSI == 0) && (mDevice == null || !mConnected || !device.getAddress().equals(mDevice.getAddress()))) {

                mDevice = device;

                if (mDeviceTimer != null) {
                    mDeviceTimer.cancel();
                    mDeviceTimer.purge();
                }

                mDeviceTimer = new Timer();
                mDeviceTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        scanLeDevice(false);
                        setDevice(device);
                    }
                }, 1000);

                mCurrentRSSI = Math.abs(rssi);
            }
        }
    };

    private void unRegisterReceivers() {

        //EventBus.getDefault().post(new HeartRateEvent("Unregistering receivers"));

        try {
            mContext.unregisterReceiver(mGattUpdateReceiver);
        } catch (Exception exp) {
            exp.printStackTrace();
        }

        try {
            mContext.unregisterReceiver(mBlueToothReceiver);
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    private void registerReceivers() {

        //EventBus.getDefault().post(new HeartRateEvent("Resistering receivers"));

        if (mContext != null) {
            mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            final IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            mContext.registerReceiver(mBlueToothReceiver, filter);
        }
    }

    private void stopListening() {

        EventBus.getDefault().post(new HeartRateEvent("Turning off heart rate monitor"));

        mBpm = HeartRateConstants.HEART_RATE_MONITOR_NOT_CONNECTED;
        mConnected = false;
        mDevice = null;
        mCurrentRSSI = 0;

        EventBus.getDefault().post(new BlueToothLEEvent(mBpm));

        scanLeDevice(false);

        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        }
    }
}
