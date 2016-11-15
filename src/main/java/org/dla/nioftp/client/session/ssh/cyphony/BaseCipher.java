package org.dla.nioftp.client.session.ssh.cyphony;

import org.dla.nioftp.client.session.ssh.packet.PacketException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 5, 2013 9:36:05 AM
 */
public class BaseCipher implements ICipher {

	final static int ENCRYPT_MODE = 0;
	final static int DECRYPT_MODE = 1;
	final int mode;
	final private int ivSize;
	final private int blockSize;
	final private boolean cbc;
	private byte[] key = null;
	private byte[] iv = null;
	private Cipher cipher;

	/**
	 * Xtor
	 *
	 * @param decrypt       true for DECRYPT_MODE, false for ENCRPT_MODE
	 * @param key           'keyx2X'
	 * @param iv            'IVx2x'
	 * @param H
	 * @param K
	 * @param messageDigest
	 */
	public BaseCipher(final boolean decrypt, byte[] key, byte[] iv, byte[] H, byte[] K, MessageDigest messageDigest, int blockSize, int ivSize, boolean cbc) {
		this.mode = decrypt == true ? javax.crypto.Cipher.DECRYPT_MODE : javax.crypto.Cipher.ENCRYPT_MODE;
		this.key = key;
		this.iv = iv;
		this.ivSize = ivSize;
		this.blockSize = blockSize;
		this.cbc = cbc;
		buildKey(H, K, messageDigest);
	}

	@Override
	public void init(final String algo, final String transformation, final String pad) throws PacketException {
		sizeIv();
		sizeKey();
		try {
			SecretKeySpec skeySpec = new SecretKeySpec(key, algo);
			StringBuilder sb = new StringBuilder(algo);
			sb.append("/");
			if (transformation.length() > 0) {  // if the re is no transformation then don't include it or 'pad'  i.e. RC4 algorithm
				sb.append(transformation).append("/").append(pad);
			}
			cipher = javax.crypto.Cipher.getInstance(sb.toString());
			cipher.init(mode, skeySpec, new IvParameterSpec(iv));
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
			throw new PacketException(ex.getMessage(), ex);
		}
	}

	/**
	 * The first inputLen bytes in the input buffer, starting at inputOffset inclusive, are processed,
	 * and the result is stored in the output buffer, starting at outputOffset inclusive
	 *
	 * @param foo          Input
	 * @param inputOffset
	 * @param inputLen
	 * @param output
	 * @param outputOffset
	 * @throws Exception
	 *
	 * @return the number of bytes stored in output
	 */
	@Override
	public int update(byte[] foo, int inputOffset, int inputLen, byte[] output, int outputOffset) throws Exception {
		return cipher.update(foo, inputOffset, inputLen, output, outputOffset);
	}

	@Override
	public int update(ByteBuffer inBuffer, ByteBuffer outBuffer) throws ShortBufferException {
		return cipher.update(inBuffer, outBuffer);
	}

	@Override
	public int doFinal(byte[] foo, int inputOffset, int inputLen, byte[] output, int outputOffset) throws Exception {
		return cipher.doFinal(foo, inputOffset, inputLen, output, outputOffset);
	}

	@Override
	public byte[] doFinal(byte[] input) throws IllegalBlockSizeException, BadPaddingException {
		return cipher.doFinal(input);
	}

	@Override
	public byte[] doFinal() throws IllegalBlockSizeException, BadPaddingException {
		return cipher.doFinal();
	}

	@Override
	public byte[] update(byte[] input) {
		return cipher.update(input);
	}

	@Override
	public int getOutputSize(final int inputSize) {
		return cipher.getOutputSize(inputSize);
	}

	private void sizeIv() {
		if (iv.length > ivSize) {
			byte[] tmp;
			tmp = new byte[ivSize];
			System.arraycopy(iv, 0, tmp, 0, tmp.length);
			iv = tmp;
		}
	}

	private void sizeKey() {
		if (key.length > blockSize) {
			byte[] tmp;
			tmp = new byte[blockSize];
			System.arraycopy(key, 0, tmp, 0, tmp.length);
			key = tmp;
		}
	}

	private void buildKey(byte[] H, byte[] K, MessageDigest messageDigest) {
		while (blockSize > key.length) {
			Buffer buffer = new Buffer(10000);
			buffer.reset();
			buffer.putMPInt(K);
			buffer.putByte(H);
			buffer.putByte(key);
			messageDigest.update(buffer.buffer, 0, buffer.index);
			byte[] foo = messageDigest.digest();
			byte[] bar = new byte[key.length + foo.length];
			System.arraycopy(key, 0, bar, 0, key.length);
			System.arraycopy(foo, 0, bar, key.length, foo.length);
			key = bar;
		}
	}

	@Override
	public int getIVSize() {
		return ivSize;
	}

	@Override
	public int getBlockSize() {
		return blockSize;
	}

	@Override
	public boolean isCBC() {
		return cbc;
	}

	public boolean isCbc() {
		return cbc;
	}
}
