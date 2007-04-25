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
package org.hippocms.repository.jr.embedded;

import javax.jcr.*;
import javax.jcr.lock.*;
import javax.jcr.version.*;
import javax.jcr.nodetype.*;
import javax.jcr.query.*;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.NoSuchElementException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.ItemId;

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.core.HierarchyManagerImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.name.NameException;

class VirtualNodeImpl
  implements Node
{
  protected Session session;
  protected boolean isVirtual;
  protected boolean isQuery;
  protected Node actual;
  protected String primaryNodeTypeName;
  protected String name;
  protected String path;
  protected int depth;

  protected Map children = null;
  protected Map properties = null;
  protected VirtualNodeImpl(Node actual, String path, int depth) throws RepositoryException {
    session = actual.getSession();
    isVirtual   = false;
    this.actual = actual;
    this.path   = path;
    this.depth  = depth;
    if(actual.isNodeType("hippo:facetsearch")) {
      children = new HashMap();
      properties = new HashMap();
    }
  }
  protected VirtualNodeImpl(Session session, String nodeTypeName, String path, String name, int depth) throws RepositoryException {
    this.session = session;
    primaryNodeTypeName = nodeTypeName;
    isVirtual   = true;
    this.actual = null;
    this.path   = path;
    this.name   = name;
    this.depth  = depth;
    children = new HashMap();
    properties = new HashMap();
  }
  protected void addNode(String name, Node node) {
    children.put(name, node);
  }
  boolean instantiated = false;
  private void instantiate() throws RepositoryException {
    if(instantiated)
      return;
    instantiated = true;
    /*
     * The XPath defined for JCR-170 lacks the possibility to select distinct nodes.
     */
    addNode("resultset", (Node)null);
    QueryManager qmngr = session.getWorkspace().getQueryManager();
    Node node = (actual != null ? actual : this);
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
      return VirtualNodeImpl.this;
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
      return VirtualNodeImpl.this.getPath() + "/@" + getName();
    }
    public String getName()
      throws RepositoryException
    {
      return name;
    }
    public Item getAncestor(int depth)
      throws ItemNotFoundException, AccessDeniedException, RepositoryException
    {
      return VirtualNodeImpl.this.getAncestor(depth - 1);
    }
    public Node getParent()
      throws ItemNotFoundException, AccessDeniedException, RepositoryException
    {
      return VirtualNodeImpl.this;
    }
    public int getDepth()
      throws RepositoryException
    {
      return VirtualNodeImpl.this.getDepth() + 1;
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
  class WrappingNodeIteratorImpl implements NodeIterator {
    NodeIterator iter;
    WrappingNodeIteratorImpl(NodeIterator superiter) {
      iter = superiter;
    }
    public boolean hasNext() {
      return iter.hasNext();
    }
    public Object next() {
      try {
        Node node = iter.nextNode();
        return new VirtualNodeImpl(node, getChildPath(node.getName()), depth+1);
      } catch(RepositoryException ex) {
        throw new NoSuchElementException();
      }
    }
    public Node nextNode() {
      try {
        Node node = iter.nextNode();
        return new VirtualNodeImpl(node, getChildPath(node.getName()), depth+1);
      } catch(RepositoryException ex) {
        throw new NoSuchElementException();
      }
    }
    public void remove() {
      iter.remove();
    }
    public void skip(long skipNum) {
      iter.skip(skipNum);
    }
    public long getSize() {
      return iter.getSize();
    }
    public long getPosition() {
      return iter.getPosition();
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

  public void accept(ItemVisitor visitor) throws RepositoryException {
    actual.accept(visitor);
  }

  public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
    return actual.getAncestor(depth);
  }
  public int getDepth() throws ItemNotFoundException, RepositoryException {
    return depth;
  }
  public String getName() throws RepositoryException {
    if(actual == null) {
      return name;
    } else
      return actual.getName();
  }
  public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
    if(isVirtual)
      return session.getRootNode().getNode(path); // FIXME
    else
      return actual.getParent();
  }
  public String getPath() throws RepositoryException {
    return path;
  }
  public String getChildPath(String name) throws RepositoryException {
    return getPath() + "/" + name;
  }
  public Session getSession() throws RepositoryException {
    if(actual == null) {
      return session;
    } else
      return actual.getSession();
  }
  public boolean isModified() {
    return actual.isModified();
  }
  public boolean isNew() {
    return actual.isNew();
  }
  public boolean isNode() {
    return actual.isNode();
  }
  public boolean isSame(Item otherItem) throws RepositoryException {
    return actual.isSame(otherItem);
  }
  public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
    actual.refresh(keepChanges);
  }
  public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
    actual.remove();
  }
  public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    actual.save();
  }

  public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, ReferentialIntegrityException, RepositoryException {
    actual.addMixin(mixinName);
  }
  public Node addNode(String relPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
    Node node = actual.addNode(relPath);
    return new VirtualNodeImpl(node, getChildPath(relPath), node.getDepth());
  }
  public Node addNode(String relPath, String primaryNodeTypeName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, RepositoryException {
    Node node = actual.addNode(relPath, primaryNodeTypeName);
    return new VirtualNodeImpl(node, getChildPath(relPath), node.getDepth());
  }
  public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
    return actual.canAddMixin(mixinName);
  }
  public void cancelMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
    actual.cancelMerge(version);
  }
  public Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
    return actual.checkin();
  }
  public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
    actual.checkout();
  }
  public void doneMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
    actual.doneMerge(version);
  }
  public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
    return actual.getBaseVersion();
  }
  public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
    return actual.getCorrespondingNodePath(workspaceName);
  }
  public NodeDefinition getDefinition() throws RepositoryException {
    return actual.getDefinition();
  }
  public int getIndex() throws RepositoryException {
    return actual.getIndex();
  }
  public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
    return actual.getLock();
  }
  public NodeType[] getMixinNodeTypes() throws RepositoryException {
    return actual.getMixinNodeTypes();
  }
  public Node getNode(String relPath) throws PathNotFoundException, RepositoryException  {
    if(isNodeType("hippo:facetsearch")) {
      if(relPath.equals("resultset")) {
        VirtualNodeImpl child = new VirtualNodeImpl(session, "hippo:facetresult", getChildPath(relPath), relPath, depth+1);
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
        VirtualNodeImpl child = new VirtualNodeImpl(session, "hippo:facetsearch", getChildPath(relPath), relPath, depth+1);
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
        Node target = actual.getNode(relPath);
        return new VirtualNodeImpl(target, getChildPath(relPath), getDepth()+1);
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
          if(!(node instanceof VirtualNodeImpl))
            node = new VirtualNodeImpl(node, getChildPath(relPath), getDepth()+1);
          return node;
        } catch(NameException ex2) {
          throw ex;
        }
      }
    }
  }
  public NodeIterator getNodes() throws RepositoryException {
    if(children != null) {
      if(isNodeType("hippo:facetsearch"))
        instantiate();
      return this . new NodeIteratorImpl();
    } else
      return this . new WrappingNodeIteratorImpl(actual.getNodes());
  }
  public NodeIterator getNodes(String namePattern) throws RepositoryException {
    return actual.getNodes(namePattern);
  }
  public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
    return actual.getPrimaryItem();
  }
  public NodeType getPrimaryNodeType() throws RepositoryException {
    return actual.getPrimaryNodeType();
  }
  public PropertyIterator getProperties() throws RepositoryException {
    if(actual == null) {
      return this . new PropertyIteratorImpl();
    } else
      return actual.getProperties();
  }
  public PropertyIterator getProperties(String namePattern) throws RepositoryException {
    return actual.getProperties(namePattern);
  }
  public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
    if(actual == null) {
      return (Property) properties.get(relPath);
    } else
      return actual.getProperty(relPath);
  }
  public PropertyIterator getReferences() throws RepositoryException {
    return actual.getReferences();
  }
  public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
    return actual.getUUID();
  }
  public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
    return actual.getVersionHistory();
  }
  public boolean hasNode(String relPath) throws RepositoryException {
    if(actual == null) {
      return children.containsKey(relPath);
    } else
      return actual.hasNode(relPath);
  }
  public boolean hasNodes() throws RepositoryException {
    if(actual == null) {
      return true;
      //FIXME return !children.isEmpty();
    } else
      return actual.hasNodes();
  }
  public boolean hasProperties() throws RepositoryException {
    return actual.hasProperties();
  }
  public boolean hasProperty(String relPath) throws RepositoryException {
    if(actual == null) {
      return properties.containsKey(relPath);
    } else
      return actual.hasProperty(relPath);
  }
  public boolean holdsLock() throws RepositoryException {
    return actual.holdsLock();
  }
  public boolean isCheckedOut() throws RepositoryException {
    return actual.isCheckedOut();
  }
  public boolean isLocked() throws RepositoryException {
    return actual.isLocked();
  }
  public boolean isNodeType(String nodeTypeName) throws RepositoryException {
    if(actual == null) {
      return primaryNodeTypeName.equals(nodeTypeName);
    } else
      return actual.isNodeType(nodeTypeName);
  }
  public Lock lock(boolean isDeep, boolean isSessionScoped) throws RepositoryException {
    return actual.lock(isDeep, isSessionScoped);
  }
  public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
    return actual.merge(srcWorkspace,bestEffort);
  }
  public void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedOperationException, UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
    actual.orderBefore(srcChildRelPath, destChildRelPath);
  }
  public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
    actual.removeMixin(mixinName);
  }
  public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException, LockException, LockException, UnsupportedRepositoryOperationException, InvalidItemStateException, RepositoryException {
    actual.restore(versionName, removeExisting);
  }
  public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
    actual.restore(version, removeExisting);
  }
  public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException, ItemExistsException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
    actual.restore(version, relPath, removeExisting);
  }
  public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
    actual.restoreByLabel(versionLabel, removeExisting);
  }
  public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
    } else
      return actual.setProperty(name, value);
  }
  public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
    } else
      return actual.setProperty(name, value);
  }
  public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
    } else
      return actual.setProperty(name, value);
  }
  public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
    } else
      return actual.setProperty(name, value);
  }
  public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
    } else
      return actual.setProperty(name, value);
  }
  public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value.getUUID())));
    } else
      return actual.setProperty(name, value);
  }
  public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, session.getValueFactory().createValue(value)));
    } else
      return actual.setProperty(name, value);
  }
  public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, values));
    } else
      return actual.setProperty(name, values);
  }
  public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, values));
    } else
      return actual.setProperty(name, values, type);
  }
  public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, value));
    } else
      return actual.setProperty(name, value, type);
  }
  public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, value));
    } else
      return actual.setProperty(name, value);
  }
  public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, values));
    } else
      return actual.setProperty(name, values);
  }
  public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, values));
    } else
      return actual.setProperty(name, values, type);
  }
  public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    if(actual == null) {
      return (Property) properties.put(name, this . new PropertyImpl(name, value));
    } else
      return actual.setProperty(name, value, type);
  }
  public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
    actual.unlock();
  }
  public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
    actual.update(srcWorkspaceName);
  }
}
