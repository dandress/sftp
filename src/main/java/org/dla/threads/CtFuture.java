package org.dla.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** a direct override of FutureTask. Probably doesn't add any value beyond logging
 *
 * @author Dennis Andress
 *
 * Feb 19, 2013 9:27:50 AM
 */
public class CtFuture<String> extends  FutureTask<String> {
	private static final Logger logger = LoggerFactory.getLogger(CtFuture.class);

	public CtFuture(Callable<String> callable) {
		super(callable);
	}

	/**
	 * {@inheritDoc }
	 * @return
	 */
	@Override
	public boolean isDone() {
		return super.isDone();
	}

	/** {@inheritDoc }
	 */
	@Override
	public void done() {
		logger.debug("FutureTask completed");
	}

	/** {@inheritDoc }
	 *
	 * @param mayInterruptIfRunning
	 * @return
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return super.cancel(mayInterruptIfRunning);
	}

	/** {@inheritDoc }
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Override
	public String get() throws InterruptedException, ExecutionException {
		return super.get();
	}

	/** {@inheritDoc }
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException \
	 */
	@Override
	public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return super.get(timeout, unit);
	}




}
