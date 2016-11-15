package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 8, 2013 12:16:27 PM
 */
public class ServiceAcceptFreighter extends BaseFreighter implements Freight {
	private String service;

	public ServiceAcceptFreighter(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
	}

	@Override
	public void unloadFreight() throws PacketException {
		final byte[] b = buffer.getString();
		service = byteToString(b);
	}

	public String getService() {
		return service;
	}

}
