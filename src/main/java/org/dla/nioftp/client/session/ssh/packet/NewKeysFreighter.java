package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Jan 31, 2013 9:28:52 AM
 */
public class NewKeysFreighter extends BaseFreighter implements Freight {

	public NewKeysFreighter(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
	}

	@Override
	public void unloadFreight() throws PacketException {
	}

}
