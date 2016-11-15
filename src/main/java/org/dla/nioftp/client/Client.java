package org.dla.nioftp.client;

import org.dla.nioftp.Constants;

import static org.dla.nioftp.client.BufferReader.DEFAULT_CHARSET_NAME;

import org.dla.nioftp.client.session.Session;
import org.dla.nioftp.client.session.ssh.cyphony.KeyExchange;
import org.dla.nioftp.client.session.ssh.cyphony.PacketAlgorithms;
import org.dla.nioftp.client.session.ssh.packet.AlgorithmStrings;
import org.dla.nioftp.client.session.ssh.packet.PacketException;
import org.dla.threads.ThreadSource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 *  Jan 22, 2013 9:34:49 AM
 */
public class Client {

	private static final Logger logger = LoggerFactory.getLogger(Client.class);
	protected Charset charset = null;
	public final String charsetName = BufferReader.DEFAULT_CHARSET_NAME;
	private PriorityBlockingQueue<Message> sendQueue;
	private int messageOrder = 0;
	private BufferReader bufferReader;
	private String serverVersionId = null;
	private Session session;
	private PacketAlgorithms packetAlgos = null;
	private int out_sequence_no = 0;
	private AsyncClientHandler asyncHandler;
	private int nextChannelId = 100;
	private static int clientId = 1;
	private final Observer observer;
	private boolean isRunning = false;

	public Client(final Observer observer) {
		this.observer = observer;
		sendQueue = new PriorityBlockingQueue<>(4);
	}

	public void init(final String destinationAddress, final int destinationPort, final String userName, final String password)
			throws IOException, InterruptedException, ExecutionException {
		logger.info("Creating new client for: {}", destinationAddress);

		asyncHandler = new AsyncClientHandler(this, destinationAddress, destinationPort);
		asyncHandler.init();
		ThreadSource.getInstance().executeThread(asyncHandler, "AsyncClientHandler", Thread.MAX_PRIORITY);

		this.session = new Session(this, observer);
		session.init(userName, password, clientId++);
		this.bufferReader = new BufferReader(this);
		isRunning = true;
	}

	public void createPacketAlgos(final KeyExchange keyExchange, final AlgorithmStrings algorithmStrings) throws PacketException {
		logger.debug("Creating PacketAlgorithms");
		packetAlgos = new PacketAlgorithms(keyExchange, algorithmStrings);
		packetAlgos.init();
	}

	/**
	 * Closes this connection, the Session and all SftpChannels. Runs as a Callable thread to allow for
	 * the use of sleep(), to give time to any packets and events to run their course. Callable was used
	 * instead of Runable to provide assurance that everything is shutdown when this method returns.
	 *
	 * @return 1 if successful. -1 if an Exception occurred.
	 * @throws IOException
	 */
	public int shutdown() {
		if (isRunning) {
			isRunning = false;
			Callable r = new Callable<Integer>() {
				@Override
				public Integer call() {
					try {
						if (session != null)
							session.shutdown();
						if (asyncHandler != null)
							asyncHandler.shutdown();
						if (bufferReader != null)
							bufferReader.shutdown();
						if (sendQueue != null) {
							for (Message msg : sendQueue) {
								msg.bbuffer.clear();
								msg = null;
							}
							sendQueue.clear();
							sendQueue = null;
						}
						bufferReader = null;
						asyncHandler = null;
						if (packetAlgos != null)
							packetAlgos.clear();
						packetAlgos = null;
						session = null;
						Thread.sleep(2000);
						ThreadSource.getInstance().interrupt();
						logger.info("shutdown completed");
						return 1;
					} catch (InterruptedException ex) {
						logger.debug(ex.getMessage());
					}
					return -1;
				}
			};
			FutureTask<Integer> future = ThreadSource.getInstance().executeThread(r, "ClientShutdown");
			try {
				if (future != null)
					return future.get();
				else
					return -1;
			} catch (ExecutionException ex) {
				logger.error(ex.getMessage(), ex);
				return -1;
			} catch (InterruptedException ex) {
				logger.warn("InterruptedException");
				return -1;
			}
		} else
			return 0;
	}

	void readFromChannel(final Integer bytesRead, final ByteBuffer readBuffer) throws PacketException, TransException {
		ByteBuffer tmpReadBuffer = null;
		if (bytesRead <= 0) {
			logger.warn("Read {} bytes. Connection is closed", bytesRead);
			if (session != null)
				session.shutdownNow();
			if (bufferReader != null)
				bufferReader.shutdown();
			if (asyncHandler != null)
				asyncHandler.shutdown();
			if (sendQueue != null) {
				for (Message msg : sendQueue) {
					msg.bbuffer.clear();
					msg = null;
				}
				sendQueue.clear();
				sendQueue = null;
			}
			bufferReader = null;
			asyncHandler = null;
			return;
		}

		if (packetAlgos != null)
			tmpReadBuffer = bufferReader.decrypt(readBuffer);
		else
			tmpReadBuffer = readBuffer.duplicate();

		if (tmpReadBuffer == null) {
			session.shutdownNow();
			return;
		}

		if (serverVersionId == null)
			initKex(readBuffer, bytesRead);
		else
			bufferReader.readIncommingBuffer(readBuffer, tmpReadBuffer);
	}

	ByteBuffer getOutgoingMessage() {
		try {
			Message msg = sendQueue.poll(100, TimeUnit.MILLISECONDS);  //  100, TimeUnit.MILLISECONDS
			if (msg != null)
				return msg.bbuffer;
		} catch (InterruptedException ex) {
		}
		return null;
	}

	private void initKex(final ByteBuffer readBuffer, int bytesRead) throws TransException, PacketException {
		logger.debug("initKex");
		// OpenSSH will send its versionId string and SSH_KEXINIT upon connection,
		// rather then waiting for the client's SSH_KEXINIT like it should
		serverVersionId = decodeReadBuffer(readBuffer);
		logger.info("Server says it is: {}", serverVersionId);
		if (bytesRead > 3 || (serverVersionId.charAt(0) == 'S' && serverVersionId.charAt(1) == 'S'
				&& serverVersionId.charAt(2) == 'H' && serverVersionId.charAt(3) == '-')) {
		} else
			throw new TransException("Server's versionId does not begin with SSH-");
		// and not be of the versino 1.5 variety
		if (bytesRead < 7 || (serverVersionId.charAt(4) == '1' && serverVersionId.charAt(6) != '9'))
			throw new TransException("Looks like a SSH-1.5 server. Got to bail1!");

		// send our versionId string
		send(Constants.ourVersionId + "\r\n");
		bufferReader.startIncomingQueue();
		session.sendKexInit();
	}

	/**
	 * Sends plain text. Only used for the initial contact
	 *
	 * @param outgoingText
	 */
	private void send(final String outgoingText) {
		try {
			byte[] data = outgoingText.getBytes(DEFAULT_CHARSET_NAME);
			Client.Message oMsg = new Client.Message(data, ++messageOrder);
			if (oMsg != null)
				sendQueue.offer(oMsg);

			if (messageOrder >= Integer.MAX_VALUE)
				messageOrder = 0;
		} catch (UnsupportedEncodingException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	/**
	 * Places a byte array in {@code sendQueue} from where it will be polled by AsyncClientHandler
	 * and send to its destination.
	 *
	 * @param data The data to be sent. Can be, and usually is, encrypted.
	 */
	public void postMessage(final byte[] data) {
		if (data == null)
			return;
		logger.trace("Message posted to sendQueue. {}", data.length);
		out_sequence_no++;	// out_sequence_no has to start at zero for the first encrypted packet
		Client.Message oMsg = new Client.Message(data, ++messageOrder);
		if (oMsg != null && sendQueue != null)
			sendQueue.offer(oMsg);

		if (messageOrder >= Integer.MAX_VALUE)
			messageOrder = 0;
	}

	/**
	 * Call to transfer a file. Causes a new SftpChannel to be created and then sends a file across it
	 *
	 * @param fileSourceDirectory The directory to find the file in
	 * @param fileName            The file to send. This name will also be used on the destination server.
	 * @throws PacketException If the was a problem creating or sending the Packet
	 */
	public void sendFile(final String fileSourceDirectory, final String fileName) throws PacketException {
		logger.debug("Request to send file: {}...", fileName);
		if (session != null)
			session.createNewChannel(nextChannelId++, fileSourceDirectory, fileName);
		else
			logger.warn("Attempt to send file {} before Session was created");
	}

	/**
	 * Returns the size() of Sessions's channels map
	 */
	public int getOpenChannelCount() {
		if (session != null && session.getChannels() != null)
			return session.getChannels().size();
		else
			return 0;
	}

	/**
	 * Decodes, as in character set - not encryption - a buffer containing text. Only
	 * used for the first contact with the server.
	 *
	 * @param readBuffer
	 * @return The decoded text string.
	 */
	private String decodeReadBuffer(ByteBuffer readBuffer) {
		if (!readBuffer.hasRemaining())
			return null;
		CharsetDecoder decoder = Charset.forName(DEFAULT_CHARSET_NAME).newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPORT);
		CharBuffer charBuffer = CharBuffer.allocate(Constants.capacity);
		CoderResult decodeRslt = decoder.decode(readBuffer, charBuffer, true);
		charBuffer.flip();
		return charBuffer.toString().trim();
	}

	public PacketAlgorithms getPacketAlgos() {
		return packetAlgos;
	}

	public String getServerVersionId() {
		return serverVersionId;
	}

	public int getOut_sequence_no() {
		return out_sequence_no;
	}

	public Session getSession() {
		return session;
	}

	class Message implements Comparable<Message> {

		final ByteBuffer bbuffer;
		final int order;

		Message(final byte[] data, final int order) {
			bbuffer = ByteBuffer.wrap(data);
			this.order = order;
			logger.trace("{} bytes added to sendQueue", data.length);
		}

		@Override
		public int compareTo(Message o) {
			final int BEFORE = -1;
			final int EQUAL = 0;
			final int AFTER = 1;

			if (this.equals(o))
				return EQUAL;
			if (this.order < o.order)
				return BEFORE;
			if (this.order > o.order)
				return AFTER;
			return EQUAL;
		}

		@Override
		public int hashCode() {
			int hash = 5;
			hash = 13 * hash + this.order;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			if (this.order != ((Message) obj).order)
				return false;
			return true;
		}
	}
}