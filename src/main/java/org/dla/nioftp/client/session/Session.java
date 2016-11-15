package org.dla.nioftp.client.session;

import org.dla.nioftp.Constants;
import org.dla.nioftp.client.Client;
import org.dla.nioftp.client.events.ChannelEvent;
import org.dla.nioftp.client.events.ChannelEventListener;
import org.dla.nioftp.client.events.ChannelEventType;
import org.dla.nioftp.client.session.channel.SftpChannel;
import org.dla.nioftp.client.session.ssh.cyphony.KeyExchange;
import org.dla.nioftp.client.session.ssh.packet.AlgorithmStrings;
import org.dla.nioftp.client.session.ssh.packet.ChanGlobalRequest;
import org.dla.nioftp.client.session.ssh.packet.DhInitFreighter;
import org.dla.nioftp.client.session.ssh.packet.DhReplyFreighter;
import org.dla.nioftp.client.session.ssh.packet.DisconnectFreighter;
import org.dla.nioftp.client.session.ssh.packet.Freight;
import org.dla.nioftp.client.session.ssh.packet.GenericFreighter;
import org.dla.nioftp.client.session.ssh.packet.KexInitFreighter;
import org.dla.nioftp.client.session.ssh.packet.KexPacket;
import org.dla.nioftp.client.session.ssh.packet.LoginFreighter;
import org.dla.nioftp.client.session.ssh.packet.MessageType;
import org.dla.nioftp.client.session.ssh.packet.NewKeysFreighter;
import org.dla.nioftp.client.session.ssh.packet.NullFreighter;
import org.dla.nioftp.client.session.ssh.packet.Packet;
import org.dla.nioftp.client.session.ssh.packet.PacketException;
import org.dla.nioftp.client.session.ssh.packet.ServiceRequestFreighter;
import org.dla.nioftp.client.session.ssh.packet.SshPacket;
import org.dla.nioftp.client.session.ssh.packet.UnimplementedFreighter;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 */public class Session extends Observable implements ChannelEventListener {

	private static final Logger logger = LoggerFactory.getLogger(Session.class);
	private KeyExchange keyExchange = null;
	private AlgorithmStrings algorithmStrings = null;
	private byte[] ourI_C;
	private String username;
	private String password;
	private int sessionId;
	private final ReentrantLock cyphonyLock = new ReentrantLock(false);
	private ConcurrentHashMap<Integer, SftpChannel> channels;
	private final Client client;
	private final Observer observer;
	private PriorityBlockingQueue<ChannelSpecs> newChannelQueue;
	/** Set to true in shutdown(). When true handleEvent() will look for a response from the server
	 for each SSH_MSG_CHANNEL_CLOSE sent by all open channels. However, if shutdownNow is called, in response to
	 an SSH_DISCONNECT, then there is no longer a network connection and we'll never see the SSH_MSG_CHANNEL_CLOSE
	 responses. So, shutdownNow() leaves this false, so handleEvent() does not look for them.*/
	private boolean shuttingDown = false;
	/**  True when shutdownNow() has been called. Used in shutdown() as a flag to avoid shutting down twice.	 */
	private boolean immediateShutdown = false;
	private final ReentrantLock shLock = new ReentrantLock(false);
	private final Condition shCondition = shLock.newCondition();


	public Session(final Client client, final Observer observer) {
		this.client = client;
		this.observer = observer;
		algorithmStrings = new AlgorithmStrings();
		channels = new ConcurrentHashMap<>(15);
		newChannelQueue = new PriorityBlockingQueue<>(10);
	}

	public void init(final String userName, final String password, final int sessionId) {
		this.username = userName;
		this.password = password;
		this.sessionId = sessionId;
		addObserver(observer);


		// ourI_C needs to be created before a KEX_INIT message is received.
		// this was the most realistic way of doing that.
		try {
			Packet writePacket = new KexPacket(MessageType.SSH_MSG_KEXINIT);
			KexInitFreighter kexInitFreight = (KexInitFreighter) writePacket.getFreighter();
			kexInitFreight.setAlgorithmStrings(algorithmStrings);
			kexInitFreight.loadFreight();
			final byte[] buf = writePacket.encode();
			ourI_C = kexInitFreight.getFreight();
		} catch (PacketException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public void handlePacket(final Packet incomingPacket) {
		Freight incomingFreight = incomingPacket.getFreighter();
		if (incomingFreight == null) {
			logger.warn("Incoming data was not readable as a Packet");
			return;
		}
		try {
			logger.debug("Incoming {}", incomingPacket.toString());
			if (incomingFreight instanceof KexInitFreighter) {
				((KexInitFreighter) incomingFreight).setAlgorithmStrings(algorithmStrings);
				incomingFreight.unloadFreight();

				keyExchange = new KeyExchange(client.getServerVersionId(), Constants.ourVersionId,
						incomingFreight.getFreight(), ourI_C);
				assert keyExchange != null;
				sendKexDhInit();
			} else if (incomingFreight instanceof DhReplyFreighter) {
				incomingFreight.unloadFreight();
				assert keyExchange != null;
				/* Again, from the RFC The server then responds with the following: byte SSH_MSG_KEXDH_REPLY
				 string server public host key	and certificates(K_S)
				 mpint     f
				 string    signature of H */
				DhReplyFreighter drf = (DhReplyFreighter) incomingFreight;
				keyExchange.setKS(drf.getK_S());
				keyExchange.setF(drf.getF());
				keyExchange.setSig_of_H(drf.getSig_of_H());
				if (keyExchange.verifyKeys()) {
					client.createPacketAlgos(keyExchange, algorithmStrings);
				}
			} else if (incomingFreight instanceof NewKeysFreighter) {
				incomingFreight.unloadFreight();
				sendNewKeys();
				postStatus(SessionStatus.StatusType.SSH_KEX);
				sendUserAuthRequest();
			} else if (incomingFreight instanceof GenericFreighter) {  // several messages use this freighter....
				incomingFreight.unloadFreight();
				final String message = incomingFreight.getMessage();	// GenericFreighter is one of the few to implement this method...
				switch (incomingPacket.getMsgType()) {
					case SSH_MSG_SERVICE_ACCEPT:
						if (message.equals("ssh-userauth")) {
							postStatus(SessionStatus.StatusType.SSH_USERAUTH_SVC_ACCEPT);
							sendLogin();
						}
						break;
					case SSH_MSG_USERAUTH_FAILURE:
						logger.warn("Login falied: {}", message);
						postStatus(SessionStatus.StatusType.SSH_AUTH_FAILED, message);
						break;
					case SSH_MSG_USERAUTH_BANNER:
						logger.info(message);
						break;
					default:
				}
			} else if (incomingFreight instanceof NullFreighter) {	// likewise, this freighter is used by more than one fileName
				switch (incomingPacket.getMsgType()) {
					case SSH_MSG_USERAUTH_SUCCESS:
						postStatus(SessionStatus.StatusType.SSH_AUTH_SUCCESS);
						break;
					case SSH_MSG_IGNORE:
						logger.debug("SSH_MSG_IGNORE received as a kepp alive");
						sendKeepAlive();
						break;
					default:
				}
			} else if (incomingFreight instanceof UnimplementedFreighter) {
				UnimplementedFreighter uf = (UnimplementedFreighter) incomingFreight;
				uf.unloadFreight();
				logger.warn("Rejected packet #{}", uf.getRejectedPacket());
			} else if (incomingFreight instanceof ChanGlobalRequest) {
				sendKeepAlive();
				incomingFreight.unloadFreight();
				logger.debug("Global Request - {}  {}", ((ChanGlobalRequest) incomingFreight).getRequestName(),
						((ChanGlobalRequest) incomingFreight).getData());
			} else if (incomingFreight instanceof DisconnectFreighter) {
				incomingFreight.unloadFreight();
				// Some SSH applications send SSH_DISCONNECT with reasonCode 11 every time the session is
				// closed. Like during a timed restart. Sending postStatus just results in SessionController.restart being called again!!
				if (((DisconnectFreighter) incomingFreight).getReasonCode() != 11)
					postStatus(SessionStatus.StatusType.SSH_DISCONNECTED, ((DisconnectFreighter) incomingFreight).getDescription());
				logger.warn("DISCONNECTED: {} - {}", ((DisconnectFreighter) incomingFreight).getReasonCode(), ((DisconnectFreighter) incomingFreight).getDescription());
			} else {
				if (incomingFreight != null)
					logger.warn("Unhandled Packet of type: {} for message: {}", incomingFreight.getClass().getCanonicalName(), incomingPacket.toString());
				else
					logger.warn("Unhandled Packet  for message: {}", incomingPacket.toString());
			}
		} catch (PacketException ex) {
			logger.error(ex.getMessage(), ex);
		}
		incomingFreight = null;
	}

	private void sendUserAuthRequest() throws PacketException {
		Packet svcRequest = new KexPacket(MessageType.SSH_MSG_SERVICE_REQUEST);
		ServiceRequestFreighter srf = (ServiceRequestFreighter) svcRequest.getFreighter();
		srf.setServiceName("ssh-userauth");
		srf.loadFreight();
		cyphonyLock.lock();
		try {
			final byte[] svcBuf = svcRequest.encode(client.getPacketAlgos(), client.getOut_sequence_no());
			client.postMessage(svcBuf);
		} finally {
			cyphonyLock.unlock();
		}

		logger.info("Requesting ssh-userauth");
	}

	private void sendNewKeys() throws PacketException {
		Packet pkKeys = new KexPacket(MessageType.SSH_MSG_NEWKEYS);
		Freight fr = pkKeys.getFreighter();
		// no freight for new_keys
		final byte[] nkBuf = pkKeys.encode();
		client.postMessage(nkBuf);
	}

	/**
	 * from the RFC First, the client sends the following: byte SSH_MSG_KEXDH_INIT mpint e
	 */
	private void sendKexDhInit() throws PacketException {
		Packet dhPacket = new KexPacket(MessageType.SSH_MSG_KEXDH_INIT);
		DhInitFreighter df = (DhInitFreighter) dhPacket.getFreighter();
		df.setE(keyExchange.getE());
		df.loadFreight();
		final byte[] dhBuf = dhPacket.encode();
		client.postMessage(dhBuf);
	}

	public void sendKexInit() throws PacketException {
		logger.debug("sending KEX init");
		Packet writePacket = new KexPacket(MessageType.SSH_MSG_KEXINIT);
		KexInitFreighter kexInitFreight = (KexInitFreighter) writePacket.getFreighter();
		kexInitFreight.setAlgorithmStrings(algorithmStrings);
		kexInitFreight.loadFreight();
		final byte[] buf = writePacket.encode();
		client.postMessage(buf);
		ourI_C = kexInitFreight.getFreight();
	}

	private void sendLogin() throws PacketException {
		Packet login = new KexPacket(MessageType.SSH_MSG_USERAUTH_REQUEST);
		LoginFreighter lf = (LoginFreighter) login.getFreighter();
		lf.setUsername(username);
		lf.setPassword(password);
		lf.loadFreight();
		cyphonyLock.lock();
		try {
			final byte[] lBuf = login.encode(client.getPacketAlgos(), client.getOut_sequence_no());
			client.postMessage(lBuf);
		} finally {
			cyphonyLock.unlock();
		}
	}

	/* OpenSSH (sshd) wouldn't accept this. No idea why, but it may be needed by somebody else... */
	private void sendKexDhGexRequest() throws PacketException {
		Packet drPacket = new KexPacket(MessageType.SSH_MSG_KEX_DH_GEX_REQUEST);
		Freight dr = drPacket.getFreighter();
		dr.loadFreight();
		final byte[] drBuf = drPacket.encode();
		client.postMessage(drBuf);
	}

	private void sendKeepAlive() throws PacketException {
		Packet pk = new SshPacket(MessageType.SSH_MSG_IGNORE);
		NullFreighter nf = (NullFreighter)pk.getFreighter();
		nf.loadFreight();	// not that there is any...
		final byte[] lBuf = pk.encode(client.getPacketAlgos(), client.getOut_sequence_no());
		client.postMessage(lBuf);
	}
	private void sendKeepAliveXX() throws PacketException {
		Packet pk = new SshPacket(MessageType.SSH_MSG_GLOBAL_REQUEST);
		ChanGlobalRequest cgr = (ChanGlobalRequest) pk.getFreighter();
		cgr.setRequestName("keepalive@callsoruce.com");
		cgr.loadFreight();
		final byte[] lBuf = pk.encode(client.getPacketAlgos(), client.getOut_sequence_no());
		client.postMessage(lBuf);
	}

	public void sendDisconnect() throws PacketException {
		Packet pk = new SshPacket(MessageType.SSH_MSG_DISCONNECT);
		DisconnectFreighter df = (DisconnectFreighter)pk.getFreighter();
		df.setDescription("Received an invalid Packet");
		df.setReasonCode(4);
		df.loadFreight();
//		final byte[] lBuf = pk.encode(client.getPacketAlgos(), client.getOut_sequence_no());
		final byte[] lBuf = pk.encode();
		client.postMessage(lBuf);
	}



	/**
	 * Creates a Channel (?)
	 * <p/>
	 * @param channelId
	 * @param ops                 Added as a ClientEventListener
	 * @param fileSourceDirectory
	 * @param fileName
	 * @throws PacketException
	 */
	// final String sourceFileName, final String destinationFileName
	public void createNewChannel(final int channelId, final String fileSourceDirectory, final String fileName) throws PacketException {
		final ChannelSpecs chSpecs = new ChannelSpecs(fileSourceDirectory, fileName, channelId);
		newChannelQueue.add(chSpecs);
		if (newChannelQueue.size() == 1) {
			makeWaitingChannel();
		}
	}

	private void makeWaitingChannel() throws PacketException {
		if (newChannelQueue.size() < 1)
			return;
		try {
			final ChannelSpecs chSpecs = newChannelQueue.poll(100, TimeUnit.MILLISECONDS);
			if (chSpecs != null) {
				final int channelId = chSpecs.channelId;
				SftpChannel sftpChannel = new SftpChannel(client, cyphonyLock, channelId, chSpecs.channelDir, chSpecs.channelFile);
				sftpChannel.addEventListener(this);
				channels.put(channelId, sftpChannel);
				sftpChannel.openSftpChannel();
				logger.debug("New Channel: {}", sftpChannel.toString());
			}
		} catch (InterruptedException ex) {
		}
	}

	public int getSessionId() {
		return sessionId;
	}

	public ConcurrentHashMap<Integer, SftpChannel> getChannels() {
		return channels;
	}

	public void shutdown() {
		if (immediateShutdown)
			return;

		logger.info("shutdown");
		shLock.lock();
		try {
			shuttingDown = true;
			if (newChannelQueue != null) {
				newChannelQueue.clear();
			}
			if (channels != null) {
				Collection<SftpChannel> chs = channels.values();
				logger.debug("Closing {} channels", chs.size());
				for (SftpChannel ch : chs) {
					if (ch != null) {
						ch.closeAndRemoveChannel();
						shCondition.await();
					}
				}
			}
			if (keyExchange != null)
				keyExchange.clear();
			keyExchange = null;
			algorithmStrings = null;
		} catch (InterruptedException ex) {
		} finally {
			shLock.unlock();
		}
		if (channels != null)
			channels.clear();
		channels = null;
		deleteObservers();
	}

	/**
	 * Called from Client when a packet of less then 0 bytes is received, the servers way of saying goodbye.
	 */
	public void shutdownNow() {
		postStatus(SessionStatus.StatusType.SSH_DISCONNECTED, "Server sent a 0 byte packet");
		if (shuttingDown)
			return;
		logger.info("shutdownNow!!");
		immediateShutdown = true;
		if (newChannelQueue != null) {
			newChannelQueue.clear();
		}
		Collection<SftpChannel> chs = channels.values();
		for (SftpChannel ch : chs) {
			if (ch != null) {
				ch.closeAndRemoveChannel();
				ch = null;
			}
		}
		if (keyExchange != null)
			keyExchange.clear();
		keyExchange = null;
		algorithmStrings = null;
		if (channels != null)
			channels.clear();
		client.shutdown();
		deleteObservers();
	}

	@Override
	public void handleEvent(ChannelEvent evt) {
		if (channels == null)
			return;
		logger.debug("ChannelEventType: {}. Channel: {} ", evt.eventType, ((SftpChannel)evt.getSource()).getChannelId());
		if (evt.eventType == ChannelEventType.CHAN_OPEN) {  // Server accepted our request for a new channel
			postStatus(FileStatus.Status.CHAN_OPENED, evt.fileName);
		} else if (evt.eventType == ChannelEventType.CHAN_CLOSED) {  // The Channel has closed. All work is done. ClientControl can move on.
			postStatus(FileStatus.Status.CHAN_CLOSED, evt.fileName);
			Object obj = channels.remove(((SftpChannel) evt.getSource()).getChannelId());
			if (obj != null) {
				logger.trace("Channel removed from channels map");
				obj = null;
			} else
				logger.warn("Channel was not found in channels map");
			((SftpChannel) evt.getSource()).removeEventListener(this);
			if (shuttingDown) {
				shLock.lock();
				try {
					shCondition.signal();
				} finally {
					shLock.unlock();
				}
			}
		} else if (evt.eventType == ChannelEventType.CHAN_FILE_OPEN) {
			postStatus(FileStatus.Status.OPENED, evt.fileName);
			try {
				makeWaitingChannel();
			} catch (PacketException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		} else if (evt.eventType == ChannelEventType.CHAN_OPEN_FAIL) {
			logger.warn("CHAN_OPEN_FAIL {}", evt.fileName);
			postStatus(FileStatus.Status.CHAN_OPEN_FAILED, evt.fileName, evt.errorText);
			try {
				makeWaitingChannel();
			} catch (PacketException ex) {
				logger.warn(ex.getMessage(), ex);
			}
		} else if (evt.eventType == ChannelEventType.CHAN_FXP_OPEN) {		// sftp subsystem open success
		} else if (evt.eventType == ChannelEventType.CHAN_FXP_INIT) {
			try {
				// can send file
				((SftpChannel) evt.getSource()).sendFileOpen();
			} catch (PacketException ex) {
				logger.error(ex.getMessage(), ex);
			}
		} else if (evt.eventType == ChannelEventType.CHAN_FILE_CLOSED) {
			postStatus(FileStatus.Status.SUCCESS, evt.fileName);
		} else if (evt.eventType == ChannelEventType.CHAN_FILE_OPEN_FAILED) {
			postStatus(FileStatus.Status.FILE_OPEN_FAILED, evt.fileName, evt.errorText);
		} else if (evt.eventType == ChannelEventType.SSH_DISCONNECTED) {
			postStatus(FileStatus.Status.DISCONNECTED, evt.fileName);
		} else if (evt.eventType == ChannelEventType.CHAN_FAIL) {
			postStatus(FileStatus.Status.FILE_OPEN_FAILED, evt.fileName);
		}
	}

	private void postStatus(FileStatus.Status status, String fileName) {
		FileStatus fs = new FileStatus(status, fileName);
		setChanged();
		notifyObservers(fs);
	}

	private void postStatus(FileStatus.Status status, String fileName, String errorMsg) {
		FileStatus fs = new FileStatus(status, fileName, errorMsg);
		setChanged();
		notifyObservers(fs);
	}

	private void postStatus(SessionStatus.StatusType statusType) {
		SessionStatus status = new SessionStatus(statusType);
		setChanged();
		notifyObservers(status);
	}

	private void postStatus(SessionStatus.StatusType statusType, String message) {
		SessionStatus status = new SessionStatus(statusType, message);
		setChanged();
		notifyObservers(status);
	}

	class ChannelSpecs implements Comparable<ChannelSpecs> {
		private final String channelDir;
		private final String channelFile;
		private final int channelId;

		public ChannelSpecs(final String dir, final String file, final int channelId) {
			this.channelDir = dir;
			this.channelFile = file;
			this.channelId = channelId;
		}

		@Override
		public int compareTo(Session.ChannelSpecs o) {
			final int BEFORE = -1;
			final int EQUAL = 0;
			final int AFTER = 1;

			if (this.equals(o))
				return EQUAL;
			if (this.channelId < o.channelId)
				return BEFORE;
			if (this.channelId > o.channelId)
				return AFTER;
			return EQUAL;
		}

		@Override
		public int hashCode() {
			int hash = 5;
			hash = 13 * hash + this.channelId;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			if (this.channelId != ((Session.ChannelSpecs) obj).channelId)
				return false;
			return true;
		}
	}
}
