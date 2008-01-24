/*
 * Copyright 2007 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoQuery;

public class Context extends AbstractMap {
    static HippoRepository repository;

    Session session;

    private String urlbasepath;
    private Context parent;
    private String path;
    private HippoQuery query;
    private List<String> arguments;
    private int index;

    public Context() {
        parent = null;
    }

    Context(Context parent, String path, int index) {
        this.parent = parent;
        this.index = index;
        this.urlbasepath = parent.urlbasepath;
        session = parent.session;
        repository = parent.repository;
        if (path.startsWith("/"))
            this.path = path;
        else if (parent.path.endsWith("/"))
            this.path = parent.path + path;
        else
            this.path = parent.path + "/" + path;
        this.query = null;
        this.arguments = null;
    }

    Context(Context parent, HippoQuery query) {
        this.parent = parent;
        this.index = -1;
        this.urlbasepath = parent.urlbasepath;
        session = parent.session;
        repository = parent.repository;
        this.path = null;
        this.query = query;
        this.arguments = new LinkedList();
    }

    Context(Context parent, HippoQuery query, List<String> arguments) {
        this.parent = parent;
        this.index = -1;
        this.urlbasepath = parent.urlbasepath;
        session = parent.session;
        repository = parent.repository;
        this.path = null;
        this.query = query;
        this.arguments = arguments;
    }

    protected void finalize() {
        if (parent == null) {
            synchronized (this) {
                if (session != null) {
                    session.logout();
                    session = null;
                }
            }
        }
    }

    void setURLPageBase(String urlbasepath) {
        this.urlbasepath = urlbasepath;
    }

    String getURLPageBase() {
        return urlbasepath;
    }

    void setRepository(String location) {
        try {
            if (repository == null) {
                //HippoRepositoryFactory.setDefaultRepository(location);
                repository = HippoRepositoryFactory.getHippoRepository();
                HippoRepositoryFactory.setDefaultRepository(repository);
            }
            if (session == null) {
                // session = repository.login("admin", "admin".toCharArray());
                session = repository.login();
                path = "/";
                // org.hippoecm.repository.Utilities.dump(System.err, session.getRootNode());
            }
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    void setPath(String path) {
        if (path.startsWith("/"))
            this.path = path;
        else if (this.path.endsWith("/"))
            this.path = this.path + path;
        else
            this.path = this.path + "/" + path;
    }

    public String getPath() {
        try {
            Item item = getItem(session, path);
            return item.getPath();
        } catch (PathNotFoundException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            return "PathNotFoundException " + path;
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
            return "RepositoryException " + ex.getMessage();
        }
    }

    public String getName() {
        try {
            Item item = getItem(session, path);
            return item.getName();
        } catch (PathNotFoundException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            return "PathNotFoundException " + path;
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
            return "RepositoryException " + ex.getMessage();
        }
    }

    /* AbstractMap implementation */

    public Collection values() {
        Set rtvalue = new LinkedHashSet();
        synchronized (this) {
            try {
                if (query != null) {
                    QueryResult result;
                    if (arguments.size() > 0)
                        result = query.execute(arguments.toArray(new String[arguments.size()]));
                    else
                        result = query.execute();
                    for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        rtvalue.add(child);
                    }
                } else {
                    Item item = getItem(session, path);
                    if (item.isNode()) {
                        Node node = (Node) item;
                        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                            Node child = iter.nextNode();
                            rtvalue.add(child);
                        }
                    }
                }
            } catch (PathNotFoundException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            } catch (RepositoryException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        return rtvalue;
    }

    public Set entrySet() {
        Set rtvalue = new LinkedHashSet();
        synchronized (this) {
            try {
                if (query != null) {
                    QueryResult result;
                    if (arguments.size() > 0)
                        result = query.execute(arguments.toArray(new String[arguments.size()]));
                    else
                        result = query.execute();
                    for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        rtvalue.add(child.getPath());
                    }
                } else {
                    Item item = getItem(session, path);
                    if (item.isNode()) {
                        Node node = (Node) item;
                        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                            Node child = iter.nextNode();
                            rtvalue.add(child.getPath());
                        }
                    }
                }
            } catch (PathNotFoundException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            } catch (RepositoryException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
        return this.new EntrySet(rtvalue);
    }

    public Object get(Object key) {
        String field = (String) key;
        synchronized (this) {
            try {
                if (path == null && !field.startsWith("/")) {
                    List newArguments = new LinkedList(arguments);
                    newArguments.add(key);
                    return new Context(this, query, arguments);
                } else {
                    String path = this.path;
                    if (!field.startsWith("_")) {
                        if (field.startsWith("/"))
                            path = field;
                        else if (path.endsWith("/"))
                            path += field;
                        else
                            path += "/" + field;
                    }
                    Item item = getItem(session, path);
                    if (item == null) {
                        return "PathNotFound " + path;
                    }
                    if (item.isNode()) {
                        Node node = (Node) item;
                        if (node.isNodeType("jcr:query")) {
                            HippoQuery query = (HippoQuery) session.getWorkspace().getQueryManager().getQuery(node);
                            return new Context(this, query);
                        } else {
                            if (field.startsWith("_")) {
                                field = field.substring(1);
                                if (field.equals("name"))
                                    return node.getName();
                                else if (field.equals("path"))
                                    return node.getPath();
                                else if (field.equals("size"))
                                    return node.getNodes().getSize();
                                else if (field.equals("index"))
                                    return index;
                                else
                                    return "Abstract element not defined _" + field;
                            } else
                                return new Context(this, node.getPath(), -1);
                        }
                    } else {
                        Property property = (Property) item;
                        // System.err.println("  getProperty -> "+property.getString());
                        return property.getString();
                    }
                }
            } catch (PathNotFoundException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                return null;
            } catch (RepositoryException ex) {
                System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
                return null;
            }
        }
    }

    static Item getItem(Session session, String path) throws RepositoryException {
        System.err.println("getItem(\""+path+"\")");
        Node node = session.getRootNode();
        if(path.startsWith("/"))
            path = path.substring(1);
        String[] pathElts = path.split("/");
        for(int pathIdx=0; pathIdx<pathElts.length && node != null; pathIdx++) {
            String relPath = pathElts[pathIdx];
            Map<String,String> conditions = null;
            if(relPath.contains("[") && relPath.endsWith("]")) {
                conditions = new TreeMap<String,String>();
                String[] conditionElts = relPath.substring(relPath.indexOf("[")+1,relPath.lastIndexOf("]")).split(",");
                for(int conditionIdx=0; conditionIdx<conditionElts.length; conditionIdx++) {
                    int pos = conditionElts[conditionIdx].indexOf("=");
                    if(pos >= 0) {
                        String key = conditionElts[conditionIdx].substring(0,pos);
                        String value = conditionElts[conditionIdx].substring(pos+1);
                        if(value.startsWith("'") && value.endsWith("'"))
                            value = value.substring(1,value.length()-1);
                        conditions.put(key, value);
                    } else
                        conditions.put(conditionElts[conditionIdx], null);
                }
                relPath = relPath.substring(0,relPath.indexOf("["));
            }
            if(conditions == null || conditions.size() == 0) {
                if(pathIdx+1==pathElts.length && node.hasProperty(relPath)) {
                    try {
                        return node.getProperty(relPath);
                    } catch(PathNotFoundException ex) {
                        return null;
                    }
                } else if(node.hasNode(relPath)) {
                    try {
                        node = node.getNode(relPath);
                    } catch(PathNotFoundException ex) {
                        return null;
                    }
                } else
                    return null;
            } else {
                for(NodeIterator iter = node.getNodes(relPath); iter.hasNext(); ) {
                    node = iter.nextNode();
                    for(Map.Entry<String,String> condition: conditions.entrySet()) {
                        if(node.hasProperty(condition.getKey())) {
                            if(condition.getValue() != null) {
                                try {
                                    if(!node.getProperty(condition.getKey()).getString().equals(condition.getValue())) {
                                        node = null;
                                        break;
                                    }
                                } catch(PathNotFoundException ex) {
                                    node = null;
                                    break;
                                } catch(ValueFormatException ex) {
                                    node = null;
                                    break;
                                }
                            }
                        } else {
                           node = null;
                            break;
                        }
                    }
                    if(node != null)
                        break;
                }
            }
        }
        return node;
    }


    class EntrySet extends AbstractSet {
        Set<String> set;

        public EntrySet(Set<String> set) {
            this.set = set;
        }

        public int size() {
            return set.size();
        }

        public EntryIterator iterator() {
            return new EntryIterator(set.iterator());
        }

        class EntryIterator implements Iterator {
            Iterator<String> keyIterator;
            int index;

            EntryIterator(Iterator iter) {
                keyIterator = iter;
                index = 0;
            }

            public boolean hasNext() {
                return keyIterator.hasNext();
            }

            public Object next() {
                String path = keyIterator.next();
                return new Context(Context.this, path, index++);
            }

            public void remove() throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }
        }
    }
}
