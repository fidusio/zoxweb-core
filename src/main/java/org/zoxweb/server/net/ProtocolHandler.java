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

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.common.SKHandler;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.io.CloseableType;
import org.zoxweb.shared.util.*;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class ProtocolHandler
        implements GetName, GetDescription, CloseableType, UsageTracker, Consumer<SelectionKey>, SKHandler {
    public static final LogWrapper log = new LogWrapper(ProtocolHandler.class).setEnabled(false);

    static class PHTimeout
            implements Runnable {
        protected final Appointment timeout;
        protected final ProtocolHandler ph;

        PHTimeout(ProtocolHandler ph, long duration) {
            this.ph = ph;
            timeout = TaskUtil.defaultTaskScheduler().queue(duration, this);
        }

        public void run() {
            if (System.currentTimeMillis() - ph.lastUsage() > timeout.getDelayInMillis()) {
                IOUtil.close(ph);
                log.getLogger().info("session timed out protocol handler closed.");

            } else {
                timeout.reset(false);
            }
        }
    }

    public static final long SESSION_TIMEOUT = (long) (Const.TimeInMillis.MINUTE.MILLIS * 2.5);
    private static final AtomicLong ID_COUNTER = new AtomicLong();

    protected final long id = ID_COUNTER.incrementAndGet();

    private volatile SelectorController selectorController;
    private volatile InetFilterRulesManager outgoingInetFilterRulesManager;

    protected volatile SocketChannel phSChannel;
    protected volatile SelectionKey phSK;
    private final AtomicLong lastUsage = new AtomicLong(-1);


    private volatile NVGenericMap properties = null;
    protected volatile Executor executor;
    protected volatile int interestOps = SelectionKey.OP_READ;
    protected final AtomicBoolean isClosed = new AtomicBoolean(false);

    protected BaseSessionCallback<?> sessionCallback;
    private final PHTimeout phTimeout;


    protected ProtocolHandler(boolean enableTimeout) {
        this(enableTimeout ? SESSION_TIMEOUT: 0);
    }

    protected ProtocolHandler(long timeout) {
        updateUsage();
        if (timeout > 0)
            phTimeout = new PHTimeout(this, timeout);
        else
            phTimeout = null;

    }


    /**
     * @return last time used
     */
    @Override
    public long lastUsage() {
        return lastUsage.get();
    }

    /**
     * @return current usage update
     */
    @Override
    public long updateUsage() {
        return updateUsage(System.currentTimeMillis());
    }

    @Override
    public long updateUsage(long toUpdate) {
        lastUsage.set(toUpdate);
        return lastUsage.get();
    }

    @Override
    abstract public void accept(SelectionKey key);

    public long getID() {
        return id;
    }


    /**
     * @return the selector
     */
    public SelectorController getSelectorController() {
        return selectorController;
    }


    /**
     * @param selectorController the selector to set
     */
    public void setSelectorController(SelectorController selectorController) {
        this.selectorController = selectorController;
    }


    public void setupConnection(AbstractSelectableChannel asc, boolean isBlocking) throws IOException {
        phSChannel = (SocketChannel) asc;
        getSelectorController().register(phSChannel, SelectionKey.OP_READ, this, isBlocking);
        interestOps = SelectionKey.OP_READ;
    }


    public InetFilterRulesManager getOutgoingInetFilterRulesManager() {
        return outgoingInetFilterRulesManager;
    }


    public void setOutgoingInetFilterRulesManager(InetFilterRulesManager inetFilterRulesManager) {
        this.outgoingInetFilterRulesManager = inetFilterRulesManager;
    }


    public void setProperties(NVGenericMap prop) {
        properties = prop;
    }

    public NVGenericMap getProperties() {
        return properties;
    }

    public void setExecutor(Executor exec) {
        this.executor = exec;
    }

    public Executor getExecutor() {
        return executor;
    }

    public BaseSessionCallback<?> getSessionCallback()
    {
        return sessionCallback;
    }


    public final void close() throws IOException {
        if (!isClosed.getAndSet(true)) {
            close_internal();
            if (phTimeout != null)
                phTimeout.timeout.cancel();
        }
    }

    @Override
    public boolean isClosed() {
        return isClosed.get() || (phSChannel != null && !phSChannel.isOpen());
    }

    abstract protected void close_internal() throws IOException;
    public boolean upgradeToTLS() throws IOException {
        throw  new UnsupportedOperationException("UpgradeToTLS not supported");
    }

    public int interestOps() {
        return interestOps;
    }
}
