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
package org.zoxweb.server.net;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.common.ConnectionCallback;
import org.zoxweb.server.net.common.SKHandler;
import org.zoxweb.server.net.common.TCPSessionCallback;
import org.zoxweb.server.net.common.UDPSessionCallback;
import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.DateUtil;
import org.zoxweb.shared.data.events.BaseEventObject;
import org.zoxweb.shared.data.events.EventListenerManager;
import org.zoxweb.shared.data.events.IPAddressEvent;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.net.DNSResolverInt;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.net.SharedNetUtil;
import org.zoxweb.shared.security.SecurityStatus;
import org.zoxweb.shared.task.ConsumerCallback;
import org.zoxweb.shared.task.ScheduledAttachment;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.Const.TimeInMillis;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.*;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance Non-blocking I/O (NIO) socket multiplexer that manages multiple server and client
 * connections using Java NIO's Selector pattern.
 *
 * <p>This class provides a unified interface for managing TCP server sockets, TCP client sockets,
 * and UDP datagram sockets. It uses a single-threaded selector loop combined with an executor
 * for concurrent request processing, enabling efficient handling of thousands of simultaneous
 * connections.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><b>Protocol Factory Pattern:</b> Uses {@link ProtocolFactory} to create protocol-specific
 *       handlers for each connection, enabling support for multiple protocols on different ports.</li>
 *   <li><b>Security Filtering:</b> Integrates with {@link org.zoxweb.server.net.InetFilterRulesManager}
 *       for IP-based access control on incoming connections.</li>
 *   <li><b>Attack Monitoring:</b> Tracks and logs rejected connections with rate statistics for
 *       security analysis and intrusion detection.</li>
 *   <li><b>Event System:</b> Dispatches {@link IPAddressEvent} events for blocked connections,
 *       enabling integration with external security systems.</li>
 *   <li><b>Connection Monitoring:</b> Uses {@link NIOChannelMonitor} for timeout detection on
 *       pending client connections.</li>
 *   <li><b>Statistics:</b> Collects metrics including total connections, select calls, attack
 *       counts, and average processing times.</li>
 * </ul>
 *
 * <h2>Threading Model:</h2>
 * <p>The selector loop runs on a dedicated thread named "NIO-SOCKET". When an executor is provided,
 * I/O processing is offloaded to the executor's thread pool. Selection key interest ops are
 * temporarily disabled during processing to prevent concurrent access issues.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * Executor executor = TaskUtil.defaultTaskProcessor();
 * TaskSchedulerProcessor scheduler = TaskUtil.defaultTaskScheduler();
 * NIOSocket nioSocket = new NIOSocket(executor, scheduler);
 *
 * // Add a TCP server
 * nioSocket.addServerSocket(8080, 50, new MyProtocolFactory());
 *
 * // Add a UDP server
 * nioSocket.addDatagramSocket(new InetSocketAddress(9090), new MyUdpProtocolFactory());
 *
 * // Cleanup
 * nioSocket.close();
 * }</pre>
 *
 * @author javaconsigliere@gmail.com
 * @see ProtocolFactory
 * @see ProtocolHandler
 * @see SelectorController
 * @see NIOChannelMonitor
 */
public class NIOSocket
        implements Runnable, DaemonController, Closeable {
    public static final LogWrapper logger = new LogWrapper(NIOSocket.class).setEnabled(false);
    private final AtomicBoolean live = new AtomicBoolean(true);
    private final SelectorController selectorController;
    private final Executor executor;
    private final TaskSchedulerProcessor taskSchedulerProcessor;
    private final AtomicLong connectionCount = new AtomicLong(0);


    private long selectedCountTotal = 0;
    private long statLogCounter = 0;
    private long attackTotalCount = 0;
    private final long startTime = System.currentTimeMillis();
    private EventListenerManager<BaseEventObject<?>, ?> eventListenerManager = null;
    private final RateCounter callsCounter = new RateCounter("nio-calls-counter");


    /**
     * Creates a new NIOSocket and starts the selector loop thread.
     *
     * <p>The constructor initializes the NIO Selector and immediately starts the selector
     * loop on a dedicated thread named "NIO-SOCKET". The socket is ready to accept
     * server and client socket registrations after construction.</p>
     *
     * @param exec the executor for offloading I/O processing; if null, processing occurs
     *             on the selector thread (not recommended for production use)
     * @param tsp  the task scheduler for connection timeout monitoring and delayed operations;
     *             used by {@link NIOChannelMonitor} to detect stale pending connections
     * @throws IOException if the Selector cannot be opened
     */
    public NIOSocket(Executor exec, TaskSchedulerProcessor tsp) throws IOException {
        logger.getLogger().info("Executor: " + exec);
        selectorController = new SelectorController(Selector.open());
        this.executor = exec;
        this.taskSchedulerProcessor = tsp;

        TaskUtil.startRunnable(this, "NIO-SOCKET");
    }

    /**
     * Creates and registers a TCP server socket on the specified address.
     *
     * <p>This method opens a new {@link ServerSocketChannel}, binds it to the specified
     * address and port, and registers it with the selector for accepting incoming connections.
     * When a connection is accepted, a new {@link ProtocolHandler} instance is created using
     * the provided factory.</p>
     *
     * @param sa      the socket address to bind to (IP address and port)
     * @param backlog the maximum number of pending connections in the listen queue;
     *                see {@link ServerSocketChannel#bind(java.net.SocketAddress, int)}
     * @param psf     the protocol factory for creating handlers for accepted connections
     * @return the SelectionKey associated with the server socket channel
     * @throws IOException          if the server socket cannot be opened or bound
     * @throws NullPointerException if sa or psf is null
     */
    public SelectionKey addServerSocket(InetSocketAddress sa, int backlog, ProtocolFactory<?> psf) throws IOException {
        SUS.checkIfNulls("Null values", sa, psf);
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(sa, backlog);
        return addServerSocket(ssc, psf);
    }

    /**
     * Registers an existing ServerSocketChannel with the selector.
     *
     * <p>Use this method when you need to configure the ServerSocketChannel before
     * registration, such as setting custom socket options. The channel must already
     * be bound to an address before calling this method.</p>
     *
     * @param ssc the pre-configured and bound server socket channel
     * @param psf the protocol factory for creating handlers for accepted connections
     * @return the SelectionKey associated with the server socket channel
     * @throws IOException          if registration fails
     * @throws NullPointerException if ssc or psf is null
     */
    public SelectionKey addServerSocket(ServerSocketChannel ssc, ProtocolFactory<?> psf) throws IOException {
        SUS.checkIfNulls("Null values", ssc, psf);

        SelectionKey sk = selectorController.register(ssc, SelectionKey.OP_ACCEPT, psf, false);
        logger.getLogger().info(ssc + " added");

        return sk;
    }

    /**
     * Initiates a non-blocking TCP client connection with callback-based notification.
     *
     * <p>This method provides a callback-style API for client connections, where the
     * provided {@link ConsumerCallback} is invoked when the connection completes
     * successfully or fails with an exception.</p>
     *
     * <p>Unlike the factory-based methods, this approach gives the caller direct access
     * to the connected SocketChannel, allowing custom handling of the connection lifecycle
     * outside the NIOSocket's protocol handler framework.</p>
     *
     * <p>If the connection completes immediately (ultra-fast local connection), the callback
     * is invoked synchronously before this method returns. Otherwise, the callback is
     * invoked asynchronously when the connection completes or times out.</p>
     *
     * @param tsc           the callback to invoke with the connected SocketChannel on success,
     *                     or with an exception on failure
     * @return the SocketChannel being connected (may not yet be connected when returned)
     * @throws IOException if the socket channel cannot be opened or connection initiation fails
     */
    public SelectionKey addClientSocket(TCPSessionCallback tsc) throws IOException {
        return addClientSocket(tsc.getRemoteAddress(), tsc, tsc.timeoutInSec(), tsc.dnsResolver());
    }

    /**
     * Initiates a non-blocking TCP client connection with callback-based notification.
     *
     * <p>This method provides a callback-style API for client connections, where the
     * provided {@link ConsumerCallback} is invoked when the connection completes
     * successfully or fails with an exception.</p>
     *
     * <p>Unlike the factory-based methods, this approach gives the caller direct access
     * to the connected SocketChannel, allowing custom handling of the connection lifecycle
     * outside the NIOSocket's protocol handler framework.</p>
     *
     * <p>If the connection completes immediately (ultra-fast local connection), the callback
     * is invoked synchronously before this method returns. Otherwise, the callback is
     * invoked asynchronously when the connection completes or times out.</p>
     *
     * @param cc           the callback to invoke with the connected SocketChannel on success,
     *                     or with an exception on failure
     * @param timeoutInSec in seconds
     * @return the SocketChannel being connected (may not yet be connected when returned)
     * @throws IOException if the socket channel cannot be opened or connection initiation fails
     */
    public SelectionKey addClientSocket(TCPSessionCallback cc, int timeoutInSec) throws IOException {
        return addClientSocket(cc.getRemoteAddress(), cc, timeoutInSec, cc.dnsResolver());
    }

    /**
     * Initiates a non-blocking TCP client connection with callback-based notification.
     *
     * <p>This method provides a callback-style API for client connections, where the
     * provided {@link ConsumerCallback} is invoked when the connection completes
     * successfully or fails with an exception.</p>
     *
     * <p>Unlike the factory-based methods, this approach gives the caller direct access
     * to the connected SocketChannel, allowing custom handling of the connection lifecycle
     * outside the NIOSocket's protocol handler framework.</p>
     *
     * <p>If the connection completes immediately (ultra-fast local connection), the callback
     * is invoked synchronously before this method returns. Otherwise, the callback is
     * invoked asynchronously when the connection completes or times out.</p>
     *
     * @param sa           the remote server address to connect to
     * @param timeoutInSec connection timeout in seconds; the connection is closed and
     *                     an exception is passed to the callback if not established within this time
     * @param cc           the callback to invoke with the connected SocketChannel on success,
     *                     or with an exception on failure
     * @return the SocketChannel being connected (may not yet be connected when returned)
     * @throws IOException if the socket channel cannot be opened or connection initiation fails
     */
    public SelectionKey addClientSocket(InetSocketAddress sa, ConnectionCallback<?> cc, int timeoutInSec, DNSResolverInt resolver) throws IOException {
        // Logic to be applied
        // 1. SocketChannel.open()
        // 2. configureBlocking(false)
        // 3. connect() ← Initiates connection FIRST
        // 4. If pending, register(OP_CONNECT) ← Only register after connection started


        if (resolver != null) {
            resolver.resolveIPAddress(sa.getHostName());
        }
        ScheduledAttachment<ConnectionCallback<?>> scheduledAttachment = new ScheduledAttachment<>();
        scheduledAttachment.attach(cc);
        SocketChannel channel = SocketChannel.open();
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);

        // crucial here
        channel.configureBlocking(false);
        cc.setChannel(channel);
        try {
            scheduledAttachment.setAppointment(taskSchedulerProcessor.queue(TimeInMillis.SECOND.mult(timeoutInSec), new NIOChannelMonitor(channel, selectorController, cc)));
            if (channel.connect(sa)) {
                // we connected ULTRA fast connection (loopback)
                SelectionKey selectionKey = selectorController.register(channel, 0, scheduledAttachment, false);
                processConnectable(selectionKey);
                return selectionKey;
            }
            // connection is pending, register for OP_CONNECT
            SelectionKey selectionKey = selectorController.register(channel, SelectionKey.OP_CONNECT, scheduledAttachment, false);
            return selectionKey;
        } catch (IOException e) {
            SharedIOUtil.close(channel);
            if(scheduledAttachment.attachment() != null)
                scheduledAttachment.attachment().exception(e);
            if (scheduledAttachment.getAppointment() != null)
                scheduledAttachment.getAppointment().cancel();
            throw e;
        }
    }


    /**
     * Creates and registers a UDP datagram socket on the specified address.
     *
     * <p>This method opens a new {@link DatagramChannel}, binds it to the specified
     * address and port, and registers it with the selector for reading incoming datagrams.
     * A single {@link UDPSessionCallback} instance is created from the factory to handle
     * all incoming datagrams on this socket.</p>
     *
     * <p>Unlike TCP sockets, UDP sockets handle all communication through a single channel
     * rather than creating new channels for each remote endpoint.</p>
     *
     * @param usc the UDPSessionCallback
     * @return the SelectionKey associated with the datagram channel
     * @throws IOException          if the datagram socket cannot be opened or bound
     * @throws NullPointerException if sa or psf is null
     */
    public SelectionKey addDatagramSocket(UDPSessionCallback usc) throws IOException {
        return addDatagramSocket(new InetSocketAddress(usc.getPort()), usc);
    }

    /**
     * Creates and registers a UDP datagram socket on the specified address.
     *
     * <p>This method opens a new {@link DatagramChannel}, binds it to the specified
     * address and port, and registers it with the selector for reading incoming datagrams.
     * A single {@link UDPSessionCallback} instance is created from the factory to handle
     * all incoming datagrams on this socket.</p>
     *
     * <p>Unlike TCP sockets, UDP sockets handle all communication through a single channel
     * rather than creating new channels for each remote endpoint.</p>
     *
     * @param sa  the socket address to bind to (IP address and port)
     * @param usc the UDPSessionCallback
     * @return the SelectionKey associated with the datagram channel
     * @throws IOException          if the datagram socket cannot be opened or bound
     * @throws NullPointerException if sa or psf is null
     */
    public SelectionKey addDatagramSocket(InetSocketAddress sa, UDPSessionCallback usc) throws IOException {
        SUS.checkIfNulls("Null values", sa, usc);
        DatagramChannel dc = DatagramChannel.open();
        dc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        dc.socket().bind(sa);
        usc.setChannel(dc);
        SelectionKey sk = selectorController.register(dc, SelectionKey.OP_READ, usc, false);
        logger.getLogger().info(dc + " added");
        return sk;
    }


    /**
     * Creates and registers a UDP datagram socket on the specified address.
     *
     * <p>This method opens a new {@link DatagramChannel}, binds it to the specified
     * address and port, and registers it with the selector for reading incoming datagrams.
     * A single {@link ProtocolHandler} instance is created from the factory to handle
     * all incoming datagrams on this socket.</p>
     *
     * <p>Unlike TCP sockets, UDP sockets handle all communication through a single channel
     * rather than creating new channels for each remote endpoint.</p>
     *
     * @param sa  the socket address to bind to (IP address and port)
     * @param ph the protocol handler
     * @return the SelectionKey associated with the datagram channel
     * @throws IOException          if the datagram socket cannot be opened or bound
     * @throws NullPointerException if sa or psf is null
     */
//    public SelectionKey addDatagramSocket(InetSocketAddress sa, ProtocolHandler ph) throws IOException {
//        SUS.checkIfNulls("Null values", sa, ph);
//        DatagramChannel dc = DatagramChannel.open();
//        dc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
//        dc.socket().bind(sa);
//
//        return addDatagramSocket(dc, ph);
//    }

    /**
     * Registers an existing DatagramChannel with the selector.
     *
     * <p>Use this method when you need to configure the DatagramChannel before
     * registration, such as setting custom socket options or connecting to a
     * specific remote address. The channel must already be bound to an address
     * before calling this method.</p>
     *
     * <p>Note: Unlike TCP, a new ProtocolHandler is created immediately from the
     * factory and attached to the SelectionKey, since UDP uses a single channel
     * for all communication.</p>
     *
     * @param dc  the pre-configured and bound datagram channel
     * @param ph the protocol handler
     * @return the SelectionKey associated with the datagram channel
     * @throws IOException          if registration fails
     * @throws NullPointerException if dc or psf is null
     */
//    public SelectionKey addDatagramSocket(DatagramChannel dc, ProtocolHandler ph) throws IOException {
//        SUS.checkIfNulls("Null values", dc, ph);
//        SelectionKey sk = selectorController.register(dc, SelectionKey.OP_READ, ph, false);
//        logger.getLogger().info(dc + " added");
//        return sk;
//    }

    /**
     * Creates and registers a TCP server socket using an {@link IPAddress} specification.
     *
     * <p>Convenience method that converts the IPAddress to an InetSocketAddress.
     * If the IPAddress has no associated InetAddress, the server will bind to all
     * available interfaces (wildcard address) on the specified port.</p>
     *
     * @param sa      the IP address specification containing the bind address and port
     * @param backlog the maximum number of pending connections in the listen queue
     * @param psf     the protocol factory for creating handlers for accepted connections
     * @return the SelectionKey associated with the server socket channel
     * @throws IOException if the server socket cannot be opened or bound
     * @see #addServerSocket(InetSocketAddress, int, ProtocolFactory)
     */
    public SelectionKey addServerSocket(IPAddress sa, int backlog, ProtocolFactory<?> psf) throws IOException {
        return addServerSocket(sa.getInetAddress() != null ? new InetSocketAddress(sa.getInetAddress(), sa.getPort()) : new InetSocketAddress(sa.getPort()),
                backlog,
                psf);
    }

    /**
     * Creates and registers a TCP server socket on the specified port.
     *
     * <p>Convenience method that binds the server to all available network interfaces
     * (wildcard address 0.0.0.0) on the specified port.</p>
     *
     * @param port    the port number to listen on
     * @param backlog the maximum number of pending connections in the listen queue
     * @param psf     the protocol factory for creating handlers for accepted connections
     * @return the SelectionKey associated with the server socket channel
     * @throws IOException if the server socket cannot be opened or bound
     * @see #addServerSocket(InetSocketAddress, int, ProtocolFactory)
     */
    public SelectionKey addServerSocket(int port, int backlog, ProtocolFactory<?> psf) throws IOException {
        return addServerSocket(new InetSocketAddress(port), backlog, psf);
    }

    /**
     * Sets the event listener manager for security events.
     *
     * <p>When set, the NIOSocket dispatches {@link IPAddressEvent} notifications whenever
     * an incoming connection is rejected by the security filter rules. This enables
     * integration with external intrusion detection systems, logging frameworks, or
     * automated blacklisting mechanisms.</p>
     *
     * <p>Events are dispatched asynchronously (async=true) to avoid blocking the
     * selector thread.</p>
     *
     * @param eventListenerManager the event manager to receive security events; may be null
     *                             to disable event dispatching
     * @see IPAddressEvent
     */
    public void setEventManager(EventListenerManager<BaseEventObject<?>, ?> eventListenerManager) {
        this.eventListenerManager = eventListenerManager;
    }

    /**
     * Returns the current event listener manager.
     *
     * @return the event manager for security events, or null if not set
     * @see #setEventManager(EventListenerManager)
     */
    public EventListenerManager<BaseEventObject<?>, ?> getEventManager() {
        return eventListenerManager;
    }


    @Override
    public void run() {
        long snapTime = System.currentTimeMillis();
        long attackTimestamp = 0;

        while (live.get()) {
            try {
                int selectedCount = 0;
                if (selectorController.isOpen()) {
                    selectedCount = selectorController.select(0);
                    long delta = System.nanoTime();
                    if (selectedCount > 0) {
                        Set<SelectionKey> selectedKeys = selectorController.selectedKeys();
                        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                        selectedCountTotal += selectedCount;

                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();
                            keyIterator.remove();

                            try {

                                // read case
                                if (key.isReadable()
                                        && key.isValid()
                                        && key.channel().isOpen()) {
                                    processReadable(key);

                                } // server socket waiting for incoming connection
                                else if (key.isAcceptable()
                                        && key.isValid()
                                        && key.channel().isOpen()) {
                                    // a connection was accepted by a ServerSocketChannel.

                                    SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
                                    // os level detection of stale connection
                                    //===================================================
                                    // Enable TCP keep-alive.
                                    sc.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                                    // section to be included after upgrade to jdk11+
									/*
									  Set extended TCP keepalive options (Java 11+)
									  TCP_KEEPIDLE corresponds roughly to tcp_keepalive_time (in seconds)
									 */

                                    //sc.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, 60);

                                    /* TCP_KEEPINTVL (in seconds) is the interval between keepalive probes.*/

                                    //sc.setOption(ExtendedSocketOptions.TCP_KEEPINTVL, 10);

                                    /* TCP_KEEPCOUNT is the number of probes to send before considering the connection dead*/

                                    //sc.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, 3);
                                    //===================================================
                                    ProtocolFactory<?> protocolFactory = (ProtocolFactory<?>) key.attachment();
                                    if (logger.isEnabled())
                                        logger.getLogger().info("Accepted: " + sc + " psf:" + protocolFactory);
                                    // check if the incoming connection is allowed


                                    if (NetUtil.checkSecurityStatus(protocolFactory.getIncomingInetFilterRulesManager(), sc, null) != SecurityStatus.ALLOW) {
                                        try {
                                            long currentAttackCount = ++attackTotalCount;
                                            if (attackTimestamp == 0) {
                                                attackTimestamp = System.currentTimeMillis();
                                            }

                                            InetSocketAddress isa = (InetSocketAddress) ((ServerSocketChannel) key.channel()).getLocalAddress();
                                            // in try block with catch exception since logger can point to file log
                                            logger.getLogger().info("@ port:" + isa.getPort() + " access denied for:" + sc.getRemoteAddress());
                                            if (eventListenerManager != null) {
                                                if (sc.getRemoteAddress() instanceof InetSocketAddress) {
                                                    InetSocketAddress remoteISA = (InetSocketAddress) sc.getRemoteAddress();
                                                    if (remoteISA.getAddress() instanceof Inet4Address) {
                                                        String remoteIPAddress = SharedNetUtil.toV4Address(remoteISA.getAddress().getAddress());
                                                        IPAddressEvent event = new IPAddressEvent(this, new IPAddress(remoteIPAddress, isa.getPort()));
                                                        eventListenerManager.dispatch(event, true);
                                                    }
                                                }
                                            }

                                            if (currentAttackCount % 500 == 0) {
                                                float burstRate = (float) ((500.00 / (float) (System.currentTimeMillis() - attackTimestamp)) * TimeInMillis.SECOND.MILLIS);
                                                float overAllRate = ((float) currentAttackCount / (float) (System.currentTimeMillis() - startTime)) * TimeInMillis.SECOND.MILLIS;
                                                logger.getLogger().info(" Burst Attacks:" + burstRate + " a/s" + " Total Attacks:" + overAllRate + " a/s" + " total:" + attackTotalCount + " in " + TimeInMillis.toString(System.currentTimeMillis() - startTime));
                                                attackTimestamp = 0;
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        } finally {
                                            // had to close after log otherwise we have an open socket
                                            SharedIOUtil.close(sc);
                                        }

                                    } else {
                                        // create a protocol instance
                                        ProtocolHandler protocolHandler = protocolFactory.newInstance();

                                        protocolHandler.setSelectorController(selectorController);
                                        protocolHandler.setExecutor(executor);
                                        protocolHandler.setOutgoingInetFilterRulesManager(protocolFactory.getOutgoingInetFilterRulesManager());

                                        // if we have an executor
                                        // accept the new connection

                                        if (executor != null && protocolFactory.isComplexSetup()) {
                                            executor.execute(() ->
                                            {
                                                try {
                                                    protocolHandler.setupConnection(sc, protocolFactory.isBlocking());
                                                    connectionCount.incrementAndGet();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                    SharedIOUtil.close(protocolHandler);
                                                }
                                            });
                                        } else {
                                            protocolHandler.setupConnection(sc, protocolFactory.isBlocking());
                                            connectionCount.incrementAndGet();
                                        }


                                    }

                                } else if (key.isValid()
                                        && key.channel().isOpen()
                                        && key.isConnectable()) {
                                    // this is used by client connection
                                    processConnectable(key);

                                }


                            } catch (Exception e) {
                                logger.getLogger().info("DFDSAFGDSAGTRWETGREWTRETGREGREFDGERAGREATGRETGRETg");
                                if (!(e instanceof CancelledKeyException || e instanceof ConnectException)) {
                                    e.printStackTrace();
                                }
                            }

                            try {
                                // key clean up
                                if (!key.isValid()
                                        || !key.channel().isOpen()) {
                                    key.cancel();
                                    key.channel().close();
                                }
                            } catch (Exception e) {

                                e.printStackTrace();
                            }

                        }
                        delta = System.nanoTime() - delta;
                        callsCounter.register(delta);
                    }
                }

                // stats
                if (getStatLogCounter() > 0 && (callsCounter.getCounts() % getStatLogCounter() == 0 || (System.currentTimeMillis() - snapTime) > getStatLogCounter())) {
                    snapTime = System.currentTimeMillis();
                    logger.getLogger().info("Average dispatch processing " + TimeInMillis.nanosToString(averageProcessingTime()) +
                            " total time:" + TimeInMillis.nanosToString(callsCounter.getDeltas()) +
                            " total dispatches:" + callsCounter.getCounts() + " total select calls:" + selectedCountTotal +
                            " last select count:" + selectedCount + " total select keys:" + selectorController.keysCount() +
                            " available workers:" + TaskUtil.availableThreads(executor) + "," + TaskUtil.pendingTasks(executor));
                }
            } catch (Exception e) {


                e.printStackTrace();
            }
        }
    }


    private void processReadable(SelectionKey key) {
        // channel has data to read
        // this is the reading part of the process
        SKHandler skHandler = (SKHandler) key.attachment();
        if (skHandler instanceof ProtocolHandler)
            ((ProtocolHandler) skHandler).updateUsage();


        // very,very,very crucial setup prior to processing
        // we are disabling the key operations by the selector
        // for the current selection key
        //int keyOPs = key.interestOps();
        key.interestOps(0);

        // a channel is ready for reading
        if (executor != null) {
            // we have an executor
            // delegate the data reading to an executor thread
            executor.execute(() ->
            {
                try {
                    skHandler.accept(key);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // very crucial step
                if (key.isValid()) {
                    // restoring selection ops for the selection key
                    key.interestOps(skHandler.interestOps());
                    selectorController.wakeup();
                }
            });
        } else {
            // no executor set so the current thread must process the incoming data
            try {
                skHandler.accept(key);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // very crucial step
            if (key.isValid()) {
                key.interestOps(skHandler.interestOps());
                selectorController.wakeup();
            }
        }
    }

    private void processConnectable(SelectionKey key) throws IOException {

        // this is used by client connection
        // stop key interest first
        key.interestOps(0);

        // finish connection
        // this a bit dicey
        //*no need to create new thread  since it takes micro-seconds at this stage*


        if (key.attachment() instanceof ScheduledAttachment &&
                ((ScheduledAttachment) key.attachment()).attachment() instanceof ConnectionCallback) {

            ConnectionCallback connectData = (ConnectionCallback) ((ScheduledAttachment) key.attachment()).attachment();
            // the code read and write processing is done by outsiders
            // the read/write or waiting for connection is done my outsiders
            // selection key is to be canceled
            SocketChannel sc = ((SocketChannel) key.channel());

            try {
                finishConnecting(key);
                connectionCount.incrementAndGet();
                key.attach(connectData);

                //**************************************************************************

                if (executor != null)
                    executor.execute(() -> {
                        try {
                            int keysOps = connectData.connected(key);
                            if (keysOps >= 0 && key.isValid()) {
                                key.interestOps(keysOps);
                                selectorController.wakeup();
                            } else
                                selectorController.cancelSelectionKey(key);
                        } catch (IOException e) {
                            SharedIOUtil.close(key.channel());
                            connectData.exception(e);

                        }
                    });
                else {
                    int keysOps = connectData.connected(key);
                    if (keysOps >= 0 && key.isValid()) {
                        key.interestOps(keysOps);
                        selectorController.wakeup();
                    } else
                        selectorController.cancelSelectionKey(key);
                }

            } catch (Exception e) {

                // e.printStackTrace();
                SharedIOUtil.close(key.channel());

                if (executor != null)
                    executor.execute(() -> connectData.exception(e));
                else
                    connectData.exception(e);
            }

        }
//        else {
//            // CURRENTLY impossible case
//
//            ProtocolHandler phTemp = null;
//            Object attachment = ((ScheduledAttachment<?>) key.attachment()).attachment();
//
//            if (attachment instanceof ProtocolHandler) {
//                phTemp = (ProtocolHandler) attachment;
//            } else if (attachment instanceof ProtocolFactory) {
//                ProtocolFactory<?> protocolFactory = (ProtocolFactory<?>) attachment;
//                phTemp = protocolFactory.newInstance();
//            }
//
//            // phTemp for lambda requirement
//            ProtocolHandler ph = phTemp;
//
//            try {
//                finishConnecting(key);
//                connectionCount.incrementAndGet();
//                // attach the protocol handler
//                ph.setSelectorController(selectorController);
//                ph.setupConnection((AbstractSelectableChannel) key.channel(), false);
//
//                // a channel is ready for reading
//                int keyOPs = key.interestOps();
//                key.interestOps(0);
//                if (executor != null) {
//                    // lambda requirement in effect here
//                    executor.execute(() ->
//                    {
//                        try {
//                            ph.accept(key);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                        // very crucial step
//                        if (key.isValid()) {
//                            key.interestOps(keyOPs);
//                            selectorController.wakeup();
//                        }
//                    });
//                } else {
//                    // no executor set so the current thread must process the incoming data
//                    try {
//                        ph.accept(key);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    // very crucial step
//                    if (key.isValid()) {
//                        key.interestOps(keyOPs);
//                        selectorController.wakeup();
//                    }
//                }
//            } catch (IOException e) {
//
//                if (ph.getSessionCallback() != null)
//                    ph.getSessionCallback().exception(e);
//
//                IOUtil.close(ph);
//                throw e;
//
//            }
//        }
    }

    /**
     * Finish the connect stage
     * @param key isConnectable() true
     * @throws IOException case of error
     */
    public static void finishConnecting(SelectionKey key) throws IOException {
        synchronized (key) {
            if (key.isValid()) {

                finishConnecting((SocketChannel) key.channel(),
                        key.attachment() instanceof ScheduledAttachment ? ((ScheduledAttachment) key.attachment()).getAppointment() : null);
                return;
            }

            throw new IOException("failed to finish connection " + key.channel());
        }
    }

    /**
     * Finish the connect stage
     * @param channel in connection pending stage
     * @param connectTimeout could be null, if not will be closed
     * @throws IOException case of error
     */
    public static void finishConnecting(SocketChannel channel, Appointment connectTimeout) throws IOException {
        try {
            if (channel.isOpen()) {
                if (channel.isConnectionPending()) {
                    channel.finishConnect();
                    if (logger.isEnabled()) logger.getLogger().info("connection finished for " + channel);
                }

                if (channel.isConnected()) {
                    return;
                }
            }
        } finally {
            SharedIOUtil.close(connectTimeout);
        }

        throw new IOException("failed to finish connection " + channel);
    }


    /**
     * Returns the average I/O processing time per dispatch in nanoseconds.
     *
     * <p>This metric measures the average time spent processing each selector dispatch
     * cycle, from receiving selected keys to completing handler invocations. Useful
     * for monitoring system performance and detecting processing bottlenecks.</p>
     *
     * @return the average processing time in nanoseconds
     */
    public long averageProcessingTime() {
        return (long) callsCounter.average();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns true if the NIOSocket has been closed via {@link #close()},
     * indicating that the selector loop has stopped and no further connections
     * will be processed.</p>
     *
     * @return true if the socket is closed, false if still running
     */
    @Override
    public boolean isClosed() {
        return !live.get();
    }

    /**
     * Closes the NIOSocket and releases all associated resources.
     *
     * <p>This method performs the following cleanup in order:</p>
     * <ol>
     *   <li>Sets the live flag to false, causing the selector loop to exit</li>
     *   <li>Closes all registered channels (servers, clients, datagrams)</li>
     *   <li>Cancels all selection keys</li>
     *   <li>Closes the underlying selector</li>
     * </ol>
     *
     * <p>This method is idempotent; subsequent calls after the first close have no effect.</p>
     *
     * @throws IOException if an I/O error occurs during cleanup (channels and selector
     *                     are still closed on a best-effort basis)
     */
    @Override
    public void close() throws IOException {
        if (live.getAndSet(false)) {
            Set<SelectionKey> keys = selectorController.keys();
            for (SelectionKey sk : keys) {
                if (sk.channel() != null)
                    SharedIOUtil.close(sk.channel());
                try {
                    selectorController.cancelSelectionKey(sk);
                } catch (Exception e) {
                }
            }

            SharedIOUtil.close(selectorController);
        }

    }

    /**
     * Returns the executor used for I/O processing.
     *
     * @return the executor for offloading I/O handlers, or null if processing
     *         occurs on the selector thread
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * Returns the scheduler used for timed operations.
     *
     * <p>The scheduler is used for connection timeout monitoring via
     * {@link NIOChannelMonitor} and rate-controlled connection attempts.</p>
     *
     * @return the scheduled executor service for timed operations
     */
    public ScheduledExecutorService getScheduler() {
        return taskSchedulerProcessor;
    }

    /**
     * Returns the total number of connections established since startup.
     *
     * <p>This count includes both accepted incoming connections and successfully
     * connected outgoing client connections. It does not include rejected connections
     * blocked by security filters.</p>
     *
     * @return the total connection count
     */
    public long totalConnections() {
        return connectionCount.get();
    }

    /**
     * Returns the statistics logging interval.
     *
     * @return the interval for logging statistics, in milliseconds or dispatch counts
     *         (depending on which threshold is reached first); 0 disables stat logging
     * @see #setStatLogCounter(long)
     */
    public long getStatLogCounter() {
        return statLogCounter;
    }

    /**
     * Sets the statistics logging interval.
     *
     * <p>When set to a value greater than 0, the NIOSocket periodically logs performance
     * statistics including average processing time, total dispatches, select calls, and
     * available worker threads. Statistics are logged when either:</p>
     * <ul>
     *   <li>The number of dispatches reaches a multiple of this value, or</li>
     *   <li>The elapsed time since the last log exceeds this value in milliseconds</li>
     * </ul>
     *
     * @param statLogCounter the logging interval; 0 to disable stat logging
     */
    public void setStatLogCounter(long statLogCounter) {
        this.statLogCounter = statLogCounter;
    }

    /**
     * Returns a snapshot of current NIOSocket statistics.
     *
     * <p>The returned map contains the following metrics:</p>
     * <ul>
     *   <li><b>time_stamp:</b> Current timestamp when stats were collected</li>
     *   <li><b>connection_counts:</b> Total connections established since startup</li>
     *   <li><b>select_calls_counts:</b> Total selector.select() calls that returned &gt; 0 keys</li>
     *   <li><b>attack_counts:</b> Total connections rejected by security filters</li>
     *   <li><b>selection_key_registration_count:</b> Total SelectionKey registrations</li>
     *   <li><b>total_selection_keys:</b> Current number of registered SelectionKeys</li>
     *   <li><b>nio-calls-counter:</b> Rate counter with timing statistics</li>
     * </ul>
     *
     * @return an NVGenericMap containing the current statistics
     */
    public NVGenericMap getStats() {
        NVGenericMap ret = new NVGenericMap("nio_socket");
        ret.add("time_stamp", DateUtil.DEFAULT_DATE_FORMAT_TZ.format(new Date()));
        ret.build(new NVLong("connection_counts", totalConnections())).
                build(new NVLong("select_calls_counts", selectedCountTotal)).
                build(new NVLong("attack_counts", attackTotalCount)).
                build(new NVLong("selection_key_registration_count", selectorController.registrationCount())).
                build(new NVLong("total_selection_keys", selectorController.selectionKeysCount())).
                build(callsCounter);
        return ret;
    }


}
