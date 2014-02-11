package concurrent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Parallel Method Execution Manager:
 * Use this to execute long running methods in parallel.
 * Define methods (SerialBlock) to run serially in an ArrayList and use addRunBlock().
 * Call execute() to start the methods.
 * @author Trushyap Patel
 */
public class ParallelMethods {
	/** Denotes path to take if an exception occurs while executing a method */
	public static enum ON_EXCEP { EXCEP, STOP, CONT; };

	/** parallel set of serialMethods to execute */
	private ArrayList<ArrayList<SerialMethod>> parallelBlocks = new ArrayList<ArrayList<SerialMethod>>();
	/** method return objects */
	private HashMap<String, Object> retObjs = new HashMap<String, Object>();
	/** method exceptions */
	private HashMap<String, Throwable> excepObjs = new HashMap<String, Throwable>();
	/** Denotes course of execution on remaining methods after an exception */
	private ON_EXCEP stopOnExcep = ON_EXCEP.CONT;
	/** Denotes count of remaining methods that have been left unprocessed */
	private int remainingMethods;
	/** Denotes whether to print debugging info */
	private boolean debug = false;

	/**
	 * Constructor.
	 * 
	 * @param path Denotes if method processing should stop if there is an exception
	 */
	public ParallelMethods(final ON_EXCEP path) {
		this.stopOnExcep = path;
	}

	/**
	 * Constructor.
	 * 
	 * @param path Denotes if method processing should stop if there is an exception
	 */
	public ParallelMethods(final ON_EXCEP path, final boolean debug) {
		this.stopOnExcep = path;
		this.debug = debug;
	}

	/**
	 * Resets this object.
	 * Specify new exception handling path.
	 * @param path Denotes if method processing should stop, continue, or exception if there is an exception
	 */
	public void reset(final ON_EXCEP path) {
		parallelBlocks.clear();
		retObjs.clear();
		excepObjs.clear();
		remainingMethods = 0;
		stopOnExcep = path;
	}

	/**
	 * Gets the return objects
	 * @return
	 */
	public HashMap<String, Object> getRetObjs() {
		return retObjs;
	}
	
	/**
	 * Returns the exceptions
	 * @return
	 */
	public HashMap<String, Throwable> getExcepObjs() {
		return excepObjs;
	}

	/**
	 * Returns the number of remaining methods.
	 * This can be used if STOP_ON_EXCEP is used.
	 * @return
	 */
	public int getReminingMethods() {
		return remainingMethods;
	}

	/**
	 * Adds the serialMethods to the parallel block
	 * @param serialMethods A list of serialMethods that should be executed serially
	 */
	public void addSerialMethods(final ArrayList<SerialMethod> serialMethods) {
		if (serialMethods != null) {
			final ArrayList<SerialMethod> serialMethodList = new ArrayList<SerialMethod>();
			for (SerialMethod serialMethod : serialMethods) {
				serialMethodList.add(serialMethod);
			}
			parallelBlocks.add(serialMethodList);
		}
	}

	/**
	 * Adds a single serialMethod to the parallel block
	 * @param serialMethod A single serialMethod that should be executed
	 */
	public void addSerialMethod(final SerialMethod serialMethod) {
		if (serialMethod != null) {
			final ArrayList<SerialMethod> serialMethodList = new ArrayList<SerialMethod>(1);
			serialMethodList.add(serialMethod);
			parallelBlocks.add(serialMethodList);
		}
	}

	/**
	 * Executes the methods in parallel.
	 * Method blocks until all methods have completed.
	 * @throws ParallelMethodException 
	 */
	public void execute() throws ParallelMethodException {
		//Keeps track of spawned threads
		final Thread[] threads = new Thread[parallelBlocks.size()];
		int idx = -1;
		//spawn each parallel set of serialMethods
		for (ArrayList<SerialMethod> block : parallelBlocks) {
			threads[++idx] = getThread(block);
			threads[idx].start();
			if (debug) System.err.print("[+T" + idx + "]");
		}
		//wait for all threads to finish
		int idx2 = -1;
		for (Thread thread : threads) {
			try {
				idx2++;
				thread.join();
				if (debug) System.err.print("[-T" + idx2 + "]");
			} catch (InterruptedException e) {
				if (debug) System.err.print("[xT" + idx2 + "]");
				addExcep(thread.getName(), e);
			}
		}
		
		// if execution style was EXCEP, then throw an exception
		if (remainingMethods > 0 && ON_EXCEP.EXCEP.equals(stopOnExcep)) {
			throw new ParallelMethodException("Exception while processing methods (Remaining: " + remainingMethods + ")");
		}
	}

	/**
	 * Returns the count of exceptions during method invocations
	 * @return
	 */
	public int getExcepCount() {
		return excepObjs.size();
	}

	/**
	 * Create a new thread to execute the serialBlock list
	 * @param serialMethodList
	 * @return
	 */
	private Thread getThread(final ArrayList<SerialMethod> serialMethodList) {
		final Thread t = new Thread(
	            new Runnable() {
					public void run() {
						int remainingMethods = serialMethodList.size(); //track remaining number of methods to process
						// Execute all methods serially
						for (SerialMethod serialMethodObj : serialMethodList) {
							try {
								/* 
								 * Invoke the method and capture the return object (if any)
								 * Note that a new instance is created for every method call if an object instance was not already specified.
								 * Since this framework should be used for long running methods, instantiating
								 * a few extra objects should not be an issue. 
								 */
								final Object obj = serialMethodObj.serialMethod.invoke(
										serialMethodObj.serialObjInst == null ? serialMethodObj.serialClass.newInstance() : serialMethodObj.serialObjInst, 
										serialMethodObj.methodParams);

								//decrement the num of methods remaining
								remainingMethods--;
								
								// Save the return object
								if (void.class != serialMethodObj.serialMethod.getReturnType()) {
									addReturn(serialMethodObj.serialName, obj);
								}
							} catch (Exception e) {
								//IllegalArgumentException
								//IllegalAccessException
								//InvocationTargetException
								//InstantiationException
								addExcep(serialMethodObj.serialName, e);
								
								//check if remaining methods should stop processing if excep occurs
								if (ON_EXCEP.STOP.equals(stopOnExcep) || ON_EXCEP.EXCEP.equals(stopOnExcep)) {
									addRemaining(remainingMethods);
									break;
								}
							}
						}
	                }
	            });
		return t;
	}
	
	/**
	 * Adds the remaining methods
	 * @param num
	 */
	private synchronized void addRemaining(final int num) {
		remainingMethods += num;
	}

	/**
	 * Adds the method exception
	 * @param blockName
	 * @param e
	 */
	private synchronized void addExcep(final String blockName, final Exception e) {
		if (debug) System.err.println("Exception: " + e.getMessage());
		excepObjs.put(blockName, e.getCause() != null ? e.getCause() : e);
	}

	/**
	 * Adds the return object
	 * @param blockName
	 * @param obj
	 */
	private synchronized void addReturn(final String blockName, final Object obj) {
		retObjs.put(blockName, obj);
	}

}

