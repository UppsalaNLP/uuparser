package org.maltparser.core.lw.graph;

import org.maltparser.core.exception.MaltChainedException;

/**
 *  LWGraphException extends the MaltChainedException class and is thrown by classes
 *  within the graph package.
 *
 * @author Johan Hall
**/
public class LWGraphException extends MaltChainedException {
	public static final long serialVersionUID = 8045568022124816379L; 

	/**
	 * Creates a LWGraphException object with a message
	 * 
	 * @param message	the message
	 */
	public LWGraphException(String message) {
		super(message);
	}
	
	/**
	 * Creates a LWGraphException object with a message and a cause to the exception.
	 * 
	 * @param message	the message
	 * @param cause		the cause to the exception
	 */
	public LWGraphException(String message, Throwable cause) {
		super(message, cause);
	}
}