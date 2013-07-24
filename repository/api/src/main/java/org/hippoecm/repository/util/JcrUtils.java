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
package org.hippoecm.repository.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.*;

import javax.jcr.*;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.HippoNode;

/**
 * Some utility methods for writing code against JCR API. This code can be removed when we upgrade to JR 2.6...
 */
public class JcrUtils {

    public static final int ALL_EVENTS = Event.NODE_ADDED | Event.NODE_REMOVED | Event.NODE_MOVED
            | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;

    /**
     * Get the node at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such node exists.
     *
     * @param baseNode existing node that should be the base for the relative path
     * @param relPath relative path to the node to get
     * @return  the node at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such node exists.
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Node getNodeIfExists(Node baseNode, String relPath) throws RepositoryException {
        try {
            return baseNode.getNode(relPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /**
     * Gets the node at <code>absPath</code> or <code>null</code> if no such node exists.
     *
     * @param absPath  the absolute path to the node to return
     * @param session  to use
     * @return  the node at <code>absPath</code> or <code>null</code> if no such node exists.
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Node getNodeIfExists(String absPath, Session session) throws RepositoryException {
        try {
            return session.getNode(absPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /**
     * Gets the node at <code>absPath</code> or <code>null</code> if no such node exists.
     * In case there are more nodes at <code>absPath</code>, the last node is returned.
     *
     * @param absPath the absolute path to the node to return
     * @param session to use
     * @return  the node at <code>absPath</code> or <code>null</code> if no such node exists.
     * @throws RepositoryException
     */
    public static Node getLastNodeIfExists(String absPath, Session session) throws RepositoryException {
        if (absPath.equals("/")) {
            return session.getRootNode();
        }
        final int idx = absPath.lastIndexOf('/');
        final String parentAbsPath = absPath.substring(0, idx);
        final String nodeName = absPath.substring(idx+1);
        final Node parentNode = session.getNode(parentAbsPath);
        final NodeIterator nodes = parentNode.getNodes(nodeName);
        Node result = null;
        while (nodes.hasNext()) {
            result = nodes.nextNode();
        }
        return result;
    }

    /**
     * Returns the string property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the string property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static String getStringProperty(Node baseNode, String relPath, String defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getString();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the long property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the long property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     * @deprecated
     */
    public static long getLongProperty(Node baseNode, String relPath, long defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getLong();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the long property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the long property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Long getLongProperty(Node baseNode, String relPath, Long defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getLong();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the double property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the double property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     * @deprecated
     */
    public static double getDoubleProperty(Node baseNode, String relPath, double defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getDouble();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the double property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the double property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Double getDoubleProperty(Node baseNode, String relPath, Double defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getDouble();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the boolean property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the boolean property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     * @deprecated
     */
    public static boolean getBooleanProperty(Node baseNode, String relPath, boolean defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getBoolean();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the boolean property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the boolean property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Boolean getBooleanProperty(Node baseNode, String relPath, Boolean defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getBoolean();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the date property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the date property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Calendar getDateProperty(Node baseNode, String relPath, Calendar defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getDate();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the decimal property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the decimal property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static BigDecimal getDecimalProperty(Node baseNode, String relPath, BigDecimal defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getDecimal();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the binary property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the binary property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Binary getBinaryProperty(Node baseNode, String relPath, Binary defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getBinary();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the node property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode  existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the node property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Node getNodeProperty(Node baseNode, String relPath, Node defaultValue) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath).getNode();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }


    /**
     * Returns the string property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the string property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static String getStringProperty(Session session, String absPath, String defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getString();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the long property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session  to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the long property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     * @deprecated
     */
    public static long getLongProperty(Session session, String absPath, long defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getLong();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the long property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session  to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the long property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Long getLongProperty(Session session, String absPath, Long defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getLong();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the double property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the double property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     * @deprecated
     */
    public static double getDoubleProperty(Session session, String absPath, double defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getDouble();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the double property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the double property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Double getDoubleProperty(Session session, String absPath, Double defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getDouble();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the boolean property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the boolean property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     * @deprecated
     */
    public static boolean getBooleanProperty(Session session, String absPath, boolean defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getBoolean();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the boolean property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the boolean property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Boolean getBooleanProperty(Session session, String absPath, Boolean defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getBoolean();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the date property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the date property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Calendar getDateProperty(Session session, String absPath, Calendar defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getDate();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the decimal property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the decimal property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static BigDecimal getDecimalProperty(Session session, String absPath, BigDecimal defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getDecimal();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the binary property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the binary property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Binary getBinaryProperty(Session session, String absPath, Binary defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getBinary();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the node property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param session to use
     * @param absPath  absolute path to the property to get
     * @param defaultValue  default value to return when the property does not exist
     * @return  the node property value at <code>absPath</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Node getNodeProperty(Session session, String absPath, Node defaultValue) throws RepositoryException {
        try {
            return session.getProperty(absPath).getNode();
        } catch (PathNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Get the property at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such property exists.
     *
     * @param baseNode existing node that should be the base for the relative path
     * @param relPath relative path to the property to get
     * @return  the property at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such property exists.
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Property getPropertyIfExists(Node baseNode, String relPath) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /**
     * Gets the property at <code>absPath</code> or <code>null</code> if no such property exists.
     *
     * @param absPath  the absolute path to the property to return
     * @param session  to use
     * @return  the property at <code>absPath</code> or <code>null</code> if no such property exists.
     * @throws RepositoryException  in case of exception accessing the Repository
     */
    public static Property getPropertyIfExists(String absPath, Session session) throws RepositoryException {
        try {
            return session.getProperty(absPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /**
     * Copies node at <code>srcAbsPath</code> to <code>destAbsPath</code> as session operation.
     *
     * @param session to use
     * @param srcAbsPath  the absolute path of the source node
     * @param destAbsPath  the absolute path of the resulting copy
     * @throws RepositoryException
     */
    public static void copy(Session session, String srcAbsPath, String destAbsPath) throws RepositoryException {
        if (destAbsPath.equals("/")) {
            throw new IllegalArgumentException("Root cannot be the destination of a copy");
        }

        if (destAbsPath.startsWith(srcAbsPath)) {
            throw new IllegalArgumentException("Destination cannot be child of source node");
        }

        final Node srcNode = session.getNode(srcAbsPath);
        final int idx = destAbsPath.lastIndexOf('/');
        final String parentDestAbsPath = idx == 0 ? destAbsPath : destAbsPath.substring(0, idx);
        final String destNodeName = destAbsPath.substring(idx+1);
        final Node destParentNode = session.getNode(parentDestAbsPath);

        copy(srcNode, destNodeName, destParentNode);
    }

    /**
     * Copies {@link Node} {@code srcNode} to {@code destParentNode} with name {@code sestNodeName}.
     *
     * @param srcNode the node to copy
     * @param destNodeName  the name of the to be newly created node
     * @param destParentNode  the parent of the to be newly created node.
     * @throws RepositoryException
     */
    public static void copy(final Node srcNode, final String destNodeName, final Node destParentNode) throws RepositoryException {
        if (isVirtual(srcNode)) {
            return;
        }
        final Node destNode;
        if (isAutoCreatedNode(srcNode.getName(), destParentNode) && destParentNode.hasNode(srcNode.getName())) {
            destNode = destParentNode.getNode(destNodeName);
        } else {
            destNode = destParentNode.addNode(destNodeName, srcNode.getPrimaryNodeType().getName());
        }

        for (NodeType nodeType : srcNode.getMixinNodeTypes()) {
            destNode.addMixin(nodeType.getName());
        }

        final PropertyIterator properties = srcNode.getProperties();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            if (!property.getDefinition().isProtected()) {
                if (property.isMultiple()) {
                    destNode.setProperty(property.getName(), property.getValues(), property.getType());
                } else {
                    destNode.setProperty(property.getName(), property.getValue());
                }
            }
        }

        final NodeIterator nodes = srcNode.getNodes();
        while (nodes.hasNext()) {
            final Node child = nodes.nextNode();
            copy(child, child.getName(), destNode);
        }
    }

    /**
     * Serialize the given <code>object</code> into a binary JCR value.
     *
     * @param session to use
     * @param object to serialize
     * @return  a binary value containing the serialized object
     * @throws RepositoryException
     */
    public static Value createBinaryValueFromObject(Session session, Object object) throws RepositoryException {
        final ValueFactory valueFactory = session.getValueFactory();
        return valueFactory.createValue(valueFactory.createBinary(new ByteArrayInputStream(objectToBytes(object))));
    }

    /**
     * @return  an empty {@link NodeIterable}
     */
    public static NodeIterable emptyNodeIterable() {
        return new NodeIterable(new NodeIterator() {
            @Override
            public Node nextNode() {
                return null;
            }

            @Override
            public void skip(final long skipNum) {
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public long getPosition() {
                return 0;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Object next() {
                return null;
            }

            @Override
            public void remove() {
            }
        });
    }

    /**
     * Make sure the node is in checked out state.
     * If the node is not in checked out state and is of type <code>mix:versionable</code>
     * this method checks out the node. If it is not <code>mix:versionable</code> and
     * <code>traverseAncestors</code> is <code>true</code> it checks out the versionable ancestor.
     *
     * @param node the node to check
     * @param traverseAncestors whether to check out the versionable ancestor that causes this node to be checked in
     * @throws RepositoryException
     */
    public static void ensureIsCheckedOut(Node node, boolean traverseAncestors) throws RepositoryException {
        if (!node.isCheckedOut()) {
            if (node.isNodeType("mix:versionable")) {
                final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
                versionManager.checkout(node.getPath());
            } else if (traverseAncestors) {
                ensureIsCheckedOut(node.getParent(), true);
            }
        }
    }

    /**
     * @param node the node to check
     * @return whether the node is virtual
     * @throws RepositoryException
     */
    public static boolean isVirtual(Node node) throws RepositoryException {
        return node instanceof HippoNode && ((HippoNode) node).isVirtual();
    }

    /**
     * Get the path of a {@link Node}, or <code>null</code> if the path cannot be retrieved.
     *
     * <p>
     * This method is mainly provided for convenience of usage, so a developer does not have to worry about exception
     * handling in case it is not of interest.
     * </p>
     *
     * @param node - The {@link Node} to get the path of
     * @return The path of the {@link Node}, or <code>null</code> if <code>node</code> is null or an exception happens.
     */
    public static String getNodePathQuietly(final Node node) {
        try {
            return ((node == null) ? null : node.getPath());
        } catch (RepositoryException ignored) {
            return null;
        }
    }

    private static boolean isAutoCreatedNode(final String childName, Node parent) throws RepositoryException {
        for (NodeDefinition nodeDefinition : parent.getPrimaryNodeType().getChildNodeDefinitions()) {
            if (childName.equals(nodeDefinition.getName())) {
                if (nodeDefinition.isAutoCreated()) {
                    return true;
                }
            }
        }
        for (NodeType nodeType : parent.getMixinNodeTypes()) {
            for (NodeDefinition nodeDefinition : nodeType.getChildNodeDefinitions()) {
                if (childName.equals(nodeDefinition.getName())) {
                    if (nodeDefinition.isAutoCreated()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static byte[] objectToBytes(Object o) throws RepositoryException {
        try {
            ByteArrayOutputStream store = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(store);
            ostream.writeObject(o);
            ostream.flush();
            return store.toByteArray();
        } catch (IOException ex) {
            throw new ValueFormatException(ex);
        }
    }

}
