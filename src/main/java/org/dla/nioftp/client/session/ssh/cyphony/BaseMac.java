package org.dla.nioftp.client.session.ssh.cyphony;

import org.dla.nioftp.client.session.ssh.packet.PacketException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 5, 2013 11:11:37 AM
 */
public class BaseMac implements IMac {

	private static final Logger logger = LoggerFactory.getLogger(BaseMac.class);
	final protected String name;
	final protected int blockSize;
	protected Mac mac;
	byte[] key = null;

	public BaseMac(final int blockSize, byte[] K, byte[] H, byte[] inKey, MessageDigest messagegDigest, final String algo, final String name) throws PacketException {
		this.blockSize = blockSize;
		this.name = name;
		this.key = expandKey(K, H, inKey, messagegDigest, blockSize);
		init(this.key, algo);
	}

	private byte[] expandKey(byte[] K, byte[] H, byte[] key, MessageDigest messageDigest, int required_length) {
		byte[] result = key;
		final int size = messageDigest.getDigestLength();
		while (result.length < required_length) {
			Buffer buf = new Buffer(10000);
			buf.putMPInt(K);
			buf.putByte(H);
			buf.putByte(result);
			messageDigest.update(buf.buffer, 0, buf.index);
			byte[] tmp = new byte[result.length + size];
			System.arraycopy(result, 0, tmp, 0, result.length);
			System.arraycopy(messageDigest.digest(), 0, tmp, result.length, size);
			// '0' reslut array
			for (int i = 0; i < result.length; i++) {
				result[i] = 0;
			}
			result = tmp;
		}
		return result;
	}

	private void init(byte[] key, final String algo) throws PacketException {
		try {
			if (key.length > blockSize) {
				byte[] temp = new byte[blockSize];
				System.arraycopy(key, 0, temp, 0, blockSize);
				key = temp;
			}
			SecretKeySpec skey = new SecretKeySpec(key, algo);
			mac = Mac.getInstance(algo);
			mac.init(skey);
		} catch (InvalidKeyException | NoSuchAlgorithmException ex) {
			throw new PacketException(ex.getMessage(), ex);
		}
	}

	@Override
	public void update(int i) {
			byte[] tmp = new byte[4];
			tmp[0] = (byte) (i >>> 24);
			tmp[1] = (byte) (i >>> 16);
			tmp[2] = (byte) (i >>> 8);
			tmp[3] = (byte) i;
			update(tmp, 0, 4);
	}

	/**
	 * Processes the first len bytes in input, starting at offset inclusive.
	 *
	 * @param foo input
	 * @param s   offset in input
	 * @param l   number of bytes to process
	 */
	@Override
	public void update(byte foo[], int s, int l) {
			mac.update(foo, s, l);
	}

	/**
	 * Reads a MAC from readBuffer. position() to limit(). When finished the buffer will be at its limit
	 * <p/>
	 * @param readBuffer
	 */
	@Override
	public void update(ByteBuffer readBuffer) {
			mac.update(readBuffer);
	}

	@Override
	public byte[] doFinal() {
			return mac.doFinal();
	}

	/**
	 *
	 * @param output javax.crypto.Mac.doFinal stores the Mac result in this array.
	 * @param offset array index where output starts
	 */
	@Override
	public void doFinal(byte[] output, int offset) {
		try {
			mac.doFinal(output, offset);
		} catch (ShortBufferException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getBlockSize() {
		return blockSize;
	}
}
