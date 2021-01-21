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

    private BluetoothDevice mBluetoothDevice;

    private Handler connectHandler = MyThreadUtils.getThreadHandler();

    private int connectingCounts;

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

    private void onScanResult(BluetoothDevice device) {
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            return;
        }

        if (!TextUtils.equals(name, BLE_NAME)) {
            return;
        }

        mBluetoothDevice = device;

        doConnect();
    }

    public void doConnect() {
        connectHandler.post(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (myBleUtils.isConnecting()) {
                    connectingCounts = connectingCounts + 1;
                    if (connectingCounts > 8) {
                        connectingCounts = 0;
                        disconnect();
                        myBleUtils.closeBle();
                    }
                    return;
                }

                if (mBluetoothDevice == null) {
                    return;
                }

                if (myBleUtils.isConnected()
                        && TextUtils.equals(myBleUtils.getBleDevice().getAddress(), mBluetoothDevice.getAddress())) {
                    return;
                }
                disconnect();
                myBleUtils.closeBle();

                myBleUtils.setBleDevice(mBluetoothDevice);

                myBleUtils.connect();
            }
        });
    }

    public void disconnect() {
        myBleUtils.disconnect();
    }

    public MyBleUtils getMyBleUtils() {
        return myBleUtils;
    }

    public MyScanUtils getMyScanUtils() {
        return myScanUtils;
    }
}
