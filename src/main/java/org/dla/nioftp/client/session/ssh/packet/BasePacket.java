package org.dla.nioftp.client.session.ssh.packet;

import org.dla.nioftp.Constants;
import org.dla.nioftp.client.session.ssh.cyphony.ICipher;
import org.dla.nioftp.client.session.ssh.cyphony.IMac;
import org.dla.nioftp.client.session.ssh.cyphony.PacketAlgorithms;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 10, 2013 3:50:56 PM
 */
public abstract class BasePacket implements Packet {

	private static final Logger logger = LoggerFactory.getLogger(BasePacket.class);
	protected Buffer buffer;
	protected boolean hasRemaining;
	protected MessageType msgType;
	protected int packetType = -1;
	protected int padLength = -1;
	protected int payloadLength = -1;
	protected ByteBuffer readBuffer = null;
	protected int destinationChannel = -1;

	/**
	 * To be used for Kex and UserAuth
	 * <p/>
	 * @param msgType
	 */
	public BasePacket(final MessageType msgType) {
		this.buffer = new Buffer(Constants.capacity);
		this.msgType = msgType;
		packetType = msgType.getCode();
		buffer.index = 5;	// skip the first 5 bytes, they'll be set when padding(...) is called, from encode(...)
		buffer.putByte((byte) msgType.getCode());
		logger.trace("Creating packetType: {}", msgType);
	}

	/**
	 * Made just for FxpWrite type packets where being able to create a larger buffer would speed
	 * up file transfer
	 * <p/>
	 * @param msgType
	 * @param bufferSize
	 */
	public BasePacket(final MessageType msgType, final int bufferSize) {
		this.buffer = new Buffer(bufferSize);
		this.msgType = msgType;
		packetType = msgType.getCode();
		buffer.index = 5;	// skip the first 5 bytes, they'll be set when padding(...) is called, from encode(...)
		buffer.putByte((byte) msgType.getCode());
	}

	/**
	 * To be used for incoming data.
	 * <p/>
	 * @param readBuffer
	 */
	public BasePacket(final ByteBuffer readBuffer) {
		this.readBuffer = readBuffer.duplicate();
	}

	@Override
	abstract public Freight getFreighter();

	@Override
	public void init(final int begin, int in_sequence_no) throws PacketException, FxpPacketException {
		readBuffer.position(begin);
		payloadLength = readBuffer.getInt(); // read 4 bytes
		if (payloadLength > Constants.capacity || payloadLength < 0) {
			readBuffer = null;
			return;
		}

		padLength = (int) readBuffer.get(); // read 1 byte
		packetType = readBuffer.get(); // read 1 byte
		msgType = MessageType.fromInt(packetType);
		logger.trace("readBuffer contains a packet of {} bytes", payloadLength);

		this.buffer = new Buffer(payloadLength + 4);
		readBuffer.position(begin);
		readBuffer.get(buffer.buffer, 0, payloadLength + 4);

		hasRemaining = readBuffer.remaining() > 0;
		if (hasRemaining)
			logger.trace("readBuffer has {} bytes remaining", readBuffer.remaining());
		buffer.setOffset(6); // bytes 0-3 are an uint32 for packet length. byte 4 is pad length. byte 5 is the command.
		// byte 6 starts the payload, which is read in a Freighter


		// need recipientID, which is our channelId, so session.incomingQueue can decide which channel gets this packet..
		if (packetType > 90) {
			destinationChannel = ((buffer.buffer[6] << 24) & 0xFF000000)
					| ((buffer.buffer[7] << 16) & 0x00FF0000)
					| ((buffer.buffer[8]) << 8 & 0x0000FF00)
					| ((buffer.buffer[9]) & 0x000000FF);
		}
	}

	@Override
	public byte[] encode(final PacketAlgorithms packetExchange, final int out_sequence_no) throws PacketException {
		byte[] tmp = null;
		if (packetExchange == null)
			return null;
		try {
			logger.trace("Sending encoded msgType: {}", msgType);
			final ICipher cipher = packetExchange.getC2scipher();
			padding(cipher.getIVSize());
			final IMac mac = packetExchange.getC2smac();
			mac.update(out_sequence_no);
			mac.update(buffer.buffer, 0, buffer.index);
			mac.doFinal(buffer.buffer, buffer.index);
			// encrypt up to where the MAC starts
			byte[] buf = buffer.buffer;
			cipher.update(buf, 0, buffer.index, buf, 0);
			buffer.skip(mac.getBlockSize());
			final int length = buffer.index;
			tmp = new byte[length];
			System.arraycopy(buffer.buffer, 0, tmp, 0, length);
			clear(); // kind of experimental but,this Packet won't be used further so let's clean up a bit...
			return tmp;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw new PacketException(ex.getMessage(), ex);
		}

	}

	@Override
	public synchronized byte[] encode() throws PacketException {
		padding(8);
		byte[] tmp = null;
		final int length = buffer.index;
		tmp = new byte[length];
		System.arraycopy(buffer.buffer, 0, tmp, 0, length);
		clear(); // kind of experimental but,this Packet won't be used further so let's clean up a bit...
		return tmp;
	}

	protected synchronized void fill(final byte[] foo, final int start, final int len) throws PacketException {
		SecureRandom secureRandom = null;
		try {
			secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(3754345345352345234L);
			byte[] tmp = new byte[len];
			secureRandom.nextBytes(tmp);
			System.arraycopy(tmp, 0, foo, start, len);
		} catch (NoSuchAlgorithmException ex) {
			throw new PacketException(ex.getMessage(), ex);
		}
	}

	/**  Empties and sets {@code  readBuffer} to null
	 *
	 */
	@Override
	public void clear() {
		if (readBuffer != null) {
			readBuffer = null;
		}
		buffer = null;
		msgType = null;
	}

	@Override
	public MessageType getMsgType() {
		return msgType;
	}

	@Override
	public int getPayloadLength() {
		assert payloadLength != -1 : "Packet's length has not been set!";
		return payloadLength;
	}

	public int getTotalLength() {
		return payloadLength + padLength;
	}

	@Override
	public boolean hasRemaining() {
		return hasRemaining;
	}

	/**
	 * the padding field, which consists of pad len bytes of random data such that the length of the packet minus the
	 * MAC is a multiple of the cipher block size or 8, whichever is larger. The amount of padding must be between 4 and
	 * 255 bytes inclusive.
	 *
	 * Handy. Reads the Buffer's length, adds bsize bytes padding to the Buffer and then puts the new length of the
	 * Buffer in the first four bytes of the Buffer, right where it belongs.
	 *
	 * @param bsize Block size. 8 or whatever the cipher size is.
	 * @throws PacketException Happens when the call to {code fill(...) throws a NoSucAlgorithmException
	 */
	private void padding(final int bsize) throws PacketException {
		byte[] padding = new byte[4];
		payloadLength = buffer.index;
		padLength = (-payloadLength) & (bsize - 1);
		if (padLength < bsize)
			padLength += bsize;
		payloadLength = payloadLength + padLength - 4;
		padding[0] = (byte) (payloadLength >>> 24);
		padding[1] = (byte) (payloadLength >>> 16);
		padding[2] = (byte) (payloadLength >>> 8);
		padding[3] = (byte) (payloadLength);
		System.arraycopy(padding, 0, buffer.buffer, 0, 4);
		final int cmd = buffer.getCommand();
		logger.trace("Index: {}  payloadLength: {}  padLength: {}  command: {}", buffer.index, payloadLength, padLength, cmd);
		buffer.buffer[4] = (byte) padLength;
		fill(buffer.buffer, buffer.index, padLength);
		buffer.skip(padLength);
	}

	@Override
	public int getDestinationChannel() {
		return destinationChannel;
	}

	@Override
	public String toString() {
		return "MessageType: " + msgType + ", " + packetType;
	}
}
