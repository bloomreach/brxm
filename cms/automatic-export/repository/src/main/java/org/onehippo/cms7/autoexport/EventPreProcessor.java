/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.hippoecm.repository.api.HippoNode;

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static org.onehippo.cms7.autoexport.AutoExportModule.log;

/**
 * EventPreProcessor filters, augments, and sorts a collection of events.
 */
public class EventPreProcessor {
    
    private final Session session;
    
    EventPreProcessor(Session session) {
        this.session = session;
    }
    
    /**
     * Removes invalid added and changed events, removes redundant node removed events, 
     * removes redundant property added events, adds missing node added events, 
     * and finally sorts the resulting collection of events
     *
     * @param events  the events to process
     * @return  the processed events
     */
    public List<ExportEvent> preProcessEvents(Collection<ExportEvent> events) {
        removeInvalidEvents(events);
        removeRedundantNodeRemovedEvents(events);
        removeRedundantPropertyAddedEvents(events);
        addMissingNodeAddedEvents(events);
        List<ExportEvent> result = new ArrayList<ExportEvent>(events);
        Collections.sort(result, new EventComparator());
        return result;
    }
    
    /*
     * Because we accumulate events during a period of time before processing them, some events
     * might be outdated. For instance when an item was added or a property changed that was later
     * removed again. Here we filter out these invalid events.
     */
    private void removeInvalidEvents(Collection<ExportEvent> events) {
        final Iterator<ExportEvent> iter = events.iterator();
        while (iter.hasNext()) {
            final Event event = iter.next();
            if (event.getType() == NODE_ADDED || event.getType() == PROPERTY_ADDED || event.getType() == PROPERTY_CHANGED) {
                try {
                    if (!session.itemExists(event.getPath())) {
                        if (log.isDebugEnabled()) {
                            log.debug("Removing invalid event " + event);
                        }
                        iter.remove();
                    }
                } catch (RepositoryException e) {
                    log.error("Unable to determine whether item still exists", e);
                }
            }
        }
    }
    
    /*
     * When a node is removed, node-removed events are not only generated
     * for the node itself but also for all of its descendant nodes. We don't
     * need those node-removed events on descendant nodes because we already
     * know they are removed from the original root node-removed event. In fact
     * they only lead to misleading warnings. Therefore we remove them here.
     */
    private void removeRedundantNodeRemovedEvents(Collection<ExportEvent> events) {
        Iterator<ExportEvent> iter = events.iterator();
        while (iter.hasNext()) {
            ExportEvent event = iter.next();
            if (event.getType() == NODE_REMOVED) {
                String eventPath = event.getPath();
                if (hasAncestorNodeRemovedEvent(eventPath, events)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Removing redundant event " + event);
                    }
                    iter.remove();
                }
            }
        }
    }
    
    private boolean hasAncestorNodeRemovedEvent(String path, Collection<ExportEvent> events) {
        for (ExportEvent event : events) {
            String otherPath = event.getPath();
            if (otherPath.equals(path)) {
                continue;
            }
            if (path.startsWith(otherPath + "/") && event.getType() == NODE_REMOVED) {
                return true;
            }
        }
        return false;
    }
    
    /*
     * When we have a property-added event for which we also have a node-added
     * event for its parent node we can ignore it.
     */
    private void removeRedundantPropertyAddedEvents(Collection<ExportEvent> events) {
        Iterator<ExportEvent> iter = events.iterator();
        while (iter.hasNext()) {
            ExportEvent event = iter.next();
            if (event.getType() == PROPERTY_ADDED) {
                String eventPath = event.getPath();
                if (hasParentNodeAddedEvent(eventPath, events)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Removing redundant event " + event);
                    }
                    iter.remove();
                }
            }
        }
    }
    
    private boolean hasParentNodeAddedEvent(String path, Collection<ExportEvent> events) {
        int offset = path.lastIndexOf('/');
        String parentPath = offset == 0 ? "/" : path.substring(0, offset);
        for (ExportEvent event : events) {
            if (event.getType() == NODE_ADDED && event.getPath().equals(parentPath)) {
                return true;
            }
        }
        return false;
    }
    
    /*
     * With JCR move actions events are only generated for the root node of the move.
     * No events are generated for nodes in the subtree. Because we need these events
     * to add initialize items for the subtree nodes we add them ourselves to the set
     * of events to be processed by visiting the subtree of nodes that were added and
     * adding add-node events for any nodes we find.
     */
    private void addMissingNodeAddedEvents(final Collection<ExportEvent> events) {
        final Collection<ExportEvent> additional = new ArrayList<ExportEvent>();
        for (ExportEvent event : events) {
            if (event.getType() == NODE_ADDED) {
                final String eventPath = event.getPath();
                try {
                    final Node node = session.getNode(eventPath);
                    node.accept(new AddNodeAddedEventsVisitor(events, additional));
                } catch (PathNotFoundException e) {
                    log.debug("Node at " + eventPath + " seems to no longer exist");
                } catch (RepositoryException e) {
                    log.error(e.getClass() + ":" + e.getMessage(), e);
                }
            }
        }
        events.addAll(additional);

    }


    private static final class AddNodeAddedEventsVisitor implements ItemVisitor {

        private final Collection<ExportEvent> events;
        private final Collection<ExportEvent> additional;

        private AddNodeAddedEventsVisitor(final Collection<ExportEvent> events, final Collection<ExportEvent> additional) {
            this.events = events;
            this.additional = additional;
        }

        @Override
        public void visit(Property property) throws RepositoryException {
        }

        @Override
        public void visit(Node node) throws RepositoryException {
            if (!isVirtual(node)) {
                try {
                    ExportEvent event = new ExportEvent(NODE_ADDED, node.getPath());
                    if (!events.contains(event)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Adding additional add-node event for " + event.getPath());
                        }
                        additional.add(event);
                    }
                }
                catch (RepositoryException e) {
                    log.error("Unable to create event during event enhancement", e);
                }
                try {
                    NodeIterator nodes = node.getNodes();
                    while (nodes.hasNext()) {
                        nodes.nextNode().accept(this);
                    }
                }
                catch (RepositoryException e) {
                    log.error("RepositoryException while traversing node hierarchy in order to enhance events", e);
                }
            }
        }

        private boolean isVirtual(Node node) throws RepositoryException {
            if (!(node instanceof HippoNode)) {
                return false;
            }
            try {
                HippoNode hippoNode = (HippoNode) node;
                Node canonical = hippoNode.getCanonicalNode();
                if (canonical == null) {
                    return true;
                }
                return !canonical.isSame(hippoNode);
            } catch (ItemNotFoundException e) {
                return true;
            }

        }

    }
}
