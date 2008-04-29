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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoNode;

public class Context extends AbstractMap {

    private static final Logger logger = LoggerFactory.getLogger(Context.class);

    private final Session session;
    private final String contextPath;
    private final String urlBasePath;
    private final String baseLocation;
    private String relativeLocation;
    int index = -1;
    private HippoQuery query;
    private List<String> arguments;
    private DocumentPathReplacer pathReplacer;
    
    Context(final Session jcrSession, final String contextPath, final String urlBasePath, final String baseLocation) {
        this.session = jcrSession;
        this.contextPath = contextPath;
        this.urlBasePath = urlBasePath;
        this.baseLocation = baseLocation;
        this.relativeLocation = null;
    }

    Context(final Session jcrSession, final String contextPath, final String baseLocation) {
        // create with urlBasePath same as baseLocation
        this(jcrSession, contextPath, baseLocation, baseLocation);
    }

    Context(Context parent, String relativePath) {
        this(parent, relativePath, -1);
    }
    
    Context(Context parent, String relativeLocation, int index) {
        this(parent.session, parent.contextPath, parent.urlBasePath, parent.baseLocation); 
        
        if (relativeLocation.startsWith("/")) {
            setRelativeLocation(relativeLocation);
        } else if (parent.relativeLocation.endsWith("/")) {
            this.relativeLocation = parent.relativeLocation + relativeLocation;
        } else {
            this.relativeLocation = parent.relativeLocation + "/" + relativeLocation;
        }

        this.index = index;
    }

    Context(Context parent, HippoQuery query, List arguments) {
        this(parent.session, parent.contextPath, parent.urlBasePath, parent.baseLocation);
        this.query = query;
        this.arguments = arguments;
    }

    String getLocation() {
        
        if (relativeLocation == null) {
            return baseLocation;
        }
        
        if (relativeLocation.startsWith("/")) {
            return baseLocation + relativeLocation;
        } else {
            return baseLocation + "/" + relativeLocation;
        }   
    }

    public String getURLBasePath() {
        return urlBasePath;
    }

    public String getBaseLocation() {
        return baseLocation;
    }

    void setRelativeLocation(String relativeLocation) {
        
        if (relativeLocation.startsWith(baseLocation)) {
            this.relativeLocation = relativeLocation.substring(baseLocation.length());
        } else {
            this.relativeLocation = relativeLocation;
        }   
    }

    boolean exists() {
        try {
            Item item = JCRConnector.getItem(session, getLocation());
            return item != null;
        } catch (RepositoryException ex) {
            logger.error("exists", ex);
            return false;
        }
    }

    public Collection values() {
        Set rtvalue = new LinkedHashSet();
        synchronized (this) {
            try {
                if (query != null) {
                    QueryResult result;
                    if ((arguments != null) && arguments.size() > 0) {
                        Map<String,String> queryArguments = new TreeMap<String,String>();
                        String[] workingArguments = arguments.toArray(new String[arguments.size()]);
                        for (int i=0; i+1<workingArguments.length; i+=2)
                            queryArguments.put(workingArguments[i], workingArguments[i+1]);
                        result = query.execute(queryArguments);
                    } else {
                        result = query.execute();
                    }
                    for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        if (child != null)
                            rtvalue.add(child);
                    }
                } else {
                    Item item = JCRConnector.getItem(session, getLocation());
                    if (item == null) {
                        logger.debug("Item has disappeared " + getLocation());
                    } else if (item.isNode()) {
                        Node node = (Node) item;
                        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                            Node child = iter.nextNode();
                            if (child != null)
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
                        Map<String,String> queryArguments = new TreeMap<String,String>();
                        String[] workingArguments = arguments.toArray(new String[arguments.size()]);
                        for (int i=0; i+1<workingArguments.length; i+=2)
                            queryArguments.put(workingArguments[i], workingArguments[i+1]);
                        result = query.execute(queryArguments);
                    } else {
                        result = query.execute();
                    }
                    for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
                        Node child = iter.nextNode();
                        if (child != null) {
                            rtvalue.add(child.getPath());
                        }
                    }
                } else {
                    Item item = JCRConnector.getItem(session, getLocation());
                    if (item == null) {
                        logger.debug("Item has disappeared " + getLocation());
                    } else if (item.isNode()) {
                        Node node = (Node) item;
                        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                            Node child = iter.nextNode();
                            if (child != null) {
                                rtvalue.add(child.getPath());
                            }
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
                if (query != null && !field.startsWith("/")) {
                    List newArguments = (arguments == null) ? new LinkedList() : new LinkedList(arguments);
                    newArguments.add(field);
                    result = new Context(this, query, newArguments);
                } else {
                    String requestedPath = getLocation();
                    if (!field.startsWith("_")) {
                        if (field.startsWith("/")) {
                            requestedPath = baseLocation + field;
                        } else if (requestedPath.endsWith("/")) {
                            requestedPath += field;
                        } else {
                            requestedPath += "/" + field;
                        }
                    }
                    Item item = JCRConnector.getItem(session, requestedPath);
                    if (item == null) {
                        logger.debug("No item found at path "+requestedPath);
                        result = null;
                    } 
                    else if (item.isNode()) {
                        Node node = (Node) item;
                        if (node.isNodeType("nt:query")) {
                            HippoQuery requestedQuery = (HippoQuery) session.getWorkspace().getQueryManager().getQuery(node);
                            result = new Context(this, requestedQuery, new LinkedList());
                        } 
                        else {
                            if (field.startsWith("_")) {
                                field = field.substring(1);
                                if (field.equals("name")) {
                                    result = node.getName();
                                } else if (field.equals("path")) {
                                    result = node.getPath();
                                } else if (field.equals("urlBasePath")) {
                                    result = this.urlBasePath;
                                } else if (field.equals("parent")) {
                                    
                                    if (node.getParent() == null) {
                                        return null;
                                    }

                                    Context context = new Context(this.session, this.contextPath, this.urlBasePath, this.baseLocation);
                                    context.setRelativeLocation(node.getParent().getPath());
                                    
                                    result = context;
                                } else if (field.equals("location")) {
                                    result = ((HippoNode)node).getCanonicalNode().getPath();
                                } else if (field.equals("size")) {
                                    result = new Long(node.getNodes().getSize());
                                } else if (field.equals("index")) {
                                    result = new Integer(index);
                                } else {
                                    logger.warn("context._" + field + " not defined");
                                    result = null;
                                }
                            } 
                            else {
                                result = new Context(this, node.getPath());
                            }    
                        }
                    } 
                    else {
                        Property property = (Property) item;
                        switch(property.getType()) {
                        case PropertyType.DATE:
                            result = property.getDate().getTime();
                            break;
                        case PropertyType.STRING:
                            String value = property.getString();
                            result = getPathReplacer().replace(session, value);
                            break;
                        default:
                            result = property.getString();
                        }
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


    private DocumentPathReplacer getPathReplacer() {
        
        // lazy
        if (pathReplacer == null) {
            pathReplacer = new DocumentPathReplacer(this.contextPath, this.urlBasePath, this.baseLocation);
        }
        
        return pathReplacer;
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
