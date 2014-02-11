package project;

import java.io.IOException;
import java.util.Date;

import concurrent.BaseWorkTask;
import static java.lang.System.*;

/**
 * Create a mecanism to verify is the initial data is set in the task
 * 
 * @author jzamora02
 */
public class MyProjectTask extends BaseWorkTask {

	private Project parent;
	
	boolean isInitialized = false;
	private String inputFileName;
	private String outputFileName; 
	
	
	@Override
	/**
	 * 
	 */
	public void run() {			
		out.println("::Working " + inputFileName);
		outputFileName = inputFileName +"_"+ (new Date()).getTime() +".bak";
		try {
			if(isInitialized){
				samples.Base64FileEncoder.encodeFile(inputFileName, outputFileName);
			}else{
				throw new IllegalArgumentException("Not valide name " + inputFileName ); 
			}
		} catch (IOException e) {
			// Write an Empty file and a log
			// Try to free some disk space
			// Call for change file privileges
			e.printStackTrace();
		}
		
		
		
	}

	
	public MyProjectTask( project.Project parent){
		this.parent = parent;
	}
	
	@Override
	public BaseWorkTask copy() {
		final MyProjectTask task = new MyProjectTask(this.parent);
		return task;
	}

	public void initData(String inputFileName) {
		// TODO Auto-generated method stub
		out.print(".");
		this.inputFileName = inputFileName;
		isInitialized = true;
	}

}
