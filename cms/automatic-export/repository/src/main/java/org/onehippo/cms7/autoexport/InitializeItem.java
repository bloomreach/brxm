/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.observation.Event;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import static org.onehippo.cms7.autoexport.AutoExportModule.log;
import static org.onehippo.cms7.autoexport.Constants.MERGE_QNAME;
import static org.onehippo.cms7.autoexport.Constants.NAME_QNAME;

final class InitializeItem {
    
    private final String name;
    private final Double sequence;
    private final String contentResource;
    private final String contentRoot;
    private final String nodeTypesResource;
    private final String namespace;
    private final File exportDir;
    private final Module module;
    
    private String contextPath;
    private Boolean isDeltaXML;
    private Boolean enabled;
    private DeltaXML deltaXML;
    
    private boolean contextNodeRemoved = false;
    
    private String stringValue;
    
    InitializeItem(String name, Double sequence, 
            String contentResource, String contentRoot, 
            String contextPath, String nodeTypesResource, 
            String namespace, File exportDir,
            Module module) {
        this.name = name;
        this.sequence = sequence;
        this.contentResource = contentResource;
        this.contentRoot = contentRoot;
        this.contextPath = contextPath;
        this.nodeTypesResource = nodeTypesResource;
        this.namespace = namespace;
        this.exportDir = exportDir;
        this.module = module;
    }

    // Constructor for testing
    InitializeItem(String name, Boolean enabled, Module module) {
        this(name, -1d, null, null, null, null, null, null, module);
        this.enabled = enabled;
    }
    
    String getName() {
        return name;
    }

    Double getSequence() {
        return sequence;
    }
    
    String getContentResource() {
        return contentResource;
    }
    
    String getContentRoot() {
        return contentRoot;
    }
    
    String getNodeTypesResource() {
        return nodeTypesResource;
    }
    
    String getNamespace() {
        return namespace;
    }
    
    String getContextPath() {
        if (contextPath == null && contentResource != null) {
            initContentResourceValues();
        }
        return contextPath;
    }
    
    String getContextNodeName() {
        String contextPath = getContextPath();
        if (contextPath != null) {
            int offset = contextPath.lastIndexOf('/');
            return contextPath.substring(offset+1);
        }
        return null;
    }
    
    Module getModule() {
        return module;
    }
    
    void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    boolean isDeltaXML() {
        if (isDeltaXML == null && contentResource != null) {
            initContentResourceValues();
        }
        return isDeltaXML == null ? false : isDeltaXML;
    }
    
    boolean isEnabled() {
        if (enabled == null && contentResource != null) {
            initContentResourceValues();
        }
        if (enabled == null) {
            enabled = true;
        }
        return enabled;
    }
    
    DeltaXML getDeltaXML() {
        return deltaXML;
    }
    
    void handleEvent(ExportEvent event) {
        if (getContentResource() == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        if (isDeltaXML == null) {
            isDeltaXML = !event.getPath().equals(contextPath);
        }
        if (isDeltaXML) {
            if (deltaXML == null) {
                deltaXML = new DeltaXML(contextPath);
            }
            deltaXML.handleEvent(event);
        }
        if (event.getType() == Event.NODE_REMOVED && contextPath.startsWith(event.getPath())) {
            contextNodeRemoved = true;
        }
        else if (event.getType() == Event.NODE_ADDED && contextPath.equals(event.getPath())) {
            contextNodeRemoved = false;
        }
    }
    
    boolean processEvents() {
        if (!isEnabled()) {
            return false;
        }
        if (isDeltaXML()) {
            return deltaXML.processEvents();
        }
        return true;
    }
    
    
    boolean isEmpty() {
        if (isDeltaXML()) {
            return deltaXML == null || deltaXML.isEmpty();
        }
        return contextNodeRemoved;
    }
    
    private void initContentResourceValues() {
        File file = new File(exportDir, contentResource);
        if (!file.exists()) {
            return;
        }
        // context must be read from file, it is the contentroot plus
        // name of the root node in the content xml file
        SAXReader reader = new SAXReader();
        Document document;
        try {
            document = reader.read(file);
            
            String contextNodeName = document.getRootElement().attributeValue(NAME_QNAME);
            contextPath = contentRoot.equals("/") ? "/" + contextNodeName : contentRoot + "/" + contextNodeName;
            
            String directive = document.getRootElement().attributeValue(MERGE_QNAME);
            isDeltaXML = directive != null && !directive.equals("");
            
            if (isDeltaXML) {
                deltaXML = parseDeltaXML(document);
                if (deltaXML == null) {
                    log.info("Content resource " + contentResource + " uses delta xml semantics that are not supported " +
                            "by automatic export. Changes to the context " + contextPath + " must be exported manually.");
                    enabled = false;
                }
            }
        }
        catch (DocumentException e) {
            log.error("Failed to read content resource " + contentResource + " as xml.", e);
        }
    }
    
    private DeltaXML parseDeltaXML(Document document) {
        DeltaXMLInstruction instruction = parseInstructionElement(document.getRootElement(), null);
        if (instruction == null) {
            return null;
        }
        return new DeltaXML(contextPath, instruction);
        
    }
    
    private DeltaXMLInstruction parseInstructionElement(Element element, DeltaXMLInstruction parent) {
        
        boolean isNode = element.getName().equals("node");
        String name = element.attributeValue(NAME_QNAME);
        String directive = element.attributeValue(MERGE_QNAME);

        DeltaXMLInstruction instruction = null;
        if (parent == null) {
            instruction = new DeltaXMLInstruction(isNode, name, directive, contextPath);
        } else {
            instruction = new DeltaXMLInstruction(isNode, name, directive, parent);
        }
        if (instruction.isCombineDirective()) {
            for (Object o : element.elements()) {
                DeltaXMLInstruction child = parseInstructionElement((Element) o, instruction);
                if (child == null) {
                    return null;
                }
                instruction.addInstruction(child);
            }
        }
        if (instruction.isUnsupportedDirective()) {
            return null;
        }
        return instruction;
    }
    
    @Override
    public String toString() {
        if (stringValue == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("InitializeItem [name = ");
            sb.append(name);
            sb.append(", sequence = ");
            sb.append(sequence);
            sb.append(", contentResource = ");
            sb.append(contentResource);
            sb.append(", contentRoot = ");
            sb.append(contentRoot);
            sb.append(", nodeTypesResource = ");
            sb.append(nodeTypesResource);
            sb.append(", contextPath = ");
            sb.append(contextPath);
            sb.append(", namespace = ");
            sb.append(namespace);
            sb.append(", isDeltaXML = ");
            sb.append(isDeltaXML);
            sb.append("]");
            stringValue = sb.toString();
        }
        return stringValue;
    }
    
}
