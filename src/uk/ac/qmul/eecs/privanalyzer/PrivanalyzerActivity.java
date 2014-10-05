package uk.ac.qmul.eecs.privanalyzer;

import uk.ac.qmul.eecs.privanalyzer.db.MeasurementsDatabase;
import uk.ac.qmul.eecs.privanalyzer.logging.Logger;
import uk.ac.qmul.eecs.privanalyzer.schedulers.Schedulers;
import uk.ac.qmul.eecs.privanalyzer.util.Utils;
import uk.ac.qmul.eecs.privanalyzer.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;

import com.bugsense.trace.BugSenseHandler;

import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

@SuppressWarnings("deprecation")
public class PrivanalyzerActivity extends TabActivity implements PrivanalyzerServiceClient {

    private static final String sTag = PrivanalyzerActivity.class.getName();
    private static Intent sServiceIntent;
    
    private TabHost mTabHost;
    
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceDisconnected(ComponentName name) {
            Session.sProfilerService = null;
        }
       
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.d(sTag, "onServiceConnected");
            Session.sProfilerService = ((PrivanalyzerService.MeasurementBinder) service).getService();
            Session.setBoundToService(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d(sTag, "onCreate");
        super.onCreate(savedInstanceState);
        Session.setGlobalContext(getApplicationContext());
        
        BugSenseHandler.initAndStartSession(getApplicationContext(), "a30fd066");
       
        setContentView(R.layout.activity_main);
        try {
            checkThirdPartyBinaries();
        } catch (Exception e) {
            Logger.e(sTag, "FATAL ERROR: failed to copy app assets", e);
            // TODO: alert the user and die gracefully?
        }
        
        Resources res = getResources(); // Resource object to get Drawables
        mTabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;    // Reusable TabSpec for each tab
        Intent intent;           // Reusable Intent for each tab

        intent = new Intent().setClass(this, AboutActivity.class);
        spec = mTabHost.newTabSpec(AboutActivity.TAB_TAG).setIndicator(
            "About", res.getDrawable(R.drawable.ic_tab_about)).setContent(intent);
        mTabHost.addTab(spec);

        intent = new Intent().setClass(this, PermissionsActivity.class);
        spec = mTabHost.newTabSpec(PermissionsActivity.TAB_TAG).setIndicator(
            "Permissions", res.getDrawable(R.drawable.ic_tab_permissions)).setContent(intent);
        mTabHost.addTab(spec);
            
        intent = new Intent().setClass(this, AppsActivity.class);
        spec = mTabHost.newTabSpec(AppsActivity.TAB_TAG).setIndicator(
            "Apps", res.getDrawable(R.drawable.ic_tab_apps)).setContent(intent);
        mTabHost.addTab(spec);
        
        mTabHost.setCurrentTabByTag(AboutActivity.TAB_TAG);
    }

    @Override
    protected void onStart() {
        Logger.d(sTag, "onStart");
        super.onStart();
        // TODO(claudiu) Re-enable this
        startAndBindService();
    }

    protected void onStop() {
        Logger.d(sTag, "onStop");
        super.onStop();
        // TODO(claudiu) Re-anable this
        stopAndUnbindService();
    }

    @Override
    protected void onResume() {
        Logger.d(sTag, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Logger.d(sTag, "onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Logger.d(sTag, "onDestroy");
        super.onDestroy();
    }

    private void startAndBindService() {
        sServiceIntent = new Intent(this, PrivanalyzerService.class);
        startService(sServiceIntent);
        if (bindService(sServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE) == false) {
            Logger.e(sTag, "BUG: bindService failed!");
        } else {
            Session.setBoundToService(true);
        }
    }

    private void stopAndUnbindService() {
        if (Session.isBoundToService()) {
            unbindService(mServiceConnection);
            Session.setBoundToService(false);
        } else {
            Logger.w(sTag, "BUG: Stopping the service, but it was not bounded! (not critical)");
        }
        if (Session.isForegroundServiceRunning() == false) {
            stopService(sServiceIntent);
        }
    }
    
    @Override
    public void onBackgroundServiceStarted() {
        Logger.d(sTag, "onBackgroundServiceStarted");
    }

    @Override
    public void onBackgroundServiceStopped() {
        Logger.d(sTag, "onBackgroundServiceStopped");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);     
    }

    private void checkThirdPartyBinaries() throws Exception {
        Context context = Session.getGlobalContext();
        for (String binaryFileName: getAssets().list("bin")) {
            File binaryFile = context.getFileStreamPath(binaryFileName);
            if (binaryFile.exists() == false) {
                Logger.d(sTag, "Start copying binary " + binaryFileName);
                BufferedOutputStream os = null;
                BufferedInputStream is = null;
                try {
                    is = new BufferedInputStream(getAssets().open("bin/" + binaryFileName));
                    os = new BufferedOutputStream(context.openFileOutput(binaryFileName,
                                                                         MODE_PRIVATE));
                    Utils.copy(is, os);
                    Utils.setFileExecutable(binaryFile.getAbsolutePath());
                    Logger.d(sTag, "...done!");
                } catch (Exception e) {
                    Logger.d(sTag, "Error while copying binary " + binaryFileName);
                    Logger.d(sTag, "Trace: " + e.getMessage());
                    throw e;
                } finally {
                    if (is != null) is.close();
                    if (os != null) os.close();
                }
            }
        }
    }
}
