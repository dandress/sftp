package org.dla.nioftp.client.session.ssh.packet;

import org.dla.nioftp.client.session.ssh.cyphony.PacketAlgorithms;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 10, 2013 3:49:17 PM
 */
public interface Packet {

	byte[] encode(final PacketAlgorithms packetExchange, final int out_sequence_no) throws PacketException;

	byte[] encode() throws PacketException;

	Freight getFreighter();

	MessageType getMsgType();

	void clear();

	boolean hasRemaining();

	void init(final int begin, int in_sequence_no) throws PacketException, FxpPacketException;

	int getPayloadLength();

	int getDestinationChannel();
}
