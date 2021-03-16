package com.xt.common;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.text.TextUtils;

import com.blankj.utilcode.util.SPStaticUtils;
import com.xt.common.ble.connect.MyBleUtils;
import com.xt.common.ble.connect.MyScanUtils;
import com.xt.common.utils.BleBroadcastUtils;
import com.xt.common.utils.MyLogUtils;
import com.xt.common.utils.thread.LimitTimesTimerTask;
import com.xt.common.utils.thread.MyThreadUtils;

import java.util.ArrayList;

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

    private final Object connectBleLock = new Object();
    private MyThreadUtils.LimitTimesTimerTaskController limitTimesTimerTaskController = new MyThreadUtils.LimitTimesTimerTaskController(Integer.MAX_VALUE, 0, 10000, new LimitTimesTimerTask.LimitTimesTimerTaskCallback() {
        @Override
        public void onDoTimerTask(int times) {
            synchronized (connectBleLock){
                doConnect();
            }
        }

        @Override
        public void onTimerTaskEnd() {

        }
    });

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
                    startConnectTask();
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

        BleBroadcastUtils.registerReceiver();
        BleBroadcastUtils.ON_RECEIVE_BLE_STATE_LISTENER_LIST.add(new BleBroadcastUtils.OnReceiveBleStateListener() {
            @Override
            public void onReceiveBleState(String action, BluetoothDevice bluetoothDevice) {
                if (TextUtils.equals(action, BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    if (MyBleUtils.isEnable()) {
                        startConnectTask();
                    }
                }
            }
        });
    }

    private void onScanResult(final BluetoothDevice device) {
        MyThreadUtils.doBackgroundWork(new Runnable() {
            @Override
            public void run() {
                final String name = device.getName();
                if (TextUtils.isEmpty(name)) {
                    return;
                }

                if (!TextUtils.equals(name, getBleDeviceName2Connect())) {
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
                            mBluetoothDevice = null;
                        }

                        if (!TextUtils.equals(name, getBleDeviceName2Connect())) {
                            return;
                        }

                        mBluetoothDevice = device;
                        saveBleDevice2Local(device);
                        MyLogUtils.d(TAG, "扫描到的设备：" + mBluetoothDevice.getName());

                        startConnectTask();
                    }
                });
            }
        });
    }

    private void saveBleDevice2Local(BluetoothDevice device) {
        if (device == null) {
            return;
        }
        String name = device.getName();
        String address = device.getAddress();
        if (TextUtils.isEmpty(name)) {
            return;
        }
        if (TextUtils.isEmpty(address)) {
            return;
        }
        SPStaticUtils.put("BluetoothDeviceName", name);
        SPStaticUtils.put("BluetoothDeviceAddress", address);
    }

    private BluetoothDevice getLocalBleDevice() {
        String bluetoothDeviceName = SPStaticUtils.getString("BluetoothDeviceName");
        if (TextUtils.isEmpty(bluetoothDeviceName)) {
            return null;
        }
        if (!TextUtils.equals(bluetoothDeviceName, getBleDeviceName2Connect())) {
            return null;
        }
        String bluetoothDeviceAddress = SPStaticUtils.getString("BluetoothDeviceAddress");
        if (TextUtils.isEmpty(bluetoothDeviceAddress)) {
            scanBle();
            return null;
        }

        BluetoothAdapter bluetoothAdapter = MyBleUtils.getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return null;
        }
        return bluetoothAdapter.getRemoteDevice(bluetoothDeviceAddress);
    }

    public void startConnectTask() {
        limitTimesTimerTaskController.startLimitTimesTimerTak();
        scanBle();
    }

    public void doConnect() {
        connectHandler.post(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (myBleUtils.isConnected()
                        && (!TextUtils.equals(myBleUtils.getBleDevice().getName(), getBleDeviceName2Connect()))) {
                    disconnect();
                    myBleUtils.closeBle();
                }

                if (mBluetoothDevice == null) {
                    mBluetoothDevice = getLocalBleDevice();
                }
                if (mBluetoothDevice == null) {
                    scanBle();
                    return;
                }

                if (!TextUtils.equals(mBluetoothDevice.getName(), getBleDeviceName2Connect())) {
                    mBluetoothDevice = null;
                    scanBle();
                    return;
                }

                myScanUtils.stopScan();

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
                            scanBle();
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

    /**
     * 过滤扫描
     * <p>
     * 连接判断
     *
     * @return
     */
    public String getBleDeviceName2Connect() {
        return BLE_NAME;
    }

    public MyBleUtils getMyBleUtils() {
        return myBleUtils;
    }

    public MyScanUtils getMyScanUtils() {
        return myScanUtils;
    }

    public void scanBle() {
        ArrayList<ScanFilter.Builder> builders = new ArrayList<>();
        builders.add(new ScanFilter.Builder().setDeviceName(getBleDeviceName2Connect()));
        myScanUtils.startBleScan(builders);
    }
}
