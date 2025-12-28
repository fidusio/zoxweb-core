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
package org.zoxweb.shared.util;

import java.util.Comparator;
import java.util.concurrent.Delayed;


/**
 * The appointment interface.
 */
public interface Appointment
        extends CloseableType, Delayed, WaitTime<Appointment> {

    Comparator<Appointment> EQUAL_COMPARATOR = new AppointmentComparator();

    /**
     * @return the delay time in millis
     */
    long getDelayInMillis();

    /**
     * Set the delay time in millis
     * @param delayInMillis to wait
     */
    void setDelayInMillis(long delayInMillis);

    /**
     * Set the delay in millis with nanos offset
     * @param delayInMillis to wait
     * @param nanoOffset nanos to add
     */
    void setDelayInNanos(long delayInMillis, long nanoOffset);

    /**
     * Returns the expiration in real time.
     * @return the expiration in real time.
     */
    long getExpirationInMillis();

    /**
     * Cancel operation.
     * @return true if canceled
     */
    boolean cancel();

    /**
     * Adjusted expiration to granular precision beyond millis
     * @return nano wait
     */
    long getPreciseExpiration();


    void close();

    /**
     * reset the appointment
     * @param runOnce if true appointment is executed once if false it will repeat
     */
    boolean reset(boolean runOnce);


    /**
     * @return the execution count
     */
    long execCount();

}