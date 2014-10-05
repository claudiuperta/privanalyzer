package uk.ac.qmul.eecs.privanalyzer.db;

import uk.ac.qmul.eecs.privanalyzer.Session;
import uk.ac.qmul.eecs.privanalyzer.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

public class ApplicationsDatabase {
    
    private static final String sTag = ApplicationsDatabase.class.getName();
    
    private static final String sDatabaseName = "applications_db";
    private static final int sDatabaseVersion = 2;
  
    private static final String sColumName = "name";
    private static final String sColumPositionX = "position_x";
    private static final String sColumPositionY = "position_y";
    private static final String sColumTextSize = "text_size";
    
    private static final String sColumnTimestamp = "timestamp";
    private static final String sColumnType = "type";
    private static final String sColumnCellId = "cell_id";
    private static final String sColumnSpeed = "speed";
    private static final String sColumnTotalCalls = "total_calls";
    private static final String sColumnTotalSMS = "total_sms";
    
    private static final String sApplicationsTable = "Applications";
    private static final String sPermissionsTable = "Permissions";
   
    private final ApplicationsDatabaseHelper mDatabaseOpenHelper;
    
    private static final HashMap<String,String> mApplicationsColumnMap = buildApplicationsColumnMap();
    private static final HashMap<String,String> mPermissionsColumnMap = buildPermissionColumnMap();
    
    public ApplicationsDatabase(Context context) {
        if (context == null) {
            Logger.d(sTag, "null context in constructor");
        }
        mDatabaseOpenHelper = new ApplicationsDatabaseHelper(context);
    }
      
    public ArrayList<MeasurementEntry> getLastResults(int count) {
        Logger.d(sTag, "Getting last results");
        ArrayList<MeasurementEntry> results = new ArrayList<MeasurementEntry>();
        long now = System.currentTimeMillis();
        
        // Get results for the last 3 days.    
        long oldTimestamp = (now / 1000) - (3 * 24 * 60 * 60);
        String selection = "timestamp > " + oldTimestamp;
        String sortOrder = "timestamp DESC";
        
        Cursor cursor = query(sApplicationsTable, null, selection, null, null, null, sortOrder);
        if (cursor == null) {
            Logger.d(sTag, "Cursor is null!");
        } else {
            if (count > Session.MAX_RESULTS_VIEW) {
                count = Session.MAX_RESULTS_VIEW;
            }
            cursor.moveToFirst();
            results.add(resultFromCursor(cursor));
            while (cursor.moveToNext() && count-- > 0) {
                results.add(resultFromCursor(cursor));
            }
        }
        return results;
    }
 
    public ArrayList<FileInfo> getUploadFiles() {
        Logger.d(sTag, "Getting list of files to be uploaded...");
        ArrayList<FileInfo> uploadFiles = new ArrayList<FileInfo>();
        long now = System.currentTimeMillis();
        
        String selection = "timestamp == 0";
        String sortOrder = "id DESC";
        
        Cursor cursor = query(sPermissionsTable, null, selection, null, null, null, sortOrder);
        if (cursor == null) {
            Logger.d(sTag, "Cursor is null!");
        } else {
          
            cursor.moveToFirst();
            uploadFiles.add(fileInfoFromCursor(cursor));
            while (cursor.moveToNext()) {
                uploadFiles.add(fileInfoFromCursor(cursor));
            }
        }
        return uploadFiles;
    }
    
    public MeasurementEntry resultFromCursor(Cursor cursor) {
        MeasurementEntry result = new MeasurementEntry();
        result.setTimestamp(cursor.getLong(cursor.getColumnIndex(sColumnTimestamp)));
        result.setType(cursor.getString(cursor.getColumnIndex(sColumnType)));
        result.setCellId(cursor.getInt(cursor.getColumnIndex(sColumnCellId)));
        result.setTotalCalls(cursor.getInt(cursor.getColumnIndex(sColumnTotalCalls)));
        result.setTotalSMS(cursor.getInt(cursor.getColumnIndex(sColumnTotalSMS)));
        result.setSpeed(cursor.getDouble(cursor.getColumnIndex(sColumnSpeed)));
        
        // TODO(claudiu)
        // ...
        return result;
    }
    
    public FileInfo fileInfoFromCursor(Cursor cursor) {
        FileInfo info = new FileInfo();
        info.setFileName(cursor.getString(cursor.getColumnIndex("filename")));
        info.setId(cursor.getInt(cursor.getColumnIndex("id")));

        return info;
    }
    
    // This should be called after 
    public synchronized Boolean insertFileInfo(FileInfo fileInfo) {
        SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
        if (db == null) {
            Logger.d(sTag, "Could not open database for writing");
            return false;
        }
        
        ContentValues initialValues = new ContentValues();
        initialValues.put("id", fileInfo.getId());
        initialValues.put("timestamp", fileInfo.getTimestamp());
        initialValues.put("filename", fileInfo.getFileName());
          
        long position =  db.insert(sPermissionsTable, null, initialValues);
        if (position == -1) {
            Logger.d(sTag, "Could not insert result");
            return false;
        }
        Logger.d(sTag, "Result inserted at position " + position);
        return true;
    }
    
    // This should be called after the file is uploaded.
    public synchronized Boolean updateFileInfo(FileInfo fileInfo) {
        SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
        if (db == null) {
            Logger.d(sTag, "Could not open database for writing");
            return false;
        } 
        ContentValues values = new ContentValues();
        values.put("timestamp", fileInfo.getTimestamp());
        values.put("filename", fileInfo.getFileName());
                 
        String whereArgs[] = new String[1];
        whereArgs[0] = "" + fileInfo.getId();
        
        long position =  db.update(sPermissionsTable, values, "id = ?", whereArgs);
        if (position == -1) {
            Logger.d(sTag, "Could not update file info");
            return false;
        }
        Logger.d(sTag, "File info updated at position " + position);
        return true;
    }
     
    public synchronized Boolean insertResult(MeasurementEntry result) {
        SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
        if (db == null) {
            Logger.d(sTag, "Could not open database for writing");
            return false;
        } 
        ContentValues initialValues = new ContentValues();
        initialValues.put(sColumnTimestamp, result.getTimestamp());
        initialValues.put(sColumnType, result.getType());
       
        
        long position =  db.insert(sApplicationsTable, null, initialValues);
        if (position == -1) {
            Logger.d(sTag, "Could not insert result");
            return false;
        }
        Logger.d(sTag, "Result inserted at position " + position);
        return true;
    }
    
    /**
    * Builds a map for all columns that may be requested, which will be given to the
    * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include
    * all columns, even if the value is the key. This allows the ContentProvider to request
    * columns w/o the need to know real column names and create the alias itself.
    */
    private static HashMap<String,String> buildApplicationsColumnMap() {
        HashMap<String,String> map = new HashMap<String,String>();
        map.put(sColumnTimestamp, sColumnTimestamp);
        map.put(sColumnType, sColumnType);
        map.put(sColumnCellId, sColumnCellId);
        map.put(sColumnTotalCalls, sColumnTotalCalls);
        map.put(sColumnTotalSMS, sColumnTotalSMS);
        map.put(sColumnSpeed, sColumnSpeed);
        
        // TODO(claudiu)
        // ...
        map.put(BaseColumns._ID, "rowid AS " +
                BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
        return map;
    }
    
    /**
     * Builds a map for all columns that may be requested, which will be given to the
     * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include
     * all columns, even if the value is the key. This allows the ContentProvider to request
     * columns w/o the need to know real column names and create the alias itself.
     */
     private static HashMap<String,String> buildPermissionColumnMap() {
         HashMap<String,String> map = new HashMap<String,String>();
         map.put("id", "id");
         map.put("timestamp", "timestamp");
         map.put("filename", "filename");
 
         map.put(BaseColumns._ID, "rowid AS " +
                 BaseColumns._ID);
         map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
                 SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
         map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS " +
                 SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
         return map;
     }
     
        
    /**
    * Performs a database query.
    * @param selection The selection clause
    * @param selectionArgs Selection arguments for "?" components in the selection
    * @param columns The columns to return
    * @return A Cursor over all rows matching the query
    */
    private Cursor query(String table, String[] projectionIn, String selection, String selectionArgs[], 
                                    String[] groupBy, String having, String sortOrder) {
        /* The SQLiteBuilder provides a map for all possible columns requested to
        * actual columns in the database, creating a simple column alias mechanism
        * by which the ContentProvider does not need to know the real column names
        */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        if (table.equalsIgnoreCase(sApplicationsTable)) {
            builder.setTables(sApplicationsTable);
            builder.setProjectionMap(mApplicationsColumnMap);
        
        } else if (table.equalsIgnoreCase(sPermissionsTable)) {
            builder.setTables(sPermissionsTable);
            builder.setProjectionMap(mPermissionsColumnMap);            
        } else {
             return null;
        }
        
        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                projectionIn, selection, groupBy, having, null, sortOrder);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    private class ApplicationsDatabaseHelper extends SQLiteOpenHelper {
   
        private static final String sFilesTableCreate =
                "CREATE TABLE IF NOT EXISTS " + sPermissionsTable + " ( " +
                "id INT(11) PRIMARY KEY, timestamp INT(11), " + "filename" + " TEXT, UNIQUE(id) ON CONFLICT IGNORE);";
                 
        private static final String sMeasurementsTableCreate =
                                      "CREATE TABLE IF NOT EXISTS " + sApplicationsTable + " (" +
                                      sColumnTimestamp + " INT(11), " + sColumnType + " TEXT, "   +
                                      sColumnCellId + " INT(11), " + sColumnTotalCalls + " INT(11), " +
                                      sColumnTotalSMS + " INT(11), " + sColumnSpeed + " DOUBLE);";
             
        // TODO(claudiu)
        // ...
        ApplicationsDatabaseHelper(Context context) {
            super(context, sDatabaseName, null, sDatabaseVersion);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(sMeasurementsTableCreate);
            db.execSQL(sFilesTableCreate);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Logger.w(sTag, "Upgrading database from version " + oldVersion + " to "
                            + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + sApplicationsTable);
            onCreate(db);
        }
    }
}