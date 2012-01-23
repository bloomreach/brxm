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
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JcrListener extends WeakReference<EventListener> implements EventListener, Comparable<JcrListener> {
    
    private final static Logger log = LoggerFactory.getLogger(JcrListener.class);
    
    private final static int MAX_EVENTS = Integer.getInteger("hippoecm.observation.maxevents", 10000);
    
    private Map<Session, Map<String, NodeState>> cache;
    
    private String path;
    private int eventTypes;
    private boolean isDeep;
    private Set<String> uuids;
    private String[] nodeTypes;
    private boolean noLocal;

    private boolean isvirtual;
    private List<String> parents;
    private final Queue<Event> events = new ConcurrentLinkedQueue<Event>();
    private Session session;
    private FacetRootsObserver fro;
    private WeakReference<UserSession> sessionRef;

    JcrListener(ReferenceQueue<EventListener> listenerQueue, Map<Session, Map<String, NodeState>> cache, UserSession userSession, EventListener upstream) {
        super(upstream, listenerQueue);
        this.cache = cache;
        sessionRef = new WeakReference<UserSession>(userSession);
    }

    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            this.events.add(events.nextEvent());
        }
        // When the update requests do not arrive anymore,
        // for instance due to the user not properly having closed
        // its session, then the event queue just keeps growing,
        // risking out of memory errors. We therefore set a limit
        // on the amount of events that may reasonably accumulate
        // during a valid session. If this number is exceeded we
        // flush the session causing its listeners to be removed and
        // and its pagemaps to be emptied.
        // This in turn causes wicket to send a page expired response
        // to the browser on the next request that comes in.
        if (this.events.size() > MAX_EVENTS) {
            PluginUserSession session = ((PluginUserSession)getSession());
            if (session != null) {
                String userID = session.getJcrSession().getUserID();
                log.warn("The event queue is full. Flushing session of user " + userID);
                session.flush();
            } else {
                // The wicket session was serialized but the JCR Session not yet logged out
                // through JcrSessionReference.cleanup()
                // When the wicket session becomes active again a reinitialization of the 
                // session will occur so we don't need these events anymore.
                log.info("The event queue is full. Clearing pending events.");
                this.events.clear();
            }
        }
    }

    @Override
    public int compareTo(final JcrListener other) {
        int result = path.compareTo(other.path);
        if (result == 0) {
            return Integer.valueOf(hashCode()).compareTo(other.hashCode());
        }
        return result;
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

        if (session != null) {
            subscribe();
        } else {
            log.error("No jcr session bound to wicket session");
        }

        parents = new ArrayList<String>();
        if (!isDeep) {
            parents.add(absPath);
        } else if (uuids != null) {
            for (Node node : getReferencedNodes()) {
                parents.add(node.getPath());
            }
        }
    }
    
    List<String> getParents() {
        return this.parents;
    }
    
    String getPath() {
        return this.path;
    }
    
    UserSession getSession() {
        return sessionRef.get();
    }
    
    void getChanges(Set<String> paths) {
        try {
            checkSession();
        } catch (ObservationException e1) {
            return;
        }

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

    void dispose() {
        if (session != null) {
            if (log.isDebugEnabled()) {
                log.debug("disposing listener " + this);
            }
            try {
                unsubscribe();
            } catch (RepositoryException ex) {
                log.error("Unable to unregister event listener, " + ex.getMessage());
            }
            session = null;
        }
        events.clear();
    }

    private void subscribe() throws RepositoryException {
        ObservationManager obMgr = session.getWorkspace().getObservationManager();
        String[] uuid = null;
        if (uuids != null) {
            uuid = uuids.toArray(new String[uuids.size()]);
        }
        obMgr.addEventListener(this, eventTypes, path, isDeep, uuid, nodeTypes, noLocal);

        // subscribe to facet search observer.
        // FIXME due to HREPTWO-2655, will not be able to receive events on newly
        // created facet search nodes.
        fro = (FacetRootsObserver) getSession().getFacetRootsObserver();

        // subscribe when listening to deep tree structures;
        // there will/might be facetsearches in there.
        if (isDeep && uuids == null) {
            if (nodeTypes == null) {
                fro.subscribe(this, path);
            } else {
                for (String type : nodeTypes) {
                    if (type.equals(HippoNodeType.NT_DOCUMENT)) {
                        fro.subscribe(this, path);
                    }
                }
            }
            return;
        }

        // subscribe when target has a facetsearch as an ancestor
        try {
            for (Node node = getRoot(); node.getDepth() > 0;) {
                if (node.isNodeType(HippoNodeType.NT_FACETSEARCH)) {
                    fro.subscribe(this, node.getPath());
                    break;
                }
                node = node.getParent();
            }

            for (Node node : getReferencedNodes()) {
                if (node.isNodeType(HippoNodeType.NT_FACETSEARCH)) {
                    fro.subscribe(this, node.getPath());
                }
            }
        } catch (PathNotFoundException pnfe) {
            log.warn("Path no longer exists, stopping observation; " + pnfe.getMessage());
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private void unsubscribe() throws RepositoryException {
        fro.unsubscribe(this);
        fro = null;

        if (session.isLive()) {
            ObservationManager obMgr = session.getWorkspace().getObservationManager();
            obMgr.removeEventListener(this);
        }
        session = null;
    }

    private Node getRoot() throws PathNotFoundException, RepositoryException {
        Session session = getSession().getJcrSession();
        if ("/".equals(path)) {
            return session.getRootNode();
        } else {
            return session.getRootNode().getNode(path.substring(1));
        }
    }

    private List<Node> getReferencedNodes() {
        if (uuids != null) {
            Session session = getSession().getJcrSession();
            ArrayList<Node> list = new ArrayList<Node>(uuids.size());
            Set<String> validUuids = new HashSet<String>(uuids.size());
            for (String id : uuids) {
                try {
                    list.add(session.getNodeByUUID(id));
                    validUuids.add(id);
                } catch (ItemNotFoundException e) {
                    log.info("Could not dereference uuid {} : {}", id, e.getMessage());
                } catch (RepositoryException e) {
                    log.warn("Could not dereference uuid {} : {}", id, e.getMessage());
                }
            }
            uuids = validUuids;
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isVirtual(Node node) throws RepositoryException {
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

    private boolean isVisible(Node node) {
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

    private NodeState getNodeState(Session session, String path) {
        synchronized (cache) {
            Map<String, NodeState> states = cache.get(session);
            if (states == null) {
                states = new HashMap<String, NodeState>();
                cache.put(session, states);
            }
            return states.get(path);
        }
    }
    
    private void processPending(NodeIterator iter, Set<Node> nodes) {
        while (iter.hasNext()) {
            Node node = iter.nextNode();
            if (node != null) {
                if (isVisible(node)) {
                    nodes.add(node);
                }
            }
        }
    }

    private void expandNew(final Set<Node> nodes) {
        for (Node node : new ArrayList<Node>(nodes)) {
            if (node.isNew()) {
                ItemVisitor visitor = new TraversingItemVisitor() {
                    public void visit(Node node) throws RepositoryException {
                        // do not traverse into virtual paths
                        if(!(node instanceof HippoNode) || node.isSame(((HippoNode)node).getCanonicalNode())) {
                            super.visit(node);
                        }
                    }

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

    private boolean blocks(Event event) throws RepositoryException {

        // check event type
        long type = event.getType();
        if (type != 0 && (eventTypes & type) == 0) {
            return true;
        }

        String eventPath = event.getPath();
        if (type != 0) {
            eventPath = eventPath.substring(0, eventPath.lastIndexOf('/'));
            if (eventPath.equals("")) {
                eventPath = "/";
            }
        }
        if (!session.itemExists(eventPath)) {
            return true;
        }
        Node parent = (Node) session.getItem(eventPath);

        // check UUIDs
        if (uuids != null) {
            if (!parent.isNodeType("mix:referenceable")) {
                return true;
            }
            String parentId = parent.getUUID();
            boolean match = false;
            for (String uuid : uuids) {
                if (uuid.equals(parentId)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return true;
            }
        }

        // check node types
        if (nodeTypes != null) {
            NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
            Set<NodeType> eventTypes = new HashSet<NodeType>();
            eventTypes.add(parent.getPrimaryNodeType());
            if (parent.hasProperty("jcr:mixinTypes")) {
                Value[] mixins = parent.getProperty("jcr:mixinTypes").getValues();
                for (Value mixin : mixins) {
                    eventTypes.add(ntMgr.getNodeType(mixin.getString()));
                }
            }
            boolean match = false;
            for (int i = 0; i < nodeTypes.length && !match; i++) {
                for (Iterator<NodeType> iter = eventTypes.iterator(); iter.hasNext();) {
                    NodeType nodeType = iter.next();
                    if (nodeType.isNodeType(nodeTypes[i])) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    break;
                }
            }
            if (!match) {
                return true;
            }
        }

        // finally check path
        boolean match = eventPath.equals(path);
        if (!match && isDeep) {
            match = isAncestor(path, eventPath);
        }
        return !match;
    }

    private void checkSession() throws ObservationException {
        // listeners can be invoked after they have been removed
        if (session == null) {
            throw new ObservationException("Listener " + this + " is no longer registerd");
        } else if (!session.isLive()) {
            log.info("resubscribing listener " + this);

            // events have references to the session, so they are useless now
            events.clear();
            try {
                unsubscribe();
            } catch (RepositoryException ex) {
                log.debug("Failed to unsubscribe");
            }

            // get new session and subscribe event listener
            session = getSession().getJcrSession();
            if (session != null) {
                try {
                    subscribe();
                } catch (RepositoryException x) {
                    log.error("Failed to re-subscribe");
                }
            } else {
                throw new ObservationException("No session found");
            }
        }
    }

    void process(Map<String, NodeState> dirty) {
        try {
            checkSession();
        } catch (ObservationException ex) {
            log.debug(ex.getMessage(), ex);
            return;
        }
        

        List<Event> events = getEvents(dirty);
        final Iterator<Event> upstream = events.iterator();
        final long size = events.size();
        if (size > 0) {
            EventIterator iter = new EventIterator() {

                public Event nextEvent() {
                    Event event = upstream.next();
                    if (log.isDebugEnabled()) {
                        log.debug("processing " + event.toString() + ", session " + sessionRef.get().getId());
                    }
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
            } catch (RuntimeException ex) {
                log.error("Error occured when processing event", ex);
            }
        }
    }

    private List<Event> getEvents(Map<String, NodeState> dirty) {
        List<Event> events = new LinkedList<Event>(this.events);
        this.events.clear();

        if (isvirtual) {
            return events;
        }

        // process pending changes
        Node root = null;
        Set<Node> nodes = new TreeSet<Node>(new NodePathComparator());
        try {
            root = getRoot();
            if (nodeTypes == null) {
                if ((root.isModified() || root.isNew()) && isVisible(root)) {
                    nodes.add(root);
                }
                // use pendingChanges to detect changes in sub-trees and properties
                if (!root.isNew()) {
                    NodeIterator iter = ((HippoSession) root.getSession()).pendingChanges(root, null, false);
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
                        NodeIterator iter = ((HippoSession) root.getSession()).pendingChanges(root, type, false);
                        processPending(iter, nodes);
                    }
                }
            }
        } catch (PathNotFoundException ex) {
            log.warn("Root node no longer exists: " + ex.getMessage());
            dispose();
            return events;
        } catch (RepositoryException ex) {
            log.error("Failed to parse pending changes", ex);
            dispose();
            return events;
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

        for (Node node : nodes) {
            try {
                String path = node.getPath();

                NodeState newState;
                if (dirty.containsKey(path)) {
                    newState = dirty.get(path);
                } else {
                    newState = new NodeState(node, false);
                    dirty.put(path, newState);
                }

                NodeState oldState = getNodeState(session, path);
                if (oldState != null) {
                    Iterator<Event> iter = oldState.getEvents(newState);
                    while (iter.hasNext()) {
                        Event event = iter.next();
                        if (!blocks(event)) {
                            events.add(event);
                        }
                    }
                } else {
                    Event changeEvent = newState.getChangeEvent();
                    if (!blocks(changeEvent)) {
                        events.add(changeEvent);
                    }
                }
            } catch (RepositoryException e) {
                log.warn("Failed to process node", e);
            }
        }
        return events;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("path", path)
                .append("isDeep", isDeep).append("UUIDs", uuids).toString();
    }
    
    private static boolean isAncestor(String candidate, String path) {
        String[] cParts = candidate.split("/");
        String[] pParts = path.split("/");

        // if candidate has more elements than path, then it cannot be an ancestor
        if (cParts.length > pParts.length) {
            return false;
        }

        for (int i = 0; i < cParts.length; i++) {
            String cEl = cParts[i];
            String pEl = pParts[i];
            if (cEl.endsWith("[1]")) {
                cEl = cEl.substring(0, cEl.length() - 3);
            }
            if (pEl.endsWith("[1]")) {
                pEl = pEl.substring(0, pEl.length() - 3);
            }
            if (!cEl.equals(pEl)) {
                return false;
            }
        }
        return true;
    }
    
    static class NodePathComparator implements Comparator<Node> {

        public int compare(Node o1, Node o2) {
            try {
                return o1.getPath().compareTo(o2.getPath());
            } catch (RepositoryException ex) {
                return 0;
            }
        }

    }
}