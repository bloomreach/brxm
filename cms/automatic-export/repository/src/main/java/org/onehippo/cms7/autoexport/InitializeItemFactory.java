/*
 *  Copyright 2011 Hippo.
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


import java.io.File;
import java.util.Collection;

import javax.jcr.observation.Event;

class InitializeItemFactory {
    
    private final File exportDir;
    private final InitializeItemRegistry registry;
    private final String namePrefix;
    private final Module module;
    
    InitializeItemFactory(Module module, InitializeItemRegistry registry, String namePrefix) {
        this.module = module;
        if (module != null) {
            exportDir = module.getExportDir();
        } else {
            exportDir = null;
        }
        this.registry = registry;
        this.namePrefix = namePrefix == null || namePrefix.isEmpty() ? "" : namePrefix + "-";
    }
    
    InitializeItem createInitializeItem(String path, int eventType) {
        String name = null;
        Double sequence = null;
        String nodeTypesResource = null;
        String contentResource = null;
        String contentRoot = null;
        String contextPath = null;
        if (path.startsWith("/jcr:system/jcr:nodeTypes")) {
            // path = /jcr:system/jcr:nodeTypes/example_1_1:doctype
            // prefix = example
            // name = example-nodetypes
            // nodeTypesResource = namespaces/example.cnd
            String relPath = path.substring("/jcr:system/jcr:nodeTypes/".length());
            int offset = relPath.indexOf(':');
            String internalPrefix = relPath.substring(0, offset);
            offset = internalPrefix.indexOf('_');
            String prefix = (offset == -1) ? internalPrefix : internalPrefix.substring(0, offset);
            nodeTypesResource = "namespaces/" + prefix + ".cnd";
            name = prefix + "-nodetypes";
            sequence = 30000.1;
        }
        else {
            boolean isNode = eventType == Event.NODE_ADDED || eventType == Event.NODE_REMOVED;
            contextPath = LocationMapper.contextNodeForPath(path, isNode);
            contentResource = LocationMapper.fileForPath(path, isNode);
            
            // contextNode = /hippo:namespaces/example
            // name = ${extension.id}/hippo-namespaces-example
            // root = /hippo:namespaces
            name = namePrefix + contextPath.substring(1).replaceAll(":", "-").replaceAll("/", "-");
            int offset = contextPath.lastIndexOf('/');
            contentRoot = contextPath.substring(0, offset);
            if (contentRoot.equals("")) {
                contentRoot = "/";
            }
            
            sequence = 30000.3;
            // find the parent content resource instruction to determine the sequence number
            Collection<InitializeItem> parents = registry.getInitializeItemsByPath(contentRoot, eventType);
            if (parents != null && !parents.isEmpty()) {
                double highestParentSequence = -1;
                for (InitializeItem parent : parents) {
                    highestParentSequence = (parent.getSequence() > highestParentSequence) ? parent.getSequence() : highestParentSequence;
                }
                sequence = highestParentSequence + 1;
            }
        }
        
        return new InitializeItem(name, sequence, contentResource, contentRoot, contextPath, nodeTypesResource, null, exportDir, module);
    }
    
    InitializeItem createInitializeItem(String namespace, String internalPrefix) {
        int offset = internalPrefix.indexOf('_');
        String prefix = offset == -1 ? internalPrefix : internalPrefix.substring(0, offset);
        return new InitializeItem(prefix, 30000.0, null, null, null, null, namespace, exportDir, module);
    }

}
