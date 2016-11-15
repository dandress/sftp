package org.dla.nioftp.client;

import org.dla.nioftp.client.session.Session;
import org.dla.nioftp.client.session.channel.SftpChannel;
import org.dla.nioftp.client.session.ssh.packet.Packet;
import org.dla.threads.ThreadSource;
import java.util.concurrent.PriorityBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 * incoming
 *  Jan 31, 2013 7:47:04 AM
 */
public class IncomingQueue implements Runnable {

	protected static final Logger logger = LoggerFactory.getLogger(IncomingQueue.class);
	final private Session session;
	private boolean abort = false;
	private PriorityBlockingQueue<IncomingQueue.Message> messageQueue;
	private int messageOrder = 0;

	public IncomingQueue(final Session session) {
		this.session = session;
		messageQueue = new PriorityBlockingQueue<>(2);
	}

	public void postPacket(final Packet packet) {
		if (!abort) {
			int order = ++messageOrder;
			Message oMsg = new Message(packet, order);
			if (oMsg != null)
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

		Message(final Packet packet, final int order) {
			this.packet = packet;
			this.order = order;
		}

		void forward() {
			if (session != null) {
				session.handlePacket(packet);
				logger.trace("Forward session packet");
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
			if (this.order != ((IncomingQueue.Message) obj).order)
				return false;
			return true;
		}
	}
}
