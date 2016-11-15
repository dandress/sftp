package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:13:52 AM
 */
public class ChanData extends BaseFreighter implements Freight  {
	private int recipientChannel;
	private String data;

	public ChanData (final Buffer buffer) {
		super(buffer);
	}


	@Override
	public void loadFreight() throws PacketException {
		buffer.putInt(recipientChannel);
		buffer.putString(stringToByte(data));
	}

	@Override
	public void unloadFreight() throws PacketException {
		recipientChannel = buffer.getInt();
		data = byteToString(buffer.getString());
	}

	public int getRecipientChannel() {
		return recipientChannel;
	}

	public void setRecipientChannel(int recipientChannel) {
		this.recipientChannel = recipientChannel;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
