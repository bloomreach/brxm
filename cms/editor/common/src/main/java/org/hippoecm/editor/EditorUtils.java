/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.hippoecm.editor;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import java.util.Calendar;

/**
 * A commons utils class for the CMS editor component
 */
public class EditorUtils {

    /**
     * Create mandatory properties specified by the given {@link NodeType} and its super-types inclusively on the
     * given {@link Node}
     *
     * <P>
     * Inclusively means that the created mandatory properties are the one defined on the super-types of {@link NodeType}
     * in addition to the {@link NodeType} itself
     * </P>
     *
     * @param node The {@link Node} on which to create the mandatory properties
     * @param nodeType The {@link NodeType} from which to check for which mandatory properties need to be created if any
     * @throws RepositoryException
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     */
    public static void createMandatoryProperties(Node node, NodeType nodeType) throws RepositoryException,
            ValueFormatException, VersionException, LockException, ConstraintViolationException {
        final Logger log = LoggerFactory.getLogger(EditorUtils.class);

        NodeType[] supers = nodeType.getSupertypes();
        NodeType[] all = new NodeType[supers.length + 1];
        System.arraycopy(supers, 0, all, 0, supers.length);
        all[supers.length] = nodeType;
        for (NodeType type : all) {
            final Node prototypeNode = getPrototypeNode(type.getName(), node.getSession());

            for (PropertyDefinition propertyDefinition : type.getDeclaredPropertyDefinitions()) {
                if (propertyDefinition.isMandatory() && !propertyDefinition.isProtected() && !"*".equals(propertyDefinition.getName())){
                    log.debug("Add the mandatory property  '{}' of '{}' to the node '{}'",
                            propertyDefinition.getName(), nodeType.getName(), node.getPath());
                    if (node.hasProperty(propertyDefinition.getName())) {
                        // even though the property existed, we need to add it again after addMixin()
                        setProperty(node, propertyDefinition, node);
                    }else {
                        setProperty(node, propertyDefinition, prototypeNode);
                    }
                }
            }
        }
    }

    /**
     * Get a prototype {@link Node} given a {@link NodeType} name
     *
     * @param nodeTypeName - The name of a {@link NodeType}
     * @return The prototype {@link Node} of that {@link NodeType} name or <code>null</code> of it does not exist
     */
    public static Node getPrototypeNode(final String nodeTypeName, final Session session) throws RepositoryException {
        if (StringUtils.isBlank(nodeTypeName)) {
            return null;
        }

        String prefix;
        String typeName = "";

        // Set prefix and type name values
        String[] values = nodeTypeName.split(":");

        if (values.length > 1) {
            typeName = values[1];
        }
        prefix = values[0];

        // Build the prototype node path
        String prototypeNodePath = "/" + HippoNodeType.NAMESPACES_PATH + "/" + prefix + "/";
        if (!"".equals(typeName)) {
            prototypeNodePath += typeName + "/";
        }
        prototypeNodePath += HippoNodeType.HIPPO_PROTOTYPES + "/" + HippoNodeType.HIPPO_PROTOTYPE;

        // Get the prototype node if any
        if (session.nodeExists(prototypeNodePath)) {
            return session.getNode(prototypeNodePath);
        }

        final Logger log = LoggerFactory.getLogger(EditorUtils.class);
        log.info("Could not find the prototype node for type '{}'", nodeTypeName);
        log.debug("Cloud not find the prototype node for type '{}' in this location '{}'", nodeTypeName, prototypeNodePath);
        return null;
    }

    private static void setProperty(final Node node, final PropertyDefinition propertyDefinition, final Node prototypeNode)
            throws RepositoryException, ValueFormatException, VersionException, LockException, ConstraintViolationException {
        final String propertyName = propertyDefinition.getName();
        final Logger log = LoggerFactory.getLogger(EditorUtils.class);

        if (prototypeNode == null) {
            setPropertyFromDefaultValues(node, propertyDefinition);
        } else {
            try {
                if (propertyDefinition.isMultiple()) {
                    Value[] propValues = prototypeNode.getProperty(propertyName).getValues();
                    if (node.hasProperty(propertyName)){
                        node.getProperty(propertyName).remove();
                    }
                    node.setProperty(propertyName, propValues);
                } else {
                    Value propValue = prototypeNode.getProperty(propertyName).getValue();
                    if (node.hasProperty(propertyName)){
                        node.getProperty(propertyName).remove();
                    }
                    node.setProperty(propertyName, propValue);
                    log.debug("Set '{}' to the property '{}' at the node '{}'", propValue.getString(), propertyName, node.getPath());
                }
            } catch (PathNotFoundException ex) {
                // Use the defaults values as a fallback
                if (log.isDebugEnabled()) {
                    log.warn("Could not get property '" + propertyName + "' from '" + prototypeNode.getPath() + "'", ex);
                } else {
                    log.warn("Could not get property '{}' from '{}'. {}", new Object[] {propertyName,
                            prototypeNode.getPath(), ex.toString()});
                }

                setPropertyFromDefaultValues(node, propertyDefinition);
            }
        }
    }

    private static void setPropertyFromDefaultValues(final Node node, final PropertyDefinition propertyDefinition)
            throws RepositoryException, ValueFormatException, VersionException, LockException, ConstraintViolationException {

        String propertyName = propertyDefinition.getName();
        if (propertyDefinition.isMultiple()) {
            node.setProperty(propertyName, new Value[0]);
        } else {
            switch (propertyDefinition.getRequiredType()) {
                case PropertyType.LONG:
                    node.setProperty(propertyName, 0);
                    break;
                case PropertyType.DOUBLE:
                    node.setProperty(propertyName, 0.0f);
                    break;
                case PropertyType.DATE:
                    node.setProperty(propertyName, Calendar.getInstance());
                    break;
                case PropertyType.REFERENCE:
                    node.setProperty(propertyName, node.getSession().getRootNode());
                    break;
                case PropertyType.STRING:
                    String[] constraints = propertyDefinition.getValueConstraints();
                    if (constraints != null && constraints.length > 0) {
                        node.setProperty(propertyName, constraints[0]);
                        break;
                    }
                default:
                    node.setProperty(propertyName, "");
            }
        }
    }

}
