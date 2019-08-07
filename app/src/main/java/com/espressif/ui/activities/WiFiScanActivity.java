package com.espressif.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.espressif.AppConstants;
import com.espressif.provision.Provision;
import com.espressif.provision.R;
import com.espressif.provision.security.Security;
import com.espressif.provision.security.Security0;
import com.espressif.provision.security.Security1;
import com.espressif.provision.session.Session;
import com.espressif.provision.transport.ResponseListener;
import com.espressif.provision.transport.SoftAPTransport;
import com.espressif.provision.transport.Transport;
import com.espressif.ui.adapters.WiFiListAdapter;
import com.espressif.ui.models.WiFiAccessPoint;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;

import espressif.WifiScan;

public class WiFiScanActivity extends AppCompatActivity {

    private static final String TAG = WiFiScanActivity.class.getSimpleName();

    private ProgressBar progressBar;
    private ArrayList<WiFiAccessPoint> apDevices;
    private WiFiListAdapter adapter;
    public static Session session;
    public Security security;
    public Transport transport;
    private Intent intent;
    private int totalCount;
    private int startIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_scan_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_wifi_scan_list);
        setSupportActionBar(toolbar);

        ListView listView = findViewById(R.id.wifi_ap_list);
        progressBar = findViewById(R.id.wifi_progress_indicator);

        progressBar.setVisibility(View.VISIBLE);
        session = null;

        apDevices = new ArrayList<>();
        intent = getIntent();
        final String pop = intent.getStringExtra(AppConstants.KEY_PROOF_OF_POSSESSION);
        Log.d(TAG, "POP : " + pop);
        final String baseUrl = intent.getStringExtra(Provision.CONFIG_BASE_URL_KEY);
        final String transportVersion = intent.getStringExtra(Provision.CONFIG_TRANSPORT_KEY);
        final String securityVersion = intent.getStringExtra(Provision.CONFIG_SECURITY_KEY);

        adapter = new WiFiListAdapter(this, R.id.tv_wifi_name, apDevices);

        // Assign adapter to ListView
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {

                progressBar.setVisibility(View.VISIBLE);
                Log.d(TAG, "Device to be connected -" + apDevices.get(pos));
                callProvision(apDevices.get(pos).getWifiName(), apDevices.get(pos).getSecurity());
            }
        });

        listView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

            }
        });

        if (securityVersion.equals(Provision.CONFIG_SECURITY_SECURITY1)) {
            this.security = new Security1(pop);
        } else {
            this.security = new Security0();
        }

        if (transportVersion.equals(Provision.CONFIG_TRANSPORT_WIFI)) {

            transport = new SoftAPTransport(baseUrl);

        } else if (transportVersion.equals(Provision.CONFIG_TRANSPORT_BLE)) {

            if (BLEProvisionLanding.bleTransport == null) {

            } else {
                transport = BLEProvisionLanding.bleTransport;
            }
        }
        fetchScanList();
    }

    @Override
    public void onBackPressed() {
        BLEProvisionLanding.isBleWorkDone = true;
        super.onBackPressed();
    }

    private void fetchScanList() {

        Log.d(TAG, "Fetch Scan List");

        session = new Session(this.transport, this.security);
        session.sessionListener = new Session.SessionListener() {

            @Override
            public void OnSessionEstablished() {
                Log.e(TAG, "Session established");
                startWifiScan();
            }

            @Override
            public void OnSessionEstablishFailed(Exception e) {
                Log.e(TAG, "Session failed");
                e.printStackTrace();
            }
        };
        session.init(null);
    }

    private void startWifiScan() {

        WifiScan.CmdScanStart configRequest = WifiScan.CmdScanStart.newBuilder()
                .setBlocking(true)
                .setPassive(false)
                .setGroupChannels(0)
                .setPeriodMs(120)
                .build();
        WifiScan.WiFiScanMsgType msgType = WifiScan.WiFiScanMsgType.TypeCmdScanStart;
        WifiScan.WiFiScanPayload payload = WifiScan.WiFiScanPayload.newBuilder()
                .setMsg(msgType)
                .setCmdScanStart(configRequest)
                .build();

        byte[] data = this.security.encrypt(payload.toByteArray());
        transport.sendConfigData(AppConstants.HANDLER_PROV_SCAN, data, new ResponseListener() {

            @Override
            public void onSuccess(byte[] returnData) {

                processStartScan(returnData);
                Log.d(TAG, "Successfully sent start scan");
                getWifiScanStatus();
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    private void processStartScan(byte[] responseData) {

        byte[] decryptedData = this.security.decrypt(responseData);

        try {
            WifiScan.WiFiScanPayload payload = WifiScan.WiFiScanPayload.parseFrom(decryptedData);
            WifiScan.RespScanStart response = WifiScan.RespScanStart.parseFrom(payload.toByteArray());
            // TODO Proto should send status as ok started or failed
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    private void getWifiScanStatus() {

        WifiScan.CmdScanStatus configRequest = WifiScan.CmdScanStatus.newBuilder()
                .build();
        WifiScan.WiFiScanMsgType msgType = WifiScan.WiFiScanMsgType.TypeCmdScanStatus;
        WifiScan.WiFiScanPayload payload = WifiScan.WiFiScanPayload.newBuilder()
                .setMsg(msgType)
                .setCmdScanStatus(configRequest)
                .build();
        byte[] data = this.security.encrypt(payload.toByteArray());
        transport.sendConfigData(AppConstants.HANDLER_PROV_SCAN, data, new ResponseListener() {
            @Override
            public void onSuccess(byte[] returnData) {
                Log.d(TAG, "Successfully got scan result");
                processGetWifiStatus(returnData);
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    private void processGetWifiStatus(byte[] responseData) {

        byte[] decryptedData = this.security.decrypt(responseData);

        try {
            WifiScan.WiFiScanPayload payload = WifiScan.WiFiScanPayload.parseFrom(decryptedData);
            WifiScan.RespScanStatus response = payload.getRespScanStatus();

            boolean scanFinished = response.getScanFinished();

            if (scanFinished) {
                totalCount = response.getResultCount();
                getFullWiFiList();
            } else {
                // TODO Error case
            }

        } catch (InvalidProtocolBufferException e) {

            e.printStackTrace();
        }
    }

    private void getFullWiFiList() {

        Log.e(TAG, "Total count : " + totalCount + " and start index is : " + startIndex);
        if (totalCount < 4) {

            getWiFiScanList(0, totalCount);

        } else {

            int temp = totalCount - startIndex;

            if (temp > 0) {

                if (temp > 4) {
                    getWiFiScanList(startIndex, 4);
                } else {
                    getWiFiScanList(startIndex, temp);
                }

            } else {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        // Add "Join network" Option as a list item
                        WiFiAccessPoint wifiAp = new WiFiAccessPoint();
                        wifiAp.setWifiName(getString(R.string.join_other_network));
                        apDevices.add(wifiAp);

                        progressBar.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    private void getWiFiScanList(int start, int count) {

        Log.d("WIFIScanList", "Getting " + count + " SSIDs");
        WifiScan.CmdScanResult configRequest = WifiScan.CmdScanResult.newBuilder()
                .setStartIndex(start)
                .setCount(count)
                .build();
        WifiScan.WiFiScanMsgType msgType = WifiScan.WiFiScanMsgType.TypeCmdScanResult;
        WifiScan.WiFiScanPayload payload = WifiScan.WiFiScanPayload.newBuilder()
                .setMsg(msgType)
                .setCmdScanResult(configRequest)
                .build();
        byte[] data = this.security.encrypt(payload.toByteArray());
        transport.sendConfigData(AppConstants.HANDLER_PROV_SCAN, data, new ResponseListener() {
            @Override
            public void onSuccess(byte[] returnData) {
                Log.d(TAG, "Successfully got SSID list");
                processGetSSIDs(returnData);
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    private void processGetSSIDs(byte[] responseData) {

        byte[] decryptedData = this.security.decrypt(responseData);
        try {

            WifiScan.WiFiScanPayload payload = WifiScan.WiFiScanPayload.parseFrom(decryptedData);
            final WifiScan.RespScanResult response = payload.getRespScanResult();

            Log.e(TAG, "Response count : " + response.getEntriesCount());

            for (int i = 0; i < response.getEntriesCount(); i++) {
                Log.e(TAG, "SSID : " + response.getEntries(i).getSsid().toStringUtf8());
            }

            runOnUiThread(new Runnable() {

                public void run() {

                    // Do your modifications here

                    for (int i = 0; i < response.getEntriesCount(); i++) {

                        Log.e(TAG, "Response : " + response.getEntries(i).getSsid().toStringUtf8());
                        String ssid = response.getEntries(i).getSsid().toStringUtf8();
                        int rssi = response.getEntries(i).getRssi();
                        boolean isAvailable = false;

                        for (int index = 0; index < apDevices.size(); index++) {

                            if (ssid.equals(apDevices.get(index).getWifiName())) {

                                isAvailable = true;

                                if (apDevices.get(index).getRssi() < rssi) {

                                    apDevices.get(index).setRssi(rssi);
                                }
                                break;
                            }
                        }

                        if (!isAvailable) {

                            WiFiAccessPoint wifiAp = new WiFiAccessPoint();

                            wifiAp.setWifiName(ssid);
                            wifiAp.setRssi(response.getEntries(i).getRssi());
                            wifiAp.setSecurity(response.getEntries(i).getAuthValue());
                            apDevices.add(wifiAp);
                        }

                        Log.e(TAG, "Size of  list : " + apDevices.size());
                    }

                    startIndex = startIndex + 4;
                    getFullWiFiList();
                }
            });

        } catch (InvalidProtocolBufferException e) {

            e.printStackTrace();
        }
    }

    private void callProvision(String ssid, int security) {

        Log.e(TAG, "Selected AP -" + ssid);
        finish();
        Intent launchProvisionInstructions = new Intent(getApplicationContext(), ProvisionActivity.class);
        launchProvisionInstructions.putExtras(getIntent());

        if (!ssid.equals(getString(R.string.join_other_network))) {
            launchProvisionInstructions.putExtra(Provision.PROVISIONING_WIFI_SSID, ssid);
            launchProvisionInstructions.putExtra(AppConstants.KEY_WIFI_SECURITY_TYPE, security);
        }
        startActivity(launchProvisionInstructions);
    }
}