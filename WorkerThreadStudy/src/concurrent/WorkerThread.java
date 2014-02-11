/*
 * Created on Jul 17, 2009
 *
 */
package concurrent;

import java.sql.Connection;

import externals.TIERSBatchException;


/**
 * @author TPatel
 * @since 77.0
 * SR88145 - BI-TFISS-BIP Performance
 *
 *
 * Clarification:
 * Shutdown = applicable to all workers
 * KillSw = applicable per worker
 *
 */
public class WorkerThread extends Thread {
	/** incremental counter used to ensure too many threads are not instantiated */
	private static int counter = 0;
	/** incremental counter used to name workers */
	private static int threadNameCounter = 0;
	
	
	/** the work queue that the workers will be looking to work for*/
	private final WorkQueue mQueue;
	
	
	/** the connection that the worker maintains and which will be used when performing the task */
	private final Connection conn;
	
	/** denotes whether the worker is working on a task or waiting for a task */
	private boolean working = false;
	/** denotes whether the worker needs to be killed off after finishing the task */
	private boolean killSw = false;
	
	/** workers will be named as this prefix + worker count */
	private static final String BASE_NAME = "MyWorker";

	protected static boolean debug = false;	//use to show thread life info
	private TimerUtil timer;

	/**
	 * Initialize worker to use a specific connection.
	 * @param conn
	 * @param queue
	 */
	private WorkerThread(final Connection conn, final WorkerThreadPool pool, final WorkQueue queue) {
		super();
		this.conn = conn;
		timer = new TimerUtil();
		timer.setLoggingEnabled(true);
		pool.addTimer(timer);
		mQueue = queue;
	}

	/**
	 * Starts up a new worker
	 * @param conn
	 * @param queue
	 * @return
	 */
	protected static WorkerThread startRunner(final Connection conn, final WorkerThreadPool pool, final WorkQueue queue) throws TIERSBatchException {
		if (counter >= 99) {
			throw new TIERSBatchException("Max WorkerThread limit exceeded: " + counter);
		}
		final WorkerThread runner = new WorkerThread(conn, pool, queue);
		runner.setName(BASE_NAME + formatStrLen("" + ++threadNameCounter, 2));
		runner.start();
		queue.addNewIdle();
		queue.addNewIdle();	//add 2 idle for every runner ??????
		return runner;
	}

	protected TimerUtil getTimer() {
		return timer;
	}

	private static final String ZEROS = "00000";

	/**
	 * Formats the string to the specified length using the padString and padded either left (PAD_LEFT) or right (PAD_RIGHT)
	 * @param str
	 * @param len
	 * @param padStr
	 * @param padLeft true if pad left, false otherwise (use constants PAD_LEFT, PAD_RIGHT)
	 * @return
	 */
	private static String formatStrLen(final String str, final int len) {
		return str == null || str.length() == 0 ? ZEROS.substring(0, len) :
				str.length() >= len ? str : ZEROS.substring(str.length(), len) + str;
	}

	/**
	 * Worker will wait/process tasks until shutdown or kill command received
	 */
	public void run() {
		if (debug) System.err.println("+" + this.getName());
		while (!mQueue.isShutdown() && !isKillSw()) {
			try {
				final BaseWorkTask task = mQueue.dequeue();
				if (task != null) {
					task.setExecutedBy(this.getName());	//give the task the worker's name
					task.setConnection(conn);	//give the task the worker's connection
					task.setTimer(timer);
					setWorking(true);
					task.run();		//process task
					setWorking(false);
					mQueue.addIdle(task);
				}
			} catch (Throwable e) {
				// NOTE: tasks *must* handle their own errors
				e.printStackTrace();
				setWorking(false);
			}
		}

		if (debug) System.err.println("-" + this.getName());
		
		//When this worker stops, decrement total count of active workers
		counter--;
	}

	/**
	 * @return
	 */
	protected boolean isWorking() {
		return working;
	}

	/**
	 * @param work
	 */
	protected void setWorking(final boolean work) {
		working = work;
	}

	/**
	 * @return
	 */
	protected boolean isKillSw() {
		return killSw;
	}

	/**
	 * @param kill
	 */
	protected void setKillSw(boolean kill) {
		killSw = kill;
	}
}
