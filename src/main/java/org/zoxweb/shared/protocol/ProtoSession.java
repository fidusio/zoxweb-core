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
package org.zoxweb.shared.protocol;

import org.zoxweb.shared.io.CloseableType;
import org.zoxweb.shared.util.BaseSubjectID;
import org.zoxweb.shared.util.GetNVProperties;

import java.util.Set;
import java.util.function.Supplier;

/**
 * A protocol-level session: the bridge between a transport connection (an HTTP
 * keep-alive connection, a WebSocket, a tunnel, ...) and the application-level
 * session that owns it (a security subject, a request-scoped context, a
 * long-running job, ...).
 *
 * <p>A {@code ProtoSession} wraps an underlying session object of type {@code S}
 * (see {@link #getSession()}), identifies the subject it belongs to
 * ({@link BaseSubjectID#getSubjectID()}), carries a free-form property bag
 * ({@link GetNVProperties#getProperties()}) and is itself closeable
 * ({@link CloseableType}). Implementations decide what "the session" and "the
 * subject" concretely are; the interface only fixes the lifecycle contract.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li><b>Attach / detach</b> - {@link #attach()} binds the session to the
 *       current execution context (typically the worker thread that is about to
 *       process a request) and {@link #detach()} unbinds it. They are meant to be
 *       called in pairs around each unit of work that runs on behalf of the
 *       session.</li>
 *   <li><b>Close decision</b> - when the transport layer believes a unit of work
 *       is finished (for example an HTTP request handler has returned), it asks
 *       {@link #canClose()} before releasing the connection. Application code can
 *       veto that release by registering <em>close monitors</em>
 *       ({@link #addCloseMonitor(Supplier)}): a session is closeable only while
 *       every registered monitor answers {@code true}. This is how a handler that
 *       answers asynchronously keeps its connection alive until the response has
 *       actually been written.</li>
 *   <li><b>Close</b> - {@link CloseableType#close()} tears the session down and
 *       closes everything in {@link #getAutoCloseables()} (the connection, its
 *       buffers, any resources parked on the session). After {@code close()},
 *       {@link CloseableType#isClosed()} is {@code true} and {@link #canClose()}
 *       must return {@code true} regardless of the monitors.</li>
 * </ol>
 *
 * <p>{@link #canClose()} is advisory: a caller that decides to shut a session down
 * (server shutdown, idle reaper, fatal error) may invoke {@code close()} without
 * consulting it.
 *
 * @param <S> the type of the underlying session object returned by {@link #getSession()}
 * @param <T> the subject identifier type, see {@link BaseSubjectID}
 * @author mnael
 * @see GetNVProperties
 * @see CloseableType
 * @see BaseSubjectID
 */
public interface ProtoSession<S, T>
    extends GetNVProperties, CloseableType, BaseSubjectID<T>
{
    /**
     * Returns the underlying session object this {@code ProtoSession} wraps.
     * What that object is depends on the implementation (a security framework
     * session, a request context, or even the implementation itself).
     *
     * @return the session object, never {@code null}
     */
    S getSession();

    /**
     * Tells whether the session may be closed right now.
     *
     * <p>The answer is {@code true} if the session is already closed, or if every
     * close monitor registered via {@link #addCloseMonitor(Supplier)} currently
     * returns {@code true}. A single monitor returning {@code false} makes the
     * session non-closeable. With no monitors registered the session is always
     * closeable.
     *
     * <p>The result is advisory: the caller is not required to obey it and may
     * invoke {@link #close()} regardless. It exists so that cooperative callers
     * (such as a keep-alive connection manager) can defer releasing the transport
     * while asynchronous work is still in flight.
     *
     * @return {@code true} if the session is closed or can be closed now
     */
    boolean canClose();

    /**
     * Returns the resources whose lifetime is tied to this session.
     *
     * <p>The set is live and mutable: callers park resources here (the protocol
     * handler, buffers, streams, ...) and the implementation closes all of them
     * when {@link #close()} is invoked. Closing is best-effort and null-tolerant;
     * a failure to close one entry does not prevent the others from being closed.
     *
     * @return the live set of {@link AutoCloseable} resources owned by this session,
     *         never {@code null}
     */
    Set<AutoCloseable> getAutoCloseables();

    /**
     * Registers a close monitor that participates in the {@link #canClose()}
     * decision.
     *
     * <p>A monitor is a {@link Supplier} that returns {@code true} when, from its
     * point of view, the session may be closed, and {@code false} while it still
     * needs the session (and the underlying connection) to stay open. Monitors are
     * consulted on every {@link #canClose()} call, so the supplier must be cheap,
     * side-effect free and safe to call from any thread. It must never return
     * {@code null}.
     *
     * <p>Typical use: an endpoint that responds asynchronously registers a monitor
     * that flips to {@code true} once the response has been written, so the server
     * does not reset the protocol handler and close the socket as soon as the
     * endpoint method returns.
     *
     * <p>Monitors are only ever consulted while the session is open; once the
     * session is closed {@link #canClose()} returns {@code true} unconditionally.
     *
     * @param closeMonitor the monitor to add; {@code null} is ignored
     */
    void addCloseMonitor(Supplier<Boolean> closeMonitor);

    /**
     * Unregisters a close monitor previously added with
     * {@link #addCloseMonitor(Supplier)}, so it no longer takes part in the
     * {@link #canClose()} decision. The same instance (by equality) that was
     * registered must be passed.
     *
     * @param closeMonitor the monitor to remove; {@code null} is ignored
     */
    void removeCloseMonitor(Supplier<Boolean> closeMonitor);

    /**
     * Attaches the session to the current execution context, typically the
     * calling thread, so that code running in that context can resolve the
     * session (and its subject) implicitly. Implementations may also use this
     * hook to refresh the session's last-access time.
     *
     * <p>Should be paired with {@link #detach()} once the unit of work is done.
     *
     * @return {@code true} if the session was attached successfully
     */
    boolean attach();

    /**
     * Detaches the session from the current execution context, undoing
     * {@link #attach()}. Safe to call even if the session was never attached
     * to this context.
     *
     * @return {@code true} if the session was detached successfully
     */
    boolean detach();
}
