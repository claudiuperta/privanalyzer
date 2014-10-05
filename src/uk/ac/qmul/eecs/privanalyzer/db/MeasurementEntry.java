package uk.ac.qmul.eecs.privanalyzer.db;

public class MeasurementEntry {
    private long mTimestamp = 0;
    private String mType = "";
    private long mCellId = 0;
    private double mSpeed = 0.0;
    private long mTotalCalls = 0;
    private long mTotalSMS = 0;
    private long mTotalBytesSent = 0;
    private long mTotalBytesReceived = 0;
    private long mTotalEmailsSent = 0;
    
    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }
    
    public long getTimestamp() {
        return mTimestamp;
    }
    
    public void setType(String type) {
        mType = type;
    }
    
    public String getType() {
        return mType;
    }
    
    public void setCellId(long cellId) {
        mCellId = cellId;
    }
    
    public long getCellId() {
        return mCellId;
    }
    public void setTotalCalls(int totalCalls) {
        mTotalCalls = totalCalls;
    }
    
    public long getTotalCalls() {
        return mTotalCalls;
    }
    
    public void setTotalSMS(int totalSMS) {
        mTotalSMS = totalSMS;
    }
    
    public long getTotalSMS() {
        return mTotalSMS;
    }
    
    public void setSpeed(double speed) {
        mSpeed = speed;
    }
    
    public double getSpeed() {
        return mSpeed;
    }
}
