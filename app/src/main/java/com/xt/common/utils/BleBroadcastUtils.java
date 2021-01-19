package com.xt.common.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.blankj.utilcode.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class BleBroadcastUtils {
    public static final List<OnReceiveBleStateListener> ON_RECEIVE_BLE_STATE_LISTENER_LIST = new ArrayList<>();

    public static void registerReceiver() {
        {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
            intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
            Utils.getApp().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (action) {
                        case BluetoothDevice.ACTION_ACL_CONNECTED:
                            //蓝牙已连接
                            break;
                        case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                            //蓝牙已断开
                            break;
                        case BluetoothAdapter.ACTION_STATE_CHANGED:
                            //蓝牙开启或关闭
                            break;
                        default:
                            break;
                    }

                    for (OnReceiveBleStateListener onReceiveBleStateListener : ON_RECEIVE_BLE_STATE_LISTENER_LIST) {
                        onReceiveBleStateListener.onReceiveBleState(action, device);
                    }
                }
            }, intentFilter);
        }
    }

    public interface OnReceiveBleStateListener {
        void onReceiveBleState(String action, BluetoothDevice bluetoothDevice);
    }
}
