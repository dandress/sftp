package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 11:25:51 AM
 */
public enum FtpMessageType {

	SSH_UNKNOWN(0),
	SSH_FXP_INIT(1),
	SSH_FXP_VERSION(2),
	SSH_FXP_OPEN(3),
	SSH_FXP_CLOSE(4),
	SSH_FXP_READ(5),
	SSH_FXP_WRITE(6),
	SSH_FXP_LSTAT(7),
	SSH_FXP_FSTAT(8),
	SSH_FXP_SETSTAT(9),
	SSH_FXP_FSETSTAT(10),
	SSH_FXP_OPENDIR(11),
	SSH_FXP_READDIR(12),
	SSH_FXP_REMOVE(13),
	SSH_FXP_MKDIR(14),
	SSH_FXP_RMDIR(15),
	SSH_FXP_REALPATH(16),
	SSH_FXP_STAT(17),
	SSH_FXP_RENAME(18),
	SSH_FXP_READLINK(19),
	SSH_FXP_SYMLINK(20),
	SSH_FXP_STATUS(101),
	SSH_FXP_HANDLE(102),
	SSH_FXP_DATA(103),
	SSH_FXP_NAME(104),
	SSH_FXP_ATTRS(105),
	SSH_FXP_EXTENDED(200),
	SSH_FXP_EXTENDED_REPLY(201);
	private int code;

	private FtpMessageType(final int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static FtpMessageType fromInt(final int code) {
		switch (code) {
			case 1:
				return SSH_FXP_INIT;
			case 2:
				return SSH_FXP_VERSION;
			case 3:
				return SSH_FXP_OPEN;
			case 4:
				return SSH_FXP_CLOSE;
			case 5:
				return SSH_FXP_READ;
			case 6:
				return SSH_FXP_WRITE;
			case 7:
				return SSH_FXP_LSTAT;
			case 8:
				return SSH_FXP_FSTAT;
			case 9:
				return SSH_FXP_SETSTAT;
			case 10:
				return SSH_FXP_FSETSTAT;
			case 11:
				return SSH_FXP_OPENDIR;
			case 12:
				return SSH_FXP_READDIR;
			case 13:
				return SSH_FXP_REMOVE;
			case 14:
				return SSH_FXP_MKDIR;
			case 15:
				return SSH_FXP_RMDIR;
			case 16:
				return SSH_FXP_REALPATH;
			case 17:
				return SSH_FXP_STAT;
			case 18:
				return SSH_FXP_RENAME;
			case 19:
				return SSH_FXP_READLINK;
			case 20:
				return SSH_FXP_SYMLINK;
			case 101:
				return SSH_FXP_STATUS;
			case 102:
				return SSH_FXP_HANDLE;
			case 103:
				return SSH_FXP_DATA;
			case 104:
				return SSH_FXP_NAME;
			case 105:
				return SSH_FXP_ATTRS;
			case 200:
				return SSH_FXP_EXTENDED;
			case 201:
				return SSH_FXP_EXTENDED_REPLY;
			default:
				return SSH_UNKNOWN;
		}
	}
}
