/*
 * Copyright 2013-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A commons utils class for the CMS editor component
 */
public class EditorUtils {

    private static final Logger log = LoggerFactory.getLogger(EditorUtils.class);

    private static final String PATH_DELIMITER = "/";

    private EditorUtils() {
    }

    /**
     * Create mandatory properties specified by the given {@link NodeType} and its super-types inclusively on the given
     * {@link Node}
     *
     * <p>
     * Inclusively means that the created mandatory properties are the one defined on the super-types of {@link
     * NodeType} in addition to the {@link NodeType} itself
     * </P>
     *
     * @param node     The {@link Node} on which to create the mandatory properties
     * @param nodeType The {@link NodeType} from which to check for which mandatory properties need to be created if
     *                  any
     * @throws RepositoryException in case of an error
     */
    public static void createMandatoryProperties(final Node node, final NodeType nodeType) throws RepositoryException {
        final NodeType[] supers = nodeType.getSupertypes();
        final NodeType[] all = new NodeType[supers.length + 1];
        System.arraycopy(supers, 0, all, 0, supers.length);
        all[supers.length] = nodeType;
        for (final NodeType type : all) {
            final Node prototypeNode = getPrototypeNode(type.getName(), node.getSession());

            for (final PropertyDefinition propertyDefinition : type.getDeclaredPropertyDefinitions()) {
                if (propertyDefinition.isMandatory()
                        && !propertyDefinition.isProtected()
                        && !"*".equals(propertyDefinition.getName())) {

                    log.debug("Add the mandatory property  '{}' of '{}' to the node '{}'",
                            propertyDefinition.getName(), nodeType.getName(), node.getPath());
                    if (node.hasProperty(propertyDefinition.getName())) {
                        // even though the property existed, we need to add it again after addMixin()
                        setProperty(node, propertyDefinition, node);
                    } else {
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

        final String prefix;
        String typeName = "";

        // Set prefix and type name values
        final String[] values = nodeTypeName.split(":");

        if (values.length > 1) {
            typeName = values[1];
        }
        prefix = values[0];

        // Build the prototype node path
        String prototypeNodePath = PATH_DELIMITER + HippoNodeType.NAMESPACES_PATH + PATH_DELIMITER + prefix + PATH_DELIMITER;
        if (!"".equals(typeName)) {
            prototypeNodePath += typeName + PATH_DELIMITER;
        }
        prototypeNodePath += HippoNodeType.HIPPO_PROTOTYPES + PATH_DELIMITER + HippoNodeType.HIPPO_PROTOTYPE;

        // Get the prototype node if any
        if (session.nodeExists(prototypeNodePath)) {
            return session.getNode(prototypeNodePath);
        }

        if (log.isDebugEnabled()) {
            log.debug("Could not find the prototype node for type '{}' in this location '{}'", nodeTypeName,
                    prototypeNodePath);
        } else {
            log.info("Could not find the prototype node for type '{}'", nodeTypeName);
        }
        return null;
    }

    private static void setProperty(final Node node, final PropertyDefinition propertyDefinition, final Node prototypeNode)
            throws RepositoryException {

        if (prototypeNode == null) {
            setPropertyFromDefaultValues(node, propertyDefinition);
        } else {
            try {
                setPropertyFromPrototypeValues(propertyDefinition, node, prototypeNode);
            } catch (PathNotFoundException ex) {
                // Use the defaults values as a fallback
                final String propertyName = propertyDefinition.getName();
                final String prototypeNodePath = prototypeNode.getPath();
                if (log.isDebugEnabled()) {
                    log.warn("Could not get property '{}' from '{}'", propertyName, prototypeNodePath, ex);
                } else {
                    log.warn("Could not get property '{}' from '{}'. {}", propertyName, prototypeNodePath, ex);
                }

                setPropertyFromDefaultValues(node, propertyDefinition);
            }
        }
    }

    /**
     * Parameters {@code node} and {@code prototypeNode} can be the same, so we have to save the property values prior
     * to removing the property.
     */
    private static void setPropertyFromPrototypeValues(final PropertyDefinition propertyDefinition, final Node node,
                                                       final Node prototypeNode) throws RepositoryException {
        final String propertyName = propertyDefinition.getName();
        final Property prototypeNodeProperty = prototypeNode.getProperty(propertyName);

        if (propertyDefinition.isMultiple()) {
            final Value[] propValues = prototypeNodeProperty.getValues();
            if (node.hasProperty(propertyName)) {
                node.getProperty(propertyName).remove();
            }
            node.setProperty(propertyName, propValues);
        } else {
            final Value propValue = prototypeNodeProperty.getValue();
            if (node.hasProperty(propertyName)) {
                node.getProperty(propertyName).remove();
            }
            node.setProperty(propertyName, propValue);
        }
    }

    private static void setPropertyFromDefaultValues(final Node node, final PropertyDefinition propertyDefinition)
            throws RepositoryException {

        final String propertyName = propertyDefinition.getName();
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
                    final String[] constraints = propertyDefinition.getValueConstraints();
                    if (constraints != null && constraints.length > 0) {
                        node.setProperty(propertyName, constraints[0]);
                    } else {
                        node.setProperty(propertyName, "");
                    }
                    break;
                default:
                    node.setProperty(propertyName, "");
            }
        }
    }

}
