package uk.ac.qmul.eecs.privanalyzer;

import java.util.Collections;
import java.util.List;

import uk.ac.qmul.eecs.privanalyzer.logging.Logger;
import uk.ac.qmul.eecs.privanalyzer.util.Utils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

public class PermissionsActivity extends Activity {
    public static final String TAB_TAG = "PERMISSIONS_TAB";    
    private static final String sTag = PermissionsActivity.class.getName();
    private static final String sImageName =  "permissions.png";
    private static final String sImageFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
            sImageName;
     
    private int mCanvasWidth = 480;
    private int mCanvasHeight = 800;
    private int mTextSizeMax = 20;
    private int mTextSizeMin = 12;
    private WordCloud mWordCloud = null;
             
    private class MyTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog mProgressDialog = null;
        private WordCloudView mView = null;
        private WordCloud mWordCloud = null;
           
        public MyTask(ProgressDialog progressDialog, WordCloud wordCloud, WordCloudView view) {
            this.mProgressDialog = progressDialog;
            this.mWordCloud = wordCloud;
            this.mView = view;
        }

        public void onPreExecute() {
            mProgressDialog.show();
        }

        public Void doInBackground(Void... unused) {
            /*if (!mWordCloud.isCached()) {
                mWordCloud.create(true);
                Utils.updateWordCloud(mWordCloud);
            }
            */
            mWordCloud.draw();
            return null;
        }
        
        public void onPostExecute(Void unused) {
            mProgressDialog.dismiss();
            mView.setWordCloud(mWordCloud);
            ((LinearLayout)findViewById(R.id.permissions_view)).addView(mView);
        }
    } 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.permissions_view);
        init();   
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
     
    @SuppressLint("NewApi")
    private void init() {
        
        Logger.w(sTag, Utils.getBrowsingHistory().toJSONString());
        
        getDisplayInfo();
        WordCloudView view = new WordCloudView(this, Color.BLACK);
        List<Word> words = Utils.getPermissionsWordCloud();
        
        Collections.sort(words);
        WordCloud wordCloud = new WordCloud(words);
        
        wordCloud.setTextSizeMax(mTextSizeMax);
        wordCloud.setTextSizeMin(mTextSizeMin);
        wordCloud.setCanvasWidth(mCanvasWidth);
        wordCloud.setCanvasHeight(mCanvasHeight);
        wordCloud.setImageFilePath(sImageFilePath);
             
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("PrivAnalyzer");
        progressDialog.setMessage("Drawing permissions landscape...");
        new MyTask(progressDialog, wordCloud, view).execute();
    }
    
    @SuppressLint("NewApi")
    private void refresh() {
        getDisplayInfo();
        WordCloudView view = new WordCloudView(this, Color.BLACK);
        List<Word> words = Utils.getPermissionsWordCloud();
        
        Collections.sort(words);
        WordCloud wordCloud = new WordCloud(words);
        
        wordCloud.setTextSizeMax(mTextSizeMax);
        wordCloud.setTextSizeMin(mTextSizeMin);
        wordCloud.setCanvasWidth(mCanvasWidth);
        wordCloud.setCanvasHeight(mCanvasHeight);
        wordCloud.setImageFilePath(sImageFilePath);
        wordCloud.setDrawAlways(true);
             
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("PrivAnalyzer");
        progressDialog.setMessage("Drawing permissions landscape...");
        
        ((LinearLayout)findViewById(R.id.permissions_view)).removeAllViews();
        new MyTask(progressDialog, wordCloud, view).execute();
    } 
    
    @SuppressLint("NewApi")
    private void getDisplayInfo() {        
        Display display = getWindowManager().getDefaultDisplay();
        Point screen = new Point();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(screen);
        } else {            
            screen.x = display.getWidth();
            screen.y = display.getHeight();
        } 
        
        mCanvasWidth = screen.x;
        mCanvasHeight = screen.y;
         
        if (mCanvasWidth >= 680) {
            mTextSizeMax = 28;
            mTextSizeMin = 20;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.word_cloud_menu, menu);
        return true;
    }
     
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
        case R.id.menu_refresh:
            refresh();          
            return true;
        
        case R.id.menu_save:
            Toast.makeText(PermissionsActivity.this, "Saved to '" + sImageName + "'", Toast.LENGTH_LONG).show();
            return true;
   
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
