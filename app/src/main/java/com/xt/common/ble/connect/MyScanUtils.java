package com.xt.common.ble.connect;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.blankj.utilcode.util.Utils;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MyScanUtils {
    /**
     * 扫描时是否过滤serviceUUID
     */
    private boolean isScanFilterServiceUuid = false;

    /**
     * 扫描广播的频率
     */
    private int scanMode = ScanSettings.SCAN_MODE_LOW_LATENCY;

    private List<ScanCallback> scanCallbackList = new ArrayList<>();

    private ScanCallback mInnerScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null) {
                return;
            }

            for (ScanCallback scanCallback : scanCallbackList) {
                scanCallback.onScanResult(callbackType, result);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public MyScanUtils() {
    }

    public void addScanCallback(ScanCallback scanCallback) {
        scanCallbackList.add(scanCallback);
    }

    public void removeScanCallback(ScanCallback scanCallback) {
        scanCallbackList.remove(scanCallback);
    }

    public void startBleScan() {
        startBleScan(null);
    }

    public void startBleScan(List<ScanFilter.Builder> builderList) {
        List<ScanFilter> scanFilters = new ArrayList<>();
        if (builderList != null) {
            for (ScanFilter.Builder builder : builderList) {
                scanFilters.add(builder.build());
            }
        }
        if (!isGranted(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) {
            return;
        }
        if (!isLocationOpen()) {
            return;
        }
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            return;
        }
        BluetoothLeScanner bluetoothLeScanner = getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            return;
        }

        bluetoothLeScanner.stopScan(mInnerScanCallback);
        bluetoothLeScanner.startScan(scanFilters, buildScanSettings(), mInnerScanCallback);
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(scanMode);
        return builder.build();
    }

    public void stopScan() {
        BluetoothLeScanner bluetoothLeScanner = getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            return;
        }
        bluetoothLeScanner.stopScan(mInnerScanCallback);
    }

    private boolean isGranted(final String... permissions) {
        for (String permission : permissions) {
            if (!isGranted(permission)) {
                return false;
            }
        }
        return true;
    }

    private boolean isGranted(final String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || PackageManager.PERMISSION_GRANTED
                == ContextCompat.checkSelfPermission(Utils.getApp(), permission);
    }

    private boolean isLocationOpen() {
        LocationManager locationManager = (LocationManager) Utils.getApp().getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps || network;
    }

    public static BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) Utils.getApp().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return null;
        }
        return bluetoothManager.getAdapter();
    }

    public static BluetoothLeScanner getBluetoothLeScanner() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return null;
        }
        return bluetoothAdapter.getBluetoothLeScanner();
    }
}
