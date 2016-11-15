package org.dla.nioftp.client.session;

/**
 *
 * @author Dennis Andress
 *
 * nioftp
 * Jun 11, 2013 10:15:37 AM
 */
public class SessionStatus {
	public enum StatusType {
		SSH_KEX,
		SSH_USERAUTH_SVC_ACCEPT,
		SSH_AUTH_SUCCESS,
		SSH_AUTH_FAILED,
		SSH_DISCONNECTED;
	}

	private final StatusType status;
	private final String message;

	public SessionStatus(final StatusType status) {
		this.status = status;
		this.message = null;
	}

	public SessionStatus(final StatusType status, final String message) {
		this.status = status;
		this.message = message;
	}

	public StatusType getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

}
