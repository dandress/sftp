package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:12:16 AM
 */
public class ChanOpenConfirm  extends BaseFreighter implements Freight {
	/** We are the recipient of this message, this is the number we sent as senderChannel!!
	 */
	private int recipientChannel;
	/** The server is the sender, this is his channelID. We'll use it as the recipient of our messages.! */
	private int senderChannel;
	private long initialWindowSize;
	private int maxPacketSize;

	public ChanOpenConfirm (final Buffer buffer) {
		super(buffer);
	}


	@Override
	public void loadFreight() throws PacketException {
	}

	@Override
	public void unloadFreight() throws PacketException {
		recipientChannel = buffer.getInt();		// our number
		senderChannel = buffer.getInt();	// server's number
		initialWindowSize = buffer.getInt();
		maxPacketSize = buffer.getInt();
	}

	public int getRecipientChannel() {
		return recipientChannel;
	}

	public void setRecipientChannel(int recipientChannel) {
		this.recipientChannel = recipientChannel;
	}

	public int getSenderChannel() {
		return senderChannel;
	}

	public void setSenderChannel(int senderChannel) {
		this.senderChannel = senderChannel;
	}

	public long getInitialWindowSize() {
		return initialWindowSize;
	}

	public void setInitialWindowSize(int initialWindowSize) {
		this.initialWindowSize = initialWindowSize;
	}

	public int getMaxPacketSize() {
		return maxPacketSize;
	}

	public void setMaxPacketSize(int maxPacketSize) {
		this.maxPacketSize = maxPacketSize;
	}

}
