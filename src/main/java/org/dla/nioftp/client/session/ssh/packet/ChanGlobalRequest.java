package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:09:36 AM
 */
public class ChanGlobalRequest extends BaseFreighter implements Freight{

	private String requestName;
	private boolean wantReply;
	private String data = null;

	public ChanGlobalRequest (final Buffer buffer) {
		super(buffer);
	}


	@Override
	public void loadFreight() throws PacketException {
		buffer.putString(stringToByte(requestName));
		buffer.putByte( (byte)(wantReply == true ? 1 : 0 ));
		if (data != null)
			buffer.putString(stringToByte(data));
	}

	@Override
	public void unloadFreight() throws PacketException {
		requestName = byteToString(buffer.getString());
		wantReply = buffer.getByte() == 1 ? true : false;
	}

	public String getRequestName() {
		return requestName;
	}

	public void setRequestName(String requestName) {
		this.requestName = requestName;
	}

	public boolean isWantReply() {
		return wantReply;
	}

	public void setWantReply(boolean wantRepy) {
		this.wantReply = wantRepy;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
