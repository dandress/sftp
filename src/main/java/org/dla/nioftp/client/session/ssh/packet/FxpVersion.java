package org.dla.nioftp.client.session.ssh.packet;

import java.util.ArrayList;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 14, 2013 9:34:31 AM
 */
public class FxpVersion extends BaseFreighter implements Freight {
	private int version;

	public FxpVersion(Buffer buffer, final int fxpLength) {
		super(buffer);
		this.fxpLength = fxpLength;
		extensionPairs = new ArrayList<ExtensionPair>();
	}

	@Override
	public void loadFreight() throws PacketException {
		buffer.putInt(version);
	}

	@Override
	public void unloadFreight() throws PacketException {
		version = buffer.getInt();
		extensionPairs = readExtensionPairs(fxpLength);
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}



}
