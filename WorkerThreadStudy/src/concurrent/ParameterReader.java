/*
 * Created on Jul 27, 2009
 *
 */
package concurrent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;


import externals.TIERSBatchController;
import externals.TIERSBatchException;

/**
 * @author TPatel
 * @since 77.0
 * SR88145 - BI-TFISS-BIP Performance
 *
 */
public class ParameterReader implements Runnable {
	private static final int DEFAULT_CHECK_FREQ_MINS = 1;
	private static final int MIN_TO_MSEC = 60*1000;	//60000 ms in a min
	private static final String DEFAULT_SEARCH_STR = "";

	private TIERSBatchController batchController;
	private String param = null;
	private String searchStr = "";
	private static boolean checkNeeded;
	private Method notifyMethod;
	private Object callClass;
	private int checkFreqMins;

	/**
	 * Starts a dynamic check for the specified paramStr in FW_BATCH_PARAMETER_CONTROL
	 * @param batchCon
	 * @param jobId
	 * @param paramStr
	 * @param caller
	 * @param notifyMeth
	 * @param checkFreqMins
	 * @throws TIERSBatchException
	 */
	public ParameterReader(final TIERSBatchController batchCon, final String jobId, final String paramStr, final Object caller, final Method notifyMeth, final int checkFreqMins) throws TIERSBatchException {
		if (caller == null || notifyMeth == null) {
			throw new TIERSBatchException("Unable to create " + this.getClass().getName() + " due to missing caller, notify, and/or check methods");
		}
		batchController = batchCon;
		batchController.setJobId(jobId);
		//checks for the specified parameter; otherwise will check the first value
		searchStr = paramStr == null || paramStr.trim().length() == 0 ? DEFAULT_SEARCH_STR : paramStr;
		callClass = caller;

		setCheckNeeded(true);
		notifyMethod = notifyMeth;
		this.checkFreqMins = checkFreqMins <= 0 ? DEFAULT_CHECK_FREQ_MINS : checkFreqMins;
	}

	/**
	 * Calls the notify method with the param value
	 */
	private void callNotify() {
		try {
			notifyMethod.invoke(callClass, new Object[] {getParam()});
		} catch (IllegalArgumentException e2) {
			e2.printStackTrace();
		} catch (IllegalAccessException e2) {
			e2.printStackTrace();
		} catch (InvocationTargetException e2) {
			e2.printStackTrace();
		}
	}

	/**
	 * Until the check method returns false, checks and notifies notify method if there is a change in paramter
	 */
	public void run() {
		int numChecks = 0;	//safeguard against runaway threads
		//BatchParameter param;
		ArrayList<?> paramList;
		String paramVal = null;
		//while the check method returns true, keep checking parameter
		while (isCheckNeeded() && ++numChecks < 100) {
			try {
				
				//param = batchController.getParameters();	//get the parameters from fw_batch table
				//paramList = param.getFunctionalParameters();
				//paramVal = findParamVal(paramList);

				//if value has changed from last time, then...
				if (checkIfChanged(paramVal)) {
					setParam(paramVal);	//save the new value
					callNotify();	//call notify method with the stored value
				}
				Thread.sleep(checkFreqMins * MIN_TO_MSEC);
			
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * If searchStr specified, finds the parameter which contains searchStr, and returns the part of the parameter before the searchStr.
	 * If not specified, then returns the first parameter.
	 * @param list
	 * @return
	 */
	private String findParamVal(ArrayList<?> list) {
		String retVal = null;
		if (list != null) {
			String val;
			for (int i = 0; i < list.size(); i++) {
				val = (String)list.get(i);
				if (val != null && (DEFAULT_SEARCH_STR.equals(searchStr) || val.indexOf(searchStr) > 0)) {
					retVal = DEFAULT_SEARCH_STR.equals(searchStr) ? val : val.substring(0, val.indexOf(searchStr));
					break;
				}
			}
		}
		return retVal;
	}

	/**
	 * Checks if the specified param value is diff from the previously stored value
	 * @param paramVal
	 * @return true if param is diff from instance value; false otherwise
	 */
	private boolean checkIfChanged(String paramVal) {
		return (getParam() == null && paramVal != null) ||
				(getParam() != null && !getParam().equalsIgnoreCase(paramVal));
	}

	/**
	 * @return
	 */
	protected String getParam() {
		return param;
	}

	/**
	 * @param string
	 */
	protected void setParam(String str) {
		param = str;
	}

	/**
	 * @return
	 */
	protected static boolean isCheckNeeded() {
		return checkNeeded;
	}

	/**
	 * @param b
	 */
	protected static void setCheckNeeded(boolean b) {
		checkNeeded = b;
	}

}
