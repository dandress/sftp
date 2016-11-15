package org.dla.nioftp.client.session.channel;

import org.dla.nioftp.client.Client;
import org.dla.nioftp.client.events.ChannelEventSource;
import org.dla.nioftp.client.events.ChannelEventType;
import org.dla.nioftp.client.session.ssh.packet.ChanClose;
import org.dla.nioftp.client.session.ssh.packet.ChanData;
import org.dla.nioftp.client.session.ssh.packet.ChanOpen;
import org.dla.nioftp.client.session.ssh.packet.ChanOpenConfirm;
import org.dla.nioftp.client.session.ssh.packet.ChanOpenFailure;
import org.dla.nioftp.client.session.ssh.packet.ChanRequest;
import org.dla.nioftp.client.session.ssh.packet.ChanRequestFailure;
import org.dla.nioftp.client.session.ssh.packet.ChanSuccess;
import org.dla.nioftp.client.session.ssh.packet.ChanWindowAdjust;
import org.dla.nioftp.client.session.ssh.packet.DisconnectFreighter;
import org.dla.nioftp.client.session.ssh.packet.Freight;
import org.dla.nioftp.client.session.ssh.packet.FtpMessageType;
import org.dla.nioftp.client.session.ssh.packet.FxpClose;
import org.dla.nioftp.client.session.ssh.packet.FxpHandle;
import org.dla.nioftp.client.session.ssh.packet.FxpInit;
import org.dla.nioftp.client.session.ssh.packet.FxpOpen;
import org.dla.nioftp.client.session.ssh.packet.FxpPacket;
import org.dla.nioftp.client.session.ssh.packet.FxpStatus;
import org.dla.nioftp.client.session.ssh.packet.FxpVersion;
import org.dla.nioftp.client.session.ssh.packet.MessageType;
import org.dla.nioftp.client.session.ssh.packet.Packet;
import org.dla.nioftp.client.session.ssh.packet.PacketException;
import org.dla.nioftp.client.session.ssh.packet.SshPacket;
import org.dla.threads.ThreadSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 * <p/>
 *  Feb 11, 2013 1:24:37 PM
 */
public class SftpChannel extends ChannelEventSource {

	private static final Logger logger = LoggerFactory.getLogger(SftpChannel.class);
	/**
	 * the server's channel number
	 */
	int serverChannel;
	/**
	 * * our channel number
	 */
	private final int channelId;
	/**
	 * Prevents sendCloseChannel() from being executed more then once. This happened during stress testing.
	 */
	private boolean closeChannelSent = false;
	/**
	 * Holds the file handle returned by the server
	 */
	byte[] handle = null;
	long windowSize = 0x100000;  // 1,048,512
	final ReentrantLock cyphonyLock;
	final Condition condition;
	final int requestWrite = 503;
	private int maxPacketSize = 32768 * 2;
	private final int requestInit = 501;  // sent to 'Xtor, but not used
	private final int requestOpen = 502;
	private final int requestClose = 504;
	private ChannelQueue channelQueue = null;
	private final String fullFilePath;
	private final String fileName;
	final Client client;

	public SftpChannel(final Client client, final ReentrantLock cyphonyLock, final int channelId,
			final String fileSourceDirectory, final String fileName) {
		this.client = client;
		this.channelId = channelId;
		this.fileName = fileName;

		StringBuilder sb = new StringBuilder(fileSourceDirectory);
		if (sb.charAt(sb.length() - 1) != '/')
			sb.append("/");
		sb.append(fileName);
		this.fullFilePath = sb.toString().trim();

		this.cyphonyLock = cyphonyLock;
		condition = cyphonyLock.newCondition();

		channelQueue = new ChannelQueue(this);
		ThreadSource.getInstance().executeThread(channelQueue, "Channel '" + channelId + "' Queue");
	}

	public ChannelQueue getChannelQueue() {
		return channelQueue;
	}

	public void handlePacket(final Packet incomingPacket) {
		Freight incomingFreight = incomingPacket.getFreighter();
		if (incomingFreight == null) {
			logger.warn("{} Packet was unreadable", toString());
			return;
		}
		logger.trace("Incoming {}", incomingPacket.toString());
		try {
			if (incomingFreight instanceof ChanOpenConfirm) {  // response to openSftpChannel()
				ChanOpenConfirm fr = (ChanOpenConfirm) incomingFreight;
				fr.unloadFreight();
				serverChannel = fr.getSenderChannel();
				windowSize = fr.getInitialWindowSize();
				logger.debug("New channel confirmation {}", toString());
				maxPacketSize = fr.getMaxPacketSize();
				logger.trace("{} is connected to serverChannel {}", toString(), serverChannel);
				fireEvent(this, ChannelEventType.CHAN_OPEN, fileName);
				openSftpSubsystem();
			} else if (incomingFreight instanceof ChanOpenFailure) { // This channel faild to open at the server side
				ChanOpenFailure cof = (ChanOpenFailure) incomingFreight; // we need to clean up a little
				cof.unloadFreight();				// but the channel hasn't openened a file, so cleanup is eash
				// Tell the Session about the failure.
				fireEvent(this, ChannelEventType.CHAN_OPEN_FAIL, fileName, cof.getMessage());
				// and then call close() instead of closeAndRemoveChannel() as the server will not send any response to the later
				// close() will also tell Session to remove this channel from its channels Map
				close();
			} else if (incomingFreight instanceof ChanClose) {
				ChanClose fr = (ChanClose) incomingFreight;
				fr.unloadFreight();		// only contains recipientChannel #
				close();
			} else if (incomingFreight instanceof ChanRequest) { // sent by OpenSSH when FxpClose is sent, followed by ChanClose
				ChanRequest fr = (ChanRequest) incomingFreight;
				fr.unloadFreight();
				final String message = fr.getMessage();
				final String data = fr.getData();
				if (message != null) {
					if (message.equals("exit-status")) { // same as disconnect
						fireEvent(this, ChannelEventType.CHAN_OPEN_FAIL, fileName, data);
						logger.warn("SSH_MSG_CHANNEL_REQUEST, exit-status received. data: {}", data);
					}
				} else {
					logger.debug("SSH_MSG_CHANNEL_REQUEST received. Presumably in response to closing the channel");
				}
			} else if (incomingFreight instanceof ChanRequestFailure) { // failure response to openSftpChannel()
				logger.warn("{} Channel open request failed", toString());
				fireEvent(this, ChannelEventType.CHAN_OPEN_FAIL, fileName);
			} else if (incomingFreight instanceof ChanSuccess) {	// response to openSftpSubsystem()
				logger.debug("{} Channel open success", toString());
				ChanSuccess freight = (ChanSuccess) incomingFreight;
				freight.unloadFreight();
				fireEvent(this, ChannelEventType.CHAN_FXP_OPEN, fileName);
				sendFxpInit();
			} else if (incomingFreight instanceof ChanWindowAdjust) {
				ChanWindowAdjust fr = (ChanWindowAdjust) incomingFreight;
				fr.unloadFreight();
				assert fr.getRecipientChannel() == channelId;
				logger.trace("Window size: {}. Adjust by: {}", windowSize, fr.getBytesToAdd());
				windowSize += fr.getBytesToAdd();
			} else if (incomingFreight instanceof ChanData) {
				ChanData freight = (ChanData) incomingFreight;
				freight.unloadFreight();
				logger.debug("ChanData: {}", freight.getData());
			} else if (incomingFreight instanceof FxpVersion) {		// server's response to SSH_FXP_INIT message
				FxpVersion freight = (FxpVersion) incomingFreight;
				freight.unloadFreight();
				logger.trace("FXP Version: {}", freight.getVersion());
				String[] extensions;
				while ((extensions = freight.getExtensionPairs()) != null) {
					logger.trace("Extension - Name: {} - Data: {}", extensions[0], extensions[1]);
				}
				fireEvent(this, ChannelEventType.CHAN_FXP_INIT, fileName);
			} else if (incomingFreight instanceof FxpHandle) {
				FxpHandle freight = (FxpHandle) incomingFreight;
				freight.unloadFreight();
				this.handle = freight.getHandle();
				logger.trace("File handle length: {}", handle.length);
				writFile();
				fireEvent(this, ChannelEventType.CHAN_FILE_OPEN, fileName);
			} else if (incomingFreight instanceof FxpStatus) {
				FxpPacket fxpPacket = (FxpPacket) incomingPacket;
				FxpStatus freight = (FxpStatus) incomingFreight;
				freight.unloadFreight();
				if (fxpPacket.getRequestId() == requestWrite) {
					if (freight.getErrorCode() == 0) { // success
						cyphonyLock.lock();
						try {
							logger.trace("{} Status received for file Writer", toString());
							condition.signal();
						} finally {
							cyphonyLock.unlock();
						}
					}
				} else if (fxpPacket.getRequestId() == requestOpen) { // file open failed
					logger.warn("{} Open request failed (FXP_OPEN): {} - {}", toString(), freight.getErrorCode(), freight.getErrorMsg());
					fireEvent(this, ChannelEventType.CHAN_FILE_OPEN_FAILED, fileName, freight.getErrorMsg());
					close(); // This way Session will remove this channel from its channel map
				} else if (fxpPacket.getRequestId() == requestClose) { // response to file shutdown
					handle = null;		// this is important, as shutdown() will try to send FXP_CLOSE if it's not null....
					logger.debug("{} Response to SSH_FXP_CLOSE: {} - {}", toString(), freight.getErrorCode(), freight.getErrorMsg());
					fireEvent(this, ChannelEventType.CHAN_FILE_CLOSED, fileName, freight.getErrorMsg());
					closeAndRemoveChannel();
				} else {
					logger.debug("{} Status response: {} - {}", toString(), freight.getErrorCode(), freight.getErrorMsg());
				}
			} else if (incomingFreight instanceof DisconnectFreighter) {
				incomingFreight.unloadFreight();
				logger.warn("{} DICONNECTED: {} - {}", toString(), ((DisconnectFreighter) incomingFreight).getReasonCode(),
						((DisconnectFreighter) incomingFreight).getDescription());
				fireEvent(this, ChannelEventType.SSH_DISCONNECTED, fileName);
			} else {
				if (incomingFreight != null)
					logger.warn("Unhandled Packet of type: {} for message: {}", incomingFreight.getClass().getCanonicalName(), incomingPacket.toString());
				else
					logger.warn("Unhandled Packet  for message: {}", incomingPacket.toString());
			}

		} catch (PacketException ex) {
			fireEvent(this, ChannelEventType.CHAN_FAIL, fileName);
			logger.error(ex.getMessage(), ex);
		}
	}

	/**
	 * Called by Session.createNewChannel, which is called by ClientOperations
	 * <p/>
	 * @throws PacketException
	 */
	public void openSftpChannel() throws PacketException {
		Packet pk = new SshPacket(MessageType.SSH_MSG_CHANNEL_OPEN);
		ChanOpen freighter = (ChanOpen) pk.getFreighter();
		freighter.setChannelType("session");
		freighter.setSenderId(channelId);
		freighter.setInitialWindowSize((int) windowSize);
		freighter.setMaxPacketSize(maxPacketSize);
		freighter.loadFreight();
		sendPacket(pk);
	}

	private void openSftpSubsystem() throws PacketException {
		Packet pk = new SshPacket(MessageType.SSH_MSG_CHANNEL_REQUEST);
		ChanRequest freighter = (ChanRequest) pk.getFreighter();
		freighter.setRecipientChannel(serverChannel);
		freighter.setRequestType("subsystem");
		freighter.setWantReply(true);
		freighter.setData("sftp");
		freighter.loadFreight();
		sendPacket(pk);

	}

	private void sendFxpInit() throws PacketException {
		FxpPacket pk = new FxpPacket(FtpMessageType.SSH_FXP_INIT, serverChannel, 5, requestInit);
		FxpInit freighter = (FxpInit) pk.getFreighter();
		freighter.setVersion(6);
		freighter.loadFreight();
		sendPacket(pk);
	}

	/** Expects  <code>   else if (incomingFreight instanceof FxpHandle) {</code> message as a response.
	 * When received the response calls writeFile() below
	 *
	 * @throws PacketException
	 */
	public void sendFileOpen() throws PacketException {
		FxpPacket pk = new FxpPacket(FtpMessageType.SSH_FXP_OPEN, serverChannel, 17 + fileName.length(), requestOpen);
		FxpOpen freight = (FxpOpen) pk.getFreighter();
		freight.setPath(fileName);
		freight.setOpenW();
		freight.loadFreight();
		sendPacket(pk);
		logger.debug("SSH_FXP_OPEN sent for {}", fileName);
	}

	private void writFile() throws PacketException {
		try {
			logger.trace("Sending: {}. windowSize: {}. maxPacketSize: {}", fullFilePath, windowSize, maxPacketSize);
			Writer writer = new Writer(this, fullFilePath, maxPacketSize);
			writer.init();
			ThreadSource.getInstance().executeThread(writer, fullFilePath);
		} catch (FileNotFoundException ex) {
			throw new PacketException(ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new PacketException(ex.getMessage(), ex);
		}
	}

	void closeFileHanle() throws PacketException {
		FxpPacket pk = new FxpPacket(FtpMessageType.SSH_FXP_CLOSE, serverChannel, 9 + handle.length, requestClose);
		FxpClose freight = (FxpClose) pk.getFreighter();
		freight.setHandle(handle);
		freight.loadFreight();
		sendPacket(pk);
	}

	private void sendCloseChannel() throws PacketException {
		if (!closeChannelSent) {
			closeChannelSent = true;
			Packet pk = new SshPacket(MessageType.SSH_MSG_CHANNEL_CLOSE);
			ChanClose freighter = (ChanClose) pk.getFreighter();
			freighter.setRecipientChannel(serverChannel);
			freighter.loadFreight();
			sendPacket(pk);
		}
	}

	public void closeAndRemoveChannel() {
		try {
			logger.debug("Close requested for: {} ", toString());
			sendCloseChannel();
		} catch (PacketException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	/**
	 * Called when ChanClosed Packet is received in response to closeAndRemoveChannel
	 */
	private void close() {
		channelQueue.abort();
		try {
			if (handle != null)
				closeFileHanle();
		} catch (PacketException ex) {
			logger.error(ex.getMessage(), ex);
		}
		channelQueue = null;
		fireEvent(this, ChannelEventType.CHAN_CLOSED, fileName);
		logger.debug("{} is closed", toString());
	}

	void sendPacket(final Packet pk) throws PacketException {
		cyphonyLock.lock();
		try {
			byte[] buf = pk.encode(client.getPacketAlgos(), client.getOut_sequence_no());
			client.postMessage(buf);
		} finally {
			cyphonyLock.unlock();
		}
	}

	@Override
	public String toString() {
		return "channelId: " + channelId + "  file: " + fileName;
	}

	public int getChannelId() {
		return channelId;
	}

	public String getFileName() {
		return fileName;
	}

	@Override
	public int hashCode() {
		int hash = 6;
		hash = 13 * hash + this.channelId;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		if (this.channelId != ((SftpChannel) obj).channelId)
			return false;
		return true;
	}
}
