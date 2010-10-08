/*
 *  Copyright 2008 Hippo.
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
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * PropertyDefinitionUtils
 * @version $Id$
 */
public class PropertyDefinitionUtils {

    private PropertyDefinitionUtils() {

    }
    
    /**
     * Returns the <code>PropertyDefinition</code> for the name property in
     * the given node type. If the node type has no matching node type
     * definition <code>null</code> is returned.
     * @param nodeType
     * @param propertyName
     * @return
     */
    public static PropertyDefinition getPropertyDefinition(NodeType nodeType, String propertyName) {
        PropertyDefinition [] pd = nodeType.getPropertyDefinitions();
        
        for (int i = 0; i < pd.length; i++) {
            if (propertyName.equals(pd[i].getName())) {
                return pd[i];
            }
        }
        
        return null;
    }
    
    /**
     * Returns a <code>PropertyDefinition</code> for the given property name.
     * This method first looks for a matching property definition in the
     * primary node type and then in the list of mixin node types of the node.
     * If a definition whose name is the same as the <code>propertyName</code>
     * is found, this definition is returned. Otherwise <code>null</code> is returned.
     * @param node
     * @param propertyName
     * @return
     * @throws RepositoryException
     */
    public static PropertyDefinition getPropertyDefinition(Node node, String propertyName) throws RepositoryException {
        NodeType nt = node.getPrimaryNodeType();
        PropertyDefinition propDef = getPropertyDefinition(nt, propertyName);
        
        if (propDef != null) {
            return propDef;
        }
        
        NodeType [] mixins = node.getMixinNodeTypes();
        
        if (mixins != null) {
            for (int i = 0; i < mixins.length; i++) {
                propDef = getPropertyDefinition(mixins[i], propertyName);
    
                if (propDef != null) {
                    return propDef;
                }
            }
        }
        
        return null;
    }
}
