package org.dla.nioftp.client.session.ssh.packet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/** For use in cases where a message does not contain data beyond the message type number and padding
 *
 *
 * @author Dennis Andress
 *
 * 
 * Jan 29, 2013 12:22:40 PM
 */
public class NullFreighter extends BaseFreighter implements Freight {
	private static final Logger logger = LoggerFactory.getLogger(NullFreighter.class);

	public NullFreighter(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
	}

	@Override
	public void unloadFreight() throws PacketException {
	}

	@Override
	public byte[] getFreight() throws PacketException {
		return null;
	}

}
