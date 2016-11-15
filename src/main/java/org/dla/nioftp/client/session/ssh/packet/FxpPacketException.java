package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * nioftp
 * Feb 20, 2013 8:28:26 AM
 */
public class FxpPacketException extends Exception {
	static final long serialVersionUID = 1l;

	public FxpPacketException(final String msg) {
		super(msg);
	}

	public FxpPacketException(final String msg, Throwable cause) {
		super(msg, cause);
	}

	public FxpPacketException(Throwable cause) {
		super(cause);
	}

	public FxpPacketException() {
		super();
	}

}
