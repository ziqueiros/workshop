/*
 * Created on Jul 17, 2009
 *
 */
package concurrent;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * @author TPatel
 * @since 77.0
 * SR88145 - BI-TFISS-BIP Performance
 *
 * Implementation of the data structure that holds the tasks that are waiting
 * to be worked on.  The underlying structure is an ArrayList.
 */
public class WorkQueue {
	private static final int MIN_QUEUE_SIZE = 20;	//in case workers < 5, then pick a queue big enough to prevent low queue
	/** size of task queue before overflow is triggered */
	private int queueSize = 0;
	/** percentage of queue to reach before allowing for tasks to resume queuing */
	private static final double PRODUCE_THRESHHOLD = .25; //wait until 25% empty
	
	
	/** the LinkedList that holds the pending tasks */
	private final List<BaseWorkTask> mQueue;
	private final Vector<BaseWorkTask> idleQueue;
	
	
	/** flag to denote that complete shutdown has been requested */
	private boolean mIsShutdown = false;
	/** flag to denote that graceful shutdown has been requested */
	private boolean mIsGracefulShutdown = false;
	/** flag to denote that task queue reached overflow size */
	private boolean isOverFlow = false;

	/**
	 * Creates the task queue and idle queue
	 * @param qSize
	 * @param task
	 */
	public WorkQueue(int workers, int qSize, BaseWorkTask task) {
		//if queuesize is too small, then recalc so that threshhold is worker count
		queueSize = qSize <= 0 || qSize * PRODUCE_THRESHHOLD <= workers ? (int) (workers / PRODUCE_THRESHHOLD) : qSize;
		queueSize = queueSize < MIN_QUEUE_SIZE ? MIN_QUEUE_SIZE : queueSize;	//check min queue size
		mQueue = new LinkedList<BaseWorkTask>();
		idleQueue = createIdle(queueSize + workers, task);	//account for objects used by workers
	}

	/**
	 * Adds an object to idle queue
	 *
	 */
	protected void addNewIdle() {
		//
		BaseWorkTask task = (BaseWorkTask) idleQueue.get(0);
		idleQueue.add(task.copy());
	}

	/**
	 * Creates an idle queue
	 * @param size
	 * @param task
	 * @return
	 */
	private Vector<BaseWorkTask> createIdle(int size, BaseWorkTask task) {
		Vector<BaseWorkTask> idleQueue = new Vector<BaseWorkTask>(size);
		//create size+1 objects
		for (int i = 0; i <= size; i++) {
			idleQueue.add(task.copy());
		}
		return idleQueue;
	}

	public boolean isShutdown() {
		synchronized (mQueue) {
			return mIsShutdown;
		}
	}

	/**
	 * Mark as shutdown and wake all worker threads
	 */
	public void shutdown() {
		synchronized (mQueue) {
			mIsShutdown = true;
			mQueue.notifyAll();
		}
	}

	/**
	 * Checks for graceful shutdown notice
	 * @return
	 */
	public boolean isGracefulShutdown() {
		synchronized (mQueue) {
			return mIsGracefulShutdown;
		}
	}

	/**
	 * Mark as gracefulShutdown and wake all worker threads
	 */
	public void gracefulShutdown() {
		synchronized (mQueue) {
			mIsGracefulShutdown = true;
			mQueue.notifyAll();
		}
	}

	/**
	 * Add task to queue.  If overflow point reached, then block until queue
	 * reaches lower threshhold.
	 * @param task
	 */
	public void enqueue(final BaseWorkTask task) {
		synchronized (mQueue) {
			if (getSize() > queueSize) {	//check if threshold reached
				isOverFlow = true;
				try {
					//wait for dequeue events since queue is full
					mQueue.wait();
					//Thread.sleep(10000);	//do not use .sleep (too much overhead)
				} catch (InterruptedException e) {
					//Here could be totally WRONG swallow this exception
					e.printStackTrace();
				}
			}
			
			//Ojo, por que el unico que agrega elementos es este
			mQueue.add(task);

			//wake all waiting workers
			mQueue.notifyAll();
		}
	}

	/**
	 * Returns number of tasks that are waiting to be worked on
	 * @return
	 */
	public int getSize() {
		return mQueue.size();
	}

	/**
	 * Returns the next task to be worked on; if no more tasks, then will
	 * block until a task becomes available.
	 * @return
	 * @throws InterruptedException
	 */
	public BaseWorkTask dequeue() throws InterruptedException {
		synchronized (mQueue) {
			//if there are no more tasks in the queue, wait...
			while (!isShutdown() && mQueue.isEmpty()) {
				mQueue.wait();
			}

			if (mQueue.isEmpty()) {
				isOverFlow = false;
				return null;
			} else {
				final BaseWorkTask result = (BaseWorkTask) mQueue.get(0);
				mQueue.remove(result);
				//if low threshhold reached, signal producer to add tasks
				if (isOverFlow && mQueue.size() <= PRODUCE_THRESHHOLD * queueSize) {
					isOverFlow = false;
					mQueue.notifyAll();	//in case maxThreshhold thread waiting;  moved from outside immediate if statement to here
				}
				return result;
			}
		}
	}

	/**
	 * Gets a task object from the pool of idle objects
	 * @return
	 */
	public BaseWorkTask getIdle() {

		/* this logic is not needed since the idle pool should have more than enough tasks */
		if (idleQueue.size() == 0) {
			System.err.println("** IDLE QUEUE is 0; TASK QUEUE is " + mQueue.size());
			return null;
		}
		return (BaseWorkTask) idleQueue.remove(0);
	}

	/**
	 * Adds object to the pool of idle tasks
	 * @param task
	 */
	public void addIdle(BaseWorkTask task) {
		idleQueue.add(task);
	}
}
