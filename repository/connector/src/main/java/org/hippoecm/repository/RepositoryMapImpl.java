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
package org.hippoecm.repository;

import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.RepositoryMap;
import org.onehippo.repository.ManagerServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryMapImpl extends AbstractMap implements RepositoryMap {

    protected final Logger log = LoggerFactory.getLogger(HippoRepository.class);

    protected Session session = null;
    private String path;
    private Item item = null;
    private int index = -1;
    private List<String> arguments = null;
    private QueryManager queryManager;

    public RepositoryMapImpl() {
        this.item = null;
        this.path = null;
        this.session = null;
    }

    public RepositoryMapImpl(Session session, String path) throws RepositoryException {
        this(session.getRootNode().getNode(path.startsWith("/") ? path.substring(1) : path));
    }

    public RepositoryMapImpl(Item context) throws RepositoryException {
        this.item = context;
        this.path = context.getPath();
        this.session = context.getSession();
        this.queryManager = session.getWorkspace().getQueryManager();
    }

    private RepositoryMapImpl(Item context, int index) throws RepositoryException {
        this.item = context;
        this.path = context.getPath();
        this.index = index;
        this.session = context.getSession();
        this.queryManager = session.getWorkspace().getQueryManager();
    }

    protected RepositoryMapImpl(String path) {
        this.item = null;
        this.path = path;
    }

    protected RepositoryMapImpl(RepositoryMapImpl parent) {
        this.item = parent.item;
        this.path = parent.path;
        this.session = parent.session;
        this.queryManager = parent.queryManager;
        this.index = parent.index;
        this.session = parent.session;
    }

    private RepositoryMapImpl(RepositoryMapImpl parent, Node context) throws RepositoryException {
        this.item = context;
        this.path = context.getPath();
        this.session = parent.session;
        this.queryManager = parent.queryManager;
        this.index = parent.index;
        this.session = parent.session;
    }

    public RepositoryMapImpl(Item context, String relPath) throws RepositoryException {
        if (context.isNode() && ((Node)context).hasNode(relPath)) {
            item = ((Node)context).getNode(relPath);
            path = item.getPath();
        } else if (context.isNode() && ((Node)context).hasProperty(relPath)) {
            item = ((Node)context).getProperty(relPath);
            path = item.getPath();
        } else {
            item = null;
            if (relPath.startsWith("/")) {
                path = relPath;
            } else {
                path = context.getPath() + "/" + relPath;
            }
        }
    }

    public RepositoryMapImpl(RepositoryMapImpl parent, String relPath) throws RepositoryException {
        this(parent, relPath, -1);
    }

    private RepositoryMapImpl(RepositoryMapImpl parent, String relPath, int index) throws RepositoryException {
        this(parent.item, relPath);
        this.index = index;
    }

    private RepositoryMapImpl(RepositoryMapImpl parent, List args) throws RepositoryException {
        this(parent.item);
        if (parent.arguments != null) {
            this.arguments = new LinkedList<String>(parent.arguments);
        } else {
            this.arguments = new LinkedList<String>();
        }
        this.arguments.addAll(args);
    }

    public boolean exists() {
        try {
            if (item == null) {
                if (session != null) {
                    if (path.startsWith("/")) {
                        HierarchyResolver resolver;
                        if(session.getWorkspace() instanceof HippoWorkspace)
                            resolver = ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver();
                        else
                            resolver = ManagerServiceFactory.getManagerService(session).getHierarchyResolver();
                        return resolver.getItem(session.getRootNode(), path.substring(1)) != null;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else if (!path.startsWith("/")) {
                return ((Node)item).hasNode(path) && ((Node)item).hasProperty(path);
            } else {
                return true;
            }
        } catch (RepositoryException ex) {
            return false;
        }
    }

    @Override
    public Collection values() {
        Set rtvalue = new LinkedHashSet() {
            public void remove() throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }
        };
        try {
            NodeIterator nodeIter = null;
            if (item != null && ((Node)item).isNodeType("nt:query")) {
                HippoQuery query = (HippoQuery)queryManager.getQuery((Node)item);
                String[] args = query.getArguments();
                for (int i = 0; i < args.length; i++) {
                    query.bindValue(args[i], item.getSession().getValueFactory().createValue(arguments.get(i)));
                }
                QueryResult result = query.execute();
                nodeIter = result.getNodes();
            } else if (item != null && item.isNode()) {
                Node node = (Node)item;
                nodeIter = node.getNodes();
            }
            if (nodeIter != null) {
                int i = 0;
                while (nodeIter.hasNext()) {
                    Node child = nodeIter.nextNode();
                    if (child != null) {
                        rtvalue.add(new RepositoryMapImpl(child, i++));
                    }
                }
            }
        } catch (RepositoryException ex) {
        }
        return rtvalue;
    }

    public Set entrySet() {
        Set rtvalue = new LinkedHashSet() {
            public void remove() throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }
        };
        try {
            NodeIterator nodeIter = null;
            if (item != null && ((Node)item).isNodeType("nt:query")) {
                HippoQuery query = (HippoQuery)queryManager.getQuery((Node)item);
                String[] args = query.getArguments();
                for (int i = 0; i < args.length; i++) {
                    query.bindValue(args[i], item.getSession().getValueFactory().createValue(arguments.get(i)));
                }
                QueryResult result = query.execute();
                nodeIter = result.getNodes();
            } else if (item != null && item.isNode()) {
                Node node = (Node)item;
                nodeIter = node.getNodes();
            }
            if (nodeIter != null) {
                while (nodeIter.hasNext()) {
                    final Node child = nodeIter.nextNode();
                    if (child != null) {
                        rtvalue.add(new Map.Entry() {
                            public Object getValue() {
                                return child;
                            }

                            public Object getKey() {
                                try {
                                    return child.getName();
                                } catch (RepositoryException ex) {
                                    return null;
                                }
                            }

                            public Object setValue(Object newValue) {
                                throw new UnsupportedOperationException();
                            }
                        });
                    }
                }
            }
        } catch (RepositoryException ex) {
        }
        return rtvalue;
    }

    @Override
    public Object get(Object key) {
        try {
            if (item != null && ((Node)item).isNodeType("nt:query")) {
                HippoQuery query = (HippoQuery)queryManager.getQuery((Node)item);
                String[] args = query.getArguments();
                if (args.length <= 0 || (arguments != null && arguments.size() == args.length)) {
                    if (log.isDebugEnabled()) {
                        log.debug("get from query item " + item.getPath() + " with key " + key + " query executed since " +
                                  args.length + " parameters supplied");
                    }
                    for (int i = 0; i < args.length; i++) {
                        query.bindValue(args[i], item.getSession().getValueFactory().createValue(arguments.get(i)));
                    }
                    QueryResult result = query.execute();
                    String[] keys = ((String)key).split("/");
                    if ("_node".equals(keys[0])) {
                        NodeIterator nodeIter = result.getNodes();
                        if (nodeIter.hasNext()) {
                            Node node = nodeIter.nextNode();
                            Map rtvalue = new RepositoryMapImpl(node);
                            for (int i = 1; i < keys.length; i++) {
                                rtvalue = (Map)rtvalue.get(keys[i]);
                            }
                        }
                    } else {
                        for (NodeIterator nodeIter = result.getNodes(); nodeIter.hasNext();) {
                            Node node = nodeIter.nextNode();
                            if (node.getName().equals(keys[0])) {
                                Map rtvalue = new RepositoryMapImpl(node);
                                for (int i = 1; i < keys.length; i++) {
                                    rtvalue = (Map)rtvalue.get(keys[i]);
                                }
                                return rtvalue;
                            }
                        }
                    }
                    return null;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("get from query item " + item.getPath() + " with key " + key + " query not executed since " +
                                  args.length + " required");
                    }
                    LinkedList newArguments = new LinkedList<String>();
                    String[] keys = ((String)key).split("/");
                    for (int i = 0; i < keys.length; i++) {
                        newArguments.add(keys[i]);
                    }
                    return new RepositoryMapImpl(this, newArguments);
                }
            } else if (item != null && item.isNode()) {
                if (log.isDebugEnabled()) {
                    log.debug("get from node item " + item.getPath() + " with key " + key);
                }
                Node node = (Node)item;
                HierarchyResolver.Entry last = new HierarchyResolver.Entry();
                Item found = null;
                String relPath = (String)key;
                while (found == null) {
                    HierarchyResolver resolver;
                    if(session.getWorkspace() instanceof HippoWorkspace)
                        resolver = ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver();
                    else
                        resolver = ManagerServiceFactory.getManagerService(session).getHierarchyResolver();
                    found = resolver.getItem(node, relPath, false, last);
                    if (found == null) {
                        node = last.node;
                        relPath = last.relPath;
                        String name;
                        if (relPath.contains("/")) {
                            name = relPath.substring(0, relPath.indexOf("/"));
                            relPath = relPath.substring(relPath.indexOf("/") + 1);
                        } else {
                            name = relPath;
                            relPath = null;
                        }
                        if (name.equals("_node")) {
                            // no-op
                        } else if (name.equals("_name")) {
                            if (relPath != null) {
                                return null;
                            }
                            return node.getName();
                        } else if (name.equals("_path")) {
                            if (relPath != null) {
                                return null;
                            }
                            return node.getPath();
                        } else if (name.equals("_parent")) {
                            node = node.getParent();
                        } else if (name.equals("_location")) {
                            if (relPath != null) {
                                return null;
                            }
                            return ((HippoNode)node).getCanonicalNode().getPath();
                        /*} else if(name.equals("_size")) {
                        if(relPath != null)
                        return null;
                        return nodeIterator.getSize();*/
                        } else if (name.equals("_index")) {
                            return new Integer(index);
                        } else if(relPath == null && !name.startsWith("_") && node.hasProperty(name)) {
                            found = node.getProperty(name);
                        } else {
                            return null;
                        }
                    }
                }
                if (found.isNode()) {
                    return new RepositoryMapImpl((Node)found);
                } else {
                    Property property = (Property)found;
                    if (property.isMultiple()) {
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
                        case PropertyType.PATH:
                            result = new String[values.length];
                            break;
                        case PropertyType.UNDEFINED:
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
                                object = new RepositoryMapImpl(session.getNodeByUUID(value.getString()));
                                break;
                            case PropertyType.PATH:
                                object = value.getString();
                                break;
                            case PropertyType.UNDEFINED:
                            default:
                                object = value.getString();
                                break;
                            }
                            result[i++] = object;
                        }
                        return result;
                    } else {
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
                        case PropertyType.PATH:
                            return property.getString();
                        case PropertyType.UNDEFINED:
                        default:
                            return property.getString();
                        }
                    }
                }
            } else {
                return new RepositoryMapImpl(item, path + "/" + (String)key);
            }
        } catch (RepositoryException ex) {
            return null;
        }
    }
}
