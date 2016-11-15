package org.dla.nioftp.client.session.ssh.packet;

import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 7:51:13 AM
 */
public class SshPacket extends BasePacket {

	private static final Logger logger = LoggerFactory.getLogger(SshPacket.class);

	public SshPacket(final MessageType msgType) {
		super(msgType);
	}

	public SshPacket(final ByteBuffer readBuffer) {
		super(readBuffer);
	}

	@Override
	public Freight getFreighter() {
		if (msgType == null)
			return null;
		switch (msgType) {
			case SSH_MSG_GLOBAL_REQUEST:
				return new ChanGlobalRequest(buffer);
			case SSH_MSG_REQUEST_SUCCESS:
				return new ChanRequestSuccess(buffer);
			case SSH_MSG_REQUEST_FAILURE:
				return new ChanRequestFailure(buffer);
			case SSH_MSG_CHANNEL_OPEN:
				return new ChanOpen(buffer);
			case SSH_MSG_CHANNEL_OPEN_CONFIRMATION:
				return new ChanOpenConfirm(buffer);
			case SSH_MSG_CHANNEL_OPEN_FAILURE:
				return new ChanOpenFailure(buffer);
			case SSH_MSG_CHANNEL_WINDOW_ADJUST:
				return new ChanWindowAdjust(buffer);
			case SSH_MSG_CHANNEL_DATA:
				return new ChanData(buffer);
			case SSH_MSG_CHANNEL_EXTENDED_DATA:
				return new ChanExData(buffer);
			case SSH_MSG_CHANNEL_EOF:
				return new ChanEOF(buffer);
			case SSH_MSG_CHANNEL_CLOSE:
				return new ChanClose(buffer);
			case SSH_MSG_CHANNEL_REQUEST:
				return new ChanRequest(buffer);
			case SSH_MSG_CHANNEL_SUCCESS:
				return new ChanSuccess(buffer);
			case SSH_MSG_CHANNEL_FAILURE:
				return new ChanFailure(buffer);
			case SSH_MSG_DISCONNECT:
				return new DisconnectFreighter(buffer);
			default:
				logger.trace("What kind of Freighter carries {}?", msgType);
				return new NullFreighter(buffer);
		}
	}

	public Buffer getBuffer() {
		return buffer;
	}
}
