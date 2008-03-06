/*
 * Copyright 2007-2008 Hippo.
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
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoNode;

public class Context extends AbstractMap {

    public static final Logger logger = LoggerFactory.getLogger(Context.class);

    private Session session;
    private String urlbasepath;

    private int index = -1;
    private String path;
    private HippoQuery query;
    private List<String> arguments;

    Context(Session jcrSession, String urlbasepath) {
        this.session = jcrSession;
        this.urlbasepath = urlbasepath;
        this.path = null;
    }

    Context(Context parent, String path, int index) {
    	this(parent.session, parent.urlbasepath);       
        this.index = index;
        if (path.startsWith("/")) {
            this.path = path;
        } else if (parent.path.endsWith("/")) {
            this.path = parent.path + path;
        } else {
            this.path = parent.path + "/" + path;
        }
    }

    Context(Context parent, HippoQuery query, List arguments) {
        this(parent.session, parent.urlbasepath);
        this.query = query;
        this.arguments = arguments;
    }

    String getURLBasePath() {
        return urlbasepath;
    }

    void setPath(String path) {
        this.path = path;
    }

    public Collection values() {
        Set rtvalue = new LinkedHashSet();
        synchronized (this) {
            try {
                if (query != null) {
                    QueryResult result;
                    if ((arguments != null) && arguments.size() > 0) {
                        result = query.execute(arguments.toArray(new String[arguments.size()]));
                    } else {
                        result = query.execute();
                    }
                    for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        rtvalue.add(child);
                    }
                } else {
                    Item item = JCRConnector.getItem(session, path);
                    if (item == null) {
                        logger.debug("Item has disappeared " + path);
                    } else if (item.isNode()) {
                        Node node = (Node) item;
                        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                            Node child = iter.nextNode();
                            rtvalue.add(child);
                        }
                    }
                }
            } catch (PathNotFoundException ex) {
                logger.error("values", ex);
            } catch (RepositoryException ex) {
                logger.error("values", ex);
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
                    if ((arguments != null) && arguments.size() > 0) {
                        result = query.execute(arguments.toArray(new String[arguments.size()]));
                    } else {
                        result = query.execute();
                    }
                    for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        rtvalue.add(child.getPath());
                    }
                } else {
                    Item item = JCRConnector.getItem(session, path);
                    if (item == null) {
                        logger.debug("Item has disappeared " + path);
                    } else if (item.isNode()) {
                        Node node = (Node) item;
                        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                            Node child = iter.nextNode();
                            rtvalue.add(child.getPath());
                        }
                    }
                }
            } catch (PathNotFoundException ex) {
                logger.error("entrySet", ex);
            } catch (RepositoryException ex) {
                logger.error("entrySet", ex);
            }
        }
        return new EntrySet(rtvalue);
    }

    public Object get(Object key) {
        Object result;
        String field = (String) key;
        synchronized (this) {
            try {
                if (path == null && !field.startsWith("/")) {
                    List newArguments = (arguments == null) ? new LinkedList() : new LinkedList(arguments);
                    newArguments.add(key);
                    result = new Context(this, query, newArguments);
                } else {
                    String requestedPath = this.path;
                    if (!field.startsWith("_")) {
                        if (field.startsWith("/")) {
                            requestedPath = field;
                        } else if (path.endsWith("/")) {
                            requestedPath += field;
                        } else {
                            requestedPath += "/" + field;
                        }
                    }
                    Item item = JCRConnector.getItem(session, requestedPath);
                    if (item == null) {
                        logger.debug("Path not found on "+requestedPath);
                        result = null;
                    } else if (item.isNode()) {
                        Node node = (Node) item;
                        if (node.isNodeType("jcr:statement")) {
                            HippoQuery requestedQuery = (HippoQuery) session.getWorkspace().getQueryManager().getQuery(node);
                            result = new Context(this, requestedQuery, new LinkedList());
                        } else {
                            if (field.startsWith("_")) {
                                field = field.substring(1);
                                if (field.equals("name")) {
                                    result = node.getName();
                                } else if (field.equals("path")) {
                                    result = node.getPath();
                                } else if (field.equals("location")) {
                                    result = ((HippoNode)node).getCanonicalNode().getPath();
                                } else if (field.equals("size")) {
                                    result = new Long(node.getNodes().getSize());
                                } else if (field.equals("index")) {
                                    result = new Integer(index);
                                } else {
                                    result = "Abstract element not defined _" + field;
                                }
                            } else
                                result = new Context(this, node.getPath(), -1);
                        }
                    } else {
                        Property property = (Property) item;
                        result = property.getString();
                    }
                }
            } catch (PathNotFoundException ex) {
                logger.error("get", ex);
                result = null;
            } catch (RepositoryException ex) {
                logger.error("get", ex);
                result = null;
            }
        }
        return result;
    }


    private class EntrySet extends AbstractSet {
        private Set<String> set;

        EntrySet(Set<String> set) {
            this.set = set;
        }

        public int size() {
            return set.size();
        }

        public EntryIterator iterator() {
            return new EntryIterator(set.iterator());
        }

        private class EntryIterator implements Iterator {
            private Iterator<String> keyIterator;
            private int idx;

            EntryIterator(Iterator<String> iter) {
                keyIterator = iter;
                idx = 0;
            }

            public boolean hasNext() {
                return keyIterator.hasNext();
            }

            public Object next() {
                return new Context(Context.this, keyIterator.next(), idx++);
            }

            public void remove() throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }
        }
    }
}
