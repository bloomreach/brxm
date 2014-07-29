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
package org.hippoecm.frontend.model.map;

import java.util.AbstractMap;
import java.util.ArrayList;
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

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrMap extends AbstractMap<String, Object> implements IHippoMap, IDetachable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrMap.class);

    private JcrNodeModel nodeModel;

    public JcrMap(JcrNodeModel node) {
        this.nodeModel = node;
    }

    public Node getNode() {
        return nodeModel.getNode();
    }

    public String getPrimaryType() {
        try {
            return getNode().getPrimaryNodeType().getName();
        } catch (RepositoryException ex) {
            handleRepositoryException(ex);
        }
        return null;
    }

    public List<String> getMixinTypes() {
        try {
            NodeType[] types = getNode().getMixinNodeTypes();
            List<String> result = new ArrayList<String>(types.length);
            for (int i = 0; i < types.length; i++) {
                result.add(types[i].getName());
            }
            return result;
        } catch (RepositoryException ex) {
            handleRepositoryException(ex);
        }
        return new ArrayList<String>(0);
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
            if (getNode() != null) {
                NodeIterator children = getNode().getNodes();
                while (children.hasNext()) {
                    Node child = children.nextNode();
                    if (!child.getDefinition().isProtected()) {
                        child.remove();
                    }
                }

                PropertyIterator properties = getNode().getProperties();
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
            handleRepositoryException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(String key, Object value) {
        Object current = get(key);
        try {
            Node node = getNode();
            if (value instanceof List) {
                if (current == null) {
                    current = new JcrList(nodeModel, key);
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
            } else if (value instanceof IHippoMap) {
                IHippoMap map = (IHippoMap) value;
                if (current == null) {
                    Node child = getNode().addNode(key, map.getPrimaryType());
                    for (String mixin : map.getMixinTypes()) {
                        child.addMixin(mixin);
                    }
                    current = new JcrMap(new JcrNodeModel(child));
                }
                map = (IHippoMap) current;
                map.clear();
                map.putAll((Map) value);
            } else {
                if (value instanceof Boolean) {
                    node.setProperty(key, ((Boolean) value).booleanValue());
                } else if (value instanceof String) {
                    node.setProperty(key, (String) value);
                } else if (value instanceof String[]) {
                    node.setProperty(key, (String[]) value);
                } else if (value instanceof Double) {
                    node.setProperty(key, ((Double) value).doubleValue());
                } else if (value instanceof Long) {
                    node.setProperty(key, ((Long) value).longValue());
                } else if (value instanceof Integer) {
                    node.setProperty(key, ((Integer) value).intValue());
                } else if (value == null) {
                    if (node.hasProperty(key)) {
                        node.getProperty(key).remove();
                    }
                } else {
                    log.warn("Unknown type of value for key " + key);
                }
            }
        } catch (RepositoryException ex) {
            handleRepositoryException(ex);
        }
        return current;
    }

    @Override
    public Object remove(Object key) {
        String strKey = (String) key;
        try {
            Object result = get(key);
            Node node = getNode();
            if (node != null) {
                if (node.hasProperty(strKey)) {
                    node.getProperty(strKey).remove();
                } else if (node.getNodes(strKey).getSize() > 0) {
                    NodeIterator nodes = node.getNodes(strKey);
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
            handleRepositoryException(ex);
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
            Node node = getNode();
            if (node != null) {
                if (node.hasProperty(strKey)) {
                    return true;
                } else if (node.getNodes(strKey).hasNext()) {
                    return true;
                }
            } else {
                log.error("Node model is invalid");
            }
            return false;
        } catch (RepositoryException ex) {
            handleRepositoryException(ex);
        }
        return false;
    }

    @Override
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
            Node node = getNode();
            if (node == null) {
                return null;
            }
            if (node.hasProperty(strKey)) {
                Property property = node.getProperty(strKey);
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
            } else if (node.hasNode(strKey)) {
                NodeDefinition def = node.getNode(strKey).getDefinition();
                if (def.allowsSameNameSiblings()) {
                    return new JcrList(nodeModel, strKey);
                } else {
                    return new JcrMap(new JcrNodeModel(node.getNode(strKey)));
                }
            }
        } catch (RepositoryException ex) {
            handleRepositoryException(ex);
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
            Node node = getNode();
            if (node != null) {
                PropertyIterator properties = node.getProperties();
                while (properties.hasNext()) {
                    Property property = properties.nextProperty();
                    if (!"jcr:primaryType".equals(property.getName())) {
                        result.add(property.getName());
                    }
                }

                NodeIterator nodes = node.getNodes();
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    result.add(child.getName());
                }
            }
        } catch (RepositoryException ex) {
            handleRepositoryException(ex);
        }
        return result;
    }

    @Override
    public int size() {
        try {
            Node node = getNode();
            if (node != null) {
                LinkedHashSet<String> names = new LinkedHashSet<String>();
                NodeIterator nodes = node.getNodes();
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    names.add(child.getName());
                }
                return names.size() + (int) node.getProperties().getSize() - 1;
            }
        } catch (RepositoryException ex) {
            handleRepositoryException(ex);
        }
        return 0;
    }

    @Override
    public Collection<Object> values() {
        LinkedHashSet<Object> result = new LinkedHashSet<Object>();
        try {
            Node node = getNode();
            if (node != null) {
                PropertyIterator properties = node.getProperties();
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
                NodeIterator nodes = node.getNodes();
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    if (child.getDefinition().allowsSameNameSiblings()) {
                        List<?> list = (List<?>) map.get(child.getName());
                        if (list == null) {
                            map.put(child.getName(), new JcrList(nodeModel, child.getName()));
                        } else {
                            continue;
                        }
                    } else {
                        map.put(child.getName(), new JcrMap(new JcrNodeModel(child)));
                    }
                }
            }
        } catch (RepositoryException ex) {
            handleRepositoryException(ex);
        }
        return result;
    }

    public void reset() {
        try {
            getNode().refresh(false);
        } catch (RepositoryException ex) {
            handleRepositoryException(ex);
        }
    }

    public void save() {
        try {
            getNode().save();
        } catch (RepositoryException ex) {
            handleRepositoryException(ex);
        }
    }

    private Object getValue(Value value) throws RepositoryException {
        switch (value.getType()) {
        case PropertyType.BOOLEAN:
            return Boolean.valueOf(value.getBoolean());
        case PropertyType.LONG:
            return Long.valueOf(value.getLong());
        case PropertyType.DOUBLE:
            return Double.valueOf(value.getDouble());
        case PropertyType.STRING:
        default:
            return value.getString();
        }
    }

    public void detach() {
        nodeModel.detach();
    }


    private void handleRepositoryException(final RepositoryException ex) {
        try {
            if (!getNode().getSession().isLive()) {
                log.error("Found session in an invalid unallowed state: not live. Return log in screen");
                throw new RestartResponseException(WebApplication.get().getHomePage());
            }
        } catch (RepositoryException e) {
            // log the original ex above
        }

        if (log.isDebugEnabled()) {
            log.warn(ex.toString(), ex);
        } else {
            log.warn(ex.toString());
        }
    }


}
