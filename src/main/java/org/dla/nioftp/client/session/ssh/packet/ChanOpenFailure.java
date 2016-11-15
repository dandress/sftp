package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 11, 2013 9:12:39 AM
 */
public class ChanOpenFailure  extends BaseFreighter implements Freight {

	private int recipientChannel;
	private int reasonCode;
	private String message;


	public ChanOpenFailure (final Buffer buffer) {
		super(buffer);
	}


	@Override
	public void loadFreight() throws PacketException {
	}

	@Override
	public void unloadFreight() throws PacketException {
		recipientChannel = buffer.getInt();
		reasonCode = buffer.getInt();
		message  = byteToString(buffer.getString());
	}

	public int getRecipientChannel() {
		return recipientChannel;
	}

	public void setRecipientChannel(int recipientChannel) {
		this.recipientChannel = recipientChannel;
	}

	public int getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(int reasonCode) {
		this.reasonCode = reasonCode;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
