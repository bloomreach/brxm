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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for HTTP sessions. The sessions are stored in weak references, so they will be automatically queued for
 * cleanup once they are no longer referenced. Queued sessions are cleaned up when a new session is added, an
 * existing session is removed, or the registry is cleared.
 *
 * Added sessions are stored in static state, so multiple instances of this class will all represent the same logical
 * registry. Added sessions are tagged with an special attribute to quickly recognize them as being added without
 * needing access to synchronized state. This attribute is removed from a session when a it is removed from this
 * registry.
 * 
 * Adding and removing sessions is not thread-safe, i.e. {@link #add(javax.servlet.http.HttpSession)} and
 * {@link #remove(javax.servlet.http.HttpSession) cannot be interleaved arbitrarily. Accessing the sessions in this
 * registry (e.g. via {@link #size}, {@link #isEmpty}, {@link #getHttpSessions}, etc.) is thread-safe.
 */
public class WeakReferenceHttpSessionRegistry implements HttpSessionRegistry {

    private static final String SESSION_ATTR_NAME_IS_REGISTERED = WeakReferenceHttpSessionRegistry.class.getName() + ".isregistered";

    private static final Logger log = LoggerFactory.getLogger(WeakReferenceHttpSessionRegistry.class);
    
    private static final ConcurrentHashMap<String, WeakHttpSessionReference> sessions = new ConcurrentHashMap<String, WeakHttpSessionReference>();
    private static final ReferenceQueue<HttpSession> referenceQueue = new ReferenceQueue<HttpSession>();

    @Override
    public void add(final HttpSession session) {
        if (session != null && session.getAttribute(SESSION_ATTR_NAME_IS_REGISTERED) == null) {
            processReferenceQueue();

            final String id = session.getId();
            log.info("Registering session {}", id);
            WeakHttpSessionReference ref = WeakHttpSessionReference.create(id, session, referenceQueue);
            sessions.put(id, ref);
            session.setAttribute(SESSION_ATTR_NAME_IS_REGISTERED, true);
        }
    }

    @Override
    public void remove(final HttpSession session) {
        if (session != null && session.getAttribute(SESSION_ATTR_NAME_IS_REGISTERED) != null) {
            processReferenceQueue();

            String id = session.getId();
            log.info("Deregistering session {}", id);
            sessions.remove(id);
            session.removeAttribute(SESSION_ATTR_NAME_IS_REGISTERED);
        }
    }

    void processReferenceQueue() {
        log.debug("Processing reference queue");
        WeakHttpSessionReference ref = (WeakHttpSessionReference)referenceQueue.poll();
        while (ref != null) {
            log.info("Removing reference to session {}", ref.getSessionId());
            sessions.remove(ref.getSessionId());
            ref = (WeakHttpSessionReference)referenceQueue.poll();
        }
    }
    
    public void clear() {
        sessions.clear();
        processReferenceQueue();
    }

    @Override
    public int size() {
        return sessions.size();
    }

    @Override
    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    @Override
    public Collection<HttpSession> getHttpSessions() {
        Collection<HttpSession> result = new ArrayList<HttpSession>();

        Enumeration<WeakHttpSessionReference> e = sessions.elements();
        while (e.hasMoreElements()) {
            WeakHttpSessionReference ref = e.nextElement();
            HttpSession session = ref.get();
            if (session != null) {
                result.add(session);
            }
        }
        
        return result;
    }

    @Override
    public HttpSession getHttpSession(final String sessionId) {
        WeakHttpSessionReference ref = sessions.get(sessionId);
        return ref == null ? null : ref.get();
    }

    @Override
    public boolean contains(final String sessionId) {
        return sessions.containsKey(sessionId);
    }

    private static class WeakHttpSessionReference extends WeakReference<HttpSession> {

        private String sessionId;

        private WeakHttpSessionReference(String sessionId, HttpSession session, ReferenceQueue<? super HttpSession> queue) {
            super(session, queue);
            this.sessionId = sessionId;
        }
        
        private String getSessionId() {
            return sessionId;
        }

        private static WeakHttpSessionReference create(String sessionId, HttpSession session, ReferenceQueue<? super HttpSession> queue) {
            return session == null ? null : new WeakHttpSessionReference(sessionId, session, queue);
        }

    }

}
