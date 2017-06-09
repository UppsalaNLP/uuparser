package org.maltparser.concurrent.graph;

import org.maltparser.core.exception.MaltChainedException;

/**
 *  ConcurrentGraphException extends the MaltChainedException class and is thrown by classes
 *  within the graph package.
 *
 * @author Johan Hall
**/
public class ConcurrentGraphException extends MaltChainedException {
	public static final long serialVersionUID = 8045568022124816379L; 

	/**
	 * Creates a ConcurrentGraphException object with a message
	 * 
	 * @param message	the message
	 */
	public ConcurrentGraphException(String message) {
		super(message);
	}
	
	/**
	 * Creates a ConcurrentGraphException object with a message and a cause to the exception.
	 * 
	 * @param message	the message
	 * @param cause		the cause to the exception
	 */
	public ConcurrentGraphException(String message, Throwable cause) {
		super(message, cause);
	}
}