package com.xt.common;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.text.TextUtils;

import com.xt.common.ble.connect.MyBleUtils;
import com.xt.common.ble.connect.MyScanUtils;
import com.xt.common.utils.MyLogUtils;
import com.xt.common.utils.thread.MyThreadUtils;

public class BleManager {
    private static final String TAG = BleManager.class.getSimpleName();
    private static BleManager instance;
    private final MyBleUtils myBleUtils;
    private final MyScanUtils myScanUtils;

    private static final String BLE_NAME = "OPPO R11s Plus";
    private static final String BLE_ADDRESS = "d4:1a:3f:c0:b0:3e";

    private BluetoothDevice mBluetoothDevice;

    private Handler connectHandler = MyThreadUtils.getThreadHandler();
    private Handler scanHandler = MyThreadUtils.getThreadHandler();

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
                    MyLogUtils.d(TAG, "BLE连接成功");
                }
                if (TextUtils.equals(myBleAction, MyBleUtils.ACTION_GATT_DISCONNECTED)) {
                    MyLogUtils.d(TAG, "BLE断开连接");
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

    private void onScanResult(final BluetoothDevice device) {
        final String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            return;
        }

        if (!TextUtils.equals(name, BLE_NAME)) {
            return;
        }

        scanHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (mBluetoothDevice != null) {
                    if (TextUtils.equals(mBluetoothDevice.getName(), name)) {
                        return;
                    }
                }

                mBluetoothDevice = device;
                MyLogUtils.d(TAG, "扫描到的设备：" + mBluetoothDevice.getName());

                doConnect();
            }
        });
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

                MyThreadUtils.doBackgroundWork(new Runnable() {
                    @Override
                    public void run() {
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
    public void scanBle(){
        BleManager.getInstance().getMyScanUtils().startBleScan(new ScanFilter.Builder().setDeviceName("OPPO R11s Plus"));
    }
}
