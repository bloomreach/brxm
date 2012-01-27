/*
 *  Copyright 2012 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.behavioral;

import java.util.Collection;

import javax.servlet.http.HttpSession;

/**
 * Registry for HTTP sessions. Implementations may choose to add and/or remove HTTP sessions automatically.
 */
public interface HttpSessionRegistry {

    /**
     * Adds an HTTP session to this registry if it has not been added already.
     *
     * @param session the session to add
     */
    public void add(HttpSession session);

    /**
     * Removes an HTTP session from this registry if it has not been removed already.
     *
     * @param session the session to remove
     */
    public void remove(HttpSession session);

    /**
     * @return the number of HTTP sessions in this registry.
     */
    public int size();

    /**
     * Returns whether or not this registry contains any HTTP sessions.
     *
     * @return true if this registry contains no HTTP sessions, false otherwise.
     */
    public boolean isEmpty();

    /**
     * Returns all the HTTP sessions in this registry.
     *
     * @return a collection of all HTTP sessions in this registry, or an empty collection if this registry is empty.
     */
    public Collection<HttpSession> getHttpSessions();

    /**
     * Returns the session in this registry with the given ID.
     *
     * @param sessionId the ID of the session
     * @return the session in this registry with the given ID, or null of this registry does not contain such a session
     */
    public HttpSession getHttpSession(String sessionId);

    /**
     * Returns whether or not this registry contains a session with the given ID.
     *
     * @param sessionId the ID of the session
     * @return true if this registry contains a session with the given ID, false otherwise.
     */
    public boolean contains(String sessionId);

    /**
     * Removes all HTTP sessions from this registry.
     */
    public void clear();

}
