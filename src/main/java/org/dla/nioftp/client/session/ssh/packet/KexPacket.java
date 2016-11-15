package org.dla.nioftp.client.session.ssh.packet;

import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 *  Jan 22, 2013 2:32:06 PM
 */
public class KexPacket extends BasePacket {

	private static final Logger logger = LoggerFactory.getLogger(KexPacket.class);

	/**
	 * Constructor to create an outgoing packet. Will create a Buffer of {@code Constants.capacity} length, that is
	 * 35000 bytes. (unless someone changed it...)
	 *
	 * @param msgType An Enum of type MessageType
	 */
	public KexPacket(final MessageType msgType) {
		super(msgType);
	}

	public KexPacket(final ByteBuffer readBuffer) {
		super(readBuffer);
	}

	@Override
	public Freight getFreighter() {
		if (msgType == null)
			return null;
		switch (msgType) {
			case SSH_MSG_UNIMPLEMENTED:
				return new UnimplementedFreighter(buffer);
			case SSH_MSG_SERVICE_ACCEPT:
				return new GenericFreighter(buffer);
			case SSH_MSG_KEXINIT:
				return new KexInitFreighter(buffer);
			case SSH_MSG_KEX_DH_GEX_REQUEST:
				return new DhGexRequestFreighter(buffer);
			case SSH_MSG_KEXDH_INIT:
				return new DhInitFreighter(buffer);
			case SSH_MSG_KEXDH_REPLY:
				return new DhReplyFreighter(buffer);
			case SSH_MSG_NEWKEYS:
				return new NewKeysFreighter(buffer);
			case SSH_MSG_DISCONNECT:
				return new DisconnectFreighter(buffer);
			case SSH_MSG_USERAUTH_REQUEST:
				return new LoginFreighter(buffer);
			case SSH_MSG_SERVICE_REQUEST:
				return new ServiceRequestFreighter(buffer);
			case SSH_MSG_USERAUTH_SUCCESS:
				return new NullFreighter(buffer);
			case SSH_MSG_USERAUTH_FAILURE:
				return new GenericFreighter(buffer);
			case SSH_MSG_USERAUTH_BANNER:
				return new GenericFreighter(buffer);
			case SSH_MSG_USERAUTH_PASSWD_CHANGEREQ:
			default:
				logger.trace("What kind of Freighter carries {}?", msgType);
				return new NullFreighter(buffer);
		}
	}
}