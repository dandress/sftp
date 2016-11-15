package org.dla.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dennis
 *
 * threading
 * Oct 29, 2013 9:18:27 AM
 * @param <T>
 */
public class CallableEngine<T> {
	private static final Logger logger = LoggerFactory.getLogger(CallableEngine.class);
	private ExecutorService executorService = null;
	private final CtThreadFactory threadFactory;
	private final ThreadGroup threadGroup;
	private final String threadGroupName;

	public CallableEngine() {
		SequencedName sn = new SequencedName("tg_");
		threadGroupName = sn.generate();
		threadGroup = new ThreadGroup(threadGroupName);
		threadFactory = new CtThreadFactory(threadGroup);
	}public void initialize(final int poolSize) {
		if (executorService == null) {
			if (poolSize == 0)
				executorService = Executors.newCachedThreadPool(threadFactory);
			else
				executorService = Executors.newFixedThreadPool(poolSize, threadFactory);
		}
	}

	public void shutdown() {
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
	}

	public FutureTask<T> executeThread(final Callable<T> callable, final String name) {
		initialize(0);
		assert threadFactory != null;
		assert executorService != null;
		if (executorService.isShutdown()) {
			logger.warn("ExecutorService is unavailable");
		}

		try {
			FutureTask<T> future = new FutureTask<>(callable);
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



}
