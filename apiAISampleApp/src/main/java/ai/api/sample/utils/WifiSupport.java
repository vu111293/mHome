package ai.api.sample.utils;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ACER on 11/5/2017.
 */

public class WifiSupport {

    private WifiReceiver receiverWifi;
    private WifiManager wifi;
    private int size = 0;
    private List<ScanResult> results;
    private Context mContext;
    private IWifiSupport mListener;

    public interface IWifiSupport {
        void onScanned(List<ScanResult> list);

        void onWifiChanged(String name, String ip);
    }

    public void init(Context context, IWifiSupport listener) {
        mContext = context;
        mListener = listener;
        wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();
        context.registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (wifi.isWifiEnabled() == false) {
            wifi.setWifiEnabled(true);
        }
        wifi.startScan();
    }

    public void scan() {
        wifi.startScan();
    }

    public List<ScanResult> getWifiList() {
        return scanResultList;
    }

    public void registerReceiver() {
        mContext.registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void unregisterReceiver() {
        mContext.unregisterReceiver(receiverWifi);
    }

    public String getScanResultSecurity(ScanResult scanResult) {
        Log.i("@@", "* getScanResultSecurity");

        final String cap = scanResult.capabilities;
        final String[] securityModes = {"WEP", "PSK", "EAP"};

        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return "OPEN";
    }

    private List<ScanResult> scanResultList = new ArrayList<>();

    class WifiReceiver extends BroadcastReceiver {
        StringBuilder sb = new StringBuilder();

        public void onReceive(Context c, Intent intent) {

            ArrayList<String> connections = new ArrayList<String>();
            ArrayList<Float> Signal_Strenth = new ArrayList<Float>();

            scanResultList.clear();
            sb = new StringBuilder();
            List<ScanResult> wifiList;
            wifiList = wifi.getScanResults();
            scanResultList = wifi.getScanResults();
            for (int i = 0; i < wifiList.size(); i++) {
                connections.add(wifiList.get(i).SSID);
            }

            Log.d("@@", "number connection: " + connections.size());
            // showWifiList();
            if (mRunSetting) {
                connectToAP(mSSIDLast, mPassLast);
                mRunSetting = false;
            }

            if (mListener != null) {
                mListener.onScanned(scanResultList);
            }

            updateWifiStatus();
        }

        private void updateWifiStatus() {
            WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            String ssid = info.getSSID();
            String ipAddress = Formatter.formatIpAddress(info.getIpAddress());
            if (mListener != null) {
                mListener.onWifiChanged(ssid, ipAddress);
            }
        }
    }


    private boolean mRunSetting = false;
    private String mSSIDLast;
    private String mPassLast;

    public void requireConnecting(String ssid, String passkey) {
        mSSIDLast = ssid;
        mPassLast = passkey;
        mRunSetting = true;
        scan();
    }

    public void connectToAP(String ssid, String passkey) {
        Log.i("@@", "* connectToAP");

        WifiConfiguration wifiConfiguration = new WifiConfiguration();

        String networkSSID = ssid;
        String networkPass = passkey;

        Log.d("@@", "# password " + networkPass);

        for (ScanResult result : scanResultList) {
            if (result.SSID.equals(networkSSID)) {

                String securityMode = getScanResultSecurity(result);

                if (securityMode.equalsIgnoreCase("OPEN")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    int res = wifi.addNetwork(wifiConfiguration);
                    Log.d("@@", "# add Network returned " + res);

                    boolean b = wifi.enableNetwork(res, true);
                    Log.d("@@", "# enableNetwork returned " + b);

                    wifi.setWifiEnabled(true);

                } else if (securityMode.equalsIgnoreCase("WEP")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\"";
                    wifiConfiguration.wepTxKeyIndex = 0;
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    int res = wifi.addNetwork(wifiConfiguration);
                    Log.d("@@", "### 1 ### add Network returned " + res);

                    boolean b = wifi.enableNetwork(res, true);
                    Log.d("@@", "# enableNetwork returned " + b);

                    wifi.setWifiEnabled(true);
                }

                wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
                wifiConfiguration.hiddenSSID = true;
                wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

                int res = wifi.addNetwork(wifiConfiguration);
                Log.d("@@", "### 2 ### add Network returned " + res);

                wifi.enableNetwork(res, true);

                boolean changeHappen = wifi.saveConfiguration();

                if (res != -1 && changeHappen) {
                    Log.d("@@", "### Change happen");

                    // AppStaticVar.connectedSsidName = networkSSID;

                } else {
                    Log.d("@@", "*** Change NOT happen");
                }

                wifi.setWifiEnabled(true);
            }
        }
    }

    public void showWifiList() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
//        builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle("Wifi list:");
        if (scanResultList == null || scanResultList.size() == 0) {
            builderSingle.setMessage("Empty!");
        } else {
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(mContext, android.R.layout.select_dialog_singlechoice);
            for (ScanResult item : scanResultList) {
                arrayAdapter.add(item.SSID + ": " + item.level);
            }
            builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int pos) {
                    connectToAP(scanResultList.get(pos).SSID, "Frozen2204");
                }
            });
        }
        builderSingle.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builderSingle.show();
    }


}
