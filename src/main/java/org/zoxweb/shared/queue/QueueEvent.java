/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.queue;

import java.util.Date;
import java.util.EventObject;

import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.ToBytes;

/**
 * Abstract base class for queue events.
 * Represents a message in a queue system with properties such as
 * content, priority, persistence, correlation ID, and reply-to address.
 *
 * @param <V> the type of the event content
 * @author mnael
 * @see EventObject
 * @see ToBytes
 * @see QueueListener
 */
@SuppressWarnings("serial")
public abstract class QueueEvent<V>
    extends EventObject
    implements ToBytes
{

	/** The event content/payload */
	protected V content;
	private boolean persistent;
	private int priority;
	private String correlationID;
	private String messageID;
	private String replyTo;
	private Date timestamp;

	/**
	 * Constructs a QueueEvent with basic properties.
	 *
	 * @param source the event source
	 * @param persistent whether the message should be persisted
	 * @param priority the message priority (must be >= 0)
	 * @param content the message content
	 */
	public QueueEvent(Object source, boolean persistent, int priority, V content)
    {
		this(source, persistent, priority, null, null, null, content);
	}

	/**
	 * Constructs a QueueEvent with all properties including content.
	 *
	 * @param source the event source
	 * @param persistent whether the message should be persisted
	 * @param priority the message priority (must be >= 0)
	 * @param timestamp the message timestamp
	 * @param correlationID the correlation ID for request/response matching
	 * @param replyTo the reply-to address
	 * @param content the message content
	 * @throws NullPointerException if content is null
	 */
	public QueueEvent(Object source, boolean persistent, int priority, Date timestamp, String correlationID, String replyTo, V content)
    {
		this(source, persistent, priority, timestamp, correlationID, replyTo);
		SUS.checkIfNulls("Null content", content);
		this.content = content;
	}

	/**
	 * Constructs a QueueEvent without content.
	 *
	 * @param source the event source
	 * @param persistent whether the message should be persisted
	 * @param priority the message priority (must be >= 0)
	 * @param timestamp the message timestamp
	 * @param correlationID the correlation ID for request/response matching
	 * @param replyTo the reply-to address
	 */
	public QueueEvent(Object source, boolean persistent, int priority, Date timestamp, String correlationID, String replyTo)
    {
		super(source);
		setPersistent(persistent);

		setPriority(priority);
		setCorrelationID(correlationID);
		setReplyTo(replyTo);
		setTimestamp(timestamp);
	}

	/**
	 * Returns the event content.
	 *
	 * @param <C> the expected content type
	 * @return the content cast to the expected type
	 */
	@SuppressWarnings("unchecked")
	public <C> C getContent()
	{
		return (C) content;
	}

	/**
	 * Returns whether the message should be persisted.
	 *
	 * @return true if persistent
	 */
	public boolean isPersistent()
	{
		return persistent;
	}

	/**
	 * Sets whether the message should be persisted.
	 *
	 * @param persistent true to persist the message
	 */
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	/**
	 * Returns the message priority.
	 *
	 * @return the priority value
	 */
	public int getPriority()
	{
		return priority;
	}

	/**
	 * Sets the message priority.
	 *
	 * @param priority the priority value (must be >= 0)
	 * @throws IllegalArgumentException if priority is negative
	 */
	public void setPriority(int priority) {
		if (priority < 0)
			throw new IllegalArgumentException("Invalid priority: " + priority);
		this.priority = priority;
	}

	/**
	 * Returns the correlation ID for request/response matching.
	 *
	 * @return the correlation ID
	 */
	public String getCorrelationID()
	{
		return correlationID;
	}

	/**
	 * Sets the correlation ID for request/response matching.
	 *
	 * @param correlationID the correlation ID to set
	 */
	public void setCorrelationID(String correlationID) {
		this.correlationID = correlationID;
	}

	/**
	 * Returns the reply-to address.
	 *
	 * @return the reply-to address
	 */
	public String getReplyTo()
	{
		return replyTo;
	}

	/**
	 * Sets the reply-to address.
	 *
	 * @param replyTo the reply-to address
	 */
	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	/**
	 * Returns the message timestamp.
	 *
	 * @return the timestamp
	 */
	public Date getTimestamp()
	{
		return timestamp;
	}

	/**
	 * Sets the message timestamp.
	 *
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Returns the message ID.
	 *
	 * @return the message ID
	 */
	public String getMessageID() {
		return messageID;
	}

	/**
	 * Sets the message ID.
	 *
	 * @param messageID the message ID to set
	 */
	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}

}