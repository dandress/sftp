package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:11:36 AM
 */
public class ChanOpen  extends BaseFreighter implements Freight {
	private String channelType;
	private int senderId;  // our channelId
	private int initialWindowSize;
	private int maxPacketSize;


	public ChanOpen (final Buffer buffer) {
		super(buffer);
	}


	@Override
	public void loadFreight() throws PacketException {
		buffer.putString(stringToByte(channelType));
		buffer.putInt(senderId);
		buffer.putInt(initialWindowSize);
		buffer.putInt(maxPacketSize);
	}

	@Override
	public void unloadFreight() throws PacketException {
	}

	public String getChannelType() {
		return channelType;
	}

	public void setChannelType(String channelType) {
		this.channelType = channelType;
	}

	public int getSenderId() {
		return senderId;
	}

	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}

	public int getInitialWindowSize() {
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
