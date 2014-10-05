package uk.ac.qmul.eecs.privanalyzer.db;

public class FileInfo {
    private String mFileName;
    private long mIndex;
    private long mTimestamp;
    
    public FileInfo() {
        
    }
    
    public String getFileName() {
        return mFileName;
    }
    
    public void setFileName(String fileName) {
        mFileName = fileName;
    }
    
    public long getId() {
        return mIndex;
    }
    
    public void setId(long day) {
        mIndex = day;
    }
    
    public long getTimestamp() {
        return mTimestamp;
    }
    
    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }
}
