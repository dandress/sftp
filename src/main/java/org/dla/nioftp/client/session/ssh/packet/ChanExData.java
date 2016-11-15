package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:14:13 AM
 */
public class ChanExData extends BaseFreighter implements Freight  {
	private int recipientChannel;
	/** There's only one dataType defined - 1. For std error */
	private int dataType;
	private String data;

	public ChanExData (final Buffer buffer) {
		super(buffer);
	}


	@Override
	public void loadFreight() throws PacketException {
		buffer.putInt(recipientChannel);
		buffer.putInt(dataType);
		buffer.putString(stringToByte(data));
	}

	@Override
	public void unloadFreight() throws PacketException {
		recipientChannel = buffer.getInt();
		dataType = buffer.getInt();
		data = byteToString(buffer.getString());
	}

	public int getRecipientChannel() {
		return recipientChannel;
	}

	public void setRecipientChannel(int recipientChannel) {
		this.recipientChannel = recipientChannel;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
