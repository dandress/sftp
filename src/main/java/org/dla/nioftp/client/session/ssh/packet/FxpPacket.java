package org.dla.nioftp.client.session.ssh.packet;

import org.dla.nioftp.Constants;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * FXP_xxx Packets are contained in SSH_MSG_CHANNEL_DATA packets. Not that
 * there's a spec describing how that is done....
 * </p>
 * <p>
 * The SSH_MSG_CHANNEL_DATA packet has but two fields, recipientChannel, and the data.
 * FXP_xxx packets canonically place these three fields in 'data' Followed by fields specific to each packet type.
 * </p>
 * <p><pre>
 * uint32 length
 * byte type
 * uint32 request-id
 * </pre></p
 * <p>
 * Here, we break from the usual pattern of Packet and Freighter and capture these FXP_xxx fields in the Packet,
 * leaving the Freighter to deal only with the 'data' field(s) that follow
 * </p>
 * <p/>
 * @author Dennis Andress
 *
 * 
 * Feb 12, 2013 10:59:21 AM
 */
public class FxpPacket extends BasePacket {

	private static final Logger logger = LoggerFactory.getLogger(FxpPacket.class);
	private int recipientChannel;
	private int fxpLength;
	private int fxpType;
	private int requestId;
	private FtpMessageType fxpMessageType;

	/**
	 *
	 * @param fxpMsgType
	 * @param recipientChannel
	 * @param fxpLength        Seems to be the length one byte for fxpMessageType plus anything that follows after
	 *                            in the 'data' section.
	 * @param requestId
	 */
	public FxpPacket(final FtpMessageType fxpMsgType, final int recipientChannel, final int fxpLength, final int requestId) {
		super(MessageType.SSH_MSG_CHANNEL_DATA);
		this.recipientChannel = recipientChannel;
		this.fxpMessageType = fxpMsgType;
		this.fxpType = fxpMsgType.getCode();
		this.fxpLength = fxpLength;
		this.requestId = requestId;
		buffer.setOffset(6);  // byte 6 starts the payload, which is read in a Freighter
		if (recipientChannel < 0 || recipientChannel > 1000)
			logger.warn("recipientChannelNumber is out of bounds. {}", recipientChannel);
		buffer.putInt(recipientChannel);
		buffer.putInt(fxpLength + 4); // I've no idea what this is about. It's not in any spec that I found.
		buffer.putInt(fxpLength); // but it worked for JSCH, and seems to work here. (fails when either line is removed.)
		buffer.putByte((byte) fxpType);
		// not every packet gets this...
		if (fxpMessageType != FtpMessageType.SSH_FXP_INIT && fxpMessageType != FtpMessageType.SSH_FXP_VERSION)
			buffer.putInt(requestId);
	}

	/**
	 *
	 * @param fxpMsgType
	 * @param recipientChannel
	 * @param fxpLength        Seems to be the length one byte for fxpMessageType plus anything that follows after
	 *                            in the 'data' section.
	 * @param bufferSize		     Allows for creating a larger than normal buffer
	 * @param requestId
	 */
	public FxpPacket(final FtpMessageType fxpMsgType, final int recipientChannel, final int fxpLength, final int requestId, final int bufferSize) {
		super(MessageType.SSH_MSG_CHANNEL_DATA, bufferSize);
		this.recipientChannel = recipientChannel;
		this.fxpMessageType = fxpMsgType;
		this.fxpType = fxpMsgType.getCode();
		this.fxpLength = fxpLength;
		this.requestId = requestId;
		buffer.setOffset(6);  // byte 6 starts the payload, which is read in a Freighter
		if (recipientChannel < 0 || recipientChannel > 1000)
			logger.warn("recipientChannelNumber is out of bounds. {}", recipientChannel);
		buffer.putInt(recipientChannel);
		buffer.putInt(fxpLength + 4); // I've no idea what this is about. It's not in any spec that I found.
		buffer.putInt(fxpLength); // but it worked for JSCH, and seems to work here. (fails when either line is removed.)
		buffer.putByte((byte) fxpType);
		// not every packet gets this...
		if (fxpMessageType != FtpMessageType.SSH_FXP_INIT && fxpMessageType != FtpMessageType.SSH_FXP_VERSION)
			buffer.putInt(requestId);
	}

	public FxpPacket(final ByteBuffer readBuffer) {
		super(readBuffer);
	}

	@Override
	public void init(final int begin, int in_sequence_no) throws PacketException, FxpPacketException {
		super.init(begin, in_sequence_no);
		if (buffer == null)
			return;
		try {
			recipientChannel = buffer.getInt();
			final int u = buffer.getInt();		// unknown, not in spec but present all the same
			fxpLength = buffer.getInt();
			fxpType = buffer.getByte();
			fxpMessageType = FtpMessageType.fromInt(fxpType);
			// not every packet gets this...
			if (fxpMessageType != FtpMessageType.SSH_FXP_INIT && fxpMessageType != FtpMessageType.SSH_FXP_VERSION)
				requestId = buffer.getInt();
		} catch (ArrayIndexOutOfBoundsException ex) {		// apparently this isn't an FxpPacket..
			throw new FxpPacketException();		// so, throw an exception and lest Session try again (using SshPacket)
		}
		if (fxpLength < 0 || fxpLength > Constants.capacity)
			throw new FxpPacketException();
	}

	@Override
	public void clear() {
		super.clear();
		fxpMessageType = null;
	}

	@Override
	public Freight getFreighter() {
		if (msgType == null)
			return null;

		// we could get any of these messages at any time.....
		// SSH_MSG_CHANNEL_DATA is the one to be handled as a FXP packet
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
				break;		// escape this switch statement and go on to the next....
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

		switch (fxpMessageType) {

			case SSH_UNKNOWN:
				logger.error("--------------------  UNKNOWN  -----------------");
				return null;
			case SSH_FXP_INIT:
				return new FxpInit(buffer, fxpLength);
			case SSH_FXP_VERSION:
				return new FxpVersion(buffer, fxpLength);
			case SSH_FXP_OPEN:
				return new FxpOpen(buffer, fxpLength);
			case SSH_FXP_CLOSE:
				return new FxpClose(buffer, fxpLength);
			case SSH_FXP_READ:
			case SSH_FXP_WRITE:
				return new FxpWrite(buffer, fxpLength);
			case SSH_FXP_LSTAT:
			case SSH_FXP_FSTAT:
			case SSH_FXP_SETSTAT:
			case SSH_FXP_FSETSTAT:
			case SSH_FXP_OPENDIR:
			case SSH_FXP_READDIR:
			case SSH_FXP_REMOVE:
			case SSH_FXP_MKDIR:
			case SSH_FXP_RMDIR:
			case SSH_FXP_REALPATH:
			case SSH_FXP_STAT:
			case SSH_FXP_RENAME:
			case SSH_FXP_READLINK:
			case SSH_FXP_SYMLINK:
			case SSH_FXP_STATUS:
				return new FxpStatus(buffer, fxpLength);
			case SSH_FXP_HANDLE:
				return new FxpHandle(buffer, fxpLength);
			case SSH_FXP_DATA:
			case SSH_FXP_NAME:
			case SSH_FXP_ATTRS:
			case SSH_FXP_EXTENDED:
			case SSH_FXP_EXTENDED_REPLY:
			default:
				logger.trace("What kind of Freighter carries {}?", fxpMessageType);
				return new NullFreighter(buffer);
		}
	}

	/**
	 * unused
	 * <p/>
	 * @param buf
	 * @param offset
	 * @param length
	 * @return
	 * @throws IOException
	 */
	private int fillXX(byte[] buf, int offset, int length) throws IOException {
		int i = 0;
		int foo = offset;
		while (length > 0) {
			readBuffer.get(buf, offset, length);
			offset += i;
			length -= i;
		}
		return offset - foo;
	}
	/*
	 private void skip(long foo) throws IOException {
	 while (foo > 0) {
	 long bar = io_in.skip(foo);
	 if (bar <= 0)
	 break;
	 foo -= bar;
	 }
	 }
	 */

	public int getFxpLength() {
		return fxpLength;
	}

	public void setFxpLength(int fxpLength) {
		this.fxpLength = fxpLength;
	}

	public int getFxpType() {
		return fxpType;
	}

	public void setFxpType(int fxpType) {
		this.fxpType = fxpType;
	}

	public int getRequestId() {
		return requestId;
	}

	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	public int getRecipientChannel() {
		return recipientChannel;
	}

	public void setRecipientChannel(int recipientChannel) {
		this.recipientChannel = recipientChannel;
	}

	@Override
	public String toString() {
		return "Packet: {" + "FxpMessageType: " + fxpMessageType + '}';
	}
}
