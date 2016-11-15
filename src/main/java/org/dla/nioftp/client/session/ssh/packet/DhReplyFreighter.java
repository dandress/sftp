package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Jan 30, 2013 2:48:04 PM
 */
public class DhReplyFreighter extends BaseFreighter implements Freight{
	private byte[] K_S = null;
	private byte[] f = null;
	private byte[] sig_of_H = null;

	public DhReplyFreighter(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
	}

	@Override
	public void unloadFreight() throws PacketException {
		K_S = buffer.getString();
		f = buffer.getMPInt();
		sig_of_H = buffer.getString();
	}

	public byte[] getK_S() {
		return K_S;
	}

	public byte[] getF() {
		return f;
	}

	public byte[] getSig_of_H() {
		return sig_of_H;
	}

}
