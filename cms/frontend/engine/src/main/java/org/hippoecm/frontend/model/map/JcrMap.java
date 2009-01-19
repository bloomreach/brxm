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
package org.hippoecm.frontend.model.map;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrMap extends AbstractMap<String, Object> implements IHippoMap {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrMap.class);

    private Node item;

    public JcrMap(Node node) {
        this.item = node;
    }
    
    public Node getNode() {
        return item;
    }

    public String getPrimaryType() {
        try {
            return item.getPrimaryNodeType().getName();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public String[] getMixinTypes() {
        try {
            NodeType[] types = item.getMixinNodeTypes();
            String[] result = new String[types.length];
            for (int i = 0; i < types.length; i++) {
                result[i] = types[i].getName();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        LinkedHashSet<Map.Entry<String, Object>> entries = new LinkedHashSet<Map.Entry<String, Object>>();
        for (final String key : (Set<String>) keySet()) {
            if ("jcr:primaryType".equals(key)) {
                continue;
            }
            entries.add(new Map.Entry<String, Object>() {
                public String getKey() {
                    return key;
                }

                public Object getValue() {
                    return get(key);
                }

                public Object setValue(Object value) {
                    return put(key, value);
                }
            });
        }
        return entries;
    }

    @Override
    public void clear() {
        try {
            if (item != null) {
                NodeIterator children = item.getNodes();
                while (children.hasNext()) {
                    Node child = children.nextNode();
                    if (!child.getDefinition().isProtected()) {
                        child.remove();
                    }
                }

                PropertyIterator properties = item.getProperties();
                while (properties.hasNext()) {
                    Property property = properties.nextProperty();
                    if (!property.getDefinition().isProtected()) {
                        property.remove();
                    }
                }
            } else {
                log.warn("Node model is not valid");
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public Object put(String key, Object value) {
        String strKey = (String) key;
        Object current = get(strKey);
        try {
            if (value instanceof List) {
                if (current == null) {
                    current = new JcrList(item, key);
                }
                if (current instanceof List) {
                    List<IHippoMap> list = (List<IHippoMap>) current;
                    for (IHippoMap entry : (List<IHippoMap>) value) {
                        if (!list.contains(entry)) {
                            list.add(entry);
                        }
                    }
                    Iterator<IHippoMap> iter = list.iterator();
                    while (iter.hasNext()) {
                        Object entry = iter.next();
                        if (!((List<IHippoMap>) value).contains(entry)) {
                            iter.remove();
                        }
                    }
                }
            } else {
                if (value instanceof Boolean) {
                    item.setProperty(strKey, (Boolean) value);
                } else if (value instanceof String) {
                    item.setProperty(strKey, (String) value);
                } else if (value instanceof String[]) {
                    item.setProperty(strKey, (String[]) value);
                } else if (value instanceof Double) {
                    item.setProperty(strKey, (Double) value);
                } else {
                    log.warn("Unknown type of value for key " + key);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return current;
    }

    @Override
    public Object remove(Object key) {
        String strKey = (String) key;
        try {
            Object result = get(key);
            if (item != null) {
                if (item.hasProperty(strKey)) {
                    item.getProperty(strKey).remove();
                } else if (item.getNodes(strKey).getSize() > 0) {
                    NodeIterator nodes = item.getNodes(strKey);
                    while (nodes.hasNext()) {
                        Node child = nodes.nextNode();
                        child.remove();
                    }
                }
            } else {
                log.warn("Node model is invalid");
            }
            return result;
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        String strKey = (String) key;
        if ("jcr:primaryType".equals(key)) {
            return false;
        }
        try {
            if (item != null) {
                if (item.hasProperty(strKey)) {
                    return true;
                } else if (item.getNodes(strKey).hasNext()) {
                    return true;
                }
            } else {
                log.error("Node model is invalid");
            }
            return false;
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return false;
    }

    public boolean containsValue(Object value) {
        for (Map.Entry<String, Object> entry : entrySet()) {
            if (entry.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object get(Object key) {
        String strKey = (String) key;
        try {
            if (item.hasProperty(strKey)) {
                Property property = item.getProperty(strKey);
                int type = property.getDefinition().getRequiredType();

                Object[] result = null;
                if (property.getDefinition().isMultiple()) {
                    Value[] values = property.getValues();
                    switch (type) {
                    case PropertyType.BOOLEAN:
                        result = new Boolean[values.length];
                        break;
                    case PropertyType.LONG:
                        result = new Long[values.length];
                        break;
                    case PropertyType.STRING:
                    default:
                        result = new String[values.length];
                        break;
                    }

                    int i = 0;
                    for (Value current : values) {
                        result[i++] = getValue(current);
                    }

                    return result;
                } else {
                    return getValue(property.getValue());
                }
            } else if (item.hasNode(strKey)) {
                NodeDefinition def = item.getNode(strKey).getDefinition();
                if (def.allowsSameNameSiblings()) {
                    return new JcrList(item, strKey);
                } else {
                    return new JcrMap(item.getNode(strKey));
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Set<String> keySet() {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        try {
            if (item != null) {
                PropertyIterator properties = item.getProperties();
                while (properties.hasNext()) {
                    Property property = properties.nextProperty();
                    if (!"jcr:primaryType".equals(property.getName())) {
                        result.add(property.getName());
                    }
                }

                NodeIterator nodes = item.getNodes();
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    result.add(child.getName());
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return result;
    }

    @Override
    public int size() {
        try {
            if (item != null) {
                LinkedHashSet<String> names = new LinkedHashSet<String>();
                NodeIterator nodes = item.getNodes();
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    names.add(child.getName());
                }
                return names.size() + (int) item.getProperties().getSize() - 1;
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return 0;
    }

    @Override
    public Collection<Object> values() {
        LinkedHashSet<Object> result = new LinkedHashSet<Object>();
        try {
            if (item != null) {
                PropertyIterator properties = item.getProperties();
                while (properties.hasNext()) {
                    Property property = properties.nextProperty();
                    if ("jcr:primaryType".equals(property.getName())) {
                        continue;
                    }
                    if (property.getDefinition().isMultiple()) {
                        Value[] values = property.getValues();
                        Object[] entry = new Object[values.length];
                        int i = 0;
                        for (Value value : values) {
                            entry[i++] = getValue(value);
                        }
                        result.add(entry);
                    } else {
                        result.add(getValue(property.getValue()));
                    }
                }

                HashMap<String, Object> map = new HashMap<String, Object>();
                NodeIterator nodes = item.getNodes();
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    if (child.getDefinition().allowsSameNameSiblings()) {
                        List list = (List) map.get(child.getName());
                        if (list == null) {
                            map.put(child.getName(), new JcrList(item, child.getName()));
                        } else {
                            continue;
                        }
                    } else {
                        map.put(child.getName(), new JcrMap(child));
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return result;
    }

    public void reset() {
        try {
            item.refresh(false);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void save() {
        try {
            item.getSession().save();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private Object getValue(Value value) throws RepositoryException {
        switch (value.getType()) {
        case PropertyType.BOOLEAN:
            return new Boolean(value.getBoolean());
        case PropertyType.LONG:
            return new Long(value.getLong());
        case PropertyType.STRING:
        default:
            return value.getString();
        }
    }

}
