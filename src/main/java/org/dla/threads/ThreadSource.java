package org.dla.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates and eases thread creation. Be sure to call setThreadGroupName before use!!
 *
 * @author Dennis Andress
 */
public class ThreadSource {

	private static ThreadSource INSTANCE = null;
	private static final Logger logger = LoggerFactory.getLogger(ThreadSource.class);
	private ExecutorService executorService = null;
	private final CtThreadFactory threadFactory;
	private final ThreadGroup threadGroup;
	private final String threadGroupName;

	private ThreadSource() {
		SequencedName sn = new SequencedName("tg_");
		threadGroupName = sn.generate();
		threadGroup = new ThreadGroup(threadGroupName);
		threadFactory = new CtThreadFactory(threadGroup);
//		executorService = Executors.newCachedThreadPool(threadFactory
		// handy replacement to make thread troubleshooting easier...
//		executorService = Executors.newFixedThreadPool(60, threadFactory);
	}

	public void initialize(final int poolSize) {
		if (executorService == null) {
			if (poolSize == 0)
				executorService = Executors.newCachedThreadPool(threadFactory);
			else
				executorService = Executors.newFixedThreadPool(poolSize, threadFactory);
		}
	}

	public static ThreadSource getInstance() throws IllegalArgumentException {
		if (INSTANCE == null) {
			INSTANCE = new ThreadSource();
		}
		return INSTANCE;
	}

	/**
	 * Starts a thread at Thread.NORM_PRIORITY. Note that 'name' is handy for troubleshooting, but makes no sense when
	 * used with a cachedThreadPool. Which is the way the code is written.
	 *
	 * Names are ignored in CtThreadPool. To use them the code in CtThreadFactory has to changed.
	 *
	 * @param runnable
	 * @param name
	 */
	public void executeThread(final Runnable runnable, final String name) {
		initialize(0);
		assert threadFactory != null;
		assert executorService != null;
		try {
			threadFactory.setName(name);
			logger.trace("Executing thread for Runnable {}", runnable.getClass().getName());
			if (!executorService.isShutdown())
				executorService.execute(runnable);
		} catch (RejectedExecutionException ex) {
			logger.warn(ex.getMessage());
		}
	}

	/**
	 * Starts a thread for a Callable. Thread starts at Thread.NORM_PRIORITY. Returns a CtFuture
	 *
	 * @param callable
	 * @param name
	 * @return
	 */
	public CtFuture executeThread(final Callable<String> callable, final String name) {
		initialize(0);
		assert threadFactory != null;
		assert executorService != null;
		if (executorService.isShutdown()) {
			logger.warn("ExecutorService is unavailable");
		}

		try {
			CtFuture<String> future = new CtFuture<>(callable);
			threadFactory.setName(name);
			logger.trace("Executing thread for Callable {}", callable.getClass().getName());
			if (!executorService.isShutdown()) {
				executorService.execute(future);
				return future;
			}
		} catch (RejectedExecutionException ex) {
			logger.warn(ex.getMessage());
		}
		return null;
	}

	/**
	 * Starts a thread at the priority passed
	 *
	 * @param runnable
	 * @param threadPriority A value of Thread.NORM_PRIORITY or similar
	 * @param name
	 */
	public void executeThread(final Runnable runnable, final String name, final int threadPriority) {
		initialize(0);
		assert threadFactory != null;
		assert executorService != null;
		try {
			threadFactory.setName(name);
			logger.trace("Executing thread for Runnable {}", runnable.getClass().getName());
			if (!executorService.isShutdown())
				executorService.execute(runnable);
		} catch (RejectedExecutionException ex) {
			logger.warn(ex.getMessage());
		}
	}

	/**
	 * Starts a thread at the priority passed. Returns a CtFuture
	 *
	 * @param callable
	 * @param threadPriority
	 * @param name
	 * @return
	 */
	public CtFuture executeThread(final Callable<String> callable, final String name, final int threadPriority) {
		initialize(0);
		assert threadFactory != null;
		assert executorService != null;
		if (executorService.isShutdown()) {
			logger.warn("ExecutorService is unavailable");
		}

		try {
			CtFuture<String> future = new CtFuture<>(callable);
			threadFactory.setName(name);
			logger.trace("Executing thread for Callable {}", callable.getClass().getName());
			if (!executorService.isShutdown()) {
				executorService.execute(future);
				return future;
			}
		} catch (RejectedExecutionException ex) {
			logger.warn(ex.getMessage());
		}
		return null;
	}

	/**
	 * Calls interrupt() on {@link #threadGroup} and shuts {@link #executorService} down.
	 * Finally sets INSTANCE to null so it will be recreated on the next call to {@link #getInstance() }
	 *
	 *
	 */
	public void interrupt() {
		if (executorService != null)
			try {
				executorService.shutdown();
				while (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
					logger.trace("Waiting for termination.  {}", threadGroup.activeCount());
					executorService.shutdownNow();
				}
			} catch (InterruptedException ex) {
				logger.trace("ThreadSource is done");
			}
		logger.debug("executor is terminated: {}", executorService.isTerminated());
		INSTANCE = null;
	}

	/**
	 * At one time this had to be called before use. Now it does nothing
	 *
	 * @param name
	 * @deprecated
	 */
	public static void setThreadGroupName(final String name) {
	}
}
