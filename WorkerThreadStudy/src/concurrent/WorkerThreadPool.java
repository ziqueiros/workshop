/*
 * Created on Jul 17, 2009
 *
 */
package concurrent;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import externals.TIERSBatchException;
import externals.TIERSBatchController;



/**
 * @author TPatel
 * @since 77.0
 * SR88145 - BI-TFISS-BIP Performance
 *
 *	This pool class uses 2 additional classes - WorkerThread and WorkQueue
 *
 *
 * To use:
 *
 * Initialize pool:
 *   WorkerThreadPool pool = new WorkerThreadPool(batchController, EXEC_THREAD_POOLSIZE);
 *
 * Add to queue (task must implement runnable or be a thread):
 * 	 pool.execute(task);
 * 	 <add as many tasks as needed>
 *
 * Graceful shutdown:
 *   pool.gracefulShutdown();
 * 
 * 5/3/2010:  Added logic to retrieve connections in order to commit as needed;
 * 
 * 2/1/2011:  Cleaned up the worker framework by parameterizing and removing System.err
 */
public final class WorkerThreadPool {
	/** Denotes whether to output debugging info */
	private boolean debug = false;
	public static final boolean COMMIT = true;
	public static final boolean ROLLBACK = false;
	public static final boolean CLOSE = true;
	public static final boolean DO_NOT_CLOSE = false;

	/** The queue that will hold the tasks */
	private final WorkQueue mQueue;
	/** The collection of workers */
	private final Collection<WorkerThread> mPool;
	/** The collection of workers */
	private final HashMap<String, Connection> connMap;
	/** The Reader thread that will poll the fw_batch_parameter_control table for any param updates */
	private Thread paramReader;
	/** The controller used to create new batch connections */
	private TIERSBatchController batchController;

	private ArrayList<TimerUtil> timers;

	/**
	 * Initializes the worker pool with the specified number of workers, each with it's own connection
	 * @param batch
	 * @param numThreads
	 * @throws TIERSBatchException
	 */
	public WorkerThreadPool(TIERSBatchController batch, int numThreads, int queueSize, BaseWorkTask task) throws TIERSBatchException {
		//at least one worker is needed
		if (numThreads < 1) {
			throw new IllegalArgumentException("Must use at least one thread");
		}

		timers = new ArrayList<TimerUtil>();
		
		//Añade elementos al queue de idles
		mQueue = new WorkQueue(numThreads, queueSize, task);
		
		mPool = new ArrayList<WorkerThread>(numThreads);
		batchController = batch;

		//create worker threads
		connMap = new HashMap<String, Connection>();
		for (int i = 0; i < numThreads; i++) {
			addWorker();
		}

	}

	/**
	 * Adds a worker to the pool;
	 * Stores the connection as well.
	 * @throws TIERSBatchException
	 */
	private void addWorker() throws TIERSBatchException {
		//get a new connection for the worker thread
		Connection workerConn = getNewConnection();
		//initialize a new worker
		WorkerThread worker = WorkerThread.startRunner(workerConn, this, mQueue);

		//add worker
		mPool.add(worker);
		//maintain a map of the worker connections
		connMap.put(worker.getName(), workerConn);
	}

	/**
	 * Creates a new connection for each worker to use to simplify LUW.
	 * @return
	 * @throws TIERSBatchException
	 */
	private Connection getNewConnection() throws TIERSBatchException {
		Connection conn = null;
		if (batchController != null) {
			conn = batchController.getConnection();
		} else {
			throw new TIERSBatchException("BatchController is null; Unable to get connections");
		}
		return conn;
	}

	/**
	 * Initializes the reader which will check the framework table for the specified search parameter for the job jobToCheck for the
	 * @param batchCon
	 * @param jobToCheck
	 * @param paramStr
	 * @param checkFreqMins
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public void initParamReader(String jobToCheck, String paramStr, int checkFreqMins) throws TIERSBatchException {
		Method notifyMethod = null;
		try {
			notifyMethod = this.getClass().getDeclaredMethod("readerNotify", new Class[] {String.class});
		} catch (Exception e) {
			e.printStackTrace();
			throw new TIERSBatchException("Unable to obtain CHECK and/or NOTIFY methods for Reader to work properly: " + e.getMessage());
		}
		ParameterReader prObj = new ParameterReader(batchController, jobToCheck, paramStr, this, notifyMethod, checkFreqMins);
		paramReader = new Thread(prObj);
		paramReader.start();
	}

	/**
	 * Adds a timer object (called by worker before exiting)
	 * @param timer
	 */
	public synchronized void addTimer(TimerUtil timer) {
		timers.add(timer);
	}

	/**
	 * Calling object can use this to retrieve all timers used in the pool.
	 * For performance profiling purposes, of course.
	 * @return
	 */
	public TimerUtil[] getTimers() {
		TimerUtil[] timerArr = new TimerUtil[timers.size()];
		return timers.toArray(timerArr);
	}

	/**
	 * Gets the current timings in action; Does not take into account workers that have been killed.
	 * @return
	 */
	public TimerUtil[] getActiveTimers() {
		TimerUtil[] timerArr = new TimerUtil[mPool.size()];
		for (int i = 0; i < mPool.size(); i++) {
			timerArr[i] = ((WorkerThread) ((ArrayList<WorkerThread>)mPool).get(i)).getTimer();
		}
		return timerArr;
	}

	/**
	 * Enqueues a task
	 * @param task
	 */
	public void execute(final BaseWorkTask task) {
		mQueue.enqueue(task);
	}

	/**
	 * Immediately shuts down executor threads
	 */
	public void shutdown() {
		mQueue.shutdown();
	}

	/**
	 * Shuts down executor threads after queue is exhausted
	 *
	 * @param waitForCompletion denotes whether to wait until all threads are finished
	 */
	public void gracefulShutdown(final boolean waitForCompletion) {
		debugLog("Graceful shutdown initiated...");

		//wait until queue is empty
		emptyQueue();

		//notify workers to shutdown
		shutdown();

		if (waitForCompletion) {
			//wait for all workers to finish working on last task
			for (int i = 0; i < mPool.size(); i++) {
				final WorkerThread worker = ((ArrayList<WorkerThread>)mPool).get(i);
				try {
					worker.join();
				} catch (InterruptedException e) {
					// we are waiting for the thread to stop, this is one of the very few
					// times to allo swallow IE
					e.printStackTrace();
				}
			}
		}

		//notify Param Reader to stop checking
		ParameterReader.setCheckNeeded(false);

		debugLog("Graceful shutdown completed.");
	}

	/**
	 * Checks if all workers are resting (not working on any tasks)
	 * @return
	 */
	public boolean allResting() {
		boolean result = true;
		for (int i = 0; i < mPool.size(); i++) {
			final WorkerThread worker = (WorkerThread)((ArrayList<WorkerThread>)mPool).get(i);
			if (worker.isWorking()) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Blocks until the queue is empty or until all workers are resting (only when queue is empty)
	 */
	private void emptyQueue() {
		debugLog("Emptying queue initiated...");
		while (mQueue.getSize() > 0 || !allResting()) {
			try {
				debugLog("[QueueSize = " + mQueue.getSize() + "]");
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// we are waiting for the thread to stop, this is one of the very few
				// times to allo swallow IE, it makes sense make it private
				e.printStackTrace();
			}
		}
		debugLog("Queue Empty...");
	}

//	/**
//	 * Checks if at least one worker is alive
//	 * @return true if at least 1 worker alive; false otherwise
//	 */
//	public boolean isAlive() {
//		boolean alive = false;
//		for (int i = 0; i < mPool.size(); i++) {
//			final WorkerThread worker = (WorkerThread)((ArrayList)mPool).get(i);
//			if (worker.isAlive()) {
//				alive = true;
//				break;
//			}
//		}
//		return alive;
//	}
//
//	public boolean readerCheck() {
//		return isAlive();
//	}

	/**
	 * This method is called by the Reader thread that checks param table for any updates
	 */
	public void readerNotify(final String val) {
		int newNum = 0;
		try {
			newNum = Integer.parseInt(val);
		} catch (NumberFormatException nfe) {
			debugLog("Invalid worker size specified in fw_batch_parameter_control");
		}
		if (newNum > 0) {
			try {
				adjustNumThreads(newNum);
			} catch (TIERSBatchException e) {
				e.printStackTrace();
				debugLog("Unable to adjust worker count using Reader");
			}
		}
	}

	/**
	 * Adjusts the number of workers.
	 * This method will be indirectly called by the Reader object that checks the param table for any updates
	 * @param newCount
	 * @throws TIERSBatchException
	 */
	public void adjustNumThreads(final int newCount) throws TIERSBatchException {
		if (mPool == null) {
			throw new TIERSBatchException("Worker pool has not been initialized (Condition should not exist)");
		} else {
			if (newCount > mPool.size()) {
				debugLog("Increasing worker count to " + newCount + "...");
				while (mPool.size() != newCount) {
					debugLog("Adding worker...");
					addWorker();
				}
			} else if (newCount < mPool.size()) {
				debugLog("Reducing worker count to " + newCount + "...");
				while (mPool.size() != newCount) {
					WorkerThread worker = (WorkerThread)((ArrayList<WorkerThread>)mPool).remove(mPool.size()-1);
					debugLog("Removing worker " + worker.getName() + "...");
					worker.setKillSw(true);
				}
			}
		}
	}

	/**
	 * Prints line to error console if debugging is enabled
	 * @param msg
	 */
	private void debugLog(final String msg) {
		if (debug) {
			System.err.println(msg);
		}
	}

	/**
	 * Gets a task object from idle object pool
	 * @return
	 */
	public BaseWorkTask getIdle() {
		return mQueue.getIdle();
	}

	/**
	 * Enables/disables console debugging
	 * @param debug
	 */
	public void setDebug(final boolean debug) {
		this.debug = debug;
	}

	/**
	 * Returns a map of the worker threads' connections
	 * @return
	 */
	public HashMap<String, Connection> getWorkerConnections() {
		return connMap;
	}

	/**
	 * Commits/rollbacks and closes the worker thread connections
	 * @param isCommit boolean indicating if commit (true) or rollback (false) is needed
	 */
	public void commitWorkers(final boolean isCommit, final boolean isClose) {
		Connection workerConn;
		int count = 0;
		for (String workerName : connMap.keySet()) {
			workerConn = (Connection) connMap.get(workerName);

			if (isCommit)
				commit(workerName, workerConn);
			else
				rollback(workerName, workerConn);

			if (isClose) {
				close(workerName, workerConn);
			}
			count++;
		}
		debugLog("WorkerThreadPool: Connection commit count: " + count);
	}

	/**
	 * Commits the connection
	 * @param wName
	 * @param wConn
	 */
	private void commit(final String wName, final Connection wConn) {
		if (wConn != null) {
			try {
				wConn.commit();
			} catch (SQLException e) {
				debugLog("WorkerThreadPool: Unable to commit for " + wName + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Commits the connection
	 * @param wName
	 * @param wConn
	 */
	private void rollback(final String wName, final Connection wConn) {
		if (wConn != null) {
			try {
				wConn.rollback();
			} catch (SQLException e) {
				debugLog("WorkerThreadPool: Unable to rollback for " + wName + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Closes the connection
	 * @param wName
	 * @param wConn
	 */
	private void close(final String wName, final Connection wConn) {
		if (wConn != null) {
			try {
				wConn.close();
			} catch (SQLException e) {
				debugLog("WorkerThreadPool: Unable to close for " + wName + ": " + e.getMessage());
			}
		}
	}
}
