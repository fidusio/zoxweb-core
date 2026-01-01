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

import java.io.IOException;
import org.zoxweb.shared.util.NVGenericMap;

/**
 * Interface for queue session management.
 * Provides methods for connecting to a queue, managing listeners,
 * and dispatching queue events.
 *
 * @author mnael
 * @see QueueEvent
 * @see QueueListener
 */
public interface QueueSession
    extends AutoCloseable
{

	/**
	 * Connects to the queue using the provided configuration.
	 *
	 * @param config the connection configuration
	 * @throws NullPointerException if config is null
	 * @throws IllegalArgumentException if config is invalid
	 * @throws IOException if connection fails
	 */
	void connect(NVGenericMap config)
			throws NullPointerException, IllegalArgumentException, IOException;

	/**
	 * Adds a consumer listener with a tag.
	 *
	 * @param tag the listener tag/identifier
	 * @param ql the queue listener to add
	 */
	void addListener(String tag, QueueListener<QueueEvent<?>> ql);

	/**
	 * Removes a consumer listener.
	 *
	 * @param ql the queue listener to remove
	 */
	void removeListener(QueueListener<QueueEvent<?>> ql);

	/**
	 * Dispatches a producer event to the queue.
	 *
	 * @param qe the queue event to dispatch
	 * @throws NullPointerException if the event is null
	 * @throws IllegalArgumentException if the event is invalid
	 * @throws IOException if dispatching fails
	 */
	void queueEvent(QueueEvent<?> qe)
			throws NullPointerException, IllegalArgumentException, IOException;

	/**
	 * Returns all registered listeners.
	 *
	 * @return array of all queue listeners
	 */
	QueueListener<QueueEvent<?>> [] getAllListeners();

	/**
	 * Returns the connection configuration.
	 *
	 * @return the configuration map
	 */
	NVGenericMap getConfig();

}