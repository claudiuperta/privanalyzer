package uk.ac.qmul.eecs.privanalyzer.logging;

import uk.ac.qmul.eecs.privanalyzer.Session;
import uk.ac.qmul.eecs.privanalyzer.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Wrapper for logging operations which can be disabled by setting LOGGING_ENABLED to false.
 */
public class Logger {
    
  private final static boolean LOGGING_ENABLED = true;
  private final static boolean sLoggingToFileEnabled = false;
  private static FileLogger sFileLogger = null;
  
  private static void initFileLogger() {
      if (sFileLogger != null) {
          return;
      }
      
      Context context = Session.getGlobalContext();
      if (context != null) {
          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
          long maxSz = prefs.getInt(context.getString(R.string.systemLogRotateSzKey),
                         context.getResources().getInteger(R.integer.systemLogRotateSzDefault));
          int rotationSeconds = context.getResources()
                                        .getInteger(R.integer.systemLogRotateSecondsDefault);
          long lastRotationTime = prefs.getLong(
                                        context.getString(R.string.lastSystemLogFileRotatioKey), 0);
          
          sFileLogger = new FileLogger("system", "log", maxSz, rotationSeconds, lastRotationTime);
      }
  }
  
  private static void writeln(String message) {
      if (sLoggingToFileEnabled) {
          initFileLogger();
          if (sFileLogger != null) {
              sFileLogger.write(message + "\n");
          }
      }
  }
  
  public static void rotateIfNecessary() {
      if (sLoggingToFileEnabled) {
          initFileLogger();
          if (sFileLogger.rotateIfNecessary()) {
              long now = System.currentTimeMillis();
              Context context = Session.getGlobalContext();
              SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
              prefs.edit().putLong(
                      context.getString(R.string.lastSystemLogFileRotatioKey), now).commit();
          }
      }
  }
  
  public static void d(String tag, String msg) {
    if (LOGGING_ENABLED) {
      Log.d(tag, msg);
      writeln("DEBUG " + tag + " " + System.currentTimeMillis() + " " + msg);
    }
  }
  
  public static void d(String tag, String msg, Throwable t) {
    if (LOGGING_ENABLED) {
      Log.d(tag, msg, t);
      writeln("DEBUG " + tag + " " + System.currentTimeMillis() + " " + msg);
    }
  }
  
  public static void e(String tag, String msg) {
    if (LOGGING_ENABLED) {
      Log.e(tag, msg);
      writeln("ERROR " + tag + " " + System.currentTimeMillis() + " " + msg);
    }
  }
  
  public static void e(String tag, String msg, Throwable t) {
    if (LOGGING_ENABLED) {
      Log.e(tag, msg, t);
      writeln("ERROR " + tag + " " + System.currentTimeMillis() + " " + msg);
    }
  }
  
  public static void i(String tag, String msg) {
    if (LOGGING_ENABLED) {
      Log.i(tag, msg);
      writeln("INFO " + tag + " " + System.currentTimeMillis() + " " + msg);
    }
  }
  
  public static void i(String tag, String msg, Throwable t) {
    if (LOGGING_ENABLED) {
      Log.i(tag, msg, t);
      writeln("INFO " + tag + " " + System.currentTimeMillis() + " " + msg);
    }
  }
  
  public static void v(String tag, String msg) {
    if (LOGGING_ENABLED) {
      Log.v(tag, msg);
      writeln("VERBOSE " + tag + " " + System.currentTimeMillis() + " " + msg);
    }
  }
  
  public static void v(String tag, String msg, Throwable t) {
    if (LOGGING_ENABLED) {
      Log.v(tag, msg, t);
      writeln("VERBOSE " + tag + " " + System.currentTimeMillis() + " " + msg);
    }
  }
  
  public static void w(String tag, String msg) {
    if (LOGGING_ENABLED) {
      Log.w(tag, msg);
      writeln("WARNING " + tag + " " + System.currentTimeMillis() + " " + msg);
    }
  }
  
  public static void w(String tag, String msg, Throwable t) {
    if (LOGGING_ENABLED) {
      Log.w(tag, msg, t);
      writeln("WARNING " + tag + " " + System.currentTimeMillis() + " " + msg);
    }
  }
}