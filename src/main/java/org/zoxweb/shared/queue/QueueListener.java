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

import java.util.EventListener;

import org.zoxweb.shared.util.GetName;

/**
 * Interface for queue event listeners.
 * Implementations receive and process incoming queue events.
 *
 * @param <E> the type of queue event this listener handles
 * @author mnael
 * @see QueueEvent
 * @see QueueSession
 * @see EventListener
 */
public interface QueueListener<E extends QueueEvent<?>>
    extends EventListener,
    		GetName
{
    /**
     * Processes an incoming queue event.
     *
     * @param event the event to process
     */
	public void incomingEvent(E event);

}