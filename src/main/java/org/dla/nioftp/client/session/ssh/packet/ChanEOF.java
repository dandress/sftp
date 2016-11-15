package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:14:32 AM
 */
public class ChanEOF extends BaseFreighter implements Freight {

	private int recipientChannel;

	public ChanEOF(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
		recipientChannel = buffer.getInt();
	}

	@Override
	public void unloadFreight() throws PacketException {
		buffer.putInt(recipientChannel);
	}

	public int getRecipientChannel() {
		return recipientChannel;
	}

	public void setRecipientChannel(int recipientChannel) {
		this.recipientChannel = recipientChannel;
	}
}
