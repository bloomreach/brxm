/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * PropertyDefinitionUtils
 * @version $Id$
 */
public class PropertyDefinitionUtils {
    
    private static final String ANY_NAME = "*";
    
    private PropertyDefinitionUtils() {

    }
    
    /**
     * Returns the <code>PropertyDefinition</code> for the name property in
     * the given node type. If the node type has no matching property
     * definition <code>null</code> is returned.
     * <P>
     * This method scans as follows: If a PropertyDefinition with the exact
     * name is found, this property definition is returned.
     * Otherwise the first residual property definition is returned.
     * Otherwise <code>null</code> is returned.
     * </P>
     * @param nodeType
     * @param propertyName
     * @param propertyType
     * @return
     */
    public static PropertyDefinition getPropertyDefinition(NodeType nodeType, String propertyName) {
        return getPropertyDefinition(nodeType, propertyName, PropertyType.UNDEFINED);
    }
    
    /**
     * Returns the <code>PropertyDefinition</code> for the name property in
     * the given node type. If the node type has no matching property
     * definition <code>null</code> is returned.
     * <P>
     * This method scans as follows: If a PropertyDefinition with the exact
     * name is found, this property definition is returned.
     * Otherwise the first residual property definition, which has the same required property type
     * as specified, is returned.
     * Otherwise <code>null</code> is returned.
     * </P>
     * @param nodeType
     * @param propertyName
     * @param requiredPropertyType
     * @return
     */
    public static PropertyDefinition getPropertyDefinition(NodeType nodeType, String propertyName, int requiredPropertyType) {
        PropertyDefinition [] pd = nodeType.getPropertyDefinitions();
        PropertyDefinition candidate = null;
        
        for (int i = 0; i < pd.length; i++) {
            if (propertyName.equals(pd[i].getName())) {
                return pd[i];
            }
            
            // consider it as candidate if there's a residual
            if (candidate == null && isResidualPropertyDefinition(pd[i])) {
                if (requiredPropertyType == PropertyType.UNDEFINED || requiredPropertyType == pd[i].getRequiredType()) {
                    candidate = pd[i];
                }
            }
        }
        
        return candidate;
    }
    
    /**
     * Returns a <code>PropertyDefinition</code> for the given property name.
     * This method first looks for a matching property definition in the
     * primary node type and then in the list of mixin node types of the node.
     * If a definition whose name is the same as the <code>propertyName</code>
     * is found, this definition is returned. Otherwise a residual property
     * definition may be returned if any available.
     * If there's no matching property, <code>null</code> is returned.
     * @param node
     * @param propertyName
     * @return
     * @throws RepositoryException
     */
    public static PropertyDefinition getPropertyDefinition(Node node, String propertyName) throws RepositoryException {
        return getPropertyDefinition(node, propertyName, PropertyType.UNDEFINED);
    }
    
    /**
     * Returns a <code>PropertyDefinition</code> for the given property name.
     * This method first looks for a matching property definition in the
     * primary node type and then in the list of mixin node types of the node.
     * If a definition whose name is the same as the <code>propertyName</code>
     * is found, this definition is returned. Otherwise a residual property
     * definition, which has the same required property type
     * as specified, may be returned if any available.
     * If there's no matching property, <code>null</code> is returned.
     * @param node
     * @param propertyName
     * @param requiredPropertyType
     * @return
     * @throws RepositoryException
     */
    public static PropertyDefinition getPropertyDefinition(Node node, String propertyName, int requiredPropertyType) throws RepositoryException {
        NodeType nt = node.getPrimaryNodeType();
        PropertyDefinition propDef = getPropertyDefinition(nt, propertyName, requiredPropertyType);
        
        // return the definition if it is not null and not a residual
        if (propDef != null && !isResidualPropertyDefinition(propDef)) {
            return propDef;
        }
        
        NodeType [] mixins = node.getMixinNodeTypes();
        
        if (mixins == null || mixins.length == 0) {
            return propDef;
        }
        
        for (int i = 0; i < mixins.length; i++) {
            PropertyDefinition candidate = getPropertyDefinition(mixins[i], propertyName, requiredPropertyType);
            
            if (candidate != null) {
                if (!isResidualPropertyDefinition(candidate)) {
                    return candidate;
                } else if (propDef == null) {
                    propDef = candidate;
                }
            }
        }
        
        return propDef;
    }
    
    public static boolean isResidualPropertyDefinition(PropertyDefinition pd) {
        String name = pd.getName();
        
        if (ANY_NAME.equals(name)) {
            return true;
        }
        
        return false;
    }
}
