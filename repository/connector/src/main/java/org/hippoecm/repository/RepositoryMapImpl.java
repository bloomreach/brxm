/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryMapImpl extends AbstractMap implements RepositoryMap {

    private final Logger log = LoggerFactory.getLogger(HippoRepository.class);

    private Session session = null;
    private Node node = null;

    public RepositoryMapImpl() {
        this.node = null;
        this.session = null;
    }

    public RepositoryMapImpl(Node node) {
        this.node = node;
    }

    public boolean exists() {
        try {
            if (node != null) {
                node.getPath();
                return true;
            }
        } catch (RepositoryException ignore) {
        }
        return false;
    }

    @Override
    public Collection<Object> values() {
        final List<Object> result = new ArrayList<>();
        try {
            if (node != null) {
                final Set<String> keys = new HashSet<>();
                for (Property property : new PropertyIterable(node.getProperties())) {
                    final String key = property.getName();
                    if (keys.contains(key)) {
                        log.warn("Duplicate key in map of node {}: {}", node.getPath(), key);
                        continue;
                    }
                    keys.add(key);
                    result.add(getValue(property));
                }
                for (Node child : new NodeIterable(node.getNodes())) {
                    final String key = child.getName();
                    if (keys.contains(key)) {
                        log.warn("Duplicate key in map of node {}: {}", node.getPath(), key);
                        continue;
                    }
                    keys.add(key);
                    result.add(new RepositoryMapImpl(child));
                }
            }
        } catch (RepositoryException ignore) {}
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public Set<Entry> entrySet() {
        final Set<Entry> result = new HashSet<>();
        try {
            if (node != null) {
                final Set<String> keys = new HashSet<>();
                for (final Property property : new PropertyIterable(node.getProperties())) {
                    final String key = property.getName();
                    if (keys.contains(key)) {
                        log.warn("Duplicate key in map of node {}: {}", node.getPath(), key);
                        continue;
                    }
                    keys.add(key);
                    result.add(new Entry() {
                        @Override
                        public Object getKey() {
                            return key;
                        }

                        @Override
                        public Object getValue() {
                            return RepositoryMapImpl.this.getValue(property);
                        }

                        @Override
                        public Object setValue(final Object value) {
                            throw new UnsupportedOperationException();
                        }
                    });
                }
                for (final Node child : new NodeIterable(node.getNodes())) {
                    final String key = child.getName();
                    if (keys.contains(key)) {
                        log.warn("Duplicate key in map of node {}: {}", node.getPath(), key);
                        continue;
                    }
                    keys.add(key);
                    result.add(new Entry() {
                        @Override
                        public Object getValue() {
                            return new RepositoryMapImpl(child);
                        }

                        @Override
                        public Object getKey() {
                            return key;
                        }

                        @Override
                        public Object setValue(Object newValue) {
                            throw new UnsupportedOperationException();
                        }
                    });
                }
            }
        } catch (RepositoryException ignore) {
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Object get(Object o) {
        final String key = o.toString();
        if (key.indexOf('/') != -1) {
            throw new IllegalArgumentException("Only direct children and properties of a node can be retrieved");
        }
        try {
            if (node != null) {
                Item found = null;
                if (node.hasProperty(key)) {
                    found = node.getProperty(key);
                } else if (node.hasNode(key)) {
                    found = node.getNode(key);
                }
                if (found == null) {
                    return null;
                }
                if (found instanceof Node) {
                    return new RepositoryMapImpl((Node) found);
                } else {
                    return getValue((Property) found);
                }
            }
        } catch (RepositoryException ignore) {
        }
        return null;
    }

    private Object getValue(final Property property) {
        try {
            if (property.isMultiple()) {
                return getMultipleValues(property);
            } else {
                return getSingleValue(property);
            }
        } catch (RepositoryException ignore) {}
        return null;
    }

    private Object getSingleValue(final Property property) {
        try {
            switch (property.getType()) {
                case PropertyType.STRING:
                    return property.getString();
                case PropertyType.LONG:
                    return property.getLong();
                case PropertyType.DATE:
                    return property.getDate();
                case PropertyType.BOOLEAN:
                    return property.getBoolean();
                case PropertyType.REFERENCE:
                    return new RepositoryMapImpl(property.getNode());
                default:
                    return property.getString();
            }
        } catch (RepositoryException ignore) {
        }
        return null;
    }

    private Object[] getMultipleValues(final Property property) {
        try {
            Value[] values = property.getValues();
            Object[] result;
            int type = property.getType();
            switch (type) {
                case PropertyType.STRING:
                    result = new String[values.length];
                    break;
                case PropertyType.LONG:
                    result = new Long[values.length];
                    break;
                case PropertyType.DATE:
                    result = new Calendar[values.length];
                    break;
                case PropertyType.BOOLEAN:
                    result = new Boolean[values.length];
                    break;
                case PropertyType.REFERENCE:
                    result = new RepositoryMap[values.length];
                    break;
                default:
                    result = new String[values.length];
                    break;
            }
            int i = 0;
            for (Value value : values) {
                Object object;
                switch (type) {
                    case PropertyType.STRING:
                        object = value.getString();
                        break;
                    case PropertyType.LONG:
                        object = value.getLong();
                        break;
                    case PropertyType.DATE:
                        object = value.getDate();
                        break;
                    case PropertyType.BOOLEAN:
                        object = value.getBoolean();
                        break;
                    case PropertyType.REFERENCE:
                        object = new RepositoryMapImpl(session.getNodeByIdentifier(value.getString()));
                        break;
                    default:
                        object = value.getString();
                        break;
                }
                result[i++] = object;
            }
            return result;
        } catch (RepositoryException ignore) {
        }
        return null;
    }
}
