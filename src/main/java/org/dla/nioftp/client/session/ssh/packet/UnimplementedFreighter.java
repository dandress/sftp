package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 8, 2013 11:41:39 AM
 */
public class UnimplementedFreighter extends BaseFreighter implements Freight {
	private int rejectedPacket;

	public UnimplementedFreighter(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
	}

	@Override
	public void unloadFreight() throws PacketException {
		rejectedPacket = buffer.getInt();
	}

	public int getRejectedPacket() {
		return rejectedPacket;
	}
}
