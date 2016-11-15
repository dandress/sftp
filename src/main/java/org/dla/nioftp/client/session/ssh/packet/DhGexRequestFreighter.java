/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 *

 *
 * @author Dennis Andress
 *
 *  Jan 29, 2013 1:33:01 PM
 */
public class DhGexRequestFreighter extends BaseFreighter implements Freight {

	public DhGexRequestFreighter(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
		buffer.putInt(1024);
		buffer.putInt(1024);
		buffer.putInt(1024);
	}

	@Override
	public void unloadFreight() throws PacketException {
	}

}
