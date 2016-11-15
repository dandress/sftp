package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:13:33 AM
 */
public class ChanWindowAdjust  extends BaseFreighter implements Freight {
	private int recipientChannel;	// our number
	private int bytesToAdd;


	public ChanWindowAdjust (final Buffer buffer) {
		super(buffer);
	}


	@Override
	public void loadFreight() throws PacketException {
		buffer.putInt(recipientChannel);
		buffer.putInt(bytesToAdd);
	}

	@Override
	public void unloadFreight() throws PacketException {
		recipientChannel = buffer.getInt();
		bytesToAdd = buffer.getInt();
	}

	public int getRecipientChannel() {
		return recipientChannel;
	}

	public void setRecipientChannel(int recipientChannel) {
		this.recipientChannel = recipientChannel;
	}

	public int getBytesToAdd() {
		return bytesToAdd;
	}

	public void setBytesToAdd(int bytesToAdd) {
		this.bytesToAdd = bytesToAdd;
	}

}
