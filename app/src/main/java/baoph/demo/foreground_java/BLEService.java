package baoph.demo.foreground_java;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;


import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;



public class BLEService extends Service{

    public static final int STATUS_SCAN_FINISH = 0;
    public static final int STATUS_SCAN_SETUP = 1;
    public static final int STATUS_SCANNING = 2;

    // Bluetooth scan devices
    private BluetoothScanBroadCast mScanDevicesReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private int mStatusScanDevices = STATUS_SCAN_FINISH;

    // Bluetooth scan LE
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private int mStatusScanBle = STATUS_SCAN_FINISH;

    // Bluetooth broadcast LE
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdvertiseCallback;
    private int mStatusAdvertising = STATUS_SCAN_FINISH;


    private boolean mIsReport = false;

    private long mLastTimeScanCallBack;

    private static final String TAG = "BLEService";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Log
        Log.e(TAG, "Starting BLEService");
        // Initialize Bluetooth notification
//        AppUtils.startNotification(this, getApplicationContext());

        initBluetooth();

        initStatus();

        //TODO: Write Timer for Request permission here

    }

    /**
     * Khoi tao cac bien
     */
    private void initBluetooth() {
        // Init
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        mBluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
    }

    /**
     * Init status
     */
    private void initStatus() {
        // Flag
        mStatusScanBle = STATUS_SCAN_FINISH;
        mStatusAdvertising = STATUS_SCAN_FINISH;
        mStatusScanDevices = STATUS_SCAN_FINISH;
    }

    private void sendNotification(String dataIntent) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, MyApplication.CHANNEL_ID)
                .setContentTitle("Notification service")
                .setContentText(dataIntent)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "On Start Command");
        String dataIntent = intent.getStringExtra("key_data_intent");
        sendNotification(dataIntent);
        // TODO: Initialize scheduler like TraceCovidService

        // Check Bluetooth and enable all service
        startAll();

        return START_STICKY;

    }

    private void startAll(){
        startBroadCastBLE();
        startScanBle();
    }

    /**
     * Phat song BLE
     */
    public void startBroadCastBLE(){
        try {
            // Check
            if (mBluetoothLeAdvertiser != null && mStatusAdvertising == STATUS_SCAN_FINISH) {
                // Set lai status
                mStatusAdvertising = STATUS_SCAN_SETUP;

                // Log
                Log.e(TAG, "startBroadCastBle setup");

                // Advertise build
                AdvertiseSettings.Builder advertiseSettings = new AdvertiseSettings.Builder();

                // Setting advertisde
                advertiseSettings.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
                advertiseSettings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW);
                advertiseSettings.setConnectable(true);

                // data advertise BLE
                AdvertiseData.Builder builder = new AdvertiseData.Builder();
                builder.setIncludeDeviceName(false);
                builder.setIncludeTxPowerLevel(false);

                // Add Manufacturer
                String bluezoneId = "KLMNEF"; //TODO: This should be discussed. How to generate a proper, unique manufacturer data
                builder.addManufacturerData(AppConstants.BLE_ID, bluezoneId.getBytes());
                builder.addServiceUuid(AppUtils.BLE_UUID_ANDROID);

                Log.e(TAG, "UUID " + AppUtils.BLE_UUID_ANDROID.toString());
                // Callback start
                mAdvertiseCallback = new AdvertiseCallback() {
                    @Override
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        super.onStartSuccess(settingsInEffect);
                        // Set bien
                        mStatusAdvertising = STATUS_SCANNING;
                        // Log
                        Log.e(TAG,"Start: BluetoothLeAdvertiser : start : success");
                    }

                    @Override
                    public void onStartFailure(int errorCode) {
                        super.onStartFailure(errorCode);
                        // Set bien
                        mStatusAdvertising = STATUS_SCAN_FINISH;
                        // Log
                        Log.e(TAG,"Start: BluetoothLeAdvertiser : start : fail : Code: " + errorCode);
                    }
                };

                // Start broadCast ble
                mBluetoothLeAdvertiser.startAdvertising(advertiseSettings.build(), builder.build(), mAdvertiseCallback);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop broadcast ble
     */
    private void stopBroadcastBle() {
        try {
            // Check phat va stop
            if (mBluetoothLeAdvertiser != null && mAdvertiseCallback != null) {
                mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Scan BLE
     */
    public void startScanBle() {
        try {
            // Check
            if (mBluetoothLeScanner != null && mStatusScanBle == STATUS_SCAN_FINISH) {
                // set status
                mStatusScanBle = STATUS_SCAN_SETUP;

                mLastTimeScanCallBack = System.currentTimeMillis();

                // Log
                Log.e(TAG,"startScanBle setup");

                // Setting BLE Scan
                ScanSettings.Builder scanSettings = new ScanSettings.Builder();
                scanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                if (mIsReport) {
                    mIsReport = false;
                    scanSettings.setReportDelay(AppConstants.Config.TIME_SCAN_BLE_REPORT_DELAY);
                } else {
                    mIsReport = true;
                }
                // Build filter
                // Build filter Android
                ScanFilter.Builder scanFilterAndroid = new ScanFilter.Builder();
                scanFilterAndroid.setServiceUuid(AppUtils.BLE_UUID_ANDROID);
                // Add filter
                List<ScanFilter> listFilter = new ArrayList<>();
                listFilter.add(scanFilterAndroid.build());


                // Callback khi scan bluetooth
                mScanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        Log.e(TAG, "On scan Result");
                        super.onScanResult(callbackType, result);
                        mStatusScanBle = STATUS_SCANNING;
                        byte[] blidContact = result.getScanRecord().getManufacturerSpecificData(AppConstants.BLE_ID);
                        String deviceName = result.getDevice().getName();
                        int rssi = result.getRssi();
                        String address = result.getDevice().getAddress();
                        List<ParcelUuid> serviceUUIDs = result.getScanRecord().getServiceUuids();
                        Log.e(TAG, "Detect on Scan " + deviceName + " " + rssi + "  " + address
                                + " Service : " + serviceUUIDs + " Manufacture " + new String(blidContact, StandardCharsets.UTF_8));
                    }

                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {
                        Log.e(TAG, "On batch scan Result");
                        super.onBatchScanResults(results);
                        mStatusScanBle = STATUS_SCANNING;
                        for (ScanResult result : results){
                            byte[] blidContact = result.getScanRecord().getManufacturerSpecificData(AppConstants.BLE_ID);
                            String deviceName = result.getDevice().getName();
                            int rssi = result.getRssi();
                            String address = result.getDevice().getAddress();
                            List<ParcelUuid> serviceUUIDs = result.getScanRecord().getServiceUuids();
                            Log.e(TAG, "Detect on Batch Scan " + deviceName + " " + rssi + "  " + address
                                    + " Service : " + serviceUUIDs + " Manufacture " + new String(blidContact, StandardCharsets.UTF_8));
                        }
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        super.onScanFailed(errorCode);
                        mStatusScanBle = STATUS_SCAN_FINISH;
                        Log.e(TAG,"startScanBle: fail : Code: " + errorCode);
                    }
                };

                // Start scan
                Log.e(TAG,"starting to scan");
                mBluetoothLeScanner.startScan(listFilter, scanSettings.build(), mScanCallback);
//                mBluetoothLeScanner.startScan(mScanCallback);

                // Status
                mStatusScanBle = STATUS_SCANNING;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop scan ble
     */
    private void stopScanBle() {
        try {
            if (mBluetoothLeScanner != null && mScanCallback != null) {
                Log.e(TAG,"stopping BLE scanning");
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopBroadcastBle();
        stopScanBle();
    }


    /**
     * Broadcast để nhận các bluetooth bình thường đã quét đc thiết bị điện thoại và các thiết bị ngoại vi, ...
     */
    class BluetoothScanBroadCast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                // nếu tìm thấy
                if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {

                    // lấy dữ liệu
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                    // Lay ten
                    String name = bluetoothDevice.getName();
                    String address = bluetoothDevice.getAddress();
                    String userIdRN = "";
                    String nameRN = "";
                    String platform = "";
                    int type = 0;

                    Log.e(TAG, "Receive " + name + " rssi : " + rssi + " address : " + address);
                }

                // Check su kien ket thuc
                if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                    // Log
                    Log.e(TAG,"Stop Discovery and restart discovery");

                    // Start
                    // bắt đầu quét.
                    if (!mBluetoothAdapter.startDiscovery()) {
                        // Set bien
                        mStatusScanDevices = STATUS_SCAN_FINISH;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}




































