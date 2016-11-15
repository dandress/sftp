package org.dla.nioftp.client;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Jan 25, 2013 9:40:46 AM
 */
public class TransException extends Exception {
	static final long serialVersionUID = 1l;

	public TransException(final String msg) {
		super(msg);
	}

	public TransException(final String msg, Throwable cause) {
		super(msg, cause);
	}

	public TransException(Throwable cause) {
		super(cause);
	}

	public TransException() {
		super();
	}

}
