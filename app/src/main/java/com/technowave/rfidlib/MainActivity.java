package com.technowave.rfidlib;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.technowave.techno_rfid.NordicId.AccessoryExtension;
import com.technowave.techno_rfid.NordicId.NurApi;
import com.technowave.techno_rfid.NurApi.NurApiAutoConnectTransport;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private final int APP_PERMISSION_REQ_CODE = 41;

    private NurApiAutoConnectTransport hAcTr;
    private static NurApi mNurApi;
    private static AccessoryExtension mAccExt; //accessories of reader like barcode scanner, beeper, vibration..

    //Need to keep track connection state with NurApi IsConnected
    private boolean mIsConnected;
    private String[] permissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    private void requestPermission() {
        /** Bluetooth Permission checks **/
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) {

            } else {
                ActivityCompat.requestPermissions(this, permissions, APP_PERMISSION_REQ_CODE);
            }
        }
    }
}