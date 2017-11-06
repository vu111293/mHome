/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.api.sample.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ai.api.android.AIConfiguration;
import ai.api.android.GsonFactory;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import ai.api.model.Status;
import ai.api.sample.R;
import ai.api.sample.TTS;
import ai.api.sample.TTS.ISpeakListener;
import ai.api.sample.config.Config;
import ai.api.sample.dialog.WifiSelectionDialog;
import ai.api.sample.service.ChatService;
import ai.api.sample.utils.WifiSupport;
import ai.api.ui.AIButton;

public class MainActivity extends AppCompatActivity implements WifiSupport.IWifiSupport,
        AIButton.AIButtonListener, View.OnClickListener, ISpeakListener {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private ListView lvMainChat;
    private EditText etMain;
    private Button btnSend;

    private String connectedDeviceName = null;
    private ArrayAdapter<String> chatArrayAdapter;

    private StringBuffer outStringBuffer;
    private BluetoothAdapter bluetoothAdapter = null;
    private ChatService chatService = null;
    private TextView mTvStatus;
    private WifiSupport mWifi;
    private WifiSelectionDialog dialog;


    // voice recognize
    public static final String SPEAK_KEY = "sample";

    private AIButton aiButton;
    private TextView resultTextView;
    private Gson gson = GsonFactory.getGson();
    private AIConfiguration config;


    private Handler handler = new Handler(new Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to,
                                    connectedDeviceName));
                            chatArrayAdapter.clear();
                            break;
                        case ChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case ChatService.STATE_LISTEN:
                        case ChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;

                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    chatArrayAdapter.add("Me:  " + writeMessage);
                    break;

                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    chatArrayAdapter.add(connectedDeviceName + ":  " + readMessage);
                    if (readMessage.startsWith("cmd")) {
                        parseCmd(readMessage.substring(3));
                    }
                    break;

                case MESSAGE_DEVICE_NAME:
                    connectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(),
                            "Connected to " + connectedDeviceName,
                            Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
            return false;
        }
    });
    private String TAG = MainActivity.class.getSimpleName();

    private void parseCmd(String cmd) {
        if (cmd.startsWith("wifi")) {
            mWifi.showWifiList();
        } else if (cmd.startsWith("connect")) {
            String[] ps = cmd.substring(7).split("/");
            if (ps.length == 2) {
                String ssid = ps[0];
                String pwd = ps[1];
                mWifi.requireConnecting(ssid, pwd);
//                mWifi.connectToAP(ssid, pwd);
//                connectTo(ssid, pwd);
            }
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setupUI();
    }

    private void setupUI() {
        mTvStatus = (TextView) findViewById(R.id.tv_wifi_status);
        findViewById(R.id.btn_scan).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // mWifi.scan();
                mWifi.scan();
                if (dialog == null) {
                    dialog = new WifiSelectionDialog(MainActivity.this);
                    dialog.setListener(new WifiSelectionDialog.IWifiSelection() {
                        @Override
                        public void requestSend(String ssid, String pwd) {
                            Log.d("@@", ssid + " x " + pwd);
                            String cmd = "cmdconnect" + ssid + "/" + pwd;
                            sendMessage(cmd);
                        }
                    });
                }

                if (!dialog.isShowing()) {
                    dialog.show();
                }
            }
        });
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        getWidgetReferences();
        bindEventHandler();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mWifi = new WifiSupport();
        mWifi.init(getApplicationContext(), this);

        setupVoice();
    }

    private void setupVoice() {
//        findViewById(R.id.btn_wifi).setOnClickListener(this);
        resultTextView = (TextView) findViewById(R.id.resultTextView);
        aiButton = (AIButton) findViewById(R.id.micButton);
        config = new AIConfiguration(Config.ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));

        aiButton.initialize(config);
        aiButton.setResultsListener(this);

        TTS.init(this);
        TTS.setSpeak(this);
    }

    private void getWidgetReferences() {
        lvMainChat = (ListView) findViewById(R.id.lvMainChat);
        etMain = (EditText) findViewById(R.id.etMain);
        btnSend = (Button) findViewById(R.id.btnSend);
    }

    private void bindEventHandler() {
        etMain.setOnEditorActionListener(mWriteListener);
        btnSend.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String message = etMain.getText().toString();
                sendMessage(message);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        String address = data.getExtras().getString(
                DeviceListActivity.DEVICE_ADDRESS);
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        chatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.insecure_connect_scan:
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent,
                        REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            case R.id.discoverable:
                ensureDiscoverable();
                return true;
        }
        return false;
    }

    private void ensureDiscoverable() {
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void sendMessage(String message) {
        if (chatService.getState() != ChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatService.write(send);

            outStringBuffer.setLength(0);
            etMain.setText(outStringBuffer);
        }
    }

    private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId,
                                      KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    private final void setStatus(int resId) {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle(subTitle);
    }

    private void setupChat() {
        chatArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        lvMainChat.setAdapter(chatArrayAdapter);

        chatService = new ChatService(this, handler);

        outStringBuffer = new StringBuffer("");
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (chatService == null)
                setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        mWifi.registerReceiver();

        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
        registerReceiver(
                new PairingRequest(), filter);


        super.onResume();

        if (chatService != null) {
            if (chatService.getState() == ChatService.STATE_NONE) {
                chatService.start();
            }
        }

        // use this method to reinit connection to recognition service
        aiButton.resume();
    }

    @Override
    public synchronized void onPause() {
        mWifi.unregisterReceiver();
        super.onPause();
        aiButton.pause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatService != null)
            chatService.stop();
    }

    @Override
    public void onScanned(List<ScanResult> list) {
        refreshWifiList(list);
    }

    @Override
    public void onWifiChanged(String name, String ip) {
        mTvStatus.setText(name + ":" + ip);
    }

    private void refreshWifiList(List<ScanResult> list) {
        List<String> wifis = new ArrayList<>();
        for (ScanResult item : list) {
            wifis.add(item.SSID);
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.updateWifiList(wifis);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {


            default:
                break;
        }
    }

    @Override
    public void onStartRecording() {
        Log.d(TAG, "onStart");
        isRecording = true;
    }

    @Override
    public void onResult(final AIResponse response) {
        runOnUiThread(new Runnable() {
            @TargetApi(Build.VERSION_CODES.GINGERBREAD)
            @Override
            public void run() {
                Log.d(TAG, "onResult");

                resultTextView.setText(gson.toJson(response));

                Log.i(TAG, "Received success response");

                // this is example how to get different parts of result object
                final Status status = response.getStatus();
                Log.i(TAG, "Status code: " + status.getCode());
                Log.i(TAG, "Status type: " + status.getErrorType());

                final Result result = response.getResult();
                Log.i(TAG, "Resolved query: " + result.getResolvedQuery());

                Log.i(TAG, "Action: " + result.getAction());
                final String speech = result.getFulfillment().getSpeech();
                Log.i(TAG, "Speech: " + speech);

                if (speech != null && !speech.isEmpty()) {
                    TTS.speak(speech, SPEAK_KEY);
                } else {
                    TTS.speak("Xin nhắc lại", SPEAK_KEY);
                }


//                final Metadata metadata = result.getMetadata();
//                if (metadata != null) {
//                    Log.i(TAG, "Intent id: " + metadata.getIntentId());
//                    Log.i(TAG, "Intent name: " + metadata.getIntentName());
//                }
//
//                final HashMap<String, JsonElement> params = result.getParameters();
//                if (params != null && !params.isEmpty()) {
//                    Log.i(TAG, "Parameters: ");
//                    for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
//                        Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
//                    }
//                }
            }

        });
    }

    @Override
    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onError");
                resultTextView.setText(error.toString());
            }
        });
        // startRecording();
        aiButton.initialize(config);
        TTS.speak("Xin nhắc lại", SPEAK_KEY);
    }

    @Override
    public void onCancelled() {
    }

    @Override
    public void onStart(String key) {

    }

    @Override
    public void onError(String key) {
        if (key.equals("sample")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startRecording();
                }
            });
        }
    }

    @Override
    public void onDone(String key) {
        if (key.equals("sample")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startRecording();
                }
            });

        }
    }

    Handler recordHandler;
    Runnable recordRunnable;
    boolean isRecording = false;

    private void startRecording() {
        if (recordHandler != null) {
            recordHandler.removeCallbacks(recordRunnable);
            recordHandler = null;
        }

        isRecording = false;
        aiButton.pause();
        recordHandler = new Handler();
        recordRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRecording) {
                    aiButton.pause();
                    aiButton.startListening();
                    recordHandler.postDelayed(recordRunnable, 1000);
                }
            }
        };
        recordHandler.postDelayed(recordRunnable, 1000);
    }

    public static class PairingRequest extends BroadcastReceiver {
        public PairingRequest() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
                try {


                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d("pairDevice()", "Start Pairing...");
//                    Method m = device.getClass().getMethod("createBond", (Class[]) null);
//                    m.invoke(device, (Object[]) null);
//                    Log.d("pairDevice()", "Pairing finished.");
////                    device.setPairingConfirmation(true);
//                    Log.d("@@", "ok");

                    byte[] pin = (byte[]) BluetoothDevice.class.getMethod("convertPinToBytes", String.class).invoke(BluetoothDevice.class, "1234");
                    Method m = device.getClass().getMethod("setPin", byte[].class);
                    m.invoke(device, pin);
                    device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);

//                    int pin=intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
//                    //the pin in case you need to accept for an specific pin
//                    Log.d("PIN", " " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY",0));
//                    //maybe you look for a name or address
//                    Log.d("Bonded", device.getName());
//                    byte[] pinBytes;
//                    pinBytes = (""+pin).getBytes("UTF-8");
//                    device.setPin(pinBytes);
//                    //setPairing confirmation if neeeded
//                    device.setPairingConfirmation(true);
                } catch (Exception e) {
                    Log.d("@@", "fail");
                    e.printStackTrace();
                }
            }
        }
    }


}
