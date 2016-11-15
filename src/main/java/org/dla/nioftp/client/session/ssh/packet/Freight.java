package org.dla.nioftp.client.session.ssh.packet;

/**  Objects which read and write the data section of SSH_TRANS protocol packets.
 * These classes move the "Freight."
 *
 * @author Dennis Andress
 *
 * 
 * Jan 29, 2013 7:22:40 AM
 */
public interface Freight {

	/** Build the data - freight - for whatever Packet is needed
	 */
	public void loadFreight() throws PacketException;

	/** Read the data of and incoming Packet and do something with it...
	 *
	 * @throws PacketException
	 */
	public void unloadFreight()  throws PacketException;

	public byte[] getFreight() throws PacketException;

	public String getMessage();
}
