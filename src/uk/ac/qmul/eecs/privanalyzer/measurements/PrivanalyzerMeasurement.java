package uk.ac.qmul.eecs.privanalyzer.measurements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.simple.JSONObject;

import uk.ac.qmul.eecs.privanalyzer.R;
import uk.ac.qmul.eecs.privanalyzer.Session;
import uk.ac.qmul.eecs.privanalyzer.db.FileInfo;
import uk.ac.qmul.eecs.privanalyzer.db.MeasurementEntry;
import uk.ac.qmul.eecs.privanalyzer.logging.Logger;
import uk.ac.qmul.eecs.privanalyzer.schedulers.Scheduler;
import uk.ac.qmul.eecs.privanalyzer.util.NetworkStatusRecorder;
import uk.ac.qmul.eecs.privanalyzer.util.Utils;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;

public class PrivanalyzerMeasurement extends AsyncTask<Void, Void, Void> implements 
                                            GooglePlayServicesClient.ConnectionCallbacks,
                                            GooglePlayServicesClient.OnConnectionFailedListener,
                                            LocationListener {

    private static final String sTag = PrivanalyzerMeasurement.class.getName();  
    private ArrayList<MeasurementEntry> mResults = new ArrayList<MeasurementEntry>();
         
    private final Scheduler mScheduler;
    private final NetworkStatusRecorder mNetworkStatusRecorder;
    private LocationClient mLocationClient;
    private int mIsGooglePlayServicesAvailable = ConnectionResult.SERVICE_MISSING;
    private double mLatitude = 0.0;
    private double mLongitude = 0.0;
    
    public static void runMeasurement(Scheduler scheduler) {
        PrivanalyzerMeasurement pm = new PrivanalyzerMeasurement(scheduler);
        Logger.i(sTag, "Running profile measurement");
        pm.execute(null, null, null);
    }
    
    public PrivanalyzerMeasurement(Scheduler scheduler) {
        mScheduler = scheduler;
        mNetworkStatusRecorder = new NetworkStatusRecorder();
    }
    
    @Override
    protected void onPreExecute() {
        Logger.d(sTag, "onPreExecute");
        
        // Take a chance to rotate log files now
        Logger.d(sTag, "checking whether log files have to be rotated");
        Logger.rotateIfNecessary();
        
        // Watch out, may be very expensive, so be sure to stop
        // recording after the measurement has finished. Recording
        // can be stopped on the background thread
        Logger.d(sTag, "start recording");
        mNetworkStatusRecorder.startRecording();
        
        int resp = GooglePlayServicesUtil.isGooglePlayServicesAvailable(
                Session.getGlobalContext());
        if(resp == ConnectionResult.SUCCESS) {
           Logger.d(sTag, "Google Play Services available");
           mIsGooglePlayServicesAvailable = ConnectionResult.SUCCESS;
            // Connect to the location service.
           mLocationClient = new LocationClient(Session.getGlobalContext(), this, this);
           mLocationClient.connect();
        } else {
           Logger.d(sTag, "Google Play Services unavailable!");
        }
    }
    
    @Override
    protected Void doInBackground(Void... arg0) {
        Logger.d(sTag, "doInBackground");
    
        //Utils.acquireWakeLock();
        boolean measurementFailed = true;
        try {         
            Utils.sleepMillis(10000);
            mNetworkStatusRecorder.stopRecording();  
            measurementFailed = false;
            
        } catch (Exception e) {
            Logger.e(sTag, "Measurement failed: " + e.getMessage(), e);
        } finally {        
            mNetworkStatusRecorder.stopRecording(); // We do it again in case of Exceptions
            mScheduler.measurementFinished(measurementFailed);
            //Utils.releaseWakeLock();
            writeResultsToDatabase();
        }
        
        return null;
    }
 
    @Override
    public void onLocationChanged(Location location) {
        Logger.w(sTag, "Callback onLocationChanged()");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Logger.w(sTag, "Failed to get location.");      
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            if (mIsGooglePlayServicesAvailable == ConnectionResult.SUCCESS) {       
                Location loc = mLocationClient.getLastLocation();
                mLatitude = loc.getLatitude();
                mLongitude = loc.getLongitude();
                Logger.i(sTag, "Last Known Location :" + loc.getLatitude() + "," + loc.getLongitude());
            }
        } catch (Exception e) {
          Logger.e(sTag, "Error in onConnected()");  
        }
    }

    @Override
    public void onDisconnected() {
        Logger.w(sTag, "Callback onDisconnected()");
    }
    
    @SuppressWarnings("unchecked")
    private void logRecordedNetworkStatuses(String filename) {
        if (mNetworkStatusRecorder != null) {
            try {
                JSONObject statusJson = mNetworkStatusRecorder.getResult();
                statusJson.put("latitude", mLatitude);
                statusJson.put("longitude", mLongitude);
                statusJson.put("applications", getApplicationList());
                statusJson.put("browsing_history", getBrowsingHistory());
                
                String status = statusJson.toJSONString();
                Logger.d(sTag, "Network status is " + status);
                
                File filesDir = Utils.getExternalFilesDir();
                FileWriter fw = new FileWriter(filesDir.getAbsolutePath() + "/" + filename, true);
                fw.write(status);
                fw.write("\n");  
                fw.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }       
    }
    
    // Get the list of installed applications every 24h
    private JSONObject getApplicationList() {      
        JSONObject applicationList = null;
        
        Context context = Session.getGlobalContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Check if current time is past the next check time
        long nextCheck = prefs.getLong(context.getString(
                R.string.fixedRepeatSchedulerCheckApplicationsNext), 0);
        long now = System.currentTimeMillis();
        
        // Get the list of apps and update next check
        if (now > nextCheck) {
            Logger.i(sTag, "Getting the list of installed applications");
            applicationList = Utils.getApplicationList();
            
            nextCheck = System.currentTimeMillis() + context.getResources().getInteger(
                    R.integer.checkApplicationsIntervalSec) * 1000;                  
            prefs.edit().putLong(context.getString(R.string.fixedRepeatSchedulerCheckApplicationsNext),
                    nextCheck).commit();          
        } else {
            applicationList = new JSONObject();
            applicationList.put("timestamp", Utils.ms_to_s(System.currentTimeMillis()));
            applicationList.put("data", new JSONArray());
        }
        
        return applicationList;
    }
     
    // Get the list of installed applications every 24h
    private JSONObject getBrowsingHistory() {      
        JSONObject browsingHistory = null;
        
        Context context = Session.getGlobalContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Check if current time is past the next check time
        long nextCheck = prefs.getLong(context.getString(
                R.string.fixedRepeatSchedulerCheckBrowserHistoryNext), 0);
        long now = System.currentTimeMillis();
        
        // Get the list of apps and update next check
        if (now > nextCheck) {
            Logger.i(sTag, "Getting the list of installed applications");
            browsingHistory = Utils.getBrowsingHistory();
            
            nextCheck = System.currentTimeMillis() + context.getResources().getInteger(
                    R.integer.checkBrowserHistoryIntervalSec) * 1000;                  
            prefs.edit().putLong(context.getString(R.string.fixedRepeatSchedulerCheckBrowserHistoryNext),
                    nextCheck).commit();          
        } else {
            browsingHistory = new JSONObject();
            browsingHistory.put("timestamp", Utils.ms_to_s(System.currentTimeMillis()));
            browsingHistory.put("browser_history", new JSONArray());
        }
        
        return browsingHistory;
    }
    
    private void addResult(MeasurementEntry result) {
        if (result == null) {
            Logger.d(sTag, "Skiping null result");
            return;
        }
        mResults.add(result);
    }
    
    private void writeResultsToDatabase () {
        FileInfo fileInfo = Utils.getLogFileInfo();
        Session.getDatabaseHandler().insertFileInfo(fileInfo);
        logRecordedNetworkStatuses(Utils.getLogFileName());
        Logger.d(sTag, "File log is " + Utils.getLogFileName());
        
        Logger.d(sTag, "writeResultsToDatabase()");        
        for (MeasurementEntry result : mResults) {
            Session.getDatabaseHandler().insertResult(result);
        }
    }
}
