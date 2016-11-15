package org.dla.threads;

import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Dennis Andress
 *
 * nioftp
 * Feb 19, 2013 9:00:28 AM
 */
class CtThreadFactory implements ThreadFactory {

	private static final Logger logger = LoggerFactory.getLogger(CtThreadFactory.class);
	private String name = "CtThread";
	final ThreadGroup threadGroup;

	public CtThreadFactory(final ThreadGroup threadGroup) {
		this.threadGroup = threadGroup;
	}

	@Override
	public Thread newThread(Runnable r) {
		// uncomment for troubleshooting. Also, change lines 26 and 28 (approx) in ThreadSource so a caching thread pool isn't used
//		Thread t = new Thread(threadGroup, r, name);
		Thread t = new Thread(threadGroup, r);
		t.setPriority(Thread.NORM_PRIORITY);
		t.setDaemon(false);
		logger.trace("{} created. Thread count: {}", t.getName(), (threadGroup.activeCount() + 1));
		return t;
	}

	public Thread newThread(Runnable r, final int threadPriority) {
		// uncomment for troubleshooting. Also, change lines 26 and 28 (approx) in ThreadSource so a caching thread pool isn't used
//		Thread t = new Thread(threadGroup, r, name);
		Thread t = new Thread(threadGroup, r);
		t.setPriority(threadPriority);
		t.setDaemon(false);
		logger.trace("{} created. Thread count: {}", t.getName(), (threadGroup.activeCount() + 1));
		return t;
	}

	/** Sets a name to be used for all threads created. Useful for troubleshooting. */
	public void setName(String name) {
		this.name = name;
	}
}
