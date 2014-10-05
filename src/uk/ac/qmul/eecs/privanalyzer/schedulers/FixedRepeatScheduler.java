package uk.ac.qmul.eecs.privanalyzer.schedulers;

import uk.ac.qmul.eecs.privanalyzer.Session;
import uk.ac.qmul.eecs.privanalyzer.logging.Logger;
import uk.ac.qmul.eecs.privanalyzer.measurements.PrivanalyzerMeasurement;
import uk.ac.qmul.eecs.privanalyzer.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class FixedRepeatScheduler implements Scheduler {
    
    private static final String sTag = FixedRepeatScheduler.class.getName();
    private static final String sAlarmAction = FixedRepeatScheduler
                                    .class.getPackage().getName() + ".ALARM_ACTION";
   
    private boolean mIsRunning = false;       // Tells whether the scheduler is running or not
    private SchedulerProfile mProfile = null; // Used to decide the time of the next measurement
    private int mBatteryMin = -1;             // Minimum battery threshold
    
    private final BroadcastReceiver mAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(sAlarmAction)) {
                alarmReceived();
            }
        }
    };
      
    private void alarmReceived() {
        
        Logger.i(sTag, "Alarm received, starting new measurement");
        
        // Determine whether the current battery level is high enough to perform a measurement
        Context context = Session.getGlobalContext();
        Resources resources = context.getResources();
        
        double batteryPercentage = Session.getPhoneStatus().currentBatteryPercentage();
        if (resources.getBoolean(R.bool.measurementsDisabled)) {
            Logger.i(sTag, "Measurements disabled");
            
        } else if (batteryPercentage < mBatteryMin) {
            Logger.i(sTag, "Batter level too low, postponing the measurement");
            int retrySeconds = resources.getInteger(
                                            R.integer.fixedRepeatSchedulerBatteryLowRetrySeconds);
            long now = System.currentTimeMillis();
            setAlarm(context, now + (retrySeconds * 1000), now);
        } else {
            PrivanalyzerMeasurement.runMeasurement(this);
        }
    }
    
    public synchronized void measurementFinished(boolean measurementFailed) {
        
        Logger.d(sTag, "measurementFinished");
        
        if (mIsRunning) {
            Context context = Session.getGlobalContext();
            Resources resources = context.getResources();
            
            long now = System.currentTimeMillis();
            long triggerTime = now;
            if (measurementFailed == true) {
                long retryTime = resources
                                     .getInteger(R.integer.fixedRepeatSchedulerRetrySeconds) * 1000;
                Logger.w(sTag, "measurement failed, retrying in "
                               + retryTime / 1000.0 + " seconds");
                triggerTime += retryTime;
            } else {
                Logger.i(sTag, "measurement succeded, scheduling next measurement");
                triggerTime += mProfile.nextMeasurementInterval();
            }
            
            sendStatusUpdate();
            setAlarm(context, triggerTime, now);
        } else {
            Logger.i(sTag, "Not scheduling next measurement, as we have been stopped");
        }
    }
    
    private void reloadParameters() {
        
        Logger.d(sTag, "reloadParameters");
        
        Context context = Session.getGlobalContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Check whether the profile has changed
        SchedulerProfile profile = loadProfile(context, prefs);
        if (mProfile == null) {
            // This is the first time we read the preferences, just store the value
            mProfile = profile;
            Logger.i(sTag, "Scheduler profile " + mProfile.getClass().getName() + " loaded");
        } else if (profile.getClass() != mProfile.getClass()) {
            Logger.i(sTag, "Scheduler profile changed from " + mProfile.getName() +
                           " to " + profile.getName() + ". Changing the alarm");
            mProfile = profile;
            long now = System.currentTimeMillis();
            long triggerTime = now + mProfile.nextMeasurementInterval();
            cancelAlarm(context, false);
            setAlarm(context, triggerTime, now);
        } else {
            Logger.d(sTag, "Scheduler profile unchanged (still "
                           + mProfile.getClass().getName() + ")");
        }
        
        // Check whether the battery threshold has changed
        int batteryMin = readBatteryThreshold(context, prefs);
        if (mBatteryMin < 0) {
            Logger.i(sTag, "Battery threshold set to " + batteryMin + " %");
        } else if (mBatteryMin != batteryMin) {
            Logger.i(sTag, "Battery threshold changed from " +
                            mBatteryMin + "% to " + batteryMin + "%");
        } else {
            Logger.d(sTag, "Battery threshold unchanged (still " + mBatteryMin + "%)");
        }
        mBatteryMin = batteryMin;
        sendStatusUpdate();
    }
    
    @Override
    public synchronized void start() {
        
        Logger.d(sTag, "start");
        
        if (mIsRunning) {
            return;
        } else {
            Logger.i(sTag, "Starting the scheduler");
        }
        
        Context context = Session.getGlobalContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
     
        // Retrieve the profile to be used
        mProfile = loadProfile(context, prefs);
        Logger.d(sTag, "profile " + mProfile.getClass().getName() + " loaded");
        
        // Retrieve the battery threshold
        mBatteryMin = readBatteryThreshold(context, prefs);
        Logger.d(sTag, "battery min threshold is " + mBatteryMin + "%");
        
      
        /* Retrieve the time when the next measurement was previously scheduled to.
         * This is used in order to avoid restarting the count-down every time the service
         * is stopped and resumed */
        long nextTime = prefs.getLong(context.getString(R.string.fixedRepeatSchedulerNextTime), 0);
        long now = System.currentTimeMillis();
        long triggerTime = now + mProfile.nextMeasurementInterval();
        if (nextTime > 0) {
            triggerTime = nextTime;
        }
        
        // Setup the alarm
        setAlarm(context, triggerTime, now);
        
        mIsRunning = true;
    }

    @Override
    public synchronized void stop() {
     
        Logger.d(sTag, "stop");
        
        if (mIsRunning == false) {
            return;
        } else {
            Logger.i(sTag, "Stopping the scheduler");
        }
        
        cancelAlarm(Session.getGlobalContext(), false);
        mIsRunning = false;
    }
    
    public synchronized boolean isRunning() {
        return mIsRunning;
    }
    
    private void cancelAlarm(Context context, boolean deleteScheduledTime) {
        
        // Cancel the current alarm
        context.unregisterReceiver(mAlarmReceiver);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(sAlarmAction), 0);
        am.cancel(pi);
        
        if (deleteScheduledTime) {
            // Remove the next measurement scheduled time.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().remove(context.getString(R.string.fixedRepeatSchedulerNextTime)).commit();
        }
    }
    
    private void setAlarm(Context context, long triggerTime, long now) {
        
        // Save the next alarm time in the preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(context.getString(R.string.fixedRepeatSchedulerNextTime),
                             triggerTime).commit();
        
        // Setup the alarm
        context.registerReceiver(mAlarmReceiver, new IntentFilter(sAlarmAction));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(sAlarmAction),
                                                           PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi);
        Logger.i(sTag, "Next measurement scheduled at " + (triggerTime / 1000.0) + " (" +
                        ((triggerTime - now) / 1000.0) + " seconds from now)");
        
        Session.setNextMeasurement(triggerTime / 1000);
    }

    private SchedulerProfile loadProfile(Context context, SharedPreferences prefs) {
        if (Session.DEBUG_ENABLED) {
            Logger.w(sTag, "Using debug scheduler, change this when moving to production.");
            return SchedulerProfile.getScheduler(
                    context.getResources().getInteger(R.integer.schedulerDebug));
        }
             
        return SchedulerProfile.getScheduler(
                context.getResources().getInteger(R.integer.schedulerDefault));
    }
    
    private int readBatteryThreshold(Context context, SharedPreferences prefs) {
        String batteryKey = context.getString(R.string.fixedRepeatSchedulerBatteryMinPrefKey);
        int batteryMin = context.getResources()
                              .getInteger(R.integer.fixedRepeatSchedulerBatteryMinPrefDefault);
        String batteryMinStr = prefs.getString(batteryKey, null);
        if (batteryMinStr != null)
            batteryMin = Integer.parseInt(batteryMinStr);
        if (batteryMin < 0 || batteryMin > 100) {
            throw new SchedulerException("Battery percentage threshold must be between 0 and 100");
        }
        return batteryMin;
    }
    
    private void sendStatusUpdate() {        
        Logger.d(sTag, "sendStatusUpdate");
        Session.getGlobalContext().sendBroadcast(new Intent(Schedulers.STATUS_UPDATE_ACTION));
    }
}
