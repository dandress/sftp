package org.dla.nioftp.client.session.ssh.packet;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Methods common to all Freighters
 *
 * @author Dennis Andress
 *
 *  Jan 29, 2013 7:59:47 AM
 */
abstract public class BaseFreighter {

	protected Buffer buffer;
	protected final String charsetName = "UTF-8";

	BaseFreighter(final Buffer buffer) {
		this.buffer = buffer;
	}

	protected void putString(final String data) throws PacketException {
		assert buffer != null;
		try {
			final byte[] bytes = data.getBytes("UTF-8");
			buffer.putString(bytes);
		} catch (UnsupportedEncodingException ex) {
			throw new PacketException(ex);
		}
	}

	protected String byteToString(final byte[] bytes) throws PacketException {
		assert buffer != null;
		try {
			return new String(bytes, 0, bytes.length, charsetName);
		} catch (UnsupportedEncodingException ex) {
			throw new PacketException(ex);
		}
	}

	protected byte[] stringToByte(final String string) {
		if (string == null)
			return null;
		try {
			return string.getBytes(charsetName);
		} catch (java.io.UnsupportedEncodingException e) {
			return string.getBytes();
		}
	}

	protected String getString() throws PacketException {
		assert buffer != null;
		try {
			final byte[] bytes = buffer.getString();
			return new String(bytes, 0, bytes.length, charsetName);
		} catch (UnsupportedEncodingException ex) {
			throw new PacketException(ex);
		}
	}

	protected void fill(final int len) throws PacketException {
		fill(buffer.buffer, buffer.index, len);
	}

	protected synchronized void fill(final byte[] foo, final int start, final int len) throws PacketException {
		SecureRandom secureRandom = null;
		try {
			secureRandom = SecureRandom.getInstance("SHA1PRNG");
			secureRandom.setSeed(3754345345352345234L);
			byte[] tmp = new byte[len];
			secureRandom.nextBytes(tmp);
			System.arraycopy(tmp, 0, foo, start, len);
		} catch (NoSuchAlgorithmException ex) {
			throw new PacketException(ex);
		}
	}

	public synchronized byte[] getFreight() throws PacketException {
		buffer.reset();		// start with index and offset at 0
		int length = buffer.getInt(); // 4 bytes
		int pad = (int) buffer.getByte(); // 1 byte
		byte[] tmp = new byte[length - 1 - pad]; // the size of everything from byte[5] to the start of the padding
		System.arraycopy(buffer.buffer, 5, tmp, 0, tmp.length); // copy from byte[5] to where padding starts
		return tmp;
	}

	/**
	 * GenericFreighter has one getter, getMessage(). Rather then cast to GenericFreighter I took the lazy approach
	 * and put getMessage() in the Interface. It's here so it doesn't have to be implemented everywhere.
	 * <p/>
	 * @return
	 */
	public String getMessage() {
		return null;
	}

	protected class ExtensionPair {

		String extensionName;
		String extensionData;
	}
	protected ArrayList<ExtensionPair> extensionPairs = null;
	protected int fxpLength;
	protected Iterator<ExtensionPair> pairIterator = null;

	/**
	 * Used by FXP_MESSAGES
	 * <p/>
	 * @return
	 */
	protected ArrayList<ExtensionPair> readExtensionPairs(final int fxpLength) throws PacketException {
		ArrayList<ExtensionPair> pairs = new ArrayList<ExtensionPair>();
		while (buffer.offset < fxpLength) {
			ExtensionPair pair = new ExtensionPair();
			pair.extensionName = byteToString(buffer.getString());
			pair.extensionData = byteToString(buffer.getString());
			pairs.add(pair);
		}
		return pairs;
	}

	public String[] getExtensionPairs() {
		if (extensionPairs == null)
			return null;
		if (pairIterator == null)
			pairIterator = extensionPairs.iterator();

		String[] rslt = new String[2];
		if (pairIterator.hasNext()) {
			ExtensionPair pair = pairIterator.next();
			rslt[0] = pair.extensionName;
			rslt[1] = pair.extensionData;
			return rslt;
		}
		pairIterator = null;
		return null;
	}
	/*
	 public int shift(final int len, final int bsize, final int mac) {
	 int shift = len + 5 + 9;
	 int pad = (-shift) & (bsize - 1);
	 if (pad < bsize)
	 pad += bsize;
	 shift += pad;
	 shift += mac;
	 shift += 32; // margin for deflater which may inflate data
	 if (buffer.buffer.length < shift + buffer.index - 5 - 9 - len) {
	 byte[] foo = new byte[shift + buffer.index - 5 - 9 - len];
	 System.arraycopy(buffer.buffer, 0, foo, 0, buffer.buffer.length);
	 buffer.buffer = foo;
	 }

	 System.arraycopy(buffer.buffer, len + 5 + 9, buffer.buffer, shift, buffer.index - 5 - 9);
	 buffer.index = 10;
	 buffer.putInt(len);
	 buffer.index = len + 5 + 9;
	 return shift;
	 }

	 public void unshift(final byte command, final int receipient, final int s, final int len) {
	 System.arraycopy(buffer.buffer, s, buffer.buffer, 5 + 9, len);
	 buffer.buffer[5] = command;
	 buffer.index = 6;
	 buffer.putInt(receipient);
	 buffer.putInt(len);
	 buffer.index = len + 5 + 9;
	 }

	 public void reset() {
	 buffer.index = 5;
	 }
	 */
}
