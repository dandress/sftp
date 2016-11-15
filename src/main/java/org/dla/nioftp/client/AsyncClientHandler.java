package org.dla.nioftp.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.dla.nioftp.Constants;
import org.dla.nioftp.client.session.ssh.packet.PacketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 * nioftp Jun 10, 2013 7:18:46 AM
 */
public class AsyncClientHandler implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(AsyncClientHandler.class);
	private ExecutorService executor = Executors.newFixedThreadPool(5);
	private AsynchronousChannelGroup syncGroup;
	private AsynchronousSocketChannel asyncChannel;
	private InetSocketAddress address;
	private final Client client;
	boolean outstandingRead = false;
	private boolean abort = false;

	public AsyncClientHandler(final Client client, final String connectionAddress, final int connectionPort) {
		this.client = client;
		address = new InetSocketAddress(connectionAddress, connectionPort);

	}

	void init() throws IOException, InterruptedException, ExecutionException {
		syncGroup = AsynchronousChannelGroup.withThreadPool(executor);
		asyncChannel = AsynchronousSocketChannel.open(syncGroup)
			.setOption(StandardSocketOptions.TCP_NODELAY, true);
		asyncChannel.connect(address).get();
	}

	void shutdown() {
		abort = true;
		try {
			if (!syncGroup.isShutdown())
				syncGroup.shutdown();
			if (!syncGroup.isTerminated())
				syncGroup.shutdownNow();
			syncGroup.awaitTermination(10, TimeUnit.SECONDS);
			executor.shutdown();
			address = null;
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (InterruptedException ex) {
			logger.info(ex.getMessage(), ex);
		}
	}

	@Override
	public void run() {
		while (!abort) {
			ByteBuffer writeBuffer = client.getOutgoingMessage();
			if (!abort && writeBuffer != null) {
				while (writeBuffer.hasRemaining()) {
					Future<Integer> integerFuture = asyncChannel.write(writeBuffer);
					try {
						Integer rslt = integerFuture.get();
						logger.trace("{} bytes writen", rslt);
					} catch (ExecutionException ex) {
						logger.error(ex.getCause().getMessage(), ex.getCause());
//						client.shutdown();
						client.getSession().shutdownNow();
						abort = true;
						break;
					} catch (InterruptedException ex) {
						logger.info(ex.getMessage());
					}
				}
			}
			if (!abort && !outstandingRead) {
				ByteBuffer incommingBuffer = ByteBuffer.allocate(Constants.capacity);
				outstandingRead = true;
				asyncChannel.read(incommingBuffer, incommingBuffer, new CompletionHandler<Integer, ByteBuffer>() {
					@Override
					public void completed(Integer result, ByteBuffer buffer) {
						outstandingRead = false;
						buffer.flip();
						try {
							client.readFromChannel(result, buffer);
							logger.trace("Bytes read: {}", result);
						} catch (PacketException | TransException ex) {
							logger.error(ex.getMessage(), ex);
						}
					}

					@Override
					public void failed(Throwable ex, ByteBuffer attachment) {
						outstandingRead = false;
						if (ex.getMessage() != null)
							logger.warn(ex.getMessage());
					}
				});
				if (asyncChannel.isOpen() == false) {
					abort = true;
					break;
				}
			} // if (!outstandingRead)

		}
		logger.debug("AsyncClientHandler has stopped");
	}
}
