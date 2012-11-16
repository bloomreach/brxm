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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.observation.Event;

import org.hippoecm.frontend.util.NodeStateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immutable class that contains the names of child nodes and the
 * names and values of properties.  Used to generate events
 * between two different states of a node.
 */
class NodeState {

    private final static Logger log = LoggerFactory.getLogger(NodeState.class);
    
    private final static MessageDigest BINARY_DIGEST;
    static {
        try {
            BINARY_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    class NodeEvent implements Event {

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
                if (path.equals("/")) {
                    return path + name;
                } else {
                    return path + "/" + name;
                }
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

        public String getIdentifier() throws RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Map getInfo() throws RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getUserData() throws RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public long getDate() throws RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private String path;
    private String userId;
    private Map<String, Value[]> properties;
    private Map<String, String> nodes;

    NodeState(Node node, boolean skipBinaries) throws RepositoryException {
        this.path = node.getPath();
        this.userId = node.getSession().getUserID();

        properties = new HashMap<String, Value[]>();
        nodes = new LinkedHashMap<String, String>();

        ValueFactory factory = node.getSession().getValueFactory();

        PropertyIterator propIter = node.getProperties();
        while (propIter.hasNext()) {
            Property property = propIter.nextProperty();
            // skip binaries, to prevent them being pulled from the database
            if (property.getType() != PropertyType.BINARY) {
                if (property.getDefinition().isMultiple()) {
                    properties.put(property.getName(), property.getValues());
                } else {
                    properties.put(property.getName(), new Value[] { property.getValue() });
                }
            } else if (!skipBinaries && BINARY_DIGEST != null) {
                if (property.getDefinition().isMultiple()) {
                    Value[] values = property.getValues();
                    Value[] hashes = new Value[values.length];
                    for (int i = 0; i < values.length; i++) {
                        byte[] md5 = BINARY_DIGEST.digest(values[i].getString().getBytes());
                        hashes[i] = factory.createValue(new BigInteger(1, md5).toString(16));
                    }
                    properties.put(property.getName(), hashes);
                } else {
                    byte[] md5 = BINARY_DIGEST.digest(property.getValue().getString().getBytes());
                    properties.put(property.getName(), new Value[] { factory.createValue(new BigInteger(1, md5).toString(16)) });
                }
            }
        }

        NodeIterator nodeIter = node.getNodes();
        while (nodeIter.hasNext()) {
            Node child = nodeIter.nextNode();
            if (child != null) {
                try {
                    nodes.put(child.getIdentifier(), child.getName());
                } catch (RepositoryException e) {
                    log.warn("Unable to add child node to list: " + e.getMessage());
                    log.debug("Error while adding child node to list: ", e);
                }
            }
        }
    }

    Iterator<Event> getEvents(NodeState newState) throws RepositoryException {
        List<Event> events = new LinkedList<Event>();

        Map<String, Value[]> newProperties = newState.properties;
        Map<String, String> newNodes = newState.nodes;

        for (Map.Entry<String, Value[]> entry : this.properties.entrySet()) {
            if (newProperties.containsKey(entry.getKey())) {
                Value[] oldValues = entry.getValue();
                Value[] newValues = newProperties.get(entry.getKey());
                if (newValues.length != oldValues.length) {
                    events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_CHANGED));
                } else {
                    for (int i = 0; i < newValues.length; i++) {
                        if (!newValues[i].equals(oldValues[i])) {
                            events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_CHANGED));
                            break;
                        }
                    }
                }
            } else {
                events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_REMOVED));
            }
        }
        for (Map.Entry<String, Value[]> entry : newProperties.entrySet()) {
            if (!this.properties.containsKey(entry.getKey())) {
                events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_ADDED));
            }
        }

        Collection<String> oldNodeIds = this.nodes.keySet();
        Collection<String> newNodeIds = newNodes.keySet();
        
        List<String> added = NodeStateUtil.added(oldNodeIds, newNodeIds);
        if (added != null) {
            for (String child : added) {
                events.add(new NodeEvent(newNodes.get(child), Event.NODE_ADDED));
            }
        }
        
        List<String> removed = NodeStateUtil.removed(oldNodeIds, newNodeIds);
        if (removed != null) {
            for (String child: removed) {
                events.add(new NodeEvent(nodes.get(child), Event.NODE_REMOVED));
            }
        }
            
        List<String> moved = NodeStateUtil.moved(new ArrayList<String>(oldNodeIds), new ArrayList<String>(newNodeIds), added, removed);
        if (moved != null) {
            for (String child : moved) {
                events.add(new NodeEvent(nodes.get(child), Event.NODE_MOVED));
            }
        }

        return events.iterator();
    }

}