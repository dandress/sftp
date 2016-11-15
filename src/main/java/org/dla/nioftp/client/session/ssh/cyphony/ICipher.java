package org.dla.nioftp.client.session.ssh.cyphony;

import org.dla.nioftp.client.session.ssh.packet.PacketException;
import java.nio.ByteBuffer;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 5, 2013 9:29:24 AM
 */
public interface ICipher {

	int getIVSize();

	int getBlockSize();

	void init(final String algo, final String transformation, final String pad) throws PacketException;

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
	int update(byte[] foo, int s1, int len, byte[] bar, int s2) throws Exception;

	int update(ByteBuffer inBuffer, ByteBuffer outBuffer) throws ShortBufferException;

	byte[] update(byte[] input);

	int getOutputSize(final int inputSize);

	boolean isCBC();

	byte[] doFinal() throws IllegalBlockSizeException, BadPaddingException;

	int doFinal(byte[] foo, int inputOffset, int inputLen, byte[] output, int outputOffset) throws Exception;

	byte[] doFinal(byte[] input) throws IllegalBlockSizeException, BadPaddingException;

}
