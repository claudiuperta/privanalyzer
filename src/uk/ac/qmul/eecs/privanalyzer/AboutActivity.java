package uk.ac.qmul.eecs.privanalyzer;

import uk.ac.qmul.eecs.privanalyzer.logging.Logger;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.LinearLayout;

public class AboutActivity extends Activity {

    public static final String TAB_TAG = "ABOUT_TAB";
    private static final String sTag = AboutActivity.class.getName();
     
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.about_view);
        init();
    }
    
    @Override
    protected void onResume() {
        Logger.d(sTag, "onResume");
        super.onResume();
        this.setContentView(R.layout.about_view);
        init();
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
    
    private void init() {
        WebView view = new WebView(this);
        view.setVerticalScrollBarEnabled(false);

        ((LinearLayout)findViewById(R.id.textView)).addView(view);
        view.loadData(getString(R.string.aboutHTML), "text/html", "utf-8");
    }
}
