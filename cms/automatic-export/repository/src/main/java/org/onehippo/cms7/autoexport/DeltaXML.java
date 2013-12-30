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

import java.util.ArrayList;
import java.util.Collection;

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;
import static org.onehippo.cms7.autoexport.AutoExportModule.log;

class DeltaXML {
    
    private final String contextPath;
    private final DeltaXMLInstruction instruction;
    private final Collection<ExportEvent> events = new ArrayList<ExportEvent>();
    private Boolean isEnabled;
    
    DeltaXML(String contextPath) {
        this.contextPath = contextPath;
        String name = contextPath.substring(contextPath.lastIndexOf('/')+1);
        instruction = new DeltaXMLInstruction(true, name, "combine", contextPath);
    }
    
    DeltaXML(String contextPath, DeltaXMLInstruction instruction) {
        this.contextPath = contextPath;
        this.instruction = instruction;
    }
    
    DeltaXMLInstruction getRootInstruction() {
        return instruction;
    }
    
    void handleEvent(ExportEvent event) {
        if (!isEnabled()) {
            return;
        }
        events.add(event);
    }

    /**
     *
     * @return whether this delta xml should be exported
     */
    boolean processEvents() {
        if (!isEnabled()) {
            return false;
        }
        boolean result = false;
        try {
            for (ExportEvent event : events) {
                result |= processEvent(event);
            }
        } finally {
            events.clear();
        }
        return result;
    }
    
    private boolean hasEvent(String path, int type) {
        for (ExportEvent event : events) {
            if (event.getPath().equals(path) && event.getType() == type) {
                return true;
            }
        }
        return false;
    }
    
    boolean processEvent(ExportEvent event) {
        String path = event.getPath();
        
        if (contextPath.startsWith(path)) {
            if (event.getType() == NODE_REMOVED) {
                // parent node needs to be overlayed, we don't handle that
                log.warn("Change not handled by export: "
                        + ExportEvent.valueOf(event.getType()) + " on " + path
                        + ". You will need to do this manually.");
                instruction.clear();
                // parent node was removed, we need to be deleted
            }
            return true;
        }
        
        String relPath = path.substring(contextPath.length()+1);
        
        int offset = relPath.lastIndexOf('/');
        String name = offset == -1 ? relPath : relPath.substring(offset+1);
        String parentPath = offset == -1 ? null : relPath.substring(0, offset);
        
        DeltaXMLInstruction parentInstruction = getParentInstruction(instruction, relPath);
        
        if (event.getType() == NODE_ADDED) {
            boolean isReorder = hasEvent(path, NODE_REMOVED);
            if (isReorder) {
                // TODO: we could support reordering if we already have a add node instruction
                log.warn("Reordering nodes is not supported by export. You will need to do this manually.");
                return false;
            }
            if (parentInstruction == null) {
                assert parentPath != null;
                parentInstruction = createCombineInstruction(instruction, parentPath);
            }
            if (parentInstruction.isCombineDirective()) {
                DeltaXMLInstruction instruction = parentInstruction.getInstruction(name, true);
                if (instruction == null) {
                    parentInstruction.addInstruction(new DeltaXMLInstruction(true, name, null, parentInstruction));
                } else {
                    // this could only happen if automatic export was not turned on 
                    // the entire time when changes were being made and we missed a
                    // node removed event on this path
                    log.warn("Inconsistent state detected: NodeAdded on " + path 
                            + " while we thought it was already there.");
                }
            }
        } else if (event.getType() == NODE_REMOVED) {
            boolean isReorder = hasEvent(path, NODE_ADDED);
            if (isReorder) {
                // TODO: we could support reordering if we already have a add node instruction
                log.warn("Reordering nodes is not supported by export. You will need to do this manually.");
                return false;
            }
            if (parentInstruction == null) {
                // parent node needs to be overlayed, we don't handle that
                log.warn("Change not handled by export: " 
                        + ExportEvent.valueOf(event.getType()) + " on " + path
                        + ". You will need to do this manually.");
                return false;
            }
            if (parentInstruction.isCombineDirective()) {
                DeltaXMLInstruction instruction = parentInstruction.getInstruction(name, true);
                if (instruction == null) {
                    // node was not defined by our project
                    // parent node needs to be overlayed, we don't handle that
                    log.warn("Change not handled by export: " 
                            + ExportEvent.valueOf(event.getType()) + " on " + path
                            + ". You will need to do this manually.");
                    return false;
                }
                parentInstruction.removeInstruction(instruction);
                if (!instruction.isNoneDirective()) {
                    // all we can do is remove the node combine directive
                    // we can't remove the node itself
                    log.warn("Change not handled by export: " 
                            + ExportEvent.valueOf(event.getType()) + " on " + path
                            + ". You will need to do this manually.");
                }
                purge(parentInstruction);
            }
        } else if (event.getType() == PROPERTY_ADDED) {
            if (parentInstruction == null) {
                assert parentPath != null;
                parentInstruction = createCombineInstruction(instruction, parentPath);
            }
            if (parentInstruction.isCombineDirective()) {
                DeltaXMLInstruction instruction = parentInstruction.getInstruction(name, false);
                if (instruction == null) {
                    parentInstruction.addInstruction(new DeltaXMLInstruction(false, name, null, parentInstruction));
                } else {
                    // this could only happen if automatic export was not turned on 
                    // the entire time when changes were being made
                    // and we missed a property removed event on this path
                    log.warn("Inconsistent state detected: PropertyAdded on " + path 
                            + " while we thought it was already there");
                }
            }
        } else if (event.getType() == PROPERTY_REMOVED) {
            if (parentInstruction == null) {
                // parent node needs to be overlayed, we don't handle that
                log.warn("Change not handled by export: " 
                        + ExportEvent.valueOf(event.getType()) + " on " + path
                        + ". You will need to do this manually.");
                return false;
            }
            if (parentInstruction.isCombineDirective()) {
                DeltaXMLInstruction instruction = parentInstruction.getInstruction(name, false);
                if (instruction == null) {
                    log.warn("Change not handled by export: " 
                            + ExportEvent.valueOf(event.getType()) + " on " + path
                            + ". You will need to do this manually.");
                    return false;
                }
                parentInstruction.removeInstruction(instruction);
                if (!instruction.isNoneDirective()) {
                    // all we can do is remove the property override directive
                    // we can't remove the property
                    log.warn("Change not handled by export: " 
                            + ExportEvent.valueOf(event.getType()) + " on " + path
                            + ". You will need to do this manually.");
                }
                purge(parentInstruction);
            }
        } else if (event.getType() == PROPERTY_CHANGED) {
            if (parentInstruction == null) {
                assert parentPath != null;
                parentInstruction = createCombineInstruction(instruction, parentPath);
            }
            if (parentInstruction.isCombineDirective()) {
                DeltaXMLInstruction instruction = parentInstruction.getInstruction(name, false);
                if (instruction == null) {
                    // the best we can do is override properties when they change
                    // we don't have the information needed to do appends
                    // on multi-valued properties nor to remove the property override again
                    // when they become redundant because the net change is zero
                    parentInstruction.addInstruction(new DeltaXMLInstruction(false, name, "override", parentInstruction));
                }
            }
        }
        
        return true;        
    }
    
    DeltaXMLInstruction getParentInstruction(DeltaXMLInstruction context, String relPath) {
        if (context.isNoneDirective()) {
            return context;
        }
        if (context.isCombineDirective()) {
            int offset = relPath.indexOf('/');
            if (offset == -1) {
                return context;
            }
            String childName = relPath.substring(0, offset);
            String subPath = relPath.substring(offset+1);
            DeltaXMLInstruction subContext = context.getInstruction(childName, true);
            if (subContext == null) {
                return null;
            }
            return getParentInstruction(subContext, subPath);
        }
        return null;
    }
    
    DeltaXMLInstruction createCombineInstruction(DeltaXMLInstruction context, String relPath) {
        assert context.isCombineDirective();
        int offset = relPath.indexOf('/');
        DeltaXMLInstruction result = null;
        if (offset != -1) {
            String name = relPath.substring(0, offset);
            DeltaXMLInstruction child = context.getInstruction(name, true);
            if (child == null) {
                child = new DeltaXMLInstruction(true, name, "combine", context);
                context.addInstruction(child);
            }
            result = createCombineInstruction(child, relPath.substring(offset+1));
        } else {
            String name = relPath;
            result = new DeltaXMLInstruction(true, name, "combine", context);
            context.addInstruction(result);
        }
        return result;
    }
    
    boolean isEmpty() {
        return instruction.isEmpty();
    }
    
    boolean isEnabled() {
        if (isEnabled == null) {
            isEnabled = isEnabled(instruction);
        }
        return isEnabled;
    }
    
    private boolean isEnabled(DeltaXMLInstruction instruction) {
        if (instruction.isUnsupportedDirective()) {
            return false;
        }
        Collection<DeltaXMLInstruction> nodeInstructions = instruction.getNodeInstructions();
        if (nodeInstructions != null) {
            for (DeltaXMLInstruction child : nodeInstructions) {
                if (!isEnabled(child)) {
                    return false;
                }
            }
        }
        Collection<DeltaXMLInstruction> propertyInstructions = instruction.getNodeInstructions();
        if (propertyInstructions != null) {
            for (DeltaXMLInstruction child : propertyInstructions) {
                if (!isEnabled(child)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private void purge(DeltaXMLInstruction instruction) {
        assert instruction.isCombineDirective();
        if (instruction.isEmpty()) {
            DeltaXMLInstruction parentInstruction = instruction.getParentInstruction();
            if (parentInstruction != null) {
                parentInstruction.removeInstruction(instruction);
                purge(parentInstruction);
            }
        }
    }

}
