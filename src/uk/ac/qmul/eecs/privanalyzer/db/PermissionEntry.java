package uk.ac.qmul.eecs.privanalyzer.db;

public class PermissionEntry {
    
    private String mName = "";
    private long mTimestamp = 0;
    private int mPositionX = 0;
    private int mPositionY = 0;
    private int mTextSize = 0;
    
    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }
    
    public long getTimestamp() {
        return mTimestamp;
    }
    
    public void setName(String name) {
        mName = name;
    }
    
    public String getName() {
        return mName;
    }
    
    public void setPositionX(int x) {
        mPositionX = x;
    }
    
    public int getPositionX() {
        return mPositionX;
    }
    
    public void setPositionY(int y) {
        mPositionY = y;
    }
    
    public int getPositionY() {
        return mPositionY;
    }
    
    public void setTextSize(int textSize) {
        mTextSize = textSize;
    }
    
    public int getTextSize() {
        return mTextSize;
    }  
}


