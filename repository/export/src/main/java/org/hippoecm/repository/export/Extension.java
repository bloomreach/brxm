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
package org.hippoecm.repository.export;

import static org.hippoecm.repository.export.Constants.NAME_QNAME;
import static org.hippoecm.repository.export.Constants.NODE_QNAME;
import static org.hippoecm.repository.export.Constants.PROPERTY_QNAME;
import static org.hippoecm.repository.export.Constants.TYPE_QNAME;
import static org.hippoecm.repository.export.Constants.VALUE_QNAME;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Session;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a hippoecm-extension.xml file.
 */
final class Extension {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ";

    private static final Logger log = LoggerFactory.getLogger("org.hippoecm.repository.export");
    // ---------- Member variables
    private final File file;
    private final List<Instruction> instructions;
    private final Document document;
    private volatile boolean changed = false;

    // ---------- Constructor
    Extension(File file) throws DocumentException, IOException {
        this.file = file;
        if (!file.exists()) {
            document = createDocument();
            instructions = new ArrayList<Instruction>();
            changed = true;
        } else {
            SAXReader reader = new SAXReader();
            document = reader.read(file);
            instructions = parseExtension(document.getRootElement());
        }
    }

    // ---------- API
    void export(Session session) {
        if (changed) {
            log.info("Exporting " + file.getName());
            XMLWriter writer;
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                OutputFormat format = OutputFormat.createPrettyPrint();
                format.setNewLineAfterDeclaration(false);
                writer = new XMLWriter(new FileWriter(file), format);
                writer.write(document);
                writer.flush();
            } catch (IOException e) {
                log.error("Exporting " + file.getName() + " failed.", e);
            }
            changed = false;
        }
        for (Instruction instruction : instructions) {
            if (instruction instanceof ResourceInstruction) {
                if (((ResourceInstruction)instruction).hasChanged()) {
                    ((ResourceInstruction)instruction).export(session);
                }
            }
        }
    }

    List<Instruction> getInstructions() {
        return instructions;
    }

    void setChanged() {
        changed = true;
    }

    ResourceInstruction findResourceInstruction(String path, boolean isNode) {
        return path.startsWith("/jcr:system/jcr:nodeTypes/")
                ? findNodetypesInstruction(path)
                : findContentResourceInstruction(path, isNode);
    }

    private NodetypesResourceInstruction findNodetypesInstruction(String path) {
        // Parse the path:
        // path = /jcr:system/jcr:nodeTypes/example_1_1:doctype/jcr:propertyDefinition
        // relPath = example_1_1:doctype/jcr:propertyDefinition
        // nodeTypeRoot = example_1_1:doctype
        // prefix = example_1_1
        String relPath = path.substring("/jcr:system/jcr:nodeTypes/".length());
        int indexOfPathSeparator = relPath.indexOf('/');
        String nodeTypeRoot = (indexOfPathSeparator == -1) ? relPath : relPath.substring(0, indexOfPathSeparator);
        int indexOfColon = nodeTypeRoot.indexOf(':');
        String prefix = (indexOfColon == -1) ? nodeTypeRoot : nodeTypeRoot.substring(0, indexOfColon);
        // find node types resource instruction that matches the prefix
        for (Instruction instruction : instructions) {
            if (instruction instanceof NodetypesResourceInstruction) {
                if (((NodetypesResourceInstruction)instruction).matchesPrefix(prefix)) {
                    return (NodetypesResourceInstruction)instruction;
                }
            }
        }
        return null;
    }

    private ContentResourceInstruction findContentResourceInstruction(String path, boolean isNode) {
        ContentResourceInstruction result = null;
        for (Instruction instruction : instructions) {
            if (instruction instanceof ContentResourceInstruction) {
                if (((ContentResourceInstruction)instruction).matchesPath(path)) {
                    // choose the instruction with the longest context path
                    if (result == null || ((ContentResourceInstruction)instruction).context.length()
                            > result.context.length()) {
                        result = (ContentResourceInstruction)instruction;
                    }
                }
            }
        }
        if (result != null) {
            // If the context node of the result is not the one that is required, return null
            // a new instruction needs to be created.
            String requiredContextNode = LocationMapper.contextNodeForPath(path, isNode);
            if (!result.context.equals(requiredContextNode)) {
                // But only if this would actually succeed (see createContentNodeInstruction)
                if (path.equals(requiredContextNode))
                    return null;
            }
        }
        return result;
    }

    NamespaceInstruction findNamespaceInstruction(String namespace) {
        for (Instruction instruction : instructions) {
            if (instruction instanceof NamespaceInstruction) {
                if (((NamespaceInstruction)instruction).matchesNamespace(namespace)) {
                    return (NamespaceInstruction)instruction;
                }
            }
        }
        return null;
    }

    ResourceInstruction createResourceInstruction(String path, boolean isNode) {
        boolean cnd = path.startsWith("/jcr:system/jcr:nodeTypes");
        return cnd ? createNodetypesResourceInstruction(path) : createContentResourceInstruction(path, isNode);
    }

    NamespaceInstruction createNamespaceInstruction(String uri, String prefix) throws IOException {
        int indexOfUnderscore = prefix.indexOf('_');
        prefix = (indexOfUnderscore == -1) ? prefix : prefix.substring(0, indexOfUnderscore);
        return new NamespaceInstructionImpl(prefix, 3000.0, uri, null);
    }

    private ContentResourceInstruction createContentResourceInstruction(String path, boolean isNode) {

        String contextNode = LocationMapper.contextNodeForPath(path, isNode);
        String fileForPath = LocationMapper.fileForPath(path, isNode);

        if (!path.equals(contextNode)) {
            log.warn("This change needs merge semantics. Can't create instruction.");
            return null;
        }
        // contextNode = /hippo:namespaces/example
        // name = hippo-namespaces-example
        // root = /hippo:namespaces
        int lastIndexOfPathSeparator = contextNode.lastIndexOf('/');
        String name = contextNode.substring(1).replaceAll(":", "-").replaceAll("/", "-");
        String root = contextNode.substring(0, lastIndexOfPathSeparator);
        if (root.equals(""))
            root = "/";

        return new ContentResourceInstruction(name, 3000.3, file.getParentFile(), fileForPath, root, contextNode, true, this);
    }

    private NodetypesResourceInstruction createNodetypesResourceInstruction(String path) {
        // path = /jcr:system/jcr:nodeTypes/example_1_1:doctype
        // relPath = example_1_1:doctype
        // prefix = example_1_1
        // name = example
        String relPath = path.substring("/jcr:system/jcr:nodeTypes/".length());
        int indexOfColon = relPath.indexOf(':');
        String prefix = relPath.substring(0, indexOfColon);
        int indexOfUnderscore = prefix.indexOf('_');
        String name = (indexOfUnderscore == -1) ? prefix : prefix.substring(0, indexOfUnderscore);
        String nodetypesresource = "namespaces/" + name + ".cnd";
        // ALERT: we use a convention for the node name of a node types resource instruction
        // It is the node type prefix + -nodetypes
        return new NodetypesResourceInstruction(name + "-nodetypes", 3000.1, file.getParentFile(), nodetypesresource, null, null, prefix);
    }

    void addInstruction(Instruction instruction) {
        instructions.add(instruction);
        Element element = instruction.createInstructionElement();
        document.getRootElement().add(element);
        changed = true;
    }

    @SuppressWarnings("rawtypes")
    void removeInstruction(Instruction instruction) {
        List nodes = document.getRootElement().elements();
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            Element node = (Element)iter.next();
            if (node.attributeValue("name").equals(instruction.getName())) {
                document.getRootElement().remove(node);
                break;
            }
        }
        instructions.remove(instruction);
        if (instruction instanceof ResourceInstruction) {
            ((ResourceInstruction)instruction).delete();
        }
        changed = true;
    }

    // ---------- private helpers
    /*
     * Creates empty hippoecm-extension.xml:
     * <sv:node xmlns:sv="http://www.jcp.org/jcr/sv/1.0" sv:name="hippo:initialize">
     *   <sv:property sv:name="jcr:primaryType" sv:type="Name">
     *     <sv:value>hippo:initializefolder</sv:value>
     *   </sv:property>
     * </sv:node>
     */
    private Document createDocument() {
        Document document = DocumentFactory.getInstance().createDocument();
        Element root = DocumentFactory.getInstance().createElement(NODE_QNAME);
        root.add(DocumentFactory.getInstance().createAttribute(root, NAME_QNAME, "hippo:initialize"));
        Element property = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        property.add(DocumentFactory.getInstance().createAttribute(property, NAME_QNAME, "jcr:primaryType"));
        property.add(DocumentFactory.getInstance().createAttribute(property, TYPE_QNAME, "Name"));
        Element value = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        value.setText("hippo:initializefolder");
        property.add(value);
        root.add(property);
        document.setRootElement(root);
        return document;
    }

    /*
     * Parse hippoecm-extension.xml file
     */
    @SuppressWarnings("rawtypes")
    private List<Instruction> parseExtension(Element root) throws DocumentException {
        List elements = root.elements(NODE_QNAME);
        List<Instruction> instructions = new ArrayList<Instruction>(elements.size());
        for (Iterator iter = elements.iterator(); iter.hasNext();) {
            Element element = (Element)iter.next();
            Instruction instruction;
            instruction = parseInstruction(element);
            if (instruction != null) {
                instructions.add(instruction);
            }
        }
        return new ArrayList<Instruction>(instructions);
    }

    /*
     * Create Instruction object from hippoecm-extension.xml entry
     */
    @SuppressWarnings("rawtypes")
    private Instruction parseInstruction(Element element) {
        Instruction instruction = null;
        String name = element.attributeValue(NAME_QNAME);
        String contentresource = null;
        String contentroot = "";
        String namespace = null;
        Element namespacePropertyValue = null;
        String nodetypesresource = null;
        Double sequence = 0.0;
        List properties = element.elements();
        for (Iterator iter = properties.iterator(); iter.hasNext();) {
            Element property = (Element)iter.next();
            String propName = property.attributeValue(NAME_QNAME);
            if (propName.equals("hippo:contentresource")) {
                contentresource = property.element(VALUE_QNAME).getText();
            } else if (propName.equals("hippo:contentroot")) {
                contentroot = property.element(VALUE_QNAME).getText();
            } else if (propName.equals("hippo:sequence")) {
                sequence = Double.parseDouble(property.element(VALUE_QNAME).getText());
            } else if (propName.equals("hippo:namespace")) {
                namespacePropertyValue = property.element(VALUE_QNAME);
                namespace = namespacePropertyValue.getText();
            } else if (propName.equals("hippo:nodetypesresource")) {
                nodetypesresource = property.element(VALUE_QNAME).getText();
            }

        }
        if (contentresource != null) {
            // context must be read from file, it is the contentroot plus
            // name of the root node in the content xml file
            SAXReader reader = new SAXReader();
            Document document;
            try {
                document = reader.read(new File(file.getParentFile(), contentresource));
                String context = contentroot + "/" + document.getRootElement().attributeValue(NAME_QNAME);
                // if contentresource file uses delta xml (h:merge) semantics, then disable export
                // we don't deal with that (yet)
                String mergeValue = document.getRootElement().attributeValue("merge");
                boolean enabled = !(mergeValue != null && !mergeValue.equals(""));

                instruction = new ContentResourceInstruction(name, sequence, file.getParentFile(), contentresource, contentroot, context, enabled, this);
            } catch (DocumentException e) {
                log.error("Failed to read contentresource file " + contentresource + " as xml. Can't create instruction.", e);
            }
        } else if (nodetypesresource != null) {
            // name = example-nodetypes
            // prefix = example
            int indexOfDash = name.indexOf("-nodetypes");
            String prefix = (indexOfDash == -1) ? name : name.substring(0, indexOfDash);
            instruction = new NodetypesResourceInstruction(name, sequence, file.getParentFile(), nodetypesresource, namespace, namespacePropertyValue, prefix);
        } else if (namespace != null) {
            instruction = new NamespaceInstructionImpl(name, sequence, namespace, namespacePropertyValue);
        }
        return instruction;
    }
}
