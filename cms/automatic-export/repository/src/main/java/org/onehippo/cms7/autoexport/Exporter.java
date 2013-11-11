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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrCompactNodeTypeDefWriter;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import static org.onehippo.cms7.autoexport.AutoExportModule.log;
import static org.onehippo.cms7.autoexport.Constants.CDATA;
import static org.onehippo.cms7.autoexport.Constants.DELTA_PREFIX;
import static org.onehippo.cms7.autoexport.Constants.DELTA_URI;
import static org.onehippo.cms7.autoexport.Constants.MERGE;
import static org.onehippo.cms7.autoexport.Constants.NAME;
import static org.onehippo.cms7.autoexport.Constants.NODE;
import static org.onehippo.cms7.autoexport.Constants.PROPERTY;
import static org.onehippo.cms7.autoexport.Constants.QMERGE;
import static org.onehippo.cms7.autoexport.Constants.QNAME;
import static org.onehippo.cms7.autoexport.Constants.QNODE;
import static org.onehippo.cms7.autoexport.Constants.QPROPERTY;
import static org.onehippo.cms7.autoexport.Constants.QTYPE;
import static org.onehippo.cms7.autoexport.Constants.QVALUE;
import static org.onehippo.cms7.autoexport.Constants.SV_PREFIX;
import static org.onehippo.cms7.autoexport.Constants.SV_URI;
import static org.onehippo.cms7.autoexport.Constants.TYPE;
import static org.onehippo.cms7.autoexport.Constants.VALUE;

final class Exporter {

    private final Module module;
    private final Session session;
    private final InitializeItemRegistry registry;
    private final Configuration configuration;
    private final List<String> subModuleExclusionPatterns;
    
    private Set<InitializeItem> export = new HashSet<InitializeItem>();
    private Set<InitializeItem> delete = new HashSet<InitializeItem>();
    
    Exporter(Module module, Session session, InitializeItemRegistry registry, Configuration configuration) {
        this.module = module;
        this.session = session;
        this.registry = registry;
        this.configuration = configuration;
        subModuleExclusionPatterns = ExportUtils.getSubModuleExclusionPatterns(configuration, module);
    }
    
    void scheduleForExport(InitializeItem item) {
        export.add(item);
    }
    
    void scheduleForDeletion(InitializeItem item) {
        delete.add(item);
    }

    void export() {
        for (InitializeItem item : delete) {
            delete(item);
        }
        delete.clear();
        for (InitializeItem item : export) {
            export(item);
        }
        export.clear();
    }
    
    private void export(InitializeItem item) {
        if (item.getContentResource() != null) {
            exportContentResource(item);
        }
        if (item.getNodeTypesResource() != null) {
            exportNodeTypesResource(item);
        }
    }
    
    private void exportContentResource(InitializeItem item) {
        log.info("Exporting " + item.getContentResource() + " to module " + module.getModulePath());
        try {
            doExportContentResource(item);
        } catch (RepositoryException e) {
            // concurrent modifications may have caused this exception
            // so try once more
            try {
                session.refresh(false);
                doExportContentResource(item);
            } catch (RepositoryException e1) {
                log.error("Exporting " + item.getContentResource() + " failed.", e);
            }
        }
    }

    private void doExportContentResource(final InitializeItem item) throws RepositoryException {
        OutputStream out = null;
        try {
            File file = new File(module.getExportDir(), item.getContentResource());
            if (!file.exists()) {
                ExportUtils.createFile(file);
            }
            out = new FileOutputStream(file);
            TransformerHandler handler = ((SAXTransformerFactory)SAXTransformerFactory.newInstance()).newTransformerHandler();
            Transformer transformer = handler.getTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
            handler.setResult(new StreamResult(out));

            if (item.isDeltaXML()) {
                exportDeltaXML(item, handler);
            } else {
                exportDereferencedView(item, handler);
            }
        } catch (IOException e) {
            log.error("Exporting " + item.getContentResource() + " failed.", e);
        } catch (TransformerConfigurationException e) {
            log.error("Exporting " + item.getContentResource() + " failed.", e);
        } catch (SAXException e) {
            log.error("Exporting " + item.getContentResource() + " failed.", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private void exportDereferencedView(InitializeItem item, ContentHandler handler) throws FileNotFoundException, TransformerConfigurationException, SAXException, RepositoryException {
        List<String> subContextPaths = new ArrayList<String>();
        for (InitializeItem child : registry.getDescendentInitializeItems(item.getContextPath())) {
            subContextPaths.add(child.getContextPath());
        }
        ExclusionContext exclusionContext = new ExclusionContext(configuration.getExclusionContext(), subModuleExclusionPatterns);
        ContentHandler filter = new FilterContentHandler(handler, item.getContentRoot(), subContextPaths, configuration.getFilterUuidPaths(), exclusionContext);
        ((HippoSession) session).exportDereferencedView(item.getContextPath(), filter, false, false);
    }
    
    private void exportDeltaXML(InitializeItem item, ContentHandler handler) throws SAXException, RepositoryException {
        if (!item.isEnabled()) {
            log.warn("Export in this context is disabled: " + item.getContextPath() + "You need to do this manually.");
            return;
        }
        
        DeltaXMLInstruction rootInstruction = item.getDeltaXML().getRootInstruction();
        handler.startDocument();
        handler.startPrefixMapping(SV_PREFIX, SV_URI);
        handler.startPrefixMapping(DELTA_PREFIX, DELTA_URI);
        exportInstruction(rootInstruction, handler);
        handler.endDocument();
    }
    
    private void exportInstruction(DeltaXMLInstruction instruction, ContentHandler handler) throws SAXException, RepositoryException {
        if (instruction.isNoneDirective()) {
            if (instruction.isNodeInstruction()) {
                List<String> subContextPaths = new ArrayList<String>();
                for (InitializeItem child : registry.getDescendentInitializeItems(instruction.getContextPath())) {
                    subContextPaths.add(child.getContextPath());
                }
                ExclusionContext exclusionContext = new ExclusionContext(configuration.getExclusionContext(), subModuleExclusionPatterns);
                ContentHandler filter = new FilterContentHandler(new EmbeddedContentHandler(handler), instruction.getParentPath(), subContextPaths, configuration.getFilterUuidPaths(), exclusionContext);
                ((HippoSession) session).exportDereferencedView(instruction.getContextPath(), filter, false, false);
            } else {
                exportPropertyInstruction(instruction, handler, false);
            }
        } else if (instruction.isCombineDirective()) {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute(SV_URI, NAME, QNAME, CDATA, instruction.getName());
            attr.addAttribute(DELTA_URI, MERGE, QMERGE, CDATA, instruction.getDirective());
            handler.startElement(SV_URI, NODE, QNODE, attr);
            if (instruction.getPropertyInstructions() != null) {
                for (DeltaXMLInstruction child : instruction.getPropertyInstructions()) {
                    exportInstruction(child, handler);
                }
            }
            if (instruction.getNodeInstructions() != null) {
                for (DeltaXMLInstruction child : instruction.getNodeInstructions()) {
                    exportInstruction(child, handler);
                }
            }
            handler.endElement(SV_URI, NODE, QNODE);
        } else if (instruction.isOverrideDirective()) {
            exportPropertyInstruction(instruction, handler, true);
        }
    }

    private void exportPropertyInstruction(DeltaXMLInstruction instruction, ContentHandler handler, boolean override) throws SAXException, RepositoryException {
        Property property = session.getProperty(instruction.getContextPath());
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute(SV_URI, NAME, QNAME, CDATA, instruction.getName());
        attr.addAttribute(SV_URI, TYPE, QTYPE, CDATA, PropertyType.nameFromValue(property.getType()));
        if (override) {
            attr.addAttribute(DELTA_URI, MERGE, QMERGE, CDATA, "override");
        }
        if (property.isMultiple()) {
            attr.addAttribute(SV_URI, "multiple", "sv:multiple", CDATA, "true");
        }
        handler.startElement(SV_URI, PROPERTY, QPROPERTY, attr);
        attr = new AttributesImpl();
        if (property.isMultiple()) {
            for (Value value : property.getValues()) {
                handler.startElement(SV_URI, VALUE, QVALUE, attr);
                String stringValue = value.getString();
                handler.characters(stringValue.toCharArray(), 0, stringValue.length());
                handler.endElement(SV_URI, VALUE, QVALUE);
            }
        } else {
            Value value = property.getValue();
            handler.startElement(SV_URI, VALUE, QVALUE, attr);
            String stringValue = value.getString();
            handler.characters(stringValue.toCharArray(), 0, stringValue.length());
            handler.endElement(SV_URI, VALUE, QVALUE);
        }
        handler.endElement(SV_URI, PROPERTY, QPROPERTY);
    } 
    
    private void exportNodeTypesResource(InitializeItem item) {
        log.info("Exporting " + item.getNodeTypesResource() + " to module " + module.getModulePath());
        try {
            File file = new File(module.getExportDir(), item.getNodeTypesResource());
            if (!file.exists()) {
                ExportUtils.createFile(file);
            }
            Writer out = new FileWriter(file);
            try {
                String cnd = null;
                cnd = JcrCompactNodeTypeDefWriter.compactNodeTypeDef(session.getWorkspace(), ExportUtils.prefixFromName(item.getName()));
                out.write(cnd);
                out.flush();
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        } catch (IOException e) {
            log.error("Exporting " + item.getNodeTypesResource() + " failed.", e);
        } catch (RepositoryException e) {
            log.error("Exporting " + item.getNodeTypesResource() + " failed.", e);
        }
    }
    
    private void delete(InitializeItem item) {
        if (item.getContentResource() != null) {
            File file = new File(module.getExportDir(), item.getContentResource());
            log.info("Deleting " + item.getContentResource() + " in module " + module.getModulePath());
            file.delete();
            deleteIfEmpty(file.getParentFile());
        }
        if (item.getNodeTypesResource() != null) {
            File file = new File(module.getExportDir(), item.getNodeTypesResource());
            log.info("Deleting " + item.getNodeTypesResource() + " in module " + module.getModulePath());
            file.delete();
            deleteIfEmpty(file.getParentFile());
        }
    }
    
    private void deleteIfEmpty(File directory) {
        if (directory.list().length == 0) {
            directory.delete();
            deleteIfEmpty(directory.getParentFile());
        }
    }

    /**
     *  ContentHandler wrapper that swallows startDocument, endDocument, startPrefixMapping and endPrefixMapping
     *  events for embedded xml export. Used for exporting fragments in delta xmls.
     */
    private static final class EmbeddedContentHandler implements ContentHandler {

        private ContentHandler handler;
        
        private EmbeddedContentHandler(ContentHandler handler) {
            this.handler = handler;
        }
        
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            handler.characters(ch, start, length);
        }

        @Override
        public void endDocument() throws SAXException {
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            handler.endElement(uri, localName, qName);
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            handler.ignorableWhitespace(ch, start, length);
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
            handler.processingInstruction(target, data);
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            handler.setDocumentLocator(locator);
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
            handler.skippedEntity(name);
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            handler.startElement(uri, localName, qName, atts);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }
        
    }

}
