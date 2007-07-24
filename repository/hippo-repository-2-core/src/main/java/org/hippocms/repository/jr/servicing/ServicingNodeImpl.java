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
package org.hippocms.repository.jr.servicing;

import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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


public class ServicingNodeImpl extends ItemDecorator implements ServicingNode {
    final static private String SVN_ID = "$Id$";

    private final static String FACET_RESULTSET = "resultset";
    private final static char FACET_SEPARATOR = '#';
    
    protected Node node;
    protected Session session;
    protected boolean isVirtual;
    protected boolean isQuery;
    protected String primaryNodeTypeName;
    protected String name;
    protected String path;
    protected int depth;
    protected long resultNodeCount = -1L;
    protected Map<String,Node> children = null;
    protected Map<String,Property> properties = null;
    protected Map facetValuesMap = null;

    protected ServicingNodeImpl(DecoratorFactory factory, Session session, Node node) {
        super(factory, session, node);
        this.session = session;
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

    protected ServicingNodeImpl(DecoratorFactory factory, Session session, Node node, String path, int depth)
            throws RepositoryException {
        super(factory, session, node);
        this.session = session;
        isVirtual = false;
        this.node = node;
        this.path = path;
        this.depth = depth;
        if (node.isNodeType("hippo:facetsearch")) {
            children = new HashMap();
            properties = new HashMap();
        }
    }

    protected ServicingNodeImpl(DecoratorFactory factory, Session session, String nodeTypeName, String path,
            String name, int depth) throws RepositoryException {
        super(factory, session, null);
        this.session = session;
        primaryNodeTypeName = nodeTypeName;
        isVirtual = true;
        this.node = null;
        this.path = path;
        this.name = name;
        this.depth = depth;
        children = new HashMap();
        properties = new HashMap();
    }

    
    /* begin of virtual node implementation */
    protected void addNode(String name, Node node) {
        children.put(name, node);
    }    

    private boolean instantiated = false;

    /**
     * Instantiate the resultset
     * @throws RepositoryException
     */
    private void instantiateResults() throws RepositoryException {
        if (instantiated) {
            return;
        }
        instantiated = true;

        Node node = (this.node != null ? this.node : this);

        StringBuffer searchquery = getSearchQuery(node);
        //System.out.println("Query results: " + searchquery.toString());

        Query facetValuesQuery = session.getWorkspace().getQueryManager().createQuery(searchquery.toString(),
                Query.XPATH);
        QueryResult facetValuesResult = facetValuesQuery.execute();

        for (NodeIterator iter = facetValuesResult.getNodes(); iter.hasNext();) {
            Node resultNode = (Node) iter.next();
            addNode(resultNode.getName(), resultNode);
        }
    }
    
    /**
     * Instantiate the list with facets
     * @throws RepositoryException
     */
    private void instantiateFacets() throws RepositoryException {
        if (instantiated) {
            return;
        }
        instantiated = true;

        Node node = (this.node != null ? this.node : this);

        addNode(FACET_RESULTSET, (Node) null);

        StringBuffer searchquery = getSearchQuery(node);

        Value[] facets = node.getProperty("hippo:facets").getValues();
        if (facets.length > 0) {

            // search for the facet values, not the nodes
            String facet = facets[0].getString();
            if (facet.indexOf(FACET_SEPARATOR) != -1) {
                facet = facet.substring(0, facet.indexOf(FACET_SEPARATOR));
            }
            searchquery.append("/@");
            searchquery.append(facet);

            //System.out.println("Query facets: " + searchquery.toString());

            buildFacetMap(searchquery.toString());
            for (Iterator iter = facetValuesMap.keySet().iterator(); iter.hasNext();) {
                addNode((String) iter.next(), (Node) null);
            }
        }
    }

    /**
     * Get the xpath expression based on the facets and searches of the node
     * @param node The current node
     * @return The xpath expression 
     * @throws RepositoryException
     */
    private StringBuffer getSearchQuery(Node node) throws RepositoryException {
        StringBuffer searchquery = new StringBuffer("");

        if (node == null) {
            return searchquery;
        }

        // Build searchquery from hippo:search multivalues
        Value[] searchClauses = new Value[0];
        try {
            if (node.getProperty("hippo:search") != null) {
                searchClauses = node.getProperty("hippo:search").getValues();
            }
        } catch (PathNotFoundException ex) {
            // safe to ignore
        }

        String clause;
        for (int i = 0; i < searchClauses.length; i++) {
            if (searchquery.length() > 0) {
                searchquery.append(",");
            }

            // check for xpath separator
            clause = searchClauses[i].getString();
            if (clause.indexOf(FACET_SEPARATOR) != -1) {
                clause = clause.substring(0, clause.indexOf(FACET_SEPARATOR));
            }
            //System.out.println("Clause: " + clause);
            searchquery.append(clause);
        }

        Value[] facets = node.getProperty("hippo:facets").getValues();
        if (facets.length > 0) {

            String facet = facets[0].getString();
            if (facet.indexOf(FACET_SEPARATOR) != -1) {
                facet = facet.substring(0, facet.indexOf(FACET_SEPARATOR));
            }

            if (searchquery.length() > 0) {
                searchquery.append(",");
            }
            searchquery.append("@");
            //System.out.println("Facet: @" + facet);
            searchquery.append(facet);
        }
        searchquery.insert(0, node.getProperty("hippo:docbase").getString() + "//node()" + "[");
        searchquery.append("]");

        return searchquery;
    }
    
    private void buildFacetMap(String xPath) throws RepositoryException {
        Query facetValuesQuery = session.getWorkspace().getQueryManager().createQuery(xPath, Query.XPATH);        QueryResult facetValuesResult = facetValuesQuery.execute();
        facetValuesMap = new HashMap();
        
        long count = 0L;
        resultNodeCount = 0L;
        for (RowIterator iter = facetValuesResult.getRows(); iter.hasNext();) {
            String facetValue = iter.nextRow().getValues()[0].getString();
            if (facetValuesMap.containsKey(facetValue)) {
                // facet value already exists in map, increase count
                count = 1 + (Long) facetValuesMap.get(facetValue);
                facetValuesMap.put(facetValue, count);
            } else {
                // a new facet value, set count to 1
                facetValuesMap.put(facetValue,1L);
            }
            resultNodeCount++;
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

    class NodeIteratorImpl implements NodeIterator {
        Iterator iter;
        int position;

        NodeIteratorImpl() {
            iter = children.entrySet().iterator();
            position = 0;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Object next() {
            Map.Entry entry = (Map.Entry) iter.next();
            ++position;
            Object rtValue = entry.getValue();
            if (rtValue == null) {
                try {
                    rtValue = getNode((String) entry.getKey());
                } catch (RepositoryException ex) {
                    return null;
                }
            }
            return rtValue;
        }

        public Node nextNode() {
            return (Node) next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void skip(long skipNum) {
            while (skipNum > 0) {
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
            while (skipNum > 0) {
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
            if (getName().equals(FACET_RESULTSET)) {
                return FACET_RESULTSET;
            }
            
            // the last search is the current one
            Value[] searches = getProperty("hippo:search").getValues();
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
            
            //System.out.println("DsplayName xpath: " + xpath);
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

    /* end of virtual node implementation */

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

    /**
     * @inheritDoc
     */
    public Node addNode(String name) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        Node child = node.addNode(name);
        return factory.getNodeDecorator(session, child, getChildPath(name), node.getDepth());
    }

    /**
     * @inheritDoc
     */
    public Node addNode(String name, String type) throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        Node child = node.addNode(name, type);
        return factory.getNodeDecorator(session, child, getChildPath(name), node.getDepth());
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
        if (isNodeType("hippo:facetsearch")) {
            if (relPath.equals(FACET_RESULTSET)) {
                ServicingNodeImpl child = new ServicingNodeImpl(factory, session, "hippo:facetresult",
                        getChildPath(relPath), relPath, depth + 1);
                try {
                    Value[] search = getProperty("hippo:search").getValues();
                    child.setProperty("hippo:search", search);
                } catch (PathNotFoundException ex) {
                    // safe to ignore
                }                
                try {
                    Value[] facets = getProperty("hippo:facets").getValues();
                    child.setProperty("hippo:facets", facets);
                } catch (PathNotFoundException ex) {
                    // safe to ignore
                }

                child.setProperty("hippo:docbase", getProperty("hippo:docbase").getString());
                if (resultNodeCount > -1) {
                    child.setProperty("hippo:count", resultNodeCount);
                }
                return child;
            } else {
                ServicingNodeImpl child = new ServicingNodeImpl(factory, session, "hippo:facetsearch",
                        getChildPath(relPath), relPath, depth + 1);
                Value[] facets = getProperty("hippo:facets").getValues();
                Value[] newFacets = new Value[Math.max(0, facets.length - 1)];
                if (facets.length > 0)
                    System.arraycopy(facets, 1, newFacets, 0, facets.length - 1);
                Value[] search = new Value[0];
                try {
                    search = getProperty("hippo:search").getValues();
                } catch (PathNotFoundException ex) {
                    // safe to ignore
                }
                Value[] newSearch;
                if (facets.length > 0) {
                    newSearch = new Value[search.length + 1];
                    System.arraycopy(search, 0, newSearch, 0, search.length);
                    
                    // check for xpath separator
                    if (facets[0].getString().indexOf(FACET_SEPARATOR) == -1) {
                        newSearch[search.length] = session.getValueFactory().createValue(
                            "@" + facets[0].getString() + "='" + relPath + "'");
                    } else {
                        newSearch[search.length] = session.getValueFactory().createValue(
                                "@" + facets[0].getString().substring(0,facets[0].getString().indexOf(FACET_SEPARATOR)) +
                                "='" + relPath + "'" + facets[0].getString().substring(facets[0].getString().indexOf(FACET_SEPARATOR)));
                    }
                } else {
                    newSearch = (Value[]) search.clone();
                }
                child.setProperty("hippo:docbase", getProperty("hippo:docbase").getString());
                child.setProperty("hippo:facets", newFacets);
                child.setProperty("hippo:search", newSearch);
                
                if (facetValuesMap != null && facetValuesMap.containsKey(child.getName())) {
                    child.setProperty("hippo:count", (Long) facetValuesMap.get(child.getName()));
                }
                
                return child;
            }
        } else {
          if(node == null) {
            Node n = children.get(relPath);
            if(n != null)
              return n;
            else
              throw new PathNotFoundException();
          } else {
            try {
                Node n = node.getNode(relPath);
                return factory.getNodeDecorator(session, n, getChildPath(relPath), getDepth() + 1);
            } catch (PathNotFoundException ex) {
                try {
                    SessionImpl session = (SessionImpl) this.session;
                    Path p = session.getQPath(relPath);
                    Path.PathElement[] elements = p.getElements();
                    Node node = this;
                    if (elements.length < 2)
                        throw ex;
                    for (int i = 0; i < elements.length; i++) {
                        node = node.getNode(elements[i].getName().getLocalName());
                    }
                    if (!(node instanceof ServicingNodeImpl))
                        node = new ServicingNodeImpl(factory, session, node, getChildPath(relPath), getDepth() + 1);
                    return node;
                } catch (ClassCastException ex2) {
                    throw ex;
                } catch (NameException ex2) {
                    throw ex;
                }
            }
          }
        }
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes() throws RepositoryException {        
        if (children != null) {
            if (isNodeType("hippo:facetsearch")) {
                instantiateFacets();
            } else if (isNodeType("hippo:facetresult")) {
                instantiateResults();
            }
            return this.new NodeIteratorImpl();
        } else {
            return new DecoratingNodeIterator(factory, session, node.getNodes(), this);
        }
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        return new DecoratingNodeIterator(factory, session, node.getNodes(namePattern));
    }

    /**
     * @inheritDoc
     */
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        if (node == null) {
            return properties.get(relPath);
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
        if (node == null) {
            return children.containsKey(relPath);
        }
        return node.hasNode(relPath);
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
        if (node == null) {
            return !children.isEmpty();
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

    public Service getService() throws RepositoryException {
        return ((ServicingWorkspaceImpl) getSession().getWorkspace()).getServicesManager().getService(this);
    }



  public void save()
    throws AccessDeniedException, ConstraintViolationException, InvalidItemStateException,
           ReferentialIntegrityException, VersionException, LockException, RepositoryException
  {
    super.save();
  }
}
