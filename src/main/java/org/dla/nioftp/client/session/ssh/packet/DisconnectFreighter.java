package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 1, 2013 10:27:39 AM
 */
public class DisconnectFreighter extends BaseFreighter implements Freight {
	private int reasonCode;
	private String description;

	public DisconnectFreighter(final Buffer buffer){
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
		buffer.putInt(reasonCode);
		buffer.putString(stringToByte(description));
	}

	@Override
	public void unloadFreight() throws PacketException {
		reasonCode = buffer.getInt();
		description = byteToString(buffer.getString());
	}

	public int getReasonCode() {
		return reasonCode;
	}

	public String getDescription() {
		return description;
	}

	public void setReasonCode(int reasonCode) {
		this.reasonCode = reasonCode;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
