/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.mock;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.onehippo.repository.util.GlobCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock version of a {@link Node}. Limitations:
 * <ul>
 *     <li>Only string properties can be retrieved and modified</li>
 *     <li>Child nodes can only be retrieved by their name, but not added by name</li>
 *     <li>Child nodes cannot be reordered</li>
 *     <li>Relative paths and patterns are not supported</li>
 *     <li>Saving changes is ignored</li>
 *     <li>Only primary node types are supported, without any inheritance (i.e. a type is of another type iff both
 *         types have the exact same name)</li>
 * </ul>
 * All methods that are not implemented throw an {@link UnsupportedOperationException}.
 */
public class MockNode extends MockItem implements Node {

    private static Logger log = LoggerFactory.getLogger(MockNode.class);

    static final String NAMESPACE_JCR_SV = "http://www.jcp.org/jcr/sv/1.0";
    static final String ROOT_IDENTIFIER = "cafebabe-cafe-babe-cafe-babecafebabe";

    private static boolean defaultSameNameSiblingSupported = true;
    private boolean sameNameSiblingSupported = defaultSameNameSiblingSupported;

    private MockNodeType primaryType;
    private String identifier;
    private final Map<String, MockProperty> properties;
    private final Map<String, List<MockNode>> children;
    private String primaryItemName;

    public MockNode(String name) {
        this(name, null);
    }

    public MockNode(String name, String primaryTypeName) {
        super(name);

        this.identifier = UUID.randomUUID().toString();
        this.properties = new HashMap<String, MockProperty>();
        this.children = new HashMap<String, List<MockNode>>();
        this.primaryItemName = null;

        if (primaryTypeName != null) {
            setPrimaryType(primaryTypeName);
        }
    }

    public static MockNode root() throws RepositoryException {
        MockNode root = new MockNode("");
        root.setPrimaryType("rep:root");
        return root;
    }

    public static boolean isDefaultSameNameSiblingSupported() {
        return defaultSameNameSiblingSupported;
    }

    public static void setDefaultSameNameSiblingSupported(boolean sameNameSiblingSupported) {
        defaultSameNameSiblingSupported = sameNameSiblingSupported;
    }

    public boolean isSameNameSiblingSupported() {
        return sameNameSiblingSupported;
    }

    public void setSameNameSiblingSupported(boolean sameNameSiblingSupported) {
        this.sameNameSiblingSupported = sameNameSiblingSupported;
    }

    public void addNode(MockNode child) throws ConstraintViolationException {
        child.setParent(this);
        String childName = child.getName();
        List<MockNode> childList = children.get(childName);

        if (childList == null) {
            childList = new LinkedList<MockNode>();
            childList.add(child);
            children.put(childName, childList);
        } else {
            if (childList.isEmpty()) {
                childList.add(child);
            } else {
                if (isSameNameSiblingSupported()) {
                    childList.add(child);
                } else {
                    throw new ConstraintViolationException("Cannot add node '" + childName + "': same name sibling support is turned off.");
                }
            }
        }
    }

    @Override
    public Node addNode(final String relPath, final String primaryNodeTypeName) throws PathNotFoundException, ConstraintViolationException {
        return addMockNode(relPath, primaryNodeTypeName);
    }

    public MockNode addMockNode(final String relPath, final String primaryNodeTypeName) throws PathNotFoundException, ConstraintViolationException {
        final String[] pathElements = relPath.split("/");
        MockNode parent = this;

        for (int i = 0; i < pathElements.length - 1; i++) {
            parent = parent.getMockNode(pathElements[i]);
        }

        final MockNode child = new MockNode(pathElements[pathElements.length - 1]);
        child.setPrimaryType(primaryNodeTypeName);
        parent.addNode(child);

        return child;
    }

    @Override
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return primaryType;
    }

    @Override
    public void setPrimaryType(final String nodeTypeName) {
        this.primaryType = new MockNodeType(nodeTypeName);
        this.primaryType.setPrimaryItemName(primaryItemName);
    }

    @Override
    public boolean isNode() {
        return true;
    }

    void removeProperty(String name) {
        MockProperty removed = properties.remove(name);
        if (removed != null) {
            removed.setParent(null);
        }
    }

    @Override
    public void remove() {
        final MockNode parent = getMockParent();
        if (parent != null) {
            List<MockNode> childList = parent.children.get(getName());
            if (childList != null) {
                childList.remove(this);
            }
        }
        setParent(null);
    }

    @Override
    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    @Override
    public Property setProperty(final String name, final Value value) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, value.getType());
        p.setValue(value);
        return p;
    }

    @Override
    public Property setProperty(final String name, final Value[] values) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, values[0].getType());
        p.setValue(values);
        return p;
    }

    @Override
    public Property setProperty(final String name, final String value) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, PropertyType.STRING);
        p.setValue(value);
        return p;
    }

    @Override
    public Property setProperty(final String name, final String value, final int type) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, type);
        p.setValue(value);
        return p;
    }

    @Override
    public Property setProperty(final String name, final String[] values) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, PropertyType.STRING);
        p.setValue(values);
        return p;
    }

    @Override
    public Property setProperty(final String name, final String[] values, final int type) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, type);
        p.setValue(values);
        return p;
    }

    @Override
    public Property getProperty(final String relPath) throws PathNotFoundException {
        checkRelativePathIsName(relPath);
        if (!properties.containsKey(relPath)) {
            throw new PathNotFoundException("Path not found: " + relPath);
        }
        return properties.get(relPath);
    }

    private MockProperty getPropertyOrAddNew(final String name, final int type) {
        MockProperty property = properties.get(name);
        if (property == null) {
            property = new MockProperty(name, type);
            property.setParent(this);
            properties.put(name, property);
        }
        return property;
    }

    @Override
    public boolean hasProperty(final String relPath) {
        checkRelativePathIsName(relPath);
        return properties.containsKey(relPath);
    }

    private void checkRelativePathIsName(final String relPath) {
        if (relPath.contains("/")) {
            throw new UnsupportedOperationException("MockNode does not support relative path '" + relPath + "', only names");
        }
    }

    @Override
    public PropertyIterator getProperties() {
        return new MockPropertyIterator(properties.values());
    }

    @Override
    public boolean hasNode(final String relPath) {
        checkRelativePathIsName(relPath);
        return children.containsKey(relPath);
    }

    @Override
    public Node getNode(final String relPath) throws PathNotFoundException {
        return getMockNode(relPath);
    }

    MockNode getMockNode(final String relPath) throws PathNotFoundException {
        checkRelativePathIsName(relPath);
        if (!children.containsKey(relPath)) {
            throw new PathNotFoundException("Node does not exist: '" + relPath + "'");
        }
        List<MockNode> childList = children.get(relPath);
        if (!childList.isEmpty()) {
            return childList.get(0);
        }
        throw new PathNotFoundException("Node does not exist: '" + relPath + "'");
    }

    @Override
    public NodeIterator getNodes() {
        List<MockNode> childrenCopy = new LinkedList<MockNode>();
        for (List<MockNode> childList : children.values()) {
            childrenCopy.addAll(childList);
        }
        return new MockNodeIterator(childrenCopy);
    }

    @Override
    public String getIdentifier() {
        return isRootNode() ? ROOT_IDENTIFIER : identifier;
    }

    @Override
    public boolean isNodeType(final String nodeTypeName) {
        return primaryType != null && primaryType.isNodeType(nodeTypeName);
    }

    @Override
    public Item getPrimaryItem() throws ItemNotFoundException {
        String path = null;
        try {
            path = getPath();
        } catch (RepositoryException e) {
            log.error("Error when getting paths.", e);
        }
        if (primaryItemName == null) {
            throw new ItemNotFoundException("MockNode '" + path + "' does not have a primary item defined. "
                    + "Use #setPrimaryItemName to define the name of the primary item.");
        }
        try {
            return getNode(primaryItemName);
        } catch (PathNotFoundException e) {
            // node does not exist, maybe it is a property?
        }
        try {
            return getProperty(primaryItemName);
        } catch (PathNotFoundException e) {
            // property does not exist either
        }
        throw new ItemNotFoundException("Primary item '" + primaryItemName + "' does not exist in MockNode '" + path + "'");
    }

    public void setPrimaryItemName(String name) {
        this.primaryItemName = name;
        if (this.primaryType != null) {
            this.primaryType.setPrimaryItemName(primaryItemName);
        }
    }

    @Override
    public String toString() {
        String path = null;
        try {
            path = getPath();
        } catch (RepositoryException e) {
            log.error("Error when getting node path.", e);
        }
        return "MockNode[path=" + path + "]";
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public Node addNode(final String relPath)  {
        throw new UnsupportedOperationException("Try using #addNode(String relPath, String primaryNodeTypeName) instead");
    }

    @Override
    public void orderBefore(final String srcChildRelPath, final String destChildRelPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final Value value, final int type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final Value[] values, final int type)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final InputStream value)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final Binary value)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final boolean value)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final double value)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final BigDecimal value)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final long value)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final Calendar value)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final Node value)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator getNodes(final String namePattern) {
        String [] nameGlobs = namePattern.split("\\|");
        for (int i = 0; i < nameGlobs.length; i++) {
            nameGlobs[i] = nameGlobs[i].trim();
        }
        return getNodes(nameGlobs);
    }

    @Override
    public NodeIterator getNodes(final String[] nameGlobs) {
        List<MockNode> childrenCopy = new LinkedList<MockNode>();

        for (String nameGlob : nameGlobs) {
            GlobCompiler compiler = new GlobCompiler();
            Pattern pattern = compiler.compile(nameGlob);

            for (List<MockNode> childList : children.values()) {
                for (MockNode child : childList) {
                    Matcher m = pattern.matcher(child.getName());

                    if (m.matches()) {
                        childrenCopy.add(child);
                    }
                }
            }
        }

        return new MockNodeIterator(childrenCopy);
    }

    @Override
    public PropertyIterator getProperties(final String namePattern) {
        String [] nameGlobs = namePattern.split("\\|");
        for (int i = 0; i < nameGlobs.length; i++) {
            nameGlobs[i] = nameGlobs[i].trim();
        }
        return getProperties(nameGlobs);
    }

    @Override
    public PropertyIterator getProperties(final String[] nameGlobs) {
        List<MockProperty> propsCopy = new LinkedList<MockProperty>();

        for (String nameGlob : nameGlobs) {
            GlobCompiler compiler = new GlobCompiler();
            Pattern pattern = compiler.compile(nameGlob);

            for (MockProperty prop : properties.values()) {
                Matcher m = pattern.matcher(prop.getName());

                if (m.matches()) {
                    propsCopy.add(prop);
                }
            }
        }

        return new MockPropertyIterator(propsCopy);
    }

    @Override
    public String getUUID() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIndex() throws RepositoryException {
        if (!isRootNode()) {
            List<MockNode> childList = ((MockNode) getParent()).children.get(getName());
            if (childList != null) {
                int offset = childList.indexOf(this);
                if (offset != -1) {
                    return offset + 1;
                }
            }
        }
        return 1;
    }

    @Override
    public PropertyIterator getReferences() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getReferences(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getWeakReferences() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyIterator getWeakReferences(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNodes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeType[] getMixinNodeTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMixin(final String mixinName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeMixin(final String mixinName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canAddMixin(final String mixinName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeDefinition getDefinition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version checkin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void doneMerge(final Version version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelMerge(final Version version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(final String srcWorkspace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator merge(final String srcWorkspace, final boolean bestEffort) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCorrespondingNodePath(final String workspaceName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator getSharedSet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSharedSet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeShare() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCheckedOut() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final String versionName, final boolean removeExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version version, final boolean removeExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version version, final String relPath, final boolean removeExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restoreByLabel(final String versionLabel, final boolean removeExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VersionHistory getVersionHistory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getBaseVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock lock(final boolean isDeep, final boolean isSessionScoped) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Lock getLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean holdsLock() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLocked() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void followLifecycleTransition(final String transition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getAllowedLifecycleTransistions() {
        throw new UnsupportedOperationException();
    }
}
