/*
 * Created on Jul 21, 2009
 * SR88145 - BI-TFISS-BIP Performance Changes
 */
package concurrent;

import java.sql.Connection;

/**
 * @author TPatel
 * @since 77.0
 * SR88145 - BI-TFISS-BIP Performance
 *
 * The base object to use when multi-threading is integrated into job.
 * Define custom task which extends this class.
 */
public abstract class BaseWorkTask {
	/** dash constant */
	protected static final String DASH = "-";
	/** space constant */
	protected static final String SPACE = " ";

	/*
	 * these are set by calling object
	 */
	protected String name;	//task name
	protected String executedBy;	//denotes which worker is executing the task
	protected Connection conn;	//worker-provided connection
	protected TimerUtil timer;

	/**
	 * The entry/exit point for the worker.
	 */
	public abstract void run();

	/**
	 * Returns the name of the Task object (if set by the parent using the setName())
	 * @return
	 */
	public String getName() {
		return name;
	}

	public void setName(final String string) {
		name = string;
	}

	/**
	 * In case the Task object is printed, prints the name of the Task
	 */
	public String toString() {
		return getName();
	}

	/**
	 * Sets the name of the thread executing the task
	 * @param name
	 */
	public void setExecutedBy(final String name) {
		executedBy = name;
	}

	/**
	 * Gets the name of the thread executing the task
	 * @return
	 */
	public String getExecutedBy() {
		return executedBy;
	}

	public void setTimer(final TimerUtil timer) {
		this.timer = timer;
	}

	/**
	 * Gets the connection held by the thread
	 * @return
	 */
	public Connection getConnection() {
		return conn;
	}

	/**
	 * Set internally by thread.  Do not call this method.
	 * @param connection
	 */
	public void setConnection(final Connection connection) {
		conn = connection;
	}

	/**
	 * Trims a String to the specified length
	 * @param str
	 * @param length
	 * @return
	 */
	protected String trimString(final String str, final int length) {
		if (str == null) {
			return null;
		}

		if (str.length() <= length) {
			return str;
		} else {
			return str.substring(0, length);
		}
	}

	/**
	 * Returns a copy of the Task.  Do not copy any task related specific data (case_num, etc).
	 * Usually calling the constructor should do it.
	 * This method will be called internally when the WorkerThreadPool is created.
	 * @return
	 */
	public abstract BaseWorkTask copy();

}
