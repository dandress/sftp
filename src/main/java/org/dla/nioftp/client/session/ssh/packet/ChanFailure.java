package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:15:40 AM
 */
public class ChanFailure extends BaseFreighter implements Freight  {
	private int recipientChannel;

	public ChanFailure (final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
		buffer.putInt(recipientChannel);
	}

	@Override
	public void unloadFreight() throws PacketException {
		recipientChannel = buffer.getInt();
	}

	public int getRecipientChannel() {
		return recipientChannel;
	}

	public void setRecipientChannel(int recipientChannel) {
		this.recipientChannel = recipientChannel;
	}

}
