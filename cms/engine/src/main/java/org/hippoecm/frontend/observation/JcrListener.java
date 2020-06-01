/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.util.TraversingItemVisitor;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.util.collections.ConcurrentHashSet;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.SynchronousEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JcrListener extends WeakReference<EventListener> implements SynchronousEventListener, Comparable<JcrListener> {
    
    private static final Logger log = LoggerFactory.getLogger(JcrListener.class);
    
    private static final int MAX_EVENTS = Integer.getInteger("hippoecm.observation.maxevents", 10000);

    private final Map<String, NodeState> stateCache;
    
    private String path;
    private String listenerPath;
    private int eventTypes;
    private boolean isDeep;
    private List<String> uuids;
    private String[] nodeTypes;
    private boolean noLocal;

    private boolean isvirtual;
    private List<String> parents;
    private final List<Event> virtualEvents = new LinkedList<Event>();
    private final Queue<Event> jcrEvents = new ConcurrentLinkedQueue<>();

    // events not the result of jcr changes but of manually created ChangeEvents
    private final Set<ChangeEvent> dispatchedEvents = new ConcurrentHashSet<>();
    private Session session;
    private FacetRootsObserver fro;
    private WeakReference<UserSession> sessionRef;
    private final InternalCmsEventDispatcherService cmsEventDispatcherService;

    JcrListener(final ReferenceQueue<EventListener> listenerQueue,
                final Map<String, NodeState> stateCache,
                final UserSession userSession,
                final EventListener upstream,
                final InternalCmsEventDispatcherService cmsEventDispatcherService) {
        super(upstream, listenerQueue);
        this.stateCache = stateCache;
        sessionRef = new WeakReference<UserSession>(userSession);
        this.cmsEventDispatcherService = cmsEventDispatcherService;
    }

    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            this.jcrEvents.add(events.nextEvent());
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
        if (this.jcrEvents.size() > MAX_EVENTS) {
            PluginUserSession userSession = ((PluginUserSession)getSession());
            if (userSession != null) {
                String userID = userSession.getJcrSession().getUserID();
                log.warn("The event queue is full. Flushing session of user {}", userID);
                userSession.flush();
            }
        }
    }

    public void onEvent(ChangeEvent dispatched) {
        dispatchedEvents.add(dispatched);
    }

    public void onVirtualEvent(final Event event) {
        virtualEvents.add(event);
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
            this.uuids = Arrays.asList(uuid);
        }
        this.nodeTypes = nodeTypes;
        this.noLocal = noLocal;

        this.isvirtual = JcrHelper.isVirtualNode(getRoot());

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

        addParentsToCache(stateCache);
    }

    private void addParentsToCache(final Map<String, NodeState> stateCache) {
        if (session != null) {
            synchronized (stateCache) {
                // prefetch fixed nodes into cache
                for (String path : getParents()) {
                    if (!stateCache.containsKey(path)) {
                        try {
                            if (session.nodeExists(path)) {
                                final Node node = session.getNode(path);
                                NodeState state = new NodeState(node, true);
                                stateCache.put(path, state);
                            }
                        } catch (RepositoryException ex) {
                            log.warn("Failed to initialize node state", ex);
                        }
                    }
                }
            }
        }
    }

    private List<String> getParents() {
        return this.parents;
    }
    
    String getPath() {
        return this.path;
    }
    
    UserSession getSession() {
        return sessionRef.get();
    }

    void dispose() {
        if (session != null) {
            if (log.isDebugEnabled()) {
                log.debug("disposing listener {}", this);
            }
            try {
                unsubscribe();
            } catch (RepositoryException ex) {
                log.error("Unable to unregister event listener, {}", ex.getMessage());
            }
            session = null;
        }
        jcrEvents.clear();
        dispatchedEvents.clear();
        virtualEvents.clear();
    }

    private void subscribe() throws RepositoryException {
        if (!isvirtual) {
            ObservationManager obMgr = session.getWorkspace().getObservationManager();
            String[] uuid = null;
            if (uuids != null) {
                uuid = uuids.toArray(new String[uuids.size()]);
            }
            obMgr.addEventListener(this, eventTypes, path, isDeep, uuid, nodeTypes, noLocal);
        }

        // do not subscribe to facet root events when listening to deep tree structures;
        // this is too expensive, as it will populate e.g. facet navigation nodes.
        if (isDeep && uuids == null) {
            return;
        }

        // subscribe to facet search observer.
        // FIXME due to HREPTWO-2655, will not be able to receive events on newly
        // created facet search nodes.
        fro = (FacetRootsObserver) getSession().getFacetRootsObserver();

        // subscribe when target has a facetsearch as an ancestor
        try {
            final Node root = getRoot();
            for (Node node = root; node.getDepth() > 0; ) {
                if (JcrHelper.isVirtualRoot(node)) {
                    fro.subscribe(this, node);
                    break;
                }
                node = node.getParent();
            }

            for (Node node : getReferencedNodes()) {
                if (JcrHelper.isVirtualRoot(node)) {
                    fro.subscribe(this, node);
                }
            }

            listenerPath = root.getPath();
            cmsEventDispatcherService.subscribe(listenerPath, this);
        } catch (PathNotFoundException pnfe) {
            log.warn("Path no longer exists, stopping observation; {}", pnfe.getMessage());
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

    }

    private void unsubscribe() throws RepositoryException {
        if (fro != null) {
            fro.unsubscribe(this, session);
            fro = null;
        }

        cmsEventDispatcherService.unsubscribe(listenerPath, this);

        if (!isvirtual && session.isLive()) {
            ObservationManager obMgr = session.getWorkspace().getObservationManager();
            obMgr.removeEventListener(this);
        }
        session = null;
    }

    private Node getRoot() throws PathNotFoundException, RepositoryException {
        final Session jcrSession = getSession().getJcrSession();
        if (uuids != null && uuids.size() == 1) {
            try {
                return jcrSession.getNodeByIdentifier(uuids.get(0));
            } catch (ItemNotFoundException e) {
                //The node doesn't exist with the uuid, try to get the node by absolute path.
                if (parents != null && parents.size() == 1) {
                    return jcrSession.getNode(parents.get(0));
                }
            }
        }
        return jcrSession.getNode(path);
    }

    /**
     * @return nodes with absolute paths and if the node finds update the uuid list.
     */
    private List<Node> checkReferencedNodesWithAbsolutePath() {
        final List<Node> nodes = new ArrayList<Node>();
        if (parents != null) {
            final List<String> ids = new ArrayList<>();
            for (final String absPath : parents) {
                try {
                    final Node node = session.getNode(absPath);
                    ids.add(node.getIdentifier());
                    nodes.add(node);
                } catch (RepositoryException e) {
                    log.info("Could not find reference node by absolute path {} : {}", absPath, e.getMessage());
                }
            }
            if (!ids.isEmpty()) {
                uuids = ids;
            }
        }
        return nodes;
    }

    private List<Node> getReferencedNodes() {
        if (uuids != null) {
            Session jcrSession = getSession().getJcrSession();
            List<Node> list = new ArrayList<Node>(uuids.size());
            List<String> validUuids = new ArrayList<String>(uuids.size());
            for (String id : uuids) {
                try {
                    list.add(jcrSession.getNodeByIdentifier(id));
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

    private boolean isVisible(Node node) {
        try {
            String nodePath = node.getPath();
            if ((isDeep && nodePath.startsWith(this.path)) || nodePath.equals(this.path)) {
                if (uuids != null) {
                    if (uuids.contains(node.getIdentifier())) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            log.warn("Unable to determine if node is visible, defaulting to false: {}", e.getMessage());
        }
        return false;
    }

    private NodeState getNodeState(String path) {
        return stateCache.get(path);
    }
    
    private void addVisibleNodes(NodeIterator pending, Set<Node> nodes) {
        while (pending.hasNext()) {
            Node node = pending.nextNode();
            if (isVisible(node)) {
                nodes.add(node);
            }
        }
    }

    private void expandNew(final Set<Node> nodes) {
        for (Node node : new ArrayList<Node>(nodes)) {
            if (node.isNew()) {
                ItemVisitor visitor = new TraversingItemVisitor() {
                    @Override
                    public void visit(Node node) throws RepositoryException {
                        // do not traverse into virtual paths
                        if(!JcrHelper.isVirtualNode(node)) {
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
            eventPath = getEventParentPath(eventPath);
        }
        if (!session.itemExists(eventPath)) {
            return true;
        }
        Node parent = (Node) session.getItem(eventPath);

        // check UUIDs
        if (uuids != null) {
            String parentId = parent.getIdentifier();
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

            boolean match = false;
            for (String nodeType : nodeTypes) {
                if (parent.isNodeType(nodeType)) {
                    match = true;
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
    
    private String getEventParentPath(String eventPath) {
        eventPath = eventPath.substring(0, eventPath.lastIndexOf('/'));
        if (eventPath.equals("")) {
            eventPath = "/";
        }
        return eventPath;
    }

    private void checkSession() throws ObservationException {
        // listeners can be invoked after they have been removed
        if (session == null) {
            throw new ObservationException("Listener " + this + " is no longer registerd");
        } else if (!session.isLive()) {
            log.info("resubscribing listener {}", this);

            // events have references to the session, so they are useless now
            jcrEvents.clear();
            dispatchedEvents.clear();
            virtualEvents.clear();
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
                        log.debug("processing {}, session {}", event, sessionRef.get().getId());
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

                @Override
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

        addParentsToCache(dirty);
    }

    private List<Event> getEvents(Map<String, NodeState> dirty) {
        List<Event> events = new ArrayList<>(virtualEvents);
        virtualEvents.clear();

        List<Event> copyJcrEvents = new LinkedList<>(this.jcrEvents);
        this.jcrEvents.clear();

        if (isvirtual) {
            return events;
        }

        Set<Node> externallyModified = getExternallyModifiedNodes(copyJcrEvents);
        createEventsForExternallyModifiedNodes(dirty, events, externallyModified);

        List<ChangeEvent> copyDispatchedEvents = new ArrayList<>(dispatchedEvents);
        dispatchedEvents.clear();
        events.addAll(copyDispatchedEvents);

        // process pending changes
        Set<Node> locallyModified;
        try {
            locallyModified = getLocallyModifiedNodes();
        } catch (PathNotFoundException | ItemNotFoundException e) {
            log.info("Root node no longer exists: {}", e.getMessage());
            dispose();
            return events;
        } catch (RepositoryException e) {
            log.error("Failed to parse pending changes", e);
            dispose();
            return events;
        }

        expandNew(locallyModified);

        createEventsForLocallyModifiedNodes(dirty, events, locallyModified);
        return events;
    }

    private Set<Node> getExternallyModifiedNodes(final List<Event> events) {
        final Set<Node> nodes = new TreeSet<Node>(new NodePathComparator());
        for (Event jcrEvent : events) {
            try {
                String eventPath = getEventParentPath(jcrEvent.getPath());
                Node parentNode = session.getNode(eventPath);
                nodes.add(parentNode);
            } catch (RepositoryException re) {
                log.info("Unable to retrieve event's parent node identifier: {}", re.getMessage());
            }
        }
        return nodes;
    }

    private void createEventsForExternallyModifiedNodes(final Map<String, NodeState> dirty, final List<Event> events, final Set<Node> nodes) {
        for (Node node : nodes) {
            try {
                String path = node.getPath();

                // only create new state if the old state is cached
                NodeState newState = null;
                NodeState oldState = getNodeState(path);
                if (oldState != null) {
                    if (dirty.containsKey(path)) {
                        newState = dirty.get(path);
                    } else {
                        newState = new NodeState(node, false);
                        dirty.put(path, newState);
                    }
                }

                addNodeStateEvents(events, path, newState);
            } catch (RepositoryException e) {
                log.warn("Ignoring node because it's no longer valid");
            }
        }
    }

    private Set<Node> getLocallyModifiedNodes() throws RepositoryException {
        final Set<Node> locallyModifiedNodes = new TreeSet<Node>(new NodePathComparator());
        Node root = getRoot();
        if (nodeTypes == null) {
            if ((root.isModified() || root.isNew()) && isVisible(root)) {
                locallyModifiedNodes.add(root);
            }
            // use pendingChanges to detect changes in sub-trees and properties
            if (!root.isNew()) {
                NodeIterator pending = ((HippoSession) root.getSession()).pendingChanges(root, null, false);
                addVisibleNodes(pending, locallyModifiedNodes);
            }
        } else {
            if ((root.isModified() || root.isNew()) && isVisible(root)) {
                for (String type : nodeTypes) {
                    try {
                        if (root.isNodeType(type)) {
                            locallyModifiedNodes.add(root);
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
                    NodeIterator pending = ((HippoSession) root.getSession()).pendingChanges(root, type, false);
                    addVisibleNodes(pending, locallyModifiedNodes);
                }
            }
        }
        List<Node> nodes = getReferencedNodes();
        if (nodes.isEmpty()) {
            //The nodes not found, try to get the referenced nodes by absolute path.
            nodes = checkReferencedNodesWithAbsolutePath();
        }
        for (Node node : nodes) {
            if (node.isModified() || node.isNew()) {
                if (nodeTypes == null) {
                    locallyModifiedNodes.add(node);
                } else {
                    try {
                        for (String type : nodeTypes) {
                            if (node.isNodeType(type)) {
                                locallyModifiedNodes.add(node);
                                break;
                            }
                        }
                    } catch (RepositoryException e) {
                        log.debug("Unable to determine if modified node is of a filtered node type", e);
                    }
                }
            }
        }

        return locallyModifiedNodes;
    }

    private void createEventsForLocallyModifiedNodes(final Map<String, NodeState> dirty, final List<Event> events, final Set<Node> locallyModifiedNodes) {
        for (Node node : locallyModifiedNodes) {
            try {
                String path = node.getPath();

                NodeState newState;
                if (dirty.containsKey(path)) {
                    newState = dirty.get(path);
                } else {
                    newState = new NodeState(node, false);
                    dirty.put(path, newState);
                }

                addNodeStateEvents(events, path, newState);
            } catch (RepositoryException e) {
                log.warn("Failed to process node", e);
            }
        }
    }

    private void addNodeStateEvents(final List<Event> events, final String path, final NodeState newState) throws RepositoryException {
        NodeState oldState = getNodeState(path);
        if (oldState != null) {
            Iterator<Event> iter = oldState.getEvents(newState);
            while (iter.hasNext()) {
                Event event = iter.next();
                if (!blocks(event)) {
                    events.add(event);
                }
            }
        } else {
            Event changeEvent = new ChangeEvent(path, session.getUserID());
            if (!blocks(changeEvent)) {
                events.add(changeEvent);
            }
        }
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