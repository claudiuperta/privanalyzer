package uk.ac.qmul.eecs.privanalyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import uk.ac.qmul.eecs.privanalyzer.logging.Logger;
import uk.ac.qmul.eecs.privanalyzer.util.Utils;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

@SuppressLint("DrawAllocation")
public class WordCloudView extends View {
    
    private static final String sTag = WordCloudView.class.getName();    
    
    private List<Word> mWords = null;
    private WordCloud mWordCloud;
    private float mAngleX = 0;
    private float mAngleY = 0;
    private float centerX, centerY;
    private int bkColor;
    private Paint bkPaint;
    private Paint paint = new Paint();
    private float radius;
    
    private String mColor1 = "#189A50";
    private String mColor2 = "#97CA9F";
    private String mColor3 = "#FADB99";
    private String mColor4 = "#F1B488";
    private String mColor5 = "#DF0101";
    
    private boolean mDone = false;
    
    public WordCloudView(Context mContext, int bkColor) {
        super(mContext);
        bkPaint = new Paint();
        this.bkColor = bkColor;
    }

    public void addWords(List<Word> words) {
        mWords = words;
    }
    
    public void setWordCloud(WordCloud wordCloud) {
        mWordCloud = wordCloud;
    }
    
    private void createWordCloud(Canvas canvas) {
        if (mWords == null) {
            Logger.w(sTag, "Empty list of words!");
            return;
        }
       
        centerX = canvas.getWidth() / 2;
        centerY = canvas.getHeight() / 2 - 80;
        radius = Math.min(centerX * .8f , centerY);
              
        mWordCloud = new WordCloud(mWords, (int) radius);
        mWordCloud.setRadius((int) radius);
        mWordCloud.create(true);
    }

    protected void onDraw(Canvas canvas) {
        Logger.i(sTag, "onDraw()");
        super.onDraw(canvas);
        
        if (mWordCloud.isCached() && !mWordCloud.drawAlways()) {
            loadPNG(canvas);
            return;
        }
        
        if (mWordCloud == null) {
            createWordCloud(canvas);
        }
            
        Bitmap.Config conf = Bitmap.Config.ARGB_8888; 
        Bitmap bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), conf); 
        Canvas pngCanvas = new Canvas(bitmap);
        
        // Set up the background 
        Rect bkRect = new Rect();
        bkRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
        Logger.i(sTag, "Width = " + canvas.getWidth() + ", Height:" + canvas.getHeight());
        
        // Let's set up the color for the rectangle     
        bkPaint.setStyle(Paint.Style.FILL);
        bkPaint.setColor(bkColor);
        canvas.drawRect(bkRect, bkPaint);
        pngCanvas.drawRect(bkRect, bkPaint);

        // Draw the WordCloud
        Iterator it = mWordCloud.iterator();
        Word word;
       
        while (it.hasNext()) {
            int i = 0;
            word = (Word) it.next();
            Logger.i(sTag, "Drawing word " + word.getText() + "x:" + word.getX() + "y:" + word.getY());
           
            paint.setTextSize((int)(word.getTextSize() * word.getScale()));
            paint.setTextAlign(Align.CENTER);  
            paint.setColor(word.getColor());            
            paint.setAntiAlias(true);
            canvas.drawText(word.getText(), word.getX(), word.getY(), paint);
            pngCanvas.drawText(word.getText(), word.getX(), word.getY(), paint);
        }
        savePNG(bitmap, mWordCloud.getImageFilePath());
        mWordCloud.setDrawAlways(false);
    }
  
    @Override
    public boolean onTrackballEvent(MotionEvent e) {
        return true;
        /*
        float x = e.getX();
        float y = e.getY();

        mAngleX = ( y)*tspeed * TRACKBALL_SCALE_FACTOR;
        mAngleY = (-x)*tspeed * TRACKBALL_SCALE_FACTOR;
        invalidate();
    
        return true; */
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        /*
        float x = e.getX();
        float y = e.getY();
        
        switch (e.getAction()) {
        case MotionEvent.ACTION_MOVE:   
            //rotate elements depending on how far the selection point is from center of cloud
            float dx = x - centerX;
            float dy = y - centerY;
            mAngleX = ( dy/radius) *tspeed * TOUCH_SCALE_FACTOR;
            mAngleY = (-dx/radius) *tspeed * TOUCH_SCALE_FACTOR;
            invalidate();
        }
        */
        return true;
    }
    
    private int getWordColor(String word) {
        if (word.equalsIgnoreCase("ACCESS_FINE_LOCATION"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("ACCESS_COARSE_LOCATION"))
            return Color.parseColor(mColor4);
        if (word.equalsIgnoreCase("READ_CALL_LOG"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("READ_CONTACTS"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("READ_PROFILE"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("READ_SMS"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("READ_SOCIAL_STREAM"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("RECEIVE_MMS"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("RECEIVE_SMS"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("SEND_SMS"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("WRITE_CONTACTS"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("WRITE_PROFILE"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("WRITE_SOCIAL_STREAM"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("WRITE_CALL_LOG"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("USE_CREDENTIALS"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("KILL_BACKGROUND_PROCESSES"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("MANAGE_ACCOUNTS"))
            return Color.parseColor(mColor5);       
        if (word.equalsIgnoreCase("BILLING"))
            return Color.parseColor(mColor5);       
        if (word.equalsIgnoreCase("WRITE_SECURE_SETTINGS"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("READ_CALENDAR"))
            return Color.parseColor(mColor4);
        if (word.equalsIgnoreCase("INTERNET"))
            return Color.parseColor(mColor3);
        if (word.equalsIgnoreCase("INSTALL_PACKAGES"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("GET_ACCOUNTS"))
            return Color.parseColor(mColor5);
        if (word.equalsIgnoreCase("CHANGE_NETWORK_STATE"))
            return Color.parseColor(mColor3);
        if (word.equalsIgnoreCase("CHANGE_WIFI_STATE"))
            return Color.parseColor(mColor3);
        if (word.equalsIgnoreCase("ACCESS_WIFI_STATE"))
            return Color.parseColor(mColor3);
        if (word.equalsIgnoreCase("ACCESS_NETWORK_STATE"))
            return Color.parseColor(mColor3);
        if (word.equalsIgnoreCase("ACCOUNT_MANAGER"))
            return Color.parseColor(mColor3);
        if (word.equalsIgnoreCase("AUTHENTICATE_ACCOUNTS"))
            return Color.parseColor(mColor4);
        if (word.equalsIgnoreCase("BLUETOOTH"))
            return Color.parseColor(mColor2);
        if (word.equalsIgnoreCase("BLUETOOTH_ADMIN"))
            return Color.parseColor(mColor3);
        if (word.equalsIgnoreCase("CALL_PHONE"))
            return Color.parseColor(mColor4);
        if (word.equalsIgnoreCase("CALL_PRIVILEGED"))
            return Color.parseColor(mColor4);
        if (word.equalsIgnoreCase("DELETE_PACKAGES"))
            return Color.parseColor(mColor5);
        
        return Color.parseColor(mColor1);                                                                                                       
    }
    
    private void savePNG(Bitmap bitmap, String imageFilePath) {
        if (imageFilePath == null) {
            Logger.e(sTag, "Error: NULL image file path!");
            return;
        }
        
        File file = new File(imageFilePath);
        FileOutputStream ostream;
        try {
            file.createNewFile();
            ostream = new FileOutputStream(file);
            bitmap.compress(CompressFormat.PNG, 100, ostream);
            ostream.flush();
            ostream.close();
            Logger.i(sTag, "Image saved!");
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(sTag, "Error when trying to save the image!");
        }
    }
    
    private void loadPNG(Canvas canvas) {
        String imageFilePath = mWordCloud.getImageFilePath();
        if (imageFilePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);
            canvas.drawBitmap(bitmap, 0, 0, null);
        } else {
            Logger.e(sTag, "Error, image file is missing!");
        }
    }
}

