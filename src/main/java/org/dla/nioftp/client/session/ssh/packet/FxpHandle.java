/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 14, 2013 1:17:40 PM
 */
public class FxpHandle extends BaseFreighter implements Freight {
	private  byte[] handle ;

	public FxpHandle(Buffer buffer, int fxpLength) {
		super(buffer);
		this.fxpLength = fxpLength;
	}

	@Override
	public void loadFreight() throws PacketException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void unloadFreight() throws PacketException {
		handle = new byte[buffer.getInt()];
		buffer.getByte(handle);
	}

	public byte[] getHandle() {
		return handle;
	}


}
