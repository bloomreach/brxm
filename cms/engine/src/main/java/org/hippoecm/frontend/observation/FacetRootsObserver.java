/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.SynchronousEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetRootsObserver implements IFacetRootsObserver {

    static final Logger log = LoggerFactory.getLogger(FacetRootsObserver.class);

    private Set<UpstreamEntry> upstream;
    private boolean broadcast = false;

    public FacetRootsObserver() {
        this.upstream = new HashSet<UpstreamEntry>();
    }

    /**
     * Broadcast facet roots update events to listeners that observe ancestors
     * or descendants of a facet root node.  To be invoked when the session has
     * been refreshed.
     */
    public void broadcastEvents() {
        broadcast = true;
    }

    void refresh() {
        if (broadcast) {
            broadcast = false;
            synchronized (upstream) {
                for (UpstreamEntry upstreamEntry : upstream) {
                    for (FacetRootListener listener : upstreamEntry.rootListeners) {
                        listener.broadcast();
                    }
                }
            }
        }
    }

    void subscribe(JcrListener listener, Node node) throws RepositoryException {
        synchronized (upstream) {
            Session session = node.getSession();
            String uuid = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
            String[] uuids = {uuid};
            if ("hippofacnav:facetnavigation".equals(node.getPrimaryNodeType().getName())) {
                uuids = uuid.split(",\\s*");
            }

            UpstreamEntry entry = new UpstreamEntry();
            entry.listener = listener;
            entry.rootListeners = new LinkedList<FacetRootListener>();
            upstream.add(entry);

            ObservationManager obMgr = session.getWorkspace().getObservationManager();
            for (String id : uuids) {
                try {
                    String docbase = session.getNodeByIdentifier(id).getPath();
                    // CMS7-5568: facet navigation can have multiple docbases.
                    FacetRootListener rootListener = new FacetRootListener(node.getPath(), session.getUserID(), listener);
                    obMgr.addEventListener(rootListener, Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, docbase, true,
                                         null, null, false);
                    entry.rootListeners.add(rootListener);
                } catch (ItemNotFoundException e) {
                    log.warn("The {} property of facet node {} refers to a non-existing UUID '{}'",
                             new Object[]{HippoNodeType.HIPPO_DOCBASE, node.getPath(), uuid});
                }
            }

        }
    }

    void unsubscribe(JcrListener listener, Session session) throws RepositoryException {
        ObservationManager obMgr = session.getWorkspace().getObservationManager();
        synchronized (upstream) {
            Iterator<UpstreamEntry> iter = upstream.iterator();
            while (iter.hasNext()) {
                UpstreamEntry entry = iter.next();
                if (entry.listener == listener) {
                    for (FacetRootListener frl : entry.rootListeners) {
                        obMgr.removeEventListener(frl);
                    }
                    iter.remove();
                }
            }
        }
    }

    private static class UpstreamEntry {
        JcrListener listener;
        LinkedList<FacetRootListener> rootListeners;
    }

    private static class FacetRootListener implements SynchronousEventListener {

        // path to the facet root node
        private String nodePath;
        private volatile boolean refresh = false;
        private final JcrListener listener;
        private final String userID;

        FacetRootListener(String path, String userID, JcrListener listener) {
            this.nodePath = path;
            this.listener = listener;
            this.userID = userID;
        }

        void broadcast() {
            if (refresh) {
                refresh = false;
                listener.onVirtualEvent(new ChangeEvent(nodePath, userID));
            }
        }

        public void onEvent(EventIterator events) {
            refresh = true;
        }

    }

}
