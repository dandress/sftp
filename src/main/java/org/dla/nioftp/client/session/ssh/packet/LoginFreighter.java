package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 6, 2013 12:48:01 PM
 */
public class LoginFreighter extends BaseFreighter implements Freight {

	private String username = null;
	private String password = null;

	public LoginFreighter(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
		assert username != null;
		assert password != null;
		buffer.putString(stringToByte(username));
		buffer.putString(stringToByte("ssh-connection"));
		buffer.putString(stringToByte("password"));
		buffer.putByte((byte) 0);
		buffer.putString(stringToByte(password));
	}

	@Override
	public void unloadFreight() throws PacketException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
