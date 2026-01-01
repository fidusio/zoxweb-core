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

import org.zoxweb.shared.util.BaseSubjectID;
import org.zoxweb.shared.util.CloseableType;
import org.zoxweb.shared.util.GetNVProperties;

import java.util.Set;

/**
 * Interface for protocol session management.
 * Provides methods for session lifecycle management including
 * attaching, detaching, and closing sessions with associated resources.
 *
 * @param <S> the session type
 * @param <T> the subject ID type
 * @author mnael
 * @see GetNVProperties
 * @see CloseableType
 * @see BaseSubjectID
 */
public interface ProtoSession<S, T>
    extends GetNVProperties, CloseableType, BaseSubjectID<T>
{
    /**
     * Returns the actual session associated with this implementation.
     *
     * @return the session object
     */
    S getSession();

    /**
     * Checks if the session can be closed.
     * It is not mandatory to obey the response; the caller can invoke close regardless.
     *
     * @return true if the session is closed or can be closed
     */
    boolean canClose();

    /**
     * Returns the set of auto-closeable resources associated with this session.
     *
     * @return set of AutoCloseable objects
     */
    Set<AutoCloseable> getAutoCloseables();

    /**
     * Attaches the session to the current context (e.g., a thread).
     *
     * @return true if the session was attached successfully
     */
    boolean attach();

    /**
     * Detaches the session from the current context.
     *
     * @return true if the session was detached successfully
     */
    boolean detach();
}
