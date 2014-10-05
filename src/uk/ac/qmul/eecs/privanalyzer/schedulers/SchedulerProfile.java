package uk.ac.qmul.eecs.privanalyzer.schedulers;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import uk.ac.qmul.eecs.privanalyzer.logging.Logger;

public abstract class SchedulerProfile {

    private final static String sTag = SchedulerProfile.class.getName();
    
    // TODO(claudiu) Schedule measurements based on the places inferred.
    // (ideally we should adapt to the user current location)
    
    private static SchedulerProfile[] sProfiles = {
    
        new SchedulerProfile("Every 5 minutes", (12 * 3) + (3 * 2) + 12) {
            @Override
            public long nextMeasurementInterval(int hour) {
                if (8 <= hour && hour <= 24) {
                    return TimeUnit.SECONDS.toMillis(60 * 1);
                } else {
                    return TimeUnit.SECONDS.toMillis(60 * 1);
                }
            }
        },     
        new SchedulerProfile("Every 10 minutes", (12 * 3) + (3 * 2) + 12) {
            @Override
            public long nextMeasurementInterval(int hour) {
                if (8 <= hour && hour <= 24) {
                    return TimeUnit.SECONDS.toMillis(60 * 10);
                } else {
                    return TimeUnit.SECONDS.toMillis(60 * 10);
                }
            }
        },
        new SchedulerProfile("Every 15 minutes", (16 * 2) + 12) {
            @Override
            public long nextMeasurementInterval(int hour) {
                if (8 <= hour && hour <= 24) {
                    return TimeUnit.SECONDS.toMillis(60 * 15);
                } else {
                    return TimeUnit.SECONDS.toMillis(60 * 60);
                }
            }
        },
        new SchedulerProfile("Every minute (debug only)", 1440) {
            @Override
            public long nextMeasurementInterval(int hour) {
                return TimeUnit.SECONDS.toMillis(60);
            }
        }
    };
    
    public static SchedulerProfile getScheduler(int code) {
        Logger.d(sTag, "getScheduler(" + code + ")");
        try {
            Logger.d(sTag, "using a " + sProfiles[code].getName() + " scheduler profile");
            return sProfiles[code];
        } catch (ArrayIndexOutOfBoundsException e) {
            Logger.e(sTag, "BUG: unknown scheduler profile code "
                           + code + " defaulting to " + sProfiles[sProfiles.length / 2].getName());
            return sProfiles[0];
        }
    }
    
    public static SchedulerProfile[] getProfiles() {
        return sProfiles;
    }
    
    private final String mName;
    private final int mNAtDay;
    
    public SchedulerProfile(String name, int nAtDay) {
        mName = name;
        mNAtDay = nAtDay;
    }
    
    public String getName() {
        return mName;
    }
    
    public int getNumberOfDailyMeasurements() {
        return mNAtDay;
    }
    
    public abstract long nextMeasurementInterval(int hour);
    
    public long nextMeasurementInterval() {
        return nextMeasurementInterval(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
    }
}