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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is used to allow the Selector object to be used in multithreaded environment.
 *
 * @author mnael
 */
public class SelectorController
        implements Closeable {
    private final Selector selector;
    private final Lock selectLock = new ReentrantLock();
    private final Lock lock = new ReentrantLock();
    private final AtomicLong registrationCounter = new AtomicLong();


    /**
     * Create a Selector Controller
     *
     * @param selector the selector object
     */
    public SelectorController(Selector selector) {
        this.selector = selector;
    }


    /**
     * Register the socket channel with the selector by applying the following procedure:
     * <ol>
     * <li> invoke lock on the general lock
     * <li> wakeup the selector
     * <li> invoke lock on the select lock
     * <li> register the channel with the selector
     * <li> unlock the general lock
     * <li> unlock the select lock
     * <li> return the selection key
     * </ol>
     *
     * @param ch         selectable channel
     * @param ops        channel ops
     * @param attachment attachment object
     * @param blocking   true if blocking
     * @return SelectionKey registered selection key with the selector
     * @throws IOException is case of error
     */
    public SelectionKey register(AbstractSelectableChannel ch,
                                 int ops,
                                 Object attachment,
                                 boolean blocking) throws IOException {
        SelectionKey ret;
        try {
            // register function global lock
            lock.lock();
            // wakeup the selector if it is in select mode
            selector.wakeup();
            // lock the selectLock to prevent it from entering select mode
            selectLock.lock();

            // configure the selection mode for the channel ###
            ch.configureBlocking(blocking);
            ret = ch.register(selector, ops, attachment);

            registrationCounter.incrementAndGet();
            // ################################################
        } finally {
            // unlock the function global lock
            lock.unlock();
            // unlock the selectLock to allow the selector entering select mode
            selectLock.unlock();

        }

        return ret;
    }


    public SelectionKey update(SelectionKey sk,
                               int ops,
                               Object attachment) throws IOException {

        try {
            // register function global lock
            lock.lock();
            // wakeup the selector if it is in select mode
            selector.wakeup();
            // lock the selectLock to prevent it from entering select mode
            selectLock.lock();

            // configure the selection mode for the channel ###


            if (selector.keys().contains(sk)) {
                sk.interestOps(ops);
                sk.attach(attachment);
            } else {
                return null;
            }
            // ################################################
        } finally {
            // unlock the function global lock
            lock.unlock();
            // unlock the selectLock to allow the selector entering select mode
            selectLock.unlock();

        }

        return sk;
    }


    /**
     * Blocking select
     *
     * @return number of selection match
     * @throws IOException is case of error
     */
    public int select() throws IOException {
        return select(0);
    }

    /**
     * Invoke the select
     *
     * @param timeout selection timeout
     * @return number of selected keys
     * @throws IOException in case of error
     */
    public int select(long timeout) throws IOException {
        try {
            // we must call the global lock just in case a registration
            // is taking place
            lock.lock();
            // and immediately unlock it
            lock.unlock();
            // lock before selection
            selectLock.lock();
            // wait for the selector to return
            return selector.select(timeout);
        } finally {
            // unlock the selection mode
            selectLock.unlock();
        }
    }

    /**
     * Cancel the selection key
     *
     * @param sk to be canceled
     */
    public void cancelSelectionKey(SelectionKey sk) {

        if (sk != null) {
            try {
                // block the select lock just in case
                lock.lock();
                // wakeup the selector
                selector.wakeup();
                // invoke the main lock
                selectLock.lock();
                sk.cancel();
            } finally {
                lock.unlock();
                selectLock.unlock();
            }
        }
    }

    public void wakeup() {
        //lock.lock();
        selector.wakeup();
        //selectLock.lock();
        //lock.unlock();
        //selectLock.unlock();
    }

    /**
     * Cancel selection key based on the channel
     *
     * @param ch that has a selection key to be canceled
     */
    public void cancelSelectionKey(SelectableChannel ch) {
        if (ch != null) {
            cancelSelectionKey(ch.keyFor(selector));
        }
    }

    public SelectionKey channelSelectionKey(SelectableChannel channel) {
        if (channel != null) {
            return channel.keyFor(selector);
        }
        return null;
    }

    public int selectionKeysCount() {
        int ret = selector.keys().size();
        wakeup();
        return ret;
    }


    public long registrationCount() {
        return registrationCounter.get();
    }


    public boolean isOpen() {
        return selector.isOpen();
    }

    public Set<SelectionKey> selectedKeys() {
        return selector.selectedKeys();
    }

    public Set<SelectionKey> keys() {
        return selector.keys();
    }

    public int keysCount() {
        return selector.keys().size();
    }

    @Override
    public void close() throws IOException {
        selector.close();
    }

}
