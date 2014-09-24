/*
 *  Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.Localized;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock version of a {@link Node}.
 * 
 * <P>
 * Limitations:
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
public class MockNode extends MockItem implements HippoNode {

    private static Logger log = LoggerFactory.getLogger(MockNode.class);

    static final String NAMESPACE_JCR_SV = "http://www.jcp.org/jcr/sv/1.0";
    static final String ROOT_IDENTIFIER = "cafebabe-cafe-babe-cafe-babecafebabe";

    private static final Pattern PATH_NAME_WITH_INDEX_PATTERN = Pattern.compile("^([^\\[\\]]+)(\\[(\\d+)\\])?$");

    private static boolean defaultSameNameSiblingSupported = true;
    private boolean sameNameSiblingSupported = defaultSameNameSiblingSupported;

    private MockNodeType primaryType;
    private String identifier;
    private final Map<String, MockProperty> properties;
    private final Map<String, List<MockNode>> children;
    private final Set<String> mixins;
    private String primaryItemName;
    private boolean checkedOut = true;

    public MockNode(String name) {
        this(name, null);
    }

    public MockNode(String name, String primaryTypeName) {
        super(name);

        this.identifier = UUID.randomUUID().toString();
        this.properties = new HashMap<>();
        this.children = new LinkedHashMap<>();
        this.mixins = new HashSet<>();
        this.primaryItemName = null;

        if (primaryTypeName != null) {
            try {
                setPrimaryType(primaryTypeName);
            } catch (RepositoryException e) {
                throw new IllegalStateException("Cannot create MockNode", e);
            }
        }
    }

    public MockNode(String name, String primaryTypeName, final MockNode original) throws RepositoryException {
        this(name, primaryTypeName);
        copyProperties(original);
        copyChildren(original);
        copyMixins(original);
    }

    public MockNode(MockNode original) throws RepositoryException {
        this(original.getName(), original.primaryItemName, original);
    }

    private void copyProperties(MockNode original) throws RepositoryException {
        for (Map.Entry<String, MockProperty> propEntry : original.properties.entrySet()) {
            final MockProperty property = new MockProperty(propEntry.getValue());
            property.setParent(this);
            properties.put(propEntry.getKey(), property);
        }
    }

    private void copyChildren(final MockNode original) throws RepositoryException {
        for (List<MockNode> sameNameSiblings : original.children.values()) {
            for (MockNode child : sameNameSiblings) {
                addNode(new MockNode(child));
            }
        }
    }

    private void copyMixins(final MockNode original) {
        this.mixins.addAll(original.mixins);
    }

    public static MockNode root() {
        return new MockNode("", "rep:root");
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

    public void addNode(MockNode child) throws RepositoryException {
        validateCheckedOut();

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
    public MockNode addNode(final String relPath, final String primaryNodeTypeName) throws RepositoryException {
        validateCheckedOut();

        final String[] pathElements = relPath.split("/");
        MockNode parent = this;

        for (int i = 0; i < pathElements.length - 1; i++) {
            parent = parent.getNode(pathElements[i]);
        }

        final MockNode child = new MockNode(pathElements[pathElements.length - 1]);
        child.setPrimaryType(primaryNodeTypeName);
        parent.addNode(child);

        return child;
    }

    /**
     * @deprecated use {@link #addNode(String, String)} instead.
     */
    @Deprecated
    public MockNode addMockNode(final String relPath, final String primaryNodeTypeName) throws RepositoryException {
        return addNode(relPath, primaryNodeTypeName);
    }

    @Override
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return primaryType;
    }

    @Override
    public void setPrimaryType(final String nodeTypeName) throws RepositoryException {
        validateCheckedOut();

        this.primaryType = new MockNodeType(nodeTypeName);
        this.primaryType.setPrimaryItemName(primaryItemName);
    }

    @Override
    public boolean isNode() {
        return true;
    }

    void removeProperty(String name) throws RepositoryException {
        validateCheckedOut();
        MockProperty removed = properties.remove(name);
        if (removed != null) {
            removed.setParent(null);
        }
    }

    @Override
    public void remove() throws RepositoryException {
        validateCheckedOut();
        final MockNode parent = getParentOrNull();
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
    public MockProperty getProperty(String relPath) throws PathNotFoundException {
        if (relPath == null) {
            throw new IllegalArgumentException("Non-null relative path is required.");
        }

        String nodeRelPath = null;
        String propName = null;
        int lastOffset = relPath.lastIndexOf('/');

        if (lastOffset == relPath.length() - 1) {
            relPath = relPath.substring(0, relPath.length() - 1);
            lastOffset = relPath.lastIndexOf('/');
        }

        if (lastOffset == -1) {
            propName = relPath;
        } else {
            nodeRelPath = relPath.substring(0, lastOffset);
            propName = relPath.substring(lastOffset + 1);
        }

        if (propName == null || "".equals(propName)) {
            throw new IllegalStateException("Failed to resolve the property name from the relative path: '" + relPath + "'.");
        }

        if (nodeRelPath == null || "".equals(nodeRelPath)) {
            if (!properties.containsKey(relPath)) {
                throw new PathNotFoundException("Path not found: " + relPath);
            }

            return properties.get(relPath);
        } else {
            return getNode(nodeRelPath).getProperty(propName);
        }
    }

    private MockProperty getPropertyOrAddNew(final String name, final int type) throws RepositoryException {
        validateCheckedOut();
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
        try {
            return (getProperty(relPath) != null);
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    @Override
    public PropertyIterator getProperties() {
        return new MockPropertyIterator(properties.values());
    }

    @Override
    public boolean hasNode(final String relPath) {
        try {
            return (getNode(relPath) != null);
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    @Override
    public MockNode getNode(final String relPath) throws PathNotFoundException {
        if (relPath == null) {
            throw new IllegalArgumentException("Non-null relative path is required.");
        }

        int offset = relPath.indexOf('/');

        if (offset == 0) {
            throw new IllegalArgumentException("Not a relative path: " + relPath);
        }

        String nodeName = null;
        int nodeIndex = 1;
        String subRelPath = null;

        if (offset == -1) {
            nodeName = relPath;
        } else if (offset == relPath.length() - 1) {
            nodeName = relPath.substring(0, relPath.length() - 1);
        } else {
            nodeName = relPath.substring(0, offset);
            subRelPath = relPath.substring(offset + 1);
        }

        Matcher m = PATH_NAME_WITH_INDEX_PATTERN.matcher(nodeName);

        if (!m.matches()) {
            throw new IllegalStateException("Node name not matched against the pattern: " + PATH_NAME_WITH_INDEX_PATTERN.pattern());
        }

        nodeName = m.group(1);
        String nodeIndexValue = m.group(3);

        if (nodeIndexValue != null) {
            nodeIndex = Integer.parseInt(nodeIndexValue);
        }

        if (!children.containsKey(nodeName)) {
            throw new PathNotFoundException("Node does not exist: '" + relPath + "'");
        }

        List<MockNode> childList = children.get(nodeName);

        if (childList.isEmpty() || childList.size() < nodeIndex) {
            throw new PathNotFoundException("Node does not exist: '" + relPath + "'");
        }

        if (subRelPath == null) {
            return childList.get(nodeIndex - 1);
        }

        MockNode child = childList.get(nodeIndex - 1);
        return child.getNode(subRelPath);
    }

    /**
     * @deprecated use {@link #getNode(String)} instead.
     */
    @Deprecated
    MockNode getMockNode(final String relPath) throws PathNotFoundException {
        return getNode(relPath);
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
        return primaryType != null && primaryType.isNodeType(nodeTypeName) || mixins.contains(nodeTypeName);
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

    @Override
    public Node addNode(final String relPath)  {
        throw new UnsupportedOperationException("Try using #addNode(String relPath, String primaryNodeTypeName) instead");
    }

    @Override
    public void orderBefore(final String srcChildRelPath, final String destChildRelPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final Value value, final int type) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, type);
        p.setValue(value);
        return p;
    }

    @Override
    public Property setProperty(final String name, final Value[] values, final int type) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, type);
        p.setValue(values);
        return p;
    }

    @Override
    public Property setProperty(final String name, final InputStream value)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property setProperty(final String name, final Binary value) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, PropertyType.BINARY);
        p.setValue(value);
        return p;
    }

    @Override
    public Property setProperty(final String name, final boolean value) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, PropertyType.BOOLEAN);
        p.setValue(value);
        return p;
    }

    @Override
    public Property setProperty(final String name, final double value) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, PropertyType.DOUBLE);
        p.setValue(value);
        return p;
    }

    @Override
    public Property setProperty(final String name, final BigDecimal value) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, PropertyType.DECIMAL);
        p.setValue(value);
        return p;
    }

    @Override
    public Property setProperty(final String name, final long value) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, PropertyType.LONG);
        p.setValue(value);
        return p;
    }

    @Override
    public Property setProperty(final String name, final Calendar value) throws RepositoryException {
        Property p = getPropertyOrAddNew(name, PropertyType.DATE);
        p.setValue(value);
        return p;
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
        Set<MockNode> childrenCopy = new LinkedHashSet<MockNode>();

        for (String nameGlob : nameGlobs) {
            PathMatcher globMatcher = FileSystems.getDefault().getPathMatcher("glob:" + nameGlob);

            for (List<MockNode> childList : children.values()) {
                for (MockNode child : childList) {
                    Path path = Paths.get(child.getName());

                    if (globMatcher.matches(path)) {
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
        Set<MockProperty> propsCopy = new LinkedHashSet<MockProperty>();

        for (String nameGlob : nameGlobs) {
            PathMatcher globMatcher = FileSystems.getDefault().getPathMatcher("glob:" + nameGlob);

            for (MockProperty prop : properties.values()) {
                Path path = Paths.get(prop.getName());

                if (globMatcher.matches(path)) {
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
    public boolean hasNodes() {
        return !children.isEmpty();
    }

    @Override
    public MockNode getCanonicalNode() throws ItemNotFoundException, RepositoryException {
        return this;
    }

    @Override
    public boolean isVirtual() throws RepositoryException {
        return false;
    }

    @Override
    public boolean recomputeDerivedData() throws RepositoryException {
        return false;
    }

    @Override
    public String getLocalizedName() throws RepositoryException {
        return getName();
    }

    @Override
    public Map<Localized, String> getLocalizedNames() throws RepositoryException {
        return Collections.emptyMap();
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
    public NodeType[] getMixinNodeTypes() {
        NodeType[] nodeTypes = new NodeType[mixins.size()];
        int pos = 0;
        for (String mixin : mixins) {
            final MockNodeType mockNodeType = new MockNodeType(mixin);
            nodeTypes[pos] = mockNodeType;
            pos++;
        }
        return nodeTypes;
    }

    @Override
    public void addMixin(final String mixinName) throws RepositoryException {
        if (mixinName != null) {
            mixins.add(mixinName);

            if (mixinName.equals(JcrConstants.MIX_VERSIONABLE)) {
                checkout();
            }
        }
    }

    @Override
    public void removeMixin(final String mixinName) {
        mixins.remove(mixinName);
    }

    @Override
    public boolean canAddMixin(final String mixinName) {
        return true;
    }

    @Override
    public NodeDefinition getDefinition() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public MockVersion checkin() throws RepositoryException {
        final MockVersionManager versionManager = getSession().getWorkspace().getVersionManager();
        final MockVersion version = versionManager.checkin(getPath());
        return version;
    }

    @Deprecated
    @Override
    public void checkout() throws RepositoryException {
        final MockVersionManager versionManager = getSession().getWorkspace().getVersionManager();
        versionManager.checkout(getPath());
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
    public boolean isCheckedOut() throws RepositoryException {
        return checkedOut;
    }

    void setCheckedOut(final boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    private void validateCheckedOut() throws RepositoryException {
        if (!isCheckedOut()) {
            throw new VersionException("Node " + getPath() + " is checked in");
        }
        if (!isRootNode()) {
            getParentOrNull().validateCheckedOut();
        }
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

    @Override
    public String getLocalizedName(final Localized localized) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator pendingChanges(final String nodeType, final boolean prune) throws NamespaceException, NoSuchNodeTypeException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator pendingChanges(final String nodeType) throws NamespaceException, NoSuchNodeTypeException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeIterator pendingChanges() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}
