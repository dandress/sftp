package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 8, 2013 12:03:05 PM
 */
public class ServiceRequestFreighter extends BaseFreighter implements Freight {
	private String serviceName;

	public ServiceRequestFreighter(final Buffer buffer) {
		super(buffer);
	}

	@Override
	public void loadFreight() throws PacketException {
		buffer.putString(serviceName.getBytes());
	}

	@Override
	public void unloadFreight() throws PacketException {
	}

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

}
