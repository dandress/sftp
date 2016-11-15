package org.dla.nioftp.client.session.channel;

import org.dla.nioftp.client.session.ssh.packet.FtpMessageType;
import org.dla.nioftp.client.session.ssh.packet.FxpPacket;
import org.dla.nioftp.client.session.ssh.packet.FxpWrite;
import org.dla.nioftp.client.session.ssh.packet.PacketException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 * nioftp
 * Jun 10, 2013 1:06:45 PM
 */
class Writer implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(Writer.class);
	File file;
	FileInputStream fin = null;
	FileChannel fChan = null;
	int bytesRead = 0;
	long offset = 0;
	byte[] data;
	long fileSize;
	private final String fileName;
	private final SftpChannel channel;
	final int maxPacketSize;
	final int recipientChannelNumber;

	Writer(final SftpChannel channel, final String sourceFileName, final int maxPacketSize) {
		this.file = new File(sourceFileName);
		this.channel = channel;
		this.maxPacketSize = maxPacketSize;
		this.recipientChannelNumber = channel.serverChannel;
		fileName = file.getName();
		data = new byte[maxPacketSize - 29];
	}

	void init() throws FileNotFoundException, IOException {
		fin = new FileInputStream(file);
		fileSize = fin.getChannel().size();
	}

	@Override
	public void run() {
		try {
			logger.trace("maxPacketSize: {}", maxPacketSize);
			int serverOffset = 0;
			int length = maxPacketSize;
			while ((bytesRead = fin.read(data)) > 0) {
				channel.windowSize -= data.length;

				offset += (long) bytesRead;
				length = Math.min((int) (fileSize - offset), length);
				final int fxpLength = 21 + channel.handle.length + bytesRead;
				FxpPacket pk = new FxpPacket(FtpMessageType.SSH_FXP_WRITE, recipientChannelNumber, fxpLength, channel.requestWrite, (maxPacketSize + 2500));
				FxpWrite freight = (FxpWrite) pk.getFreighter();
				freight.setHandle(channel.handle);
				freight.setOffset(serverOffset);
//				if (serverOffset == 0) // log on the first write
//					logger.trace("data array length: {}. bytesRead: {}. fxpLength: {}", data.length, bytesRead, pk.getFxpLength());
				serverOffset += bytesRead;
				freight.setDataLength(bytesRead);
				freight.setDataStart(0);
				freight.setData(data);
				freight.loadFreight();
				logger.debug("Wrote {} bytes of {} total, for file {}", offset, fileSize, fileName);
				channel.sendPacket(pk);

				channel.cyphonyLock.lock();
				try {
					// the thread will now give up the lock until condition.signal() is called
					logger.trace("SftpChannel {} File Writer is waiting for a status reply from the server.", channel.getChannelId());
					channel.condition.await();
				} catch (InterruptedException ex) {
					break;
				} finally {
					channel.cyphonyLock.unlock();
				}
			}  // while()...
			logger.debug("Writer thread for {} exited", fileName);
			data = null;
		} catch (IOException | PacketException ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			try {
				if (channel != null)
					channel.closeFileHanle();
				if (file != null)
					file = null;
				if (fin != null) {
					fin.close();
					fin = null;
				}
				if (fChan != null) {
					fChan.close();
					fChan = null;
				}
			} catch (IOException | PacketException ex) {
				logger.error(ex.getMessage(), ex);
			}
		}
	} // run



}
