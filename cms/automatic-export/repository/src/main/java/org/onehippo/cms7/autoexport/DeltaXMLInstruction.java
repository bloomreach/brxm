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
package org.onehippo.cms7.autoexport;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class DeltaXMLInstruction {

    private final boolean isNode;
    private final String name;
    private String parentPath;
    private final String contextPath;
    private final String directive;
    private DeltaXMLInstruction parent;
    private Map<String, DeltaXMLInstruction> nodeInstructions;
    private Map<String, DeltaXMLInstruction> propertyInstructions;
    
    DeltaXMLInstruction(boolean isNode, String name, String directive, String contextPath) {
        this.isNode = isNode;
        this.name = name;
        this.directive = directive;
        this.contextPath = contextPath;
        parentPath = contextPath.substring(0, contextPath.lastIndexOf('/'));
        if (parentPath.isEmpty()) {
            parentPath = "/";
        }
    }
    
    DeltaXMLInstruction(boolean isNode, String name, String directive, DeltaXMLInstruction parent) {
        this.isNode = isNode;
        this.name = name;
        this.parentPath = parent.getContextPath();
        this.contextPath = parentPath.equals("/") ? parentPath + name : parentPath + "/" + name;
        this.directive = directive;
        this.parent = parent;
    }
    
    boolean isNodeInstruction() {
        return isNode;
    }
    
    String getName() {
        return name;
    }
    
    String getParentPath() {
        return parentPath;
    }
    
    String getContextPath() {
        return contextPath;
    }
    
    boolean isCombineDirective() {
        return "combine".equals(directive);
    }
    
    boolean isUnsupportedDirective() {
        return !(isCombineDirective() || isOverrideDirective() || isNoneDirective());
    }
    
    boolean isNoneDirective() {
        return directive == null || directive.isEmpty();
    }
    
    boolean isOverrideDirective() {
        return "override".equals(directive);
    }
    
    String getDirective() {
        return directive;
    }
    
    DeltaXMLInstruction getParentInstruction() {
        return parent;
    }
    
    void addInstruction(DeltaXMLInstruction instruction) {
        if (instruction.isNodeInstruction()) {
            if (nodeInstructions == null) {
                nodeInstructions = new HashMap<String, DeltaXMLInstruction>();
            }
            nodeInstructions.put(instruction.getName(), instruction);
        } else {
            if (propertyInstructions == null) {
                propertyInstructions = new HashMap<String, DeltaXMLInstruction>();
            }
            propertyInstructions.put(instruction.getName(), instruction);
        }
    }
    
    void removeInstruction(DeltaXMLInstruction instruction) {
        if (instruction.isNodeInstruction()) {
            if (nodeInstructions != null) {
                nodeInstructions.remove(instruction.getName());
            }
        } else {
            if (propertyInstructions != null) {
                propertyInstructions.remove(instruction.getName());
            }
        }
    }
    
    boolean isEmpty() {
        return (nodeInstructions == null || nodeInstructions.isEmpty()) && (propertyInstructions == null || propertyInstructions.isEmpty());
    }
    
    void clear() {
        if (nodeInstructions != null) {
            nodeInstructions = null;
        }
        if (propertyInstructions != null) {
            propertyInstructions = null;
        }
    }
        
    DeltaXMLInstruction getInstruction(String name, boolean isNode) {
        if (isNode) {
            return nodeInstructions != null ? nodeInstructions.get(name) : null;
        } else {
            return propertyInstructions != null ? propertyInstructions.get(name) : null;
        }
    }
    
    Collection<DeltaXMLInstruction> getPropertyInstructions() {
        if (propertyInstructions != null) {
            return propertyInstructions.values();
        }
        return null;
    }
    
    Collection<DeltaXMLInstruction> getNodeInstructions() {
        if (nodeInstructions != null) {
            return nodeInstructions.values();
        }
        return null;
    }

}
