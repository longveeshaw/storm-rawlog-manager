package com.nl.util.log;

import com.nl.util.log.Hierarchy;
import com.nl.util.log.LogTarget;
import com.nl.util.log.Logger;
import com.nl.util.log.Priority;
import com.nl.util.log.format.ExtendedPatternFormatter;
import com.nl.util.log.output.io.StreamTarget;
import com.nl.util.log.output.io.rotate.RevolvingFileStrategy;
import com.nl.util.log.output.io.rotate.RotateStrategyBySize;
import com.nl.util.log.output.io.rotate.RotatingFileTarget;

import java.io.File;
import java.io.IOException;

/**
 * Simple wrapper to the incorporated LogKit to log under a single logging name.
 *
 * @author Bruce Ritchie
 */
public class Log {

    private static final Logger debugLog = Hierarchy.getDefaultHierarchy().getLoggerFor("eng-DEBUG");
    private static final Logger infoLog = Hierarchy.getDefaultHierarchy().getLoggerFor("eng-INFO");
    private static final Logger warnLog = Hierarchy.getDefaultHierarchy().getLoggerFor("eng-WARN");
    private static final Logger errorLog = Hierarchy.getDefaultHierarchy().getLoggerFor("eng-ERR");
    //private static final Logger sqlLog = Hierarchy.getDefaultHierarchy().getLoggerFor("eng-SQL");


    private static String logNameDebug = null;
    private static String logNameInfo = null;
    private static String logNameWarn = null;
    private static String logNameError = null;
    //private static String logNameSql  = null;


    private static String debugPattern = null;
    private static String infoPattern = null;
    private static String warnPattern = null;
    private static String errorPattern = null;
   // private static String sqlPattern = null;
    private static String logDirectory = null;


    private static long maxDebugSize = 1024;
    private static long maxInfoSize = 1024;
    private static long maxWarnSize = 1024;
    private static long maxErrorSize = 1024;
    //private static long maxSqlSize = 1024;

    private static boolean debugEnabled;

    static {
        initLog();
    }

    private Log() {
    }

    /**
     * This method is used to initialize the Log class. For normal operations this method
     * should <b>never</b> be called, rather it's only publically available so that the class
     * can be reset by the setup process once the messengerHome directory has been specified.
     */
    public static void initLog() {
        try {
            logDirectory = "/home/stormdev/storm/logs/"; 
            if (!logDirectory.endsWith(File.separator)) {
                logDirectory = logDirectory + File.separator;
            }

            File logDir = new File(logDirectory);
            if (!logDir.exists()) {
                logDir.mkdir();
            }

            logNameDebug = logDirectory + "debug.log";
            logNameInfo = logDirectory + "info.log";
            logNameWarn = logDirectory + "warn.log";
            logNameError = logDirectory + "error.log";
           // logNameSql= logDirectory + "sql.log";

            debugEnabled = true;
        } catch (Exception e) {
            // we'll get an exception if messengerHome isn't setup yet - we ignore that since
            // it's sure to be logged elsewhere :)
        }

        if (debugPattern == null) {
            debugPattern = "%{time:yyyy-MM-dd HH:mm:ss} %{message}\\n%{throwable}";
        }
        if (infoPattern == null) {
            infoPattern = "%{time:yyyy-MM-dd HH:mm:ss} %{message}\\n%{throwable}";
        }
        if (warnPattern == null) {
            warnPattern = "%{time:yyyy-MM-dd HH:mm:ss} %{message}\\n%{throwable}";
        }
        if (errorPattern == null) {
            errorPattern = "%{time:yyyy-MM-dd HH:mm:ss} [%{method}] %{message}\\n%{throwable}";
        }
       /* if (sqlPattern == null) {
        	sqlPattern = "%{time:yyyy-MM-dd HH:mm:ss} [%{method}] %{message}\\n%{throwable}";
        }*/

        createLogger(debugPattern, logNameDebug, maxDebugSize, debugLog, Priority.DEBUG);
        createLogger(infoPattern, logNameInfo, maxInfoSize, infoLog, Priority.INFO);
        createLogger(warnPattern, logNameWarn, maxWarnSize, warnLog, Priority.WARN);
        createLogger(errorPattern, logNameError, maxErrorSize, errorLog, Priority.ERROR);
        //createLogger(sqlPattern, logNameSql, maxSqlSize, sqlLog, Priority.SQL);

    }

    private static void createLogger(String pattern, String logName, long maxLogSize,
                                     Logger logger, Priority priority) {
        // debug log file
        ExtendedPatternFormatter formatter = new ExtendedPatternFormatter(pattern);
        StreamTarget target;
        Exception ioe = null;

        try {
            // messengerHome was not setup correctly
            if (logName == null) {
                throw new IOException("LogName was null - MessengerHome not set?");
            } else {
                RevolvingFileStrategy fileStrategy = new RevolvingFileStrategy(logName, 3);
                RotateStrategyBySize rotateStrategy = new RotateStrategyBySize(maxLogSize * 1024);
                target = new RotatingFileTarget(formatter, rotateStrategy, fileStrategy);
            }
        } catch (IOException e) {
            ioe = e;
            // can't log to file, log to stderr
            target = new StreamTarget(System.err, formatter);
        }

        logger.setLogTargets(new LogTarget[]{target});
        logger.setPriority(priority);

        if (ioe != null) {
            logger.debug("Error occurred opening log file: " + ioe.getMessage());
        }
    }

    public static boolean isErrorEnabled() {
        return errorLog.isErrorEnabled();
    }

    public static boolean isFatalEnabled() {
        return errorLog.isFatalErrorEnabled();
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static boolean isInfoEnabled() {
        return infoLog.isInfoEnabled();
    }

    public static boolean isWarnEnabled() {
        return warnLog.isWarnEnabled();
    }

    public static void debug(String s) {
        if (isDebugEnabled()) {
            debugLog.debug(s);
        }
    }

//    public static void ctrl(String s) {
//        ctrlLog.info(s);
//    }

    public static void debug(Throwable throwable) {
        if (isDebugEnabled()) {
            debugLog.debug("", throwable);
        }
    }

    public static void debug(String s, Throwable throwable) {
        if (isDebugEnabled()) {
            debugLog.debug(s, throwable);
        }
    }

//    public static void markDebugLogFile(User user) {
//        RotatingFileTarget target = (RotatingFileTarget) debugLog.getLogTargets()[0];
//        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a yyyy.MM.dd");
//        String date = sdf.format(new java.util.Date());
//        String logline = " --- Marker inserted by " + user.getUsername() + " at " + date + " --- \n";
//        target.write(logline);
//    }

    public static void rotateDebugLogFile() {
        RotatingFileTarget target = (RotatingFileTarget) debugLog.getLogTargets()[0];
        try {
            target.rotate();
        } catch (IOException e) {
            System.err.println("Warning: There was an error rotating the Jive debug log file. " +
                    "Logging may not work correctly until a restart happens.");
        }
    }

    public static void info(String s) {
        if (isInfoEnabled()) {
            infoLog.info(s);
        }
    }

    public static void info(Throwable throwable) {
        if (isInfoEnabled()) {
            infoLog.info("", throwable);
        }
    }

    public static void info(String s, Throwable throwable) {
        if (isInfoEnabled()) {
            infoLog.info(s, throwable);
        }
    }

//    public static void markInfoLogFile(User user) {
//        RotatingFileTarget target = (RotatingFileTarget) infoLog.getLogTargets()[0];
//        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a yyyy.MM.dd");
//        String date = sdf.format(new java.util.Date());
//        String logline = " --- Marker inserted by " + user.getUsername() + " at " + date + " --- \n";
//        target.write(logline);
//    }

    public static void rotateInfoLogFile() {
        RotatingFileTarget target = (RotatingFileTarget) infoLog.getLogTargets()[0];
        try {
            target.rotate();
        } catch (IOException e) {
            System.err.println("Warning: There was an error rotating the Jive info log file. " +
                    "Logging may not work correctly until a restart happens.");
        }
    }

    public static void warn(String s) {
        if (isWarnEnabled()) {
            warnLog.warn(s);
        }
    }

    public static void warn(Throwable throwable) {
        if (isWarnEnabled()) {
            warnLog.warn("", throwable);
        }
    }

    public static void warn(String s, Throwable throwable) {
        if (isWarnEnabled()) {
            warnLog.warn(s, throwable);
        }
    }

//    public static void markWarnLogFile(User user) {
//        RotatingFileTarget target = (RotatingFileTarget) warnLog.getLogTargets()[0];
//        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a yyyy.MM.dd");
//        String date = sdf.format(new java.util.Date());
//        String logline = " --- Marker inserted by " + user.getUsername() + " at " + date + " --- \n";
//        target.write(logline);
//    }

    public static void rotateWarnLogFile() {
        RotatingFileTarget target = (RotatingFileTarget) warnLog.getLogTargets()[0];
        try {
            target.rotate();
        } catch (IOException e) {
            System.err.println("Warning: There was an error rotating the Jive warn log file. " +
                    "Logging may not work correctly until a restart happens.");
        }
    }

    public static void error(String s) {
        if (isErrorEnabled()) {
            errorLog.error(s);
            if (isDebugEnabled()) {
                printToStdErr(s, null);
            }
        }
    }

    public static void error(Throwable throwable) {
        if (isErrorEnabled()) {
            errorLog.error("", throwable);
            if (isDebugEnabled()) {
                printToStdErr(null, throwable);
            }
        }
    }

    public static void error(String s, Throwable throwable) {
        if (isErrorEnabled()) {
            errorLog.error(s, throwable);
            if (isDebugEnabled()) {
                printToStdErr(s, throwable);
            }
        }
    }

//    public static void markErrorLogFile(User user) {
//        RotatingFileTarget target = (RotatingFileTarget) errorLog.getLogTargets()[0];
//        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a yyyy.MM.dd");
//        String date = sdf.format(new java.util.Date());
//        String logline = " --- Marker inserted by " + user.getUsername() + " at " + date + " --- \n";
//        target.write(logline);
//    }

    public static void rotateErrorLogFile() {
        RotatingFileTarget target = (RotatingFileTarget) errorLog.getLogTargets()[0];
        try {
            target.rotate();
        } catch (IOException e) {
            System.err.println("Warning: There was an error rotating the Jive error log file. " +
                    "Logging may not work correctly until a restart happens.");
        }
    }

    public static void fatal(String s) {
        if (isFatalEnabled()) {
            errorLog.fatalError(s);
            if (isDebugEnabled()) {
                printToStdErr(s, null);
            }
        }
    }

    public static void fatal(Throwable throwable) {
        if (isFatalEnabled()) {
            errorLog.fatalError("", throwable);
            if (isDebugEnabled()) {
                printToStdErr(null, throwable);
            }
        }
    }

    public static void fatal(String s, Throwable throwable) {
        if (isFatalEnabled()) {
            errorLog.fatalError(s, throwable);
            if (isDebugEnabled()) {
                printToStdErr(s, throwable);
            }
        }
    }

    /**
     * Returns the directory that log files exist in. The directory name will
     * have a File.separator as the last character in the string.
     *
     * @return the directory that log files exist in.
     */
    public static String getLogDirectory() {
        return logDirectory;
    }

    private static void printToStdErr(String s, Throwable throwable) {
//        if (s != null) {
//            System.err.println(s);
//        }
//        if (throwable != null) {
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            throwable.printStackTrace(pw);
//            System.err.print(sw.toString());
//            System.err.print("\n");
//        }
    }
}
