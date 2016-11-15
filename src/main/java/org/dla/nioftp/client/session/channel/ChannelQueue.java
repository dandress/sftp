package org.dla.nioftp.client.session.channel;

import org.dla.nioftp.client.session.ssh.packet.Packet;
import org.dla.threads.ThreadSource;
import java.util.concurrent.PriorityBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 *  Jan 31, 2013 7:47:04 AM
 */
public class ChannelQueue implements Runnable {

	protected static final Logger logger = LoggerFactory.getLogger(ChannelQueue.class);
	private boolean abort = false;
	private PriorityBlockingQueue<ChannelQueue.Message> messageQueue;
	private int messageOrder = 0;
	final SftpChannel channel;

	public ChannelQueue(final SftpChannel channel) {
		this.channel = channel;
		messageQueue = new PriorityBlockingQueue<>(2);
	}

	public void postPacket(final Packet packet) {
		if (!abort) {
			int order = ++messageOrder;
			Message oMsg = new Message(packet, order, channel);
			messageQueue.offer(oMsg);

			if (messageOrder >= Integer.MAX_VALUE)
				messageOrder = 0;
		}
	}

	@Override
	public void run() {
		while (abort == false) {
			try {
				Message m = messageQueue.take(); // blocks until there is something in the queue
				if (m != null && !abort) {
					m.forward();
				}
			} catch (InterruptedException ex) {
				logger.trace("Queue '{}' interrupted", Thread.currentThread().getName());
				break;
			}
		}
		logger.trace("Queue '{}' aborted", Thread.currentThread().getName());
	}

	public void abort() {
		this.abort = true;

		if (messageQueue != null) {
			for (Message msg : messageQueue) {
				msg.packet.clear();
				msg = null;
			}
			messageQueue.clear();
			messageQueue = null;
		}
	}

	class Message implements Comparable<Message> {

		final Packet packet;
		final int order;
		final SftpChannel channel;

		Message(final Packet packet, final int order, final SftpChannel channel) {
			this.packet = packet;
			this.order = order;
			this.channel = channel;
		}

		void forward() {
			if (channel != null) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						logger.trace("Forwarding packet to {}", channel.toString());
						channel.handlePacket(packet);
					}
				};
				ThreadSource.getInstance().executeThread(r, "incomingQueue-channel-" + ((SftpChannel) channel).getChannelId(), Thread.MAX_PRIORITY);
			}
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
			int hash = 3;
			hash = 19 * hash + this.order;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			if (this.order != ((ChannelQueue.Message) obj).order)
				return false;
			return true;
		}
	}
}
