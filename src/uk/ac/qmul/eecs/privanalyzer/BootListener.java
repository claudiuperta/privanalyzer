package uk.ac.qmul.eecs.privanalyzer;

import uk.ac.qmul.eecs.privanalyzer.logging.Logger;
import uk.ac.qmul.eecs.privanalyzer.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootListener extends BroadcastReceiver {

    private static String sTag = BootListener.class.getName();
    
    @Override
    public void onReceive(Context context, Intent intent) {
    
        Logger.d(sTag, "onReceive");
        Logger.i(sTag, "Boot intercepted");
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean bootDefault = context.getResources().getBoolean(R.bool.startOnBootDefault);
        if (prefs.getBoolean(context.getString(R.string.startOnBootPrefKey), bootDefault)) {
            Logger.i(sTag, "Starting the scheduler");
            Intent serviceIntent = new Intent(context, PrivanalyzerService.class);
            serviceIntent.putExtra(PrivanalyzerService.EXTRA_START_SCHEDULER, true);
            context.startService(serviceIntent);
        }
    }
}
