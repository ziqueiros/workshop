/*
 * Created on Mar 9, 2009
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package concurrent;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * @author TPatel
 *
 * In order to use this class properly:
 * In your class, instantiate this class, and then set the outStream (System.err, etc) and enable logging.
 * 
 * When you want to capture the time for a call, use getTime() to hold the start time.
 * After the call that you want to time, call the incrementTimings() method with a key and the start time.
 * 
 * printTimes() can be used to print the stored timings.
 */
public class TimerUtil {
	public static final int LOG_BEGIN = 1;	//logs beginning (no \n)
	public static final int LOG_MIDDLE = 2; //logs middle (no \n)
	public static final int LOG_END = 3; //logs end (w \n)
	public static final int LOG_ALL = 0; //logs all (w \n)

	private final TreeMap timeMap = new TreeMap();	//holds the timings
	private final HashMap qtyMap = new HashMap();		//holds the qty
	private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");

	public long globalStartTime;	//holds the time that the obj was created
	private long endDt;
	private boolean loggingEnabled = false;	//by default turned off
	private PrintStream out = null;

	/**
	 * Default constructor;
	 * Note that logging is disabled until setLoggingEnabled is called
	 */
	public TimerUtil() {
		globalStartTime = System.currentTimeMillis();
	}

	public TimerUtil(final PrintStream outStream) {
		this();
		this.out = outStream;
		setLoggingEnabled(true);
	}

	public void resetGlobalTime() {
		globalStartTime = System.currentTimeMillis();
	}

	public void setLoggingEnabled(final boolean val) {
		loggingEnabled = val;
	}
	
	public void setPrintStream(final PrintStream outStream) {
		this.out = outStream;
		if (outStream == null) {
			setLoggingEnabled(false);
		}
	}

	/**
	 * Prints the timings held in the qty and time maps
	 *
	 */
	public void printTimes() {
		  if (loggingEnabled && out != null && qtyMap != null && timeMap != null) {
				debugLog("Timings:");
				for (final Iterator iter = timeMap.keySet().iterator(); iter.hasNext(); ) {
					  final String key = (String) iter.next();
					  out.println("  " + key + ": " + ((Integer)qtyMap.get(key)).intValue() + " / " + ((Long)timeMap.get(key)).longValue()/1000);
				}
		  }
	}

	/**
	 * Adds the call and time to the map using a key
	 * @param str
	 * @param startDt
	 */
	public void incrementTimings(final String key, final long startDt) {
		if (loggingEnabled) {
		  endDt = getTime();
 
		  Integer intQty = (Integer) qtyMap.get(key);
		  if (intQty == null) {
				intQty = new Integer(0);
		  }
		  qtyMap.put(key, new Integer(intQty.intValue() + 1));
 
		  Long accmTime = (Long) timeMap.get(key);	//gets the cumulative time
		  if (accmTime == null) {
				accmTime = new Long(0);
		  }
		  timeMap.put(key, new Long(accmTime.longValue() + endDt - startDt));
		}
	}

	public void incrementTimings(TimerUtil timer) {
		if (loggingEnabled) {
			Iterator iter = timer.qtyMap.keySet().iterator();
			String key;
			Integer intQty;
			Long accmTime;
			while (iter.hasNext()) {
				key = (String)iter.next();
				int qty = ((Integer)timer.qtyMap.get(key)).intValue();
				long time = ((Long)timer.timeMap.get(key)).longValue();

				intQty = (Integer) qtyMap.get(key);
				if (intQty == null) {
					  intQty = new Integer(qty);
				} else {
					intQty = new Integer(intQty.intValue() + qty);
				}
				qtyMap.put(key, intQty);
 
				accmTime = (Long) timeMap.get(key);	//gets the cumulative time
				if (accmTime == null) {
					  accmTime = new Long(time);
				} else {
					accmTime = new Long(accmTime.longValue() + time);
				}
				timeMap.put(key, accmTime);
			}
		}
	}

	/**
	 * Adds the array of timers to this timer object
	 * @param timers
	 */
	public void incrementTimings(TimerUtil[] timers) {
		if (timers != null) {
			for (int i = 0; i < timers.length; i++) {
				incrementTimings(timers[i]);
			}
		}
	}
	/**
	 * Returns the current system time (long)
	 * @return
	 */
	public static long getTime() {
		  return System.currentTimeMillis();
	}
 
	/**
	 * Prints the str and displays time relative to when obj was instantiated
	 * @param str
	 */
	public void debugLog(final String str) {
//		if (loggingEnabled && out != null) {
//		  final long currTime = System.currentTimeMillis();
//		  out.println(
//				  "{" + sdf.format(new Date(currTime)) + 
//				  ": (" + 
//				  ((currTime - globalStartTime)/(1000*60)) + 
//				  " elapsed)} " + str);
//		}
		debugLog(str, LOG_ALL);
	}

	/**
	/**
	 * Prints the str and displays time relative to when obj was instantiated.
	 * Pinting of the time elapsed and end of line chars is optional.
	 * @param str
	 * @param type LOG_ALL, LOG_BEGIN, LOG_END, LOG_MIDDLE
	 */
	public void debugLog(final String str, final int type) {
		if (loggingEnabled && out != null) {
			switch (type) {
				case LOG_BEGIN:
				case LOG_ALL:
					final long currTime = System.currentTimeMillis();
					out.print(
							"{" + sdf.format(new Date(currTime)) + 
							": (" + 
							((currTime - globalStartTime)/(1000*60)) + 
							" elapsed)} ");
				case LOG_MIDDLE:
				case LOG_END:
					out.print(str);
					break;
			}
			if (type == LOG_ALL || type == LOG_END) {
				out.println();
			}
		}
	}
}
