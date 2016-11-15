package org.dla.nioftp.client;

import java.nio.ByteBuffer;
import org.dla.nioftp.client.session.channel.SftpChannel;
import org.dla.nioftp.client.session.ssh.cyphony.ICipher;
import org.dla.nioftp.client.session.ssh.cyphony.IMac;
import org.dla.nioftp.client.session.ssh.cyphony.PacketAlgorithms;
import org.dla.nioftp.client.session.ssh.packet.FxpPacket;
import org.dla.nioftp.client.session.ssh.packet.FxpPacketException;
import org.dla.nioftp.client.session.ssh.packet.KexPacket;
import org.dla.nioftp.client.session.ssh.packet.MessageType;
import org.dla.nioftp.client.session.ssh.packet.Packet;
import org.dla.nioftp.client.session.ssh.packet.PacketException;
import org.dla.nioftp.client.session.ssh.packet.SshPacket;
import org.dla.threads.ThreadSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 * nioftp
 * Jun 7, 2013 9:14:00 AM
 */
public class BufferReader {
	private static final Logger logger = LoggerFactory.getLogger(BufferReader.class);
	public static final String DEFAULT_CHARSET_NAME = "US-ASCII";
	private IncomingQueue incomingQueue;
	private int in_sequence_no = 0;
	private Client client;

	public BufferReader(final Client client) {
		this.client = client;
	}

	ByteBuffer decrypt(ByteBuffer readBuffer) throws PacketException {
		final PacketAlgorithms packetAlgos = client.getPacketAlgos();
		if (packetAlgos != null) {
			final int PACKET_MAX_SIZE = 256 * 1024;
			try {
				ICipher cipher = packetAlgos.getS2ccipher();
				IMac mac = packetAlgos.getS2cmac();
				int cipher_size = cipher.getIVSize();
				final int packetSize = readBuffer.limit();
				byte[] packet = new byte[packetSize];
				readBuffer.get(packet, 0, cipher_size);
				cipher.update(packet, 0, cipher_size, packet, 0);

				int need = ((packet[0] << 24) & 0xFF000000)
						| ((packet[1] << 16) & 0x00FF0000)
						| ((packet[2]) << 8 & 0x0000FF00)
						| ((packet[3]) & 0x000000FF);

				need += 4;
				need -= cipher_size;

				if (need < 5 || need > PACKET_MAX_SIZE) {
					logger.warn("Packet size {} is unuseable", need);
					return null;
				}
				if (need > packet.length) {
					logger.warn("-------   !!  Need a bigger array!!  ");
					return null;
				}

				if (need % cipher_size != 0)
					throw new PacketException("Bad packet length");

				if (need > 0) {
					readBuffer.get(packet, cipher_size, need);
					cipher.update(packet, cipher_size, need, packet, cipher_size);
				}

				mac.update(in_sequence_no);
				mac.update(packet, 0, need + cipher_size);
				byte[] ourMac = mac.doFinal();

				byte[] packetMac = new byte[mac.getBlockSize()];
				if (readBuffer.remaining() >= mac.getBlockSize()) {
					readBuffer.get(packetMac, 0, mac.getBlockSize());
					boolean matched = java.util.Arrays.equals(packetMac, ourMac);
					if (!matched)
						logger.warn("Packet's Mac did NOT verify");
					else
						logger.trace("Packet's Mac is verified");
				}
				logger.trace("readBuffer position: {}, limit: {}, remaining: {}", readBuffer.position(), readBuffer.limit(), readBuffer.remaining());
				return ByteBuffer.wrap(packet, 0, need + cipher_size);
			} catch (Exception ex) {
				throw new PacketException(ex.getMessage(), ex);
			}
		}
		return null;
	}

	void readIncommingBuffer(ByteBuffer readBuffer, ByteBuffer tmpReadBuffer) {
		logger.trace("reading incomming buffer");
		try {
			// get the 5 byte as an int. This is the SSH command. Use it to decide what to do with the packet.
			int pcktCommand = tmpReadBuffer.get(5);
			if (pcktCommand > 79 && pcktCommand < 105) {
				boolean hasRemaing = true;
				while (hasRemaing) {
					try {
						Packet packet;
						if (pcktCommand == 94)
							packet = new FxpPacket(tmpReadBuffer);
						else
							packet = new SshPacket(tmpReadBuffer);
						packet.init(0, in_sequence_no);
						final SftpChannel channel = client.getSession().getChannels().get(packet.getDestinationChannel());
						if (channel != null) {
							channel.getChannelQueue().postPacket(packet);
						} else {
							if (packet.getMsgType() == MessageType.SSH_MSG_DISCONNECT)
								client.getSession().handlePacket(packet);
							if (packet.getMsgType() == MessageType.SSH_MSG_GLOBAL_REQUEST)
								client.getSession().handlePacket(packet);
							logger.trace("Session Message (A): {} msgType: {}", packet.getDestinationChannel(), packet.getMsgType());
						}
						hasRemaing = readBuffer.hasRemaining();
						in_sequence_no++;
						if (hasRemaing) {
							tmpReadBuffer = decrypt(readBuffer);
							if (tmpReadBuffer == null) {
								logger.warn("Bad Packet at (A)");
								hasRemaing = false;
								return;
							}
							pcktCommand = tmpReadBuffer.get(5);
						}
					} catch (FxpPacketException ex) {  // Assumes that the packet wasn't an FXpPacket and needs to be handled as an SshPacket....
						tmpReadBuffer.rewind();
						Packet packet;
						if (pcktCommand == 94)
							packet = new FxpPacket(tmpReadBuffer);
						else
							packet = new SshPacket(tmpReadBuffer);
						packet.init(0, in_sequence_no);
						final SftpChannel channel = client.getSession().getChannels().get(packet.getDestinationChannel());
						if (channel != null) {
							channel.getChannelQueue().postPacket(packet);
						} else {
							if (packet.getMsgType() == MessageType.SSH_MSG_DISCONNECT)
								client.getSession().handlePacket(packet);
							if (packet.getMsgType() == MessageType.SSH_MSG_GLOBAL_REQUEST)
								client.getSession().handlePacket(packet);
							logger.trace("Session Message  (B): {} msgType: {}", packet.getDestinationChannel(), packet.getMsgType());
						}
						hasRemaing = readBuffer.hasRemaining();
						in_sequence_no++;
						if (hasRemaing) {
							tmpReadBuffer = decrypt(readBuffer);
							if (tmpReadBuffer == null) {
								logger.warn("Bad Packet at (B)");
								hasRemaing = false;
								return;
							}
							pcktCommand = tmpReadBuffer.get(5);
						}
					}
				}
			} else if (pcktCommand < 62) {
				boolean hasRemaing = true;
				int begin = 0;
				while (hasRemaing) {
					final Packet packet = new KexPacket(tmpReadBuffer);
					packet.init(begin, in_sequence_no);
					incomingQueue.postPacket(packet);
					hasRemaing = packet.hasRemaining();	// This happens when unencrypted binData has more then one packet in. SSH_NEWKEYS is the most likely culprit
					begin = packet.getPayloadLength() + 4;
					in_sequence_no++;
					// this is to handle encrypted packets that have not yet been read from readIncommingBuffer
					if (readBuffer.hasRemaining() && client.getPacketAlgos() != null) {
						tmpReadBuffer = decrypt(readBuffer);
						if (tmpReadBuffer == null) {
							logger.warn("Bad Packet at pcktCommand < 62");
							client.getSession().shutdownNow();
							return;
						}
						begin = 0;
						hasRemaing = true;
					}
				}
			}
		} catch (PacketException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (FxpPacketException ex) {
			// DDN (Don't Do Nothing). This will only be thrown by an FxpPacket, and there is a catch block just for it...
		}
	}

	void startIncomingQueue() {
		incomingQueue = new IncomingQueue(client.getSession());
		ThreadSource.getInstance().executeThread(incomingQueue, "incomingQueue", Thread.MAX_PRIORITY);
	}

	void shutdown() {
		if (incomingQueue != null)
			incomingQueue.abort();
	}
}
