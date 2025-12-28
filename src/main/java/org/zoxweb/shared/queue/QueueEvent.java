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

@SuppressWarnings("serial")
public abstract class QueueEvent<V>
    extends EventObject
    implements ToBytes
{
	
	protected V content;
	private boolean persistent;
	private int priority;
	private String correlationID;
	private String messageID;
	private String replyTo;
	private Date timestamp;
    
	
	
	
	public QueueEvent(Object source, boolean persistent, int priority, V content)
    {
		this(source, persistent, priority, null, null, null, content);
	}
	
	
	public QueueEvent(Object source, boolean persistent, int priority, Date timestamp, String correlationID, String replyTo, V content)
    {
		this(source, persistent, priority, timestamp, correlationID, replyTo);
		SUS.checkIfNulls("Null content", content);
		this.content = content;
	}
	
	public QueueEvent(Object source, boolean persistent, int priority, Date timestamp, String correlationID, String replyTo)
    {
		super(source);
		setPersistent(persistent);
		
		setPriority(priority);
		setCorrelationID(correlationID);
		setReplyTo(replyTo);
		setTimestamp(timestamp);
	}
	
	@SuppressWarnings("unchecked")
	public <C> C getContent()
	{
		return (C) content;
	}
	
	public boolean isPersistent()
	{
		return persistent;
	}
	
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}


	public int getPriority()
	{
		return priority;
	}
	
	public void setPriority(int priority) {
		if (priority < 0)
			throw new IllegalArgumentException("Invalid priority: " + priority);
		this.priority = priority;
	}


	public String getCorrelationID()
	{
		return correlationID;
	}
	
	public void setCorrelationID(String correlationID) {
		this.correlationID = correlationID;
	}


	public String getReplyTo()
	{
		return replyTo;
	}
	
	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}


	public Date getTimestamp()
	{
		return timestamp;
	}


	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}


	public String getMessageID() {
		return messageID;
	}


	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}

	
	
}