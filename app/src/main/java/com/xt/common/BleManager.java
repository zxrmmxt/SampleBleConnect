package com.xt.common;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.text.TextUtils;

import com.xt.common.ble.connect.MyBleUtils;
import com.xt.common.ble.connect.MyScanUtils;
import com.xt.common.utils.MyLogUtils;
import com.xt.common.utils.thread.MyThreadUtils;

public class BleManager {
    private static BleManager instance;
    private final MyBleUtils myBleUtils;
    private final MyScanUtils myScanUtils;

    private static final String BLE_NAME = "OPPO R11s Plus";
    private static final String BLE_ADDRESS = "d4:1a:3f:c0:b0:3e";

    private Handler connectHandler = MyThreadUtils.getThreadHandler();

    public static BleManager getInstance() {
        if (instance == null) {
            instance = new BleManager();
        }
        return instance;
    }

    private BleManager() {
        myBleUtils = new MyBleUtils();
        myScanUtils = new MyScanUtils();

        myBleUtils.addMyBleCallback(new MyBleUtils.OnMyBleCallback() {
            @Override
            public void onMyBle(MyBleUtils.MyBleData myBleData) {
                String myBleAction = myBleData.getMyBleAction();
                if (TextUtils.equals(myBleAction, MyBleUtils.ACTION_GATT_CONNECTED)) {
                    MyLogUtils.d(BleManager.class.getSimpleName(), "连接成功");
                }
                if (TextUtils.equals(myBleAction, MyBleUtils.ACTION_GATT_DISCONNECTED)) {
                    doConnect();
                }
            }
        });

        myScanUtils.addScanCallback(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                BleManager.this.onScanResult(result.getDevice());
            }
        });
    }

    public void doConnect() {
        synchronized (BleManager.class) {
            if (myBleUtils.getBleDevice() == null) {
                return;
            }

            if (myBleUtils.isConnected()) {
                if (TextUtils.equals(myBleUtils.getDeviceAddress(), BLE_ADDRESS)) {
                    return;
                }
                disconnect();
            }

            if (myBleUtils.isConnecting()) {
                return;
            }

            myBleUtils.connect();
        }
    }

    public void disconnect() {
        if (myBleUtils.isDisconnected()) {
            return;
        }
        myBleUtils.disconnect();
    }

    public MyBleUtils getMyBleUtils() {
        return myBleUtils;
    }

    public MyScanUtils getMyScanUtils() {
        return myScanUtils;
    }

    private void onScanResult(BluetoothDevice device) {
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            return;
        }

        if (!TextUtils.equals(name, BLE_ADDRESS)) {
            return;
        }

        if (TextUtils.equals(BLE_ADDRESS, myBleUtils.getDeviceAddress())) {
            return;
        }

        myBleUtils.setBleDevice(device);

        connectHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doConnect();
            }
        }, 1000);
    }

}
