package uk.ac.qmul.eecs.privanalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;

import uk.ac.qmul.eecs.privanalyzer.logging.Logger;
import uk.ac.qmul.eecs.privanalyzer.util.Utils;

public class WordCloud implements Iterable {

    private final static String sTag = WordCloud.class.getName();
    
    private List<Word> mWordList;
    private int radius;
    private static final int DEFAULT_RADIUS = 3;
    private static final int TEXT_SIZE_MAX = 20, TEXT_SIZE_MIN = 12;
    
    private int mTextSizeMax, mTextSizeMin;
    private float sin_mAngleX,cos_mAngleX,sin_mAngleY,cos_mAngleY,sin_mAngleZ,cos_mAngleZ;
    private float mAngleZ = 0;
    private float mAngleX = 0;
    private float mAngleY = 0;
    
    private int mCanvasWidth = 480;
    private int mCanvasHeight = 800;
    
    private String mImageFilePath = null;
    private boolean mDrawAlways = false;
    
    public WordCloud() {
        this(new ArrayList<Word>());
    }
    
    public WordCloud(List<Word> tags) {
        this(tags,DEFAULT_RADIUS); 
    }
    
    //Constructor just copies the existing tags in its List
    public WordCloud(List<Word> tags, int radius) {
        this(tags, radius, TEXT_SIZE_MIN, TEXT_SIZE_MAX);
    }  
  
    public WordCloud(List<Word> wordList, int radius, int textSizeMin, int textSizeMax) {
        this.mWordList = wordList;
        this.radius = radius;
        this.mTextSizeMax = textSizeMax;
        this.mTextSizeMin = textSizeMin;     
    }  
    
    //create method calculates the correct initial location of each tag
    public void create(boolean distrEven) {
        
        float centerX = mCanvasWidth / 2;
        float centerY = mCanvasHeight / 2 - 80;
        float radius = Math.min(centerX * .8f , centerY);
        this.setRadius((int) radius);
        
        //calculate and set the location of each Tag
        positionAll(distrEven);
        sineCosine( mAngleX, mAngleY, mAngleZ);
        updateAll();
        
        //Now, let's calculate and set the color for each tag:
        //first loop through all tags to find the smallest and largest popularities
        //largest popularity gets tcolor2, smallest gets tcolor1, the rest in between
        int smallest = 9999;
        int largest = 0;
        for (int i = 0; i< mWordList.size(); i++) {
            int j = mWordList.get(i).getPopularity();
            largest = Math.max(largest, j);
            smallest = Math.min(smallest, j);
        }
        //figuring out and assigning the colors/ textsize
        Word word;
        for (int i=0; i< mWordList.size(); i++) {
            word = mWordList.get(i);
            int j = word.getPopularity();
            float percentage =  ( smallest == largest ) ? 1.0f : ((float)j-smallest) / ((float)largest-smallest);
            word.setTextSize(getTextSizeGradient(percentage));
        }       
    }
    
    //updates the transparency/scale of all elements
    public void update() {
        // if mAngleX and mAngleY under threshold, skip motion calculations for performance
        if( Math.abs(mAngleX) > .1 || Math.abs(mAngleY) > .1 ) {
            sineCosine(mAngleX, mAngleY, mAngleZ);
            updateAll();
        }
    }
    
    public void add() {
        // TODO (claudiu)  
    }
    
    @Override
    public Iterator iterator() {
        return mWordList.iterator();
    }   

    private void positionAll(boolean distrEven) {
        double phi = 0;
        double theta = 0;
        int max = mWordList.size();
        //distribute: (disrtEven is used to specify whether distribute random or even 
        for (int i = 1;  i< max + 1; i++) {
            if (distrEven) {
                phi = Math.acos(-1.0 + (2.0*i -1.0)/max);
                theta = Math.sqrt(max*Math.PI) * phi;
            } else {
                phi = Math.random()*(Math.PI);
                theta = Math.random()*(2 * Math.PI);
            }
            //coordinate conversion:
            mWordList.get(i-1).setLocX((int)(radius * Math.cos(theta) * Math.sin(phi)));
            mWordList.get(i-1).setLocY((int)(radius * Math.sin(theta) * Math.sin(phi)));
            mWordList.get(i-1).setLocZ((int)(radius * Math.cos(phi)));
        }       
    }   
    
    private void updateAll() {       
        //update transparency/scale for all tags:
        int max = mWordList.size();
        for (int j = 0; j < max; j++) {
            //There exists two options for this part:
            // multiply positions by a x-rotation matrix
            float rx1 = (mWordList.get(j).getLocX());
            float ry1 = (mWordList.get(j).getLocY()) * cos_mAngleX +
                        mWordList.get(j).getLocZ() * -sin_mAngleX;
            float rz1 = (mWordList.get(j).getLocY()) * sin_mAngleX +
                        mWordList.get(j).getLocZ() * cos_mAngleX;                        
            // multiply new positions by a y-rotation matrix
            float rx2 = rx1 * cos_mAngleY + rz1 * sin_mAngleY;
            float ry2 = ry1;
            float rz2 = rx1 * -sin_mAngleY + rz1 * cos_mAngleY;
            // multiply new positions by a z-rotation matrix
            float rx3 = rx2 * cos_mAngleZ + ry2 * -sin_mAngleZ;
            float ry3 = rx2 * sin_mAngleZ + ry2 * cos_mAngleZ;
            float rz3 = rz2;
            // set arrays to new positions
            mWordList.get(j).setLocX(rx3);
            mWordList.get(j).setLocY(ry3);
            mWordList.get(j).setLocZ(rz3);

            // add perspective
            int diameter = 2 * radius;
            float per = diameter / (diameter+rz3);
            // let's set position, scale, alpha for the tag;
            mWordList.get(j).setLoc2DX((int)(rx3 * per));
            mWordList.get(j).setLoc2DY((int)(ry3 * per));
            mWordList.get(j).setScale(per);
            mWordList.get(j).setAlpha(per / 2);
        }   
        depthSort();
    }   
    
    private void depthSort() {
        Logger.e(sTag, "Sorting the words on their weight (after):");
        Collections.sort(mWordList);    
    }
    
    private int getTextSizeGradient(float perc) {
        int size;
        size = (int)( perc*mTextSizeMax + (1-perc)*mTextSizeMin );
        return size;
    }
    
    private void sineCosine(float mAngleX,float mAngleY,float mAngleZ) {
        double degToRad = (Math.PI / 180);
        sin_mAngleX= (float) Math.sin( mAngleX * degToRad);
        cos_mAngleX= (float) Math.cos( mAngleX * degToRad);
        sin_mAngleY= (float) Math.sin( mAngleY * degToRad);
        cos_mAngleY= (float) Math.cos( mAngleY * degToRad);
        sin_mAngleZ= (float) Math.sin( mAngleZ * degToRad);
        cos_mAngleZ= (float) Math.cos( mAngleZ * degToRad);
    }
    
    public int getRadius() {
        return radius;
    }
    
    public void setRadius(int radius) {
        this.radius = radius;
    }
    
  
    public float getRvalue(float[] color) {
        if (color.length > 0) {
            return color[0];
        }
        return 0;
    }
    
    public float getGvalue(float[] color) {
        if (color.length > 0) {
            return color[1];
        }
        return 0;   
    }
    
    public float getBvalue(float[] color) {
        if (color.length > 1) {
            return color[2];
        }
        return 0;   
    }
    
    public float getAlphaValue(float[] color) {
        if (color.length >= 4) {
            return color[3];
        }
        return 0;   
    } 
    
    public float getAngleX() {
        return mAngleX;
    }
    
    public void setAngleX(float mAngleX) {
        this.mAngleX = mAngleX;
    }
    
    public float getAngleY() {
        return mAngleY;
    }
    
    public void setAngleY(float mAngleY) {
        this.mAngleY = mAngleY;
    }
    
    public void setTextSizeMax(int textSize) {
        mTextSizeMax = textSize;
    }
    
    public void setTextSizeMin(int textSize) {
        mTextSizeMin = textSize;
    }
    
    public void setCanvasWidth(int canvasWidth) {
        mCanvasWidth = canvasWidth;
    }
    
    public void setCanvasHeight(int canvasHeight) {
        mCanvasHeight = canvasHeight;
    }
    
    public int getCanvasWidth() {
        return mCanvasWidth;
    }
    
    public int getCanvasHeight() {
        return mCanvasHeight;
    }
    
    public void setImageFilePath(String filePath) {
        mImageFilePath = filePath;
    }
    
    public String getImageFilePath() {
        return mImageFilePath;
    }
    
    public void setDrawAlways(boolean flag) {
        mDrawAlways = flag;
    }
  
    public void draw() {           
        if (isCached() && !drawAlways())
            return;
        
        create(true);
        Paint paint = new Paint();
        Word word;

        float a = 1f, b = 2f;

        Rect bkRect = new Rect();
        bkRect.set(0, 0, getCanvasWidth(), getCanvasHeight());

        float centerX = getCanvasWidth() / 2;
        float centerY = getCanvasHeight() / 2 - 80;    
 
        Iterator it = iterator();

        List<Rect> rectList = new ArrayList<Rect>();
        while (it.hasNext()) {
            int i = 0;
            word = (Word) it.next();
            Logger.i(sTag, "Processing word " + word.getText());
            boolean found = false;
            float x = .0f, y = .0f;

            int tries = 0;
            while (tries < 10000 && !found) {
                i += 1;
                float angle = (float) (0.1 * i);
                x = centerX + (a + b * angle) * (float) Math.cos(angle);
                y = centerY + (a + b * angle) * (float) Math.sin(angle);

                paint.setTextSize((int)(word.getTextSize() * word.getScale()));
                paint.setTextAlign(Align.CENTER);

                Rect bounds = new Rect();
                paint.getTextBounds(word.getText(), 0, word.getText().length(), bounds);
                Rect finalBounds = new Rect(bounds.left - bounds.width()/2 + (int)x, 
                        bounds.top + (int)y, 
                        bounds.right - bounds.width()/2 + (int)x, 
                        bounds.bottom + (int)y);

                if (!bkRect.contains(finalBounds) || intersect(finalBounds, rectList)) {
                    tries++;
                } else {
                    rectList.add(finalBounds);
                    found = true;
                    Logger.w(sTag, "Free space found for centreX:" + x + ", centreY:" + y );
                    word.setX(x);
                    word.setY(y);
                }
            } 
        }
    }
 
    public boolean intersect(Rect rect, List<Rect> rectList) {
        for (Rect r : rectList) {
            if (Rect.intersects(rect, r))
                return true;
        }
        return false;
    }  
    
    public boolean isCached() {
        return Utils.fileExists(mImageFilePath);
    }
    
    public boolean drawAlways() {
       return mDrawAlways;
    }
}