/*
 *  Copyright 2008-2011 Hippo.
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
package org.hippoecm.frontend.observation;

import java.lang.ref.ReferenceQueue;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventJournal;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrObservationManager implements ObservationManager {
    
    private final static Logger log = LoggerFactory.getLogger(JcrObservationManager.class);
    private final static JcrObservationManager INSTANCE = new JcrObservationManager();

    private final WeakHashMap<Session, Map<String, NodeState>> cache = new WeakHashMap<Session, Map<String, NodeState>>();
    private final ReferenceQueue<EventListener> listenerQueue;
    private final Map<EventListener, JcrListener> listeners;

    private JcrObservationManager() {
        this.listeners = new WeakHashMap<EventListener, JcrListener>();
        this.listenerQueue = new ReferenceQueue<EventListener>();
    }
    
    public void setUserData(String userData) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public EventJournal getEventJournal() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public EventJournal getEventJournal(int eventTypes, String absPath, boolean isDeep, String[] uuid,
            String[] nodeTypeName) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static JcrObservationManager getInstance() {
        return INSTANCE;
    }

    public void addEventListener(EventListener listener, int eventTypes, String absPath, boolean isDeep, String[] uuid,
            String[] nodeTypeName, boolean noLocal) throws RepositoryException {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {

            Session jcrSession = session.getJcrSession();
            Map<String, NodeState> states;
            synchronized (cache) {
                states = cache.get(jcrSession);
                if (states == null) {
                    states = new HashMap<String, NodeState>();
                    cache.put(jcrSession, states);
                }
            }

            JcrListener realListener = new JcrListener(listenerQueue, states, session, listener);
            try {
                realListener.init(eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
                synchronized (listeners) {
                    listeners.put(listener, realListener);
                }

                // prefetch fixed nodes into cache
                if (realListener.getParents().size() > 0) {
                    for (String path : realListener.getParents()) {
                        if (!states.containsKey(path)) {
                            try {
                                if (jcrSession.itemExists(path)) {
                                    NodeState state = new NodeState((Node) jcrSession.getItem(path), true);
                                    states.put(path, state);
                                }
                            } catch (RepositoryException ex) {
                                log.warn("Failed to initialize node state", ex);
                            }
                        }
                    }
                }
            } catch (ObservationException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("No session found");
        }
    }

    public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
        // create a local copy of the event listeners
        final Set<EventListener> currentListeners = new HashSet<EventListener>();
        synchronized (listeners) {
            for (EventListener el : listeners.keySet()) {
                currentListeners.add(el);
            }
        }

        return new SimpleEventListenerIterator(currentListeners);
    }

    public void removeEventListener(EventListener listener) throws RepositoryException {
        cleanup();

        JcrListener realListener = null;
        synchronized (listeners) {
            if (listeners.containsKey(listener)) {
                realListener = listeners.remove(listener);
            }
        }
        if (realListener != null) {
            realListener.dispose();
        } else {
            log.info("Listener was not registered");
        }
    }

    private void prune(Set<String> paths) {
        // filter out descendants
        Iterator<String> pathIter = paths.iterator();
        while (pathIter.hasNext()) {
            String[] ancestors = pathIter.next().split("/");
            StringBuilder compound = new StringBuilder("/");
            for (int i = 1; i < ancestors.length - 1; i++) {
                compound.append(ancestors[i]);
                if (paths.contains(compound.toString())) {
                    pathIter.remove();
                    break;
                }
                compound.append('/');
            }
        }
    }

    public void refreshSession() {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {
            // copy set of listeners; don't synchronize on map while notifying observers
            // as it may need to be modified as a result of the event.
            SortedSet<JcrListener> set = new TreeSet<JcrListener>(new Comparator<JcrListener>() {

                public int compare(JcrListener o1, JcrListener o2) {
                    int result = o1.getPath().compareTo(o2.getPath());
                    if (result == 0) {
                        return Integer.valueOf(o1.hashCode()).compareTo(o2.hashCode());
                    }
                    return result;
                }

            });
            synchronized (listeners) {
                for (JcrListener listener : listeners.values()) {
                    if (listener.getSession() == session) {
                        set.add(listener);
                    }
                }
            }

            // notify facet search listeners.
            // FIXME due to HREPTWO-2655, will not be able to receive events on newly
            // created facet search nodes.
            FacetRootsObserver fso = (FacetRootsObserver) session.getFacetRootsObserver();
            fso.refresh();

            Node root = session.getRootNode();
            if (root != null) {
                try {
                    root.refresh(true);
                } catch (RepositoryException ex) {
                    log.error("Failed to refresh session", ex);
                }
            } else {
                log.info("Root not found; cleaning up listeners");
                synchronized (listeners) {
                    for (Iterator<JcrListener> iter = listeners.values().iterator(); iter.hasNext();) {
                        JcrListener listener = iter.next();
                        if (listener.getSession() == session) {
                            iter.remove();
                        }
                    }
                }
            }
        } else {
            log.error("No session found");
        }
    }

    public void cleanupListeners(UserSession session) {
        cleanup();

        synchronized (listeners) {
            for (Iterator<JcrListener> iter = listeners.values().iterator(); iter.hasNext();) {
                JcrListener listener = iter.next();
                if (listener.getSession() == session) {
                    iter.remove();
                    listener.dispose();
                }
            }
        }
    }

    public void processEvents() {
        cleanup();

        UserSession session = UserSession.get();
        if (session != null) {
            // copy set of listeners; don't synchronize on map while notifying observers
            // as it may need to be modified as a result of the event.
            SortedSet<JcrListener> set = new TreeSet<JcrListener>();
            synchronized (listeners) {
                for (JcrListener listener : listeners.values()) {
                    if (listener.getSession() == session) {
                        set.add(listener);
                    }
                }
            }

            Map<String, NodeState> dirty = new HashMap<String, NodeState>();
            for (JcrListener listener : set) {
                listener.process(dirty);
            }

            Session jcrSession = session.getJcrSession();
            Map<String, NodeState> states;
            synchronized (cache) {
                states = cache.get(jcrSession);
                if (states == null) {
                    states = new HashMap<String, NodeState>();
                    cache.put(jcrSession, states);
                }
            }

            // update cache
            for (Map.Entry<String, NodeState> nodes : dirty.entrySet()) {
                states.put(nodes.getKey(), nodes.getValue());
            }

            // remove stale entries
            Iterator<Map.Entry<String, NodeState>> cacheIter = states.entrySet().iterator();
            while (cacheIter.hasNext()) {
                Map.Entry<String, NodeState> entry = cacheIter.next();
                try {
                    if (!jcrSession.itemExists(entry.getKey())) {
                        cacheIter.remove();
                    }
                } catch (RepositoryException ex) {
                    log.warn("Could not determine whether " + entry.getKey() + " exists", ex);
                }
            }
        } else {
            log.error("No session found");
        }
    }

    private void cleanup() {
        synchronized (listeners) {
            // cleanup weak-ref-table
            listeners.size();

            // cleanup gc'ed listeners
            JcrListener jcrListener;
            while ((jcrListener = (JcrListener) listenerQueue.poll()) != null) {
                jcrListener.dispose();
            }
        }
    }

    private static class SimpleEventListenerIterator implements EventListenerIterator {
        private final Iterator<EventListener> listenerIterator;
        private final long size;
        private long pos;

        public SimpleEventListenerIterator(final Set<EventListener> listeners) {
            listenerIterator = listeners.iterator();
            size = listeners.size();
            pos = 0;
        }

        public EventListener nextEventListener() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            pos++;
            return listenerIterator.next();
        }

        public void skip(long skipNum) {
            while (skipNum-- > 0) {
                next();
            }
        }

        public long getSize() {
            return size;
        }

        public long getPosition() {
            return pos;
        }

        public void remove() {
            throw new UnsupportedOperationException("EventListenerIterator.remove()");
        }

        public boolean hasNext() {
            return listenerIterator.hasNext();
        }

        public Object next() {
            return nextEventListener();
        }
    }

}
