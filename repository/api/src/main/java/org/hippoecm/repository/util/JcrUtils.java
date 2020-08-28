/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.Event;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.repository.util.JcrConstants;

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_MOVED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PERSIST;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;

/**
 * Some utility methods for writing code against JCR API. This code can be removed when we upgrade to JR 2.6...
 */
public class JcrUtils {

    /**
     * Some properties of the following mixins  depend on data that is stored in version history. Since version history
     * is not copied the mixins and it's properties must also not be copied.
     */
    private static final Set<String> PROTECTED_MIXINS = Stream.of(
            HippoNodeType.NT_HIPPO_VERSION_INFO,
            HippoNodeType.HIPPO_MIXIN_BRANCH_INFO
    ).collect(Collectors.toSet());


    public static final int ALL_EVENTS = NODE_ADDED | NODE_REMOVED | NODE_MOVED
            | PROPERTY_ADDED | PROPERTY_CHANGED | PROPERTY_REMOVED | PERSIST;

    /**
     * Get the node at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such node exists.
     *
     * @param baseNode existing node that should be the base for the relative path
     * @param relPath  relative path to the node to get
     * @return the node at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such node exists.
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Node getNodeIfExists(Node baseNode, String relPath) throws RepositoryException {
        try {
            if (baseNode.hasNode(relPath)) {
                return baseNode.getNode(relPath);
            }
        } catch (PathNotFoundException ignore) {
        }
        return null;
    }

    /**
     * Gets the node at <code>absPath</code> or <code>null</code> if no such node exists.
     *
     * @param absPath the absolute path to the node to return
     * @param session to use
     * @return the node at <code>absPath</code> or <code>null</code> if no such node exists.
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Node getNodeIfExists(String absPath, Session session) throws RepositoryException {
        try {
            if (session.nodeExists(absPath)) {
                return session.getNode(absPath);
            }
        } catch (PathNotFoundException ignore) {
        }
        return null;
    }

    /**
     * Gets the node at <code>absPath</code> or <code>null</code> if no such node exists. In case there are more nodes
     * at <code>absPath</code>, the last node is returned.
     *
     * @param absPath the absolute path to the node to return
     * @param session to use
     * @return the node at <code>absPath</code> or <code>null</code> if no such node exists.
     * @throws RepositoryException
     */
    public static Node getLastNodeIfExists(String absPath, Session session) throws RepositoryException {
        if (absPath.equals("/")) {
            return session.getRootNode();
        }
        final int idx = absPath.lastIndexOf('/');
        final String parentAbsPath = absPath.substring(0, idx);
        final String nodeName = absPath.substring(idx + 1);
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
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the string property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static String getStringProperty(Node baseNode, String relPath, String defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                return baseNode.getProperty(relPath).getString();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the enum value of a string property at <code>relPath</code> from <code>baseNode</code> or
     * <code>defaultValue</code> if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the enum value of a string property at <code>relPath</code> from <code>baseNode</code> or
     *         <code>defaultValue</code> if no such property exists or property doesn't have any enum value
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static <E extends Enum<E>> E getEnumProperty(Node baseNode, String relPath, E defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                String propertyValue = baseNode.getProperty(relPath).getString();
                return (E) Enum.valueOf(defaultValue.getClass(), propertyValue);
            }
        } catch (PathNotFoundException | IllegalArgumentException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the multiple string property values at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the multiple string property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static String[] getMultipleStringProperty(final Node baseNode, final String relPath, final String[] defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                final Value[] values = baseNode.getProperty(relPath).getValues();
                final String[] result = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    result[i] = values[i].getString();
                }
                return result;
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the multiple string property values at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the multiple string property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static List<String> getStringListProperty(final Node baseNode, final String relPath,
                                                     final List<String> defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                final Value[] values = baseNode.getProperty(relPath).getValues();
                final ArrayList<String> result = new ArrayList<>(values.length);
                for (final Value value : values) {
                    result.add(value.getString());
                }
                return result;
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the unique and unordered multiple string property values at <code>relPath</code>
     * from <code>baseNode</code> or <code>defaultValue</code> if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the unique and unordered multiple string property value at <code>relPath</code> from <code>baseNode</code>
     * or <code>defaultValue</code> if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Set<String> getStringSetProperty(final Node baseNode, final String relPath,
                                                     final Set<String> defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                final Value[] values = baseNode.getProperty(relPath).getValues();
                final HashSet<String> result = new HashSet<>(values.length);
                for (final Value value : values) {
                    result.add(value.getString());
                }
                return result;
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the long property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the long property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Long getLongProperty(Node baseNode, String relPath, Long defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                return baseNode.getProperty(relPath).getLong();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the double property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the double property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Double getDoubleProperty(Node baseNode, String relPath, Double defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                return baseNode.getProperty(relPath).getDouble();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the boolean property value at <code>relPath</code> from <code>baseNode</code> or
     * <code>defaultValue</code> if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the boolean property value at <code>relPath</code> from <code>baseNode</code> or
     * <code>defaultValue</code> if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Boolean getBooleanProperty(Node baseNode, String relPath, Boolean defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                return baseNode.getProperty(relPath).getBoolean();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the date property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the date property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Calendar getDateProperty(Node baseNode, String relPath, Calendar defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                return baseNode.getProperty(relPath).getDate();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the decimal property value at <code>relPath</code> from <code>baseNode</code> or
     * <code>defaultValue</code> if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the decimal property value at <code>relPath</code> from <code>baseNode</code> or
     * <code>defaultValue</code> if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static BigDecimal getDecimalProperty(Node baseNode, String relPath, BigDecimal defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                return baseNode.getProperty(relPath).getDecimal();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the binary property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the binary property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Binary getBinaryProperty(Node baseNode, String relPath, Binary defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                return baseNode.getProperty(relPath).getBinary();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the node property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param relPath      relative path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the node property value at <code>relPath</code> from <code>baseNode</code> or <code>defaultValue</code>
     * if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Node getNodeProperty(Node baseNode, String relPath, Node defaultValue) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                return baseNode.getProperty(relPath).getNode();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }


    /**
     * Returns the string property value at <code>absPath</code> or <code>defaultValue</code> if no such property
     * exists.
     *
     * @param session      to use
     * @param absPath      absolute path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the string property value at <code>absPath</code> or <code>defaultValue</code> if no such property
     * exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static String getStringProperty(Session session, String absPath, String defaultValue) throws RepositoryException {
        try {
            if (session.itemExists(absPath)) {
                return session.getProperty(absPath).getString();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the long property value at <code>absPath</code> or <code>defaultValue</code> if no such property exists.
     *
     * @param session      to use
     * @param absPath      absolute path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the long property value at <code>absPath</code> or <code>defaultValue</code> if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Long getLongProperty(Session session, String absPath, Long defaultValue) throws RepositoryException {
        try {
            if (session.itemExists(absPath)) {
                return session.getProperty(absPath).getLong();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the double property value at <code>absPath</code> or <code>defaultValue</code> if no such property
     * exists.
     *
     * @param session      to use
     * @param absPath      absolute path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the double property value at <code>absPath</code> or <code>defaultValue</code> if no such property
     * exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Double getDoubleProperty(Session session, String absPath, Double defaultValue) throws RepositoryException {
        try {
            if (session.itemExists(absPath)) {
                return session.getProperty(absPath).getDouble();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the boolean property value at <code>absPath</code> or <code>defaultValue</code> if no such property
     * exists.
     *
     * @param session      to use
     * @param absPath      absolute path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the boolean property value at <code>absPath</code> or <code>defaultValue</code> if no such property
     * exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Boolean getBooleanProperty(Session session, String absPath, Boolean defaultValue) throws RepositoryException {
        try {
            if (session.itemExists(absPath)) {
                return session.getProperty(absPath).getBoolean();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the date property value at <code>absPath</code> or <code>defaultValue</code> if no such property exists.
     *
     * @param session      to use
     * @param absPath      absolute path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the date property value at <code>absPath</code> or <code>defaultValue</code> if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Calendar getDateProperty(Session session, String absPath, Calendar defaultValue) throws RepositoryException {
        try {
            if (session.itemExists(absPath)) {
                return session.getProperty(absPath).getDate();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the decimal property value at <code>absPath</code> or <code>defaultValue</code> if no such property
     * exists.
     *
     * @param session      to use
     * @param absPath      absolute path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the decimal property value at <code>absPath</code> or <code>defaultValue</code> if no such property
     * exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static BigDecimal getDecimalProperty(Session session, String absPath, BigDecimal defaultValue) throws RepositoryException {
        try {
            if (session.itemExists(absPath)) {
                return session.getProperty(absPath).getDecimal();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the binary property value at <code>absPath</code> or <code>defaultValue</code> if no such property
     * exists.
     *
     * @param session      to use
     * @param absPath      absolute path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the binary property value at <code>absPath</code> or <code>defaultValue</code> if no such property
     * exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Binary getBinaryProperty(Session session, String absPath, Binary defaultValue) throws RepositoryException {
        try {
            if (session.itemExists(absPath)) {
                return session.getProperty(absPath).getBinary();
            }
        } catch (PathNotFoundException | ValueFormatException ignore) {
        }
        return defaultValue;
    }

    /**
     * Returns the node property value at <code>absPath</code> or <code>defaultValue</code> if no such property exists.
     *
     * @param session      to use
     * @param absPath      absolute path to the property to get
     * @param defaultValue default value to return when the property does not exist
     * @return the node property value at <code>absPath</code> or <code>defaultValue</code> if no such property exists
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Node getNodeProperty(Session session, String absPath, Node defaultValue) throws RepositoryException {
        try {
            if (session.itemExists(absPath)) {
                return session.getProperty(absPath).getNode();
            }
        } catch (PathNotFoundException ignore) {
        }
        return defaultValue;
    }

    /**
     * Get the property at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such property
     * exists.
     *
     * @param baseNode existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @return the property at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such property
     * exists.
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Property getPropertyIfExists(Node baseNode, String relPath) throws RepositoryException {
        try {
            if (baseNode.hasProperty(relPath)) {
                return baseNode.getProperty(relPath);
            }
        } catch (PathNotFoundException ignore) {
        }
        return null;
    }

    /**
     * Gets the property at <code>absPath</code> or <code>null</code> if no such property exists.
     *
     * @param absPath the absolute path to the property to return
     * @param session to use
     * @return the property at <code>absPath</code> or <code>null</code> if no such property exists.
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Property getPropertyIfExists(String absPath, Session session) throws RepositoryException {
        try {
            if (session.itemExists(absPath)) {
                return session.getProperty(absPath);
            }
        } catch (PathNotFoundException ignore) {
        }
        return null;
    }

    /**
     * Copies node at <code>srcAbsPath</code> to <code>destAbsPath</code> as session operation.
     *
     * @param session     to use
     * @param srcAbsPath  the absolute path of the source node
     * @param destAbsPath the absolute path of the resulting copy
     * @return the created node
     * @throws RepositoryException
     * @throws IllegalArgumentException if srcNode is same as destParentNode or destParentNode is a descendant of srcNode
     *                                  or destAbsPath is the root node path.
     */
    public static Node copy(Session session, String srcAbsPath, String destAbsPath) throws RepositoryException {
        if (destAbsPath.equals("/")) {
            throw new IllegalArgumentException("Root cannot be the destination of a copy");
        }

        if (destAbsPath.startsWith(srcAbsPath) && destAbsPath.substring(srcAbsPath.length()).startsWith("/")) {
            throw new IllegalArgumentException("Destination cannot be descendant of source node");
        }

        final Node srcNode = session.getNode(srcAbsPath);
        final int idx = destAbsPath.lastIndexOf('/');
        final String parentDestAbsPath = idx == 0 ? "/" : destAbsPath.substring(0, idx);
        final String destNodeName = destAbsPath.substring(idx + 1);
        final Node destParentNode = session.getNode(parentDestAbsPath);

        return copy(srcNode, destNodeName, destParentNode);
    }

    /**
     * Copies {@link Node} {@code srcNode} to {@code destParentNode} with name {@code destNodeName}.
     *
     * @param srcNode        the node to copy
     * @param destNodeName   the name of the to be newly created node
     * @param destParentNode the parent of the to be newly created node
     * @return the created node
     * @throws RepositoryException
     * @throws IllegalArgumentException if srcNode is same as destParentNode or destParentNode is a descendant of srcNode
     */
    public static Node copy(final Node srcNode, final String destNodeName, final Node destParentNode) throws RepositoryException {
        if (isVirtual(srcNode)) {
            return null;
        }
        if (destNodeName.indexOf('/') != -1) {
            throw new IllegalArgumentException(destNodeName + " is a path, not a name");
        }
        if (srcNode.isSame(destParentNode)) {
            throw new IllegalArgumentException("Destination parent node cannot be the same as source node");
        }
        if (isAncestor(srcNode, destParentNode)) {
            throw new IllegalArgumentException("Destination parent node cannot be descendant of source node");
        }
        final DefaultCopyHandler chain = new DefaultCopyHandler(destParentNode, PROTECTED_MIXINS);
        final NodeInfo nodeInfo = new NodeInfo(srcNode);
        final NodeInfo newInfo = new NodeInfo(destNodeName, 0, nodeInfo.getNodeType(), nodeInfo.getMixinTypes());
        chain.startNode(newInfo);
        final Node destNode = chain.getCurrent();
        copyToChain(srcNode, chain);
        chain.endNode();
        return destNode;
    }

    /**
     * Copies {@link Node} {@code srcNode} to {@code destNode}.
     *
     * @param srcNode  the node to copy
     * @param destNode the node that the contents of srcNode will be copied to
     * @throws RepositoryException
     * @throws IllegalArgumentException if scrNode is same as destNode or destNode is a descendant of srcNode
     */
    public static void copyTo(final Node srcNode, Node destNode) throws RepositoryException {
        if (srcNode.isSame(destNode)) {
            throw new IllegalArgumentException("Destination node cannot be the same as source node");
        }
        if (isAncestor(srcNode, destNode)) {
            throw new IllegalArgumentException("Destination node cannot be descendant of source node");
        }
        copyTo(srcNode, new OverwritingCopyHandler(destNode, PROTECTED_MIXINS));
    }

    /**
     * Copies {@link Node} {@code srcNode} to {@code destNode} with a {@code handler} to rewrite content if necessary.
     *
     * @param srcNode the node to copy
     * @param chain   the handler that intercepts node and property creation, can be null
     * @throws RepositoryException
     */
    public static Node copyTo(Node srcNode, final CopyHandler chain) throws RepositoryException {
        chain.startNode(new NodeInfo(srcNode));
        Node result = chain.getCurrent();
        copyToChain(srcNode, chain);
        chain.endNode();
        return result;
    }

    public static void copyToChain(final Node srcNode, CopyHandler chain) throws RepositoryException {
        for (Property property : new PropertyIterable(srcNode.getProperties())) {
            PropInfo propInfo;
            if (property.isMultiple()) {
                propInfo = new PropInfo(property.getName(), property.getType(), property.getValues());
            } else {
                propInfo = new PropInfo(property.getName(), property.getType(), property.getValue());
            }
            chain.setProperty(propInfo);
        }

        for (final Node child : new NodeIterable(srcNode.getNodes())) {
            if (isVirtual(child)) {
                continue;
            }
            // virtual nodes are checked in with rep:root type
            if ("rep:root".equals(getPrimaryNodeType(child).getName())) {
                continue;
            }
            if (chain.skipNode(child)) {
                continue;
            }
            NodeInfo info = new NodeInfo(child);
            chain.startNode(info);
            copyToChain(child, chain);
            chain.endNode();
        }
    }

    /**
     * Retrieve the primary node type.  Can handle frozen nodes as well as regular nodes.
     *
     * @param node
     * @return the primary node type
     * @throws RepositoryException
     */
    public static NodeType getPrimaryNodeType(Node node) throws RepositoryException {
        if (node.isNodeType(JcrConstants.NT_FROZEN_NODE)) {
            return node.getSession().getWorkspace().getNodeTypeManager().getNodeType(
                    node.getProperty(JcrConstants.JCR_FROZEN_PRIMARY_TYPE).getString());
        } else {
            return node.getPrimaryNodeType();
        }
    }

    /**
     * Retrieve the mixin node types present on a node.  Can handle frozen nodes as well as regular nodes.
     *
     * @param node
     * @return the mixin node types present on a node
     * @throws RepositoryException
     */
    public static NodeType[] getMixinNodeTypes(Node node) throws RepositoryException {
        if (node.isNodeType(JcrConstants.NT_FROZEN_NODE)) {
            Session session = node.getSession();
            if (!node.hasProperty(JcrConstants.JCR_FROZEN_MIXIN_TYPES)) {
                return new NodeType[0];
            }
            final NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
            Value[] mixins = node.getProperty(JcrConstants.JCR_FROZEN_MIXIN_TYPES).getValues();
            NodeType[] mixinTypes = new NodeType[mixins.length];
            int i = 0;
            for (Value mixin : mixins) {
                mixinTypes[i++] = nodeTypeManager.getNodeType(mixin.getString());
            }
            return mixinTypes;
        } else {
            return node.getMixinNodeTypes();
        }
    }

    /**
     * Serialize the given <code>object</code> into a binary JCR value.
     *
     * @param session to use
     * @param object  to serialize
     * @return a binary value containing the serialized object
     * @throws RepositoryException
     */
    public static Value createBinaryValueFromObject(Session session, Object object) throws RepositoryException {
        final ValueFactory valueFactory = session.getValueFactory();
        return valueFactory.createValue(valueFactory.createBinary(new ByteArrayInputStream(objectToBytes(object))));
    }

    /**
     * @return an empty {@link NodeIterable}
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
     * Make sure the node is in checked out state. If the node is not in checked out state it will get checked out
     *
     * @param node the node to check
     * @throws RepositoryException
     */
    public static void ensureIsCheckedOut(Node node) throws RepositoryException {
        if (!node.isCheckedOut()) {
            if (node.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
                versionManager.checkout(node.getPath());
                node.getSession().refresh(true);
            }
            ensureIsCheckedOut(node.getParent());
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
     * <p/>
     * <p> This method is mainly provided for convenience of usage, so a developer does not have to worry about
     * exception handling in case it is not of interest. </p>
     *
     * @param node - The {@link Node} to get the path of
     * @return The path of the {@link Node}, or <code>null</code> if <code>node</code> is null or an exception happens.
     */
    public static String getNodePathQuietly(final Node node) {
        try {
            return node == null ? null : node.getPath();
        } catch (RepositoryException ignored) {
            return null;
        }
    }

    /**
     * Get the name of a {@link Node}, or <code>null</code> if the node cannot be retrieved.
     * <p/>
     * <p> This method is mainly provided for convenience of usage, so a developer does not have to worry about
     * exception handling in case it is not of interest. </p>
     *
     * @param node - The {@link Node} to get the name of
     * @return The name of the {@link Node}, or <code>null</code> if <code>node</code> is null or an exception happens.
     */
    public static String getNodeNameQuietly(final Node node) {
        try {
            return node == null ? null : node.getName();
        } catch (RepositoryException ignored) {
            return null;
        }
    }

    /**
     * Calls {@link Node#getProperties()}  on the given node and returns
     * the it as an {@link Iterable} instance for use in a Java 5 for-each loop.
     *
     * @param node node
     * @return properties of the node as an iterable
     * @throws RepositoryException if the {@link Node#getProperties()} call fails
     */
    public static Iterable<Property> getProperties(Node node) throws RepositoryException {
        return new PropertyIterable(node.getProperties());
    }

    public static boolean isPropertyEvent(final Event event) {
        return event.getType() == PROPERTY_REMOVED || event.getType() == PROPERTY_ADDED || event.getType() == PROPERTY_CHANGED;
    }

    /**
     * <p>
     * Returns the next sibling {@link Node} of {@code current}. If there is no next sibling it returns {@code null}.
     * If the parent {@link Node} of {@code current} is not an orderable {@link Node}, an
     * {@link UnsupportedRepositoryOperationException}
     * will be thrown. Checking whether the parent is orderable can be done easily as follows
     * <pre>
     *         Node parent = current.getParent();
     *         parent.getPrimaryNodeType().hasOrderableChildNodes()
     * </pre>
     * </p>
     * <p>
     * If the caller of this method uses this method to reorder nodes, then take into account whether the
     * parent node allows same name siblings or not. If you do not know, use
     * <pre>
     *         Node parent = current.getParent();
     *         if (parent.getDefinition().allowsSameNameSiblings()) {
     *             Node next = getNextSiblingIfExists(current);
     *             parent.orderBefore(current.getName() + "[" + current.getIndex() + "]",
     *                                next.getName()+ "[" + next.getIndex() + "]");
     *         } else {
     *             parent.orderBefore(current.getName(), getNextSiblingIfExists(current).getName());
     *         }
     * </pre>
     * </p>
     *
     * @param current the {@link Node} for which to find the next sibling
     * @return the next sibling of current if there is a next one and {@code null} in case there is no next sibling
     * @throws UnsupportedRepositoryOperationException if the parent of {@code current} is not orderable
     * @throws RepositoryException                     if some repository exception happens
     */
    public static Node getNextSiblingIfExists(final Node current) throws RepositoryException {
        final Node parent = current.getParent();
        if (!parent.getPrimaryNodeType().hasOrderableChildNodes()) {
            throw new UnsupportedRepositoryOperationException(String.format(
                    "Node '%s' does not have an orderable parent hence invoking #getNextSiblingIfExists does not make sense.",
                    current.getPath()));
        }

        final NodeIterator nodeIterator = parent.getNodes();
        while (nodeIterator.hasNext()) {
            final Node sibling = nodeIterator.nextNode();
            if (sibling.isSame(current)) {
                return nodeIterator.hasNext() ? nodeIterator.nextNode() : null;
            }
        }

        throw new ItemNotFoundException("The 'current' item does not longer exist below its parent");
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

    private static boolean isAncestor(final Node ancestor, final Node descendant) throws RepositoryException {
        try {
            Node node = descendant;
            do {
                node = node.getParent();
            } while (!ancestor.isSame(node));
            return true;
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    /**
     * <p>
     *     Returns the list of jcr nodes which are descendants of {@code source} and of type {@code nodeType},
     *     where {@code source} is never included in the result. When {@code prune} is {@code true}, matching descendants
     *     are added and the descendants of a matching descendant are not scanned (and thus also not added)
     * </p>
     * @param source
     * @param nodeType
     * @return
     */
    public static List<Node> getDescendants(final Node source, final String nodeType, final boolean prune) throws RepositoryException {
        final List<Node> populate = new ArrayList<>();
        for (Node child : new NodeIterable(source.getNodes())) {
            populateNodes(child, populate, nodeType, prune);
        }
        return populate;
    }

    private static void populateNodes(final Node node, final List<Node> nodes, final String nodeType, final boolean prune) throws RepositoryException {
        if (node.isNodeType(nodeType)) {
            nodes.add(node);
            if (prune) {
                return;
            }
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            populateNodes(child, nodes, nodeType, prune);
        }

        return;
    }
}
