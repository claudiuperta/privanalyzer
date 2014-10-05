package uk.ac.qmul.eecs.privanalyzer.util;

import uk.ac.qmul.eecs.privanalyzer.Session;
import uk.ac.qmul.eecs.privanalyzer.logging.Logger;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class NetworkStatusRecorder {

    private static final String sTag = NetworkStatusRecorder.class.getName();

    private final TelephonyManager mTelephonyManager;
    private final ConnectivityManager mConnectivityManager;
    private final WifiManager mWifiManager;
    
    private boolean mIsStarted = false;
    private long mAllAppsTotalBytesReceived = 0;
    private long mAllAppsTotalBytesSent = 0;

    private boolean mWiFiIsConnected;
    private long mLastWiFiConnected;
    private long mLastWiFiDisconnected;
    
    private long mLastWiFiScanResults;    
    
    public class RSSI {
        public final long timestamp;
        public final long value;
        public RSSI(long timestamp, long value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
    
    public class Status {
        public final long timestamp;
        public final NetworkInfo networkInfo;
        public final WifiInfo wifiInfo;
        public Status(long timestamp, NetworkInfo ninfo, WifiInfo winfo) {
            this.timestamp = timestamp;
            networkInfo = ninfo;
            wifiInfo = winfo;
        }
    }
    
    public class Cell {
        public final long timestamp;
        public final CellLocation cellLocation;
        public Cell(long timestamp, CellLocation cellLocation) {
            this.timestamp = timestamp;
            this.cellLocation = cellLocation;
        }
    }
    
    private List<ScanResult> mScanResults = null;
    private ArrayList<RSSI> mMobileRSSIs;
    private ArrayList<RSSI> mWifiRSSIs;
    private ArrayList<Status> mStatuses;
    private ArrayList<Cell> mCells;
    
    private final PhoneStateListener mStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            long now = System.currentTimeMillis();
            super.onSignalStrengthsChanged(signalStrength);
            
            long rssi;
            if (signalStrength.isGsm()) {
                rssi = Utils.translateGSMRssi(signalStrength.getGsmSignalStrength());
            } else {
                rssi = signalStrength.getCdmaDbm();
            }
            mMobileRSSIs.add(new RSSI(now, rssi));
        }
        @Override
        public void onCellLocationChanged(CellLocation cellLocation) {
            mCells.add(new Cell(System.currentTimeMillis(), cellLocation));
        }
    };
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long now = System.currentTimeMillis();
            
            String action = intent.getAction();
            if (action == null) {
                return;
            } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                int rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
                mWifiRSSIs.add(new RSSI(now, rssi));
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                NetworkInfo ninfo = mConnectivityManager.getActiveNetworkInfo();
                WifiInfo winfo = mWifiManager.getConnectionInfo();
                mStatuses.add(new Status(now, ninfo, winfo));
            } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    mWiFiIsConnected = true;
                    mLastWiFiConnected = System.currentTimeMillis();
                } else {
                    mWiFiIsConnected = false;
                    mLastWiFiDisconnected = System.currentTimeMillis();
                }
            } else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Logger.d(sTag, "Received scan results!");
                mLastWiFiScanResults = System.currentTimeMillis();
                mScanResults = mWifiManager.getScanResults();
            }         
        }
    };
        
    public NetworkStatusRecorder() {
        Context context = Session.getGlobalContext();
        mConnectivityManager = (ConnectivityManager) context
                                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                
        mMobileRSSIs = new ArrayList<RSSI>();
        mWifiRSSIs = new ArrayList<RSSI>();
        mStatuses = new ArrayList<Status>();
        mCells = new ArrayList<Cell>();
    }
    
    public void startRecording() {
        if (mIsStarted) {
            return;
        }
        mIsStarted = true;
        
        // Save the total number of bytes sent/received before the test.
        mAllAppsTotalBytesReceived = TrafficStats.getTotalRxBytes();
        mAllAppsTotalBytesSent = TrafficStats.getTotalTxBytes();
        
        Logger.i(sTag, "start recording");
        
        mTelephonyManager.listen(mStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        mTelephonyManager.listen(mStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        filter.addAction(WifiManager.EXTRA_SUPPLICANT_CONNECTED);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);      
        Session.getGlobalContext().registerReceiver(mReceiver, filter);
        
        mWifiManager.startScan();
    }
    
    public void stopRecording() {
        if (mIsStarted == false) {
            return;
        }
        
        try {
            Logger.i(sTag, "stop recording");
            
            // Get the total number of bytes sent/received after the test
            long allAppsTotalBytesReceived = TrafficStats.getTotalRxBytes();
            long allAppsTotalBytesSent = TrafficStats.getTotalTxBytes();

            long myAppTotalBytesReceived = TrafficStats.getUidRxBytes(
                    Session.getGlobalContext().getApplicationInfo().uid);
            long myAppTotalBytesSent = TrafficStats.getUidTxBytes(
                    Session.getGlobalContext().getApplicationInfo().uid);
  
            mAllAppsTotalBytesReceived = allAppsTotalBytesReceived - mAllAppsTotalBytesReceived;
            mAllAppsTotalBytesSent = allAppsTotalBytesSent - mAllAppsTotalBytesSent;
            
            Logger.d(sTag, "All sent: " + mAllAppsTotalBytesSent + " All received: " + mAllAppsTotalBytesReceived);
            
            mTelephonyManager.listen(mStateListener, PhoneStateListener.LISTEN_NONE);
            Session.getGlobalContext().unregisterReceiver(mReceiver);
        } finally {
            mIsStarted = false;
        }       
    }
    
    public List<RSSI> getWifiRSSIs() {
        return mWifiRSSIs;
    }
    
    public List<RSSI> getMobileRSSIs() {
        return mMobileRSSIs;
    }
    
    public List<Status> getStatuses() {
        return mStatuses;
    }
    
    public List<Cell> getCells() {
        return mCells;
    }
    
    @SuppressWarnings("unchecked")
    public JSONObject getResult() {
        JSONObject result = new JSONObject();
        result.put("timestamp", Utils.ms_to_s(System.currentTimeMillis()));
                   
        JSONArray cells = new JSONArray();
        for (Cell c: mCells) {
            // TODO: this is copied from NetworkStatus. Refactor
            JSONObject cell = new JSONObject();
            if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                GsmCellLocation location = (GsmCellLocation) mTelephonyManager.getCellLocation();
                if (location != null) {
                    cell.put("type", "GSM");
                    cell.put("LAC", location.getLac());
                    cell.put("CID", location.getCid());
                }
            } else {
                CdmaCellLocation location = (CdmaCellLocation) mTelephonyManager.getCellLocation();
                if (location != null) {
                    cell.put("type", "CDMA");
                    cell.put("base_station_id", location.getBaseStationId());
                    cell.put("base_station_latitude", location.getBaseStationLatitude());
                    cell.put("base_station_longitude", location.getBaseStationLongitude());
                    cell.put("network_id", location.getNetworkId());
                    cell.put("system_id", location.getSystemId());
                }
            }
            cells.add(cell);
        }
        result.put("cell_locations", cells);
       
        result.put("connection_type", getConnectionType());
        
        JSONObject wifi = new JSONObject();
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            wifi.put("ssid", wifiInfo.getSSID());
            wifi.put("wifi_bssid", wifiInfo.getBSSID());
            wifi.put("wifi_rssi", wifiInfo.getRssi());
        }
        
        wifi.put("enabled", mWifiManager.isWifiEnabled());
        wifi.put("is_connected", mWiFiIsConnected);
        wifi.put("last_connected", mLastWiFiConnected);
        wifi.put("last_disconnected", mLastWiFiDisconnected);

        wifi.put("last_wifi_scan_results", mLastWiFiScanResults);
        JSONArray wifi_networks = new JSONArray();     
        
        if (mScanResults != null) {
            for (ScanResult r: mScanResults) {
                JSONObject network = new JSONObject();
                network.put("ssid", r.SSID);
                network.put("bssid", r.BSSID);
                network.put("dbmi", r.level);
            
                wifi_networks.add(network);
            }
        }
        wifi.put("scan_result", wifi_networks);
        result.put("wifi_info", wifi);
        
        JSONObject trafficStats = new JSONObject();
        trafficStats.put("all_apps_bytes_sent", mAllAppsTotalBytesSent);
        trafficStats.put("all_apps_bytes_received", mAllAppsTotalBytesReceived);
        result.put("traffic_stats", trafficStats);
        
        result.put("device_info", Session.getPhoneStatus().getDeviceInfo());
        
        return result;
    }
      
    public String getConnectionType() {
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        if (info == null) {
            return "NONE";
        }      
        int networkType = info.getType();
        int networkSubtype = info.getSubtype();
        if (networkType == ConnectivityManager.TYPE_WIFI) {
            return "WIFI";
        } else if (networkType == ConnectivityManager.TYPE_MOBILE) {
            
            switch(networkSubtype){
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "MOBILE_1RTT"; // ~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "MOBILE_CDMA"; // ~ 14-64 kbps
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "MOBILE_EDGE"; // ~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "MOBILE_EVDO_0"; // ~ 400-1000 kbps
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "MOBILE_EVDO_A"; // ~ 600-1400 kbps
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "MOBILE_GPRS"; // ~ 100 kbps
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "MOBILE_HSDPA"; // ~ 2-14 Mbps
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "MOBILE_HSPA"; // ~ 700-1700 kbps
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "MOBILE_HSUPA"; // ~ 1-23 Mbps
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "MOBILE_UMTS"; // ~ 400-7000 kbps
            /*
             * Above API level 7, make sure to set android:targetSdkVersion 
             * to appropriate level to use these
             */
            case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11 
                return "MOBILE_EHRPD"; // ~ 1-2 Mbps
            case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
                return "MOBILE_EVDO_B"; // ~ 5 Mbps
            case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
                return "MOBILE_HSPAP"; // ~ 10-20 Mbps
            case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
                return "MOBILE_IDEN"; // ~25 kbps 
            case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
                return "MOBILE_LTE"; // ~ 10+ Mbps
            // Unknown
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default:
                return "MOBILE_UNKNOWN";
            }
        }
        return "NONE";
    } 
}
