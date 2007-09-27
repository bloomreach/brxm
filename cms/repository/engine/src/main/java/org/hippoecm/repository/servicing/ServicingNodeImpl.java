/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.servicing;

import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.Path;

import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.HitsRequested;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class ServicingNodeImpl extends ItemDecorator implements HippoNode {
    final static private String SVN_ID = "$Id$";

    // FIXME
    private final static char FACET_SEPARATOR = '#';

    protected Node node;
    protected Session session;
    protected boolean isVirtual;
    protected String primaryNodeTypeName;
    protected String name;
    protected String path;
    protected int depth;
    protected Map<String,Node[]> children = null;
    protected Map<String,Property> properties = null;
    protected NodeView selection;

    private static Pattern facetPropertyPattern;
    static {
        facetPropertyPattern = Pattern.compile("^@([^=]+)='(.+)'$");
    }

    protected ServicingNodeImpl(DecoratorFactory factory, Session session, Node node, NodeView selection) {
        super(factory, session, node);
        this.session = session;
        this.selection = selection;
        isVirtual = false;
        this.node = node;
        try {
            this.path = node.getPath();
        } catch (RepositoryException ex) {
            this.path = null;
        }
        try {
            this.depth = node.getDepth();
        } catch (RepositoryException ex) {
            this.depth = -1;
        }
    }

    protected ServicingNodeImpl(DecoratorFactory factory, Session session, Node node, String path, int depth,
            NodeView selection) throws RepositoryException {
        super(factory, session, node);
        this.session = session;
        this.selection = selection;
        isVirtual = false;
        this.node = node;
        this.path = path;
        this.depth = depth;
        if (node.isNodeType(HippoNodeType.NT_FACETSEARCH) ||
            node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
            children = new TreeMap<String,Node[]>();
            properties = new HashMap();
        }
    }

    protected ServicingNodeImpl(DecoratorFactory factory, Session session, String nodeTypeName, String path,
            String name, int depth, NodeView selection) throws RepositoryException {
        super(factory, session, null);
        this.session = session;
        this.selection = selection;
        primaryNodeTypeName = nodeTypeName;
        isVirtual = true;
        this.node = null;
        this.path = path;
        this.name = name;
        this.depth = depth;
        children = new TreeMap<String,Node[]>();
        properties = new HashMap();
    }

    
    protected void addNode(String name, Node node) {
        if(children.containsKey(name)) {
            Node[] oldSiblings = children.get(name);
            Node[] newSiblings = new Node[oldSiblings.length+1];
            System.arraycopy(oldSiblings, 0, newSiblings, 0, oldSiblings.length);
            newSiblings[oldSiblings.length] = node;
            children.put(name, newSiblings);
        } else {
            Node[] siblings = new Node[1];
            siblings[0] = node;
            children.put(name, siblings);
        }
    }

    private boolean instantiated = false;
    protected void instantiate(String relPath) throws ValueFormatException, PathNotFoundException, VersionException,
                                                      UnsupportedRepositoryOperationException, ItemNotFoundException,
                                                      LockException, ConstraintViolationException, RepositoryException {
        if(instantiated)
            return;
        Node node = (this.node != null ? this.node : this);
        if(isNodeType(HippoNodeType.NT_FACETSEARCH)) {
            ServicingSessionImpl session = (ServicingSessionImpl) this.session;
            
            String facet = null;
            Value[] facets = null;
            try {
                facets = getProperty(HippoNodeType.HIPPO_FACETS).getValues();
                if(facets.length > 0)
                    facet = facets[0].getString();
            } catch (PathNotFoundException ex) {
                // safe to ignore
            }
            
            ServicingNodeImpl resultset;
            resultset = new ServicingNodeImpl(factory, session, HippoNodeType.NT_FACETRESULT,
                                              getChildPath(HippoNodeType.HIPPO_RESULTSET),
                                              HippoNodeType.HIPPO_RESULTSET, depth+1, selection);
            try {
                Value[] search = getProperty(HippoNodeType.HIPPO_SEARCH).getValues();
                resultset.setProperty(HippoNodeType.HIPPO_SEARCH, search);
            } catch (PathNotFoundException ex) {
                // safe to ignore
            }                
            resultset.setProperty(HippoNodeType.HIPPO_DOCBASE, getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
            if(facets == null)
                resultset.setProperty(HippoNodeType.HIPPO_FACETS, facets);
            try {
                resultset.setProperty(HippoNodeType.HIPPO_COUNT, getProperty(HippoNodeType.HIPPO_COUNT).getLong());
            } catch (PathNotFoundException ex) {
                // safe to ignore
            }
            addNode(HippoNodeType.HIPPO_RESULTSET, resultset);

            if(facet != null) {
                Map<String,Map<String,org.hippoecm.repository.FacetedNavigationEngine.Count>> facetSearchResultMap;
                facetSearchResultMap = new TreeMap<String,Map<String,FacetedNavigationEngine.Count>>();
                Map<String,FacetedNavigationEngine.Count> facetSearchResult;
                facetSearchResult = new TreeMap<String,FacetedNavigationEngine.Count>();
                facetSearchResultMap.put(facet, facetSearchResult);
                Value[] currentFacetPath = new Value[0];
                try {
                    currentFacetPath = getProperty(HippoNodeType.HIPPO_SEARCH).getValues();
                } catch(PathNotFoundException ex) {
                    // safe to ignore
                }
                FacetedNavigationEngine facetedEngine = session.getFacetedNavigationEngine();
                FacetedNavigationEngine.Query initialQuery;
                initialQuery = facetedEngine.parse(getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
                Map<String,String> currentFacetQuery = new TreeMap<String,String>();
                for(int i=0; i<currentFacetPath.length; i++) {
                    Matcher matcher = facetPropertyPattern.matcher(currentFacetPath[i].getString());
                    if(matcher.matches() && matcher.groupCount() == 2) {
                        currentFacetQuery.put(matcher.group(1), matcher.group(2));
                    }
                }
                String queryName;
                try {
                    queryName = node.getProperty(HippoNodeType.HIPPO_QUERYNAME).getString();
                } catch(PathNotFoundException ex) {
                    queryName = node.getName();
                }
        
                HitsRequested hitsRequested = new HitsRequested();
                hitsRequested.setResultRequested(false);
                facetedEngine.view(queryName, initialQuery, session.getFacetedNavigationContext(),
                                   currentFacetQuery, null, facetSearchResultMap, null, hitsRequested);

                Value[] newSearch;
                for(Map.Entry<String,FacetedNavigationEngine.Count> facetValue : facetSearchResult.entrySet()) {
                    if(relPath == null || relPath.equals(facetValue.getKey())) {
                        ServicingNodeImpl child = new ServicingNodeImpl(factory, session, HippoNodeType.NT_FACETSEARCH,
                                                                        getChildPath(facetValue.getKey()),
                                                                        facetValue.getKey(), depth+1, selection);
                        Value[] newFacets = new Value[Math.max(0, facets.length - 1)];
                        if(facets.length > 0)
                            System.arraycopy(facets, 1, newFacets, 0, facets.length - 1);
                        Value[] search = new Value[0];
                        try {
                            search = getProperty(HippoNodeType.HIPPO_SEARCH).getValues();
                        } catch (PathNotFoundException ex) {
                            // safe to ignore
                        }
                        if (facets.length > 0) {
                            newSearch = new Value[search.length + 1];
                            System.arraycopy(search, 0, newSearch, 0, search.length);
                            // check for xpath separator
                            if(facets[0].getString().indexOf(FACET_SEPARATOR) == -1)
                                newSearch[search.length] = session.getValueFactory().createValue("@" +
                                        facets[0].getString() + "='" + facetValue.getKey() + "'");
                            else
                                newSearch[search.length] = session.getValueFactory().createValue("@" +
                                        facets[0].getString().substring(0,facets[0].getString().indexOf(FACET_SEPARATOR)) +
                                        "='" + facetValue.getKey() + "'" +
                                        facets[0].getString().substring(facets[0].getString().indexOf(FACET_SEPARATOR)));
                        } else {
                            newSearch = (Value[]) search.clone(); // FIXME should not be necessary?
                        }
                        child.setProperty(HippoNodeType.HIPPO_QUERYNAME,
                                          getProperty(HippoNodeType.HIPPO_QUERYNAME).getString());
                        child.setProperty(HippoNodeType.HIPPO_DOCBASE,
                                          getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
                        child.setProperty(HippoNodeType.HIPPO_FACETS, newFacets);
                        child.setProperty(HippoNodeType.HIPPO_SEARCH, newSearch);
                        child.setProperty(HippoNodeType.HIPPO_COUNT, facetValue.getValue().count);
                        addNode(facetValue.getKey(), child);
                    }
                }
            }
            if(relPath == null)
                instantiated = true;

        } else if(isNodeType(HippoNodeType.NT_FACETRESULT)) {

            ServicingSessionImpl session = (ServicingSessionImpl) this.session;
            FacetedNavigationEngine facetedEngine = session.getFacetedNavigationEngine();
            Map<String,String> currentFacetQuery = new TreeMap<String,String>();
            Value[] currentFacetPath = new Value[0];
            try {
                currentFacetPath = getProperty(HippoNodeType.HIPPO_SEARCH).getValues();
            } catch(PathNotFoundException ex) {
            }
            for(int i=0; i<currentFacetPath.length; i++) {
                Matcher matcher = facetPropertyPattern.matcher(currentFacetPath[i].getString());
                if(matcher.matches() && matcher.groupCount() == 2) {
                    currentFacetQuery.put(matcher.group(1), matcher.group(2));
                }
            }
            FacetedNavigationEngine.Query initialQuery;
            initialQuery = facetedEngine.parse(getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
            String queryname = null;
            try {
                queryname = node.getProperty(HippoNodeType.HIPPO_QUERYNAME).getString();
            } catch(PathNotFoundException ex) {
            }
      
            HitsRequested hitsRequested = new HitsRequested();
            hitsRequested.setResultRequested(true);
            hitsRequested.setLimit(1000000);
            hitsRequested.setOffset(0);
            FacetedNavigationEngine.Result result;
            result = facetedEngine.view(queryname, initialQuery, session.getFacetedNavigationContext(), currentFacetQuery,
                                        null, hitsRequested);

            for(Iterator<String> iter = result.iterator(); iter.hasNext(); ) {
                String nodePath = iter.next();
                addNode(nodePath, session.getRootNode().getNode(nodePath.substring(1)));
            }

        } else if(isNodeType(HippoNodeType.NT_FACETSELECT)) {

            String path = getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
            if(path.startsWith("/")) {
                node = session.getRootNode();
                path = path.substring(1);
                node = node.getNode(path);
            } else {
                node = getNode(path);
            }

            Property modifyFacets = (hasProperty(HippoNodeType.HIPPO_FACETS)?getProperty(HippoNodeType.HIPPO_FACETS):null);
            Property modifyValues = (hasProperty(HippoNodeType.HIPPO_VALUES)?getProperty(HippoNodeType.HIPPO_VALUES):null);
            Property modifyModes  = (hasProperty(HippoNodeType.HIPPO_MODES)?getProperty(HippoNodeType.HIPPO_MODES):null);
            if(modifyFacets == null || modifyValues == null || modifyModes == null)
                if(modifyFacets != null || modifyValues != null && modifyModes != null)
                    throw new RepositoryException("iternal error");
            NodeView targetView = (modifyFacets == null ? selection
                                                    : new NodeView(selection, modifyFacets, modifyValues, modifyModes));

            for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                node = iter.nextNode();
                if(node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    node = targetView.match(unwrap(node));
                    if(node != null)
                        addNode(node.getName(), factory.getNodeDecorator(session, node, targetView));
                } else if(node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    for(NodeIterator sub = node.getNodes(); sub.hasNext(); ) {
                        node = targetView.match(unwrap(sub.nextNode()));
                        if(node != null)
                            addNode(node.getName(), factory.getNodeDecorator(session, node, targetView));
                    }
                } else {
                    node = unwrap(node);
                    addNode(node.getName(), factory.getNodeDecorator(session, node, targetView));
                }
            }

        }
    }

    protected void instantiate() throws ValueFormatException, PathNotFoundException, VersionException,
                                        UnsupportedRepositoryOperationException, ItemNotFoundException, LockException,
                                        ConstraintViolationException, RepositoryException {
        instantiate(null);
    }

    class NodeIteratorImpl implements NodeIterator {
        Iterator<Map.Entry<String,Node[]>> iter;
        Node[] currentSiblings;
        int relPosition;
        int absPosition;

        NodeIteratorImpl() {
            iter = children.entrySet().iterator();
            absPosition = 0;
            currentSiblings = null;
        }
        NodeIteratorImpl(Node node) {
            iter = null;
            absPosition = 0;
            relPosition = 0;
            currentSiblings = new Node[1];
            currentSiblings[0] = node;
        }
        NodeIteratorImpl(Node[] nodes) {
            iter = null;
            absPosition = 0;
            relPosition = 0;
            currentSiblings = nodes;
        }

        public boolean hasNext() {
            if(currentSiblings != null && relPosition < currentSiblings.length)
                return true;
            return iter != null ? iter.hasNext() : false;
        }

        public Object next() {
            Node rtValue = null;
            if(currentSiblings != null && relPosition < currentSiblings.length) {
                rtValue = currentSiblings[relPosition++];
            } else {
                if(iter == null)
                    throw new NoSuchElementException();
                Map.Entry<String,Node[]> entry = (Map.Entry<String,Node[]>) iter.next();
                currentSiblings = entry.getValue();
                if (currentSiblings == null) {
                    try {
                        rtValue = getNode(entry.getKey());
                    } catch (RepositoryException ex) {
                }
                } else {
                    relPosition = 0;
                    rtValue = currentSiblings[relPosition++];
                }
            }
            ++absPosition;
            return rtValue;
        }

        public Node nextNode() {
            return (Node) next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void skip(long skipNum) {
            while (skipNum-- > 0) {
                ++absPosition;
                iter.next();
            }
        }

        public long getSize() {
            int count = 0;
            for (Iterator<Node[]> i = children.values().iterator(); i.hasNext(); )
                count += i.next().length;
            return count;
        }

        public long getPosition() {
            return absPosition;
        }
    }

    class PropertyImpl implements Property {
        String name;
        int type;
        Value single;
        Value[] multi;
        PropertyDefinitionImpl propertyDefintion;

        class PropertyDefinitionImpl implements PropertyDefinition {
            public int getRequiredType() {
                return PropertyType.UNDEFINED;
            }

            public String[] getValueConstraints() {
                return null;
            }

            public Value[] getDefaultValues() {
                return null;
            }

            public boolean isMultiple() {
                return (multi != null);
            }

            public NodeType getDeclaringNodeType() {
                // FIXME
                return null;
            }

            public String getName() {
                return "*";
            }

            public boolean isAutoCreated() {
                return false;
            }

            public boolean isMandatory() {
                return false;
            }

            public int getOnParentVersion() {
                return OnParentVersionAction.COPY;
            }

            public boolean isProtected() {
                return false;
            }
        }

        public PropertyImpl(String name, Value value) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            this.name = name;
            setValue(value);
            propertyDefintion = this.new PropertyDefinitionImpl();
        }

        public PropertyImpl(String name, String value) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            this.name = name;
            setValue(value);
            propertyDefintion = this.new PropertyDefinitionImpl();
        }

        public PropertyImpl(String name, String[] values) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            this.name = name;
            setValue(values);
            propertyDefintion = this.new PropertyDefinitionImpl();
        }

        public PropertyImpl(String name, Value[] values) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            this.name = name;
            setValue(values);
            propertyDefintion = this.new PropertyDefinitionImpl();
        }

        public void setValue(Value value) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            single = value;
            multi = null;
        }

        public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            single = null;
            multi = values;
        }

        public void setValue(String value) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            single = session.getValueFactory().createValue(value);
            multi = null;
        }

        public void setValue(String[] values) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            single = null;
            multi = new Value[values.length];
            for (int i = 0; i < values.length; i++)
                multi[i] = session.getValueFactory().createValue(values[i]);
        }

        public void setValue(java.io.InputStream value) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            throw new RepositoryException("Not supported");
        }

        public void setValue(long value) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            single = session.getValueFactory().createValue(value);
            multi = null;
        }

        public void setValue(double value) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            single = session.getValueFactory().createValue(value);
            multi = null;
        }

        public void setValue(java.util.Calendar value) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            single = session.getValueFactory().createValue(value);
            multi = null;
        }

        public void setValue(boolean value) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            single = session.getValueFactory().createValue(value);
            multi = null;
        }

        public void setValue(Node value) throws ValueFormatException, VersionException, LockException,
                ConstraintViolationException, RepositoryException {
            single = session.getValueFactory().createValue(value.getUUID());
            multi = null;
        }

        public Value getValue() throws ValueFormatException, RepositoryException {
            if (single == null)
                throw new ValueFormatException("multi value propery");
            return single;
        }

        public Value[] getValues() throws ValueFormatException, RepositoryException {
            if (multi == null)
                throw new ValueFormatException("single value property");
            return multi;
        }

        public String getString() throws ValueFormatException, RepositoryException {
            if (single == null)
                throw new ValueFormatException("multi value propery");
            return single.getString();
        }

        public java.io.InputStream getStream() throws ValueFormatException, RepositoryException {
            throw new RepositoryException("Not supported");
        }

        public long getLong() throws ValueFormatException, RepositoryException {
            return single.getLong();
        }

        public double getDouble() throws ValueFormatException, RepositoryException {
            return single.getDouble();
        }

        public java.util.Calendar getDate() throws ValueFormatException, RepositoryException {
            return single.getDate();
        }

        public boolean getBoolean() throws ValueFormatException, RepositoryException {
            return single.getBoolean();
        }

        public Node getNode() throws ValueFormatException, RepositoryException {
            return ServicingNodeImpl.this;
        }

        public long getLength() throws ValueFormatException, RepositoryException {
            if (multi == null)
                throw new ValueFormatException("multi value property");
            return -1;
        }

        public long[] getLengths() throws ValueFormatException, RepositoryException {
            if (multi == null)
                throw new ValueFormatException("single value property");
            long[] rtvalue = new long[multi.length];
            for (int i = 0; i < multi.length; i++)
                rtvalue[i] = -1;
            return rtvalue;
        }

        public PropertyDefinition getDefinition() throws RepositoryException {
            return propertyDefintion;
        }

        public int getType() throws RepositoryException {
            return type;
        }

        public String getPath() throws RepositoryException {
            return ServicingNodeImpl.this.getPath() + "/" + getName();
        }

        public String getName() throws RepositoryException {
            return name;
        }

        public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
            return ServicingNodeImpl.this.getAncestor(depth - 1);
        }

        public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
            return ServicingNodeImpl.this;
        }

        public int getDepth() throws RepositoryException {
            return ServicingNodeImpl.this.getDepth() + 1;
        }

        public Session getSession() throws RepositoryException {
            return session;
        }

        public boolean isNode() {
            return false;
        }

        public boolean isNew() {
            return false;
        }

        public boolean isModified() {
            return false;
        }

        public boolean isSame(Item otherItem) throws RepositoryException {
            return otherItem == this;
        }

        public void accept(ItemVisitor visitor) throws RepositoryException {
            visitor.visit(this);
        }

        public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException,
                InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException,
                NoSuchNodeTypeException, RepositoryException {
        }

        public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        }

        public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
            properties.remove(name);
        }
    }

    class PropertyIteratorImpl implements PropertyIterator {
        Iterator<Property> iter;
        int position;

        PropertyIteratorImpl() {
            iter = properties.values().iterator();
            position = 0;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Object next() {
            ++position;
            return iter.next();
        }

        public Property nextProperty() {
            ++position;
            return iter.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void skip(long skipNum) {
            while (skipNum-- > 0) {
                ++position;
                iter.next();
            }
        }

        public long getSize() {
            int count = 0;
            for (Iterator i = children.values().iterator(); i.hasNext(); i.next())
                ++count;
            return count;
        }

        public long getPosition() {
            return position;
        }
    }

    public int getDepth() throws ItemNotFoundException, RepositoryException {
        return depth;
    }
    
    public String getDisplayName() throws RepositoryException {
        if (isVirtual) {
            
            // hippo:authorId#//element(*,hippo:author)[hippo:id=?]/@hippo:name
            // just return the resultset
            if (getName().equals(HippoNodeType.HIPPO_RESULTSET)) {
                return HippoNodeType.HIPPO_RESULTSET;
            }
            
            // the last search is the current one
            Value[] searches = getProperty(HippoNodeType.HIPPO_SEARCH).getValues();
            if (searches.length == 0) {
                return getName();
            }
            String search = searches[searches.length-1].getString();
                
            // check for search seperator
            if (search.indexOf(FACET_SEPARATOR) == -1) {
                return getName();
            }
            
            // check for sql parameter '?'
            String xpath = search.substring(search.indexOf(FACET_SEPARATOR)+1);
            if (xpath.indexOf('?') == -1) {
                return getName();
            }
                
            // construct query
            xpath = xpath.substring(0,xpath.indexOf('?')) + getName() + xpath.substring(xpath.indexOf('?')+1);
            
            Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
            
            // execute
            QueryResult result = query.execute();
            RowIterator iter = result.getRows();
            if (iter.hasNext()) {
                return iter.nextRow().getValues()[0].getString();
            } else {
                return getName();
            }
        } else {
            return getName();
        }
    }

    public String getName() throws RepositoryException {
        if (node == null) {
            return name;
        } else
            return super.getName();
    }

    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        if (isVirtual)
            return session.getRootNode().getNode(path); // FIXME
        else
            return super.getParent();
    }

    public String getPath() throws RepositoryException {
        return path;
    }

    public String getChildPath(String name) throws RepositoryException {
        String path = getPath();
        if (path.endsWith("/"))
            return path + name;
        else
            return path + "/" + name;
    }

    public Session getSession() throws RepositoryException {
        if (node == null) {
            return session;
        } else
            return super.getSession();
    }

    public static Node unwrap(Node node) {
        if (node == null) {
            return null;
        }
        if (node instanceof ServicingNodeImpl) {
            node = (Node) ((ServicingNodeImpl) node).unwrap();
        } else {
            throw new IllegalStateException("node is not of type ServicingNodeImpl");
        }
        return node;
    }

    static void decoratePathProperty(Node node) throws RepositoryException {
        if(node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            try {
                String path = node.getPath();
                if(path.startsWith("/"))
                    path = path.substring(1);
                String[] pathElements = path.split("/");
                for(int i=1; i<pathElements.length; i++)
                pathElements[i] = pathElements[i-1] + "/" + pathElements[i];
                node.setProperty(HippoNodeType.HIPPO_PATHS, pathElements);
            } catch(ValueFormatException ex) {
                // FIXME: log some serious error
                throw ex;
            } catch(VersionException ex) {
                // FIXME: log some serious error
                throw ex;
            } catch(LockException ex) {
                // FIXME: log some serious error
                throw ex;
            } catch(ConstraintViolationException ex) {
                // FIXME: log some serious error
                throw ex;
            }
        }
    }

    /**
     * @inheritDoc
     */
    public Node addNode(String name) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        Node child = node.addNode(name);
        decoratePathProperty(child);
        return factory.getNodeDecorator(session, child, getChildPath(name), node.getDepth(), selection);
    }

    /**
     * @inheritDoc
     */
    public Node addNode(String name, String type) throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        Node child = node.addNode(name, type);
        decoratePathProperty(child);
        return factory.getNodeDecorator(session, child, getChildPath(name), node.getDepth(), selection);
    }

    /**
     * @inheritDoc
     */
    public void orderBefore(String srcChildRelPath, String destChildRelPath)
            throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException,
            ItemNotFoundException, LockException, RepositoryException {
        node.orderBefore(srcChildRelPath, destChildRelPath);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, value));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, value));
        }
        Property prop = node.setProperty(name, value, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, values));
        }
        Property prop = node.setProperty(name, values);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, values));
        }
        Property prop = node.setProperty(name, values, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, values));
        }
        Property prop = node.setProperty(name, values);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, values));
        }
        Property prop = node.setProperty(name, values, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, session.getValueFactory().createValue(
                    value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, value));
        }
        Property prop = node.setProperty(name, value, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, session.getValueFactory().createValue(
                    value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, session.getValueFactory().createValue(
                    value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, double value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, session.getValueFactory().createValue(
                    value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, session.getValueFactory().createValue(
                    value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException,
            LockException, ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, session.getValueFactory().createValue(
                    value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if (node == null) {
            return properties.put(name, this.new PropertyImpl(name, session.getValueFactory().createValue(
                    value.getUUID())));
        }
        Property prop = node.setProperty(name, ServicingNodeImpl.unwrap(value));
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        instantiate(null); // FIXME: instantiate(relPath);
        if(children != null) {
            int pathPosition = relPath.indexOf("/");
            String childName = (pathPosition < 0 ? relPath : relPath.substring(0,pathPosition));
            int indexPosition = childName.indexOf("[");
            Node[] siblings = children.get(indexPosition < 0 ? childName : childName.substring(0,indexPosition-1));
            if(selection != null)
                siblings = selection.getNodes(siblings);
            if(indexPosition > 0)
                indexPosition = Integer.parseInt(childName.substring(indexPosition+1));
            else
                indexPosition = 0;
            if(siblings == null || indexPosition >= siblings.length || siblings[indexPosition] == null)
                throw new PathNotFoundException();
            if(pathPosition < 0)
                return siblings[indexPosition];
            else
                return siblings[indexPosition].getNode(relPath.substring(pathPosition+1));
        } else {
            try {
                Node n;
                if(isNodeType(HippoNodeType.NT_FACETSELECT) && selection != null)
                    n = selection.getNode(node.getNodes(relPath));
                else
                    n = node.getNode(relPath);
                return factory.getNodeDecorator(session,n,getChildPath(relPath),getDepth()+1,selection);
            } catch (PathNotFoundException ex) {
                ServicingSessionImpl session = (ServicingSessionImpl) this.session;
                try {
                    Path p = session.getQPath(relPath);
                    Path.PathElement[] elements = p.getElements();
                    Node node = this;
                    if (elements.length < 2)
                        throw ex;
                    for (int i = 0; i < elements.length; i++) {
                        node = node.getNode(elements[i].getName().getLocalName());
                    }
                    if (!(node instanceof ServicingNodeImpl))
                        node = new ServicingNodeImpl(factory,session,node,getChildPath(relPath),getDepth()+1,selection);
                    return node;
                } catch (ClassCastException ex2) {
                    throw ex;
                } catch (NameException ex2) {
                    throw ex;
                }
            }
          }
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes() throws RepositoryException {        
        if (children != null) {
            instantiate();
            if(isNodeType(HippoNodeType.NT_FACETSELECT) && selection != null)
                return selection.getNodes(this . new NodeIteratorImpl());
            else
                return this . new NodeIteratorImpl();
        } else {
            NodeIterator iter = new DecoratingNodeIterator(factory, session, node.getNodes(), this);
            if(isNodeType(HippoNodeType.NT_HANDLE) && selection != null)
                iter = selection.getNodes(iter);
            return iter;
        }
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        if (children != null) {
            instantiate();

            int pathPosition = namePattern.indexOf("/");
            String childName = (pathPosition < 0 ? namePattern : namePattern.substring(0,pathPosition));
            int indexPosition = childName.indexOf("[");
            Node[] siblings = children.get(indexPosition < 0 ? childName : childName.substring(0,indexPosition-1));
            if(selection != null)
                siblings = selection.getNodes(siblings);
            if(indexPosition > 0)
                indexPosition = Integer.parseInt(childName.substring(indexPosition+1));
            else
                indexPosition = -1;
            if(siblings==null || indexPosition>=siblings.length || (indexPosition>=0 && siblings[indexPosition]==null))
                throw new PathNotFoundException();
            if(isNodeType(HippoNodeType.NT_FACETSELECT) && selection != null)
                siblings = selection.getNodes(siblings);
            if(pathPosition < 0) {
                if(indexPosition >= 0)
                    return this . new NodeIteratorImpl(siblings[indexPosition]);
                else
                    return this . new NodeIteratorImpl(siblings);
            } else {
                return siblings[indexPosition].getNodes(namePattern.substring(pathPosition+1));
            }
        } else {
            NodeIterator iter = new DecoratingNodeIterator(factory, session, node.getNodes(namePattern), this);
            if(isNodeType(HippoNodeType.NT_HANDLE) && selection != null)
                iter = selection.getNodes(iter);
            return iter;
        }
    }

    /**
     * @inheritDoc
     */
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        if (node == null) {
            Property rtvalue = properties.get(relPath);
            if(rtvalue == null)
              throw new PathNotFoundException(relPath);
            return rtvalue;
        }

        Property prop = node.getProperty(relPath);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getProperties() throws RepositoryException {
        if (node == null) {
            return this.new PropertyIteratorImpl();
        }
        return new DecoratingPropertyIterator(factory, session, node.getProperties());
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        return new DecoratingPropertyIterator(factory, session, node.getProperties(namePattern));
    }

    /**
     * @inheritDoc
     */
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        return factory.getItemDecorator(session, node.getPrimaryItem());
    }

    /**
     * @inheritDoc
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return node.getUUID();
    }

    /**
     * @inheritDoc
     */
    public int getIndex() throws RepositoryException {
        return node.getIndex();
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getReferences() throws RepositoryException {
        return new DecoratingPropertyIterator(factory, session, node.getReferences());
    }

    /**
     * @inheritDoc
     */
    public boolean hasNode(String relPath) throws RepositoryException {
        instantiate(null); // FIXME: instantiate(relPath);
        if(children != null) {
            int pathPosition = relPath.indexOf("/");
            String childName = (pathPosition < 0 ? relPath : relPath.substring(0,pathPosition));
            int indexPosition = childName.indexOf("[");
            Node[] siblings = children.get(indexPosition < 0 ? childName : childName.substring(0,indexPosition-1));
            if(selection != null)
                siblings = selection.getNodes(siblings);
            if(indexPosition > 0)
                indexPosition = Integer.parseInt(childName.substring(indexPosition+1));
            else
                indexPosition = 0;
            if(siblings == null || indexPosition >= siblings.length || siblings[indexPosition] == null)
                return false;
            if(pathPosition < 0)
                return true;
            else
                return siblings[indexPosition].hasNode(relPath.substring(pathPosition+1));
        } else {
            try {
                Node n;
                if(isNodeType(HippoNodeType.NT_FACETSELECT) && selection != null)
                    if(selection.getNode(node.getNodes(relPath)) != null)
                        return true;
                    else
                        return false;
                else
                    return node.hasNode(relPath);
            } catch (PathNotFoundException ex) {
                ServicingSessionImpl session = (ServicingSessionImpl) this.session;
                try {
                    Path p = session.getQPath(relPath);
                    Path.PathElement[] elements = p.getElements();
                    Node node = this;
                    if (elements.length < 2)
                        throw ex;
                    for (int i = 0; i < elements.length-1; i++) {
                        node = node.getNode(elements[i].getName().getLocalName());
                    }
                    return node.hasNode(elements[elements.length-1].getName().getLocalName());
                } catch (PathNotFoundException ex2) {
                    return false;
                } catch (ClassCastException ex2) {
                    throw ex;
                } catch (NameException ex2) {
                    throw ex;
                }
            }
          }
    }

    /**
     * @inheritDoc
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        if (node == null) {
            return properties.containsKey(relPath);
        }
        return node.hasProperty(relPath);
    }

    /**
     * @inheritDoc
     */
    public boolean hasNodes() throws RepositoryException {

        // FIXME: doesn't really check if the node has childeren. A result set can be empty 
        if (isNodeType(HippoNodeType.NT_FACETSEARCH) || isNodeType(HippoNodeType.NT_FACETRESULT)) {
            return true;
        }
        return node.hasNodes();
    }

    /**
     * @inheritDoc
     */
    public boolean hasProperties() throws RepositoryException {
        return node.hasProperties();
    }

    /**
     * @inheritDoc
     */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return node.getPrimaryNodeType();
    }

    /**
     * @inheritDoc
     */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        return node.getMixinNodeTypes();
    }

    /**
     * @inheritDoc
     */
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        if (node == null) {
            return primaryNodeTypeName.equals(nodeTypeName);
        }
        return node.isNodeType(nodeTypeName);
    }

    /**
     * @inheritDoc
     */
    public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        node.addMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        node.removeMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        return node.canAddMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public NodeDefinition getDefinition() throws RepositoryException {
        return node.getDefinition();
    }

    /**
     * @inheritDoc
     */
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {
        Version version = node.checkin();
        return factory.getVersionDecorator(session, version);
    }

    /**
     * @inheritDoc
     */
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        node.checkout();
    }

    /**
     * @inheritDoc
     */
    public void doneMerge(Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        node.doneMerge(VersionDecorator.unwrap(version));
    }

    /**
     * @inheritDoc
     */
    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        node.cancelMerge(VersionDecorator.unwrap(version));
    }

    /**
     * @inheritDoc
     */
    public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException,
            InvalidItemStateException, RepositoryException {
        node.update(srcWorkspaceName);
    }

    /**
     * @inheritDoc
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException,
            AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        NodeIterator nodes = node.merge(srcWorkspace, bestEffort);
        return new DecoratingNodeIterator(factory, session, nodes);
    }

    /**
     * @inheritDoc
     */
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException,
            NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        return node.getCorrespondingNodePath(workspaceName);
    }

    /**
     * @inheritDoc
     */
    public boolean isCheckedOut() throws RepositoryException {
        return node.isCheckedOut();
    }

    /**
     * @inheritDoc
     */
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        node.restore(versionName, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException, RepositoryException {
        node.restore(VersionDecorator.unwrap(version), removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException,
            ItemExistsException, VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        node.restore(VersionDecorator.unwrap(version), relPath, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException,
            ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException,
            RepositoryException {
        node.restoreByLabel(versionLabel, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        VersionHistory hist = node.getVersionHistory();
        return factory.getVersionHistoryDecorator(session, hist);
    }

    /**
     * @inheritDoc
     */
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        return factory.getVersionDecorator(session, node.getBaseVersion());
    }

    /**
     * @inheritDoc
     */
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        Lock lock = node.lock(isDeep, isSessionScoped);
        return factory.getLockDecorator(session, lock);
    }

    /**
     * @inheritDoc
     */
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            RepositoryException {
        Lock lock = node.getLock();
        return factory.getLockDecorator(session, lock);
    }

    /**
     * @inheritDoc
     */
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException,
            InvalidItemStateException, RepositoryException {
        node.unlock();
    }

    /**
     * @inheritDoc
     */
    public boolean holdsLock() throws RepositoryException {
        return node.holdsLock();
    }

    /**
     * @inheritDoc
     */
    public boolean isLocked() throws RepositoryException {
        return node.isLocked();
    }

    public void save()
        throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
               ReferentialIntegrityException, VersionException, LockException, RepositoryException
    {
        super.save();
    }
}
