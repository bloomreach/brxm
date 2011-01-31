/*
 *  Copyright 2011 Hippo (www.hippo.nl).
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.impl.SessionDecorator;
import org.hippoecm.repository.util.JcrCompactNodeTypeDefWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;


class Extension {

	
	// ---------- Constants
	
    private static final Namespace JCR_NAMESPACE = new Namespace("sv", "http://www.jcp.org/jcr/sv/1.0");
    private static final QName NAME_QNAME = new QName("name", JCR_NAMESPACE);
    private static final QName TYPE_QNAME = new QName("type", JCR_NAMESPACE);
    private static final QName NODE_QNAME = new QName("node", JCR_NAMESPACE);
    private static final QName PROPERTY_QNAME = new QName("property", JCR_NAMESPACE);
    private static final QName VALUE_QNAME = new QName("value", JCR_NAMESPACE);

    private static final Logger log = LoggerFactory.getLogger("org.hippoecm.repository.export");
    
    // ---------- Member variables
    
	private final File m_file;
    private final List<Instruction> m_instructions;

    private boolean m_changed = false;
    private Document m_document;

    
    // ---------- Constructor
    
    Extension(File file) throws DocumentException, IOException {
        m_file = file;
        m_instructions = new ArrayList<Instruction>(10);
        if (!m_file.exists()) {
            m_file.createNewFile();
            createDocument();
            m_changed = true;
        } else {
            SAXReader reader = new SAXReader();
            m_document = reader.read(m_file);
            parseExtension(m_document.getRootElement());
        }
    }
        
    
    // ---------- API

    boolean hasChanged() {
    	return m_changed;
    }

    synchronized void export() {
    	log.debug("Exporting " + m_file.getName());
        XMLWriter writer;
		try {
			writer = new XMLWriter(new FileWriter(m_file), new OutputFormat("  ", true, "UTF-8"));
	        writer.write(m_document);
	        writer.flush();
		} catch (IOException e) {
			log.error("Exporting " + m_file.getName() + " failed.", e);
		}
        m_changed = false;
    }
    
    List<Instruction> getInstructions() {
    	return m_instructions;
    }
    
    ResourceInstruction findResourceInstruction(String path) {
    	return path.startsWith("/jcr:system/jcr:nodeTypes/")
    		? findNodetypesInstruction(path)
    		: findContentResourceInstruction(path);
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
		for (Instruction instruction : m_instructions) {
            if (instruction instanceof NodetypesResourceInstruction) {
                if (((NodetypesResourceInstruction) instruction).matchesPrefix(prefix)) {
                    return (NodetypesResourceInstruction) instruction;
                }
            }
        }
		return null;
    }
    
    private ContentResourceInstruction findContentResourceInstruction(String path) {
        for (Instruction instruction : m_instructions) {
            if (instruction instanceof ContentResourceInstruction) {
                if (((ContentResourceInstruction) instruction).matchesPath(path)) {
                    return (ContentResourceInstruction) instruction;
                }
            }
        }
        return null;
    }
    
    NamespaceInstruction findNamespaceInstruction(String namespace) {
        for (Instruction instruction : m_instructions) {
            if (instruction instanceof NamespaceInstruction) {
            	if (((NamespaceInstruction) instruction).matchesNamespace(namespace)) {
            		return (NamespaceInstruction) instruction;
            	}
            }
        }
        return null;
    }

    ResourceInstruction createResourceInstruction(String path) {
        boolean cnd = path.startsWith("/jcr:system/jcr:nodeTypes");
    	return cnd ? createNodetypesResourceInstruction(path) : createContentResourceInstruction(path);
    }
    
    NamespaceInstruction createNamespaceInstruction(String uri, String prefix) throws IOException {
    	int indexOfUnderscore = prefix.indexOf('_');
    	prefix = (indexOfUnderscore == -1) ? prefix : prefix.substring(0, indexOfUnderscore);
    	return new NamespaceInstruction(prefix, 3000.0, uri);
    }

    private ContentResourceInstruction createContentResourceInstruction(String path) {
    	// path = /hippo:namespaces/example
    	// name = example-content
    	// root = /hippo:namespaces
        int lastIndexOfPathSeparator = path.lastIndexOf('/');
        String name = path.substring(lastIndexOfPathSeparator+1) + "-content";
        String root = path.substring(0, lastIndexOfPathSeparator);
        if (root.equals("")) root = "/";
        File file = new File(m_file.getParent(), name.replace(':', '$') + ".xml");
        return new ContentResourceInstruction(name, 3000.3, file, root, path, true);
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
        File file = new File(m_file.getParent(), name + ".cnd");
        // ALERT: we use a convention for the node name of a node types resource instruction
        // It is the node type prefix + -nodetypes
    	return new NodetypesResourceInstruction(name + "-nodetypes", 3000.1, file, null, prefix);
    }
    
    void addInstruction(Instruction instruction) {
        m_instructions.add(instruction);
        Element element = instruction.createInstructionElement();
        m_document.getRootElement().add(element);
        m_changed = true;
    }

    @SuppressWarnings("rawtypes")
    void removeInstruction(Instruction instruction) {
		List nodes = m_document.getRootElement().elements();
    	for (Iterator iter = nodes.iterator(); iter.hasNext();) {
    		Element node = (Element)iter.next();
    		if (node.attributeValue("name").equals(instruction.m_name)) {
    			m_document.getRootElement().remove(node);
    			break;
    		}
    	}
    	m_instructions.remove(instruction);
    	if (instruction instanceof ResourceInstruction) {
    		((ResourceInstruction) instruction).delete();
    	}
    	m_changed = true;
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
    private void createDocument() {
        m_document = DocumentFactory.getInstance().createDocument();
        Element root = DocumentFactory.getInstance().createElement(NODE_QNAME);
        root.add(DocumentFactory.getInstance().createAttribute(root, NAME_QNAME, "hippo:initialize"));
        Element property = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        property.add(DocumentFactory.getInstance().createAttribute(property, NAME_QNAME, "jcr:primaryType"));
        property.add(DocumentFactory.getInstance().createAttribute(property, TYPE_QNAME, "Name"));
        Element value = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        value.setText("hippo:initializefolder");
        property.add(value);
        root.add(property);
        m_document.setRootElement(root);
    }
    
    /*
     * Parse hippoecm-extension.xml file
     */
    @SuppressWarnings("rawtypes")
    private void parseExtension(Element root) throws DocumentException {
		List elements = root.elements(NODE_QNAME);
        for (Iterator iter = elements.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            Instruction instruction;
            instruction = parseInstruction(element);
            if (instruction != null) {
                m_instructions.add(instruction);
            }
        }
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
        String nodetypesresource = null;
        Double sequence = 0.0;
        List properties = element.elements();
        for (Iterator iter = properties.iterator(); iter.hasNext();) {
            Element property = (Element) iter.next();
            String propName = property.attributeValue(NAME_QNAME);
            if (propName.equals("hippo:contentresource")) {
                contentresource = property.element(VALUE_QNAME).getText();
            }
            else if (propName.equals("hippo:contentroot")) {
                contentroot = property.element(VALUE_QNAME).getText();
            }
            else if (propName.equals("hippo:sequence")) {
                sequence = Double.parseDouble(property.element(VALUE_QNAME).getText());
            }
            else if (propName.equals("hippo:namespace")) {
            	namespace = property.element(VALUE_QNAME).getText();
            }
            else if (propName.equals("hippo:nodetypesresource")) {
            	nodetypesresource = property.element(VALUE_QNAME).getText();
            }
            
        }
        if (contentresource != null) {
            File file = new File(m_file.getParent(), contentresource);
            // context must be read from file, it is the contentroot plus
            // name of the root node in the content xml file
            SAXReader reader = new SAXReader();
            Document document;
			try {
				document = reader.read(file);
	            String context = contentroot + "/" + document.getRootElement().attributeValue(NAME_QNAME);
	            // if contentresource file uses delta xml (h:merge) semantics, then disable export
	            // we don't deal with that (yet)
	            String mergeValue = document.getRootElement().attributeValue("merge");
	            boolean enabled = !(mergeValue != null && !mergeValue.equals(""));
	            instruction = new ContentResourceInstruction(name, sequence, file, contentroot, context, enabled);
			} catch (DocumentException e) {
				log.error("Failed to read contentresource file as xml. Can't create instruction.", e);
			}
        }
        else if (nodetypesresource != null) {
        	File file = new File(m_file.getParent(), nodetypesresource);
        	// name = example-nodetypes
        	// prefix = example
        	int indexOfDash = name.indexOf("-nodetypes");
        	String prefix = (indexOfDash == -1) ? name : name.substring(0, indexOfDash);
        	instruction = new NodetypesResourceInstruction(name, sequence, file, namespace, prefix);
        }
        else if (namespace != null) {
        	instruction = new NamespaceInstruction(name, sequence, namespace);
        }
        return instruction;
    }
    

    static abstract class Instruction {
    	
        protected final String m_name;
        protected final Double m_sequence;

        private Instruction(String name, Double sequence) {
            m_name = name;
            m_sequence = sequence;
        }

        abstract Element createInstructionElement();

        Element createBaseInstructionElement() {
            // create element:
            // <sv:node sv:name="{m_name}"/>
            Element instructionNode = DocumentFactory.getInstance().createElement(NODE_QNAME);
            instructionNode.add(DocumentFactory.getInstance().createAttribute(instructionNode, NAME_QNAME, m_name));
            // create element:
            // <sv:property sv:name="jcr:primaryType" sv:type="Name">
            //   <sv:value>hippo:initializeitem</sv:value>
            // </sv:property>
            Element primaryTypeProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            primaryTypeProperty.add(DocumentFactory.getInstance().createAttribute(primaryTypeProperty, NAME_QNAME, "jcr:primaryType"));
            primaryTypeProperty.add(DocumentFactory.getInstance().createAttribute(primaryTypeProperty, TYPE_QNAME, "Name"));
            Element primaryTypePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            primaryTypePropertyValue.setText("hippo:initializeitem");
            primaryTypeProperty.add(primaryTypePropertyValue);
            instructionNode.add(primaryTypeProperty);
            // create element:
            // <sv:property sv:name="hippo:sequence" sv:type="Double">
            //   <sv:value>{m_sequence}</sv:value>
            // </sv:property>
            Element sequenceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            sequenceProperty.add(DocumentFactory.getInstance().createAttribute(sequenceProperty, NAME_QNAME, "hippo:sequence"));
            sequenceProperty.add(DocumentFactory.getInstance().createAttribute(sequenceProperty, TYPE_QNAME, "Double"));
            Element sequencePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            sequencePropertyValue.setText(String.valueOf(m_sequence));
            sequenceProperty.add(sequencePropertyValue);
            instructionNode.add(sequenceProperty);
            return instructionNode;
        }
    }
    
    static abstract class ResourceInstruction extends Instruction {
    	
    	final File m_file;
        boolean m_changed = false;
    	
    	ResourceInstruction(String name, Double sequence, File file) {
    		super(name, sequence);
    		m_file = file;
    	}
    	
        boolean hasChanged() {
        	return m_changed;
        }

        abstract void export(Session session);
        
        void delete() {
        	m_file.delete();
        }
        
        void nodeAdded(String path) {
        	m_changed = true;
        }
        
        boolean nodeRemoved(String path) {
        	m_changed = true;
        	return false;
        }
        
        void propertyAdded(String path) {
        	m_changed = true;
        }
        
        void propertyChanged(String path) {
        	m_changed = true;
        }
        
        void propertyRemoved(String path) {
        	m_changed = true;
        }
        
    }

    static class ContentResourceInstruction extends ResourceInstruction {
        
        private final String m_root;
        private final String m_context;
        private final boolean m_enabled;
        
        ContentResourceInstruction(String name, Double sequence, File file, String root, String context, boolean enabled) {
            super(name, sequence, file);
            m_root = root;
            m_context = context;
            if (!m_file.exists()) {
            	m_changed = true;
            }
            m_enabled = enabled;
        }
        
        @Override
        synchronized void export(Session session) {
        	if (!m_enabled) {
        		log.info("Export in this context is disabled due to merge semantics. Changes will be lost.");
        		return;
        	}
        	log.debug("Exporting " + m_file.getName());
        	try {
            	if (!m_file.exists()) m_file.createNewFile();
                OutputStream out = null;
                try {
                    out = new FileOutputStream(m_file);
                    SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
                    TransformerHandler handler = stf.newTransformerHandler();
                    Transformer transformer = handler.getTransformer();
                    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
                    handler.setResult(new StreamResult(out));
                    ContentHandler filter = new FilterNamespaceDeclarationsHandler(handler);
                    ((SessionDecorator) session).exportDereferencedView(m_context, filter, true, false);
				} finally {
                    try {
                        out.close();
                    } catch (IOException ex) {}
                }
        	}
        	catch (IOException e) {
        		log.error("Exporting " + m_file.getName() + " failed.", e);
        	}
        	catch (RepositoryException e) {
        		log.error("Exporting " + m_file.getName() + " failed.", e);
	        } 
        	catch (TransformerConfigurationException e) {
        		log.error("Exporting " + m_file.getName() + " failed.", e);
			} 
        	catch (SAXException e) {
        		log.error("Exporting " + m_file.getName() + " failed.", e);
	        }
            m_changed = false;
        }
        
        boolean nodeRemoved(String path) {
        	m_changed = true;
        	return path.equals(m_context);
        }
        
        @Override
        Element createInstructionElement() {
            Element element = createBaseInstructionElement();
            // create element:
            // <sv:property sv:name="hippo:contentresource" sv:type="String">
            //   <sv:value>{this.m_file.getName()}</sv:value>
            // </sv:property>
            Element contentResourceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            contentResourceProperty.add(DocumentFactory.getInstance().createAttribute(contentResourceProperty, NAME_QNAME, "hippo:contentresource"));
            contentResourceProperty.add(DocumentFactory.getInstance().createAttribute(contentResourceProperty, TYPE_QNAME, "String"));
            Element contentResourcePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            contentResourcePropertyValue.setText(m_file.getName());
            contentResourceProperty.add(contentResourcePropertyValue);
            element.add(contentResourceProperty);

            // create element:
            // <sv:property sv:name="hippo:contentroot" sv:type="String">
            //   <sv:value>{this.m_root}</sv:value>
            // </sv:property>
            Element contentRootProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            contentRootProperty.add(DocumentFactory.getInstance().createAttribute(contentRootProperty, NAME_QNAME, "hippo:contentroot"));
            contentRootProperty.add(DocumentFactory.getInstance().createAttribute(contentRootProperty, TYPE_QNAME, "String"));
            Element contentRootPropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            contentRootPropertyValue.setText(m_root);
            contentRootProperty.add(contentRootPropertyValue);
            element.add(contentRootProperty);
            
            return element;
        }
        
        boolean matchesPath(String path) {
        	return path.startsWith(m_context);
        }
        
        @Override
        public String toString() {
        	return "ResourceContentInstruction[context=" + m_context + "]";
        }
        
        // filter out all namespace declarations except {http://www.jcp.org/jcr/sv/1.0}sv
        private static class FilterNamespaceDeclarationsHandler implements ContentHandler {

        	private final ContentHandler m_handler;

        	private String m_svprefix;
        	
        	private FilterNamespaceDeclarationsHandler(ContentHandler handler) {
        		m_handler = handler;
        	}
        	
			@Override
			public void setDocumentLocator(Locator locator) {
				m_handler.setDocumentLocator(locator);
			}

			@Override
			public void startDocument() throws SAXException {
				m_handler.startDocument();
			}

			@Override
			public void endDocument() throws SAXException {
				m_handler.endDocument();
			}

			@Override
			public void startPrefixMapping(String prefix, String uri) throws SAXException {
				// only forward prefix mappings in the jcr/sv namespace
				if (uri.equals("http://www.jcp.org/jcr/sv/1.0")) {
					m_svprefix = prefix;
					m_handler.startPrefixMapping(prefix, uri);
				}
			}

			@Override
			public void endPrefixMapping(String prefix) throws SAXException {
				if (prefix.equals(m_svprefix)) {
					m_handler.endPrefixMapping(prefix);
				}
			}

			@Override
			public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
				m_handler.startElement(uri, localName, qName, atts);
			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				m_handler.endElement(uri, localName, qName);
			}

			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
				m_handler.characters(ch, start, length);
			}

			@Override
			public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
				m_handler.ignorableWhitespace(ch, start, length);
			}

			@Override
			public void processingInstruction(String target, String data) throws SAXException {
				m_handler.processingInstruction(target, data);
			}

			@Override
			public void skippedEntity(String name) throws SAXException {
				m_handler.skippedEntity(name);
			}
        }
        
    }
    
    static class NodetypesResourceInstruction extends ResourceInstruction {
    	
    	private String m_internalPrefix;
    	private final String m_prefix;
    	private final String m_namespace;
    	
    	NodetypesResourceInstruction(String name, Double sequence, File file, String namespace, String internalPrefix) {
    		super(name, sequence, file);
    		m_namespace = namespace;
    		if (!m_file.exists()) {
    			m_changed = true;
    		}
    		m_internalPrefix = internalPrefix;
    		int indexOfUnderscore = internalPrefix.indexOf('_');
    		m_prefix = (indexOfUnderscore == -1) ? internalPrefix : internalPrefix.substring(0, indexOfUnderscore);
    	}

		@Override
		synchronized void export(Session session) {
			log.debug("Exporting to " + m_file.getName());
			try {
				if (!m_file.exists()) m_file.createNewFile();
				Writer out = new FileWriter(m_file);
				try {
					String cnd = null;
					try {
						log.debug("Trying to export cnd for internal prefix " + m_internalPrefix);
						cnd = JcrCompactNodeTypeDefWriter.compactNodeTypeDef(session.getWorkspace(), m_internalPrefix);
						// HACK: we only get events for /jcr:system/jcr:nodeTypes/example_1_1 instead
						// of for /jcr:system/jcr:nodeTypes/example
						// here we fix that prefix
						cnd = cnd.replaceAll(m_internalPrefix, m_prefix);
					} 
					catch (NamespaceException e) {
						log.debug("Failed. Now trying regular prefix " + m_prefix);
						// update all content was already finished, we can use regular prefix
						// but we need to first get a fresh session because the old session
						// does not seem to pick up the last step in update all content
						session = ((HippoSession) session).impersonate(new SimpleCredentials("system", new char[]{}));
						cnd = JcrCompactNodeTypeDefWriter.compactNodeTypeDef(session.getWorkspace(), m_prefix);
					}
					out.write(cnd);
					out.flush();
				}
				finally {
					try {
						out.close();
					}
					catch (IOException e) {}
				}
			}
			catch (IOException e) {
        		log.error("Exporting " + m_file.getName() + " failed.", e);
			}
			catch (RepositoryException e) {
        		log.error("Exporting " + m_file.getName() + " failed.", e);
			}
			m_changed = false;
		}

		@Override
		Element createInstructionElement() {
            Element element = createBaseInstructionElement();
            // create element:
            // <sv:property sv:name="hippo:nodetypesresource" sv:type="String">
            //   <sv:value>{this.m_file.getName()}</sv:value>
            // </sv:property>
            Element cndProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            cndProperty.add(DocumentFactory.getInstance().createAttribute(cndProperty, NAME_QNAME, "hippo:nodetypesresource"));
            cndProperty.add(DocumentFactory.getInstance().createAttribute(cndProperty, TYPE_QNAME, "String"));
            Element cndPropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            cndPropertyValue.setText(m_file.getName());
            cndProperty.add(cndPropertyValue);
            element.add(cndProperty);
            
            if (m_namespace != null) {
                // create element:
                // <sv:property sv:name="hippo:namespace" sv:type="String">
                //   <sv:value>{this.m_namespace}</sv:value>
                // </sv:property>
                Element namespaceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
                namespaceProperty.add(DocumentFactory.getInstance().createAttribute(namespaceProperty, NAME_QNAME, "hippo:namespace"));
                namespaceProperty.add(DocumentFactory.getInstance().createAttribute(namespaceProperty, TYPE_QNAME, "String"));
                Element namespacePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
                namespacePropertyValue.setText(m_namespace);
                namespaceProperty.add(namespacePropertyValue);
                element.add(namespaceProperty);
            }
            
            return element;
		}
		
		
        void nodeAdded(String path) {
        	setInternalPrefixFromPath(path);
        	m_changed = true;
        }
        
        boolean nodeRemoved(String path) {
        	setInternalPrefixFromPath(path);
        	m_changed = true;
        	// TODO: should determine whether or not context was removed
        	return false;
        }
        
        // Don't think this can happen on a node type node
        void propertyAdded(String path) {
        	setInternalPrefixFromPath(path);
        	m_changed = true;
        }
        
        // Don't think this can happen on a node type node
        void propertyChanged(String path) {
        	setInternalPrefixFromPath(path);
        	m_changed = true;
        }
        
        // Don't think this can happen on a node type node
        void propertyRemoved(String path) {
        	setInternalPrefixFromPath(path);
        	m_changed = true;
        }
        
        private void setInternalPrefixFromPath(String path) {
        	// path = /jcr:system/jcr:nodeTypes/example_1_2:doctype/jcr:propertyDefinition
        	// relPath = example_1_2:doctype/jcr:propertyDefinition
        	String relPath = path.substring("/jcr:system/jcr:nodeTypes/".length());
        	// nodeTypeRoot = example_1_2:doctype
        	int indexOfPathSeparator = relPath.indexOf('/');
        	String nodeTypeRoot = (indexOfPathSeparator == -1) ? relPath : relPath.substring(0, indexOfPathSeparator);
        	// internalPrefix = example_1_2
        	int indexOfColon = nodeTypeRoot.indexOf(':');
        	m_internalPrefix = (indexOfColon == -1) ? nodeTypeRoot : nodeTypeRoot.substring(0, indexOfColon);
        }
        
		boolean matchesPrefix(String internalPrefix) {
			// internalPrefix = example_1_2
			// prefix = example
    		int indexOfUnderscore = internalPrefix.indexOf('_');
    		String prefix = (indexOfUnderscore == -1) ? internalPrefix : internalPrefix.substring(0, indexOfUnderscore);
    		return prefix.equals(m_prefix);
		}
		
		@Override
		public String toString() {
			return "NodetypesResourceInstruction[prefix=" + m_prefix + "]"; 
		}
    }

    static class NamespaceInstruction extends Instruction {

    	private final String m_namespace;
    	private final String m_namespaceroot;
    	
    	NamespaceInstruction(String name, Double sequence, String namespace) {
    		super(name, sequence);
    		m_namespace = namespace;
    		int lastIndexOfPathSeparator = namespace.lastIndexOf('/');
    		m_namespaceroot = (lastIndexOfPathSeparator == -1) ? namespace : namespace.substring(0, lastIndexOfPathSeparator);
    	}
    	
		@Override
		Element createInstructionElement() {
            Element element = createBaseInstructionElement();
            // create element:
            // <sv:property sv:name="hippo:namespace" sv:type="String">
            //   <sv:value>{this.m_namespace}</sv:value>
            // </sv:property>
            Element namespaceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
            namespaceProperty.add(DocumentFactory.getInstance().createAttribute(namespaceProperty, NAME_QNAME, "hippo:namespace"));
            namespaceProperty.add(DocumentFactory.getInstance().createAttribute(namespaceProperty, TYPE_QNAME, "String"));
            Element namespacePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
            namespacePropertyValue.setText(m_namespace);
            namespaceProperty.add(namespacePropertyValue);
            element.add(namespaceProperty);
            return element;
		}
		
		boolean matchesNamespace(String namespace) {
			int lastIndexOfPathSeparator = namespace.lastIndexOf('/');
			String namespaceroot = (lastIndexOfPathSeparator == -1) ? namespace : namespace.substring(0, lastIndexOfPathSeparator);
			return namespaceroot.equals(m_namespaceroot);
		}
    			
		@Override
		public String toString() {
			return "NamespaceInstruction[namespace=" + m_namespace + "]"; 
		}
    }
}

