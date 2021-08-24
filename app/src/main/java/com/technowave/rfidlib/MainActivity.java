package com.technowave.rfidlib;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.technowave.techno_rfid.Beeper;
import com.technowave.techno_rfid.NordicId.AccessoryExtension;
import com.technowave.techno_rfid.NordicId.NurApi;
import com.technowave.techno_rfid.NordicId.NurApiListener;
import com.technowave.techno_rfid.NordicId.NurEventAutotune;
import com.technowave.techno_rfid.NordicId.NurEventClientInfo;
import com.technowave.techno_rfid.NordicId.NurEventDeviceInfo;
import com.technowave.techno_rfid.NordicId.NurEventEpcEnum;
import com.technowave.techno_rfid.NordicId.NurEventFrequencyHop;
import com.technowave.techno_rfid.NordicId.NurEventIOChange;
import com.technowave.techno_rfid.NordicId.NurEventInventory;
import com.technowave.techno_rfid.NordicId.NurEventNxpAlarm;
import com.technowave.techno_rfid.NordicId.NurEventProgrammingProgress;
import com.technowave.techno_rfid.NordicId.NurEventTagTrackingChange;
import com.technowave.techno_rfid.NordicId.NurEventTagTrackingData;
import com.technowave.techno_rfid.NordicId.NurEventTraceTag;
import com.technowave.techno_rfid.NordicId.NurEventTriggeredRead;
import com.technowave.techno_rfid.NordicId.NurRespReaderInfo;
import com.technowave.techno_rfid.NordicId.NurTag;
import com.technowave.techno_rfid.NordicId.NurTagStorage;
import com.technowave.techno_rfid.NurApi.BleScanner;
import com.technowave.techno_rfid.NurApi.NurApiAutoConnectTransport;
import com.technowave.techno_rfid.NurApi.NurDeviceListActivity;
import com.technowave.techno_rfid.NurApi.NurDeviceSpec;

public class MainActivity extends AppCompatActivity implements NurApiListener {
    private static final String TAG = "MainActivity";


    //This is true while searching single tag operation ongoing.
    boolean mSingleTagDoTask;
    //Need to keep track when state change on trigger button
    private boolean mTriggerDown;
    //UI
    private TextView mResultTextView;
    private TextView mStatusTextView;
    private TextView mEPCTextView;
    private ToggleButton mInvStreamButton;

    //This demo (Inventory stream) just counts different tags found
    private int mTagsAddedCounter;
    //These values will be shown in the UI
    private String mUiStatusMsg;
    private String mUiResultMsg;
    private String mUiEpcMsg;

    private int mUiStatusColor;
    private int mUiResultColor;
    private int mUiEpcColor;


    private final int APP_PERMISSION_REQ_CODE = 41;

    //When connected, this flag is set depending if Accessories like barcode scan, beep etc supported.
    private static boolean mIsAccessorySupported;

    static boolean mShowingSmartPair = false;
    static boolean mAppPaused = false;

    private Button mConnectButton;
    private TextView mConnectionStatusTextView;

    //These values will be shown in the UI
    private String mUiConnStatusText;
    private int mUiConnStatusTextColor;
    private String mUiConnButtonText;

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

        mInvStreamButton = (ToggleButton)findViewById(R.id.toggleButtonInvStream);
        mStatusTextView = (TextView)findViewById(R.id.text_status);
        mResultTextView = (TextView)findViewById(R.id.text_result);
        mEPCTextView = (TextView)findViewById(R.id.text_epc);

        mConnectButton = findViewById(R.id.buttonConnect);
        mConnectionStatusTextView = findViewById(R.id.textViewStatus);

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConnectClick();
            }
        });

        requestPermission();

        //Init beeper to make some noise at various situations
        Beeper.init(this);
        Beeper.setEnabled(true);

        //Bluetooth LE scanner need to find EXA's near
        BleScanner.init(this);

        mIsConnected = false;

        //Create NurApi handle.
        mNurApi = new NurApi();

        //Accessory extension contains device specific API like barcode read, beep etc..
        //This included in NurApi.jar
        mAccExt = new AccessoryExtension(mNurApi);

        // In this activity, we use mNurApiListener for receiving events
        mNurApi.setListener(this);

        mTriggerDown = false;
        mSingleTagDoTask = false;
        mTagsAddedCounter = 0;


        mInvStreamButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    StartInventoryStream();
                } else {
                    StopInventoryStream();
                }
                showInventory();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Inventory onStart ");
        mUiEpcMsg="EPC";
        mUiResultMsg = "Result";
        mUiStatusMsg = "Waiting button press...";
        mUiStatusColor = Color.BLACK;
        mUiResultColor = Color.BLACK;
        mUiEpcColor = Color.BLACK;
        showOnUI();
        showInventory();
    }

    private void showInventory() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mResultTextView.setText(mUiResultMsg);
                mResultTextView.setTextColor(mUiResultColor);
                mStatusTextView.setText(mUiStatusMsg);
                mStatusTextView.setTextColor(mUiStatusColor);
                mEPCTextView.setText(mUiEpcMsg);
                mEPCTextView.setTextColor(mUiEpcColor);
            }
        });
    }

    /**
     * Stop streaming.
     */
    private void StopInventoryStream()
    {
        try {
            if (mNurApi.isInventoryStreamRunning())
                mNurApi.stopInventoryStream();
            mTriggerDown = false;
            mUiStatusMsg = "Waiting button press...";
            mUiStatusColor = Color.BLACK;
        }
        catch (Exception ex)
        {
            mUiResultMsg = ex.getMessage();
            mUiResultColor = Color.RED;
        }
    }



    private void StartInventoryStream()
    {
        if(mSingleTagDoTask  || mTriggerDown) {
            mInvStreamButton.setChecked(false);
            return; //Already running tasks so let's not disturb that operation.
        }
        try {
            mNurApi.clearIdBuffer(); //This command clears all tag data currently stored into the module’s memory as well as the API's internal storage.
            mNurApi.startInventoryStream(); //Kick inventory stream on. Now inventoryStreamEvent handler offers inventory results.
            mTriggerDown = true; //Flag to indicate inventory stream running
            mTagsAddedCounter = 0;
            mUiResultMsg = "Tags:" + String.valueOf(mTagsAddedCounter);
            mUiStatusMsg = "Streaming...";
        }
        catch (Exception ex)
        {
            mUiResultMsg = ex.getMessage();
        }
    }

    public void onConnectClick() {
        if (mNurApi.isConnected()) {
            hAcTr.dispose();
            hAcTr = null;
        } else {
            Toast.makeText(MainActivity.this, "Start searching. Make sure device power ON!", Toast.LENGTH_LONG).show();
            NurDeviceListActivity.startDeviceRequest(MainActivity.this, mNurApi);
        }
    }

    void showConnecting() {
        if (hAcTr != null) {
            mUiConnStatusText = "Connecting to " + hAcTr.getAddress();
            mUiConnStatusTextColor = Color.YELLOW;
        } else {
            mUiConnStatusText = "Disconnected";
            mUiConnStatusTextColor = Color.RED;
            mUiConnButtonText = "CONNECT";
        }
        showOnUI();
    }

    private void showOnUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionStatusTextView.setText(mUiConnStatusText);
                mConnectionStatusTextView.setTextColor(mUiConnStatusTextColor);
                mConnectButton.setText(mUiConnButtonText);
            }
        });
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

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case NurDeviceListActivity.REQUEST_SELECT_DEVICE: {
                if (data == null || resultCode != NurDeviceListActivity.RESULT_OK)
                    return;

                try {
                    NurDeviceSpec spec = new NurDeviceSpec(data.getStringExtra(NurDeviceListActivity.SPECSTR));

                    if (hAcTr != null) {
                        System.out.println("Dispose transport");
                        hAcTr.dispose();
                    }

                    String strAddress;
                    hAcTr = NurDeviceSpec.createAutoConnectTransport(this, mNurApi, spec);
                    strAddress = spec.getAddress();
                    Log.i(TAG, "Dev selected: code = " + strAddress);
                    hAcTr.setAddress(strAddress);

                    showConnecting();

                    //If you want connect to same device automatically later on, you can save 'strAddress" and use that for connecting at app startup for example.
                    //saveSettings(spec);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            break;
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    boolean showSmartPairUI() {
        if (mNurApi.isConnected() || mAppPaused)
            return false;

        try {
            Log.d(TAG, "showSmartPairUI()");
            Intent startIntent = new Intent(this, Class.forName("com.nordicid.smartpair.SmartPairConnActivity"));
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void triggeredReadEvent(NurEventTriggeredRead event) {
    }

    @Override
    public void traceTagEvent(NurEventTraceTag event) {
    }

    @Override
    public void programmingProgressEvent(NurEventProgrammingProgress event) {
    }

    @Override
    public void nxpEasAlarmEvent(NurEventNxpAlarm event) {
    }

    @Override
    public void logEvent(int level, String txt) {
    }

    @Override
    public void inventoryStreamEvent(NurEventInventory event) {
        try {
            if (event.stopped) {
                //InventoryStreaming is not active for ever. It automatically stopped after ~20 sec but it can be started again immediately if needed.
                //check if need to restart streaming
                if (mTriggerDown)
                    mNurApi.startInventoryStream(); //Trigger button still down so start it again.

            } else {

                if(event.tagsAdded>0) {
                    //At least one new tag found
                    if(mIsAccessorySupported)
                        mAccExt.beepAsync(20); //Beep on device
                    else
                        Beeper.beep(Beeper.BEEP_40MS); //Cannot beep on device so we beep on phone

                    NurTagStorage tagStorage = mNurApi.getStorage(); //Storage contains all tags found

                    //Iterate just received tags based on event.tagsAdded
                    for(int x=mTagsAddedCounter;x<mTagsAddedCounter+event.tagsAdded;x++) {
                        //Real application should handle all tags iterated here.
                        //But this just show how to get tag from storage.
                        String epcString;
                        NurTag tag = tagStorage.get(x);
                        epcString = NurApi.byteArrayToHexString(tag.getEpc());
                        //showing just EPC of last tag
                        mUiEpcMsg = epcString;
                    }

                    //Finally show count of tags found
                    mTagsAddedCounter += event.tagsAdded;
                    mUiResultMsg = "Tags:" + String.valueOf(mTagsAddedCounter);
                    mUiResultColor = Color.rgb(0, 128, 0);
                    showInventory(); //Show results on UI
                }
            }
        }
        catch (Exception ex)
        {
            //mStatusTextView.setText(ex.getMessage());
            // mUiStatusColor = Color.RED;
            showInventory();
        }
    }

    @Override
    public void inventoryExtendedStreamEvent(NurEventInventory event) {
    }

    @Override
    public void frequencyHopEvent(NurEventFrequencyHop event) {
    }

    @Override
    public void epcEnumEvent(NurEventEpcEnum event) {
    }

    @Override
    public void disconnectedEvent() {
        mIsConnected = false;
        Log.i(TAG, "Disconnected!");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Reader disconnected", Toast.LENGTH_SHORT).show();
                showConnecting();

                // Show smart pair ui
                if (!mShowingSmartPair && hAcTr != null) {
                    String clsName = hAcTr.getClass().getSimpleName();
                    if (clsName.equals("NurApiSmartPairAutoConnect")) {
                        mShowingSmartPair = showSmartPairUI();
                    }
                } else {
                    mShowingSmartPair = false;
                }
            }
        });
    }

    @Override
    public void deviceSearchEvent(NurEventDeviceInfo event) {
    }

    @Override
    public void debugMessageEvent(String event) {
    }

    @Override
    public void connectedEvent() {
        //Device is connected.
        // Let's find out is device provided with accessory support (Barcode reader, battery info...) like EXA
        try {
            if (mAccExt.isSupported()) {
                //Yes. Accessories supported
                mIsAccessorySupported = true;
                //Let's take name of device from Accessory api
                mUiConnStatusText = "Connected to " + mAccExt.getConfig().name;
            } else {
                //Accessories not supported. Probably fixed reader.
                mIsAccessorySupported = false;
                NurRespReaderInfo ri = mNurApi.getReaderInfo();
                mUiConnStatusText = "Connected to " + ri.name;
            }
        } catch (Exception ex) {
            mUiConnStatusText = ex.getMessage();
        }

        mIsConnected = true;
        Log.i(TAG, "Connected!");
        Beeper.beep(Beeper.BEEP_100MS);

        mUiConnStatusTextColor = Color.GREEN;
        mUiConnButtonText = "DISCONNECT";
        showOnUI();
    }

    @Override
    public void clientDisconnectedEvent(NurEventClientInfo event) {
    }

    @Override
    public void clientConnectedEvent(NurEventClientInfo event) {
    }

    @Override
    public void bootEvent(String event) {
    }

    @Override
    public void IOChangeEvent(NurEventIOChange event) {
        Log.i(TAG, "Key " + event.source);
    }

    @Override
    public void autotuneEvent(NurEventAutotune event) {
    }

    @Override
    public void tagTrackingScanEvent(NurEventTagTrackingData event) {
    }

    //@Override
    public void tagTrackingChangeEvent(NurEventTagTrackingChange event) {
    }
}