package uk.ac.qmul.eecs.privanalyzer;

import uk.ac.qmul.eecs.privanalyzer.logging.Logger;
import uk.ac.qmul.eecs.privanalyzer.schedulers.Scheduler;
import uk.ac.qmul.eecs.privanalyzer.schedulers.SchedulerException;
import uk.ac.qmul.eecs.privanalyzer.schedulers.Schedulers;
import uk.ac.qmul.eecs.privanalyzer.util.NetworkStatus;
import uk.ac.qmul.eecs.privanalyzer.util.PhoneStatus;
import uk.ac.qmul.eecs.privanalyzer.R;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class PrivanalyzerService extends Service {

    public static final String EXTRA_START_SCHEDULER = "EXTRA_START_SCHEDULER";    
    private static final String sTag = PrivanalyzerService.class.getName();

    private final IBinder mBinder = new MeasurementBinder();
    private static PrivanalyzerServiceClient mServiceClient;
    
    private final BroadcastReceiver mConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                Scheduler s = Session.getScheduler();
                if (s == null) {
                    Logger.e(sTag, "BUG: scheduler not found");
                    return;
                }
                
                ConnectivityManager connectivityManager = (ConnectivityManager)
                                                getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
                
                if (ni != null && ni.isConnected()) {
                    if (s.isRunning() == false) {
                        Logger.i(sTag, "Connection is available, resuming the scheduler");
                        s.start();
                    }
                } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY,
                                                  Boolean.FALSE)) {
                    if (s.isRunning() == true) {
                        Logger.i(sTag, "Connection lost, pausing the scheduler");
                        s.stop();
                    }
                }
            }
        }
    };
    
    public class MeasurementBinder extends Binder {
        public PrivanalyzerService getService() {
            Logger.d(sTag, "Binder.getService");
            return PrivanalyzerService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent arg0) {
        Logger.d(sTag, "onBind");
        return mBinder;
    }
    
    @Override
    public void onCreate() {
        Logger.d(sTag, "onCreate");
        Session.setGlobalContext(getApplicationContext());
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Logger.d(sTag, "onStart");
        handleIntent(intent);
    }

    @Override
    public void onDestroy() {
        Logger.d(sTag, "onDestroy");
        mServiceClient = null;
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Logger.w(sTag, "Android is low on memory");
        super.onLowMemory();
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (intent.getBooleanExtra(EXTRA_START_SCHEDULER, false)) {
                Logger.i(sTag, "Service asked to start scheduler");
                start();
            } else {
                /* This is a bit complicated. I noticed that when the service is running
                 * in foreground and the user swipes the application from the recent apps
                 * list, then the app gets killed and crashes with a log message like:
                 * 
                 * I/ActivityManager(  256): Killing 32011:uk.ac.qmul.eecs.profiler/u0a148: remove task
                 * W/ActivityManager(  256): Scheduling restart of crashed service [...]
                 *
                 * Now, Android is nice enough to restart our service after some time
                 * (e.g., 5 seconds) so this wouldn't be a big deal. However, if the user re-opens
                 * the app during this time frame, then the foreground service does not restart
                 * because the MainActivity, not the OS, starts the service. To make things worse,
                 * when the app crashes, the service icon gets stuck in the tray icon (even if the
                 * app is officially dead..I honestly *don't know why*). So you end up in a
                 * situation where the tray icon is there, and the foreground service was not
                 * restarted properly. Interestingly, this does not happen for the GPSLogger app
                 * which uses a very similar way of starting/stopping the foreground service. In
                 * fact, the GPSLogger app *does not even crash* when the user swipes it. I believe
                 * we should spend some time trying to figure out why this happens and to produce
                 * a proper fix for the problem (TODO). In the meanwhile, I decided to use the
                 * following little dirty trick. The service uses a flag in the shared preferences
                 * telling whether it was started on the foreground or not (checkout the calls to
                 * Session.saveForegroundServiceRunning(boolean value)). In this way, when the
                 * service gets started, it can check whether it was previously running or not. This
                 * is the reason of this call to Session.loadSavedForegroundServiceRunning(). Notice
                 * that the MainActivity still calls Session.isForegroundServiceRunning() to check
                 * whether the service is running or not.
                 */
                if (Session.loadSavedForegroundServiceRunning() == true) {
                    Logger.i(sTag, "Service start command received. " +
                    		"Scheduler was previously running. Restarting it");
                    start();
                } else {
                    Logger.i(sTag, "Service start command received. Nothing else to do");
                }
            }
        } else {
            Logger.w(sTag, "Service restarted with null intent (app crashed?)");
            start();
        }
    }

    public void start() {
        
        Logger.i(sTag, "Starting");
        if (Session.isForegroundServiceRunning() == true) {
            Logger.w(sTag, "Service was already started. Ignoring start command");
            return;
        }
        Session.saveForegroundServiceRunning(true);
        
        NetworkStatus nStatus = Session.getNetworkStatus();
        if (nStatus == null) {
            nStatus = new NetworkStatus();
            Session.setNetworkStatus(nStatus);
        }
        
        PhoneStatus pStatus = Session.getPhoneStatus();
        if (pStatus == null) {
            pStatus = new PhoneStatus();
            Session.setPhoneStatus(pStatus);
        }
        
        registerReceiver(mConnectionReceiver,
                         new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        Scheduler scheduler;
        try {
            Context context = Session.getGlobalContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context); 
            String typeKey = context.getString(R.string.schedulerTypePrefKey);
            String schedulerType = prefs.getString(typeKey,context.getResources()
                                                         .getString(R.string.schedulerTypeDefault));
            scheduler = Schedulers.createScheduler(schedulerType);
        } catch (SchedulerException e) {
            Logger.e(sTag, "BUG: failed to instantiate the scheduler object");
            throw e;
        }
        scheduler.start();

        nStatus.start();
        pStatus.start();
           
        Session.setScheduler(scheduler);
        
        if (mServiceClient != null)
            mServiceClient.onBackgroundServiceStarted();
        Session.setForegroundServiceRunning(true);
    }
    
    public void stop() {
        
        Logger.i(sTag, "Stopping");
        if (Session.isForegroundServiceRunning() == false) {
            Logger.w(sTag, "Service was not running. Ignoring stop command");
            return;
        }
        Session.saveForegroundServiceRunning(false);
        
        unregisterReceiver(mConnectionReceiver);
        Session.getScheduler().stop();
        Session.getNetworkStatus().stop();
        Session.getPhoneStatus().stop();
        
        stopForeground(true);
        if (mServiceClient != null)
            mServiceClient.onBackgroundServiceStopped();
        Session.setForegroundServiceRunning(false);
    }
      
    public static void setServiceClient(PrivanalyzerServiceClient mainForm) {
        mServiceClient = mainForm;
    }
}
