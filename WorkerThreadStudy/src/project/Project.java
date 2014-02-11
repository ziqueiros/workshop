package project;

import java.io.File;

import concurrent.WorkerThreadPool;
import externals.TIERSBatchController;
import externals.TIERSBatchException;

public class Project {

	private static final boolean WAIT_FOR_COMPLETE = true;

	/**
	 * @param args
	 * @throws TIERSBatchException
	 */
	public static void main(String[] args) throws TIERSBatchException {

		(new Project()).working();

	}

	public void working() throws TIERSBatchException {
		
		MyProjectTask myTask = new MyProjectTask(this);

		int queueSize = 3;
		int workerPoolSize = 3;
		TIERSBatchController tbc = new TIERSBatchController();

		final WorkerThreadPool workerPool = new WorkerThreadPool(tbc,
				workerPoolSize, queueSize, myTask);

		File folder = new File("C:\\Users\\jzamora02\\Desktop\\base64test");
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
		    if (file.isFile()) {

				myTask = (MyProjectTask) workerPool.getIdle();

				myTask.setName(file.getName());
				myTask.initData(file.getAbsolutePath());
				workerPool.execute(myTask);
		    	
		    }
		}


		


		
		workerPool.gracefulShutdown(true);
		workerPool.commitWorkers(true, true);
		
		//timer.incrementTimings(workerPool.getTimers()[0]
		
	}

}
