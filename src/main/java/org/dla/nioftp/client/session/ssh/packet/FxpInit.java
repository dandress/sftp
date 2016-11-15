package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 12, 2013 8:28:35 AM
 */
public class FxpInit extends BaseFreighter implements Freight  {
	private int version;
	private final  int fxpLength;

	public FxpInit (final Buffer buffer, final int fxpLength) {
		super(buffer);
		this.fxpLength = fxpLength;
	}

	@Override
	public void loadFreight() throws PacketException {
		buffer.putInt(version);
	}

	@Override
	public void unloadFreight() throws PacketException {
		version = buffer.getInt();
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}

