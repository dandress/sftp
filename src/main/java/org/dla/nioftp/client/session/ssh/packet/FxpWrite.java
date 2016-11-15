package org.dla.nioftp.client.session.ssh.packet;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 14, 2013 2:14:11 PM
 */
public class FxpWrite extends BaseFreighter implements Freight {
	private byte[] handle;
	private long	offset;
	private byte[] data;

	private int	 dataStart = -1;
	private int	 dataLength = -1;

	public FxpWrite(Buffer buffer, int fxpLength) {
		super(buffer);
		this.fxpLength = fxpLength;
	}

	@Override
	public void loadFreight() throws PacketException {
		assert dataStart != -1;
		assert dataLength != -1;
		buffer.putString(handle);
		buffer.putLong(offset);
		buffer.putString(data, dataStart, dataLength);
	}

	@Override
	public void unloadFreight() throws PacketException {

	}


	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public byte[] getHandle() {
		return handle;
	}

	public void setHandle(byte[] handle) {
		this.handle = handle;
	}

	public int getDataStart() {
		return dataStart;
	}

	public void setDataStart(int dataStart) {
		this.dataStart = dataStart;
	}

	public int getDataLength() {
		return dataLength;
	}

	public void setDataLength(int dataLength) {
		this.dataLength = dataLength;
	}

}
