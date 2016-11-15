package org.dla.nioftp.client.session.ssh.packet;

/**
 * byte SSH_FXP_OPEN
 * uint32 request-id		-- handled by FtpPacket
 * string filename [UTF-8]
 * uint32 desired-access
 * uint32 flags
 * ATTRS attrs
 * <p/>
 * @author Dennis Andress
 *
 * 
 * Feb 14, 2013 11:11:40 AM
 */
public class FxpOpen extends BaseFreighter implements Freight {

	private String path;
	private int access;	// desired-access
	private int flags = 0;  // flags  JSCH called this attrs...
	private int attrs = 0;  // flags  JSCH called this attrs...

	public FxpOpen(final Buffer buffer, final int fxpLength) {
		super(buffer);
		this.fxpLength = fxpLength;
	}

	@Override
	public void loadFreight() throws PacketException {
		buffer.putString(stringToByte(path));
		buffer.putInt(access);
		buffer.putInt(flags);
//		buffer.putInt(attrs);		// gernerally ignored. The server should create meaningful values if this is not present
	}

	@Override
	public void unloadFreight() throws PacketException {
		throw new UnsupportedOperationException("Not supported yet.");
	}



	public void setOpenA() {
		access = FileConstants.ACL_WRITE_DATA; // | FileConstants.FXF_CREAT;  // and possiblY FXF_APPEND
		flags = FileConstants.FLAG_CREATE_NEW;
	}
	public void setOpenW() {
		// these values do not match what the spec called for. They are from JCSH. These work, the spec's didn't
		access = FileConstants.FXF_WRITE | FileConstants.FXF_CREAT;// | FileConstants.FXF_APPEND;
		flags =  FileConstants.FLAG_APPEND_DATA_ATOMIC;		// allows for sending file without calculating offset....
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setAttrs(int attrs) {
		this.attrs = attrs;
	}
}
