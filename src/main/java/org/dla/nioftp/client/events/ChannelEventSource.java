package org.dla.nioftp.client.events;

import org.dla.nioftp.client.session.channel.SftpChannel;
import org.dla.threads.ThreadSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Dennis Andress
 *
 *  Jan 15, 2013 8:05:49 AM
 */
public class ChannelEventSource {

	private final List<ChannelEventListener> listeners = new ArrayList<>();

	public ChannelEventSource() {
	}

	public void addEventListener(final ChannelEventListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeEventListener(final ChannelEventListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	protected void fireEvent(final SftpChannel source, final ChannelEventType type, final String fileName) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				synchronized (listeners) {
					ArrayList<ChannelEventListener> copy = new ArrayList<>(listeners);
					ChannelEvent evt = new ChannelEvent(source, type, fileName);
					Iterator<ChannelEventListener> itr = copy.iterator();
					while (itr.hasNext()) {
						itr.next().handleEvent(evt);
					}
				}
			}
		};
		ThreadSource.getInstance().executeThread(r, "ClientEventSource");
	}

	protected void fireEvent(final SftpChannel source, final ChannelEventType type, final String fileName, final String errorText) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				synchronized (listeners) {
					ArrayList<ChannelEventListener> copy = new ArrayList<>(listeners);
					ChannelEvent evt = new ChannelEvent(source, type, fileName, errorText);
					Iterator<ChannelEventListener> itr = copy.iterator();
					while (itr.hasNext()) {
						itr.next().handleEvent(evt);
					}
				}
			}
		};
		ThreadSource.getInstance().executeThread(r, "ClientEventSource");
	}
}
