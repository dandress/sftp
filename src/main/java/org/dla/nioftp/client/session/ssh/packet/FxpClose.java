package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * nioftp
 * Feb 15, 2013 11:05:34 AM
 */
public class FxpClose extends BaseFreighter implements Freight {
	private byte[] handle;

	public FxpClose(Buffer buffer, int fxpLength) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
		buffer.putString(handle);
	}

	@Override
	public void unloadFreight() throws PacketException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setHandle(byte[] handle) {
		this.handle = handle;
	}

}
