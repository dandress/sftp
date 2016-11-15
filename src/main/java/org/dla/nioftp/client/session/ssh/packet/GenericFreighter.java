package org.dla.nioftp.client.session.ssh.packet;

/**  Freighter for messages containing a single String
 *
 * @author Dennis Andress
 *
 * 
 * Feb 9, 2013 3:44:41 PM
 */
public class GenericFreighter extends BaseFreighter implements Freight {
	private String message;

	public GenericFreighter(final Buffer buffer) {
		super(buffer);
	}
	@Override
	public void loadFreight() throws PacketException {
	}

	@Override
	public void unloadFreight() throws PacketException {
		final byte[] b = buffer.getString();
		message = byteToString(b);
	}

	@Override
	public String getMessage() {
		return message;
	}
}


