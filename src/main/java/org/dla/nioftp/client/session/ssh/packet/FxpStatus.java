package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 14, 2013 1:46:22 PM
 */
public class FxpStatus extends BaseFreighter implements Freight {
	private int errorCode;
	private String errorMsg;


	public FxpStatus(Buffer buffer, int fxpLength) {
		super(buffer);
		this.fxpLength = fxpLength;
	}

	@Override
	public void loadFreight() throws PacketException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void unloadFreight() throws PacketException {
		errorCode = buffer.getInt();
		errorMsg = byteToString(buffer.getString());
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

}
