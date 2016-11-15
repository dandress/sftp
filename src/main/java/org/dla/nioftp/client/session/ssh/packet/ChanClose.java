package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:14:50 AM
 */
public class ChanClose extends BaseFreighter implements Freight {

	private int recipientChannel;

	public ChanClose(final Buffer buffer) {
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

	public void setRecipientChannel(int recipientChannel) {
		this.recipientChannel = recipientChannel;
	}
}
