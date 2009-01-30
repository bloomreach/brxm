/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrObservationManager implements ObservationManager {

    static final Logger log = LoggerFactory.getLogger(JcrObservationManager.class);

    private static JcrObservationManager INSTANCE = new JcrObservationManager();

    private class JcrListener extends WeakReference<EventListener> implements EventListener {
        List<Event> events;
        ObservationManager obMgr;
        WeakReference<UserSession> sessionRef;

        JcrListener(UserSession userSession, EventListener upstream) {
            super(upstream, listenerQueue);
            this.events = new LinkedList<Event>();
            sessionRef = new WeakReference<UserSession>(userSession);
        }

        synchronized public void onEvent(EventIterator events) {
            while (events.hasNext()) {
                this.events.add(events.nextEvent());
            }
        }

        void init(int eventTypes, String absPath, boolean isDeep, String[] uuid, String[] nodeTypeName, boolean noLocal)
                throws RepositoryException {
            if (sessionRef.get() == null) {
                log.error("initializing event listener without wicket session");
                return;
            }
            Session session = sessionRef.get().getJcrSession();
            if (session != null) {
                obMgr = session.getWorkspace().getObservationManager();
                obMgr.addEventListener(this, eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
            } else {
                log.error("No jcr session bound to wicket session");
            }
        }

        void dispose() {
            if (obMgr != null) {
                try {
                    obMgr.removeEventListener(this);
                } catch (RepositoryException ex) {
                    log.error("Unable to unregister event listener, " + ex.getMessage());
                }
                obMgr = null;
            }
        }

        UserSession getSession() {
            return sessionRef.get();
        }

        synchronized void process() {
            final Iterator<Event> upstream = events.iterator();
            final long size = events.size();
            EventIterator iter = new EventIterator() {

                public Event nextEvent() {
                    Event event = upstream.next();
                    upstream.remove();
                    return event;
                }

                public long getPosition() {
                    throw new UnsupportedOperationException("skip() is not implemented yet");
                }

                public long getSize() {
                    return size;
                }

                public void skip(long skipNum) {
                    throw new UnsupportedOperationException("skip() is not implemented yet");
                }

                public boolean hasNext() {
                    return upstream.hasNext();
                }

                public Object next() {
                    return nextEvent();
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove() is not implemented yet");
                }

            };
            get().onEvent(iter);
        }
    }

    public static JcrObservationManager getInstance() {
        return INSTANCE;
    }

    ReferenceQueue<EventListener> listenerQueue;
    Map<EventListener, JcrListener> listeners;

    private JcrObservationManager() {
        this.listeners = Collections.synchronizedMap(new WeakHashMap<EventListener, JcrListener>());
        this.listenerQueue = new ReferenceQueue<EventListener>();
    }

    public void addEventListener(EventListener listener, int eventTypes, String absPath, boolean isDeep, String[] uuid,
            String[] nodeTypeName, boolean noLocal) throws RepositoryException {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {
            JcrListener realListener = new JcrListener(session, listener);
            listeners.put(listener, realListener);
            realListener.init(eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
        } else {
            log.error("No session found");
        }
    }

    public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
        throw new UnsupportedOperationException("getRegisteredEventListeners() is not implemented yet");
    }

    public void removeEventListener(EventListener listener) throws RepositoryException {
        cleanup();

        if (listeners.containsKey(listener)) {
            JcrListener realListener = listeners.remove(listener);
            realListener.dispose();
        }
    }

    public void process() {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {
            // copy set of listeners; don't synchronize on map while notifying observers
            // as it may need to be modified as a result of the event.
            Set<Map.Entry<EventListener, JcrListener>> set;
            synchronized (listeners) {
                set = new HashSet<Map.Entry<EventListener, JcrListener>>(listeners.entrySet());
            }
            for (Map.Entry<EventListener, JcrListener> entry : set) {
                JcrListener listener = entry.getValue();
                if (listener.getSession() == session) {
                    listener.process();
                }
            }
        } else {
            log.error("No session found");
        }
    }

    private void cleanup() {
        JcrListener ref;
        synchronized (listeners) {
            // cleanup gc'ed listeners
            while ((ref = (JcrListener) listenerQueue.poll()) != null) {
                ref.dispose();
            }
        }
    }

}