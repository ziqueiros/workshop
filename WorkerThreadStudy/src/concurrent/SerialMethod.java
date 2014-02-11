package concurrent;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Defines a method to execute.
 * @author Trushyap Patel
 *
 */
public class SerialMethod{
	/** contains all instantiated serialMethod names */
//	private static HashMap<String, String> serialNames = new HashMap<String, String>();//Commented as part of SR230166
	//Begins - SR230166 - Modified for batch multi-processing 
	/** contains all instantiated serialMethod names */ 
	private  HashMap<String, String> serialNames = new HashMap<String, String>();
	//Ends - SR230166
	/** unique name representing this object (used for return object and exception retrieval) */
	protected String serialName;
	/** class containing method */
	protected Class<?> serialClass;
	/** method to execute */
	protected Method serialMethod;
	/** method params */
	protected Object[] methodParams;
	/** object instance */
	protected Object serialObjInst;


	/**
	 * Defines a serial method which will be called on the specified targetObject.
	 * @param serialName The unique name for this method block (can be used for return obj retrieval)
	 * @param targetObject The target object (use an instance)
	 * @param methodName The target method name
	 * @param methodParams The method parameters
	 * @throws ParallelMethodException
	 */
	public SerialMethod(final String serialName, final Object targetObject, final String methodName, final Object... methodParams) throws ParallelMethodException {
		this(serialName, targetObject.getClass(), methodName, methodParams);
		this.serialObjInst = targetObject;
	}

	/**
	 * Defines a serial method which will be called on a new instance of the specified targetClass.
	 * This object represents one method needing execution.
	 * @param serialName The unique name for this method block (can be used for return obj retrieval)
	 * @param targetClass The target class (use .class as the parameter)
	 * @param methodName The target method name
	 * @param methodParams The method parameters
	 * @throws ParallelMethodException
	 */
	public SerialMethod(final String serialName, final Class<?> targetClass, final String methodName, final Object... methodParams) throws ParallelMethodException {
		/* Check for unique identifier */
		if (serialNames.get(serialName) != null) {
			throw new ParallelMethodException("SerialMethod " + serialName + " already created");
		}
		serialNames.put(serialName, serialName);

		this.serialName = serialName;
		this.serialClass = targetClass;
		this.methodParams = methodParams;
		
		/* Get the parameter classes (in order to search for the method) */
		Class<?>[] paramTypes = null;
		if (methodParams == null) {
			paramTypes = new Class[] {String.class};
		} else {
			paramTypes = new Class[methodParams.length];
			int i = 0;
			for (Object param : methodParams) {
				paramTypes[i++] = param == null ? String.class : param.getClass();
			}
		}

		/* Search for method */
		serialMethod = null;
		try {
			serialMethod = targetClass.getDeclaredMethod(methodName, paramTypes);
			serialMethod.setAccessible(true);
		} catch (SecurityException e) {
			throw new ParallelMethodException("SecurityException: " + e.getMessage());
		} catch (NoSuchMethodException e) {
			Method[] methods = targetClass.getDeclaredMethods();
			boolean found = false;
			for (Method method : methods) {
				if (method.getName().equals(methodName) && methodParams.length == method.getParameterTypes().length) {
					found = true;
					//System.err.println("Potential match with: " + method.getName());
					serialMethod = method;
					serialMethod.setAccessible(true);
					break;
				}
			}
			if (!found)
				throw new ParallelMethodException("NoSuchMethodException: " + e.getMessage());
		}

	}
	
}