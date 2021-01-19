package com.xt.samplebleconnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.view.View;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;
import com.xt.common.BleManager;
import com.xt.common.ble.connect.MyBleUtils;
import com.xt.common.ble.connect.MyScanUtils;
import com.xt.common.utils.BleBroadcastUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BleManager.getInstance().doConnect();
            }
        });
        aa();
    }

    private void aa() {
        BleBroadcastUtils.registerReceiver();
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        if (PermissionUtils.isGranted(permissions)) {
            MyBleUtils.enable();
            BleManager.getInstance().getMyScanUtils().startBleScan();
            return;
        }
        PermissionUtils.permission(PermissionConstants.LOCATION).callback(new PermissionUtils.SingleCallback() {
            @Override
            public void callback(boolean isAllGranted, @NonNull List<String> granted, @NonNull List<String> deniedForever, @NonNull List<String> denied) {
                if (!isAllGranted) {
                    return;
                }
                MyBleUtils.enable();
                BleManager.getInstance().getMyScanUtils().startBleScan();
            }
        }).request();
    }
}