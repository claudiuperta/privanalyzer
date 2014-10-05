package uk.ac.qmul.eecs.privanalyzer.util;

import uk.ac.qmul.eecs.privanalyzer.Session;
import uk.ac.qmul.eecs.privanalyzer.Word;
import uk.ac.qmul.eecs.privanalyzer.WordCloud;
import uk.ac.qmul.eecs.privanalyzer.db.FileInfo;
import uk.ac.qmul.eecs.privanalyzer.logging.Logger;
import uk.ac.qmul.eecs.privanalyzer.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import java.util.regex.*;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONTokener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.util.Base64;


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Utils {
    
    private static final String sTag = Utils.class.getName();
    private static final String sColor1 = "#238b45";
    private static final String sColor2 = "#00FF00";
    private static final String sColor3 = "#FE9A2E";
    private static final String sColor4 = "#FE642E";
    private static final String sColor5 = "#FF0000";
    
    public static String getTimestamp() {
        String dateTimeString = new String("yyyy-MM-dd_HH:mm:ss");
        SimpleDateFormat sdf = new SimpleDateFormat(dateTimeString, Locale.US);
        String timestamp = sdf.format(new Date());
        
        return timestamp;
    }

    public static String getFileTimestamp() {
        String dateTimeString = new String("yyyy-MM-dd_HH-mm-ss");
        SimpleDateFormat sdf = new SimpleDateFormat(dateTimeString, Locale.US);
        String timestamp = sdf.format(new Date());
        
        return timestamp;
    }
    
    public static void setFileExecutable(String path) throws Exception {
        String command = "chmod 744 " + path;
        Process p = Runtime.getRuntime().exec(command);
        int ret = p.waitFor();
        if (ret != 0) {
            throw new Exception("Command: " + command + " failed with return value " + ret);
        }
    }
    
    public static void copy(File src, File dst) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(src));
            bos = new BufferedOutputStream(new FileOutputStream(dst));
            copy(bis, bos);
        } finally {
            if (bis != null) bis.close();
            if (bos != null) bos.close();
        }
    }
    
    public static void copy(InputStream src, OutputStream dst) throws IOException {
        byte[] buff = new byte[256 * 1024];
        int nread;
        while ((nread = src.read(buff)) != -1) {
            dst.write(buff, 0, nread);
        }
    }
    
    public static void copy(File src, OutputStream dst) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(src));
            copy(bis, dst);
        } finally {
            if (bis != null) bis.close();
        }
    }
    
    public static void copy(InputStream src, File dst) throws IOException {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(dst));
            copy(src, bos);
        } finally {
            if (bos != null) bos.close();
        }
    }
    
    public static void compressFile(File src, File dst) throws IOException {
        BufferedInputStream bis = null;
        GZIPOutputStream gzos = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(src));
            gzos = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(dst)));
        
            byte[] buff = new byte[256 * 1024];
            int nread;
            while ((nread = bis.read(buff)) != -1) {
                gzos.write(buff, 0, nread);
            }
        } finally {
            if (bis != null) bis.close();
            if (gzos != null) gzos.close();
        }
    }
    
    public static byte[] compressString(String src) throws IOException {
        InputStream srcStream = new ByteArrayInputStream(src.getBytes());
        ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
        GZIPOutputStream gzos = null;
        try {
            gzos = new GZIPOutputStream(dstStream);

            byte[] buff = new byte[256 * 1024];
            int nread;
            while ((nread = srcStream.read(buff)) != -1) {
                gzos.write(buff, 0, nread);
            }
            
            gzos.close();
            gzos = null;
            
            return dstStream.toByteArray();
        } finally {
            srcStream.close();
            dstStream.close();
            if (gzos != null) gzos.close();
        }
    }
    
    public static void sendCompressedJSON(DataOutputStream dos, JSONObject obj)
        throws IOException {
        byte[] result = compressString(obj.toJSONString());
        dos.writeBytes(result.length + "\n");
        dos.write(result);
    }
    
    @SuppressWarnings("unchecked")
    public static JSONArray toJSONArray(long[] values) {
        JSONArray array = new JSONArray();
        for (long val: values) {
            array.add(val);
        }
        return array;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> JSONArray toJSONArray(List<T> values) {
        JSONArray array = new JSONArray();
        for (T val: values) {
            array.add(val);
        }
        return array;
    }
    
    public static boolean isSDCardWriteable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return false;
        } else {
            return false;
        }
    }
    
    public static File getExternalFilesDir() {
        Context context = Session.getGlobalContext();
        String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
        File filesDir = new File(sdcard + "/" + context.getString(R.string.externalFilesDir));
        filesDir.mkdirs();
        return filesDir;
    }
    
    public static File moveFileToSDCard(File file) throws IOException {
        if (isSDCardWriteable() == false)
            throw new IOException("External files directory is not writeable");
        File filesDir = getExternalFilesDir();
        File output = new File(filesDir.getAbsolutePath() + "/" + file.getName());
        copy(file, output);
        file.delete();
        return output;
    }
    
    public static void deleteFile(String filepath) {
        if (filepath != null) {
            File file = new File(filepath);
            file.delete();
        }
    }
    
    // See: http://andy-malakov.blogspot.it/2010/06/alternative-to-threadsleep.html
    private static final long sSleepPrecision = TimeUnit.MILLISECONDS.toNanos(1);    // TODO: tune
    private static final long sSpinYieldPrecision = TimeUnit.NANOSECONDS.toNanos(1); // TODO: tune
    
    public static void sleepMillisBusyWait(long millisDuration) {
        long start = System.nanoTime();
        long now = start;
        while (TimeUnit.NANOSECONDS.toMillis(now - start) < millisDuration) {
            now = System.nanoTime();
        }
    }
    
    public static void sleepMillis(long millisDuration) throws InterruptedException {
        sleepNanos(TimeUnit.MILLISECONDS.toNanos(millisDuration));
    }
    
    public static void sleepMillisWithSpin(long millisDuration) throws InterruptedException {
        sleepNanosWithSpin(TimeUnit.MILLISECONDS.toNanos(millisDuration));
    }
    
    public static void sleepNanos(long nanoDuration) throws InterruptedException {
        final long end = System.nanoTime() + nanoDuration;
        long timeLeft = nanoDuration;
        do {
            if (timeLeft > sSleepPrecision) {
                Thread.sleep(1);
            } else {
                // Equivalent to Thread.yield()
                Thread.sleep(0);
            }
            timeLeft = end - System.nanoTime();
            if (Thread.interrupted())
                throw new InterruptedException();
        } while (timeLeft > 0);
    }
    
    public static void sleepNanosWithSpin(long nanoDuration) throws InterruptedException { 
        final long end = System.nanoTime() + nanoDuration;
        long timeLeft = nanoDuration; 
        do { 
            if (timeLeft > sSleepPrecision) {
                Thread.sleep(1); 
            }
            else if (timeLeft > sSpinYieldPrecision) {
                // Equivalent to Thread.yield()
                Thread.sleep(0);
            }
            timeLeft = end - System.nanoTime(); 
            if (Thread.interrupted()) 
                throw new InterruptedException(); 
        } while (timeLeft > 0); 
    }
    
    private static WakeLock mWakeLock = null;
    
    public static synchronized void acquireWakeLock() {
      if (mWakeLock == null) {
        PowerManager pm = (PowerManager) Session.getGlobalContext()
                                        .getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tag");
      }
      mWakeLock.acquire();
    }

    public static synchronized void releaseWakeLock() {
      if (mWakeLock != null) {
        try {
          mWakeLock.release();
        } catch (RuntimeException e) {
          Logger.e(sTag, "Exception when releasing wakeup lock", e);
        }
      }
    }

    protected static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    public static double average(long[] values) {
        
        if (values.length == 0) {
            return 0.0;
        }
        
        double av = 0;
        for (long v: values) {
            av += v;
        }
        av = av / values.length;
        return av;
    }
    
    public static double variance(long[] values) {
        
        if (values.length == 0)
            return 0.0;
        
        double av = average(values);
        double var = 0.0;
        for (long v: values) {
            var += (v - av) * (v - av);
        }
        var = Math.sqrt(var / values.length);
        return var;
    }
    
    public static long[] quartiles(long[] values) {
        
        long[] q = new long[5];
        if (values.length == 0) {
            return q;
        }

        Arrays.sort(values);
        q[0] = values[0];
        q[1] = values[(int) (values.length * 0.25)];
        q[2] = values[(int) (values.length * 0.5)];
        q[3] = values[(int) (values.length * 0.75)];
        q[4] = values[values.length - 1];
        
        return q;
    }
    
    public static double ms_to_s(long millis) {
        return millis / 1000.0;
    }
    
    public static int translateGSMRssi(int rssi) {
        if (rssi != NeighboringCellInfo.UNKNOWN_RSSI) {
            return -113 + 2 * rssi;
        }
        return rssi;
    }
    
    @SuppressWarnings("unchecked")
    public static JSONObject dumpPreferences() {
        
        JSONObject dump = new JSONObject();
        
        Context context = Session.getGlobalContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs != null) {
            for (Map.Entry<String,?> entry: prefs.getAll().entrySet()) {
                dump.put(entry.getKey(), entry.getValue());
            }
        }
        return dump;
    }
    
    @SuppressLint("SimpleDateFormat")
	public static String timestampToString(long timestamp) {
        if (timestamp == 0) {
            return "";
        }
        Date d = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM, HH:mm");
        return sdf.format(d);
    }
    
    public static boolean uploadFile(String fileName) {
        Logger.d(sTag, "Uploading file " + fileName);
        String uploadURL = Utils.getUploadURL();
        if (uploadURL == null)
            return false;
        
        String filePath = Utils.getExternalFilesDir().getAbsolutePath() + "/" + fileName;              
        return uploadFile(filePath, fileName, uploadURL);
    }
    
    public static boolean uploadFile(String filePath, String fileName, String uploadURL) {
        HttpURLConnection conn = null;
        DataOutputStream dos = null;  
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024; 
        File sourceFile = new File(filePath); 

        if (!sourceFile.isFile()) {
            Logger.e(sTag, "File does not exist " + filePath);
            return false;          
        }
        
        try { 
            FileInputStream fileInputStream = new FileInputStream(sourceFile);
            URL url = new URL(uploadURL);

            // Open a HTTP  connection to  the URL
            conn = (HttpURLConnection) url.openConnection(); 
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("uploaded_file", fileName); 
            String basic_auth = new String(Base64.encode(("stupiduser" + ":" + "Stupid100Password.").getBytes(), 
                    Base64.DEFAULT));
            conn.setRequestProperty("Authorization", "Basic " + basic_auth);
            
            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd); 
            dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                    + filePath + "\"" + lineEnd);

            dos.writeBytes(lineEnd);

            // create a buffer of  maximum size
            bytesAvailable = fileInputStream.available(); 

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);  

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            int serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();

            Logger.i(sTag, "HTTP Response is : "
                    + serverResponseMessage + ": " + serverResponseCode);

            //close the streams //
            fileInputStream.close();
            dos.flush();
            dos.close();
            
            return true;
            
        } catch (MalformedURLException ex) {
            Logger.e(sTag, "Exception when uploading file: " + ex.getMessage(), ex);  
        } catch (Exception e) {
            Logger.e(sTag, "Exception when uploading file: " + e.getMessage(), e);  
        }
        
        return false;
    } 
       
    public static String getUploadURL() {
        String uploadURL = null;
        
        try {
            String url = "http://json-dns.appspot.com/profiler";
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResponse = httpClient.execute(httpGet);

            // Handle response back from script.
            if(httpResponse != null) {
                Logger.d(sTag, httpResponse.getStatusLine().toString());
                JSONParser parser = new JSONParser();
                JSONObject data = (JSONObject) parser.parse(
                        IOUtils.toString(httpResponse.getEntity().getContent()));
                String fqdn = (String) data.get("fqdn");
                String IP = (String) data.get("ip");
                Logger.d(sTag, "fqdn: " + fqdn + ", IP: " + IP);
                uploadURL = "http://" + fqdn + "/profiler/upload.php";
            } else { // Error, no response.
                Logger.e(sTag, "No response from server.");
            }
        } catch (IOException e) {
            Logger.e(sTag, e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            Logger.e(sTag, e.getMessage());
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            Logger.e(sTag, e.getMessage());
            e.printStackTrace();
        }
        
        return uploadURL;
    }
       
    public static String getDeviceId() {
        Context context = Session.getGlobalContext();
        TelephonyManager TelephonyMgr = (TelephonyManager)context.getSystemService(
                Context.TELEPHONY_SERVICE);
        return TelephonyMgr.getDeviceId();
    }
      
    public static FileInfo getLogFileInfo() {
        FileInfo fileInfo = new FileInfo();
        long day = System.currentTimeMillis() / (1000 * 24 * 3600);   
        fileInfo.setFileName(getDeviceId() + "_" + day + ".log");
        fileInfo.setTimestamp(0);
        fileInfo.setId(day);
        
        return fileInfo;
    }
    public static String getLogFileName() {
        long day = System.currentTimeMillis() / (1000 * 24 * 3600);       
        return getDeviceId() + "_" + day + ".log";
    }
    
    public static String getLogFileNameUpload() {
        long day = System.currentTimeMillis() / (1000 * 24 * 3600);
        return getDeviceId() + "_" + (day - 1) + ".log";
    }
    
    public static boolean compressFile(String src, String dst) {
       File srcFile = new File(src);
       File dstFile = new File(dst);
       
       try {
        compressFile(srcFile, dstFile);
        return true;
       } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        Logger.e(sTag, "Exception while compressing " + src + ": " + e.getMessage());
        
        return false;
       }
    }
  
    public static List<Word> getPermissionsWordCloud() {
        List<PackageInfo> packageList = Session.getGlobalContext().getPackageManager().getInstalledPackages(
                PackageManager.GET_PERMISSIONS);
        
        Map<String, Integer> permissionsMap = new HashMap<String, Integer>(); 
        
        Logger.i(sTag, "Getting list of installed apps...found " + packageList.size() + " packages."); 
        for (int i = 0; i < packageList.size(); i++) {
            PackageInfo packageInfo = packageList.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = packageInfo.applicationInfo.loadLabel(
                           Session.getGlobalContext().getPackageManager()).toString();                
                String permissions = " Permissions: ";
                if (packageInfo.requestedPermissions != null) {
                    for (String permission : packageInfo.requestedPermissions) {
                        String parts[] = permission.split("\\.");
                        String key = parts[parts.length - 1];
                        Integer value = permissionsMap.get(key);
                        if (value == null) {
                            value = 0;
                        }
                        value++;
                        permissionsMap.put(key, value);
                    }
                }
            }
        }
        
        List<Word> words = new ArrayList<Word>();
        for (Map.Entry<String, Integer> entry : permissionsMap.entrySet()) {
            Word word = new Word(entry.getKey(), entry.getValue());
            word.setColor(getPermissionColor(entry.getKey()));
            words.add(word);
        }
        
        return words;
    }
    
    
    public static List<Word> getAppsWordCloud() {
        
        List<PackageInfo> packageList = Session.getGlobalContext().getPackageManager().getInstalledPackages(
                PackageManager.GET_PERMISSIONS);
        
        Map<String, Integer> applicationsMap = new HashMap<String, Integer>(); 
        List<Word> words = new ArrayList<Word>();
        
        for (int i = 0; i < packageList.size(); i++) {
            PackageInfo packageInfo = packageList.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String key = packageInfo.applicationInfo.loadLabel(
                           Session.getGlobalContext().getPackageManager()).toString();
                Integer value = 0;
                if (packageInfo.requestedPermissions != null) {
                    value = packageInfo.requestedPermissions.length;
                    String wordColor = sColor1;
                    for (String permission : packageInfo.requestedPermissions) {
                        String parts[] = permission.split("\\.");
                        String k = parts[parts.length - 1];
                        String color = getPermissionColorString(k);
                        Logger.i(sTag, "Got color: " + color + ", Previous color: " + wordColor);
                        if (compareColors(color, wordColor) > 0) {
                            wordColor = color;
                        }
                        Logger.i(sTag, "Current color: " + wordColor);
                    }
                    Logger.i(sTag, "Using color: " + wordColor);
                    Word word = new Word(key, value);
                    word.setColor(Color.parseColor(wordColor));
                    words.add(word);
                }
                applicationsMap.put(key, value);  
            }
        }
        return words;
    }
    
    public static WordCloud updateWordCloud(WordCloud wordCloud) {

        Paint paint = new Paint();
        Word word;

        float a = 1f, b = 2f;

        Rect bkRect = new Rect();
        bkRect.set(0, 0, wordCloud.getCanvasWidth(), wordCloud.getCanvasHeight());

        float centerX = wordCloud.getCanvasWidth() / 2;
        float centerY = wordCloud.getCanvasHeight() / 2 - 80;    
 
        Iterator it = wordCloud.iterator();

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
        return wordCloud;
    }
 
    public static boolean intersect(Rect rect, List<Rect> rectList) {
        for (Rect r : rectList) {
            if (Rect.intersects(rect, r))
                return true;
        }
        return false;
    }
      
    public static int getPermissionColor(String word) {
        return Color.parseColor(getPermissionColorString(word));                                                                                                       
    }
    
    public static String getPermissionColorString(String word) {
        if (word.equalsIgnoreCase("ACCESS_FINE_LOCATION"))
            return sColor5;
        if (word.equalsIgnoreCase("READ_HISTORY_BOOKMARKS"))
            return sColor4;
        if (word.equalsIgnoreCase("WRITE_HISTORY_BOOKMARKS"))
            return sColor4;
        if (word.equalsIgnoreCase("ACCESS_COARSE_LOCATION"))
            return sColor4;
        if (word.equalsIgnoreCase("READ_CALL_LOG"))
            return sColor5;
        if (word.equalsIgnoreCase("READ_CONTACTS"))
            return sColor5;
        if (word.equalsIgnoreCase("READ_PROFILE"))
            return (sColor5);
        if (word.equalsIgnoreCase("READ_SMS"))
            return (sColor5);
        if (word.equalsIgnoreCase("READ_SOCIAL_STREAM"))
            return (sColor5);
        if (word.equalsIgnoreCase("RECEIVE_MMS"))
            return (sColor5);
        if (word.equalsIgnoreCase("RECEIVE_SMS"))
            return (sColor5);
        if (word.equalsIgnoreCase("SEND_SMS"))
            return (sColor5);
        if (word.equalsIgnoreCase("WRITE_SMS"))
            return (sColor5);
        if (word.equalsIgnoreCase("ACCESS_SUPERUSER"))
            return (sColor5);
        if (word.equalsIgnoreCase("WRITE_CONTACTS"))
            return (sColor5);
        if (word.equalsIgnoreCase("WRITE_PROFILE"))
            return (sColor5);
        if (word.equalsIgnoreCase("WRITE_SOCIAL_STREAM"))
            return (sColor5);
        if (word.equalsIgnoreCase("WRITE_CALL_LOG"))
            return (sColor5);
        if (word.equalsIgnoreCase("USE_CREDENTIALS"))
            return (sColor5);
        if (word.equalsIgnoreCase("KILL_BACKGROUND_PROCESSES"))
            return (sColor5);
        if (word.equalsIgnoreCase("MANAGE_ACCOUNTS"))
            return (sColor5);       
        if (word.equalsIgnoreCase("BILLING"))
            return (sColor5);       
        if (word.equalsIgnoreCase("WRITE_SECURE_SETTINGS"))
            return (sColor5);
        if (word.equalsIgnoreCase("READ_CALENDAR"))
            return (sColor4);
        if (word.equalsIgnoreCase("INTERNET"))
            return (sColor2);
        if (word.equalsIgnoreCase("INSTALL_PACKAGES"))
            return (sColor5);
        if (word.equalsIgnoreCase("GET_ACCOUNTS"))
            return (sColor5);
        if (word.equalsIgnoreCase("CHANGE_NETWORK_STATE"))
            return (sColor3);
        if (word.equalsIgnoreCase("CHANGE_WIFI_STATE"))
            return (sColor3);
        if (word.equalsIgnoreCase("ACCESS_WIFI_STATE"))
            return (sColor2);
        if (word.equalsIgnoreCase("ACCESS_NETWORK_STATE"))
            return (sColor2);
        if (word.equalsIgnoreCase("ACCOUNT_MANAGER"))
            return (sColor3);
        if (word.equalsIgnoreCase("AUTHENTICATE_ACCOUNTS"))
            return (sColor4);
        if (word.equalsIgnoreCase("BLUETOOTH"))
            return (sColor2);
        if (word.equalsIgnoreCase("BLUETOOTH_ADMIN"))
            return (sColor3);
        if (word.equalsIgnoreCase("CALL_PHONE"))
            return (sColor4);
        if (word.equalsIgnoreCase("CALL_PRIVILEGED"))
            return (sColor4);
        if (word.equalsIgnoreCase("DELETE_PACKAGES"))
            return (sColor5);
        
        return (sColor1);                                                                                                       
    }
    
    public static int compareColors(String lhs, String rhs) {
        
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put(sColor1, 1);
        map.put(sColor2, 2);
        map.put(sColor3, 3);
        map.put(sColor4, 4);
        map.put(sColor5, 5);
        
        return map.get(lhs) - map.get(rhs);
    }
    
    public static boolean fileExists(String filePath) {
        if (filePath != null) {
            File f = new File(filePath);
            if (f.exists() && f.length() != 0) {
                return true;
            }
        }
        return false;
    }
      
    @SuppressWarnings("unchecked")
    public static JSONObject getApplicationList() {
        
        List<PackageInfo> packageList = Session.getGlobalContext().getPackageManager().getInstalledPackages(
                PackageManager.GET_PERMISSIONS);
              
        Logger.i(sTag, "Getting list of installed apps...found " + packageList.size() + " packages."); 
        
        JSONArray applicationList = new JSONArray();
        for (int i = 0; i < packageList.size(); i++) {
            PackageInfo packageInfo = packageList.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                JSONObject app = new JSONObject();
                app.put("package_name",  packageInfo.applicationInfo.packageName);
                app.put("application_name", packageInfo.applicationInfo.loadLabel(
                           Session.getGlobalContext().getPackageManager()).toString());
                JSONArray permissionList = new JSONArray();
                if (packageInfo.requestedPermissions != null) {
                    for (String permission : packageInfo.requestedPermissions) {
                      permissionList.add(permission);
                    }
                }
                app.put("permissions", permissionList);
                applicationList.add(app);
            }
        }
       
        JSONObject applications = new JSONObject();
        applications.put("timestamp",  Utils.ms_to_s(System.currentTimeMillis()));
        applications.put("applications", applicationList);
        
        return applications;
    }
    
    @SuppressWarnings("unchecked")
    public static JSONObject getBrowsingHistory() {     
        String title = "";
        String url = "";
        
        String[] proj = new String[] { 
                Browser.BookmarkColumns.TITLE,
                Browser.BookmarkColumns.URL 
        };
        
        // 0 = history
        // 1 = bookmark
        String sel = Browser.BookmarkColumns.BOOKMARK + " = 0";
        Cursor mCur = Session.getGlobalContext().getContentResolver().query(
                Browser.BOOKMARKS_URI, proj,
                sel, null, null);
        mCur.moveToFirst();
        
        JSONArray browsingHistoryList = new JSONArray();
        if (mCur.moveToFirst() && mCur.getCount() > 0) {
            boolean cont = true;
            while (mCur.isAfterLast() == false && cont) {
                title = mCur.getString(mCur
                        .getColumnIndex(Browser.BookmarkColumns.TITLE));
                url = mCur.getString(mCur
                        .getColumnIndex(Browser.BookmarkColumns.URL));
                
                JSONObject item = new JSONObject();
                item.put("title", title);
                item.put("url", url);
                browsingHistoryList.add(item);
                
                Logger.d(sTag, "title: " + title + "url: " + url);
                mCur.moveToNext();
            }
        }

        mCur.close();
        
        JSONObject browsingHistory = new JSONObject();
        browsingHistory.put("browsing_history", browsingHistoryList);
        
        return browsingHistory;
    }
    
    @SuppressWarnings("unchecked")
    private static void appendJSON(JSONObject json, String filename) {
        if (json != null && filename != null) {
            try {               
                String data = json.toJSONString();
                File filesDir = getExternalFilesDir();
                FileWriter fw = new FileWriter(filesDir.getAbsolutePath() + "/" + filename, true);
                fw.write(data + "\n");
                fw.close();
            } catch (FileNotFoundException e) {
                Logger.e(sTag, "File not found: " + filename);
                e.printStackTrace();
            } catch (IOException e) {
                Logger.e(sTag, "IO exception while appending to " + filename);
                e.printStackTrace();
            }
        }       
    }
}
