package org.hippoecm.repository.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

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
import org.hippoecm.repository.util.JcrCompactNodeTypeDefWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
            Element root = m_document.getRootElement();
            parseExtension(root);
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
    
    private ResourceInstruction findNodetypesInstruction(String path) {
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
                    return (ResourceInstruction) instruction;
                }
            }
        }
		return null;
    }
    
    private ResourceInstruction findContentResourceInstruction(String path) {
        for (Instruction instruction : m_instructions) {
            if (instruction instanceof ResourceInstruction) {
                if (path.startsWith(((ResourceInstruction) instruction).m_context)) {
                    return (ResourceInstruction) instruction;
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
    
    NamespaceInstruction createNamespaceInstruction(String namespace) throws IOException {
    	// if namespace = http://example.org/example/1.0
    	// then name = example-org-example-1.0
    	URL url = new URL(namespace);
    	String path = url.getPath();
    	String host = url.getHost();
    	String name = host.replace('.','-') + path.replace('/', '-');
    	return new NamespaceInstruction(name, 3000.0, namespace);
    }

    private ContentResourceInstruction createContentResourceInstruction(String path) {
    	// path = /hippo:namespaces/example
    	// name = example
    	// root = /hippo:namespaces
        int lastIndexOfPathSeparator = path.lastIndexOf('/');
        String name = path.substring(lastIndexOfPathSeparator+1);
        String root = path.substring(0, lastIndexOfPathSeparator);
        if (root.equals("")) root = "/";
        File file = new File(m_file.getParent(), name.replace(':', '$') + ".xml");
        return new ContentResourceInstruction(name, 3000.3, file, root);
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
            instruction = new ContentResourceInstruction(name, sequence, file, contentroot);
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
        String m_context;
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
        	return m_context.equals(path);
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
        
        ContentResourceInstruction(String name, Double sequence, File file, String root) {
            super(name, sequence, file);
            m_root = root;
            m_context = m_root.equals("/") ? m_root + m_name : m_root + "/" + m_name;
            if (!m_file.exists()) {
            	m_changed = true;
            }
            else {
            	parseContent();
            }
        }
        
        @Override
        synchronized void export(Session session) {
        	log.debug("Exporting " + m_file.getName());
        	try {
            	if (!m_file.exists()) m_file.createNewFile();
                OutputStream out = null;
                try {
                    out = new FileOutputStream(m_file);
                    ((HippoSession) session).exportDereferencedView(m_context, out, true, false);
                } finally {
                    try {
                        out.close();
                    } catch (IOException ex) {}
                }
        	}
        	catch (IOException e) {
        		log.error("Exporting " + m_file.getName() + " failed.");
        	}
        	catch (RepositoryException e) {
        		log.error("Exporting " + m_file.getName() + " failed.");
        	}
            m_changed = false;
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
        
        @Override
        public String toString() {
        	return "ResourceContentInstruction[context=" + m_context + "]";
        }
        
        private void parseContent() {}
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
    
    static class NodetypesResourceInstruction extends ResourceInstruction {
    	
    	private String m_internalPrefix;
    	private final String m_prefix;
    	private final String m_namespace;
    	
    	NodetypesResourceInstruction(String name, Double sequence, File file, String namespace, String internalPrefix) {
    		super(name, sequence, file);
    		m_namespace = namespace;
    		m_context = "/jcr:system/jcr:nodeTypes/" + m_name;
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
					String cnd = JcrCompactNodeTypeDefWriter.compactNodeTypeDef(session.getWorkspace(),m_internalPrefix);
					// HACK: we only get events for /jcr:system/jcr:nodeTypes/example_1_1 instead
					// of for /jcr:system/jcr:nodeTypes/example
					// here we fix that prefix
					cnd = cnd.replaceAll(m_internalPrefix, m_prefix);
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
        		log.error("Exporting " + m_file.getName() + " failed.");
			}
			catch (RepositoryException e) {
        		log.error("Exporting " + m_file.getName() + " failed.");
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
        	return m_context.equals(path);
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
			return "NodetypesResourceInstruction[context=" + m_context + "]"; 
		}
    }

}

