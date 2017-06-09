package org.maltparser.concurrent.test;

import org.maltparser.core.exception.MaltChainedException;

/**
 *  ExperimentException extends the MaltChainedException class and is thrown by classes
 *  within the graph package.
 *
 * @author Johan Hall
**/
public class ExperimentException extends MaltChainedException {
	public static final long serialVersionUID = 8045568022124816379L; 

	/**
	 * Creates a ExperimentException object with a message
	 * 
	 * @param message	the message
	 */
	public ExperimentException(String message) {
		super(message);
	}
	
	/**
	 * Creates a ExperimentException object with a message and a cause to the exception.
	 * 
	 * @param message	the message
	 * @param cause		the cause to the exception
	 */
	public ExperimentException(String message, Throwable cause) {
		super(message, cause);
	}
}