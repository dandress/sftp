package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 14, 2013 11:25:20 AM
 */
public class FileConstants {

	public static final int FXF_READ = 0x00000001;
	public static final int FXF_WRITE = 0x00000002;
	public static final int FXF_APPEND = 0x00000004;
	public static final int FXF_CREAT = 0x00000008;
	public static final int FXF_TRUNC = 0x00000010;
	public static final int FXF_EXCL = 0x00000020;
	public static final int FILEXFER_ATTR_SIZE = 0x00000001;
	public static final int FILEXFER_ATTR_UIDGID = 0x00000002;
	public static final int FILEXFER_ATTR_PERMISSIONS = 0x00000004;
	public static final int FILEXFER_ATTR_ACMODTIME = 0x00000008;
	public static final int FILEXFER_ATTR_EXTENDED = 0x80000000;
	public static final int FX_OK = 0;
	public static final int FX_EOF = 1;
	public static final int FX_NO_SUCH_FILE = 2;
	public static final int FX_PERMISSION_DENIED = 3;
	public static final int FX_FAILURE = 4;
	public static final int FX_BAD_MESSAGE = 5;
	public static final int FX_NO_CONNECTION = 6;
	public static final int FX_CONNECTION_LOST = 7;
	public static final int FX_OP_UNSUPPORTED = 8;
// ----
	public static final int ACL_READ_DATA = 0x00000001;
	public static final int ACL_LIST_DIRECTORY = 0x00000001;
	public static final int ACL_WRITE_DATA = 0x00000002;
	public static final int ACL_ADD_FILE = 0x00000002;
	public static final int ACL_APPEND_DATA = 0x00000004;
	public static final int ACL_ADD_SUBDIRECTORY = 0x00000004;
	public static final int ACL_READ_NAMED_ATTRS = 0x00000008;
	public static final int ACL_WRITE_NAMED_ATTRS = 0x00000010;
	public static final int ACL_EXECUTE = 0x00000020;
	public static final int ACL_DELETE_CHILD = 0x00000040;
	public static final int ACL_READ_ATTRIBUTES = 0x00000080;
	public static final int ACL_WRITE_ATTRIBUTES = 0x00000100;
	public static final int ACL_DELETE = 0x00010000;
	public static final int ACL_READ_ACL = 0x00020000;
	public static final int ACL_WRITE_ACL = 0x00040000;
	public static final int ACL_WRITE_OWNER = 0x00080000;
	public static final int ACL_SYNCHRONIZE = 0x00100000;
	// ---
	public static final int FLAG_ACCESS_DISPOSITION = 0x00000007;
	public static final int FLAG_CREATE_NEW = 0x00000000;
	public static final int FLAG_CREATE_TRUNCATE = 0x00000001;
	public static final int FLAG_OPEN_EXISTING = 0x00000002;
	public static final int FLAG_OPEN_OR_CREATE = 0x00000003;
	public static final int FLAG_TRUNCATE_EXISTING = 0x00000004;
	public static final int FLAG_APPEND_DATA = 0x00000008;
	public static final int FLAG_APPEND_DATA_ATOMIC = 0x00000010;
	public static final int FLAG_TEXT_MODE = 0x00000020;
	public static final int FLAG_BLOCK_READ = 0x00000040;
	public static final int FLAG_BLOCK_WRITE = 0x00000080;
	public static final int FLAG_BLOCK_DELETE = 0x00000100;
	public static final int FLAG_BLOCK_ADVISORY = 0x00000200;
	public static final int FLAG_NOFOLLOW = 0x00000400;
	public static final int FLAG_DELETE_ON_CLOSE = 0x00000800;
	public static final int FLAG_ACCESS_AUDIT_ALARM_INFO = 0x00001000;
	public static final int FLAG_ACCESS_BACKUP = 0x00002000;
	public static final int FLAG_BACKUP_STREAM = 0x00004000;
	public static final int FLAG_OVERRIDE_OWNER = 0x00008000;
}
