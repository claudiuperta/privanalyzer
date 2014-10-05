package uk.ac.qmul.eecs.privanalyzer;

import uk.ac.qmul.eecs.privanalyzer.db.MeasurementsDatabase;
import uk.ac.qmul.eecs.privanalyzer.schedulers.Scheduler;
import uk.ac.qmul.eecs.privanalyzer.util.NetworkStatus;
import uk.ac.qmul.eecs.privanalyzer.util.PhoneStatus;
import uk.ac.qmul.eecs.privanalyzer.R;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Session extends Application {
    private static Scheduler sScheduler = null;
    private static boolean sIsForegroundServiceRunning = false;
    private static boolean sIsBoundToService = false;
    private static Context sApplicationContext = null;
    private static NetworkStatus sNetworkStatus = null;
    private static PhoneStatus sPhoneStatus = null;
    private static long sNextMeasurementTimestamp = 0;
    private static long sLastMeasurementTimestamp = 0;
     
    public static int MAX_RESULTS_VIEW = 100;
    public static PrivanalyzerService sProfilerService = null;
    public static MeasurementsDatabase sResultsDatabaseHandler = null;
    
    // TODO(claudiu) Make sure this is false before publishing the app
    public static boolean DEBUG_ENABLED = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
    }
    
    public static Context getGlobalContext() {
        return sApplicationContext;
    }
    
    public static void setGlobalContext(Context context) {
        sApplicationContext = context;
    }
    
    public static void setForegroundServiceRunning(boolean val) {
        sIsForegroundServiceRunning = val;
    }
    
    public static MeasurementsDatabase getDatabaseHandler() {
    	if (sResultsDatabaseHandler == null) {
    		sResultsDatabaseHandler = new MeasurementsDatabase(getGlobalContext());
    	}
    	return sResultsDatabaseHandler;
    }
    
    public static boolean isForegroundServiceRunning() {
        return sIsForegroundServiceRunning;
    }
    
    public static void saveForegroundServiceRunning(boolean value) {
        SharedPreferences prefs = PreferenceManager
                                        .getDefaultSharedPreferences(sApplicationContext);
        prefs.edit().putBoolean(sApplicationContext.getString(
                                        R.string.foregroundServiceRunningKey), value).commit();
    }
    
    public static boolean loadSavedForegroundServiceRunning() {
        SharedPreferences prefs = PreferenceManager
                                        .getDefaultSharedPreferences(sApplicationContext);
        return prefs.getBoolean(sApplicationContext.getString(R.string.foregroundServiceRunningKey),
             sApplicationContext.getResources().getBoolean(R.bool.foregroundServiceRunningDefault));
    }
    
    public static NetworkStatus getNetworkStatus() {
        return sNetworkStatus;
    }
    
    public static void setNetworkStatus(NetworkStatus status) {
        sNetworkStatus = status;
    }
    
    public static PhoneStatus getPhoneStatus() {
        return sPhoneStatus;
    }
    
    public static void setPhoneStatus(PhoneStatus status) {
        sPhoneStatus = status;
    }
    
    public static boolean isSchedulerRunning() {
        return sScheduler != null && sScheduler.isRunning();
    }
    
    public static void setScheduler(Scheduler scheduler) {
        sScheduler = scheduler;
    }
    
    public static Scheduler getScheduler() {
        return sScheduler;
    }
    
    public static void setBoundToService(boolean val) {
        sIsBoundToService = val;
    }
    
    public static boolean isBoundToService() {
        return sIsBoundToService;
    }
   
    public static String getStatus() {
        if (isBoundToService()) {
            return "Running";
        } else {
            return "Stopped";
        }
    }
    
    private static String timestampToString(long timestamp) {
        if (timestamp == 0) {
            return "";
        }
        Date d = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM, HH:mm");
        return sdf.format(d); 
    }
    
    public static String getLastMeasurementDate() {
       return timestampToString(sLastMeasurementTimestamp);
    }
    
    public synchronized static void setNextMeasurement(long timestamp) {
        sNextMeasurementTimestamp = timestamp;
    }
    
    public static String getNextMeasurementDate() {
        return timestampToString(sNextMeasurementTimestamp);
    }
}
