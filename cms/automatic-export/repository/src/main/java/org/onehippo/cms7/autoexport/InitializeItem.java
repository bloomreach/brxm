/*
 *  Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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
import java.io.IOException;

import javax.jcr.observation.Event;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sf.json.JSONObject;
import static org.onehippo.cms7.autoexport.AutoExportModule.log;
import static org.onehippo.cms7.autoexport.Constants.DELTA_URI;
import static org.onehippo.cms7.autoexport.Constants.FILE;
import static org.onehippo.cms7.autoexport.Constants.MERGE;
import static org.onehippo.cms7.autoexport.Constants.NAME;
import static org.onehippo.cms7.autoexport.Constants.QNAME;
import static org.onehippo.cms7.autoexport.Constants.QNODE;
import static org.onehippo.cms7.autoexport.Constants.SV_URI;

final class InitializeItem {
    
    private final String name;
    private final Double sequence;
    private final String contentResource;
    private final String contentRoot;
    private final String nodeTypesResource;
    private final String namespace;
    private String resourceBundles;
    private final File exportDir;
    private final Module module;
    
    private String contextPath;
    private Boolean isDelta;
    private Boolean enabled;
    private Delta delta;
    
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
        if (contentResource != null && contentResource.endsWith(".zip")) {
            enabled = false;
        }
    }

    InitializeItem(String name, Double sequence,
            String contentResource, String contentRoot,
            String contextPath, String nodeTypesResource,
            String namespace, String resourceBundles,
            File exportDir, Module module) {
        this.name = name;
        this.sequence = sequence;
        this.contentResource = contentResource;
        this.contentRoot = contentRoot;
        this.contextPath = contextPath;
        this.nodeTypesResource = nodeTypesResource;
        this.namespace = namespace;
        this.resourceBundles = resourceBundles;
        this.exportDir = exportDir;
        this.module = module;
        if (contentResource != null && contentResource.endsWith(".zip")) {
            enabled = false;
        }
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

    String getResourceBundles() {
        return resourceBundles;
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
    
    boolean isDelta() {
        if (isDelta == null && contentResource != null) {
            initContentResourceValues();
        } else if (isDelta == null && resourceBundles != null) {
            initResourceBundlesValues();
        }
        return isDelta == null ? false : isDelta;
    }
    
    boolean isEnabled() {
        if (enabled == null && contentResource != null) {
            initContentResourceValues();
        } else if (enabled == null && resourceBundles != null) {
            initResourceBundlesValues();
        }
        if (enabled == null) {
            enabled = true;
        }
        return enabled;
    }
    
    Delta getDelta() {
        return delta;
    }
    
    void handleEvent(ExportEvent event) {
        if (getContentResource() == null && getResourceBundles() == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        if (isDelta == null) {
            isDelta = !event.getPath().equals(contextPath) || resourceBundles != null;
        }
        String contextPath = this.contextPath != null ? this.contextPath : "/hippo:configuration/hippo:translations";
        if (isDelta) {
            if (delta == null) {
                delta = new Delta(contextPath);
            }
            delta.handleEvent(event);
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
        if (isDelta()) {
            return delta.processEvents();
        }
        return true;
    }
    
    
    boolean isEmpty() {
        if (isDelta()) {
            return delta == null || delta.isEmpty();
        }
        return contextNodeRemoved;
    }

    private void initResourceBundlesValues() {
        File file = new File(exportDir, resourceBundles);
        if (!file.exists()) {
            return;
        }
        try {
            final JSONObject o = JSONObject.fromObject(FileUtils.readFileToString(file));
            delta = parseDelta(o);
            isDelta = true;
            enabled = true;
        } catch (IOException e) {
            log.error("Failed to parse resource bundles file", e);
        }
    }

    private Delta parseDelta(final JSONObject o) {
        final DeltaInstruction rootInstruction = new DeltaInstruction(true, "hippo:translations", "combine", "/hippo:configuration/hippo:translations");
        parseInstruction(o, rootInstruction);
        return new Delta("/hippo:configuration/hippo:translations", rootInstruction);
    }

    private void parseInstruction(final JSONObject o, final DeltaInstruction parentInstruction) {
        for (Object key : o.keySet()) {
            final Object value = o.get(key);
            if (value instanceof JSONObject) {
                DeltaInstruction instruction = new DeltaInstruction(true, key.toString(), "combine", parentInstruction);
                parseInstruction((JSONObject) value, instruction);
                parentInstruction.addInstruction(instruction);
            }
            if (value instanceof String) {
                DeltaInstruction instruction = new DeltaInstruction(false, key.toString(), "override", parentInstruction);
                parentInstruction.addInstruction(instruction);
            }
        }
    }

    private void initContentResourceValues() {
        if (contentResource.endsWith(".zip")) {
            return;
        }
        File file = new File(exportDir, contentResource);
        if (!file.exists()) {
            return;
        }
        // context must be read from file, it is the contentroot plus
        // name of the root node in the content xml file
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        try {
            final DocumentBuilder builder = dbf.newDocumentBuilder();
            Document document = builder.parse(file);

            String contextNodeName = document.getDocumentElement().getAttributeNS(SV_URI, NAME);
            contextPath = contentRoot.equals("/") ? "/" + contextNodeName : contentRoot + "/" + contextNodeName;
            
            String directive = document.getDocumentElement().getAttributeNS(DELTA_URI, MERGE);
            isDelta = directive != null && !directive.equals("");
            
            if (isDelta) {
                delta = parseDelta(document);
                if (delta == null) {
                    log.info("Content resource " + contentResource + " uses delta xml semantics that are not supported " +
                            "by automatic export. Changes to the context " + contextPath + " must be exported manually.");
                    enabled = false;
                }
            }
            if (enabled != Boolean.FALSE && containsFileReferenceValues(document)) {
                log.info("Content resource " + contentResource + " uses external file reference values. " +
                        "This is not supported by automatic export. Changes to the context " + contextPath
                        + " must be exported manually.");
                enabled = false;
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error("Failed to read content resource " + contentResource + " as xml.", e);
        }
    }
    
    private Delta parseDelta(Document document) {
        DeltaInstruction instruction = parseInstructionElement(document.getDocumentElement(), null);
        if (instruction == null) {
            return null;
        }
        return new Delta(contextPath, instruction);
        
    }
    
    private DeltaInstruction parseInstructionElement(Element element, DeltaInstruction parent) {
        
        boolean isNode = element.getTagName().equals(QNODE);
        String name = element.getAttribute(QNAME);
        String directive = element.getAttributeNS(DELTA_URI, MERGE);

        DeltaInstruction instruction;
        if (parent == null) {
            instruction = new DeltaInstruction(isNode, name, directive, contextPath);
        } else {
            instruction = new DeltaInstruction(isNode, name, directive, parent);
        }
        if (instruction.isCombineDirective()) {
            final NodeList childNodes = element.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node item = childNodes.item(i);
                if (item instanceof Element) {
                    DeltaInstruction child = parseInstructionElement((Element) item, instruction);
                    if (child == null) {
                        return null;
                    }
                    instruction.addInstruction(child);
                }
            }
        }
        if (instruction.isUnsupportedDirective()) {
            return null;
        }
        return instruction;
    }

    private boolean containsFileReferenceValues(Document document) {
        return containsFileReferenceValues(document.getDocumentElement());
    }

    private boolean containsFileReferenceValues(final Element element) {
        if (element.hasAttributeNS(DELTA_URI, FILE)) {
            return true;
        }
        final NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node item = childNodes.item(i);
            if (item instanceof Element) {
                if (containsFileReferenceValues((Element) item)) {
                    return true;
                }
            }
        }
        return false;
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
            sb.append(", isDelta = ");
            sb.append(isDelta);
            sb.append("]");
            stringValue = sb.toString();
        }
        return stringValue;
    }
    
}
