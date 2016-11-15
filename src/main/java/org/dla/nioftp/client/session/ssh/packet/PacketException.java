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
 * Jan 25, 2013 7:06:01 AM
 */
public class PacketException extends  Exception {
	static final long serialVersionUID = 1l;

	public PacketException(final String msg) {
		super(msg);
	}

	public PacketException(final String msg, Throwable cause) {
		super(msg, cause);
	}

	public PacketException(Throwable cause) {
		super(cause);
	}

	public PacketException() {
		super();
	}


}
