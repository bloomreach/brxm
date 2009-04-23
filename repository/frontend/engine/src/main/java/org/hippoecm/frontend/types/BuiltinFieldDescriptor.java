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
package org.hippoecm.frontend.types;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuiltinFieldDescriptor extends JavaFieldDescriptor {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BuiltinFieldDescriptor.class);

    BuiltinFieldDescriptor(String prefix, ItemDefinition definition) {
        super(prefix, getType(definition));

        setPath(definition.getName());
        if (definition instanceof NodeDefinition) {
            NodeDefinition ntDef = (NodeDefinition) definition;
            setMultiple(ntDef.allowsSameNameSiblings());
            setMandatory(ntDef.isMandatory());
        } else {
            PropertyDefinition propDef = (PropertyDefinition) definition;
            setMultiple(propDef.isMultiple());
            setMandatory(propDef.isMandatory());
        }

        setMandatory(definition.isMandatory());

        if (isMultiple()) {
            if (definition.getDeclaringNodeType().hasOrderableChildNodes()) {
                setOrdered(true);
            }
        }
    }

    static String getType(ItemDefinition def) {
        if (def instanceof NodeDefinition) {
            NodeDefinition ntDef = (NodeDefinition) def;
            NodeType[] types = ntDef.getRequiredPrimaryTypes();
            if (types.length == 0) {
                return "nt:base";
            } else if (types.length > 1) {
                log.warn("multiple primary types specified; this is not supported.  Only the first one is used.");
            }
            return types[0].getName();
        } else {
            return PropertyType.nameFromValue(((PropertyDefinition) def).getRequiredType());
        }
    }
}