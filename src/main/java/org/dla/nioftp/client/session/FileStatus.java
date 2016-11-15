package org.dla.nioftp.client.session;

/**
 *
 * @author Dennis Andress
 *
 * nioftp Jun 11, 2013 11:20:48 AM
 */
public class FileStatus {

	public enum Status {

		OPENED, SUCCESS, CHAN_OPENED, CHAN_OPEN_FAILED, CHAN_CLOSED, FILE_OPEN_FAILED, DISCONNECTED
	};
	private final Status status;
	private final String fileName;
	private final String errorMsg;

	public FileStatus(final Status status, final String fileName) {
		this.status = status;
		this.fileName = fileName;
		this.errorMsg = null;
	}

	public FileStatus(final Status status, final String fileName, final String errorMsg) {
		this.status = status;
		this.fileName = fileName;
		this.errorMsg = errorMsg;
	}

	public Status getStatus() {
		return status;
	}

	public String getFileName() {
		return fileName;
	}

	public String getErrorMsg() {
		return errorMsg;
	}
}
