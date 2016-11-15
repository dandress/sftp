package org.dla.nioftp.client.session.ssh.packet;

/**
 * From: http://tools.ietf.org/html/rfc4254#page-13
 *
 * byte SSH_MSG_CHANNEL_REQUEST
 * uint32 recipient channel
 * string "subsystem"
 * boolean want reply
 * string subsystem name
 *
 * <p/>
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:15:07 AM
 */
public class ChanRequest extends BaseFreighter implements Freight {

	private int recipientChannel;
	private String requestType;
	private boolean wantReply;
	private String data = null;

	public ChanRequest(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
		buffer.putInt(recipientChannel);
		buffer.putString(stringToByte(requestType));
		buffer.putByte((byte) (wantReply == true ? 1 : 0));
		if (data != null)
			buffer.putString(stringToByte(data));
	}

	@Override
	public void unloadFreight() throws PacketException {
		recipientChannel = buffer.getInt();
		requestType = byteToString(buffer.getString());
		wantReply = buffer.getByte() == 1 ? true : false;
	}

	public int getRecipientChannel() {
		return recipientChannel;
	}

	public void setRecipientChannel(int recipientChannel) {
		this.recipientChannel = recipientChannel;
	}

	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public boolean isWantReply() {
		return wantReply;
	}

	public void setWantReply(boolean wantReply) {
		this.wantReply = wantReply;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
