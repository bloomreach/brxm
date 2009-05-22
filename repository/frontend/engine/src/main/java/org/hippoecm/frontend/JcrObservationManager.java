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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum TriState {
    FALSE, TRUE, UNKNOWN
};

public class JcrObservationManager implements ObservationManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(JcrObservationManager.class);

    private static JcrObservationManager INSTANCE = new JcrObservationManager();

    private static class ObservationException extends Exception {
        private static final long serialVersionUID = 1L;

        public ObservationException(String message) {
            super(message);
        }
    }

    private class NodeCache {

        private class NodeEvent implements Event {

            int type;
            String name;

            NodeEvent(String name, int type) {
                this.name = name;
                this.type = type;
                if (type != 0 && name == null) {
                    throw new IllegalArgumentException("Name is mandatory when type of event is known");
                }
            }

            public String getPath() throws RepositoryException {
                if (type != 0) {
                    return path + "/" + name;
                } else {
                    return path;
                }
            }

            public int getType() {
                return type;
            }

            public String getUserID() {
                return userId;
            }

        }

        private String path;
        private String userId;
        private Map<String, Object> properties;
        private List<String> nodes;

        NodeCache(Node node) throws RepositoryException {
            this.path = node.getPath();
            this.userId = node.getSession().getUserID();

            properties = new HashMap<String, Object>();
            nodes = new LinkedList<String>();

            process(node, properties, nodes);
        }

        void process(Node node, Map<String, Object> properties, List<String> nodes) throws RepositoryException {
            PropertyIterator propIter = node.getProperties();
            while (propIter.hasNext()) {
                Property property = propIter.nextProperty();
                JcrPropertyModel propertyModel = new JcrPropertyModel(property);
                if (property.getDefinition().isMultiple()) {
                    List<Object> values = new ArrayList<Object>(propertyModel.size());
                    Iterator<?> iter = propertyModel.iterator(0, propertyModel.size());
                    while (iter.hasNext()) {
                        values.add(propertyModel.model(iter.next()).getObject());
                    }
                    properties.put(property.getName(), values);
                } else {
                    JcrPropertyValueModel pvm = new JcrPropertyValueModel(propertyModel);
                    properties.put(property.getName(), pvm.getObject());
                }
            }

            NodeIterator nodeIter = node.getNodes();
            while (nodeIter.hasNext()) {
                Node child = nodeIter.nextNode();
                nodes.add(child.getName() + "[" + child.getIndex() + "]");
            }
        }

        Iterator<Event> update(Node node) throws RepositoryException {
            List<Event> events = new LinkedList<Event>();

            Map<String, Object> properties = new HashMap<String, Object>();
            List<String> nodes = new LinkedList<String>();
            process(node, properties, nodes);

            for (Map.Entry<String, Object> entry : this.properties.entrySet()) {
                if (properties.containsKey(entry.getKey())) {
                    Object value = properties.get(entry.getKey());
                    if (!value.equals(entry.getValue())) {
                        events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_CHANGED));
                    }
                } else {
                    events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_REMOVED));
                }
            }
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (!this.properties.containsKey(entry.getKey())) {
                    events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_ADDED));
                }
            }

            for (String child : nodes) {
                if (!this.nodes.contains(child)) {
                    events.add(new NodeEvent(child, Event.NODE_ADDED));
                }
            }
            for (String child : this.nodes) {
                if (!nodes.contains(child)) {
                    events.add(new NodeEvent(child, Event.NODE_REMOVED));
                }
            }

            this.properties = properties;
            this.nodes = nodes;
            return events.iterator();
        }
    }

    private class JcrListener extends WeakReference<EventListener> implements EventListener {
        String path;
        int eventTypes;
        boolean isDeep;
        Set<String> uuids;
        String[] nodeTypes;
        boolean noLocal;

        boolean isvirtual;
        Map<String, NodeCache> pending;
        List<Event> events;
        Session session;
        FacetSearchObserver fso;
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

        void init(int eventTypes, String absPath, boolean isDeep, String[] uuid, String[] nodeTypes, boolean noLocal)
                throws RepositoryException, ObservationException {
            if (sessionRef.get() == null) {
                throw new ObservationException("No session found");
            }
            if (!absPath.startsWith("/")) {
                throw new ObservationException("Invalid path");
            }

            this.path = absPath;
            this.eventTypes = eventTypes;
            this.isDeep = isDeep;
            if (uuid != null) {
                this.uuids = new HashSet<String>();
                for (String id : uuid) {
                    uuids.add(id);
                }
            }
            this.nodeTypes = nodeTypes;
            this.noLocal = noLocal;

            this.isvirtual = isVirtual(getRoot());

            session = getSession().getJcrSession();
            pending = new HashMap<String, NodeCache>();

            if (session != null) {
                subscribe();
            } else {
                log.error("No jcr session bound to wicket session");
            }
        }

        void dispose() {
            if (session != null) {
                try {
                    unsubscribe();
                } catch (RepositoryException ex) {
                    log.error("Unable to unregister event listener, " + ex.getMessage());
                }
                session = null;
            }
            events.clear();
            pending = null;
        }

        void subscribe() throws RepositoryException {
            ObservationManager obMgr = session.getWorkspace().getObservationManager();
            String[] uuid = null;
            if (uuids != null) {
                uuid = uuids.toArray(new String[uuids.size()]);
            }
            obMgr.addEventListener(this, eventTypes, path, isDeep, uuid, nodeTypes, noLocal);

            fso = getSession().getJcrSessionModel().getFacetSearchObserver();

            // subscribe when listening to deep tree structures;
            // there will/might be facetsearches in there.
            if (isDeep && uuids == null) {
                if (nodeTypes == null) {
                    fso.subscribe(this, path);
                } else {
                    for (String type : nodeTypes) {
                        if (type.equals(HippoNodeType.NT_DOCUMENT)) {
                            fso.subscribe(this, path);
                        }
                    }
                }
                return;
            }

            // subscribe when target has a facetsearch as an ancestor
            try {
                for (Node node = getRoot(); node.getDepth() > 0;) {
                    if (node.isNodeType(HippoNodeType.NT_FACETSEARCH)) {
                        fso.subscribe(this, node.getPath());
                        break;
                    }
                    node = node.getParent();
                }

                for (Node node : getReferencedNodes()) {
                    if (node.isNodeType(HippoNodeType.NT_FACETSEARCH)) {
                        fso.subscribe(this, node.getPath());
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }

        void unsubscribe() throws RepositoryException {
            fso.unsubscribe(this);
            fso = null;

            if (session.isLive()) {
                ObservationManager obMgr = session.getWorkspace().getObservationManager();
                obMgr.removeEventListener(this);
            }
            session = null;
        }

        UserSession getSession() {
            return sessionRef.get();
        }

        Node getRoot() throws PathNotFoundException, RepositoryException {
            Session session = getSession().getJcrSession();
            if ("/".equals(path)) {
                return session.getRootNode();
            } else {
                return session.getRootNode().getNode(path.substring(1));
            }
        }

        List<Node> getReferencedNodes() {
            if (uuids != null) {
                Session session = getSession().getJcrSession();
                ArrayList<Node> list = new ArrayList<Node>(uuids.size());
                for (String id : uuids) {
                    try {
                        list.add(session.getNodeByUUID(id));
                    } catch (ItemNotFoundException e) {
                        log.warn("Could not dereference uuid {} : {}", id, e.getMessage());
                    } catch (RepositoryException e) {
                        log.warn("Could not dereference uuid {} : {}", id, e.getMessage());
                    }
                }
                return list;
            } else {
                return Collections.emptyList();
            }
        }

        boolean isVirtual(Node node) throws RepositoryException {
            if (node instanceof HippoNode) {
                try {
                    Node canonical = ((HippoNode) node).getCanonicalNode();
                    if (canonical == null) {
                        return true;
                    }
                    if (!canonical.isSame(node)) {
                        return true;
                    }
                } catch (ItemNotFoundException e) {
                    log.debug("Item not found, assuming node was virtual: " + e.getMessage());
                    return true;
                }
            }
            return false;
        }

        boolean isVisible(Node node) {
            try {
                String path = node.getPath();
                if ((isDeep && path.startsWith(this.path)) || path.equals(this.path)) {
                    if (uuids != null) {
                        if (node.isNodeType("mix:referenceable") && uuids.contains(node.getUUID())) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            } catch (RepositoryException e) {
                log.warn("Unable to determine if node is visible, defaulting to false: " + e.getMessage());
            }
            return false;
        }

        void processPending(NodeIterator iter, List<Node> nodes) {
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node != null) {
                    if (isVisible(node)) {
                        nodes.add(node);
                    }
                }
            }
        }

        void expandNew(final List<Node> nodes) {
            for (Node node : new ArrayList<Node>(nodes)) {
                if (node.isNew()) {
                    ItemVisitor visitor = new TraversingItemVisitor() {

                        @Override
                        protected void entering(Property property, int level) throws RepositoryException {
                        }

                        @Override
                        protected void entering(Node node, int level) throws RepositoryException {
                            if (level > 0) {
                                nodes.add(node);
                            }
                        }

                        @Override
                        protected void leaving(Property property, int level) throws RepositoryException {
                        }

                        @Override
                        protected void leaving(Node node, int level) throws RepositoryException {
                        }

                    };
                    try {
                        visitor.visit(node);
                    } catch (RepositoryException e) {
                        log.warn("Error during visiting node", e);
                    }
                }
            }
        }

        synchronized void getChanges(Set<String> paths) {
            if (events.size() > 0) {
                for (Event event : events) {
                    String path;
                    try {
                        path = event.getPath();
                        if (event.getType() != 0) {
                            path = path.substring(0, path.lastIndexOf('/'));
                        }
                        paths.add(path);
                    } catch (RepositoryException e) {
                        log.warn("Failed to get path from event", e);
                    }
                }
            }
        }

        synchronized void process() {
            // listeners can be invoked after they have been removed
            if (session == null) {
                log.debug("Listener " + this + " is no longer registerd");
                return;
            } else if (!session.isLive()) {
                events.clear();
                try {
                    unsubscribe();
                } catch (RepositoryException ex) {
                    log.debug("Failed to unsubscribe");
                }
                session = getSession().getJcrSession();
                if (session != null) {
                    try {
                        subscribe();
                    } catch (RepositoryException x) {
                        log.error("Failed to re-subscribe");
                    }
                } else {
                    return;
                }
            }

            // process pending changes
            Node root = null;
            List<Node> nodes = new LinkedList<Node>();
            if (!isvirtual) {
                try {
                    root = getRoot();
                    if (nodeTypes == null) {
                        if ((root.isModified() || root.isNew()) && isVisible(root)) {
                            nodes.add(root);
                        }
                        // use pendingChanges to detect changes in sub-trees and properties
                        if (!root.isNew()) {
                            NodeIterator iter = ((HippoSession) root.getSession()).pendingChanges(root, null, true);
                            processPending(iter, nodes);
                        }
                    } else {
                        if ((root.isModified() || root.isNew()) && isVisible(root)) {
                            for (String type : nodeTypes) {
                                try {
                                    if (root.isNodeType(type)) {
                                        nodes.add(root);
                                        break;
                                    }
                                } catch (RepositoryException e) {
                                    log.debug("Unable to determine if node is of type " + type, e);
                                }
                            }
                        }
                        // use pendingChanges to detect changes in sub-trees and properties
                        if (!root.isNew()) {
                            for (String type : nodeTypes) {
                                NodeIterator iter = ((HippoSession) root.getSession()).pendingChanges(root, type, true);
                                processPending(iter, nodes);
                            }
                        }
                    }
                } catch (PathNotFoundException ex) {
                    log.warn("Root node no longer exists: " + ex.getMessage());
                    dispose();
                    return;
                } catch (RepositoryException ex) {
                    log.error("Failed to parse pending changes", ex);
                    dispose();
                    return;
                }

                for (Node node : getReferencedNodes()) {
                    if (nodeTypes == null) {
                        if (node.isModified() || node.isNew()) {
                            nodes.add(node);
                        }
                    } else {
                        if (node.isModified() || node.isNew()) {
                            for (String type : nodeTypes) {
                                try {
                                    if (root.isNodeType(type)) {
                                        nodes.add(root);
                                        break;
                                    }
                                } catch (RepositoryException e) {
                                    log.debug("Unable to determine if node is of type " + type, e);
                                }
                            }
                        }
                    }
                }

                expandNew(nodes);

                List<String> paths = new LinkedList<String>();
                for (Node node : nodes) {
                    String path;
                    try {
                        path = node.getPath();
                        paths.add(path);
                        if (pending.containsKey(path)) {
                            Iterator<Event> iter = pending.get(path).update(node);
                            while (iter.hasNext()) {
                                events.add(iter.next());
                            }
                        } else {
                            NodeCache cache = new NodeCache(node);
                            pending.put(path, cache);
                            events.add(cache.new NodeEvent(null, 0));
                        }
                    } catch (RepositoryException e) {
                        log.warn("Failed to process node", e);
                    }
                }

                for (String path : new ArrayList<String>(pending.keySet())) {
                    if (!paths.contains(path)) {
                        NodeCache cache = pending.remove(path);
                        events.add(cache.new NodeEvent(null, 0));
                    }
                }
            }

            final Iterator<Event> upstream = events.iterator();
            final long size = events.size();
            if (size > 0) {
                EventIterator iter = new EventIterator() {

                    public Event nextEvent() {
                        Event event = upstream.next();
                        log.debug("processing " + event.toString() + ", session " + sessionRef.get().getId());
                        return event;
                    }

                    public long getPosition() {
                        throw new UnsupportedOperationException("getPosition() is not implemented yet");
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
                try {
                    EventListener listener = get();
                    if (listener != null) {
                        listener.onEvent(iter);
                    } else {
                        log.info("Listener disappeared during processing");
                    }
                    events.clear();
                } catch (RuntimeException ex) {
                    log.error("Error occured when processing event", ex);
                }
            }
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("path", path).append("isDeep",
                    isDeep).append("UUIDs", uuids).toString();
        }
    }

    public static JcrObservationManager getInstance() {
        return INSTANCE;
    }

    ReferenceQueue<EventListener> listenerQueue;
    Map<EventListener, JcrListener> listeners;

    private JcrObservationManager() {
        this.listeners = new WeakHashMap<EventListener, JcrListener>();
        this.listenerQueue = new ReferenceQueue<EventListener>();
    }

    public void addEventListener(EventListener listener, int eventTypes, String absPath, boolean isDeep, String[] uuid,
            String[] nodeTypeName, boolean noLocal) throws RepositoryException {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {
            JcrListener realListener = new JcrListener(session, listener);
            try {
                realListener.init(eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
                synchronized (listeners) {
                    listeners.put(listener, realListener);
                }
            } catch (ObservationException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("No session found");
        }
    }

    public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
        throw new UnsupportedOperationException("getRegisteredEventListeners() is not implemented yet");
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
                    int result = o1.path.compareTo(o2.path);
                    if (result == 0) {
                        return new Integer(o1.hashCode()).compareTo(o2.hashCode());
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

            // create set of paths that need to be refreshed
            Set<String> paths = new TreeSet<String>();
            for (JcrListener listener : set) {
                listener.getChanges(paths);
            }

            try {
                if (paths.contains("")) {
                    session.getRootNode().refresh(true);
                } else {
                    prune(paths);

                    // do the refresh
                    for (String path : paths) {
                        log.info("Refreshing {}, keeping changes", path);
                        try {
                            session.getRootNode().getNode(path.substring(1)).refresh(true);
                        } catch (PathNotFoundException ex) {
                            log.error("Could not find path " + path + " for event, discarding event and continue: " + ex.getMessage());
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error("Failed to refresh session", ex);
            }
        } else {
            log.error("No session found");
        }
    }

    void processEvents() {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {
            // copy set of listeners; don't synchronize on map while notifying observers
            // as it may need to be modified as a result of the event.
            SortedSet<JcrListener> set = new TreeSet<JcrListener>(new Comparator<JcrListener>() {

                public int compare(JcrListener o1, JcrListener o2) {
                    int result = o1.path.compareTo(o2.path);
                    if (result == 0) {
                        return new Integer(o1.hashCode()).compareTo(o2.hashCode());
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

            for (JcrListener listener : set) {
                listener.process();
            }
        } else {
            log.error("No session found");
        }
    }

    private void cleanup() {
        JcrListener ref;
        synchronized (listeners) {
            // cleanup weak-ref-table
            listeners.size();

            // cleanup gc'ed listeners
            while ((ref = (JcrListener) listenerQueue.poll()) != null) {
                ref.dispose();
            }
        }
    }

}
