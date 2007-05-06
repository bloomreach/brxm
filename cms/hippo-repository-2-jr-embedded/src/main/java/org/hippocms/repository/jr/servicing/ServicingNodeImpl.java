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

import java.lang.Object;
import java.lang.String;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.NoSuchElementException;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ItemVisitor;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.MergeException;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;

import org.apache.jackrabbit.core.XASession;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.HierarchyManagerImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.NameException;

// FIXME: depend only on JTA, not on Atomikos
import com.atomikos.icatch.config.TSInitInfo;
import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;

public class ServicingNodeImpl extends ItemDecorator
  implements ServicingNode
{
    final static private String SVN_ID = "$Id$";

    protected Node node;
    protected Session session;
    protected boolean isVirtual;
    protected boolean isQuery;
    protected String primaryNodeTypeName;
    protected String name;
    protected String path;
    protected int depth;
    protected Map children = null;
    protected Map properties = null;

    protected ServicingNodeImpl(DecoratorFactory factory, Session session, Node node) {
      super(factory, session, node);
      this.session = session;
      isVirtual = false;
      this.node = node;
      try {
        this.path = node.getPath();
      } catch(RepositoryException ex) {
        this.path = null;
      }
      try {
        this.depth = node.getDepth();
      } catch(RepositoryException ex) {
        this.depth = -1;
      }
    }
    protected ServicingNodeImpl(DecoratorFactory factory, Session session, Node node, String path, int depth) throws RepositoryException {
      super(factory, session, node);
      this.session = session;
      isVirtual = false;
      this.node = node;
      this.path = path;
      this.depth = depth;
      if(node.isNodeType("hippo:facetsearch")) {
        children = new HashMap();
        properties = new HashMap();
      }
    }
    protected ServicingNodeImpl(DecoratorFactory factory, Session session, String nodeTypeName, String path, String name, int depth) throws RepositoryException {
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
  private void instantiate() throws RepositoryException {
    if(instantiated)
      return;
    instantiated = true;
    /*
     * The XPath defined for JCR-170 lacks the possibility to select distinct nodes.
     */
    addNode("resultset", (Node)null);
    QueryManager qmngr = session.getWorkspace().getQueryManager();
    Node node = (this.node != null ? this.node : this);
    String searchquery = null;
    Value[] searchClauses = new Value[0];
    try {
      if(node.getProperty("hippo:search") != null)
        searchClauses = node.getProperty("hippo:search").getValues();
    } catch(PathNotFoundException ex) {
      // safe to ignore
    }
    for(int i=0; i<searchClauses.length; i++) {
      if(searchquery != null)
        searchquery += ",";
      else
        searchquery = "";
      searchquery += searchClauses[i].getString();
    }
    Value[] facets = node.getProperty("hippo:facets").getValues();
    if(facets.length > 0) {
      if(searchquery != null)
        searchquery += ",";
      else
        searchquery = "";
      searchquery += "@" + facets[0].getString();
      if(searchquery != null)
        searchquery = "[" + searchquery + "]";
      else
        searchquery = "";
      searchquery = node.getProperty("hippo:docbase").getString() + "//node()" + searchquery;
      searchquery += "/@" + facets[0].getString();
      Query facetValuesQuery = qmngr.createQuery(searchquery, Query.XPATH); 
      QueryResult facetValuesResult = facetValuesQuery.execute();
      Set facetValuesSet = new HashSet();
      for(RowIterator iter=facetValuesResult.getRows(); iter.hasNext(); ) {
        facetValuesSet.add(iter.nextRow().getValues()[0].getString());
      }
      for(Iterator iter = facetValuesSet.iterator(); iter.hasNext(); ) {
        //System.out.println("      "+iter.next());
        addNode((String) iter.next(), (Node)null);
      }
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

    public PropertyImpl(String name, Value value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      this.name = name;
      setValue(value);
      propertyDefintion = this . new PropertyDefinitionImpl();
    }
    public PropertyImpl(String name, String value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      this.name = name;
      setValue(value);
      propertyDefintion = this . new PropertyDefinitionImpl();
    }
    public PropertyImpl(String name, String[] values)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      this.name = name;
      setValue(values);
      propertyDefintion = this . new PropertyDefinitionImpl();
    }
    public PropertyImpl(String name, Value[] values)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      this.name = name;
      setValue(values);
      propertyDefintion = this . new PropertyDefinitionImpl();
    }
    public void setValue(Value value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      single = value;
      multi  = null;
    }
    public void setValue(Value[] values)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      single = null;
      multi  = values;
    }
    public void setValue(String value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      single = session.getValueFactory().createValue(value);
      multi  = null;
    }
    public void setValue(String[] values)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      single = null;
      multi  = new Value[values.length];
      for(int i=0; i<values.length; i++)
        multi[i] = session.getValueFactory().createValue(values[i]);
    }
    public void setValue(java.io.InputStream value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      throw new RepositoryException("Not supported");
    }
    public void setValue(long value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      single = session.getValueFactory().createValue(value);
      multi  = null;
    }
    public void setValue(double value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      single = session.getValueFactory().createValue(value);
      multi  = null;
    }
    public void setValue(java.util.Calendar value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      single = session.getValueFactory().createValue(value);
      multi  = null;
    }
    public void setValue(boolean value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      single = session.getValueFactory().createValue(value);
      multi  = null;
    }
    public void setValue(Node value)
      throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException
    {
      single = session.getValueFactory().createValue(value.getUUID());
      multi  = null;
    }
    public Value getValue()
      throws ValueFormatException, RepositoryException
    {
      if(single == null)
        throw new ValueFormatException("multi value propery");
      return single;
    }
    public Value[] getValues()
      throws ValueFormatException, RepositoryException
    {
      if(multi == null)
        throw new ValueFormatException("single value property");
      return multi;
    }
    public String getString()
      throws ValueFormatException, RepositoryException
    {
      if(single == null)
        throw new ValueFormatException("multi value propery");
      return single.getString();
    }
    public java.io.InputStream getStream()
      throws ValueFormatException, RepositoryException
    {
      throw new RepositoryException("Not supported");
    }
    public long getLong()
      throws ValueFormatException, RepositoryException
    {
      return single.getLong();
    }
    public double getDouble()
      throws ValueFormatException, RepositoryException
    {
      return single.getDouble();
    }
    public java.util.Calendar getDate()
      throws ValueFormatException, RepositoryException
    {
      return single.getDate();
    }
    public boolean getBoolean()
      throws ValueFormatException, RepositoryException
    {
      return single.getBoolean();
    }
    public Node getNode()
      throws ValueFormatException, RepositoryException
    {
      return ServicingNodeImpl.this;
    }
    public long getLength()
      throws ValueFormatException, RepositoryException
    {
      if(multi == null)
        throw new ValueFormatException("multi value property");
      return -1;
    }
    public long[] getLengths()
      throws ValueFormatException, RepositoryException
    {
      if(multi == null)
        throw new ValueFormatException("single value property");
      long[] rtvalue = new long[multi.length];
      for(int i=0; i<multi.length; i++)
        rtvalue[i] = -1;
      return rtvalue;
    }
    public PropertyDefinition getDefinition()
      throws RepositoryException
    {
      return propertyDefintion;
    }
    public int getType()
      throws RepositoryException
    {
      return type;
    }

    public String getPath()
      throws RepositoryException
    {
      return ServicingNodeImpl.this.getPath() + "/@" + getName();
    }
    public String getName()
      throws RepositoryException
    {
      return name;
    }
    public Item getAncestor(int depth)
      throws ItemNotFoundException, AccessDeniedException, RepositoryException
    {
      return ServicingNodeImpl.this.getAncestor(depth - 1);
    }
    public Node getParent()
      throws ItemNotFoundException, AccessDeniedException, RepositoryException
    {
      return ServicingNodeImpl.this;
    }
    public int getDepth()
      throws RepositoryException
    {
      return ServicingNodeImpl.this.getDepth() + 1;
    }
    public Session getSession()
      throws RepositoryException
    {
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
    public void save()
      throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException,
             ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException
    {
    }
    public void refresh(boolean keepChanges)
      throws InvalidItemStateException, RepositoryException
    {
    }
    public void remove()
      throws VersionException, LockException, ConstraintViolationException, RepositoryException
    {
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
      if(rtValue == null) {
        try {
          rtValue = getNode((String) entry.getKey());
        } catch(RepositoryException ex) {
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
      while(skipNum > 0) {
        ++position;
        iter.next();
      }
    }
    public long getSize() {
      int count = 0;
      for(Iterator i = children.values().iterator(); i.hasNext(); i.next())
        ++count;
      return count;
    }
    public long getPosition() {
      return position;
    }
  }
  class PropertyIteratorImpl implements PropertyIterator {
    Iterator iter;
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
      return (Property) iter.next();
    }
    public void remove() {
      throw new UnsupportedOperationException();
    }
    public void skip(long skipNum) {
      while(skipNum > 0) {
        ++position;
        iter.next();
      }
    }
    public long getSize() {
      int count = 0;
      for(Iterator i = children.values().iterator(); i.hasNext(); i.next())
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
  public String getName() throws RepositoryException {
    if(node == null) {
      return name;
    } else
      return super.getName();
  }
  public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
    if(isVirtual)
      return session.getRootNode().getNode(path); // FIXME
    else
      return super.getParent();
  }
  public String getPath() throws RepositoryException {
    return path;
  }
  public String getChildPath(String name) throws RepositoryException {
    return getPath() + "/" + name;
  }
  public Session getSession() throws RepositoryException {
    if(node == null) {
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
    public Node addNode(String name) throws ItemExistsException,
            PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        Node child = node.addNode(name);
        return factory.getNodeDecorator(session, child, getChildPath(name), node.getDepth());
    }

    /**
     * @inheritDoc
     */
    public Node addNode(String name, String type) throws ItemExistsException,
            PathNotFoundException, NoSuchNodeTypeException, LockException,
            VersionException, ConstraintViolationException, RepositoryException {
        Node child = node.addNode(name, type);
        return factory.getNodeDecorator(session, child, getChildPath(name), node.getDepth());
    }

    /**
     * @inheritDoc
     */
    public void orderBefore(String srcChildRelPath, String destChildRelPath)
            throws UnsupportedRepositoryOperationException, VersionException,
            ConstraintViolationException, ItemNotFoundException,
            LockException, RepositoryException {
        node.orderBefore(srcChildRelPath, destChildRelPath);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, value));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value value, int type)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, value));
        }
        Property prop = node.setProperty(name, value, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value[] values)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, values));
        }
        Property prop = node.setProperty(name, values);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, values));
        }
        Property prop = node.setProperty(name, values, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String[] values)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, values));
        }
        Property prop = node.setProperty(name, values);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, values));
        }
        Property prop = node.setProperty(name, values, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, String value, int type)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, value));
        }
        Property prop = node.setProperty(name, value, type);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, InputStream value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, boolean value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, double value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, long value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Calendar value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
        }
        Property prop = node.setProperty(name, value);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Property setProperty(String name, Node value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if(node == null) {
            return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value.getUUID())));
        }
        Property prop = node.setProperty(name, ServicingNodeImpl.unwrap(value));
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
    if(isNodeType("hippo:facetsearch")) {
      if(relPath.equals("resultset")) {
        ServicingNodeImpl child = new ServicingNodeImpl(factory, session, "hippo:facetresult", getChildPath(relPath), relPath, depth+1);
        String searchquery = null;
        Value[] searchClauses = new Value[0];
        try {
          searchClauses = getProperty("hippo:search").getValues();
        } catch(PathNotFoundException ex) {
          // safe to ignore
        }
        for(int i=0; i<searchClauses.length; i++) {
          if(searchquery != null)
            searchquery += ",";
          else
            searchquery = "";
          searchquery += searchClauses[i].getString();
        }
        if(searchquery != null)
          searchquery = "[" + searchquery + "]";
        else
          searchquery = "";
        Workspace workspace = getSession().getWorkspace();
        QueryManager qmngr = workspace.getQueryManager();
        Query query = qmngr.createQuery(getProperty("hippo:docbase").getString()+"//node()"+searchquery, Query.XPATH);
        QueryResult qresult = query.execute();
        int count = 0;
        for(NodeIterator iter=qresult.getNodes(); iter.hasNext(); count++) {
          Node node = iter.nextNode();
          child.addNode(node.getName(), node);
        }
        //child.setProperty("resultCount", count);
        return child;
      } else {
        ServicingNodeImpl child = new ServicingNodeImpl(factory, session, "hippo:facetsearch", getChildPath(relPath), relPath, depth+1);
        Value[] facets = getProperty("hippo:facets").getValues();
        Value[] newFacets = new Value[Math.max(0,facets.length-1)];
        if(facets.length > 0)
          System.arraycopy(facets,1,newFacets,0,facets.length-1);
        Value[] search = new Value[0];
        try {
          search = getProperty("hippo:search").getValues();
        } catch(PathNotFoundException ex) {
          // safe to ignore
        }
        Value[] newSearch;
        if(facets.length > 0) {
          newSearch = new Value[search.length+1];
          System.arraycopy(search,0,newSearch,0,search.length);
          newSearch[search.length] = session.getValueFactory().createValue("@" + facets[0].getString() + "='" + relPath + "'");
        } else
          newSearch = (Value[]) search.clone();
        child.setProperty("hippo:docbase",getProperty("hippo:docbase").getString());
        child.setProperty("hippo:facets",newFacets);
        child.setProperty("hippo:search",newSearch);
        return child;
      }
    } else {
      try {
        Node n = node.getNode(relPath);
        return factory.getNodeDecorator(session, n, getChildPath(relPath), getDepth()+1);
      } catch(PathNotFoundException ex) {
        SessionImpl session = (SessionImpl) this.session;
        try {
          Path p = session.getQPath(relPath);
          Path.PathElement[] elements = p.getElements();
          Node node = this;
          if(elements.length < 2)
            throw ex;
          for(int i=0; i<elements.length; i++) {
            node = node.getNode(elements[i].getName().getLocalName());
          }
          if(!(node instanceof ServicingNodeImpl))
            node = new ServicingNodeImpl(factory, session, node, getChildPath(relPath), getDepth()+1);
          return node;
        } catch(NameException ex2) {
          throw ex;
        }
      }
    }
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes() throws RepositoryException {
      if(children != null) {
        if(isNodeType("hippo:facetsearch"))
          instantiate();
        return this . new NodeIteratorImpl();
      } else
        return new DecoratingNodeIterator(factory, session, node.getNodes(), this);
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes(String namePattern)
            throws RepositoryException {
        return new DecoratingNodeIterator(factory, session, node.getNodes(namePattern));
    }

    /**
     * @inheritDoc
     */
    public Property getProperty(String relPath)
            throws PathNotFoundException, RepositoryException {
        if(node == null) {
            return (Property) properties.get(relPath);
        }
        Property prop = node.getProperty(relPath);
        return factory.getPropertyDecorator(session, prop);
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getProperties() throws RepositoryException {
        if(node == null) {
            return this . new PropertyIteratorImpl();
        }
        return new DecoratingPropertyIterator(factory, session, node.getProperties());
    }

    /**
     * @inheritDoc
     */
    public PropertyIterator getProperties(String namePattern)
            throws RepositoryException {
        return new DecoratingPropertyIterator(factory, session, node.getProperties(namePattern));
    }

    /**
     * @inheritDoc
     */
    public Item getPrimaryItem() throws ItemNotFoundException,
            RepositoryException {
        return factory.getItemDecorator(session, node.getPrimaryItem());
    }

    /**
     * @inheritDoc
     */
    public String getUUID() throws UnsupportedRepositoryOperationException,
            RepositoryException {
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
        if(node == null) {
            return children.containsKey(relPath);
        }
        return node.hasNode(relPath);
    }

    /**
     * @inheritDoc
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        if(node == null) {
            return properties.containsKey(relPath);
        }
        return node.hasProperty(relPath);
    }

    /**
     * @inheritDoc
     */
    public boolean hasNodes() throws RepositoryException {
        if(node == null) {
            return true; // FIXME return !children.isEmpty();
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
        if(node == null) {
            return primaryNodeTypeName.equals(nodeTypeName);
        }
        return node.isNodeType(nodeTypeName);
    }

    /**
     * @inheritDoc
     */
    public void addMixin(String mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        node.addMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public void removeMixin(String mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        node.removeMixin(mixinName);
    }

    /**
     * @inheritDoc
     */
    public boolean canAddMixin(String mixinName)
            throws NoSuchNodeTypeException, RepositoryException {
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
    public Version checkin() throws VersionException,
            UnsupportedRepositoryOperationException, InvalidItemStateException,
            LockException, RepositoryException {
        Version version = node.checkin();
        return factory.getVersionDecorator(session, version);
    }

    /**
     * @inheritDoc
     */
    public void checkout() throws UnsupportedRepositoryOperationException,
            LockException, RepositoryException {
        node.checkout();
    }

    /**
     * @inheritDoc
     */
    public void doneMerge(Version version)
            throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        node.doneMerge(VersionDecorator.unwrap(version));
    }

    /**
     * @inheritDoc
     */
    public void cancelMerge(Version version)
            throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        node.cancelMerge(VersionDecorator.unwrap(version));
    }

    /**
     * @inheritDoc
     */
    public void update(String srcWorkspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException,
            LockException, InvalidItemStateException, RepositoryException {
        node.update(srcWorkspaceName);
    }

    /**
     * @inheritDoc
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort)
            throws NoSuchWorkspaceException, AccessDeniedException,
            MergeException, LockException, InvalidItemStateException,
            RepositoryException {
        NodeIterator nodes = node.merge(srcWorkspace, bestEffort);
        return new DecoratingNodeIterator(factory, session, nodes);
    }

    /**
     * @inheritDoc
     */
    public String getCorrespondingNodePath(String workspaceName)
            throws ItemNotFoundException, NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException {
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
    public void restore(String versionName, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        node.restore(versionName, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            RepositoryException {
        node.restore(VersionDecorator.unwrap(version), removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restore(Version version,
                        String relPath,
                        boolean removeExisting)
            throws PathNotFoundException, ItemExistsException,
            VersionException, ConstraintViolationException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        node.restore(VersionDecorator.unwrap(version), relPath, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public void restoreByLabel(String versionLabel, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        node.restoreByLabel(versionLabel, removeExisting);
    }

    /**
     * @inheritDoc
     */
    public VersionHistory getVersionHistory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        VersionHistory hist = node.getVersionHistory();
        return factory.getVersionHistoryDecorator(session, hist);
    }

    /**
     * @inheritDoc
     */
    public Version getBaseVersion()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return factory.getVersionDecorator(session, node.getBaseVersion());
    }

    /**
     * @inheritDoc
     */
    public Lock lock(boolean isDeep, boolean isSessionScoped)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        Lock lock = node.lock(isDeep, isSessionScoped);
        return factory.getLockDecorator(session, lock);
    }

    /**
     * @inheritDoc
     */
    public Lock getLock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, RepositoryException {
        Lock lock = node.getLock();
        return factory.getLockDecorator(session, lock);
    }

    /**
     * @inheritDoc
     */
    public void unlock() throws UnsupportedRepositoryOperationException,
            LockException, AccessDeniedException, InvalidItemStateException,
            RepositoryException {
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
        return ((ServicingWorkspaceImpl)getSession().getWorkspace()).getServicesManager().getService(this);
    }

}
