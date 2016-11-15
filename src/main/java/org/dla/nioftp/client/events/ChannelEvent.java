package org.dla.nioftp.client.events;

import org.dla.nioftp.client.session.channel.SftpChannel;
import java.util.EventObject;

/**
 *
 * @author Dennis Andress
 *
 * 
 * Jan 15, 2013 7:59:02 AM
 */
public class ChannelEvent extends EventObject {

	final public ChannelEventType eventType;
	final public String fileName;
	final public String errorText;

	public ChannelEvent(final SftpChannel source, final ChannelEventType type, final String fileName) {
		super(source);
		this.eventType = type;
		this.fileName = fileName;
		this.errorText = null;
	}
	public ChannelEvent(final SftpChannel source, final ChannelEventType type, final String fileName, final String errorText) {
		super(source);
		this.eventType = type;
		this.fileName = fileName;
		this.errorText = errorText;
	}
}
