package org.dla.nioftp.client.session.ssh.cyphony;

/**
 *
 * @author Dennis Andress
 *
 *  Feb 4, 2013 2:00:35 PM
 */
class Buffer {

	final byte[] temp = new byte[4];
	byte[] buffer;
	int index;
	int offset;

	Buffer(final int size) {
		buffer = new byte[size];
		index = 0;
		offset = 0;
	}

	void putInt(final int val) {
		temp[0] = (byte) (val >>> 24);
		temp[1] = (byte) (val >>> 16);
		temp[2] = (byte) (val >>> 8);
		temp[3] = (byte) (val);
		System.arraycopy(temp, 0, buffer, index, 4);
		index += 4;
	}

	void putString(byte[] foo) {
		putString(foo, 0, foo.length);
	}
	void putByte(byte[] foo) {
		putByte(foo, 0, foo.length);
	}

	void putByte(byte[] foo, final int begin, final int length) {
		System.arraycopy(foo, begin, buffer, index, length);
		index += length;
	}

	void putString(byte[] foo, final int begin, final int length) {
		putInt(length);
		putByte(foo, begin, length);
	}

	void putByte(byte foo) {
		buffer[index++] = foo;
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
		putByte(foo, 0, foo.length);
	}

	int getLength() {
		return index - offset;
	}

	void getByte(byte[] foo, final int start, final int length) {
		System.arraycopy(buffer, offset, foo, start, length);
		offset += length;
	}
	void reset() {
		index = 0;
		offset = 0;
	}

}