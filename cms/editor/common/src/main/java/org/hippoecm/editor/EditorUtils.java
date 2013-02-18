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

import javax.jcr.*;
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
     * @param node
     * @param nodeType
     * @throws RepositoryException
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     */
    public static void createMandatoryProperties(Node node, NodeType nodeType) throws RepositoryException,
            ValueFormatException, VersionException, LockException, ConstraintViolationException {

        NodeType[] supers = nodeType.getSupertypes();
        NodeType[] all = new NodeType[supers.length + 1];
        System.arraycopy(supers, 0, all, 0, supers.length);
        all[supers.length] = nodeType;
        for (NodeType type : all) {
            for (PropertyDefinition propertyDefinition : type.getPropertyDefinitions()) {
                if (propertyDefinition.getDeclaringNodeType() == type) {
                    if (propertyDefinition.isMandatory() && !propertyDefinition.isProtected() && !"*".equals(propertyDefinition.getName())
                            && !node.hasProperty(propertyDefinition.getName())) {

                        if (propertyDefinition.isMultiple()) {
                            node.setProperty(propertyDefinition.getName(), new Value[0]);
                        } else {
                            switch (propertyDefinition.getRequiredType()) {
                                case PropertyType.LONG:
                                    node.setProperty(propertyDefinition.getName(), 0);
                                    break;
                                case PropertyType.DOUBLE:
                                    node.setProperty(propertyDefinition.getName(), 0.0f);
                                    break;
                                case PropertyType.DATE:
                                    node.setProperty(propertyDefinition.getName(), Calendar.getInstance());
                                    break;
                                case PropertyType.REFERENCE:
                                    node.setProperty(propertyDefinition.getName(), node.getSession().getRootNode());
                                    break;
                                case PropertyType.STRING:
                                    String[] constraints = propertyDefinition.getValueConstraints();
                                    if (constraints != null && constraints.length > 0) {
                                        node.setProperty(propertyDefinition.getName(), constraints[0]);
                                        break;
                                    }
                                default:
                                    node.setProperty(propertyDefinition.getName(), "");
                            }
                        }
                    }
                }
            }
        }
    }

}
