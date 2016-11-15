package org.dla.nioftp.client.session.ssh.cyphony;

import java.nio.ByteBuffer;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Feb 5, 2013 9:30:55 AM
 */
public interface IMac {

	String getName();

	int getBlockSize();

	void update(byte[] foo, int start, int len);

	void update(ByteBuffer readBuffer);

	void update(int foo);

	void doFinal(byte[] buf, int offset);

	byte[] doFinal();
}
