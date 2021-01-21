package com.xt.common.ble.connect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.blankj.utilcode.util.Utils;
import com.xt.common.utils.BleBroadcastUtils;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MyBleUtils {
    private final List<OnMyBleCallback> onMyBleCallbackList = new ArrayList<>();

    private BluetoothDevice mBluetoothDevice;

    private BluetoothGatt mBluetoothGatt;

    // 蓝牙连接状态
    private int mConnectionState = STATE_DISCONNECTED;
    // 蓝牙连接已断开
    private static final int STATE_DISCONNECTED = 0;
    // 蓝牙正在连接
    private static final int STATE_CONNECTING = 1;
    // 蓝牙已连接
    private static final int STATE_CONNECTED = 2;

    // 蓝牙已连接
    public final static String ACTION_GATT_CONNECTED = "com.xt.common.ble.ACTION_GATT_CONNECTED";
    // 蓝牙已断开
    public final static String ACTION_GATT_DISCONNECTED = "com.xt.common.ble.ACTION_GATT_DISCONNECTED";
    // 发现GATT服务
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.xt.common.ble.ACTION_GATT_SERVICES_DISCOVERED";
    // 收到蓝牙数据
    public final static String ACTION_DATA_AVAILABLE = "com.xt.common.ble.ACTION_DATA_AVAILABLE";
    // 连接失败
    public final static String ACTION_CONNECTING_FAIL = "com.xt.common.ble.ACTION_CONNECTING_FAIL";
    // 蓝牙数据
    public final static String EXTRA_DATA = "com.xt.common.ble.EXTRA_DATA";

    /**
     * 00001000-0000-1000-8000-00805F9B34FB
     * 0000100A-0000-1000-8000-00805F9B34FB
     * 广为人知的uuid
     */
    private static final ParcelUuid SERVICE_UUID_WELL_KNOWN = getParcelUuid("1000");

    // 服务标识
    public static final ParcelUuid SERVICE_UUID = SERVICE_UUID_WELL_KNOWN;
    // 特征标识（读取数据）
    private static final ParcelUuid CHARACTERISTIC_READ_UUID = getParcelUuid("FFF2");
    // 特征标识（发送数据）
    private static final ParcelUuid CHARACTERISTIC_WRITE_UUID = getParcelUuid("FFF1");
    // 描述标识
    private static final ParcelUuid DESCRIPTOR_UUID = getParcelUuid("2902");

    private BluetoothAdapter bluetoothAdapter;

    /**
     * 蓝牙操作回调
     * 蓝牙连接状态才会回调
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mBluetoothDevice == null) {
                    return;
                }
                BluetoothDevice device = gatt.getDevice();
                if (!TextUtils.equals(device.getAddress(), mBluetoothDevice.getAddress())) {
                    return;
                }


                // 蓝牙已连接
                if (mConnectionState == STATE_CONNECTED) {
                    return;
                }
                mConnectionState = STATE_CONNECTED;
                notifyBleState(ACTION_GATT_CONNECTED);
                // 搜索GATT服务
                mBluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                closeBle();

                // 蓝牙已断开连接
                if (mConnectionState == STATE_DISCONNECTED) {
                    return;
                }
                mConnectionState = STATE_DISCONNECTED;
                notifyBleState(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // 发现GATT服务
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setBleNotification();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // 收到数据
            notifyBleData(ACTION_DATA_AVAILABLE, characteristic);
        }
    };


    public MyBleUtils() {
        BluetoothManager bluetoothManager = (BluetoothManager) Utils.getApp().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return;
        }
        bluetoothAdapter = bluetoothManager.getAdapter();

        BleBroadcastUtils.ON_RECEIVE_BLE_STATE_LISTENER_LIST.add(new BleBroadcastUtils.OnReceiveBleStateListener() {
            @Override
            public void onReceiveBleState(String action, BluetoothDevice bluetoothDevice) {
                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        if (mBluetoothDevice == null) {
                            break;
                        }
                        if (!TextUtils.equals(bluetoothDevice.getAddress(), mBluetoothDevice.getAddress())) {
                            break;
                        }

                        //蓝牙已连接
                        if (mConnectionState == STATE_CONNECTED) {
                            break;
                        }
                        mConnectionState = STATE_CONNECTED;
                        notifyBleState(ACTION_GATT_CONNECTED);
                        // 搜索GATT服务
                        mBluetoothGatt.discoverServices();
                        break;
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        closeBle();
                        //蓝牙已断开
                        if (mConnectionState == STATE_DISCONNECTED) {
                            break;
                        }
                        mConnectionState = STATE_DISCONNECTED;
                        notifyBleState(ACTION_GATT_DISCONNECTED);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * 蓝牙连接
     *
     * @return true：成功 false：
     */
    public boolean connect() {
        if (mBluetoothDevice == null) {
            return false;
        }
        mBluetoothGatt = mBluetoothDevice.connectGatt(Utils.getApp(), false, mGattCallback);
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public boolean isConnected() {
        return mConnectionState == STATE_CONNECTED;
    }

    public boolean isConnecting() {
        return mConnectionState == STATE_CONNECTING;
    }

    public boolean isDisconnected() {
        return mConnectionState == STATE_DISCONNECTED;
    }

    /**
     * 蓝牙断开连接
     */
    public void disconnect() {
        mConnectionState = STATE_DISCONNECTED;
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * 执行gatt.disconnect()后立即执行gatt.close(),会导致gattCallback无法收到STATE_DISCONNECTED的状态。
     * gattCallback收到STATE_DISCONNECTED后再执行gatt.close();，这样逻辑上会更清析一些。
     */
    public void closeBle() {
        mConnectionState = STATE_DISCONNECTED;
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * 发送数据
     *
     * @param data 数据
     * @return true：发送成功 false：发送失败
     */
    public boolean writeData(byte[] data) {
        // 获取蓝牙设备的服务
        BluetoothGattService gattService = null;
        if (mBluetoothGatt != null) {
            gattService = mBluetoothGatt.getService(SERVICE_UUID.getUuid());
        }
        if (gattService == null) {
            return false;
        }

        // 获取蓝牙设备的特征
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(CHARACTERISTIC_WRITE_UUID.getUuid());
        if (gattCharacteristic == null) {
            return false;
        }

        // 发送数据
        gattCharacteristic.setValue(data);
        return mBluetoothGatt.writeCharacteristic(gattCharacteristic);
    }

    /**
     * 发送通知
     *
     * @param action 广播Action
     */
    public void notifyBleState(String action) {
        MyBleData myBleData = new MyBleData(action);
        for (OnMyBleCallback onMyBleCallback : onMyBleCallbackList) {
            onMyBleCallback.onMyBle(myBleData);
        }
    }

    public void addMyBleCallback(OnMyBleCallback onMyBleCallback) {
        onMyBleCallbackList.add(onMyBleCallback);
    }

    public void removeMyBleCallback(OnMyBleCallback onMyBleCallback) {
        onMyBleCallbackList.remove(onMyBleCallback);
    }

    public BluetoothDevice getBleDevice() {
        return mBluetoothDevice;
    }

    public void setBleDevice(BluetoothDevice bleDevice) {
        this.mBluetoothDevice = bleDevice;
    }

    public String getDeviceName() {
        if (mBluetoothDevice == null) {
            return null;
        }

        return mBluetoothDevice.getName();
    }

    public String getDeviceAddress() {
        if (mBluetoothDevice == null) {
            return null;
        }

        return mBluetoothDevice.getAddress();
    }

    public static BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) Utils.getApp().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return null;
        }
        return bluetoothManager.getAdapter();
    }

    public static boolean isEnable() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }
        return bluetoothAdapter.isEnabled();
    }

    public static void enable() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
        if (bluetoothAdapter.isEnabled()) {
            return;
        }
        bluetoothAdapter.enable();
    }

    /**
     * 设置蓝牙设备在数据改变时，通知App
     */
    private void setBleNotification() {
        if (mBluetoothGatt == null) {
            notifyBleState(ACTION_CONNECTING_FAIL);
            return;
        }

        // 获取蓝牙设备的服务
        BluetoothGattService gattService = mBluetoothGatt.getService(SERVICE_UUID.getUuid());
        if (gattService == null) {
            notifyBleState(ACTION_CONNECTING_FAIL);
            return;
        }

        // 获取蓝牙设备的特征
        BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(CHARACTERISTIC_READ_UUID.getUuid());
        if (gattCharacteristic == null) {
            notifyBleState(ACTION_CONNECTING_FAIL);
            return;
        }

        // 获取蓝牙设备特征的描述符
        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(DESCRIPTOR_UUID.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (mBluetoothGatt.writeDescriptor(descriptor)) {
            // 蓝牙设备在数据改变时，通知App，App在收到数据后回调onCharacteristicChanged方法
            mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
        }
    }

    /**
     * 发送通知
     *
     * @param action         广播Action
     * @param characteristic 数据
     */
    private void notifyBleData(String action, BluetoothGattCharacteristic characteristic) {
        MyBleData myBleData;
        if (CHARACTERISTIC_READ_UUID.equals(characteristic.getUuid())) {
            myBleData = new MyBleData(action, characteristic.getValue());
        } else {
            myBleData = new MyBleData(action);
        }
        for (OnMyBleCallback onMyBleCallback : onMyBleCallbackList) {
            onMyBleCallback.onMyBle(myBleData);
        }
    }

    private static ParcelUuid getParcelUuid(String uuid16Bit) {
        return ParcelUuid.fromString("0000" + uuid16Bit + "-0000-1000-8000-00805F9B34FB");
    }

    public static class MyBleData {
        String myBleAction;
        byte[] myBleData;

        public MyBleData(String myBleAction) {
            this.myBleAction = myBleAction;
        }

        public MyBleData(String myBleAction, byte[] myBleData) {
            this.myBleAction = myBleAction;
            this.myBleData = myBleData;
        }

        public String getMyBleAction() {
            return myBleAction;
        }

        public byte[] getMyBleData() {
            return myBleData;
        }
    }

    public static abstract class OnMyBleCallback {
        public void onMyBle(MyBleData myBleData) {
        }
    }
}
