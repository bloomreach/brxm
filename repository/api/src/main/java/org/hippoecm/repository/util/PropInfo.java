/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

public final class PropInfo {

    private final String name;

    private final int type;

    private final Value[] values;

    private boolean multiple;

    public PropInfo(String name, int type, Value[] values) {
        this.name = name;
        this.type = type;
        this.values = values;
        this.multiple = true;
    }

    public PropInfo(String name, int type, Value value) {
        this.name = name;
        this.type = type;
        this.multiple = false;
        this.values = new Value[]{value};
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public Value getValue() {
        return values[0];
    }

    public Value[] getValues() {
        return values;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public PropertyDefinition getApplicablePropertyDef(NodeType[] nodeTypes) {
        PropertyDefinition undefinedResidualDefinition = null;
        PropertyDefinition residualDefinition = null;
        for (NodeType nodeType : nodeTypes) {
            PropertyDefinition[] propDefs = nodeType.getPropertyDefinitions();
            for (PropertyDefinition propDef : propDefs) {
                if (!propDef.isMultiple() == isMultiple()) {
                    continue;
                }
                if (propDef.getName().equals(getName())) {
                    return propDef;
                } else if ("*".equals(propDef.getName())) {
                    if (PropertyType.UNDEFINED == propDef.getRequiredType()) {
                        undefinedResidualDefinition = propDef;
                    } else if (propDef.getRequiredType() == getType()) {
                        residualDefinition = propDef;
                    }
                }
            }
        }
        if (residualDefinition != null) {
            return residualDefinition;
        }
        if (undefinedResidualDefinition != null) {
            return undefinedResidualDefinition;
        }
        return null;
    }

    @Override
    public String toString() {
        return "PropInfo[" + getName() + '(' + PropertyType.nameFromValue(getType()) + ')' + "]";
    }
}
