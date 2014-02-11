package concurrent;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * This class is an improvement upon TimerUtil.  Use this timing class instead.
 * 
 * 
 * Usage:
 * 
 * For timing methods, add the following:
 * 	timer.startCapture();	//just after start of the method
 * 	timer.endCapture();	//just before end of method
 * 
 * For custom timings, add the following:
 * 	timer.startCapture("finderX");
 * 	timer.endCapture("finderX");
 *
 * @author TPatel
 */
public class MethodTimer {
	private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm:ss");

	private HashMap<String, TimeObject> openMethods = new HashMap<String, TimeObject>();
	private TreeMap<String, TimeObject> timingsMap = new TreeMap<String, TimeObject>(); 
	
	private MethodTimer[] otherTimers;

	private static int IGNORE_TRACE_LEVEL = 0;
	
	private static final long START_TIME = System.currentTimeMillis();
	private long elapsedTime;
	private long overheadTime;
	
	private boolean debug;
	
	private static final String SPACES = "                                                                                                                                                                                                      ";
	private static final String BLANK = "";
	private static final String METH_SEPARATOR = "|";
	private static final String METH_END = ".";
	private static final String LEVEL_SEPARATOR = "_";
	private static final String LINE_SEPARATOR = "~";
	private static final String CAPTURE_SEPARATOR = "/";
	private static final String NEW_LINE = System.getProperty("line.separator");

	/* For printing purposes */
	private PrintStream out = null;
	private final DecimalFormat df = new DecimalFormat("#.#");
	private boolean loggingEnabled = false;	//by default turned off
	public static final int LOG_BEGIN = 1;	//logs beginning (no \n)
	public static final int LOG_MIDDLE = 2; //logs middle (no \n)
	public static final int LOG_END = 3; //logs end (w \n)
	public static final int LOG_ALL = 0; //logs all (w \n)

	private int logLevel;
	public static final int LEVEL_DEBUG = 0;
	public static final int LEVEL_INFO = 1;
	public static final int LEVEL_WARN = 2;
	public static final int LEVEL_ERROR = 3;
	private static final String[] LEVEL_ARR = {
		"D", "I", "W", "E"
	};

	// ----------------------------------------------------------------------
	// Start exposed methods
	// ----------------------------------------------------------------------

	/**
	 * Prints the timings; Formats with spaces
	 */
	public String toString() {
		elapsedTime = System.currentTimeMillis() - START_TIME;
		
		StringBuilder str = new StringBuilder(NEW_LINE);
		TimeObject to;
		final TreeMap<String, TimeObject> consoldatedTimerMap = otherTimers == null ? timingsMap : getConsolidatedTimings();
		
		str.append("Total time: " + (elapsedTime/1000) + "s (Overhead: " + overheadTime + "ms)");
		
		final TreeMap<String, TimeObject> sortedMap = new TreeMap<String, TimeObject>();
		for (String key : consoldatedTimerMap.keySet()) {
			to = consoldatedTimerMap.get(key);
			sortedMap.put(to.lineKey, to);
		}
		
		for (String key : sortedMap.keySet()) {
			to = sortedMap.get(key);
			str.append(NEW_LINE).append(getSpaces(to.level, to.key.contains(CAPTURE_SEPARATOR)) + to);
		}
		
		return str.toString();
	}

	public void startCapture() {
		start(BLANK);
	}
	
	public void startCapture(final String capturePoint) {
		start(capturePoint);
	}

	public void endCapture() {
		end(BLANK);
	}

	public void endCapture(final String capturePoint) {
		end(capturePoint);
	}

	/**
	 * Use to add timers from workers/threads;
	 * Method may be reused to reset the timers within this object
	 * @param timers
	 */
	public void consolidate(final MethodTimer[] timers) {
		this.otherTimers = timers;
	}

	
	// ----------------------------------------------------------------------
	// Following for debugging/printing
	// ----------------------------------------------------------------------

	/**
	 * Default constructor
	 */
	public MethodTimer() { }
	/**
	 * Constructor which sets output stream;
	 * Also enables logging by default
	 * @param outStream
	 */
	public MethodTimer(final PrintStream outStream) {
		super();
		this.out = outStream;
		setLoggingEnabled(true);
	}

	/**
	 * Use this method to enable/disable logging
	 * @param val
	 */
	public void setLoggingEnabled(final boolean val) {
		loggingEnabled = val;
	}
	
	/**
	 * Use this to turn internal logging (for debugging purposes only)
	 * User should really not need to set this
	 * @param val
	 */
	public void setDebug(final boolean val) {
		this.debug = val;
	}

	/**
	 * Sets the logging level; default is LEVEL_DEBUG
	 * @param level
	 */
	public void setLogLevel(final int level) {
		this.logLevel = level;
	}

	public void setLogLevel(final String level) {
		this.logLevel = level == null || BLANK.equals(level.trim()) ? 0 :
			"DEBUG".equalsIgnoreCase(level) ? LEVEL_DEBUG :
				"INFO".equalsIgnoreCase(level) ? LEVEL_INFO :
					"WARN".equalsIgnoreCase(level) ? LEVEL_WARN :
						"ERROR".equalsIgnoreCase(level) ? LEVEL_ERROR : 
							LEVEL_DEBUG;
	}

	/**
	 * Prints the timings at the DEBUG level log
	 * @param str
	 */
	public void printTimings() {
		log(this.toString());
	}
	/**
	 * Prints the timings at the specified log level
	 * @param logLevel
	 */
	public void printTimings(final int logLevel) {
		log(logLevel, this.toString());
	}

	/**
	 * Logs the string at the DEBUG level log using type LOG_ALL
	 * @param str
	 */
	public void log(final String str) {
		log(LEVEL_DEBUG, str, LOG_ALL);
	}

	/**
	 * Prints the string at the DEBUG level log with the specified type
	 * @param str
	 * @param logType denotes how to print the line (LOG_BEGIN, _MIDDLE, _END, _ALL)
	 */
	public void log(final String str, final int logType) {
		log(LEVEL_DEBUG, str, logType);
	}

	/**
	 * Prints the string at the specified level using type LOG_ALL
	 * @param logLevel
	 * @param str
	 */
	public void log(final int logLevel, final String str) {
		log(logLevel, str, LOG_ALL);
	}

	/**
	 * Prints the string if:
	 *  - logging is enabled
	 *  - print stream is specified
	 *  - logging level is > timer log level
	 * @param logLevel
	 * @param str
	 * @param logType
	 */
	public void log(final int logLevel, final String str, final int logType) {
		if (loggingEnabled && out != null && logLevel >= this.logLevel) {
			switch (logType) {
				case LOG_BEGIN:
				case LOG_ALL:
					final long currTime = System.currentTimeMillis();
					out.print(
							sdf.format(new Date(currTime)) + 
							" +" + 
							df.format((currTime - START_TIME)/(1000.0*60)) + 
							" " +
							LEVEL_ARR[logLevel] + ": "
							);
				case LOG_MIDDLE:
				case LOG_END:
					out.print(str);
					break;
			}
			if (logType == LOG_ALL || logType == LOG_END) {
				out.println();
			}
		}
	}


	
	// ----------------------------------------------------------------------
	// End exposed methods
	// ----------------------------------------------------------------------
	
	/**
	 * Returns spacing for printing map
	 * @param level
	 * @return
	 */
	private String getSpaces(final int level, final boolean isCapPoint) {
		return SPACES.substring(0, (level-1)*3 + (isCapPoint ? 2 : 0));
	}

	/**
	 * Constructs key to use for storing timings
	 * @param traceArr
	 * @param capPoint
	 * @return
	 */
	private String getKey(StackTraceElement[] traceArr, String capPoint, final boolean isLineKey) {
		final StringBuilder sb = new StringBuilder();
		/*
		 * ignore: 
		 * Last element: main()
		 * First 3 elements: .getStackTrace, start(), and startCapture()
		 */

		/*
		 * Trying to distinguish between overloaded methods is problematic since
		 * endCapture and startCapture will not be at the same line.
		 * 
		 * Workaround is to also include calling methods and line numbers.  Don't include
		 * line number for timing method since the capture start and end line numbers are
		 * not the same, and will not be able to match up the keys
		 */

		int lineKeyInt = isLineKey ? 0 : 1;
		for (int i = traceArr.length; i > IGNORE_TRACE_LEVEL; i--) {
			sb.append(traceArr.length - i + 1)
					.append(i == IGNORE_TRACE_LEVEL + lineKeyInt ? BLANK : LINE_SEPARATOR + getLineNum(traceArr[i-1]))
					.append(LEVEL_SEPARATOR)
					.append(getMethodName(traceArr[i-1]))
					.append(METH_SEPARATOR);
		}

		sb.append(BLANK.equals(capPoint) ? BLANK : CAPTURE_SEPARATOR + capPoint).append(METH_END);	//Add double || if capPoint is present

		return sb.toString();
	}

	/**
	 * Returns method name or if constuctor, returns class name
	 * @param element
	 * @return
	 */
	private String getMethodName(StackTraceElement element) {
		return "<init>".equals(element.getMethodName()) ? 
				element.getClassName().substring(element.getClassName().lastIndexOf(".")+1) : 
				element.getMethodName();
	}
	/**
	 * Gets stack trace line number call
	 * @param element
	 * @return
	 */
	private int getLineNum(StackTraceElement element) {
		return element.getLineNumber();
	}

	/**
	 * Extracts method in which timing was requested
	 * @param traceArr
	 * @return
	 */
	private String getMethName(StackTraceElement[] traceArr) {
		/*
		 * ignore:  (same logic as getKey())
		 */
		return traceArr == null || traceArr.length <= IGNORE_TRACE_LEVEL ? "TooShallow" : getMethodName(traceArr[IGNORE_TRACE_LEVEL]);
	}

	/**
	 * Gets the level of the timings call
	 * @param traceArr
	 * @return
	 */
	private int getLevel(StackTraceElement[] traceArr) {
		return traceArr == null ? 0 : traceArr.length - IGNORE_TRACE_LEVEL;
	}
	
	private void determineIgnoreLevel(StackTraceElement[] traceArr) {
//		IGNORE_TRACE_LEVEL = 3;

		System.err.println("Determing ignore level...");
		String current;
		String previous;
		for (int i = 0; i < traceArr.length; i++) {
			System.err.print(traceArr[i].getMethodName() + " - ");
			if (i > 0 && "startCapture".equals(traceArr[i].getMethodName()) && "start".equals(traceArr[i-1].getMethodName())) {
				IGNORE_TRACE_LEVEL = i+1;
				break;
			}
		}
		System.err.println("Ignore level = " + IGNORE_TRACE_LEVEL);
	}

	/**
	 * Internal start capture method; initiates timing save
	 * @param capturePoint
	 */
	private void start(final String capturePoint) {
		
		final long start = System.currentTimeMillis();
		
		//Get the snapshot of the stack trace
		final StackTraceElement[] traceArr = Thread.currentThread().getStackTrace();

		if (IGNORE_TRACE_LEVEL == 0) {
			determineIgnoreLevel(traceArr);
		}

		//Get the key used to keep track of this timing
		final String key = getKey(traceArr, capturePoint, false);

		//There should not be any open timings with the same key
		if (openMethods.get(key) == null) {
			TimeObject timeObj = timingsMap.get(key);
			if (timeObj == null) {
				//create a new timing object
				timeObj = new TimeObject(key, getMethName(traceArr) + (capturePoint.equals(BLANK) ? BLANK : "_" + capturePoint), getLevel(traceArr), getKey(traceArr, capturePoint, true));
				timingsMap.put(key, timeObj);
			}
			timeObj.start();
			openMethods.put(key, timeObj);
			if (debug) log("Starting " + key);
		} else {
			log(LEVEL_WARN, key + " was not ended; timing not initiated; check line " + traceArr[IGNORE_TRACE_LEVEL].getClassName() + " " + traceArr[IGNORE_TRACE_LEVEL].getLineNumber());
		}
		overheadTime += System.currentTimeMillis() - start;
	}

	/**
	 * Internal end capture method; Saves method timing
	 * @param capturePoint
	 */
	private void end(final String capturePoint) {
		final long start = System.currentTimeMillis();

		//Get the snapshot of the stack trace
		final StackTraceElement[] traceArr = Thread.currentThread().getStackTrace();
		
		final String key = getKey(traceArr, capturePoint, false);
		final TimeObject timeObj = openMethods.remove(key);
		
		//There should be an open timings with the same key
		if (timeObj == null) {
			log(LEVEL_WARN, key + " was not started; timing not captured; check line " + traceArr[IGNORE_TRACE_LEVEL].getClassName() + " " + traceArr[IGNORE_TRACE_LEVEL].getLineNumber());
		} else {
			timeObj.end();
		}
		overheadTime += System.currentTimeMillis() - start;
	}

	/**
	 * Internal object used to represent a timing block
	 * @author TPatel
	 *
	 */
	private class TimeObject {
		/** Value used to refer to this object */
		private String key;
		/** Number of times timing was taken */
		private int qty;
		/** Method name of timing event */
		private String methodName;
		/** Total time of timing block (in ms) */
		private long totalTime;
		/** Level of method call (the deeper the call, the higher the value) */
		private int level;
		/** Snapshot of the time before the timing block was called; used to calculate block timing */
		private long startTime;
		
		private String lineKey;
		
		private TimeObject(final String key, final String methodName, final int level, final String lineKey) {
			this.key = key;
			this.methodName = methodName;
			this.level = level;
			this.lineKey = lineKey;
		}
		
		private void start() {
			startTime = System.currentTimeMillis();
		}

		private void end() {
			totalTime += (System.currentTimeMillis() - startTime);
			qty++;
		}
		
		public String toString() {
			if (elapsedTime == 0) {
				return key + " elapsed time = 0";
			}
			return methodName + ": " + qty + "/" + (totalTime/1000) + " [" + (totalTime*100/elapsedTime) + "%]";
		}
		
		private void add(final TimeObject to) {
			this.qty += to.qty;
			this.totalTime += to.totalTime;
		}

		private TimeObject copy() {
			TimeObject tObj = new TimeObject(key, methodName, level, lineKey);
			tObj.qty = this.qty;
			tObj.totalTime = this.totalTime;
			return tObj;
		}
	}

	/**
	 * 
	 * @return
	 */
	private TreeMap<String, TimeObject> getConsolidatedTimings() {
		final long start = System.currentTimeMillis();

		TreeMap<String, TimeObject> totalTree = new TreeMap<String, TimeObject>();
		
		TimeObject value;
		for (String key : timingsMap.keySet()) {
			value = timingsMap.get(key);
			totalTree.put(key, value.copy());
		}
		
		if (otherTimers != null) {
			TimeObject totalTimeObj, timeObj;
			for (MethodTimer timer : otherTimers) {
				for (String key : timer.timingsMap.keySet()) {
					timeObj = timer.timingsMap.get(key);
					
					totalTimeObj = totalTree.get(key);
					if (totalTimeObj == null) {
						totalTree.put(key, timeObj.copy());	//copy of obj is not needed, but copying for simplicity
					} else {
						totalTimeObj.add(timeObj);
					}
				}
			}
		}
		
		overheadTime += System.currentTimeMillis() - start;
		return totalTree;
	}

}
