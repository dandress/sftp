package org.dla.nioftp.client.session.ssh.packet;

/**
 *
* @author Dennis Andress
 *
 * 
 * Jan 29, 2013 1:47:23 PM
 */
public class DhInitFreighter extends BaseFreighter implements Freight {

	protected byte[] e = null;

	public DhInitFreighter(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
		assert e != null;
		buffer.putMPInt(e);
	}

	@Override
	public void unloadFreight() throws PacketException {
	}

	public byte[] getE() {
		return e;
	}

	public void setE(byte[] e) {
		this.e = e;
	}

}
