package org.dla.nioftp.client.session.ssh.packet;

/**  Does not normally contain data beyond the type field handled by Packet
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:10:55 AM
 *
 */public class ChanRequestFailure extends BaseFreighter implements Freight {

	public ChanRequestFailure (final Buffer buffer) {
		super(buffer);
	}


	@Override
	public void loadFreight() throws PacketException {
	}

	@Override
	public void unloadFreight() throws PacketException {
	}

}
