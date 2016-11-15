package org.dla.nioftp.client.session.ssh.packet;

/**
 * Works like most any other buffer in that calling any 'putXXX' method
 * increments 'index.' And calling any 'getXXX' method increments 'offset.'
 * In practice this means that access must be done sequentially - data must
 * be put into, and read from, the buffer in order.
 *
 * @author Dennis Andress
 *
 *  Jan 23, 2013 7:32:38 AM
 */
public class Buffer {

	final byte[] temp = new byte[4];
	final byte[] buffer;
	int index;
	int offset;

	Buffer(final int size) {
		buffer = new byte[size];
		index = 0;
		offset = 0;
	}

	Buffer(byte[] buffer) {
		this.buffer = buffer;
		index = 0;
		offset = 0;
	}

	Buffer() {
		this(1024 * 10 * 2);
	}

	void putByte(byte foo) {
		buffer[index++] = foo;
	}

	void putByte(byte[] foo) throws ArrayIndexOutOfBoundsException {
		putByte(foo, 0, foo.length);
	}

	void putByte(byte[] foo, final int begin, final int length) throws ArrayIndexOutOfBoundsException {
		try {
			System.arraycopy(foo, begin, buffer, index, length);
			index += length;
		} catch (ArrayIndexOutOfBoundsException ex) {
			StringBuilder sb = new StringBuilder("Destination array size: ");
			sb.append(buffer.length).append("index: ").append(index).append(" length: ").append(length);
			throw new ArrayIndexOutOfBoundsException(sb.toString());
		}
	}

	void putString(byte[] foo) throws ArrayIndexOutOfBoundsException {
		putString(foo, 0, foo.length);
	}

	void putString(byte[] foo, final int begin, final int length) throws ArrayIndexOutOfBoundsException {
		putInt(length);
		putByte(foo, begin, length);
	}

	void putInt(final int val) {
		temp[0] = (byte) (val >>> 24);
		temp[1] = (byte) (val >>> 16);
		temp[2] = (byte) (val >>> 8);
		temp[3] = (byte) (val);
		System.arraycopy(temp, 0, buffer, index, 4);
		index += 4;
	}

	void putLong(final long val) {
		temp[0] = (byte) (val >>> 56);
		temp[1] = (byte) (val >>> 48);
		temp[2] = (byte) (val >>> 40);
		temp[3] = (byte) (val >>> 36);
		System.arraycopy(temp, 0, buffer, index, 4);

		temp[0] = (byte) (val >>> 24);
		temp[1] = (byte) (val >>> 16);
		temp[2] = (byte) (val >>> 8);
		temp[3] = (byte) (val);
		System.arraycopy(temp, 0, buffer, index + 4, 4);
		index += 8;
	}

	void skip(final int n) {
		index += n;
	}

	void putPad(int n) {
		while (n > 0) {
			buffer[index++] = (byte) 0;
			n--;
		}
	}

	void putMPInt(byte[] foo) {
		int i = foo.length;
		if ((foo[0] & 0x80) != 0) {
			i++;
			putInt(i);
			putByte((byte) 0);
		} else {
			putInt(i);
		}
		putByte(foo);
	}

	int getLength() {
		return index - offset;
	}

	int getOffset() {
		return offset;
	}

	void setOffset(final int s) {
		offset = s;
	}

	long getLong() {
		long foo = getInt() & 0xffffffffL;
		foo = (foo << 32) | (getInt() & 0xffffffffL);
		return foo;
	}

	int getInt() {
		int foo = getShort();
		foo = ((foo << 16) & 0xffff0000) | (getShort() & 0xffff);
		return foo;
	}

	long getUint() {
		long foo = 0L;
		long bar = 0L;
		foo = getByte();
		foo = ((foo << 8) & 0xff00) | (getByte() & 0xff);
		bar = getByte();
		bar = ((bar << 8) & 0xff00) | (getByte() & 0xff);
		foo = ((foo << 16) & 0xffff0000) | (bar & 0xffff);
		return foo;
	}

	int getShort() {
		int foo = getByte();
		foo = ((foo << 8) & 0xff00) | (getByte() & 0xff);
		return foo;
	}

	int getByte() {
		if (offset < buffer.length)
			return (buffer[offset++] & 0xff);
		return 0;
	}

	void getByte(byte[] foo) {
		getByte(foo, 0, foo.length);
	}

	void getByte(byte[] foo, final int start, final int length) {
		try {
			System.arraycopy(buffer, offset, foo, start, length);
			offset += length;
		} catch (ArrayIndexOutOfBoundsException ex) {
			StringBuilder sb = new StringBuilder("Destination array size: ");
			sb.append(buffer.length).append("index: ").append(index).append(" length: ").append(length);
			throw new ArrayIndexOutOfBoundsException(sb.toString());
		}
	}

	private int getByte(final int length) {
		int foo = offset;
		offset += length;
		return foo;
	}

	byte[] getMPInt() throws PacketException {
		final int i = getInt();	// uint32
		if (i < 0 || i > 8 * 1024) {
			throw new PacketException("Length bytes were invalid");
		}
		byte[] foo = new byte[i];
		getByte(foo, 0, i);
		return foo;
	}

	byte[] getMPIntBits() {
		final int bits = getInt();
		final int bytes = (bits + 7) / 8;
		byte[] foo = new byte[bytes];
		getByte(foo, 0, bytes);
		if ((foo[0] & 0x80) != 0) {
			byte[] bar = new byte[foo.length + 1];
			bar[0] = 0;
			System.arraycopy(foo, 0, bar, 1, foo.length);
			foo = bar;
		}
		return foo;
	}

	byte[] getString() throws PacketException {
		final int i = getInt();
		if (i < 0 || i > 256 * 1024) {
			throw new PacketException("String length bytes were invalid");
		}
		byte[] foo = new byte[i];
		getByte(foo, 0, i);
		return foo;
	}

	byte[] getString(int[] start, int[] length) {
		int i = getInt();
		start[0] = getByte(i);
		length[0] = i;
		return buffer;
	}

	void reset() {
		index = 0;
		offset = 0;
	}

	void shift() {
		if (offset == 0)
			return;

		System.arraycopy(buffer, offset, buffer, 0, index - offset);
		index = index - offset;
		offset = 0;
	}

	void rewind() {
		offset = 0;
	}

	byte getCommand() {
		return buffer[5];
	}

//	void checkFreeSize(final int n) {
//		if (buffer.length < index + n) {
//			byte[] tmp = new byte[buffer.length * 2];
//			System.arraycopy(buffer, 0, tmp, 0, index);
//			buffer = tmp;
//		}
//	}

	byte[][] getBytes(final int n, final String msg) throws PacketException {
		byte[][] tmp = new byte[n][];
		for (int i = 0; i < n; i++) {
			int j = getInt();
			if (getLength() < j)
				throw new PacketException(msg);
			tmp[i] = new byte[j];
			getByte(tmp[i]);
		}
		return tmp;
	}

	static Buffer fromBytes(byte[][] args) {
		int length = args.length + 4;
		for (int i = 0; i < args.length; i++) {
			length += args[i].length;
		}

		Buffer buf = new Buffer(length);
		for (int i = 0; i < args.length; i++) {
			buf.putString(args[i]);
		}

		return buf;
	}

	public byte[] getBuffer() {
		return buffer;
	}
}
