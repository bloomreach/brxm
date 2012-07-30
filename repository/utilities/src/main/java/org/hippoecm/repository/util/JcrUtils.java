/*
 *  Copyright 2012 Hippo.
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

import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.HippoNode;

/**
 * Some utility methods for writing code against JCR API. This code can be removed when we upgrade to JR 2.6...
 */
public class JcrUtils {

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
     */
    public static long getLongProperty(Node baseNode, String relPath, long defaultValue) throws RepositoryException {
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
     */
    public static double getDoubleProperty(Node baseNode, String relPath, double defaultValue) throws RepositoryException {
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
     */
    public static boolean getBooleanProperty(Node baseNode, String relPath, boolean defaultValue) throws RepositoryException {
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
     */
    public static long getLongProperty(Session session, String absPath, long defaultValue) throws RepositoryException {
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
     */
    public static double getDoubleProperty(Session session, String absPath, double defaultValue) throws RepositoryException {
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
     */
    public static boolean getBooleanProperty(Session session, String absPath, boolean defaultValue) throws RepositoryException {
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

    private static void copy(final Node srcNode, final String destNodeName, final Node destParentNode) throws RepositoryException {
        if (srcNode instanceof HippoNode && ((HippoNode)srcNode).isVirtual()) {
            return;
        }
        final Node destNode = destParentNode.addNode(destNodeName, srcNode.getPrimaryNodeType().getName());
        for (NodeType nodeType : srcNode.getMixinNodeTypes()) {
            destNode.addMixin(nodeType.getName());
        }

        final PropertyIterator properties = srcNode.getProperties();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            if (!property.getDefinition().isProtected()) {
                if (property.isMultiple()) {
                    destNode.setProperty(property.getName(), property.getValues());
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
}
